package com.xinghuo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.xinghuo.dto.ApiDtos;
import com.xinghuo.dto.PublicQuoteRequest;
import com.xinghuo.exception.BadRequestException;
import com.xinghuo.model.Quote;
import com.xinghuo.repository.QuoteRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class QuoteServiceCharacterizationTest {

    @Test
    void createGeneratesBusinessNumberNormalizesFieldsAndBindsScreenshot() {
        QuoteRepository repo = mock(QuoteRepository.class);
        UploadService uploads = mock(UploadService.class);
        when(repo.findByIdempotencyKey("quote-key")).thenReturn(Optional.empty());
        when(repo.findByBusinessNo(anyString())).thenReturn(Optional.empty());
        QuoteService service = new QuoteService(repo, uploads, mock(AuditService.class), mock(OrderService.class));
        UUID screenshotId = UUID.randomUUID();

        ApiDtos.QuoteResponse response = service.create(
                request(screenshotId),
                "quote-key",
                "ip-hash",
                42L);

        ArgumentCaptor<Quote> captor = ArgumentCaptor.forClass(Quote.class);
        verify(repo).save(captor.capture());
        Quote saved = captor.getValue();
        assertTrue(saved.getBusinessNo().matches("XH[A-Z0-9]{10}"));
        assertEquals(Quote.Status.NEW, saved.getStatus());
        assertEquals("MW4", saved.getGame());
        assertEquals("完成迷彩挑战", saved.getDemand());
        assertEquals("10%", saved.getCurrentProgress());
        assertEquals("PC", saved.getPlatform());
        assertEquals("微信", saved.getContactMethod());
        assertEquals("wx-user", saved.getCustomerContact());
        assertEquals(42L, saved.getCustomerAccountId());
        assertEquals("quote-key", saved.getIdempotencyKey());
        assertEquals("ip-hash", saved.getCustomerIpHash());
        assertNotNull(saved.getCreatedAt());
        assertEquals(saved.getId(), response.id());
        assertEquals(saved.getBusinessNo(), response.businessNo());
        verify(uploads).bind(screenshotId, "QUOTE", saved.getId().toString());
    }

    @Test
    void repeatedIdempotencyKeyReturnsExistingQuoteWithoutSavingOrBindingAgain() {
        QuoteRepository repo = mock(QuoteRepository.class);
        UploadService uploads = mock(UploadService.class);
        Quote existing = quote(Quote.Status.QUOTED);
        when(repo.findByIdempotencyKey("same-key")).thenReturn(Optional.of(existing));
        QuoteService service = new QuoteService(repo, uploads, mock(AuditService.class), mock(OrderService.class));

        ApiDtos.QuoteResponse response = service.create(
                request(UUID.randomUUID()),
                "same-key",
                "different-ip",
                7L);

        assertEquals(existing.getId(), response.id());
        assertEquals(existing.getBusinessNo(), response.businessNo());
        assertEquals("QUOTED", response.status());
        verify(repo, never()).save(org.mockito.ArgumentMatchers.any(Quote.class));
        verify(uploads, never()).bind(org.mockito.ArgumentMatchers.any(), eq("QUOTE"), anyString());
    }

    @Test
    void convertedQuoteCannotMoveBackToAnotherStatus() {
        QuoteRepository repo = mock(QuoteRepository.class);
        AuditService audit = mock(AuditService.class);
        Quote existing = quote(Quote.Status.CONVERTED);
        when(repo.findById(existing.getId())).thenReturn(Optional.of(existing));
        QuoteService service = new QuoteService(repo, mock(UploadService.class), audit, mock(OrderService.class));

        assertThrows(BadRequestException.class, () -> service.updateStatus(
                existing.getId(),
                new ApiDtos.QuoteStatusUpdate("CONTACTED", "误操作"),
                "admin",
                mock(HttpServletRequest.class)));

        assertEquals(Quote.Status.CONVERTED, existing.getStatus());
        verify(audit, never()).log(
                eq("QUOTE_STATUS_UPDATE"),
                eq("QUOTE"),
                anyString(),
                anyString(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void convertToOrderMarksQuoteConvertedAfterCreatingOrder() {
        QuoteRepository repo = mock(QuoteRepository.class);
        AuditService audit = mock(AuditService.class);
        OrderService orders = mock(OrderService.class);
        Quote existing = quote(Quote.Status.QUOTED);
        HttpServletRequest request = mock(HttpServletRequest.class);
        ApiDtos.AdminOrderResponse orderResponse = mock(ApiDtos.AdminOrderResponse.class);
        when(repo.findById(existing.getId())).thenReturn(Optional.of(existing));
        when(orders.createFromQuote(existing, "admin", request, "确认转单")).thenReturn(orderResponse);
        QuoteService service = new QuoteService(repo, mock(UploadService.class), audit, orders);

        ApiDtos.AdminOrderResponse result = service.convertToOrder(
                existing.getId(),
                new ApiDtos.QuoteConvertRequest("确认转单"),
                "admin",
                request);

        assertEquals(orderResponse, result);
        assertEquals(Quote.Status.CONVERTED, existing.getStatus());
        assertNotNull(existing.getUpdatedAt());
        verify(orders).createFromQuote(existing, "admin", request, "确认转单");
        verify(audit).log(
                eq("QUOTE_CONVERTED"),
                eq("QUOTE"),
                eq(existing.getBusinessNo()),
                eq("admin"),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                eq("确认转单"),
                eq(request));
    }

    private PublicQuoteRequest request(UUID screenshotId) {
        return new PublicQuoteRequest(
                "  MW4  ",
                "  完成迷彩挑战  ",
                "  10%  ",
                screenshotId,
                "  PC  ",
                "  本周  ",
                "  微信  ",
                "  wx-user  ",
                true,
                "  home  ",
                "  taobao  ",
                "  cpc  ",
                "  autumn  ",
                "  https://example.test/source  ");
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
