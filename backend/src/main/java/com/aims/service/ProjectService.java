package com.aims.service;

import com.aims.dto.common.PageResponse;
import com.aims.dto.project.ProjectRequest;
import com.aims.dto.project.ProjectResponse;
import com.aims.entity.Project;
import com.aims.entity.ProjectUserAssignment;
import com.aims.entity.User;
import com.aims.entity.enums.ProjectStatus;
import com.aims.entity.enums.Role;
import com.aims.exception.BadRequestException;
import com.aims.exception.ResourceNotFoundException;
import com.aims.repository.ProjectRepository;
import com.aims.repository.ProjectUserAssignmentRepository;
import com.aims.repository.UserRepository;
import com.aims.security.UserPrincipal;
import com.aims.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectUserAssignmentRepository assignmentRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public PageResponse<ProjectResponse> getAll(String search, ProjectStatus status, Pageable pageable) {
        UserPrincipal current = SecurityUtils.getCurrentUser();
        if (current != null && Role.USER.name().equals(current.getRole())) {
            List<ProjectResponse> assigned = projectRepository.findByAssignedUserId(current.getId())
                    .stream().map(p -> toResponse(p, false)).toList();
            return PageResponse.<ProjectResponse>builder()
                    .content(assigned)
                    .page(0)
                    .size(assigned.size())
                    .totalElements(assigned.size())
                    .totalPages(1)
                    .last(true)
                    .build();
        }
        return PageResponse.from(projectRepository.findWithFilters(search, status, pageable)
                .map(p -> toResponse(p, true)));
    }

    @Transactional(readOnly = true)
    public ProjectResponse getById(Long id) {
        Project project = findProject(id);
        boolean showBudget = isAdmin();
        if (!isAdmin()) {
            Long userId = SecurityUtils.getCurrentUserId();
            boolean assigned = assignmentRepository.findByUserId(userId).stream()
                    .anyMatch(a -> a.getProject().getId().equals(id));
            if (!assigned) throw new ResourceNotFoundException("Project not found");
        }
        return toResponse(project, showBudget);
    }

    @Transactional
    public ProjectResponse create(ProjectRequest request) {
        requireAdmin();
        Project project = mapToEntity(new Project(), request);
        project = projectRepository.save(project);
        syncAssignments(project, request.getAssignedUserIds());
        auditService.log("PROJECT_CREATED", "PROJECT", project.getId(), null, project.getProjectName());
        notifyAssignedUsers(project, "New Project Assigned", "You have been assigned to project: " + project.getProjectName(), "PROJECT_ASSIGNED");
        return toResponse(project, true);
    }

    @Transactional
    public ProjectResponse update(Long id, ProjectRequest request) {
        requireAdmin();
        Project project = findProject(id);
        project = mapToEntity(project, request);
        project = projectRepository.save(project);
        syncAssignments(project, request.getAssignedUserIds());
        auditService.log("PROJECT_UPDATED", "PROJECT", id, null, project.getProjectName());
        notifyAssignedUsers(project, "Project Updated", "Project updated: " + project.getProjectName(), "PROJECT_UPDATED");
        return toResponse(project, true);
    }

    @Transactional
    public void delete(Long id) {
        requireAdmin();
        Project project = findProject(id);
        assignmentRepository.deleteByProjectId(id);
        projectRepository.delete(project);
        auditService.log("PROJECT_DELETED", "PROJECT", id, project.getProjectName(), null);
    }

    private void syncAssignments(Project project, List<Long> userIds) {
        assignmentRepository.deleteByProjectId(project.getId());
        if (userIds == null) return;
        for (Long userId : userIds) {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
            assignmentRepository.save(ProjectUserAssignment.builder().project(project).user(user).build());
        }
    }

    private void notifyAssignedUsers(Project project, String title, String message, String type) {
        for (ProjectUserAssignment a : assignmentRepository.findByProjectId(project.getId())) {
            notificationService.notify(a.getUser().getId(), title, message, type, "PROJECT", project.getId(), false);
        }
    }

    private Project mapToEntity(Project project, ProjectRequest request) {
        project.setProjectName(request.getProjectName());
        project.setClientName(request.getClientName());
        project.setMidClientName(request.getMidClientName());
        project.setCandidateWorkingCount(request.getCandidateWorkingCount() != null ? request.getCandidateWorkingCount() : 0);
        project.setInterviewCandidateCount(request.getInterviewCandidateCount() != null ? request.getInterviewCandidateCount() : 0);
        project.setOnboardedCandidateCount(request.getOnboardedCandidateCount() != null ? request.getOnboardedCandidateCount() : 0);
        project.setStartDate(request.getStartDate());
        project.setEndDate(request.getEndDate());
        project.setBudget(request.getBudget());
        if (request.getStatus() != null) project.setStatus(request.getStatus());
        project.setRemarks(request.getRemarks());
        return project;
    }

    private ProjectResponse toResponse(Project project, boolean includeBudget) {
        List<Long> userIds = assignmentRepository.findByProjectId(project.getId()).stream()
                .map(a -> a.getUser().getId()).toList();
        return ProjectResponse.builder()
                .id(project.getId())
                .projectName(project.getProjectName())
                .clientName(project.getClientName())
                .midClientName(project.getMidClientName())
                .candidateWorkingCount(project.getCandidateWorkingCount())
                .interviewCandidateCount(project.getInterviewCandidateCount())
                .onboardedCandidateCount(project.getOnboardedCandidateCount())
                .startDate(project.getStartDate())
                .endDate(project.getEndDate())
                .budget(includeBudget ? project.getBudget() : null)
                .status(project.getStatus())
                .remarks(project.getRemarks())
                .assignedUserIds(userIds)
                .build();
    }

    private Project findProject(Long id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));
    }

    private boolean isAdmin() {
        UserPrincipal p = SecurityUtils.getCurrentUser();
        return p != null && Role.ADMIN.name().equals(p.getRole());
    }

    private void requireAdmin() {
        if (!isAdmin()) throw new BadRequestException("Only admin can modify projects");
    }
}
