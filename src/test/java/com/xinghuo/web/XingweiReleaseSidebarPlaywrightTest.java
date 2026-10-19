package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.*;
import com.microsoft.playwright.*;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

@EnabledIfEnvironmentVariable(named="PLAYWRIGHT_UI_TESTS",matches="1")
class XingweiReleaseSidebarPlaywrightTest {
    private Browser chromium(Playwright p){var options=new BrowserType.LaunchOptions();String executable=System.getenv("XINGHUO_TEST_CHROMIUM");if(executable!=null&&!executable.isBlank())options.setExecutablePath(Path.of(executable));return p.chromium().launch(options);}
    private String render(String template){
        var resolver=new ClassLoaderTemplateResolver();resolver.setPrefix("templates/");resolver.setSuffix(".html");var engine=new SpringTemplateEngine();engine.setTemplateResolver(resolver);
        var context=new Context();context.setVariables(Map.of("workspaceName","星维网络服务工作室","username","测试员工","restoreAdmin",false,"csrf",new DefaultCsrfToken("X-CSRF-TOKEN","_csrf","test-token"),"consoleVersion","20261016-v6a","consoleStage","V6A · UNIFIED DELIVERY","consoleStageLabel","V6A · 统一交付","releaseHistory",List.of(Map.of("date","2026-10-16","version","V6A","status","本版本","title","交付记录","detail","具体部署结果由发布记录确认"))));return engine.process(template,context);
    }
    @Test void chromiumNativeBrandKeepsTechnicalMetadataHidden()throws Exception{try(Playwright p=Playwright.create();Browser b=chromium(p)){journey(b);}}
    @Test void webkitNativeBrandKeepsTechnicalMetadataHidden()throws Exception{try(Playwright p=Playwright.create();Browser b=p.webkit().launch()){journey(b);}}
    private void journey(Browser b)throws Exception {
        String home=render("admin-xingwei");
        Page page=b.newPage(new Browser.NewPageOptions().setViewportSize(1440,900));
        page.route("**/*",r->{String path=java.net.URI.create(r.request().url()).getPath();if(path.startsWith("/assets/"))r.fulfill(new Route.FulfillOptions().setPath(Path.of("src/main/resources/static"+path)));else r.fulfill(new Route.FulfillOptions().setContentType("text/html").setBody(home));});
        page.addInitScript("""
            window.mismatch=false;
            window.fetch=async url=>{
              if(url.endsWith('/access'))return new Response(JSON.stringify({mode:'SHADOW_RBAC',dataScope:'SELF',allowAll:false,permissionCodes:['order:view']}));
              if(url.endsWith('/health'))return new Response(JSON.stringify({version:window.mismatch?'other-version':'20261016-v6a',stage:'V6A · UNIFIED DELIVERY',serverTime:'2026-10-16T23:30:00+08:00'}));
              return new Response(JSON.stringify({error:'无组织查看权限'}),{status:403});
            };
            """);
        page.navigate("http://xinghuo.test/admin/xingwei");
        page.waitForFunction("()=>document.documentElement.dataset.xwRuntimeState==='match'");
        assertTrue(page.locator(".xw-runtime-metadata").isHidden());
        assertEquals(0,page.locator("#xwReleaseMenu,#xwReleaseHistoryLink,a[href*='legacy=1']").count());
        assertFalse(page.locator("body").innerText().contains("20261016-v6a"));
        assertFalse(page.locator("#xwBackendStatus").getAttribute("title").contains("20261016"));
        page.waitForFunction("()=>document.querySelector('.xw-brand-mark').naturalWidth>0");
        assertTrue(page.locator(".xw-brand-mark").getAttribute("src").startsWith("/assets/brand-logo.svg"));
        page.screenshot(new Page.ScreenshotOptions().setPath(Path.of("/tmp/admin-cutover-desktop.png")));
        for(int width:new int[]{390,320}){
            page.setViewportSize(width,844);page.evaluate("()=>scrollTo(0,0)");
            if(!"true".equals(page.locator("#xwMenuButton").getAttribute("aria-expanded")))page.locator("#xwMenuButton").click();
            assertEquals("true",page.locator("#xwMenuButton").getAttribute("aria-expanded"));
            page.waitForFunction("()=>Math.abs(document.querySelector('#xwSidebar').getBoundingClientRect().left)<1");
            assertEquals(true,page.evaluate("()=>document.documentElement.scrollWidth<=innerWidth+1"));
        }
        page.screenshot(new Page.ScreenshotOptions().setPath(Path.of("/tmp/admin-cutover-mobile.png")));
        page.evaluate("()=>{window.mismatch=true;dispatchEvent(new Event('online'));}");
        page.waitForFunction("()=>document.documentElement.dataset.xwRuntimeState==='mismatch'");
        assertTrue(page.locator("#xwBackendStatus").textContent().contains("页面需要更新"));
        assertFalse(page.locator("body").innerText().contains("other-version"));
        assertFalse(page.locator("#xwBackendStatus").getAttribute("title").contains("other-version"));
        assertEquals(true,page.evaluate("()=>document.documentElement.scrollWidth<=innerWidth+1"));
    }
}
