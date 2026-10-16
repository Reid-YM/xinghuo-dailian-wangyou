package com.xinghuo.observability;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/xingwei/release-stability")
@PreAuthorize("@xingweiPermissionGuard.has(authentication,'deploy:view')")
public class XingweiReleaseStabilityController {
    private final XingweiReleaseStabilityService service;

    public XingweiReleaseStabilityController(XingweiReleaseStabilityService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<XingweiReleaseStabilityService.Snapshot> snapshot() {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(service.snapshot());
    }
}
