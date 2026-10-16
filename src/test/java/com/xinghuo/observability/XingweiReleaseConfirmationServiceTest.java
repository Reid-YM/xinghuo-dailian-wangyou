package com.xinghuo.observability;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class XingweiReleaseConfirmationServiceTest {
    @TempDir Path root;

    @Test
    void stableConfirmationAppendsAuditHistoryAndKeepsPullRequestAssociation() throws Exception {
        Path attestationFile = root.resolve("release-attestation.json");
        Path confirmationDir = Files.createDirectory(root.resolve("confirmations"));
        Files.writeString(attestationFile, trustedAttestationJson(525));

        XingweiReleaseAttestationService attestations = mock(XingweiReleaseAttestationService.class);
        XingweiReleaseStabilityService stability = mock(XingweiReleaseStabilityService.class);
        when(attestations.snapshot()).thenReturn(releaseSnapshot());
        when(stability.snapshot()).thenReturn(stabilitySnapshot("COMPLETE", "OK", "OK"));

        XingweiReleaseConfirmationService service = new XingweiReleaseConfirmationService(
                new ObjectMapper(), attestations, stability, attestationFile.toString(), confirmationDir.toString());

        XingweiReleaseConfirmationService.Snapshot first = service.confirm(
                new XingweiReleaseConfirmationService.Command(
                        "STABLE", "30 分钟窗口正常", currentRelease(), "a".repeat(40)),
                "admin");
        assertEquals("STABLE", first.status());
        assertEquals(525, first.release().pullRequestNumber());
        assertEquals(1, first.history().size());
        assertEquals("admin", first.latest().confirmedBy());
        assertEquals("COMPLETE", first.latest().window30State());

        XingweiReleaseConfirmationService.Snapshot second = service.confirm(
                new XingweiReleaseConfirmationService.Command(
                        "NEEDS_ATTENTION", "继续观察外部依赖", currentRelease(), "a".repeat(40)),
                "admin");
        assertEquals("NEEDS_ATTENTION", second.status());
        assertEquals(2, second.history().size());
        assertEquals("STABLE", second.history().get(0).decision());
        assertEquals("NEEDS_ATTENTION", second.history().get(1).decision());

        Path stored = confirmationDir.resolve("xinghuo-v2-standard-20261017070000.confirmation.json");
        assertTrue(Files.isRegularFile(stored));
        var json = new ObjectMapper().readTree(stored.toFile());
        assertEquals("admin-human-confirmation", json.path("source").asText());
        assertEquals(2, json.path("events").size());
        assertEquals(525, json.path("pullRequestNumber").asInt());
    }

    @Test
    void rejectsStalePageAndPrematureStableDecision() throws Exception {
        Path attestationFile = root.resolve("release-attestation.json");
        Path confirmationDir = Files.createDirectory(root.resolve("confirmations"));
        Files.writeString(attestationFile, trustedAttestationJson(525));

        XingweiReleaseAttestationService attestations = mock(XingweiReleaseAttestationService.class);
        XingweiReleaseStabilityService stability = mock(XingweiReleaseStabilityService.class);
        when(attestations.snapshot()).thenReturn(releaseSnapshot());
        when(stability.snapshot()).thenReturn(stabilitySnapshot("IN_PROGRESS", "OK", "OK"));

        XingweiReleaseConfirmationService service = new XingweiReleaseConfirmationService(
                new ObjectMapper(), attestations, stability, attestationFile.toString(), confirmationDir.toString());

        assertThrows(
                XingweiReleaseConfirmationService.Conflict.class,
                () -> service.confirm(
                        new XingweiReleaseConfirmationService.Command(
                                "STABLE", "", "xinghuo-v2-standard-20261017065959.jar", "a".repeat(40)),
                        "admin"));

        assertThrows(
                XingweiReleaseConfirmationService.Rejected.class,
                () -> service.confirm(
                        new XingweiReleaseConfirmationService.Command(
                                "STABLE", "", currentRelease(), "a".repeat(40)),
                        "admin"));

        assertThrows(
                XingweiReleaseConfirmationService.Rejected.class,
                () -> service.confirm(
                        new XingweiReleaseConfirmationService.Command(
                                "NEEDS_ATTENTION", "", currentRelease(), "a".repeat(40)),
                        "admin"));
    }

    @Test
    void snapshotFailsClosedForInvalidExistingConfirmationFile() throws Exception {
        Path attestationFile = root.resolve("release-attestation.json");
        Path confirmationDir = Files.createDirectory(root.resolve("confirmations"));
        Files.writeString(attestationFile, trustedAttestationJson(525));
        Files.writeString(
                confirmationDir.resolve("xinghuo-v2-standard-20261017070000.confirmation.json"),
                "{\"schemaVersion\":1,\"source\":\"forged\",\"events\":[]}");

        XingweiReleaseAttestationService attestations = mock(XingweiReleaseAttestationService.class);
        XingweiReleaseStabilityService stability = mock(XingweiReleaseStabilityService.class);
        when(attestations.snapshot()).thenReturn(releaseSnapshot());
        when(stability.snapshot()).thenReturn(stabilitySnapshot("COMPLETE", "OK", "OK"));

        XingweiReleaseConfirmationService service = new XingweiReleaseConfirmationService(
                new ObjectMapper(), attestations, stability, attestationFile.toString(), confirmationDir.toString());
        XingweiReleaseConfirmationService.Snapshot snapshot = service.snapshot();
        assertEquals("INVALID", snapshot.status());
        assertTrue(!snapshot.available());
        assertNotNull(snapshot.release());
    }

    private XingweiReleaseAttestationService.Snapshot releaseSnapshot() {
        Instant publishedAt = Instant.parse("2026-10-17T07:00:00Z");
        XingweiReleaseAttestationService.HealthComparison comparison =
                new XingweiReleaseAttestationService.HealthComparison(
                        new XingweiReleaseAttestationService.HealthProbe(true, 40, 44),
                        new XingweiReleaseAttestationService.HealthProbe(true, 38, 45));
        XingweiReleaseAttestationService.Attestation item =
                new XingweiReleaseAttestationService.Attestation(
                        2,
                        publishedAt,
                        "a".repeat(40),
                        "b".repeat(64),
                        currentRelease(),
                        "xinghuo-v2-standard-20261017063053.jar",
                        8080,
                        8081,
                        8080,
                        true,
                        new XingweiReleaseAttestationService.Checks(true, true, true),
                        comparison);
        return new XingweiReleaseAttestationService.Snapshot(true, "AVAILABLE", publishedAt.plusSeconds(1800), item);
    }

    private XingweiReleaseStabilityService.Snapshot stabilitySnapshot(
            String windowState,
            String currentSeverity,
            String windowSeverity) {
        Instant now = Instant.parse("2026-10-17T07:30:30Z");
        var window = new XingweiReleaseStabilityService.WindowObservation(
                30, windowState, "COMPLETE".equals(windowState) ? 30 : 12,
                200, 3, 0, 0.0, windowSeverity);
        var current = new XingweiReleaseStabilityService.CurrentObservation(
                true,
                "AVAILABLE",
                Instant.parse("2026-10-17T07:00:00Z"),
                Instant.parse("2026-10-17T06:59:00Z"),
                1830,
                "COVERED",
                currentSeverity,
                List.of(window));
        return new XingweiReleaseStabilityService.Snapshot(
                now,
                current,
                true,
                "AVAILABLE",
                XingweiReleaseHistoryService.Baseline.empty(),
                List.of(),
                60,
                60);
    }

    private String currentRelease() {
        return "xinghuo-v2-standard-20261017070000.jar";
    }

    private String trustedAttestationJson(int pullRequestNumber) {
        return """
                {
                  "schemaVersion":2,
                  "source":"production-host-gateway",
                  "publishedAt":"2026-10-17T07:00:00Z",
                  "gitSha":"%s",
                  "pullRequestNumber":%d,
                  "jarSha256":"%s",
                  "currentRelease":"%s",
                  "previousRelease":"xinghuo-v2-standard-20261017063053.jar",
                  "mainPort":8080,
                  "canaryPort":8081,
                  "nginxUpstreamPort":8080,
                  "canaryStopped":true,
                  "checks":{"predeploy18080":true,"canary8081":true,"production8080":true},
                  "healthComparison":{
                    "preDeploy8080":{"passed":true,"homeMillis":40,"servicesMillis":44},
                    "postDeploy8080":{"passed":true,"homeMillis":38,"servicesMillis":45}
                  }
                }
                """.formatted("a".repeat(40), pullRequestNumber, "b".repeat(64), currentRelease());
    }
}
