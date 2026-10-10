package com.xinghuo.upload.web;

import com.xinghuo.upload.infrastructure.UploadService;
import java.time.Duration;
import java.util.UUID;
import lombok.Generated;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value={"/api/site-assets"})
public class PublicSiteAssetController {
    private final UploadService uploads;

    @GetMapping(value={"/{assetId}"})
    ResponseEntity<Resource> siteAsset(@PathVariable UUID assetId) {
        Resource r = this.uploads.loadPublicSiteAsset(assetId);
        return ResponseEntity.ok()
                .contentType(this.uploads.mediaType(assetId))
                .cacheControl(CacheControl.maxAge(Duration.ofDays(7L)).cachePublic())
                .body(r);
    }

    @Generated
    public PublicSiteAssetController(UploadService uploads) {
        this.uploads = uploads;
    }
}
