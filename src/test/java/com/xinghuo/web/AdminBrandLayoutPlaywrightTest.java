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

class AdminBrandLayoutPlaywrightTest {
    private static final Path STATIC = Path.of("src/main/resources/static");
    private static final Path ASSETS = STATIC.resolve("assets");
    private static final Path TEMPLATES = Path.of("src/main/resources/templates");

    @Test
    void backendSurfacesLoadTheSharedBrandFixLayerAndOfficialFavicon() throws Exception {
        String resilience = Files.readString(ASSETS.resolve("admin-resilience.js"));
        String frontendControls = Files.readString(ASSETS.resolve("admin-frontend-controls.js"));
        String login = Files.readString(TEMPLATES.resolve("admin-login.html"));
        String guides = Files.readString(TEMPLATES.resolve("admin-guides.html"));
        String fixes = Files.readString(ASSETS.resolve("admin-brand-layout-fixes.css"));
        String rootFavicon = Files.readString(STATIC.resolve("favicon.svg"));
        String brandLogo = Files.readString(ASSETS.resolve("brand-logo.svg"));

        assertTrue(resilience.contains("/assets/admin-brand-layout-fixes.css?v=20261008b002"));
        assertTrue(frontendControls.contains("/assets/admin-brand-layout-fixes.css?v=20261008b002"));
        assertTrue(login.contains("/assets/admin-brand-layout-fixes.css?v=20261008b002"));
        assertTrue(guides.contains("/assets/admin-brand-layout-fixes.css?v=20261008b002"));
        assertTrue(resilience.contains("/assets/brand-logo.svg?v=20261008logo02"));
        assertTrue(frontendControls.contains("/assets/brand-logo.svg?v=20261008logo02"));
        assertTrue(login.contains("/assets/brand-logo.svg?v=20261008logo02"));
        assertTrue(guides.contains("/assets/brand-logo.svg?v=20261008logo02"));
        assertTrue(fixes.contains("url('/assets/brand-logo.svg')"));
        assertEquals(brandLogo.strip(), rootFavicon.strip());
        assertTrue(Files.exists(STATIC.resolve("favicon.ico")));
        assertTrue(Files.exists(STATIC.resolve("apple-touch-icon.png")));
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "PLAYWRIGHT_UI_TESTS", matches = "1")
    void mainAdminBrandTextIsNotClippedAndNarrowTopbarDoesNotOverflow() {
        try (Playwright playwright = Playwright.create(); Browser browser = playwright.chromium().launch()) {
            Page page = browser.newPage();
            page.setViewportSize(1280, 844);
            page.setContent("""
                    <body class="admin-control-center-v2">
                      <div class="admin-shell-with-rail">
                        <aside class="sidebar">
                          <div class="admin-brand">
                            <b class="brand brand-mark"><span class="brand-dot"></span><span data-admin-brand>星火网游代练平台</span></b>
                            <div class="muted small">admin</div>
                          </div>
                        </aside>
                        <main class="admin-main">
                          <div class="admin-v2-topbar">
                            <div class="admin-v2-context">
                              <button class="btn ghost admin-v2-menu" type="button">菜单</button>
                              <span class="admin-v2-context-mark">X</span>
                              <div><small>星火后台控制中心</small><strong>这是一个很长的后台模块标题用于验证窄屏不会错位</strong></div>
                            </div>
                            <div class="admin-v2-actions">
                              <button class="btn ghost admin-v2-dashboard-shortcut">回到总览</button>
                              <a class="btn ghost" href="/admin/frontend-controls">前端控制</a>
                              <a class="btn ghost" href="/">查看前台</a>
                            </div>
                          </div>
                          <section data-panel="dashboard">
                            <div class="admin-toolbar"><h1>今日概览</h1><div class="row-actions"><button>动作一</button><button>动作二</button><button>动作三</button></div></div>
                          </section>
                        </main>
                      </div>
                    </body>
                    """);
            page.addStyleTag(new Page.AddStyleTagOptions().setPath(ASSETS.resolve("app.css")));
            page.addStyleTag(new Page.AddStyleTagOptions().setPath(ASSETS.resolve("admin-control-center-v2.css")));
            page.addStyleTag(new Page.AddStyleTagOptions().setPath(ASSETS.resolve("admin-brand-layout-fixes.css")));

            assertTrue(((String) page.evaluate("() => getComputedStyle(document.querySelector('.admin-v2-context-mark')).backgroundImage")).contains("brand-logo.svg"));
            assertTrue(((String) page.evaluate("() => getComputedStyle(document.querySelector('.brand-dot')).backgroundImage")).contains("brand-logo.svg"));
            assertTrue((Boolean) page.evaluate("""
                    () => {
                      const card = document.querySelector('.admin-brand').getBoundingClientRect();
                      const label = document.querySelector('[data-admin-brand]').getBoundingClientRect();
                      return card.height >= 90
                        && label.height >= 16
                        && label.top >= card.top - 1
                        && label.bottom <= card.bottom + 1;
                    }
                    """));

            page.setViewportSize(390, 844);
            assertEquals("none", page.evaluate("() => getComputedStyle(document.querySelector('.admin-v2-actions a[href=\"/\"]')).display"));
            assertEquals("none", page.evaluate("() => getComputedStyle(document.querySelector('.admin-v2-actions a[href=\"/admin/frontend-controls\"]')).display"));
            assertTrue((Boolean) page.evaluate("() => document.documentElement.scrollWidth <= innerWidth + 1"));

            page.setViewportSize(640, 844);
            assertEquals("none", page.evaluate("() => getComputedStyle(document.querySelector('.admin-v2-actions a[href=\"/\"]')).display"));
            assertTrue((Boolean) page.evaluate("() => getComputedStyle(document.querySelector('.admin-v2-actions a[href=\"/admin/frontend-controls\"]')).display !== 'none'"));
            assertTrue((Boolean) page.evaluate("() => document.documentElement.scrollWidth <= innerWidth + 1"));
        }
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "PLAYWRIGHT_UI_TESTS", matches = "1")
    void frontendControlHeaderUsesSharedLogoAndFitsPhoneWidth() {
        try (Playwright playwright = Playwright.create(); Browser browser = playwright.chromium().launch()) {
            Page page = browser.newPage();
            page.setViewportSize(390, 844);
            page.setContent("""
                    <body class="frontend-control-admin">
                      <header class="fc-topbar">
                        <div class="fc-topbar-inner">
                          <div class="fc-brand"><span class="fc-brand-mark">X</span><div><small>星火后台</small><strong>前端控制中心</strong></div></div>
                          <div class="fc-top-actions"><span>admin</span><a class="btn ghost" href="/admin">返回运营后台</a><a class="btn primary" href="/">预览前台</a></div>
                        </div>
                      </header>
                    </body>
                    """);
            page.addStyleTag(new Page.AddStyleTagOptions().setPath(ASSETS.resolve("admin-frontend-controls.css")));
            page.addStyleTag(new Page.AddStyleTagOptions().setPath(ASSETS.resolve("admin-brand-layout-fixes.css")));

            assertTrue(((String) page.evaluate("() => getComputedStyle(document.querySelector('.fc-brand-mark')).backgroundImage")).contains("brand-logo.svg"));
            assertEquals("none", page.evaluate("() => getComputedStyle(document.querySelector('.fc-top-actions a[href=\"/\"]')).display"));
            assertTrue((Boolean) page.evaluate("() => document.documentElement.scrollWidth <= innerWidth + 1"));
        }
    }
}
