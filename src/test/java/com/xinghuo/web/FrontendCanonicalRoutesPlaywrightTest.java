package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.AriaRole;
import com.xinghuo.account.model.Account;
import com.xinghuo.account.service.AccountSessionService;
import com.xinghuo.account.web.AccountAuthInterceptor;
import com.xinghuo.account.web.AccountCenterPageController;
import com.xinghuo.controller.PageController;
import com.xinghuo.dto.ApiDtos;
import com.xinghuo.guide.*;
import com.xinghuo.model.*;
import com.xinghuo.service.*;
import jakarta.servlet.RequestDispatcher;
import java.math.BigDecimal;
import java.net.URI;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@EnabledIfEnvironmentVariable(named="PLAYWRIGHT_UI_TESTS",matches="1")
class FrontendCanonicalRoutesPlaywrightTest {
    static final String NO="XHABCDEF1234";
    static final String CATALOG="""
        {"games":[{"code":"CODMW4","name":"测试游戏","seoSlug":"test-game","active":true}],
        "categories":[{"code":"LEVEL","gameCode":"CODMW4","name":"等级服务"}],"platforms":[],"gamePlatforms":[]}
        """;
    static final String SERVICES="""
        [{"id":1,"slug":"level","gameCode":"CODMW4","name":"等级服务","category":"LEVEL","shortDesc":"真实服务说明",
        "priceMode":"FIXED","priceFixed":25,"status":"AVAILABLE","durationText":"按进度确认"}]
        """;
    static final String GUIDE="""
        {"guide":{"id":1,"slug":"test-guide","gameCode":"CODMW4","title":"测试攻略","summary":"攻略说明"},
        "sections":[{"heading":"正文","bodyMarkdown":"实际攻略内容"}],"products":[],"faqs":[],"relatedGuides":[]}
        """;

