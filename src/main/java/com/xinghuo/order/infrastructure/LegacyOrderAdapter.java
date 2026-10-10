package com.xinghuo.order.infrastructure;

import com.xinghuo.dto.ApiDtos;
import com.xinghuo.model.CustomerOrder;
import com.xinghuo.order.application.OrderAdminView;
import com.xinghuo.order.application.OrderCreateCommand;
import com.xinghuo.order.application.OrderLegacyPort;
import com.xinghuo.order.application.OrderPublicCaseAdminView;
import com.xinghuo.order.application.OrderPublicView;
import com.xinghuo.order.application.OrderUpdateCommand;
import com.xinghuo.order.application.PublicCaseView;
import com.xinghuo.order.domain.OrderProgressAsset;
import com.xinghuo.service.OrderService;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/** Bridges application-owned order contracts to the existing order service during migration. */
@Component
public class LegacyOrderAdapter implements OrderLegacyPort {
    private final OrderService legacy;

    public LegacyOrderAdapter(OrderService legacy) {
        this.legacy = legacy;
    }

    @Override
    public List<PublicCaseView> publicCases() {
        return legacy.publicCases().stream().map(this::publicCase).toList();
    }

    @Override
    public OrderPublicView getPublic(String publicNo) {
        ApiDtos.OrderPublicResponse response = legacy.getPublic(publicNo);
        return new OrderPublicView(
                response.businessNo(),
                response.status(),
                response.progressPercent(),
                response.progressText(),
                response.updatedAt(),
                response.hasProgressImage(),
                response.customerNote());
    }

    @Override
    public OrderProgressAsset progressAsset(String publicNo) {
        CustomerOrder order = legacy.findByPublicNo(publicNo);
        return new OrderProgressAsset(order.getProgressImageAssetId(), order.getOrderNo());
    }

    @Override
    public List<OrderAdminView> adminList() {
        return legacy.list().stream().map(this::adminView).toList();
    }

    @Override
    public OrderAdminView create(OrderCreateCommand command, String operator) {
        return adminView(legacy.create(toLegacy(command), operator, currentRequest()));
    }

    @Override
    public OrderAdminView update(String orderNo, OrderUpdateCommand command, String operator) {
        return adminView(legacy.update(orderNo, toLegacy(command), operator, currentRequest()));
    }

    @Override
    public OrderAdminView attachImage(String orderNo, UUID assetId, String operator) {
        return adminView(legacy.attachImage(orderNo, assetId, operator, currentRequest()));
    }

    @Override
    public void delete(String orderNo, String operator) {
        legacy.delete(orderNo, operator, currentRequest());
    }

    @Override
    public OrderPublicCaseAdminView setPublicCase(String orderNo, boolean enabled, String summary, String operator) {
        Map<String, Object> response = legacy.setPublicCase(orderNo, enabled, summary, operator);
        return new OrderPublicCaseAdminView(
                (String) response.get("orderNo"),
                Boolean.TRUE.equals(response.get("allowPublicCase")),
                (String) response.get("publicSummary"));
    }

    private ApiDtos.CreateOrderRequest toLegacy(OrderCreateCommand source) {
        return new ApiDtos.CreateOrderRequest(
                source.status(),
                source.progressPercent(),
                source.progressText(),
                source.customerNote(),
                source.internalNote(),
                source.reason());
    }

    private ApiDtos.UpdateOrderRequest toLegacy(OrderUpdateCommand source) {
        return new ApiDtos.UpdateOrderRequest(
                source.version(),
                source.status(),
                source.progressPercent(),
                source.progressText(),
                source.customerNote(),
                source.internalNote(),
                source.reason());
    }

    private OrderAdminView adminView(ApiDtos.AdminOrderResponse source) {
        return new OrderAdminView(
                source.id(),
                source.orderNo(),
                source.businessNo(),
                source.sourceQuoteId(),
                source.status(),
                source.progressPercent(),
                source.progressText(),
                source.hasProgressImage(),
                source.customerNote(),
                source.internalNote(),
                source.updatedAt(),
                source.version());
    }

    private PublicCaseView publicCase(Map<String, Object> source) {
        return new PublicCaseView(
                (String) source.get("maskedNo"),
                (String) source.get("summary"),
                (Instant) source.get("finishedAt"),
                (String) source.get("status"));
    }

    private HttpServletRequest currentRequest() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes servlet) {
            return servlet.getRequest();
        }
        return null;
    }
}
