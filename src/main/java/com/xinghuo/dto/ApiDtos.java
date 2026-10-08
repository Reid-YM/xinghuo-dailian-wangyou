package com.xinghuo.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class ApiDtos {
    private ApiDtos() {
    }

    public record AdminOrderResponse(UUID id, String orderNo, String businessNo, UUID sourceQuoteId, String status, Integer progressPercent, String progressText, boolean hasProgressImage, String customerNote, String internalNote, Instant updatedAt, long version) {
    }

    public record AdminServiceResponse(Long id, String slug, String gameCode, String name, String category, String shortDesc, String priceMode, BigDecimal priceFixed, BigDecimal priceMin, BigDecimal priceMax, String durationText, String status, String coverImage, BigDecimal hotScore, int manualPriority, Instant expiresAt, boolean active, int sortOrder, long version) {
    }

    public record AuditResponse(Long id, String action, String targetType, String targetId, String operatorName, String reason, String ipAddress, Instant createdAt) {
    }

    public record CatalogNameRequest(@NotBlank @Size(max=80) @NotBlank @Size(max=80) String name, @Size(max=40) @Size(max=40) String iconKey, @Size(max=240) @Size(max=240) String description, Integer sortOrder, Boolean active, @Size(max=500) @Size(max=500) String reason) {
    }

    public record CatalogResponse(List<GameOptionResponse> games, List<CategoryOptionResponse> categories, List<PlatformOptionResponse> platforms, List<GamePlatformResponse> gamePlatforms) {
    }

    public record CatalogUpdateRequest(@NotBlank @Size(max=80) @NotBlank @Size(max=80) String name, @Size(max=40) @Size(max=40) String iconKey, @Size(max=240) @Size(max=240) String description, Integer sortOrder, Boolean active, @Size(max=500) @Size(max=500) String reason) {
    }

    public record CategoryCreateRequest(@NotBlank @Size(max=32) @NotBlank @Size(max=32) String gameCode, @NotBlank @Size(max=80) @NotBlank @Size(max=80) String name, @Size(max=40) @Size(max=40) String iconKey, @Size(max=240) @Size(max=240) String description, Integer sortOrder, Boolean active, @Size(max=500) @Size(max=500) String reason) {
    }

    public record CategoryOptionResponse(Long id, String gameCode, String code, String name, String iconKey, String description, boolean active, int sortOrder, long serviceCount) {
    }

    public record CategoryUpdateRequest(@NotBlank @Size(max=80) @NotBlank @Size(max=80) String name, @Size(max=40) @Size(max=40) String iconKey, @Size(max=240) @Size(max=240) String description, Integer sortOrder, Boolean active, @Size(max=500) @Size(max=500) String reason) {
    }

    public record ContactChannelResponse(String label, String wechatId, String qrImage, String note, String jumpUrl) {
    }

    public record CreateOrderRequest(@NotBlank String status, @Min(value=0L) @Max(value=100L) @Min(value=0L) @Max(value=100L) Integer progressPercent, @NotBlank @Size(max=240) @NotBlank @Size(max=240) String progressText, @Size(max=500) @Size(max=500) String customerNote, @Size(max=1200) @Size(max=1200) String internalNote, @Size(max=500) @Size(max=500) String reason) {
    }

    public record CreateQuoteRequest(@NotBlank @Size(max=100) @NotBlank @Size(max=100) String game, @NotBlank @Size(max=200) @NotBlank @Size(max=200) String demand, @Size(max=500) @Size(max=500) String currentProgress, UUID screenshotAssetId, @Size(max=50) @Size(max=50) String platform, @Size(max=100) @Size(max=100) String expectedTime, @Size(max=40) @Size(max=40) String contactMethod, @Size(max=120) @Size(max=120) String customerContact) {
    }

    public record DashboardResponse(long totalServices, long newQuotes, long inProgressOrders, long completedOrders, long trackingEvents, long newFeedback) {
    }

    public record GameBulkUpdateRequest(@NotEmpty @Size(max=100) @NotEmpty @Size(max=100) List<@Size(max=32) String> codes, Boolean active, Boolean featured, @Pattern(regexp="ACTIVE|READY|PLANNED|PAUSED") @Pattern(regexp="ACTIVE|READY|PLANNED|PAUSED") String businessStatus, @Min(value=0L) @Max(value=100L) @Min(value=0L) @Max(value=100L) Integer popularityScore, @Size(max=500) @Size(max=500) String reason) {
    }

    public record GameOptionResponse(String code, String name, String nameEn, String aliases, String iconKey, String description, String publisher, String genres, String logoImage, String coverImage, int popularityScore, boolean featured, String businessStatus, String seoSlug, String seoTitle, String seoDescription, boolean active, int sortOrder, long categoryCount, long serviceCount, List<String> platformCodes) {
    }

    public record GamePlatformResponse(String gameCode, String platformCode, boolean active, int sortOrder) {
    }

    public record GameProfileUpdateRequest(@NotBlank @Size(max=80) @NotBlank @Size(max=80) String name, @Size(max=120) @Size(max=120) String nameEn, @Size(max=240) @Size(max=240) String aliases, @Size(max=40) @Size(max=40) String iconKey, @Size(max=240) @Size(max=240) String description, @Size(max=120) @Size(max=120) String publisher, @Size(max=160) @Size(max=160) String genres, @Size(max=500) @Size(max=500) String logoImage, @Size(max=500) @Size(max=500) String coverImage, @Min(value=0L) @Max(value=100L) @Min(value=0L) @Max(value=100L) Integer popularityScore, Boolean featured, @Pattern(regexp="ACTIVE|READY|PLANNED|PAUSED") @Pattern(regexp="ACTIVE|READY|PLANNED|PAUSED") String businessStatus, @Pattern(regexp="[a-z0-9-]{2,120}") @Pattern(regexp="[a-z0-9-]{2,120}") String seoSlug, @Size(max=160) @Size(max=160) String seoTitle, @Size(max=300) @Size(max=300) String seoDescription, Integer sortOrder, Boolean active, List<@Size(max=32) String> platformCodes, @Size(max=500) @Size(max=500) String reason) {
    }

    public record OrderPublicResponse(String businessNo, String status, Integer progressPercent, String progressText, Instant updatedAt, boolean hasProgressImage, String customerNote) {
    }

    public record PlatformOptionResponse(String code, String name, String iconKey, String platformGroup, String description, boolean active, int sortOrder) {
    }

    public record QuoteAdminResponse(UUID id, String businessNo, String game, String demand, String currentProgress, UUID screenshotAssetId, String platform, String expectedTime, String contactMethod, String customerContact, String customerIpHash, String status, Instant createdAt) {
    }

    public record QuoteConvertRequest(@Size(max=500) @Size(max=500) String reason) {
    }

    public record QuoteResponse(UUID id, String businessNo, String status, Instant createdAt) {
    }

    public record QuoteStatusUpdate(@NotBlank String status, @Size(max=500) @Size(max=500) String reason) {
    }

    public record RecordPublicResponse(String businessNo, String stage, String status, String title, String expectedTime, Integer progressPercent, String progressText, Instant createdAt, Instant updatedAt, boolean hasProgressImage, String customerNote) {
    }

    public record ServiceResponse(Long id, String slug, String gameCode, String name, String category, String shortDesc, String priceMode, BigDecimal priceFixed, BigDecimal priceMin, BigDecimal priceMax, String durationText, String status, String coverImage) {
    }

    public record ServiceUpsert(@NotBlank @Size(max=32) @NotBlank @Size(max=32) String gameCode, @NotBlank @Pattern(regexp="[a-z0-9-]{2,100}") @NotBlank @Pattern(regexp="[a-z0-9-]{2,100}") String slug, @NotBlank @Size(max=120) @NotBlank @Size(max=120) String name, @NotBlank String category, @NotBlank @Size(max=240) @NotBlank @Size(max=240) String shortDesc, @NotBlank String priceMode, BigDecimal priceFixed, BigDecimal priceMin, BigDecimal priceMax, @NotBlank @Size(max=100) @NotBlank @Size(max=100) String durationText, @NotBlank String status, @Size(max=500) @Size(max=500) String coverImage, BigDecimal hotScore, Integer manualPriority, Instant expiresAt, Boolean active, Integer sortOrder, @Size(max=500) @Size(max=500) String reason) {
    }

    public record SiteConfigResponse(String brandName, String legalEntityName, String serviceStatus, String statusShortText, String primaryButtonLabel, boolean quoteEnabled, boolean orderQueryEnabled, String heroTitle, String heroSubtitle, String startingPriceMode, BigDecimal startingPrice, boolean showStartingPrice, int homeServiceLimit, String heroImage, int heroImageHeight, String heroImageFit, int heroImagePositionX, int heroImagePositionY, String heroBackgroundImage, int heroBackgroundOpacity, String heroBackgroundFit, int heroBackgroundPositionX, int heroBackgroundPositionY, int heroBackgroundBlur, int serviceCoverHeight, String serviceCoverFit, int serviceCoverPositionX, int serviceCoverPositionY, List<String> trustTags, List<TrustCaseResponse> trustCases, List<TrustFaqResponse> trustFaq, boolean announcementEnabled, String announcementText, String contactTitle, String contactIntro, String wechatId, String wechatQrImage, int wechatQrSize, List<ContactChannelResponse> contactChannels, String qqId, String qqLink, String phone, String serviceHours, String taobaoUrl, String xianyuUrl, String contactNotice, String wecomKefuUrl, String wecomKefuNote, String quickContactTitle, String taobaoLabel, String xianyuLabel, String wecomKefuLabel, String wecomKefuButton, String quoteSuccessText, String orderHelpText, String footerText, String seoTitle, String seoDescription, String toolUpdatedNote, boolean toolPublicEnabled, List<ServiceResponse> hotServices) {
    }

    public record SiteConfigUpdate(@NotBlank @Size(max=80) @NotBlank @Size(max=80) String brandName, @NotBlank @Size(max=120) @NotBlank @Size(max=120) String legalEntityName, @NotBlank String serviceStatus, @Size(max=120) @Size(max=120) String statusShortText, @Size(max=40) @Size(max=40) String primaryButtonLabel, boolean quoteEnabled, boolean orderQueryEnabled, @NotBlank @Size(max=120) @NotBlank @Size(max=120) String heroTitle, @NotBlank @Size(max=240) @NotBlank @Size(max=240) String heroSubtitle, @NotBlank String startingPriceMode, @NotNull @DecimalMin(value="0.00") @NotNull @DecimalMin(value="0.00") BigDecimal startingPrice, boolean showStartingPrice, @Min(value=2L) @Max(value=8L) @Min(value=2L) @Max(value=8L) int homeServiceLimit, @Size(max=500) @Size(max=500) String heroImage, @Min(value=80L) @Max(value=420L) @Min(value=80L) @Max(value=420L) int heroImageHeight, @NotBlank String heroImageFit, @Min(value=0L) @Max(value=100L) @Min(value=0L) @Max(value=100L) int heroImagePositionX, @Min(value=0L) @Max(value=100L) @Min(value=0L) @Max(value=100L) int heroImagePositionY, @Size(max=500) @Size(max=500) String heroBackgroundImage, @Min(value=0L) @Max(value=100L) @Min(value=0L) @Max(value=100L) int heroBackgroundOpacity, @NotBlank String heroBackgroundFit, @Min(value=0L) @Max(value=100L) @Min(value=0L) @Max(value=100L) int heroBackgroundPositionX, @Min(value=0L) @Max(value=100L) @Min(value=0L) @Max(value=100L) int heroBackgroundPositionY, @Min(value=0L) @Max(value=24L) @Min(value=0L) @Max(value=24L) int heroBackgroundBlur, @Min(value=70L) @Max(value=240L) @Min(value=70L) @Max(value=240L) int serviceCoverHeight, @NotBlank String serviceCoverFit, @Min(value=0L) @Max(value=100L) @Min(value=0L) @Max(value=100L) int serviceCoverPositionX, @Min(value=0L) @Max(value=100L) @Min(value=0L) @Max(value=100L) int serviceCoverPositionY, @NotNull @Size(max=6) @NotNull @Size(max=6) List<@NotBlank @Size(max=30) String> trustTags, @Size(max=8) @Size(max=8) List<TrustCaseResponse> trustCases, @Size(max=12) @Size(max=12) List<TrustFaqResponse> trustFaq, boolean announcementEnabled, @Size(max=240) @Size(max=240) String announcementText, @NotBlank @Size(max=80) @NotBlank @Size(max=80) String contactTitle, @NotBlank @Size(max=300) @NotBlank @Size(max=300) String contactIntro, @Size(max=100) @Size(max=100) String wechatId, @Size(max=500) @Size(max=500) String wechatQrImage, @Min(value=120L) @Max(value=420L) @Min(value=120L) @Max(value=420L) int wechatQrSize, @Size(max=6) @Size(max=6) List<ContactChannelResponse> contactChannels, @Size(max=100) @Size(max=100) String qqId, @Size(max=500) @Size(max=500) String qqLink, @Size(max=50) @Size(max=50) String phone, @Size(max=100) @Size(max=100) String serviceHours, @Size(max=500) @Size(max=500) String taobaoUrl, @Size(max=500) @Size(max=500) String xianyuUrl, @NotBlank @Size(max=500) @NotBlank @Size(max=500) String contactNotice, @NotBlank @Size(max=300) @NotBlank @Size(max=300) String quoteSuccessText, @NotBlank @Size(max=300) @NotBlank @Size(max=300) String orderHelpText, @NotBlank @Size(max=200) @NotBlank @Size(max=200) String footerText, @Size(max=160) @Size(max=160) String seoTitle, @Size(max=300) @Size(max=300) String seoDescription, @Size(max=500) @Size(max=500) String reason) {
    }

    public record SiteImageUploadResponse(String url) {
    }

    public record SlotResponse(int slotNo, Long serviceId, String serviceName, String mode) {
    }

    public record SlotUpdate(@Min(value=1L) @Max(value=2L) @Min(value=1L) @Max(value=2L) int slotNo, Long serviceId, @NotBlank String mode, @Size(max=500) @Size(max=500) String reason) {
    }

    public record TestFeedbackCreateRequest(@NotBlank @Size(max=120) @NotBlank @Size(max=120) String title, @Size(max=240) @Size(max=240) String pagePath, @NotBlank @Size(max=5000) @NotBlank @Size(max=5000) String content, @Size(max=16) @Size(max=16) String priority, @Size(max=1000) @Size(max=1000) String operatorNote) {
    }

    public record TestFeedbackResponse(Long id, String title, String pagePath, String content, String priority, String status, String operatorNote, UUID attachmentAssetId, String createdBy, Instant createdAt, Instant updatedAt) {
    }

    public record TestFeedbackUpdateRequest(@NotBlank @Size(max=120) @NotBlank @Size(max=120) String title, @Size(max=240) @Size(max=240) String pagePath, @NotBlank @Size(max=5000) @NotBlank @Size(max=5000) String content, @NotBlank @Size(max=16) @NotBlank @Size(max=16) String priority, @NotBlank @Size(max=24) @NotBlank @Size(max=24) String status, @Size(max=1000) @Size(max=1000) String operatorNote) {
    }

    public record TrackRequest(@NotBlank @Size(max=64) @NotBlank @Size(max=64) String eventName, @Size(max=255) @Size(max=255) String path, @Size(max=255) @Size(max=255) String refValue) {
    }

    public record TrustCaseResponse(String tag, String title, String metric, String detail) {
    }

    public record TrustFaqResponse(String question, String answer) {
    }

    public record UpdateOrderRequest(@jakarta.validation.constraints.NotNull Long version, @NotBlank String status, @Min(value=0L) @Max(value=100L) @Min(value=0L) @Max(value=100L) Integer progressPercent, @NotBlank @Size(max=240) @NotBlank @Size(max=240) String progressText, @Size(max=500) @Size(max=500) String customerNote, @Size(max=1200) @Size(max=1200) String internalNote, @Size(max=500) @Size(max=500) String reason) {
    }

    public record UploadResponse(UUID assetId) {
    }
}


