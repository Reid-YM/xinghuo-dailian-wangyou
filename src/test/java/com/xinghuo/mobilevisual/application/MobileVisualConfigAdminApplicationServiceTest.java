package com.xinghuo.mobilevisual.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

class MobileVisualConfigAdminApplicationServiceTest {
    @Test
    void updateRecordsBeforeAndAfterThroughAuditPort() {
        MobileVisualConfigApplicationService configs = mock(MobileVisualConfigApplicationService.class);
        MobileVisualConfigAuditPort audit = mock(MobileVisualConfigAuditPort.class);
        MobileVisualConfigAdminApplicationService service =
                new MobileVisualConfigAdminApplicationService(configs, audit);
        UpdateMobileVisualConfigCommand command = new UpdateMobileVisualConfigCommand(80, 8);
        MobileVisualConfigAuditContext context = new MobileVisualConfigAuditContext(
                "admin", "后台手机背景参数保存", "127.0.0.1", "req-1");
        MobileVisualConfigView before = new MobileVisualConfigView(70, 6);
        MobileVisualConfigView after = new MobileVisualConfigView(80, 8);
        when(configs.get()).thenReturn(before);
        when(configs.update(command)).thenReturn(after);

        MobileVisualConfigView result = service.update(command, context);

        assertEquals(after, result);
        verify(audit).record(before, after, context);
    }
}
