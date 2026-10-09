package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

@EnabledIfEnvironmentVariable(named = "PLAYWRIGHT_UI_TESTS", matches = "1")
class MobileFlatOverflowPlaywrightTest {
    private static final Path ASSETS = Path.of("src/main/resources/static/assets");

    @Test
    void chromium390RemovesGlassShellAndContainsWideContent() {
        try (Playwright playwright = Playwright.create(); Browser browser = playwright.chromium().launch()) {
            Page page = browser.newPage(new Browser.NewPageOptions().setViewportSize(390, 844));
            page.setContent(markup("services"));
            addLegacyConflictStyles(page);
            addMobileStyles(page);

            assertEquals("rgba(0, 0, 0, 0)", css(page, ".page-shell", "backgroundColor"));
            assertEquals("none", css(page, ".page-shell", "boxShadow"));
            assertEquals("none", normalizedBackdrop(page, ".page-shell"));
            assertEquals("rgb(16, 20, 26)", css(page, ".service-browser-console", "backgroundColor"));
            assertEquals("visible", css(page, ".service-browser-console", "overflow"));
            assertTrue((Boolean) page.evaluate("() => document.querySelector('.wide-card').getBoundingClientRect().right <= innerWidth + 0.5"));
            assertTrue((Boolean) page.evaluate("() => document.documentElement.scrollWidth <= innerWidth"));
        }
    }

    @Test
    void webkit360KeepsFilterRailScrollableWithoutClippingPage() {
        try (Playwright playwright = Playwright.create(); Browser browser = playwright.webkit().launch()) {
            Page page = browser.newPage(new Browser.NewPageOptions().setViewportSize(360, 780));
            page.setContent(markup("services"));
            addLegacyConflictStyles(page);
            addMobileStyles(page);

            String overflowX = css(page, ".filters", "overflowX");
            assertTrue(overflowX.equals("auto") || overflowX.equals("scroll"));
            assertTrue((Boolean) page.evaluate("() => { const el=document.querySelector('.filters'); return el.scrollWidth > el.clientWidth; }"));
            assertTrue((Boolean) page.evaluate("() => document.querySelector('.service-browser-console').getBoundingClientRect().right <= innerWidth + 0.5"));
            assertTrue((Boolean) page.evaluate("() => document.documentElement.scrollWidth <= innerWidth"));
        }
    }

    private static String markup(String pageName) {
        return """
                <!doctype html>
                <html data-frontend="mobile">
                <body class="mobile-frontend" data-page="__PAGE__">
                  <main class="container section page-shell services-browser">
                    <div class="page-hero page-hero-compact service-browser-hero">
                      <h1>手机前端布局</h1>
                    </div>
                    <section class="service-browser-console">
                      <div class="browser-row">
                        <div class="filters">
                          <button class="filter-btn">全部游戏</button>
                          <button class="filter-btn">使命召唤 MW4</button>
                          <button class="filter-btn">DMZ 任务与物资</button>
                          <button class="filter-btn">PlayStation / Xbox</button>
                          <button class="filter-btn">一个很长的筛选项目用于验证横向滚动</button>
                        </div>
                      </div>
                    </section>
                    <section class="section">
                      <article class="card wide-card" style="width:900px">
                        <h2>ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789</h2>
                        <img alt="test" style="width:1000px;height:60px" src="data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='1000' height='60'%3E%3Crect width='1000' height='60' fill='%2310141a'/%3E%3C/svg%3E">
                      </article>
                    </section>
                  </main>
                </body>
                </html>
                """.replace("__PAGE__", pageName);
    }

    private static void addLegacyConflictStyles(Page page) {
        page.addStyleTag(new Page.AddStyleTagOptions().setContent("""
                .page-shell,.page-hero,.service-browser-console,.card {
                  background: rgba(255,255,255,.08);
                  box-shadow: 0 20px 60px rgba(0,0,0,.5);
                  -webkit-backdrop-filter: blur(18px);
                  backdrop-filter: blur(18px);
                  overflow: hidden;
                }
                .filters { display:flex; flex-wrap:nowrap; width:620px; }
                .filter-btn { flex:0 0 auto; white-space:nowrap; }
                """));
    }

    private static void addMobileStyles(Page page) {
        page.addStyleTag(new Page.AddStyleTagOptions().setPath(ASSETS.resolve("mobile-app.css")));
        page.addStyleTag(new Page.AddStyleTagOptions().setPath(ASSETS.resolve("mobile-compact.css")));
        page.addStyleTag(new Page.AddStyleTagOptions().setPath(ASSETS.resolve("mobile-screenshot-fixes.css")));
        page.addStyleTag(new Page.AddStyleTagOptions().setPath(ASSETS.resolve("mobile-flat-overflow.css")));
    }

    private static String css(Page page, String selector, String property) {
        return (String) page.evaluate("([selector, property]) => getComputedStyle(document.querySelector(selector))[property]", new String[]{selector, property});
    }

    private static String normalizedBackdrop(Page page, String selector) {
        return (String) page.evaluate("selector => { const s=getComputedStyle(document.querySelector(selector)); return s.backdropFilter || s.webkitBackdropFilter || 'none'; }", selector);
    }
}
