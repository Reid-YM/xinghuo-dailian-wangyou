package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

@EnabledIfEnvironmentVariable(named = "PLAYWRIGHT_UI_TESTS", matches = "1")
class MobileCompactLayoutPlaywrightTest {
    private static final Path ASSETS = Path.of("src/main/resources/static/assets");

    @Test
    void chromium390UsesCompactFieldsWithoutHorizontalOverflow() {
        try (Playwright playwright = Playwright.create(); Browser browser = playwright.chromium().launch()) {
            Page page = browser.newPage(new Browser.NewPageOptions().setViewportSize(390, 844));
            renderFixture(page);

            assertBetween(number(page, "#textField", "height"), 40, 43);
            assertBetween(number(page, "#selectField", "height"), 40, 43);
            assertBetween(number(page, "#checkField", "height"), 17, 19);
            assertBetween(number(page, "#checkField", "width"), 17, 19);
            assertBetween(number(page, "#primaryButton", "height"), 40, 45);
            assertTrue((Boolean) page.evaluate("() => document.documentElement.scrollWidth <= window.innerWidth"));
        }
    }

    @Test
    void webkit360KeepsSmallControlsAligned() {
        try (Playwright playwright = Playwright.create(); Browser browser = playwright.webkit().launch()) {
            Page page = browser.newPage(new Browser.NewPageOptions().setViewportSize(360, 780));
            renderFixture(page);

            assertBetween(number(page, "#textField", "height"), 39, 41);
            assertBetween(number(page, "#checkField", "height"), 17, 19);
            Number offset = (Number) page.evaluate("""
                    () => {
                      const check = document.querySelector('#checkField').getBoundingClientRect();
                      const text = document.querySelector('#checkText').getBoundingClientRect();
                      return Math.abs(check.top - text.top);
                    }
                    """);
            assertTrue(offset.doubleValue() <= 4.5, "checkbox and label text should align at mobile width");
            assertTrue((Boolean) page.evaluate("() => document.documentElement.scrollWidth <= window.innerWidth"));
        }
    }

    private static void renderFixture(Page page) {
        page.setContent("""
                <!doctype html>
                <html data-frontend="mobile">
                <body class="mobile-frontend">
                  <main class="container">
                    <section class="section">
                      <div class="card quote-main-card">
                        <div class="grid2">
                          <div class="field"><label for="textField">联系方式</label><input id="textField" value="test"></div>
                          <div class="field"><label for="selectField">平台</label><select id="selectField"><option>PC</option></select></div>
                        </div>
                        <label class="toggle-row" id="checkRow"><input id="checkField" type="checkbox"><span id="checkText">我已阅读并同意服务规则</span></label>
                        <textarea id="noteField">测试备注</textarea>
                        <div class="actions"><button class="btn primary" id="primaryButton">提交</button><button class="btn" id="secondaryButton">返回</button></div>
                      </div>
                    </section>
                  </main>
                </body>
                </html>
                """);
        page.addStyleTag(new Page.AddStyleTagOptions().setPath(ASSETS.resolve("app.css")));
        page.addStyleTag(new Page.AddStyleTagOptions().setPath(ASSETS.resolve("mobile-app.css")));
        page.addStyleTag(new Page.AddStyleTagOptions().setPath(ASSETS.resolve("mobile-compact.css")));
    }

    private static double number(Page page, String selector, String dimension) {
        String expression = "() => document.querySelector('" + selector + "').getBoundingClientRect()." + dimension;
        return ((Number) page.evaluate(expression)).doubleValue();
    }

    private static void assertBetween(double value, double min, double max) {
        assertTrue(value >= min && value <= max, "expected " + value + " between " + min + " and " + max);
    }
}
