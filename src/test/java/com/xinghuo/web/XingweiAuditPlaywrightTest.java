package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.*;
import com.microsoft.playwright.*;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

@EnabledIfEnvironmentVariable(named="PLAYWRIGHT_UI_TESTS",matches="1")
class XingweiAuditPlaywrightTest {
    private Browser chromium(Playwright p){var options=new BrowserType.LaunchOptions();String executable=System.getenv("XINGHUO_TEST_CHROMIUM");if(executable!=null&&!executable.isBlank())options.setExecutablePath(Path.of(executable));return p.chromium().launch(options);}
    private void setup(Page page)throws Exception{
        String html=Files.readString(Path.of("src/main/resources/templates/admin-xingwei-audits.html")).replaceAll("<script[^>]*></script>","");
        page.route("**/*",route->route.fulfill(new Route.FulfillOptions().setContentType("text/html;charset=utf-8").setBody(html)));
        page.navigate("https://example.test/admin/xingwei/audits?action=UPDATE&page=1");
        for(String css:new String[]{"admin-xingwei.css","admin-xingwei-channels.css"})page.addStyleTag(new Page.AddStyleTagOptions().setPath(Path.of("src/main/resources/static/assets/"+css)));
        page.evaluate("""
          () => {window.failDetail=false;window.failList=false;window.auditUrls=[];
            window.fetch=async url=>{
              window.auditUrls.push(url);
              if(url==='/api/admin/xingwei/audits/1')return new Response(JSON.stringify(window.failDetail?{error:'摘要读取失败'}:{entry:{id:1,action:'UPDATE',operatorName:'运营',requestId:'REQ-1'},changes:[{field:'status',before:'OPEN',after:'RESOLVED'},{field:'password',before:'内容已隐藏',after:'内容已隐藏'}]}),{status:window.failDetail?503:200});
              if(url.includes('/member-activity'))return new Response(JSON.stringify({items:[{id:7,member:'MEM-7',action:'LOGIN_OK',ipMasked:'192.168.*.*',createdAt:'2026-10-16T01:00:00Z'}],actions:['LOGIN_OK'],totalCount:1,pageSize:50}));
              return new Response(JSON.stringify(window.failList?{error:'暂时无法读取'}:{items:[{id:1,action:'UPDATE',operatorName:'运营<script>bad</script>',targetType:'ORDER',targetId:'O1',reason:'跟进处理',createdAt:'2026-10-16T01:00:00Z'}],actions:['UPDATE'],totalCount:101,pageSize:50}),{status:window.failList?503:200});
            };
          }
          """);
        page.addScriptTag(new Page.AddScriptTagOptions().setPath(Path.of("src/main/resources/static/assets/admin-xingwei-audits.js")));
        page.waitForFunction("() => document.querySelector('#auditCount').textContent.includes('101')");
    }
    @Test
    void chromiumFiltersAndOpensRestrictedChangeSummary()throws Exception{
        try(Playwright p=Playwright.create();Browser b=chromium(p)){
            Page page=b.newPage();setup(page);
            assertEquals("UPDATE",page.locator("select[name=action]").inputValue());
            assertEquals(0,page.locator("#auditRows script").count());
            page.getByText("查看变更摘要",new Page.GetByTextOptions().setExact(true)).click();
            page.waitForFunction("() => document.querySelector('#auditDetail').textContent.includes('RESOLVED')");
            assertTrue(page.locator("#auditDetail").textContent().contains("内容已隐藏"));
            page.locator("#auditClose").click();
            page.locator("#auditFilters input[name=q]").fill("O1");page.locator("#auditFilters button[type=submit]").click();
            page.waitForURL("**q=O1**");
            assertTrue(page.url().contains("action=UPDATE"));assertFalse(page.url().contains("page=1"));
        }
    }
    @Test void chromiumMemberActivitySourceUsesNativeReadOnlyRouteAndRestoresUrl()throws Exception{try(Playwright p=Playwright.create();Browser b=chromium(p)){
        Page page=b.newPage();setup(page);page.locator("#auditSource").selectOption("members");page.waitForFunction("()=>document.querySelector('#auditRows').textContent.includes('MEM-7')");
        assertTrue(page.url().contains("source=members"));assertFalse(page.url().contains("page="));
        assertTrue(page.locator("#auditRows").textContent().contains("192.168.*.*"));assertEquals(0,page.locator("#auditRows button").count());
        assertEquals(true,page.evaluate("()=>window.auditUrls.some(url=>url.includes('/api/admin/xingwei/audits/member-activity'))"));
        page.locator("#auditSource").selectOption("staff");page.waitForFunction("()=>document.querySelector('#auditRows').textContent.includes('O1')");assertEquals(1,page.locator("#auditRows button").count());
    }}
    @Test
    void webkitMobileRetriesDetailsAndRecoversListPagination()throws Exception{
        try(Playwright p=Playwright.create();Browser b=p.webkit().launch()){
            Page page=b.newPage(new Browser.NewPageOptions().setViewportSize(390,844));setup(page);
            assertEquals(true,page.evaluate("() => document.documentElement.scrollWidth<=innerWidth"));
            page.evaluate("() => window.failDetail=true");page.getByText("查看变更摘要",new Page.GetByTextOptions().setExact(true)).click();
            page.waitForFunction("() => !document.querySelector('#auditDetailRetry').hidden");
            page.evaluate("() => window.failDetail=false");page.locator("#auditDetailRetry").click();
            page.waitForFunction("() => document.querySelector('#auditDetail').textContent.includes('RESOLVED')");page.locator("#auditClose").click();
            page.locator("#auditNext").click();page.waitForURL("**page=2");
            page.goBack();page.waitForFunction("() => document.querySelector('#auditPage').textContent.includes('2')");
            page.evaluate("() => window.failList=true");page.locator("#auditRefresh").click();page.waitForFunction("() => !document.querySelector('#auditRetry').hidden");
            page.evaluate("() => window.failList=false");page.locator("#auditRetry").click();page.waitForFunction("() => document.querySelector('#auditMessage').textContent.includes('已更新')");
        }
    }
}
