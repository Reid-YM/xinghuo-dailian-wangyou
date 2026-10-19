package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.AriaRole;
import com.xinghuo.account.model.*;
import com.xinghuo.account.repo.*;
import com.xinghuo.account.service.*;
import com.xinghuo.account.web.*;
import com.xinghuo.controller.PageController;
import com.xinghuo.service.*;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.http.HttpMethod;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.*;

/** Real password verification, session generation/parsing and browser HttpOnly-cookie round trips.
 * Repository storage, audit and business data use isolated in-memory test doubles. */
@EnabledIfEnvironmentVariable(named="PLAYWRIGHT_UI_TESTS",matches="1")
class FrontendRealSessionPlaywrightTest {
    static final String EMAIL="session-test@example.test",PASSWORD="TestingOnly2026!";
    @Test void browserSessionSurvivesRefreshAndLogoutAndRevocationRemovePrivateContent() throws Exception {
        try(Playwright p=Playwright.create();Browser a=p.chromium().launch();Browser b=p.webkit().launch()){
            for(Browser browser:List.of(a,b))for(String base:List.of("/next",""))try(Fixture f=new Fixture(browser)){
                f.page.navigate(f.origin+base+"/account/orders");f.page.locator("#n4LoginForm").waitFor();
                assertTrue(f.records.isEmpty());assertEquals(0,f.probe.reads.get());
                login(f.page,"incorrect-password");
                f.page.waitForFunction("() => document.querySelector('#n4LoginMessage')?.textContent.includes('账号或密码不正确')");
                assertTrue(f.records.isEmpty());
                login(f.page,PASSWORD);f.page.waitForSelector("#n5RecordsBody tbody tr");
                assertEquals(base+"/account/orders",URI.create(f.page.url()).getPath());
                assertTrue(f.page.locator("#n5RecordsBody").textContent().contains("会话用户 7"));
                assertEquals(1,f.records.size());assertTrue(f.page.context().cookies().stream().anyMatch(c->c.name.equals(AccountSessionService.COOKIE_NAME)&&c.httpOnly));
                assertEquals(Boolean.FALSE,f.page.evaluate("() => document.cookie.includes('XH_SID')"));
                f.page.reload();f.page.waitForSelector("#n5RecordsBody tbody tr");assertTrue(f.probe.reads.get()>0);
                f.page.getByRole(AriaRole.BUTTON,new Page.GetByRoleOptions().setName("退出登录").setExact(true)).click();f.page.waitForSelector("#n4LoginForm");
                assertTrue(f.page.context().cookies().stream().noneMatch(c->c.name.equals(AccountSessionService.COOKIE_NAME)));
                assertEquals(0,f.page.locator("#n5BusinessCenter").count());
                login(f.page,PASSWORD);f.page.waitForSelector("#n5RecordsBody tbody tr");
                f.records.keySet().forEach(f.sessions::revoke);
                f.page.reload();f.page.waitForSelector("#n4LoginForm");assertEquals(0,f.page.locator("#n5BusinessCenter").count());
                assertEquals(List.of(),f.errors);
            }
        }
    }
    static void login(Page page,String password){page.getByLabel("账号",new Page.GetByLabelOptions().setExact(true)).fill(EMAIL);page.getByLabel("密码",new Page.GetByLabelOptions().setExact(true)).fill(password);page.locator("#n4LoginButton").click();}
    @RestController static class BusinessProbe {
        final AtomicInteger reads=new AtomicInteger();
        @RequireAccount @GetMapping("/api/console/orders") Map<String,Object> orders(HttpServletRequest request){
            long id=AccountAuthInterceptor.requireAccount(request).getId();reads.incrementAndGet();
            return Map.of("items",List.of(Map.of("businessNo",FrontendCanonicalRoutesPlaywrightTest.NO,"serviceName","会话用户 "+id,"status","IN_PROGRESS")),"total",1);
        }
        @RequireAccount @GetMapping("/api/console/quotes") Map<String,Object> quotes(){return Map.of("items",List.of(),"total",0);}
        @RequireAccount @GetMapping("/api/console/summary") Map<String,Object> summary(){return Map.of("quoteTotal",0,"orderTotal",1);}
    }
    static class Fixture implements AutoCloseable {
        final Map<String,AccountSession> records=new ConcurrentHashMap<>();final AccountSessionService sessions;final BusinessProbe probe=new BusinessProbe();
        final MockMvc mvc;final com.sun.net.httpserver.HttpServer server;final String origin;final Page page;final List<String> errors=new ArrayList<>();
        Fixture(Browser browser)throws Exception{
            var accounts=mock(AccountRepository.class);var repository=mock(AccountSessionRepository.class);var encoder=new BCryptPasswordEncoder(4);
            var account=Account.builder().id(7L).contactType(Account.ContactType.EMAIL).contactValue(EMAIL).contactNorm(EMAIL).email(EMAIL).emailNorm(EMAIL).emailVerified(true).contactVerified(true)
                .publicUserId("XH-SESSION-TEST").passwordHash(encoder.encode(PASSWORD)).displayName("会话验收用户").role("BUYER").levelCode("NEW").status(Account.AccountStatus.ACTIVE).build();
            when(accounts.findById(7L)).thenReturn(Optional.of(account));when(accounts.findByContactTypeAndContactNorm(Account.ContactType.EMAIL,EMAIL)).thenReturn(Optional.of(account));
            when(repository.save(any())).thenAnswer(inv->{AccountSession row=inv.getArgument(0);records.put(row.getId(),row);return row;});
            when(repository.findById(anyString())).thenAnswer(inv->Optional.ofNullable(records.get(inv.getArgument(0))));
            when(repository.findByIdAndRevokedAtIsNull(anyString())).thenAnswer(inv->Optional.ofNullable(records.get(inv.getArgument(0))).filter(row->row.getRevokedAt()==null));
            sessions=new AccountSessionService(repository,accounts,"false","http://127.0.0.1");var audit=mock(AccountAuditService.class);
            var service=new AccountService(accounts,encoder,sessions,audit,mock(RateLimiter.class),mock(AccountLockService.class),mock(AccountEmailService.class));
            var contacts=mock(QuoteContactService.class);when(contacts.list(any())).thenReturn(List.of());
            var api=new AccountApiController(service,sessions,audit,accounts,mock(UploadService.class),contacts);var preview=new FrontendNextPreviewController();
            mvc=MockMvcBuilders.standaloneSetup(api,probe,new AccountCenterPageController(),new PageController(mock(ServiceCatalogService.class),mock(TaxonomyService.class),new ObjectMapper(),mock(SiteConfigService.class)),preview)
                .setControllerAdvice(new AccountExceptionHandler()).addInterceptors(new AccountAuthInterceptor(sessions),new FrontendMigrationConfig.Replacement(true))
                .setViewResolvers(new org.springframework.web.servlet.view.InternalResourceViewResolver("/templates/",".html")).build();
            server=com.sun.net.httpserver.HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);server.createContext("/",this::handle);server.start();origin="http://127.0.0.1:"+server.getAddress().getPort();
            page=browser.newPage(new Browser.NewPageOptions().setViewportSize(390,900));page.onPageError(errors::add);
            page.route("**/*",route->{String path=URI.create(route.request().url()).getPath();
                if(List.of("/api/account/login","/api/account/me","/api/account/logout","/api/console/orders","/api/console/quotes","/api/console/summary").contains(path))route.resume();
                else if(path.startsWith("/api/")||path.endsWith(".js")||path.endsWith(".css"))FrontendNextN30DocumentJourneyPlaywrightTest.fulfill(route,mvc,new HashSet<>());
                else if(path.matches(".*\\.(svg|png|ico)$"))route.fulfill(new Route.FulfillOptions().setStatus(404));
                else route.resume();
            });
        }
        void handle(com.sun.net.httpserver.HttpExchange exchange)throws java.io.IOException{
            try{
                URI uri=exchange.getRequestURI();var request=request(HttpMethod.valueOf(exchange.getRequestMethod()),uri).content(exchange.getRequestBody().readAllBytes());
                exchange.getRequestHeaders().forEach((key,values)->request.header(key,values.toArray()));
                String cookie=exchange.getRequestHeaders().getFirst("Cookie");if(cookie!=null)for(String part:cookie.split(";")){String[] pair=part.trim().split("=",2);if(pair.length==2)request.cookie(new Cookie(pair[0],pair[1]));}
                var result=mvc.perform(request).andReturn();var response=result.getResponse();
                if(response.getForwardedUrl()!=null&&response.getForwardedUrl().startsWith("/next")){
                    var forward=get(response.getForwardedUrl()+(uri.getRawQuery()==null?"":"?"+uri.getRawQuery())).requestAttr(RequestDispatcher.FORWARD_REQUEST_URI,uri.getPath());
                    for(String key:List.of("frontendGame","frontendOriginalMetadata")){Object value=result.getRequest().getAttribute(key);if(value!=null)forward.requestAttr(key,value);}
                    response=mvc.perform(forward).andReturn().getResponse();
                }
                for(String header:response.getHeaderNames())exchange.getResponseHeaders().put(header,new ArrayList<>(response.getHeaders(header)));
                if(response.getRedirectedUrl()!=null){exchange.sendResponseHeaders(response.getStatus(),-1);return;}
                byte[] bytes=response.getContentAsByteArray();exchange.sendResponseHeaders(response.getStatus(),bytes.length);exchange.getResponseBody().write(bytes);
            }catch(Throwable error){errors.add(error.getClass().getSimpleName());exchange.sendResponseHeaders(500,-1);}finally{exchange.close();}
        }
        public void close(){page.close();server.stop(0);}
    }
}
