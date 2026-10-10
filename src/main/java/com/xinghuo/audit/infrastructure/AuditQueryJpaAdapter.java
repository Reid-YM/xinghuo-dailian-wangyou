package com.xinghuo.audit.infrastructure;

import com.xinghuo.audit.application.AuditEntryView;
import com.xinghuo.audit.application.AuditQueryPort;
import com.xinghuo.repository.AuditLogRepository;
import java.util.List;
import lombok.Generated;
import org.springframework.stereotype.Component;

@Component
public class AuditQueryJpaAdapter implements AuditQueryPort {
    private final AuditLogRepository repo;

    @Override
    public List<AuditEntryView> latest100() {
        return this.repo.findTop100ByOrderByCreatedAtDesc().stream()
                .map(x -> new AuditEntryView(
                        x.getId(),
                        x.getAction(),
                        x.getTargetType(),
                        x.getTargetId(),
                        x.getOperatorName(),
                        x.getReason(),
                        x.getIpAddress(),
                        x.getCreatedAt()))
                .toList();
    }

    @Generated
    public AuditQueryJpaAdapter(AuditLogRepository repo) {
        this.repo = repo;
    }
}
