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
class FrontendNextN6AccountSecurityPlaywrightTest {
    private static final Path NEXT = Path.of("src/main/resources/static/next");

    @Test
    void chromiumUpdatesProfileVerifiesEmailAndManagesOtherSessions() {
        try (Playwright playwright = Playwright.create(); Browser browser = playwright.chromium().launch()) {
            Page page = page(browser);
            page.evaluate("""
                    () => {
                      window.__writes=[]; window.__reloads=0; window.confirm=()=>true;
                      window.XHFrontendNextN4={
                        state:{
                          profile:{displayName:'旧昵称',avatarUrl:'/api/site-assets/9',contactMasked:'t***@example.com',email:'test@example.com',emailVerified:false},
                          sessions:[
                            {id:'session-current-id',ip:'127.***.1',deviceLabel:'Chrome · 当前机',createdAt:'2026-10-14T00:00:00Z',lastSeenAt:'2026-10-14T01:00:00Z',current:true},
                            {id:'session-other-id',ip:'10.***.2',deviceLabel:'Safari · 手机',createdAt:'2026-10-13T00:00:00Z',lastSeenAt:'2026-10-14T00:30:00Z',current:false}
                          ]
                        },
                        reload:async()=>{window.__reloads++;}
                      };
                      window.fetch=async (url,options={}) => {
                        window.__writes.push({url,method:options.method||'GET',body:options.body||'',headers:options.headers||{}});
                        return new Response(JSON.stringify({ok:true,message:'操作成功'}),{status:200,headers:{'Content-Type':'application/json'}});
                      };
                    }
                    """);
            page.addScriptTag(new Page.AddScriptTagOptions().setPath(NEXT.resolve("n6-account-security.js")));
            page.waitForSelector("#n6AccountSecurity");

            assertTrue(page.locator("#n6AccountSecurity").textContent().contains("账号设置与安全"));
            assertTrue(page.locator("#n6Devices").textContent().contains("Chrome · 当前机 · 当前设备"));
            assertTrue(page.locator("#n6Devices").textContent().contains("Safari · 手机"));
            assertTrue(!page.locator("#n6Devices").textContent().contains("session-current-id"));
            assertTrue(!page.locator("#n6Devices").textContent().contains("session-other-id"));

            page.locator("#n6DisplayName").fill("新昵称");
            page.locator("#n6ProfileForm button[type=submit]").click();
            page.waitForFunction("() => window.__writes.length===1 && window.__reloads===1");
            assertEquals("/api/account/profile", page.evaluate("() => window.__writes[0].url"));
            assertEquals("新昵称", page.evaluate("() => JSON.parse(window.__writes[0].body).displayName"));
            assertEquals("/api/site-assets/9", page.evaluate("() => JSON.parse(window.__writes[0].body).avatarUrl"));

            page.locator("#n6Email").fill("new@example.com");
            page.locator("#n6EmailRequestForm button[type=submit]").click();
            page.waitForFunction("() => window.__writes.length===2");
            assertEquals("/api/account/email/verify/request", page.evaluate("() => window.__writes[1].url"));
            assertEquals("new@example.com", page.evaluate("() => JSON.parse(window.__writes[1].body).email"));

            page.locator("#n6EmailCode").fill("123456");
            page.locator("#n6EmailConfirmForm button[type=submit]").click();
            page.waitForFunction("() => window.__writes.length===3 && window.__reloads===2");
            assertEquals("/api/account/email/verify/confirm", page.evaluate("() => window.__writes[2].url"));
            assertEquals("123456", page.evaluate("() => JSON.parse(window.__writes[2].body).code"));

            page.locator("#n6Devices .n6-device").nth(1).locator("button").click();
            page.waitForFunction("() => window.__writes.length===4 && window.__reloads===3");
            assertEquals("/api/account/sessions/session-other-id", page.evaluate("() => window.__writes[3].url"));
            assertEquals("DELETE", page.evaluate("() => window.__writes[3].method"));

            page.locator("#n6RevokeOthers").click();
            page.waitForFunction("() => window.__writes.length===5 && window.__reloads===4");
            assertEquals("/api/account/sessions/revoke-all", page.evaluate("() => window.__writes[4].url"));
            assertEquals("POST", page.evaluate("() => window.__writes[4].method"));
            assertEquals("XMLHttpRequest", page.evaluate("() => window.__writes[4].headers['X-Requested-With']"));
        }
    }

