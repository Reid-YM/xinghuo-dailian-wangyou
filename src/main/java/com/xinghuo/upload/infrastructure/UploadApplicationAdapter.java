package com.xinghuo.upload.infrastructure;

import com.xinghuo.exception.NotFoundException;
import com.xinghuo.model.UploadAsset;
import com.xinghuo.upload.application.UploadImage;
import com.xinghuo.upload.application.UploadPayload;
import com.xinghuo.upload.application.UploadPort;
import com.xinghuo.upload.application.UploadStored;
import java.io.IOException;
import java.util.UUID;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

@Component
public class UploadApplicationAdapter implements UploadPort {
    private final UploadService uploads;

    public UploadApplicationAdapter(UploadService uploads) {
        this.uploads = uploads;
    }

    @Override
    public UploadStored storeQuoteScreenshot(UploadImage image) {
        UploadAsset stored = uploads.storeImage(image.content(), UploadAsset.Kind.QUOTE_SCREENSHOT);
        return new UploadStored(stored.getId());
    }

    @Override
    public UploadPayload loadPublicSiteAsset(UUID assetId) {
        return payload(assetId, uploads.loadPublicSiteAsset(assetId));
    }

    @Override
    public UploadPayload loadOwnedAsset(UUID assetId, String ownerType, String ownerId) {
        return payload(assetId, uploads.load(assetId, ownerType, ownerId));
    }

    private UploadPayload payload(UUID assetId, Resource resource) {
        try (var input = resource.getInputStream()) {
            return new UploadPayload(input.readAllBytes(), uploads.mediaType(assetId).toString());
        }
        catch (IOException e) {
            throw new NotFoundException();
        }
    }
}
