package com.xinghuo.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import com.xinghuo.security.XingweiPermissionGuard;
import com.xinghuo.security.XingweiPermissionGuard.AccessSnapshot;
import com.xinghuo.service.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

class XingweiAuxiliaryWorkspaceSecurityTest {
    @Test void operationalMetricsOnlyQueryModulesTheViewerCanRead(){
        var guard=mock(XingweiPermissionGuard.class);var jdbc=mock(org.springframework.jdbc.core.JdbcTemplate.class);var tracking=mock(AdminTrackingController.class);var actor=new UsernamePasswordAuthenticationToken("viewer","",List.of());when(guard.resolve(actor)).thenReturn(new AccessSnapshot("SHADOW_RBAC",false,List.of(),List.of(),"ALL",1L,null));when(guard.has(actor,"dashboard:view")).thenReturn(true);when(guard.has(actor,"quote:view")).thenReturn(true);when(tracking.summary(7)).thenReturn(java.util.Map.of());when(jdbc.queryForObject("SELECT COUNT(*) FROM quotes WHERE status='NEW'",Long.class)).thenReturn(5L);
        var controller=new XingweiAuxiliaryWorkspaceController(guard,mock(ToolStationService.class),mock(UploadService.class),mock(AuditService.class),tracking,jdbc);
        var result=(java.util.Map<?,?>)controller.stats(7,actor);assertEquals(List.of(java.util.Map.of("label","新询价","count",5L)),result.get("operations"));verify(jdbc).queryForObject("SELECT COUNT(*) FROM quotes WHERE status='NEW'",Long.class);verifyNoMoreInteractions(jdbc);
    }
    @Test void narrowAndReadOnlyAccountsCannotWriteGlobalToolDataOrReadGlobalTraffic(){
        var guard=mock(XingweiPermissionGuard.class);var tools=mock(ToolStationService.class);var uploads=mock(UploadService.class);var audit=mock(AuditService.class);var tracking=mock(AdminTrackingController.class);
        var actor=new UsernamePasswordAuthenticationToken("employee","",List.of());when(guard.has(eq(actor),anyString())).thenReturn(true);when(guard.resolve(actor)).thenReturn(new AccessSnapshot("SHADOW_RBAC",false,List.of(),List.of(),"DEPARTMENT",1L,"03"));
        when(tracking.summary(anyInt())).thenReturn(java.util.Map.of());
        var controller=new XingweiAuxiliaryWorkspaceController(guard,tools,uploads,audit,tracking,mock(org.springframework.jdbc.core.JdbcTemplate.class));
        assertThrows(AccessDeniedException.class,()->controller.read(actor));assertThrows(AccessDeniedException.class,()->controller.save(null,"stale","修改",actor,null));assertThrows(AccessDeniedException.class,()->controller.image(null,actor,null));assertThrows(AccessDeniedException.class,()->controller.stats(7,actor));verifyNoInteractions(tools,uploads,audit,tracking);
        when(guard.resolve(actor)).thenReturn(new AccessSnapshot("SHADOW_RBAC",false,List.of(),List.of(),"ALL",1L,null));when(guard.has(actor,"content:manage")).thenReturn(false);
        assertThrows(AccessDeniedException.class,()->controller.reset("stale","恢复",actor,null));assertEquals(false,((java.util.Map<?,?>)controller.access(actor)).get("canManage"));
        controller.stats(30,actor);verify(tracking).summary(30);
    }
}
