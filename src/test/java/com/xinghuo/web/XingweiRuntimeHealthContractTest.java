package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class XingweiRuntimeHealthContractTest {
    private static final Path CONTROLLER = Path.of(
            "src/main/java/com/xinghuo/observability/XingweiRuntimeHealthController.java");
    private static final Path METRICS = Path.of(
            "src/main/java/com/xinghuo/observability/XingweiRuntimeRequestMetrics.java");
    private static final Path SERVICE = Path.of(
            "src/main/java/com/xinghuo/observability/XingweiRuntimeHealthService.java");

    @Test
    void endpointRequiresDeployViewAndDisablesCaching() throws Exception {
        String controller = Files.readString(CONTROLLER);
        assertTrue(controller.contains("/api/admin/xingwei/runtime-health"));
        assertTrue(controller.contains("@xingweiPermissionGuard.has(authentication,'deploy:view')"));
        assertTrue(controller.contains("CacheControl.noStore()"));
    }

    @Test
    void requestSnapshotDoesNotStoreRequestIdentityOrBusinessContent() throws Exception {
        String metrics = Files.readString(METRICS);
        assertFalse(metrics.contains("HttpServletRequest"));
        assertFalse(metrics.contains("requestURI"));
        assertFalse(metrics.contains("queryString"));
        assertFalse(metrics.contains("username"));
        assertFalse(metrics.contains("businessId"));
    }

    @Test
    void databaseCheckReturnsOnlyConnectivityAndLatency() throws Exception {
        String service = Files.readString(SERVICE);
        assertTrue(service.contains("SELECT 1"));
        assertTrue(service.contains("DatabaseSnapshot(boolean up, long checkMillis)"));
        assertFalse(service.contains("getMessage()"));
        assertFalse(service.contains("getDataSource()"));
    }
}
