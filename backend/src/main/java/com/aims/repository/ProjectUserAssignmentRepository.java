package com.aims.repository;

import com.aims.entity.ProjectUserAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface ProjectUserAssignmentRepository extends JpaRepository<ProjectUserAssignment, Long> {
    List<ProjectUserAssignment> findByUserId(Long userId);
    List<ProjectUserAssignment> findByProjectId(Long projectId);

    @Query("SELECT a.project.id, a.user.id FROM ProjectUserAssignment a WHERE a.project.id IN :projectIds")
    List<Object[]> findUserIdsByProjectIds(@Param("projectIds") Collection<Long> projectIds);

    boolean existsByProjectIdAndUserId(Long projectId, Long userId);

    void deleteByProjectId(Long projectId);
}
