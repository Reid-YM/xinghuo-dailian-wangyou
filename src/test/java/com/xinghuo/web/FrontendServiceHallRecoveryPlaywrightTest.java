package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.*;
import com.microsoft.playwright.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

@EnabledIfEnvironmentVariable(named="PLAYWRIGHT_UI_TESTS",matches="1")
class FrontendServiceHallRecoveryPlaywrightTest {
    interface Check {void run(Browser browser)throws Exception;}
    private void both(Check check)throws Exception {
        try(Playwright p=Playwright.create();Browser chrome=p.chromium().launch();Browser webkit=p.webkit().launch()){
            for(Browser browser:List.of(chrome,webkit))check.run(browser);
        }
    }
    @Test void servicesRenderBeforeSlowCatalogAndFiltersBecomeReadyWhenItArrives()throws Exception {
        both(browser->{for(String base:List.of("","/next"))try(var f=new FrontendCanonicalRoutesPlaywrightTest.Fixture(browser,320,false)){
            List<Route> held=new ArrayList<>();f.page.route("**/api/catalog",held::add);
            f.page.navigate(f.origin+base+"/services?game=CODMW4");
            f.page.waitForSelector(".serviceCard",new Page.WaitForSelectorOptions().setTimeout(4000));
            assertFalse(held.isEmpty());assertNull(f.page.evaluate("()=>window.XHFrontendNextN2.state.catalog"));
            assertEquals("false",f.page.locator(".serviceGrid").getAttribute("aria-busy"));
            assertEquals(1,f.page.getByRole(com.microsoft.playwright.options.AriaRole.MAIN).count());
            assertTrue(f.page.locator("#n2DataStatus").innerText().contains("服务已读取"));
            assertFalse(f.page.locator(".serviceGrid").innerText().contains("正在读取"));
            assertTrue(f.page.locator("#qGame").isDisabled());
            f.json(held.getFirst(),200,FrontendCanonicalRoutesPlaywrightTest.CATALOG);
            f.page.waitForFunction("()=>document.querySelector('#n2DataStatus').textContent.includes('服务目录已更新')");
            assertFalse(f.page.locator("#qGame").isDisabled());
            assertEquals("CODMW4",f.page.locator("#hallGame").inputValue());
            assertEquals(true,f.page.evaluate("()=>document.documentElement.scrollWidth<=innerWidth"));
            assertEquals(List.of(),f.errors);
        }});
    }
    @Test void desktopCardsStayBesideCategoriesAndInFirstScreenAfterFilteringAndReload()throws Exception {
        both(browser->{try(var f=new FrontendCanonicalRoutesPlaywrightTest.Fixture(browser,1280,false)){
            for(String base:List.of("","/next")) {
                f.open(base+"/services",".serviceCard");
                assertDesktopCards(f.page);
                f.page.locator("#search").fill("等级");assertDesktopCards(f.page);
                f.page.locator("#search").fill("");
                f.page.reload();f.page.waitForSelector(".serviceCard");assertDesktopCards(f.page);
            }
            assertEquals(List.of(),f.errors);
        }});
    }
    @Test void sparseResultsDoNotStretchToTallSidebarAndSameNamedCategoriesAreDistinct()throws Exception {
        both(browser->{try(var f=new FrontendCanonicalRoutesPlaywrightTest.Fixture(browser,1280,false)){
            var catalog=new StringBuilder("{\"games\":[{\"code\":\"CODMW4\",\"name\":\"使命召唤\"},{\"code\":\"DMZ\",\"name\":\"DMZ\"}],\"platforms\":[],\"gamePlatforms\":[],\"categories\":[");
            var services=new StringBuilder("[");
            for(int i=0;i<20;i++) {
                if(i>0){catalog.append(',');services.append(',');}
                String name=i<2?"迷彩任务":"分类"+i;
                String game=i==1?"DMZ":"CODMW4";
                catalog.append("{\"code\":\"CAT").append(i).append("\",\"gameCode\":\"").append(game).append("\",\"name\":\"").append(name).append("\"}");
                services.append("{\"id\":").append(i+1).append(",\"slug\":\"service-").append(i).append("\",\"gameCode\":\"").append(game).append("\",\"category\":\"CAT").append(i==2||i==3?0:i).append("\",\"name\":\"服务").append(i).append("\",\"priceMode\":\"FIXED\",\"priceFixed\":20}");
            }
            String catalogJson=catalog.append("]}").toString(),servicesJson=services.append("]").toString();
            f.page.route("**/api/catalog",route->f.json(route,200,catalogJson));
            f.page.route("**/api/services",route->f.json(route,200,servicesJson));
            for(String base:List.of("","/next")) {
                f.open(base+"/services?category=cat0",".serviceCard");
                f.page.waitForFunction("()=>document.querySelector('#n2DataStatus').textContent.includes('服务目录已更新')");
                assertEquals(3,f.page.locator(".serviceCard:not([hidden])").count());
                assertTrue(f.page.locator(".cat[data-cat='cat0']").innerText().contains("使命召唤"));
                assertTrue(f.page.locator(".cat[data-cat='cat1']").innerText().contains("DMZ"));
                assertCompactResults(f.page);
                f.page.locator(".cat[data-cat='all']").click();
                assertEquals(20,f.page.locator(".serviceCard:not([hidden])").count());
                f.page.locator(".cat[data-cat='cat1']").click();assertCompactResults(f.page);
                f.page.reload();f.page.waitForSelector(".serviceCard:not([hidden])");assertCompactResults(f.page);
                assertEquals("true",f.page.locator(".cat[data-cat='cat1']").getAttribute("aria-pressed"));
                assertEquals(1,f.page.locator(".serviceCard:not([hidden])").count());
            }
            assertEquals(List.of(),f.errors);
        }});
    }
    private void assertCompactResults(Page page) {
        assertEquals(true,page.evaluate("""
            ()=>{const side=document.querySelector('.services .side').getBoundingClientRect();
                const card=document.querySelector('.serviceCard:not([hidden])').getBoundingClientRect();
                const summary=document.querySelector('#hallResults').getBoundingClientRect();
                const body=document.querySelector('.serviceCard:not([hidden]) .serviceBody').getBoundingClientRect();
                return side.height>600 && card.top-summary.bottom<40 && card.height<400
                    && card.bottom-body.bottom<4 && card.left>=side.right
                    && document.documentElement.scrollWidth<=innerWidth;}
            """));
    }
    private void assertDesktopCards(Page page) {
        assertEquals(true,page.evaluate("""
            ()=>{const side=document.querySelector('.services .side').getBoundingClientRect();
                const card=document.querySelector('.serviceGrid .serviceCard:not([hidden])').getBoundingClientRect();
                const summary=document.querySelector('#hallResults').getBoundingClientRect();
                return card.left>=side.right && card.top>=side.top && card.top<side.top+180
                    && card.top<innerHeight && card.width>200 && summary.bottom<=card.top
                    && document.documentElement.scrollWidth<=innerWidth;}
            """));
    }
    @Test void pendingFetchAndPendingJsonBodyBothTimeOutAndRecoverWithRetry()throws Exception {
        both(browser->{for(boolean bodyStall:List.of(false,true))try(var f=new FrontendCanonicalRoutesPlaywrightTest.Fixture(browser,390,false)){
            if(bodyStall)f.page.addInitScript("""
                (()=>{const original=window.fetch.bind(window);window.restoreHallFetch=()=>{window.fetch=original;};window.fetch=(url,options)=>String(url)==='/api/services'
                  ?Promise.resolve({ok:true,json:()=>new Promise(()=>{})}):original(url,options);})();
                """);
            else f.page.route("**/api/services",route->{});
            f.page.navigate(f.origin+"/services");
            f.page.waitForSelector("#n2DataStatus button",new Page.WaitForSelectorOptions().setTimeout(15000));
            assertEquals(0,f.page.locator(".serviceCard").count());
            assertTrue(f.page.locator(".serviceGrid").innerText().contains("无法读取"));
            assertEquals("false",f.page.locator(".serviceGrid").getAttribute("aria-busy"));
            assertEquals(1,f.page.getByRole(com.microsoft.playwright.options.AriaRole.MAIN).count());
            assertTrue(f.page.locator("#qSvc").isDisabled());
            if(bodyStall)f.page.evaluate("()=>window.restoreHallFetch()");
            else f.page.unroute("**/api/services");
            f.page.locator("#n2DataStatus button").click();
            f.page.waitForFunction("()=>document.querySelector('#n2DataStatus').textContent.includes('服务目录已更新')");
            assertEquals(1,f.page.locator(".serviceCard").count());assertFalse(f.page.locator("#qSvc").isDisabled());
            assertEquals(List.of(),f.errors);
        }});
    }
    @Test void supersededRefreshCannotOverwriteNewServicesAndFailureKeepsRealPreviousCards()throws Exception {
        both(browser->{try(var f=new FrontendCanonicalRoutesPlaywrightTest.Fixture(browser,390,false)){
            f.open("/services",".serviceCard");
            f.page.waitForFunction("()=>document.querySelector('#n2DataStatus').textContent.includes('服务目录已更新')");
            f.page.evaluate("""
                ()=>{const original=window.fetch.bind(window);let delay=true;window.fetch=(url,options)=>{
                    if(url==='/api/services'&&delay){delay=false;return new Promise(resolve=>window.releaseOld=()=>resolve({ok:true,json:async()=>{window.oldDecoded=true;return [{slug:'obsolete',name:'过期服务'}];}}));}
                    return original(url,options);
                };window.XHFrontendNextN2.reload();}
                """);
            f.page.waitForFunction("()=>!!window.releaseOld");
            f.page.route("**/api/services",route->f.json(route,200,FrontendCanonicalRoutesPlaywrightTest.SERVICES.replace("等级服务","最新真实服务")));
            f.page.evaluate("()=>{window.XHFrontendNextN2.reload();}");
            f.page.waitForFunction("()=>document.querySelector('.serviceGrid').textContent.includes('最新真实服务')");
            f.page.evaluate("()=>window.releaseOld()");f.page.waitForFunction("()=>window.oldDecoded===true");
            assertFalse(f.page.locator(".serviceGrid").textContent().contains("过期服务"));
            f.page.unroute("**/api/services");f.page.route("**/api/services",route->f.json(route,503,"{}"));
            f.page.evaluate("()=>{window.XHFrontendNextN2.reload();}");f.page.waitForSelector("#n2DataStatus button");
            assertTrue(f.page.locator(".serviceGrid").textContent().contains("最新真实服务"));
            assertTrue(f.page.locator("#n2DataStatus").textContent().contains("保留上次真实数据"));
            assertEquals("false",f.page.locator(".serviceGrid").getAttribute("aria-busy"));
            assertEquals(1,f.page.getByRole(com.microsoft.playwright.options.AriaRole.MAIN).count());
            assertEquals(List.of(),f.errors);
        }});
    }
}
