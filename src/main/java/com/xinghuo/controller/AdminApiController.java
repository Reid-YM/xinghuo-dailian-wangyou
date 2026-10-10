package com.xinghuo.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.xinghuo.catalog.application.CatalogApplicationService;
import com.xinghuo.catalog.web.CatalogAdminWebMapper;
import com.xinghuo.dashboard.application.DashboardApplicationService;
import com.xinghuo.dashboard.web.DashboardWebMapper;
import com.xinghuo.dto.ApiDtos;
import com.xinghuo.exception.BadRequestException;
import com.xinghuo.exception.NotFoundException;
import com.xinghuo.feedback.application.FeedbackApplicationService;
import com.xinghuo.feedback.web.FeedbackWebMapper;
import com.xinghuo.gamestatus.application.GameStatusApplicationService;
import com.xinghuo.gamestatus.web.GameStatusWebMapper;
import com.xinghuo.model.UploadAsset;
import com.xinghuo.order.application.OrderApplicationService;
import com.xinghuo.order.web.OrderWebMapper;
import com.xinghuo.quote.application.QuoteApplicationService;
import com.xinghuo.quote.web.QuoteWebMapper;
import com.xinghuo.repository.AuditLogRepository;
import com.xinghuo.service.SiteConfigService;
import com.xinghuo.service.UploadService;
import com.xinghuo.siteconfig.application.SiteConfigPublicApplicationService;
import com.xinghuo.siteconfig.web.SiteConfigWebMapper;
import com.xinghuo.slot.application.SlotApplicationService;
import com.xinghuo.slot.web.SlotWebMapper;
import com.xinghuo.taxonomy.application.TaxonomyApplicationService;
import com.xinghuo.taxonomy.web.TaxonomyWebMapper;
import com.xinghuo.toolstation.application.ToolStationApplicationService;
import com.xinghuo.toolstation.web.ToolStationWebMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import lombok.Generated;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping(value={"/api/admin"})
public class AdminApiController {
    private final DashboardApplicationService dashboard;
    private final SiteConfigService config;
    private final SiteConfigPublicApplicationService siteConfigQuery;
    private final CatalogApplicationService services;
    private final QuoteApplicationService quotes;
    private final OrderApplicationService orders;
    private final SlotApplicationService slots;
    private final UploadService uploads;
    private final AuditLogRepository audits;
    private final TaxonomyApplicationService taxonomy;
    private final FeedbackApplicationService feedback;

    private final ToolStationApplicationService toolStation;
    private final ToolStationWebMapper toolStationMapper;
    private final GameStatusApplicationService gameStatuses;

    @GetMapping(value={"/dashboard"})
    ApiDtos.DashboardResponse dashboard() {
        return DashboardWebMapper.toResponse(this.dashboard.get());
    }

    @GetMapping(value={"/site-config"})
    ApiDtos.SiteConfigResponse getConfig() {
        return SiteConfigWebMapper.toResponse(this.siteConfigQuery.get());
    }

    @PutMapping(value={"/game-status/{gameCode}"})
    Map<String, Object> updateGameStatus(@PathVariable String gameCode, @RequestBody Map<String, Object> body, Authentication auth, HttpServletRequest request) {
        return GameStatusWebMapper.toAdminResponse(
                this.gameStatuses.update(GameStatusWebMapper.toCommand(
                        gameCode,
                        body,
                        auth == null ? "admin" : auth.getName())));
    }

    @PatchMapping(value={"/orders/{orderNo}/public-case"})
    Map<String, Object> setPublicCase(@PathVariable String orderNo, @RequestBody Map<String, Object> body, Authentication auth) {
        boolean enabled = Boolean.parseBoolean(String.valueOf(body.getOrDefault("enabled", "false")));
        String summary = this.str(body.get("summary"));
        return OrderWebMapper.toResponse(this.orders.setPublicCase(
                orderNo, enabled, summary, auth == null ? "admin" : auth.getName()));
    }

    private String str(Object v) {
        return v == null ? null : String.valueOf(v);
    }

    @PatchMapping(value={"/site-config/section/{section}"})
    ApiDtos.SiteConfigResponse updateConfigSection(@PathVariable String section, @RequestBody Map<String, Object> body, Authentication a, HttpServletRequest req) {
        return this.config.updateSection(section, body, a.getName(), req);
    }

