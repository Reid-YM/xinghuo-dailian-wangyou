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
    void wideDesktopUsesOperationalCanvasWithoutHorizontalOverflow() {
        try (Playwright playwright = Playwright.create(); Browser browser = playwright.chromium().launch()) {
            Page page = browser.newPage(new Browser.NewPageOptions().setViewportSize(1920, 1080));
            renderPublicShell(page);

            Number containerWidth = (Number) page.evaluate("() => document.querySelector('main.container').getBoundingClientRect().width");
            assertTrue(containerWidth.doubleValue() >= 1380.0, "24-inch desktop should use the available canvas");
            assertEquals(Boolean.FALSE, page.evaluate("() => document.documentElement.scrollWidth > window.innerWidth"));
            assertEquals("none", page.evaluate("() => getComputedStyle(document.querySelector('.site-header')).backdropFilter"));
            assertEquals("4", page.evaluate("() => getComputedStyle(document.querySelector('.service-browser-grid')).gridTemplateColumns.split(' ').length.toString()"));
        }
    }

    @Test
    void chromiumAndroidAndWechatWindowsStayTouchSafeAndOverflowFree() {
        try (Playwright playwright = Playwright.create(); Browser browser = playwright.chromium().launch()) {
            assertMobileContract(browser, 412, 915,
                    "Mozilla/5.0 (Linux; Android 16; Pixel) AppleWebKit/537.36 Chrome/141 Mobile Safari/537.36");
            assertMobileContract(browser, 375, 812,
                    "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 MicroMessenger/8.0.60 Mobile Safari/537.36");
        }
    }

    @Test
    void webkitIphoneAndSafariWindowsStayTouchSafeAndOverflowFree() {
        try (Playwright playwright = Playwright.create(); Browser browser = playwright.webkit().launch()) {
            assertMobileContract(browser, 390, 844,
                    "Mozilla/5.0 (iPhone; CPU iPhone OS 19_0 like Mac OS X) AppleWebKit/605.1.15 Version/19.0 Mobile/15E148 Safari/604.1");

            Page desktop = browser.newPage(new Browser.NewPageOptions().setViewportSize(1440, 900));
            renderPublicShell(desktop);
            assertEquals(Boolean.FALSE, desktop.evaluate("() => document.documentElement.scrollWidth > window.innerWidth"));
            Number width = (Number) desktop.evaluate("() => document.querySelector('main.container').getBoundingClientRect().width");
            assertTrue(width.doubleValue() > 1200.0, "Safari desktop should not collapse into the legacy narrow canvas");
        }
    }

    @Test
    void experienceLayerKeepsRuntimeThemeControlsAndLoadsGlobally() throws Exception {
        String css = Files.readString(ASSETS.resolve("site-experience-v2.css"));
        String fragment = Files.readString(SITE_FRAGMENT);

        assertTrue(css.contains("--max:1440px"));
        assertTrue(css.contains("env(safe-area-inset-bottom"));
        assertTrue(css.contains("min-height:100dvh"));
        assertTrue(css.contains("input,select,textarea{font-size:16px!important}"));
        assertTrue(css.contains("@media(forced-colors:active)"));
        assertTrue(css.contains("@media(prefers-reduced-motion:reduce)"));
        assertTrue(css.contains("var(--accent)"));
        assertTrue(css.contains("var(--surface-2)"));

        int shell = fragment.indexOf("site-shell.css");
        int experience = fragment.indexOf("site-experience-v2.css");
        int runtime = fragment.indexOf("frontend-controls-runtime.js");
        assertTrue(shell >= 0 && experience > shell, "experience layer must load after the legacy shell");
        assertTrue(runtime > experience, "runtime frontend controls must remain able to override theme variables");
    }

    private static void assertMobileContract(Browser browser, int width, int height, String userAgent) {
        Page page = browser.newPage(new Browser.NewPageOptions()
                .setViewportSize(width, height)
                .setUserAgent(userAgent));
        renderPublicShell(page);

        assertEquals(Boolean.FALSE, page.evaluate("() => document.documentElement.scrollWidth > window.innerWidth"));
        assertEquals("16px", page.evaluate("() => getComputedStyle(document.querySelector('input')).fontSize"));
        Number buttonHeight = (Number) page.evaluate("() => document.querySelector('.mobile-dock a').getBoundingClientRect().height");
        assertTrue(buttonHeight.doubleValue() >= 44.0, "mobile quick actions must remain touch-safe");
        assertEquals("grid", page.evaluate("() => getComputedStyle(document.querySelector('.mobile-dock')).display"));
        assertEquals("none", page.evaluate("() => getComputedStyle(document.querySelector('.site-header')).backdropFilter"));
        page.close();
    }

    private static void renderPublicShell(Page page) {
        page.setContent("""
                <body data-page="home">
                  <header class="site-header">
                    <div class="container topbar">
                      <a class="brand brand-mark"><span class="brand-dot"></span><span>星火网游代练平台</span></a>
                      <nav class="nav"><a>服务</a><a>攻略</a><a>工具站</a><a class="btn primary desktop-keep">提前询价</a></nav>
                    </div>
                  </header>
                  <main class="container">
                    <section class="hero hero-r3">
                      <div class="hero-copy">
                        <div class="hero-signal"><span class="hero-kicker">可靠手打 · 纯绿服务 · 进度可查</span></div>
                        <h1>使命召唤 MW4 任务代练</h1>
                        <div class="hero-promise"><span><b>可靠手打</b><small>人工推进</small></span><span><b>纯绿服务</b><small>不使用外挂</small></span><span><b>进度可查</b><small>统一业务编号</small></span></div>
                        <div class="hero-actions"><a class="btn primary">提前询价</a><a class="btn ghost">服务大厅</a><a class="btn ghost hero-progress-link">查业务进度</a></div>
                      </div>
                      <aside class="hero-panel hero-media-panel"><div class="hero-visual"></div></aside>
                    </section>
                    <section class="service-browser-grid grid4">
                      <article class="service-card card">服务 A</article><article class="service-card card">服务 B</article>
                      <article class="service-card card">服务 C</article><article class="service-card card">服务 D</article>
                    </section>
                    <form><label>联系方式<input type="text" value="微信号"></label><select><option>Steam</option></select></form>
                  </main>
                  <nav class="mobile-dock"><a>首页</a><a>业务记录</a><a class="primary">提前询价</a></nav>
                  <footer class="site-footer"><div class="container footer-row">星火网游</div></footer>
                </body>
                """);
        page.addStyleTag(new Page.AddStyleTagOptions().setPath(ASSETS.resolve("app.css")));
        page.addStyleTag(new Page.AddStyleTagOptions().setPath(ASSETS.resolve("site-shell.css")));
        page.addStyleTag(new Page.AddStyleTagOptions().setPath(ASSETS.resolve("site-experience-v2.css")));
    }
}
