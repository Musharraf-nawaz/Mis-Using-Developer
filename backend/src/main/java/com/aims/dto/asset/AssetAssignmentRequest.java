package com.aims.dto.asset;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class AssetAssignmentRequest {
    @NotNull
    private Long assetId;
    @NotNull
    private Long employeeId;
    private String employeeName;
    private String employeeDepartment;
    @NotNull
    private LocalDate assignedDate;
    private LocalDate expectedReturnDate;
    private String remarks;
}
