package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.*;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.AriaRole;
import java.net.URI;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

@EnabledIfEnvironmentVariable(named="PLAYWRIGHT_UI_TESTS",matches="1")
class FrontendAccountContextPlaywrightTest {
    static final String FIRST=FrontendCanonicalRoutesPlaywrightTest.NO,SECOND="XHABCDEF1235",QUOTE="XHABCDEF1236";
    @Test void detailPreviousNextSourceQuoteAndFilteredReturnUseTheCurrentOwnedList() throws Exception {
        both(browser->{for(String base:List.of("/next",""))try(var f=new FrontendCanonicalRoutesPlaywrightTest.Fixture(browser,390,true)){
            java.util.function.Consumer<Route> orders=route->{String path=URI.create(route.request().url()).getPath();
                if(path.equals("/api/console/orders"))f.json(route,200,"{\"items\":["+order(FIRST,"第一个服务","IN_PROGRESS","2026-10-18T08:00:00Z")+","+order(SECOND,"第二个服务","COMPLETED","2026-10-19T08:00:00Z")+"],\"total\":2}");
                else f.json(route,200,"{\"order\":"+order(path.endsWith(FIRST)?FIRST:SECOND,path.endsWith(FIRST)?"第一个服务":"第二个服务",path.endsWith(FIRST)?"IN_PROGRESS":"COMPLETED","2026-10-19T08:00:00Z")+",\"sourceQuote\":{\"businessNo\":\""+QUOTE+"\"},\"timeline\":[],\"afterSalesCases\":[]}");
            };
            f.page.route("**/api/console/orders",orders);f.page.route("**/api/console/orders/**",orders);
            java.util.function.Consumer<Route> quotes=route->{String path=URI.create(route.request().url()).getPath();String quote="{\"businessNo\":\""+QUOTE+"\",\"game\":\"测试游戏\",\"demand\":\"来源询价需求\"}";f.json(route,200,path.equals("/api/console/quotes")?"{\"items\":["+quote+"],\"total\":1}":quote);};
            f.page.route("**/api/console/quotes",quotes);f.page.route("**/api/console/quotes/**",quotes);
            f.open(base+"/account/orders","#n5RecordsBody tbody tr");
            int history=((Number)f.page.evaluate("() => window.history.length")).intValue();
            f.page.locator("#n5Tab-orders").click();assertEquals(history,((Number)f.page.evaluate("() => window.history.length")).intValue());
            f.page.locator("#n5RecordsBody tbody tr").first().getByRole(AriaRole.BUTTON,new Locator.GetByRoleOptions().setName("查看详情")).click();
            f.page.waitForSelector("#n19DetailContext");
            var context=f.page.locator("#n19DetailContext");
            assertTrue(context.getByRole(AriaRole.BUTTON,new Locator.GetByRoleOptions().setName("上一条")).isDisabled());
            context.getByRole(AriaRole.BUTTON,new Locator.GetByRoleOptions().setName("下一条")).click();
            f.page.waitForFunction("() => location.pathname.endsWith('/orders/"+SECOND+"') && document.querySelector('#n5Detail')?.textContent.includes('第二个服务')");
            assertTrue(context.getByRole(AriaRole.BUTTON,new Locator.GetByRoleOptions().setName("下一条")).isDisabled());
            context.getByRole(AriaRole.BUTTON,new Locator.GetByRoleOptions().setName("来源询价")).click();
            f.page.waitForFunction("() => location.pathname.endsWith('/requests/"+QUOTE+"') && document.querySelector('#n5Detail')?.textContent.includes('来源询价需求')");
            context.getByRole(AriaRole.BUTTON,new Locator.GetByRoleOptions().setName("返回询价列表")).click();
            f.page.waitForFunction("() => location.pathname.endsWith('/account/requests') && document.querySelector('#n5Detail').hidden");
            f.open(base+"/account/orders","#n20Sort");f.page.locator("#n20Sort").selectOption("updated-desc");
            f.page.waitForFunction("() => new URL(location.href).searchParams.get('sort') === 'updated-desc'");
            assertTrue(f.page.locator("#n5RecordsBody tbody tr:visible").first().textContent().contains(SECOND));
            f.page.locator("#n20Status").selectOption("COMPLETED");
            f.page.waitForFunction("() => new URL(location.href).searchParams.get('status') === 'COMPLETED'");
            assertEquals(1,f.page.locator("#n5RecordsBody tbody tr:visible").count());
            f.page.locator("#n5RecordsBody tbody tr:visible").getByRole(AriaRole.BUTTON,new Locator.GetByRoleOptions().setName("查看详情")).click();f.page.waitForSelector("#n19DetailContext");
            assertTrue(context.getByRole(AriaRole.BUTTON,new Locator.GetByRoleOptions().setName("上一条")).isDisabled());
            assertTrue(context.getByRole(AriaRole.BUTTON,new Locator.GetByRoleOptions().setName("下一条")).isDisabled());
            assertTrue(context.getByRole(AriaRole.LINK,new Locator.GetByRoleOptions().setName("查看公开业务记录")).getAttribute("href").contains("status=COMPLETED"));
            context.getByRole(AriaRole.BUTTON,new Locator.GetByRoleOptions().setName("返回订单列表")).click();
            f.page.waitForFunction("() => location.pathname.endsWith('/account/orders') && new URL(location.href).searchParams.get('status') === 'COMPLETED'");
            assertEquals(List.of(),f.errors);
        }});
    }
    @Test void personalAssetPaginationSurvivesReloadBackAndKeepsEachAssetRouteSeparate() throws Exception {
        both(browser->{for(String base:List.of("/next",""))try(var f=new FrontendCanonicalRoutesPlaywrightTest.Fixture(browser,390,true)){
            List<String> reads=new ArrayList<>();
            f.page.route("**/api/account/progress",route->f.json(route,200,"[{\"toolKey\":\"CODMW4\"}]"));
            f.page.route("**/api/account/favorites*",route->{String query=URI.create(route.request().url()).getQuery();reads.add(query);int page=query!=null&&query.contains("page=1")?1:0;List<String> items=new ArrayList<>();for(int n=page*20;n<Math.min(21,(page+1)*20);n++)items.add("{\"targetType\":\"GUIDE\",\"targetId\":\"guide-"+n+"\"}");f.json(route,200,"["+String.join(",",items)+"]");});
            f.page.route("**/api/account/submissions*",route->f.json(route,200,"[{\"id\":1,\"title\":\"已发布投稿\",\"status\":\"PUBLISHED\",\"publishedSlug\":\"test-guide\"}]"));
            f.open(base+"/account/favorites?page=2","#n21AssetNav");var asset=f.page.locator("[data-personal-asset=favorites]");
            f.page.waitForFunction("() => document.querySelector('[data-personal-asset=favorites]')?.textContent.includes('第 2 页')");
            assertEquals(base+"/account/submit",f.page.getByRole(AriaRole.LINK,new Page.GetByRoleOptions().setName("撰写与管理投稿")).getAttribute("href"));
            assertEquals(base+"/mastery",f.page.getByRole(AriaRole.LINK,new Page.GetByRoleOptions().setName("打开工具站")).getAttribute("href"));
            assertEquals(1,asset.locator(".n5-source").count());assertTrue(asset.textContent().contains("guide-20"));
            assertEquals(base+"/guides/guide-20",asset.locator(".n5-source a").getAttribute("href"));
            f.page.reload();f.page.waitForFunction("() => document.querySelector('[data-personal-asset=favorites]')?.textContent.includes('第 2 页')");
            asset.getByRole(AriaRole.BUTTON,new Locator.GetByRoleOptions().setName("上一页")).click();
            f.page.waitForFunction("() => !new URL(location.href).searchParams.has('page') && document.querySelector('[data-personal-asset=favorites]')?.textContent.includes('第 1 页')");
            assertEquals(20,asset.locator(".n5-source").count());
            f.page.goBack();f.page.waitForFunction("() => new URL(location.href).searchParams.get('page') === '2' && document.querySelector('[data-personal-asset=favorites]')?.textContent.includes('第 2 页')");
            f.page.locator("#n21AssetNav [data-n21-asset=submissions]").click();
            f.page.waitForFunction("() => location.pathname.endsWith('/account/submissions') && !new URL(location.href).searchParams.has('page')");
            var published=f.page.locator("[data-personal-asset=submissions]").getByRole(AriaRole.LINK,new Locator.GetByRoleOptions().setName("查看已发布内容"));
            published.click();f.page.waitForSelector("#n22GuideDetail h1");assertTrue(URI.create(f.page.url()).getPath().equals(base+"/guides/test-guide"));
            assertEquals(base+"/guides",f.page.locator(".n22-breadcrumb a").nth(1).getAttribute("href"));
            assertEquals("/guides?legacy=1",f.page.getByRole(AriaRole.LINK,new Page.GetByRoleOptions().setName("攻略中心（旧版）")).getAttribute("href"));
            f.page.goBack();f.page.waitForSelector("#n21AssetNav");
            f.page.getByRole(AriaRole.LINK,new Page.GetByRoleOptions().setName("撰写与管理投稿")).click();f.page.waitForSelector("#n25Submit:not([disabled])");
            assertEquals(base+"/account/submit",URI.create(f.page.url()).getPath());
            assertTrue(reads.stream().anyMatch(query->query.contains("page=1")));assertEquals(List.of(),f.errors);
        }});
    }
    static String order(String no,String name,String status,String updated){return "{\"businessNo\":\""+no+"\",\"gameName\":\"测试游戏\",\"serviceName\":\""+name+"\",\"status\":\""+status+"\",\"updatedAt\":\""+updated+"\"}";}
    interface Check{void run(Browser browser)throws Exception;}
    void both(Check check)throws Exception{try(Playwright p=Playwright.create();Browser a=p.chromium().launch();Browser b=p.webkit().launch()){for(Browser browser:List.of(a,b))check.run(browser);}}
}
