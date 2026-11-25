package com.example.smartfoodordering.repository;

import com.example.smartfoodordering.entity.*;
import com.example.smartfoodordering.enums.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findTop100ByOrderByCreatedAtDesc();
    List<AuditLog> findTop100ByActorEmailIgnoreCaseOrderByCreatedAtDesc(String actorEmail);
}
