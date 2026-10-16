package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.*;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.AriaRole;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

@EnabledIfEnvironmentVariable(named="PLAYWRIGHT_UI_TESTS",matches="1")
class XingweiAdminAccountLifecyclePlaywrightTest {
    Page setup(Browser browser) throws Exception {
        Page page=browser.newPage(new Browser.NewPageOptions().setViewportSize(390,844));
        String html=Files.readString(Path.of("src/main/resources/templates/admin-xingwei-security.html")).replace("th:content=\"${csrf.token}\"","content=\"test-csrf\"").replace("th:content=\"${csrf.headerName}\"","content=\"X-CSRF-TOKEN\"").replace("th:attr=\"data-admin-username=${username}\"","data-admin-username=\"first\"");
        page.route("**/*",r->{String path=java.net.URI.create(r.request().url()).getPath();if(path.startsWith("/assets/"))r.fulfill(new Route.FulfillOptions().setPath(Path.of("src/main/resources/static"+path)));else r.fulfill(new Route.FulfillOptions().setContentType("text/html").setBody(html));});
        page.addInitScript("""
            window.calls=[];window.conflict=false;window.canManage=true;window.failRead=false;
            window.fetch=async(url,options={})=>{let method=options.method||'GET';window.calls.push({url,method,headers:Object.fromEntries(new Headers(options.headers||{})),body:options.body});
              if(method!=='GET')return new Response(JSON.stringify(window.conflict?{message:'账号已变化，请重新读取'}:{message:'已保存，旧会话已撤销'}),{status:window.conflict?409:200});
              if(window.failRead)return new Response(JSON.stringify({message:'读取失败'}),{status:503});
              return new Response(JSON.stringify({canCreate:window.canManage,canManage:window.canManage,restoreAdmin:true,roles:[{code:'XW_MANAGEMENT',name:'管理层',enabled:true}],departments:[{code:'01',name:'管理中心'}],items:[{id:3,username:'operator',displayName:'测试员工',legacyRole:'OPERATOR',loginEnabled:true,roleCodes:['XW_MANAGEMENT'],authVersion:5},{id:1,username:'first',legacyRole:'ADMIN',loginEnabled:true,roleCodes:[],authVersion:0}]}));};
            """);
        page.navigate("http://xinghuo.test/admin/xingwei/security");page.waitForSelector(".xaa-record");return page;
    }
    @Test void chromiumCreateAndPasswordResetSendActualFormValuesWithoutReadingCredentials() throws Exception {
        try(Playwright p=Playwright.create();Browser b=p.chromium().launch()){
            Page page=setup(b);page.locator("#adminAccountCreate").click();page.locator("#adminAccount-username").fill("new.staff");page.locator("#adminAccount-displayName").fill("新员工");page.locator("#adminAccount-password").fill("Sample-Login-913!");page.locator("#adminAccount-roleCodes").selectOption("XW_MANAGEMENT");page.locator("#adminAccountSave").click();page.waitForFunction("()=>!document.querySelector('#adminAccountDialog').open");
            assertEquals("new.staff",page.evaluate("()=>JSON.parse(window.calls.find(c=>c.method==='POST').body).username"));assertEquals("Sample-Login-913!",page.evaluate("()=>JSON.parse(window.calls.find(c=>c.method==='POST').body).password"));assertEquals("test-csrf",page.evaluate("()=>window.calls.find(c=>c.method==='POST').headers['x-csrf-token']"));
            page.locator(".xaa-record").first().getByRole(AriaRole.BUTTON,new Locator.GetByRoleOptions().setName("重置密码").setExact(true)).click();page.locator("#adminAccount-newPassword").fill("Next-Login-931!");page.locator("#adminAccountSave").click();page.waitForFunction("()=>!document.querySelector('#adminAccountDialog').open");
            assertEquals(5,page.evaluate("()=>JSON.parse(window.calls.find(c=>c.url.endsWith('/3/password')).body).authVersion"));assertEquals("Next-Login-931!",page.evaluate("()=>JSON.parse(window.calls.find(c=>c.url.endsWith('/3/password')).body).newPassword"));
            assertFalse(page.locator("#adminAccountRows").textContent().contains("Sample-Login-913!"));
        }
    }
    @Test void webkitConflictKeepsInputSelfDisableIsUnavailableAndReadsRecover() throws Exception {
        try(Playwright p=Playwright.create();Browser b=p.webkit().launch()){
            Page page=setup(b);page.locator(".xaa-record").first().getByRole(AriaRole.BUTTON,new Locator.GetByRoleOptions().setName("登录状态").setExact(true)).click();page.locator("#adminAccount-enabled").uncheck();page.evaluate("()=>window.conflict=true");page.locator("#adminAccountSave").click();page.waitForFunction("()=>document.querySelector('#adminAccountFormMessage').textContent.includes('账号已变化')");assertFalse(page.locator("#adminAccount-enabled").isChecked());assertFalse(page.locator("#adminAccountSave").isDisabled());assertEquals(false,page.evaluate("()=>JSON.parse(window.calls.find(c=>c.method==='PATCH').body).enabled"));page.locator("#adminAccountClose").click();
            page.locator(".xaa-record").nth(1).getByRole(AriaRole.BUTTON,new Locator.GetByRoleOptions().setName("登录状态").setExact(true)).click();assertTrue(page.locator("#adminAccount-enabled").isDisabled());assertTrue(page.locator("#adminAccount-legacyRole").isDisabled());page.locator("#adminAccountClose").click();
            page.evaluate("()=>window.failRead=true");page.locator("#adminAccountRefresh").click();page.waitForFunction("()=>document.querySelector('#adminAccountMessage').textContent.includes('读取失败')");assertEquals("false",page.locator("#adminAccountRows").getAttribute("aria-busy"));
            page.evaluate("()=>{window.failRead=false;window.canManage=false;}");page.locator("#adminAccountRefresh").click();page.waitForFunction("()=>document.querySelector('#adminAccountCreate').hidden");assertEquals(0,page.locator(".xaa-record button").count());assertEquals(true,page.evaluate("()=>document.documentElement.scrollWidth<=innerWidth"));
        }
    }
}
