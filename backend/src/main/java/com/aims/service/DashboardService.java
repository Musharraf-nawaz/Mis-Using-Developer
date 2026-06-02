package com.aims.service;

import com.aims.dto.dashboard.DashboardResponse;
import com.aims.entity.AssetAssignment;
import com.aims.entity.Interview;
import com.aims.entity.enums.AssetStatus;
import com.aims.entity.enums.AssignmentStatus;
import com.aims.entity.enums.InterviewStatus;
import com.aims.repository.AssetAssignmentRepository;
import com.aims.repository.AssetRepository;
import com.aims.repository.InterviewRepository;
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

    private final AssetRepository assetRepository;
    private final AssetAssignmentRepository assignmentRepository;
    private final InterviewRepository interviewRepository;

    @Transactional(readOnly = true)
    public DashboardResponse getDashboard() {
        LocalDate today = LocalDate.now();

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
                .build();

        List<Map<String, Object>> assetStatusDistribution = new ArrayList<>();
        assetRepository.countByStatusGrouped().forEach(row -> {
            Map<String, Object> item = new HashMap<>();
            item.put("status", row[0].toString());
            item.put("count", row[1]);
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
                .assetStats(assetStats)
                .interviewStats(interviewStats)
                .assetStatusDistribution(assetStatusDistribution)
                .monthlyInterviewStats(monthlyInterviewStats)
                .assetAllocationTrends(assetAllocationTrends)
                .recentAssignments(recentAssignments)
                .recentReturns(recentReturns)
                .upcomingInterviews(upcomingInterviews)
                .build();
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
                .timestamp(i.getInterviewDate().toString())
                .interviewDate(i.getInterviewDate() != null ? i.getInterviewDate().toString() : null)
                .interviewTime(i.getInterviewTime() != null ? i.getInterviewTime().toString() : null)
                .today(isToday)
                .build();
    }
}
