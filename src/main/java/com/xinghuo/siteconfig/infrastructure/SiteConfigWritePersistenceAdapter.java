package com.xinghuo.siteconfig.infrastructure;

import com.xinghuo.exception.BadRequestException;
import com.xinghuo.model.SiteConfig;
import com.xinghuo.repository.SiteConfigRepository;
import com.xinghuo.siteconfig.application.SiteConfigFullUpdateCommand;
import com.xinghuo.siteconfig.application.SiteConfigWritePort;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

/** JPA-owned mutation adapter for the singleton SiteConfig aggregate. */
@Component
public class SiteConfigWritePersistenceAdapter implements SiteConfigWritePort {
    private final SiteConfigRepository repo;

    public SiteConfigWritePersistenceAdapter(SiteConfigRepository repo) {
        this.repo = repo;
    }

    @Override
    public void updateSiteImage(String slot, String url) {
        SiteConfig config = requireConfig();
        String key = slot == null ? "" : slot.trim().toUpperCase(Locale.ROOT);
        switch (key) {
            case "HERO" -> config.setHeroImage(blankToNull(url));
            case "BACKGROUND" -> config.setHeroBackgroundImage(blankToNull(url));
            case "BACKGROUND_MOBILE" -> config.setHeroBackgroundMobileImage(blankToNull(url));
            case "WECHAT_QR" -> config.setWechatQrImage(blankToNull(url));
            default -> throw new BadRequestException("非法图片位置");
        }
        config.setUpdatedAt(Instant.now());
    }

