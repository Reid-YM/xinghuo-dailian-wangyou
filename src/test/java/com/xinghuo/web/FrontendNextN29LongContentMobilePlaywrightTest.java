package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

class FrontendNextN29LongContentMobilePlaywrightTest {
    private static final Path NEXT = Path.of("src/main/resources/static/next");
    private static final Map<String, String> FIXTURES = new LinkedHashMap<>();

    static {
        String word = "MW4CUSTOMERPROGRESSHISTORY".repeat(25);
        FIXTURES.put("n23-guides-center.js",
                "<section class='page active n23-guides'><div class='n23-head'><h1>" + word
                        + "</h1></div><div class='n23-grid'><article class='n23-card'><h3>" + word
                        + "</h3><p>" + word + "</p><a href='#'>" + word + "</a></article></div></section>");
        FIXTURES.put("n24-mastery.js",
                "<section class='page active n24-mastery'><div class='n24-weapon-head'>"
                        + "<span>1</span><img class='n24-thumb' alt='' /><div><h3>" + word
                        + "</h3><p>" + word + "</p></div><span>" + word + "</span></div>"
                        + "<div class='n24-checks'><label class='n24-check'><input type='checkbox' />"
                        + word + "</label></div><div class='n24-state'>" + word + "</div></section>");
        FIXTURES.put("n25-submission-workbench.js",
                "<section class='page active n25-submit'><div class='n25-grid'><div class='n25-panel'>"
                        + "<div class='n25-preview'>" + word + "</div></div><div class='n25-panel'>"
                        + "<article class='n25-item'><div class='n25-item-head'><h3>" + word
                        + "</h3><span>" + word + "</span></div><p>" + word
                        + "</p></article></div></div></section>");
        FIXTURES.put("n26-records-center.js",
                "<section class='page active n26-records'><div class='n26-head'><h1>" + word
                        + "</h1></div><div class='n26-list'><article class='n26-card'><div class='n26-card-head'>"
                        + "<strong>" + word + "</strong></div><h3>" + word
                        + "</h3><p>" + word + "</p></article></div></section>");
        FIXTURES.put("n27-support-trust.js",
                "<section class='page active n27-page'><div class='n27-section-head'><h2>" + word
                        + "</h2></div><div class='n27-grid'><article class='n27-card'><h3>" + word
                        + "</h3><p>" + word + "</p></article><div class='n27-empty'>"
                        + word + "</div></div></section>");
        FIXTURES.put("n28-legal-single-source.js",
                "<section class='page active n28-legal'><div class='n28-document'><div class='n28-section'>"
                        + "<h3>" + word + "</h3><p>" + word
                        + "</p><a class='n28-link' href='/next/privacy'>" + word
                        + "</a></div></div></section>");
    }

    @Test
    void everyLongTextSensitiveNextStageHasWrapAndMinWidthGuards() throws Exception {
        for (String script : FIXTURES.keySet()) {
            String code = Files.readString(NEXT.resolve(script));
            assertTrue(code.contains("/* N29 mobile: allow long customer-provided"),
                    () -> script + " has no explicit N29 overflow guard");
            assertTrue(code.contains("overflow-wrap:anywhere"),
                    () -> script + " cannot break unspaced text");
            assertTrue(code.contains("min-width:0"),
                    () -> script + " grid/flex children can overflow");
        }
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "PLAYWRIGHT_UI_TESTS", matches = "1")
    void chromiumAndWebkitContainLongUserAndPublishedTextWithinMobileViewport() throws Exception {
        String shell = Files.readString(NEXT.resolve("index.html"));
        String baseCss = between(shell, "<style>", "</style>");
        try (Playwright playwright = Playwright.create()) {
            BrowserType.LaunchOptions options = new BrowserType.LaunchOptions();
            String executable = System.getenv("XINGHUO_TEST_CHROMIUM");
            if (executable != null && !executable.isBlank()) {
                options.setExecutablePath(Path.of(executable));
            }
            try (Browser chromium = playwright.chromium().launch(options);
                 Browser webkit = playwright.webkit().launch()) {
                assertStages(chromium, baseCss, 320);
                assertStages(chromium, baseCss, 390);
                assertStages(webkit, baseCss, 320);
                assertStages(webkit, baseCss, 390);
            }
        }
    }

    private static void assertStages(Browser browser, String baseCss, int width) throws Exception {
        for (Map.Entry<String, String> fixture : FIXTURES.entrySet()) {
            String code = Files.readString(NEXT.resolve(fixture.getKey()));
            String tick = Character.toString((char) 96);
            String stageCss = between(code, "style.textContent = " + tick, tick + ";");
            Page page = browser.newPage(new Browser.NewPageOptions().setViewportSize(width, 780));
            try {
                page.setContent("<!doctype html><html lang='zh-CN'><head>"
                        + "<meta name='viewport' content='width=device-width, initial-scale=1' />"
                        + "<style>" + baseCss + "</style><style>" + stageCss + "</style>"
                        + "</head><body><main class='shell'>" + fixture.getValue()
                        + "</main></body></html>");
                Number documentWidth = (Number) page.evaluate(
                        "() => document.documentElement.scrollWidth");
                Number viewportWidth = (Number) page.evaluate(
                        "() => document.documentElement.clientWidth");
                assertTrue(documentWidth.doubleValue() <= viewportWidth.doubleValue() + 1,
                        () -> fixture.getKey() + " " + browser.browserType().name() + " "
                                + width + "px overflow: scrollWidth=" + documentWidth
                                + " clientWidth=" + viewportWidth);
                String css = (String) page.evaluate(
                        "() => getComputedStyle(document.querySelector('section.page')).display");
                assertEquals("block", css, fixture.getKey() + " should be visible");
            } finally {
                page.close();
            }
        }
    }

    private static String between(String source, String start, String end) {
        int begin = source.indexOf(start);
        assertTrue(begin >= 0, () -> "missing start marker " + start);
        int finish = source.indexOf(end, begin + start.length());
        assertTrue(finish > begin, () -> "missing end marker " + end);
        return source.substring(begin + start.length(), finish);
    }
}
