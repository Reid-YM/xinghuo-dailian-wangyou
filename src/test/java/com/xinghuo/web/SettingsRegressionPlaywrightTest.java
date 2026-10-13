package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

@EnabledIfEnvironmentVariable(named = "PLAYWRIGHT_UI_TESTS", matches = "1")
class SettingsRegressionPlaywrightTest {
    private static final Path ASSETS = Path.of("src/main/resources/static/assets");

    @Test
    void settingsWithoutHashDefaultsToProfileAndSecurityAnchorsStillWork() {
        try (Playwright playwright = Playwright.create(); Browser browser = playwright.chromium().launch()) {
            Page page = browser.newPage();
            page.setContent("""
                    <body data-page="account-settings">
                      <main data-settings-hub>
                        <nav>
                          <a href="#profile" data-settings-target="profile">资料</a>
                          <a href="#contact" data-settings-target="contact">回访</a>
                          <a href="#security" data-settings-target="security">安全</a>
                        </nav>
                        <div data-settings-view="profile">PROFILE</div>
                        <div data-settings-view="contact" hidden>CONTACT</div>
                        <div data-settings-view="security" hidden>
                          <section id="password">PASSWORD</section>
                        </div>
                      </main>
                    </body>
                    """);
            page.evaluate("""
                    () => {
                      window.__scrollCalls = 0;
                      Element.prototype.scrollIntoView = function () { window.__scrollCalls += 1; };
                    }
                    """);
            page.addScriptTag(new Page.AddScriptTagOptions().setPath(ASSETS.resolve("account-center.js")));

            assertEquals(Boolean.FALSE, page.evaluate("() => document.querySelector('[data-settings-view=profile]').hidden"));
            assertEquals(Boolean.TRUE, page.evaluate("() => document.querySelector('[data-settings-view=security]').hidden"));
            assertEquals(Boolean.TRUE, page.evaluate("() => document.querySelector('[data-settings-target=profile]').classList.contains('is-active')"));

            page.evaluate("() => { location.hash = '#password'; }");
            page.waitForFunction("() => !document.querySelector('[data-settings-view=security]').hidden");
            page.waitForTimeout(30);
            assertEquals(1, ((Number) page.evaluate("() => window.__scrollCalls")).intValue());
        }
    }

    @Test
    void brokenAvatarFromAnotherRendererFallsBackToInitial() {
        try (Playwright playwright = Playwright.create(); Browser browser = playwright.chromium().launch()) {
            Page page = browser.newPage();
            page.setContent("""
                    <body data-page="account-settings">
                      <span class="account-avatar" data-profile-avatar-preview></span>
                      <form id="accountProfileFormV2">
                        <input name="displayName">
                        <input name="avatarFile" type="file">
                        <div data-profile-msg hidden></div>
                        <button type="submit" data-submit>保存资料</button>
                      </form>
                    </body>
                    """);
            page.evaluate("""
                    () => {
                      const profile = {displayName:'旧昵称', avatarUrl:'', publicUserId:'U-1'};
                      window.fetch = async (url) => {
                        if (url === '/api/account/me') {
                          return new Response(JSON.stringify(profile), {status:200, headers:{'Content-Type':'application/json'}});
                        }
                        throw new Error('unexpected fetch ' + url);
                      };
                    }
                    """);
            page.addScriptTag(new Page.AddScriptTagOptions().setPath(ASSETS.resolve("account-profile-controller.js")));
            page.waitForFunction("() => document.querySelector('[name=displayName]').value === '旧昵称'");

            page.evaluate("""
                    () => {
                      const host = document.querySelector('.account-avatar');
                      host.dataset.avatarInitial = '旧';
                      const image = document.createElement('img');
                      image.src = '/broken-avatar.png';
                      host.replaceChildren(image);
                      image.dispatchEvent(new Event('error'));
                    }
                    """);

            assertEquals("旧", page.locator(".account-avatar-fallback").textContent());
            assertEquals(0, page.locator(".account-avatar img").count());
        }
    }
}
