package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.Route;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

@EnabledIfEnvironmentVariable(named = "PLAYWRIGHT_UI_TESTS", matches = "1")
class FrontendControlV31PlaywrightTest {
    private static final Path ENHANCER = Path.of("src/main/resources/static/assets/admin-frontend-controls-v31.js");

    @Test
    void chromiumSavesDraftThenPublishesWithoutLegacyImmediatePatch() {
        AtomicInteger draftSaves = new AtomicInteger();
        AtomicInteger publishes = new AtomicInteger();
        AtomicInteger legacyImmediatePatches = new AtomicInteger();

        try (Playwright playwright = Playwright.create(); Browser browser = playwright.chromium().launch()) {
            Page page = browser.newPage(new Browser.NewPageOptions().setViewportSize(1440, 1000));
            page.route("https://frontend.test/**", route -> {
                String url = route.request().url();
                String method = route.request().method();
                if (url.contains("/api/admin/frontend-controls/workspace")) {
                    fulfillJson(route, workspaceJson(false, false, "420"));
                    return;
                }
                if (url.contains("/api/admin/frontend-controls/draft?") && "PATCH".equals(method)) {
                    draftSaves.incrementAndGet();
                    fulfillJson(route, workspaceJson(true, false, "520"));
                    return;
                }
                if (url.contains("/api/admin/frontend-controls/publish?") && "POST".equals(method)) {
                    publishes.incrementAndGet();
                    fulfillJson(route, workspaceJson(false, true, "520"));
                    return;
                }
                if (url.contains("/api/admin/frontend-controls?") && "PATCH".equals(method)) {
                    legacyImmediatePatches.incrementAndGet();
                    fulfillJson(route, "{}");
                    return;
                }
                route.fulfill(new Route.FulfillOptions()
                        .setStatus(200)
                        .setContentType("text/html; charset=utf-8")
                        .setBody(adminHtml()));
            });

            page.navigate("https://frontend.test/admin/frontend-controls");
            page.addScriptTag(new Page.AddScriptTagOptions().setPath(ENHANCER));
            page.waitForSelector("#fcV31Publish");

            page.locator("input[name='homeHeroHeight']").fill("520");
            page.locator("#frontendControlsForm button[type='submit']").click();
            page.waitForFunction("() => document.querySelector('#frontendControlState')?.textContent.includes('草稿已保存')");

            assertEquals(1, draftSaves.get());
            assertEquals(0, legacyImmediatePatches.get(), "V3.1 must intercept the legacy immediate-save submit path");
            assertTrue(page.locator("#fcV31DraftPill").isVisible());

            page.locator("#fcV31Publish").click();
            page.waitForFunction("() => document.querySelector('#frontendControlState')?.textContent.includes('已发布')");

            assertEquals(1, publishes.get());
            assertEquals(0, legacyImmediatePatches.get());
            assertTrue(page.locator("#fcV31DraftPill").isHidden());

            page.locator("[data-v31-preview-device='mobile']").click();
            assertTrue(page.locator("#fcV31Canvas").getAttribute("class").contains("mobile"));
            assertEquals("询价", page.locator(".fc-v31-preview-dock span").last().textContent());
        }
    }

    private static void fulfillJson(Route route, String body) {
        route.fulfill(new Route.FulfillOptions()
                .setStatus(200)
                .setContentType("application/json")
                .setBody(body));
    }

