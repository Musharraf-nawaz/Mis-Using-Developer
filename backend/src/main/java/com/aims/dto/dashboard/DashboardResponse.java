package com.aims.dto.dashboard;

import com.aims.entity.enums.AssetStatus;
import com.aims.entity.enums.ProjectStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardResponse {
    private boolean admin;
    private AssetStats assetStats;
    private InterviewStats interviewStats;
    private ProjectStats projectStats;
    private UserDashboardStats userStats;
    private List<ProjectSummary> assignedProjects;
    private List<AssignedAssetItem> assignedAssets;
    private List<Map<String, Object>> assetStatusDistribution;
    private List<Map<String, Object>> monthlyInterviewStats;
    private List<Map<String, Object>> assetAllocationTrends;
    private List<ActivityItem> recentAssignments;
    private List<ActivityItem> recentReturns;
    private List<ActivityItem> upcomingInterviews;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AssetStats {
        private long totalAssets;
        private long availableAssets;
        private long assignedAssets;
        private long returnedAssets;
        private long damagedAssets;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InterviewStats {
        private long todayInterviews;
        private long upcomingInterviews;
        private long completedInterviews;
        private long cancelledInterviews;
        private long scheduledInterviews;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProjectStats {
        private long totalProjects;
        private long activeProjects;
        private BigDecimal totalBudget;
        private long workingCandidates;
        private long interviewCandidates;
        private long onboardedCandidates;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserDashboardStats {
        private long assignedProjects;
        private long workingCandidates;
        private long onboardedCandidates;
        private long assignedAssets;
        private long upcomingInterviews;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProjectSummary {
        private Long id;
        private String projectName;
        private Integer candidateWorkingCount;
        private Integer onboardedCandidateCount;
        private ProjectStatus status;
        private String remarks;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AssignedAssetItem {
        private Long id;
        private String assetName;
        private String serialNumber;
        private String assignedDate;
        private AssetStatus status;
        private String photoUrl;
        private String videoUrl;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActivityItem {
        private Long id;
        private String title;
        private String description;
        private String type;
        private String timestamp;
        private String interviewDate;
        private String interviewTime;
        private Boolean today;
    }
}
