package com.xinghuo.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinghuo.model.AuditLog;
import com.xinghuo.repository.AuditLogRepository;
import com.xinghuo.util.RequestUtil;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import lombok.Generated;
import org.springframework.stereotype.Service;

@Service
public class AuditService {
    private final AuditLogRepository repo;
    private final ObjectMapper mapper;

    public void log(String action, String targetType, String targetId, String operator, Object before, Object after, String reason, HttpServletRequest req) {
        this.repo.save(AuditLog.builder().action(action).targetType(targetType).targetId(targetId).operatorName(operator).beforeJson(this.json(before)).afterJson(this.json(after)).reason(reason).ipAddress(req == null ? null : RequestUtil.clientIp((HttpServletRequest)req)).requestId(this.requestIdOf(req)).createdAt(Instant.now()).build());
    }

    private String requestIdOf(HttpServletRequest req) {
        if (req == null) {
            return null;
        }
        Object a = req.getAttribute("requestId");
        return a != null ? String.valueOf(a) : req.getHeader("X-Request-Id");
    }

    private String json(Object o) {
        if (o == null) {
            return null;
        }
        try {
            return this.mapper.writeValueAsString(o);
        }
        catch (Exception e) {
            return String.valueOf(o);
        }
    }

    @Generated
    public AuditService(AuditLogRepository repo, ObjectMapper mapper) {
        this.repo = repo;
        this.mapper = mapper;
    }
}

