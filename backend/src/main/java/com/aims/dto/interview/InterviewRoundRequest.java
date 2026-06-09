package com.aims.dto.interview;

import com.aims.entity.enums.RoundStatus;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class InterviewRoundRequest {
    private Integer roundNumber;
    private String interviewLink;
    private LocalDate interviewDate;
    private LocalTime interviewTime;
    private String companyToRepresent;
    private String interviewer;
    private RoundStatus status;
}
