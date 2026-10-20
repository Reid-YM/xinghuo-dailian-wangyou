package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.playwright.*;
import com.xinghuo.account.service.AccountSessionService;
import com.xinghuo.controller.PublicApiController;
import com.xinghuo.dto.ApiDtos;
import com.xinghuo.dto.PublicQuoteRequest;
import com.xinghuo.service.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.mockito.ArgumentCaptor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/** Isolated browser -> real MVC request validation -> mocked business persistence. No external writes. */
@EnabledIfEnvironmentVariable(named="PLAYWRIGHT_UI_TESTS",matches="1")
class CatalogQuoteIntegrationPlaywrightTest {
    static final String CATALOG="""
        {"games":[{"code":"CODMW4","name":"游戏甲"},{"code":"SECOND","name":"游戏乙"}],
        "categories":[{"code":"LEVEL","gameCode":"CODMW4","name":"等级服务"},{"code":"RANK","gameCode":"SECOND","name":"排位服务"}],
        "platforms":[{"code":"PC","name":"后台配置的电脑平台","active":true},{"code":"CONSOLE","name":"后台配置的主机平台","active":true},{"code":"OFF","name":"停用平台","active":false}],
        "gamePlatforms":[{"gameCode":"CODMW4","platformCode":"PC","active":true},{"gameCode":"CODMW4","platformCode":"OFF","active":true},{"gameCode":"CODMW4","platformCode":"CONSOLE","active":false},{"gameCode":"SECOND","platformCode":"CONSOLE","active":true}]}
        """;
    static final String SERVICES=FrontendCanonicalRoutesPlaywrightTest.SERVICES.strip().replace("\"category\":\"LEVEL\"","\"category\":\"level\"").replaceAll("\\]$","") + "," + """
        {"id":2,"slug":"second-rank","gameCode":"SECOND","category":"rank","name":"游戏乙排位","priceMode":"QUOTE"}]
        """;
    interface Check {void run(Browser browser)throws Exception;}
    void both(Check check)throws Exception {
        try(Playwright p=Playwright.create();Browser chrome=p.chromium().launch();Browser webkit=p.webkit().launch()) {
            for(Browser browser:List.of(chrome,webkit))check.run(browser);
        }
    }
    void routes(FrontendCanonicalRoutesPlaywrightTest.Fixture f,AtomicReference<String> catalog,AtomicReference<String> services) {
        f.page.route("**/api/catalog",r->f.json(r,200,catalog.get()));
        f.page.route("**/api/services",r->f.json(r,200,services.get()));
    }
    void ready(Page page) {page.waitForFunction("()=>document.querySelector('#qSvc')?.disabled===false && document.querySelector('#n2DataStatus')?.textContent.includes('已更新')");}
    void reload(Page page) {page.evaluate("()=>window.XHFrontendNextN2.reload()");}

