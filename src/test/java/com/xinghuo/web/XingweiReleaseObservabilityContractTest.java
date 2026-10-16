package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class XingweiReleaseObservabilityContractTest {
    @Test
    void releasePageSeparatesRuntimeHostTruthAndOperationsAlerts() throws Exception {
        String template = Files.readString(Path.of("src/main/resources/templates/admin-xingwei-releases.html"));
        String trend = Files.readString(Path.of("src/main/resources/static/assets/admin-xingwei-runtime-trend.js"));
        String release = Files.readString(Path.of("src/main/resources/static/assets/admin-xingwei-release-attestation.js"));
        String releaseCss = Files.readString(Path.of("src/main/resources/static/assets/admin-xingwei-release-observability.css"));
        String alertJs = Files.readString(Path.of("src/main/resources/static/assets/admin-xingwei-operations-alerts.js"));
        String alertCss = Files.readString(Path.of("src/main/resources/static/assets/admin-xingwei-operations-alerts.css"));

        assertTrue(template.contains("应用运行真值"));
        assertTrue(template.contains("生产主机发布真值"));
        assertTrue(template.contains("稳定运营自动复核"));
        assertTrue(template.contains("最近 15 分钟请求与错误趋势"));
        assertTrue(template.contains("最近成功发布凭证"));
        assertTrue(template.contains("发布前后 8080 健康对比"));
        assertTrue(template.contains("异常阈值与复核闭环"));
        assertTrue(template.contains("id=\"xwrRuntimeTrend\""));
        assertTrue(template.contains("id=\"xwrReleaseAttestation\""));
        assertTrue(template.contains("id=\"xwrReleaseHealthBefore\""));
        assertTrue(template.contains("id=\"xwrReleaseHealthAfter\""));
        assertTrue(template.contains("id=\"xwrOperationsAlerts\""));
        assertTrue(template.contains("不暴露服务器路径"));
        assertTrue(template.contains("本页不执行重启、回滚、部署或业务写入"));
        assertTrue(trend.contains("recentMinutes"));
        assertTrue(trend.contains("dataset.serverErrors"));
        assertTrue(release.contains("/api/admin/xingwei/release-attestation"));
        assertTrue(release.contains("healthComparison"));
        assertTrue(release.contains("deploy:view"));
        assertTrue(release.contains("cache: 'no-store'"));
        assertTrue(alertJs.contains("/api/admin/xingwei/operations-alerts"));
        assertTrue(alertJs.contains("recommendedAction"));
        assertTrue(alertJs.contains("REFRESH_INTERVAL_MS = 60000"));
        assertTrue(releaseCss.contains("@media(max-width:760px)"));
        assertTrue(alertCss.contains("@media(max-width:620px)"));
    }
}
