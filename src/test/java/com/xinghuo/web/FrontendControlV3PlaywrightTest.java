package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.Route;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

@EnabledIfEnvironmentVariable(named = "PLAYWRIGHT_UI_TESTS", matches = "1")
class FrontendControlV3PlaywrightTest {
    private static final Path RUNTIME = Path.of("src/main/resources/static/assets/frontend-controls-runtime.js");

    @Test
    void chromiumAppliesDesktopOrderingSizingAndVisibility() {
        try (Playwright playwright = Playwright.create(); Browser browser = playwright.chromium().launch()) {
            Page page = browser.newPage(new Browser.NewPageOptions().setViewportSize(1920, 1080));
            serve(page, desktopHtml(), """
                    {
                      "homeModuleOrder":"services,hero,command,game,why,hot,tool,cases,faq",
                      "homeHeroHeight":"520",
                      "homeCardScale":"110",
                      "homeHotVisible":"false",
                      "homeHeroVisible":"true",
                      "homeHeroPromisesVisible":"true",
                      "homeHeroMediaVisible":"true",
                      "homeQuotePanelVisible":"true",
                      "homeRecordPanelVisible":"true",
                      "homeGameEntryVisible":"true",
                      "homeWhyVisible":"true",
                      "homeServicesVisible":"true",
                      "homeToolVisible":"true",
                      "homeCasesVisible":"true",
                      "homeFaqVisible":"true",
                      "siteHeaderVisible":"true",
                      "siteFooterVisible":"true",
                      "servicesEntryVisible":"true",
                      "quoteEntryVisible":"true",
                      "recordsEntryVisible":"true",
                      "mobileDockVisible":"true",
                      "motionEnabled":"true"
                    }
                    """);
            page.addScriptTag(new Page.AddScriptTagOptions().setPath(RUNTIME));
            page.waitForFunction("() => document.querySelector('#homeHero').style.minHeight === '520px'");

            assertEquals("home-service-showcase", page.locator("main > :first-child").getAttribute("class"));
            assertEquals("520px", page.locator("#homeHero").evaluate("el => el.style.minHeight"));
            assertEquals(Boolean.TRUE, page.locator(".xh-desktop-hot").evaluate("el => el.hidden"));
            Number padding = (Number) page.locator(".service-card").evaluate("el => parseFloat(getComputedStyle(el).paddingTop)");
            assertEquals(18.0, padding.doubleValue(), 0.1);
        }
    }

    @Test
    void webkitAppliesMobileOrderingDockLabelsAndDataDrivenHiddenStates() {
        try (Playwright playwright = Playwright.create(); Browser browser = playwright.webkit().launch()) {
            Page page = browser.newPage(new Browser.NewPageOptions().setViewportSize(390, 844));
            serve(page, mobileHtml(), """
                    {
                      "homeModuleOrder":"quote,hero,launcher,game,support,hot,services,why,tool,cases,faq",
                      "homeHeroHeight":"430",
                      "homeCardScale":"120",
                      "homeHotVisible":"true",
                      "homeHeroVisible":"true",
                      "homeHeroPromisesVisible":"true",
                      "homeHeroMediaVisible":"true",
                      "homeLauncherVisible":"true",
                      "homeQuotePanelVisible":"true",
                      "homeRecordPanelVisible":"false",
                      "homeGameEntryVisible":"true",
                      "homeWhyVisible":"true",
                      "homeServicesVisible":"true",
                      "homeToolVisible":"true",
                      "homeCasesVisible":"true",
                      "homeFaqVisible":"true",
                      "siteHeaderVisible":"true",
                      "siteFooterVisible":"true",
                      "servicesEntryVisible":"true",
                      "quoteEntryVisible":"true",
                      "recordsEntryVisible":"true",
                      "mobileDockVisible":"true",
                      "mobileDockServicesLabel":"服务V3",
                      "mobileDockGuidesLabel":"攻略V3",
                      "mobileDockRecordsLabel":"记录V3",
                      "mobileDockContactLabel":"客服V3",
                      "mobileDockQuoteLabel":"询价V3",
                      "motionEnabled":"true"
                    }
                    """);
            page.addScriptTag(new Page.AddScriptTagOptions().setPath(RUNTIME));
            page.waitForFunction("() => document.querySelector('#homeHero').style.minHeight === '430px'");

            Number quoteIndex = (Number) page.evaluate("""
                    () => Array.from(document.querySelector('main.xh-mobile-home').children)
                      .indexOf(document.querySelector('.xh-mobile-quote-card'))
                    """);
            String childOrder = String.valueOf(page.evaluate("""
                    () => Array.from(document.querySelector('main.xh-mobile-home').children)
                      .map(el => el.id || el.className || el.tagName)
                      .join(' | ')
                    """));
            assertEquals(0, quoteIndex.intValue(), "mobile module order mismatch: " + childOrder);
            assertEquals(Boolean.TRUE, page.locator(".xh-mobile-two-up").evaluate("el => el.hidden"),
                    "support wrapper must collapse when record is disabled and the existing global contact control already hid contact");
            assertEquals(Boolean.TRUE, page.locator("#publicCaseSection").evaluate("el => el.hidden"),
                    "V3 must not expose an empty data-driven case section merely because the feature is allowed");
            assertEquals(Boolean.TRUE, page.locator("#gameEntrySection").evaluate("el => el.hidden"),
                    "V3 must leave data-driven game visibility to the business loader when enabled");
            assertEquals("服务V3", page.locator(".mobile-dock a[href='/services']").textContent());
            assertEquals("攻略V3", page.locator(".mobile-dock a[href='/guides']").textContent());
            assertEquals("记录V3", page.locator(".mobile-dock a[href='/records']").textContent());
            assertEquals("客服V3", page.locator(".mobile-dock a[href='/contact']").textContent());
            assertEquals("询价V3", page.locator(".mobile-dock [data-primary-cta]").textContent());
            Number padding = (Number) page.locator(".service-card").evaluate("el => parseFloat(getComputedStyle(el).paddingTop)");
            assertEquals(19.0, padding.doubleValue(), 0.1);
        }
    }

