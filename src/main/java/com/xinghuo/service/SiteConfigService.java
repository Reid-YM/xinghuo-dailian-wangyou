package com.xinghuo.service;

import com.xinghuo.dto.ApiDtos;
import com.xinghuo.exception.BadRequestException;
import com.xinghuo.gamestatus.application.GameStatusApplicationService;
import com.xinghuo.gamestatus.application.GameStatusView;
import com.xinghuo.model.ServiceItem;
import com.xinghuo.model.SiteConfig;
import com.xinghuo.repository.ServiceItemRepository;
import com.xinghuo.repository.SiteConfigRepository;
import com.xinghuo.service.AuditService;
import com.xinghuo.sitesetting.application.SiteSettingApplicationService;
import com.xinghuo.slot.application.SlotApplicationService;
import com.xinghuo.slot.web.SlotWebMapper;
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
    private final SlotApplicationService slotService;
    private final AuditService audit;
    private final GameStatusApplicationService gameStatuses;
    private final SiteSettingApplicationService siteSettings;

    @Transactional(readOnly=true)
    public ApiDtos.SiteConfigResponse publicConfig() {
        SiteConfig c = this.get();
        return new ApiDtos.SiteConfigResponse(c.getBrandName(), c.getLegalEntityName(), this.mw4Status().name().toLowerCase(Locale.ROOT), this.mw4ShortText(c), this.mw4CtaLabel(c), c.isQuoteEnabled(), c.isOrderQueryEnabled(), c.getHeroTitle(), c.getHeroSubtitle(), c.getStartingPriceMode().name().toLowerCase(Locale.ROOT), this.displayStartingPrice(c), c.isShowStartingPrice(), c.getHomeServiceLimit(), c.getHomeServiceMobileLimit(), c.getHomeServiceDesktopColumns(), c.getHomeServiceMobileColumns(), this.normalizeDensity(c.getServiceCardDensity()).toLowerCase(Locale.ROOT), this.normalizeDensity(c.getServiceCardMobileDensity()).toLowerCase(Locale.ROOT), c.getHeroImage(), c.getHeroImageHeight(), c.getHeroImageFit().toLowerCase(Locale.ROOT), c.getHeroImagePositionX(), c.getHeroImagePositionY(), c.getHeroImageMobileHeight(), c.getHeroImageMobileFit().toLowerCase(Locale.ROOT), c.getHeroImageMobilePositionX(), c.getHeroImageMobilePositionY(), c.getHeroBackgroundImage(), c.getHeroBackgroundMobileImage(), c.getHeroBackgroundMobileHeight(), c.getHeroBackgroundOpacity(), c.getHeroBackgroundFit().toLowerCase(Locale.ROOT), c.getHeroBackgroundPositionX(), c.getHeroBackgroundPositionY(), c.getHeroBackgroundBlur(), c.getHeroBackgroundMobileFit().toLowerCase(Locale.ROOT), c.getHeroBackgroundMobilePositionX(), c.getHeroBackgroundMobilePositionY(), c.getServiceCoverHeight(), c.getServiceCoverFit().toLowerCase(Locale.ROOT), c.getServiceCoverPositionX(), c.getServiceCoverPositionY(), c.getServiceCoverMobileHeight(), c.getServiceCoverMobileFit().toLowerCase(Locale.ROOT), c.getServiceCoverMobilePositionX(), c.getServiceCoverMobilePositionY(), this.splitTags(c.getTrustTags()), this.parseTrustCases(c), c.getTrustCaseHomeLimit(), c.getTrustCaseHomeMobileLimit(), this.parseTrustFaq(c), c.getTrustFaqHomeLimit(), c.getTrustFaqHomeMobileLimit(), c.isAnnouncementEnabled(), c.getAnnouncementText(), c.getContactTitle(), c.getContactIntro(), c.getWechatId(), c.getWechatQrImage(), c.getWechatQrSize(), this.parseContactChannels(c), c.getHomeContactLimit(), c.getHomeContactMobileLimit(), c.getQqId(), c.getQqLink(), c.getPhone(), c.getServiceHours(), c.getTaobaoUrl(), c.getXianyuUrl(), c.getContactNotice(), c.getWecomKefuUrl(), c.getWecomKefuNote(), c.getQuickContactTitle(), c.getTaobaoLabel(), c.getXianyuLabel(), c.getWecomKefuLabel(), c.getWecomKefuButton(), c.getQuoteSuccessText(), c.getOrderHelpText(), c.getFooterText(), c.getSeoTitle(), c.getSeoDescription(), c.getToolUpdatedNote(), c.isToolPublicEnabled(), SlotWebMapper.toPublicResponses(this.slotService.publicHotServices()));
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
            case "BACKGROUND_MOBILE": {
                c.setHeroBackgroundMobileImage(this.blankToNull(url));
                break;
            }
            case "WECHAT_QR": {
                c.setWechatQrImage(this.blankToNull(url));
                break;
            }
            default: {
                throw new BadRequestException("非法图片位置");
            }
        }
        c.setUpdatedAt(Instant.now());
        ApiDtos.SiteConfigResponse after = this.publicConfig();
        this.audit.log("SITE_IMAGE_UPDATE", "SITE_CONFIG", key, operator, before, after, "更新站点图片", req);
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
        c.setHomeServiceMobileLimit(r.homeServiceMobileLimit());
        c.setHomeServiceDesktopColumns(r.homeServiceDesktopColumns());
        c.setHomeServiceMobileColumns(r.homeServiceMobileColumns());
        c.setServiceCardDensity(this.normalizeDensity(r.serviceCardDensity()));
        c.setServiceCardMobileDensity(this.normalizeDensity(r.serviceCardMobileDensity()));
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
        c.setHeroBackgroundMobileImage(this.blankToNull(r.heroBackgroundMobileImage()));
        c.setHeroBackgroundMobileHeight(r.heroBackgroundMobileHeight());
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
        c.setTrustCaseHomeLimit(r.trustCaseHomeLimit());
        c.setTrustCaseHomeMobileLimit(r.trustCaseHomeMobileLimit());
        c.setTrustFaq(this.serializeTrustFaq(r.trustFaq(), c));
        c.setTrustFaqHomeLimit(r.trustFaqHomeLimit());
        c.setTrustFaqHomeMobileLimit(r.trustFaqHomeMobileLimit());
        c.setAnnouncementEnabled(r.announcementEnabled());
        c.setAnnouncementText(this.blankToNull(r.announcementText()));
        c.setContactTitle(r.contactTitle().trim());
        c.setContactIntro(r.contactIntro().trim());
        c.setWechatId(this.blankToNull(r.wechatId()));
        c.setWechatQrImage(this.blankToNull(r.wechatQrImage()));
        c.setWechatQrSize(r.wechatQrSize());
        c.setContactChannels(this.serializeContactChannels(r.contactChannels(), c));
        c.setHomeContactLimit(r.homeContactLimit());
        c.setHomeContactMobileLimit(r.homeContactMobileLimit());
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
                c.setHomeServiceMobileLimit(this.integer(r, "homeServiceMobileLimit", c.getHomeServiceMobileLimit(), 2, 8));
                c.setHomeServiceDesktopColumns(this.integer(r, "homeServiceDesktopColumns", c.getHomeServiceDesktopColumns(), 1, 4));
                c.setHomeServiceMobileColumns(this.integer(r, "homeServiceMobileColumns", c.getHomeServiceMobileColumns(), 1, 2));
                c.setServiceCardDensity(this.normalizeDensity(this.text(r, "serviceCardDensity", c.getServiceCardDensity(), 16)));
                c.setServiceCardMobileDensity(this.normalizeDensity(this.text(r, "serviceCardMobileDensity", c.getServiceCardMobileDensity(), 16)));
                c.setHomeContactLimit(this.integer(r, "homeContactLimit", c.getHomeContactLimit(), 1, 6));
                c.setHomeContactMobileLimit(this.integer(r, "homeContactMobileLimit", c.getHomeContactMobileLimit(), 1, 6));
                c.setTrustCaseHomeLimit(this.integer(r, "trustCaseHomeLimit", c.getTrustCaseHomeLimit(), 1, 8));
                c.setTrustCaseHomeMobileLimit(this.integer(r, "trustCaseHomeMobileLimit", c.getTrustCaseHomeMobileLimit(), 1, 8));
                c.setTrustFaqHomeLimit(this.integer(r, "trustFaqHomeLimit", c.getTrustFaqHomeLimit(), 1, 12));
                c.setTrustFaqHomeMobileLimit(this.integer(r, "trustFaqHomeMobileLimit", c.getTrustFaqHomeMobileLimit(), 1, 12));
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
                c.setHeroBackgroundMobileImage(this.nullableText(r, "heroBackgroundMobileImage", 500));
                c.setHeroBackgroundMobileHeight(this.integer(r, "heroBackgroundMobileHeight", c.getHeroBackgroundMobileHeight(), 360, 760));
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
                throw new BadRequestException("非法配置分区");
            }
        }
        this.syncSiteSettings(c);
        c.setUpdatedAt(Instant.now());
        ApiDtos.SiteConfigResponse after = this.publicConfig();
        this.audit.log("SITE_CONFIG_" + key.toUpperCase(Locale.ROOT), "SITE_CONFIG", key, operator, before, after, String.valueOf(r.getOrDefault("reason", "后台分区保存")), req);
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
            throw new BadRequestException("字段 " + k + " 不合法");
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
            throw new BadRequestException("字段 " + k + " 过长");
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
            throw new BadRequestException("字段 " + k + " 超出范围");
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
            throw new BadRequestException("价格不合法");
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
        GameStatusView g = this.gameStatuses.resolve("CODMW4", null);
        return g == null ? fallback.getStatusShortText() : g.getPublicLabel();
    }

    private String mw4CtaLabel(SiteConfig fallback) {
        GameStatusView g = this.gameStatuses.resolve("CODMW4", null);
        return g == null || g.getCtaLabel() == null ? fallback.getPrimaryButtonLabel() : g.getCtaLabel();
    }

    private SiteConfig.ServiceStatus parseStatus(String s) {
        try {
            return SiteConfig.ServiceStatus.valueOf((String)s.toUpperCase(Locale.ROOT));
        }
        catch (Exception e) {
            throw new BadRequestException("非法接单状态");
        }
    }

    private SiteConfig.StartingPriceMode parsePriceMode(String s) {
        try {
            return SiteConfig.StartingPriceMode.valueOf((String)s.toUpperCase(Locale.ROOT));
        }
        catch (Exception e) {
            throw new BadRequestException("非法起步价模式");
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
                rows.add(new ApiDtos.ContactChannelResponse((String)(label == null ? "客服" + (rows.size() + 1) : label), wechat, qr, note, jump));
                if (rows.size() >= 6) break;
            }
        }
        if (rows.isEmpty() && (c.getWechatId() != null || c.getWechatQrImage() != null)) {
            rows.add(new ApiDtos.ContactChannelResponse("主客服", c.getWechatId(), c.getWechatQrImage(), "发送业务编号确认报价", null));
        }
        if (rows.isEmpty()) {
            rows.add(new ApiDtos.ContactChannelResponse("主客服", "AAC9878", null, "发送业务编号确认报价", null));
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
                rows.add(new ApiDtos.TrustCaseResponse(tag == null ? "服务示例" : tag, title == null ? "服务示例" : title, metric == null ? "按进度确认" : metric, detail == null ? "" : detail));
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
            lines.add(String.join((CharSequence)"\t", new CharSequence[]{label == null ? "客服" + (lines.size() + 1) : label, wechat == null ? "" : wechat, qr == null ? "" : qr, note == null ? "" : note, jump == null ? "" : jump}));
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
            lines.add(String.join((CharSequence)"\t", tag == null ? "案例" : tag, title == null ? "服务案例" : title, metric == null ? "按进度确认" : metric, detail == null ? "" : detail));
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
        return List.of(new ApiDtos.TrustCaseResponse("武器升级", "MW4 武器 1→满级配件", "约 50–90 分钟 · ¥35–60", "按当前等级抵扣，完成后可在业务记录查看进度说明。"), new ApiDtos.TrustCaseResponse("活动任务", "赛季限时任务清单", "约 3–6 小时 · 按任务报价", "先核对剩余任务与截止时间，再确认是否开工。"), new ApiDtos.TrustCaseResponse("迷彩挑战", "剩余挑战补齐", "当天可交付 · 按截图估价", "按剩余迷彩挑战项目确认，正式名称以官方公布为准；网页不收密码。"), new ApiDtos.TrustCaseResponse("排位/段位", "当前段到目标段", "按段差报价 · 异常即停", "开工前确认目标、平台与可操作时间，异常及时沟通。"));
    }

    private List<ApiDtos.TrustFaqResponse> defaultTrustFaq() {
        return List.of(new ApiDtos.TrustFaqResponse("怎么查进度？", "提交后会生成业务编号，在「业务记录」输入即可查看当前状态和客服备注。"), new ApiDtos.TrustFaqResponse("要不要给密码？", "网页、查询页、截图都不要发密码、验证码或令牌。具体上号方式加微信后单独确认。"), new ApiDtos.TrustFaqResponse("怎么付款？", "确认报价后走淘宝或闲鱼担保，网页不收钱。"), new ApiDtos.TrustFaqResponse("能取消吗？", "未开工可取消；已开工按完成进度核对后退剩余部分。"), new ApiDtos.TrustFaqResponse("支持哪些平台？", "Steam、Battle.net、PlayStation、Xbox、Switch 2 等平台按具体游戏确认。"), new ApiDtos.TrustFaqResponse("排位会不会掉？", "异常立即停单并沟通，不硬冲。排位类服务会先确认当前段位、目标段位和风险。"), new ApiDtos.TrustFaqResponse("截图怎么发？", "询价页可上传任务截图，也可以复制业务编号后加微信补发。截图里不要包含密码或验证码。"), new ApiDtos.TrustFaqResponse("多久回复？", "客服时间通常为 10:00–24:00，高峰期会按业务编号顺序处理。"));
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
            throw new BadRequestException("客服渠道字段过长");
        }
        return out;
    }

    private String normalizeFit(String s) {
        String v;
        String string = v = s == null ? "COVER" : s.trim().toUpperCase(Locale.ROOT);
        if (!Set.of("COVER", "CONTAIN", "FILL").contains(v)) {
            throw new BadRequestException("非法图片适配模式");
        }
        return v;
    }

    private String normalizeDensity(String s) {
        String v;
        String string = v = s == null ? "COMFORTABLE" : s.trim().toUpperCase(Locale.ROOT);
        if (!Set.of("COMPACT", "COMFORTABLE", "SPACIOUS").contains(v)) {
            throw new BadRequestException("非法组件密度");
        }
        return v;
    }

    private String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    @Generated
    public SiteConfigService(SiteConfigRepository repo, ServiceItemRepository serviceRepo, SlotApplicationService slotService, AuditService audit, GameStatusApplicationService gameStatuses, SiteSettingApplicationService siteSettings) {
        this.repo = repo;
        this.serviceRepo = serviceRepo;
        this.slotService = slotService;
        this.audit = audit;
        this.gameStatuses = gameStatuses;
        this.siteSettings = siteSettings;
    }
}

