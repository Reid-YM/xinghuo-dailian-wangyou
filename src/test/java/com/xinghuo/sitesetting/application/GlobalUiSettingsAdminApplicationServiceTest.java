package com.xinghuo.sitesetting.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import org.junit.jupiter.api.Test;

class GlobalUiSettingsAdminApplicationServiceTest {
    @Test
    void updateRecordsBeforeAndAfterThroughAuditPort() throws Exception {
        SiteSettingApplicationService settings = mock(SiteSettingApplicationService.class);
        GlobalUiSettingsAuditPort audit = mock(GlobalUiSettingsAuditPort.class);
        GlobalUiSettingsAdminApplicationService service =
                new GlobalUiSettingsAdminApplicationService(settings, audit);
        GlobalUiSettingsUpdateCommand command = new GlobalUiSettingsUpdateCommand(Map.of("navServicesVisible", "false"));
        GlobalUiSettingsAuditContext context = new GlobalUiSettingsAuditContext(
                "admin", "后台全局前端控制", "127.0.0.1", "req-1");
        Map<String, String> before = Map.of("navServicesVisible", "true");
        Map<String, String> after = Map.of("navServicesVisible", "false");
        when(settings.globalUiSettings()).thenReturn(before);
        when(settings.updateGlobalUiSettings(command)).thenReturn(after);

        Map<String, String> result = service.update(command, context);

        assertEquals(after, result);
        verify(audit).record(before, after, context);
    }
}
