package com.example.smartfoodordering.service.impl;

import com.example.smartfoodordering.dto.response.AuditLogResponse;
import com.example.smartfoodordering.entity.AuditLog;
import com.example.smartfoodordering.enums.AuditAction;
import com.example.smartfoodordering.repository.AuditLogRepository;
import com.example.smartfoodordering.service.AuditService;
import com.example.smartfoodordering.util.RequestUtil;
import com.example.smartfoodordering.util.TextUtil;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditServiceImpl implements AuditService {
    private final AuditLogRepository auditLogRepository;

    public AuditServiceImpl(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    /** Runs in its own transaction so security events survive a rollback of the business transaction. */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(String actorEmail, AuditAction action, String entityType, Long entityId, String details) {
        AuditLog entry = new AuditLog();
        entry.setActorEmail(TextUtil.truncate(actorEmail, 150));
        entry.setAction(action);
        entry.setEntityType(entityType);
        entry.setEntityId(entityId);
        entry.setDetails(TextUtil.truncate(details, 800));
        entry.setIpAddress(RequestUtil.clientIp());
        auditLogRepository.save(entry);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditLogResponse> recent() {
        return auditLogRepository.findTop100ByOrderByCreatedAtDesc().stream()
                .map(a -> new AuditLogResponse(a.getCreatedAt(), a.getActorEmail(), a.getAction(), a.getEntityType(),
                        a.getEntityId(), a.getDetails(), a.getIpAddress()))
                .toList();
    }
}
