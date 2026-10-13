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
class AccountAuthBundlePlaywrightTest {
    private static final Path ASSETS = Path.of("src/main/resources/static/assets");

    @Test
    void dedicatedAuthBundleBootsLoginRegisterAndResetAcrossBrowsers() {
        try (Playwright playwright = Playwright.create()) {
            verify(playwright.chromium().launch());
            verify(playwright.webkit().launch());
        }
    }

    private void verify(Browser browser) {
        try (browser) {
            verifyLogin(browser);
            verifyRegister(browser);
            verifyReset(browser);
        }
    }

    private void verifyLogin(Browser browser) {
        Page page = browser.newPage();
        page.setContent("""
                <body data-page="account-login">
                  <form id="accountLoginForm">
                    <input type="hidden" name="redirect" value="">
                    <input type="hidden" name="contactType" value="EMAIL" data-contact-type>
                    <div class="field">
                      <input name="contactValue" type="email">
                      <small data-login-contact-hint></small>
                      <small id="loginContactError"></small>
                    </div>
                    <label class="field password-field">
                      <input name="password" type="password">
                      <button type="button" data-toggle-password>显示</button>
                    </label>
                    <div id="accountMsg"></div>
                    <button type="submit" data-submit>登录</button>
                  </form>
                </body>
                """);
        loadAuthRuntime(page);

        assertEquals(Boolean.TRUE, page.evaluate("() => !!window.XHAccountCore"));
        assertEquals(Boolean.TRUE, page.evaluate("() => !!window.XHAccount && window.XHAccount === window.XHAccountAuth"));
        page.locator("[data-toggle-password]").click();
        assertEquals("text", page.locator("input[name=password]").getAttribute("type"));
        assertEquals("隐藏", page.locator("[data-toggle-password]").textContent());

        page.locator("#accountLoginForm [data-submit]").click();
        assertEquals("true", page.locator("input[name=contactValue]").getAttribute("aria-invalid"));
        page.close();
    }

    private void verifyRegister(Browser browser) {
        Page page = browser.newPage();
        page.setContent("""
                <body data-page="account-register">
                  <form id="accountRegisterForm">
                    <input type="hidden" name="contactType" value="EMAIL" data-contact-type>
                    <div class="field"><input name="displayName"></div>
                    <div class="field"><label data-contact-label></label><input name="contactValue" type="email"><small data-contact-hint></small></div>
                    <div class="field"><input name="code"></div>
                    <button type="button" data-register-code>发送验证码</button>
                    <label class="field password-field"><input name="password" type="password"><button type="button" data-toggle-password>显示</button></label>
                    <div data-password-meter data-score="0"><i></i><span></span></div>
                    <label class="field password-field"><input name="passwordConfirm" type="password"><button type="button" data-toggle-password>显示</button></label>
                    <input type="checkbox" name="agree">
                    <div id="accountMsg"></div>
                    <button type="submit" data-submit>创建账号</button>
                  </form>
                </body>
                """);
        loadAuthRuntime(page);

        page.locator("input[name=password]").fill("Abc12345!");
        int score = Integer.parseInt(page.locator("[data-password-meter]").getAttribute("data-score"));
        assertTrue(score >= 2);
        assertTrue(page.locator("[data-password-meter] span").textContent().contains("密码强度"));

        page.locator("#accountRegisterForm [data-submit]").click();
        assertEquals("true", page.locator("input[name=displayName]").getAttribute("aria-invalid"));
        assertEquals("true", page.locator("input[name=contactValue]").getAttribute("aria-invalid"));
        page.close();
    }

    private void verifyReset(Browser browser) {
        Page page = browser.newPage();
        page.setContent("""
                <body data-page="account-reset">
                  <form id="accountResetConfirmForm">
                    <div class="field"><input name="email" type="email"></div>
                    <div class="field"><input name="code"></div>
                    <div class="field"><div class="password-field"><input name="newPassword" type="password"><button type="button" data-toggle-password>显示</button></div></div>
                    <div class="field"><div class="password-field"><input name="confirmPassword" type="password"><button type="button" data-toggle-password>显示</button></div></div>
                    <div id="accountResetConfirmMsg"></div>
                    <button type="submit" data-submit>确认重置</button>
                  </form>
                </body>
                """);
        loadAuthRuntime(page);

        page.locator("#accountResetConfirmForm [data-submit]").click();
        assertEquals("true", page.locator("input[name=email]").getAttribute("aria-invalid"));
        assertEquals("true", page.locator("input[name=code]").getAttribute("aria-invalid"));
        assertEquals("true", page.locator("input[name=newPassword]").getAttribute("aria-invalid"));
        page.close();
    }

    private void loadAuthRuntime(Page page) {
        page.addScriptTag(new Page.AddScriptTagOptions().setPath(ASSETS.resolve("account-core.js")));
        page.addScriptTag(new Page.AddScriptTagOptions().setPath(ASSETS.resolve("account-auth.js")));
    }
}
