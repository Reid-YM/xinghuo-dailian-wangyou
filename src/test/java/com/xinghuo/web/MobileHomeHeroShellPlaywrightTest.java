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
class MobileHomeHeroShellPlaywrightTest {
    private static final Path ASSETS = Path.of("src/main/resources/static/assets");

    @Test
    void chromium390RemovesBlurFillerAndKeepsHeroCompact() {
        try (Playwright playwright = Playwright.create(); Browser browser = playwright.chromium().launch()) {
            Page page = browser.newPage(new Browser.NewPageOptions().setViewportSize(390, 844));
            page.setContent(markup());
            addStyles(page);

            assertEquals("none", string(page, "getComputedStyle(document.querySelector('.mobile-hero-backdrop')).display"));

            double heroHeight = number(page, "document.querySelector('#homeHero').getBoundingClientRect().height").doubleValue();
            assertTrue(heroHeight >= 399.0 && heroHeight <= 420.0,
                () -> "390px homepage hero should stay compact, got " + heroHeight);

            double appbarHeight = number(page, "document.querySelector('.xh-v4-appbar').getBoundingClientRect().height").doubleValue();
            assertTrue(appbarHeight <= 56.0, () -> "appbar shell is too tall: " + appbarHeight);

            double statusTop = number(page, "document.querySelector('.xh-v4-status').getBoundingClientRect().top").doubleValue();
            double heroTop = number(page, "document.querySelector('#homeHero').getBoundingClientRect().top").doubleValue();
            assertTrue(statusTop - heroTop <= 72.0, () -> "status is leaving an oversized top gap: " + (statusTop - heroTop));

            double actionsBottom = number(page, "document.querySelector('.xh-v4-hero-actions').getBoundingClientRect().bottom").doubleValue();
            double heroBottom = number(page, "document.querySelector('#homeHero').getBoundingClientRect().bottom").doubleValue();
            assertTrue(heroBottom - actionsBottom <= 16.0,
                () -> "CTA shell leaves too much empty space below: " + (heroBottom - actionsBottom));

            assertNoOverflow(page);
        }
    }

    private static String markup() {
        return """
            <!doctype html>
            <html lang="zh-CN" data-frontend="mobile">
            <body class="mobile-frontend" data-page="home" data-frontend="mobile">
              <main class="xh-v4">
                <section class="xh-v4-hero hero" id="homeHero">
                  <div class="mobile-hero-backdrop"></div>
                  <div id="heroBackground" class="hero-bg-layer"></div>
                  <div class="xh-v4-hero-shade"></div>
                  <nav class="xh-v4-appbar">
                    <a class="xh-v4-brand"><span class="xh-v4-logo">火</span><span class="xh-v4-brand-text"><strong>星火网游</strong><small>GAME SERVICE</small></span></a>
                    <div class="xh-v4-appbar-actions"><a>客服</a><a>我的</a></div>
                  </nav>
                  <div class="xh-v4-hero-copy hero-copy">
                    <div class="xh-v4-status">● MW4 正式版预约中</div>
                    <div class="xh-v4-eyebrow">CALL OF DUTY · MW4</div>
                    <h1 id="mobileHeroTitle">使命召唤 MW4 任务代练</h1>
                    <p class="xh-v4-subtitle">武器升级 · 迷彩解锁 · DMZ · 活动任务</p>
                    <div class="xh-v4-meta"><div class="xh-v4-price"><small>参考起步</small><strong>¥20 起</strong></div></div>
                    <div class="actions hero-actions xh-v4-hero-actions"><a class="btn primary xh-v4-primary">提前询价</a><a class="btn ghost xh-v4-progress">查看服务大厅</a></div>
                  </div>
                </section>
              </main>
            </body>
            </html>
            """;
    }

    private static void addStyles(Page page) {
        for (String file : new String[]{
            "app.css",
            "mobile-app.css",
            "mobile-home-v4.css",
            "mobile-tune-v1.css",
            "mobile-home-shell-v1.css"
        }) {
            page.addStyleTag(new Page.AddStyleTagOptions().setPath(ASSETS.resolve(file)));
        }
    }

    private static void assertNoOverflow(Page page) {
        double scroll = number(page, "document.documentElement.scrollWidth").doubleValue();
        double client = number(page, "document.documentElement.clientWidth").doubleValue();
        assertTrue(scroll <= client + 1.0,
            () -> "homepage hero overflowed horizontally: scroll=" + scroll + ", client=" + client);
    }

    private static Number number(Page page, String expression) {
        return (Number) page.evaluate("() => " + expression);
    }

    private static String string(Page page, String expression) {
        return String.valueOf(page.evaluate("() => " + expression));
    }
}
