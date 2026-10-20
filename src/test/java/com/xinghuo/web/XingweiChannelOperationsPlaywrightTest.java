package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.*;
import com.microsoft.playwright.*;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

@EnabledIfEnvironmentVariable(named="PLAYWRIGHT_UI_TESTS",matches="1")
class XingweiChannelOperationsPlaywrightTest {
    private static final Path ASSETS = Path.of("src/main/resources/static/assets");
    private void setup(Page page) throws Exception {
        String html = Files.readString(Path.of("src/main/resources/templates/admin-xingwei-channels.html"))
                .replaceAll("<script[^>]*></script>","");
        page.route("**/*", route -> route.fulfill(new Route.FulfillOptions().setContentType("text/html; charset=utf-8").setBody(html)));
        page.navigate("https://example.test/admin/xingwei/channels?channel=TAOBAO&status=TODO&q=标题&page=1");
        page.addStyleTag(new Page.AddStyleTagOptions().setPath(ASSETS.resolve("admin-xingwei.css")));
        page.addStyleTag(new Page.AddStyleTagOptions().setPath(ASSETS.resolve("admin-xingwei-channels.css")));
        page.evaluate("""
            () => {
              window.calls=[]; window.conflict=false; window.failRead=false; window.canManage=true;
              document.querySelector('meta[name=_csrf]').content='test-csrf';
              document.querySelector('meta[name=_csrf_header]').content='X-CSRF-TOKEN';
              window.fetch=async (url,opts={}) => {
                window.calls.push({url,method:opts.method,body:opts.body,headers:opts.headers});
                if(opts.method==='POST') return new Response('{}',{status:201});
                if(opts.method==='PUT') return new Response(JSON.stringify(window.conflict?{error:'记录已被其他人更新，请刷新后重新编辑'}:{}),{status:window.conflict?409:200});
                if(window.failRead) return new Response('{}',{status:503});
                return new Response(JSON.stringify({items:[{id:'T1',channelCode:'TAOBAO',title:'标题<script>unsafe</script>',details:'任务说明',status:'TODO',ownerAdminUserId:2,ownerName:'运营人员',departmentCode:'03',dueDate:'2026-10-30',version:0}],channels:[{channelCode:'TAOBAO',serviceState:'UNKNOWN',note:'尚待核对',version:0}],owners:[{id:2,name:'运营人员',departmentCode:'03'}],canManage:window.canManage,canManageChannels:window.canManage,totalCount:61,pageSize:30}));
              };
            }
            """);
        page.addScriptTag(new Page.AddScriptTagOptions().setPath(ASSETS.resolve("admin-xingwei-channels.js")));
        page.waitForFunction("() => document.querySelector('#channelCount').textContent.includes('61')");
    }

    @Test
    void chromiumCreatesWithCsrfAndRetainsInputOnOptimisticConflict() throws Exception {
        try (Playwright p=Playwright.create(); Browser browser=p.chromium().launch()) {
            Page page=browser.newPage(); setup(page);
            assertEquals("TAOBAO",page.locator("#channelFilters select[name=channel]").inputValue());
            assertTrue(page.locator("#channelPage").textContent().contains("2"));
            assertTrue(page.locator("#channelTasks").textContent().contains("<script>unsafe</script>"));
            assertEquals(0,page.locator("#channelTasks script").count());
            page.locator("#channelNew").click();
            page.locator("#channelTaskForm input[name=title]").fill("新增商品巡检");
            page.locator("#channelTaskForm select[name=ownerAdminUserId]").selectOption("2");
            page.locator("#channelTaskForm button[type=submit]").click();
            page.waitForFunction("() => !document.querySelector('#channelTaskDialog').open");
            assertEquals("test-csrf",page.evaluate("() => window.calls.find(x=>x.method==='POST').headers['X-CSRF-TOKEN']"));
            page.getByText("编辑任务",new Page.GetByTextOptions().setExact(true)).click();
            page.locator("#channelTaskForm input[name=title]").fill("保留我的输入");
            page.evaluate("() => window.conflict=true");
            page.locator("#channelTaskForm button[type=submit]").click();
            page.waitForFunction("() => document.querySelector('#channelTaskFeedback').textContent.includes('其他人更新')");
            assertEquals("保留我的输入",page.locator("#channelTaskForm input[name=title]").inputValue());
            assertEquals(true,page.evaluate("() => document.querySelector('#channelTaskDialog').open"));
            assertFalse(page.locator("#channelTaskForm button[type=submit]").isDisabled());
        }
    }

    @Test
    void webkitMobileRestoresFiltersHandlesReadFailureAndShowsReadOnlyState() throws Exception {
        try (Playwright p=Playwright.create(); Browser browser=p.webkit().launch()) {
            Page page=browser.newPage(new Browser.NewPageOptions().setViewportSize(390,844)); setup(page);
            assertEquals(true,page.evaluate("() => document.documentElement.scrollWidth<=innerWidth"));
            page.locator("#channelNext").click();
            page.waitForFunction("() => new URL(location.href).searchParams.get('page')==='2'");
            page.waitForFunction("() => document.querySelector('#channelPage').textContent.includes('3')");
            page.goBack();
            page.waitForFunction("() => document.querySelector('#channelPage').textContent.includes('2')");
            page.evaluate("() => {window.failRead=true;}"); page.locator("#channelRefresh").click();
            page.waitForFunction("() => !document.querySelector('#channelRetry').hidden");
            page.evaluate("() => {window.failRead=false;window.canManage=false;}"); page.locator("#channelRetry").click();
            page.waitForFunction("() => document.querySelector('#channelMessage').textContent.includes('仅可查看')");
            assertTrue(page.locator("#channelNew").isHidden());
            assertEquals(0,page.getByText("编辑任务",new Page.GetByTextOptions().setExact(true)).count());
            assertEquals(true,page.evaluate("() => document.documentElement.scrollWidth<=innerWidth"));
        }
    }
}