    @Override
    public void update(SiteConfigFullUpdateCommand command) {
        Map<String, Object> values = command.values();
        SiteConfig config = requireConfig();
        config.setBrandName(string(values, "brandName").trim());
        config.setLegalEntityName(string(values, "legalEntityName").trim());
        config.setServiceStatus(parseStatus(string(values, "serviceStatus")));
        config.setQuoteEnabled(booleanValue(values, "quoteEnabled"));
        config.setOrderQueryEnabled(booleanValue(values, "orderQueryEnabled"));
        config.setHeroTitle(string(values, "heroTitle").trim());
        config.setHeroSubtitle(string(values, "heroSubtitle").trim());
        config.setStartingPriceMode(parsePriceMode(string(values, "startingPriceMode")));
        config.setStartingPrice((BigDecimal) values.get("startingPrice"));
        config.setShowStartingPrice(booleanValue(values, "showStartingPrice"));
        config.setHomeServiceLimit(intValue(values, "homeServiceLimit"));
        config.setHomeServiceMobileLimit(intValue(values, "homeServiceMobileLimit"));
        config.setHomeServiceDesktopColumns(intValue(values, "homeServiceDesktopColumns"));
        config.setHomeServiceMobileColumns(intValue(values, "homeServiceMobileColumns"));
        config.setServiceCardDensity(normalizeDensity(string(values, "serviceCardDensity")));
        config.setServiceCardMobileDensity(normalizeDensity(string(values, "serviceCardMobileDensity")));
        config.setHeroImage(blankToNull(string(values, "heroImage")));
        config.setHeroImageHeight(intValue(values, "heroImageHeight"));
        config.setHeroImageFit(normalizeFit(string(values, "heroImageFit")));
        config.setHeroImagePositionX(intValue(values, "heroImagePositionX"));
        config.setHeroImagePositionY(intValue(values, "heroImagePositionY"));
        config.setHeroImageMobileHeight(intValue(values, "heroImageMobileHeight"));
        config.setHeroImageMobileFit(normalizeFit(string(values, "heroImageMobileFit")));
        config.setHeroImageMobilePositionX(intValue(values, "heroImageMobilePositionX"));
        config.setHeroImageMobilePositionY(intValue(values, "heroImageMobilePositionY"));
        config.setHeroBackgroundImage(blankToNull(string(values, "heroBackgroundImage")));
        config.setHeroBackgroundMobileImage(blankToNull(string(values, "heroBackgroundMobileImage")));
        config.setHeroBackgroundMobileHeight(intValue(values, "heroBackgroundMobileHeight"));
        config.setHeroBackgroundOpacity(intValue(values, "heroBackgroundOpacity"));
        config.setHeroBackgroundFit(normalizeFit(string(values, "heroBackgroundFit")));
        config.setHeroBackgroundPositionX(intValue(values, "heroBackgroundPositionX"));
        config.setHeroBackgroundPositionY(intValue(values, "heroBackgroundPositionY"));
        config.setHeroBackgroundBlur(intValue(values, "heroBackgroundBlur"));
        config.setHeroBackgroundMobileFit(normalizeFit(string(values, "heroBackgroundMobileFit")));
        config.setHeroBackgroundMobilePositionX(intValue(values, "heroBackgroundMobilePositionX"));
        config.setHeroBackgroundMobilePositionY(intValue(values, "heroBackgroundMobilePositionY"));
        config.setServiceCoverHeight(intValue(values, "serviceCoverHeight"));
        config.setServiceCoverFit(normalizeFit(string(values, "serviceCoverFit")));
        config.setServiceCoverPositionX(intValue(values, "serviceCoverPositionX"));
        config.setServiceCoverPositionY(intValue(values, "serviceCoverPositionY"));
        config.setServiceCoverMobileHeight(intValue(values, "serviceCoverMobileHeight"));
        config.setServiceCoverMobileFit(normalizeFit(string(values, "serviceCoverMobileFit")));
        config.setServiceCoverMobilePositionX(intValue(values, "serviceCoverMobilePositionX"));
        config.setServiceCoverMobilePositionY(intValue(values, "serviceCoverMobilePositionY"));
        config.setTrustTags(joinTags(values.get("trustTags")));
        config.setTrustCases(serializeTrustCases(values.get("trustCases"), config));
        config.setTrustCaseHomeLimit(intValue(values, "trustCaseHomeLimit"));
        config.setTrustCaseHomeMobileLimit(intValue(values, "trustCaseHomeMobileLimit"));
        config.setTrustFaq(serializeTrustFaq(values.get("trustFaq"), config));
        config.setTrustFaqHomeLimit(intValue(values, "trustFaqHomeLimit"));
        config.setTrustFaqHomeMobileLimit(intValue(values, "trustFaqHomeMobileLimit"));
        config.setAnnouncementEnabled(booleanValue(values, "announcementEnabled"));
        config.setAnnouncementText(blankToNull(string(values, "announcementText")));
        config.setContactTitle(string(values, "contactTitle").trim());
        config.setContactIntro(string(values, "contactIntro").trim());
        config.setWechatId(blankToNull(string(values, "wechatId")));
        config.setWechatQrImage(blankToNull(string(values, "wechatQrImage")));
        config.setWechatQrSize(intValue(values, "wechatQrSize"));
        config.setContactChannels(serializeContactChannels(values.get("contactChannels"), config));
        config.setHomeContactLimit(intValue(values, "homeContactLimit"));
        config.setHomeContactMobileLimit(intValue(values, "homeContactMobileLimit"));
        config.setQqId(blankToNull(string(values, "qqId")));
        config.setQqLink(blankToNull(string(values, "qqLink")));
        config.setPhone(blankToNull(string(values, "phone")));
        config.setServiceHours(blankToNull(string(values, "serviceHours")));
        config.setTaobaoUrl(blankToNull(string(values, "taobaoUrl")));
        config.setXianyuUrl(blankToNull(string(values, "xianyuUrl")));
        config.setContactNotice(string(values, "contactNotice").trim());
        config.setQuoteSuccessText(string(values, "quoteSuccessText").trim());
        config.setOrderHelpText(string(values, "orderHelpText").trim());
        config.setFooterText(string(values, "footerText").trim());
        config.setSeoTitle(blankToNull(string(values, "seoTitle")));
        config.setSeoDescription(blankToNull(string(values, "seoDescription")));
        config.setUpdatedAt(Instant.now());
    }

