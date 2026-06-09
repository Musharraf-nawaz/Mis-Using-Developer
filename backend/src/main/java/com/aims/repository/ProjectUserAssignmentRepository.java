package com.aims.repository;

import com.aims.entity.ProjectUserAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectUserAssignmentRepository extends JpaRepository<ProjectUserAssignment, Long> {
    List<ProjectUserAssignment> findByUserId(Long userId);
    List<ProjectUserAssignment> findByProjectId(Long projectId);
    void deleteByProjectId(Long projectId);
}
