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
    void chromiumPhoneWidthsKeepLandscapeKeyArtSeparateFromSolidActionDeck() {
        try (Playwright playwright = Playwright.create(); Browser browser = playwright.chromium().launch()) {
            verifyWidth(browser, 390, 844, 395.0, 400.0);
            verifyWidth(browser, 430, 932, 418.0, 422.0);
        }
    }

    private static void verifyWidth(Browser browser, int width, int height, double minHero, double maxHero) {
        Page page = browser.newPage(new Browser.NewPageOptions().setViewportSize(width, height));
        page.setContent(markup());
        addStyles(page);

        assertEquals("none", string(page, "getComputedStyle(document.querySelector('.mobile-hero-backdrop')).display"));
        assertEquals("none", string(page, "getComputedStyle(document.querySelector('#homeHero'),'::after').display"));
        assertEquals("none", string(page, "getComputedStyle(document.querySelector('#heroBackground')).filter"));
        assertEquals("none", string(page, "getComputedStyle(document.querySelector('.xh-v4-subtitle')).display"));
        assertEquals("none", string(page, "getComputedStyle(document.querySelector('.xh-v4-eyebrow')).display"));

        double heroTop = number(page, "document.querySelector('#homeHero').getBoundingClientRect().top").doubleValue();
        double heroBottom = number(page, "document.querySelector('#homeHero').getBoundingClientRect().bottom").doubleValue();
        double heroHeight = heroBottom - heroTop;
        assertTrue(heroHeight >= minHero && heroHeight <= maxHero,
            () -> width + "px compact Hero height out of range: " + heroHeight);

        double appbarTop = number(page, "document.querySelector('.xh-v4-appbar').getBoundingClientRect().top").doubleValue();
        double appbarBottom = number(page, "document.querySelector('.xh-v4-appbar').getBoundingClientRect().bottom").doubleValue();
        assertTrue(Math.abs(appbarTop - heroTop) <= 1.0,
            () -> width + "px app bar must start at Hero top");
        assertTrue(appbarBottom - appbarTop >= 51.0 && appbarBottom - appbarTop <= 53.0,
            () -> width + "px app bar height is wrong: " + (appbarBottom - appbarTop));

        double artworkTop = number(page, "document.querySelector('#heroBackground').getBoundingClientRect().top").doubleValue();
        double artworkBottom = number(page, "document.querySelector('#heroBackground').getBoundingClientRect().bottom").doubleValue();
        double artworkHeight = artworkBottom - artworkTop;
        assertTrue(Math.abs(artworkTop - appbarBottom) <= 1.0,
            () -> width + "px key art must begin directly below site chrome");
        assertTrue(width / artworkHeight >= 1.76 && width / artworkHeight <= 1.79,
            () -> width + "px key art should stay near 16:9, ratio=" + (width / artworkHeight));
        assertTrue(heroBottom - artworkBottom >= 124.0 && heroBottom - artworkBottom <= 128.0,
            () -> width + "px solid action deck height is wrong: " + (heroBottom - artworkBottom));

        double statusTop = number(page, "document.querySelector('.xh-v4-status').getBoundingClientRect().top").doubleValue();
        double statusBottom = number(page, "document.querySelector('.xh-v4-status').getBoundingClientRect().bottom").doubleValue();
        assertTrue(statusTop >= artworkTop + 11.0 && statusTop <= artworkTop + 13.0,
            () -> width + "px status chip should sit just inside key art top");
        assertTrue(statusBottom < artworkBottom,
            () -> width + "px status chip must stay inside key art");

        double titleTop = number(page, "document.querySelector('#mobileHeroTitle').getBoundingClientRect().top").doubleValue();
        double titleBottom = number(page, "document.querySelector('#mobileHeroTitle').getBoundingClientRect().bottom").doubleValue();
        assertTrue(titleTop >= artworkBottom + 13.0 && titleTop <= artworkBottom + 15.0,
            () -> width + "px title must live in solid action deck, not over key art");

        double actionsTop = number(page, "document.querySelector('.xh-v4-hero-actions').getBoundingClientRect().top").doubleValue();
        double actionsBottom = number(page, "document.querySelector('.xh-v4-hero-actions').getBoundingClientRect().bottom").doubleValue();
        assertTrue(actionsTop - titleBottom >= 24.0 && actionsTop - titleBottom <= 34.0,
            () -> width + "px title-to-CTA spacing is wrong: " + (actionsTop - titleBottom));
        assertTrue(heroBottom - actionsBottom >= 13.0 && heroBottom - actionsBottom <= 15.0,
            () -> width + "px CTA bottom spacing is wrong: " + (heroBottom - actionsBottom));

        double firstLeft = number(page, "document.querySelector('.xh-v4-hero-actions .btn:first-child').getBoundingClientRect().left").doubleValue();
        double firstRight = number(page, "document.querySelector('.xh-v4-hero-actions .btn:first-child').getBoundingClientRect().right").doubleValue();
        double secondLeft = number(page, "document.querySelector('.xh-v4-hero-actions .btn:nth-child(2)').getBoundingClientRect().left").doubleValue();
        assertTrue(secondLeft > firstRight,
            () -> width + "px CTA buttons must remain two clean columns: " + (secondLeft - firstRight));
        assertTrue(firstLeft >= 11.0,
            () -> width + "px primary CTA must respect side gutter");

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
                    <div class="actions hero-actions xh-v4-hero-actions"><a class="btn primary xh-v4-primary">立即询价</a><a class="btn ghost xh-v4-progress">查询进度</a></div>
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
            "mobile-home-hero-r4.css"
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
