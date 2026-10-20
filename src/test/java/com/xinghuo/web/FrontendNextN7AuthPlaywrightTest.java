package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.Route;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

@EnabledIfEnvironmentVariable(named = "PLAYWRIGHT_UI_TESTS", matches = "1")
class FrontendNextN7AuthPlaywrightTest {
    private static final Path NEXT = Path.of("src/main/resources/static/next");

    @Test
    void chromiumNormalizesLoginToEmailAndPostsRegistrationToExistingAccountApi() {
        try (Playwright playwright = Playwright.create(); Browser browser = playwright.chromium().launch()) {
            Page page = page(browser);
            page.evaluate("""
                    () => {
                      window.__writes=[];
                      window.fetch=async (url,options={}) => {
                        window.__writes.push({url,method:options.method||'GET',body:options.body||'',headers:options.headers||{}});
                        if (url === '/api/account/register') {
                          return new Response(JSON.stringify({error:'CONTACT_TAKEN',message:'该邮箱已注册，请直接登录'}),{status:409,headers:{'Content-Type':'application/json'}});
                        }
                        return new Response(JSON.stringify({ok:true,message:'验证码已发送'}),{status:200,headers:{'Content-Type':'application/json'}});
                      };
                    }
                    """);
            page.addScriptTag(new Page.AddScriptTagOptions().setPath(NEXT.resolve("n7-auth.js")));
            page.waitForSelector("#n7AuthSwitcher");

            assertEquals(2, page.locator(".n4-login > *").count());
            assertEquals(1, page.locator("#n7AuthHost > #n4LoginForm").count());
            assertEquals(1, page.locator("#n4ContactType option").count());
            assertEquals("EMAIL", page.locator("#n4ContactType").inputValue());
            assertEquals("email", page.locator("#n4ContactValue").getAttribute("type"));
            assertTrue(page.locator("#n4LoginForm").textContent().contains("使用注册邮箱和密码登录"));

            page.locator(".n7-auth-tab[data-mode=register]").click();
            page.waitForSelector("#n7RegisterPanel:not([hidden])");
            page.locator("#n7RegisterEmail").fill("new-user@example.com");
            page.locator("#n7RegisterName").fill("新用户");
            page.locator("#n7RegisterSendCode").click();
            page.waitForFunction("() => window.__writes.length===1");
            assertEquals("/api/account/register/verify/request", page.evaluate("() => window.__writes[0].url"));
            assertEquals("new-user@example.com", page.evaluate("() => JSON.parse(window.__writes[0].body).email"));

            String strongValue = "N" + "e".repeat(7) + "4!";
            page.locator("#n7RegisterCode").fill("123456");
            page.locator("#n7RegisterPassword").fill(strongValue);
            page.locator("#n7RegisterConfirm").fill(strongValue);
            page.locator("#n7RegisterAgree").check();
            page.locator("#n7RegisterSubmit").click();
            page.waitForFunction("() => window.__writes.length===2");
            assertEquals("/api/account/register", page.evaluate("() => window.__writes[1].url"));
            assertEquals("EMAIL", page.evaluate("() => JSON.parse(window.__writes[1].body).contactType"));
            assertEquals("new-user@example.com", page.evaluate("() => JSON.parse(window.__writes[1].body).contactValue"));
            assertEquals("fetch", page.evaluate("() => window.__writes[1].headers['X-Requested-With']"));
            page.waitForSelector("#n7RegisterMessage:has-text('该邮箱已注册')");
        }
    }

    @Test
    void webkitRequestsResetThenReturnsToLoginAfterSuccessfulPasswordReset() {
        try (Playwright playwright = Playwright.create(); Browser browser = playwright.webkit().launch()) {
            Page page = page(browser);
            page.evaluate("""
                    () => {
                      window.__writes=[];
                      window.fetch=async (url,options={}) => {
                        window.__writes.push({url,method:options.method||'GET',body:options.body||'',headers:options.headers||{}});
                        const message=url.endsWith('/password/reset') ? '密码已重置，请用新密码登录' : '如果该邮箱已绑定账号，我们已发送重置验证码';
                        return new Response(JSON.stringify({ok:true,message}),{status:200,headers:{'Content-Type':'application/json'}});
                      };
                    }
                    """);
            page.addScriptTag(new Page.AddScriptTagOptions().setPath(NEXT.resolve("n7-auth.js")));
            page.waitForSelector("#n7AuthSwitcher");
            page.locator(".n7-auth-tab[data-mode=reset]").click();
            page.locator("#n7ResetEmail").fill("owner@example.com");
            page.locator("#n7ResetRequest").click();
            page.waitForFunction("() => window.__writes.length===1");
            assertEquals("/api/account/password/reset/request", page.evaluate("() => window.__writes[0].url"));
            page.waitForSelector("#n7ResetConfirmWrap:not([hidden])");

            String strongValue = "R" + "e".repeat(7) + "5!";
            page.locator("#n7ResetCode").fill("654321");
            page.locator("#n7ResetPassword").fill(strongValue);
            page.locator("#n7ResetConfirm").fill(strongValue);
            page.locator("#n7ResetSubmit").click();
            page.waitForFunction("() => window.__writes.length===2");
            assertEquals("/api/account/password/reset", page.evaluate("() => window.__writes[1].url"));
            assertEquals("owner@example.com", page.evaluate("() => JSON.parse(window.__writes[1].body).email"));
            assertEquals("fetch", page.evaluate("() => window.__writes[1].headers['X-Requested-With']"));
            page.waitForSelector("#n4LoginForm:not([hidden])");
            assertEquals("owner@example.com", page.locator("#n4ContactValue").inputValue());
            assertTrue(page.locator("#n4LoginMessage").textContent().contains("密码已重置"));
        }
    }

    private static Page page(Browser browser) {
        Page page = browser.newPage();
        page.route("**/*", route -> route.fulfill(new Route.FulfillOptions()
                .setContentType("text/html")
                .setBody("""
                        <body>
                          <section class="page active" data-page="account">
                            <div class="shell n4-login-wrap">
                              <div class="n4-login">
                                <section class="n4-login-copy"><h1>登录后查看真实服务记录</h1></section>
                                <form id="n4LoginForm" class="n4-login-form">
                                  <h2>账号登录</h2>
                                  <p class="n4-note">支持邮箱、手机号、QQ、微信号凭据；也可使用微信扫码登录。</p>
                                  <div class="n4-fields">
                                    <div class="n4-field"><label>登录方式</label><select id="n4ContactType"><option value="PHONE">手机号</option><option value="EMAIL">邮箱</option><option value="QQ">QQ</option><option value="WECHAT">微信号</option></select></div>
                                    <div class="n4-field"><label>账号</label><input id="n4ContactValue" autocomplete="username"></div>
                                    <div class="n4-field"><label>密码</label><input id="n4Password" type="password"></div>
                                  </div>
                                  <label class="n4-note"><input id="n4Remember" type="checkbox">在此设备保持登录</label>
                                  <button id="n4LoginButton" type="submit">登录并进入个人中心 →</button>
                                  <div id="n4LoginMessage" class="n4-message"></div>
                                  <div class="n4-divider">或</div>
                                  <button id="n4WechatLogin" type="button">微信开放平台扫码登录</button>
                                  <div class="n4-mini-links"><a href="/account/register">注册账号</a><a href="/account/reset">忘记密码</a></div>
                                </form>
                              </div>
                            </div>
                          </section>
                        </body>
                        """)));
        page.navigate("https://example.test/next/account");
        return page;
    }
}
