package com.xinghuo.observability;

import java.util.concurrent.TimeUnit;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/xingwei/release-review-queue")
@PreAuthorize("@xingweiPermissionGuard.has(authentication,'deploy:view')")
public class XingweiReleaseReviewQueueController {
    private final XingweiReleaseReviewQueueService service;

    public XingweiReleaseReviewQueueController(XingweiReleaseReviewQueueService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<XingweiReleaseReviewQueueService.Snapshot> snapshot() {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(0, TimeUnit.SECONDS).noStore().mustRevalidate())
                .body(service.snapshot());
    }
}
