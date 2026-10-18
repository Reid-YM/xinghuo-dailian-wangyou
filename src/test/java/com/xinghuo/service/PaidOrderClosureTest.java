package com.xinghuo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.xinghuo.dto.ApiDtos;
import com.xinghuo.exception.ConflictException;
import com.xinghuo.model.CustomerOrder;
import com.xinghuo.repository.CustomerOrderRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

class PaidOrderClosureTest {
    private final CustomerOrderRepository repo = mock(CustomerOrderRepository.class);
    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final OrderService service = new OrderService(
            repo, mock(UploadService.class), mock(AuditService.class), jdbc);
    private CustomerOrder order;

    @BeforeEach
    void setUp() {
        order = CustomerOrder.builder()
                .id(UUID.randomUUID())
                .orderNo("XH-ABCDEFGHJKLMNPQRSTUVWXYZ")
                .businessNo("XHABCDEFGHJK")
                .status(CustomerOrder.Status.WAITING_CONFIRMATION)
                .progressText("等待客服确认")
                .updatedAt(Instant.now())
                .createdAt(Instant.now())
                .version(0)
                .build();
        when(repo.findByOrderNo(order.getOrderNo())).thenReturn(Optional.of(order));
    }

    private ApiDtos.UpdateOrderRequest change(String status, Integer progress) {
        return new ApiDtos.UpdateOrderRequest(0L, status, progress,
                "履约进度由工作人员更新", null, null, "客服核对并确认");
    }

    @Test
    void deliveryCompletionAndClosureStaySeparateFromWeChatCapture() {
        when(jdbc.queryForObject(anyString(), eq(Integer.class), eq(order.getBusinessNo())))
                .thenReturn(1, 1, 1, 1);
        service.update(order.getOrderNo(), change("IN_PROGRESS", 20), "staff", null);
        assertEquals(CustomerOrder.Status.IN_PROGRESS, order.getStatus());
        assertNull(order.getFinishedAt());

        service.update(order.getOrderNo(), change("COMPLETED", 80), "staff", null);
        assertEquals(CustomerOrder.Status.COMPLETED, order.getStatus());
        assertEquals(100, order.getProgressPercent());
        assertNotNull(order.getFinishedAt());
        Instant finished = order.getFinishedAt();

        service.update(order.getOrderNo(), change("CLOSED", null), "staff", null);
        assertEquals(CustomerOrder.Status.CLOSED, order.getStatus());
        assertEquals(100, order.getProgressPercent());
        assertEquals(finished, order.getFinishedAt());
        assertThrows(ConflictException.class,
                () -> service.update(order.getOrderNo(), change("IN_PROGRESS", 10), "staff", null));
    }

    @Test
    void cannotCloseAnOrderBeforeServiceCompletion() {
        assertThrows(ConflictException.class,
                () -> service.update(order.getOrderNo(), change("CLOSED", 100), "staff", null));
        assertEquals(CustomerOrder.Status.WAITING_CONFIRMATION, order.getStatus());
    }

    @Test
    void existingButUnpaidWeChatPaymentMustNeverCountAsCompleted() {
        when(jdbc.queryForObject(anyString(), eq(Integer.class), eq(order.getBusinessNo())))
                .thenReturn(1, 0);
        assertThrows(ConflictException.class,
                () -> service.update(order.getOrderNo(), change("COMPLETED", 100), "staff", null));
        assertEquals(CustomerOrder.Status.WAITING_CONFIRMATION, order.getStatus());
    }

    @Test
    void historicalOfflineOrdersMayStillCompleteWithoutOnlinePayment() {
        when(jdbc.queryForObject(anyString(), eq(Integer.class), eq(order.getBusinessNo())))
                .thenReturn(0);
        service.update(order.getOrderNo(), change("COMPLETED", null), "staff", null);
        assertEquals(CustomerOrder.Status.COMPLETED, order.getStatus());
        service.update(order.getOrderNo(), change("CLOSED", null), "staff", null);
        assertEquals(CustomerOrder.Status.CLOSED, order.getStatus());
    }
}
