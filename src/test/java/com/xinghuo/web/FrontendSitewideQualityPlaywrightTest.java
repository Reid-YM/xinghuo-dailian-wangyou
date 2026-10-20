package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.*;
import com.microsoft.playwright.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

@EnabledIfEnvironmentVariable(named="PLAYWRIGHT_UI_TESTS",matches="1")
class FrontendSitewideQualityPlaywrightTest {
    interface Check { void run(Browser browser)throws Exception; }
    private void both(Check check)throws Exception {
        try(Playwright p=Playwright.create();Browser chrome=p.chromium().launch();Browser webkit=p.webkit().launch()) {
            for(Browser browser:List.of(chrome,webkit))check.run(browser);
        }
    }
    @Test void primaryNavigationFollowsPublicPagesDetailsAndBrowserHistory()throws Exception {
        both(browser->{for(int width:new int[]{320,1280})try(var f=new FrontendCanonicalRoutesPlaywrightTest.Fixture(browser,width,false)){
            var paths=new LinkedHashMap<String,String>();
            paths.put("/guides","guides");paths.put("/guides/test-guide","guides");paths.put("/mastery","mastery");
            paths.put("/records","records");paths.put("/record/"+FrontendCanonicalRoutesPlaywrightTest.NO,"records");
            paths.put("/contact","contact");paths.put("/service/level","services");paths.put("/game/test-game","services");
            for(String base:List.of("","/next"))for(var entry:paths.entrySet()) {
                f.page.navigate(f.origin+base+entry.getKey());
                f.page.waitForFunction("()=>document.documentElement.dataset.siteConfiguration==='ready'");
                assertActive(f.page,entry.getValue());
                assertEquals(true,f.page.evaluate("()=>document.documentElement.scrollWidth<=innerWidth"));
                assertEquals(1,f.page.getByRole(com.microsoft.playwright.options.AriaRole.MAIN).count());
            }
            f.open("/guides","#n23GuideSearch");
            f.page.locator((width==320?".next-mobile-dock":".nav")+" a[data-go=services]").click();assertActive(f.page,"services");
            f.page.goBack();f.page.waitForSelector("#n23GuidesCenter.active");assertActive(f.page,"guides");
            f.page.goForward();assertActive(f.page,"services");
            assertEquals(List.of(),f.errors);
        }
        try(var account=new FrontendCanonicalRoutesPlaywrightTest.Fixture(browser,390,true)) {
            account.open("/account/submit","#n25SubmissionWorkbench.active");assertActive(account.page,"account");
        }});
    }
    private void assertActive(Page page,String section) {
        page.waitForFunction("""
            section=>{const nav=innerWidth<=1000?document.querySelector('.next-mobile-dock'):document.querySelector('.nav');
              const links=[...nav.querySelectorAll('.navlink[aria-current="page"]')];
              if(links.length!==1)return false;
              const item=links[0].getBoundingClientRect(),bounds=nav.getBoundingClientRect();
              return (links[0].dataset.go===section || new URL(links[0].href).pathname.replace(/^\\/next(?=\\/|$)/,'')==='/'+section)
                && (innerWidth>1000 || (item.left>=bounds.left-1 && item.right<=bounds.right+1));}
            """,section,new Page.WaitForFunctionOptions().setTimeout(5000));
    }
    @Test void customerPagesUseBusinessLabelsWithoutChangingFilterValues()throws Exception {
        both(browser->{try(var f=new FrontendCanonicalRoutesPlaywrightTest.Fixture(browser,320,false)){
            f.page.route("**/api/site-config",r->f.json(r,200,"""
                {"brandName":"测试星火","quoteEnabled":true,"footerText":"前台仅展示客户需要的信息","wecomKefuUrl":"https://example.test/support"}
                """));
            f.page.route("**/api/guides?*",r->f.json(r,200,"""
                [{"slug":"test-guide","title":"测试攻略","gameCode":"CODMW4","difficulty":"BEGINNER"}]
                """));
            f.page.route("**/api/guides/test-guide",r->f.json(r,200,FrontendCanonicalRoutesPlaywrightTest.GUIDE.replace("\"title\":\"测试攻略\"","\"difficulty\":\"EXPERT\",\"title\":\"测试攻略\"")));
            f.open("/guides",".n23-card");
            assertEquals("测试游戏",f.page.locator("#n23GuideGame option[value=CODMW4]").textContent());
            assertTrue(f.page.locator(".n23-card-meta").innerText().contains("入门"));
            assertFalse(f.page.locator("#n23GuidesCenter").innerText().contains("CODMW4"));
            f.page.locator("#n23GuideGame").selectOption("CODMW4");
            assertTrue(f.page.url().contains("game=CODMW4"));
            f.page.waitForSelector(".n23-card");
            assertTrue(f.page.locator(".n23-summary").innerText().contains("测试游戏"));
            assertFalse(f.page.locator("#n23GuidesCenter").innerText().contains("CODMW4"));
            f.open("/guides/test-guide","#n22GuideDetail h1");
            assertTrue(f.page.locator("#n22GuideDetail").innerText().contains("精通"));
            assertFalse(f.page.locator("#n22GuideDetail").innerText().contains("EXPERT"));
            for(String path:List.of("/contact","/trust","/account/register","/account/reset")) {
                f.page.navigate(f.origin+path);f.page.waitForFunction("()=>document.documentElement.dataset.siteConfiguration==='ready'");
                String copy=f.page.locator(".page.active").innerText();
                for(String internal:List.of("/api/","写死","后台配置","后台站点","账号枚举","浏览器登录会话")) assertFalse(copy.contains(internal),path+": "+internal);
            }
            assertEquals("报价与服务安排以客服确认为准。",f.page.locator("[data-config=footerText]").textContent());
            f.page.unroute("**/api/site-config");
            f.page.route("**/api/site-config",r->f.json(r,200,"{\"brandName\":\"测试星火\",\"quoteEnabled\":true,\"footerText\":\"自定义服务说明\"}"));
            f.open("/services",".serviceCard");
            assertEquals("自定义服务说明",f.page.locator("[data-config=footerText]").textContent());
            assertEquals("星火 · 游戏服务",f.page.locator(".badge").textContent());
            assertEquals(List.of(),f.errors);
        }});
    }
    @Test void stalledPublicReadsExitLoadingAndRetryAfterTransportRecovers()throws Exception {
        both(browser->{for(String area:List.of("guides","mastery","configuration"))try(var f=new FrontendCanonicalRoutesPlaywrightTest.Fixture(browser,390,false)){
            String endpoint=area.equals("guides")?"/api/guides?":area.equals("mastery")?"/api/tool-station":"/api/site-config";
            f.page.addInitScript("""
                (()=>{const original=window.fetch.bind(window);window.restorePublicReads=()=>{window.fetch=original;};
                  window.fetch=(url,options)=>String(url).startsWith('%s')
                    ?%s:original(url,options);})();
                """.formatted(endpoint,area.equals("mastery")?"new Promise(()=>{})":"Promise.resolve({ok:true,json:()=>new Promise(()=>{})})"));
            String path=area.equals("configuration")?"/contact":"/"+area;
            f.page.navigate(f.origin+path);
            String retry=area.equals("guides")?".n23-state button":area.equals("mastery")?".n24-state button":"#nextConfigState button";
            f.page.waitForSelector(retry,new Page.WaitForSelectorOptions().setTimeout(15000));
            assertFalse(f.page.locator(".page.active").innerText().contains("正在加载"));
            f.page.evaluate("()=>window.restorePublicReads()");f.page.locator(retry).click();
            String ready=area.equals("guides")?".n23-card":area.equals("mastery")?"#n24ModePanel":"#n27Contact .n27-grid";
            f.page.waitForSelector(ready,new Page.WaitForSelectorOptions().setTimeout(5000));
            assertEquals(1,f.page.getByRole(com.microsoft.playwright.options.AriaRole.MAIN).count());
            assertEquals(List.of(),f.errors);
        }});
    }
}
