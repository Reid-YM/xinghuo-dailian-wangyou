package com.xinghuo.audit.application;

import java.util.List;
import lombok.Generated;
import org.springframework.stereotype.Service;

@Service
public class AuditQueryApplicationService {
    private final AuditQueryPort port;

    public List<AuditEntryView> latest100() {
        return this.port.latest100();
    }

    @Generated
    public AuditQueryApplicationService(AuditQueryPort port) {
        this.port = port;
    }
}
