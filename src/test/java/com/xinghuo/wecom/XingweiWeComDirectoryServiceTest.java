package com.xinghuo.wecom;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinghuo.service.XingweiOrgRbacService;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class XingweiWeComDirectoryServiceTest {
    @Test
    void missingDepartmentNamesStayReadableWhileProtocolIdsRemainAvailable() throws Exception {
        var client = mock(XingweiWeComDirectoryClient.class);
        var org = mock(XingweiOrgRbacService.class);
        var mapper = new ObjectMapper();
        when(client.configured()).thenReturn(true);
        when(client.listDepartments()).thenReturn(mapper.readTree("""
            {"department":[{"id":2,"parentid":1,"name":"业务中心"}]}
            """));
        when(client.listUsers()).thenReturn(mapper.readTree("""
            {"userlist":[{"userid":"private-member","name":"员工甲",
              "department":[2,98765],"status":1,"mobile":"private-contact"}]}
            """));
        when(org.positions()).thenReturn(List.of(Map.of("code", "P001", "name", "客服")));
        var snapshot = new XingweiWeComDirectoryService(client, org).snapshot();
        var employees = (List<?>) snapshot.get("employees");
        var employee = (Map<?, ?>) employees.getFirst();
        assertEquals(List.of("业务中心", "部门名称待同步"), employee.get("departments"));
        assertEquals(List.of(2L, 98765L), employee.get("departmentIds"));
        assertEquals("private-member", employee.get("userId"));
        assertFalse(employee.containsKey("mobile"));
        assertEquals(Map.of("employees", 1, "departments", 1, "positions", 1), snapshot.get("counts"));
        assertEquals(true, snapshot.get("readOnly"));
    }
}
