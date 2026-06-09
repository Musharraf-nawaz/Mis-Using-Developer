package com.aims.entity;

import com.aims.entity.enums.FinalInterviewStatus;
import com.aims.entity.enums.InterviewMode;
import com.aims.entity.enums.InterviewRound;
import com.aims.entity.enums.InterviewStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "interviews")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Interview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "candidate_name", nullable = false, length = 150)
    private String candidateName;

    @Column(name = "candidate_email", nullable = false)
    private String candidateEmail;

    @Column(name = "candidate_phone", length = 20)
    private String candidatePhone;

    @Column(name = "candidate_profile", length = 200)
    private String candidateProfile;

    @Column(name = "client_name", length = 150)
    private String clientName;

    @Column(name = "company_to_represent", length = 150)
    private String companyToRepresent;

    @Column(name = "interview_link", length = 500)
    private String interviewLink;

    @Column(name = "candidate_cv_url", length = 500)
    private String candidateCvUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "final_status", length = 20)
    private FinalInterviewStatus finalStatus;

    @Column(columnDefinition = "TEXT")
    private String skills;

    @Column(length = 50)
    private String experience;

    @Column(name = "interviewer_name", nullable = false, length = 150)
    private String interviewerName;

    @Column(name = "interviewer_email")
    private String interviewerEmail;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "interviewer_id")
    private User interviewer;

    @Column(name = "interview_date", nullable = false)
    private LocalDate interviewDate;

    @Column(name = "interview_time", nullable = false)
    private LocalTime interviewTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "interview_mode", nullable = false, length = 20)
    private InterviewMode interviewMode;

    @Enumerated(EnumType.STRING)
    @Column(name = "interview_round", nullable = false, length = 20)
    private InterviewRound interviewRound;

    @Enumerated(EnumType.STRING)
    @Column(name = "interview_status", nullable = false, length = 20)
    @Builder.Default
    private InterviewStatus interviewStatus = InterviewStatus.SCHEDULED;

    @Column(columnDefinition = "TEXT")
    private String feedback;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    private User createdBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
