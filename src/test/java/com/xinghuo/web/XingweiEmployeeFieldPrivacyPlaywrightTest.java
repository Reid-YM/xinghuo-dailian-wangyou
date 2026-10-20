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
class XingweiEmployeeFieldPrivacyPlaywrightTest {
    @Test void employeeAndRoleEditorsKeepPrivateFieldsHiddenWithoutLosingWrites() throws Exception {
        try (Playwright p=Playwright.create()) {
            for (BrowserType type : new BrowserType[]{p.chromium(),p.webkit()}) {
                try (Browser browser=type.launch()) { verify(browser); }
            }
        }
    }
    private void verify(Browser browser) throws Exception {
        var resolver=new ClassLoaderTemplateResolver();resolver.setPrefix("templates/");resolver.setSuffix(".html");
        var engine=new SpringTemplateEngine();engine.setTemplateResolver(resolver);
        var context=new Context();context.setVariables(Map.of("username","manager","workspaceName","星维工作台",
                "csrf",new DefaultCsrfToken("X-CSRF-TOKEN","_csrf","test-token"),
                "consoleVersion","20261019-v6b","consoleStage","V6B · UNIFIED DELIVERY","consoleStageLabel","内部版本"));
        String html=engine.process("admin-xingwei",context);
        Page page=browser.newPage(new Browser.NewPageOptions().setViewportSize(390,844));
        page.route("**/*",route->{String path=java.net.URI.create(route.request().url()).getPath();
            if(path.startsWith("/assets/"))route.fulfill(new Route.FulfillOptions().setPath(Path.of("src/main/resources/static"+path)));
            else route.fulfill(new Route.FulfillOptions().setContentType("text/html").setBody(html));});
        page.addInitScript("""
          window.writes=[];
          window.org={dataScope:'ALL',counts:{departments:1,positions:1,employees:1,roles:1},
            departments:[{code:'01',name:'业务中心',positionCount:1,employeeCount:1}],
            positions:[{code:'P001',name:'业务客服',departmentCode:'01',departmentName:'业务中心'}],
            roles:[{code:'XW_STAFF',name:'业务员工',dataScope:'SELF',enabled:true,permissionCount:1,permissionCodes:['employee:manage']}],
            permissions:[{code:'employee:manage',name:'员工维护',module:'员工'}],
            employees:[{id:3,username:'worker',displayName:'员工甲',employeeNo:'E001',departmentCode:'01',departmentName:'业务中心',positionCode:'P001',positionName:'业务客服',status:'ACTIVE',legacyRole:'OPERATOR',roleCodes:['XW_STAFF'],notes:'私密员工备注',wecomUserId:'private-wecom-id'}]};
          window.fetch=async(url,options={})=>{
            if(options.method==='PUT'){
              const body=JSON.parse(options.body);window.writes.push({url,body,headers:options.headers});
              if(url.endsWith('/profile'))window.org.employees[0].displayName=body.displayName;
              return new Response('{}');
            }
            if(url.endsWith('/health'))return new Response(JSON.stringify({version:'20261019-v6b',stage:'V6B · UNIFIED DELIVERY'}));
            if(url.endsWith('/access'))return new Response(JSON.stringify({mode:'SHADOW_RBAC',allowAll:false,dataScope:'ALL',roleCodes:['XW_STAFF'],permissionCodes:['employee:view','employee:manage','rbac:view','rbac:manage','org:view']}));
            return new Response(JSON.stringify(window.org));
          };
          """);
        page.navigate("https://xinghuo.test/admin/xingwei#org-employees");
        page.locator("[data-employee-edit='3']").click();
        assertThat(page.locator("#xwEmployeeDialog")).isVisible();
        assertEquals(0,page.locator("#xwEmployeeNotes").count());
        assertFalse(page.locator("body").innerText().contains("私密员工备注"));
        assertFalse(page.locator("body").innerText().contains("private-wecom-id"));
        assertFalse(page.locator("#xwAccessMode").getAttribute("title").contains("XW_STAFF"));
        assertFalse(page.locator("#xwAccessMode").getAttribute("title").contains("employee:manage"));
        page.locator("#xwEmployeeName").fill("员工乙");
        page.locator("#xwEmployeeForm button[type=submit]").click();
        page.waitForFunction("()=>window.writes.some(w=>w.url.endsWith('/profile'))&&!document.querySelector('#xwEmployeeDialog').open");
        assertEquals(false,page.evaluate("()=>Object.hasOwn(window.writes.find(w=>w.url.endsWith('/profile')).body,'notes')"));
        assertEquals("员工乙",page.evaluate("()=>window.org.employees[0].displayName"));
        page.navigate("https://xinghuo.test/admin/xingwei#org-roles");
        page.locator("[data-role-edit='XW_STAFF']").click();
        assertThat(page.locator("#xwRoleDialog")).isVisible();
        assertTrue(page.locator("#xwPermissionChecks").innerText().contains("员工维护"));
        assertFalse(page.locator("#xwRoleDialog").innerText().contains("employee:manage"));
        assertFalse(page.locator("body").innerText().contains("XW_STAFF"));
        page.locator("#xwRoleForm button[type=submit]").click();
        page.waitForFunction("()=>window.writes.some(w=>w.url.endsWith('/permissions'))");
        assertEquals("employee:manage",page.evaluate("()=>window.writes.find(w=>w.url.endsWith('/permissions')).body.permissionCodes[0]"));
        assertEquals(true,page.evaluate("()=>document.documentElement.scrollWidth<=innerWidth+1"));
        page.close();
    }
}
