package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.microsoft.playwright.*;
import com.xinghuo.repository.SiteSettingRepository;
import com.xinghuo.service.FrontendControlService;
import com.xinghuo.service.SiteSettingService;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

@EnabledIfEnvironmentVariable(named="PLAYWRIGHT_UI_TESTS",matches="1")
class XingweiSiteControlPlaywrightTest {
    private Browser chromium(Playwright p){var options=new BrowserType.LaunchOptions();String executable=System.getenv("XINGHUO_TEST_CHROMIUM");if(executable!=null&&!executable.isBlank())options.setExecutablePath(Path.of(executable));return p.chromium().launch(options);}
    private static final Path ASSETS=Path.of("src/main/resources/static/assets");
    private String render(String name,boolean mode)throws Exception{
        var resolver=new ClassLoaderTemplateResolver();resolver.setPrefix("templates/");resolver.setSuffix(".html");var engine=new SpringTemplateEngine();engine.setTemplateResolver(resolver);
        var context=new Context(Locale.SIMPLIFIED_CHINESE);context.setVariable("xingweiMode",mode);context.setVariable("username","test");
        var csrf=new DefaultCsrfToken("X-CSRF-TOKEN","_csrf","test-csrf");context.setVariable("csrf",csrf);context.setVariable("_csrf",csrf);
        return engine.process(name.replace(".html",""),context).replaceAll("<script[^>]*></script>","");
    }
    private void setupSite(Page page,String section)throws Exception{
        String html=render("admin-xingwei-site.html",true);
        page.route("**/*",r->r.fulfill(new Route.FulfillOptions().setContentType("text/html;charset=utf-8").setBody(html)));
        page.navigate("https://example.test/admin/xingwei/site?section="+section);
        for(String css:List.of("admin-xingwei.css","admin-xingwei-channels.css","admin-xingwei-site.css"))page.addStyleTag(new Page.AddStyleTagOptions().setPath(ASSETS.resolve(css)));
        page.evaluate("""
          () => {window.calls=[];window.conflict=false;window.siteValues={brandName:'星火',legalEntityName:'星维网络',serviceStatus:'OPEN',statusShortText:'可接服务',primaryButtonLabel:'咨询服务',quoteEnabled:true,orderQueryEnabled:true,heroImage:'',heroImageHeight:220,heroImageFit:'cover',heroImagePositionX:50,heroImagePositionY:50,heroBackgroundImage:'',heroBackgroundOpacity:40,heroBackgroundFit:'cover',heroBackgroundPositionX:50,heroBackgroundPositionY:50,heroBackgroundBlur:0,serviceCoverHeight:180,serviceCoverFit:'cover',serviceCoverPositionX:50,serviceCoverPositionY:50};
            window.fetch=async(url,options={})=>{window.calls.push({url,method:options.method,body:options.body,headers:options.headers});
              if(url.endsWith('/access'))return new Response(JSON.stringify({canManageSite:true,canManageSeo:true,canViewSite:true}));
              if(url.includes('/images/'))return new Response(JSON.stringify({url:'/api/site-assets/test-image'}));
              if(options.method==='PATCH'){
                if(window.conflict)return new Response(JSON.stringify({error:'配置已变化，请重新读取；输入已保留'}),{status:409});
                Object.assign(window.siteValues,JSON.parse(options.body));
              }
              return new Response(JSON.stringify(window.siteValues),{headers:{ETag:'"revision1"'}});
            };
          }
          """);
        page.addScriptTag(new Page.AddScriptTagOptions().setPath(ASSETS.resolve("admin-xingwei-site.js")));
        page.waitForFunction("() => !document.querySelector('#siteSave').disabled");
    }
    @Test
    void chromiumCarriesCsrfAndRevisionAndRetainsConflictingForm()throws Exception{
        try(Playwright p=Playwright.create();Browser b=chromium(p)){
            Page page=b.newPage();setupSite(page,"basic");
            page.locator("[data-field=brandName]").fill("新的品牌");page.locator("input[name=reason]").fill("更新品牌文案");page.locator("#siteSave").click();
            page.waitForFunction("() => document.querySelector('#siteMessage').textContent.includes('已保存')");
            assertEquals("test-csrf",page.evaluate("() => window.calls.find(x=>x.method==='PATCH').headers['X-CSRF-TOKEN']"));
            assertEquals("\"revision1\"",page.evaluate("() => window.calls.find(x=>x.method==='PATCH').headers['If-Match']"));
            page.evaluate("() => window.conflict=true");page.locator("[data-field=brandName]").fill("保留草稿");page.locator("input[name=reason]").fill("第二次调整");page.locator("#siteSave").click();
            page.waitForFunction("() => document.querySelector('#siteMessage').textContent.includes('配置已变化')");
            assertEquals("保留草稿",page.locator("[data-field=brandName]").inputValue());assertFalse(page.locator("#siteSave").isDisabled());
        }
    }
    @Test void chromiumNativeSitePreservesAllBasicFieldsAndAllowsFallbackStatusEditing()throws Exception{
        try(Playwright p=Playwright.create();Browser b=chromium(p)){
            Page page=b.newPage();setupSite(page,"basic");assertEquals("OPEN",page.locator("[data-field=serviceStatus]").inputValue());
            page.locator("[data-field=serviceStatus]").selectOption("RESERVATION");page.locator("[data-field=statusShortText]").fill("预约服务");page.locator("[data-field=primaryButtonLabel]").fill("联系预约");page.locator("input[name=reason]").fill("调整全站兜底入口");page.locator("#siteSave").click();page.waitForFunction("()=>document.querySelector('#siteMessage').textContent.includes('已保存')");
            assertEquals(true,page.evaluate("()=>{const data=JSON.parse(window.calls.find(c=>c.method==='PATCH').body);return data.serviceStatus==='RESERVATION'&&data.statusShortText==='预约服务'&&data.primaryButtonLabel==='联系预约'&&data.brandName==='星火';}"));
        }
    }
    @Test
    void webkitMobileUploadsDraftImageWithoutChangingSiteUntilSave()throws Exception{
        try(Playwright p=Playwright.create();Browser b=p.webkit().launch()){
            Page page=b.newPage(new Browser.NewPageOptions().setViewportSize(390,844));setupSite(page,"visual");
            page.locator("input[type=file]").first().setInputFiles(new com.microsoft.playwright.options.FilePayload("test.png","image/png",new byte[]{1,2,3}));
            page.waitForFunction("() => document.querySelector('#siteMessage').textContent.includes('图片已上传')");
            assertEquals("/api/site-assets/test-image",page.locator("[data-field=heroImage]").inputValue());
            assertEquals(0,page.evaluate("() => window.calls.filter(x=>x.method==='PATCH').length"));
            assertEquals(true,page.evaluate("() => document.documentElement.scrollWidth<=innerWidth"));
            page.locator("input[name=reason]").fill("替换主图");page.locator("#siteSave").click();page.waitForFunction("() => document.querySelector('#siteMessage').textContent.includes('已保存')");
        }
    }
    @Test
    void newFrontendControlsReadOnlyModeUsesOnlyNewPermissionControlledApis()throws Exception{
        var repo=mock(SiteSettingRepository.class);when(repo.findAll()).thenReturn(List.of());
        Map<String,String> global=new SiteSettingService(repo).globalUiSettings();Map<String,String> front=new FrontendControlService(repo).settings();
        try(Playwright p=Playwright.create();Browser b=chromium(p)){
            Page page=b.newPage();String html=render("admin-xingwei-frontend-controls.html",true);
            assertTrue(html.contains("data-xingwei-mode=\"true\""));assertTrue(html.contains("href=\"/admin/xingwei/site\""));
            page.route("**/*",r->r.fulfill(new Route.FulfillOptions().setContentType("text/html;charset=utf-8").setBody(html)));
            page.navigate("https://example.test/admin/xingwei/frontend-controls");
            page.evaluate("""
              data=>{window.calls=[];window.fetch=async(url,opts={})=>{window.calls.push(url);return new Response(JSON.stringify(url.endsWith('/access')?{canManageSite:false}:url.endsWith('/global-ui')?data.global:data.front),{headers:{ETag:'"revision"'}});};}
              """,Map.of("global",global,"front",front));
            page.addScriptTag(new Page.AddScriptTagOptions().setPath(ASSETS.resolve("admin-frontend-controls.js")));
            page.waitForFunction("() => document.querySelector('#globalUiState').textContent.includes('只读')");
            assertTrue(page.locator("#globalUiForm button[type=submit]").isDisabled());assertTrue(page.locator("#frontendControlsForm button[type=submit]").isDisabled());
            assertEquals(true,page.evaluate("() => window.calls.every(x=>x.startsWith('/api/admin/xingwei/site/'))"));
        }
    }
}
