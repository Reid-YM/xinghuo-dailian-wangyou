package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class InfrastructureObservabilityContractTest {
    private static final Path ROOT = Path.of(".");

    @Test
    void applicationLogsCarryRequestCorrelationContext() throws Exception {
        String config = Files.readString(ROOT.resolve("src/main/resources/application.yml"));
        String filter = Files.readString(ROOT.resolve(
                "src/main/java/com/xinghuo/observability/RequestCorrelationFilter.java"));

        assertTrue(config.contains("requestId:%X{requestId:-none}"));
        assertTrue(filter.contains("X-Request-Id"));
        assertTrue(filter.contains("MDC.put(MDC_KEY, requestId)"));
        assertTrue(filter.contains("response.setHeader(HEADER, requestId)"));
        assertTrue(filter.contains("MDC.remove(MDC_KEY)"));
        assertTrue(filter.contains("durationMs"));
    }

    @Test
    void productionDeployPersistsVerifiedReleaseManifestOnlyAfterPostChecks() throws Exception {
        String deploy = Files.readString(ROOT.resolve("scripts/deploy-from-github.sh"));

        int verifyJar = deploy.indexOf("[ \"$CURRENT_SHA\" = \"$CANDIDATE_SHA\" ]");
        int writeManifest = deploy.indexOf("MANIFEST_TMP=$(mktemp)");
        assertTrue(verifyJar >= 0 && writeManifest > verifyJar,
                "release manifest must be written after deployed JAR verification");
        assertTrue(deploy.contains("RELEASE_MANIFEST=${RELEASE_MANIFEST:-$APP_DIR/current-release.env}"));
        assertTrue(deploy.contains("DEPLOY_GIT_SHA=$HEAD_SHA"));
        assertTrue(deploy.contains("JAR_SHA256=$CURRENT_SHA"));
        assertTrue(deploy.contains("DEPLOY_SOURCE=github-main"));
        assertTrue(deploy.contains("sudo mv -Tf \"$RELEASE_MANIFEST.next\" \"$RELEASE_MANIFEST\""));
        assertTrue(deploy.contains("release manifest Git SHA 校验失败"));
        assertTrue(deploy.contains("release manifest JAR SHA256 校验失败"));
    }
}
