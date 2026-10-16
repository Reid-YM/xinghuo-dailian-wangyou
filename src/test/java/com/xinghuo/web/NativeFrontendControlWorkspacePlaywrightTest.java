package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.playwright.*;
import com.xinghuo.repository.SiteSettingRepository;
import com.xinghuo.service.*;
import java.nio.file.Path;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

@EnabledIfEnvironmentVariable(named="PLAYWRIGHT_UI_TESTS",matches="1")
class NativeFrontendControlWorkspacePlaywrightTest {
    private Browser chromium(Playwright p){var options=new BrowserType.LaunchOptions();String executable=System.getenv("XINGHUO_TEST_CHROMIUM");if(executable!=null&&!executable.isBlank())options.setExecutablePath(Path.of(executable));return p.chromium().launch(options);}
    private Page setup(Browser browser,boolean manage)throws Exception{
        var resolver=new ClassLoaderTemplateResolver();resolver.setPrefix("templates/");resolver.setSuffix(".html");
        var engine=new SpringTemplateEngine();engine.setTemplateResolver(resolver);var context=new Context();context.setVariables(Map.of("xingweiMode",true,"csrf",new DefaultCsrfToken("X-CSRF-TOKEN","_csrf","test-token")));
        String html=engine.process("admin-xingwei-frontend-controls",context);assertFalse(html.contains("/assets/app.css"));
        var repo=mock(SiteSettingRepository.class);when(repo.findAll()).thenReturn(List.of());
        String fixture=new ObjectMapper().writeValueAsString(Map.of("global",new SiteSettingService(repo).globalUiSettings(),"front",new FrontendControlService(repo).settings(),"manage",manage));
        Page page=browser.newPage(new Browser.NewPageOptions().setViewportSize(1440,900));
        page.route("**/*",r->{String path=java.net.URI.create(r.request().url()).getPath();if(path.startsWith("/assets/"))r.fulfill(new Route.FulfillOptions().setPath(Path.of("src/main/resources/static"+path)));else r.fulfill(new Route.FulfillOptions().setContentType("text/html").setBody(html));});
        page.addInitScript("""
            const fixture=FIXTURE;
            window.calls=[];window.failRead=false;window.confirm=()=>true;window.conflict=false;window.hold=false;window.revision={global:1,front:1};
            window.fetch=async(url,options={})=>{
              const method=options.method||'GET';window.calls.push({url,method,headers:options.headers,body:options.body});
              if(method==='GET'&&window.failRead)return new Response(JSON.stringify({message:'暂时无法读取配置'}),{status:503});
              if(url.endsWith('/access'))return new Response(JSON.stringify({canManageSite:fixture.manage}));
              const key=url.endsWith('/global-ui')?'global':'front';
              if(method==='PATCH'){
                if(window.hold)await new Promise(resolve=>window.releaseSave=resolve);
                if(window.conflict)return new Response(JSON.stringify({message:'配置已变化，请核对当前输入'}),{status:409});
                Object.assign(fixture[key],JSON.parse(options.body));window.revision[key]++;
              }
              return new Response(JSON.stringify(fixture[key]),{headers:{ETag:'"'+key+window.revision[key]+'"'}});
            };
            """.replace("FIXTURE",fixture));
        page.navigate("http://xinghuo.test/admin/xingwei/frontend-controls");
        page.waitForFunction("()=>document.querySelector('#globalUiState').textContent.includes('已读取')||document.querySelector('#globalUiState').textContent.includes('只读')");return page;
    }
    @Test void chromiumNativeShellSavesWithRevisionsAndRetainsOtherFormDraft()throws Exception{try(Playwright p=Playwright.create();Browser b=chromium(p)){journey(b);}}
    @Test void webkitNativeShellSavesWithRevisionsAndRetainsOtherFormDraft()throws Exception{try(Playwright p=Playwright.create();Browser b=p.webkit().launch()){journey(b);}}
    @Test void chromiumNativeLoginKeepsCsrfAndAccessibleMobileForm()throws Exception{try(Playwright p=Playwright.create();Browser b=chromium(p)){login(b);}}
    @Test void webkitNativeLoginKeepsCsrfAndAccessibleMobileForm()throws Exception{try(Playwright p=Playwright.create();Browser b=p.webkit().launch()){login(b);}}
    private void login(Browser browser)throws Exception{
        var resolver=new ClassLoaderTemplateResolver();resolver.setPrefix("templates/");resolver.setSuffix(".html");
        var engine=new SpringTemplateEngine();engine.setTemplateResolver(resolver);var context=new Context();
        context.setVariables(Map.of("_csrf",new DefaultCsrfToken("X-CSRF-TOKEN","_csrf","login-token"),"param",Map.of("error",List.of("true"),"logout",false)));
        String html=engine.process("admin-login",context);Page page=browser.newPage();
        page.route("**/*",r->{String path=java.net.URI.create(r.request().url()).getPath();if(path.startsWith("/assets/"))r.fulfill(new Route.FulfillOptions().setPath(Path.of("src/main/resources/static"+path)));else r.fulfill(new Route.FulfillOptions().setContentType("text/html").setBody(html));});
        page.navigate("http://xinghuo.test/admin/login");
        assertEquals("login-token",page.locator("input[name=_csrf]").inputValue());assertEquals("/login",page.locator("form").getAttribute("action"));
        assertTrue(page.locator("[role=alert]").isVisible());assertEquals("username",page.locator("#username").getAttribute("autocomplete"));assertEquals("current-password",page.locator("#password").getAttribute("autocomplete"));
        page.setViewportSize(1440,900);page.screenshot(new Page.ScreenshotOptions().setPath(Path.of("/tmp/native-login-desktop.png")));
        page.setViewportSize(390,844);assertEquals(true,page.evaluate("()=>document.documentElement.scrollWidth<=innerWidth"));assertTrue(page.locator("button[type=submit]").isVisible());page.screenshot(new Page.ScreenshotOptions().setPath(Path.of("/tmp/native-login-mobile.png")));
    }
    private void journey(Browser browser)throws Exception{
        Page page=setup(browser,true);
        assertEquals(1,page.locator("body.xw-frontend-native").count());assertEquals(0,page.locator("link[href*='admin-brand-layout-fixes'],link[href*='admin-light-theme'],link[href*='/assets/app.css']").count());
        page.locator("input[name=navServicesLabel]").fill("服务新入口");page.locator("input[name=homePrimaryCtaLabel]").fill("另一分区尚未保存");
        page.evaluate("()=>window.hold=true");page.locator("#globalUiForm button[type=submit]").click();page.waitForFunction("()=>typeof window.releaseSave==='function'");
        page.evaluate("()=>document.querySelector('#globalUiForm').dispatchEvent(new Event('submit',{bubbles:true,cancelable:true}))");
        assertEquals(1,page.evaluate("()=>window.calls.filter(c=>c.method==='PATCH').length"));page.evaluate("()=>{window.hold=false;window.releaseSave();}");
        page.waitForFunction("()=>document.querySelector('#globalUiState').textContent.includes('保存成功')");
        assertEquals("服务新入口",page.locator("input[name=navServicesLabel]").inputValue());
        assertEquals("另一分区尚未保存",page.locator("input[name=homePrimaryCtaLabel]").inputValue());
        assertEquals(true,page.evaluate("()=>{const e=new Event('beforeunload',{cancelable:true});window.dispatchEvent(e);return e.defaultPrevented;}"));
        assertEquals(true,page.evaluate("()=>{const c=window.calls.find(c=>c.method==='PATCH');return c.headers['If-Match']==='\"global1\"'&&c.headers['X-CSRF-TOKEN']==='test-token'&&JSON.parse(c.body).navServicesLabel==='服务新入口';}"));
        page.evaluate("()=>window.conflict=true");page.locator("input[name=navServicesLabel]").fill("冲突草稿");page.locator("#globalUiForm button[type=submit]").click();page.waitForFunction("()=>document.querySelector('#globalUiState').textContent.includes('保存失败')");assertEquals("冲突草稿",page.locator("input[name=navServicesLabel]").inputValue());
        assertEquals(true,page.evaluate("()=>window.calls.every(c=>c.url.startsWith('/api/admin/xingwei/site/'))"));
        page.evaluate("()=>window.failRead=true");page.locator("#frontendControlReload").click();page.waitForFunction("()=>document.querySelector('#globalUiState').textContent.includes('读取失败')");assertEquals("冲突草稿",page.locator("input[name=navServicesLabel]").inputValue());
        page.evaluate("()=>window.failRead=false");page.locator("#frontendControlReload").click();page.waitForFunction("()=>document.querySelector('#globalUiState').textContent.includes('已读取')");assertEquals("服务新入口",page.locator("input[name=navServicesLabel]").inputValue());
        page.evaluate("()=>scrollTo(0,0)");page.screenshot(new Page.ScreenshotOptions().setPath(Path.of("/tmp/native-controls-desktop.png")).setFullPage(false));
        page.setViewportSize(390,844);assertEquals(true,page.evaluate("()=>document.documentElement.scrollWidth<=innerWidth"));page.screenshot(new Page.ScreenshotOptions().setPath(Path.of("/tmp/native-controls-mobile.png")).setFullPage(false));
        Page viewer=setup(browser,false);assertTrue(viewer.locator("#globalUiForm button[type=submit]").isDisabled());assertTrue(viewer.locator("#frontendControlsForm button[type=submit]").isDisabled());viewer.close();
    }
}