    @Override
    public void updateSection(String section, Map<String, Object> values) {
        SiteConfig config = requireConfig();
        String key = section == null ? "" : section.trim().toLowerCase(Locale.ROOT);
        switch (key) {
            case "basic" -> {
                config.setBrandName(text(values, "brandName", config.getBrandName(), 80));
                config.setLegalEntityName(text(values, "legalEntityName", config.getLegalEntityName(), 120));
                config.setServiceStatus(parseStatus(text(values, "serviceStatus", config.getServiceStatus().name(), 16)));
                config.setStatusShortText(nullableText(values, "statusShortText", 120));
                config.setPrimaryButtonLabel(nullableText(values, "primaryButtonLabel", 40));
                config.setQuoteEnabled(bool(values, "quoteEnabled", config.isQuoteEnabled()));
                config.setOrderQueryEnabled(bool(values, "orderQueryEnabled", config.isOrderQueryEnabled()));
                config.setLegalEntityName(text(values, "legalEntityName", config.getLegalEntityName(), 120));
            }
            case "home" -> {
                config.setHeroTitle(text(values, "heroTitle", config.getHeroTitle(), 120));
                config.setHeroSubtitle(text(values, "heroSubtitle", config.getHeroSubtitle(), 240));
                config.setStartingPriceMode(parsePriceMode(text(values, "startingPriceMode", config.getStartingPriceMode().name(), 16)));
                config.setStartingPrice(decimal(values, "startingPrice", config.getStartingPrice()));
                config.setShowStartingPrice(bool(values, "showStartingPrice", config.isShowStartingPrice()));
                config.setHomeServiceLimit(integer(values, "homeServiceLimit", config.getHomeServiceLimit(), 2, 8));
                config.setHomeServiceMobileLimit(integer(values, "homeServiceMobileLimit", config.getHomeServiceMobileLimit(), 2, 8));
                config.setHomeServiceDesktopColumns(integer(values, "homeServiceDesktopColumns", config.getHomeServiceDesktopColumns(), 1, 4));
                config.setHomeServiceMobileColumns(integer(values, "homeServiceMobileColumns", config.getHomeServiceMobileColumns(), 1, 2));
                config.setServiceCardDensity(normalizeDensity(text(values, "serviceCardDensity", config.getServiceCardDensity(), 16)));
                config.setServiceCardMobileDensity(normalizeDensity(text(values, "serviceCardMobileDensity", config.getServiceCardMobileDensity(), 16)));
                config.setHomeContactLimit(integer(values, "homeContactLimit", config.getHomeContactLimit(), 1, 6));
                config.setHomeContactMobileLimit(integer(values, "homeContactMobileLimit", config.getHomeContactMobileLimit(), 1, 6));
                config.setTrustCaseHomeLimit(integer(values, "trustCaseHomeLimit", config.getTrustCaseHomeLimit(), 1, 8));
                config.setTrustCaseHomeMobileLimit(integer(values, "trustCaseHomeMobileLimit", config.getTrustCaseHomeMobileLimit(), 1, 8));
                config.setTrustFaqHomeLimit(integer(values, "trustFaqHomeLimit", config.getTrustFaqHomeLimit(), 1, 12));
                config.setTrustFaqHomeMobileLimit(integer(values, "trustFaqHomeMobileLimit", config.getTrustFaqHomeMobileLimit(), 1, 12));
                Object tags = values.get("trustTags");
                if (tags instanceof List<?> list) {
                    config.setTrustTags(String.join("|", list.stream()
                            .map(String::valueOf)
                            .map(String::trim)
                            .filter(value -> !value.isBlank())
                            .limit(6L)
                            .toList()));
                }
            }
            case "visual" -> {
                config.setHeroImage(nullableText(values, "heroImage", 500));
                config.setHeroImageHeight(integer(values, "heroImageHeight", config.getHeroImageHeight(), 80, 420));
                config.setHeroImageFit(normalizeFit(text(values, "heroImageFit", config.getHeroImageFit(), 16)));
                config.setHeroImagePositionX(integer(values, "heroImagePositionX", config.getHeroImagePositionX(), 0, 100));
                config.setHeroImagePositionY(integer(values, "heroImagePositionY", config.getHeroImagePositionY(), 0, 100));
                config.setHeroImageMobileHeight(integer(values, "heroImageMobileHeight", config.getHeroImageMobileHeight(), 80, 420));
                config.setHeroImageMobileFit(normalizeFit(text(values, "heroImageMobileFit", config.getHeroImageMobileFit(), 16)));
                config.setHeroImageMobilePositionX(integer(values, "heroImageMobilePositionX", config.getHeroImageMobilePositionX(), 0, 100));
                config.setHeroImageMobilePositionY(integer(values, "heroImageMobilePositionY", config.getHeroImageMobilePositionY(), 0, 100));
                config.setHeroBackgroundImage(nullableText(values, "heroBackgroundImage", 500));
                config.setHeroBackgroundMobileImage(nullableText(values, "heroBackgroundMobileImage", 500));
                config.setHeroBackgroundMobileHeight(integer(values, "heroBackgroundMobileHeight", config.getHeroBackgroundMobileHeight(), 360, 760));
                config.setHeroBackgroundOpacity(integer(values, "heroBackgroundOpacity", config.getHeroBackgroundOpacity(), 0, 100));
                config.setHeroBackgroundFit(normalizeFit(text(values, "heroBackgroundFit", config.getHeroBackgroundFit(), 16)));
                config.setHeroBackgroundPositionX(integer(values, "heroBackgroundPositionX", config.getHeroBackgroundPositionX(), 0, 100));
                config.setHeroBackgroundPositionY(integer(values, "heroBackgroundPositionY", config.getHeroBackgroundPositionY(), 0, 100));
                config.setHeroBackgroundBlur(integer(values, "heroBackgroundBlur", config.getHeroBackgroundBlur(), 0, 24));
                config.setHeroBackgroundMobileFit(normalizeFit(text(values, "heroBackgroundMobileFit", config.getHeroBackgroundMobileFit(), 16)));
                config.setHeroBackgroundMobilePositionX(integer(values, "heroBackgroundMobilePositionX", config.getHeroBackgroundMobilePositionX(), 0, 100));
                config.setHeroBackgroundMobilePositionY(integer(values, "heroBackgroundMobilePositionY", config.getHeroBackgroundMobilePositionY(), 0, 100));
                config.setServiceCoverHeight(integer(values, "serviceCoverHeight", config.getServiceCoverHeight(), 70, 240));
                config.setServiceCoverFit(normalizeFit(text(values, "serviceCoverFit", config.getServiceCoverFit(), 16)));
                config.setServiceCoverPositionX(integer(values, "serviceCoverPositionX", config.getServiceCoverPositionX(), 0, 100));
                config.setServiceCoverPositionY(integer(values, "serviceCoverPositionY", config.getServiceCoverPositionY(), 0, 100));
                config.setServiceCoverMobileHeight(integer(values, "serviceCoverMobileHeight", config.getServiceCoverMobileHeight(), 70, 240));
                config.setServiceCoverMobileFit(normalizeFit(text(values, "serviceCoverMobileFit", config.getServiceCoverMobileFit(), 16)));
                config.setServiceCoverMobilePositionX(integer(values, "serviceCoverMobilePositionX", config.getServiceCoverMobilePositionX(), 0, 100));
                config.setServiceCoverMobilePositionY(integer(values, "serviceCoverMobilePositionY", config.getServiceCoverMobilePositionY(), 0, 100));
            }
            case "notice" -> {
                config.setAnnouncementEnabled(bool(values, "announcementEnabled", config.isAnnouncementEnabled()));
                config.setAnnouncementText(nullableText(values, "announcementText", 240));
            }
            case "contact" -> {
                config.setContactTitle(text(values, "contactTitle", config.getContactTitle(), 80));
                config.setContactIntro(text(values, "contactIntro", config.getContactIntro(), 300));
                config.setWechatId(nullableText(values, "wechatId", 100));
                config.setWechatQrImage(nullableText(values, "wechatQrImage", 500));
                config.setWechatQrSize(integer(values, "wechatQrSize", config.getWechatQrSize(), 120, 420));
                config.setContactChannels(serializeContactChannels(values.get("contactChannels"), config));
                config.setQqId(nullableText(values, "qqId", 100));
                config.setQqLink(nullableText(values, "qqLink", 500));
                config.setPhone(nullableText(values, "phone", 50));
                config.setServiceHours(nullableText(values, "serviceHours", 100));
                config.setTaobaoUrl(nullableText(values, "taobaoUrl", 500));
                config.setXianyuUrl(nullableText(values, "xianyuUrl", 500));
                config.setContactNotice(text(values, "contactNotice", config.getContactNotice(), 500));
                config.setWecomKefuUrl(nullableText(values, "wecomKefuUrl", 500));
                config.setWecomKefuNote(nullableText(values, "wecomKefuNote", 300));
                config.setQuickContactTitle(nullableText(values, "quickContactTitle", 40));
                config.setTaobaoLabel(nullableText(values, "taobaoLabel", 40));
                config.setXianyuLabel(nullableText(values, "xianyuLabel", 40));
                config.setWecomKefuLabel(nullableText(values, "wecomKefuLabel", 40));
                config.setWecomKefuButton(nullableText(values, "wecomKefuButton", 40));
            }
            case "trust" -> {
                config.setTrustCases(serializeTrustCases(values.get("trustCases"), config));
                config.setTrustFaq(serializeTrustFaq(values.get("trustFaq"), config));
            }
            case "copy" -> {
                config.setQuoteSuccessText(text(values, "quoteSuccessText", config.getQuoteSuccessText(), 300));
                config.setOrderHelpText(text(values, "orderHelpText", config.getOrderHelpText(), 300));
                config.setFooterText(text(values, "footerText", config.getFooterText(), 200));
            }
            case "seo" -> {
                config.setSeoTitle(nullableText(values, "seoTitle", 160));
                config.setSeoDescription(nullableText(values, "seoDescription", 300));
            }
            case "tools" -> {
                config.setToolUpdatedNote(nullableText(values, "toolUpdatedNote", 120));
                config.setToolPublicEnabled(bool(values, "toolPublicEnabled", config.isToolPublicEnabled()));
            }
            default -> throw new BadRequestException("非法配置分区");
        }
        config.setUpdatedAt(Instant.now());
    }

