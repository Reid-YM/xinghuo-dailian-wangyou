package com.xinghuo.order.application;

import com.xinghuo.order.domain.OrderProgressAsset;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

/** Application boundary for public order queries and administrative order operations. */
@Service
public class OrderApplicationService {
    private final OrderPort port;

    public OrderApplicationService(OrderPort port) {
        this.port = port;
    }

    public List<PublicCaseView> publicCases() {
        return port.publicCases();
    }

    public OrderPublicView getPublic(String publicNo) {
        return port.getPublic(publicNo);
    }

    public OrderProgressAsset progressAsset(String publicNo) {
        return port.progressAsset(publicNo);
    }

    public List<AdminOrderView> list() {
        return port.list();
    }

    public AdminOrderView create(CreateOrderCommand command, String operator) {
        return port.create(command, operator);
    }

    public AdminOrderView createFromQuote(OrderFromQuoteCommand command, String operator) {
        return port.createFromQuote(command, operator);
    }

    public AdminOrderView update(String orderNo, UpdateOrderCommand command, String operator) {
        return port.update(orderNo, command, operator);
    }

    public AdminOrderView attachImage(String orderNo, UUID assetId, String operator) {
        return port.attachImage(orderNo, assetId, operator);
    }

    public PublicCaseSettingsView setPublicCase(
            String orderNo, boolean enabled, String summary, String operator) {
        return port.setPublicCase(orderNo, enabled, summary, operator);
    }

    public void delete(String orderNo, String operator) {
        port.delete(orderNo, operator);
    }
}
