package com.aims.dto.interview;

import com.aims.entity.enums.InterviewMode;
import com.aims.entity.enums.InterviewRound;
import com.aims.entity.enums.InterviewStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class InterviewRequest {
    @NotBlank
    private String candidateName;
    @NotBlank @Email
    private String candidateEmail;
    private String candidatePhone;
    private String candidateProfile;
    private String skills;
    private String experience;
    @NotBlank
    private String interviewerName;
    private String interviewerEmail;
    private Long interviewerId;
    @NotNull
    private LocalDate interviewDate;
    @NotNull
    private LocalTime interviewTime;
    @NotNull
    private InterviewMode interviewMode;
    @NotNull
    private InterviewRound interviewRound;
    private InterviewStatus interviewStatus;
    private String feedback;
    private String notes;
}
