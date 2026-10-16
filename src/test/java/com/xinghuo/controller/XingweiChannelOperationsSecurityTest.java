package com.xinghuo.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

import com.xinghuo.security.XingweiPermissionGuard;
import com.xinghuo.service.XingweiChannelOperationsService;
import com.xinghuo.service.XingweiChannelOperationsService.TaskInput;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.context.SecurityContextHolder;

class XingweiChannelOperationsSecurityTest {
    @Configuration
    @EnableMethodSecurity
    static class Security {}
    @AfterEach
    void clear() { SecurityContextHolder.clearContext(); }
    @Test
    void methodSecuritySeparatesChannelViewingFromWrites() {
        var guard = mock(XingweiPermissionGuard.class);
        var service = mock(XingweiChannelOperationsService.class);
        var actor = new UsernamePasswordAuthenticationToken("operator","",List.of());
        when(guard.has(actor,"channel:view")).thenReturn(true);
        when(service.snapshot(actor,"","","",0)).thenReturn(Map.of("items",List.of()));
        try (var context = new AnnotationConfigApplicationContext()) {
            context.register(Security.class);
            context.registerBean("xingweiPermissionGuard",XingweiPermissionGuard.class,() -> guard);
            context.registerBean(XingweiChannelOperationsService.class,() -> service);
            context.registerBean(XingweiChannelOperationsController.class);
            context.refresh();
            SecurityContextHolder.getContext().setAuthentication(actor);
            var controller = context.getBean(XingweiChannelOperationsController.class);
            assertEquals("admin-xingwei-channels",controller.page());
            assertEquals(List.of(),controller.snapshot(actor,"","","",0).get("items"));
            assertThrows(AccessDeniedException.class,() -> controller.create(actor,new TaskInput("TAOBAO","任务","","TODO",1L,null,null),null));
            verify(service,never()).create(any(),any(),any());
            when(guard.has(actor,"channel:view")).thenReturn(false);
            assertThrows(AccessDeniedException.class,controller::page);
            assertThrows(AccessDeniedException.class,() -> controller.snapshot(actor,"","","",0));
        }
    }
}
