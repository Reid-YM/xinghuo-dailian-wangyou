package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class XingweiReleaseObservabilityContractTest {
    @Test
    void releasePageSeparatesRuntimeTruthFromHostReleaseTruth() throws Exception {
        String template = Files.readString(Path.of("src/main/resources/templates/admin-xingwei-releases.html"));
        String trend = Files.readString(Path.of("src/main/resources/static/assets/admin-xingwei-runtime-trend.js"));
        String release = Files.readString(Path.of("src/main/resources/static/assets/admin-xingwei-release-attestation.js"));
        String css = Files.readString(Path.of("src/main/resources/static/assets/admin-xingwei-release-observability.css"));

        assertTrue(template.contains("应用运行真值"));
        assertTrue(template.contains("生产主机发布真值"));
        assertTrue(template.contains("最近 15 分钟请求与错误趋势"));
        assertTrue(template.contains("最近成功发布凭证"));
        assertTrue(template.contains("id=\"xwrRuntimeTrend\""));
        assertTrue(template.contains("id=\"xwrReleaseAttestation\""));
        assertTrue(template.contains("不暴露服务器路径"));
        assertTrue(trend.contains("recentMinutes"));
        assertTrue(trend.contains("data-server-errors"));
        assertTrue(release.contains("/api/admin/xingwei/release-attestation"));
        assertTrue(release.contains("deploy:view"));
        assertTrue(release.contains("cache: 'no-store'"));
        assertTrue(css.contains("@media(max-width:760px)"));
    }
}
