package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.*;
import com.microsoft.playwright.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

@EnabledIfEnvironmentVariable(named="PLAYWRIGHT_UI_TESTS",matches="1")
class XingweiToolsAndStatisticsPlaywrightTest {
    private String renderedTools(){var resolver=new ClassLoaderTemplateResolver();resolver.setPrefix("templates/");resolver.setSuffix(".html");var engine=new SpringTemplateEngine();engine.setTemplateResolver(resolver);var context=new Context();context.setVariables(Map.of("xingweiToolMode",true,"csrf",new DefaultCsrfToken("X-CSRF-TOKEN","_csrf","test-csrf")));return engine.process("admin-xingwei-tool-station",context);}
    private Browser chromium(Playwright playwright){var options=new BrowserType.LaunchOptions();String executable=System.getenv("XINGHUO_TEST_CHROMIUM");if(executable!=null&&!executable.isBlank())options.setExecutablePath(Path.of(executable));return playwright.chromium().launch(options);}
    private Page setup(Browser browser,boolean manage)throws Exception{
        String tools=renderedTools(),statistics=Files.readString(Path.of("src/main/resources/templates/admin-xingwei-statistics.html"));
        Page page=browser.newPage(new Browser.NewPageOptions().setViewportSize(390,844));page.route("**/*",r->{String path=java.net.URI.create(r.request().url()).getPath();if(path.startsWith("/assets/"))r.fulfill(new Route.FulfillOptions().setPath(Path.of("src/main/resources/static"+path)));else r.fulfill(new Route.FulfillOptions().setContentType("text/html").setBody(path.endsWith("/statistics")?statistics:tools));});
        page.addInitScript("""
            window.calls=[];window.canManage=CAN_MANAGE;window.conflict=false;window.version=1;
            window.toolData={games:[{code:'CODMW4',weapons:[{code:'W1',name:'实际武器',weaponType:'AR',maxLevel:30,levelingEnabled:true,gunsmith:true,warzoneCamos:[],onlineCamos:{},apexAttachments:[]}],camoTracks:{modes:{}}}]};
            window.fetch=async(url,options={})=>{const method=options.method||'GET';window.calls.push({url,method,headers:options.headers,body:options.body});
              if(url.endsWith('/access'))return new Response(JSON.stringify({canManage:window.canManage}));
              if(url.includes('/statistics')){const days=Number(new URL(url,location.origin).searchParams.get('days'));return new Response(JSON.stringify({days,total:12,recent:5,lastEventAt:'2026-10-16 10:00',funnel:[{label:'进入询价',count:3}],trend:[{date:'10-16',count:5}],events:[{event:'<script>不能执行</script>',total:12,recent:5,lastAt:'2026-10-16'}]}));}
              if(method==='PUT'){if(window.conflict)return new Response(JSON.stringify({message:'资料已变化，当前输入尚未保存'}),{status:409});window.toolData=JSON.parse(options.body);window.version++;}
              return new Response(JSON.stringify(window.toolData),{headers:{ETag:'"r'+window.version+'"'}});};
            """.replace("CAN_MANAGE",String.valueOf(manage)));
        page.onDialog(dialog->{if(dialog.type().equals("prompt"))dialog.accept("更新公开工具资料");else dialog.accept();});return page;
    }
    @Test void chromiumConsecutiveSavesUseCurrentWeaponAndRevisionAndKeepConflictingDraft()throws Exception{
        try(Playwright p=Playwright.create();Browser b=chromium(p)){
            Page page=setup(b,true);page.navigate("http://xinghuo.test/admin/xingwei/tool-station");page.waitForFunction("()=>document.querySelector('#toolStationForm input[name=name]').value==='实际武器'");
            page.locator("#toolStationForm input[name=name]").fill("第一次更新");page.locator("#toolStationSaveAll").click();page.waitForFunction("()=>window.version===2");
            page.locator("#toolStationForm input[name=name]").fill("第二次更新");page.locator("#toolStationSaveAll").click();page.waitForFunction("()=>window.version===3");
            assertEquals("第二次更新",page.evaluate("()=>window.toolData.games[0].weapons[0].name"));assertEquals("\"r2\"",page.evaluate("()=>window.calls.filter(c=>c.method==='PUT')[1].headers['If-Match']"));assertEquals("test-csrf",page.evaluate("()=>window.calls.find(c=>c.method==='PUT').headers['X-CSRF-TOKEN']"));
            page.evaluate("()=>window.conflict=true");page.locator("#toolStationForm input[name=name]").fill("保留当前草稿");page.locator("#toolStationSaveAll").click();page.waitForFunction("()=>document.querySelector('#toastArea').textContent.includes('资料已变化')");assertEquals("保留当前草稿",page.locator("#toolStationForm input[name=name]").inputValue());assertEquals("第二次更新",page.evaluate("()=>window.toolData.games[0].weapons[0].name"));
            assertEquals(true,page.evaluate("()=>window.calls.every(c=>c.url.startsWith('/api/admin/xingwei/'))"));assertEquals(0,page.locator("script[data-admin-global-ui],script[data-admin-resilience]").count());assertEquals(true,page.evaluate("()=>document.documentElement.scrollWidth<=innerWidth"));
        }
    }
    @Test void webkitReadOnlyToolingAndNativeTrafficStatisticsRemainUsableOnPhone()throws Exception{
        try(Playwright p=Playwright.create();Browser b=p.webkit().launch()){
            Page page=setup(b,false);page.navigate("http://xinghuo.test/admin/xingwei/tool-station");page.waitForFunction("()=>document.querySelector('#toolStationForm input[name=name]').value==='实际武器'");assertTrue(page.locator("#toolStationSaveAll").isDisabled());assertTrue(page.locator("#toolStationReset").isDisabled());assertTrue(page.locator("#toolStationForm input[name=name]").isDisabled());assertFalse(page.locator("[data-tool-weapon]").first().isDisabled());assertEquals(true,page.evaluate("()=>document.documentElement.scrollWidth<=innerWidth"));
            page.navigate("http://xinghuo.test/admin/xingwei/statistics");page.waitForFunction("()=>document.querySelector('#statisticsMessage').textContent.includes('7 天')");assertEquals(0,page.locator("#statisticsEvents script").count());assertTrue(page.locator("#statisticsEvents").textContent().contains("<script>不能执行</script>"));page.locator("#statisticsFilters select").selectOption("30");page.locator("#statisticsFilters button").click();page.waitForFunction("()=>document.querySelector('#statisticsMessage').textContent.includes('30 天')");assertEquals(true,page.evaluate("()=>document.documentElement.scrollWidth<=innerWidth"));
        }
    }
}
