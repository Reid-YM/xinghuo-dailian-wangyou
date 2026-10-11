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
class HeaderBrandLogoPlaywrightTest {
    private static final Path ASSETS = Path.of("src/main/resources/static/assets");

    @Test
    void intrinsicLogoDimensionsStayStableWithoutInlineStyle() {
        try (Playwright playwright = Playwright.create()) {
            verifyAcrossWidths(playwright.chromium().launch());
            verifyAcrossWidths(playwright.webkit().launch());
        }
    }

    private void verifyAcrossWidths(Browser browser) {
        try (browser) {
            for (int width : new int[]{320, 390, 1440}) {
                Page page = browser.newPage(new Browser.NewPageOptions().setViewportSize(width, 800));
                page.setContent("""
                        <header class="site-header">
                          <div class="container topbar">
                            <a class="brand brand-mark" href="/">
                              <img src="/assets/brand-logo.svg" alt="" aria-hidden="true" width="32" height="32" decoding="async">
                              <span>星火网游代练平台</span>
                            </a>
                            <nav class="nav"><a class="btn primary desktop-keep" href="/quote">提前询价</a></nav>
                            <button type="button" class="nav-toggle" aria-label="打开导航菜单"><span></span><span></span><span></span></button>
                          </div>
                        </header>
                        """);
                page.addStyleTag(new Page.AddStyleTagOptions().setPath(ASSETS.resolve("site-experience-v2.css")));

                Number renderedWidth = (Number) page.evaluate("() => document.querySelector('.brand-mark img').getBoundingClientRect().width");
                Number renderedHeight = (Number) page.evaluate("() => document.querySelector('.brand-mark img').getBoundingClientRect().height");
                assertEquals(32.0, renderedWidth.doubleValue(), 0.5, "logo width must remain stable at viewport " + width);
                assertEquals(32.0, renderedHeight.doubleValue(), 0.5, "logo height must remain stable at viewport " + width);
                assertEquals(null, page.locator(".brand-mark img").getAttribute("style"));
                assertTrue((Boolean) page.evaluate("""
                        () => {
                          const img = document.querySelector('.brand-mark img').getBoundingClientRect();
                          const header = document.querySelector('.site-header').getBoundingClientRect();
                          return img.left >= header.left && img.right <= header.right;
                        }
                        """));
                page.close();
            }
        }
    }
}
