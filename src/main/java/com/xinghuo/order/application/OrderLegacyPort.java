package com.xinghuo.order.application;

import com.xinghuo.order.domain.OrderProgressAsset;
import java.util.List;

/** Temporary migration port for public order reads. */
public interface OrderLegacyPort {
    List<PublicCaseView> publicCases();

    OrderPublicView getPublic(String publicNo);

    OrderProgressAsset progressAsset(String publicNo);
}
