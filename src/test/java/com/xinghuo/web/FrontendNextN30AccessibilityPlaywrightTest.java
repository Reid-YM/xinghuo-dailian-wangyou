package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.*;
import com.microsoft.playwright.*;
import java.net.URI;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@EnabledIfEnvironmentVariable(named = "PLAYWRIGHT_UI_TESTS", matches = "1")
class FrontendNextN30AccessibilityPlaywrightTest {
    @Test void publicAndGuestPagesHaveNamedControlsLandmarksAndWorkingSkipNavigation() throws Exception {
        both(browser -> {
            for (int width : new int[]{320, 1280}) {
                try (Fixture f = new Fixture(browser, width, false)) {
                    for (String path : List.of("/next", "/next/services", "/next/quote", "/next/account",
                            "/next/guides", "/next/mastery", "/next/account/submit", "/next/records",
                            "/next/contact", "/next/trust", "/next/rules", "/next/privacy", "/next/guides/test-guide")) {
                        f.open(path);
                        if (path.equals("/next/account")) f.page.waitForSelector("#n7AuthSwitcher");
                        assertEquals(List.of(), unnamedControls(f.page), path);
                        f.page.locator(".next-skip-link").focus();
                        f.page.keyboard().press("Enter");
                        assertEquals(Boolean.TRUE, f.page.evaluate("() => document.activeElement === document.querySelector('.page.active')"), path);
                        assertEquals("main", f.page.locator(".page.active").getAttribute("role"), path);
                        assertEquals(1, f.page.getByRole(com.microsoft.playwright.options.AriaRole.MAIN).count(), path);
                        assertEquals(Boolean.TRUE, f.page.evaluate("() => document.documentElement.scrollWidth <= innerWidth + 1"), path);
                        if (width == 320) {
                            assertEquals(List.of(), f.page.evaluate("""
                                () => [...document.querySelectorAll('.page.active button,.page.active select,.page.active input:not([type=checkbox]):not([type=radio]),.page.active .btn')]
                                  .filter(e => e.getClientRects().length && e.getBoundingClientRect().height < 43.5).map(e => e.id || e.textContent.trim())
                                """), path);
                            assertEquals(List.of(), f.page.evaluate("""
                                () => [...document.querySelectorAll('.page.active input:not([type=checkbox]):not([type=radio]),.page.active select,.page.active textarea')]
                                  .filter(e => e.getClientRects().length && parseFloat(getComputedStyle(e).fontSize) < 16).map(e => e.id)
                                """), path);
                        }
                        if (browser.browserType().name().equals("chromium")
                                && List.of("/next/quote", "/next/account/submit", "/next/mastery").contains(path)) {
                            java.nio.file.Files.createDirectories(java.nio.file.Path.of("target/frontend-review"));
                            f.page.evaluate("async () => { await Promise.all(document.querySelector('.page.active').getAnimations().map(a => a.finished.catch(() => {}))); }");
                            f.page.screenshot(new Page.ScreenshotOptions().setFullPage(true).setPath(java.nio.file.Path.of(
                                    "target/frontend-review/a11y-" + path.substring(6).replace('/', '-') + "-" + width + ".png")));
                        }
                    }
                    f.open("/next/services");
                    var category = f.page.locator("button.cat[data-cat=level]");
                    category.focus(); f.page.keyboard().press("Enter");
                    f.page.waitForFunction("() => new URL(location.href).searchParams.get('category') === 'level'");
                    assertEquals("true", category.getAttribute("aria-pressed"));
                }
            }
        });
    }

