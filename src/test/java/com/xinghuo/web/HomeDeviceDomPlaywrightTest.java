package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

@EnabledIfEnvironmentVariable(named = "PLAYWRIGHT_UI_TESTS", matches = "1")
class HomeDeviceDomPlaywrightTest {
    private static final Path TEMPLATES = Path.of("src/main/resources/templates");
    private static final Path ASSETS = Path.of("src/main/resources/static/assets");

    @Test
    void chromiumRendersIndependentDesktopAndMobileHomeWithoutOverflow() throws Exception {
        try (Playwright playwright = Playwright.create(); Browser browser = playwright.chromium().launch()) {
            assertDesktop(browser, 1920, 1080, "Chromium desktop 1920");
            assertDesktop(browser, 1366, 768, "Chromium desktop 1366");
            assertDesktop(browser, 1024, 768, "Chromium notebook 1024");
            assertDesktop(browser, 820, 1180, "Chromium tablet-desktop 820");
            assertMobile(browser, 430, 932, "Chromium mobile 430");
            assertMobile(browser, 390, 844, "Chromium mobile 390");
            assertMobile(browser, 320, 700, "Chromium mobile 320");
        }
    }

    @Test
    void webkitRendersIndependentPhoneHome() throws Exception {
        try (Playwright playwright = Playwright.create(); Browser browser = playwright.webkit().launch()) {
            assertMobile(browser, 390, 844, "WebKit iPhone 390");
        }
    }

    private static void assertDesktop(Browser browser, int width, int height, String label) throws Exception {
        Page page = render(browser, TEMPLATES.resolve("desktop/index.html"), ASSETS.resolve("desktop-storefront.css"), ASSETS.resolve("desktop-storefront.js"), width, height, false);
        assertNotNull(page.querySelector(".xh-desktop-home"), label + " desktop root missing");
        assertNotNull(page.querySelector(".xh-desktop-command"), label + " desktop command area missing");
        assertNull(page.querySelector(".xh-mobile-launcher"), label + " leaked mobile launcher");
        assertNull(page.querySelector(".mobile-dock"), label + " leaked mobile dock");
        assertNotNull(page.querySelector("#quoteForm"), label + " quote form missing");
        assertNotNull(page.querySelector("#orderForm"), label + " record form missing");
        Number heroHeight = number(page, "document.querySelector('#homeHero').getBoundingClientRect().height");
        assertTrue(heroHeight.doubleValue() >= 500, () -> label + " desktop hero collapsed to " + heroHeight + "px");
        assertNoOverflow(page, label);
        page.close();
    }

