package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class XingweiOperationsAlertContractTest {
    @Test
    void alertEndpointIsReadOnlyNoStoreAndDeployViewProtected() throws Exception {
        String controller = Files.readString(Path.of("src/main/java/com/xinghuo/observability/XingweiOperationsAlertController.java"));
        String service = Files.readString(Path.of("src/main/java/com/xinghuo/observability/XingweiOperationsAlertService.java"));

        assertTrue(controller.contains("/api/admin/xingwei/operations-alerts"));
        assertTrue(controller.contains("deploy:view"));
        assertTrue(controller.contains("CacheControl.noStore()"));
        assertTrue(service.contains("RECENT_WINDOW_MINUTES = 5"));
        assertTrue(service.contains("SERVER_WARNING_COUNT = 3L"));
        assertTrue(service.contains("SERVER_CRITICAL_COUNT = 10L"));
        assertTrue(service.contains("HEAP_WARNING_PERCENT = 90.0"));
        assertTrue(service.contains("release-health-comparison-pending"));
        assertTrue(service.contains("recommendedAction"));

        assertFalse(service.contains("requestBody"));
        assertFalse(service.contains("businessNo"));
        assertFalse(service.contains("clientIp"));
        assertFalse(service.contains("restart("));
        assertFalse(service.contains("rollback("));
    }
}
