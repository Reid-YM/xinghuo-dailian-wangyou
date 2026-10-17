package com.xinghuo.observability;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

class XingweiReleaseReviewQueueServiceTest {
    @TempDir Path tempDir;

    @Test
    void keepsOnlyCurrentReleaseActionableAndSummarizesHistoricalConfirmation() throws Exception {
        Path attestationFile = tempDir.resolve("release-attestation.json");
        Path historyDir = tempDir.resolve("history");
        Path confirmationDir = tempDir.resolve("confirmations");
        Files.createDirectories(historyDir);
        Files.createDirectories(confirmationDir);

        String currentRelease = "xinghuo-v2-standard-20261017080000.jar";
        String previousRelease = "xinghuo-v2-standard-20261017070000.jar";
        String currentSha = "a".repeat(40);
        String previousSha = "b".repeat(40);
        Files.writeString(attestationFile, attestationJson(currentRelease, previousRelease, currentSha, 526, "2026-10-17T08:00:00Z"));
        Files.writeString(historyDir.resolve("xinghuo-v2-standard-20261017080000.json"),
                attestationJson(currentRelease, previousRelease, currentSha, 526, "2026-10-17T08:00:00Z"));
        Files.writeString(historyDir.resolve("xinghuo-v2-standard-20261017070000.json"),
                attestationJson(previousRelease, "xinghuo-v2-standard-20261017060000.jar", previousSha, 525, "2026-10-17T07:00:00Z"));
        Files.writeString(confirmationDir.resolve("xinghuo-v2-standard-20261017070000.confirmation.json"), """
                {
                  "schemaVersion":1,
                  "source":"admin-human-confirmation",
                  "release":"xinghuo-v2-standard-20261017070000.jar",
                  "gitSha":"%s",
                  "events":[{
                    "decision":"STABLE",
                    "note":"观察正常",
                    "confirmedBy":"admin",
                    "confirmedAt":"2026-10-17T07:35:00Z"
                  }]
                }
                """.formatted(previousSha));

        ObjectMapper mapper = new ObjectMapper();
        XingweiReleaseAttestationService attestationService =
                new XingweiReleaseAttestationService(mapper, attestationFile.toString());
        XingweiReleaseConfirmationService confirmationService = mock(XingweiReleaseConfirmationService.class);
        when(confirmationService.snapshot()).thenReturn(new XingweiReleaseConfirmationService.Snapshot(
                true,
                "PENDING",
                Instant.parse("2026-10-17T08:31:00Z"),
                new XingweiReleaseConfirmationService.ReleaseReference(
                        currentRelease, currentSha, 526, Instant.parse("2026-10-17T08:00:00Z")),
                true,
                "ELIGIBLE",
                null,
                List.of()));
        XingweiReleaseChangeRiskService riskService = mock(XingweiReleaseChangeRiskService.class);
        when(riskService.rules()).thenReturn(new XingweiReleaseChangeRiskService.Rules(
                XingweiReleaseChangeRiskService.RULES_VERSION,
                List.of("DATABASE"),
                List.of("ADMIN_UI"),
                "low-or-unknown"));
        when(riskService.riskFor(526)).thenReturn(new XingweiReleaseChangeRiskService.RiskSummary(
                true, true, "MEDIUM", 8, List.of("OBSERVABILITY", "ADMIN_UI"),
                XingweiReleaseChangeRiskService.RULES_VERSION, "GITHUB_PR_FILES"));
        when(riskService.riskFor(525)).thenReturn(new XingweiReleaseChangeRiskService.RiskSummary(
                true, true, "LOW", 3, List.of("TESTS"),
                XingweiReleaseChangeRiskService.RULES_VERSION, "GITHUB_PR_FILES"));

        XingweiReleaseReviewQueueService service = new XingweiReleaseReviewQueueService(
                mapper,
                attestationService,
                confirmationService,
                riskService,
                attestationFile.toString(),
                historyDir.toString(),
                confirmationDir.toString());

        XingweiReleaseReviewQueueService.Snapshot snapshot = service.snapshot();

        assertTrue(snapshot.available());
        assertEquals(2, snapshot.releases().size());
        assertEquals(1, snapshot.summary().stableCount());
        assertEquals(1, snapshot.summary().unconfirmedCount());
        assertEquals(1, snapshot.summary().actionableNowCount());
        assertTrue(snapshot.summary().currentNeedsAction());
        assertEquals("READY", snapshot.releases().get(0).actionState());
        assertEquals("REVIEW_READY", snapshot.releases().get(0).reviewPriority());
        assertTrue(snapshot.releases().get(0).priorityReason().contains("自动观察完整正常"));
        assertTrue(snapshot.releases().get(0).current());
        assertEquals("MEDIUM", snapshot.releases().get(0).risk().level());
        assertEquals("COMPLETE", snapshot.releases().get(1).actionState());
        assertEquals("ARCHIVED_COMPLETE", snapshot.releases().get(1).reviewPriority());
        assertFalse(snapshot.releases().get(1).current());
        assertEquals("STABLE", snapshot.releases().get(1).confirmationStatus());
        assertEquals(2, snapshot.riskRuntime().lookupCount());
        assertEquals(0, snapshot.riskRuntime().unknownCount());
        assertEquals(2400L, snapshot.riskRuntime().externalBudgetMillis());
        assertEquals(1200L, snapshot.riskRuntime().perLookupBudgetMillis());
    }

