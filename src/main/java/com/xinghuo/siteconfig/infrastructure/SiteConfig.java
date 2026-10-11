package com.xinghuo.siteconfig.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "site_config")
@Getter
@Setter
@NoArgsConstructor
public class SiteConfig {
    @Id
    private Long id;

    @Column(name = "brand_name", nullable = false, length = 80)
    private String brandName;

    @Column(name = "legal_entity_name", nullable = false, length = 120)
    private String legalEntityName;

    @Enumerated(EnumType.STRING)
    @Column(name = "service_status", nullable = false, length = 16)
    private ServiceStatus serviceStatus;

    @Column(name = "status_short_text", length = 120)
    private String statusShortText;

    @Column(name = "primary_button_label", length = 40)
    private String primaryButtonLabel;

    @Column(name = "quote_enabled", nullable = false)
    private boolean quoteEnabled;

    @Column(name = "order_query_enabled", nullable = false)
    private boolean orderQueryEnabled;

    @Column(name = "hero_title", nullable = false, length = 120)
    private String heroTitle;

    @Column(name = "hero_subtitle", nullable = false, length = 240)
    private String heroSubtitle;

    @Enumerated(EnumType.STRING)
    @Column(name = "starting_price_mode", nullable = false, length = 16)
    private StartingPriceMode startingPriceMode;

