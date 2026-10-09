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
class FrontendResponsivePlaywrightTest {
    private static final Path ASSETS = Path.of("src/main/resources/static/assets");
    private static final Path SITE_FRAGMENT = Path.of("src/main/resources/templates/fragments/site.html");

    @Test
    void wideDesktopUsesNovaOperationalCanvasWithoutLegacyGlass() {
        try (Playwright playwright = Playwright.create(); Browser browser = playwright.chromium().launch()) {
            Page page = browser.newPage(new Browser.NewPageOptions().setViewportSize(1920, 1080));
            renderNovaShell(page);
            Number width = number(page, "document.querySelector('.nv-home-hero').getBoundingClientRect().width");
            assertTrue(width.doubleValue() >= 1120.0, () -> "Nova desktop canvas collapsed to " + width + "px");
            assertNoHorizontalOverflow(page, "Chromium 1920 desktop");
            assertEquals("grid", string(page, "getComputedStyle(document.querySelector('.nv-home-hero')).display"));
            assertEquals("none", string(page, "getComputedStyle(document.querySelector('.nv-mobile-dock')).display"));
        }
    }

    @Test
    void chromiumAndroidWechatAndNarrowWindowsStayTouchSafe() {
        try (Playwright playwright = Playwright.create(); Browser browser = playwright.chromium().launch()) {
            assertMobileContract(browser, 412, 915,
                    "Mozilla/5.0 (Linux; Android 16; Pixel) AppleWebKit/537.36 Chrome/141 Mobile Safari/537.36",
                    "Android Chrome 412");
            assertMobileContract(browser, 375, 812,
                    "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 MicroMessenger/8.0.60 Mobile Safari/537.36",
                    "Android WeChat 375");
            assertMobileContract(browser, 320, 700,
                    "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 Chrome/141 Mobile Safari/537.36",
                    "Android narrow 320");
        }
    }

    @Test
    void webkitIphoneAndMacSafariStayTouchSafeAndWide() {
        try (Playwright playwright = Playwright.create(); Browser browser = playwright.webkit().launch()) {
            assertMobileContract(browser, 390, 844,
                    "Mozilla/5.0 (iPhone; CPU iPhone OS 19_0 like Mac OS X) AppleWebKit/605.1.15 Version/19.0 Mobile/15E148 Safari/604.1",
                    "iPhone Safari 390");

            Page desktop = browser.newPage(new Browser.NewPageOptions().setViewportSize(1440, 900));
            renderNovaShell(desktop);
            assertNoHorizontalOverflow(desktop, "macOS Safari 1440");
            Number width = number(desktop, "document.querySelector('.nv-home-hero').getBoundingClientRect().width");
            assertTrue(width.doubleValue() >= 1100.0,
                    () -> "Safari desktop collapsed into a narrow canvas: " + width + "px");
        }
    }

    @Test
    void novaLayersAreAuthoritativeAndRuntimeControlsRemainAfterShell() throws Exception {
        String core = Files.readString(ASSETS.resolve("nova-ui.css"));
        String pages = Files.readString(ASSETS.resolve("nova-pages.css"));
        String responsive = Files.readString(ASSETS.resolve("nova-responsive.css"));
        String fragment = Files.readString(SITE_FRAGMENT);

        assertTrue(core.contains("--nv-accent:#ff7a1a"));
        assertTrue(core.contains("env(safe-area-inset-bottom"));
        assertTrue(core.contains("@media(prefers-reduced-motion:reduce)"));
        assertTrue(core.contains("input,select,textarea{font-size:16px"));
        assertTrue(pages.contains(".service-detail-page"));
        assertTrue(pages.contains(".guide-grid"));
        assertTrue(responsive.contains("min-height: 180px"));
        assertTrue(responsive.contains("aspect-ratio: 16 / 9"));
        assertTrue(fragment.contains("data-shell=\"nova\""));
        assertTrue(fragment.contains("public-field-sanitizer.js"));
        assertTrue(fragment.contains("frontend-controls-runtime.js"));
        assertTrue(!fragment.contains("site-experience-v2.css"));
        assertTrue(!fragment.contains("site-shell.js"));
    }

