package com.xinghuo.frontendcontrol.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import org.junit.jupiter.api.Test;

class FrontendControlAdminApplicationServiceTest {
    @Test
    void updateRecordsBeforeAndAfterThroughAuditPort() {
        FrontendControlApplicationService controls = mock(FrontendControlApplicationService.class);
        FrontendControlAuditPort audit = mock(FrontendControlAuditPort.class);
        FrontendControlAdminApplicationService service = new FrontendControlAdminApplicationService(controls, audit);
        FrontendControlUpdateCommand command = new FrontendControlUpdateCommand(Map.of("themeRadius", "22"));
        FrontendControlAuditContext context = new FrontendControlAuditContext(
                "admin", "后台前端控制中心", "127.0.0.1", "req-1");
        Map<String, String> before = Map.of("themeRadius", "18");
        Map<String, String> after = Map.of("themeRadius", "22");
        when(controls.settings()).thenReturn(before);
        when(controls.update(command)).thenReturn(after);

        Map<String, String> result = service.update(command, context);

        assertEquals(after, result);
        verify(audit).record(before, after, context);
    }
}