    private SiteConfig requireConfig() {
        return repo.findById(1L).orElseThrow();
    }

    private String string(Map<String, Object> values, String key) {
        Object value = values.get(key);
        return value == null ? null : String.valueOf(value);
    }

    private boolean booleanValue(Map<String, Object> values, String key) {
        Object value = values.get(key);
        return value instanceof Boolean b ? b : Boolean.parseBoolean(String.valueOf(value));
    }

    private int intValue(Map<String, Object> values, String key) {
        Object value = values.get(key);
        return value instanceof Number number ? number.intValue() : Integer.parseInt(String.valueOf(value));
    }

    private String joinTags(Object value) {
        if (!(value instanceof List<?> list)) {
            return "";
        }
        return String.join("|", list.stream()
                .map(String::valueOf)
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .toList());
    }

    private String text(Map<String, Object> values, String key, String defaultValue, int max) {
        Object value = values.get(key);
        String text = value == null ? defaultValue : String.valueOf(value).trim();
        if (text == null || text.isBlank() || text.length() > max) {
            throw new BadRequestException("字段 " + key + " 不合法");
        }
        return text;
    }

    private String nullableText(Map<String, Object> values, String key, int max) {
        Object value = values.get(key);
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        if (text.isBlank()) {
            return null;
        }
        if (text.length() > max) {
            throw new BadRequestException("字段 " + key + " 过长");
        }
        return text;
    }

