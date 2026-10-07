package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

@EnabledIfEnvironmentVariable(named = "PLAYWRIGHT_UI_TESTS", matches = "1")
class AdminControlCenterV2PlaywrightTest {
    private static final Path ASSETS = Path.of("src/main/resources/static/assets");

    @Test
    void controlCenterBuildsWorkspaceChromeAndTracksVisiblePanel() {
        try (Playwright playwright = Playwright.create(); Browser browser = playwright.chromium().launch()) {
            Page page = browser.newPage();
            page.setContent("""
                    <body class="admin-solid-ui">
                      <div class="admin-shell admin-shell-with-rail">
                        <aside class="sidebar">
                          <div class="admin-brand"><b>星火后台</b></div>
                          <nav class="admin-workspace-nav">
                            <div class="nav-section"><div class="nav-section-items">
                              <button class="nav-item active" data-nav="dashboard"><span>运营总览</span><small>数据</small></button>
                              <button class="nav-item" data-nav="orders"><span>履约订单</span><small>进度</small></button>
                            </div></div>
                          </nav>
                        </aside>
                        <main class="admin-main">
                          <section data-panel="dashboard"><div class="admin-toolbar"><h1>今日概览</h1></div></section>
                          <section class="hidden" data-panel="orders"><div class="admin-toolbar"><h1>履约订单</h1></div></section>
                        </main>
                      </div>
                    </body>
                    """);
            page.addScriptTag(new Page.AddScriptTagOptions().setPath(ASSETS.resolve("admin-control-center-v2.js")));

            assertTrue((Boolean) page.evaluate("() => document.body.classList.contains('admin-control-center-v2')"));
            assertEquals(Boolean.FALSE, page.evaluate("() => document.body.classList.contains('admin-solid-ui')"));
            assertEquals(1, page.locator("#adminV2Topbar").count());
            assertEquals("今日概览", page.locator("#adminV2CurrentTitle").textContent());
            assertEquals(1, page.locator("#adminV2NavSearch").count());

            page.evaluate("""
                    () => {
                      document.querySelector('[data-panel=dashboard]').classList.add('hidden');
                      document.querySelector('[data-panel=orders]').classList.remove('hidden');
                    }
                    """);
            page.waitForFunction("() => document.querySelector('#adminV2CurrentTitle')?.textContent === '履约订单'");
            assertEquals("履约订单", page.locator("#adminV2CurrentTitle").textContent());
        }
    }

    @Test
    void navigationSearchFiltersItemsWithoutRemovingBusinessContracts() {
        try (Playwright playwright = Playwright.create(); Browser browser = playwright.chromium().launch()) {
            Page page = browser.newPage();
            page.setContent("""
                    <body>
                      <div class="admin-shell admin-shell-with-rail">
                        <aside class="sidebar">
                          <div class="admin-brand"><b>星火后台</b></div>
                          <nav class="admin-workspace-nav">
                            <div class="nav-section"><div class="nav-section-items">
                              <button class="nav-item" data-nav="quotes"><span>询价线索</span></button>
                              <button class="nav-item" data-nav="orders"><span>履约订单</span></button>
                            </div></div>
                          </nav>
                        </aside>
                        <main class="admin-main"><section data-panel="dashboard"><div class="admin-toolbar"><h1>总览</h1></div></section></main>
                      </div>
                    </body>
                    """);
            page.addScriptTag(new Page.AddScriptTagOptions().setPath(ASSETS.resolve("admin-control-center-v2.js")));
            page.locator("#adminV2NavSearch").fill("订单");

            assertTrue((Boolean) page.evaluate("() => document.querySelector('[data-nav=quotes]').classList.contains('admin-v2-filter-hidden')"));
            assertEquals(Boolean.FALSE, page.evaluate("() => document.querySelector('[data-nav=orders]').classList.contains('admin-v2-filter-hidden')"));
            assertEquals("orders", page.locator("[data-nav=orders]").getAttribute("data-nav"));
        }
    }

    @Test
    void resilienceLoadsControlCenterAssetsAndStylesStayAdminScoped() throws Exception {
        String resilience = Files.readString(ASSETS.resolve("admin-resilience.js"));
        String css = Files.readString(ASSETS.resolve("admin-control-center-v2.css"));
        assertTrue(resilience.contains("/assets/admin-control-center-v2.js?v=20261007a002"));
        assertTrue(resilience.contains("/assets/admin-control-center-v2.css?v=20261007a002"));
        assertTrue(css.contains("body.admin-control-center-v2 .admin-shell-with-rail"));
        assertTrue(css.contains("body.admin-control-center-v2 .table-wrap"));
        assertTrue(css.contains("body.admin-control-center-v2 .admin-v2-topbar"));
    }
}
