package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class VNextPreviewContractTest {
    private static final Path ROOT = Path.of("");

    @Test
    void approvedVnextBuildOwnsCoreProductionShellsWithoutChangingServerRoutes() throws Exception {
        String pom = Files.readString(ROOT.resolve("pom.xml"));
        String vite = Files.readString(ROOT.resolve("frontend-vnext/vite.config.ts"));
        String controller = Files.readString(ROOT.resolve("src/main/java/com/xinghuo/controller/VNextPreviewController.java"));
        String security = Files.readString(ROOT.resolve("src/main/java/com/xinghuo/security/SecurityConfig.java"));

        assertTrue(pom.contains("frontend-maven-plugin"));
        assertTrue(pom.contains("frontend-vnext"));
        assertTrue(vite.contains("base: '/vnext/'"));
        assertTrue(vite.contains("../target/classes/static/vnext"));
        assertTrue(vite.contains("entryFileNames: 'assets/app.js'"));
        assertTrue(vite.contains("'assets/app.css'"));
        assertTrue(controller.contains("forward:/vnext/index.html"));
        assertTrue(security.contains("\"/vnext\", \"/vnext/**\""));
        assertFalse(controller.contains("@GetMapping(\"/\")"),
                "Production route ownership must remain in PageController so SEO/auth behavior is preserved");

        for (String name : List.of("index.html", "services.html", "quote.html", "records.html", "account.html")) {
            String html = Files.readString(ROOT.resolve("src/main/resources/templates/" + name));
            assertTrue(html.contains("id=\"root\""), name + " must expose the React mount point");
            assertTrue(html.contains("/vnext/assets/app.css"), name + " must load the VNext production CSS");
            assertTrue(html.contains("/vnext/assets/app.js"), name + " must load the VNext production JS");
            assertTrue(html.contains("/assets/vnext-formal-ui.css"), name + " must load the approved theme bridge");
            assertFalse(html.contains("/assets/public.js"), name + " must not double-bind the legacy public runtime");
        }
    }

    @Test
    void responsiveWechatAndRealQuoteContractsAreExplicit() throws Exception {
        String css = Files.readString(ROOT.resolve("frontend-vnext/src/styles.css"));
        String environment = Files.readString(ROOT.resolve("frontend-vnext/src/lib/environment.ts"));
        String quotePage = Files.readString(ROOT.resolve("frontend-vnext/src/pages/QuotePage.tsx"));
        String quoteFlow = Files.readString(ROOT.resolve("frontend-vnext/src/features/quote/useQuoteFlow.ts"));
        String api = Files.readString(ROOT.resolve("frontend-vnext/src/lib/api.ts"));

        assertTrue(css.contains("env(safe-area-inset-top"));
        assertTrue(css.contains("env(safe-area-inset-bottom"));
        assertTrue(css.contains("@media (max-width: 479px)"));
        assertTrue(css.contains("@media (max-width: 767px)"));
        assertTrue(css.contains("@media (max-width: 1023px)"));
        assertTrue(css.contains("@media (max-width: 1279px)"));
        assertTrue(css.contains("@media (min-width: 1920px)"));
        assertTrue(css.contains("font-size: 16px !important"));
        assertTrue(css.contains("@media (prefers-reduced-motion: reduce)"));
        assertTrue(css.contains("@media (forced-colors: active)"));
        assertTrue(environment.contains("MicroMessenger"));
        assertTrue(environment.contains("visualViewport"));
        assertTrue(api.contains("'/api/services'"));
        assertTrue(api.contains("'/api/quote'"));
        assertTrue(api.contains("'Idempotency-Key': idempotencyKey"));
        assertTrue(api.contains("'/api/uploads/quote'"));
        assertTrue(quoteFlow.contains("privacyConsent: true"));
        assertTrue(quoteFlow.contains("loadAccountMe"));
        assertTrue(quotePage.contains("/record/${encodeURIComponent(success.businessNo)}"));
    }

    @Test
    void legacyDeepBusinessPagesAndAdminFunctionalityRemainAvailable() throws Exception {
        String pageController = Files.readString(ROOT.resolve("src/main/java/com/xinghuo/controller/PageController.java"));
        String admin = Files.readString(ROOT.resolve("src/main/resources/templates/admin.html"));
        String adminThemeLoader = Files.readString(ROOT.resolve("src/main/resources/static/assets/admin-image-editor.js"));

        assertTrue(pageController.contains("return \"service-detail\""));
        assertTrue(pageController.contains("return \"record-detail\""));
        assertTrue(pageController.contains("return \"admin\""));
        assertTrue(admin.contains("data-nav=\"quotes\""));
        assertTrue(admin.contains("data-nav=\"orders\""));
        assertTrue(adminThemeLoader.contains("vnext-formal-ui.css"));
    }
}
