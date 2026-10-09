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
    void chromiumPhoneWidthsKeepArtworkAndCtaTightlyCoupled() {
        try (Playwright playwright = Playwright.create(); Browser browser = playwright.chromium().launch()) {
            verifyWidth(browser, 390, 844, 395.0, 405.0);
            verifyWidth(browser, 430, 932, 417.0, 427.0);
        }
    }

    private static void verifyWidth(Browser browser, int width, int height, double minHero, double maxHero) {
        Page page = browser.newPage(new Browser.NewPageOptions().setViewportSize(width, height));
        page.setContent(markup());
        addStyles(page);

        assertEquals("none", string(page, "getComputedStyle(document.querySelector('.mobile-hero-backdrop')).display"));
        assertEquals("none", string(page, "getComputedStyle(document.querySelector('#homeHero'),'::after').display"));

        double heroHeight = number(page, "document.querySelector('#homeHero').getBoundingClientRect().height").doubleValue();
        assertTrue(heroHeight >= minHero && heroHeight <= maxHero,
            () -> width + "px homepage hero height out of compact range: " + heroHeight);

        double heroTop = number(page, "document.querySelector('#homeHero').getBoundingClientRect().top").doubleValue();
        double artworkTop = number(page, "document.querySelector('#heroBackground').getBoundingClientRect().top").doubleValue();
        double artworkHeight = number(page, "document.querySelector('#heroBackground').getBoundingClientRect().height").doubleValue();
        assertTrue(artworkTop - heroTop >= 103.0 && artworkTop - heroTop <= 113.0,
            () -> width + "px artwork top should follow the compact header: " + (artworkTop - heroTop));
        assertTrue(Math.abs(artworkHeight - width * 0.5625) <= 1.5,
            () -> width + "px artwork should keep a 16:9 slot: " + artworkHeight);

        double actionsTop = number(page, "document.querySelector('.xh-v4-hero-actions').getBoundingClientRect().top").doubleValue();
        double artworkBottom = number(page, "document.querySelector('#heroBackground').getBoundingClientRect().bottom").doubleValue();
        double artworkToCtaGap = actionsTop - artworkBottom;
        assertTrue(artworkToCtaGap >= 10.0 && artworkToCtaGap <= 16.0,
            () -> width + "px artwork-to-CTA gap is too large: " + artworkToCtaGap);

        double actionsBottom = number(page, "document.querySelector('.xh-v4-hero-actions').getBoundingClientRect().bottom").doubleValue();
        double heroBottom = number(page, "document.querySelector('#homeHero').getBoundingClientRect().bottom").doubleValue();
        assertTrue(heroBottom - actionsBottom >= 10.0 && heroBottom - actionsBottom <= 13.0,
            () -> width + "px CTA bottom gap is wrong: " + (heroBottom - actionsBottom));

        double appbarHeight = number(page, "document.querySelector('.xh-v4-appbar').getBoundingClientRect().height").doubleValue();
        assertTrue(appbarHeight <= 53.0, () -> width + "px appbar shell is too tall: " + appbarHeight);

        double statusTop = number(page, "document.querySelector('.xh-v4-status').getBoundingClientRect().top").doubleValue();
        assertTrue(statusTop - heroTop <= 66.0,
            () -> width + "px status is leaving an oversized top gap: " + (statusTop - heroTop));

        double supportWidth = number(page, "document.querySelector('.floating-customer-service').getBoundingClientRect().width").doubleValue();
        assertTrue(supportWidth <= 42.5, () -> "floating support should remain compact: " + supportWidth);
        assertEquals("none", string(page, "getComputedStyle(document.querySelector('.floating-customer-service-text')).display"));

        assertNoOverflow(page);
        page.close();
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
                    <div class="xh-v4-meta"><div class="xh-v4-price"><small>参考起步</small><strong>¥20 起</strong><span>人工确认为准</span></div><div class="xh-v4-trust"><span>可靠手打</span><span>进度可查</span></div></div>
                    <div class="actions hero-actions xh-v4-hero-actions"><a class="btn primary xh-v4-primary">提前询价</a><a class="btn ghost xh-v4-progress">查看服务大厅</a></div>
                  </div>
                </section>
              </main>
              <a class="floating-customer-service"><span class="floating-customer-service-icon"></span><span class="floating-customer-service-text">联系客服</span></a>
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
