package com.xinghuo.observability;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.http.HttpClient;
import java.util.List;
import org.junit.jupiter.api.Test;

class XingweiReleaseChangeRiskServiceTest {
    private XingweiReleaseChangeRiskService service() {
        return new XingweiReleaseChangeRiskService(new ObjectMapper(), HttpClient.newHttpClient());
    }

    @Test
    void marksDatabaseSecurityPaymentAndDeploymentAsHigh() {
        XingweiReleaseChangeRiskService.RiskSummary summary = service().classifyPaths(List.of(
                "src/main/resources/db/migration/V99__example.sql",
                "src/main/java/com/xinghuo/security/Guard.java",
                "src/main/java/com/xinghuo/payment/Pay.java",
                "scripts/deploy-from-github.sh",
                "src/test/java/com/xinghuo/payment/PayTest.java"));

        assertEquals("HIGH", summary.level());
        assertEquals(5, summary.changedFileCount());
        assertTrue(summary.categories().contains("DATABASE"));
        assertTrue(summary.categories().contains("SECURITY"));
        assertTrue(summary.categories().contains("PAYMENT"));
        assertTrue(summary.categories().contains("DEPLOYMENT"));
        assertTrue(summary.categories().contains("TESTS"));
    }

    @Test
    void marksAdminBackendOrIntegrationAsMedium() {
        XingweiReleaseChangeRiskService.RiskSummary summary = service().classifyPaths(List.of(
                "src/main/java/com/xinghuo/observability/Release.java",
                "src/main/resources/static/assets/admin-xingwei-release-confirmation.js",
                "src/main/java/com/xinghuo/wecom/Directory.java"));

        assertEquals("MEDIUM", summary.level());
        assertTrue(summary.categories().contains("OBSERVABILITY"));
        assertTrue(summary.categories().contains("ADMIN_UI"));
        assertTrue(summary.categories().contains("WECOM"));
    }

    @Test
    void keepsDocsTestsAndFrontendNextLow() {
        XingweiReleaseChangeRiskService.RiskSummary summary = service().classifyPaths(List.of(
                "docs/development/example.md",
                "src/test/java/com/xinghuo/web/ExampleTest.java",
                "src/main/resources/static/next/shell.js"));

        assertEquals("LOW", summary.level());
        assertEquals(List.of("DOCS", "TESTS", "FRONTEND_NEXT"), summary.categories());
        assertEquals(XingweiReleaseChangeRiskService.RULES_VERSION, summary.rulesVersion());
    }

    @Test
    void publishesBoundedLookupAndCacheRules() {
        XingweiReleaseChangeRiskService.Rules rules = service().rules();

        assertEquals("v2-20261017", rules.version());
        assertEquals(1200L, rules.perPrLookupBudgetMillis());
        assertEquals(90_000L, rules.unknownCacheTtlMillis());
        assertTrue(rules.successCacheTtlMillis() > rules.unknownCacheTtlMillis());
        assertTrue(rules.lowOrUnknown().contains("超出预算"));
    }
}
