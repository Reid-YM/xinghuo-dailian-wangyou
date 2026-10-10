package com.xinghuo.order.application;

import com.xinghuo.order.domain.OrderProgressAsset;
import java.util.List;
import java.util.UUID;

/** Temporary migration port while order behavior is moved out of the legacy service. */
public interface OrderLegacyPort {
    List<PublicCaseView> publicCases();

    OrderPublicView getPublic(String publicNo);

    OrderProgressAsset progressAsset(String publicNo);

    List<OrderAdminView> adminList();

    OrderAdminView create(OrderCreateCommand command, String operator);

    OrderAdminView update(String orderNo, OrderUpdateCommand command, String operator);

    OrderAdminView attachImage(String orderNo, UUID assetId, String operator);

    void delete(String orderNo, String operator);

    OrderPublicCaseAdminView setPublicCase(String orderNo, boolean enabled, String summary, String operator);
}
