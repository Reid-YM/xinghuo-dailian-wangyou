package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class DeploymentGovernanceContractTest {
    private static final Path ROOT = Path.of(".");

    @Test
    void productionRequestsCanAdvanceLinearlyWithoutPerReleaseForceReset() throws Exception {
        String workflow = Files.readString(ROOT.resolve(".github/workflows/deploy-production.yml"));
        assertTrue(workflow.contains("request_parent=$(git rev-parse \"${GITHUB_SHA}^\")"));
        assertTrue(workflow.contains("git diff --name-only \"${request_parent}\" \"${GITHUB_SHA}\""));
        assertTrue(workflow.contains("deploy_sha=$(sed -n 's/^deploy_sha=//p' .deploy-request"));
        assertTrue(workflow.contains("[ \"${deploy_sha}\" = \"${main_sha}\" ]"));
        assertFalse(workflow.contains("deploy request must be created directly on current main"));
    }

    @Test
    void productionStillRequiresMergedPrAndSuccessfulManualMainCi() throws Exception {
        String workflow = Files.readString(ROOT.resolve(".github/workflows/deploy-production.yml"));
        assertTrue(workflow.contains("Require merged Pull Request origin"));
        assertTrue(workflow.contains(".merge_commit_sha == $sha"));
        assertTrue(workflow.contains("Require successful manual main CI"));
        assertTrue(workflow.contains("head_sha=${DEPLOY_SHA}&branch=main&event=workflow_dispatch&per_page=10"));
        assertTrue(workflow.contains(".conclusion == \"success\""));
        assertFalse(workflow.contains("branch=main&event=push&per_page=10"));
        assertTrue(workflow.contains("workflow_dispatch:"));
        assertTrue(workflow.contains("confirm:"));
    }

    @Test
    void ciRemainsManualOnlyUntilOperationsExplicitlyChangesPolicy() throws Exception {
        String ci = Files.readString(ROOT.resolve(".github/workflows/ci.yml"));
        String triggerBlock = ci.substring(0, ci.indexOf("permissions:"));

        assertTrue(triggerBlock.contains("workflow_dispatch:"));
        assertFalse(triggerBlock.contains("\n  push:"));
        assertFalse(triggerBlock.contains("\n  pull_request:"));
    }

    @Test
    void governanceDocumentationExplainsPlatformProtectionBoundary() throws Exception {
        String doc = Files.readString(ROOT.resolve("docs/PRODUCTION-DEPLOYMENT-GOVERNANCE.md"));
        assertTrue(doc.contains("不需要为了每次发布 force reset"));
        assertTrue(doc.contains("Administration 权限"));
        assertTrue(doc.contains("Chromium + WebKit UI Regression"));
        assertTrue(doc.contains("workflow_dispatch"));
        assertTrue(doc.contains("同一个 main SHA"));
    }

    @Test
    void manualValidationRunbookLocksBrowserAndThreePortEvidenceBeforeCssRetirement() throws Exception {
        String runbook = Files.readString(ROOT.resolve("docs/MANUAL-REFACTOR-VALIDATION.md"));

        assertTrue(runbook.contains("Java 21 / Maven Build"));
        assertTrue(runbook.contains("Chromium + WebKit UI Regression"));
        assertTrue(runbook.contains("LegacyBridgePlaywrightTest"));
        assertTrue(runbook.contains("18080"));
        assertTrue(runbook.contains("8081"));
        assertTrue(runbook.contains("8080"));
        assertTrue(runbook.contains("vnext-legacy-bridge-v3.css"));
        assertTrue(runbook.contains("vnext-account-submit-v3.css"));
        assertTrue(runbook.contains("deploy/production"));
        assertTrue(runbook.contains("workflow 定义未同步"));
    }
}
