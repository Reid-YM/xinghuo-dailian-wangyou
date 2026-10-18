package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.Route;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

class FrontendNextN30AuthReturnJourneyPlaywrightTest {
    private static final Path RUNTIME = Path.of("src/main/resources/static/next/n15-auth-return.js");

    @Test
    void allReleasedPreviewFamiliesHaveSafeAuthReturnMapping() throws Exception {
        String runtime = Files.readString(RUNTIME);
        for (String route : new String[] {"account/submit", "guides", "mastery", "records",
                "contact", "trust", "rules", "privacy"}) {
            assertTrue(runtime.contains("url.pathname === '/" + route + "'"),
                    () -> "N30 authentication return misses " + route);
            assertTrue(runtime.contains("url.pathname = '/next/" + route + "'"),
                    () -> "N30 authentication return escapes Next for " + route);
        }
        assertTrue(runtime.contains("/^\\/guides\\/[^/]+\\/?$/"));
        assertTrue(runtime.contains("url.searchParams.get('legacy') !== '1'"));
        assertTrue(runtime.contains("url.searchParams.delete('token')"));
        assertTrue(runtime.contains("url.origin !== location.origin"));
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "PLAYWRIGHT_UI_TESTS", matches = "1")
    void chromiumAndWebkitKeepGuideSubmissionRecordsAndTrustAfterLogin() throws Exception {
        try (Playwright playwright = Playwright.create()) {
            BrowserType.LaunchOptions options = new BrowserType.LaunchOptions();
            String executable = System.getenv("XINGHUO_TEST_CHROMIUM");
            if (executable != null && !executable.isBlank()) {
                options.setExecutablePath(Path.of(executable));
            }
            try (Browser chromium = playwright.chromium().launch(options);
                 Browser webkit = playwright.webkit().launch()) {
                assertReturns(chromium);
                assertReturns(webkit);
            }
        }
    }

    private static void assertReturns(Browser browser) {
        Page page = browser.newPage();
        try {
            page.route("**/*", route -> route.fulfill(new Route.FulfillOptions()
                    .setContentType("text/html; charset=utf-8")
                    .setBody("<!doctype html><html lang='zh-CN'><body><a href='/account/login'>登录</a></body></html>")));
            page.navigate("https://example.test/next/account?next=%2Fguides%3Fgame%3DCODMW4%23latest");
            page.addScriptTag(new Page.AddScriptTagOptions().setPath(RUNTIME));

            assertEquals("/next/guides?game=CODMW4#latest",
                    page.evaluate("() => XHFrontendNextN15.requestedReturn()"));
            assertEquals("/next/guides?game=CODMW4#latest",
                    page.evaluate("() => new URL(XHFrontendNextN15.authUrl('login'), location.origin).searchParams.get('next')"));
            assertEquals("/next/guides/guide-mw4?utm_source=test#part",
                    page.evaluate("() => XHFrontendNextN15.safeAccountReturn('/guides/guide-mw4?utm_source=test#part')"));
            assertEquals("/next/account/submit",
                    page.evaluate("() => XHFrontendNextN15.safeAccountReturn('/account/submit')"));
            assertEquals("/next/records?q=XHABCDEF1234",
                    page.evaluate("() => XHFrontendNextN15.safeAccountReturn('/records?q=XHABCDEF1234')"));
            for (String route : new String[] {"mastery", "contact", "trust", "rules", "privacy"}) {
                assertEquals("/next/" + route,
                        page.evaluate("path => XHFrontendNextN15.safeAccountReturn(path)", "/" + route));
            }
            assertEquals("/guides?legacy=1",
                    page.evaluate("() => XHFrontendNextN15.safeAccountReturn('/guides?legacy=1')"));
            assertEquals("/guest-access?check=1",
                    page.evaluate("() => XHFrontendNextN15.safeAccountReturn('/guest-access?check=1')"));
            assertEquals("/next/account",
                    page.evaluate("() => XHFrontendNextN15.safeAccountReturn('//malicious.example/path')"));
            assertEquals("/next/account",
                    page.evaluate("() => XHFrontendNextN15.safeAccountReturn('/admin/system')"));
            assertEquals("/next/account",
                    page.evaluate("() => XHFrontendNextN15.safeAccountReturn('/logout')"));
            assertEquals("/next/account",
                    page.evaluate("() => XHFrontendNextN15.safeAccountReturn('/api/private')"));
            assertEquals("/next/guides?game=CODMW4#latest",
                    page.evaluate("() => new URL(document.querySelector('a').href).searchParams.get('next')"));
        } finally {
            page.close();
        }
    }
}
