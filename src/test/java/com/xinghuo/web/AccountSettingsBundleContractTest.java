package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class AccountSettingsBundleContractTest {
    private static final Path MAIN = Path.of("src/main");
    private static final Path TEMPLATES = MAIN.resolve("resources/templates");
    private static final Path ASSETS = MAIN.resolve("resources/static/assets");

    @Test
    void settingsPageUsesDedicatedRuntimeStackWithoutAccountMonolith() throws Exception {
        String html = Files.readString(TEMPLATES.resolve("account-settings.html"));
        String core = "/assets/account-core.js?v=20261012ac01";
        String settings = "/assets/account-settings.js?v=20261012ase01";
        assertTrue(html.contains(core));
        assertTrue(html.contains(settings));
        assertTrue(html.indexOf(core) < html.indexOf(settings));
        assertTrue(html.contains("/assets/account-center.js"));
        assertTrue(html.contains("/assets/account-profile-controller.js"));
        assertFalse(html.contains("/assets/account.js"));
    }

    @Test
    void settingsRuntimeOwnsSecurityContactEmailAndSessionsButNotProfileForm() throws Exception {
        String js = Files.readString(ASSETS.resolve("account-settings.js"));
        assertTrue(js.contains("window.XHAccountCore"));
        assertTrue(js.contains("/api/account/password/change"));
        assertTrue(js.contains("/api/account/email/verify/request"));
        assertTrue(js.contains("/api/account/email/verify/confirm"));
        assertTrue(js.contains("/api/account/sessions"));
        assertTrue(js.contains("/api/account/sessions/revoke-all"));
        assertTrue(js.contains("/api/account/security/events"));
        assertTrue(js.contains("/api/account/quote-contacts"));
        assertFalse(js.contains("accountProfileFormV2"));
        assertFalse(js.contains("/api/account/profile/avatar"));
        assertFalse(js.contains("/api/account/profile'"));
        assertFalse(js.contains("async function request("));
        assertFalse(js.contains("'X-Requested-With': 'fetch'"));
        assertFalse(js.contains("innerHTML"));
    }

    @Test
    void profileControllerIsTheSingleProfileFormOwnerAmongLoadedSettingsRuntimes() throws Exception {
        String profile = Files.readString(ASSETS.resolve("account-profile-controller.js"));
        String settings = Files.readString(ASSETS.resolve("account-settings.js"));
        String center = Files.readString(ASSETS.resolve("account-center.js"));
        assertTrue(profile.contains("accountProfileFormV2"));
        assertTrue(profile.contains("/api/account/profile/avatar"));
        assertTrue(profile.contains("/api/account/profile"));
        assertFalse(settings.contains("accountProfileFormV2"));
        assertFalse(center.contains("accountProfileFormV2"));
    }
}
