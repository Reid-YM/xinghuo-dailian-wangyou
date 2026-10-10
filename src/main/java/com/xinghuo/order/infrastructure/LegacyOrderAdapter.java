package com.xinghuo.order.infrastructure;

import com.xinghuo.dto.ApiDtos;
import com.xinghuo.model.CustomerOrder;
import com.xinghuo.order.application.OrderLegacyPort;
import com.xinghuo.order.domain.OrderProgressAsset;
import com.xinghuo.service.OrderService;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/** Bridges the new order application boundary to the existing order service. */
@Component
public class LegacyOrderAdapter implements OrderLegacyPort {
    private final OrderService legacy;

    public LegacyOrderAdapter(OrderService legacy) {
        this.legacy = legacy;
    }

    @Override
    public List<Map<String, Object>> publicCases() {
        return legacy.publicCases();
    }

    @Override
    public ApiDtos.OrderPublicResponse getPublic(String publicNo) {
        return legacy.getPublic(publicNo);
    }

    @Override
    public OrderProgressAsset progressAsset(String publicNo) {
        CustomerOrder order = legacy.findByPublicNo(publicNo);
        return new OrderProgressAsset(order.getProgressImageAssetId(), order.getOrderNo());
    }
}
