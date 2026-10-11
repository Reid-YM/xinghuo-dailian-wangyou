package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class AccountAuthBundleContractTest {
    private static final Path MAIN = Path.of("src/main");
    private static final Path TEMPLATES = MAIN.resolve("resources/templates");
    private static final Path ASSETS = MAIN.resolve("resources/static/assets");

    @Test
    void authPagesLoadDedicatedBundleInsteadOfAccountMonolith() throws Exception {
        for (String name : new String[]{"account-login.html", "account-register.html", "account-reset.html"}) {
            String html = Files.readString(TEMPLATES.resolve(name));
            assertTrue(html.contains("/assets/account-auth.js?v=20261012aa01"), name + " must load account-auth.js");
            assertFalse(html.contains("/assets/account.js"), name + " must not load the account monolith");
        }
    }

    @Test
    void accountWorkspacePagesKeepTheirExistingOwners() throws Exception {
        String overview = Files.readString(TEMPLATES.resolve("account.html"));
        assertTrue(overview.contains("/assets/account-center.js"));
        assertFalse(overview.contains("/assets/account.js"));
        assertFalse(overview.contains("/assets/account-auth.js"));

        for (String name : new String[]{"account-settings.html", "account-submit.html"}) {
            String html = Files.readString(TEMPLATES.resolve(name));
            assertTrue(html.contains("/assets/account.js"), name + " still uses the legacy account workspace runtime");
            assertFalse(html.contains("/assets/account-auth.js"), name + " must not load the auth-only bundle");
        }
    }

    @Test
    void authBundlePreservesSecurityAndCompatibilityContracts() throws Exception {
        String js = Files.readString(ASSETS.resolve("account-auth.js"));
        assertTrue(js.contains("'X-Requested-With': 'fetch'"));
        assertTrue(js.contains("credentials: 'same-origin'"));
        assertTrue(js.contains("url.origin !== location.origin"));
        assertTrue(js.contains("/api/account/login"));
        assertTrue(js.contains("/api/account/register/verify/request"));
        assertTrue(js.contains("/api/account/register"));
        assertTrue(js.contains("/api/account/password/reset/request"));
        assertTrue(js.contains("/api/account/password/reset"));
        assertTrue(js.contains("bytes > 72"));
        assertTrue(js.contains("window.XHAccount = publicApi"));
        assertFalse(js.contains("innerHTML"));
    }
}
