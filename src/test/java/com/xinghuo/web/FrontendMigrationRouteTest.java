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
    @Test void defaultRoutesActuallyForwardAndExplicitRollbackAndSwitchOffStillRenderLegacy() throws Exception {
        var mvc=MockMvcBuilders.standaloneSetup(new Pages()).addInterceptors(new FrontendMigrationConfig.Replacement(true)).build();
        mvc.perform(get("/")).andExpect(forwardedUrl("/next")).andExpect(header().string("Cache-Control","no-store"));
        mvc.perform(get("/services")).andExpect(forwardedUrl("/next/services"));
        mvc.perform(get("/services").param("legacy","1")).andExpect(view().name("old-services"));
        mvc.perform(get("/account/register")).andExpect(forwardedUrl("/next/account"));
        mvc.perform(get("/redirect")).andExpect(redirectedUrl("/account/login"));
        var rollback=MockMvcBuilders.standaloneSetup(new Pages()).addInterceptors(new FrontendMigrationConfig.Replacement(false)).build();
        rollback.perform(get("/")).andExpect(view().name("old-home"));
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
        @GetMapping("/redirect") String redirect(){return "redirect:/account/login";}
    }
}