    @PatchMapping(value={"/site-config"})
    ApiDtos.SiteConfigResponse updateConfig(@Valid @RequestBody ApiDtos.SiteConfigUpdate r, Authentication a, HttpServletRequest req) {
        return this.config.update(r, a.getName(), req);
    }

    @PostMapping(value={"/site-config/image/{slot}"})
    ApiDtos.SiteImageUploadResponse uploadSiteImage(@PathVariable String slot, @RequestParam(value="file") MultipartFile file, Authentication a, HttpServletRequest req) {
        String key;
        UploadAsset.Kind kind = switch (key = slot.trim().toUpperCase(Locale.ROOT)) {
            case "HERO" -> UploadAsset.Kind.SITE_HERO;
            case "BACKGROUND" -> UploadAsset.Kind.SITE_BACKGROUND;
            case "BACKGROUND_MOBILE" -> UploadAsset.Kind.SITE_BACKGROUND;
            case "WECHAT_QR" -> UploadAsset.Kind.SITE_WECHAT_QR;
            case "CONTACT_QR" -> UploadAsset.Kind.SITE_WECHAT_QR;
            default -> throw new BadRequestException("非法图片位置");
        };
        UploadAsset asset = this.uploads.storeImage(file, kind);
        this.uploads.bind(asset.getId(), "SITE_CONFIG", key);
        String url = "/api/site-assets/" + String.valueOf(asset.getId());
        if (!"CONTACT_QR".equals(key)) {
            this.config.updateSiteImage(key, url, a.getName(), req);
        }
        return new ApiDtos.SiteImageUploadResponse(url);
    }

    @DeleteMapping(value={"/site-config/image/{slot}"})
    ApiDtos.SiteConfigResponse clearSiteImage(@PathVariable String slot, Authentication a, HttpServletRequest req) {
        return this.config.updateSiteImage(slot, null, a.getName(), req);
    }

    @PostMapping(value={"/services/cover-image"})
    ApiDtos.SiteImageUploadResponse uploadServiceCover(@RequestParam(value="file") MultipartFile file) {
        UploadAsset asset = this.uploads.storeImage(file, UploadAsset.Kind.SERVICE_COVER);
        return new ApiDtos.SiteImageUploadResponse("/api/site-assets/" + String.valueOf(asset.getId()));
    }

    @PostMapping(value={"/tool-station/image"})
    ApiDtos.SiteImageUploadResponse uploadToolStationImage(@RequestParam(value="file") MultipartFile file) {
        UploadAsset asset = this.uploads.storeImage(file, UploadAsset.Kind.TOOL_STATION_COVER);
        return new ApiDtos.SiteImageUploadResponse("/api/site-assets/" + String.valueOf(asset.getId()));
    }

    @GetMapping(value={"/catalog"})
    ApiDtos.CatalogResponse catalog() {
        return TaxonomyWebMapper.toResponse(this.taxonomy.catalog(true));
    }

    @PostMapping(value={"/catalog/games"})
    ApiDtos.GameOptionResponse addGame(@Valid @RequestBody ApiDtos.CatalogNameRequest r, Authentication a, HttpServletRequest req) {
        return TaxonomyWebMapper.toResponse(
                this.taxonomy.addGame(TaxonomyWebMapper.toCommand(r), a.getName()));
    }

    @PostMapping(value={"/catalog/categories"})
    ApiDtos.CategoryOptionResponse addCategory(@Valid @RequestBody ApiDtos.CategoryCreateRequest r, Authentication a, HttpServletRequest req) {
        return TaxonomyWebMapper.toResponse(
                this.taxonomy.addCategory(TaxonomyWebMapper.toCommand(r), a.getName()));
    }

    @PutMapping(value={"/catalog/games/{code}"})
    ApiDtos.GameOptionResponse updateGame(@PathVariable String code, @Valid @RequestBody ApiDtos.CatalogUpdateRequest r, Authentication a, HttpServletRequest req) {
        return TaxonomyWebMapper.toResponse(
                this.taxonomy.updateGame(code, TaxonomyWebMapper.toCommand(r), a.getName()));
    }

