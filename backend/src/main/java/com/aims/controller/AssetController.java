package com.aims.controller;

import com.aims.dto.asset.AssetAssignmentRequest;
import com.aims.dto.asset.AssetAssignmentResponse;
import com.aims.dto.asset.AssetRequest;
import com.aims.dto.asset.AssetResponse;
import com.aims.dto.common.ApiResponse;
import com.aims.dto.common.PageResponse;
import com.aims.entity.enums.AssetStatus;
import com.aims.service.AssetService;
import com.aims.service.ExportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.aims.entity.enums.MediaType;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/assets")
@RequiredArgsConstructor
@Tag(name = "Asset Management")
public class AssetController {

    private final AssetService assetService;
    private final ExportService exportService;

    @GetMapping
    @Operation(summary = "Get all assets with filters")
    public ResponseEntity<ApiResponse<PageResponse<AssetResponse>>> getAll(
            @RequestParam(required = false, defaultValue = "") String search,
            @RequestParam(required = false) String company,
            @RequestParam(required = false) String assetType,
            @RequestParam(required = false) AssetStatus status,
            @RequestParam(required = false) Long assignedToId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.fromString(sortDir), sortBy));
        return ResponseEntity.ok(ApiResponse.success(
                assetService.getAll(search, company, assetType, status, assignedToId, pageable)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AssetResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(assetService.getById(id)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AssetResponse>> create(@Valid @RequestBody AssetRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Asset created", assetService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AssetResponse>> update(@PathVariable Long id, @Valid @RequestBody AssetRequest request) {
        return ResponseEntity.ok(ApiResponse.success(assetService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        assetService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Asset deleted", null));
    }

    @PostMapping("/{id}/media")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AssetResponse>> uploadMedia(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            @RequestParam("type") MediaType type) {
        return ResponseEntity.ok(ApiResponse.success(assetService.uploadMedia(id, file, type)));
    }

    @PatchMapping("/{id}/offboarded")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AssetResponse>> updateOffboarded(
            @PathVariable Long id,
            @RequestBody Map<String, Boolean> body) {
        boolean offboarded = body != null && Boolean.TRUE.equals(body.get("offboarded"));
        return ResponseEntity.ok(ApiResponse.success(
                assetService.updateOffboardedStatus(id, offboarded)));
    }

    @PostMapping("/assign")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AssetAssignmentResponse>> assign(@Valid @RequestBody AssetAssignmentRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Asset assigned", assetService.assign(request)));
    }

    @PostMapping("/return/{assignmentId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AssetAssignmentResponse>> returnAsset(
            @PathVariable Long assignmentId, @RequestBody(required = false) Map<String, String> body) {
        String remarks = body != null ? body.get("remarks") : null;
        return ResponseEntity.ok(ApiResponse.success(assetService.returnAsset(assignmentId, remarks)));
    }

    @GetMapping("/{id}/history")
    public ResponseEntity<ApiResponse<List<AssetAssignmentResponse>>> getHistory(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(assetService.getAssignmentHistory(id)));
    }

    @GetMapping("/export/csv")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> exportCsv() throws Exception {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=assets.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(exportService.exportAssetsToCsv());
    }

    @GetMapping("/export/excel")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> exportExcel() throws Exception {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=assets.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(exportService.exportAssetsToExcel());
    }
}
