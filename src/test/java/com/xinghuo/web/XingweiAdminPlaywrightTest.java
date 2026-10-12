package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

@EnabledIfEnvironmentVariable(named = "PLAYWRIGHT_UI_TESTS", matches = "1")
class XingweiAdminPlaywrightTest {
    private static final Path ASSETS = Path.of("src/main/resources/static/assets");
    private static final Path TEMPLATE = Path.of("src/main/resources/templates/admin-xingwei.html");

    @Test
    void desktopAndPhoneLayoutsStayInsideViewportAndNavigationWorks() throws Exception {
        try (Playwright playwright = Playwright.create(); Browser browser = playwright.chromium().launch()) {
            Page page = browser.newPage();
            page.setViewportSize(1440, 900);
            page.setContent(Files.readString(TEMPLATE));
            page.addStyleTag(new Page.AddStyleTagOptions().setPath(ASSETS.resolve("admin-xingwei.css")));
            page.addScriptTag(new Page.AddScriptTagOptions().setPath(ASSETS.resolve("admin-xingwei.js")));

            assertTrue((Boolean) page.evaluate("() => document.documentElement.scrollWidth <= innerWidth + 1"));
            assertEquals("286px", page.evaluate("() => getComputedStyle(document.querySelector('.xw-shell')).gridTemplateColumns.split(' ')[0]"));
            assertEquals(16, ((Number) page.evaluate("() => document.querySelectorAll('.xw-module').length")).intValue());

            page.locator("#xwModuleSearch").fill("微信");
            assertTrue(((Number) page.evaluate("() => [...document.querySelectorAll('.xw-module')].filter(x => !x.hidden).length")).intValue() >= 2);
            assertTrue(((String) page.locator("#xwSearchCount").textContent()).contains("匹配"));

            page.setViewportSize(390, 844);
            page.locator("#xwModuleSearch").fill("");
            assertTrue((Boolean) page.evaluate("() => document.documentElement.scrollWidth <= innerWidth + 1"));
            assertTrue((Boolean) page.evaluate("() => getComputedStyle(document.querySelector('.xw-menu-button')).display !== 'none'"));
            assertTrue(!(Boolean) page.evaluate("() => document.querySelector('#xwSidebar').classList.contains('open')"));

            page.locator("#xwMenuButton").click();
            assertTrue((Boolean) page.evaluate("() => document.querySelector('#xwSidebar').classList.contains('open')"));
            page.locator(".xw-nav-item[href='#business']").click();
            assertTrue(!(Boolean) page.evaluate("() => document.querySelector('#xwSidebar').classList.contains('open')"));
            assertTrue((Boolean) page.evaluate("() => document.documentElement.scrollWidth <= innerWidth + 1"));
        }
    }
}