    @Test
    void webkitRejectsWeakPasswordClientSideThenPostsStrongChangeWithConfirmation() {
        try (Playwright playwright = Playwright.create(); Browser browser = playwright.webkit().launch()) {
            Page page = page(browser);
            page.evaluate("""
                    () => {
                      window.__writes=[]; window.__reloads=0; window.__confirms=0;
                      window.confirm=()=>{window.__confirms++; return true;};
                      window.XHFrontendNextN4={
                        state:{profile:{displayName:'用户',avatarUrl:null,email:'secure@example.com',emailVerified:true},sessions:[{id:'session-current-only',ip:'127.***.1',deviceLabel:'WebKit',createdAt:'2026-10-14T00:00:00Z',lastSeenAt:'2026-10-14T01:00:00Z',current:true}]},
                        reload:async()=>{window.__reloads++;}
                      };
                      window.fetch=async (url,options={}) => {
                        window.__writes.push({url,method:options.method||'GET',body:options.body||'',headers:options.headers||{}});
                        return new Response(JSON.stringify({ok:true,message:'密码已修改，请重新登录'}),{status:200,headers:{'Content-Type':'application/json'}});
                      };
                    }
                    """);
            page.addScriptTag(new Page.AddScriptTagOptions().setPath(NEXT.resolve("n6-account-security.js")));
            page.waitForSelector("#n6PasswordForm");

            String oldValue = "O" + "l".repeat(7) + "1!";
            String weakValue = "a".repeat(8);
            String strongValue = "N" + "e".repeat(7) + "2!";
            page.locator("#n6OldPassword").fill(oldValue);
            page.locator("#n6NewPassword").fill(weakValue);
            page.locator("#n6ConfirmPassword").fill(weakValue);
            page.locator("#n6PasswordForm button[type=submit]").click();
            page.waitForSelector("#n6PasswordMessage:has-text('至少 8 位，并至少包含两类字符')");
            assertEquals(0, ((Number) page.evaluate("() => window.__writes.length")).intValue());
            assertEquals(0, ((Number) page.evaluate("() => window.__confirms")).intValue());

            page.locator("#n6NewPassword").fill(strongValue);
            page.locator("#n6ConfirmPassword").fill(strongValue);
            page.locator("#n6PasswordForm button[type=submit]").click();
            page.waitForFunction("() => window.__writes.length===1 && window.__reloads===1 && window.__confirms===1");
            assertEquals("/api/account/password/change", page.evaluate("() => window.__writes[0].url"));
            assertEquals(oldValue, page.evaluate("() => JSON.parse(window.__writes[0].body).oldPassword"));
            assertEquals(strongValue, page.evaluate("() => JSON.parse(window.__writes[0].body).newPassword"));
            assertEquals("XMLHttpRequest", page.evaluate("() => window.__writes[0].headers['X-Requested-With']"));
            assertTrue(page.locator("#n6Toast").textContent().contains("密码已修改"));
        }
    }

    private static Page page(Browser browser) {
        Page page = browser.newPage();
        page.route("**/*", route -> route.fulfill(new Route.FulfillOptions()
                .setContentType("text/html")
                .setBody("""
                        <body>
                          <section class="page active" data-page="account">
                            <section class="n4-shell"></section>
                            <section class="n4-section"></section>
                            <section id="n5BusinessCenter"></section>
                          </section>
                          <div class="badge">FRONTEND NEXT</div>
                        </body>
                        """)));
        page.navigate("https://example.test/next/account");
        return page;
    }
}
