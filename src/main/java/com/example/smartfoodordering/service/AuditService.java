package com.example.smartfoodordering.service;

import com.example.smartfoodordering.dto.response.AuditLogResponse;
import com.example.smartfoodordering.enums.AuditAction;
import java.util.List;

public interface AuditService {
    void log(String actorEmail, AuditAction action, String entityType, Long entityId, String details);

    List<AuditLogResponse> recent();
}