    @Test
    void marksRetiredReleaseWithoutConfirmationAsArchivedUnconfirmed() throws Exception {
        Path attestationFile = tempDir.resolve("current2.json");
        Path historyDir = tempDir.resolve("history2");
        Path confirmationDir = tempDir.resolve("confirmations2");
        Files.createDirectories(historyDir);
        Files.createDirectories(confirmationDir);
        String currentRelease = "xinghuo-v2-standard-20261017090000.jar";
        String oldRelease = "xinghuo-v2-standard-20261017080000.jar";
        String currentSha = "c".repeat(40);
        String oldSha = "d".repeat(40);
        Files.writeString(attestationFile, attestationJson(currentRelease, oldRelease, currentSha, 527, "2026-10-17T09:00:00Z"));
        Files.writeString(historyDir.resolve("xinghuo-v2-standard-20261017080000.json"),
                attestationJson(oldRelease, "xinghuo-v2-standard-20261017070000.jar", oldSha, 526, "2026-10-17T08:00:00Z"));

        ObjectMapper mapper = new ObjectMapper();
        XingweiReleaseConfirmationService confirmationService = mock(XingweiReleaseConfirmationService.class);
        when(confirmationService.snapshot()).thenReturn(new XingweiReleaseConfirmationService.Snapshot(
                true, "STABLE", Instant.now(),
                new XingweiReleaseConfirmationService.ReleaseReference(currentRelease, currentSha, 527, Instant.parse("2026-10-17T09:00:00Z")),
                true, "ELIGIBLE",
                new XingweiReleaseConfirmationService.Event("STABLE", "", "admin", Instant.now(), "OK", "COVERED", "COMPLETE", 0, 20),
                List.of()));
        XingweiReleaseChangeRiskService riskService = mock(XingweiReleaseChangeRiskService.class);
        when(riskService.rules()).thenReturn(new XingweiReleaseChangeRiskService.Rules("v1", List.of(), List.of(), "rule"));
        when(riskService.riskFor(org.mockito.ArgumentMatchers.any())).thenReturn(
                new XingweiReleaseChangeRiskService.RiskSummary(false, false, "UNKNOWN", 0, List.of(), "v1", "TEST"));

        XingweiReleaseReviewQueueService service = new XingweiReleaseReviewQueueService(
                mapper,
                new XingweiReleaseAttestationService(mapper, attestationFile.toString()),
                confirmationService,
                riskService,
                attestationFile.toString(), historyDir.toString(), confirmationDir.toString());

        XingweiReleaseReviewQueueService.Snapshot snapshot = service.snapshot();
        XingweiReleaseReviewQueueService.ReleaseReview old = snapshot.releases().stream()
                .filter(item -> oldRelease.equals(item.release()))
                .findFirst().orElseThrow();
        assertEquals("UNCONFIRMED", old.confirmationStatus());
        assertEquals("ARCHIVED_UNCONFIRMED", old.actionState());
        assertEquals("ARCHIVED_GAP", old.reviewPriority());
        assertTrue(old.priorityReason().contains("不可补写"));
        assertEquals(1, snapshot.summary().archivedGapCount());
        assertFalse(old.current());
    }

