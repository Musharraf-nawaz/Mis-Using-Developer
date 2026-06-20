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

        DashboardResponse.AssetStats assetStats = DashboardResponse.AssetStats.builder()
                .totalAssets(assetRepository.count())
                .availableAssets(assetRepository.countByStatus(AssetStatus.AVAILABLE))
                .assignedAssets(assetRepository.countByStatus(AssetStatus.ASSIGNED))
                .returnedAssets(assetRepository.countByStatus(AssetStatus.RETURNED))
                .damagedAssets(assetRepository.countByStatus(AssetStatus.DAMAGED))
                .build();

        DashboardResponse.InterviewStats interviewStats = DashboardResponse.InterviewStats.builder()
                .todayInterviews(interviewRepository.countByInterviewDateAndInterviewStatus(
                        today, InterviewStatus.SCHEDULED))
                .upcomingInterviews(interviewRepository.countByInterviewStatus(InterviewStatus.SCHEDULED))
                .completedInterviews(interviewRepository.countByInterviewStatus(InterviewStatus.COMPLETED))
                .cancelledInterviews(interviewRepository.countByInterviewStatus(InterviewStatus.CANCELLED))
                .scheduledInterviews(interviewRepository.countByInterviewStatus(InterviewStatus.SCHEDULED))
                .build();

        DashboardResponse.ProjectStats projectStats = null;
        DashboardResponse.UserDashboardStats userStats = null;
        List<DashboardResponse.ProjectSummary> assignedProjects = List.of();
        List<DashboardResponse.AssignedAssetItem> assignedAssets = List.of();

        if (isAdmin) {
            try {
                Object[] sums = projectRepository.sumCandidateCounts();
                long working = sums != null && sums.length > 0 && sums[0] != null ? ((Number) sums[0]).longValue() : 0;
                long interviewCand = sums != null && sums.length > 1 && sums[1] != null ? ((Number) sums[1]).longValue() : 0;
                long onboarded = sums != null && sums.length > 2 && sums[2] != null ? ((Number) sums[2]).longValue() : 0;
                projectStats = DashboardResponse.ProjectStats.builder()
                        .totalProjects(projectRepository.count())
                        .activeProjects(projectRepository.countByStatus(ProjectStatus.ACTIVE))
                        .totalBudget(projectRepository.sumBudget())
                        .workingCandidates(working)
                        .interviewCandidates(interviewCand)
                        .onboardedCandidates(onboarded)
                        .build();
            } catch (Exception ex) {
                log.warn("Failed to load admin project stats: {}", ex.getMessage());
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

        List<Map<String, Object>> assetStatusDistribution = safeAssetStatusDistribution();

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

    private List<Map<String, Object>> safeAssetStatusDistribution() {
        try {
            List<Map<String, Object>> distribution = new ArrayList<>();
            assetRepository.countByStatusGrouped().forEach(row -> {
                Map<String, Object> item = new HashMap<>();
                item.put("status", row[0] != null ? row[0].toString() : "UNKNOWN");
                item.put("count", row[1]);
                distribution.add(item);
            });
            return distribution;
        } catch (Exception ex) {
            log.warn("Failed to load asset status distribution: {}", ex.getMessage());
            return List.of();
        }
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
}
