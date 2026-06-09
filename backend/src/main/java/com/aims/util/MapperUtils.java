package com.aims.util;

import com.aims.dto.asset.AssetAssignmentResponse;
import com.aims.dto.asset.AssetMediaResponse;
import com.aims.dto.asset.AssetResponse;
import com.aims.dto.audit.AuditLogResponse;
import com.aims.dto.interview.InterviewResponse;
import com.aims.dto.notification.NotificationResponse;
import com.aims.dto.user.UserResponse;
import com.aims.entity.*;
import com.aims.entity.enums.MediaType;

import java.util.List;

public final class MapperUtils {

    private MapperUtils() {}

    public static UserResponse toUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(user.getRole())
                .department(user.getDepartment())
                .phone(user.getPhone())
                .employeeId(user.getEmployeeId())
                .status(user.getStatus())
                .createdAt(user.getCreatedAt())
                .build();
    }

    public static AssetResponse toAssetResponse(Asset asset) {
        return toAssetResponse(asset, List.of());
    }

    public static AssetResponse toAssetResponse(Asset asset, List<AssetMedia> mediaList) {
        String photoUrl = null;
        String videoUrl = null;
        List<AssetMediaResponse> mediaResponses = mediaList.stream()
                .map(MapperUtils::toMediaResponse)
                .toList();
        for (AssetMedia m : mediaList) {
            if (m.getMediaType() == MediaType.PHOTO) photoUrl = m.getFileUrl();
            if (m.getMediaType() == MediaType.VIDEO) videoUrl = m.getFileUrl();
        }
        return AssetResponse.builder()
                .id(asset.getId())
                .companyName(asset.getCompanyName())
                .assetName(asset.getAssetName())
                .associatedDeveloper(asset.getAssociatedDeveloper())
                .projectName(asset.getProjectName())
                .assetCategory(asset.getAssetCategory())
                .assetType(asset.getAssetType())
                .serialNumber(asset.getSerialNumber())
                .assetTag(asset.getAssetTag())
                .purchaseDate(asset.getPurchaseDate())
                .purchaseCost(asset.getPurchaseCost())
                .assignedToId(asset.getAssignedTo() != null ? asset.getAssignedTo().getId() : null)
                .assignedToName(asset.getAssignedTo() != null ? asset.getAssignedTo().getFullName() : null)
                .assignedDate(asset.getAssignedDate())
                .returnDate(asset.getReturnDate())
                .projectOffboarded(asset.getProjectOffboarded())
                .status(asset.getStatus())
                .warrantyExpiryDate(asset.getWarrantyExpiryDate())
                .vendorName(asset.getVendorName())
                .photoUrl(photoUrl)
                .videoUrl(videoUrl)
                .media(mediaResponses)
                .condition(asset.getCondition())
                .remarks(asset.getRemarks())
                .createdAt(asset.getCreatedAt())
                .updatedAt(asset.getUpdatedAt())
                .build();
    }

    public static AssetMediaResponse toMediaResponse(AssetMedia media) {
        return AssetMediaResponse.builder()
                .id(media.getId())
                .mediaType(media.getMediaType())
                .fileUrl(media.getFileUrl())
                .fileName(media.getFileName())
                .uploadedAt(media.getUploadedAt())
                .build();
    }

    public static AssetAssignmentResponse toAssignmentResponse(AssetAssignment aa) {
        return AssetAssignmentResponse.builder()
                .id(aa.getId())
                .assetId(aa.getAsset().getId())
                .assetName(aa.getAsset().getAssetName())
                .assetTag(aa.getAsset().getAssetTag())
                .employeeId(aa.getEmployee().getId())
                .employeeName(aa.getEmployeeName())
                .employeeDepartment(aa.getEmployeeDepartment())
                .assignedDate(aa.getAssignedDate())
                .expectedReturnDate(aa.getExpectedReturnDate())
                .actualReturnDate(aa.getActualReturnDate())
                .status(aa.getStatus())
                .remarks(aa.getRemarks())
                .createdAt(aa.getCreatedAt())
                .build();
    }

    public static InterviewResponse toInterviewResponse(Interview interview) {
        return InterviewResponse.builder()
                .id(interview.getId())
                .candidateName(interview.getCandidateName())
                .candidateEmail(interview.getCandidateEmail())
                .candidatePhone(interview.getCandidatePhone())
                .candidateProfile(interview.getCandidateProfile())
                .clientName(interview.getClientName())
                .companyToRepresent(interview.getCompanyToRepresent())
                .interviewLink(interview.getInterviewLink())
                .candidateCvUrl(interview.getCandidateCvUrl())
                .finalStatus(interview.getFinalStatus())
                .skills(interview.getSkills())
                .experience(interview.getExperience())
                .interviewerName(interview.getInterviewerName())
                .interviewerEmail(interview.getInterviewerEmail())
                .interviewerId(interview.getInterviewer() != null ? interview.getInterviewer().getId() : null)
                .interviewDate(interview.getInterviewDate())
                .interviewTime(interview.getInterviewTime())
                .interviewMode(interview.getInterviewMode())
                .interviewRound(interview.getInterviewRound())
                .interviewStatus(interview.getInterviewStatus())
                .feedback(interview.getFeedback())
                .notes(interview.getNotes())
                .createdAt(interview.getCreatedAt())
                .updatedAt(interview.getUpdatedAt())
                .build();
    }

    public static NotificationResponse toNotificationResponse(Notification n) {
        return NotificationResponse.builder()
                .id(n.getId())
                .title(n.getTitle())
                .message(n.getMessage())
                .type(n.getType())
                .entityType(n.getEntityType())
                .entityId(n.getEntityId())
                .read(n.getRead())
                .createdAt(n.getCreatedAt())
                .build();
    }

    public static AuditLogResponse toAuditLogResponse(AuditLog log) {
        return AuditLogResponse.builder()
                .id(log.getId())
                .action(log.getAction())
                .userEmail(log.getUserEmail())
                .entityType(log.getEntityType())
                .entityId(log.getEntityId())
                .oldValue(log.getOldValue())
                .newValue(log.getNewValue())
                .createdAt(log.getCreatedAt())
                .build();
    }
}
