package com.xinghuo.security;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinghuo.controller.PageController;
import com.xinghuo.guide.GuideContentService;
import com.xinghuo.guide.GuidePageController;
import com.xinghuo.service.*;
import com.xinghuo.wecom.*;
import org.junit.jupiter.api.*;
import org.springframework.context.annotation.*;
import org.springframework.mock.web.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

class AdminDefaultReplacementSecurityTest {
    @Configuration @EnableWebSecurity @EnableWebMvc @Import(SecurityConfig.class)
    static class TestConfig {
        @Bean AdminUserService users(){var service=mock(AdminUserService.class);when(service.sessionCurrent(any())).thenReturn(true);return service;}
        @Bean XingweiWeComBindingService bindings(){var service=mock(XingweiWeComBindingService.class);when(service.activeSsoUser(anyString(),anyString(),anyString())).thenReturn(true);return service;}
        @Bean XingweiWeComProperties properties(){return new XingweiWeComProperties("ww-test","server-only-test","1000003",true,true,false);}
        @Bean XingweiPermissionGuard xingweiPermissionGuard(){return mock(XingweiPermissionGuard.class);}
        @Bean PageController pages(){return new PageController(mock(ServiceCatalogService.class),mock(TaxonomyService.class),new ObjectMapper(),mock(SiteConfigService.class));}
        @Bean GuidePageController guides(){return new GuidePageController(mock(GuideContentService.class),new ObjectMapper());}
        @Bean Probe probe(){return new Probe();}
        @Bean InternalResourceViewResolver views(){var resolver=new InternalResourceViewResolver();resolver.setPrefix("/templates/");resolver.setSuffix(".html");return resolver;}
    }
    @RestController static class Probe {
        @GetMapping("/api/admin/quotes") String legacy(){return "legacy";}
        @PostMapping("/api/admin/quotes") String legacyWrite(){return "legacy write";}
        @GetMapping("/api/admin/xingwei/probe") @PreAuthorize("@xingweiPermissionGuard.has(authentication,'order:view')") String nativeRead(){return "native";}
        @PostMapping("/api/admin/xingwei/probe") @PreAuthorize("@xingweiPermissionGuard.has(authentication,'order:manage')") String nativeWrite(){return "native write";}
    }
    AnnotationConfigWebApplicationContext context;MockMvc mvc;XingweiPermissionGuard guard;
    @BeforeEach void setup(){context=new AnnotationConfigWebApplicationContext();context.setServletContext(new MockServletContext());context.register(TestConfig.class);context.refresh();mvc=MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();guard=context.getBean(XingweiPermissionGuard.class);}
    @AfterEach void close(){context.close();}
    @Test void adminAndOperatorDefaultToNativeButOnlyRecoveryAdminCanRequestLegacy() throws Exception {
        mvc.perform(get("/admin").with(user("operator").roles("OPERATOR"))).andExpect(status().isFound()).andExpect(redirectedUrl("/admin/xingwei")).andExpect(header().string("Cache-Control","no-store"));
        mvc.perform(get("/admin").param("legacy","1").with(user("operator").roles("OPERATOR"))).andExpect(status().isForbidden());
        mvc.perform(get("/admin").param("legacy","1").with(user("admin").roles("ADMIN"))).andExpect(status().isOk()).andExpect(view().name("admin"));
        mvc.perform(get("/admin/guides").with(user("operator").roles("OPERATOR"))).andExpect(redirectedUrl("/admin/xingwei/guides"));
        mvc.perform(get("/admin/guides").param("legacy","1").with(user("operator").roles("OPERATOR"))).andExpect(status().isForbidden());
    }
    @Test void oldApisCannotBypassNativePermissionsAndNativeWritesStillRequireCsrf() throws Exception {
        mvc.perform(get("/api/admin/quotes").with(user("operator").roles("OPERATOR"))).andExpect(status().isForbidden());
        mvc.perform(post("/api/admin/quotes").with(user("operator").roles("OPERATOR")).with(csrf())).andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/quotes").with(user("admin").roles("ADMIN"))).andExpect(status().isOk());
        mvc.perform(get("/api/admin/xingwei/probe").with(user("operator").roles("OPERATOR"))).andExpect(status().isForbidden());
        when(guard.has(any(),eq("order:view"))).thenReturn(true);
        mvc.perform(get("/api/admin/xingwei/probe").with(user("operator").roles("OPERATOR"))).andExpect(status().isOk());
        when(guard.has(any(),eq("order:manage"))).thenReturn(true);
        mvc.perform(post("/api/admin/xingwei/probe").with(user("operator").roles("OPERATOR"))).andExpect(status().isForbidden());
        mvc.perform(post("/api/admin/xingwei/probe").with(user("operator").roles("OPERATOR")).with(csrf())).andExpect(status().isOk());
        mvc.perform(get("/api/admin/xingwei/probe")).andExpect(status().isUnauthorized());
    }
    @Test void validWeComSessionCanEnterDefaultRedirectButCannotEnterLegacyWorkspace() throws Exception {
        var session=new MockHttpSession();session.setAttribute(XingweiWeComSsoController.SSO_SESSION_KEY,new XingweiWeComSsoController.SsoIdentity("operator","staff_1","OPERATOR"));
        mvc.perform(get("/admin").session(session).with(user("operator").roles("OPERATOR"))).andExpect(redirectedUrl("/admin/xingwei"));
        mvc.perform(get("/admin").param("legacy","1").session(session).with(user("operator").roles("OPERATOR"))).andExpect(status().isForbidden());
    }
    @Test void anonymousBrowserNavigationRedirectsWhileProgrammaticRequestsReturnUnauthorized()throws Exception{
        for(String path:new String[]{"/admin","/admin/xingwei"}){
            mvc.perform(get(path).accept(org.springframework.http.MediaType.TEXT_HTML)).andExpect(status().isFound()).andExpect(redirectedUrlPattern("**/admin/login"));
            mvc.perform(get(path)).andExpect(status().isUnauthorized());
        }
    }
}
