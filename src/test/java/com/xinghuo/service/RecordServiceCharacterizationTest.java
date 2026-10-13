package com.xinghuo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.xinghuo.dto.ApiDtos;
import com.xinghuo.exception.NotFoundException;
import com.xinghuo.model.CustomerOrder;
import com.xinghuo.model.Quote;
import com.xinghuo.repository.CustomerOrderRepository;
import com.xinghuo.repository.QuoteRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RecordServiceCharacterizationTest {

    private final CustomerOrderRepository orders = mock(CustomerOrderRepository.class);
    private final QuoteRepository quotes = mock(QuoteRepository.class);
    private final RecordService service = new RecordService(orders, quotes);

    @Test
    void invalidNumberIsRejectedBeforeAnyRepositoryLookup() {
        assertThrows(NotFoundException.class, () -> service.get("not-a-business-number"));

        verifyNoInteractions(orders, quotes);
    }

    @Test
    void businessNumberIsNormalizedAndOrderWinsBeforeQuoteFallback() {
        String businessNo = "XHABC1234567";
        Instant createdAt = Instant.parse("2026-10-10T01:00:00Z");
        Instant updatedAt = Instant.parse("2026-10-10T02:00:00Z");
        CustomerOrder order = mock(CustomerOrder.class);
        when(order.getBusinessNo()).thenReturn(businessNo);
        when(order.getStatus()).thenReturn(CustomerOrder.Status.IN_PROGRESS);
        when(order.getProgressPercent()).thenReturn(45);
        when(order.getProgressText()).thenReturn("正在处理中");
        when(order.getCreatedAt()).thenReturn(createdAt);
        when(order.getUpdatedAt()).thenReturn(updatedAt);
        when(order.getCustomerNote()).thenReturn("客户可见备注");
        when(order.getSourceQuoteId()).thenReturn(null);
        when(order.getProgressImageAssetId()).thenReturn(null);
        when(orders.findByBusinessNo(businessNo)).thenReturn(Optional.of(order));

        ApiDtos.RecordPublicResponse response = service.get("  xhabc1234567  ");

        assertEquals(businessNo, response.businessNo());
        assertEquals("ORDER", response.stage());
        assertEquals("IN_PROGRESS", response.status());
        assertEquals("服务订单", response.title());
        assertNull(response.expectedTime());
        assertEquals(45, response.progressPercent());
        assertEquals("正在处理中", response.progressText());
        assertEquals(createdAt, response.createdAt());
        assertEquals(updatedAt, response.updatedAt());
        assertFalse(response.hasProgressImage());
        assertEquals("客户可见备注", response.customerNote());
        verify(orders).findByBusinessNo(businessNo);
        verifyNoInteractions(quotes);
    }

    @Test
    void businessNumberFallsBackToQuoteWhenNoOrderExists() {
        String businessNo = "XHABC1234567";
        Instant createdAt = Instant.parse("2026-10-10T03:00:00Z");
        Instant updatedAt = Instant.parse("2026-10-10T04:00:00Z");
        Quote quote = mock(Quote.class);
        when(quote.getBusinessNo()).thenReturn(businessNo);
        when(quote.getStatus()).thenReturn(Quote.Status.QUOTED);
        when(quote.getGame()).thenReturn("CODMW4");
        when(quote.getDemand()).thenReturn("迷彩任务");
        when(quote.getExpectedTime()).thenReturn("2 小时");
        when(quote.getCreatedAt()).thenReturn(createdAt);
        when(quote.getUpdatedAt()).thenReturn(updatedAt);
        when(orders.findByBusinessNo(businessNo)).thenReturn(Optional.empty());
        when(quotes.findByBusinessNo(businessNo)).thenReturn(Optional.of(quote));

        ApiDtos.RecordPublicResponse response = service.get(businessNo);

        assertEquals("QUOTE", response.stage());
        assertEquals("QUOTED", response.status());
        assertEquals("CODMW4 · 迷彩任务", response.title());
        assertEquals("2 小时", response.expectedTime());
        assertNull(response.progressPercent());
        assertEquals("已完成报价，请与客服确认方案", response.progressText());
        assertFalse(response.hasProgressImage());
        assertNull(response.customerNote());
        verify(orders).findByBusinessNo(businessNo);
        verify(quotes).findByBusinessNo(businessNo);
    }

    @Test
    void legacyOrderNumberUsesLegacyLookupAndDoesNotFallBackToQuote() {
        String legacyNo = "XH-" + "A".repeat(24);
        when(orders.findByOrderNo(legacyNo)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.get(legacyNo.toLowerCase()));

        verify(orders).findByOrderNo(legacyNo);
        verifyNoInteractions(quotes);
    }

    @Test
    void orderWithSourceQuoteUsesQuoteTitleExpectedTimeAndImageFlag() {
        String businessNo = "XHABC1234567";
        UUID sourceQuoteId = UUID.fromString("11111111-2222-3333-4444-555555555555");
        UUID imageId = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
        CustomerOrder order = mock(CustomerOrder.class);
        Quote quote = mock(Quote.class);
        when(order.getBusinessNo()).thenReturn(businessNo);
        when(order.getStatus()).thenReturn(CustomerOrder.Status.COMPLETED);
        when(order.getSourceQuoteId()).thenReturn(sourceQuoteId);
        when(order.getProgressImageAssetId()).thenReturn(imageId);
        when(quote.getGame()).thenReturn("CODMW4");
        when(quote.getDemand()).thenReturn("排位任务");
        when(quote.getExpectedTime()).thenReturn("今晚完成");
        when(orders.findByBusinessNo(businessNo)).thenReturn(Optional.of(order));
        when(quotes.findById(sourceQuoteId)).thenReturn(Optional.of(quote));

        ApiDtos.RecordPublicResponse response = service.get(businessNo);

        assertEquals("CODMW4 · 排位任务", response.title());
        assertEquals("今晚完成", response.expectedTime());
        assertTrue(response.hasProgressImage());
        verify(quotes).findById(sourceQuoteId);
    }
}
