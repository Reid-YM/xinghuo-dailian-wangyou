package com.xinghuo.observability;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class XingweiReleaseAttestationServiceTest {
    @TempDir Path tempDir;

    @Test
    void acceptsCompleteV2HostAttestationWithBeforeAfterHealthComparison() throws Exception {
        Path file = tempDir.resolve("release-attestation.json");
        Files.writeString(file, """
                {
                  "schemaVersion": 2,
                  "source": "production-host-gateway",
                  "publishedAt": "2026-10-17T05:07:04Z",
                  "gitSha": "acf8af76b156047ff33072cbeaf6b7789b8bb624",
                  "jarSha256": "d797326b0ddbcc1601db98ad59cda36d4fd709ad01d763458b107a874dbaf4ba",
                  "currentRelease": "xinghuo-v2-standard-20261017050537.jar",
                  "previousRelease": "xinghuo-v2-standard-20261017040039.jar",
                  "mainPort": 8080,
                  "canaryPort": 8081,
                  "nginxUpstreamPort": 8080,
                  "canaryStopped": true,
                  "checks": {"predeploy18080": true, "canary8081": true, "production8080": true},
                  "healthComparison": {
                    "preDeploy8080": {"passed": true, "homeMillis": 41, "servicesMillis": 58},
                    "postDeploy8080": {"passed": true, "homeMillis": 47, "servicesMillis": 62}
                  }
                }
                """);

        XingweiReleaseAttestationService service =
                new XingweiReleaseAttestationService(new ObjectMapper(), file.toString());
        XingweiReleaseAttestationService.Snapshot snapshot = service.snapshot();

        assertTrue(snapshot.available());
        assertEquals("AVAILABLE", snapshot.status());
        assertNotNull(snapshot.attestation());
        assertEquals(2, snapshot.attestation().schemaVersion());
        assertEquals(8080, snapshot.attestation().nginxUpstreamPort());
        assertTrue(snapshot.attestation().canaryStopped());
        assertTrue(snapshot.attestation().checks().production8080());
        assertNotNull(snapshot.attestation().healthComparison());
        assertEquals(41, snapshot.attestation().healthComparison().preDeploy8080().homeMillis());
        assertEquals(62, snapshot.attestation().healthComparison().postDeploy8080().servicesMillis());
    }

    @Test
    void keepsV1ReadableWithoutInventingHealthComparison() throws Exception {
        Path file = tempDir.resolve("release-attestation-v1.json");
        Files.writeString(file, """
                {
                  "schemaVersion": 1,
                  "source": "production-host-gateway",
                  "publishedAt": "2026-10-17T05:07:04Z",
                  "gitSha": "acf8af76b156047ff33072cbeaf6b7789b8bb624",
                  "jarSha256": "d797326b0ddbcc1601db98ad59cda36d4fd709ad01d763458b107a874dbaf4ba",
                  "currentRelease": "xinghuo-v2-standard-20261017050537.jar",
                  "previousRelease": "xinghuo-v2-standard-20261017040039.jar",
                  "mainPort": 8080,
                  "canaryPort": 8081,
                  "nginxUpstreamPort": 8080,
                  "canaryStopped": true,
                  "checks": {"predeploy18080": true, "canary8081": true, "production8080": true}
                }
                """);

        XingweiReleaseAttestationService service =
                new XingweiReleaseAttestationService(new ObjectMapper(), file.toString());
        XingweiReleaseAttestationService.Snapshot snapshot = service.snapshot();

        assertTrue(snapshot.available());
        assertEquals(1, snapshot.attestation().schemaVersion());
        assertNull(snapshot.attestation().healthComparison());
    }

    @Test
    void rejectsV2CredentialWhenHealthComparisonIsMissingOrIncomplete() throws Exception {
        Path file = tempDir.resolve("release-attestation-incomplete-v2.json");
        Files.writeString(file, """
                {
                  "schemaVersion": 2,
                  "source": "production-host-gateway",
                  "publishedAt": "2026-10-17T05:07:04Z",
                  "gitSha": "acf8af76b156047ff33072cbeaf6b7789b8bb624",
                  "jarSha256": "d797326b0ddbcc1601db98ad59cda36d4fd709ad01d763458b107a874dbaf4ba",
                  "currentRelease": "xinghuo-v2-standard-20261017050537.jar",
                  "previousRelease": "xinghuo-v2-standard-20261017040039.jar",
                  "mainPort": 8080,
                  "canaryPort": 8081,
                  "nginxUpstreamPort": 8080,
                  "canaryStopped": true,
                  "checks": {"predeploy18080": true, "canary8081": true, "production8080": true},
                  "healthComparison": {
                    "preDeploy8080": {"passed": true, "homeMillis": 41, "servicesMillis": 58}
                  }
                }
                """);

        XingweiReleaseAttestationService service =
                new XingweiReleaseAttestationService(new ObjectMapper(), file.toString());
        XingweiReleaseAttestationService.Snapshot snapshot = service.snapshot();

        assertFalse(snapshot.available());
        assertEquals("INVALID", snapshot.status());
        assertNull(snapshot.attestation());
    }

    @Test
    void rejectsPartialOrContradictoryAttestationWithoutLeakingDetails() throws Exception {
        Path file = tempDir.resolve("release-attestation.json");
        Files.writeString(file, """
                {
                  "schemaVersion": 1,
                  "source": "production-host-gateway",
                  "publishedAt": "2026-10-17T05:07:04Z",
                  "gitSha": "acf8af76b156047ff33072cbeaf6b7789b8bb624",
                  "jarSha256": "d797326b0ddbcc1601db98ad59cda36d4fd709ad01d763458b107a874dbaf4ba",
                  "currentRelease": "xinghuo-v2-standard-20261017050537.jar",
                  "previousRelease": "xinghuo-v2-standard-20261017040039.jar",
                  "mainPort": 8080,
                  "canaryPort": 8081,
                  "nginxUpstreamPort": 8081,
                  "canaryStopped": false,
                  "checks": {"predeploy18080": true, "canary8081": true, "production8080": false}
                }
                """);

        XingweiReleaseAttestationService service =
                new XingweiReleaseAttestationService(new ObjectMapper(), file.toString());
        XingweiReleaseAttestationService.Snapshot snapshot = service.snapshot();

        assertFalse(snapshot.available());
        assertEquals("INVALID", snapshot.status());
        assertNull(snapshot.attestation());
    }

    @Test
    void rejectsUntrustedSourceAndNonProductionPorts() throws Exception {
        Path file = tempDir.resolve("release-attestation.json");
        Files.writeString(file, """
                {
                  "schemaVersion": 1,
                  "source": "application-self-report",
                  "publishedAt": "2026-10-17T05:07:04Z",
                  "gitSha": "acf8af76b156047ff33072cbeaf6b7789b8bb624",
                  "jarSha256": "d797326b0ddbcc1601db98ad59cda36d4fd709ad01d763458b107a874dbaf4ba",
                  "currentRelease": "xinghuo-v2-standard-20261017050537.jar",
                  "previousRelease": "xinghuo-v2-standard-20261017040039.jar",
                  "mainPort": 9090,
                  "canaryPort": 9091,
                  "nginxUpstreamPort": 9090,
                  "canaryStopped": true,
                  "checks": {"predeploy18080": true, "canary8081": true, "production8080": true}
                }
                """);

        XingweiReleaseAttestationService service =
                new XingweiReleaseAttestationService(new ObjectMapper(), file.toString());
        XingweiReleaseAttestationService.Snapshot snapshot = service.snapshot();

        assertFalse(snapshot.available());
        assertEquals("INVALID", snapshot.status());
    }

    @Test
    void reportsMissingWhenHostCredentialDoesNotExist() {
        Path file = tempDir.resolve("missing.json");
        XingweiReleaseAttestationService service =
                new XingweiReleaseAttestationService(new ObjectMapper(), file.toString());

        XingweiReleaseAttestationService.Snapshot snapshot = service.snapshot();

        assertFalse(snapshot.available());
        assertEquals("MISSING", snapshot.status());
    }
}
