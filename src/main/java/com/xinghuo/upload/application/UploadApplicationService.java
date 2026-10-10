package com.xinghuo.upload.application;

import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class UploadApplicationService {
    private final UploadPort port;

    public UploadApplicationService(UploadPort port) {
        this.port = port;
    }

    public UploadStored storeQuoteScreenshot(byte[] content, String originalName) {
        return port.storeQuoteScreenshot(new UploadImage(content, originalName));
    }

    public UploadPayload loadPublicSiteAsset(UUID assetId) {
        return port.loadPublicSiteAsset(assetId);
    }

    public UploadPayload loadOwnedAsset(UUID assetId, String ownerType, String ownerId) {
        return port.loadOwnedAsset(assetId, ownerType, ownerId);
    }
}
