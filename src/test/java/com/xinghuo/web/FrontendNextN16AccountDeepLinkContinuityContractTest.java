package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class FrontendNextN16AccountDeepLinkContinuityContractTest {
    private static final Path MAIN = Path.of("src/main");

    private static String read(String relative) throws Exception {
        return Files.readString(MAIN.resolve(relative));
    }

    @Test
    void previewControllerExposesAccountDeepLinksAndFingerprintsN16() throws Exception {
        String preview = read("java/com/xinghuo/web/FrontendNextPreviewController.java");

        assertTrue(preview.contains("N16_RUNTIME"));
        assertTrue(preview.contains("/next/n16-account-deep-links.js"));
        assertTrue(preview.contains("html.contains(N16_RUNTIME)"));
        assertTrue(preview.contains("next/n16-account-deep-links.js"));
        for (String route : new String[]{
                "/next/account/requests", "/next/account/orders", "/next/account/progress", "/next/account/settings",
                "/next/account/quotes", "/next/account/demands", "/next/account/order-list",
                "/next/account/tool-progress", "/next/account/settings/profile", "/next/account/settings/contact",
                "/next/account/settings/security", "/next/account/devices", "/next/account/activity"}) {
            assertTrue(preview.contains(route), route);
        }
    }

    @Test
    void runtimeCanonicalizesAliasesAndKeepsAccountTabsInTheUrl() throws Exception {
        String runtime = read("resources/static/next/n16-account-deep-links.js");

        assertTrue(runtime.contains("['quotes', { canonical: 'requests', tab: 'quotes'"));
        assertTrue(runtime.contains("['demands', { canonical: 'requests', tab: 'quotes'"));
        assertTrue(runtime.contains("['order-list', { canonical: 'orders', tab: 'orders'"));
        assertTrue(runtime.contains("['tool-progress', { canonical: 'progress', tab: 'orders'"));
        assertTrue(runtime.contains("return tab === 'quotes' ? '/next/account/requests' : '/next/account/orders'"));
        assertTrue(runtime.contains("navigation.pushState"));
        assertTrue(runtime.contains("window.addEventListener('popstate'"));
        assertTrue(runtime.contains("window.addEventListener('hashchange'"));
        assertTrue(runtime.contains("n5.state.active = info.tab"));
        assertTrue(runtime.contains(".n5-tab[data-n5-tab="));
    }

    @Test
    void settingsAndProgressDeepLinksReuseExistingN5AndN6Modules() throws Exception {
        String runtime = read("resources/static/next/n16-account-deep-links.js");
        String n5 = read("resources/static/next/n5.js");
        String n6 = read("resources/static/next/n6-account-security.js");
        String n4 = read("resources/static/next/n4.js");

        assertTrue(runtime.contains("[data-personal-asset=\"progress\"]"));
        assertTrue(runtime.contains("#n6ProfileForm"));
        assertTrue(runtime.contains("#n6ContactForm"));
        assertTrue(runtime.contains("#n6PasswordForm"));
        assertTrue(runtime.contains("#n6Devices"));
        assertTrue(runtime.contains("最近安全活动"));
        assertTrue(runtime.contains("settingsHashes"));

        assertTrue(n5.contains("data.personalAsset") || n5.contains("dataset.personalAsset"));
        assertTrue(n5.contains("/api/account/progress"));
        assertTrue(n6.contains("form.id = 'n6ProfileForm'"));
        assertTrue(n6.contains("form.id='n6ContactForm'"));
        assertTrue(n6.contains("form.id = 'n6PasswordForm'"));
        assertTrue(n6.contains("list.id = 'n6Devices'"));
        assertTrue(n4.contains("'最近安全活动'"));
    }

    @Test
    void authReturnAndOrdinaryAccountLinksStayInsideNextUnlessLegacyIsExplicit() throws Exception {
        String runtime = read("resources/static/next/n16-account-deep-links.js");

        assertTrue(runtime.contains("function supportedAccountTarget"));
        assertTrue(runtime.contains("normalizeReturnParam()"));
        assertTrue(runtime.contains("current.searchParams.set('next', target)"));
        assertTrue(runtime.contains("url.searchParams.get('legacy') === '1'"));
        assertTrue(runtime.contains("link.dataset.n16Normalized = '1'"));
        assertTrue(runtime.contains("/next/account/${info.canonical}"));
    }

    @Test
    void futureFormalMigrationPreservesCanonicalAccountSubrouteSemantics() throws Exception {
        String migration = read("java/com/xinghuo/web/FrontendMigrationConfig.java");

        assertTrue(migration.contains("Map.entry(\"/account/settings\",\"/next/account/settings\")"));
        assertTrue(migration.contains("Map.entry(\"/account/requests\",\"/next/account/requests\")"));
        assertTrue(migration.contains("Map.entry(\"/account/orders\",\"/next/account/orders\")"));
        assertTrue(migration.contains("Map.entry(\"/account/progress\",\"/next/account/progress\")"));
        assertTrue(migration.contains("app.frontend-next.default-enabled:false"));
        assertTrue(migration.contains("\"1\".equals(request.getParameter(\"legacy\"))"));
    }

    @Test
    void n16IsNavigationOnlyAndIntroducesNoBusinessApiOrClientStorage() throws Exception {
        String runtime = read("resources/static/next/n16-account-deep-links.js");

        assertFalse(runtime.contains("fetch("));
        assertFalse(runtime.contains("localStorage"));
        assertFalse(runtime.contains("sessionStorage"));
        assertFalse(runtime.contains("innerHTML"));
        assertFalse(runtime.contains("insertAdjacentHTML"));
        assertFalse(runtime.contains("method: 'POST'"));
        assertFalse(runtime.contains("method: 'PATCH'"));
        assertFalse(runtime.contains("method: 'PUT'"));
        assertFalse(runtime.contains("method: 'DELETE'"));
        assertFalse(runtime.contains("/api/payment"));
        assertFalse(runtime.contains("/api/refund"));
    }
}
