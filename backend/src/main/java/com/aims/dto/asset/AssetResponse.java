package com.aims.dto.asset;

import com.aims.entity.enums.AssetStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssetResponse {
    private Long id;
    private String companyName;
    private String assetName;
    private String associatedDeveloper;
    private String projectName;
    private String assetCategory;
    private String assetType;
    private String serialNumber;
    private String assetTag;
    private LocalDate purchaseDate;
    private BigDecimal purchaseCost;
    private Long assignedToId;
    private String assignedToName;
    private LocalDate assignedDate;
    private LocalDate returnDate;
    private Boolean projectOffboarded;
    private AssetStatus status;
    private LocalDate warrantyExpiryDate;
    private String vendorName;
    private String photoUrl;
    private String videoUrl;
    private List<AssetMediaResponse> media;
    private String condition;
    private String remarks;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
