package com.xinghuo.order.application;

import com.xinghuo.order.domain.OrderProgressAsset;
import java.util.List;
import org.springframework.stereotype.Service;

/** Application boundary for public order/query use cases. */
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
}
