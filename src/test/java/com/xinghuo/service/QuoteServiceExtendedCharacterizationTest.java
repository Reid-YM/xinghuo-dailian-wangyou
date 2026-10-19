package com.xinghuo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.xinghuo.dto.ApiDtos;
import com.xinghuo.dto.QuoteLeadDtos;
import com.xinghuo.exception.BadRequestException;
import com.xinghuo.model.Quote;
import com.xinghuo.repository.QuoteRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class QuoteServiceExtendedCharacterizationTest {

    @Test
    void updateCrmNormalizesFieldsAndUsesDefaultAuditReason() {
        QuoteRepository repo = mock(QuoteRepository.class);
        AuditService audit = mock(AuditService.class);
        Quote quote = quote(Quote.Status.NEW);
        when(repo.findById(quote.getId())).thenReturn(Optional.of(quote));
        QuoteService service = new QuoteService(repo, mock(UploadService.class), audit, mock(OrderService.class));
        HttpServletRequest request = mock(HttpServletRequest.class);

        QuoteLeadDtos.QuoteLeadResponse response = service.updateCrm(
                quote.getId(),
                new QuoteLeadDtos.QuoteCrmUpdateRequest(
                        82,
                        "HIGH",
                        "  今天联系  ",
                        "  已确认预算  ",
                        "   "),
                "admin",
                request);

        assertEquals(82, quote.getLeadScore());
        assertEquals("HIGH", quote.getLeadIntentLevel());
        assertEquals("今天联系", quote.getNextAction());
        assertEquals("已确认预算", quote.getFollowUpNote());
        assertNotNull(quote.getLastContactedAt());
        assertNotNull(quote.getUpdatedAt());
        assertEquals(82, response.leadScore());
        assertEquals("HIGH", response.leadIntentLevel());
        verify(audit).log(
                eq("QUOTE_CRM_UPDATE"),
                eq("QUOTE"),
                eq(quote.getBusinessNo()),
                eq("admin"),
                any(),
                any(),
                eq("更新线索跟进信息"),
                eq(request));
    }

    @Test
    void omittedCrmInternalNotePreservesHistoryWhileUpdatingContactPlan() {
        QuoteRepository repo = mock(QuoteRepository.class);
        Quote quote = quote(Quote.Status.NEW);
        quote.setFollowUpNote("历史内部跟进记录");
        when(repo.findById(quote.getId())).thenReturn(Optional.of(quote));
        QuoteService service = new QuoteService(repo, mock(UploadService.class), mock(AuditService.class), mock(OrderService.class));
        service.updateCrm(quote.getId(), new QuoteLeadDtos.QuoteCrmUpdateRequest(60, "MEDIUM", "明天联系", null, "更新联系计划"), "admin", mock(HttpServletRequest.class));
        assertEquals("历史内部跟进记录", quote.getFollowUpNote());
        assertEquals("明天联系", quote.getNextAction());
    }

    @Test
    void generateLeadSuggestionCapsCompleteLeadAtHundredAndHighIntent() {
        QuoteRepository repo = mock(QuoteRepository.class);
        AuditService audit = mock(AuditService.class);
        Quote quote = quote(Quote.Status.NEW);
        quote.setDemand("完成全部迷彩挑战并在本周内交付账号进度");
        quote.setCurrentProgress("当前 30%");
        quote.setPlatform("PC");
        quote.setExpectedTime("本周内");
        quote.setScreenshotAssetId(UUID.randomUUID());
        quote.setFirstTouchSource("home");
        when(repo.findById(quote.getId())).thenReturn(Optional.of(quote));
        QuoteService service = new QuoteService(repo, mock(UploadService.class), audit, mock(OrderService.class));
        HttpServletRequest request = mock(HttpServletRequest.class);

        QuoteLeadDtos.QuoteAiSuggestionResponse response = service.generateLeadSuggestion(
                quote.getId(),
                "admin",
                request);

        assertEquals(100, response.leadScore());
        assertEquals("HIGH", response.leadIntentLevel());
        assertEquals(100, quote.getLeadScore());
        assertEquals("HIGH", quote.getLeadIntentLevel());
        assertNotNull(quote.getNextAction());
        assertTrue(quote.getAiSuggestion().contains("判断依据："));
        verify(audit).log(
                eq("QUOTE_AI_SUGGESTION"),
                eq("QUOTE"),
                eq(quote.getBusinessNo()),
                eq("admin"),
                any(),
                any(),
                eq("生成AI线索建议"),
                eq(request));
    }

    @Test
    void closedQuoteIsRejectedBeforeOrderCreation() {
        QuoteRepository repo = mock(QuoteRepository.class);
        OrderService orders = mock(OrderService.class);
        Quote quote = quote(Quote.Status.CLOSED);
        when(repo.findById(quote.getId())).thenReturn(Optional.of(quote));
        QuoteService service = new QuoteService(repo, mock(UploadService.class), mock(AuditService.class), orders);
        HttpServletRequest request = mock(HttpServletRequest.class);

        assertThrows(BadRequestException.class, () -> service.convertToOrder(
                quote.getId(),
                new ApiDtos.QuoteConvertRequest("尝试转单"),
                "admin",
                request));

        assertEquals(Quote.Status.CLOSED, quote.getStatus());
        verify(orders, never()).createFromQuote(any(), any(), any(), any());
    }

    private Quote quote(Quote.Status status) {
        Instant now = Instant.parse("2026-10-12T00:00:00Z");
        return Quote.builder()
                .id(UUID.randomUUID())
                .businessNo("XHABCDEF2345")
                .game("MW4")
                .demand("完成迷彩挑战")
                .contactMethod("微信")
                .customerContact("wx-user")
                .status(status)
                .idempotencyKey("existing-key")
                .createdAt(now)
                .updatedAt(now)
                .build();
    }
}