    private static void assertMobileContract(Browser browser, int width, int height, String userAgent, String label) {
        Page page = browser.newPage(new Browser.NewPageOptions().setViewportSize(width, height).setUserAgent(userAgent));
        renderNovaShell(page);
        assertNoHorizontalOverflow(page, label);
        assertEquals("16px", string(page, "getComputedStyle(document.querySelector('input')).fontSize"), label + " form zoom guard");
        assertEquals("none", string(page, "getComputedStyle(document.querySelector('.nv-nav')).display"), label + " desktop nav hidden");
        assertEquals("grid", string(page, "getComputedStyle(document.querySelector('.nv-mobile-dock')).display"), label + " mobile dock visible");
        assertEquals(5, number(page, "document.querySelectorAll('.nv-mobile-dock > a').length").intValue());
        Number dockHeight = number(page, "document.querySelector('.nv-mobile-dock > a').getBoundingClientRect().height");
        assertTrue(dockHeight.doubleValue() >= 44.0, () -> label + " dock target only " + dockHeight + "px");
        Number primaryHeight = number(page, "document.querySelector('.nv-btn--primary').getBoundingClientRect().height");
        assertTrue(primaryHeight.doubleValue() >= 44.0, () -> label + " primary target only " + primaryHeight + "px");
        Number mediaHeight = number(page, "document.querySelector('.nv-home-media').getBoundingClientRect().height");
        assertTrue(mediaHeight.doubleValue() >= 180.0, () -> label + " hero media collapsed to " + mediaHeight + "px");
        page.close();
    }

    private static void assertNoHorizontalOverflow(Page page, String label) {
        Number scrollWidth = number(page, "document.documentElement.scrollWidth");
        Number clientWidth = number(page, "document.documentElement.clientWidth");
        assertTrue(scrollWidth.doubleValue() <= clientWidth.doubleValue() + 1,
                () -> label + " overflowed horizontally: scrollWidth=" + scrollWidth + ", clientWidth=" + clientWidth);
    }

    private static Number number(Page page, String expression) { return (Number) page.evaluate("() => " + expression); }
    private static String string(Page page, String expression) { return String.valueOf(page.evaluate("() => " + expression)); }

    private static void renderNovaShell(Page page) {
        page.setContent("""
                <body data-page="home">
                  <header class="nv-header"><div class="nv-topbar"><a class="nv-brand"><span><strong>星火网游</strong><small>GAME SERVICE</small></span></a><nav class="nv-nav"><a>服务</a><a>工具</a><a>攻略</a></nav><button class="nv-menu-button"><span></span><span></span><span></span></button></div></header>
                  <main class="nv-home">
                    <section class="nv-home-hero"><div class="nv-home-copy"><span class="nv-eyebrow">CALL OF DUTY · MW4</span><h1>任务服务，先把目标说清楚。</h1><p>选择游戏与目标，获取统一业务编号。</p><div class="nv-home-actions"><a class="nv-btn nv-btn--primary">提前询价</a><a class="nv-btn nv-btn--ghost">服务大厅</a></div></div><aside class="nv-home-media"><div class="nv-home-art"></div></aside></section>
                    <div class="nv-home-grid"><section class="nv-panel"><div class="nv-task-grid"><a class="nv-task">武器升级</a><a class="nv-task">迷彩任务</a><a class="nv-task">活动任务</a><a class="nv-task">自定义</a></div></section><section class="nv-panel"><form><input value="微信号"><select><option>Steam</option></select></form></section></div>
                  </main>
                  <nav class="nv-mobile-dock"><a>首页</a><a>服务</a><a class="nv-dock-primary">询价</a><a>进度</a><a>我的</a></nav>
                </body>
                """);
        page.addStyleTag(new Page.AddStyleTagOptions().setPath(ASSETS.resolve("nova-ui.css")));
        page.addStyleTag(new Page.AddStyleTagOptions().setPath(ASSETS.resolve("nova-pages.css")));
        page.addStyleTag(new Page.AddStyleTagOptions().setPath(ASSETS.resolve("nova-responsive.css")));
    }
}
