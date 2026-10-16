package com.xinghuo.observability;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class XingweiReleaseAttestationServiceTest {
    @TempDir Path tempDir;

    @Test
    void acceptsOnlyCompleteSuccessfulHostAttestation() throws Exception {
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
                  "nginxUpstreamPort": 8080,
                  "canaryStopped": true,
                  "checks": {"predeploy18080": true, "canary8081": true, "production8080": true}
                }
                """);

        XingweiReleaseAttestationService service =
                new XingweiReleaseAttestationService(new ObjectMapper(), file.toString());
        XingweiReleaseAttestationService.Snapshot snapshot = service.snapshot();

        assertTrue(snapshot.available());
        assertEquals("AVAILABLE", snapshot.status());
        assertNotNull(snapshot.attestation());
        assertEquals(8080, snapshot.attestation().nginxUpstreamPort());
        assertTrue(snapshot.attestation().canaryStopped());
        assertTrue(snapshot.attestation().checks().production8080());
    }

    @Test
    void rejectsPartialOrContradictoryAttestationWithoutLeakingDetails() throws Exception {
        Path file = tempDir.resolve("release-attestation.json");
        Files.writeString(file, """
                {
                  "schemaVersion": 1,
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
        assertEquals(null, snapshot.attestation());
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