    private static void assertMobile(Browser browser, int width, int height, String label) throws Exception {
        Page page = render(browser, TEMPLATES.resolve("mobile/index.html"), ASSETS.resolve("mobile-storefront.css"), ASSETS.resolve("mobile-storefront.js"), width, height, true);
        assertNotNull(page.querySelector(".xh-mobile-home"), label + " mobile root missing");
        assertNotNull(page.querySelector(".xh-mobile-hero-v4"), label + " V4/V5 hero missing");
        assertEquals(4, ((Number) page.evaluate("() => document.querySelectorAll('.xh-mobile-hero-feature').length")).intValue(), label + " hero feature count");
        assertNotNull(page.querySelector(".xh-mobile-progress-card"), label + " progress card missing");
        assertNotNull(page.querySelector(".xh-mobile-launcher"), label + " mobile launcher missing");
        assertNull(page.querySelector(".xh-desktop-command"), label + " leaked desktop command area");
        assertNotNull(page.querySelector(".mobile-dock"), label + " mobile dock missing");
        assertNotNull(page.querySelector("#quoteForm"), label + " quote form missing");
        assertNotNull(page.querySelector("#orderForm"), label + " record form missing");
        assertEquals("grid", string(page, "getComputedStyle(document.querySelector('.mobile-dock')).display"), label + " mobile dock hidden");
        assertEquals("fixed", string(page, "getComputedStyle(document.querySelector('.mobile-dock')).position"), label + " mobile dock must float fixed");
        assertEquals(5, ((Number) page.evaluate("() => document.querySelectorAll('.mobile-dock > a').length")).intValue(), label + " dock item count");
        assertNotNull(page.querySelector(".mobile-dock [data-dock-key='home'][aria-current='page']"), label + " active home item missing");
        Number dockIcon = number(page, "document.querySelector('.mobile-dock svg').getBoundingClientRect().width");
        assertTrue(dockIcon.doubleValue() >= 20, () -> label + " dock icon too small: " + dockIcon + "px");
        Number submitHeight = number(page, "document.querySelector('.xh-mobile-submit').getBoundingClientRect().height");
        assertTrue(submitHeight.doubleValue() >= 48, () -> label + " submit touch target too small: " + submitHeight + "px");
        Number inputFont = number(page, "parseFloat(getComputedStyle(document.querySelector('#home-contact')).fontSize)");
        assertTrue(inputFont.doubleValue() >= 16, () -> label + " input font can trigger iOS zoom: " + inputFont + "px");

        Number heroHeight = number(page, "document.querySelector('#homeHero').getBoundingClientRect().height");
        assertTrue(heroHeight.doubleValue() >= 400 && heroHeight.doubleValue() <= 540,
                () -> label + " V5 first-screen composition drifted: hero=" + heroHeight + "px");
        assertEquals("block", string(page, "getComputedStyle(document.querySelector('#gameEntrySection')).display"), label + " MW4 reference card hidden");
        Number gameCardHeight = number(page, "document.querySelector('#gameEntrySection').getBoundingClientRect().height");
        assertTrue(gameCardHeight.doubleValue() >= 90, () -> label + " MW4 game card too short: " + gameCardHeight + "px");
        assertNoOverflow(page, label);
        page.close();
    }

    private static Page render(Browser browser, Path template, Path deviceCss, Path deviceJs, int width, int height, boolean mobile) throws Exception {
        Page page = browser.newPage(new Browser.NewPageOptions().setViewportSize(width, height));
        page.setContent(Files.readString(template));
        page.addStyleTag(new Page.AddStyleTagOptions().setPath(ASSETS.resolve("app.css")));
        page.addStyleTag(new Page.AddStyleTagOptions().setPath(ASSETS.resolve("site-experience-v2.css")));
        page.addStyleTag(new Page.AddStyleTagOptions().setPath(deviceCss));
        if (mobile) {
            page.addStyleTag(new Page.AddStyleTagOptions().setPath(ASSETS.resolve("mobile-home-v4.css")));
            page.addStyleTag(new Page.AddStyleTagOptions().setPath(ASSETS.resolve("mobile-floating-dock-v4.css")));
            page.addStyleTag(new Page.AddStyleTagOptions().setPath(ASSETS.resolve("mobile-home-reference-v5.css")));
        }
        page.addStyleTag(new Page.AddStyleTagOptions().setPath(ASSETS.resolve("site-overflow-guard.css")));
        page.addScriptTag(new Page.AddScriptTagOptions().setPath(deviceJs));
        return page;
    }

    private static void assertNoOverflow(Page page, String label) {
        Number scrollWidth = number(page, "document.documentElement.scrollWidth");
        Number clientWidth = number(page, "document.documentElement.clientWidth");
        assertTrue(scrollWidth.doubleValue() <= clientWidth.doubleValue() + 1,
                () -> label + " overflowed horizontally: scrollWidth=" + scrollWidth + ", clientWidth=" + clientWidth);
    }

    private static Number number(Page page, String expression) {
        return (Number) page.evaluate("() => " + expression);
    }

    private static String string(Page page, String expression) {
        return String.valueOf(page.evaluate("() => " + expression));
    }
}
