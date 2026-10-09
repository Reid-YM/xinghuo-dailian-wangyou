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
    void chromiumPhoneWidthsKeepHeroAsOneFullBleedArtworkWithFloatingCopyAndCta() {
        try (Playwright playwright = Playwright.create(); Browser browser = playwright.chromium().launch()) {
            verifyWidth(browser, 390, 844, 395.0, 401.0);
            verifyWidth(browser, 430, 932, 436.0, 441.0);
        }
    }

    private static void verifyWidth(Browser browser, int width, int height, double minHero, double maxHero) {
        Page page = browser.newPage(new Browser.NewPageOptions().setViewportSize(width, height));
        page.setContent(markup());
        addStyles(page);

        assertEquals("none", string(page, "getComputedStyle(document.querySelector('.mobile-hero-backdrop')).display"));
        assertEquals("none", string(page, "getComputedStyle(document.querySelector('#homeHero'),'::after').display"));
        assertEquals("none", string(page, "getComputedStyle(document.querySelector('#heroBackground')).filter"));

        double heroTop = number(page, "document.querySelector('#homeHero').getBoundingClientRect().top").doubleValue();
        double heroBottom = number(page, "document.querySelector('#homeHero').getBoundingClientRect().bottom").doubleValue();
        double heroHeight = heroBottom - heroTop;
        assertTrue(heroHeight >= minHero && heroHeight <= maxHero,
            () -> width + "px full-bleed hero height out of range: " + heroHeight);

        double artworkTop = number(page, "document.querySelector('#heroBackground').getBoundingClientRect().top").doubleValue();
        double artworkBottom = number(page, "document.querySelector('#heroBackground').getBoundingClientRect().bottom").doubleValue();
        assertTrue(Math.abs(artworkTop - heroTop) <= 1.0,
            () -> width + "px artwork should begin at hero top: " + (artworkTop - heroTop));
        assertTrue(Math.abs(artworkBottom - heroBottom) <= 1.0,
            () -> width + "px artwork should fill hero bottom: " + (heroBottom - artworkBottom));

        double appbarTop = number(page, "document.querySelector('.xh-v4-appbar').getBoundingClientRect().top").doubleValue();
        double appbarBottom = number(page, "document.querySelector('.xh-v4-appbar').getBoundingClientRect().bottom").doubleValue();
        assertTrue(Math.abs(appbarTop - heroTop) <= 1.0,
            () -> width + "px floating app bar must start inside artwork");
        assertTrue(appbarBottom - appbarTop >= 56.0 && appbarBottom - appbarTop <= 60.0,
            () -> width + "px floating app bar height is wrong: " + (appbarBottom - appbarTop));

        double statusTop = number(page, "document.querySelector('.xh-v4-status').getBoundingClientRect().top").doubleValue();
        assertTrue(statusTop >= appbarBottom + 8.0 && statusTop <= appbarBottom + 12.0,
            () -> width + "px status chip should float just below app bar: " + (statusTop - appbarBottom));

        double titleTop = number(page, "document.querySelector('#mobileHeroTitle').getBoundingClientRect().top").doubleValue();
        double titleBottom = number(page, "document.querySelector('#mobileHeroTitle').getBoundingClientRect().bottom").doubleValue();
        assertTrue(titleTop > heroTop && titleBottom < heroBottom,
            () -> width + "px title must stay over the artwork");

        double subtitleBottom = number(page, "document.querySelector('.xh-v4-subtitle').getBoundingClientRect().bottom").doubleValue();
        double actionsTop = number(page, "document.querySelector('.xh-v4-hero-actions').getBoundingClientRect().top").doubleValue();
        double actionsBottom = number(page, "document.querySelector('.xh-v4-hero-actions').getBoundingClientRect().bottom").doubleValue();
        assertTrue(actionsTop > titleBottom,
            () -> width + "px CTA should sit below the floating title");
        assertTrue(actionsTop - subtitleBottom >= 23.0 && actionsTop - subtitleBottom <= 29.0,
            () -> width + "px subtitle-to-CTA breathing room is wrong: " + (actionsTop - subtitleBottom));
        assertTrue(heroBottom - actionsBottom >= 13.0 && heroBottom - actionsBottom <= 15.0,
            () -> width + "px CTA must float inside artwork bottom edge: " + (heroBottom - actionsBottom));

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
            "mobile-home-hero-r2.css"
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
