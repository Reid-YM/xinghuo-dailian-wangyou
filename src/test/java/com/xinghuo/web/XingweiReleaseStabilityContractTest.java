package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class XingweiReleaseStabilityContractTest {
    @Test
    void endpointAndPageStayReadOnlyPermissionedAndPrivacyBounded() throws Exception {
        String controller = Files.readString(Path.of("src/main/java/com/xinghuo/observability/XingweiReleaseStabilityController.java"));
        String service = Files.readString(Path.of("src/main/java/com/xinghuo/observability/XingweiReleaseStabilityService.java"));
        String history = Files.readString(Path.of("src/main/java/com/xinghuo/observability/XingweiReleaseHistoryService.java"));
        String metrics = Files.readString(Path.of("src/main/java/com/xinghuo/observability/XingweiRuntimeRequestMetrics.java"));
        String template = Files.readString(Path.of("src/main/resources/templates/admin-xingwei-releases.html"));
        String script = Files.readString(Path.of("src/main/resources/static/assets/admin-xingwei-release-stability.js"));
        String css = Files.readString(Path.of("src/main/resources/static/assets/admin-xingwei-release-stability.css"));

        assertTrue(controller.contains("/api/admin/xingwei/release-stability"));
        assertTrue(controller.contains("deploy:view"));
        assertTrue(controller.contains("CacheControl.noStore()"));
        assertTrue(controller.contains("@GetMapping"));
        assertFalse(controller.contains("@PostMapping"));
        assertFalse(controller.contains("@PutMapping"));
        assertFalse(controller.contains("@DeleteMapping"));

        assertTrue(service.contains("OBSERVATION_WINDOWS = {5, 15, 30}"));
        assertTrue(service.contains("PROCESS_RESTARTED"));
        assertTrue(service.contains("EXPIRED"));
        assertTrue(metrics.contains("RETENTION_MINUTES = 60"));
        assertTrue(history.contains("release-attestations"));
        assertTrue(history.contains("LinkOption.NOFOLLOW_LINKS"));
        assertTrue(history.contains("parseTrusted"));

        assertTrue(template.contains("5 / 15 / 30 分钟稳定性观察"));
        assertTrue(template.contains("id=\"xwrReleaseWindow5\""));
        assertTrue(template.contains("id=\"xwrReleaseWindow15\""));
        assertTrue(template.contains("id=\"xwrReleaseWindow30\""));
        assertTrue(template.contains("id=\"xwrReleaseStabilityHistory\""));
        assertTrue(script.contains("credentials: 'same-origin'"));
        assertTrue(script.contains("cache: 'no-store'"));
        assertTrue(script.contains("REFRESH_INTERVAL_MS = 60000"));
        assertTrue(script.contains("status === 403"));
        assertTrue(css.contains("@media(max-width:760px)"));

        String combined = service + history + script;
        assertFalse(combined.contains("requestBody"));
        assertFalse(combined.contains("clientIp"));
        assertFalse(combined.contains("businessNo"));
        assertFalse(combined.contains("orderNo"));
    }

    @Test
    void productionGatewayArchivesHistoryOnlyAfterProductionVerification() throws Exception {
        String script = Files.readString(Path.of("scripts/deploy-from-github.sh"));
        int currentShaVerified = script.indexOf("发布后的 current.jar SHA256 与候选包不一致");
        int historyDir = script.indexOf("RELEASE_ATTESTATION_HISTORY_DIR");
        int archivePrevious = script.indexOf("已归档上一版主机发布凭证");
        int currentCredential = script.indexOf("主机侧发布凭证已更新（含发布前后 8080 健康对比）");
        int archiveCurrent = script.indexOf("当前主机发布凭证已加入历史基线");

        assertTrue(historyDir >= 0);
        assertTrue(currentShaVerified >= 0);
        assertTrue(archivePrevious > currentShaVerified);
        assertTrue(currentCredential > archivePrevious);
        assertTrue(archiveCurrent > currentCredential);
        assertTrue(script.contains("$(basename \"$PREVIOUS_JAR\" .jar).json"));
        assertTrue(script.contains("$(basename \"$CURRENT_JAR\" .jar).json"));
        assertTrue(script.contains("归档失败；不影响已完成的生产切流"));
    }
}
