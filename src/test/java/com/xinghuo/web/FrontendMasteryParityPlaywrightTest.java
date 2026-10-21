package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.*;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import com.microsoft.playwright.*;
import java.nio.file.*;
import java.net.URI;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

@EnabledIfEnvironmentVariable(named="PLAYWRIGHT_UI_TESTS", matches="1")
class FrontendMasteryParityPlaywrightTest {
    @Test void nextRetainsSharedMasteryRulesCompleteChallengesImagesAndLegacyLocalRecords() throws Exception {
        String html = new FrontendNextPreviewController().frontendNext().getBody();
        String catalog = Files.readString(Path.of("src/main/resources/static/assets/tool-station-games.json"));
        try (Playwright p=Playwright.create();Browser chromium=p.chromium().launch();Browser webkit=p.webkit().launch()) {
            for (Browser browser:List.of(chromium,webkit)) try(Page page=browser.newPage(new Browser.NewPageOptions().setViewportSize(390,844))) {
                List<String> errors=new ArrayList<>();page.onPageError(errors::add);
                page.route("**/*",route->{
                    String path=URI.create(route.request().url()).getPath();
                    if(path.equals("/api/tool-station"))route.fulfill(new Route.FulfillOptions().setContentType("application/json").setBody(catalog));
                    else if(path.startsWith("/api/"))route.fulfill(new Route.FulfillOptions().setStatus(401).setContentType("application/json").setBody("{}"));
                    else if(path.startsWith("/assets/")||path.endsWith(".js"))route.fulfill(new Route.FulfillOptions().setPath(Path.of("src/main/resources/static"+path)));
                    else route.fulfill(new Route.FulfillOptions().setContentType("text/html;charset=utf-8").setBody(html));
                });
                page.addInitScript("""
                    if(!localStorage.getItem('xinghuo-mw4-camo-tracker-v2'))localStorage.setItem('xinghuo-mw4-camo-tracker-v2',JSON.stringify({activeMode:'MULTIPLAYER',
                      progress:{MULTIPLAYER:{HAN86:{TIER_1:true}}},favorites:['HAN86'],guideDone:{'HAN86::mil-0':true}}));
                    """);
                page.onDialog(Dialog::accept);
                page.navigate("https://example.test/next/mastery");page.waitForSelector("[data-mastery-item]");
                assertThat(page.locator(".n24-weapon")).hasCount(33);
                assertThat(page.locator(".n24-tier-grid")).containsText("已有 1/33 把武器的历史记录");
                page.locator("[data-mastery-item]").nth(2).check();
                assertEquals(true,page.evaluate("()=>Object.values(JSON.parse(localStorage.getItem('xinghuo-mw4-camo-tracker-v2')).progress.MULTIPLAYER).every(w=>w.TIER_3)"));
                page.locator("[data-mastery-item]").nth(2).uncheck();
                assertEquals(true,page.evaluate("()=>JSON.parse(localStorage.getItem('xinghuo-mw4-camo-tracker-v2')).progress.MULTIPLAYER.HAN86.TIER_1"));
                page.locator("[data-mastery-item]").nth(1).check();
                page.getByLabel("搜索武器",new Page.GetByLabelOptions().setExact(true)).fill("汉86");
                assertThat(page.locator(".n24-weapon")).hasCount(1);
                page.locator("[data-guide-weapon=HAN86] summary").click();
                assertThat(page.locator("[data-guide-item^='HAN86::mil-']")).hasCount(8);
                assertThat(page.locator("[data-guide-item^='HAN86::special-']")).hasCount(3);
                assertThat(page.locator("[data-guide-item^='HAN86::comp-']")).hasCount(4);
                assertThat(page.locator(".n24-guide")).containsText("满级 72");
                assertThat(page.locator(".n24-guide")).containsText("消音50次");
                assertThat(page.locator(".n24-guide")).containsText("DMZ：消音125次");
                assertThat(page.locator("[data-guide-item='HAN86::mil-0']")).isChecked();
                String progress=String.valueOf(page.evaluate("()=>JSON.stringify(JSON.parse(localStorage.getItem('xinghuo-mw4-camo-tracker-v2')).progress)"));
                page.locator("[data-guide-item='HAN86::special-1']").check();
                assertEquals(progress,page.evaluate("()=>JSON.stringify(JSON.parse(localStorage.getItem('xinghuo-mw4-camo-tracker-v2')).progress)"));
                page.locator(".n24-guide img").first().scrollIntoViewIfNeeded();
                page.waitForFunction("()=>document.querySelector('.n24-guide img').naturalWidth>0");
                assertTrue(page.locator(".n24-guide img").first().getAttribute("src").startsWith("/assets/mini-camo/"));
                for(int width:new int[]{320,390,1440}) {
                    page.setViewportSize(width,900);assertEquals(true,page.evaluate("()=>document.documentElement.scrollWidth<=innerWidth"));
                }
                page.setViewportSize(390,844);
                page.locator(".n24-guide").screenshot(new Locator.ScreenshotOptions().setPath(Path.of("target/next-mastery-challenges.png")));
                page.reload();page.waitForSelector("[data-mastery-item]");
                assertThat(page.locator("[data-mastery-item]").nth(1)).isChecked();
                page.locator("[data-guide-weapon=HAN86] summary").click();
                assertThat(page.locator("[data-guide-item='HAN86::special-1']")).isChecked();
                page.locator("#n24Reset").click();
                assertThat(page.locator("[data-mastery-item]:checked")).hasCount(0);
                assertThat(page.locator("[data-guide-item='HAN86::special-1']")).isChecked();
                assertThat(page.locator("[data-favorite=HAN86]")).hasAttribute("aria-pressed","true");
                page.locator("#n24Tab-WARZONE").click();
                assertThat(page.locator("[data-mastery-item]:checked")).hasCount(0);
                assertThat(page.locator(".n24-guide")).containsText("职业通用挑战参考");
                page.locator("#n24Tab-PYROCLAST").click();page.locator(".n24-global input").nth(3).check();
                assertEquals(1,page.locator(".n24-global input:checked").count());
                page.locator("#n24Reset").click();
                assertEquals(0,page.locator(".n24-global input:checked").count());
                assertEquals(true,page.evaluate("()=>JSON.parse(localStorage.getItem('xinghuo-mw4-camo-tracker-v2')).guideDone['HAN86::special-1']"));
                assertEquals(List.of(),errors);
            }
        }
    }
}
