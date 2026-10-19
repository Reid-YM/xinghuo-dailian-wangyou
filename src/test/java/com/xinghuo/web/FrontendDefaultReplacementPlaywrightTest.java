package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.*;
import com.microsoft.playwright.*;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

@EnabledIfEnvironmentVariable(named="PLAYWRIGHT_UI_TESTS",matches="1")
class FrontendDefaultReplacementPlaywrightTest {
    private Browser chromium(Playwright p){var options=new BrowserType.LaunchOptions();String executable=System.getenv("XINGHUO_TEST_CHROMIUM");if(executable!=null&&!executable.isBlank())options.setExecutablePath(Path.of(executable));return p.chromium().launch(options);}
    private Page setup(Browser browser,int width,AtomicBoolean failServices)throws Exception {
        Page page=browser.newPage(new Browser.NewPageOptions().setViewportSize(width,900));
        String html=new FrontendNextPreviewController().frontendNext().getBody();
        var publicMapping=new com.xinghuo.service.ServiceCatalogService(null,null,null);
        var first=com.xinghuo.model.ServiceItem.builder().id(1L).slug("actual-service").gameCode("G1").category("TASK").name("真实任务服务").priceMode(com.xinghuo.model.ServiceItem.PriceMode.FIXED).priceFixed(new java.math.BigDecimal("88")).status(com.xinghuo.model.ServiceItem.Status.AVAILABLE).build();
        var second=com.xinghuo.model.ServiceItem.builder().id(2L).slug("second-service").gameCode("G2").category("TASK").name("另一服务").priceMode(com.xinghuo.model.ServiceItem.PriceMode.QUOTE).status(com.xinghuo.model.ServiceItem.Status.AVAILABLE).build();
        String realPublicServices=new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(java.util.List.of(publicMapping.pub(first),publicMapping.pub(second)));
        page.route("**/*",r->{
            String path=URI.create(r.request().url()).getPath();
            if(path.endsWith(".css")){r.fulfill(new Route.FulfillOptions().setContentType("text/css").setPath(Path.of("src/main/resources/static"+path)));return;}
            if(path.endsWith(".png")){r.fulfill(new Route.FulfillOptions().setContentType("image/svg+xml").setBody("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"20\" height=\"20\"><rect width=\"20\" height=\"20\" fill=\"gold\"/></svg>"));return;}
            if(path.endsWith(".js")){try{r.fulfill(new Route.FulfillOptions().setContentType("application/javascript").setBody(Files.readString(Path.of("src/main/resources/static"+path))));}catch(Exception e){throw new RuntimeException(e);}return;}
            String body=null;int status=200;
            if(path.equals("/api/site-config"))body="{\"brandName\":\"真实配置品牌\",\"heroTitle\":\"后台维护的首页标题\",\"heroSubtitle\":\"真实服务说明\",\"quoteEnabled\":true,\"trustTags\":[\"进度可查\"],\"footerText\":\"配置页脚\",\"wecomKefuUrl\":\"https://support.example.test/help\"}";
            else if(path.equals("/api/public/cases"))body="[]";
            else if(path.equals("/api/catalog"))body="{\"games\":[{\"code\":\"G1\",\"name\":\"游戏一\",\"seoSlug\":\"actual-game\"},{\"code\":\"G2\",\"name\":\"游戏二\"}],\"categories\":[{\"code\":\"TASK\",\"gameCode\":\"G1\",\"name\":\"任务\"}],\"platforms\":[{\"code\":\"PC\",\"name\":\"PC\"}],\"gamePlatforms\":[{\"gameCode\":\"G1\",\"platformCode\":\"PC\",\"active\":true}]}";
            else if(path.equals("/api/services")){status=failServices.get()?503:200;body=failServices.get()?"{}":realPublicServices;}
            else if(path.equals("/api/account/me")){status=401;body="{\"message\":\"请先登录\"}";}
            else if(path.endsWith("/availability"))body="{\"enabled\":false}";
            else if(path.equals("/api/public/site-settings"))body="{\"navServicesLabel\":\"真实服务\",\"navMasteryVisible\":false}";
            else if(path.equals("/api/public/frontend-controls"))body="{\"homeHeroVisible\":true,\"themeAccentColor\":\"#22aa66\",\"themeRadius\":12}";
            else if(path.startsWith("/api/")){status=404;body="{}";}
            if(body!=null){r.fulfill(new Route.FulfillOptions().setStatus(status).setContentType("application/json;charset=utf-8").setBody(body));return;}
            r.fulfill(new Route.FulfillOptions().setContentType("text/html;charset=utf-8").setHeaders(Map.of("Content-Security-Policy","default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data: https:; connect-src 'self'")).setBody(html));
        });
        return page;
    }
    @Test void chromiumDirectPathsRefreshHistoryConfigurationAndServiceLinksWorkUnderCsp() throws Exception {
        try(Playwright p=Playwright.create();Browser browser=chromium(p)){
            Page page=setup(browser,1440,new AtomicBoolean(false));
            page.navigate("https://example.test/services?game=G1");
            page.waitForFunction("()=>document.documentElement.dataset.siteConfiguration==='ready' && document.querySelectorAll('.serviceCard').length===2");
            assertEquals("services",page.locator("html").getAttribute("data-frontend-page"));
            assertTrue(page.locator(".serviceCard").first().textContent().contains("¥88"));
            assertEquals("/service/actual-service",page.locator(".serviceCard").first().locator("a").getAttribute("href"));
            assertTrue(page.locator(".serviceCard").nth(1).isHidden());
            assertEquals("https://support.example.test/help",page.locator("[data-support-link]").getAttribute("href"));
            page.reload();page.waitForFunction("()=>document.documentElement.dataset.frontendPage==='services'");
            page.locator(".nav .navlink[data-go=quote]").click();page.locator("#goal").fill("保留的需求草稿");
            page.locator(".nav .navlink[data-go=home]").click();assertEquals("https://example.test/",page.url());page.waitForFunction("()=>document.querySelectorAll('#nextHomeGames a').length===2");assertEquals("/game/actual-game",page.locator("#nextHomeGames a").first().getAttribute("href"));assertEquals("/services?game=G2",page.locator("#nextHomeGames a").nth(1).getAttribute("href"));assertEquals("星火 · 新版服务平台",page.locator(".badge").textContent());assertFalse(page.locator(".badge").getAttribute("title").contains("并行"));
            page.goBack();assertEquals("quote",page.locator("html").getAttribute("data-frontend-page"));assertEquals("保留的需求草稿",page.locator("#goal").inputValue());
            page.goBack();assertEquals("services",page.locator("html").getAttribute("data-frontend-page"));
            page.navigate("https://example.test/next/quote");page.waitForFunction("()=>document.documentElement.dataset.frontendPage==='quote'");
            assertTrue(page.locator("script[src^='/next/']").all().stream().allMatch(node->URI.create(node.getAttribute("src")).getRawQuery().matches("v=[0-9a-f]{64}")));
        }
    }
    @Test void chromiumAttributionAndActualQuoteTrackingSurviveCanonicalNavigationAndReload()throws Exception{
        try(Playwright p=Playwright.create();Browser browser=chromium(p)){
            Page page=setup(browser,390,new AtomicBoolean(false));var events=new java.util.ArrayList<String>();var quoteBodies=new java.util.ArrayList<String>();
            page.onRequest(request->{String path=URI.create(request.url()).getPath();if(path.equals("/api/track"))events.add(request.postData());if(path.equals("/api/quote"))quoteBodies.add(request.postData());});
            page.route("**/api/track",r->r.fulfill(new Route.FulfillOptions().setStatus(204)));
            page.route("**/api/quote",r->r.fulfill(new Route.FulfillOptions().setContentType("application/json").setBody("{\"businessNo\":\"XHABCDEF1234\"}")));
            page.navigate("https://example.test/?utm_source=taobao&utm_medium=shop&utm_campaign=autumn");page.waitForFunction("()=>document.documentElement.dataset.siteConfiguration==='ready'");
            page.locator("#homeHero [data-primary-cta]").click();page.locator("#goal").fill("先记录需求");assertEquals("taobao",page.evaluate("()=>window.XHFrontendShell.attribution.utmSource"));page.reload();page.waitForFunction("()=>!document.querySelector('#qGame').disabled");
            page.locator("#goal").fill("当前进度到目标");page.locator("#n3ContactMethod").selectOption("微信");page.locator("#n3CustomerContact").fill("private_wechat_123");page.locator("#n3PrivacyConsent").check();page.locator("#submit").click();page.locator("#n3QuoteMessage").filter(new Locator.FilterOptions().setHasText("询价提交成功，业务编号：XHABCDEF1234")).waitFor();
            var json=new com.fasterxml.jackson.databind.ObjectMapper();assertEquals(1,quoteBodies.size());var quote=json.readTree(quoteBodies.getFirst());assertEquals("taobao",quote.path("utmSource").asText());assertEquals("autumn",quote.path("utmCampaign").asText());assertEquals("taobao",quote.path("firstTouchSource").asText());
            assertEquals(1,events.stream().filter(body->body.contains("quote_submit")).count());assertTrue(events.stream().anyMatch(body->body.contains("quote_started")));assertTrue(events.stream().anyMatch(body->body.contains("hero_quote_click")));assertTrue(events.stream().noneMatch(body->body.contains("private_wechat_123")||body.contains("当前进度")));
        }
    }
    @Test void webkitMobileUsesPathAndAuthModeAndShowsRecoverableErrorsWithoutDemoRecords() throws Exception {
        try(Playwright p=Playwright.create();Browser browser=p.webkit().launch()){
            AtomicBoolean fail=new AtomicBoolean(true);Page page=setup(browser,390,fail);
            page.navigate("https://example.test/services");page.waitForSelector("#n2DataStatus button");
            assertEquals(0,page.locator(".serviceCard").count());assertTrue(page.locator(".serviceGrid").textContent().contains("无法读取"));
            fail.set(false);page.locator("#n2DataStatus button").click();page.waitForSelector(".serviceCard");
            assertEquals(2,page.locator(".serviceCard").count());
            assertEquals(true,page.evaluate("()=>document.documentElement.scrollWidth<=innerWidth"));
            page.navigate("https://example.test/account/register");page.waitForSelector("#n7RegisterPanel");
            assertEquals("account",page.locator("html").getAttribute("data-frontend-page"));
            assertTrue(page.locator("#n7RegisterPanel").isVisible());
            assertEquals(0,page.getByText("星火玩家007").count());
        }
    }
    @Test void webkitFreshConfigurationControlsRecommendationsImagesAndFaqWithoutPlaceholderCards() throws Exception {
        try(Playwright p=Playwright.create();Browser browser=p.webkit().launch()){
            Page page=setup(browser,390,new AtomicBoolean(false));
            String configuration="""
                {"brandName":"配置品牌","heroTitle":"真实首页","quoteEnabled":true,"serviceStatus":"reservation","statusShortText":"本周预约中",
                "heroImage":"/hero.png","heroImageHeight":180,"heroImageFit":"contain","heroImagePositionX":0,"heroImagePositionY":0,
                "heroBackgroundImage":"/background.png","heroBackgroundPositionX":0,"heroBackgroundPositionY":100,"heroBackgroundOpacity":0,"heroBackgroundBlur":3,
                "serviceCoverHeight":180,"serviceCoverFit":"contain","serviceCoverPositionX":0,"serviceCoverPositionY":100,
                "showStartingPrice":true,"startingPrice":0,"startingPriceMode":"manual","homeServiceLimit":2,
                "hotServices":[{"slug":"actual-service","name":"后台推荐服务","priceMode":"fixed","priceFixed":88,"coverImage":"/cover.png"}],
                "trustCases":[{"tag":"说明","title":"真实服务说明","metric":"阶段确认","detail":"<script>不会执行</script>"}],
                "trustFaq":[{"question":"如何确认金额？","answer":"客服核对后确认"}]}
                """;
            page.route("**/api/site-config",r->r.fulfill(new Route.FulfillOptions().setContentType("application/json").setBody(configuration)));
            page.navigate("https://example.test/");page.waitForFunction("()=>document.documentElement.dataset.siteConfiguration==='ready'");
            assertEquals("后台推荐服务",page.locator("#nextHotServices h3").textContent());
            assertEquals("/service/actual-service",page.locator("#nextHotServices a").getAttribute("href"));
            assertEquals("¥88",page.locator("#nextHotServices .price").textContent());
            assertEquals("0% 0%",page.locator("[data-config=heroImage]").evaluate("e=>getComputedStyle(e).objectPosition"));
            assertEquals("0% 100%",page.locator("#heroBackground").evaluate("e=>getComputedStyle(e).backgroundPosition"));
            assertEquals("0",page.locator("#heroBackground").evaluate("e=>getComputedStyle(e).opacity"));
            assertEquals("180px",page.locator("#nextHotServices .service-cover").evaluate("e=>getComputedStyle(e).height"));
            assertEquals("contain",page.locator("#nextHotServices img").evaluate("e=>getComputedStyle(e).objectFit"));
            assertTrue(page.locator("#nextStartingPrice").textContent().contains("¥0"));assertTrue(page.locator("#nextServiceStatus").textContent().contains("预约中"));
            page.waitForFunction("()=>document.querySelectorAll('#nextHomeServices a').length===2");
            assertEquals(0,page.locator("#nextTrustCaseGrid script").count());assertTrue(page.locator("#nextTrustCaseGrid").textContent().contains("<script>不会执行</script>"));
            page.locator("#nextFaq summary").click();assertTrue(page.getByText("客服核对后确认",new Page.GetByTextOptions().setExact(true)).isVisible());
            page.unroute("**/api/site-config");
            page.route("**/api/site-config",r->r.fulfill(new Route.FulfillOptions().setContentType("application/json").setBody(configuration.replace("后台推荐服务","更新后的推荐服务"))));
            page.reload();page.waitForFunction("()=>document.querySelector('#nextHotServices h3')?.textContent==='更新后的推荐服务'");
            assertEquals(true,page.evaluate("()=>document.documentElement.scrollWidth<=innerWidth"));
        }
    }

}
