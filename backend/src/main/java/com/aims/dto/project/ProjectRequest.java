package com.aims.dto.project;

import com.aims.entity.enums.ProjectStatus;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class ProjectRequest {
    @NotBlank
    private String projectName;
    @NotBlank
    private String clientName;
    private String midClientName;
    private Integer candidateWorkingCount;
    private Integer interviewCandidateCount;
    private Integer onboardedCandidateCount;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal budget;
    private ProjectStatus status;
    private String remarks;
    private List<Long> assignedUserIds;
}
