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
class SiteShellInteractionPlaywrightTest {
    private static final Path ASSETS = Path.of("src/main/resources/static/assets");

    @Test
    void mobileNavigationAndAccountHydrationLiveInSiteShell() {
        try (Playwright playwright = Playwright.create(); Browser browser = playwright.chromium().launch()) {
            Page page = browser.newPage(new Browser.NewPageOptions().setViewportSize(390, 844));
            page.setContent("""
                    <body data-page="home">
                      <header class="site-header">
                        <div class="topbar">
                          <nav class="nav">
                            <a href="/services">服务</a>
                            <a href="/guides">攻略</a>
                            <a href="/records">业务记录</a>
                            <a href="/account" data-account-entry><span data-account-entry-text>个人中心</span></a>
                            <a class="btn primary" href="/quote">提前询价</a>
                          </nav>
                          <button type="button" class="nav-toggle" aria-label="打开导航菜单"><span></span><span></span><span></span></button>
                        </div>
                      </header>
                    </body>
                    """);
            page.evaluate("""
                    () => {
                      window.fetch = async (url) => {
                        if (url === '/api/account/me') {
                          return new Response(JSON.stringify({displayName:'星火用户', publicUserId:'U-100'}), {
                            status:200, headers:{'Content-Type':'application/json'}
                          });
                        }
                        throw new Error('unexpected fetch ' + url);
                      };
                    }
                    """);
            page.addScriptTag(new Page.AddScriptTagOptions().setPath(ASSETS.resolve("site-shell.js")));
            page.waitForFunction("() => document.querySelector('#mobileNavDrawer') !== null");
            page.waitForFunction("() => document.querySelector('[data-account-entry] .account-entry-name')?.textContent === '星火用户'");

            assertEquals("/account", page.locator("[data-account-entry]").getAttribute("href"));
            assertEquals(Boolean.TRUE, page.evaluate("() => document.querySelector('#mobileNavDrawer').hidden"));

            page.locator(".nav-toggle").click();
            assertEquals(Boolean.FALSE, page.evaluate("() => document.querySelector('#mobileNavDrawer').hidden"));
            assertTrue(page.locator("#mobileNavDrawer a[href='/records']").count() == 1);
            assertTrue(page.locator("#mobileNavDrawer a[href='/account']").count() == 1);

            page.keyboard().press("Escape");
            assertEquals(Boolean.TRUE, page.evaluate("() => document.querySelector('#mobileNavDrawer').hidden"));
        }
    }

    @Test
    void horizontalOverflowHintIsAppliedWithoutUiFixes() {
        try (Playwright playwright = Playwright.create(); Browser browser = playwright.chromium().launch()) {
            Page page = browser.newPage(new Browser.NewPageOptions().setViewportSize(390, 844));
            page.setContent("""
                    <body>
                      <div class="filters" style="width:120px;overflow-x:auto;white-space:nowrap">
                        <span style="display:inline-block;width:420px">LONG FILTER ROW</span>
                      </div>
                    </body>
                    """);
            page.addScriptTag(new Page.AddScriptTagOptions().setPath(ASSETS.resolve("site-shell.js")));
            page.waitForFunction("() => document.querySelector('.filters').classList.contains('x-overflow')");
            assertTrue((Boolean) page.evaluate("() => document.querySelector('.filters').classList.contains('x-overflow')"));

            page.evaluate("() => { const el=document.querySelector('.filters'); el.scrollLeft=40; el.dispatchEvent(new Event('scroll')); }");
            page.waitForFunction("() => document.querySelector('.filters').classList.contains('x-scrolled')");
            assertTrue((Boolean) page.evaluate("() => document.querySelector('.filters').classList.contains('x-scrolled')"));
        }
    }
}
