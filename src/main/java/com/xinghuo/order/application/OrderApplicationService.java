package com.xinghuo.order.application;

import com.xinghuo.order.domain.OrderProgressAsset;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

/** Application boundary for public order/query and administration use cases. */
@Service
public class OrderApplicationService {
    private final OrderLegacyPort legacy;

    public OrderApplicationService(OrderLegacyPort legacy) {
        this.legacy = legacy;
    }

    public List<PublicCaseView> publicCases() {
        return legacy.publicCases();
    }

    public OrderPublicView getPublic(String publicNo) {
        return legacy.getPublic(publicNo);
    }

    public OrderProgressAsset progressAsset(String publicNo) {
        return legacy.progressAsset(publicNo);
    }

    public List<OrderAdminView> adminList() {
        return legacy.adminList();
    }

    public OrderAdminView create(OrderCreateCommand command, String operator) {
        return legacy.create(command, operator);
    }

    public OrderAdminView update(String orderNo, OrderUpdateCommand command, String operator) {
        return legacy.update(orderNo, command, operator);
    }

    public OrderAdminView attachImage(String orderNo, UUID assetId, String operator) {
        return legacy.attachImage(orderNo, assetId, operator);
    }

    public void delete(String orderNo, String operator) {
        legacy.delete(orderNo, operator);
    }

    public OrderPublicCaseAdminView setPublicCase(String orderNo, boolean enabled, String summary, String operator) {
        return legacy.setPublicCase(orderNo, enabled, summary, operator);
    }
}
