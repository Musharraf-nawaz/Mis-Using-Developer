package com.aims.dto.asset;

import com.aims.entity.enums.AssignmentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssetAssignmentResponse {
    private Long id;
    private Long assetId;
    private String assetName;
    private String assetTag;
    private Long employeeId;
    private String employeeName;
    private String employeeDepartment;
    private LocalDate assignedDate;
    private LocalDate expectedReturnDate;
    private LocalDate actualReturnDate;
    private AssignmentStatus status;
    private String remarks;
    private LocalDateTime createdAt;
}
