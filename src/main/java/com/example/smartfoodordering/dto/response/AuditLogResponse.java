package com.example.smartfoodordering.dto.response;

import com.example.smartfoodordering.enums.AuditAction;
import java.time.LocalDateTime;

public record AuditLogResponse(LocalDateTime at, String actor, AuditAction action, String entityType, Long entityId,
                               String details, String ipAddress) {
}