    private static String workspaceJson(boolean hasDraft, boolean hasPrevious, String heroHeight) {
        return """
                {
                  "published": {
                    "siteHeaderVisible":"true","siteFooterVisible":"true","mobileDockVisible":"true",
                    "homeHeroVisible":"true","homeLauncherVisible":"true","homeGameEntryVisible":"true",
                    "homeQuotePanelVisible":"true","homeRecordPanelVisible":"true","homeWhyVisible":"true",
                    "homeHotVisible":"true","homeServicesVisible":"true","homeToolVisible":"true",
                    "homeCasesVisible":"true","homeFaqVisible":"true","homeHeroHeight":"%s",
                    "homeModuleOrder":"","homeHeroKicker":"可靠手打 · 纯绿服务 · 进度可查",
                    "homePrimaryCtaLabel":"提前询价","homeQuoteTitle":"选择需求，获取报价",
                    "homeGameTitle":"选择游戏","homeWhyTitle":"四件可以被验证的事",
                    "mobileDockServicesLabel":"服务","mobileDockGuidesLabel":"攻略","mobileDockRecordsLabel":"记录",
                    "mobileDockContactLabel":"客服","mobileDockQuoteLabel":"询价",
                    "themeAccentColor":"#f4bd34","themeBackgroundColor":"#0b0d10",
                    "themeSurfaceColor":"#171b21","themeTextColor":"#f5f6f7","themeRadius":"18"
                  },
                  "draft": {
                    "siteHeaderVisible":"true","siteFooterVisible":"true","mobileDockVisible":"true",
                    "homeHeroVisible":"true","homeLauncherVisible":"true","homeGameEntryVisible":"true",
                    "homeQuotePanelVisible":"true","homeRecordPanelVisible":"true","homeWhyVisible":"true",
                    "homeHotVisible":"true","homeServicesVisible":"true","homeToolVisible":"true",
                    "homeCasesVisible":"true","homeFaqVisible":"true","homeHeroHeight":"%s",
                    "homeModuleOrder":"","homeHeroKicker":"可靠手打 · 纯绿服务 · 进度可查",
                    "homePrimaryCtaLabel":"提前询价","homeQuoteTitle":"选择需求，获取报价",
                    "homeGameTitle":"选择游戏","homeWhyTitle":"四件可以被验证的事",
                    "mobileDockServicesLabel":"服务","mobileDockGuidesLabel":"攻略","mobileDockRecordsLabel":"记录",
                    "mobileDockContactLabel":"客服","mobileDockQuoteLabel":"询价",
                    "themeAccentColor":"#f4bd34","themeBackgroundColor":"#0b0d10",
                    "themeSurfaceColor":"#171b21","themeTextColor":"#f5f6f7","themeRadius":"18"
                  },
                  "hasDraft":%s,
                  "hasPrevious":%s,
                  "draftInheritGlobal":false
                }
                """.formatted(heroHeight, heroHeight, hasDraft, hasPrevious);
    }

    private static String adminHtml() {
        return """
                <!doctype html><html><head>
                  <meta name="_csrf" content="token"><meta name="_csrf_header" content="X-CSRF-TOKEN">
                </head><body>
                  <header class="fc-topbar"><strong class="fc-brand"><strong>前端控制中心</strong></strong></header>
                  <main class="fc-shell"><section class="fc-hero"><span class="eyebrow"></span><h1></h1><p></p><div class="fc-status-card"></div></section>
                  <form id="frontendControlsForm">
                    <section id="frontendDeviceScopeCard"><button type="button" class="btn primary" data-frontend-scope="global">全局</button><button type="button" class="btn ghost" data-frontend-scope="desktop">电脑端</button><button type="button" class="btn ghost" data-frontend-scope="mobile">手机端</button><button type="button" id="resetFrontendScope">恢复继承</button></section>
                    <section id="layout"><input type="hidden" name="homeModuleOrder"><div id="homeModuleOrderList"></div><button type="button" id="resetHomeModuleOrder">恢复排序</button></section>
                    <section id="home">
                      <input type="number" name="homeHeroHeight"><input type="number" name="homeCardScale" value="0">
                      <input type="checkbox" name="siteHeaderVisible"><input type="checkbox" name="siteFooterVisible"><input type="checkbox" name="mobileDockVisible">
                      <input type="checkbox" name="homeHeroVisible"><input type="checkbox" name="homeLauncherVisible"><input type="checkbox" name="homeGameEntryVisible">
                      <input type="checkbox" name="homeQuotePanelVisible"><input type="checkbox" name="homeRecordPanelVisible"><input type="checkbox" name="homeWhyVisible">
                      <input type="checkbox" name="homeHotVisible"><input type="checkbox" name="homeServicesVisible"><input type="checkbox" name="homeToolVisible"><input type="checkbox" name="homeCasesVisible"><input type="checkbox" name="homeFaqVisible">
                      <input name="homeHeroKicker"><input name="homePrimaryCtaLabel"><input name="homeQuoteTitle"><input name="homeGameTitle"><input name="homeWhyTitle">
                      <input name="mobileDockServicesLabel"><input name="mobileDockGuidesLabel"><input name="mobileDockRecordsLabel"><input name="mobileDockContactLabel"><input name="mobileDockQuoteLabel">
                      <input type="color" name="themeAccentColor"><input type="color" name="themeBackgroundColor"><input type="color" name="themeSurfaceColor"><input type="color" name="themeTextColor"><input type="number" name="themeRadius">
                    </section>
                    <section id="theme"></section>
                    <div class="fc-sticky-save"><div><b>配置</b><span id="frontendControlState">已读取</span></div><button class="btn primary" type="submit">保存</button></div>
                  </form></main>
                  <div id="toastArea"></div>
                </body></html>
                """;
    }
}