    private boolean bool(Map<String, Object> values, String key, boolean defaultValue) {
        Object value = values.get(key);
        return value == null ? defaultValue : Boolean.parseBoolean(String.valueOf(value));
    }

    private int integer(Map<String, Object> values, String key, int defaultValue, int min, int max) {
        Object value = values.get(key);
        int number = value == null ? defaultValue : Integer.parseInt(String.valueOf(value));
        if (number < min || number > max) {
            throw new BadRequestException("字段 " + key + " 超出范围");
        }
        return number;
    }

    private BigDecimal decimal(Map<String, Object> values, String key, BigDecimal defaultValue) {
        Object value = values.get(key);
        if (value == null) {
            return defaultValue;
        }
        try {
            BigDecimal number = new BigDecimal(String.valueOf(value));
            if (number.signum() < 0) {
                throw new IllegalArgumentException();
            }
            return number;
        }
        catch (Exception e) {
            throw new BadRequestException("价格不合法");
        }
    }

    private SiteConfig.ServiceStatus parseStatus(String value) {
        try {
            return SiteConfig.ServiceStatus.valueOf(value.toUpperCase(Locale.ROOT));
        }
        catch (Exception e) {
            throw new BadRequestException("非法接单状态");
        }
    }

    private SiteConfig.StartingPriceMode parsePriceMode(String value) {
        try {
            return SiteConfig.StartingPriceMode.valueOf(value.toUpperCase(Locale.ROOT));
        }
        catch (Exception e) {
            throw new BadRequestException("非法起步价模式");
        }
    }

