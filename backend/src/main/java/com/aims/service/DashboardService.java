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
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

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
        LocalDate today = LocalDate.now();
        UserPrincipal current = SecurityUtils.getCurrentUser();
        boolean isAdmin = current != null && Role.ADMIN.name().equals(current.getRole());

        Map<String, Long> assetCounts = new HashMap<>();
        long totalAssets = 0;
        for (Object[] row : assetRepository.countByStatusGrouped()) {
            String status = row[0].toString();
            long count = ((Number) row[1]).longValue();
            assetCounts.put(status, count);
            totalAssets += count;
        }

        DashboardResponse.AssetStats assetStats = DashboardResponse.AssetStats.builder()
                .totalAssets(totalAssets)
                .availableAssets(assetCounts.getOrDefault(AssetStatus.AVAILABLE.name(), 0L))
                .assignedAssets(assetCounts.getOrDefault(AssetStatus.ASSIGNED.name(), 0L))
                .returnedAssets(assetCounts.getOrDefault(AssetStatus.RETURNED.name(), 0L))
                .damagedAssets(assetCounts.getOrDefault(AssetStatus.DAMAGED.name(), 0L))
                .build();

        Map<String, Long> interviewCounts = new HashMap<>();
        for (Object[] row : interviewRepository.countByStatusGrouped()) {
            if (row[0] != null && row[1] instanceof Number) {
                interviewCounts.put(row[0].toString(), ((Number) row[1]).longValue());
            }
        }
        long scheduled = interviewCounts.getOrDefault(InterviewStatus.SCHEDULED.name(), 0L);

        DashboardResponse.InterviewStats interviewStats = DashboardResponse.InterviewStats.builder()
                .todayInterviews(interviewRepository.countByInterviewDateAndInterviewStatus(today, InterviewStatus.SCHEDULED))
                .upcomingInterviews(interviewRepository.countByInterviewStatus(InterviewStatus.SCHEDULED))
                .completedInterviews(interviewRepository.countByInterviewStatus(InterviewStatus.COMPLETED))
                .cancelledInterviews(interviewRepository.countByInterviewStatus(InterviewStatus.CANCELLED))
                .scheduledInterviews(scheduled)
                .build();

        DashboardResponse.ProjectStats projectStats = null;
        DashboardResponse.UserDashboardStats userStats = null;
        List<DashboardResponse.ProjectSummary> assignedProjects = List.of();
        List<DashboardResponse.AssignedAssetItem> assignedAssets = List.of();

        if (isAdmin) {
            Object[] sums = normalizeAggregateRow(projectRepository.sumCandidateCounts());
            projectStats = DashboardResponse.ProjectStats.builder()
                    .totalProjects(projectRepository.count())
                    .activeProjects(projectRepository.countByStatus(ProjectStatus.ACTIVE))
                    .totalBudget(projectRepository.sumBudget())
                    .workingCandidates(aggregateAt(sums, 0))
                    .interviewCandidates(aggregateAt(sums, 1))
                    .onboardedCandidates(aggregateAt(sums, 2))
                    .build();
        } else if (current != null) {
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
            List<Asset> assets = assetRepository.findByAssignedToId(current.getId());
            assignedAssets = buildAssignedAssets(assets.stream().limit(MAX_ASSIGNED_ASSETS).toList());
            userStats = DashboardResponse.UserDashboardStats.builder()
                    .assignedProjects(myProjects.size())
                    .workingCandidates(working)
                    .onboardedCandidates(onboarded)
                    .assignedAssets(myAssets)
                    .upcomingInterviews(scheduled)
                    .build();
        }

        List<Map<String, Object>> assetStatusDistribution = new ArrayList<>();
        assetCounts.forEach((status, count) -> {
            Map<String, Object> item = new HashMap<>();
            item.put("status", status);
            item.put("count", count);
            assetStatusDistribution.add(item);
        });

        List<Map<String, Object>> monthlyInterviewStats = new ArrayList<>();
        interviewRepository.countByMonthAndStatus(today.minusMonths(6)).forEach(row -> {
            Map<String, Object> item = new HashMap<>();
            item.put("month", row[0]);
            item.put("status", row[1].toString());
            item.put("count", row[2]);
            monthlyInterviewStats.add(item);
        });

        List<Map<String, Object>> assetAllocationTrends = new ArrayList<>();
        assetRepository.countAssignmentsByMonth(today.minusMonths(6)).forEach(row -> {
            Map<String, Object> item = new HashMap<>();
            item.put("month", row[0]);
            item.put("count", row[1]);
            assetAllocationTrends.add(item);
        });

        List<DashboardResponse.ActivityItem> recentAssignments = assignmentRepository
                .findRecentByStatus(AssignmentStatus.ACTIVE, PageRequest.of(0, 5))
                .stream()
                .map(this::toAssignmentActivity)
                .toList();

        List<DashboardResponse.ActivityItem> recentReturns = assignmentRepository
                .findRecentByStatus(AssignmentStatus.RETURNED, PageRequest.of(0, 5))
                .stream()
                .map(this::toAssignmentActivity)
                .toList();

        List<DashboardResponse.ActivityItem> upcomingInterviews = interviewRepository
                .findUpcomingByStatusFromDate(InterviewStatus.SCHEDULED, today, PageRequest.of(0, 8))
                .stream()
                .map(this::toInterviewActivity)
                .toList();

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

    private DashboardResponse.ActivityItem toAssignmentActivity(AssetAssignment aa) {
        return DashboardResponse.ActivityItem.builder()
                .id(aa.getId())
                .title(aa.getAsset().getAssetName())
                .description(aa.getEmployeeName() + " - " + aa.getAssignedDate())
                .type("ASSIGNMENT")
                .timestamp(aa.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
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

    private static Object[] normalizeAggregateRow(Object row) {
        if (row instanceof Object[] values) {
            if (values.length == 1 && values[0] instanceof Object[] nested) {
                return nested;
            }
            return values;
        }
        return new Object[0];
    }

    private static long aggregateAt(Object[] row, int index) {
        if (row == null || index >= row.length || row[index] == null) {
            return 0L;
        }
        return ((Number) row[index]).longValue();
    }
}
