package com.xinghuo.order.infrastructure;

import com.xinghuo.dto.ApiDtos;
import com.xinghuo.model.CustomerOrder;
import com.xinghuo.order.application.OrderLegacyPort;
import com.xinghuo.order.application.OrderPublicView;
import com.xinghuo.order.application.PublicCaseView;
import com.xinghuo.order.domain.OrderProgressAsset;
import com.xinghuo.service.OrderService;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/** Bridges application-owned order views to the existing order service. */
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

    private PublicCaseView publicCase(Map<String, Object> source) {
        return new PublicCaseView(
                (String) source.get("maskedNo"),
                (String) source.get("summary"),
                (Instant) source.get("finishedAt"),
                (String) source.get("status"));
    }
}
