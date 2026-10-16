package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class XingweiReleaseAttestationContractTest {
    @Test
    void endpointIsReadOnlyNoStoreAndProtectedByDeployView() throws Exception {
        String controller = Files.readString(Path.of("src/main/java/com/xinghuo/observability/XingweiReleaseAttestationController.java"));
        String service = Files.readString(Path.of("src/main/java/com/xinghuo/observability/XingweiReleaseAttestationService.java"));

        assertTrue(controller.contains("/api/admin/xingwei/release-attestation"));
        assertTrue(controller.contains("deploy:view"));
        assertTrue(controller.contains("CacheControl.noStore()"));
        assertTrue(service.contains("release-attestation.json"));
        assertTrue(service.contains("TRUSTED_SOURCE = \"production-host-gateway\""));
        assertTrue(service.contains("EXPECTED_MAIN_PORT = 8080"));
        assertTrue(service.contains("EXPECTED_CANARY_PORT = 8081"));
        assertTrue(service.contains("schemaVersion != 1 && schemaVersion != 2"));
        assertTrue(service.contains("healthComparison"));
        assertTrue(service.contains("production8080"));
        assertTrue(service.contains("nginxUpstreamPort != EXPECTED_MAIN_PORT"));
        assertTrue(service.contains("canaryStopped"));
    }

    @Test
    void productionGatewayWritesV2CredentialOnlyAfterPostDeployVerification() throws Exception {
        String script = Files.readString(Path.of("scripts/deploy-from-github.sh"));
        int postDeploySha = script.indexOf("发布后的 current.jar SHA256 与候选包不一致");
        int postDeployProbe = script.indexOf("发布后 8080 健康探针");
        int credential = script.indexOf("主机侧发布凭证已更新（含发布前后 8080 健康对比）");

        assertTrue(postDeployProbe >= 0);
        assertTrue(postDeploySha > postDeployProbe);
        assertTrue(credential > postDeploySha);
        assertTrue(script.contains("\"schemaVersion\": 2"));
        assertTrue(script.contains("source\": \"production-host-gateway"));
        assertTrue(script.contains("部署前 8080 健康基线"));
        assertTrue(script.contains("\"healthComparison\""));
        assertTrue(script.contains("\"preDeploy8080\""));
        assertTrue(script.contains("\"postDeploy8080\""));
        assertTrue(script.contains("predeploy18080\": true"));
        assertTrue(script.contains("canary8081\": true"));
        assertTrue(script.contains("production8080\": true"));
        assertTrue(script.contains("nginxUpstreamPort\": 8080"));
        assertTrue(script.contains("canaryStopped\": true"));
        assertTrue(script.contains("sudo mv -Tf \"$RELEASE_ATTESTATION_FILE.next\" \"$RELEASE_ATTESTATION_FILE\""));
    }
}
