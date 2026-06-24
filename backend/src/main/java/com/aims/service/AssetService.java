package com.aims.service;

import com.aims.dto.asset.AssetAssignmentRequest;
import com.aims.dto.asset.AssetAssignmentResponse;
import com.aims.dto.asset.AssetRequest;
import com.aims.dto.asset.AssetResponse;
import com.aims.dto.common.PageResponse;
import com.aims.entity.Asset;
import com.aims.entity.AssetAssignment;
import com.aims.entity.AssetMedia;
import com.aims.entity.User;
import com.aims.entity.enums.AssetStatus;
import com.aims.entity.enums.MediaType;
import com.aims.entity.enums.AssignmentStatus;
import com.aims.entity.enums.Role;
import com.aims.exception.BadRequestException;
import com.aims.exception.ResourceNotFoundException;
import com.aims.repository.AssetAssignmentRepository;
import com.aims.repository.AssetMediaRepository;
import com.aims.repository.AssetRepository;
import com.aims.repository.UserRepository;
import com.aims.security.UserPrincipal;
import com.aims.util.MapperUtils;
import com.aims.util.SecurityUtils;
import com.aims.util.AssetMediaUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AssetService {

    private final AssetRepository assetRepository;
    private final AssetAssignmentRepository assignmentRepository;
    private final AssetMediaRepository assetMediaRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;
    private final AuditService auditService;
    private final NotificationService notificationService;

    @Transactional(readOnly = true)
    public PageResponse<AssetResponse> getAll(String search, String company, String assetType,
                                               AssetStatus status, Long assignedToId, Pageable pageable) {
        UserPrincipal current = SecurityUtils.getCurrentUser();
        if (current != null && Role.USER.name().equals(current.getRole())) {
            assignedToId = current.getId();
        }
        Page<Asset> page = assetRepository.findWithFilters(search, company, assetType, status, assignedToId, pageable);
        List<Long> assetIds = page.getContent().stream().map(Asset::getId).toList();
        Map<Long, List<AssetMedia>> mediaByAsset = assetIds.isEmpty()
                ? Map.of()
                : AssetMediaUtils.groupByAssetId(assetMediaRepository.findByAssetIdIn(assetIds));
        return PageResponse.from(page.map(a -> MapperUtils.toAssetSummaryResponse(
                a, mediaByAsset.getOrDefault(a.getId(), List.of()))));
    }

    @Transactional(readOnly = true)
    public AssetResponse getById(Long id) {
        Asset asset = findAsset(id);
        return MapperUtils.toAssetResponse(asset, assetMediaRepository.findByAssetId(id));
    }

    @Transactional
    public AssetResponse create(AssetRequest request) {
        normalizeOptionalFields(request);
        if (request.getSerialNumber() != null && assetRepository.existsBySerialNumber(request.getSerialNumber())) {
            throw new BadRequestException("Serial number already exists");
        }
        if (request.getAssetTag() != null && assetRepository.existsByAssetTag(request.getAssetTag())) {
            throw new BadRequestException("Asset tag already exists");
        }
        Asset asset = mapToEntity(new Asset(), request);
        asset.setStatus(request.getStatus() != null ? request.getStatus() : AssetStatus.AVAILABLE);
        asset = assetRepository.save(asset);
        auditService.log("ASSET_CREATED", "ASSET", asset.getId(), null, asset.getAssetName());
        return MapperUtils.toAssetResponse(asset, List.of());
    }

    @Transactional
    public AssetResponse update(Long id, AssetRequest request) {
        normalizeOptionalFields(request);
        Asset asset = findAsset(id);
        asset = mapToEntity(asset, request);
        asset = assetRepository.save(asset);
        auditService.log("ASSET_UPDATED", "ASSET", asset.getId(), null, asset.getAssetName());
        return MapperUtils.toAssetResponse(asset, assetMediaRepository.findByAssetId(id));
    }

    @Transactional
    public AssetResponse uploadMedia(Long id, MultipartFile file, MediaType mediaType) {
        Asset asset = findAsset(id);
        String category = mediaType == MediaType.PHOTO ? "photo" : "video";
        String fileUrl = fileStorageService.store(file, category);
        assetMediaRepository.findByAssetIdAndMediaType(id, mediaType).forEach(assetMediaRepository::delete);
        AssetMedia media = AssetMedia.builder()
                .asset(asset)
                .mediaType(mediaType)
                .fileUrl(fileUrl)
                .fileName(file.getOriginalFilename())
                .build();
        assetMediaRepository.save(media);
        auditService.log("ASSET_MEDIA_UPLOADED", "ASSET", id, null, mediaType.name());
        return MapperUtils.toAssetResponse(asset, assetMediaRepository.findByAssetId(id));
    }

    @Transactional
    public void delete(Long id) {
        Asset asset = findAsset(id);
        if (asset.getStatus() == AssetStatus.ASSIGNED) {
            throw new BadRequestException("Cannot delete assigned asset");
        }
        assetRepository.delete(asset);
        auditService.log("ASSET_DELETED", "ASSET", id, asset.getAssetName(), null);
    }

    @Transactional
    public AssetResponse updateOffboardedStatus(Long id, boolean offboarded) {
        Asset asset = findAsset(id);
        asset.setProjectOffboarded(offboarded);
        if (offboarded && asset.getStatus() == AssetStatus.ASSIGNED) {
            asset.setStatus(AssetStatus.RETURNED);
            asset.setReturnDate(LocalDate.now());
        }
        asset = assetRepository.save(asset);
        auditService.log("ASSET_OFFBOARDED_UPDATED", "ASSET", id, null, String.valueOf(offboarded));
        return MapperUtils.toAssetResponse(asset);
    }

    @Transactional
    public AssetAssignmentResponse assign(AssetAssignmentRequest request) {
        Asset asset = findAsset(request.getAssetId());
        if (asset.getStatus() != AssetStatus.AVAILABLE && asset.getStatus() != AssetStatus.RETURNED) {
            throw new BadRequestException("Asset is not available for assignment");
        }
        User employee = userRepository.findById(request.getEmployeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));

        AssetAssignment assignment = AssetAssignment.builder()
                .asset(asset)
                .employee(employee)
                .employeeName(request.getEmployeeName() != null ? request.getEmployeeName() : employee.getFullName())
                .employeeDepartment(request.getEmployeeDepartment() != null ?
                        request.getEmployeeDepartment() : employee.getDepartment())
                .assignedDate(request.getAssignedDate())
                .expectedReturnDate(request.getExpectedReturnDate())
                .status(AssignmentStatus.ACTIVE)
                .remarks(request.getRemarks())
                .build();
        assignment = assignmentRepository.save(assignment);

        asset.setAssignedTo(employee);
        asset.setAssignedDate(request.getAssignedDate());
        asset.setStatus(AssetStatus.ASSIGNED);
        assetRepository.save(asset);

        auditService.log("ASSET_ASSIGNED", "ASSET", asset.getId(), null, employee.getFullName());
        notificationService.notify(employee.getId(), "Asset Assigned",
                "Asset " + asset.getAssetName() + " has been assigned to you.",
                "ASSET_ASSIGNED", "ASSET", asset.getId(), true);

        return MapperUtils.toAssignmentResponse(assignment);
    }

    @Transactional
    public AssetAssignmentResponse returnAsset(Long assignmentId, String remarks) {
        AssetAssignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found"));
        if (assignment.getStatus() != AssignmentStatus.ACTIVE) {
            throw new BadRequestException("Assignment is not active");
        }

        assignment.setStatus(AssignmentStatus.RETURNED);
        assignment.setActualReturnDate(LocalDate.now());
        assignment.setRemarks(remarks);
        assignmentRepository.save(assignment);

        Asset asset = assignment.getAsset();
        asset.setAssignedTo(null);
        asset.setReturnDate(LocalDate.now());
        asset.setStatus(AssetStatus.RETURNED);
        assetRepository.save(asset);

        auditService.log("ASSET_RETURNED", "ASSET", asset.getId(), null, asset.getAssetName());
        return MapperUtils.toAssignmentResponse(assignment);
    }

    @Transactional(readOnly = true)
    public List<AssetAssignmentResponse> getAssignmentHistory(Long assetId) {
        return assignmentRepository.findByAssetIdOrderByCreatedAtDesc(assetId).stream()
                .map(MapperUtils::toAssignmentResponse)
                .toList();
    }

    private Asset findAsset(Long id) {
        return assetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Asset not found with id: " + id));
    }

    private Asset mapToEntity(Asset asset, AssetRequest request) {
        asset.setCompanyName(request.getCompanyName());
        asset.setAssetName(request.getAssetName());
        asset.setAssociatedDeveloper(request.getAssociatedDeveloper());
        asset.setProjectName(request.getProjectName());
        asset.setAssetCategory(
                request.getAssetCategory() != null && !request.getAssetCategory().isBlank()
                        ? request.getAssetCategory()
                        : "GENERAL"
        );
        asset.setAssetType(
                request.getAssetType() != null && !request.getAssetType().isBlank()
                        ? request.getAssetType()
                        : "IT_ASSET"
        );
        asset.setSerialNumber(request.getSerialNumber());
        asset.setAssetTag(request.getAssetTag());
        asset.setAssignedDate(request.getAssignedDate());
        asset.setPurchaseDate(request.getPurchaseDate());
        asset.setPurchaseCost(request.getPurchaseCost());
        asset.setProjectOffboarded(request.getProjectOffboarded() != null && request.getProjectOffboarded());
        if (request.getStatus() != null) asset.setStatus(request.getStatus());
        asset.setWarrantyExpiryDate(request.getWarrantyExpiryDate());
        asset.setVendorName(request.getVendorName());
        asset.setCondition(request.getCondition());
        asset.setRemarks(request.getRemarks());
        return asset;
    }

    private void normalizeOptionalFields(AssetRequest request) {
        if (request.getSerialNumber() != null && request.getSerialNumber().isBlank()) {
            request.setSerialNumber(null);
        }
        if (request.getAssetTag() != null && request.getAssetTag().isBlank()) {
            request.setAssetTag(null);
        }
    }
}