    @Test void canonicalDocumentsRenderTheRightModuleOnRefreshInBothEnginesAndWidths() throws Exception {
        both(browser -> {
            for(int width:new int[]{320,1280}) try(Fixture f=new Fixture(browser,width,false)) {
                var pages=new LinkedHashMap<String,String>();
                pages.put("/",".page[data-page=home].active");pages.put("/services",".page[data-page=services].active");
                pages.put("/quote","#submit[data-n3-real-submit=true]");pages.put("/service/level","#n10ServiceDetail h1");
                pages.put("/game/test-game","#nextGameDetail h1");pages.put("/guides","#n23GuideSearch");
                pages.put("/guides/test-guide","#n22GuideDetail h1");pages.put("/mastery","#n24ModePanel");
                pages.put("/records","#n26RecordsCenter.active");pages.put("/record/"+NO,"#n13RecordDetail .n13-facts");
                pages.put("/contact","#n27Contact.active");pages.put("/trust","#n27Trust.active");
                pages.put("/rules","#n28Legal .n28-document");pages.put("/privacy","#n28Legal .n28-document");
                pages.put("/rules-en","#n28Legal .n28-document");pages.put("/privacy-en","#n28Legal .n28-document");
                pages.put("/account/login","#n4LoginForm");pages.put("/account/register","#n7RegisterPanel");pages.put("/account/reset","#n7ResetPanel");
                for(var entry:pages.entrySet()) {
                    f.open(entry.getKey(),entry.getValue());
                    assertEquals(entry.getKey(),URI.create(f.page.url()).getPath());
                    assertEquals(1,f.page.locator(".page.active").count(),entry.getKey());
                    assertEquals(Boolean.TRUE,f.page.evaluate("() => document.documentElement.scrollWidth <= innerWidth+1"),entry.getKey());
                    assertEquals(Boolean.FALSE,f.page.evaluate("() => window.XHFrontendShell.isPreview"));
                    f.page.waitForFunction("() => ![...document.querySelectorAll('a[href]')].some(a => a.getAttribute('href').startsWith('/next'))");
                    if(!entry.getKey().startsWith("/account")&&!entry.getKey().startsWith("/record"))
                        assertEquals(0,f.page.locator("meta[name=robots][content^=noindex]").count(),entry.getKey());
                }
                assertEquals(List.of(),f.errors,f.pageDiagnostics.events().toString());
            }
        });
    }
    @Test void nativeNavigationBackAndLoginReturnKeepCanonicalPathsAndExplicitLegacyEscape() throws Exception {
        both(browser -> { try(Fixture f=new Fixture(browser,390,false)) {
            f.open("/","#nextHomeGames a");f.page.locator("#nextHomeGames a").click();
            f.page.waitForSelector("#nextGameDetail h1");assertTrue(f.page.url().endsWith("/game/test-game"));
            assertEquals("/game/test-game",f.page.getByRole(AriaRole.LINK,new Page.GetByRoleOptions().setName("重新打开游戏资料")).getAttribute("href"));
            f.page.locator(".next-game-card").getByRole(AriaRole.LINK,new Locator.GetByRoleOptions().setName("查看详情")).click();
            f.page.waitForSelector("#n10ServiceDetail h1");assertTrue(f.page.url().endsWith("/service/level"));
            f.page.goBack();f.page.waitForSelector("#nextGameDetail h1");
            f.page.locator(".next-mobile-dock [data-go=services]").click();assertTrue(f.page.url().endsWith("/services"));
            f.page.goBack();f.page.waitForSelector("#nextGameDetail h1");
            f.open("/account/submit","#n4LoginForm");
            assertEquals("/account/login",URI.create(f.page.url()).getPath());
            assertEquals("/account/submit",f.page.evaluate("() => new URL(location.href).searchParams.get('next')"));
            assertEquals("/account/submit",f.page.evaluate("() => window.XHFrontendNextN15.requestedReturn()"));
            f.page.getByLabel("账号",new Page.GetByLabelOptions().setExact(true)).fill("test@example.test");f.page.getByLabel("密码",new Page.GetByLabelOptions().setExact(true)).fill("test-password");
            f.page.locator("#n4LoginForm button[type=submit]").click();
            f.page.waitForSelector("#n25Submit:not([disabled])");assertTrue(f.page.url().endsWith("/account/submit"));
            assertEquals(List.of(),f.errors,f.pageDiagnostics.events().toString());
        }});
    }
    @Test void authenticatedDeepLinksAndFiltersRetainCanonicalUrlsAcrossTabsAndReload() throws Exception {
        both(browser -> { try(Fixture f=new Fixture(browser,390,true)) {
            f.open("/account/orders","#n5RecordsBody tbody tr");
            f.page.locator("#n20Search").fill("测试服务");
            f.page.waitForFunction("() => new URL(location.href).searchParams.get('q') === '测试服务'");
            f.page.locator("#n5RecordsBody tbody tr").first().getByRole(AriaRole.BUTTON,new Locator.GetByRoleOptions().setName("查看详情").setExact(true)).click();
            f.page.waitForFunction("() => location.pathname.endsWith('/orders/"+NO+"')");
            f.page.reload();f.page.waitForSelector("#n5Detail:not([hidden])");assertTrue(f.page.locator("#n5Detail").textContent().contains("测试服务"));
            f.page.locator("#n17AccountNav [data-n17-key=requests]").click();f.page.waitForFunction("() => location.pathname === '/account/requests'");
            assertEquals("true",f.page.locator("#n5Tab-quotes").getAttribute("aria-selected"));
            for(String path:List.of("/account/progress","/account/favorites","/account/submissions","/account/settings","/account/orders/"+NO,"/account/requests/"+NO)) {
                f.open(path,path.contains("/orders/")||path.contains("/requests/")?"#n5Detail:not([hidden])":path.endsWith("settings")?"#n6PasswordForm":"#n5BusinessCenter");
                assertEquals(path,URI.create(f.page.url()).getPath());
                assertEquals(1,f.page.locator("meta[name=robots][content='noindex,nofollow']").count());
            }
            assertEquals(List.of(),f.errors,f.pageDiagnostics.events().toString());
        }});
    }
    @Test void passwordLoginReturnsToTheExactAccountDetailInEitherNamespace() throws Exception {
        both(browser->{for(String base:List.of("/next",""))try(Fixture f=new Fixture(browser,390,false)) {
            String target=base+"/account/orders/"+NO+"?status=IN_PROGRESS";
            String login=base.isEmpty()?"/account/login":"/next/account";
            f.open(login+"?next="+java.net.URLEncoder.encode("/account/orders/"+NO+"?status=IN_PROGRESS",java.nio.charset.StandardCharsets.UTF_8),"#n4LoginForm");
            assertEquals(target,f.page.evaluate("() => window.XHFrontendNextN15.requestedReturn()"));
            assertEquals(base+"/account",f.page.evaluate("() => window.XHFrontendNextN15.safeAccountReturn('/account?auth=reset&token=test-transient')"));
            f.page.getByLabel("账号",new Page.GetByLabelOptions().setExact(true)).fill("test@example.test");
            f.page.getByLabel("密码",new Page.GetByLabelOptions().setExact(true)).fill("test-password");
            f.page.locator("#n4LoginForm button[type=submit]").click();f.page.waitForSelector("#n5Detail:not([hidden])");
            assertEquals(base+"/account/orders/"+NO,URI.create(f.page.url()).getPath());
            assertEquals("IN_PROGRESS",f.page.evaluate("() => new URL(location.href).searchParams.get('status')"));
            assertEquals(List.of(),f.errors,f.pageDiagnostics.events().toString());
        }});
    }
    interface Check {void run(Browser browser) throws Exception;}
    void both(Check check)throws Exception{try(Playwright p=Playwright.create();Browser a=p.chromium().launch();Browser b=p.webkit().launch()){for(Browser browser:List.of(a,b))check.run(browser);}}
    static class Fixture implements AutoCloseable {
        final Page page;final MockMvc mvc;final com.sun.net.httpserver.HttpServer server;final String origin;final AtomicBoolean logged;final List<String> errors=Collections.synchronizedList(new ArrayList<>());
        final FrontendBrowserErrorTracker pageDiagnostics=new FrontendBrowserErrorTracker(errors);
        Fixture(Browser browser,int width,boolean loggedIn)throws Exception{
            logged=new AtomicBoolean(loggedIn);var mapper=new ObjectMapper();
            var config=mock(SiteConfigService.class);var site=mock(ApiDtos.SiteConfigResponse.class);
            when(site.brandName()).thenReturn("测试星火");when(site.serviceStatus()).thenReturn("open");when(config.publicConfig()).thenReturn(site);
            var taxonomy=mock(TaxonomyService.class);var game=GameCatalog.builder().code("CODMW4").name("测试游戏").seoSlug("test-game").active(true).build();
            when(taxonomy.publicGameBySeoSlug("test-game")).thenReturn(game);when(taxonomy.publicGames()).thenReturn(List.of(game));
            var services=mock(ServiceCatalogService.class);var service=ServiceItem.builder().id(1L).slug("level").gameCode("CODMW4").name("等级服务").category("LEVEL").shortDesc("真实服务说明").priceMode(ServiceItem.PriceMode.FIXED).priceFixed(BigDecimal.valueOf(25)).status(ServiceItem.Status.AVAILABLE).active(true).build();
            when(services.publicItems()).thenReturn(List.of(service));when(services.publicBySlug("level")).thenReturn(service);
            var guides=mock(GuideContentService.class);var detail=mapper.readValue(GUIDE,GuideModels.GuideDetail.class);
            when(guides.publicDetail("test-guide")).thenReturn(detail);when(guides.publicList(any(),any(),anyInt())).thenReturn(List.of(detail.guide()));
            var sessions=mock(AccountSessionService.class);when(sessions.current(any())).thenAnswer(inv->logged.get()?Optional.of(mock(Account.class)):Optional.empty());
            var preview=new FrontendNextPreviewController();preview.configuration(config);
            mvc=MockMvcBuilders.standaloneSetup(new PageController(services,taxonomy,mapper,config),new GuidePageController(guides,mapper),new AccountCenterPageController(),preview,
                new FrontendNextGameController(preview,taxonomy),new FrontendNextN23GuidesController(preview),new FrontendNextN24MasteryController(preview),new FrontendNextN25SubmissionController(preview),new FrontendNextN26RecordsController(preview),new FrontendNextN27SupportTrustController(preview),new FrontendNextN28LegalController(preview))
                .addInterceptors(new AccountAuthInterceptor(sessions),new FrontendMigrationConfig.Replacement(true))
                .setViewResolvers(new org.springframework.web.servlet.view.InternalResourceViewResolver("/templates/", ".html")).build();
            server=com.sun.net.httpserver.HttpServer.create(new java.net.InetSocketAddress("127.0.0.1",0),0);
            server.createContext("/",this::document);server.start();origin="http://127.0.0.1:"+server.getAddress().getPort();
            page=browser.newPage(new Browser.NewPageOptions().setViewportSize(width,900));pageDiagnostics.attach(page);page.route("**/*",this::fulfill);
        }
        void open(String path,String selector){page.navigate(origin+path);page.waitForSelector(selector);page.waitForFunction("() => document.documentElement.dataset.siteConfiguration === 'ready'");}
        void fulfill(Route route){try{
            URI uri=URI.create(route.request().url());String path=uri.getPath();
            if(path.equals("/api/account/login")){logged.set(true);json(route,200,"{\"id\":1,\"displayName\":\"测试用户\"}");}
            else if(path.equals("/api/account/me"))json(route,logged.get()?200:401,"{\"id\":1,\"email\":\"test@example.test\",\"displayName\":\"测试用户\",\"emailVerified\":true}");
            else if(path.equals("/api/site-config"))json(route,200,"{\"brandName\":\"测试星火\",\"quoteEnabled\":true,\"serviceStatus\":\"open\"}");
            else if(path.equals("/api/catalog"))json(route,200,CATALOG);
            else if(path.equals("/api/services"))json(route,200,SERVICES);
            else if(path.equals("/api/guides/test-guide"))json(route,200,GUIDE);
            else if(path.equals("/api/guides"))json(route,200,"[{\"slug\":\"test-guide\",\"title\":\"测试攻略\",\"gameCode\":\"CODMW4\"}]");
            else if(path.equals("/api/console/orders"))json(route,200,"{\"items\":[{\"businessNo\":\""+NO+"\",\"gameName\":\"测试游戏\",\"serviceName\":\"测试服务\",\"status\":\"IN_PROGRESS\"}],\"total\":1}");
            else if(path.equals("/api/console/quotes"))json(route,200,"{\"items\":[{\"businessNo\":\""+NO+"\",\"game\":\"测试游戏\",\"demand\":\"测试需求\"}],\"total\":1}");
            else if(path.equals("/api/console/orders/"+NO))json(route,200,"{\"order\":{\"businessNo\":\""+NO+"\",\"serviceName\":\"测试服务\"},\"timeline\":[],\"afterSalesCases\":[]}");
            else if(path.equals("/api/console/quotes/"+NO))json(route,200,"{\"businessNo\":\""+NO+"\",\"game\":\"测试游戏\",\"demand\":\"测试需求\"}");
            else if(path.equals("/api/records/"+NO))json(route,200,"{\"businessNo\":\""+NO+"\",\"stage\":\"QUOTE\",\"status\":\"NEW\",\"title\":\"真实询价\",\"progressText\":\"等待客服确认\"}");
            else if(path.startsWith("/api/")||path.endsWith(".js")||path.endsWith(".css"))FrontendNextN30DocumentJourneyPlaywrightTest.fulfill(route,mvc,new HashSet<>());
            else if(path.matches(".*\\.(svg|png|jpg|jpeg|ico|webp|woff2?)$")) {
                Path asset=Path.of("src/main/resources/static"+path);
                if(Files.isRegularFile(asset))route.fulfill(new Route.FulfillOptions().setContentType(path.endsWith(".svg")?"image/svg+xml":"application/octet-stream").setPath(asset));
                else route.fulfill(new Route.FulfillOptions().setStatus(404));
            } else route.resume();
        }catch(Exception e){throw new RuntimeException(e);}}
        void document(com.sun.net.httpserver.HttpExchange exchange) throws java.io.IOException {
            try {
                URI uri=exchange.getRequestURI();String path=uri.getPath();
                var initial=mvc.perform(get(uri)).andReturn();var response=initial.getResponse();
                if(response.getRedirectedUrl()!=null){exchange.getResponseHeaders().set("Location",response.getRedirectedUrl());exchange.sendResponseHeaders(response.getStatus(),-1);return;}
                String html;
                if(uri.getQuery()!=null&&uri.getQuery().contains("legacy=1")) {
                    assertFalse(String.valueOf(response.getForwardedUrl()).startsWith("/next"));
                    html=Files.readString(Path.of("src/main/resources/templates",initial.getModelAndView().getViewName()+".html"));
                } else if(path.equals("/next") || path.startsWith("/next/")) {
                    assertEquals(200,response.getStatus(),path);
                    html=response.getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
                } else {
                    assertNotNull(response.getForwardedUrl(),path);
                    String target=response.getForwardedUrl();
                    var forward=get(target+(uri.getRawQuery()==null?"":"?"+uri.getRawQuery())).requestAttr(RequestDispatcher.FORWARD_REQUEST_URI,path);
                    for(String key:List.of("frontendGame","frontendOriginalMetadata")){Object value=initial.getRequest().getAttribute(key);if(value!=null)forward.requestAttr(key,value);}
                    var next=mvc.perform(forward).andReturn().getResponse();assertEquals(200,next.getStatus(),path);
                    assertEquals("no-store",next.getHeader("Cache-Control"));
                    html=next.getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
                    if(next.getHeader("X-Robots-Tag")!=null)exchange.getResponseHeaders().set("X-Robots-Tag",next.getHeader("X-Robots-Tag"));
                }
                byte[] bytes=html.getBytes(java.nio.charset.StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type","text/html;charset=utf-8");exchange.getResponseHeaders().set("Cache-Control","no-store");
                exchange.sendResponseHeaders(200,bytes.length);exchange.getResponseBody().write(bytes);
            } catch(Throwable error) { errors.add(error.toString());exchange.sendResponseHeaders(500,-1); }
            finally {exchange.close();}
        }
        void json(Route r,int status,String body){r.fulfill(new Route.FulfillOptions().setStatus(status).setContentType("application/json").setBody(body));}
        public void close(){page.close();server.stop(0);}
    }
}
