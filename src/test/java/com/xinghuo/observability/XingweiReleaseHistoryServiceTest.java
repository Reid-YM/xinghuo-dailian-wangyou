package com.xinghuo.observability;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class XingweiReleaseHistoryServiceTest {
    @TempDir Path tempDir;

    @Test
    void buildsMedianBaselineFromTrustedV2HistoryAndIgnoresMismatchedFile() throws Exception {
        Path history = tempDir.resolve("history");
        Files.createDirectories(history);
        writeV2(history, "20261017060000", "20261017050000", "a".repeat(40), 40, 50);
        writeV2(history, "20261017070000", "20261017060000", "b".repeat(40), 60, 70);

        Files.writeString(history.resolve("xinghuo-v2-standard-20261017080000.json"), v2Json(
                "20261017070000",
                "20261017060000",
                "c".repeat(40),
                80,
                90));

        ObjectMapper mapper = new ObjectMapper();
        XingweiReleaseAttestationService attestation = new XingweiReleaseAttestationService(
                mapper,
                tempDir.resolve("current.json").toString());
        XingweiReleaseHistoryService service = new XingweiReleaseHistoryService(
                mapper,
                attestation,
                history.toString());

        XingweiReleaseHistoryService.Snapshot snapshot = service.snapshot();

        assertTrue(snapshot.available());
        assertEquals("AVAILABLE", snapshot.status());
        assertEquals(2, snapshot.entries().size());
        assertEquals("xinghuo-v2-standard-20261017070000.jar", snapshot.entries().get(0).currentRelease());
        assertTrue(snapshot.baseline().ready());
        assertEquals("ESTABLISHED", snapshot.baseline().state());
        assertEquals(2, snapshot.baseline().comparableReleaseCount());
        assertEquals(60, snapshot.baseline().medianPostDeployMaxMillis());
        assertEquals(2, snapshot.baseline().stableReleaseCount());
        assertEquals(0, snapshot.baseline().watchReleaseCount());
    }

    @Test
    void marksLargePostDeployRegressionAsWatchWithoutTurningItIntoRollbackDecision() throws Exception {
        Path history = tempDir.resolve("watch-history");
        Files.createDirectories(history);
        writeV2(history, "20261017060000", "20261017050000", "d".repeat(40), 100, 1200);

        ObjectMapper mapper = new ObjectMapper();
        XingweiReleaseHistoryService service = new XingweiReleaseHistoryService(
                mapper,
                new XingweiReleaseAttestationService(mapper, tempDir.resolve("current.json").toString()),
                history.toString());

        XingweiReleaseHistoryService.Snapshot snapshot = service.snapshot();

        assertEquals(1, snapshot.entries().size());
        assertEquals("WATCH", snapshot.entries().get(0).state());
        assertEquals(1100, snapshot.entries().get(0).deltaMillis());
        assertFalse(snapshot.baseline().ready());
        assertEquals(1, snapshot.baseline().watchReleaseCount());
    }

    @Test
    void reportsMissingWithoutLeakingDirectoryWhenHistoryDoesNotExist() {
        ObjectMapper mapper = new ObjectMapper();
        XingweiReleaseHistoryService service = new XingweiReleaseHistoryService(
                mapper,
                new XingweiReleaseAttestationService(mapper, tempDir.resolve("current.json").toString()),
                tempDir.resolve("missing-history").toString());

        XingweiReleaseHistoryService.Snapshot snapshot = service.snapshot();

        assertFalse(snapshot.available());
        assertEquals("MISSING", snapshot.status());
        assertTrue(snapshot.entries().isEmpty());
        assertFalse(snapshot.baseline().ready());
    }

    private void writeV2(
            Path directory,
            String currentStamp,
            String previousStamp,
            String gitSha,
            int beforeMax,
            int afterMax) throws Exception {
        Files.writeString(
                directory.resolve("xinghuo-v2-standard-" + currentStamp + ".json"),
                v2Json(currentStamp, previousStamp, gitSha, beforeMax, afterMax));
    }

    private String v2Json(
            String currentStamp,
            String previousStamp,
            String gitSha,
            int beforeMax,
            int afterMax) {
        return """
                {
                  "schemaVersion": 2,
                  "source": "production-host-gateway",
                  "publishedAt": "2026-10-17T06:00:00Z",
                  "gitSha": "%s",
                  "jarSha256": "%s",
                  "currentRelease": "xinghuo-v2-standard-%s.jar",
                  "previousRelease": "xinghuo-v2-standard-%s.jar",
                  "mainPort": 8080,
                  "canaryPort": 8081,
                  "nginxUpstreamPort": 8080,
                  "canaryStopped": true,
                  "checks": {"predeploy18080": true, "canary8081": true, "production8080": true},
                  "healthComparison": {
                    "preDeploy8080": {"passed": true, "homeMillis": %d, "servicesMillis": %d},
                    "postDeploy8080": {"passed": true, "homeMillis": %d, "servicesMillis": %d}
                  }
                }
                """.formatted(
                gitSha,
                "e".repeat(64),
                currentStamp,
                previousStamp,
                Math.max(1, beforeMax - 5),
                beforeMax,
                Math.max(1, afterMax - 5),
                afterMax);
    }
}
