package com.aims.service;

import com.aims.dto.dashboard.DashboardResponse;
import com.aims.entity.Asset;
import com.aims.entity.AssetAssignment;
import com.aims.entity.AssetMedia;
import com.aims.entity.Interview;
import com.aims.entity.Project;
import com.aims.entity.enums.AssetStatus;
import com.aims.entity.enums.AssignmentStatus;
import com.aims.entity.enums.InterviewStatus;
import com.aims.entity.enums.ProjectStatus;
import com.aims.entity.enums.Role;
import com.aims.repository.AssetAssignmentRepository;
import com.aims.repository.AssetMediaRepository;
import com.aims.repository.AssetRepository;
import com.aims.repository.InterviewRepository;
import com.aims.repository.ProjectRepository;
import com.aims.security.UserPrincipal;
import com.aims.util.AssetMediaUtils;
import com.aims.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final int MAX_ASSIGNED_ASSETS = 6;

    private final AssetRepository assetRepository;
    private final AssetAssignmentRepository assignmentRepository;
    private final AssetMediaRepository assetMediaRepository;
    private final InterviewRepository interviewRepository;
    private final ProjectRepository projectRepository;

    @Transactional(readOnly = true)
    @Cacheable(value = "dashboard", key = "T(com.aims.util.SecurityUtils).dashboardCacheKey()")
    public DashboardResponse getDashboard() {
        try {
            return buildDashboard();
        } catch (Exception ex) {
            log.error("Dashboard load failed, returning fallback response", ex);
            return buildFallbackDashboard();
        }
    }

    private DashboardResponse buildDashboard() {
        LocalDate today = LocalDate.now();
        UserPrincipal current = SecurityUtils.getCurrentUser();
        boolean isAdmin = current != null && Role.ADMIN.name().equals(current.getRole());

        Map<String, Long> assetCounts = loadAssetCounts();
        long totalAssets = assetCounts.values().stream().mapToLong(Long::longValue).sum();

        DashboardResponse.AssetStats assetStats = DashboardResponse.AssetStats.builder()
                .totalAssets(totalAssets)
                .availableAssets(assetCounts.getOrDefault(AssetStatus.AVAILABLE.name(), 0L))
                .assignedAssets(assetCounts.getOrDefault(AssetStatus.ASSIGNED.name(), 0L))
                .returnedAssets(assetCounts.getOrDefault(AssetStatus.RETURNED.name(), 0L))
                .damagedAssets(assetCounts.getOrDefault(AssetStatus.DAMAGED.name(), 0L))
                .build();

        Map<String, Long> interviewCounts = loadInterviewCounts();
        long scheduled = interviewCounts.getOrDefault(InterviewStatus.SCHEDULED.name(), 0L);

        DashboardResponse.InterviewStats interviewStats = DashboardResponse.InterviewStats.builder()
                .todayInterviews(interviewRepository.countByInterviewDateAndInterviewStatus(
                        today, InterviewStatus.SCHEDULED))
                .upcomingInterviews(scheduled)
                .completedInterviews(interviewCounts.getOrDefault(InterviewStatus.COMPLETED.name(), 0L))
                .cancelledInterviews(interviewCounts.getOrDefault(InterviewStatus.CANCELLED.name(), 0L))
                .scheduledInterviews(scheduled)
                .build();

        DashboardResponse.ProjectStats projectStats = null;
        DashboardResponse.UserDashboardStats userStats = null;
        List<DashboardResponse.ProjectSummary> assignedProjects = List.of();
        List<DashboardResponse.AssignedAssetItem> assignedAssets = List.of();

        if (isAdmin) {
            try {
                Object[] sums = normalizeAggregateRow(projectRepository.sumCandidateCounts());
                projectStats = DashboardResponse.ProjectStats.builder()
                        .totalProjects(projectRepository.count())
                        .activeProjects(projectRepository.countByStatus(ProjectStatus.ACTIVE))
                        .totalBudget(projectRepository.sumBudget())
                        .workingCandidates(aggregateAt(sums, 0))
                        .interviewCandidates(aggregateAt(sums, 1))
                        .onboardedCandidates(aggregateAt(sums, 2))
                        .build();
            } catch (Exception ex) {
                log.warn("Failed to load admin project stats: {}", ex.getMessage());
                projectStats = DashboardResponse.ProjectStats.builder()
                        .totalProjects(0)
                        .activeProjects(0)
                        .totalBudget(java.math.BigDecimal.ZERO)
                        .workingCandidates(0)
                        .interviewCandidates(0)
                        .onboardedCandidates(0)
                        .build();
            }
        } else if (current != null) {
            try {
            List<Project> myProjects = projectRepository.findByAssignedUserId(current.getId());
            long working = myProjects.stream()
                    .mapToLong(p -> p.getCandidateWorkingCount() != null ? p.getCandidateWorkingCount() : 0)
                    .sum();
            long onboarded = myProjects.stream()
                    .mapToLong(p -> p.getOnboardedCandidateCount() != null ? p.getOnboardedCandidateCount() : 0)
                    .sum();
            long myAssets = assetRepository.countByAssignedToIdAndStatus(current.getId(), AssetStatus.ASSIGNED);
            assignedProjects = myProjects.stream()
                    .map(p -> DashboardResponse.ProjectSummary.builder()
                            .id(p.getId())
                            .projectName(p.getProjectName())
                            .candidateWorkingCount(p.getCandidateWorkingCount())
                            .onboardedCandidateCount(p.getOnboardedCandidateCount())
                            .status(p.getStatus())
                            .remarks(p.getRemarks())
                            .build())
                    .toList();
            assignedAssets = buildAssignedAssets(
                    assetRepository.findByAssignedToId(current.getId()).stream().limit(MAX_ASSIGNED_ASSETS).toList());
            userStats = DashboardResponse.UserDashboardStats.builder()
                    .assignedProjects(myProjects.size())
                    .workingCandidates(working)
                    .onboardedCandidates(onboarded)
                    .assignedAssets(myAssets)
                    .upcomingInterviews(interviewStats.getUpcomingInterviews())
                    .build();
            } catch (Exception ex) {
                log.warn("Failed to load user dashboard stats: {}", ex.getMessage());
            }
        }

        List<Map<String, Object>> assetStatusDistribution = assetCounts.entrySet().stream()
                .map(e -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("status", e.getKey());
                    item.put("count", e.getValue());
                    return item;
                })
                .toList();

        List<Map<String, Object>> monthlyInterviewStats = safeMonthlyInterviewStats(today);
        List<Map<String, Object>> assetAllocationTrends = safeAssetAllocationTrends(today);
        List<DashboardResponse.ActivityItem> recentAssignments = safeRecentAssignments();
        List<DashboardResponse.ActivityItem> recentReturns = safeRecentReturns();
        List<DashboardResponse.ActivityItem> upcomingInterviews = safeUpcomingInterviews(today);

        return DashboardResponse.builder()
                .admin(isAdmin)
                .assetStats(assetStats)
                .interviewStats(interviewStats)
                .projectStats(projectStats)
                .userStats(userStats)
                .assignedProjects(assignedProjects)
                .assignedAssets(assignedAssets)
                .assetStatusDistribution(assetStatusDistribution)
                .monthlyInterviewStats(monthlyInterviewStats)
                .assetAllocationTrends(assetAllocationTrends)
                .recentAssignments(recentAssignments)
                .recentReturns(recentReturns)
                .upcomingInterviews(upcomingInterviews)
                .build();
    }

    private DashboardResponse buildFallbackDashboard() {
        UserPrincipal current = SecurityUtils.getCurrentUser();
        boolean isAdmin = current != null && Role.ADMIN.name().equals(current.getRole());
        DashboardResponse.AssetStats emptyAssets = DashboardResponse.AssetStats.builder()
                .totalAssets(0).availableAssets(0).assignedAssets(0).returnedAssets(0).damagedAssets(0)
                .build();
        DashboardResponse.InterviewStats emptyInterviews = DashboardResponse.InterviewStats.builder()
                .todayInterviews(0).upcomingInterviews(0).completedInterviews(0)
                .cancelledInterviews(0).scheduledInterviews(0)
                .build();
        return DashboardResponse.builder()
                .admin(isAdmin)
                .assetStats(emptyAssets)
                .interviewStats(emptyInterviews)
                .assignedProjects(List.of())
                .assignedAssets(List.of())
                .assetStatusDistribution(List.of())
                .monthlyInterviewStats(List.of())
                .assetAllocationTrends(List.of())
                .recentAssignments(List.of())
                .recentReturns(List.of())
                .upcomingInterviews(List.of())
                .build();
    }

    private List<DashboardResponse.AssignedAssetItem> buildAssignedAssets(List<Asset> assets) {
        if (assets.isEmpty()) return List.of();
        List<Long> assetIds = assets.stream().map(Asset::getId).toList();
        Map<Long, List<AssetMedia>> mediaByAsset = AssetMediaUtils.groupByAssetId(
                assetMediaRepository.findByAssetIdIn(assetIds));
        return assets.stream()
                .map(asset -> {
                    List<AssetMedia> media = mediaByAsset.getOrDefault(asset.getId(), List.of());
                    return DashboardResponse.AssignedAssetItem.builder()
                            .id(asset.getId())
                            .assetName(asset.getAssetName())
                            .serialNumber(asset.getSerialNumber())
                            .assignedDate(asset.getAssignedDate() != null ? asset.getAssignedDate().toString() : null)
                            .status(asset.getStatus())
                            .photoUrl(AssetMediaUtils.photoUrl(media))
                            .videoUrl(null)
                            .build();
                })
                .toList();
    }

    private Map<String, Long> loadAssetCounts() {
        Map<String, Long> counts = new HashMap<>();
        try {
            for (Object[] row : assetRepository.countByStatusGrouped()) {
                if (row[0] != null && row[1] instanceof Number) {
                    counts.put(row[0].toString(), ((Number) row[1]).longValue());
                }
            }
        } catch (Exception ex) {
            log.warn("Failed to load asset counts: {}", ex.getMessage());
        }
        return counts;
    }

    private Map<String, Long> loadInterviewCounts() {
        Map<String, Long> counts = new HashMap<>();
        try {
            for (Object[] row : interviewRepository.countByStatusGrouped()) {
                if (row[0] != null && row[1] instanceof Number) {
                    counts.put(row[0].toString(), ((Number) row[1]).longValue());
                }
            }
        } catch (Exception ex) {
            log.warn("Failed to load interview counts: {}", ex.getMessage());
        }
        return counts;
    }

    private List<Map<String, Object>> safeMonthlyInterviewStats(LocalDate today) {
        try {
            List<Map<String, Object>> stats = new ArrayList<>();
            interviewRepository.countByMonthAndStatus(today.minusMonths(6)).forEach(row -> {
                Map<String, Object> item = new HashMap<>();
                item.put("month", row[0]);
                item.put("status", row[1] != null ? row[1].toString() : "UNKNOWN");
                item.put("count", row[2]);
                stats.add(item);
            });
            return stats;
        } catch (Exception ex) {
            log.warn("Failed to load monthly interview stats: {}", ex.getMessage());
            return List.of();
        }
    }

    private List<Map<String, Object>> safeAssetAllocationTrends(LocalDate today) {
        try {
            List<Map<String, Object>> trends = new ArrayList<>();
            assetRepository.countAssignmentsByMonth(today.minusMonths(6)).forEach(row -> {
                Map<String, Object> item = new HashMap<>();
                item.put("month", row[0]);
                item.put("count", row[1]);
                trends.add(item);
            });
            return trends;
        } catch (Exception ex) {
            log.warn("Failed to load asset allocation trends: {}", ex.getMessage());
            return List.of();
        }
    }

    private List<DashboardResponse.ActivityItem> safeRecentAssignments() {
        try {
            return assignmentRepository
                    .findRecentByStatus(AssignmentStatus.ACTIVE, PageRequest.of(0, 5))
                    .stream()
                    .map(this::toAssignmentActivity)
                    .toList();
        } catch (Exception ex) {
            log.warn("Failed to load recent assignments: {}", ex.getMessage());
            return List.of();
        }
    }

    private List<DashboardResponse.ActivityItem> safeRecentReturns() {
        try {
            return assignmentRepository
                    .findRecentByStatus(AssignmentStatus.RETURNED, PageRequest.of(0, 5))
                    .stream()
                    .map(this::toAssignmentActivity)
                    .toList();
        } catch (Exception ex) {
            log.warn("Failed to load recent returns: {}", ex.getMessage());
            return List.of();
        }
    }

    private List<DashboardResponse.ActivityItem> safeUpcomingInterviews(LocalDate today) {
        try {
            return interviewRepository
                    .findUpcomingByStatusFromDate(InterviewStatus.SCHEDULED, today, PageRequest.of(0, 8))
                    .stream()
                    .map(this::toInterviewActivity)
                    .toList();
        } catch (Exception ex) {
            log.warn("Failed to load upcoming interviews: {}", ex.getMessage());
            return List.of();
        }
    }

    private DashboardResponse.ActivityItem toAssignmentActivity(AssetAssignment aa) {
        return DashboardResponse.ActivityItem.builder()
                .id(aa.getId())
                .title(aa.getAsset().getAssetName())
                .description(aa.getEmployeeName() + " - " + aa.getAssignedDate())
                .type("ASSIGNMENT")
                .timestamp(aa.getCreatedAt() != null
                        ? aa.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                        : "")
                .build();
    }

    private DashboardResponse.ActivityItem toInterviewActivity(Interview i) {
        boolean isToday = i.getInterviewDate() != null && i.getInterviewDate().isEqual(LocalDate.now());
        return DashboardResponse.ActivityItem.builder()
                .id(i.getId())
                .title(i.getCandidateName())
                .description(i.getInterviewerName() + " at " + i.getInterviewTime())
                .type("INTERVIEW")
                .timestamp(i.getInterviewDate() != null ? i.getInterviewDate().toString() : "")
                .interviewDate(i.getInterviewDate() != null ? i.getInterviewDate().toString() : null)
                .interviewTime(i.getInterviewTime() != null ? i.getInterviewTime().toString() : null)
                .today(isToday)
                .build();
    }

    /** JPA multi-column aggregates may return Object[]{a,b,c} or a single nested row. */
    private Object[] normalizeAggregateRow(Object raw) {
        if (raw == null) {
            return new Object[]{0L, 0L, 0L};
        }
        if (raw instanceof Object[] row) {
            if (row.length == 1 && row[0] instanceof Object[] nested) {
                return nested;
            }
            return row;
        }
        return new Object[]{0L, 0L, 0L};
    }

    private long aggregateAt(Object[] row, int index) {
        if (row == null || index < 0 || index >= row.length || row[index] == null) {
            return 0L;
        }
        Object value = row[index];
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException ex) {
            return 0L;
        }
    }
}
