package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.*;
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
class XingweiWeComHrPlaywrightTest {
    Page page(Browser browser) {
        var resolver=new ClassLoaderTemplateResolver();resolver.setPrefix("templates/");resolver.setSuffix(".html");
        var engine=new SpringTemplateEngine();engine.setTemplateResolver(resolver);var context=new Context();
        context.setVariable("csrf",new DefaultCsrfToken("X-CSRF-TOKEN","_csrf","test-token"));
        String html=engine.process("admin-xingwei-wecom",context);
        Page page=browser.newPage(new Browser.NewPageOptions().setViewportSize(390,844));
        page.route("**/*",route->{String path=java.net.URI.create(route.request().url()).getPath();
            if(path.startsWith("/assets/"))route.fulfill(new Route.FulfillOptions().setPath(Path.of("src/main/resources/static"+path)));
            else route.fulfill(new Route.FulfillOptions().setContentType("text/html").setBody(html));});
        page.addInitScript("""
            window.calls=[];window.management=true;window.failCommit=false;
            window.fetch=async(url,options={})=>{
              if(url.endsWith('/capabilities'))return new Response(JSON.stringify({enabled:window.management,canManage:window.management}));
              if(options.method==='POST'){
                const body=JSON.parse(options.body);window.calls.push({url,body,headers:options.headers});
                if(url.endsWith('/preview')){
                  window.previewAction=body.action;
                  return new Response(JSON.stringify({confirmationToken:'hidden-confirmation',name:body.name||'员工甲',departmentName:'业务中心',positionName:'客服',actionLabel:'核对变更',localAccessWillBeRevoked:body.action==='DISABLE_MEMBER'}));
                }
                return window.failCommit?new Response(JSON.stringify({error:'private-vendor-token employee:manage'}),{status:502}):new Response(JSON.stringify({confirmed:true}));
              }
              return new Response(JSON.stringify({configured:true,counts:{employees:1,departments:2,positions:1},
                employees:[{userId:'hidden-member',name:'员工甲',position:'客服',departmentIds:[2],departments:['业务中心'],status:1}],
                departments:[{id:1,parentId:0,name:'企业'},{id:2,parentId:1,name:'业务中心'}],
                positions:[{code:'hidden-position',name:'客服',departmentName:'业务中心',enabled:true}]}));
            };
            """);
        page.navigate("https://xinghuo.test/admin/xingwei/wecom");
        page.waitForFunction("()=>!document.querySelector('#xwcCreateMember').hidden");return page;
    }
    @Test void createUsesPreviewThenNameConfirmationAndResetsNextDialog() {
        try(Playwright playwright=Playwright.create()){
            for(BrowserType type:new BrowserType[]{playwright.chromium(),playwright.webkit()})try(Browser browser=type.launch()){
                Page page=page(browser);page.locator("#xwcCreateMember").click();
                page.locator("#xwcHrName").fill("员工乙");page.locator("#xwcHrDepartment").selectOption("2");page.locator("#xwcHrMobile").fill("13800000000");
                page.locator("#xwcHrSubmit").click();page.waitForFunction("()=>!document.querySelector('#xwcHrPreview').hidden");
                assertEquals(1,page.evaluate("()=>window.calls.length"));
                assertEquals("hidden-position",page.evaluate("()=>window.calls[0].body.positionCode"));
                assertEquals("test-token",page.evaluate("()=>window.calls[0].headers['X-CSRF-TOKEN']"));
                assertFalse(page.locator("#xwcHrDialog").innerText().contains("hidden-"));
                page.locator("#xwcHrConfirm").fill("错误名称");page.locator("#xwcHrSubmit").click();assertEquals(1,page.evaluate("()=>window.calls.length"));
                page.locator("#xwcHrConfirm").fill("员工乙");page.locator("#xwcHrSubmit").click();
                page.waitForFunction("()=>!document.querySelector('#xwcHrDialog').open&&!document.querySelector('#xwcRefresh').disabled");
                assertEquals(2,page.evaluate("()=>window.calls.length"));assertEquals("hidden-confirmation",page.evaluate("()=>window.calls[1].body.confirmationToken"));
                page.locator("#xwcCreateDepartment").click();page.locator("#xwcHrName").fill("新部门");page.locator("#xwcHrSubmit").click();
                page.waitForFunction("()=>window.calls.length===3");assertEquals("CREATE_DEPARTMENT",page.evaluate("()=>window.calls[2].body.action"));
                page.close();
            }
        }
    }
    @Test void transferDisableAndDepartmentRenameUseHiddenTargetsAndUncertainResultCannotBeResubmitted() {
        try(Playwright playwright=Playwright.create()){
            for(BrowserType type:new BrowserType[]{playwright.chromium(),playwright.webkit()})try(Browser browser=type.launch()){
                Page page=page(browser);
                page.locator("#xwcEmployeeRows button").first().click();page.locator("#xwcHrSubmit").click();page.waitForFunction("()=>window.calls.length===1");
                assertEquals("TRANSFER_MEMBER",page.evaluate("()=>window.calls[0].body.action"));assertEquals("hidden-member",page.evaluate("()=>window.calls[0].body.userId"));
                page.locator("#xwcHrClose").click();page.locator("#xwcDepartmentRows button").click();page.locator("#xwcHrName").fill("新中心");page.locator("#xwcHrSubmit").click();page.waitForFunction("()=>window.calls.length===2");
                assertEquals("RENAME_DEPARTMENT",page.evaluate("()=>window.calls[1].body.action"));assertEquals(2,page.evaluate("()=>window.calls[1].body.departmentId"));
                page.locator("#xwcHrClose").click();page.locator("#xwcEmployeeRows button").nth(1).click();assertTrue(page.locator("#xwcHrNotice").innerText().contains("先撤销"));
                page.locator("#xwcHrSubmit").click();page.waitForFunction("()=>window.calls.length===3&&!document.querySelector('#xwcHrConfirmField').hidden");
                page.evaluate("()=>window.failCommit=true");page.locator("#xwcHrConfirm").fill("员工甲");page.locator("#xwcHrSubmit").click();
                page.waitForFunction("()=>document.querySelector('#xwcHrMessage').textContent.includes('需核实')");
                assertTrue(page.locator("#xwcHrSubmit").isDisabled());assertEquals(4,page.evaluate("()=>window.calls.length"));
                assertFalse(page.locator("body").innerText().contains("private-vendor-token"));assertFalse(page.locator("body").innerText().contains("employee:manage"));
                page.locator("#xwcHrClose").click();page.evaluate("()=>window.management=false");page.locator("#xwcRefresh").click();
                page.waitForFunction("()=>document.querySelector('#xwcRefresh').disabled===false&&document.querySelector('#xwcCreateMember').hidden");
                assertEquals(0,page.locator("#xwcEmployeeRows button").count());assertEquals(0,page.locator("#xwcDepartmentRows button").count());
                page.close();
            }
        }
    }
}
