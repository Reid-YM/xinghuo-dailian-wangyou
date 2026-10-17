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
        assertTrue(service.contains("REVIEW_NOW"));
        assertTrue(service.contains("priorityReason"));
        assertTrue(service.contains("RiskRuntime"));
        assertFalse(service.contains("systemctl"));
        assertFalse(service.contains("rollback"));
    }

    @Test
    void riskRulesUseBoundedBudgetAndFailUnknownInsteadOfPretendingLow() throws Exception {
        String risk = Files.readString(Path.of(
                "src/main/java/com/xinghuo/observability/XingweiReleaseChangeRiskService.java"));
        String ui = Files.readString(Path.of(
                "src/main/resources/static/assets/admin-xingwei-release-confirmation.js"));
        String priorityUi = Files.readString(Path.of(
                "src/main/resources/static/assets/admin-xingwei-release-review-priority.js"));
        String roadmap = Files.readString(Path.of(
                "src/main/resources/templates/fragments/admin-delivery-roadmap.html"));

        assertTrue(risk.contains("PER_PR_LOOKUP_BUDGET_MILLIS = 1200L"));
        assertTrue(risk.contains("REQUEST_TIMEOUT_CEILING_MILLIS = 700L"));
        assertTrue(risk.contains("SUCCESS_CACHE_TTL_MILLIS = TimeUnit.HOURS.toMillis(6)"));
        assertTrue(risk.contains("UNKNOWN_CACHE_TTL_MILLIS = TimeUnit.SECONDS.toMillis(90)"));
        assertTrue(risk.contains("LOOKUP_BUDGET_EXHAUSTED"));
        assertTrue(risk.contains("GITHUB_UNAVAILABLE"));
        assertTrue(risk.contains("PR_FILE_LIST_TRUNCATED"));
        assertTrue(risk.contains("\"UNKNOWN\""));
        assertTrue(risk.contains("cacheState"));
        assertTrue(risk.contains("withCacheState(\"HIT\")"));
        assertTrue(risk.contains("DATABASE"));
        assertTrue(risk.contains("PAYMENT"));
        assertTrue(risk.contains("SECURITY"));
        assertTrue(risk.contains("DEPLOYMENT"));
        assertTrue(ui.contains("/api/admin/xingwei/release-review-queue"));
        assertTrue(ui.contains("风险标签只决定人工复核优先级，不触发自动回滚"));
        assertTrue(ui.contains("ARCHIVED_UNCONFIRMED"));
        assertTrue(priorityUi.contains("复核优先级与风险查询预算"));
        assertTrue(priorityUi.contains("不代表整个接口 SLA"));
        assertTrue(priorityUi.contains("UNKNOWN 短缓存"));
        assertTrue(priorityUi.contains("cacheState === 'HIT'"));
        assertTrue(priorityUi.contains("缓存命中"));
        assertFalse(priorityUi.contains("fetch("));
        assertTrue(roadmap.contains("admin-xingwei-release-review-priority.js?v=20261017v1"));
    }
}
