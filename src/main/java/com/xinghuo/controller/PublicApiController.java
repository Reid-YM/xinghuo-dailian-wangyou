package com.xinghuo.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.xinghuo.dto.ApiDtos;
import com.xinghuo.dto.PublicQuoteRequest;
import com.xinghuo.exception.BadRequestException;
import com.xinghuo.exception.NotFoundException;
import com.xinghuo.model.CustomerOrder;
import com.xinghuo.model.GameStatus;
import com.xinghuo.model.UploadAsset;
import com.xinghuo.account.model.Account;
import com.xinghuo.account.service.AccountSessionService;
import com.xinghuo.account.service.QuoteContactService;
import com.xinghuo.service.GameStatusService;
import com.xinghuo.service.OrderService;
import com.xinghuo.service.QuoteService;
import com.xinghuo.service.RecordService;
import com.xinghuo.service.ServiceCatalogService;
import com.xinghuo.service.SiteConfigService;
import com.xinghuo.service.SiteSettingService;
import com.xinghuo.service.TaxonomyService;
import com.xinghuo.service.ToolStationService;
import com.xinghuo.service.TrackingService;
import com.xinghuo.service.UploadService;
import com.xinghuo.util.HashUtil;
import com.xinghuo.util.RequestUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.Duration;
import java.util.LinkedHashMap;
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
    private final SiteConfigService config;
    private final ServiceCatalogService services;
    private final QuoteService quotes;
    private final OrderService orders;
    private final RecordService records;
    private final UploadService uploads;
    private final TrackingService tracking;
    private final TaxonomyService taxonomy;
    private final AccountSessionService accountSessions;
    private final ToolStationService toolStation;
    private final GameStatusService gameStatuses;
    private final SiteSettingService siteSettings;
    private final QuoteContactService quoteContacts;

    @GetMapping(value={"/site-config"})
    ApiDtos.SiteConfigResponse config() {
        return this.config.publicConfig();
    }

    @GetMapping(value={"/public/game-status"})
    Map<String, Object> gameStatus(@RequestParam(value="game", required=false) String game) {
        GameStatus s = this.gameStatuses.require(this.resolveGameCode(game));
        LinkedHashMap<String, Object> out = new LinkedHashMap<String, Object>();
        out.put("game", s.getGameCode());
        out.put("status", s.getStatusCode().name().toLowerCase(Locale.ROOT));
        out.put("label", s.getPublicLabel());
        out.put("detail", s.getPublicDetail());
        out.put("ctaLabel", s.getCtaLabel());
        out.put("acceptingOrders", s.getStatusCode().acceptingOrders());
        out.put("effectiveAt", s.getEffectiveAt());
        out.put("updatedAt", s.getUpdatedAt());
        return out;
    }

    @GetMapping(value={"/public/game-status/all"})
    List<Map<String, Object>> gameStatusAll() {
        return this.gameStatuses.all().stream().map(s -> {
            Map<String, Object> m = new LinkedHashMap<String, Object>();
            m.put("game", s.getGameCode());
            m.put("status", s.getStatusCode().name().toLowerCase(Locale.ROOT));
            m.put("label", s.getPublicLabel());
            m.put("ctaLabel", s.getCtaLabel());
            m.put("acceptingOrders", s.getStatusCode().acceptingOrders());
            return m;
        }).toList();
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
        return this.orders.publicCases();
    }

    @GetMapping(value={"/services"})
    List<ApiDtos.ServiceResponse> services() {
        return this.services.publicList();
    }

    @GetMapping(value={"/catalog"})
    ApiDtos.CatalogResponse catalog() {
        return this.taxonomy.catalog(false);
    }

    @GetMapping(value={"/tool-station"})
    JsonNode toolStation() {
        return this.toolStation.publicData();
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
            throw new BadRequestException("\u5f53\u524d\u5df2\u5173\u95ed\u7f51\u9875\u8be2\u4ef7\uff0c\u8bf7\u901a\u8fc7\u5ba2\u670d\u6e20\u9053\u8054\u7cfb");
        }
        Optional<Account> current = this.accountSessions.current(req);
        Long accountId = current.map(Account::getId).orElse(null);
        // 已登录且账号里有可用联系方式时，用它补齐表单没填的部分，避免让用户重复填写；
        // 表单里自己填了就以表单为准，用户可以临时换成别的联系方式。
        PublicQuoteRequest payload = body;
        if (current.isPresent() && this.isBlankContact(body)) {
            Optional<QuoteContactService.StoredContact> preferred = this.quoteContacts.preferred(current.get());
            if (preferred.isPresent()) {
                QuoteContactService.StoredContact contact = preferred.get();
                payload = body.withCustomerContact(contact.value(), quoteContactMethod(contact.type()));
            }
        }
        ApiDtos.QuoteResponse out = this.quotes.create(payload, key, HashUtil.sha256(RequestUtil.clientIp(req)), accountId);
        return ResponseEntity.status((HttpStatusCode)HttpStatus.CREATED).body(out);
    }

    private boolean isBlankContact(PublicQuoteRequest body) {
        return body.customerContact() == null || body.customerContact().isBlank();
    }

    /** 询价表单的「联系方式」取值是中文：微信 / QQ / 手机。 */
    private static String quoteContactMethod(String typeName) {
        if (typeName == null) {
            return null;
        }
        return switch (typeName.toUpperCase(Locale.ROOT)) {
            case "PHONE" -> "\u624b\u673a";
            case "WECHAT" -> "\u5fae\u4fe1";
            case "QQ" -> "QQ";
            default -> null;
        };
    }

    @GetMapping(value={"/records/{businessNo}"})
    ApiDtos.RecordPublicResponse record(@PathVariable String businessNo, HttpServletRequest req) {
        if (!this.config.isOrderQueryEnabled()) {
            throw new BadRequestException("\u8fdb\u5ea6\u67e5\u8be2\u6b63\u5728\u7ef4\u62a4\uff0c\u8bf7\u8054\u7cfb\u5ba2\u670d\u67e5\u8be2");
        }
        return this.records.get(businessNo);
    }

    @GetMapping(value={"/records/{businessNo}/image"})
    ResponseEntity<Resource> recordImage(@PathVariable String businessNo, HttpServletRequest req) {
        if (!this.config.isOrderQueryEnabled()) {
            throw new BadRequestException("\u8fdb\u5ea6\u67e5\u8be2\u6b63\u5728\u7ef4\u62a4\uff0c\u8bf7\u8054\u7cfb\u5ba2\u670d\u67e5\u8be2");
        }
        CustomerOrder o = this.orders.findByPublicNo(businessNo);
        if (o.getProgressImageAssetId() == null) {
            throw new NotFoundException();
        }
        Resource r = this.uploads.load(o.getProgressImageAssetId(), "ORDER", o.getOrderNo());
        return ((ResponseEntity.BodyBuilder)ResponseEntity.ok().contentType(this.uploads.mediaType(o.getProgressImageAssetId())).cacheControl(CacheControl.noStore())).body(r);
    }

    @GetMapping(value={"/orders/{orderNo}"})
    ApiDtos.OrderPublicResponse order(@PathVariable String orderNo, HttpServletRequest req) {
        if (!this.config.isOrderQueryEnabled()) {
            throw new BadRequestException("\u8fdb\u5ea6\u67e5\u8be2\u6b63\u5728\u7ef4\u62a4\uff0c\u8bf7\u8054\u7cfb\u5ba2\u670d\u67e5\u8be2");
        }
        return this.orders.getPublic(orderNo);
    }

    @GetMapping(value={"/orders/{orderNo}/image"})
    ResponseEntity<Resource> orderImage(@PathVariable String orderNo, HttpServletRequest req) {
        if (!this.config.isOrderQueryEnabled()) {
            throw new BadRequestException("\u8fdb\u5ea6\u67e5\u8be2\u6b63\u5728\u7ef4\u62a4\uff0c\u8bf7\u8054\u7cfb\u5ba2\u670d\u67e5\u8be2");
        }
        CustomerOrder o = this.orders.findByPublicNo(orderNo);
        if (o.getProgressImageAssetId() == null) {
            throw new NotFoundException();
        }
        Resource r = this.uploads.load(o.getProgressImageAssetId(), "ORDER", o.getOrderNo());
        return ((ResponseEntity.BodyBuilder)ResponseEntity.ok().contentType(this.uploads.mediaType(o.getProgressImageAssetId())).cacheControl(CacheControl.noStore())).body(r);
    }

    @PostMapping(value={"/track"})
    ResponseEntity<Void> track(@Valid @RequestBody ApiDtos.TrackRequest body, HttpServletRequest req) {
        this.tracking.track(body, HashUtil.sha256(RequestUtil.clientIp(req)));
        return ResponseEntity.noContent().build();
    }

    @Generated
    public PublicApiController(SiteConfigService config, ServiceCatalogService services, QuoteService quotes, OrderService orders, RecordService records, UploadService uploads, TrackingService tracking, TaxonomyService taxonomy, AccountSessionService accountSessions, ToolStationService toolStation, GameStatusService gameStatuses, SiteSettingService siteSettings, com.xinghuo.account.service.QuoteContactService quoteContacts) {
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
        this.gameStatuses = gameStatuses;
        this.siteSettings = siteSettings;
        this.quoteContacts = quoteContacts;
    }
}

