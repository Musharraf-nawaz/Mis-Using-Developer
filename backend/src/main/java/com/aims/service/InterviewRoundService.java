package com.aims.service;

import com.aims.dto.interview.InterviewRoundRequest;
import com.aims.dto.interview.InterviewRoundResponse;
import com.aims.entity.Interview;
import com.aims.entity.InterviewRound;
import com.aims.entity.enums.FinalInterviewStatus;
import com.aims.entity.enums.Role;
import com.aims.entity.enums.RoundStatus;
import com.aims.exception.BadRequestException;
import com.aims.exception.ResourceNotFoundException;
import com.aims.repository.InterviewRepository;
import com.aims.repository.InterviewRoundRepository;
import com.aims.security.UserPrincipal;
import com.aims.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InterviewRoundService {

    private final InterviewRoundRepository roundRepository;
    private final InterviewRepository interviewRepository;
    private final NotificationService notificationService;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<InterviewRoundResponse> getRounds(Long interviewId) {
        ensureInterview(interviewId);
        return toResponses(roundRepository.findByInterviewIdOrderByRoundNumberAsc(interviewId));
    }

    @Transactional
    public List<InterviewRoundResponse> initializeRounds(Long interviewId) {
        requireAdmin();
        Interview interview = ensureInterview(interviewId);
        if (!roundRepository.findByInterviewIdOrderByRoundNumberAsc(interviewId).isEmpty()) {
            return getRounds(interviewId);
        }
        List<InterviewRound> rounds = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            InterviewRound round = InterviewRound.builder()
                    .interview(interview)
                    .roundNumber(i)
                    .status(RoundStatus.SCHEDULED)
                    .companyToRepresent(interview.getCompanyToRepresent())
                    .interviewer(interview.getInterviewerName())
                    .interviewDate(i == 1 ? interview.getInterviewDate() : null)
                    .interviewTime(i == 1 ? interview.getInterviewTime() : null)
                    .interviewLink(i == 1 ? interview.getInterviewLink() : null)
                    .build();
            rounds.add(roundRepository.save(round));
        }
        return toResponses(rounds);
    }

    private List<InterviewRoundResponse> toResponses(List<InterviewRound> rounds) {
        Map<Integer, RoundStatus> statusByRound = rounds.stream()
                .collect(Collectors.toMap(InterviewRound::getRoundNumber, InterviewRound::getStatus, (a, b) -> a));
        return rounds.stream()
                .map(round -> toResponse(round, statusByRound))
                .toList();
    }

    @Transactional
    public InterviewRoundResponse updateRound(Long interviewId, Integer roundNumber, InterviewRoundRequest request) {
        requireAdmin();
        Interview interview = ensureInterview(interviewId);
        validateRoundAccess(interviewId, roundNumber);

        InterviewRound round = roundRepository.findByInterviewIdAndRoundNumber(interviewId, roundNumber)
                .orElseGet(() -> roundRepository.save(InterviewRound.builder()
                        .interview(interview)
                        .roundNumber(roundNumber)
                        .status(RoundStatus.SCHEDULED)
                        .build()));

        if (request.getInterviewLink() != null) round.setInterviewLink(request.getInterviewLink());
        if (request.getInterviewDate() != null) round.setInterviewDate(request.getInterviewDate());
        if (request.getInterviewTime() != null) round.setInterviewTime(request.getInterviewTime());
        if (request.getCompanyToRepresent() != null) round.setCompanyToRepresent(request.getCompanyToRepresent());
        if (request.getInterviewer() != null) round.setInterviewer(request.getInterviewer());
        if (request.getStatus() != null) {
            round.setStatus(request.getStatus());
            handleStatusSideEffects(interview, roundNumber, request.getStatus());
        }
        round = roundRepository.save(round);
        auditService.log("INTERVIEW_ROUND_UPDATED", "INTERVIEW", interviewId, null, "Round " + roundNumber);
        List<InterviewRound> allRounds = roundRepository.findByInterviewIdOrderByRoundNumberAsc(interviewId);
        Map<Integer, RoundStatus> statusByRound = allRounds.stream()
                .collect(Collectors.toMap(InterviewRound::getRoundNumber, InterviewRound::getStatus, (a, b) -> a));
        return toResponse(round, statusByRound);
    }

    private void handleStatusSideEffects(Interview interview, int roundNumber, RoundStatus status) {
        if (status == RoundStatus.PASSED && roundNumber < 3) {
            notificationService.notify(SecurityUtils.getCurrentUserId(),
                    "Round 2 Available", "Next interview round is now available for " + interview.getCandidateName(),
                    "ROUND_AVAILABLE", "INTERVIEW", interview.getId(), false);
        }
        if (status == RoundStatus.FAILED) {
            interview.setFinalStatus(FinalInterviewStatus.REJECTED);
            interviewRepository.save(interview);
            notificationService.notify(SecurityUtils.getCurrentUserId(),
                    "Candidate Rejected", interview.getCandidateName() + " rejected at round " + roundNumber,
                    "CANDIDATE_REJECTED", "INTERVIEW", interview.getId(), false);
        }
        if (status == RoundStatus.PASSED && roundNumber == 3) {
            interview.setFinalStatus(FinalInterviewStatus.SELECTED);
            interviewRepository.save(interview);
            notificationService.notify(SecurityUtils.getCurrentUserId(),
                    "Candidate Selected", interview.getCandidateName() + " selected",
                    "CANDIDATE_SELECTED", "INTERVIEW", interview.getId(), false);
        }
    }

    private void validateRoundAccess(Long interviewId, int roundNumber) {
        if (roundNumber == 1) return;
        InterviewRound prev = roundRepository.findByInterviewIdAndRoundNumber(interviewId, roundNumber - 1)
                .orElseThrow(() -> new BadRequestException("Previous round not found"));
        if (prev.getStatus() != RoundStatus.PASSED) {
            throw new BadRequestException("Round " + roundNumber + " available only when round " + (roundNumber - 1) + " is PASSED");
        }
    }

    private InterviewRoundResponse toResponse(InterviewRound round, Map<Integer, RoundStatus> statusByRound) {
        boolean available = round.getRoundNumber() == 1
                || statusByRound.getOrDefault(round.getRoundNumber() - 1, RoundStatus.SCHEDULED) == RoundStatus.PASSED;
        return InterviewRoundResponse.builder()
                .id(round.getId())
                .roundNumber(round.getRoundNumber())
                .interviewLink(round.getInterviewLink())
                .interviewDate(round.getInterviewDate())
                .interviewTime(round.getInterviewTime())
                .companyToRepresent(round.getCompanyToRepresent())
                .interviewer(round.getInterviewer())
                .status(round.getStatus())
                .available(available)
                .build();
    }

    private Interview ensureInterview(Long id) {
        return interviewRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Interview not found"));
    }

    private void requireAdmin() {
        UserPrincipal p = SecurityUtils.getCurrentUser();
        if (p == null || !Role.ADMIN.name().equals(p.getRole())) {
            throw new BadRequestException("Only admin can modify interview rounds");
        }
    }
}
