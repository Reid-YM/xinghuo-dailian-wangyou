package com.xinghuo.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinghuo.model.AuditLog;
import com.xinghuo.repository.AuditLogRepository;
import com.xinghuo.util.RequestUtil;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import org.springframework.stereotype.Service;

@Service
public class AuditService {
    private final AuditLogRepository repo;
    private final ObjectMapper mapper;

    public AuditService(AuditLogRepository repo, ObjectMapper mapper) {
        this.repo = repo;
        this.mapper = mapper;
    }

    public void log(
            String action,
            String targetType,
            String targetId,
            String operator,
            Object before,
            Object after,
            String reason,
            HttpServletRequest request) {
        AuditLog entry = AuditLog.builder()
                .action(action)
                .targetType(targetType)
                .targetId(targetId)
                .operatorName(operator)
                .beforeJson(this.json(before))
                .afterJson(this.json(after))
                .reason(reason)
                .ipAddress(request == null ? null : RequestUtil.clientIp(request))
                .requestId(this.requestIdOf(request))
                .createdAt(Instant.now())
                .build();
        this.repo.save(entry);
    }

    private String requestIdOf(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        Object requestId = request.getAttribute("requestId");
        return requestId != null ? String.valueOf(requestId) : request.getHeader("X-Request-Id");
    }

    private String json(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return this.mapper.writeValueAsString(value);
        }
        catch (Exception ignored) {
            return String.valueOf(value);
        }
    }
}