    private String serializeContactChannels(Object value, SiteConfig fallback) {
        if (value == null) {
            return fallback == null ? null : fallback.getContactChannels();
        }
        ArrayList<Map<?, ?>> rows = new ArrayList<>();
        if (value instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Map<?, ?> map) {
                    rows.add(map);
                }
            }
        }
        ArrayList<String> lines = new ArrayList<>();
        for (Map<?, ?> row : rows) {
            String label = clean(stringValue(row.get("label")), 40);
            String wechat = clean(stringValue(row.get("wechatId")), 100);
            String qr = clean(stringValue(row.get("qrImage")), 500);
            String note = clean(stringValue(row.get("note")), 120);
            String jump = clean(stringValue(row.get("jumpUrl")), 500);
            if (label == null && wechat == null && qr == null) {
                continue;
            }
            lines.add(String.join("\t",
                    label == null ? "客服" + (lines.size() + 1) : label,
                    wechat == null ? "" : wechat,
                    qr == null ? "" : qr,
                    note == null ? "" : note,
                    jump == null ? "" : jump));
            if (lines.size() >= 6) {
                break;
            }
        }
        return lines.isEmpty() ? null : String.join("\n", lines);
    }

    private String serializeTrustCases(Object value, SiteConfig fallback) {
        if (value == null) {
            return fallback == null ? null : fallback.getTrustCases();
        }
        ArrayList<Map<?, ?>> rows = new ArrayList<>();
        if (value instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Map<?, ?> map) {
                    rows.add(map);
                }
            }
        }
        ArrayList<String> lines = new ArrayList<>();
        for (Map<?, ?> row : rows) {
            String tag = clean(stringValue(row.get("tag")), 40);
            String title = clean(stringValue(row.get("title")), 100);
            String metric = clean(stringValue(row.get("metric")), 120);
            String detail = clean(stringValue(row.get("detail")), 240);
            if (title == null && metric == null && detail == null) {
                continue;
            }
            lines.add(String.join("\t",
                    tag == null ? "案例" : tag,
                    title == null ? "服务案例" : title,
                    metric == null ? "按进度确认" : metric,
                    detail == null ? "" : detail));
            if (lines.size() >= 8) {
                break;
            }
        }
        return lines.isEmpty() ? null : String.join("\n", lines);
    }

    private String serializeTrustFaq(Object value, SiteConfig fallback) {
        if (value == null) {
            return fallback == null ? null : fallback.getTrustFaq();
        }
        ArrayList<Map<?, ?>> rows = new ArrayList<>();
        if (value instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Map<?, ?> map) {
                    rows.add(map);
                }
            }
        }
        ArrayList<String> lines = new ArrayList<>();
        for (Map<?, ?> row : rows) {
            String question = clean(stringValue(row.get("question")), 100);
            String answer = clean(stringValue(row.get("answer")), 400);
            if (question == null || answer == null) {
                continue;
            }
            lines.add(question + "\t" + answer);
            if (lines.size() >= 12) {
                break;
            }
        }
        return lines.isEmpty() ? null : String.join("\n", lines);
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

    private String normalizeFit(String value) {
        String normalized = value == null ? "COVER" : value.trim().toUpperCase(Locale.ROOT);
        if (!Set.of("COVER", "CONTAIN", "FILL").contains(normalized)) {
            throw new BadRequestException("非法图片适配模式");
        }
        return normalized;
    }

    private String normalizeDensity(String value) {
        String normalized = value == null ? "COMFORTABLE" : value.trim().toUpperCase(Locale.ROOT);
        if (!Set.of("COMPACT", "COMFORTABLE", "SPACIOUS").contains(normalized)) {
            throw new BadRequestException("非法组件密度");
        }
        return normalized;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
