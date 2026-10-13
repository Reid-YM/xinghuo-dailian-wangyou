package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

@EnabledIfEnvironmentVariable(named = "PLAYWRIGHT_UI_TESTS", matches = "1")
class XingweiServiceCatalogWorkbenchPlaywrightTest {
    private static final Path MAIN = Path.of("src/main/resources");

    @Test
    void serviceCatalogRendersFiltersAndStaysInsidePhoneViewport() throws Exception {
        try (Playwright playwright = Playwright.create(); Browser browser = playwright.chromium().launch()) {
            Page page = browser.newPage();
            page.setViewportSize(1440, 900);
            page.setContent(Files.readString(MAIN.resolve("templates/admin-xingwei-catalog.html")));
            page.addStyleTag(new Page.AddStyleTagOptions().setPath(MAIN.resolve("static/assets/admin-xingwei.css")));
            page.addStyleTag(new Page.AddStyleTagOptions().setPath(MAIN.resolve("static/assets/admin-xingwei-org.css")));
            page.addStyleTag(new Page.AddStyleTagOptions().setPath(MAIN.resolve("static/assets/admin-xingwei-catalog.css")));
            page.evaluate("""
                    () => { window.fetch = async () => ({ok:true, json:async () => ({
                      readOnly:true, source:'EXISTING_SERVICE_CATALOG',
                      counts:{services:2,activeServices:1,games:2,categories:2,platforms:2},
                      services:[
                        {id:1,slug:'weapon-leveling',gameCode:'MW4',name:'武器升级',category:'LEVELING',shortDesc:'手打升级',priceMode:'FIXED',priceFixed:88,durationText:'1-2天',status:'AVAILABLE',manualPriority:10,active:true,sortOrder:1,version:3},
                        {id:2,slug:'rank-service',gameCode:'OTHER',name:'排位服务',category:'RANK',shortDesc:'测试',priceMode:'RANGE',priceMin:100,priceMax:200,durationText:'咨询',status:'OFFLINE',manualPriority:0,active:false,sortOrder:2,version:1}
                      ],
                      games:[
                        {code:'MW4',name:'使命召唤现代战争4',businessStatus:'ACTIVE',active:true,categoryCount:1,serviceCount:1,platformCodes:['PC']},
                        {code:'OTHER',name:'其他游戏',businessStatus:'PLANNED',active:true,categoryCount:1,serviceCount:1,platformCodes:['PC','PS5']}
                      ],
                      categories:[{code:'LEVELING',name:'升级',gameCode:'MW4',active:true,serviceCount:1},{code:'RANK',name:'排位',gameCode:'OTHER',active:true,serviceCount:1}], platforms:[]
                    })}); };
                    """);
            page.addScriptTag(new Page.AddScriptTagOptions().setPath(MAIN.resolve("static/assets/admin-xingwei-catalog.js")));
            page.waitForFunction("() => document.querySelectorAll('#xcatServiceRows tr').length === 2");
            assertEquals("2", page.locator("#xcatServiceCount").textContent());
            assertTrue(page.locator("#xcatServiceRows").textContent().contains("武器升级"));
            assertTrue(page.locator("#xcatGameRows").textContent().contains("使命召唤现代战争4"));

            page.locator("#xcatSearch").fill("排位");
            assertEquals(1, ((Number) page.evaluate("() => [...document.querySelectorAll('#xcatServiceRows tr')].filter(x => !x.hidden).length")).intValue());
            page.locator("#xcatSearch").fill("");
            page.locator("#xcatStateFilter").selectOption("inactive");
            assertEquals(1, ((Number) page.evaluate("() => [...document.querySelectorAll('#xcatServiceRows tr')].filter(x => !x.hidden).length")).intValue());

            page.setViewportSize(390, 844);
            assertTrue((Boolean) page.evaluate("() => document.documentElement.scrollWidth <= innerWidth + 1"));
        }
    }
}
