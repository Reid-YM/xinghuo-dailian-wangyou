package com.xinghuo.repository;

import com.xinghuo.model.AuditLog;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository
extends JpaRepository<AuditLog, Long> {
    public List<AuditLog> findTop100ByOrderByCreatedAtDesc();
}

