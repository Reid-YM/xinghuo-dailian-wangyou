package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.*;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.AriaRole;
import java.net.URI;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

@EnabledIfEnvironmentVariable(named="PLAYWRIGHT_UI_TESTS",matches="1")
class FrontendGuideSearchPlaywrightTest {
    @Test void serverSearchSortRetryAndBackPreserveControlsAndIgnoreSupersededResponses() throws Exception {
        try(Playwright p=Playwright.create();Browser chromium=p.chromium().launch();Browser webkit=p.webkit().launch()) {
            for(Browser browser:List.of(chromium,webkit))for(String base:List.of("/next",""))
                try(var f=new FrontendCanonicalRoutesPlaywrightTest.Fixture(browser,390,false)) {
                    List<String> reads=new ArrayList<>();
                    List<Route> delayed=new ArrayList<>();
                    List<Route> catalogs=new ArrayList<>();
                    f.page.route("**/api/catalog",catalogs::add);
                    boolean[] failed={false};
                    f.page.route("**/api/guides?*",route->{
                        URI uri=URI.create(route.request().url());
                        if(!uri.getPath().equals("/api/guides")){route.fallback();return;}
                        String q=Objects.toString(uri.getQuery(),"");reads.add(q);
                        if(q.contains("q=slow")){delayed.add(route);return;}
                        if(q.contains("q=retry")&&!failed[0]){failed[0]=true;f.json(route,503,"{}");return;}
                        String title=q.contains("q=Rare")?"仅服务器匹配的旧攻略":q.contains("q=retry")?"重试恢复攻略":"初始攻略";
                        f.json(route,200,"[{\"slug\":\"test-guide\",\"title\":\""+title+"\",\"gameCode\":\"CODMW4\"}]");
                    });
                    f.open(base+"/guides",".n23-card");
                    f.page.waitForCondition(()->!catalogs.isEmpty());
                    f.json(catalogs.removeFirst(),200,FrontendCanonicalRoutesPlaywrightTest.CATALOG.replace("\"games\":[", "\"games\":[{\"code\":\"OTHER\",\"name\":\"另一游戏\"},"));
                    f.page.waitForFunction("() => !!document.querySelector('#n23GuideGame option[value=OTHER]')");
                    var search=f.page.getByLabel("搜索攻略",new Page.GetByLabelOptions().setExact(true));
                    int beforeComposition=reads.size();
                    search.evaluate("el => {el.focus();el.dispatchEvent(new CompositionEvent('compositionstart',{bubbles:true}));el.value='中';el.dispatchEvent(new InputEvent('input',{bubbles:true,isComposing:true}));}");
                    f.page.waitForTimeout(250);assertEquals(beforeComposition,reads.size());
                    search.evaluate("el => {el.value='中文';el.dispatchEvent(new CompositionEvent('compositionend',{bubbles:true}));}");
                    f.page.waitForCondition(()->reads.size()>beforeComposition);
                    f.page.waitForSelector(".n23-grid[aria-busy=false]");
                    search.fill("slow");f.page.waitForCondition(()->!delayed.isEmpty());
                    assertEquals("true",f.page.locator(".n23-grid").getAttribute("aria-busy"));
                    search.fill("Rare English");f.page.getByText("仅服务器匹配的旧攻略",new Page.GetByTextOptions().setExact(true)).waitFor();
                    f.json(delayed.removeFirst(),200,"[{\"slug\":\"old\",\"title\":\"过期结果\"}]");
                    f.page.waitForTimeout(250);
                    assertEquals(0,f.page.getByText("过期结果",new Page.GetByTextOptions().setExact(true)).count());
                    assertEquals("Rare English",search.inputValue());assertTrue(search.evaluate("el => document.activeElement===el").equals(true));
                    int beforeSpace=reads.size();search.fill("Rare English ");
                    f.page.waitForCondition(()->reads.size()>beforeSpace);
                    f.page.waitForSelector(".n23-grid[aria-busy=false]");
                    assertEquals("Rare English ",search.inputValue());
                    search.evaluate("el => el.setSelectionRange(5,5)");
                    f.page.evaluate("() => window.XHFrontendNextN23.reload()");
                    f.page.waitForSelector(".n23-grid[aria-busy=false]");
                    assertEquals(5,((Number)search.evaluate("el => el.selectionStart")).intValue());
                    f.page.getByLabel("攻略排序",new Page.GetByLabelOptions().setExact(true)).selectOption("popular");
                    f.page.waitForCondition(()->reads.stream().anyMatch(q->q.contains("sort=popular")&&q.contains("q=Rare+English")));
                    f.page.getByLabel("筛选攻略游戏",new Page.GetByLabelOptions().setExact(true)).selectOption("CODMW4");
                    f.page.waitForCondition(()->reads.stream().anyMatch(q->q.contains("game=CODMW4")&&q.contains("sort=popular")));
                    f.page.locator(".n23-card").click();f.page.waitForSelector("#n22GuideDetail h1");
                    f.page.goBack();f.page.waitForSelector(".n23-card");
                    assertEquals("Rare English",search.inputValue());assertEquals("popular",f.page.locator("#n23GuideSort").inputValue());
                    assertEquals("CODMW4",f.page.locator("#n23GuideGame").inputValue());
                    search.fill("retry");f.page.getByRole(AriaRole.HEADING,new Page.GetByRoleOptions().setName("攻略读取失败")).waitFor();
                    assertEquals("retry",search.inputValue());
                    f.page.locator(".n23-grid").getByRole(AriaRole.BUTTON,new Locator.GetByRoleOptions().setName("重新读取")).click();
                    f.page.getByText("重试恢复攻略",new Page.GetByTextOptions().setExact(true)).waitFor();
                    assertEquals(List.of(),f.errors);
                }
        }
    }
}
