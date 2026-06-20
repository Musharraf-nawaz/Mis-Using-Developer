package com.aims.dto.interview;

import com.aims.entity.enums.InterviewRound;
import com.aims.entity.enums.InterviewStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Builder
public class InterviewCalendarResponse {
    private Long id;
    private String candidateName;
    private LocalDate interviewDate;
    private LocalTime interviewTime;
    private InterviewRound interviewRound;
    private InterviewStatus interviewStatus;
}
