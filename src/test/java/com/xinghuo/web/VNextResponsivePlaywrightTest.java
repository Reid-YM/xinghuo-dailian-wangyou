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
class VNextResponsivePlaywrightTest {
    private static final Path CSS = Path.of("frontend-vnext/src/styles.css");

    @Test
    void chromiumCoversNarrowWechatTabletAndUltraWide() throws Exception {
        try (Playwright playwright = Playwright.create(); Browser browser = playwright.chromium().launch()) {
            assertLayout(browser, 320, 700, true, false, "320 narrow");
            assertLayout(browser, 375, 812, true, true, "375 WeChat");
            assertLayout(browser, 768, 1024, false, false, "768 tablet");
            assertLayout(browser, 1440, 900, false, false, "1440 desktop");
            assertLayout(browser, 1920, 1080, false, false, "1920 desktop");
            assertLayout(browser, 2560, 1440, false, false, "2560 desktop");
        }
    }

    @Test
    void webkitCoversIphoneAndDesktopSafari() throws Exception {
        try (Playwright playwright = Playwright.create(); Browser browser = playwright.webkit().launch()) {
            assertLayout(browser, 390, 844, true, false, "390 iPhone Safari");
            assertLayout(browser, 430, 932, true, true, "430 iPhone WeChat");
            assertLayout(browser, 1536, 864, false, false, "1536 Safari desktop");
        }
    }

    private static void assertLayout(Browser browser, int width, int height, boolean mobile, boolean wechat, String label)
            throws Exception {
        String userAgent = wechat
                ? "Mozilla/5.0 (iPhone; CPU iPhone OS 19_0 like Mac OS X) AppleWebKit/605.1.15 MicroMessenger/8.0.60 Mobile/15E148"
                : mobile
                    ? "Mozilla/5.0 (iPhone; CPU iPhone OS 19_0 like Mac OS X) AppleWebKit/605.1.15 Version/19.0 Mobile/15E148 Safari/604.1"
                    : "Mozilla/5.0 AppleWebKit/537.36 Chrome/141 Safari/537.36";
        Page page = browser.newPage(new Browser.NewPageOptions().setViewportSize(width, height).setUserAgent(userAgent));
        page.setContent(fixture(wechat));
        String css = Files.readString(CSS).replace("@import \"tailwindcss\";", "");
        page.addStyleTag(new Page.AddStyleTagOptions().setContent(css));

        Number scrollWidth = (Number) page.evaluate("() => document.documentElement.scrollWidth");
        Number clientWidth = (Number) page.evaluate("() => document.documentElement.clientWidth");
        assertTrue(scrollWidth.doubleValue() <= clientWidth.doubleValue() + 1,
                () -> label + " horizontal overflow: " + scrollWidth + " > " + clientWidth);

        String dockDisplay = String.valueOf(page.evaluate("() => getComputedStyle(document.querySelector('.v-mobile-dock')).display"));
        assertEquals(mobile && width <= 767 ? "grid" : "none", dockDisplay, label + " mobile dock state");

        Number primaryHeight = (Number) page.evaluate("() => document.querySelector('.v-primary').getBoundingClientRect().height");
        assertTrue(primaryHeight.doubleValue() >= 44.0, () -> label + " primary CTA below touch minimum");

        String inputSize = String.valueOf(page.evaluate("() => getComputedStyle(document.querySelector('input')).fontSize"));
        if (mobile && width <= 767) {
            assertEquals("16px", inputSize, label + " input must avoid iOS focus zoom");
        }

        if (width >= 1920) {
            Number shellWidth = (Number) page.evaluate("() => document.querySelector('.v-shell').getBoundingClientRect().width");
            assertTrue(shellWidth.doubleValue() >= 1500.0,
                    () -> label + " collapsed into a legacy narrow desktop canvas: " + shellWidth);
        }
        page.close();
    }

    private static String fixture(boolean wechat) {
        return """
                <html class="%s"><body>
                  <div class="v-app">
                    <header class="v-header"><div class="v-shell v-header-inner"><button class="v-brand"><span class="v-brand-mark">XH</span></button><button class="v-button v-primary">提前询价</button></div></header>
                    <main>
                      <section class="v-hero"><div class="v-shell v-hero-grid"><div class="v-hero-copy"><div class="v-kicker">VNext</div><h1>跨端响应式重构</h1><p class="v-lead">手机、电脑与微信窗口保持同一业务能力。</p><div class="v-hero-actions"><button class="v-button v-primary">查看服务</button></div><input type="text" value="测试" /></div><div class="v-hero-panel"><div class="v-device-stack"><div class="v-device v-device-desktop"></div><div class="v-device v-device-phone"></div></div></div></div></section>
                      <section class="v-section v-shell"><div class="v-feature-grid"><article class="v-card">A</article><article class="v-card">B</article><article class="v-card">C</article><article class="v-card">D</article></div></section>
                    </main>
                    <nav class="v-mobile-dock"><button>首页</button><button>服务</button><button>询价</button><button>进度</button><button>我的</button></nav>
                  </div>
                </body></html>
                """.formatted(wechat ? "is-wechat is-mobile is-ios" : "");
    }
}
