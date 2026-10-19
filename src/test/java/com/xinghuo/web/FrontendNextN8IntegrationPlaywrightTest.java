package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.Route;
import com.microsoft.playwright.options.WaitForSelectorState;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

@EnabledIfEnvironmentVariable(named = "PLAYWRIGHT_UI_TESTS", matches = "1")
class FrontendNextN8IntegrationPlaywrightTest {
    private static final Path NEXT = Path.of("src/main/resources/static/next");

    @Test
    void chromiumReplacesStalePreviewCopyAndReportsCompleteRuntimeInventory() {
        try (Playwright playwright = Playwright.create(); Browser browser = playwright.chromium().launch()) {
            Page page = page(browser, 1280, 800);
            installRuntimeMarkers(page);
            page.addScriptTag(new Page.AddScriptTagOptions().setPath(NEXT.resolve("n8-integration.js")));
            page.waitForSelector("#frontend-next-n8-style",
                    new Page.WaitForSelectorOptions().setState(WaitForSelectorState.ATTACHED));

            assertEquals("n8-integration", page.locator("html").getAttribute("data-frontend-next-stage"));
            assertEquals("8/8", page.locator("html").getAttribute("data-frontend-next-runtimes"));
            assertEquals("新版预览", page.locator(".badge").textContent());
            assertTrue(page.locator(".badge").getAttribute("title").contains("旧版入口"));
            assertTrue(page.locator(".float .sub").textContent().contains("不代表当前账号真实订单"));
            assertTrue(page.locator(".section .sub").textContent().contains("跟踪订单和交付进度"));

            String css = page.locator("#frontend-next-n8-style").textContent();
            assertTrue(css.contains("prefers-reduced-motion:reduce"));
            assertTrue(css.contains("scroll-behavior:auto"));
        }
    }

    @Test
    void webkitShowsOfflineWarningAndClearsItAfterReconnectWithoutChangingBusinessDom() {
        try (Playwright playwright = Playwright.create(); Browser browser = playwright.webkit().launch()) {
            Page page = page(browser, 390, 844);
            installRuntimeMarkers(page);
            page.addScriptTag(new Page.AddScriptTagOptions().setPath(NEXT.resolve("n8-integration.js")));
            page.waitForSelector("#n8NetworkStatus",
                    new Page.WaitForSelectorOptions().setState(WaitForSelectorState.ATTACHED));

            page.evaluate("() => window.XHFrontendNextN8.setNetworkState(false)");
            assertFalse(page.locator("#n8NetworkStatus").isHidden());
            assertEquals("status", page.locator("#n8NetworkStatus").getAttribute("role"));
            assertEquals("polite", page.locator("#n8NetworkStatus").getAttribute("aria-live"));
            assertTrue(page.locator("#n8NetworkStatus").textContent().contains("当前网络已断开"));
            assertEquals("offline", page.locator("html").getAttribute("data-network-state"));
            assertEquals("订单表单内容", page.locator("#businessDraft").inputValue());

            double width = page.locator("#n8NetworkStatus").boundingBox().width;
            assertTrue(width <= 390.0, "offline banner must stay inside the mobile viewport");

            page.evaluate("() => window.XHFrontendNextN8.setNetworkState(true)");
            assertTrue(page.locator("#n8NetworkStatus").isHidden());
            assertEquals("online", page.locator("html").getAttribute("data-network-state"));
            assertEquals("订单表单内容", page.locator("#businessDraft").inputValue());
        }
    }

    private static void installRuntimeMarkers(Page page) {
        page.evaluate("""
                () => {
                  window.XHFrontendNextN2={};
                  window.XHFrontendNextN3={};
                  window.XHFrontendNextN4={};
                  window.XHFrontendNextN4Mini={};
                  window.XHFrontendNextN5={};
                  window.XHFrontendNextN5Actions={};
                  window.XHFrontendNextN6={};
                  window.XHFrontendNextN7Auth={};
                }
                """);
    }

    private static Page page(Browser browser, int width, int height) {
        BrowserContext context = browser.newContext(new Browser.NewContextOptions()
                .setViewportSize(width, height));
        Page page = context.newPage();
        page.route("**/*", route -> route.fulfill(new Route.FulfillOptions()
                .setContentType("text/html; charset=utf-8")
                .setBody("""
                        <!doctype html><html><head><meta charset="utf-8"></head><body>
                          <section class="page active" data-page="home">
                            <div class="float"><p class="sub">赛季服务 · 挑战任务执行中<br>Frontend Next V1 不连接生产订单。</p></div>
                            <div class="section"><p class="sub">先统一视觉和组件体系，再由新版后台逐步驱动页面内容。</p></div>
                          </section>
                          <input id="businessDraft" value="订单表单内容">
                          <div class="badge">FRONTEND NEXT · V1 PARALLEL PREVIEW</div>
                        </body></html>
                        """)));
        page.navigate("https://example.test/next");
        return page;
    }
}
