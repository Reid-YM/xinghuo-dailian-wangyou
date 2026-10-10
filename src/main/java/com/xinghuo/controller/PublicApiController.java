package com.xinghuo.controller;

import com.xinghuo.account.service.RateLimiter;
import com.xinghuo.account.service.RequestUtils;
import com.xinghuo.dto.ApiDtos;
import com.xinghuo.exception.BadRequestException;
import com.xinghuo.exception.NotFoundException;
import com.xinghuo.model.UploadAsset;
import com.xinghuo.order.application.OrderApplicationService;
import com.xinghuo.order.domain.OrderProgressAsset;
import com.xinghuo.order.web.OrderWebMapper;
import com.xinghuo.siteconfig.application.SiteRuntimeApplicationService;
import com.xinghuo.upload.infrastructure.UploadService;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.util.UUID;
import lombok.Generated;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping(value={"/api"})
public class PublicApiController {
    private static final int REQUEST_BUCKET_PER_MINUTE = 60;
    private final SiteRuntimeApplicationService runtimeConfig;
    private final OrderApplicationService orders;
    private final UploadService uploads;
    private final RateLimiter rateLimiter;

    @GetMapping(value={"/site-assets/{assetId}"})
    ResponseEntity<Resource> siteAsset(@PathVariable UUID assetId) {
        Resource r = this.uploads.loadPublicSiteAsset(assetId);
        return ((ResponseEntity.BodyBuilder)ResponseEntity.ok().contentType(this.uploads.mediaType(assetId)).cacheControl(CacheControl.maxAge((Duration)Duration.ofDays(7L)).cachePublic())).body(r);
    }

    @PostMapping(value={"/uploads/quote"})
    ApiDtos.UploadResponse quoteUpload(@RequestParam(value="file") MultipartFile file) {
        return new ApiDtos.UploadResponse(this.uploads.storeImage(file, UploadAsset.Kind.QUOTE_SCREENSHOT).getId());
    }

    @GetMapping(value={"/records/{businessNo}/image"})
    ResponseEntity<Resource> recordImage(@PathVariable String businessNo, HttpServletRequest req) {
        if (!this.runtimeConfig.orderQueryEnabled()) {
            throw new BadRequestException("进度查询正在维护，请联系客服查询");
        }
        this.limitByIp(req, "pub:recordimg");
        OrderProgressAsset asset = this.orders.progressAsset(businessNo);
        if (asset.assetId() == null) {
            throw new NotFoundException();
        }
        Resource r = this.uploads.load(asset.assetId(), "ORDER", asset.storageOwnerId());
        return ((ResponseEntity.BodyBuilder)ResponseEntity.ok().contentType(this.uploads.mediaType(asset.assetId())).cacheControl(CacheControl.noStore())).body(r);
    }

    @GetMapping(value={"/orders/{orderNo}"})
    ApiDtos.OrderPublicResponse order(@PathVariable String orderNo, HttpServletRequest req) {
        if (!this.runtimeConfig.orderQueryEnabled()) {
            throw new BadRequestException("进度查询正在维护，请联系客服查询");
        }
        this.limitByIp(req, "pub:order");
        return OrderWebMapper.toResponse(this.orders.getPublic(orderNo));
    }

    @GetMapping(value={"/orders/{orderNo}/image"})
    ResponseEntity<Resource> orderImage(@PathVariable String orderNo, HttpServletRequest req) {
        if (!this.runtimeConfig.orderQueryEnabled()) {
            throw new BadRequestException("进度查询正在维护，请联系客服查询");
        }
        this.limitByIp(req, "pub:orderimg");
        OrderProgressAsset asset = this.orders.progressAsset(orderNo);
        if (asset.assetId() == null) {
            throw new NotFoundException();
        }
        Resource r = this.uploads.load(asset.assetId(), "ORDER", asset.storageOwnerId());
        return ((ResponseEntity.BodyBuilder)ResponseEntity.ok().contentType(this.uploads.mediaType(asset.assetId())).cacheControl(CacheControl.noStore())).body(r);
    }

    private void limitByIp(HttpServletRequest req, String bucket) {
        String ip = RequestUtils.clientIp(req);
        this.rateLimiter.check(bucket + ":" + (ip == null ? "unknown" : ip),
                REQUEST_BUCKET_PER_MINUTE, Duration.ofMinutes(1L));
    }

    @Generated
    public PublicApiController(SiteRuntimeApplicationService runtimeConfig,
                               OrderApplicationService orders,
                               UploadService uploads,
                               RateLimiter rateLimiter) {
        this.runtimeConfig = runtimeConfig;
        this.orders = orders;
        this.uploads = uploads;
        this.rateLimiter = rateLimiter;
    }
}
