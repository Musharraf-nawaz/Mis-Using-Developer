package com.aims.service;

import com.aims.entity.Interview;
import com.aims.entity.enums.InterviewStatus;
import com.aims.repository.InterviewRepository;
import com.aims.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class InterviewReminderService {

    private static final String REMINDER_TYPE = "INTERVIEW_REMINDER_5M";
    private static final String ENTITY_TYPE = "INTERVIEW";

    private final InterviewRepository interviewRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationService notificationService;

    @Scheduled(cron = "0 * * * * *")
    public void sendFiveMinuteReminders() {
        LocalDateTime now = LocalDateTime.now();

        interviewRepository.findUpcomingByStatusFromDate(
                        InterviewStatus.SCHEDULED, LocalDate.now(), PageRequest.of(0, 50))
                .stream()
                .filter(this::hasOwner)
                .filter(i -> isInReminderWindow(i, now))
                .forEach(this::notifyIfNotSent);
    }

    private boolean hasOwner(Interview interview) {
        return interview.getInterviewer() != null || interview.getCreatedBy() != null;
    }

    private boolean isInReminderWindow(Interview interview, LocalDateTime now) {
        if (interview.getInterviewDate() == null || interview.getInterviewTime() == null) {
            return false;
        }
        LocalDateTime interviewDateTime = LocalDateTime.of(interview.getInterviewDate(), interview.getInterviewTime());
        long minutesUntilInterview = Duration.between(now, interviewDateTime).toMinutes();
        return minutesUntilInterview >= 4 && minutesUntilInterview <= 5;
    }

    private void notifyIfNotSent(Interview interview) {
        String message = "Interview with " + interview.getCandidateName() + " starts in 5 minutes at "
                + interview.getInterviewTime() + ".";

        if (interview.getInterviewer() != null) {
            Long interviewerId = interview.getInterviewer().getId();
            if (!notificationRepository.existsByUserIdAndTypeAndEntityTypeAndEntityId(
                    interviewerId, REMINDER_TYPE, ENTITY_TYPE, interview.getId())) {
                notificationService.notify(interviewerId, "Interview Reminder", message,
                        REMINDER_TYPE, ENTITY_TYPE, interview.getId(), true);
            }
        }

        if (interview.getCreatedBy() != null) {
            Long creatorId = interview.getCreatedBy().getId();
            if (!notificationRepository.existsByUserIdAndTypeAndEntityTypeAndEntityId(
                    creatorId, REMINDER_TYPE, ENTITY_TYPE, interview.getId())) {
                notificationService.notify(creatorId, "Interview Reminder", message,
                        REMINDER_TYPE, ENTITY_TYPE, interview.getId(), true);
            }
        }
    }
}
