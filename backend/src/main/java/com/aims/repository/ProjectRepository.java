package com.aims.repository;

import com.aims.entity.Project;
import com.aims.entity.enums.ProjectStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {

    @Query("SELECT p FROM Project p WHERE (:search IS NULL OR LOWER(p.projectName) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(p.clientName) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "AND (:status IS NULL OR p.status = :status)")
    Page<Project> findWithFilters(@Param("search") String search, @Param("status") ProjectStatus status, Pageable pageable);

    @Query("SELECT p FROM Project p JOIN ProjectUserAssignment pua ON pua.project = p WHERE pua.user.id = :userId")
    List<Project> findByAssignedUserId(@Param("userId") Long userId);

    long countByStatus(ProjectStatus status);

    @Query("SELECT COALESCE(SUM(p.budget), 0) FROM Project p")
    BigDecimal sumBudget();
}
