package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

class FrontendNextN29MobileNavigationPlaywrightTest {
    private static final Path HTML = Path.of("src/main/resources/static/next/index.html");

    @Test
    void mobileDockKeepsAllNextDestinationsAndAccessibilityFallbacks() throws Exception {
        String html = Files.readString(HTML);
        assertTrue(html.contains("class=\"next-mobile-dock\" aria-label=\"新版移动端快捷导航\""));
        for (String path : new String[] {"/next/services", "/next/quote", "/next/account",
                "/next/guides", "/next/mastery", "/next/records", "/next/contact"}) {
            assertTrue(html.contains("href=\"" + path + "\""), "missing direct Next route " + path);
        }
        assertTrue(html.contains("viewport-fit=cover"));
        assertTrue(html.contains("env(safe-area-inset-bottom"));
        assertTrue(html.contains("min-height:46px"));
        assertTrue(html.contains(":focus-visible"));
        assertTrue(html.contains("prefers-reduced-motion:reduce"));
        assertTrue(html.contains("overscroll-behavior-x:contain"));
        assertTrue(html.contains(".next-mobile-dock .navlink.active"));
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "PLAYWRIGHT_UI_TESTS", matches = "1")
    void chromiumAndWebkitMobileDockRemainVisibleAndTouchSafe() throws Exception {
        String html = Files.readString(HTML);
        try (Playwright playwright = Playwright.create()) {
            BrowserType.LaunchOptions chromiumOptions = new BrowserType.LaunchOptions();
            String executable = System.getenv("XINGHUO_TEST_CHROMIUM");
            if (executable != null && !executable.isBlank()) {
                chromiumOptions.setExecutablePath(Path.of(executable));
            }
            try (Browser chromium = playwright.chromium().launch(chromiumOptions);
                 Browser webkit = playwright.webkit().launch()) {
                assertDock(chromium, html, 320);
                assertDock(chromium, html, 390);
                assertDock(chromium, html, 768);
                assertDock(webkit, html, 390);
                Page desktop = chromium.newPage(new Browser.NewPageOptions().setViewportSize(1280, 900));
                desktop.setContent(html);
                assertEquals("none", String.valueOf(desktop.evaluate(
                        "() => getComputedStyle(document.querySelector('.next-mobile-dock')).display")));
                desktop.close();
            }
        }
    }

    private static void assertDock(Browser browser, String html, int width) {
        Page page = browser.newPage(new Browser.NewPageOptions().setViewportSize(width, 780));
        page.setContent(html);
        assertEquals("flex", String.valueOf(page.evaluate(
                "() => getComputedStyle(document.querySelector('.next-mobile-dock')).display")));
        assertEquals("fixed", String.valueOf(page.evaluate(
                "() => getComputedStyle(document.querySelector('.next-mobile-dock')).position")));
        assertEquals(8, page.locator(".next-mobile-dock a").count());
        Number height = (Number) page.evaluate(
                "() => document.querySelector('.next-mobile-dock a').getBoundingClientRect().height");
        assertTrue(height.doubleValue() >= 44, "mobile navigation target too small at " + width + "px");
        Number left = (Number) page.evaluate(
                "() => document.querySelector('.next-mobile-dock').getBoundingClientRect().left");
        Number right = (Number) page.evaluate(
                "() => document.querySelector('.next-mobile-dock').getBoundingClientRect().right");
        assertTrue(left.doubleValue() >= -1 && right.doubleValue() <= width + 1,
                "navigation bar escaped the viewport at " + width + "px");
        assertEquals("/next/mastery", page.locator(".next-mobile-dock a[href='/next/mastery']").getAttribute("href"));
        page.close();
    }
}
