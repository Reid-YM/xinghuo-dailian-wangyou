package com.xinghuo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.xinghuo.dto.ApiDtos;
import com.xinghuo.exception.BadRequestException;
import com.xinghuo.model.AdminTestFeedback;
import com.xinghuo.repository.AdminTestFeedbackRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class AdminTestFeedbackServiceTest {

    @Test
    void createTrimsFieldsAndFallsBackToNormalPriority() {
        AdminTestFeedbackRepository repo = mock(AdminTestFeedbackRepository.class);
        AuditService audit = mock(AuditService.class);
        HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        AdminTestFeedbackService service = new AdminTestFeedbackService(repo, audit);
        when(repo.save(any(AdminTestFeedback.class))).thenAnswer(invocation -> {
            AdminTestFeedback feedback = invocation.getArgument(0);
            feedback.setId(7L);
            return feedback;
        });

        ApiDtos.TestFeedbackResponse response = service.create(
                new ApiDtos.TestFeedbackCreateRequest(
                        "  页面按钮错位  ",
                        "  /admin  ",
                        "  需要修复手机端按钮位置  ",
                        "not-a-priority",
                        "   "),
                "tester",
                httpRequest);

        assertEquals(7L, response.id());
        assertEquals("页面按钮错位", response.title());
        assertEquals("/admin", response.pagePath());
        assertEquals("需要修复手机端按钮位置", response.content());
        assertEquals("NORMAL", response.priority());
        assertEquals("NEW", response.status());
        assertNull(response.operatorNote());
        assertEquals("tester", response.createdBy());
    }

    @Test
    void invalidUpdateStatusStillFailsBeforeSave() {
        AdminTestFeedbackRepository repo = mock(AdminTestFeedbackRepository.class);
        AuditService audit = mock(AuditService.class);
        AdminTestFeedbackService service = new AdminTestFeedbackService(repo, audit);
        Instant now = Instant.parse("2026-10-12T00:00:00Z");
        AdminTestFeedback existing = AdminTestFeedback.builder()
                .id(9L)
                .title("现有反馈")
                .content("现有内容")
                .priority(AdminTestFeedback.Priority.NORMAL)
                .status(AdminTestFeedback.Status.NEW)
                .createdBy("tester")
                .createdAt(now)
                .updatedAt(now)
                .build();
        when(repo.findById(9L)).thenReturn(Optional.of(existing));

        assertThrows(BadRequestException.class, () -> service.update(
                9L,
                new ApiDtos.TestFeedbackUpdateRequest(
                        "现有反馈",
                        null,
                        "现有内容",
                        "HIGH",
                        "NOT_A_STATUS",
                        null),
                "tester",
                mock(HttpServletRequest.class)));

        verify(repo, never()).save(any(AdminTestFeedback.class));
    }

    @Test
    void blankRequiredFeedbackStillFailsFast() {
        AdminTestFeedbackRepository repo = mock(AdminTestFeedbackRepository.class);
        AdminTestFeedbackService service = new AdminTestFeedbackService(repo, mock(AuditService.class));

        assertThrows(BadRequestException.class, () -> service.create(
                new ApiDtos.TestFeedbackCreateRequest("   ", null, "内容", null, null),
                "tester",
                mock(HttpServletRequest.class)));

        verify(repo, never()).save(any(AdminTestFeedback.class));
    }
}
