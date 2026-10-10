package com.xinghuo.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.xinghuo.account.model.Account;
import com.xinghuo.account.service.AccountSessionService;
import com.xinghuo.account.service.QuoteContactService;
import com.xinghuo.account.service.RateLimiter;
import com.xinghuo.account.service.RequestUtils;
import com.xinghuo.catalog.application.CatalogApplicationService;
import com.xinghuo.catalog.web.CatalogWebMapper;
import com.xinghuo.dto.ApiDtos;
import com.xinghuo.dto.PublicQuoteRequest;
import com.xinghuo.exception.BadRequestException;
import com.xinghuo.exception.NotFoundException;
import com.xinghuo.gamestatus.application.GameStatusApplicationService;
import com.xinghuo.gamestatus.web.GameStatusWebMapper;
import com.xinghuo.model.UploadAsset;
import com.xinghuo.order.application.OrderApplicationService;
import com.xinghuo.order.domain.OrderProgressAsset;
import com.xinghuo.order.web.OrderWebMapper;
import com.xinghuo.quote.application.QuoteApplicationService;
import com.xinghuo.quote.web.QuoteWebMapper;
import com.xinghuo.record.application.RecordApplicationService;
import com.xinghuo.record.web.RecordWebMapper;
import com.xinghuo.service.SiteConfigService;
import com.xinghuo.service.UploadService;
import com.xinghuo.sitesetting.application.SiteSettingApplicationService;
import com.xinghuo.taxonomy.application.TaxonomyApplicationService;
import com.xinghuo.taxonomy.web.TaxonomyWebMapper;
import com.xinghuo.toolstation.application.ToolStationApplicationService;
import com.xinghuo.toolstation.web.ToolStationWebMapper;
import com.xinghuo.tracking.application.TrackingApplicationService;
import com.xinghuo.tracking.web.TrackingWebMapper;
import com.xinghuo.util.HashUtil;
import com.xinghuo.util.RequestUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.Generated;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
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
    private final SiteConfigService config;
    private final CatalogApplicationService services;
    private final QuoteApplicationService quotes;
    private final OrderApplicationService orders;
    private final RecordApplicationService records;
    private final UploadService uploads;
    private final TrackingApplicationService tracking;
    private final TaxonomyApplicationService taxonomy;
    private final AccountSessionService accountSessions;
    private final ToolStationApplicationService toolStation;
    private final ToolStationWebMapper toolStationMapper;
    private final GameStatusApplicationService gameStatuses;
    private final SiteSettingApplicationService siteSettings;
    private final RateLimiter rateLimiter;
    private final QuoteContactService quoteContacts;

    @GetMapping(value={"/site-config"})
    ApiDtos.SiteConfigResponse config() {
        return this.config.publicConfig();
    }

    @GetMapping(value={"/public/game-status"})
    Map<String, Object> gameStatus(@RequestParam(value="game", required=false) String game) {
        return GameStatusWebMapper.toPublicResponse(
                this.gameStatuses.require(this.resolveGameCode(game)));
    }

    @GetMapping(value={"/public/game-status/all"})
    List<Map<String, Object>> gameStatusAll() {
        return this.gameStatuses.all().stream()
                .map(GameStatusWebMapper::toSummaryResponse)
                .toList();
    }

    private String resolveGameCode(String raw) {
        if (raw == null || raw.isBlank()) {
            return "CODMW4";
        }
        String v = raw.trim();
        try {
            return this.taxonomy.publicGameBySeoSlug(v.toLowerCase(Locale.ROOT)).getCode();
        }
        catch (Exception ignore) {
            return v.toUpperCase(Locale.ROOT);
        }
    }

    @GetMapping(value={"/public/site-settings"})
    Map<String, String> siteSettings() {
        return this.siteSettings.publicSettings();
    }

    @GetMapping(value={"/public/cases"})
    List<Map<String, Object>> publicCases() {
        return OrderWebMapper.toCaseResponses(this.orders.publicCases());
    }

    @GetMapping(value={"/services"})
    List<ApiDtos.ServiceResponse> services() {
        return CatalogWebMapper.toResponses(this.services.publicList());
    }

    @GetMapping(value={"/catalog"})
    ApiDtos.CatalogResponse catalog() {
        return TaxonomyWebMapper.toResponse(this.taxonomy.catalog(false));
    }

    @GetMapping(value={"/tool-station"})
    JsonNode toolStation() {
        return this.toolStationMapper.toResponse(this.toolStation.publicData());
    }

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
        if (!this.config.isQuoteEnabled()) {
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
        if (!this.config.isOrderQueryEnabled()) {
            throw new BadRequestException("进度查询正在维护，请联系客服查询");
        }
        this.limitByIp(req, "pub:record");
        return RecordWebMapper.toResponse(this.records.get(businessNo));
    }

    @GetMapping(value={"/records/{businessNo}/image"})
    ResponseEntity<Resource> recordImage(@PathVariable String businessNo, HttpServletRequest req) {
        if (!this.config.isOrderQueryEnabled()) {
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
        if (!this.config.isOrderQueryEnabled()) {
            throw new BadRequestException("进度查询正在维护，请联系客服查询");
        }
        this.limitByIp(req, "pub:order");
        return OrderWebMapper.toResponse(this.orders.getPublic(orderNo));
    }

    @GetMapping(value={"/orders/{orderNo}/image"})
    ResponseEntity<Resource> orderImage(@PathVariable String orderNo, HttpServletRequest req) {
        if (!this.config.isOrderQueryEnabled()) {
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

    @PostMapping(value={"/track"})
    ResponseEntity<Void> track(@Valid @RequestBody ApiDtos.TrackRequest body, HttpServletRequest req) {
        this.tracking.track(TrackingWebMapper.toCommand(
                body, HashUtil.sha256(RequestUtil.clientIp(req))));
        return ResponseEntity.noContent().build();
    }

    @Generated
    public PublicApiController(SiteConfigService config, CatalogApplicationService services, QuoteApplicationService quotes, OrderApplicationService orders, RecordApplicationService records, UploadService uploads, TrackingApplicationService tracking, TaxonomyApplicationService taxonomy, AccountSessionService accountSessions, ToolStationApplicationService toolStation, ToolStationWebMapper toolStationMapper, GameStatusApplicationService gameStatuses, SiteSettingApplicationService siteSettings, RateLimiter rateLimiter, com.xinghuo.account.service.QuoteContactService quoteContacts) {
        this.config = config;
        this.services = services;
        this.quotes = quotes;
        this.orders = orders;
        this.records = records;
        this.uploads = uploads;
        this.tracking = tracking;
        this.taxonomy = taxonomy;
        this.accountSessions = accountSessions;
        this.toolStation = toolStation;
        this.toolStationMapper = toolStationMapper;
        this.gameStatuses = gameStatuses;
        this.siteSettings = siteSettings;
        this.rateLimiter = rateLimiter;
        this.quoteContacts = quoteContacts;
    }
}
