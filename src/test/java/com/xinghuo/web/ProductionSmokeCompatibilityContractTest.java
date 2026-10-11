package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ProductionSmokeCompatibilityContractTest {
    private static final Path ROOT = Path.of(".");

    @Test
    void activeSharedBridgeAndFeatureOwnedCanonicalStylesArePartOfTheSmokeContract() throws Exception {
        String smoke = Files.readString(ROOT.resolve("scripts/smoke_test.sh"));

        assertTrue(smoke.contains("check_code /assets/vnext-legacy-bridge-v3.css 200"));
        assertTrue(smoke.contains("check_code /assets/service-detail.css 200"));
        assertTrue(smoke.contains("check_code /assets/record-detail.css 200"));
        assertTrue(smoke.contains("check_code /assets/account-submit.css 200"));
        assertTrue(smoke.contains("check_code /assets/account-center.css 200"));
        assertTrue(smoke.contains("check_code /assets/account-center-refine.css 200"));
        assertFalse(smoke.contains("check_code /assets/vnext-account-submit-v3.css 200"));
    }

    @Test
    void theSameSmokeContractRunsOnPredeployCanaryAndFinalPorts() throws Exception {
        String githubDeploy = Files.readString(ROOT.resolve("scripts/deploy-from-github.sh"));
        String zeroDowntime = Files.readString(ROOT.resolve("scripts/deploy-zero-downtime.sh"));

        assertTrue(githubDeploy.contains("SMOKE_PORT=${SMOKE_PORT:-18080}"));
        assertTrue(githubDeploy.contains("bash \"$WORK/scripts/smoke_test.sh\" \"$SMOKE_PORT\""));

        assertTrue(zeroDowntime.contains("CANARY_PORT=${CANARY_PORT:-8081}"));
        assertTrue(zeroDowntime.contains("MAIN_PORT=${MAIN_PORT:-8080}"));
        assertTrue(zeroDowntime.contains("run_smoke \"$CANARY_PORT\""));
        assertTrue(zeroDowntime.contains("run_smoke \"$MAIN_PORT\""));
    }
}
