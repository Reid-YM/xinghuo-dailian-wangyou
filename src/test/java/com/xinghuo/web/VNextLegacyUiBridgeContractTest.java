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
    void sharedBridgeCoversRemainingSurfacesWhileFeatureStylesOwnRetiredSurfaces() throws IOException {
        String css = read("src/main/resources/static/assets/vnext-legacy-bridge-v3.css");
        String serviceCss = read("src/main/resources/static/assets/service-detail.css");
        String submissionCss = read("src/main/resources/static/assets/account-submit.css");
        String accountCenterRefine = read("src/main/resources/static/assets/account-center-refine.css");

        assertFalse(css.contains("body[data-page=\"service-detail\"]"));
        assertTrue(css.contains("body[data-page=\"record-detail\"]"));
        assertFalse(css.contains("body.account-center-page"));
        assertTrue(css.contains("body.admin-solid-ui"));
        assertTrue(css.contains("body.frontend-control-admin"));
        assertTrue(css.contains("@media(min-width:1920px)"));
        assertTrue(css.contains("@media(min-width:2400px)"));
        assertTrue(css.contains("env(safe-area-inset-bottom"));
        assertTrue(css.contains("prefers-reduced-motion"));

        assertTrue(serviceCss.contains("body[data-page=\"service-detail\"]"));
        assertTrue(serviceCss.contains(".contact-cta"));
        assertTrue(serviceCss.contains("@media(min-width:1920px)"));
        assertTrue(serviceCss.contains("env(safe-area-inset-bottom"));

        assertTrue(submissionCss.contains("body[data-page=\"account-submit\"]"));
        assertTrue(submissionCss.contains(".nexus-commandbar"));
        assertTrue(submissionCss.contains(".submission-editor-layout"));
        assertTrue(submissionCss.contains("@media(min-width:1920px)"));
        assertTrue(submissionCss.contains("env(safe-area-inset-bottom"));

        assertTrue(accountCenterRefine.contains("body.account-center-page"));
        assertTrue(accountCenterRefine.contains(".account-center-shell"));
        assertTrue(accountCenterRefine.contains("@media(min-width:1920px)"));
        assertTrue(accountCenterRefine.contains("html.is-ios body.account-center-page"));
        assertTrue(accountCenterRefine.contains("env(safe-area-inset-bottom"));
    }

    @Test
    void publicAndAccountPagesOwnStylesDeclaratively() throws IOException {
        String sharedCss = read("src/main/resources/static/assets/site-image-controls.css");
        String imageRuntime = read("src/main/resources/static/assets/site-image-controls.js");
        String bridgeFragment = read("src/main/resources/templates/fragments/vnext-legacy.html");
        String accountCenterFragment = read("src/main/resources/templates/fragments/account-center.html");
        String serviceDesktop = read("src/main/resources/templates/service-detail.html");
        String serviceMobile = read("src/main/resources/templates/mobile/service-detail.html");
        String recordDesktop = read("src/main/resources/templates/record-detail.html");
        String recordMobile = read("src/main/resources/templates/mobile/record-detail.html");
        String orderDesktop = read("src/main/resources/templates/order-detail.html");
        String orderMobile = read("src/main/resources/templates/mobile/order-detail.html");
        String submitDesktop = read("src/main/resources/templates/account-submit.html");
        String submitMobile = read("src/main/resources/templates/mobile/account-submit.html");

        assertFalse(sharedCss.contains("@import url(\"/assets/vnext-legacy-bridge-v3.css"));
        assertFalse(sharedCss.contains("@import url(\"/assets/service-detail.css"));
        assertFalse(sharedCss.contains("@import url(\"/assets/account-submit.css"));
        assertFalse(sharedCss.contains("@import url(\"/assets/vnext-account-submit-v3.css"));
        assertTrue(bridgeFragment.contains("th:fragment=\"deepPageBridge\""));
        assertTrue(bridgeFragment.contains("th:fragment=\"accountSubmitBridge\""));
        assertTrue(bridgeFragment.contains("/assets/vnext-legacy-bridge-v3.css?v=20261010ui03"));
        assertTrue(bridgeFragment.contains("data-vnext-legacy-bridge=\"1\""));
        assertTrue(bridgeFragment.contains("/assets/account-submit.css?v=20261011refactor01"));
        assertTrue(bridgeFragment.contains("data-account-submit-style=\"1\""));
        assertFalse(bridgeFragment.contains("vnext-account-submit-v3.css"));
        assertTrue(accountCenterFragment.contains("th:fragment=\"bar\""));
        assertFalse(accountCenterFragment.contains("fragments/vnext-legacy :: deepPageBridge"));

        assertTrue(serviceDesktop.contains("/assets/service-detail.css?v=20261011refactor02"));
        assertTrue(serviceMobile.contains("/assets/service-detail.css?v=20261011refactor02"));
        assertFalse(serviceDesktop.contains("fragments/vnext-legacy :: deepPageBridge"));
        assertFalse(serviceMobile.contains("fragments/vnext-legacy :: deepPageBridge"));
        assertTrue(recordDesktop.indexOf("fragments/site :: header") < recordDesktop.indexOf("fragments/vnext-legacy :: deepPageBridge"));
        assertTrue(recordMobile.indexOf("fragments/site :: header") < recordMobile.indexOf("fragments/vnext-legacy :: deepPageBridge"));
        assertTrue(orderDesktop.indexOf("</header>") < orderDesktop.indexOf("fragments/vnext-legacy :: deepPageBridge"));
        assertTrue(orderMobile.indexOf("</header>") < orderMobile.indexOf("fragments/vnext-legacy :: deepPageBridge"));
        assertTrue(submitDesktop.indexOf("fragments/site :: header") < submitDesktop.indexOf("fragments/vnext-legacy :: accountSubmitBridge"));
        assertTrue(submitMobile.indexOf("fragments/site :: header") < submitMobile.indexOf("fragments/vnext-legacy :: accountSubmitBridge"));

        for (String name : List.of(
                "account-orders.html",
                "account-progress.html",
                "account-requests.html",
                "account-settings.html")) {
            String desktop = read("src/main/resources/templates/" + name);
            String mobile = read("src/main/resources/templates/mobile/" + name);
            assertTrue(desktop.contains("fragments/account-center :: bar"));
            assertTrue(mobile.contains("fragments/account-center :: bar"));
            assertTrue(desktop.contains("/assets/account-center.css"));
            assertTrue(desktop.contains("/assets/account-center-refine.css"));
            assertTrue(mobile.contains("/assets/account-center.css"));
            assertTrue(mobile.contains("/assets/account-center-refine.css"));
        }
        String mobileAccount = read("src/main/resources/templates/mobile/account.html");
        assertTrue(mobileAccount.contains("fragments/account-center :: bar"));
        assertTrue(mobileAccount.contains("/assets/account-center.css"));
        assertTrue(mobileAccount.contains("/assets/account-center-refine.css"));

        assertFalse(imageRuntime.contains("ensureStylesheet"));
        assertFalse(imageRuntime.contains("vnext-legacy-bridge-v3.css"));
        assertFalse(imageRuntime.contains("vnext-account-submit-v3.css"));
        assertFalse(imageRuntime.contains("account-submit.css"));
        assertFalse(imageRuntime.contains("service-detail.css"));
        assertFalse(imageRuntime.contains("data-vnext-legacy-bridge"));
        assertFalse(imageRuntime.contains("data-account-submit-style"));
        assertTrue(imageRuntime.contains("window.SiteImageControls = { resolveBackground, resolveMain, background, main, serviceCoverVars }"));

        for (String name : List.of(
                "service-detail.html",
                "record-detail.html",
                "order-detail.html",
                "account-orders.html",
                "account-progress.html",
                "account-requests.html",
                "account-settings.html",
                "account-submit.html")) {
            String html = read("src/main/resources/templates/" + name);
            assertTrue(html.contains("/assets/site-image-controls.js"), name + " still needs the image layout runtime");
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
