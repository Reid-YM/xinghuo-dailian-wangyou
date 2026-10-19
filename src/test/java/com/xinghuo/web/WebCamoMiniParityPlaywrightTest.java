package com.xinghuo.web;

import com.microsoft.playwright.*;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import static org.junit.jupiter.api.Assertions.*;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

@EnabledIfEnvironmentVariable(named = "PLAYWRIGHT_UI_TESTS", matches = "1")
class WebCamoMiniParityPlaywrightTest {
    private void verify(Browser browser) throws Exception {
        String html = Files.readString(Path.of("src/main/resources/templates/mw4-camo-tracker.html"));
        String catalog = Files.readString(Path.of("src/main/resources/static/assets/tool-station-games.json"));
        Page page = browser.newPage(new Browser.NewPageOptions().setViewportSize(390, 844));
        page.route("**/*", route -> {
            String path = java.net.URI.create(route.request().url()).getPath();
            if (path.equals("/api/tool-station")) route.fulfill(new Route.FulfillOptions().setContentType("application/json").setBody(catalog));
            else if (path.startsWith("/api/")) route.fulfill(new Route.FulfillOptions().setStatus(401).setBody("{}"));
            else if (path.startsWith("/assets/")) route.fulfill(new Route.FulfillOptions().setPath(Path.of("src/main/resources/static" + path)));
            else route.fulfill(new Route.FulfillOptions().setContentType("text/html").setBody(html));
        });
        page.addInitScript("""
            if (!localStorage.getItem('xinghuo-mw4-camo-tracker-v2')) {
              localStorage.setItem('xinghuo-mw4-camo-tracker-v2', JSON.stringify({activeMode:'MULTIPLAYER',
                progress:{MULTIPLAYER:{HAN86:{TIER_1:true}}}, favorites:['HAN86']}));
            }
            """);
        page.navigate("http://xinghuo.test/mastery");
        assertThat(page.locator("[data-weapon-wrap]")).hasCount(33);
        page.locator("#weaponSearch").fill("汉86");
        page.locator("[data-toggle-weapon=HAN86]").click();
        assertThat(page.locator("[data-guide-item^='HAN86::mil-']")).hasCount(8);
        assertThat(page.locator("[data-guide-item^='HAN86::special-']")).hasCount(3);
        assertThat(page.locator("[data-guide-item^='HAN86::comp-']")).hasCount(4);
        assertThat(page.locator(".camo-guide")).containsText("满级 72");
        assertThat(page.locator(".camo-guide")).containsText("46级，消音器50次击杀");
        assertThat(page.locator(".camo-guide")).containsText("和声间奏");
        assertThat(page.locator(".camo-guide")).containsText("分歧");
        assertEquals(0, page.locator("[data-camo-item]").count());
        assertThat(page.locator("[data-fav=HAN86]")).hasAttribute("aria-label", "取消收藏 汉86");
        assertThat(page.locator("#tierCards")).containsText("已有 1/33 把武器的历史记录");
        page.locator("[data-mastery-item]").nth(2).check();
        assertEquals(1, page.locator("[data-mastery-item]:checked").count());
        assertEquals(true, page.evaluate("()=>Object.values(JSON.parse(localStorage.getItem('xinghuo-mw4-camo-tracker-v2')).progress.MULTIPLAYER).every(w=>w.TIER_3)"));
        page.locator("[data-mastery-item]").nth(2).uncheck();
        assertEquals(0, page.locator("[data-mastery-item]:checked").count());
        assertEquals(true, page.evaluate("()=>JSON.parse(localStorage.getItem('xinghuo-mw4-camo-tracker-v2')).progress.MULTIPLAYER.HAN86.TIER_1"));
        page.locator("[data-mastery-item]").nth(1).check();
        String overall = page.locator("#overallCount").textContent();
        page.locator("[data-guide-item='HAN86::special-1']").check();
        assertEquals(overall, page.locator("#overallCount").textContent());
        for (int width : new int[]{320, 390, 1440}) {
            page.setViewportSize(width, 900);
            assertEquals(true, page.evaluate("()=>document.documentElement.scrollWidth<=innerWidth"));
        }
        page.locator(".camo-guide img").first().scrollIntoViewIfNeeded();
        page.waitForFunction("()=>document.querySelector('.camo-guide img').naturalWidth>0");
        assertTrue(page.locator(".camo-guide img").first().getAttribute("src").startsWith("/assets/mini-camo/"));
        page.screenshot(new Page.ScreenshotOptions().setPath(Path.of("target/web-camo-desktop.png")));
        page.setViewportSize(390, 844);
        page.screenshot(new Page.ScreenshotOptions().setPath(Path.of("target/web-camo-mobile.png")));
        page.reload();
        page.locator("#weaponSearch").fill("汉86");
        page.locator("[data-toggle-weapon=HAN86]").click();
        assertThat(page.locator("[data-guide-item='HAN86::special-1']")).isChecked();
        assertThat(page.locator("[data-mastery-item]").nth(1)).isChecked();
        assertEquals(0, page.locator("[data-camo-item]").count());
        page.locator("[data-mode=WARZONE]").click();
        assertEquals(0, page.locator("[data-mastery-item]:checked").count());
        page.locator("[data-toggle-weapon=HAN86]").click();
        assertThat(page.locator(".camo-guide")).containsText("职业通用挑战参考");
        page.locator("[data-mode=PYROCLAST]").click();
        assertThat(page.locator("#pyroCards")).containsText("150");
        assertThat(page.locator("#pyroCards")).containsText("250");
        page.locator(".camo-pyro-card").nth(3).click();
        assertEquals(1, page.locator("[data-pyro-item]:checked").count());
        page.close();
    }

    @Test void chromiumMatchesMiniProgram128() throws Exception {
        try (Playwright p = Playwright.create()) {
            var options = new BrowserType.LaunchOptions();
            String executable = System.getenv("XINGHUO_TEST_CHROMIUM");
            if (executable != null && !executable.isBlank()) options.setExecutablePath(Path.of(executable));
            try (Browser browser = p.chromium().launch(options)) { verify(browser); }
        }
    }

    @Test void webkitMatchesMiniProgram128() throws Exception {
        try (Playwright p = Playwright.create(); Browser browser = p.webkit().launch()) { verify(browser); }
    }
}
