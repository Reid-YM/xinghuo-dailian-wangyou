package com.xinghuo.order.web;

import com.xinghuo.account.service.RateLimiter;
import com.xinghuo.account.service.RequestUtils;
import com.xinghuo.dto.ApiDtos;
import com.xinghuo.exception.BadRequestException;
import com.xinghuo.exception.NotFoundException;
import com.xinghuo.order.application.OrderApplicationService;
import com.xinghuo.order.domain.OrderProgressAsset;
import com.xinghuo.siteconfig.application.SiteRuntimeApplicationService;
import com.xinghuo.upload.infrastructure.UploadService;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import lombok.Generated;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value={"/api"})
public class PublicOrderController {
    private static final int REQUEST_BUCKET_PER_MINUTE = 60;
    private final SiteRuntimeApplicationService runtimeConfig;
    private final OrderApplicationService orders;
    private final UploadService uploads;
    private final RateLimiter rateLimiter;

    @GetMapping(value={"/orders/{orderNo}"})
    ApiDtos.OrderPublicResponse order(@PathVariable String orderNo, HttpServletRequest req) {
        this.requireOrderQueryEnabled();
        this.limitByIp(req, "pub:order");
        return OrderWebMapper.toResponse(this.orders.getPublic(orderNo));
    }

    @GetMapping(value={"/records/{businessNo}/image"})
    ResponseEntity<Resource> recordImage(@PathVariable String businessNo, HttpServletRequest req) {
        this.requireOrderQueryEnabled();
        this.limitByIp(req, "pub:recordimg");
        return this.progressImage(businessNo);
    }

    @GetMapping(value={"/orders/{orderNo}/image"})
    ResponseEntity<Resource> orderImage(@PathVariable String orderNo, HttpServletRequest req) {
        this.requireOrderQueryEnabled();
        this.limitByIp(req, "pub:orderimg");
        return this.progressImage(orderNo);
    }

    private ResponseEntity<Resource> progressImage(String businessNo) {
        OrderProgressAsset asset = this.orders.progressAsset(businessNo);
        if (asset.assetId() == null) {
            throw new NotFoundException();
        }
        Resource r = this.uploads.load(asset.assetId(), "ORDER", asset.storageOwnerId());
        return ResponseEntity.ok()
                .contentType(this.uploads.mediaType(asset.assetId()))
                .cacheControl(CacheControl.noStore())
                .body(r);
    }

    private void requireOrderQueryEnabled() {
        if (!this.runtimeConfig.orderQueryEnabled()) {
            throw new BadRequestException("进度查询正在维护，请联系客服查询");
        }
    }

    private void limitByIp(HttpServletRequest req, String bucket) {
        String ip = RequestUtils.clientIp(req);
        this.rateLimiter.check(bucket + ":" + (ip == null ? "unknown" : ip),
                REQUEST_BUCKET_PER_MINUTE, Duration.ofMinutes(1L));
    }

    @Generated
    public PublicOrderController(SiteRuntimeApplicationService runtimeConfig,
                                 OrderApplicationService orders,
                                 UploadService uploads,
                                 RateLimiter rateLimiter) {
        this.runtimeConfig = runtimeConfig;
        this.orders = orders;
        this.uploads = uploads;
        this.rateLimiter = rateLimiter;
    }
}
