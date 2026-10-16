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
    private Page setup(Browser browser,int width,AtomicBoolean failServices)throws Exception {
        Page page=browser.newPage(new Browser.NewPageOptions().setViewportSize(width,900));
        String html=new FrontendNextPreviewController().frontendNext().getBody();
        page.route("**/*",r->{
            String path=URI.create(r.request().url()).getPath();
            if(path.endsWith(".js")){try{r.fulfill(new Route.FulfillOptions().setContentType("application/javascript").setBody(Files.readString(Path.of("src/main/resources/static"+path))));}catch(Exception e){throw new RuntimeException(e);}return;}
            String body=null;int status=200;
            if(path.equals("/api/site-config"))body="{\"brandName\":\"真实配置品牌\",\"heroTitle\":\"后台维护的首页标题\",\"heroSubtitle\":\"真实服务说明\",\"quoteEnabled\":true,\"trustTags\":[\"进度可查\"],\"footerText\":\"配置页脚\",\"wecomKefuUrl\":\"https://support.example.test/help\"}";
            else if(path.equals("/api/catalog"))body="{\"games\":[{\"code\":\"G1\",\"name\":\"游戏一\"},{\"code\":\"G2\",\"name\":\"游戏二\"}],\"categories\":[{\"code\":\"TASK\",\"gameCode\":\"G1\",\"name\":\"任务\"}],\"platforms\":[{\"code\":\"PC\",\"name\":\"PC\"}],\"gamePlatforms\":[{\"gameCode\":\"G1\",\"platformCode\":\"PC\",\"active\":true}]}";
            else if(path.equals("/api/services")){status=failServices.get()?503:200;body=failServices.get()?"{}":"[{\"id\":1,\"slug\":\"actual-service\",\"gameCode\":\"G1\",\"category\":\"TASK\",\"name\":\"真实任务服务\",\"priceMode\":\"FIXED\",\"priceFixed\":88},{\"id\":2,\"slug\":\"second-service\",\"gameCode\":\"G2\",\"name\":\"另一服务\",\"priceMode\":\"QUOTE\"}]";}
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
        try(Playwright p=Playwright.create();Browser browser=p.chromium().launch()){
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
            page.locator(".nav .navlink[data-go=home]").click();assertEquals("https://example.test/",page.url());
            page.goBack();assertEquals("quote",page.locator("html").getAttribute("data-frontend-page"));assertEquals("保留的需求草稿",page.locator("#goal").inputValue());
            page.goBack();assertEquals("services",page.locator("html").getAttribute("data-frontend-page"));
            page.navigate("https://example.test/next/quote");page.waitForFunction("()=>document.documentElement.dataset.frontendPage==='quote'");
            assertTrue(page.locator("script[src^='/next/']").all().stream().allMatch(node->URI.create(node.getAttribute("src")).getRawQuery().matches("v=[0-9a-f]{64}")));
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
}