    @PutMapping(value={"/catalog/games/{code}/profile"})
    ApiDtos.GameOptionResponse updateGameProfile(@PathVariable String code, @Valid @RequestBody ApiDtos.GameProfileUpdateRequest r, Authentication a, HttpServletRequest req) {
        return TaxonomyWebMapper.toResponse(
                this.taxonomy.updateGameProfile(code, TaxonomyWebMapper.toCommand(r), a.getName()));
    }

    @PatchMapping(value={"/catalog/games/bulk"})
    List<ApiDtos.GameOptionResponse> bulkUpdateGames(@Valid @RequestBody ApiDtos.GameBulkUpdateRequest r, Authentication a, HttpServletRequest req) {
        return TaxonomyWebMapper.toGameResponses(
                this.taxonomy.bulkUpdateGames(TaxonomyWebMapper.toCommand(r), a.getName()));
    }

    @PostMapping(value={"/catalog/games/image/{kind}"})
    ApiDtos.SiteImageUploadResponse uploadGameImage(@PathVariable String kind, @RequestParam(value="file") MultipartFile file) {
        UploadAsset.Kind imageKind = switch (kind.trim().toUpperCase(Locale.ROOT)) {
            case "LOGO" -> UploadAsset.Kind.GAME_LOGO;
            case "COVER" -> UploadAsset.Kind.GAME_COVER;
            default -> throw new BadRequestException("非法游戏图片类型");
        };
        UploadAsset asset = this.uploads.storeImage(file, imageKind);
        return new ApiDtos.SiteImageUploadResponse("/api/site-assets/" + String.valueOf(asset.getId()));
    }

    @DeleteMapping(value={"/catalog/games/{code}"})
    @PreAuthorize(value="hasRole('ADMIN')")
    ResponseEntity<Void> deleteGame(@PathVariable String code, Authentication a, HttpServletRequest req) {
        this.taxonomy.deleteGame(code, a.getName());
        return ResponseEntity.noContent().build();
    }

    @PutMapping(value={"/catalog/categories/{id}"})
    ApiDtos.CategoryOptionResponse updateCategory(@PathVariable Long id, @Valid @RequestBody ApiDtos.CategoryUpdateRequest r, Authentication a, HttpServletRequest req) {
        return TaxonomyWebMapper.toResponse(
                this.taxonomy.updateCategory(id, TaxonomyWebMapper.toCommand(r), a.getName()));
    }

    @DeleteMapping(value={"/catalog/categories/{id}"})
    @PreAuthorize(value="hasRole('ADMIN')")
    ResponseEntity<Void> deleteCategory(@PathVariable Long id, Authentication a, HttpServletRequest req) {
        this.taxonomy.deleteCategory(id, a.getName());
        return ResponseEntity.noContent().build();
    }

    @GetMapping(value={"/services"})
    List<ApiDtos.AdminServiceResponse> listServices() {
        return CatalogAdminWebMapper.toResponses(this.services.adminList());
    }

    @PostMapping(value={"/services"})
    ApiDtos.AdminServiceResponse createService(@Valid @RequestBody ApiDtos.ServiceUpsert r, Authentication a, HttpServletRequest req) {
        return CatalogAdminWebMapper.toResponse(this.services.create(CatalogAdminWebMapper.toCommand(r), a.getName()));
    }

    @PutMapping(value={"/services/{id}"})
    ApiDtos.AdminServiceResponse updateService(@PathVariable Long id, @Valid @RequestBody ApiDtos.ServiceUpsert r, Authentication a, HttpServletRequest req) {
        return CatalogAdminWebMapper.toResponse(this.services.update(id, CatalogAdminWebMapper.toCommand(r), a.getName()));
    }

    @DeleteMapping(value={"/services/{id}"})
    @PreAuthorize(value="hasRole('ADMIN')")
    ResponseEntity<Void> deleteService(@PathVariable Long id, Authentication a, HttpServletRequest req) {
        this.services.delete(id, a.getName());
        return ResponseEntity.noContent().build();
    }

    @GetMapping(value={"/tool-station"})
    JsonNode toolStation() {
        return this.toolStationMapper.toResponse(this.toolStation.publicData());
    }

