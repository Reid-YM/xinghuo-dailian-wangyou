package com.xinghuo.web;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VNextLegacyUiBridgeContractTest {
    private static final Path ROOT = Path.of(System.getProperty("user.dir"));

    private static String read(String relative) throws IOException {
        return Files.readString(ROOT.resolve(relative));
    }

    @Test
    void deepPageBridgeCoversPublicAccountAndAdminSurfaces() throws IOException {
        String css = read("src/main/resources/static/assets/vnext-legacy-bridge-v3.css");
        String submissionCss = read("src/main/resources/static/assets/vnext-account-submit-v3.css");

        assertTrue(css.contains("body[data-page=\"service-detail\"]"));
        assertTrue(css.contains("body[data-page=\"record-detail\"]"));
        assertTrue(css.contains("body.account-center-page"));
        assertTrue(css.contains("body.admin-solid-ui"));
        assertTrue(css.contains("body.frontend-control-admin"));
        assertTrue(css.contains("@media(min-width:1920px)"));
        assertTrue(css.contains("@media(min-width:2400px)"));
        assertTrue(css.contains("env(safe-area-inset-bottom"));
        assertTrue(css.contains("prefers-reduced-motion"));

        assertTrue(submissionCss.contains("body[data-page=\"account-submit\"]"));
        assertTrue(submissionCss.contains(".nexus-commandbar"));
        assertTrue(submissionCss.contains(".submission-editor-layout"));
        assertTrue(submissionCss.contains("@media(min-width:1920px)"));
        assertTrue(submissionCss.contains("env(safe-area-inset-bottom"));
    }

    @Test
    void legacyPublicAndAccountPagesLoadTheBridgeThroughRuntimeLoader() throws IOException {
        String sharedCss = read("src/main/resources/static/assets/site-image-controls.css");
        String sharedRuntime = read("src/main/resources/static/assets/site-image-controls.js");

        assertFalse(sharedCss.contains("@import url(\"/assets/vnext-legacy-bridge-v3.css"));
        assertFalse(sharedCss.contains("@import url(\"/assets/vnext-account-submit-v3.css"));
        assertTrue(sharedRuntime.contains("page === 'service-detail'"));
        assertTrue(sharedRuntime.contains("page === 'record-detail'"));
        assertTrue(sharedRuntime.contains("body?.classList.contains('account-center-page')"));
        assertTrue(sharedRuntime.contains("/assets/vnext-legacy-bridge-v3.css?v=20261010ui03"));
        assertTrue(sharedRuntime.contains("/assets/vnext-account-submit-v3.css?v=20261010ui03"));

        for (String name : List.of(
                "service-detail.html",
                "record-detail.html",
                "account-orders.html",
                "account-progress.html",
                "account-requests.html",
                "account-settings.html",
                "account-submit.html")) {
            String html = read("src/main/resources/templates/" + name);
            assertTrue(html.contains("/assets/site-image-controls.js"), name + " must keep the runtime bridge loader");
            assertTrue(html.contains("/assets/site-image-controls.css"), name + " must keep image control styling");
        }
    }

    @Test
    void adminEntrypointsUseSingleBridgePathWithoutChangingBusinessApis() throws IOException {
        String editor = read("src/main/resources/static/assets/admin-image-editor.js");
        String controls = read("src/main/resources/static/assets/admin-frontend-controls.js");
        String brandFixes = read("src/main/resources/static/assets/admin-brand-layout-fixes.css");
        String guideAdmin = read("src/main/resources/templates/admin-guides.html");

        assertFalse(editor.contains("/assets/vnext-legacy-bridge-v3.css?v=20261010ui03"));
        assertTrue(editor.contains("/assets/vnext-formal-ui.css?v=20261010ui03"));
        assertTrue(editor.contains("/assets/admin-brand-layout-fixes.css?v=20261008b002"));
        assertTrue(editor.contains("data-admin-brand-layout-fixes"));
        assertFalse(controls.contains("/assets/vnext-legacy-bridge-v3.css?v=20261010ui03"));
        assertFalse(controls.contains("ensureVNextBridge"));
        assertTrue(controls.contains("/assets/admin-brand-layout-fixes.css?v=20261008b002"));
        assertTrue(brandFixes.startsWith("@import url(\"/assets/vnext-legacy-bridge-v3.css?v=20261010ui03\");"));
        assertTrue(guideAdmin.contains("/assets/admin-brand-layout-fixes.css"));

        assertTrue(controls.contains("api('/api/admin/global-ui-settings')"));
        assertTrue(controls.contains("api('/api/admin/frontend-controls')"));
        assertTrue(controls.contains("method: 'PATCH'"));
    }
}
