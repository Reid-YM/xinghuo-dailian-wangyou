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
                            <div class="nav-section">
                              <button data-nav-group="site" type="button"><span>站点运营</span></button>
                              <div class="nav-section-items">
                                <button class="nav-item" data-nav="config"><span>站点配置</span></button>
                              </div>
                            </div>
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
            assertEquals(1, page.locator(".admin-workspace-nav a[href='/admin/frontend-controls']").count());
            assertEquals("前端控制中心", page.locator(".admin-workspace-nav a[href='/admin/frontend-controls'] span").textContent());
            assertEquals(1, page.locator("#adminV2Topbar a[href='/admin/frontend-controls']").count());

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
    void controlCenterRecoversDashboardWhenEveryPanelBecomesHidden() {
        try (Playwright playwright = Playwright.create(); Browser browser = playwright.chromium().launch()) {
            Page page = browser.newPage();
            page.setContent("""
                    <body class="admin-solid-ui">
                      <div class="admin-shell admin-shell-with-rail">
                        <aside class="sidebar">
                          <div class="admin-brand"><b>星火后台</b></div>
                          <nav class="admin-workspace-nav">
                            <div class="nav-section"><div class="nav-section-items">
                              <button class="nav-item active" data-nav="dashboard"><span>运营总览</span></button>
                              <button class="nav-item" data-nav="orders"><span>履约订单</span></button>
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

            page.evaluate("() => document.querySelectorAll('[data-panel]').forEach(panel => panel.classList.add('hidden'))");
            page.waitForFunction("() => !document.querySelector('[data-panel=dashboard]').classList.contains('hidden')");

            assertEquals(Boolean.FALSE, page.evaluate("() => document.querySelector('[data-panel=dashboard]').classList.contains('hidden')"));
            assertTrue((Boolean) page.evaluate("() => document.querySelector('[data-nav=dashboard]').classList.contains('active')"));
            page.waitForFunction("() => document.querySelector('#adminV2CurrentTitle')?.textContent === '今日概览'");
            assertEquals("今日概览", page.locator("#adminV2CurrentTitle").textContent());
        }
    }

    @Test
    void controlCenterDoesNotMutateStandaloneAdminPages() {
        try (Playwright playwright = Playwright.create(); Browser browser = playwright.chromium().launch()) {
            Page page = browser.newPage();
            page.setContent("""
                    <body class="admin-solid-ui">
                      <header class="guide-admin-top">攻略后台</header>
                      <main class="guide-admin-main"><section>攻略内容</section></main>
                    </body>
                    """);
            page.addScriptTag(new Page.AddScriptTagOptions().setPath(ASSETS.resolve("admin-control-center-v2.js")));

            assertTrue((Boolean) page.evaluate("() => document.body.classList.contains('admin-solid-ui')"));
            assertEquals(Boolean.FALSE, page.evaluate("() => document.body.classList.contains('admin-control-center-v2')"));
            assertEquals(0, page.locator("#adminV2Topbar").count());
            assertEquals(0, page.locator("link[data-admin-control-center-v2]").count());
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
    void completeWhiteThemeOverridesLegacyNestedDarkSurfaces() {
        try (Playwright playwright = Playwright.create(); Browser browser = playwright.chromium().launch()) {
            Page page = browser.newPage();
            page.setContent("""
                    <body class="admin-control-center-v2">
                      <main class="admin-main">
                        <section data-panel="dashboard">
                          <div class="game-kpi">游戏指标</div>
                          <div class="game-library-panel">游戏资料</div>
                          <div class="admin-contact-channel">客服配置</div>
                          <div class="tool-image-preview">图片预览</div>
                          <div class="tracking-funnel">转化漏斗</div>
                          <label class="toggle-row">开关</label>
                          <div class="game-profile-modal"><div class="modal-head">资料弹窗标题</div></div>
                        </section>
                      </main>
                    </body>
                    """);
            page.addStyleTag(new Page.AddStyleTagOptions().setPath(ASSETS.resolve("app.css")));
            page.addStyleTag(new Page.AddStyleTagOptions().setPath(ASSETS.resolve("admin-control-center-v2.css")));
            page.addStyleTag(new Page.AddStyleTagOptions().setPath(ASSETS.resolve("admin-light-theme.css")));

            assertEquals("rgb(255, 255, 255)", page.evaluate("() => getComputedStyle(document.querySelector('.game-kpi')).backgroundColor"));
            assertEquals("rgb(255, 255, 255)", page.evaluate("() => getComputedStyle(document.querySelector('.game-library-panel')).backgroundColor"));
            assertEquals("rgb(255, 255, 255)", page.evaluate("() => getComputedStyle(document.querySelector('.admin-contact-channel')).backgroundColor"));
            assertEquals("rgb(248, 250, 252)", page.evaluate("() => getComputedStyle(document.querySelector('.tool-image-preview')).backgroundColor"));
            assertEquals("rgb(255, 255, 255)", page.evaluate("() => getComputedStyle(document.querySelector('.tracking-funnel')).backgroundColor"));
            assertEquals("rgb(255, 255, 255)", page.evaluate("() => getComputedStyle(document.querySelector('.toggle-row')).backgroundColor"));
            assertEquals("rgb(255, 255, 255)", page.evaluate("() => getComputedStyle(document.querySelector('.modal-head')).backgroundColor"));
            assertEquals("rgba(0, 0, 0, 0)", page.evaluate("() => getComputedStyle(document.querySelector('[data-panel=dashboard]')).backgroundColor"));
            assertEquals("none", page.evaluate("() => getComputedStyle(document.body, '::after').display"));
        }
    }

    @Test
    void wideDesktopUsesOperationalCanvasInsteadOfNarrowTemplateColumn() {
        try (Playwright playwright = Playwright.create(); Browser browser = playwright.chromium().launch()) {
            Page page = browser.newPage();
            page.setViewportSize(1920, 1080);
            page.setContent("""
                    <body class="admin-control-center-v2">
                      <div class="admin-shell-with-rail">
                        <aside class="sidebar"><div class="admin-brand"><b class="brand">星火后台</b></div></aside>
                        <main class="admin-main">
                          <section data-panel="dashboard">
                            <div class="admin-toolbar"><div><span class="eyebrow">运营数据</span><h1>今日概览</h1></div></div>
                            <div class="kpis"><div class="kpi"><b>12</b></div><div class="kpi"><b>8</b></div><div class="kpi"><b>3</b></div><div class="kpi"><b>1</b></div></div>
                          </section>
                        </main>
                        <aside class="admin-right-rail"><div class="right-rail-card">快捷入口</div></aside>
                      </div>
                    </body>
                    """);
            page.addStyleTag(new Page.AddStyleTagOptions().setPath(ASSETS.resolve("admin-control-center-v2.css")));

            Number panelWidth = (Number) page.evaluate("() => document.querySelector('[data-panel=dashboard]').getBoundingClientRect().width");
            assertTrue(panelWidth.doubleValue() > 1200.0);
            assertEquals("rgb(255, 255, 255)", page.evaluate("() => getComputedStyle(document.querySelector('.sidebar')).backgroundColor"));
            assertEquals("rgba(0, 0, 0, 0)", page.evaluate("() => getComputedStyle(document.querySelector('.admin-toolbar')).backgroundColor"));
            assertEquals("3px", page.evaluate("() => getComputedStyle(document.querySelector('.kpi'), '::before').width"));
        }
    }

    @Test
    void frontendControlCenterUsesLightAdminSurface() {
        try (Playwright playwright = Playwright.create(); Browser browser = playwright.chromium().launch()) {
            Page page = browser.newPage();
            page.setContent("""
                    <body class="frontend-control-admin">
                      <header class="fc-topbar"><div class="fc-topbar-inner">顶部</div></header>
                      <main class="fc-shell">
                        <section class="fc-card"><div class="field"><label>字段</label><input value="内容"></div></section>
                      </main>
                    </body>
                    """);
            page.addStyleTag(new Page.AddStyleTagOptions().setPath(ASSETS.resolve("admin-frontend-controls.css")));

            assertEquals("rgb(244, 246, 248)", page.evaluate("() => getComputedStyle(document.body).backgroundColor"));
            assertEquals("rgb(255, 255, 255)", page.evaluate("() => getComputedStyle(document.querySelector('.fc-card')).backgroundColor"));
            assertEquals("rgb(255, 255, 255)", page.evaluate("() => getComputedStyle(document.querySelector('.fc-card input')).backgroundColor"));
        }
    }

    @Test
    void resilienceLoadsControlCenterAssetsAndWhiteThemeStaysAdminScoped() throws Exception {
        String resilience = Files.readString(ASSETS.resolve("admin-resilience.js"));
        String css = Files.readString(ASSETS.resolve("admin-control-center-v2.css"));
        String light = Files.readString(ASSETS.resolve("admin-light-theme.css"));
        assertTrue(resilience.contains("/assets/admin-control-center-v2.js?v=20261007a004"));
        assertTrue(resilience.contains("/assets/admin-control-center-v2.css?v=20261007a005"));
        assertTrue(resilience.contains("/assets/admin-light-theme.css?v=20261007l003"));
        assertTrue(resilience.contains("restoreVisiblePanel"));
        assertTrue(resilience.contains("hasControlCenter"));
        assertTrue(css.contains("body.admin-control-center-v2 .admin-shell-with-rail"));
        assertTrue(css.contains("body.admin-control-center-v2 .table-wrap"));
        assertTrue(css.contains("width:min(100%,1680px)"));
        assertTrue(light.contains("body.admin-control-center-v2"));
        assertTrue(light.contains("--ui-bg:#f4f6f8"));
        assertTrue(light.contains("body.admin-control-center-v2 .game-kpi"));
        assertTrue(light.contains("body.admin-control-center-v2 .tool-image-preview"));
        assertTrue(light.contains("background:#f4f6f8!important"));
        assertTrue(light.contains("body.admin-login-ui"));
    }
}
