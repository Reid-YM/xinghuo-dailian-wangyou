package com.xinghuo.wecom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.xinghuo.service.AuditService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

class AdminEmployeeControllerTest {
    private final WeComHrService hr = mock(WeComHrService.class);
    private final AuditService audits = mock(AuditService.class);
    private final AdminEmployeeController controller = new AdminEmployeeController(hr, audits);

    @Test
    void operatorCannotReadContactsOrWriteReadiness() {
        when(hr.employees()).thenReturn(new WeComHrService.EmployeeListResponse(true, true, 1,
                List.of(new WeComHrService.EmployeeItem("user1", "员工", "客服",
                        "13800000000", "private@example.com", "work@example.com", 1,
                        List.of(1L), List.of("客服部")))));

        WeComHrService.EmployeeListResponse response = controller.employees(auth("ROLE_OPERATOR"));

        assertEquals(1, response.total());
        assertEquals("user1", response.items().getFirst().userId());
        assertEquals("", response.items().getFirst().mobile());
        assertEquals("", response.items().getFirst().email());
        assertEquals("", response.items().getFirst().businessEmail());
        assertTrue(response.readConfigured());
        assertEquals(false, response.writeConfigured());
    }

    @Test
    void adminCanReadContacts() {
        WeComHrService.EmployeeListResponse source = new WeComHrService.EmployeeListResponse(true, true, 1,
                List.of(new WeComHrService.EmployeeItem("user1", "员工", "客服",
                        "13800000000", "private@example.com", "work@example.com", 1,
                        List.of(1L), List.of("客服部"))));
        when(hr.employees()).thenReturn(source);

        assertEquals(source, controller.employees(auth("ROLE_ADMIN")));
    }

    @Test
    void adminWritesAreAuditedWithoutContactDetails() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        WeComHrService.NewEmployeeRequest employee = new WeComHrService.NewEmployeeRequest(
                "user1", "员工", "13800000000", "private@example.com", 1L, "客服");
        Authentication admin = auth("ROLE_ADMIN");

        controller.create(employee, admin, request);
        controller.transfer("user1", new WeComHrService.AssignmentRequest(2L, "经理"), admin, request);
        controller.departure("user1", admin, request);

        verify(audits).log(eq("WECOM_EMPLOYEE_CREATE"), eq("WECOM_EMPLOYEE"), eq("user1"),
                eq("admin"), isNull(), eq(Map.of("departmentId", 1L, "position", "客服")),
                eq("企业微信新增成员"), eq(request));
        verify(audits).log(eq("WECOM_EMPLOYEE_TRANSFER"), eq("WECOM_EMPLOYEE"), eq("user1"),
                eq("admin"), isNull(), eq(Map.of("departmentId", 2L, "position", "经理")),
                eq("企业微信调岗"), eq(request));
        verify(audits).log(eq("WECOM_EMPLOYEE_DISABLE"), eq("WECOM_EMPLOYEE"), eq("user1"),
                eq("admin"), isNull(), eq(Map.of("enabled", false)),
                eq("企业微信离职停用"), eq(request));
    }

    private Authentication auth(String role) {
        return new UsernamePasswordAuthenticationToken("admin", "", List.of(new SimpleGrantedAuthority(role)));
    }
}
