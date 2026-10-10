package com.xinghuo.upload.application;

import java.util.UUID;

public interface UploadPort {
    UploadStored storeQuoteScreenshot(UploadImage image);

    UploadPayload loadPublicSiteAsset(UUID assetId);

    UploadPayload loadOwnedAsset(UUID assetId, String ownerType, String ownerId);
}
