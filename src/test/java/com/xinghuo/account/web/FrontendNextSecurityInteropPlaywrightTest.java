package com.xinghuo.account.web;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.microsoft.playwright.*;
import com.xinghuo.account.service.AccountException;
import com.xinghuo.account.service.AccountSessionService;
import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.*;

/** Browser requests run through the production interceptor, rather than a permissive API fixture. */
@EnabledIfEnvironmentVariable(named="PLAYWRIGHT_UI_TESTS",matches="1")
class FrontendNextSecurityInteropPlaywrightTest {
    @Test void chromiumLoginAndAuthWritesMatchActualServerCsrfPolicy() throws Exception { verify("chromium"); }
    @Test void webkitLoginAndAuthWritesMatchActualServerCsrfPolicy() throws Exception { verify("webkit"); }
    private void verify(String browserName) throws Exception {
        var sessions=mock(AccountSessionService.class);when(sessions.current(any())).thenReturn(Optional.empty());
        MockMvc mvc=MockMvcBuilders.standaloneSetup(new Probe()).addInterceptors(new AccountAuthInterceptor(sessions)).setControllerAdvice(new AccountExceptionHandler()).build();
        try(Playwright p=Playwright.create();Browser browser=(browserName.equals("chromium")?p.chromium():p.webkit()).launch()) {
            Page page=browser.newPage();
            page.route("**/*",route->{
                String path=URI.create(route.request().url()).getPath();
                if(!path.startsWith("/api/")) {route.fulfill(new Route.FulfillOptions().setContentType("text/html;charset=utf-8").setBody("<html><head></head><body><section class='page active' data-page='account'></section><div class='badge'></div></body></html>"));return;}
                try {
                    var req=MockMvcRequestBuilders.request(HttpMethod.valueOf(route.request().method()),path);
                    route.request().headers().forEach((k,v)->req.header(k,v));
                    if(route.request().postData()!=null)req.content(route.request().postData());
                    var response=mvc.perform(req).andReturn().getResponse();
                    route.fulfill(new Route.FulfillOptions().setStatus(response.getStatus()).setContentType("application/json;charset=utf-8").setBody(response.getContentAsString(java.nio.charset.StandardCharsets.UTF_8)));
                } catch(Exception e) {throw new RuntimeException(e);}
            });
            page.navigate("https://example.test/next/account");
            assertEquals(403,((Number)page.evaluate("async()=> (await fetch('/api/account/login',{method:'POST',headers:{'X-Requested-With':'XMLHttpRequest'}})).status")).intValue());
            page.addScriptTag(new Page.AddScriptTagOptions().setPath(Path.of("src/main/resources/static/next/n4.js")));
            page.waitForSelector("#n4LoginForm");
            page.locator("#n4ContactValue").fill("test@example.test");page.locator("#n4Password").fill("TestingOnly2026!");page.locator("#n4LoginButton").click();
            page.waitForSelector(".n4-shell");assertTrue(page.locator(".n4-shell").textContent().contains("策略测试用户"));
            page.addScriptTag(new Page.AddScriptTagOptions().setPath(Path.of("src/main/resources/static/next/n7-auth.js")));
            assertEquals(true,page.evaluate("async()=>{for(const path of ['/api/account/register','/api/account/password/reset/request','/api/account/password/reset']){const r=await window.XHFrontendNextN7Auth.request(path,{method:'POST',body:{}});if(!r.ok)return false;}return true;}"));
            assertEquals(401,((Number)page.evaluate("async()=> (await fetch('/api/account/private-probe')).status")).intValue());
        }
    }
    @RestController @RequestMapping("/api/account")
    public static class Probe {
        private boolean logged;
        @PostMapping("/**") public Map<String,Object> write(){logged=true;return Map.of("ok",true,"displayName","策略测试用户");}
        @GetMapping("/me") public Map<String,Object> me(){if(!logged)throw AccountException.unauthenticated();return Map.of("displayName","策略测试用户");}
        @GetMapping("/orders") public Map<String,Object> orders(){return Map.of("items",List.of(),"total",0);}
        @GetMapping({"/sessions","/security/events"}) public List<Object> list(){return List.of();}
        @GetMapping("/wechat/availability") public Map<String,Object> availability(){return Map.of("enabled",false);}
        @RequireAccount @GetMapping("/private-probe") public Map<String,Object> protectedRead(){return Map.of("ok",true);}
    }
}
