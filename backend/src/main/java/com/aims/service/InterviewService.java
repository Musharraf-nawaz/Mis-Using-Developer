package com.aims.service;

import com.aims.dto.common.PageResponse;
import com.aims.dto.interview.InterviewCalendarResponse;
import com.aims.dto.interview.InterviewRequest;
import com.aims.dto.interview.RescheduleInterviewRequest;
import com.aims.dto.interview.InterviewResponse;
import com.aims.entity.Interview;
import com.aims.entity.User;
import com.aims.entity.enums.InterviewStatus;
import com.aims.exception.ResourceNotFoundException;
import com.aims.repository.InterviewRepository;
import com.aims.repository.InterviewRoundRepository;
import com.aims.repository.UserRepository;
import com.aims.dto.interview.InterviewRoundResponse;
import com.aims.entity.enums.RoundStatus;
import com.aims.util.MapperUtils;
import com.aims.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InterviewService {

    private final InterviewRepository interviewRepository;
    private final InterviewRoundRepository roundRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;
    private final AuditService auditService;
    private final NotificationService notificationService;

    @Transactional(readOnly = true)
    public PageResponse<InterviewResponse> getAll(String search, LocalDate date, String interviewer,
                                                   InterviewStatus status, String profile, Pageable pageable) {
        Page<Interview> page = interviewRepository.findWithFilters(search, date, interviewer, status, profile, pageable);
        return PageResponse.from(page.map(MapperUtils::toInterviewResponse));
    }

    @Transactional(readOnly = true)
    public InterviewResponse getById(Long id) {
        Interview interview = findInterview(id);
        InterviewResponse response = MapperUtils.toInterviewResponse(interview);
        response.setRounds(roundRepository.findByInterviewIdOrderByRoundNumberAsc(id).stream()
                .map(r -> InterviewRoundResponse.builder()
                        .id(r.getId())
                        .roundNumber(r.getRoundNumber())
                        .interviewLink(r.getInterviewLink())
                        .interviewDate(r.getInterviewDate())
                        .interviewTime(r.getInterviewTime())
                        .companyToRepresent(r.getCompanyToRepresent())
                        .interviewer(r.getInterviewer())
                        .status(r.getStatus())
                        .available(isRoundAvailable(id, r.getRoundNumber()))
                        .build())
                .toList());
        return response;
    }

    @Transactional
    public InterviewResponse uploadCv(Long id, MultipartFile file) {
        Interview interview = findInterview(id);
        String fileUrl = fileStorageService.store(file, "cv");
        interview.setCandidateCvUrl(fileUrl);
        interview = interviewRepository.save(interview);
        return MapperUtils.toInterviewResponse(interview);
    }

    private boolean isRoundAvailable(Long interviewId, int roundNumber) {
        if (roundNumber == 1) return true;
        return roundRepository.findByInterviewIdAndRoundNumber(interviewId, roundNumber - 1)
                .map(r -> r.getStatus() == RoundStatus.PASSED)
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public List<InterviewCalendarResponse> getCalendar(LocalDate start, LocalDate end, InterviewStatus status) {
        return interviewRepository.findByDateRange(start, end, status).stream()
                .map(MapperUtils::toCalendarResponse)
                .toList();
    }

    @Transactional
    public InterviewResponse create(InterviewRequest request) {
        Interview interview = mapToEntity(new Interview(), request);
        interview.setInterviewStatus(InterviewStatus.SCHEDULED);
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId != null) {
            userRepository.findById(userId).ifPresent(interview::setCreatedBy);
        }
        interview = interviewRepository.save(interview);
        auditService.log("INTERVIEW_CREATED", "INTERVIEW", interview.getId(), null, interview.getCandidateName());
        notificationService.notify(userId != null ? userId : 1L,
                "Interview Scheduled",
                "Interview with " + interview.getCandidateName() + " scheduled on " + interview.getInterviewDate(),
                "INTERVIEW_SCHEDULED", "INTERVIEW", interview.getId(), true);
        return MapperUtils.toInterviewResponse(interview);
    }

    @Transactional
    public InterviewResponse update(Long id, InterviewRequest request) {
        Interview interview = findInterview(id);
        interview = mapToEntity(interview, request);
        interview = interviewRepository.save(interview);
        auditService.log("INTERVIEW_UPDATED", "INTERVIEW", interview.getId(), null, interview.getCandidateName());
        return MapperUtils.toInterviewResponse(interview);
    }

    @Transactional
    public InterviewResponse reschedule(Long id, RescheduleInterviewRequest request) {
        Interview interview = findInterview(id);
        interview.setInterviewDate(request.getInterviewDate());
        interview.setInterviewTime(request.getInterviewTime());
        interview.setInterviewStatus(InterviewStatus.RESCHEDULED);
        interview = interviewRepository.save(interview);
        auditService.log("INTERVIEW_RESCHEDULED", "INTERVIEW", id, null, interview.getCandidateName());
        return MapperUtils.toInterviewResponse(interview);
    }

    @Transactional
    public InterviewResponse cancel(Long id, String notes) {
        Interview interview = findInterview(id);
        interview.setInterviewStatus(InterviewStatus.CANCELLED);
        interview.setNotes(notes);
        interview = interviewRepository.save(interview);
        auditService.log("INTERVIEW_CANCELLED", "INTERVIEW", id, null, interview.getCandidateName());
        return MapperUtils.toInterviewResponse(interview);
    }

    @Transactional
    public InterviewResponse complete(Long id, String feedback) {
        Interview interview = findInterview(id);
        interview.setInterviewStatus(InterviewStatus.COMPLETED);
        interview.setFeedback(feedback);
        interview = interviewRepository.save(interview);
        auditService.log("INTERVIEW_COMPLETED", "INTERVIEW", id, null, interview.getCandidateName());
        return MapperUtils.toInterviewResponse(interview);
    }

    @Transactional
    public void delete(Long id) {
        Interview interview = findInterview(id);
        interviewRepository.delete(interview);
        auditService.log("INTERVIEW_DELETED", "INTERVIEW", id, interview.getCandidateName(), null);
    }

    private Interview findInterview(Long id) {
        return interviewRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Interview not found with id: " + id));
    }

    private Interview mapToEntity(Interview interview, InterviewRequest request) {
        interview.setCandidateName(request.getCandidateName());
        interview.setCandidateEmail(request.getCandidateEmail());
        interview.setCandidatePhone(request.getCandidatePhone());
        interview.setCandidateProfile(request.getCandidateProfile());
        interview.setClientName(request.getClientName());
        interview.setMidClientName(request.getMidClientName());
        interview.setCompanyToRepresent(request.getCompanyToRepresent());
        interview.setInterviewLink(request.getInterviewLink());
        if (request.getCandidateCvUrl() != null) interview.setCandidateCvUrl(request.getCandidateCvUrl());
        if (request.getFinalStatus() != null) interview.setFinalStatus(request.getFinalStatus());
        interview.setSkills(request.getSkills());
        interview.setExperience(request.getExperience());
        interview.setInterviewerName(request.getInterviewerName());
        interview.setInterviewerEmail(request.getInterviewerEmail());
        if (request.getInterviewerId() != null) {
            userRepository.findById(request.getInterviewerId()).ifPresent(interview::setInterviewer);
        }
        interview.setInterviewDate(request.getInterviewDate());
        interview.setInterviewTime(request.getInterviewTime());
        interview.setInterviewMode(request.getInterviewMode());
        interview.setInterviewRound(request.getInterviewRound());
        if (request.getInterviewStatus() != null) interview.setInterviewStatus(request.getInterviewStatus());
        if (request.getFeedback() != null) interview.setFeedback(request.getFeedback());
        if (request.getNotes() != null) interview.setNotes(request.getNotes());
        return interview;
    }
}
