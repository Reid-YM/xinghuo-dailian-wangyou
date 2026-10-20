package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.Route;
import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

@EnabledIfEnvironmentVariable(named = "PLAYWRIGHT_UI_TESTS", matches = "1")
class FrontendNextN26AsyncNavigationPlaywrightTest {
    private static final Path SCRIPT = Path.of("src/main/resources/static/next/n26-records-center.js");
    private static final String NO = "XHABCDEFG001";
    private static final String HTML = """
            <!doctype html><html lang="zh-CN"><head><meta charset="utf-8"></head><body>
            <nav class="nav"></nav><section class="page active" data-page="home"></section>
            <footer class="next-footer"></footer></body></html>
            """;

    private static void useFixture(Page page, List<Route> pending) {
        page.route("**/*", route -> {
            String path = URI.create(route.request().url()).getPath();
            if (path.startsWith("/api/records/")) {
                pending.add(route);
            } else {
                route.fulfill(new Route.FulfillOptions()
                        .setStatus(200).setContentType("text/html; charset=utf-8").setBody(HTML));
            }
        });
        page.navigate("https://example.test/next/records");
    }

    private static void reply(Route route, String number) {
        route.fulfill(new Route.FulfillOptions().setStatus(200)
                .setContentType("application/json")
                .setBody("{\"businessNo\":\"" + number + "\",\"stage\":\"QUOTE\",\"status\":\"NEW\"}"));
    }

    @Test
    void leavingRecordsWhileLookupIsPendingDoesNotRedirectOrSaveStaleResults() {
        try (Playwright playwright = Playwright.create();
             Browser chromium = playwright.chromium().launch();
             Browser webkit = playwright.webkit().launch()) {
            for (Browser browser : List.of(chromium, webkit)) {
                Page page = browser.newPage(new Browser.NewPageOptions().setViewportSize(390, 844));
                try {
                    List<Route> pending = new ArrayList<>();
                    useFixture(page, pending);
                    page.addScriptTag(new Page.AddScriptTagOptions().setPath(SCRIPT));
                    page.locator("#n26BusinessNo").fill(NO);
                    page.locator("#n26LookupForm button[type=submit]").click();
                    page.waitForCondition(() -> pending.size() == 1);

                    // The shell updates the history within the same document.
                    page.evaluate("() => { history.pushState({}, '', '/next/services'); "
                            + "window.dispatchEvent(new CustomEvent('xh:pagechange', {detail:{page:'services'}})); }");
                    reply(pending.get(0), NO);
                    page.waitForTimeout(200);
                    assertEquals("/next/services", URI.create(page.url()).getPath());
                    assertEquals("[]", page.evaluate(
                            "() => JSON.stringify(JSON.parse(localStorage.getItem('xh_client_records_v1') || '[]'))"));

                    // Returning to the record center restores an enabled lookup form.
                    page.goBack();
                    page.waitForSelector("#n26RecordsCenter.active #n26LookupForm button:not([disabled])");
                    assertEquals("/next/records", URI.create(page.url()).getPath());
                } finally {
                    page.close();
                }
            }
        }
    }

    @Test
    void savedHistoryIsFetchedInBoundedBatchesAndKeepsOriginalOrder() {
        try (Playwright playwright = Playwright.create();
             Browser chromium = playwright.chromium().launch();
             Browser webkit = playwright.webkit().launch()) {
            for (Browser browser : List.of(chromium, webkit)) {
                Page page = browser.newPage(new Browser.NewPageOptions().setViewportSize(390, 844));
                try {
                    List<Route> pending = new ArrayList<>();
                    useFixture(page, pending);
                    page.evaluate("() => localStorage.setItem('xh_client_records_v1', JSON.stringify("
                            + "Array.from({length:8}, (_, i) => ({businessNo:'XHABCDEFG' + String(i+1).padStart(3,'0'), stage:'QUOTE'}))))");
                    page.addScriptTag(new Page.AddScriptTagOptions().setPath(SCRIPT));
                    page.waitForCondition(() -> pending.size() == 4);
                    page.waitForTimeout(100);
                    assertEquals(4, pending.size(), "read limit must be four concurrent requests");

                    for (int i = 0; i < 8; i++) {
                        final int expected = i + 1;
                        page.waitForCondition(() -> pending.size() >= expected);
                        reply(pending.get(i), "XHABCDEFG" + String.format("%03d", i + 1));
                    }
                    page.waitForSelector("#n26ListHost .n26-card:nth-child(8)");
                    assertEquals(8, page.locator("#n26ListHost .n26-card").count());
                    assertTrue(page.locator("#n26ListHost .n26-card").first().textContent().contains("XHABCDEFG001"));
                    assertTrue(page.locator("#n26ListHost .n26-card").last().textContent().contains("XHABCDEFG008"));
                } finally {
                    page.close();
                }
            }
        }
    }
}