    @Test void backendCatalogControlsChoicesAndRealQuoteControllerReceivesDisplayName()throws Exception {
        both(browser->{try(var f=new FrontendCanonicalRoutesPlaywrightTest.Fixture(browser,390,false)) {
            var mapper=new ObjectMapper();var taxonomy=mock(TaxonomyService.class);var quotes=mock(QuoteService.class);
            var config=mock(SiteConfigService.class);when(config.isQuoteEnabled()).thenReturn(true);
            var sessions=mock(AccountSessionService.class);when(sessions.current(any())).thenReturn(Optional.empty());
            when(taxonomy.catalog(false)).thenReturn(mapper.readValue(CATALOG,ApiDtos.CatalogResponse.class));
            when(quotes.create(any(),anyString(),anyString(),isNull())).thenReturn(new ApiDtos.QuoteResponse(UUID.randomUUID(),FrontendCanonicalRoutesPlaywrightTest.NO,"NEW",null));
            var mvc=MockMvcBuilders.standaloneSetup(new PublicApiController(config,null,quotes,null,null,null,null,taxonomy,sessions,null,null,null,null)).build();
            f.page.route("**/api/catalog",r->{try {
                var response=mvc.perform(get("/api/catalog")).andReturn().getResponse();
                f.json(r,response.getStatus(),response.getContentAsString(StandardCharsets.UTF_8));
            }catch(Exception e){throw new RuntimeException(e);}});
            f.page.route("**/api/services",r->f.json(r,200,SERVICES));
            f.page.route("**/api/quote",r->{try {
                var response=mvc.perform(post("/api/quote").contentType("application/json").header("Idempotency-Key",r.request().headers().get("idempotency-key")).content(r.request().postData())).andReturn().getResponse();
                f.json(r,response.getStatus(),response.getContentAsString(StandardCharsets.UTF_8));
            }catch(Exception e){throw new RuntimeException(e);}});
            f.open("/quote?service=level","#n3Platform");ready(f.page);
            assertEquals("LEVEL",f.page.locator("#qCat").inputValue());
            assertEquals(List.of("","PC"),f.page.locator("#n3Platform option").evaluateAll("els=>els.map(e=>e.value)"));
            f.page.locator("#n3Platform").selectOption("PC");
            f.page.locator("#qGame").selectOption("SECOND");
            assertEquals(List.of("","CONSOLE"),f.page.locator("#n3Platform option").evaluateAll("els=>els.map(e=>e.value)"));
            assertEquals("",f.page.locator("#n3Platform").inputValue());
            f.page.locator("#n3Platform").selectOption("CONSOLE");
            f.page.locator("#goal").fill("测试需求，不写入生产");f.page.locator("#n3ContactMethod").selectOption("微信");
            f.page.locator("#n3CustomerContact").fill("fixture-contact");f.page.locator("#n3PrivacyConsent").check();f.page.locator("#submit").click();
            f.page.waitForSelector("#n3QuoteMessage:has-text('"+FrontendCanonicalRoutesPlaywrightTest.NO+"')");
            var payload=ArgumentCaptor.forClass(PublicQuoteRequest.class);
            verify(quotes,times(1)).create(payload.capture(),anyString(),anyString(),isNull());
            assertEquals("SECOND",payload.getValue().game());assertEquals("游戏乙排位",payload.getValue().demand());
            assertEquals("后台配置的主机平台",payload.getValue().platform());assertTrue(payload.getValue().privacyConsent());
            assertEquals("/record/"+FrontendCanonicalRoutesPlaywrightTest.NO,f.page.locator("#n3QuoteMessage a").getAttribute("href"));
            assertEquals(true,f.page.evaluate("()=>document.documentElement.scrollWidth<=innerWidth+1"));assertEquals(List.of(),f.errors);
        }});
    }
    @Test void serviceCardQuoteKeepsCatalogCategoryWhenPublicServiceCodeIsLowercase()throws Exception {
        both(browser->{try(var f=new FrontendCanonicalRoutesPlaywrightTest.Fixture(browser,390,false)) {
            routes(f,new AtomicReference<>(CATALOG),new AtomicReference<>(SERVICES));
            f.open("/services",".serviceCard");ready(f.page);
            f.page.locator(".serviceCard[data-service-slug=second-rank] [data-n2-quote]").click();
            f.page.waitForSelector(".page[data-page=quote].active");
            assertEquals("SECOND",f.page.locator("#qGame").inputValue());assertEquals("RANK",f.page.locator("#qCat").inputValue());
            assertEquals("second-rank",f.page.locator("#qSvc").inputValue());
            assertEquals(List.of("","CONSOLE"),f.page.locator("#n3Platform option").evaluateAll("els=>els.map(e=>e.value)"));
            assertEquals(List.of(),f.errors);
        }});
    }
    @Test void catalogRefreshPreservesValidPlatformAndClearsRemovedAssociationWithoutLosingDraft()throws Exception {
        both(browser->{try(var f=new FrontendCanonicalRoutesPlaywrightTest.Fixture(browser,1280,false)) {
            var catalog=new AtomicReference<>(CATALOG);routes(f,catalog,new AtomicReference<>(SERVICES));
            f.open("/quote?service=level","#n3Platform");ready(f.page);
            f.page.locator("#n3Platform").selectOption("PC");f.page.locator("#goal").fill("保留的需求");
            reload(f.page);assertEquals("PC",f.page.locator("#n3Platform").inputValue());
            catalog.set(CATALOG.replace("\"platformCode\":\"PC\",\"active\":true","\"platformCode\":\"PC\",\"active\":false"));reload(f.page);
            assertTrue(f.page.locator("#n3Platform").isDisabled());assertEquals("",f.page.locator("#n3Platform").inputValue());
            assertNull(f.page.evaluate("()=>window.XHFrontendNextN3.payload().platform"));
            assertEquals("保留的需求",f.page.locator("#goal").inputValue());
            catalog.set(CATALOG);reload(f.page);assertFalse(f.page.locator("#n3Platform").isDisabled());
            assertEquals(List.of("","PC"),f.page.locator("#n3Platform option").evaluateAll("els=>els.map(e=>e.value)"));assertEquals(List.of(),f.errors);
        }});
    }
    @Test void emptyServiceRefreshClearsStaleCategoriesAndCanRecoverWithoutReloadingPage()throws Exception {
        both(browser->{try(var f=new FrontendCanonicalRoutesPlaywrightTest.Fixture(browser,390,false)) {
            var services=new AtomicReference<>(SERVICES);routes(f,new AtomicReference<>(CATALOG),services);
            f.open("/services?category=level",".serviceCard:visible");ready(f.page);
            assertEquals(3,f.page.locator(".services .cat").count());services.set("[]");reload(f.page);
            assertEquals(0,f.page.locator(".serviceCard").count());assertEquals(1,f.page.locator(".services .cat").count());
            assertTrue(f.page.locator(".services .cat").textContent().contains("(0)"));
            assertTrue(f.page.locator("#hallResults").textContent().contains("0 项"));assertTrue(f.page.locator("#qSvc").isDisabled());
            f.page.locator("#hallResults button").click();assertFalse(f.page.url().contains("category="));
            assertEquals("true",f.page.locator(".services .cat").getAttribute("aria-pressed"));
            services.set(SERVICES);reload(f.page);ready(f.page);
            assertEquals(2,f.page.locator(".serviceCard:visible").count());assertEquals(3,f.page.locator(".services .cat").count());
            assertEquals(true,f.page.evaluate("()=>document.documentElement.scrollWidth<=innerWidth+1"));assertEquals(List.of(),f.errors);
        }});
    }
}
