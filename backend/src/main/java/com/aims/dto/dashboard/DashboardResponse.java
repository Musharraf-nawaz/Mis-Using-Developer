package com.aims.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardResponse {
    private AssetStats assetStats;
    private InterviewStats interviewStats;
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
