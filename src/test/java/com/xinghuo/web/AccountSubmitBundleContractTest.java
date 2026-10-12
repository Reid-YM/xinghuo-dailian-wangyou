package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class AccountSubmitBundleContractTest {
    private static final Path MAIN = Path.of("src/main");
    private static final Path TEMPLATES = MAIN.resolve("resources/templates");
    private static final Path ASSETS = MAIN.resolve("resources/static/assets");

    @Test
    void submissionPageLoadsCoreAndDedicatedRuntimeInsteadOfAccountMonolith() throws Exception {
        String html = Files.readString(TEMPLATES.resolve("account-submit.html"));
        String core = "/assets/account-core.js?v=20261012ac01";
        String submit = "/assets/account-submit.js?v=20261012as01";
        assertTrue(html.contains(core));
        assertTrue(html.contains(submit));
        assertTrue(html.indexOf(core) < html.indexOf(submit));
        assertFalse(html.contains("/assets/account.js"));
    }

    @Test
    void submissionRuntimeUsesCoreAndKeepsSubmissionBehavior() throws Exception {
        String js = Files.readString(ASSETS.resolve("account-submit.js"));
        assertTrue(js.contains("window.XHAccountCore"));
        assertTrue(js.contains("/api/account/submissions?size=50"));
        assertTrue(js.contains("/api/account/submissions/${item.id}/withdraw"));
        assertTrue(js.contains("request('/api/account/submissions'"));
        assertTrue(js.contains("/api/catalog"));
        assertTrue(js.contains("xinghuo:submission-draft:v1"));
        assertTrue(js.contains("localStorage.setItem"));
        assertTrue(js.contains("localStorage.removeItem"));
        assertTrue(js.contains("text.replace(/\\*\\*/g, '')"));
        assertFalse(js.contains("innerHTML"));
        assertFalse(js.contains("async function request("));
        assertFalse(js.contains("'X-Requested-With': 'fetch'"));
    }

    @Test
    void legacyAccountRuntimeStillOwnsSettingsUntilNextSplit() throws Exception {
        String settings = Files.readString(TEMPLATES.resolve("account-settings.html"));
        assertTrue(settings.contains("/assets/account.js"));
        assertFalse(settings.contains("/assets/account-submit.js"));
    }
}