    @Test void authenticationAndAccountTabsUseKeyboardWithoutMovingFocusIntoHiddenPanels() throws Exception {
        both(browser -> {
            try (Fixture guest = new Fixture(browser, 390, false)) {
                guest.open("/next/account");
                guest.page.waitForSelector("#n7AuthSwitcher");
                guest.page.locator("#n7Tab-login").focus();
                guest.page.keyboard().press("ArrowRight");
                assertEquals("true", guest.page.locator("#n7Tab-register").getAttribute("aria-selected"));
                assertEquals("n7Tab-register", guest.page.evaluate("() => document.activeElement.id"));
                guest.page.keyboard().press("Tab");
                assertEquals("n7RegisterEmail", guest.page.evaluate("() => document.activeElement.id"));
                assertEquals(List.of(), unnamedControls(guest.page));
                guest.page.locator("#n7Tab-register").focus(); guest.page.keyboard().press("End");
                assertEquals("true", guest.page.locator("#n7Tab-reset").getAttribute("aria-selected"));
                assertEquals(List.of(), unnamedControls(guest.page));
            }
            try (Fixture account = new Fixture(browser, 390, true)) {
                account.open("/next/account/settings");
                account.page.waitForSelector("#n6PasswordForm");
                assertEquals(List.of(), unnamedControls(account.page));
                account.page.locator("#n5Tab-orders").focus(); account.page.keyboard().press("ArrowRight");
                assertEquals("true", account.page.locator("#n5Tab-quotes").getAttribute("aria-selected"));
                assertEquals("n5Tab-quotes", account.page.evaluate("() => document.activeElement.id"));
                assertEquals("n5Tab-quotes", account.page.locator("#n5RecordsBody").getAttribute("aria-labelledby"));
                account.open("/next/account/submit");
                var detail = account.page.getByRole(com.microsoft.playwright.options.AriaRole.BUTTON,
                        new Page.GetByRoleOptions().setName("查看正文").setExact(true));
                detail.click();
                var dialog = account.page.getByRole(com.microsoft.playwright.options.AriaRole.DIALOG,
                        new Page.GetByRoleOptions().setName("测试投稿正文").setExact(true));
                dialog.waitFor();
                assertTrue(dialog.isVisible());
                account.page.keyboard().press("Escape");
                account.page.waitForSelector(".n25-dialog", new Page.WaitForSelectorOptions()
                        .setState(com.microsoft.playwright.options.WaitForSelectorState.DETACHED));
                assertEquals("查看正文", account.page.evaluate("() => document.activeElement.textContent"));
                account.page.locator("#n25Submit").click();
                assertEquals("n25Game", account.page.evaluate("() => document.activeElement.id"));
                assertEquals("true", account.page.locator("#n25Game").getAttribute("aria-invalid"));
                account.page.locator("#n25Game").selectOption("CODMW4");
                account.page.locator("#n25Submit").click();
                assertEquals("n25Title", account.page.evaluate("() => document.activeElement.id"));
                account.page.locator("#n25Title").fill("必填项检查");
                account.page.locator("#n25Submit").click();
                assertEquals("n25Content", account.page.evaluate("() => document.activeElement.id"));
            }
        });
    }

    @Test void masteryKeyboardModesCheckboxesAndSearchRetainFocusAcrossRenders() throws Exception {
        both(browser -> {
            try (Fixture f = new Fixture(browser, 390, false)) {
                f.open("/next/mastery");
                f.page.waitForSelector("#n24ModePanel");
                f.page.locator("#n24Tab-MULTIPLAYER").focus(); f.page.keyboard().press("ArrowRight");
                assertEquals("n24Tab-WARZONE", f.page.evaluate("() => document.activeElement.id"));
                assertEquals("true", f.page.locator("#n24Tab-WARZONE").getAttribute("aria-selected"));
                assertEquals("n24Tab-WARZONE", f.page.locator("#n24ModePanel").getAttribute("aria-labelledby"));
                f.page.keyboard().press("Home");
                assertEquals("n24Tab-MULTIPLAYER", f.page.evaluate("() => document.activeElement.id"));
                var checkbox = f.page.locator("input[data-weapon-code=__mastery]");
                checkbox.focus(); f.page.keyboard().press("Space");
                assertTrue(checkbox.isChecked());
                assertEquals("__mastery", f.page.evaluate("() => document.activeElement.dataset.weaponCode"));
                var original = f.page.locator("#n24Search").elementHandle();
                f.page.getByLabel("搜索武器").fill("Weapon A");
                f.page.waitForFunction("e => !e.isConnected", original);
                assertEquals("n24Search", f.page.evaluate("() => document.activeElement.id"));
                assertEquals("Weapon A", f.page.getByLabel("搜索武器").inputValue());
            }
        });
    }

    @Test void masterySearchCompositionAndInterleavedChangesKeepTheLatestQuery() throws Exception {
        both(browser -> {
            try (Fixture f = new Fixture(browser, 390, false)) {
                f.open("/next/mastery");
                f.page.getByLabel("搜索武器").focus();
                assertEquals("Weapon", f.page.evaluate("""
                    () => {
                      const input = document.querySelector('#n24Search');
                      input.value = 'Weapon'; input.dispatchEvent(new Event('input', {bubbles:true}));
                      document.querySelector('[data-favorite=A]').click();
                      return document.querySelector('#n24Search').value;
                    }
                    """));
                f.page.getByLabel("搜索武器").focus();
                var composing = f.page.locator("#n24Search").elementHandle();
                f.page.evaluate("""
                    () => {
                      const input = document.querySelector('#n24Search');
                      input.dispatchEvent(new CompositionEvent('compositionstart', {bubbles:true}));
                      input.value = '武器';
                      input.dispatchEvent(new InputEvent('input', {bubbles:true,isComposing:true}));
                    }
                    """);
                f.page.evaluate("() => new Promise(resolve => setTimeout(resolve, 250))");
                assertEquals(Boolean.TRUE, composing.evaluate("e => e.isConnected"));
                assertEquals("武器", f.page.getByLabel("搜索武器").inputValue());
                f.page.locator("#n24Search").dispatchEvent("compositionend");
                f.page.waitForFunction("e => !e.isConnected", composing);
                assertEquals("武器", f.page.getByLabel("搜索武器").inputValue());
                assertEquals("武器", f.page.evaluate("() => new URL(location.href).searchParams.get('q')"));
                f.page.evaluate("() => window.go('services')");
                f.page.evaluate("() => new Promise(resolve => setTimeout(resolve, 250))");
                assertEquals("services", f.page.locator(".page.active").getAttribute("data-page"));
            }
        });
    }

