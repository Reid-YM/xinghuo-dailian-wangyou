package com.xinghuo.security;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinghuo.controller.XingweiWeComHrController;
import com.xinghuo.service.*;
import com.xinghuo.wecom.*;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.*;
import org.springframework.context.annotation.*;
import org.springframework.mock.web.*;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

class XingweiWeComHrSecurityTest {
    @Configuration @EnableWebSecurity @EnableWebMvc @Import(SecurityConfig.class)
    static class Config {
        @Bean AdminUserService users(){var users=mock(AdminUserService.class);when(users.sessionCurrent(any())).thenReturn(true);return users;}
        @Bean XingweiWeComBindingService bindings(){return mock(XingweiWeComBindingService.class);}
        @Bean XingweiWeComProperties properties(){return new XingweiWeComProperties("test","test-read","1",false,false,false);}
        @Bean XingweiPermissionGuard xingweiPermissionGuard(){return mock(XingweiPermissionGuard.class);}
        @Bean XingweiWeComHrService hr(){return mock(XingweiWeComHrService.class);}
        @Bean XingweiWeComHrAccessService local(){return mock(XingweiWeComHrAccessService.class);}
        @Bean AuditService audits(){return mock(AuditService.class);}
        @Bean XingweiWeComHrController controller(XingweiWeComHrService hr,XingweiWeComHrAccessService local,XingweiPermissionGuard guard,AuditService audit){return new XingweiWeComHrController(hr,local,guard,audit);}
    }
    AnnotationConfigWebApplicationContext context; MockMvc mvc; XingweiPermissionGuard guard; XingweiWeComHrService hr;
    final ObjectMapper mapper=new ObjectMapper();
    @BeforeEach void setup(){context=new AnnotationConfigWebApplicationContext();context.setServletContext(new MockServletContext());context.register(Config.class);context.refresh();mvc=MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();guard=context.getBean(XingweiPermissionGuard.class);hr=context.getBean(XingweiWeComHrService.class);}
    @AfterEach void close(){context.close();}
    void scope(String scope){when(guard.has(any(),anyString())).thenReturn(true);when(guard.resolve(any())).thenReturn(new XingweiPermissionGuard.AccessSnapshot("SHADOW_RBAC",false,List.of("employee:manage"),List.of(),scope,4L,"03"));when(hr.ready()).thenReturn(true);}
    @Test void realSecurityChainRequiresLoginModulePermissionGlobalScopeAndCsrf() throws Exception {
        mvc.perform(get("/api/admin/xingwei/wecom/hr/capabilities")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/admin/xingwei/wecom/hr/preview").with(user("staff").roles("OPERATOR")).with(csrf()).contentType("application/json").content("{}")).andExpect(status().isForbidden());
        scope("DEPARTMENT");
        mvc.perform(post("/api/admin/xingwei/wecom/hr/preview").with(user("staff").roles("OPERATOR")).with(csrf()).contentType("application/json").content("{}")).andExpect(status().isForbidden());
        scope("ALL");
        mvc.perform(post("/api/admin/xingwei/wecom/hr/preview").with(user("staff").roles("OPERATOR")).contentType("application/json").content("{}")).andExpect(status().isForbidden());
        verify(hr,never()).prepare(any());verify(hr,never()).execute(any());
    }
    String preview(MockHttpSession session, String action) throws Exception {
        var payload=mapper.createObjectNode().put("userid","hidden-member").put("enable",0);
        when(hr.prepare(any())).thenReturn(new XingweiWeComHrService.Plan(action,"hidden-member","员工甲","业务中心","客服","hidden-position","fingerprint",payload,Instant.now().plusSeconds(100)));
        var response=mvc.perform(post("/api/admin/xingwei/wecom/hr/preview").session(session).with(user("staff").roles("OPERATOR")).with(csrf()).contentType("application/json").content("{\"action\":\""+action+"\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.targetId").doesNotExist()).andExpect(jsonPath("$.payload").doesNotExist()).andReturn();
        assertFalse(response.getResponse().getContentAsString().contains("hidden-member"));
        return mapper.readTree(response.getResponse().getContentAsString()).path("confirmationToken").asText();
    }
    @Test void confirmationIsSessionBoundNameCheckedAndConsumedOnceBeforeWrite() throws Exception {
        scope("ALL");var session=new MockHttpSession();String token=preview(session,"TRANSFER_MEMBER");
        String body=mapper.writeValueAsString(java.util.Map.of("confirmationToken",token,"confirmName","员工甲"));
        mvc.perform(post("/api/admin/xingwei/wecom/hr/commit").session(new MockHttpSession()).with(user("staff").roles("OPERATOR")).with(csrf()).contentType("application/json").content(body)).andExpect(status().isConflict());
        mvc.perform(post("/api/admin/xingwei/wecom/hr/commit").session(session).with(user("other").roles("OPERATOR")).with(csrf()).contentType("application/json").content(body)).andExpect(status().isConflict());
        mvc.perform(post("/api/admin/xingwei/wecom/hr/commit").session(session).with(user("staff").roles("OPERATOR")).with(csrf()).contentType("application/json").content(body.replace("员工甲","错误姓名"))).andExpect(status().isBadRequest());
        mvc.perform(post("/api/admin/xingwei/wecom/hr/commit").session(session).with(user("staff").roles("OPERATOR")).with(csrf()).contentType("application/json").content(body)).andExpect(status().isOk());
        mvc.perform(post("/api/admin/xingwei/wecom/hr/commit").session(session).with(user("staff").roles("OPERATOR")).with(csrf()).contentType("application/json").content(body)).andExpect(status().isConflict());
        verify(hr,times(1)).execute(any());verify(context.getBean(AuditService.class),times(2)).log(anyString(),anyString(),anyString(),anyString(),any(),any(),anyString(),any());
    }
    @Test void permissionAndWriteFlagAreRecheckedAtConfirmationTime() throws Exception {
        scope("ALL");var session=new MockHttpSession();String token=preview(session,"TRANSFER_MEMBER");
        String body=mapper.writeValueAsString(java.util.Map.of("confirmationToken",token,"confirmName","员工甲"));
        scope("SELF");
        mvc.perform(post("/api/admin/xingwei/wecom/hr/commit").session(session).with(user("staff").roles("OPERATOR")).with(csrf()).contentType("application/json").content(body)).andExpect(status().isForbidden());
        scope("ALL");when(hr.ready()).thenReturn(false);
        mvc.perform(post("/api/admin/xingwei/wecom/hr/commit").session(session).with(user("staff").roles("OPERATOR")).with(csrf()).contentType("application/json").content(body)).andExpect(status().isConflict());
        verify(hr,never()).execute(any());
    }
    @Test void disableRevokesLocalAccessBeforeRemoteCallAndReportsUncertainResultWithoutRetry() throws Exception {
        scope("ALL");var session=new MockHttpSession();String token=preview(session,"DISABLE_MEMBER");
        doThrow(new IllegalStateException("secret-vendor-diagnostic")).when(hr).execute(any());
        var response=mvc.perform(post("/api/admin/xingwei/wecom/hr/commit").session(session).with(user("staff").roles("OPERATOR")).with(csrf()).contentType("application/json").content(mapper.writeValueAsString(java.util.Map.of("confirmationToken",token,"confirmName","员工甲"))))
                .andExpect(status().isBadGateway()).andExpect(jsonPath("$.error").value(org.hamcrest.Matchers.containsString("本地后台访问已停用"))).andReturn();
        assertFalse(response.getResponse().getContentAsString().contains("secret-vendor"));
        var local=context.getBean(XingweiWeComHrAccessService.class);var order=inOrder(local,hr);
        order.verify(local).beginDisable(eq("hidden-member"),eq("staff"),any());order.verify(hr).execute(any());
    }
}
