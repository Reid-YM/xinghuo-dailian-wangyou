package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.*;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import com.microsoft.playwright.*;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

@EnabledIfEnvironmentVariable(named="PLAYWRIGHT_UI_TESTS",matches="1")
class XingweiWorkspacePanelsPlaywrightTest {
    private Browser chromium(Playwright p){var options=new BrowserType.LaunchOptions();String executable=System.getenv("XINGHUO_TEST_CHROMIUM");if(executable!=null&&!executable.isBlank())options.setExecutablePath(Path.of(executable));return p.chromium().launch(options);}
    private Page setup(Browser browser,boolean scoped,String hash){return setup(browser,scoped?"orders":"all",hash);}
    private Page setup(Browser browser,String scope,String hash){
        var resolver=new ClassLoaderTemplateResolver();resolver.setPrefix("templates/");resolver.setSuffix(".html");var engine=new SpringTemplateEngine();engine.setTemplateResolver(resolver);
        var context=new Context();context.setVariables(Map.of("workspaceName","星维网络服务工作室","username","测试员工","restoreAdmin",false,"csrf",new DefaultCsrfToken("X-CSRF-TOKEN","_csrf","test"),"consoleVersion","20261019-v6b","consoleStage","V6B · UNIFIED DELIVERY","consoleStageLabel","V6B · 统一交付"));String html=engine.process("admin-xingwei",context);
        Page page=browser.newPage(new Browser.NewPageOptions().setViewportSize(1440,900));
        page.route("**/*",r->{String path=java.net.URI.create(r.request().url()).getPath();if(path.startsWith("/assets/"))r.fulfill(new Route.FulfillOptions().setPath(Path.of("src/main/resources/static"+path)));else r.fulfill(new Route.FulfillOptions().setContentType("text/html").setBody(html));});
        page.addInitScript("""
            window.scope='SCOPE_VALUE';window.scoped=window.scope!=='all';
            window.fetch=async url=>{
              const result=body=>new Response(JSON.stringify(body),{headers:{'Content-Type':'application/json'}});
              if(url.endsWith('/access'))return result({mode:window.scoped?'SHADOW_RBAC':'LEGACY_ADMIN_OVERRIDE',allowAll:!window.scoped,dataScope:window.scoped?'SELF':'ALL',permissionCodes:window.scope==='content'?['content:view']:window.scope==='employee'?['employee:view']:window.scoped?['order:view']:[],roleCodes:[]});
              if(url.endsWith('/health'))return result({version:'20261019-v6b',stage:'V6B · UNIFIED DELIVERY'});
              if(window.scoped&&window.scope!=='employee')return new Response(JSON.stringify({error:'无组织查看权限'}),{status:403});
              return result({counts:{},departments:[],positions:[],employees:[],roles:[],permissions:[],dataScope:'ALL'});
            };
            """.replace("SCOPE_VALUE",scope));
        page.navigate("http://xinghuo.test/admin/xingwei"+hash);page.waitForFunction("()=>window.xwAccess&&document.documentElement.dataset.xwRuntimeState==='match'");return page;
    }
    private void selected(Page page,String key,String title){
        page.waitForFunction("key=>[...document.querySelectorAll('[data-workspace]')].filter(p=>!p.hidden).every(p=>p.dataset.workspace===key)",key);
        assertTrue(page.locator("[data-workspace='"+key+"']").first().isVisible());
        assertEquals(title,page.locator("#xwWorkspaceTitle").textContent());assertEquals("page",page.locator(".xw-nav-item[href='#"+key+"']").getAttribute("aria-current"));
        assertEquals(true,page.evaluate("()=>document.documentElement.scrollWidth<=innerWidth+1"));
    }
    @Test void chromiumWorkspacesSearchHistoryAndPhoneNavigation(){try(Playwright p=Playwright.create();Browser b=chromium(p)){navigation(b);}}
    @Test void webkitWorkspacesSearchHistoryAndPhoneNavigation(){try(Playwright p=Playwright.create();Browser b=p.webkit().launch()){navigation(b);}}
    private void navigation(Browser browser){
        Page page=setup(browser,false,"");selected(page,"overview","运营总览");assertTrue(page.locator("#business").isHidden());assertTrue(page.locator("#org-console").isHidden());assertEquals(0,page.locator("#xwReleaseMenu").count());assertTrue(page.locator(".xw-runtime-metadata").isHidden());
        page.locator(".xw-workspace-links a[href='#business']").click();selected(page,"business","业务中台");assertEquals(14,page.locator("#business .xw-module").count());assertEquals(5,page.locator("#business .xw-module:visible").count());
        for(String[] workspace:new String[][]{{"organization","组织与权限"},{"channels","渠道与微信"},{"system","系统工具"}}){page.locator(".xw-nav-item[href='#"+workspace[0]+"']").click();selected(page,workspace[0],workspace[1]);}
        page.screenshot(new Page.ScreenshotOptions().setPath(Path.of("/tmp/admin-workspace-system-desktop.png")));
        page.locator("#xwModuleSearch").fill("微信");assertEquals("模块搜索",page.locator("#xwWorkspaceTitle").textContent());assertTrue(page.locator("#organization").isVisible());assertTrue(page.locator("#channels").isVisible());assertTrue(page.locator("#org-console").isHidden());assertEquals(0,page.locator(".xw-nav-item[aria-current]").count());
        page.locator("#xwSearchClear").click();selected(page,"system","系统工具");
        page.locator("#xwModuleSearch").fill("七大中心");page.locator("a.xw-module[href='#org-console']").click();selected(page,"organization","组织与权限");assertTrue(page.locator("#org-console").isVisible());assertEquals("",page.locator("#xwModuleSearch").inputValue());
        page.locator("#xwWorkspaceGroups a[href='#organization/roles']").click();page.locator("a.xw-module[href='#org-roles']").click();assertTrue(page.url().endsWith("#org-roles"));assertTrue(page.locator("#org-roles").isVisible());
        page.goBack();selected(page,"organization","组织与权限");page.goBack();selected(page,"organization","组织与权限");page.goBack();selected(page,"system","系统工具");page.goForward();selected(page,"organization","组织与权限");page.reload();selected(page,"organization","组织与权限");
        page.setViewportSize(390,844);page.locator("#xwMenuButton").click();page.locator(".xw-nav-item[href='#channels']").click();selected(page,"channels","渠道与微信");assertEquals("false",page.locator("#xwMenuButton").getAttribute("aria-expanded"));
        page.waitForFunction("()=>document.querySelector('#xwSidebar').getBoundingClientRect().right<=1&&scrollY<=1");page.screenshot(new Page.ScreenshotOptions().setPath(Path.of("/tmp/admin-workspace-channels-mobile.png")));
        page.locator("#xwMenuButton").click();page.locator(".xw-nav-item[href='#overview']").click();selected(page,"overview","运营总览");page.setViewportSize(1920,1080);assertEquals(true,page.evaluate("()=>document.documentElement.scrollWidth<=innerWidth+1"));page.screenshot(new Page.ScreenshotOptions().setPath(Path.of("/tmp/admin-workspace-overview-wide.png")));page.close();
    }
    @Test void chromiumScopedSearchAndUnavailableWorkspace(){try(Playwright p=Playwright.create();Browser b=chromium(p)){permissions(b);}}
    @Test void webkitScopedSearchAndUnavailableWorkspace(){try(Playwright p=Playwright.create();Browser b=p.webkit().launch()){permissions(b);}}
    private void permissions(Browser browser){
        Page page=setup(browser,true,"#channels");selected(page,"channels","渠道与微信");assertEquals(0,page.locator("#channels .xw-module:visible").count());assertTrue(page.locator("#xwWorkspaceEmpty").textContent().contains("没有可用模块"));
        page.locator("#xwModuleSearch").fill("支付");assertEquals(0,page.locator(".xw-module:visible").count());assertTrue(page.locator("#xwWorkspaceEmpty").isVisible());assertTrue(page.locator("#xwWorkspaceEmpty").textContent().contains("没有匹配"));
        page.locator("#xwModuleSearch").fill("订单");assertTrue(page.locator("#business").isVisible());assertTrue(page.locator("#channels").isHidden());assertEquals(0,page.locator(".xw-module[data-global-scope]:visible").count());assertTrue(page.locator("a.xw-module[href='/admin/xingwei/business?tab=orders']").isVisible());
        page.locator("#xwSearchClear").click();selected(page,"channels","渠道与微信");page.navigate("http://xinghuo.test/admin/xingwei#org-roles");selected(page,"organization","组织与权限");assertEquals(0,page.locator("[data-role-edit]:visible,[data-employee-edit]:visible").count());
        page.navigate("http://xinghuo.test/admin/xingwei#unknown-section");selected(page,"overview","运营总览");assertTrue(page.locator("#org-console").isHidden());page.close();
    }

