package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.playwright.*;
import java.net.URI;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

@EnabledIfEnvironmentVariable(named = "PLAYWRIGHT_UI_TESTS", matches = "1")
class FrontendNextN30MasterySyncPlaywrightTest {
    private static final String PROGRESS = "/api/account/progress/mw4_camo_tracker";
    private static final String REMOTE = "{\"progress\":{\"MULTIPLAYER\":{\"B\":{\"GOLD\":true}}},\"favorites\":[]}";
    private static final ObjectMapper JSON = new ObjectMapper();

    @Test void slowInitialReadNeverOverwritesNewLocalChecksOrUploadsAnUnknownVersion() throws Exception {
        bothEngines(browser -> {
            try (Fixture fixture = new Fixture(browser, true, false, false)) {
                fixture.open();
                fixture.page.locator("input[data-weapon-code=A]").check();
                fixture.page.evaluate("() => new Promise(resolve => setTimeout(resolve, 950))");
                assertEquals(0, fixture.writes.get());
                assertNotNull(fixture.held.get());
                fixture.cloud(fixture.held.get(), 3, REMOTE);
                fixture.page.waitForSelector("#n24Sync[data-state=conflict]");
                assertTrue(fixture.page.locator("input[data-weapon-code=A]").isChecked());
                assertFalse(fixture.page.locator("input[data-weapon-code=B]").isChecked());
                fixture.page.locator("[data-favorite=A]").click();
                assertTrue(fixture.page.locator("#n24UseLocal").isVisible(), "rerender must retain conflict actions");
                fixture.page.locator("#n24UseLocal").click();
                fixture.page.waitForSelector("#n24Sync[data-state=synced]");
                assertEquals(1, fixture.writes.get());
                var body = JSON.readTree(fixture.lastWrite.get());
                assertEquals(3, body.path("version").asInt());
                var payload = JSON.readTree(body.path("payload").asText());
                assertTrue(payload.path("progress").path("MULTIPLAYER").path("A").path("GOLD").asBoolean());
                assertEquals("A", payload.path("favorites").get(0).asText());
            }
        });
    }

    @Test void versionConflictPreservesLocalUntilUserChoosesCloudAndNextWriteUsesFreshVersion() throws Exception {
        bothEngines(browser -> {
            try (Fixture fixture = new Fixture(browser, false, true, false)) {
                fixture.open();
                fixture.page.waitForSelector("#n24Sync[data-state=synced]");
                fixture.page.locator("input[data-weapon-code=A]").check();
                fixture.page.waitForSelector("#n24Sync[data-state=conflict]");
                assertEquals(1, fixture.writes.get());
                assertTrue(fixture.page.locator("input[data-weapon-code=A]").isChecked());
                fixture.page.locator("#n24UseCloud").click();
                fixture.page.waitForSelector("#n24Sync[data-state=synced]");
                assertFalse(fixture.page.locator("input[data-weapon-code=A]").isChecked());
                assertTrue(fixture.page.locator("input[data-weapon-code=B]").isChecked());
                assertEquals(1, fixture.writes.get(), "choosing cloud must not upload local automatically");
                fixture.page.locator("input[data-weapon-code=A]").check();
                fixture.page.waitForSelector("#n24Sync[data-state=synced]");
                assertEquals(2, fixture.writes.get());
                assertEquals(2, JSON.readTree(fixture.lastWrite.get()).path("version").asInt());
            }
        });
    }

    @Test void failedCloudReadRetriesReadBeforeAnyWriteAndLocalSurvivesReload() throws Exception {
        bothEngines(browser -> {
            try (Fixture fixture = new Fixture(browser, false, false, true)) {
                fixture.open();
                fixture.page.waitForSelector("#n24Sync[data-state=error]");
                fixture.page.locator("input[data-weapon-code=A]").check();
                fixture.page.evaluate("() => new Promise(resolve => setTimeout(resolve, 950))");
                assertEquals(0, fixture.writes.get());
                fixture.page.locator("#n24SyncAction").click();
                fixture.page.waitForSelector("#n24Sync[data-state=conflict]");
                assertEquals(2, fixture.reads.get());
                assertTrue(fixture.page.locator("input[data-weapon-code=A]").isChecked());
                fixture.page.reload();
                fixture.page.waitForSelector("#n24Sync[data-state=conflict]");
                assertTrue(fixture.page.locator("input[data-weapon-code=A]").isChecked());
                assertEquals(0, fixture.writes.get());
            }
        });
    }

