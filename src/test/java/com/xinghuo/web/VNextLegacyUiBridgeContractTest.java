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
    void sharedBridgeIsRetiredAndCanonicalStylesOwnEveryFormerSurface() throws IOException {
        Path retiredBridge = ROOT.resolve("src/main/resources/static/assets/vnext-legacy-bridge-v3.css");
        String serviceCss = read("src/main/resources/static/assets/service-detail.css");
        String recordCss = read("src/main/resources/static/assets/record-detail.css");
        String submissionCss = read("src/main/resources/static/assets/account-submit.css");
        String accountCenterRefine = read("src/main/resources/static/assets/account-center-refine.css");
        String frontendControlCss = read("src/main/resources/static/assets/admin-frontend-controls.css");
        String adminControlCss = read("src/main/resources/static/assets/admin-control-center-v2.css");
        String adminLightCss = read("src/main/resources/static/assets/admin-light-theme.css");
        String guideCss = read("src/main/resources/static/assets/admin-guides-workspace.css");

        assertFalse(Files.exists(retiredBridge));

        assertTrue(serviceCss.contains("body[data-page=\"service-detail\"]"));
        assertTrue(serviceCss.contains(".contact-cta"));
        assertTrue(serviceCss.contains("@media(min-width:1920px)"));
        assertTrue(serviceCss.contains("env(safe-area-inset-bottom"));

        assertTrue(recordCss.contains("body[data-page=\"record-detail\"]"));
        assertTrue(recordCss.contains(".record-meta-grid"));
        assertTrue(recordCss.contains("@media(min-width:1920px)"));
        assertTrue(recordCss.contains("env(safe-area-inset-bottom"));

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

        assertTrue(frontendControlCss.contains(".frontend-control-admin"));
        assertTrue(frontendControlCss.contains("--v3-bg:#0b0c0f"));
        assertTrue(frontendControlCss.contains("@media(min-width:1920px)"));
        assertTrue(frontendControlCss.contains("@media(min-width:2400px)"));
        assertTrue(frontendControlCss.contains("env(safe-area-inset-bottom"));
        assertTrue(frontendControlCss.contains("prefers-reduced-motion"));

        assertTrue(adminControlCss.contains("body.admin-control-center-v2"));
        assertTrue(adminLightCss.contains("body.admin-control-center-v2"));
        assertTrue(adminLightCss.contains("body.admin-solid-ui"));
        assertTrue(guideCss.contains("body.admin-solid-ui"));
        assertTrue(guideCss.contains(".guide-admin-toolbar"));
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
        assertFalse(sharedCss.contains("@import url(\"/assets/record-detail.css"));
        assertFalse(sharedCss.contains("@import url(\"/assets/account-submit.css"));
        assertFalse(sharedCss.contains("@import url(\"/assets/vnext-account-submit-v3.css"));
        assertFalse(bridgeFragment.contains("th:fragment=\"deepPageBridge\""));
        assertTrue(bridgeFragment.contains("th:fragment=\"accountSubmitBridge\""));
        assertFalse(bridgeFragment.contains("vnext-legacy-bridge-v3.css"));
        assertFalse(bridgeFragment.contains("data-vnext-legacy-bridge"));
        assertTrue(bridgeFragment.contains("/assets/account-submit.css?v=20261011refactor01"));
        assertTrue(bridgeFragment.contains("data-account-submit-style=\"1\""));
        assertTrue(accountCenterFragment.contains("th:fragment=\"bar\""));
        assertFalse(accountCenterFragment.contains("fragments/vnext-legacy :: deepPageBridge"));

        assertTrue(serviceDesktop.contains("/assets/service-detail.css?v=20261011refactor02"));
        assertTrue(serviceMobile.contains("/assets/service-detail.css?v=20261011refactor02"));
        assertFalse(serviceDesktop.contains("fragments/vnext-legacy :: deepPageBridge"));
        assertFalse(serviceMobile.contains("fragments/vnext-legacy :: deepPageBridge"));

        assertTrue(recordDesktop.contains("/assets/record-detail.css?v=20261011refactor03"));
        assertTrue(recordMobile.contains("/assets/record-detail.css?v=20261011refactor03"));
        assertTrue(orderDesktop.contains("/assets/record-detail.css?v=20261011refactor03"));
        assertTrue(orderMobile.contains("/assets/record-detail.css?v=20261011refactor03"));
        assertFalse(recordDesktop.contains("fragments/vnext-legacy :: deepPageBridge"));
        assertFalse(recordMobile.contains("fragments/vnext-legacy :: deepPageBridge"));
        assertFalse(orderDesktop.contains("fragments/vnext-legacy :: deepPageBridge"));
        assertFalse(orderMobile.contains("fragments/vnext-legacy :: deepPageBridge"));

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
        assertFalse(imageRuntime.contains("record-detail.css"));
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
    void adminEntrypointsUseCanonicalStylesWithoutLegacyBridgeAndKeepBusinessApis() throws IOException {
        String editor = read("src/main/resources/static/assets/admin-image-editor.js");
        String controls = read("src/main/resources/static/assets/admin-frontend-controls.js");
        String frontendControlCss = read("src/main/resources/static/assets/admin-frontend-controls.css");
        String brandFixes = read("src/main/resources/static/assets/admin-brand-layout-fixes.css");
        String guideAdmin = read("src/main/resources/templates/admin-guides.html");

        assertFalse(editor.contains("/assets/vnext-legacy-bridge-v3.css"));
        assertTrue(editor.contains("/assets/vnext-formal-ui.css?v=20261010ui03"));
        assertTrue(editor.contains("/assets/admin-brand-layout-fixes.css?v=20261008b002"));
        assertTrue(editor.contains("data-admin-brand-layout-fixes"));
        assertFalse(controls.contains("/assets/vnext-legacy-bridge-v3.css"));
        assertFalse(controls.contains("ensureVNextBridge"));
        assertTrue(controls.contains("/assets/admin-brand-layout-fixes.css?v=20261008b002"));
        assertTrue(frontendControlCss.contains("原共享 vnext legacy bridge 中的前端控制中心视觉规则迁移到业务自有资产"));
        assertFalse(brandFixes.contains("vnext-legacy-bridge-v3.css"));
        assertTrue(guideAdmin.contains("/assets/admin-guides-workspace.css"));
        assertTrue(guideAdmin.contains("/assets/admin-light-theme.css"));
        assertTrue(guideAdmin.contains("/assets/admin-brand-layout-fixes.css"));

        assertTrue(controls.contains("api('/api/admin/global-ui-settings')"));
        assertTrue(controls.contains("api('/api/admin/frontend-controls')"));
        assertTrue(controls.contains("method: 'PATCH'"));
    }
}
