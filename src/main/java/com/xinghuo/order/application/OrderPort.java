package com.xinghuo.order.application;

import com.xinghuo.order.domain.OrderProgressAsset;
import java.util.List;
import java.util.UUID;

/** Order-owned persistence and integration boundary. */
public interface OrderPort {
    List<PublicCaseView> publicCases();

    OrderPublicView getPublic(String publicNo);

    OrderProgressAsset progressAsset(String publicNo);

    List<AdminOrderView> list();

    AdminOrderView create(CreateOrderCommand command, String operator);

    AdminOrderView createFromQuote(OrderFromQuoteCommand command, String operator);

    AdminOrderView update(String orderNo, UpdateOrderCommand command, String operator);

    AdminOrderView attachImage(String orderNo, UUID assetId, String operator);

    PublicCaseSettingsView setPublicCase(
            String orderNo, boolean enabled, String summary, String operator);

    void delete(String orderNo, String operator);
}