    private static void serve(Page page, String html, String controlsJson) {
        page.route("https://frontend.test/**", route -> {
            if (route.request().url().endsWith("/api/public/frontend-controls")) {
                route.fulfill(new Route.FulfillOptions()
                        .setStatus(200)
                        .setContentType("application/json")
                        .setBody(controlsJson));
                return;
            }
            route.fulfill(new Route.FulfillOptions()
                    .setStatus(200)
                    .setContentType("text/html; charset=utf-8")
                    .setBody(html));
        });
        page.navigate("https://frontend.test/");
    }

    private static String desktopHtml() {
        return """
                <!doctype html><html data-device-mode="desktop"><head></head>
                <body class="xh-desktop-home-page">
                  <header class="site-header"></header>
                  <main class="xh-desktop-home">
                    <section id="homeHero"><div class="hero-promise"></div><aside class="hero-media-panel"></aside></section>
                    <section class="xh-desktop-command"><div class="quote-card-home"></div><aside><div class="desktop-secondary-panel"></div><div class="quick-contact-card"></div></aside></section>
                    <section id="gameEntrySection" hidden></section>
                    <section aria-label="为什么选择星火"><div class="section-title"><span class="eyebrow"></span><h2></h2><a class="text-link"></a></div><article class="card"></article></section>
                    <section class="xh-desktop-hot"></section>
                    <section class="home-service-showcase"><article class="service-card"></article></section>
                    <section class="xh-desktop-lower-grid"></section>
                    <section id="publicCaseSection" hidden></section>
                    <section class="xh-desktop-faq"></section>
                  </main>
                  <footer></footer>
                </body></html>
                """;
    }

    private static String mobileHtml() {
        return """
                <!doctype html><html data-device-mode="mobile"><head></head>
                <body class="xh-mobile-home-page">
                  <header class="site-header"></header>
                  <main class="xh-mobile-home">
                    <section id="homeHero"><div class="hero-promise"></div></section>
                    <section class="xh-mobile-launcher"></section>
                    <section id="gameEntrySection" class="xh-mobile-game-entry" hidden></section>
                    <section class="quote-card-home xh-mobile-quote-card"></section>
                    <section class="xh-mobile-two-up"><div class="desktop-secondary-panel"></div><div class="quick-contact-card" hidden></div></section>
                    <section class="xh-mobile-hot"></section>
                    <section class="xh-mobile-services"><article class="service-card"></article></section>
                    <section aria-label="为什么选择星火"><div class="section-title"><span class="eyebrow"></span><h2></h2><a class="text-link"></a></div><article class="card"></article></section>
                    <section class="xh-mobile-tool"></section>
                    <section id="publicCaseSection" hidden></section>
                    <section class="xh-mobile-faq"></section>
                  </main>
                  <nav class="mobile-dock"><a href="/services">服务</a><a href="/guides">攻略</a><a href="/records">记录</a><a href="/contact">客服</a><a href="#quick-actions" data-primary-cta>询价</a></nav>
                  <footer></footer>
                </body></html>
                """;
    }
}
