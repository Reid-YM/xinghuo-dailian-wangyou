package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class XingweiReleaseReviewQueueContractTest {
    @Test
    void queueIsDeployViewNoStoreAndReadOnly() throws Exception {
        String controller = Files.readString(Path.of(
                "src/main/java/com/xinghuo/observability/XingweiReleaseReviewQueueController.java"));
        String service = Files.readString(Path.of(
                "src/main/java/com/xinghuo/observability/XingweiReleaseReviewQueueService.java"));

        assertTrue(controller.contains("/api/admin/xingwei/release-review-queue"));
        assertTrue(controller.contains("deploy:view"));
        assertTrue(controller.contains("@GetMapping"));
        assertTrue(controller.contains("noStore()"));
        assertFalse(controller.contains("@PostMapping"));
        assertTrue(service.contains("ARCHIVED_UNCONFIRMED"));
        assertTrue(service.contains("真正可操作的仍只有当前可信生产 release"));
        assertFalse(service.contains("systemctl"));
        assertFalse(service.contains("rollback"));
    }

    @Test
    void riskRulesFailUnknownInsteadOfPretendingLow() throws Exception {
        String risk = Files.readString(Path.of(
                "src/main/java/com/xinghuo/observability/XingweiReleaseChangeRiskService.java"));
        String ui = Files.readString(Path.of(
                "src/main/resources/static/assets/admin-xingwei-release-confirmation.js"));

        assertTrue(risk.contains("GITHUB_UNAVAILABLE"));
        assertTrue(risk.contains("PR_FILE_LIST_TRUNCATED"));
        assertTrue(risk.contains("\"UNKNOWN\""));
        assertTrue(risk.contains("DATABASE"));
        assertTrue(risk.contains("PAYMENT"));
        assertTrue(risk.contains("SECURITY"));
        assertTrue(risk.contains("DEPLOYMENT"));
        assertTrue(ui.contains("/api/admin/xingwei/release-review-queue"));
        assertTrue(ui.contains("风险标签只决定人工复核优先级，不触发自动回滚"));
        assertTrue(ui.contains("ARCHIVED_UNCONFIRMED"));
    }
}