    private static Object unnamedControls(Page page) {
        return page.evaluate("""
            () => [...document.querySelectorAll('.page.active input:not([type=hidden]),.page.active select,.page.active textarea')]
              .filter(e => e.getClientRects().length && !e.getAttribute('aria-label')?.trim()
                && !e.getAttribute('aria-labelledby')?.split(/\\s+/).some(id => document.getElementById(id)?.textContent.trim())
                && ![...(e.labels || [])].some(label => label.textContent.trim()))
              .map(e => e.id || e.outerHTML.slice(0,120))
            """);
    }
    interface Check { void run(Browser browser) throws Exception; }
    private void both(Check check) throws Exception {
        try (Playwright p = Playwright.create(); Browser chromium = p.chromium().launch(); Browser webkit = p.webkit().launch()) {
            for (Browser browser : List.of(chromium, webkit)) check.run(browser);
        }
    }
    private static class Fixture implements AutoCloseable {
        final Page page;
        Fixture(Browser browser, int width, boolean logged) {
            var preview = new FrontendNextPreviewController();
            var mvc = MockMvcBuilders.standaloneSetup(preview, new FrontendNextN23GuidesController(preview),
                    new FrontendNextN24MasteryController(preview), new FrontendNextN25SubmissionController(preview),
                    new FrontendNextN26RecordsController(preview), new FrontendNextN27SupportTrustController(preview),
                    new FrontendNextN28LegalController(preview)).build();
            page = browser.newPage(new Browser.NewPageOptions().setViewportSize(width, 900));
            page.route("**/*", route -> {
                String path = URI.create(route.request().url()).getPath();
                if (path.equals("/api/account/me")) json(route, logged ? 200 : 401, "{\"id\":1,\"displayName\":\"测试用户\",\"email\":\"test@example.test\",\"emailVerified\":true}");
                else if (logged && path.equals("/api/account/submissions")) json(route, 200, "[{\"id\":1,\"title\":\"测试投稿\",\"status\":\"SUBMITTED\"}]");
                else if (logged && path.equals("/api/account/submissions/1")) json(route, 200, "{\"title\":\"测试投稿正文\",\"contentMd\":\"# 正文\\n\\n投稿内容\"}");
                else if (path.equals("/api/catalog")) json(route, 200, "{\"games\":[{\"code\":\"CODMW4\",\"name\":\"使命召唤\"}],\"categories\":[{\"code\":\"LEVEL\",\"gameCode\":\"CODMW4\",\"name\":\"等级服务\"}],\"platforms\":[],\"gamePlatforms\":[]}");
                else if (path.equals("/api/services")) json(route, 200, "[{\"slug\":\"level\",\"name\":\"等级服务\",\"gameCode\":\"CODMW4\",\"category\":\"LEVEL\",\"priceMode\":\"QUOTE\"}]");
                else if (path.equals("/api/tool-station")) json(route, 200, """
                    {"games":[{"code":"CODMW4","name":"MW4","weapons":[{"code":"A","name":"Weapon A","weaponType":"AR"}],
                    "camoTracks":{"order":["MULTIPLAYER","WARZONE"],"modes":{
                    "MULTIPLAYER":{"code":"MULTIPLAYER","name":"多人","items":[{"code":"GOLD","name":"金色"}]},
                    "WARZONE":{"code":"WARZONE","name":"战区","items":[{"code":"GOLD","name":"金色"}]}}}}]}
                    """);
                else FrontendNextN30DocumentJourneyPlaywrightTest.fulfill(route, mvc, new HashSet<>());
            });
        }
        void open(String path) {
            page.navigate("https://example.test" + path);
            page.waitForFunction("() => document.documentElement.dataset.siteConfiguration === 'ready' && Array.isArray(window.XHFrontendNextN2?.state?.services)");
            if (path.equals("/next/guides")) page.waitForSelector("#n23GuidesCenter.active");
            else if (path.equals("/next/mastery")) page.waitForSelector("#n24ModePanel");
            else if (path.equals("/next/account/submit")) page.waitForSelector("#n25SubmissionWorkbench.active");
            else if (path.equals("/next/records")) page.waitForSelector("#n26RecordsCenter.active");
            else if (path.endsWith("contact")) page.waitForSelector("#n27Contact.active");
            else if (path.endsWith("trust")) page.waitForSelector("#n27Trust.active");
            else if (path.endsWith("rules") || path.endsWith("privacy")) page.waitForSelector("#n28Legal.active");
            else if (path.endsWith("test-guide")) page.waitForSelector("#n22GuideDetail.active");
        }
        @Override public void close() { page.close(); }
    }
    private static void json(Route r, int status, String body) { r.fulfill(new Route.FulfillOptions().setStatus(status).setContentType("application/json").setBody(body)); }
}