    @PutMapping(value={"/tool-station"})
    JsonNode saveToolStation(@RequestBody JsonNode data) {
        return this.toolStationMapper.toResponse(
                this.toolStation.save(this.toolStationMapper.toJson(data)));
    }

    @PostMapping(value={"/tool-station/reset"})
    JsonNode resetToolStation() {
        return this.toolStationMapper.toResponse(this.toolStation.reset());
    }

    @PostMapping(value={"/services/{id}/restore"})
    ApiDtos.AdminServiceResponse restoreService(@PathVariable Long id, Authentication a, HttpServletRequest req) {
        return CatalogAdminWebMapper.toResponse(this.services.restore(id, a.getName()));
    }

    @GetMapping(value={"/quotes"})
    List<ApiDtos.QuoteAdminResponse> quotes() {
        return QuoteWebMapper.toAdminResponses(this.quotes.list());
    }

    @PatchMapping(value={"/quotes/{id}/status"})
    ApiDtos.QuoteAdminResponse quoteStatus(@PathVariable UUID id, @Valid @RequestBody ApiDtos.QuoteStatusUpdate r, Authentication a, HttpServletRequest req) {
        return QuoteWebMapper.toResponse(
                this.quotes.updateStatus(id, QuoteWebMapper.toCommand(r), a.getName()));
    }

    @DeleteMapping(value={"/quotes/{id}"})
    @PreAuthorize(value="hasRole('ADMIN')")
    ResponseEntity<Void> deleteQuote(@PathVariable UUID id, Authentication a, HttpServletRequest req) {
        this.quotes.delete(id, a.getName());
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value={"/quotes/{id}/convert"})
    ApiDtos.AdminOrderResponse convertQuote(@PathVariable UUID id, @Valid @RequestBody ApiDtos.QuoteConvertRequest r, Authentication a, HttpServletRequest req) {
        return QuoteWebMapper.toResponse(
                this.quotes.convertToOrder(id, QuoteWebMapper.toCommand(r), a.getName()));
    }

    @GetMapping(value={"/quotes/{id}/image"})
    ResponseEntity<Resource> quoteImage(@PathVariable UUID id) {
        var q = this.quotes.find(id);
        if (q.screenshotAssetId() == null) {
            throw new NotFoundException();
        }
        return ResponseEntity.ok().contentType(this.uploads.mediaType(q.screenshotAssetId())).cacheControl(CacheControl.noStore()).body(this.uploads.load(q.screenshotAssetId(), "QUOTE", q.id().toString()));
    }

    @GetMapping(value={"/orders"})
    List<ApiDtos.AdminOrderResponse> orders() {
        return OrderWebMapper.toAdminResponses(this.orders.list());
    }

    @PostMapping(value={"/orders"})
    ApiDtos.AdminOrderResponse createOrder(@Valid @RequestBody ApiDtos.CreateOrderRequest r, Authentication a, HttpServletRequest req) {
        return OrderWebMapper.toResponse(
                this.orders.create(OrderWebMapper.toCommand(r), a.getName()));
    }

    @PatchMapping(value={"/orders/{orderNo}"})
    ApiDtos.AdminOrderResponse updateOrder(@PathVariable String orderNo, @Valid @RequestBody ApiDtos.UpdateOrderRequest r, Authentication a, HttpServletRequest req) {
        return OrderWebMapper.toResponse(
                this.orders.update(orderNo, OrderWebMapper.toCommand(r), a.getName()));
    }

    @PostMapping(value={"/orders/{orderNo}/image"})
    ApiDtos.AdminOrderResponse orderImage(@PathVariable String orderNo, @RequestParam(value="file") MultipartFile file, Authentication a, HttpServletRequest req) {
        UploadAsset x = this.uploads.storeImage(file, UploadAsset.Kind.ORDER_PROGRESS);
        return OrderWebMapper.toResponse(this.orders.attachImage(orderNo, x.getId(), a.getName()));
    }

    @DeleteMapping(value={"/orders/{orderNo}"})
    @PreAuthorize(value="hasRole('ADMIN')")
    ResponseEntity<Void> deleteOrder(@PathVariable String orderNo, Authentication a, HttpServletRequest req) {
        this.orders.delete(orderNo, a.getName());
        return ResponseEntity.noContent().build();
    }

