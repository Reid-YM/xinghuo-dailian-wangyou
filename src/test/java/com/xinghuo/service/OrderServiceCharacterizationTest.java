package com.xinghuo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.xinghuo.dto.ApiDtos;
import com.xinghuo.exception.ConflictException;
import com.xinghuo.model.CustomerOrder;
import com.xinghuo.repository.CustomerOrderRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class OrderServiceCharacterizationTest {

    @Test
    void createGeneratesCanonicalBusinessNumberAndNormalizesNotes() {
        CustomerOrderRepository repo = mock(CustomerOrderRepository.class);
        when(repo.findByBusinessNo(anyString())).thenReturn(Optional.empty());
        when(repo.findByOrderNo(anyString())).thenReturn(Optional.empty());
        OrderService service = new OrderService(repo, mock(UploadService.class), mock(AuditService.class));

        service.create(
                new ApiDtos.CreateOrderRequest(
                        "IN_PROGRESS",
                        35,
                        "  正在处理  ",
                        "  用户备注  ",
                        "   ",
                        "手工建单"),
                "admin",
                mock(HttpServletRequest.class));

        ArgumentCaptor<CustomerOrder> captor = ArgumentCaptor.forClass(CustomerOrder.class);
        verify(repo).save(captor.capture());
        CustomerOrder saved = captor.getValue();
        assertTrue(saved.getBusinessNo().matches("XH[A-Z0-9]{10}"));
        assertEquals(saved.getBusinessNo(), saved.getOrderNo());
        assertEquals(CustomerOrder.Status.IN_PROGRESS, saved.getStatus());
        assertEquals(35, saved.getProgressPercent());
        assertEquals("正在处理", saved.getProgressText());
        assertEquals("用户备注", saved.getCustomerNote());
        assertNull(saved.getInternalNote());
        assertNotNull(saved.getId());
    }

    @Test
    void staleVersionRejectsUpdateBeforeMutatingOrder() {
        CustomerOrderRepository repo = mock(CustomerOrderRepository.class);
        CustomerOrder existing = order(CustomerOrder.Status.IN_PROGRESS, 25, 3L);
        when(repo.findByOrderNo(existing.getOrderNo())).thenReturn(Optional.of(existing));
        OrderService service = new OrderService(repo, mock(UploadService.class), mock(AuditService.class));

        assertThrows(ConflictException.class, () -> service.update(
                existing.getOrderNo(),
                new ApiDtos.UpdateOrderRequest(
                        2L,
                        "COMPLETED",
                        null,
                        "完成",
                        null,
                        null,
                        "旧页面提交"),
                "admin",
                mock(HttpServletRequest.class)));

        assertEquals(CustomerOrder.Status.IN_PROGRESS, existing.getStatus());
        assertEquals(25, existing.getProgressPercent());
        assertNull(existing.getFinishedAt());
        verify(repo, never()).flush();
    }

    @Test
    void omittedInternalNotePreservesHistoryWhenProgressIsUpdated() {
        CustomerOrderRepository repo = mock(CustomerOrderRepository.class);
        CustomerOrder existing = order(CustomerOrder.Status.IN_PROGRESS, 25, 3L);
        existing.setInternalNote("历史内部处理备注");
        when(repo.findByOrderNo(existing.getOrderNo())).thenReturn(Optional.of(existing));
        OrderService service = new OrderService(repo, mock(UploadService.class), mock(AuditService.class));

        service.update(existing.getOrderNo(), new ApiDtos.UpdateOrderRequest(
                3L, "IN_PROGRESS", 45, "更新履约进度", null, null, "更新进度"),
                "admin", mock(HttpServletRequest.class));

        assertEquals(45, existing.getProgressPercent());
        assertEquals("更新履约进度", existing.getProgressText());
        assertEquals("历史内部处理备注", existing.getInternalNote());
        verify(repo).flush();
    }

    @Test
    void completingOrderWithoutPercentDefaultsToHundredAndSetsFinishedAt() {
        CustomerOrderRepository repo = mock(CustomerOrderRepository.class);
        CustomerOrder existing = order(CustomerOrder.Status.IN_PROGRESS, 80, 7L);
        when(repo.findByOrderNo(existing.getOrderNo())).thenReturn(Optional.of(existing));
        OrderService service = new OrderService(repo, mock(UploadService.class), mock(AuditService.class));

        ApiDtos.AdminOrderResponse response = service.update(
                existing.getOrderNo(),
                new ApiDtos.UpdateOrderRequest(
                        7L,
                        "COMPLETED",
                        null,
                        "  已完成  ",
                        "  已交付  ",
                        "  内部完成  ",
                        "完成订单"),
                "admin",
                mock(HttpServletRequest.class));

        assertEquals(CustomerOrder.Status.COMPLETED, existing.getStatus());
        assertEquals(100, existing.getProgressPercent());
        assertEquals("已完成", existing.getProgressText());
        assertEquals("已交付", existing.getCustomerNote());
        assertEquals("内部完成", existing.getInternalNote());
        assertNotNull(existing.getFinishedAt());
        assertEquals("COMPLETED", response.status());
        assertEquals(100, response.progressPercent());
        verify(repo).flush();
    }

    private CustomerOrder order(CustomerOrder.Status status, Integer progressPercent, long version) {
        Instant now = Instant.parse("2026-10-12T00:00:00Z");
        String businessNo = "XHABCDEF2345";
        return CustomerOrder.builder()
                .id(java.util.UUID.randomUUID())
                .orderNo(businessNo)
                .businessNo(businessNo)
                .status(status)
                .progressPercent(progressPercent)
                .progressText("处理中")
                .createdAt(now)
                .updatedAt(now)
                .version(version)
                .build();
    }
}
