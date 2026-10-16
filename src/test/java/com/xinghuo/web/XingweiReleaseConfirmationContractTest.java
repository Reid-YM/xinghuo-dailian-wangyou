package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class XingweiReleaseConfirmationContractTest {
    @Test
    void confirmationEndpointKeepsHostTruthSeparateAndWritesOnlyForAdmin() throws Exception {
        String controller = Files.readString(Path.of("src/main/java/com/xinghuo/observability/XingweiReleaseConfirmationController.java"));
        String service = Files.readString(Path.of("src/main/java/com/xinghuo/observability/XingweiReleaseConfirmationService.java"));
        String template = Files.readString(Path.of("src/main/resources/templates/admin-xingwei-releases.html"));
        String js = Files.readString(Path.of("src/main/resources/static/assets/admin-xingwei-release-confirmation.js"));

        assertTrue(controller.contains("/api/admin/xingwei/release-confirmation"));
        assertTrue(controller.contains("has(authentication,'deploy:view')"));
        assertTrue(controller.contains("resolve(authentication).allowAll()"));
        assertTrue(controller.contains("CacheControl.noStore()"));

        assertTrue(service.contains("admin-human-confirmation"));
        assertTrue(service.contains("expectedRelease"));
        assertTrue(service.contains("expectedGitSha"));
        assertTrue(service.contains("ATOMIC_MOVE"));
        assertTrue(service.contains("MAX_NOTE_LENGTH = 240"));
        assertTrue(service.contains("window30State"));
        assertTrue(service.contains("\"COMPLETE\".equals(window30.state())"));
        assertTrue(service.contains("\"OK\".equals(current.severity())"));
        assertTrue(service.contains("NEEDS_ATTENTION"));
        assertTrue(service.contains("pullRequestNumber"));

        assertTrue(template.contains("人工确认独立于生产发布真值"));
        assertTrue(template.contains("id=\"xwrReleaseConfirmation\""));
        assertTrue(template.contains("id=\"xwrReleaseConfirmStable\""));
        assertTrue(template.contains("id=\"xwrReleaseConfirmAttention\""));
        assertTrue(template.contains("name=\"_csrf\""));
        assertTrue(template.contains("不记录 IP、UA、请求体、订单号或业务编号"));

        assertTrue(js.contains("credentials:'same-origin'"));
        assertTrue(js.contains("cache:'no-store'"));
        assertTrue(js.contains("headers[csrfHeader] = csrfToken"));
        assertTrue(js.contains("expectedRelease"));
        assertTrue(js.contains("expectedGitSha"));
        assertTrue(js.contains("response.status"));
        assertTrue(js.contains("status === 401 || status === 403"));
        assertFalse(js.contains("innerHTML = item.note"));
    }

    @Test
    void productionGatewayAddsChangeAssociationWithoutReplacingDeploymentTruth() throws Exception {
        String deploy = Files.readString(Path.of("scripts/deploy-from-github.sh"));
        String workflow = Files.readString(Path.of(".github/workflows/deploy-production.yml"));
        String gateway = Files.readString(Path.of("scripts/github-deploy-gateway.sh"));

        assertTrue(deploy.contains("RELEASE_CONFIRMATION_DIR"));
        assertTrue(deploy.contains("root -g xinghuo -m 0770"));
        assertTrue(deploy.contains("Merge pull request #"));
        assertTrue(deploy.contains("\"pullRequestNumber\": $PULL_REQUEST_JSON_VALUE"));
        assertTrue(deploy.contains("CURRENT_SHA=$(sudo sha256sum"));
        assertTrue(deploy.indexOf("CURRENT_SHA=$(sudo sha256sum") < deploy.indexOf("\"pullRequestNumber\": $PULL_REQUEST_JSON_VALUE"));
        assertTrue(deploy.contains("source\": \"production-host-gateway"));
        assertTrue(deploy.contains("主机侧发布凭证已更新"));

        assertTrue(workflow.contains("Require merged Pull Request origin"));
        assertTrue(workflow.contains("Require successful main CI"));
        assertTrue(gateway.contains("only 'deploy <40-char-sha>' is allowed"));
        assertFalse(workflow.contains("release-confirmation"));
        assertFalse(gateway.contains("release-confirmation"));
    }
}
