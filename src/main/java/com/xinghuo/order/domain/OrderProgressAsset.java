package com.xinghuo.order.domain;

import java.util.UUID;

/**
 * Minimal domain projection required by the public progress-image endpoints.
 * It deliberately hides the persistence entity from the web layer.
 */
public record OrderProgressAsset(UUID assetId, String storageOwnerId) {
}
