package com.aims.dto.interview;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class RescheduleInterviewRequest {
    @NotNull
    private LocalDate interviewDate;
    @NotNull
    private LocalTime interviewTime;
}