    @GetMapping(value={"/slots"})
    List<ApiDtos.SlotResponse> slots() {
        return SlotWebMapper.toSlotResponses(this.slots.list());
    }

    @PatchMapping(value={"/slots"})
    ApiDtos.SlotResponse updateSlot(@Valid @RequestBody ApiDtos.SlotUpdate r, Authentication a, HttpServletRequest req) {
        return SlotWebMapper.toResponse(
                this.slots.update(SlotWebMapper.toCommand(r), a.getName()));
    }

    @GetMapping(value={"/audits"})
    List<ApiDtos.AuditResponse> audits() {
        return this.audits.findTop100ByOrderByCreatedAtDesc().stream().map(x -> new ApiDtos.AuditResponse(x.getId(), x.getAction(), x.getTargetType(), x.getTargetId(), x.getOperatorName(), x.getReason(), x.getIpAddress(), x.getCreatedAt())).toList();
    }

    @GetMapping(value={"/test-feedback"})
    List<ApiDtos.TestFeedbackResponse> feedback() {
        return FeedbackWebMapper.toResponses(this.feedback.list());
    }

    @PostMapping(value={"/test-feedback"})
    ApiDtos.TestFeedbackResponse createFeedback(@Valid @RequestBody ApiDtos.TestFeedbackCreateRequest r, Authentication a, HttpServletRequest req) {
        return FeedbackWebMapper.toResponse(
                this.feedback.create(FeedbackWebMapper.toCommand(r), a.getName()));
    }

    @PutMapping(value={"/test-feedback/{id}"})
    ApiDtos.TestFeedbackResponse updateFeedback(@PathVariable Long id, @Valid @RequestBody ApiDtos.TestFeedbackUpdateRequest r, Authentication a, HttpServletRequest req) {
        return FeedbackWebMapper.toResponse(
                this.feedback.update(id, FeedbackWebMapper.toCommand(r), a.getName()));
    }

    @PostMapping(value={"/test-feedback/{id}/attachment"})
    ApiDtos.TestFeedbackResponse uploadFeedbackAttachment(@PathVariable Long id, @RequestParam(value="file") MultipartFile file, Authentication a, HttpServletRequest req) {
        UploadAsset asset = this.uploads.storeImage(file, UploadAsset.Kind.TEST_FEEDBACK_ATTACHMENT);
        this.uploads.bind(asset.getId(), "TEST_FEEDBACK", String.valueOf(id));
        return FeedbackWebMapper.toResponse(this.feedback.attach(id, asset.getId(), a.getName()));
    }

    @GetMapping(value={"/test-feedback/{id}/attachment"})
    ResponseEntity<Resource> feedbackAttachment(@PathVariable Long id) {
        var item = this.feedback.get(id);
        if (item.attachmentAssetId() == null) {
            throw new NotFoundException();
        }
        return ResponseEntity.ok().contentType(this.uploads.mediaType(item.attachmentAssetId())).cacheControl(CacheControl.noStore()).body(this.uploads.load(item.attachmentAssetId(), "TEST_FEEDBACK", String.valueOf(id)));
    }

    @Generated
    public AdminApiController(DashboardApplicationService dashboard, SiteConfigService config, SiteConfigPublicApplicationService siteConfigQuery, CatalogApplicationService services, QuoteApplicationService quotes, OrderApplicationService orders, SlotApplicationService slots, UploadService uploads, AuditLogRepository audits, TaxonomyApplicationService taxonomy, FeedbackApplicationService feedback, ToolStationApplicationService toolStation, ToolStationWebMapper toolStationMapper, GameStatusApplicationService gameStatuses) {
        this.dashboard = dashboard;
        this.config = config;
        this.siteConfigQuery = siteConfigQuery;
        this.services = services;
        this.quotes = quotes;
        this.orders = orders;
        this.slots = slots;
        this.uploads = uploads;
        this.audits = audits;
        this.taxonomy = taxonomy;
        this.feedback = feedback;

        this.toolStation = toolStation;
        this.toolStationMapper = toolStationMapper;
        this.gameStatuses = gameStatuses;
    }
}
