package com.xinghuo.security;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.xinghuo.controller.*;
import com.xinghuo.service.*;
import com.xinghuo.wecom.*;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.junit.jupiter.api.*;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

class XingweiCatalogMaintenanceSecurityTest {
    @Configuration @EnableWebSecurity @EnableWebMvc @Import(SecurityConfig.class)
    static class TestConfig {
        @Bean AdminUserService users(){var service=mock(AdminUserService.class);when(service.sessionCurrent(any())).thenReturn(true);return service;}
        @Bean XingweiWeComBindingService bindings(){return mock(XingweiWeComBindingService.class);}
        @Bean XingweiWeComProperties properties(){return new XingweiWeComProperties("test-corp","server-only-test","1000003",false,false,false);}
        @Bean XingweiPermissionGuard xingweiPermissionGuard(){return mock(XingweiPermissionGuard.class);}
        @Bean ServiceCatalogService services(){return mock(ServiceCatalogService.class);}
        @Bean QuoteService quotes(){return mock(QuoteService.class);}
        @Bean OrderService orders(){return mock(OrderService.class);}
        @Bean XingweiCatalogDetailsService details(XingweiPermissionGuard guard,ServiceCatalogService services){return new XingweiCatalogDetailsService(guard,services,mock(TaxonomyService.class),mock(XingweiCatalogRevisionService.class),mock(EntityManager.class),mock(UploadService.class),mock(XingweiServicePublishReviewService.class),mock(XingweiTaxonomyControlService.class),mock(AuditService.class));}
        @Bean XingweiCatalogDetailsController catalogue(XingweiCatalogDetailsService details){return new XingweiCatalogDetailsController(details);}
        @Bean XingweiBusinessMaintenanceService maintenance(XingweiPermissionGuard guard,QuoteService quotes,OrderService orders){return new XingweiBusinessMaintenanceService(mock(JdbcTemplate.class),guard,mock(XingweiBusinessScopeService.class),mock(XingweiCatalogRevisionService.class),quotes,orders,mock(EntityManager.class),mock(AuditService.class));}
        @Bean XingweiBusinessMaintenanceController business(XingweiBusinessMaintenanceService work){return new XingweiBusinessMaintenanceController(work);}
    }
    AnnotationConfigWebApplicationContext context;MockMvc mvc;XingweiPermissionGuard guard;
    @BeforeEach void setup(){context=new AnnotationConfigWebApplicationContext();context.setServletContext(new MockServletContext());context.register(TestConfig.class);context.refresh();mvc=MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();guard=context.getBean(XingweiPermissionGuard.class);}
    @AfterEach void close(){context.close();}
    @Test void actualNativeEndpointsDenyMissingModulePermissionsAndAnonymousRequests()throws Exception{
        mvc.perform(get("/api/admin/xingwei/catalog-details")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/xingwei/catalog-details").with(user("staff").roles("OPERATOR"))).andExpect(status().isForbidden()).andExpect(header().string("Cache-Control","no-store"));
        mvc.perform(get("/api/admin/xingwei/maintenance").with(user("staff").roles("OPERATOR"))).andExpect(status().isForbidden());
        mvc.perform(get("/admin/xingwei/catalog-details").with(user("staff").roles("OPERATOR"))).andExpect(status().isForbidden());
        mvc.perform(get("/admin/xingwei/maintenance").with(user("staff").roles("OPERATOR"))).andExpect(status().isForbidden());
        verifyNoInteractions(context.getBean(ServiceCatalogService.class),context.getBean(QuoteService.class),context.getBean(OrderService.class));
    }
    @Test void actualWritesRequireCsrfAndRejectDepartmentCatalogueOrNonAdminDeletes()throws Exception{
        String profile="{\"name\":\"Actual game\",\"reason\":\"metadata review\"}";
        mvc.perform(put("/api/admin/xingwei/catalog-details/games/MW4").with(user("staff").roles("OPERATOR")).contentType("application/json").content(profile)).andExpect(status().isForbidden());
        when(guard.has(any(),anyString())).thenReturn(true);when(guard.resolve(any())).thenReturn(new XingweiPermissionGuard.AccessSnapshot("SHADOW_RBAC",false,List.of(),List.of(),"DEPARTMENT",1L,"03"));
        mvc.perform(put("/api/admin/xingwei/catalog-details/games/MW4").with(user("staff").roles("OPERATOR")).with(csrf()).header("If-Match","old").contentType("application/json").content(profile)).andExpect(status().isForbidden());
        mvc.perform(delete("/api/admin/xingwei/maintenance/orders/O-1").with(user("staff").roles("OPERATOR")).with(csrf()).contentType("application/json").content("{\"confirmation\":\"B-1\"}")).andExpect(status().isForbidden());
        mvc.perform(delete("/api/admin/xingwei/catalog-details/games/MW4").with(user("staff").roles("OPERATOR")).with(csrf()).contentType("application/json").content("{\"confirmation\":\"MW4\"}")).andExpect(status().isForbidden());
        verifyNoInteractions(context.getBean(ServiceCatalogService.class),context.getBean(QuoteService.class),context.getBean(OrderService.class));
    }
}
