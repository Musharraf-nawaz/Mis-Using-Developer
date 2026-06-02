package com.aims.dto.asset;

import com.aims.entity.enums.AssetStatus;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class AssetRequest {
    @NotBlank
    private String companyName;
    @NotBlank
    private String assetName;
    private String assetCategory;
    private String assetType;
    private String associatedDeveloper;
    private String projectName;
    private String serialNumber;
    private String assetTag;
    private LocalDate assignedDate;
    private LocalDate purchaseDate;
    private BigDecimal purchaseCost;
    private AssetStatus status;
    private Boolean projectOffboarded;
    private String condition;
    private String remarks;
}
