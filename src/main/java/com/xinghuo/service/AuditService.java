package com.xinghuo.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinghuo.repository.AuditLogRepository;
import org.springframework.stereotype.Service;

/** @deprecated compatibility bridge; use com.xinghuo.audit.infrastructure.AuditService. */
@Deprecated(forRemoval = true)
@Service
public class AuditService extends com.xinghuo.audit.infrastructure.AuditService {
    public AuditService(AuditLogRepository repo, ObjectMapper mapper) {
        super(repo, mapper);
    }
}
