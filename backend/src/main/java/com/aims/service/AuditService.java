package com.aims.service;

import com.aims.dto.audit.AuditLogResponse;
import com.aims.dto.common.PageResponse;
import com.aims.entity.AuditLog;
import com.aims.entity.User;
import com.aims.repository.AuditLogRepository;
import com.aims.util.MapperUtils;
import com.aims.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    @Async
    @Transactional
    public void log(String action, String entityType, Long entityId, String oldValue, String newValue) {
        var currentUser = SecurityUtils.getCurrentUser();
        AuditLog log = AuditLog.builder()
                .action(action)
                .userEmail(currentUser != null ? currentUser.getEmail() : "system")
                .entityType(entityType)
                .entityId(entityId)
                .oldValue(oldValue)
                .newValue(newValue)
                .build();
        auditLogRepository.save(log);
    }

    @Transactional(readOnly = true)
    public PageResponse<AuditLogResponse> getAll(Pageable pageable) {
        Page<AuditLog> page = auditLogRepository.findAllByOrderByCreatedAtDesc(pageable);
        return PageResponse.from(page.map(MapperUtils::toAuditLogResponse));
    }

    @Transactional(readOnly = true)
    public PageResponse<AuditLogResponse> getByEntity(String entityType, Long entityId, Pageable pageable) {
        Page<AuditLog> page = auditLogRepository.findByEntityTypeAndEntityIdOrderByCreatedAtDesc(
                entityType, entityId, pageable);
        return PageResponse.from(page.map(MapperUtils::toAuditLogResponse));
    }
}
