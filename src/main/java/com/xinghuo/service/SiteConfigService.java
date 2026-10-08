package com.xinghuo.service;

import com.xinghuo.dto.ApiDtos;
import com.xinghuo.exception.BadRequestException;
import com.xinghuo.model.GameStatus;
import com.xinghuo.model.ServiceItem;
import com.xinghuo.model.SiteConfig;
import com.xinghuo.repository.ServiceItemRepository;
import com.xinghuo.repository.SiteConfigRepository;
import com.xinghuo.service.AuditService;
import com.xinghuo.service.GameStatusService;
import com.xinghuo.service.SiteSettingService;
import com.xinghuo.service.SlotService;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import lombok.Generated;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SiteConfigService {
    private final SiteConfigRepository repo;
    private final ServiceItemRepository serviceRepo;
    private final SlotService slotService;
    private final AuditService audit;
    private final GameStatusService gameStatuses;
    private final SiteSettingService siteSettings;

    @Transactional(readOnly=true)
    public ApiDtos.SiteConfigResponse publicConfig() {
        SiteConfig c = this.get();
        return new ApiDtos.SiteConfigResponse(c.getBrandName(), c.getLegalEntityName(), this.mw4Status().name().toLowerCase(Locale.ROOT), this.mw4ShortText(c), this.mw4CtaLabel(c), c.isQuoteEnabled(), c.isOrderQueryEnabled(), c.getHeroTitle(), c.getHeroSubtitle(), c.getStartingPriceMode().name().toLowerCase(Locale.ROOT), this.displayStartingPrice(c), c.isShowStartingPrice(), c.getHomeServiceLimit(), c.getHeroImage(), c.getHeroImageHeight(), c.getHeroImageFit().toLowerCase(Locale.ROOT), c.getHeroImagePositionX(), c.getHeroImagePositionY(), c.getHeroImageMobileHeight(), c.getHeroImageMobileFit().toLowerCase(Locale.ROOT), c.getHeroImageMobilePositionX(), c.getHeroImageMobilePositionY(), c.getHeroBackgroundImage(), c.getHeroBackgroundOpacity(), c.getHeroBackgroundFit().toLowerCase(Locale.ROOT), c.getHeroBackgroundPositionX(), c.getHeroBackgroundPositionY(), c.getHeroBackgroundBlur(), c.getHeroBackgroundMobileFit().toLowerCase(Locale.ROOT), c.getHeroBackgroundMobilePositionX(), c.getHeroBackgroundMobilePositionY(), c.getServiceCoverHeight(), c.getServiceCoverFit().toLowerCase(Locale.ROOT), c.getServiceCoverPositionX(), c.getServiceCoverPositionY(), c.getServiceCoverMobileHeight(), c.getServiceCoverMobileFit().toLowerCase(Locale.ROOT), c.getServiceCoverMobilePositionX(), c.getServiceCoverMobilePositionY(), this.splitTags(c.getTrustTags()), this.parseTrustCases(c), this.parseTrustFaq(c), c.isAnnouncementEnabled(), c.getAnnouncementText(), c.getContactTitle(), c.getContactIntro(), c.getWechatId(), c.getWechatQrImage(), c.getWechatQrSize(), this.parseContactChannels(c), c.getQqId(), c.getQqLink(), c.getPhone(), c.getServiceHours(), c.getTaobaoUrl(), c.getXianyuUrl(), c.getContactNotice(), c.getWecomKefuUrl(), c.getWecomKefuNote(), c.getQuickContactTitle(), c.getTaobaoLabel(), c.getXianyuLabel(), c.getWecomKefuLabel(), c.getWecomKefuButton(), c.getQuoteSuccessText(), c.getOrderHelpText(), c.getFooterText(), c.getSeoTitle(), c.getSeoDescription(), c.getToolUpdatedNote(), c.isToolPublicEnabled(), this.slotService.publicHotServices());
    }

    @Transactional(readOnly=true)
    public SiteConfig get() {
        return (SiteConfig)this.repo.findById(1L).orElseThrow();
    }

    @Transactional(readOnly=true)
    public boolean isQuoteEnabled() {
        return this.get().isQuoteEnabled();
    }

    @Transactional(readOnly=true)
    public boolean isOrderQueryEnabled() {
        return this.get().isOrderQueryEnabled();
    }

    @Transactional
    public ApiDtos.SiteConfigResponse updateSiteImage(String slot, String url, String operator, HttpServletRequest req) {
        String key;
        SiteConfig c = this.get();
        ApiDtos.SiteConfigResponse before = this.publicConfig();
        switch (key = slot == null ? "" : slot.trim().toUpperCase(Locale.ROOT)) {
            case "HERO": {
                c.setHeroImage(this.blankToNull(url));
                break;
            }
            case "BACKGROUND": {
                c.setHeroBackgroundImage(this.blankToNull(url));
                break;
            }
            case "WECHAT_QR": {
                c.setWechatQrImage(this.blankToNull(url));
                break;
            }
            default: {
                throw new BadRequestException("\u975e\u6cd5\u56fe\u7247\u4f4d\u7f6e");
            }
        }
        c.setUpdatedAt(Instant.now());
        ApiDtos.SiteConfigResponse after = this.publicConfig();
        this.audit.log("SITE_IMAGE_UPDATE", "SITE_CONFIG", key, operator, before, after, "\u66f4\u65b0\u7ad9\u70b9\u56fe\u7247", req);
        return after;
    }

    @Transactional
    public ApiDtos.SiteConfigResponse update(ApiDtos.SiteConfigUpdate r, String operator, HttpServletRequest req) {
        SiteConfig c = this.get();
        ApiDtos.SiteConfigResponse before = this.publicConfig();
        c.setBrandName(r.brandName().trim());
        c.setLegalEntityName(r.legalEntityName().trim());
        c.setServiceStatus(this.parseStatus(r.serviceStatus()));
        c.setQuoteEnabled(r.quoteEnabled());
        c.setOrderQueryEnabled(r.orderQueryEnabled());
        c.setHeroTitle(r.heroTitle().trim());
        c.setHeroSubtitle(r.heroSubtitle().trim());
        c.setStartingPriceMode(this.parsePriceMode(r.startingPriceMode()));
        c.setStartingPrice(r.startingPrice());
        c.setShowStartingPrice(r.showStartingPrice());
        c.setHomeServiceLimit(r.homeServiceLimit());
        c.setHeroImage(this.blankToNull(r.heroImage()));
        c.setHeroImageHeight(r.heroImageHeight());
        c.setHeroImageFit(this.normalizeFit(r.heroImageFit()));
        c.setHeroImagePositionX(r.heroImagePositionX());
        c.setHeroImagePositionY(r.heroImagePositionY());
        c.setHeroImageMobileHeight(r.heroImageMobileHeight());
        c.setHeroImageMobileFit(this.normalizeFit(r.heroImageMobileFit()));
        c.setHeroImageMobilePositionX(r.heroImageMobilePositionX());
        c.setHeroImageMobilePositionY(r.heroImageMobilePositionY());
        c.setHeroBackgroundImage(this.blankToNull(r.heroBackgroundImage()));
        c.setHeroBackgroundOpacity(r.heroBackgroundOpacity());
        c.setHeroBackgroundFit(this.normalizeFit(r.heroBackgroundFit()));
        c.setHeroBackgroundPositionX(r.heroBackgroundPositionX());
        c.setHeroBackgroundPositionY(r.heroBackgroundPositionY());
        c.setHeroBackgroundBlur(r.heroBackgroundBlur());
        c.setHeroBackgroundMobileFit(this.normalizeFit(r.heroBackgroundMobileFit()));
        c.setHeroBackgroundMobilePositionX(r.heroBackgroundMobilePositionX());
        c.setHeroBackgroundMobilePositionY(r.heroBackgroundMobilePositionY());
        c.setServiceCoverHeight(r.serviceCoverHeight());
        c.setServiceCoverFit(this.normalizeFit(r.serviceCoverFit()));
        c.setServiceCoverPositionX(r.serviceCoverPositionX());
        c.setServiceCoverPositionY(r.serviceCoverPositionY());
        c.setServiceCoverMobileHeight(r.serviceCoverMobileHeight());
        c.setServiceCoverMobileFit(this.normalizeFit(r.serviceCoverMobileFit()));
        c.setServiceCoverMobilePositionX(r.serviceCoverMobilePositionX());
        c.setServiceCoverMobilePositionY(r.serviceCoverMobilePositionY());
        c.setTrustTags(String.join((CharSequence)"|", r.trustTags().stream().map(String::trim).filter(x -> !x.isBlank()).toList()));
        c.setTrustCases(this.serializeTrustCases(r.trustCases(), c));
        c.setTrustFaq(this.serializeTrustFaq(r.trustFaq(), c));
        c.setAnnouncementEnabled(r.announcementEnabled());
        c.setAnnouncementText(this.blankToNull(r.announcementText()));
        c.setContactTitle(r.contactTitle().trim());
        c.setContactIntro(r.contactIntro().trim());
        c.setWechatId(this.blankToNull(r.wechatId()));
        c.setWechatQrImage(this.blankToNull(r.wechatQrImage()));
        c.setWechatQrSize(r.wechatQrSize());
        c.setContactChannels(this.serializeContactChannels(r.contactChannels(), c));
        c.setQqId(this.blankToNull(r.qqId()));
        c.setQqLink(this.blankToNull(r.qqLink()));
        c.setPhone(this.blankToNull(r.phone()));
        c.setServiceHours(this.blankToNull(r.serviceHours()));
        c.setTaobaoUrl(this.blankToNull(r.taobaoUrl()));
        c.setXianyuUrl(this.blankToNull(r.xianyuUrl()));
        c.setContactNotice(r.contactNotice().trim());
        c.setQuoteSuccessText(r.quoteSuccessText().trim());
        c.setOrderHelpText(r.orderHelpText().trim());
        c.setFooterText(r.footerText().trim());
        c.setSeoTitle(this.blankToNull(r.seoTitle()));
        c.setSeoDescription(this.blankToNull(r.seoDescription()));
        c.setUpdatedAt(Instant.now());
        ApiDtos.SiteConfigResponse after = this.publicConfig();
        this.audit.log("SITE_CONFIG_UPDATE", "SITE_CONFIG", "1", operator, before, after, r.reason(), req);
        return after;
    }

    @Transactional
    public ApiDtos.SiteConfigResponse updateSection(String section, Map<String, Object> r, String operator, HttpServletRequest req) {
        String key;
        SiteConfig c = this.get();
        ApiDtos.SiteConfigResponse before = this.publicConfig();
        switch (key = section == null ? "" : section.trim().toLowerCase(Locale.ROOT)) {
            case "basic": {
                c.setBrandName(this.text(r, "brandName", c.getBrandName(), 80));
                c.setLegalEntityName(this.text(r, "legalEntityName", c.getLegalEntityName(), 120));
                c.setServiceStatus(this.parseStatus(this.text(r, "serviceStatus", c.getServiceStatus().name(), 16)));
                c.setStatusShortText(this.nullableText(r, "statusShortText", 120));
                c.setPrimaryButtonLabel(this.nullableText(r, "primaryButtonLabel", 40));
                c.setQuoteEnabled(this.bool(r, "quoteEnabled", c.isQuoteEnabled()));
                c.setOrderQueryEnabled(this.bool(r, "orderQueryEnabled", c.isOrderQueryEnabled()));
                c.setLegalEntityName(this.text(r, "legalEntityName", c.getLegalEntityName(), 120));
                break;
            }
            case "home": {
                c.setHeroTitle(this.text(r, "heroTitle", c.getHeroTitle(), 120));
                c.setHeroSubtitle(this.text(r, "heroSubtitle", c.getHeroSubtitle(), 240));
                c.setStartingPriceMode(this.parsePriceMode(this.text(r, "startingPriceMode", c.getStartingPriceMode().name(), 16)));
                c.setStartingPrice(this.decimal(r, "startingPrice", c.getStartingPrice()));
                c.setShowStartingPrice(this.bool(r, "showStartingPrice", c.isShowStartingPrice()));
                c.setHomeServiceLimit(this.integer(r, "homeServiceLimit", c.getHomeServiceLimit(), 2, 8));
                Object tags = r.get("trustTags");
                if (!(tags instanceof List<?>)) break;
                List<?> xs = (List<?>)tags;
                c.setTrustTags(String.join("|", xs.stream().map(String::valueOf).map(String::trim).filter(x -> !x.isBlank()).limit(6L).toList()));
                break;
            }
            case "visual": {
                c.setHeroImage(this.nullableText(r, "heroImage", 500));
                c.setHeroImageHeight(this.integer(r, "heroImageHeight", c.getHeroImageHeight(), 80, 420));
                c.setHeroImageFit(this.normalizeFit(this.text(r, "heroImageFit", c.getHeroImageFit(), 16)));
                c.setHeroImagePositionX(this.integer(r, "heroImagePositionX", c.getHeroImagePositionX(), 0, 100));
                c.setHeroImagePositionY(this.integer(r, "heroImagePositionY", c.getHeroImagePositionY(), 0, 100));
                c.setHeroImageMobileHeight(this.integer(r, "heroImageMobileHeight", c.getHeroImageMobileHeight(), 80, 420));
                c.setHeroImageMobileFit(this.normalizeFit(this.text(r, "heroImageMobileFit", c.getHeroImageMobileFit(), 16)));
                c.setHeroImageMobilePositionX(this.integer(r, "heroImageMobilePositionX", c.getHeroImageMobilePositionX(), 0, 100));
                c.setHeroImageMobilePositionY(this.integer(r, "heroImageMobilePositionY", c.getHeroImageMobilePositionY(), 0, 100));
                c.setHeroBackgroundImage(this.nullableText(r, "heroBackgroundImage", 500));
                c.setHeroBackgroundOpacity(this.integer(r, "heroBackgroundOpacity", c.getHeroBackgroundOpacity(), 0, 100));
                c.setHeroBackgroundFit(this.normalizeFit(this.text(r, "heroBackgroundFit", c.getHeroBackgroundFit(), 16)));
                c.setHeroBackgroundPositionX(this.integer(r, "heroBackgroundPositionX", c.getHeroBackgroundPositionX(), 0, 100));
                c.setHeroBackgroundPositionY(this.integer(r, "heroBackgroundPositionY", c.getHeroBackgroundPositionY(), 0, 100));
                c.setHeroBackgroundBlur(this.integer(r, "heroBackgroundBlur", c.getHeroBackgroundBlur(), 0, 24));
                c.setHeroBackgroundMobileFit(this.normalizeFit(this.text(r, "heroBackgroundMobileFit", c.getHeroBackgroundMobileFit(), 16)));
                c.setHeroBackgroundMobilePositionX(this.integer(r, "heroBackgroundMobilePositionX", c.getHeroBackgroundMobilePositionX(), 0, 100));
                c.setHeroBackgroundMobilePositionY(this.integer(r, "heroBackgroundMobilePositionY", c.getHeroBackgroundMobilePositionY(), 0, 100));
                c.setServiceCoverHeight(this.integer(r, "serviceCoverHeight", c.getServiceCoverHeight(), 70, 240));
                c.setServiceCoverFit(this.normalizeFit(this.text(r, "serviceCoverFit", c.getServiceCoverFit(), 16)));
                c.setServiceCoverPositionX(this.integer(r, "serviceCoverPositionX", c.getServiceCoverPositionX(), 0, 100));
                c.setServiceCoverPositionY(this.integer(r, "serviceCoverPositionY", c.getServiceCoverPositionY(), 0, 100));
                c.setServiceCoverMobileHeight(this.integer(r, "serviceCoverMobileHeight", c.getServiceCoverMobileHeight(), 70, 240));
                c.setServiceCoverMobileFit(this.normalizeFit(this.text(r, "serviceCoverMobileFit", c.getServiceCoverMobileFit(), 16)));
                c.setServiceCoverMobilePositionX(this.integer(r, "serviceCoverMobilePositionX", c.getServiceCoverMobilePositionX(), 0, 100));
                c.setServiceCoverMobilePositionY(this.integer(r, "serviceCoverMobilePositionY", c.getServiceCoverMobilePositionY(), 0, 100));
                break;
            }
            case "notice": {
                c.setAnnouncementEnabled(this.bool(r, "announcementEnabled", c.isAnnouncementEnabled()));
                c.setAnnouncementText(this.nullableText(r, "announcementText", 240));
                break;
            }
            case "contact": {
                c.setContactTitle(this.text(r, "contactTitle", c.getContactTitle(), 80));
                c.setContactIntro(this.text(r, "contactIntro", c.getContactIntro(), 300));
                c.setWechatId(this.nullableText(r, "wechatId", 100));
                c.setWechatQrImage(this.nullableText(r, "wechatQrImage", 500));
                c.setWechatQrSize(this.integer(r, "wechatQrSize", c.getWechatQrSize(), 120, 420));
                c.setContactChannels(this.serializeContactChannels(r.get("contactChannels"), c));
                c.setQqId(this.nullableText(r, "qqId", 100));
                c.setQqLink(this.nullableText(r, "qqLink", 500));
                c.setPhone(this.nullableText(r, "phone", 50));
                c.setServiceHours(this.nullableText(r, "serviceHours", 100));
                c.setTaobaoUrl(this.nullableText(r, "taobaoUrl", 500));
                c.setXianyuUrl(this.nullableText(r, "xianyuUrl", 500));
                c.setContactNotice(this.text(r, "contactNotice", c.getContactNotice(), 500));
                c.setWecomKefuUrl(this.nullableText(r, "wecomKefuUrl", 500));
                c.setWecomKefuNote(this.nullableText(r, "wecomKefuNote", 300));
                c.setQuickContactTitle(this.nullableText(r, "quickContactTitle", 40));
                c.setTaobaoLabel(this.nullableText(r, "taobaoLabel", 40));
                c.setXianyuLabel(this.nullableText(r, "xianyuLabel", 40));
                c.setWecomKefuLabel(this.nullableText(r, "wecomKefuLabel", 40));
                c.setWecomKefuButton(this.nullableText(r, "wecomKefuButton", 40));
                break;
            }
            case "trust": {
                c.setTrustCases(this.serializeTrustCases(r.get("trustCases"), c));
                c.setTrustFaq(this.serializeTrustFaq(r.get("trustFaq"), c));
                break;
            }
            case "copy": {
                c.setQuoteSuccessText(this.text(r, "quoteSuccessText", c.getQuoteSuccessText(), 300));
                c.setOrderHelpText(this.text(r, "orderHelpText", c.getOrderHelpText(), 300));
                c.setFooterText(this.text(r, "footerText", c.getFooterText(), 200));
                break;
            }
            case "seo": {
                c.setSeoTitle(this.nullableText(r, "seoTitle", 160));
                c.setSeoDescription(this.nullableText(r, "seoDescription", 300));
                break;
            }
            case "tools": {
                c.setToolUpdatedNote(this.nullableText(r, "toolUpdatedNote", 120));
                c.setToolPublicEnabled(this.bool(r, "toolPublicEnabled", c.isToolPublicEnabled()));
                break;
            }
            default: {
                throw new BadRequestException("\u975e\u6cd5\u914d\u7f6e\u5206\u533a");
            }
        }
        this.syncSiteSettings(c);
        c.setUpdatedAt(Instant.now());
        ApiDtos.SiteConfigResponse after = this.publicConfig();
        this.audit.log("SITE_CONFIG_" + key.toUpperCase(Locale.ROOT), "SITE_CONFIG", key, operator, before, after, String.valueOf(r.getOrDefault("reason", "\u540e\u53f0\u5206\u533a\u4fdd\u5b58")), req);
        return after;
    }

    private void syncSiteSettings(SiteConfig c) {
        this.siteSettings.put("brandName", c.getBrandName());
        this.siteSettings.put("legalEntityName", c.getLegalEntityName());
        this.siteSettings.put("wechat", c.getWechatId());
        this.siteSettings.put("serviceHours", c.getServiceHours());
        this.siteSettings.put("siteTitleSuffix", c.getBrandName());
    }

    private String text(Map<String, Object> m, String k, String def, int max) {
        String x;
        Object v = m.get(k);
        String string = x = v == null ? def : String.valueOf(v).trim();
        if (x == null || x.isBlank() || x.length() > max) {
            throw new BadRequestException("\u5b57\u6bb5 " + k + " \u4e0d\u5408\u6cd5");
        }
        return x;
    }

    private String nullableText(Map<String, Object> m, String k, int max) {
        Object v = m.get(k);
        if (v == null) {
            return null;
        }
        String x = String.valueOf(v).trim();
        if (x.isBlank()) {
            return null;
        }
        if (x.length() > max) {
            throw new BadRequestException("\u5b57\u6bb5 " + k + " \u8fc7\u957f");
        }
        return x;
    }

    private boolean bool(Map<String, Object> m, String k, boolean def) {
        Object v = m.get(k);
        return v == null ? def : Boolean.parseBoolean(String.valueOf(v));
    }

    private int integer(Map<String, Object> m, String k, int def, int min, int max) {
        int x;
        Object v = m.get(k);
        int n = x = v == null ? def : Integer.parseInt(String.valueOf(v));
        if (x < min || x > max) {
            throw new BadRequestException("\u5b57\u6bb5 " + k + " \u8d85\u51fa\u8303\u56f4");
        }
        return x;
    }

    private BigDecimal decimal(Map<String, Object> m, String k, BigDecimal def) {
        Object v = m.get(k);
        if (v == null) {
            return def;
        }
        try {
            BigDecimal x = new BigDecimal(String.valueOf(v));
            if (x.signum() < 0) {
                throw new Exception();
            }
            return x;
        }
        catch (Exception e) {
            throw new BadRequestException("\u4ef7\u683c\u4e0d\u5408\u6cd5");
        }
    }

    private BigDecimal displayStartingPrice(SiteConfig c) {
        if (c.getStartingPriceMode() != SiteConfig.StartingPriceMode.AUTO) {
            return c.getStartingPrice();
        }
        return this.serviceRepo.findPublic(Instant.now()).stream().map(this::entryPrice).filter(Objects::nonNull).min(BigDecimal::compareTo).orElse(c.getStartingPrice());
    }

    private BigDecimal entryPrice(ServiceItem s) {
        if (s.getPriceMode() == ServiceItem.PriceMode.FIXED) {
            return s.getPriceFixed();
        }
        if (s.getPriceMode() == ServiceItem.PriceMode.RANGE) {
            return s.getPriceMin();
        }
        return null;
    }

    private SiteConfig.ServiceStatus mw4Status() {
        SiteConfig.ServiceStatus serviceStatus;
        if (this.gameStatuses.resolve("CODMW4", null) == null) {
            serviceStatus = this.get().getServiceStatus();
        } else {
            switch (this.gameStatuses.require("CODMW4").getStatusCode()) {
                default: {
                    throw new MatchException(null, null);
                }
                case OPEN: {
                    serviceStatus = SiteConfig.ServiceStatus.OPEN;
                    break;
                }
                case PAUSED: {
                    serviceStatus = SiteConfig.ServiceStatus.SUSPENDED;
                    break;
                }
                case MAINTENANCE: {
                    serviceStatus = SiteConfig.ServiceStatus.MAINTENANCE;
                    break;
                }
                case LIMITED:
                case PREORDER: {
                    serviceStatus = SiteConfig.ServiceStatus.RESERVATION;
                }
            }
        }
        return serviceStatus;
    }

    private String mw4ShortText(SiteConfig fallback) {
        GameStatus g = this.gameStatuses.resolve("CODMW4", null);
        return g == null ? fallback.getStatusShortText() : g.getPublicLabel();
    }

    private String mw4CtaLabel(SiteConfig fallback) {
        GameStatus g = this.gameStatuses.resolve("CODMW4", null);
        return g == null || g.getCtaLabel() == null ? fallback.getPrimaryButtonLabel() : g.getCtaLabel();
    }

    private SiteConfig.ServiceStatus parseStatus(String s) {
        try {
            return SiteConfig.ServiceStatus.valueOf((String)s.toUpperCase(Locale.ROOT));
        }
        catch (Exception e) {
            throw new BadRequestException("\u975e\u6cd5\u63a5\u5355\u72b6\u6001");
        }
    }

    private SiteConfig.StartingPriceMode parsePriceMode(String s) {
        try {
            return SiteConfig.StartingPriceMode.valueOf((String)s.toUpperCase(Locale.ROOT));
        }
        catch (Exception e) {
            throw new BadRequestException("\u975e\u6cd5\u8d77\u6b65\u4ef7\u6a21\u5f0f");
        }
    }

    private List<String> splitTags(String x) {
        return x == null || x.isBlank() ? List.of() : Arrays.stream(x.split("\\|")).map(String::trim).filter(s -> !s.isBlank()).toList();
    }

    private List<ApiDtos.ContactChannelResponse> parseContactChannels(SiteConfig c) {
        String raw = c.getContactChannels();
        ArrayList<ApiDtos.ContactChannelResponse> rows = new ArrayList<ApiDtos.ContactChannelResponse>();
        if (raw != null && !raw.isBlank()) {
            for (String line : raw.split("\\R")) {
                if (line == null || line.isBlank()) continue;
                String[] parts = line.split("\\t", -1);
                String label = this.clean(parts.length > 0 ? parts[0] : "", 40);
                String wechat = this.clean(parts.length > 1 ? parts[1] : "", 100);
                String qr = this.clean(parts.length > 2 ? parts[2] : "", 500);
                String note = this.clean(parts.length > 3 ? parts[3] : "", 120);
                String jump = this.clean(parts.length > 4 ? parts[4] : "", 500);
                if (label == null && wechat == null && qr == null) continue;
                rows.add(new ApiDtos.ContactChannelResponse((String)(label == null ? "\u5ba2\u670d" + (rows.size() + 1) : label), wechat, qr, note, jump));
                if (rows.size() >= 6) break;
            }
        }
        if (rows.isEmpty() && (c.getWechatId() != null || c.getWechatQrImage() != null)) {
            rows.add(new ApiDtos.ContactChannelResponse("\u4e3b\u5ba2\u670d", c.getWechatId(), c.getWechatQrImage(), "\u53d1\u9001\u4e1a\u52a1\u7f16\u53f7\u786e\u8ba4\u62a5\u4ef7", null));
        }
        if (rows.isEmpty()) {
            rows.add(new ApiDtos.ContactChannelResponse("\u4e3b\u5ba2\u670d", "AAC9878", null, "\u53d1\u9001\u4e1a\u52a1\u7f16\u53f7\u786e\u8ba4\u62a5\u4ef7", null));
        }
        return rows;
    }

    private List<ApiDtos.TrustCaseResponse> parseTrustCases(SiteConfig c) {
        String raw = c.getTrustCases();
        ArrayList<ApiDtos.TrustCaseResponse> rows = new ArrayList<ApiDtos.TrustCaseResponse>();
        if (raw != null && !raw.isBlank()) {
            for (String line : raw.split("\\R")) {
                if (line == null || line.isBlank()) continue;
                String[] parts = line.split("\\t", -1);
                String tag = this.clean(parts.length > 0 ? parts[0] : "", 40);
                String title = this.clean(parts.length > 1 ? parts[1] : "", 100);
                String metric = this.clean(parts.length > 2 ? parts[2] : "", 120);
                String detail = this.clean(parts.length > 3 ? parts[3] : "", 240);
                if (title == null && metric == null && detail == null) continue;
                rows.add(new ApiDtos.TrustCaseResponse(tag == null ? "\u670d\u52a1\u793a\u4f8b" : tag, title == null ? "\u670d\u52a1\u793a\u4f8b" : title, metric == null ? "\u6309\u8fdb\u5ea6\u786e\u8ba4" : metric, detail == null ? "" : detail));
                if (rows.size() >= 8) break;
            }
        }
        return rows.isEmpty() ? this.defaultTrustCases() : rows;
    }

    private List<ApiDtos.TrustFaqResponse> parseTrustFaq(SiteConfig c) {
        String raw = c.getTrustFaq();
        ArrayList<ApiDtos.TrustFaqResponse> rows = new ArrayList<ApiDtos.TrustFaqResponse>();
        if (raw != null && !raw.isBlank()) {
            for (String line : raw.split("\\R")) {
                if (line == null || line.isBlank()) continue;
                String[] parts = line.split("\\t", -1);
                String question = this.clean(parts.length > 0 ? parts[0] : "", 100);
                String answer = this.clean(parts.length > 1 ? parts[1] : "", 400);
                if (question == null || answer == null) continue;
                rows.add(new ApiDtos.TrustFaqResponse(question, answer));
                if (rows.size() >= 12) break;
            }
        }
        return rows.isEmpty() ? this.defaultTrustFaq() : rows;
    }

    private String serializeContactChannels(Object value, SiteConfig fallback) {
        if (value == null) {
            return fallback == null ? null : fallback.getContactChannels();
        }
        ArrayList<ApiDtos.ContactChannelResponse> rows = new ArrayList<ApiDtos.ContactChannelResponse>();
        if (value instanceof List) {
            List list = (List)value;
            for (Object item : list) {
                if (item instanceof ApiDtos.ContactChannelResponse) {
                    ApiDtos.ContactChannelResponse c = (ApiDtos.ContactChannelResponse)item;
                    rows.add(c);
                    continue;
                }
                if (!(item instanceof Map)) continue;
                Map m = (Map)item;
                rows.add(new ApiDtos.ContactChannelResponse(this.stringValue(m.get("label")), this.stringValue(m.get("wechatId")), this.stringValue(m.get("qrImage")), this.stringValue(m.get("note")), this.stringValue(m.get("jumpUrl"))));
            }
        }
        ArrayList<String> lines = new ArrayList<String>();
        for (ApiDtos.ContactChannelResponse row : rows) {
            String label = this.clean(row.label(), 40);
            String wechat = this.clean(row.wechatId(), 100);
            String qr = this.clean(row.qrImage(), 500);
            String note = this.clean(row.note(), 120);
            String jump = this.clean(row.jumpUrl(), 500);
            if (label == null && wechat == null && qr == null) continue;
            lines.add(String.join((CharSequence)"\t", new CharSequence[]{label == null ? "\u5ba2\u670d" + (lines.size() + 1) : label, wechat == null ? "" : wechat, qr == null ? "" : qr, note == null ? "" : note, jump == null ? "" : jump}));
            if (lines.size() < 6) continue;
            break;
        }
        return lines.isEmpty() ? null : String.join("\n", lines);
    }

    private String serializeTrustCases(Object value, SiteConfig fallback) {
        if (value == null) {
            return fallback == null ? null : fallback.getTrustCases();
        }
        ArrayList<ApiDtos.TrustCaseResponse> rows = new ArrayList<ApiDtos.TrustCaseResponse>();
        if (value instanceof List) {
            List list = (List)value;
            for (Object item : list) {
                if (item instanceof ApiDtos.TrustCaseResponse) {
                    ApiDtos.TrustCaseResponse c = (ApiDtos.TrustCaseResponse)item;
                    rows.add(c);
                    continue;
                }
                if (!(item instanceof Map)) continue;
                Map m = (Map)item;
                rows.add(new ApiDtos.TrustCaseResponse(this.stringValue(m.get("tag")), this.stringValue(m.get("title")), this.stringValue(m.get("metric")), this.stringValue(m.get("detail"))));
            }
        }
        ArrayList<String> lines = new ArrayList<String>();
        for (ApiDtos.TrustCaseResponse row : rows) {
            String tag = this.clean(row.tag(), 40);
            String title = this.clean(row.title(), 100);
            String metric = this.clean(row.metric(), 120);
            String detail = this.clean(row.detail(), 240);
            if (title == null && metric == null && detail == null) continue;
            lines.add(String.join((CharSequence)"\t", tag == null ? "\u6848\u4f8b" : tag, title == null ? "\u670d\u52a1\u6848\u4f8b" : title, metric == null ? "\u6309\u8fdb\u5ea6\u786e\u8ba4" : metric, detail == null ? "" : detail));
            if (lines.size() < 8) continue;
            break;
        }
        return lines.isEmpty() ? null : String.join("\n", lines);
    }

    private String serializeTrustFaq(Object value, SiteConfig fallback) {
        if (value == null) {
            return fallback == null ? null : fallback.getTrustFaq();
        }
        ArrayList<ApiDtos.TrustFaqResponse> rows = new ArrayList<ApiDtos.TrustFaqResponse>();
        if (value instanceof List) {
            List list = (List)value;
            for (Object item : list) {
                if (item instanceof ApiDtos.TrustFaqResponse) {
                    ApiDtos.TrustFaqResponse f = (ApiDtos.TrustFaqResponse)item;
                    rows.add(f);
                    continue;
                }
                if (!(item instanceof Map)) continue;
                Map m = (Map)item;
                rows.add(new ApiDtos.TrustFaqResponse(this.stringValue(m.get("question")), this.stringValue(m.get("answer"))));
            }
        }
        ArrayList<String> lines = new ArrayList<>();
        for (ApiDtos.TrustFaqResponse row : rows) {
            String question = this.clean(row.question(), 100);
            String answer = this.clean(row.answer(), 400);
            if (question == null || answer == null) continue;
            lines.add(question + "\t" + answer);
            if (lines.size() < 12) continue;
            break;
        }
        return lines.isEmpty() ? null : String.join("\n", lines);
    }

    private List<ApiDtos.TrustCaseResponse> defaultTrustCases() {
        return List.of(new ApiDtos.TrustCaseResponse("\u6b66\u5668\u5347\u7ea7", "MW4 \u6b66\u5668 1\u2192\u6ee1\u7ea7\u914d\u4ef6", "\u7ea6 50\u201390 \u5206\u949f \u00b7 \u00a535\u201360", "\u6309\u5f53\u524d\u7b49\u7ea7\u62b5\u6263\uff0c\u5b8c\u6210\u540e\u53ef\u5728\u4e1a\u52a1\u8bb0\u5f55\u67e5\u770b\u8fdb\u5ea6\u8bf4\u660e\u3002"), new ApiDtos.TrustCaseResponse("\u6d3b\u52a8\u4efb\u52a1", "\u8d5b\u5b63\u9650\u65f6\u4efb\u52a1\u6e05\u5355", "\u7ea6 3\u20136 \u5c0f\u65f6 \u00b7 \u6309\u4efb\u52a1\u62a5\u4ef7", "\u5148\u6838\u5bf9\u5269\u4f59\u4efb\u52a1\u4e0e\u622a\u6b62\u65f6\u95f4\uff0c\u518d\u786e\u8ba4\u662f\u5426\u5f00\u5de5\u3002"), new ApiDtos.TrustCaseResponse("\u8ff7\u5f69\u6311\u6218", "\u5269\u4f59\u6311\u6218\u8865\u9f50", "\u5f53\u5929\u53ef\u4ea4\u4ed8 \u00b7 \u6309\u622a\u56fe\u4f30\u4ef7", "\u6309\u5269\u4f59\u8ff7\u5f69\u6311\u6218\u9879\u76ee\u786e\u8ba4\uff0c\u6b63\u5f0f\u540d\u79f0\u4ee5\u5b98\u65b9\u516c\u5e03\u4e3a\u51c6\uff1b\u7f51\u9875\u4e0d\u6536\u5bc6\u7801\u3002"), new ApiDtos.TrustCaseResponse("\u6392\u4f4d/\u6bb5\u4f4d", "\u5f53\u524d\u6bb5\u5230\u76ee\u6807\u6bb5", "\u6309\u6bb5\u5dee\u62a5\u4ef7 \u00b7 \u5f02\u5e38\u5373\u505c", "\u5f00\u5de5\u524d\u786e\u8ba4\u76ee\u6807\u3001\u5e73\u53f0\u4e0e\u53ef\u64cd\u4f5c\u65f6\u95f4\uff0c\u5f02\u5e38\u53ca\u65f6\u6c9f\u901a\u3002"));
    }

    private List<ApiDtos.TrustFaqResponse> defaultTrustFaq() {
        return List.of(new ApiDtos.TrustFaqResponse("\u600e\u4e48\u67e5\u8fdb\u5ea6\uff1f", "\u63d0\u4ea4\u540e\u4f1a\u751f\u6210\u4e1a\u52a1\u7f16\u53f7\uff0c\u5728\u300c\u4e1a\u52a1\u8bb0\u5f55\u300d\u8f93\u5165\u5373\u53ef\u67e5\u770b\u5f53\u524d\u72b6\u6001\u548c\u5ba2\u670d\u5907\u6ce8\u3002"), new ApiDtos.TrustFaqResponse("\u8981\u4e0d\u8981\u7ed9\u5bc6\u7801\uff1f", "\u7f51\u9875\u3001\u67e5\u8be2\u9875\u3001\u622a\u56fe\u90fd\u4e0d\u8981\u53d1\u5bc6\u7801\u3001\u9a8c\u8bc1\u7801\u6216\u4ee4\u724c\u3002\u5177\u4f53\u4e0a\u53f7\u65b9\u5f0f\u52a0\u5fae\u4fe1\u540e\u5355\u72ec\u786e\u8ba4\u3002"), new ApiDtos.TrustFaqResponse("\u600e\u4e48\u4ed8\u6b3e\uff1f", "\u786e\u8ba4\u62a5\u4ef7\u540e\u8d70\u6dd8\u5b9d\u6216\u95f2\u9c7c\u62c5\u4fdd\uff0c\u7f51\u9875\u4e0d\u6536\u94b1\u3002"), new ApiDtos.TrustFaqResponse("\u80fd\u53d6\u6d88\u5417\uff1f", "\u672a\u5f00\u5de5\u53ef\u53d6\u6d88\uff1b\u5df2\u5f00\u5de5\u6309\u5b8c\u6210\u8fdb\u5ea6\u6838\u5bf9\u540e\u9000\u5269\u4f59\u90e8\u5206\u3002"), new ApiDtos.TrustFaqResponse("\u652f\u6301\u54ea\u4e9b\u5e73\u53f0\uff1f", "Steam\u3001Battle.net\u3001PlayStation\u3001Xbox\u3001Switch 2 \u7b49\u5e73\u53f0\u6309\u5177\u4f53\u6e38\u620f\u786e\u8ba4\u3002"), new ApiDtos.TrustFaqResponse("\u6392\u4f4d\u4f1a\u4e0d\u4f1a\u6389\uff1f", "\u5f02\u5e38\u7acb\u5373\u505c\u5355\u5e76\u6c9f\u901a\uff0c\u4e0d\u786c\u51b2\u3002\u6392\u4f4d\u7c7b\u670d\u52a1\u4f1a\u5148\u786e\u8ba4\u5f53\u524d\u6bb5\u4f4d\u3001\u76ee\u6807\u6bb5\u4f4d\u548c\u98ce\u9669\u3002"), new ApiDtos.TrustFaqResponse("\u622a\u56fe\u600e\u4e48\u53d1\uff1f", "\u8be2\u4ef7\u9875\u53ef\u4e0a\u4f20\u4efb\u52a1\u622a\u56fe\uff0c\u4e5f\u53ef\u4ee5\u590d\u5236\u4e1a\u52a1\u7f16\u53f7\u540e\u52a0\u5fae\u4fe1\u8865\u53d1\u3002\u622a\u56fe\u91cc\u4e0d\u8981\u5305\u542b\u5bc6\u7801\u6216\u9a8c\u8bc1\u7801\u3002"), new ApiDtos.TrustFaqResponse("\u591a\u4e45\u56de\u590d\uff1f", "\u5ba2\u670d\u65f6\u95f4\u901a\u5e38\u4e3a 10:00\u201324:00\uff0c\u9ad8\u5cf0\u671f\u4f1a\u6309\u4e1a\u52a1\u7f16\u53f7\u987a\u5e8f\u5904\u7406\u3002"));
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private String clean(String value, int max) {
        if (value == null) {
            return null;
        }
        String out = value.trim().replace("\t", " ").replace("\r", " ").replace("\n", " ");
        if (out.isBlank()) {
            return null;
        }
        if (out.length() > max) {
            throw new BadRequestException("\u5ba2\u670d\u6e20\u9053\u5b57\u6bb5\u8fc7\u957f");
        }
        return out;
    }

    private String normalizeFit(String s) {
        String v;
        String string = v = s == null ? "COVER" : s.trim().toUpperCase(Locale.ROOT);
        if (!Set.of("COVER", "CONTAIN", "FILL").contains(v)) {
            throw new BadRequestException("\u975e\u6cd5\u56fe\u7247\u9002\u914d\u6a21\u5f0f");
        }
        return v;
    }

    private String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    @Generated
    public SiteConfigService(SiteConfigRepository repo, ServiceItemRepository serviceRepo, SlotService slotService, AuditService audit, GameStatusService gameStatuses, SiteSettingService siteSettings) {
        this.repo = repo;
        this.serviceRepo = serviceRepo;
        this.slotService = slotService;
        this.audit = audit;
        this.gameStatuses = gameStatuses;
        this.siteSettings = siteSettings;
    }
}


