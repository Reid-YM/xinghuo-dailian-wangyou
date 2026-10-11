package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

@EnabledIfEnvironmentVariable(named = "PLAYWRIGHT_UI_TESTS", matches = "1")
class QuoteStyleOwnershipPlaywrightTest {
    private static final Path ASSETS = Path.of("src/main/resources/static/assets");

    @Test
    void authoritativeExperienceLayerControlsQuoteSupportCardAcrossBrowsers() {
        try (Playwright playwright = Playwright.create()) {
            for (BrowserType browserType : List.of(playwright.chromium(), playwright.webkit())) {
                try (Browser browser = browserType.launch()) {
                    Page page = browser.newPage(new Browser.NewPageOptions().setViewportSize(1440, 900));
                    page.setContent("""
                            <body data-page="quote">
                              <header class="site-header"><div class="topbar">HEADER</div></header>
                              <main>
                                <div class="quote-layout">
                                  <section class="quote-main-card">QUOTE</section>
                                  <aside class="consult-card">SUPPORT</aside>
                                </div>
                              </main>
                            </body>
                            """);
                    page.addStyleTag(new Page.AddStyleTagOptions().setPath(ASSETS.resolve("app.css")));
                    page.addStyleTag(new Page.AddStyleTagOptions().setPath(ASSETS.resolve("site-experience-v2.css")));

                    assertEquals("start", page.evaluate("() => getComputedStyle(document.querySelector('.quote-layout')).alignItems"));
                    assertEquals("sticky", page.evaluate("() => getComputedStyle(document.querySelector('.consult-card')).position"));
                    assertEquals("82px", page.evaluate("() => getComputedStyle(document.querySelector('.consult-card')).top"));

                    page.setViewportSize(800, 900);
                    assertEquals("relative", page.evaluate("() => getComputedStyle(document.querySelector('.consult-card')).position"));
                    assertEquals(0.0, ((Number) page.evaluate(
                            "() => parseFloat(getComputedStyle(document.querySelector('.consult-card')).top) || 0"
                    )).doubleValue());
                }
            }
        }
    }
}
