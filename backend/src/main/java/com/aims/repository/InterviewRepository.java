package com.aims.repository;

import com.aims.entity.Interview;
import com.aims.entity.enums.InterviewStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface InterviewRepository extends JpaRepository<Interview, Long> {

    @Query("SELECT i FROM Interview i WHERE " +
           "(:search IS NULL OR LOWER(i.candidateName) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(i.candidateEmail) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "AND (:date IS NULL OR i.interviewDate = :date) " +
           "AND (:interviewer IS NULL OR LOWER(i.interviewerName) LIKE LOWER(CONCAT('%', :interviewer, '%'))) " +
           "AND (:status IS NULL OR i.interviewStatus = :status) " +
           "AND (:profile IS NULL OR i.candidateProfile = :profile)")
    Page<Interview> findWithFilters(@Param("search") String search,
                                    @Param("date") LocalDate date,
                                    @Param("interviewer") String interviewer,
                                    @Param("status") InterviewStatus status,
                                    @Param("profile") String profile,
                                    Pageable pageable);

    @Query("SELECT i FROM Interview i WHERE i.interviewDate BETWEEN :start AND :end " +
           "AND (:status IS NULL OR i.interviewStatus = :status) ORDER BY i.interviewDate, i.interviewTime")
    List<Interview> findByDateRange(@Param("start") LocalDate start,
                                    @Param("end") LocalDate end,
                                    @Param("status") InterviewStatus status);

    long countByInterviewStatus(InterviewStatus status);

    long countByInterviewDateAndInterviewStatus(LocalDate date, InterviewStatus status);

    @Query(value = """
            SELECT TO_CHAR(i.interview_date, 'YYYY-MM') AS month, i.interview_status AS status, COUNT(*) AS cnt
            FROM interviews i
            WHERE i.interview_date >= :fromDate
            GROUP BY TO_CHAR(i.interview_date, 'YYYY-MM'), i.interview_status
            ORDER BY month
            """, nativeQuery = true)
    List<Object[]> countByMonthAndStatus(@Param("fromDate") LocalDate fromDate);

    List<Interview> findByInterviewDateAndInterviewStatusOrderByInterviewTimeAsc(
            LocalDate date, InterviewStatus status);

    @Query("SELECT i FROM Interview i WHERE i.interviewStatus = :status AND i.interviewDate >= :fromDate " +
           "ORDER BY i.interviewDate ASC, i.interviewTime ASC")
    List<Interview> findUpcomingByStatusFromDate(@Param("status") InterviewStatus status,
                                                 @Param("fromDate") LocalDate fromDate,
                                                 Pageable pageable);

    @Query("SELECT i.interviewStatus, COUNT(i) FROM Interview i GROUP BY i.interviewStatus")
    List<Object[]> countByStatusGrouped();
}
