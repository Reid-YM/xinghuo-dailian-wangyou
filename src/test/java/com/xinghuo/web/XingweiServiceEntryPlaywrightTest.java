package com.xinghuo.web;

import com.microsoft.playwright.*;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import static org.junit.jupiter.api.Assertions.*;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

@EnabledIfEnvironmentVariable(named="PLAYWRIGHT_UI_TESTS",matches="1")
class XingweiServiceEntryPlaywrightTest {
    @Test void nativeServiceEntryPreservesPermissionsCsrfAndConcurrentUpdateProtection() {
        try (Playwright p=Playwright.create()) {
            for (BrowserType type : new BrowserType[]{p.chromium(),p.webkit()}) {
                try (Browser browser=type.launch()) { verify(browser); }
            }
        }
    }
    private void verify(Browser browser) {
        var resolver=new ClassLoaderTemplateResolver();resolver.setPrefix("templates/");resolver.setSuffix(".html");
        var engine=new SpringTemplateEngine();engine.setTemplateResolver(resolver);
        var context=new Context();context.setVariables(Map.of("csrf",new DefaultCsrfToken("X-CSRF-TOKEN","_csrf","test-token")));
        String html=engine.process("admin-xingwei-frontend-next",context);
        Page page=browser.newPage(new Browser.NewPageOptions().setViewportSize(390,844));
        page.route("**/*",route->{String path=java.net.URI.create(route.request().url()).getPath();
            if(path.startsWith("/assets/"))route.fulfill(new Route.FulfillOptions().setPath(Path.of("src/main/resources/static"+path)));
            else route.fulfill(new Route.FulfillOptions().setContentType("text/html").setBody(html));});
        page.addInitScript("""
          window.writes=[];window.reads=[];window.canManage=true;window.mode='legacy';window.revision='"one"';window.conflict=false;window.failRead=false;
          window.fetch=async(url,options={})=>{
            if(!url.startsWith('/api/admin/xingwei/site/'))throw new Error('Unexpected legacy endpoint');
            if(options.method==='PATCH'){
              window.writes.push({url,body:JSON.parse(options.body),headers:options.headers});
              if(window.conflict)return new Response('{}',{status:409});
              window.mode=JSON.parse(options.body).servicesEntryMode;window.revision='"two"';
              return new Response(JSON.stringify({servicesEntryMode:window.mode}),{headers:{ETag:window.revision}});
            }
            window.reads.push(url);
            if(url.endsWith('/access'))return new Response(JSON.stringify({canManageSite:window.canManage}));
            if(window.failRead)return new Response('{}',{status:500});
            return new Response(JSON.stringify({servicesEntryMode:window.mode}),{headers:{ETag:window.revision}});
          };
          """);
        page.onDialog(Dialog::accept);
        page.navigate("https://xinghuo.test/admin/xingwei/frontend-next");
        assertThat(page.locator("#saveRolloutMode")).isEnabled();
        page.locator("#servicesEntryMode").selectOption("next");
        page.locator("#saveRolloutMode").click();
        assertThat(page.locator("#rolloutMessage")).containsText("保存成功");
        assertEquals("/api/admin/xingwei/site/global-ui",page.evaluate("()=>window.writes[0].url"));
        assertEquals("\"one\"",page.evaluate("()=>window.writes[0].headers['If-Match']"));
        assertEquals("test-token",page.evaluate("()=>window.writes[0].headers['X-CSRF-TOKEN']"));
        assertEquals("next",page.evaluate("()=>window.writes[0].body.servicesEntryMode"));
        page.evaluate("()=>window.conflict=true");
        page.locator("#servicesEntryMode").selectOption("legacy");
        page.locator("#saveRolloutMode").click();
        assertThat(page.locator("#rolloutMessage")).containsText("其他人员修改");
        assertThat(page.locator("#saveRolloutMode")).isDisabled();
        assertEquals("\"two\"",page.evaluate("()=>window.writes[1].headers['If-Match']"));
        page.evaluate("()=>{window.conflict=false;window.canManage=false;}");
        page.locator("#reloadRolloutMode").click();
        assertThat(page.locator("#rolloutMessage")).containsText("查看权限");
        assertThat(page.locator("#saveRolloutMode")).isDisabled();
        assertThat(page.locator("#servicesEntryMode")).isDisabled();
        assertEquals(2,page.evaluate("()=>window.writes.length"));
        page.evaluate("()=>{window.canManage=true;window.failRead=true;}");
        page.locator("#reloadRolloutMode").click();
        assertThat(page.locator("#rolloutMessage")).containsText("请求失败");
        assertThat(page.locator("#saveRolloutMode")).isDisabled();
        page.evaluate("()=>window.failRead=false");
        page.locator("#reloadRolloutMode").click();
        assertThat(page.locator("#saveRolloutMode")).isEnabled();
        assertFalse(page.locator("body").innerText().contains("/api/"));
        assertFalse(page.locator("body").innerText().contains("N9 Development"));
        assertEquals(true,page.evaluate("()=>document.documentElement.scrollWidth<=innerWidth+1"));
        page.close();
    }
}
