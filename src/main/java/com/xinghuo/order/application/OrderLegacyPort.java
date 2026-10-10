package com.xinghuo.order.application;

import com.xinghuo.dto.ApiDtos;
import com.xinghuo.order.domain.OrderProgressAsset;
import java.util.List;
import java.util.Map;

/** Temporary migration port for public order reads. */
public interface OrderLegacyPort {
    List<Map<String, Object>> publicCases();

    ApiDtos.OrderPublicResponse getPublic(String publicNo);

    OrderProgressAsset progressAsset(String publicNo);
}
