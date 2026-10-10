package com.xinghuo.audit.web;

import com.xinghuo.audit.application.AuditQueryApplicationService;
import com.xinghuo.dto.ApiDtos;
import java.util.List;
import lombok.Generated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value={"/api/admin/audits"})
public class AdminAuditController {
    private final AuditQueryApplicationService audits;

    @GetMapping
    List<ApiDtos.AuditResponse> audits() {
        return this.audits.latest100().stream()
                .map(x -> new ApiDtos.AuditResponse(
                        x.id(),
                        x.action(),
                        x.targetType(),
                        x.targetId(),
                        x.operatorName(),
                        x.reason(),
                        x.ipAddress(),
                        x.createdAt()))
                .toList();
    }

    @Generated
    public AdminAuditController(AuditQueryApplicationService audits) {
        this.audits = audits;
    }
}