    @Test void clearingLastFavoriteRemainsAnUnsyncedEditAfterReload() throws Exception {
        bothEngines(browser -> {
            try (Fixture fixture = new Fixture(browser, false, false, false)) {
                fixture.page.addInitScript("if (!localStorage.getItem('xinghuo-mw4-camo-tracker-v2')) localStorage.setItem('xinghuo-mw4-camo-tracker-v2', JSON.stringify({progress:{},favorites:['A']}));");
                fixture.open();
                fixture.page.waitForSelector("#n24Sync[data-state=conflict]");
                fixture.page.locator("[data-favorite=A]").click();
                assertEquals(0, fixture.page.evaluate("() => JSON.parse(localStorage.getItem('xinghuo-mw4-camo-tracker-v2')).favorites.length"));
                fixture.page.reload();
                fixture.page.waitForSelector("#n24Sync[data-state=conflict]");
                assertFalse(fixture.page.locator("input[data-weapon-code=B]").isChecked());
                assertEquals(0, fixture.writes.get());
            }
        });
    }

    interface Check { void run(Browser browser) throws Exception; }
    private void bothEngines(Check check) throws Exception {
        try (Playwright playwright = Playwright.create(); Browser chromium = playwright.chromium().launch();
             Browser webkit = playwright.webkit().launch()) {
            for (Browser browser : List.of(chromium, webkit)) check.run(browser);
        }
    }

    private static class Fixture implements AutoCloseable {
        final Page page;
        final AtomicInteger reads = new AtomicInteger(), writes = new AtomicInteger();
        final AtomicReference<Route> held = new AtomicReference<>();
        final AtomicReference<String> lastWrite = new AtomicReference<>();
        Fixture(Browser browser, boolean delay, boolean conflict, boolean failRead) {
            page = browser.newPage(new Browser.NewPageOptions().setViewportSize(390, 844));
            page.onDialog(Dialog::accept);
            page.route("**/*", route -> {
                String path = URI.create(route.request().url()).getPath();
                if (path.endsWith("n24-mastery.js")) {
                    route.fulfill(new Route.FulfillOptions().setContentType("application/javascript")
                            .setPath(Path.of("src/main/resources/static/next/n24-mastery.js")));
                } else if (path.equals("/api/tool-station")) {
                    json(route, 200, """
                        {"games":[{"code":"CODMW4","name":"MW4","weapons":[
                        {"code":"A","name":"Weapon A","weaponType":"AR"},{"code":"B","name":"Weapon B","weaponType":"AR"}],
                        "camoTracks":{"order":["MULTIPLAYER"],"modes":{"MULTIPLAYER":{"code":"MULTIPLAYER","name":"多人","items":[{"code":"GOLD","name":"金色"}]}}}}]}
                        """);
                } else if (path.equals("/api/account/me")) {
                    json(route, 200, "{\"displayName\":\"测试账号\"}");
                } else if (path.equals(PROGRESS) && route.request().method().equals("GET")) {
                    int count = reads.incrementAndGet();
                    if (delay && count == 1) { held.set(route); return; }
                    if (failRead && count == 1) json(route, 503, "{}");
                    else if (conflict && count == 1) cloud(route, 1, "{\"progress\":{},\"favorites\":[]}");
                    else cloud(route, 2, REMOTE);
                } else if (path.equals(PROGRESS)) {
                    assertEquals("PUT", route.request().method());
                    assertEquals("fetch", route.request().headers().get("x-requested-with"));
                    lastWrite.set(route.request().postData());
                    if (conflict && writes.incrementAndGet() == 1) json(route, 409, "{}");
                    else {
                        if (!conflict) writes.incrementAndGet();
                        try { cloud(route, JSON.readTree(lastWrite.get()).path("version").asInt() + 1,
                                JSON.readTree(lastWrite.get()).path("payload").asText()); }
                        catch (Exception e) { throw new RuntimeException(e); }
                    }
                } else {
                    route.fulfill(new Route.FulfillOptions().setContentType("text/html;charset=utf-8").setBody("""
                        <html><head><meta charset="utf-8"><style>.page{display:none}.page.active{display:block}</style></head>
                        <body><section class="page active" data-page="home"></section><footer class="next-footer"></footer>
                        <script src="/next/n24-mastery.js"></script></body></html>
                        """));
                }
            });
        }
        void open() { page.navigate("https://example.test/next/mastery"); page.waitForSelector("#n24Sync"); }
        void cloud(Route route, int version, String payload) {
            try { json(route, 200, JSON.writeValueAsString(Map.of("version", version, "payload", payload))); }
            catch (Exception e) { throw new RuntimeException(e); }
        }
        @Override public void close() { page.close(); }
    }
    private static void json(Route route, int status, String body) {
        route.fulfill(new Route.FulfillOptions().setStatus(status).setContentType("application/json").setBody(body));
    }
}
