package com.xinghuo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.xinghuo.dto.ApiDtos;
import com.xinghuo.model.CustomerOrder;
import com.xinghuo.model.Quote;
import com.xinghuo.repository.CustomerOrderRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

class OrderServiceExtendedCharacterizationTest {

    @Test
    void createFromQuoteReturnsExistingOrderWithoutCreatingOrAuditingAgain() {
        CustomerOrderRepository repo = mock(CustomerOrderRepository.class);
        UploadService uploads = mock(UploadService.class);
        AuditService audit = mock(AuditService.class);
        OrderService service = new OrderService(repo, uploads, audit);

        UUID quoteId = UUID.randomUUID();
        Quote quote = Quote.builder()
                .id(quoteId)
                .businessNo("XHQUOTE23456")
                .game("CODMW4")
                .demand("武器升级")
                .customerAccountId(88L)
                .build();
        CustomerOrder existing = order("XHEXIST23456", CustomerOrder.Status.WAITING_CONFIRMATION);
        existing.setSourceQuoteId(quoteId);
        when(repo.findBySourceQuoteId(quoteId)).thenReturn(Optional.of(existing));

        ApiDtos.AdminOrderResponse response = service.createFromQuote(
                quote,
                "admin",
                mock(HttpServletRequest.class),
                "再次转单");

        assertEquals(existing.getBusinessNo(), response.businessNo());
        verify(repo, never()).save(any(CustomerOrder.class));
        verify(audit, never()).log(
                any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void freshQuoteOrderIsFlushedBeforeGuestJdbcCanLinkIt() {
        CustomerOrderRepository repo = mock(CustomerOrderRepository.class);
        AuditService audit = mock(AuditService.class);
        OrderService service = new OrderService(repo, mock(UploadService.class), audit);
        UUID quoteId = UUID.randomUUID();
        Quote quote = Quote.builder()
                .id(quoteId)
                .businessNo("XHNEW2345678")
                .game("CODMW4")
                .demand("测试需求")
                .build();
        when(repo.findBySourceQuoteId(quoteId)).thenReturn(Optional.empty());

        service.createFromQuote(quote, "guest:1", mock(HttpServletRequest.class), "游客接受报价");

        // Subsequent JDBC ownership writes in GuestQuoteFlowService must find the saved order.
        InOrder writes = inOrder(repo, audit);
        writes.verify(repo).save(any(CustomerOrder.class));
        writes.verify(repo).flush();
        writes.verify(audit).log(any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void attachImageBindsAssetToOrderAndMarksResponseAsHavingImage() {
        CustomerOrderRepository repo = mock(CustomerOrderRepository.class);
        UploadService uploads = mock(UploadService.class);
        AuditService audit = mock(AuditService.class);
        OrderService service = new OrderService(repo, uploads, audit);

        CustomerOrder existing = order("XHIMAGE23456", CustomerOrder.Status.IN_PROGRESS);
        when(repo.findByOrderNo(existing.getOrderNo())).thenReturn(Optional.of(existing));
        UUID assetId = UUID.randomUUID();

        ApiDtos.AdminOrderResponse response = service.attachImage(
                existing.getOrderNo(),
                assetId,
                "admin",
                mock(HttpServletRequest.class));

        verify(uploads).bind(assetId, "ORDER", existing.getOrderNo());
        assertEquals(assetId, existing.getProgressImageAssetId());
        assertTrue(response.hasProgressImage());
    }

    @Test
    void publicCasesAreMaskedAndCappedAtTwelve() {
        CustomerOrderRepository repo = mock(CustomerOrderRepository.class);
        OrderService service = new OrderService(repo, mock(UploadService.class), mock(AuditService.class));
        List<CustomerOrder> rows = new ArrayList<>();
        for (int i = 0; i < 13; i++) {
            CustomerOrder order = order("XHCASE" + String.format("%06d", i), CustomerOrder.Status.COMPLETED);
            order.setAllowPublicCase(true);
            order.setPublicSummary("案例" + i);
            order.setFinishedAt(Instant.parse("2026-10-12T00:00:00Z").minusSeconds(i));
            rows.add(order);
        }
        when(repo.findByAllowPublicCaseTrueOrderByFinishedAtDescUpdatedAtDesc()).thenReturn(rows);

        List<java.util.Map<String, Object>> result = service.publicCases();

        assertEquals(12, result.size());
        assertEquals("XH****00", result.get(0).get("maskedNo"));
        assertEquals("案例0", result.get(0).get("summary"));
        assertEquals("completed", result.get(0).get("status"));
    }

    private CustomerOrder order(String businessNo, CustomerOrder.Status status) {
        Instant now = Instant.parse("2026-10-12T00:00:00Z");
        return CustomerOrder.builder()
                .id(UUID.randomUUID())
                .orderNo(businessNo)
                .businessNo(businessNo)
                .status(status)
                .progressPercent(status == CustomerOrder.Status.COMPLETED ? 100 : 50)
                .progressText(status == CustomerOrder.Status.COMPLETED ? "已完成" : "处理中")
                .createdAt(now)
                .updatedAt(now)
                .version(0L)
                .build();
    }
}