    @Column(name = "starting_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal startingPrice;

    @Column(name = "show_starting_price", nullable = false)
    private boolean showStartingPrice;

    @Column(name = "home_service_limit", nullable = false)
    private int homeServiceLimit;

    @Column(name = "home_service_mobile_limit", nullable = false)
    private int homeServiceMobileLimit;

    @Column(name = "home_service_desktop_columns", nullable = false)
    private int homeServiceDesktopColumns;

    @Column(name = "home_service_mobile_columns", nullable = false)
    private int homeServiceMobileColumns;

    @Column(name = "service_card_density", nullable = false, length = 16)
    private String serviceCardDensity;

    @Column(name = "service_card_mobile_density", nullable = false, length = 16)
    private String serviceCardMobileDensity;

    @Column(name = "hero_image", length = 500)
    private String heroImage;

    @Column(name = "hero_image_height", nullable = false)
    private int heroImageHeight;

    @Column(name = "hero_image_fit", nullable = false, length = 16)
    private String heroImageFit;

    @Column(name = "hero_image_position_x", nullable = false)
    private int heroImagePositionX;

    @Column(name = "hero_image_position_y", nullable = false)
    private int heroImagePositionY;

    @Column(name = "hero_image_mobile_height", nullable = false)
    private int heroImageMobileHeight;

    @Column(name = "hero_image_mobile_fit", nullable = false, length = 16)
    private String heroImageMobileFit;

    @Column(name = "hero_image_mobile_position_x", nullable = false)
    private int heroImageMobilePositionX;

    @Column(name = "hero_image_mobile_position_y", nullable = false)
    private int heroImageMobilePositionY;

    @Column(name = "hero_background_image", length = 500)
    private String heroBackgroundImage;

    @Column(name = "hero_background_mobile_image", length = 500)
    private String heroBackgroundMobileImage;

    @Column(name = "hero_background_mobile_height", nullable = false)
    private int heroBackgroundMobileHeight;

    @Column(name = "hero_background_opacity", nullable = false)
    private int heroBackgroundOpacity;

    @Column(name = "hero_background_fit", nullable = false, length = 16)
    private String heroBackgroundFit;

    @Column(name = "hero_background_position_x", nullable = false)
    private int heroBackgroundPositionX;

    @Column(name = "hero_background_position_y", nullable = false)
    private int heroBackgroundPositionY;

    @Column(name = "hero_background_blur", nullable = false)
    private int heroBackgroundBlur;

    @Column(name = "hero_background_mobile_fit", nullable = false, length = 16)
    private String heroBackgroundMobileFit;

    @Column(name = "hero_background_mobile_position_x", nullable = false)
    private int heroBackgroundMobilePositionX;

    @Column(name = "hero_background_mobile_position_y", nullable = false)
    private int heroBackgroundMobilePositionY;

    @Column(name = "service_cover_height", nullable = false)
    private int serviceCoverHeight;

    @Column(name = "service_cover_fit", nullable = false, length = 16)
    private String serviceCoverFit;

    @Column(name = "service_cover_position_x", nullable = false)
    private int serviceCoverPositionX;

    @Column(name = "service_cover_position_y", nullable = false)
    private int serviceCoverPositionY;

    @Column(name = "service_cover_mobile_height", nullable = false)
    private int serviceCoverMobileHeight;

    @Column(name = "service_cover_mobile_fit", nullable = false, length = 16)
    private String serviceCoverMobileFit;

    @Column(name = "service_cover_mobile_position_x", nullable = false)
    private int serviceCoverMobilePositionX;

    @Column(name = "service_cover_mobile_position_y", nullable = false)
    private int serviceCoverMobilePositionY;

    @Column(name = "trust_tags", nullable = false, length = 500)
    private String trustTags;

    @Column(name = "trust_cases", columnDefinition = "TEXT")
    private String trustCases;

    @Column(name = "trust_case_home_limit", nullable = false)
    private int trustCaseHomeLimit;

    @Column(name = "trust_case_home_mobile_limit", nullable = false)
    private int trustCaseHomeMobileLimit;

    @Column(name = "trust_faq", columnDefinition = "TEXT")
    private String trustFaq;

    @Column(name = "trust_faq_home_limit", nullable = false)
    private int trustFaqHomeLimit;

    @Column(name = "trust_faq_home_mobile_limit", nullable = false)
    private int trustFaqHomeMobileLimit;

    @Column(name = "announcement_enabled", nullable = false)
    private boolean announcementEnabled;

    @Column(name = "announcement_text", length = 240)
    private String announcementText;

    @Column(name = "contact_title", nullable = false, length = 80)
    private String contactTitle;

    @Column(name = "contact_intro", nullable = false, length = 300)
    private String contactIntro;

    @Column(name = "wechat_id", length = 100)
    private String wechatId;

    @Column(name = "wechat_qr_image", length = 500)
    private String wechatQrImage;

    @Column(name = "wechat_qr_size", nullable = false)
    private int wechatQrSize;

    @Column(name = "contact_channels", columnDefinition = "TEXT")
    private String contactChannels;

    @Column(name = "home_contact_limit", nullable = false)
    private int homeContactLimit;

    @Column(name = "home_contact_mobile_limit", nullable = false)
    private int homeContactMobileLimit;

    @Column(name = "qq_id", length = 100)
    private String qqId;

    @Column(name = "qq_link", length = 500)
    private String qqLink;

    @Column(name = "phone", length = 50)
    private String phone;

    @Column(name = "service_hours", length = 100)
    private String serviceHours;

    @Column(name = "taobao_url", length = 500)
    private String taobaoUrl;

    @Column(name = "xianyu_url", length = 500)
    private String xianyuUrl;

    @Column(name = "contact_notice", nullable = false, length = 500)
    private String contactNotice;

    @Column(name = "wecom_kefu_url", length = 500)
    private String wecomKefuUrl;

    @Column(name = "wecom_kefu_note", length = 300)
    private String wecomKefuNote;

    @Column(name = "quick_contact_title", length = 40)
    private String quickContactTitle;

    @Column(name = "taobao_label", length = 40)
    private String taobaoLabel;

    @Column(name = "xianyu_label", length = 40)
    private String xianyuLabel;

    @Column(name = "wecom_kefu_label", length = 40)
    private String wecomKefuLabel;

    @Column(name = "wecom_kefu_button", length = 40)
    private String wecomKefuButton;

    @Column(name = "quote_success_text", nullable = false, length = 300)
    private String quoteSuccessText;

    @Column(name = "order_help_text", nullable = false, length = 300)
    private String orderHelpText;

    @Column(name = "footer_text", nullable = false, length = 200)
    private String footerText;

    @Column(name = "seo_title", length = 160)
    private String seoTitle;

    @Column(name = "seo_description", length = 300)
    private String seoDescription;

    @Column(name = "tool_updated_note", length = 120)
    private String toolUpdatedNote;

    @Column(name = "tool_public_enabled", nullable = false)
    private boolean toolPublicEnabled;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private long version;

    @Builder
    public SiteConfig(
            Long id,
            String brandName,
            String legalEntityName,
            ServiceStatus serviceStatus,
            String statusShortText,
            String primaryButtonLabel,
            boolean quoteEnabled,
            boolean orderQueryEnabled,
            String heroTitle,
            String heroSubtitle,
            StartingPriceMode startingPriceMode,
            BigDecimal startingPrice,
            boolean showStartingPrice,
            int homeServiceLimit,
            String heroImage,
            int heroImageHeight,
            String heroImageFit,
            int heroImagePositionX,
            int heroImagePositionY,
            String heroBackgroundImage,
            int heroBackgroundOpacity,
            String heroBackgroundFit,
            int heroBackgroundPositionX,
            int heroBackgroundPositionY,
            int heroBackgroundBlur,
            int serviceCoverHeight,
            String serviceCoverFit,
            int serviceCoverPositionX,
            int serviceCoverPositionY,
            String trustTags,
            String trustCases,
            String trustFaq,
            boolean announcementEnabled,
            String announcementText,
            String contactTitle,
            String contactIntro,
            String wechatId,
            String wechatQrImage,
            int wechatQrSize,
            String contactChannels,
            String qqId,
            String qqLink,
            String phone,
            String serviceHours,
            String taobaoUrl,
            String xianyuUrl,
            String contactNotice,
            String quoteSuccessText,
            String orderHelpText,
            String footerText,
            String seoTitle,
            String seoDescription,
            String toolUpdatedNote,
            boolean toolPublicEnabled,
            Instant updatedAt,
            long version) {
        this.id = id;
        this.brandName = brandName;
        this.legalEntityName = legalEntityName;
        this.serviceStatus = serviceStatus;
        this.statusShortText = statusShortText;
        this.primaryButtonLabel = primaryButtonLabel;
        this.quoteEnabled = quoteEnabled;
        this.orderQueryEnabled = orderQueryEnabled;
        this.heroTitle = heroTitle;
        this.heroSubtitle = heroSubtitle;
        this.startingPriceMode = startingPriceMode;
        this.startingPrice = startingPrice;
        this.showStartingPrice = showStartingPrice;
        this.homeServiceLimit = homeServiceLimit;
        this.heroImage = heroImage;
        this.heroImageHeight = heroImageHeight;
        this.heroImageFit = heroImageFit;
        this.heroImagePositionX = heroImagePositionX;
        this.heroImagePositionY = heroImagePositionY;
        this.heroBackgroundImage = heroBackgroundImage;
        this.heroBackgroundOpacity = heroBackgroundOpacity;
        this.heroBackgroundFit = heroBackgroundFit;
        this.heroBackgroundPositionX = heroBackgroundPositionX;
        this.heroBackgroundPositionY = heroBackgroundPositionY;
        this.heroBackgroundBlur = heroBackgroundBlur;
        this.serviceCoverHeight = serviceCoverHeight;
        this.serviceCoverFit = serviceCoverFit;
        this.serviceCoverPositionX = serviceCoverPositionX;
        this.serviceCoverPositionY = serviceCoverPositionY;
        this.trustTags = trustTags;
        this.trustCases = trustCases;
        this.trustFaq = trustFaq;
        this.announcementEnabled = announcementEnabled;
        this.announcementText = announcementText;
        this.contactTitle = contactTitle;
        this.contactIntro = contactIntro;
        this.wechatId = wechatId;
        this.wechatQrImage = wechatQrImage;
        this.wechatQrSize = wechatQrSize;
        this.contactChannels = contactChannels;
        this.qqId = qqId;
        this.qqLink = qqLink;
        this.phone = phone;
        this.serviceHours = serviceHours;
        this.taobaoUrl = taobaoUrl;
        this.xianyuUrl = xianyuUrl;
        this.contactNotice = contactNotice;
        this.quoteSuccessText = quoteSuccessText;
        this.orderHelpText = orderHelpText;
        this.footerText = footerText;
        this.seoTitle = seoTitle;
        this.seoDescription = seoDescription;
        this.toolUpdatedNote = toolUpdatedNote;
        this.toolPublicEnabled = toolPublicEnabled;
        this.updatedAt = updatedAt;
        this.version = version;
    }

    public enum ServiceStatus {
        PREHEAT,
        RESERVATION,
        OPEN,
        SUSPENDED,
        MAINTENANCE,
        BUSY,
        CLOSED;

        public boolean acceptingOrders() {
            return this == OPEN;
        }

        public boolean preLaunch() {
            return this == PREHEAT || this == RESERVATION || this == BUSY;
        }
    }

    public enum StartingPriceMode {
        MANUAL,
        AUTO
    }
}
