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
    void authPagesLoadCoreBeforeDedicatedBundleInsteadOfAccountMonolith() throws Exception {
        for (String name : new String[]{"account-login.html", "account-register.html", "account-reset.html"}) {
            String html = Files.readString(TEMPLATES.resolve(name));
            String core = "/assets/account-core.js?v=20261012ac01";
            String auth = "/assets/account-auth.js?v=20261012aa02";
            assertTrue(html.contains(core), name + " must load account-core.js");
            assertTrue(html.contains(auth), name + " must load account-auth.js");
            assertTrue(html.indexOf(core) < html.indexOf(auth), name + " must load account-core.js before account-auth.js");
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
    void accountCoreOwnsSharedSecurityAndPasswordContracts() throws Exception {
        String core = Files.readString(ASSETS.resolve("account-core.js"));
        assertTrue(core.contains("'X-Requested-With': 'fetch'"));
        assertTrue(core.contains("credentials: 'same-origin'"));
        assertTrue(core.contains("url.origin !== location.origin"));
        assertTrue(core.contains("bytes > 72"));
        assertTrue(core.contains("window.XHAccountCore = Object.freeze"));
        assertFalse(core.contains("innerHTML"));
    }

    @Test
    void authBundleOwnsOnlyAuthFlowsAndUsesCore() throws Exception {
        String auth = Files.readString(ASSETS.resolve("account-auth.js"));
        assertTrue(auth.contains("window.XHAccountCore"));
        assertTrue(auth.contains("/api/account/login"));
        assertTrue(auth.contains("/api/account/register/verify/request"));
        assertTrue(auth.contains("/api/account/register"));
        assertTrue(auth.contains("/api/account/password/reset/request"));
        assertTrue(auth.contains("/api/account/password/reset"));
        assertTrue(auth.contains("window.XHAccount = publicApi"));
        assertFalse(auth.contains("'X-Requested-With': 'fetch'"));
        assertFalse(auth.contains("async function request("));
        assertFalse(auth.contains("function passwordStrength("));
        assertFalse(auth.contains("innerHTML"));
    }
}
