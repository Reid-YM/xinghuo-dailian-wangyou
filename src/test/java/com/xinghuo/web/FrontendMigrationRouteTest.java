package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.xinghuo.account.service.AccountSessionService;
import com.xinghuo.account.web.AccountAuthInterceptor;
import com.xinghuo.account.web.RequireAccount;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;

class FrontendMigrationRouteTest {
    @Test void defaultRoutesIgnoreRetiredQueryWhileOperatorRollbackRemainsAvailable() throws Exception {
        var mvc=MockMvcBuilders.standaloneSetup(new Pages()).addInterceptors(new FrontendMigrationConfig.Replacement(true)).build();
        mvc.perform(get("/")).andExpect(forwardedUrl("/next")).andExpect(header().string("Cache-Control","no-store"));
        mvc.perform(get("/services")).andExpect(forwardedUrl("/next/services"));
        mvc.perform(get("/services").param("legacy","1")).andExpect(forwardedUrl("/next/services"));
        mvc.perform(get("/account/register")).andExpect(forwardedUrl("/next/account"));
        mvc.perform(get("/redirect")).andExpect(redirectedUrl("/account/login"));
        var rollback=MockMvcBuilders.standaloneSetup(new Pages()).addInterceptors(new FrontendMigrationConfig.Replacement(false)).build();
        rollback.perform(get("/")).andExpect(view().name("old-home"));
    }
    @Test void bareContextKeepsSafeFallbackAndAdminRoutes() {
        new org.springframework.boot.test.context.runner.ApplicationContextRunner()
            .withUserConfiguration(FrontendMigrationConfig.class).run(context->{
                var registry=mock(org.springframework.web.servlet.config.annotation.InterceptorRegistry.class);
                var interceptor=org.mockito.ArgumentCaptor.forClass(org.springframework.web.servlet.HandlerInterceptor.class);
                context.getBean(FrontendMigrationConfig.class).addInterceptors(registry);
                verify(registry).addInterceptor(interceptor.capture());
                var mvc=MockMvcBuilders.standaloneSetup(new Pages()).addInterceptors(interceptor.getValue()).build();
                mvc.perform(get("/")).andExpect(view().name("old-home")).andExpect(header().doesNotExist("X-Xinghuo-Frontend"));
                mvc.perform(get("/services")).andExpect(view().name("old-services"));
                mvc.perform(get("/account/register")).andExpect(view().name("old-register"));
                mvc.perform(get("/admin/xingwei")).andExpect(view().name("new-admin"));
                mvc.perform(get("/next/services")).andExpect(view().name("next-preview"));
            });
    }
    @Test void actualApplicationConfigurationEnablesNextAndEnvironmentRollbackRestoresLegacy() {
        var runner=new org.springframework.boot.test.context.runner.ApplicationContextRunner()
            .withInitializer(new org.springframework.boot.test.context.ConfigDataApplicationContextInitializer())
            .withUserConfiguration(FrontendMigrationConfig.class);
        for(boolean rollback:new boolean[]{false,true}) {
            var configured=rollback ? runner.withPropertyValues("FRONTEND_NEXT_DEFAULT_ENABLED=false") : runner;
            configured.run(context->{
                assertEquals(!rollback,context.getEnvironment().getProperty("app.frontend-next.default-enabled",Boolean.class));
                var registry=mock(org.springframework.web.servlet.config.annotation.InterceptorRegistry.class);
                var interceptor=org.mockito.ArgumentCaptor.forClass(org.springframework.web.servlet.HandlerInterceptor.class);
                context.getBean(FrontendMigrationConfig.class).addInterceptors(registry);
                verify(registry).addInterceptor(interceptor.capture());
                var mvc=MockMvcBuilders.standaloneSetup(new Pages()).addInterceptors(interceptor.getValue()).build();
                mvc.perform(get("/")).andExpect(view().name(rollback ? "old-home" : "forward:/next"));
                mvc.perform(get("/services")).andExpect(view().name(rollback ? "old-services" : "forward:/next/services"));
                mvc.perform(get("/services").param("legacy","1")).andExpect(view().name(rollback ? "old-services" : "forward:/next/services"));
                mvc.perform(get("/admin/xingwei")).andExpect(view().name("new-admin"));
            });
        }
    }
    @Test void accountAuthenticationCannotBeBypassedByTheReplacement() throws Exception {
        var sessions=mock(AccountSessionService.class);when(sessions.current(any())).thenReturn(Optional.empty());
        var mvc=MockMvcBuilders.standaloneSetup(new Pages()).addInterceptors(new AccountAuthInterceptor(sessions),new FrontendMigrationConfig.Replacement(true)).build();
        mvc.perform(get("/account")).andExpect(status().isFound()).andExpect(redirectedUrl("/account/login?next=%2Faccount"));
    }
    @Controller public static class Pages {
        @GetMapping("/") String home(){return "old-home";}
        @GetMapping("/services") String services(){return "old-services";}
        @GetMapping("/account/register") String register(){return "old-register";}
        @RequireAccount @GetMapping("/account") String account(){return "old-account";}
        @GetMapping("/admin/xingwei") String admin(){return "new-admin";}
        @GetMapping("/next/services") String next(){return "next-preview";}
        @GetMapping("/redirect") String redirect(){return "redirect:/account/login";}
    }
}
