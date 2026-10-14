package com.xinghuo.wecom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinghuo.service.XingweiOrgRbacService;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class WeComHrServicePositionsTest {
    @Test
    void positionsComeFromV63DatabaseAndOmitDisabledEntries() {
        XingweiOrgRbacService org = mock(XingweiOrgRbacService.class);
        when(org.positions()).thenReturn(List.of(
                Map.of("departmentName", "游戏业务中心", "name", "代练员", "enabled", true),
                Map.of("departmentName", "游戏业务中心", "name", "客服", "enabled", true),
                Map.of("departmentName", "技术中心", "name", "停用岗位", "enabled", false)));
        WeComHrService service = new WeComHrService(mock(WeComClient.class), new ObjectMapper(), org);

        JsonNode positions = service.positions();
        assertEquals(2, positions.path("游戏业务中心").size());
        assertEquals("代练员", positions.path("游戏业务中心").get(0).asText());
        assertFalse(positions.has("技术中心"));
    }
}
