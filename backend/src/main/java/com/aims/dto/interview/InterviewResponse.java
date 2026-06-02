package com.aims.dto.interview;

import com.aims.entity.enums.InterviewMode;
import com.aims.entity.enums.InterviewRound;
import com.aims.entity.enums.InterviewStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterviewResponse {
    private Long id;
    private String candidateName;
    private String candidateEmail;
    private String candidatePhone;
    private String candidateProfile;
    private String skills;
    private String experience;
    private String interviewerName;
    private String interviewerEmail;
    private Long interviewerId;
    private LocalDate interviewDate;
    private LocalTime interviewTime;
    private InterviewMode interviewMode;
    private InterviewRound interviewRound;
    private InterviewStatus interviewStatus;
    private String feedback;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
