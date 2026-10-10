package com.xinghuo.controller;

import com.xinghuo.account.model.Account;
import com.xinghuo.account.service.AccountSessionService;
import com.xinghuo.account.service.QuoteContactService;
import com.xinghuo.account.service.RateLimiter;
import com.xinghuo.account.service.RequestUtils;
import com.xinghuo.dto.ApiDtos;
import com.xinghuo.dto.PublicQuoteRequest;
import com.xinghuo.exception.BadRequestException;
import com.xinghuo.exception.NotFoundException;
import com.xinghuo.model.UploadAsset;
import com.xinghuo.order.application.OrderApplicationService;
import com.xinghuo.order.domain.OrderProgressAsset;
import com.xinghuo.order.web.OrderWebMapper;
import com.xinghuo.quote.application.QuoteApplicationService;
import com.xinghuo.quote.web.QuoteWebMapper;
import com.xinghuo.record.application.RecordApplicationService;
import com.xinghuo.record.web.RecordWebMapper;
import com.xinghuo.siteconfig.application.SiteRuntimeApplicationService;
import com.xinghuo.upload.infrastructure.UploadService;
import com.xinghuo.util.HashUtil;
import com.xinghuo.util.RequestUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.Duration;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import lombok.Generated;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping(value={"/api"})
public class PublicApiController {
    private static final int REQUEST_BUCKET_PER_MINUTE = 60;
    private final SiteRuntimeApplicationService runtimeConfig;
    private final QuoteApplicationService quotes;
    private final OrderApplicationService orders;
    private final RecordApplicationService records;
    private final UploadService uploads;
    private final AccountSessionService accountSessions;
    private final RateLimiter rateLimiter;
    private final QuoteContactService quoteContacts;

    @GetMapping(value={"/site-assets/{assetId}"})
    ResponseEntity<Resource> siteAsset(@PathVariable UUID assetId) {
        Resource r = this.uploads.loadPublicSiteAsset(assetId);
        return ((ResponseEntity.BodyBuilder)ResponseEntity.ok().contentType(this.uploads.mediaType(assetId)).cacheControl(CacheControl.maxAge((Duration)Duration.ofDays(7L)).cachePublic())).body(r);
    }

    @PostMapping(value={"/uploads/quote"})
    ApiDtos.UploadResponse quoteUpload(@RequestParam(value="file") MultipartFile file) {
        return new ApiDtos.UploadResponse(this.uploads.storeImage(file, UploadAsset.Kind.QUOTE_SCREENSHOT).getId());
    }

    @PostMapping(value={"/quote"})
    ResponseEntity<ApiDtos.QuoteResponse> quote(@Valid @RequestBody PublicQuoteRequest body, @RequestHeader(value="Idempotency-Key") String key, HttpServletRequest req) {
        if (!this.runtimeConfig.quoteEnabled()) {
            throw new BadRequestException("当前已关闭网页询价，请通过客服渠道联系");
        }
        Optional<Account> current = this.accountSessions.current(req);
        Long accountId = current.map(Account::getId).orElse(null);
        PublicQuoteRequest payload = body;
        if (current.isPresent() && this.isBlankContact(body)) {
            Optional<QuoteContactService.StoredContact> preferred = this.quoteContacts.preferred(current.get());
            if (preferred.isPresent()) {
                QuoteContactService.StoredContact contact = preferred.get();
                payload = body.withCustomerContact(contact.value(), quoteContactMethod(contact.type()));
            }
        }
        ApiDtos.QuoteResponse out = QuoteWebMapper.toResponse(this.quotes.create(QuoteWebMapper.toCommand(
                payload,
                key,
                HashUtil.sha256(RequestUtil.clientIp(req)),
                accountId)));
        return ResponseEntity.status((HttpStatusCode)HttpStatus.CREATED).body(out);
    }

    private boolean isBlankContact(PublicQuoteRequest body) {
        return body.customerContact() == null || body.customerContact().isBlank();
    }

    private static String quoteContactMethod(String typeName) {
        if (typeName == null) {
            return null;
        }
        return switch (typeName.toUpperCase(Locale.ROOT)) {
            case "PHONE" -> "手机";
            case "WECHAT" -> "微信";
            case "QQ" -> "QQ";
            default -> null;
        };
    }

    @GetMapping(value={"/records/{businessNo}"})
    ApiDtos.RecordPublicResponse record(@PathVariable String businessNo, HttpServletRequest req) {
        if (!this.runtimeConfig.orderQueryEnabled()) {
            throw new BadRequestException("进度查询正在维护，请联系客服查询");
        }
        this.limitByIp(req, "pub:record");
        return RecordWebMapper.toResponse(this.records.get(businessNo));
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
    public PublicApiController(SiteRuntimeApplicationService runtimeConfig, QuoteApplicationService quotes, OrderApplicationService orders, RecordApplicationService records, UploadService uploads, AccountSessionService accountSessions, RateLimiter rateLimiter, com.xinghuo.account.service.QuoteContactService quoteContacts) {
        this.runtimeConfig = runtimeConfig;
        this.quotes = quotes;
        this.orders = orders;
        this.records = records;
        this.uploads = uploads;
        this.accountSessions = accountSessions;
        this.rateLimiter = rateLimiter;
        this.quoteContacts = quoteContacts;
    }
}
