package com.xinghuo.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.xinghuo.security.XingweiPermissionGuard;
import com.xinghuo.security.XingweiPermissionGuard.AccessSnapshot;
import com.xinghuo.service.XingweiIntegrationConfigService;
import com.xinghuo.service.XingweiPaymentOverviewService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.ui.ExtendedModelMap;

class XingweiIntegrationPaymentScopeTest {
    @Test void scopedPaymentViewerNeverReceivesGlobalCountsOrRecentPayments(){
        var config=mock(XingweiIntegrationConfigService.class);var guard=mock(XingweiPermissionGuard.class);var overview=mock(XingweiPaymentOverviewService.class);var actor=new UsernamePasswordAuthenticationToken("dept","",List.of());when(guard.has(actor,"payment:view")).thenReturn(true);when(guard.resolve(actor)).thenReturn(new AccessSnapshot("SHADOW_RBAC",false,List.of("payment:view"),List.of(),"DEPARTMENT",1L,"03"));
        var controller=new XingweiIntegrationsController(config,guard,overview);var model=new ExtendedModelMap();controller.integrations(model,null,actor);assertFalse(model.containsAttribute("paymentOverview"));verifyNoInteractions(overview);
    }
}
