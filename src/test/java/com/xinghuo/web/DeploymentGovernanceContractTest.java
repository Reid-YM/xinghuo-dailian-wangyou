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
    void productionStillRequiresMergedPrAndSuccessfulMainCi() throws Exception {
        String workflow = Files.readString(ROOT.resolve(".github/workflows/deploy-production.yml"));
        assertTrue(workflow.contains("Require merged Pull Request origin"));
        assertTrue(workflow.contains(".merge_commit_sha == $sha"));
        assertTrue(workflow.contains("Require successful main CI"));
        assertTrue(workflow.contains(".conclusion == \"success\""));
        assertTrue(workflow.contains("workflow_dispatch:"));
        assertTrue(workflow.contains("confirm:"));
    }

    @Test
    void governanceDocumentationExplainsPlatformProtectionBoundary() throws Exception {
        String doc = Files.readString(ROOT.resolve("docs/PRODUCTION-DEPLOYMENT-GOVERNANCE.md"));
        assertTrue(doc.contains("不需要为了每次发布 force reset"));
        assertTrue(doc.contains("Administration 权限"));
        assertTrue(doc.contains("Chromium UI Regression"));
    }
}
