package com.aims.repository;

import com.aims.entity.InterviewRound;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InterviewRoundRepository extends JpaRepository<InterviewRound, Long> {
    List<InterviewRound> findByInterviewIdOrderByRoundNumberAsc(Long interviewId);
    Optional<InterviewRound> findByInterviewIdAndRoundNumber(Long interviewId, Integer roundNumber);
}
