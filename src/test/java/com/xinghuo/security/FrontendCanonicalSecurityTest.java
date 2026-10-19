package com.xinghuo.security;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.xinghuo.account.model.Account;
import com.xinghuo.account.service.AccountSessionService;
import com.xinghuo.account.web.*;
import com.xinghuo.service.AdminUserService;
import com.xinghuo.web.FrontendMigrationConfig;
import com.xinghuo.wecom.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.context.annotation.*;
import org.springframework.mock.web.*;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

class FrontendCanonicalSecurityTest {
    @Configuration @EnableWebSecurity @EnableWebMvc
    @Import({SecurityConfig.class,AccountWebConfig.class,FrontendMigrationConfig.class})
    static class Config {
        @Bean AdminUserService users(){var users=mock(AdminUserService.class);when(users.sessionCurrent(any())).thenReturn(true);return users;}
        @Bean XingweiWeComBindingService bindings(){return mock(XingweiWeComBindingService.class);}
        @Bean XingweiWeComProperties properties(){return new XingweiWeComProperties("test","test","test",false,false,false);}
        @Bean AccountSessionService sessions(){return mock(AccountSessionService.class);}
        @Bean AccountAuthInterceptor accounts(AccountSessionService sessions){return new AccountAuthInterceptor(sessions);}
        @Bean AccountCenterPageController pages(){return new AccountCenterPageController();}
        @Bean InternalResourceViewResolver views(){return new InternalResourceViewResolver("/templates/",".html");}
    }
    AnnotationConfigWebApplicationContext context;MockMvc mvc;AccountSessionService sessions;
    @BeforeEach void setup(){
        context=new AnnotationConfigWebApplicationContext();context.setServletContext(new MockServletContext());
        context.getEnvironment().getPropertySources().addFirst(new org.springframework.core.env.MapPropertySource("frontend-test",Map.of("app.frontend-next.default-enabled","true")));
        context.register(Config.class);context.refresh();mvc=MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        sessions=context.getBean(AccountSessionService.class);when(sessions.current(any())).thenReturn(Optional.empty());
    }
    @AfterEach void close(){context.close();}
    static List<String> paths(){return List.of("/account/orders/XHABCDEF1234","/account/requests/XHABCDEF1234","/account/favorites","/account/submissions");}
    @Test void springSecurityAllowsAccountSessionPolicyToHandleNewPathsAndDoesNotGrantAnonymousAccess()throws Exception{
        for(String path:paths())mvc.perform(get(path)).andExpect(status().isFound()).andExpect(redirectedUrl("/account/login?next="+java.net.URLEncoder.encode(path,java.nio.charset.StandardCharsets.UTF_8)));
        mvc.perform(get("/account/favorites").with(user("admin").roles("ADMIN"))).andExpect(status().isFound()).andExpect(redirectedUrl("/account/login?next=%2Faccount%2Ffavorites"));
    }
    @Test void actualAccountSessionCanReachTheViewsWhileUnknownNestedPathsStayDenied()throws Exception{
        when(sessions.current(any())).thenReturn(Optional.of(mock(Account.class)));
        for(String path:paths())mvc.perform(get(path)).andExpect(forwardedUrl("/next"+path));
        mvc.perform(get("/account/orders/XHABCDEF1234/extra")).andExpect(status().is4xxClientError());
    }
    @Test void explicitRollbackSurvivesAliasAndAccountLoginRedirects()throws Exception{
        mvc.perform(get("/account/profile?legacy=1")).andExpect(redirectedUrl("/account/login?legacy=1&next=%2Faccount%2Fprofile%3Flegacy%3D1"));
        when(sessions.current(any())).thenReturn(Optional.of(mock(Account.class)));
        mvc.perform(get("/account/profile?legacy=1")).andExpect(redirectedUrl("/account/settings?legacy=1#profile"));
        mvc.perform(get("/account/orders?legacy=1")).andExpect(view().name("account-orders"));
    }
}