    @Test void chromiumSubgroupsKeepTheirOwnContentAndHistory(){try(Playwright p=Playwright.create();Browser b=chromium(p)){subgroups(b);}}
    @Test void webkitSubgroupsKeepTheirOwnContentAndHistory(){try(Playwright p=Playwright.create();Browser b=p.webkit().launch()){subgroups(b);}}
    private void group(Page page,String workspace,String key){
        page.locator("#xwWorkspaceGroups a[href='#"+workspace+"/"+key+"']").click();
        assertEquals("#"+workspace+"/"+key,page.evaluate("()=>location.hash"));
        assertEquals("page",page.locator("#xwWorkspaceGroups a[href='#"+workspace+"/"+key+"']").getAttribute("aria-current"));
        assertEquals(true,page.evaluate("()=>document.documentElement.scrollWidth<=innerWidth+1"));
    }
    private void subgroups(Browser browser){
        Page page=setup(browser,false,"#business");assertTrue(page.url().endsWith("#business/orders"));assertEquals(5,page.locator("#business .xw-module:visible").count());
        group(page,"business","catalog");assertEquals("服务与目录",page.locator("#business [data-workspace-heading]").textContent());assertEquals(2,page.locator("#business .xw-module:visible").count());assertTrue(page.locator("a.xw-module[href='/admin/xingwei/catalog-details']").isVisible());
        page.screenshot(new Page.ScreenshotOptions().setPath(Path.of("/tmp/admin-subgroups-catalog-desktop.png")));
        group(page,"business","content");assertEquals(3,page.locator("#business .xw-module:visible").count());group(page,"business","operations");assertEquals(4,page.locator("#business .xw-module:visible").count());
        page.goBack();assertTrue(page.url().endsWith("#business/content"));page.goForward();assertTrue(page.url().endsWith("#business/operations"));
        page.locator("#xwModuleSearch").fill("目录");assertTrue(page.locator("#xwWorkspaceGroups").isHidden());assertEquals("业务中台",page.locator("#business [data-workspace-heading]").textContent());assertTrue(page.locator("a.xw-module[href='/admin/xingwei/catalog-details']").isVisible());page.locator("#xwSearchClear").click();assertTrue(page.url().endsWith("#business/operations"));assertEquals(4,page.locator("#business .xw-module:visible").count());
        page.navigate("http://xinghuo.test/admin/xingwei#business/catalog");page.reload();assertTrue(page.locator("a.xw-module[href='/admin/xingwei/catalog-details']").isVisible());
        page.navigate("http://xinghuo.test/admin/xingwei#organization/employees");assertTrue(page.locator("#org-employees").isVisible());assertTrue(page.locator("#org-roles").isHidden());assertTrue(page.locator("#org-positions").isHidden());
        group(page,"organization","structure");assertTrue(page.locator("#org-departments").isVisible());assertTrue(page.locator("#org-positions").isVisible());assertTrue(page.locator("#org-employees").isHidden());
        group(page,"organization","roles");assertTrue(page.locator("#org-roles").isVisible());assertTrue(page.locator("#org-employees").isHidden());assertTrue(page.locator("#org-positions").isHidden());
        page.navigate("http://xinghuo.test/admin/xingwei#org-employees");assertThat(page.locator("#org-employees")).isVisible();assertEquals("page",page.locator("#xwWorkspaceGroups a[href='#organization/employees']").getAttribute("aria-current"));
        page.setViewportSize(390,844);page.locator("#xwMenuButton").click();page.locator(".xw-nav-item[href='#business']").click();page.waitForFunction("()=>document.querySelector('#xwSidebar').getBoundingClientRect().right<=1");
        Locator content=page.locator("#xwWorkspaceGroups a[href='#business/content']");content.focus();content.press("Enter");assertTrue(page.url().endsWith("#business/content"));assertEquals("#business/content",page.evaluate("()=>document.activeElement.getAttribute('href')"));page.waitForFunction("()=>scrollY<=1");page.screenshot(new Page.ScreenshotOptions().setPath(Path.of("/tmp/admin-subgroups-content-mobile.png")));page.close();
        Page author=setup(browser,"content","#business");assertTrue(author.url().endsWith("#business/content"));assertEquals(1,author.locator("#business .xw-module:visible").count());assertEquals(1,author.locator("#xwWorkspaceGroups a").count());
        author.navigate("http://xinghuo.test/admin/xingwei#business/catalog");author.waitForFunction("()=>window.xwAccess");assertTrue(author.locator("#xwWorkspaceEmpty").isVisible());assertEquals(0,author.locator(".xw-module:visible").count());
        author.navigate("http://xinghuo.test/admin/xingwei#business/missing");author.waitForFunction("()=>location.hash==='#business/content'");assertEquals(1,author.locator("#business .xw-module:visible").count());author.close();
        Page employee=setup(browser,"employee","#organization");assertTrue(employee.url().endsWith("#organization/structure"));assertEquals(0,employee.locator("a.xw-module[href='#org-console']:visible").count());assertTrue(employee.locator("#org-positions").isVisible());assertEquals(0,employee.locator("#xwWorkspaceGroups a[href='#organization/roles']").count());group(employee,"organization","employees");assertTrue(employee.locator("#org-employees").isVisible());assertEquals(0,employee.locator("[data-employee-edit]:visible").count());employee.close();
    }
    @Test void chromiumDetailSearchReachesRealFieldAndRespectsPermissions(){try(Playwright p=Playwright.create();Browser b=chromium(p)){detailSearch(b);}}
    @Test void webkitDetailSearchReachesRealFieldAndRespectsPermissions(){try(Playwright p=Playwright.create();Browser b=p.webkit().launch()){detailSearch(b);}}
    private void detailSearch(Browser browser){
        Page page=setup(browser,false,"#business/catalog");
        page.locator("#xwModuleSearch").fill("首页 主图");
        Locator result=page.locator("#xwSearchResults a[href='/admin/xingwei/site?section=visual#site-field-heroImage']");
        assertTrue(result.isVisible());assertTrue(page.locator("a.xw-module[href='/admin/xingwei/site']").isVisible());
        page.setViewportSize(390,844);page.waitForFunction("()=>document.querySelector('#xwSidebar').getBoundingClientRect().right<=1");assertEquals(true,page.evaluate("()=>document.documentElement.scrollWidth<=innerWidth+1"));
        page.screenshot(new Page.ScreenshotOptions().setPath(Path.of("/tmp/admin-detail-search-mobile.png")));
        var resolver=new ClassLoaderTemplateResolver();resolver.setPrefix("templates/");resolver.setSuffix(".html");var engine=new SpringTemplateEngine();engine.setTemplateResolver(resolver);
        var context=new Context();var csrf=new DefaultCsrfToken("X-CSRF-TOKEN","_csrf","test");context.setVariable("csrf",csrf);context.setVariable("_csrf",csrf);
        String html=engine.process("admin-xingwei-site",context);
        page.route("**/admin/xingwei/site?**",r->r.fulfill(new Route.FulfillOptions().setContentType("text/html;charset=utf-8").setBody(html)));
        page.addInitScript("""
            const originalFetch=window.fetch;
            window.fetch=async (url,options)=>{
              if(url.startsWith('/api/admin/xingwei/site/')){
                await new Promise(resolve=>setTimeout(resolve,80));
                return new Response(JSON.stringify(url.endsWith('/access')?{canViewSite:true,canManageSite:true,canManageSeo:true}:{}),{headers:{ETag:'"test"'}});
              }
              return originalFetch(url,options);
            };
            """);
        result.focus();result.press("Enter");
        page.waitForFunction("()=>document.activeElement?.id==='site-field-heroImage'");
        assertEquals("visual",page.locator("#siteSection").inputValue());
        assertEquals("site-field-heroImage",page.evaluate("()=>document.querySelector('.xw-located-field').id"));
        assertEquals(true,page.evaluate("()=>{const r=document.querySelector('.xw-located-field').getBoundingClientRect();return r.top>=0&&r.bottom<=innerHeight;}"));
        assertEquals(0,page.locator("dialog[open]").count());
        page.screenshot(new Page.ScreenshotOptions().setPath(Path.of("/tmp/admin-detail-target-mobile.png")));page.close();
        Page scoped=setup(browser,true,"#business");scoped.locator("#xwModuleSearch").fill("订单 进度");
        assertTrue(scoped.locator("#xwSearchResults").textContent().contains("客户可见进度"));assertTrue(scoped.locator("#xwSearchResults").textContent().contains("选择记录"));
        assertEquals(0,scoped.locator("#xwSearchResults a[href*='/site']").count());
        scoped.locator("#xwModuleSearch").fill("首页主图");assertEquals(0,scoped.locator("#xwSearchResults a").count());assertTrue(scoped.locator("#xwWorkspaceEmpty").isVisible());
        scoped.locator("#xwModuleSearch").fill("不可能存在的设置");assertEquals(0,scoped.locator("#xwSearchResults a").count());
        scoped.locator("#xwSearchClear").click();assertTrue(scoped.locator("#xwSearchResults").isHidden());assertEquals("#business/orders",scoped.evaluate("()=>location.hash"));scoped.close();
        Page employee=setup(browser,"employee","#organization");employee.locator("#xwModuleSearch").fill("员工编号");
        Locator record=employee.locator("#xwSearchResults a[href='#org-employees']").first();assertTrue(record.isVisible());record.click();assertTrue(employee.locator("#org-employees").isVisible());assertEquals("",employee.locator("#xwModuleSearch").inputValue());employee.close();
    }

}
