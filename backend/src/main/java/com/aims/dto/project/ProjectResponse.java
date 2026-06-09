package com.aims.dto.project;

import com.aims.entity.enums.ProjectStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class ProjectResponse {
    private Long id;
    private String projectName;
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
