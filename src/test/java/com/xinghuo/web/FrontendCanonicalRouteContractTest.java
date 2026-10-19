package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.xinghuo.account.service.AccountSessionService;
import com.xinghuo.account.web.AccountAuthInterceptor;
import com.xinghuo.account.web.AccountCenterPageController;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.*;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.ModelAndView;

class FrontendCanonicalRouteContractTest {
    @Test void completedPublicAndAccountRoutesForwardOnlyAfterTheOriginalHandlerSucceeds() {
        var replacement=new FrontendMigrationConfig.Replacement(true);
        for(String path:List.of("/services","/service/level","/game/test-game","/guides","/guides/test-guide","/mastery","/records","/record/XHABCDEF1234","/contact","/trust","/rules","/privacy","/rules-en","/privacy-en","/account/submit","/account/favorites","/account/submissions","/account/orders/XHABCDEF1234","/account/requests/XHABCDEF1234")) {
            var request=new MockHttpServletRequest("GET",path);var response=new MockHttpServletResponse();var view=new ModelAndView("original");
            view.addObject("pageTitle","Original SEO");replacement.postHandle(request,response,new Object(),view);
            assertEquals("forward:/next"+path,view.getViewName(),path);
            assertEquals("no-store",response.getHeader("Cache-Control"));
            assertEquals("Original SEO",((Map<?,?>)request.getAttribute("frontendOriginalMetadata")).get("pageTitle"));
        }
        for(String path:List.of("/admin","/api/account/me","/guest/access/test-capability-token","/api/wechat-pay/notify","/account/wechat/callback","/unknown")) {
            var view=new ModelAndView("original");replacement.postHandle(new MockHttpServletRequest("GET",path),new MockHttpServletResponse(),new Object(),view);
            assertEquals("original",view.getViewName(),path);
        }
        for(String name:List.of("redirect:/account/login","forward:/existing")) {
            var view=new ModelAndView(name);replacement.postHandle(new MockHttpServletRequest("GET","/services"),new MockHttpServletResponse(),new Object(),view);assertEquals(name,view.getViewName());
        }
    }
    @Test void rollbackAndWritesKeepTheOriginalHandlerAndMetadata() {
        for(String method:List.of("POST","PUT","PATCH","DELETE")) {
            var view=new ModelAndView("original");new FrontendMigrationConfig.Replacement(true).postHandle(new MockHttpServletRequest(method,"/services"),new MockHttpServletResponse(),new Object(),view);assertEquals("original",view.getViewName());
        }
        var request=new MockHttpServletRequest("GET","/guides");request.setParameter("legacy","1");
        var view=new ModelAndView("guides");new FrontendMigrationConfig.Replacement(true).postHandle(request,new MockHttpServletResponse(),new Object(),view);assertEquals("guides",view.getViewName());
        new FrontendMigrationConfig.Replacement(false).postHandle(new MockHttpServletRequest("GET","/guides"),new MockHttpServletResponse(),new Object(),view);assertEquals("guides",view.getViewName());
    }
    @Test void newCanonicalAccountDeepLinksAndAssetsRequireTheOriginalAccountSession() throws Exception {
        var sessions=mock(AccountSessionService.class);when(sessions.current(any())).thenReturn(Optional.empty());
        var mvc=MockMvcBuilders.standaloneSetup(new AccountCenterPageController()).addInterceptors(new AccountAuthInterceptor(sessions),new FrontendMigrationConfig.Replacement(true)).build();
        for(String path:List.of("/account/orders/XHABCDEF1234","/account/requests/XHABCDEF1234","/account/favorites","/account/submissions"))
            mvc.perform(get(path)).andExpect(status().isFound()).andExpect(redirectedUrl("/account/login?next="+java.net.URLEncoder.encode(path,java.nio.charset.StandardCharsets.UTF_8)));
    }
    @Test void originalSeoAndServerStructuredDataSurviveCanonicalForwardingWithoutLeakingOtherModelFields() throws Exception {
        var request=new MockHttpServletRequest("GET","/next/service/level");request.setAttribute(jakarta.servlet.RequestDispatcher.FORWARD_REQUEST_URI,"/service/level");
        request.setAttribute("frontendOriginalMetadata",Map.of("serviceTitle","Original <title>","serviceDescription","Description <text>","canonicalUrl","https://wydlgf.cn/service/level","productJsonLd","{\"@type\":\"Product\",\"name\":\"</script><script>bad</script>\",\"offers\":{\"price\":25}}","notPublic","private-model-data"));
        String html=new FrontendNextPreviewController().frontendNext(request).getBody();
        assertTrue(html.contains("<title>Original &lt;title&gt;</title>"));assertFalse(html.contains("private-model-data"));assertFalse(html.contains("<script>bad</script>"));
        var match=java.util.regex.Pattern.compile("<script type=\"application/ld\\+json\">(.*?)</script>").matcher(html);assertTrue(match.find());
        var json=new com.fasterxml.jackson.databind.ObjectMapper().readTree(match.group(1));assertEquals(25,json.get("offers").get("price").asInt());assertEquals("</script><script>bad</script>",json.get("name").asText());
        assertFalse(html.contains("name=\"robots\" content=\"noindex"));
    }
    @Test void oneTimeGuestConfirmationRemainsStandaloneAndGetNeverExchangesTheCapability() throws Exception {
        var guests=mock(com.xinghuo.service.GuestQuoteFlowService.class);
        var mvc=MockMvcBuilders.standaloneSetup(new com.xinghuo.controller.GuestAccessController(guests))
                .addInterceptors(new FrontendMigrationConfig.Replacement(true)).build();
        mvc.perform(get("/guest/access/"+"test".repeat(10))).andExpect(status().isOk()).andExpect(view().name("guest-access"));
        verifyNoInteractions(guests);
    }

    @Test void previewExplicitRollbackReturnsToOriginalDocumentAndPreservesSafeReturnContext() throws Exception {
        var preview=new FrontendNextPreviewController();
        for(String path:List.of("/next/services","/next/guides/test-guide","/next/record/XHABCDEF1234","/next/account/orders/XHABCDEF1234")){
            var request=new MockHttpServletRequest("GET",path);request.setParameter("legacy","1");request.setQueryString("legacy=1&from=account-orders");
            var response=preview.frontendNext(request);
            assertEquals(302,response.getStatusCode().value());assertEquals(path.substring(5)+"?legacy=1&from=account-orders",response.getHeaders().getLocation().toString());
            assertEquals("no-store",response.getHeaders().getCacheControl());
        }
        var auth=new MockHttpServletRequest("GET","/next/account");auth.setParameter("legacy","1");auth.setParameter("auth","reset");auth.setQueryString("legacy=1&auth=reset&token=test-mail-token");
        assertEquals("/account/reset?legacy=1&auth=reset&token=test-mail-token",preview.frontendNext(auth).getHeaders().getLocation().toString());
    }

}