    @Test
    void elevatesReadyCurrentReleaseWhenRiskIsUnknown() throws Exception {
        Path attestationFile = tempDir.resolve("current3.json");
        Path historyDir = tempDir.resolve("history3");
        Path confirmationDir = tempDir.resolve("confirmations3");
        Files.createDirectories(historyDir);
        Files.createDirectories(confirmationDir);
        String currentRelease = "xinghuo-v2-standard-20261017100000.jar";
        String currentSha = "f".repeat(40);
        Files.writeString(attestationFile,
                attestationJson(currentRelease, "xinghuo-v2-standard-20261017090000.jar", currentSha, 528, "2026-10-17T10:00:00Z"));

        ObjectMapper mapper = new ObjectMapper();
        XingweiReleaseConfirmationService confirmationService = mock(XingweiReleaseConfirmationService.class);
        when(confirmationService.snapshot()).thenReturn(new XingweiReleaseConfirmationService.Snapshot(
                true, "PENDING", Instant.now(),
                new XingweiReleaseConfirmationService.ReleaseReference(currentRelease, currentSha, 528, Instant.parse("2026-10-17T10:00:00Z")),
                true, "ELIGIBLE", null, List.of()));
        XingweiReleaseChangeRiskService riskService = mock(XingweiReleaseChangeRiskService.class);
        when(riskService.rules()).thenReturn(new XingweiReleaseChangeRiskService.Rules("v2", List.of(), List.of(), "rule"));
        when(riskService.riskFor(528)).thenReturn(
                new XingweiReleaseChangeRiskService.RiskSummary(false, false, "UNKNOWN", 0, List.of(), "v2", "LOOKUP_BUDGET_EXHAUSTED"));

        XingweiReleaseReviewQueueService service = new XingweiReleaseReviewQueueService(
                mapper,
                new XingweiReleaseAttestationService(mapper, attestationFile.toString()),
                confirmationService,
                riskService,
                attestationFile.toString(), historyDir.toString(), confirmationDir.toString());

        XingweiReleaseReviewQueueService.ReleaseReview current = service.snapshot().releases().get(0);
        assertEquals("READY", current.actionState());
        assertEquals("REVIEW_NOW", current.reviewPriority());
        assertTrue(current.priorityReason().contains("风险信息不完整"));
    }

    private String attestationJson(String current, String previous, String gitSha, int pr, String publishedAt) {
        return """
                {
                  "schemaVersion":2,
                  "source":"production-host-gateway",
                  "publishedAt":"%s",
                  "gitSha":"%s",
                  "pullRequestNumber":%d,
                  "jarSha256":"%s",
                  "currentRelease":"%s",
                  "previousRelease":"%s",
                  "mainPort":8080,
                  "canaryPort":8081,
                  "nginxUpstreamPort":8080,
                  "canaryStopped":true,
                  "checks":{"predeploy18080":true,"canary8081":true,"production8080":true},
                  "healthComparison":{
                    "preDeploy8080":{"passed":true,"homeMillis":40,"servicesMillis":45},
                    "postDeploy8080":{"passed":true,"homeMillis":42,"servicesMillis":48}
                  }
                }
                """.formatted(publishedAt, gitSha, pr, "e".repeat(64), current, previous);
    }
}
