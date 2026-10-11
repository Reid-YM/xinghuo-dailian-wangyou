/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  jakarta.persistence.Column
 *  jakarta.persistence.Entity
 *  jakarta.persistence.EnumType
 *  jakarta.persistence.Enumerated
 *  jakarta.persistence.Id
 *  jakarta.persistence.Table
 *  jakarta.persistence.Version
 *  lombok.Generated
 */
package com.xinghuo.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Generated;

@Entity
@Table(name="site_config")
public class SiteConfig {
    @Id
    private Long id;
    @Column(name="brand_name", nullable=false, length=80)
    private String brandName;
    @Column(name="legal_entity_name", nullable=false, length=120)
    private String legalEntityName;
    @Enumerated(value=EnumType.STRING)
    @Column(name="service_status", nullable=false, length=16)
    private ServiceStatus serviceStatus;
    @Column(name="status_short_text", length=120)
    private String statusShortText;
    @Column(name="primary_button_label", length=40)
    private String primaryButtonLabel;
    @Column(name="quote_enabled", nullable=false)
    private boolean quoteEnabled;
    @Column(name="order_query_enabled", nullable=false)
    private boolean orderQueryEnabled;
    @Column(name="hero_title", nullable=false, length=120)
    private String heroTitle;
    @Column(name="hero_subtitle", nullable=false, length=240)
    private String heroSubtitle;
    @Enumerated(value=EnumType.STRING)
    @Column(name="starting_price_mode", nullable=false, length=16)
    private StartingPriceMode startingPriceMode;
    @Column(name="starting_price", nullable=false, precision=10, scale=2)
    private BigDecimal startingPrice;
    @Column(name="show_starting_price", nullable=false)
    private boolean showStartingPrice;
    @Column(name="home_service_limit", nullable=false)
    private int homeServiceLimit;
    @Column(name="hero_image", length=500)
    private String heroImage;
    @Column(name="hero_image_height", nullable=false)
    private int heroImageHeight;
    @Column(name="hero_image_fit", nullable=false, length=16)
    private String heroImageFit;
    @Column(name="hero_image_position_x", nullable=false)
    private int heroImagePositionX;
    @Column(name="hero_image_position_y", nullable=false)
    private int heroImagePositionY;
    @Column(name="hero_background_image", length=500)
    private String heroBackgroundImage;
    @Column(name="hero_background_opacity", nullable=false)
    private int heroBackgroundOpacity;
    @Column(name="hero_background_fit", nullable=false, length=16)
    private String heroBackgroundFit;
    @Column(name="hero_background_position_x", nullable=false)
    private int heroBackgroundPositionX;
    @Column(name="hero_background_position_y", nullable=false)
    private int heroBackgroundPositionY;
    @Column(name="hero_background_blur", nullable=false)
    private int heroBackgroundBlur;
    @Column(name="service_cover_height", nullable=false)
    private int serviceCoverHeight;
    @Column(name="service_cover_fit", nullable=false, length=16)
    private String serviceCoverFit;
    @Column(name="service_cover_position_x", nullable=false)
    private int serviceCoverPositionX;
    @Column(name="service_cover_position_y", nullable=false)
    private int serviceCoverPositionY;
    @Column(name="trust_tags", nullable=false, length=500)
    private String trustTags;
    @Column(name="trust_cases", columnDefinition="TEXT")
    private String trustCases;
    @Column(name="trust_faq", columnDefinition="TEXT")
    private String trustFaq;
    @Column(name="announcement_enabled", nullable=false)
    private boolean announcementEnabled;
    @Column(name="announcement_text", length=240)
    private String announcementText;
    @Column(name="contact_title", nullable=false, length=80)
    private String contactTitle;
    @Column(name="contact_intro", nullable=false, length=300)
    private String contactIntro;
    @Column(name="wechat_id", length=100)
    private String wechatId;
    @Column(name="wechat_qr_image", length=500)
    private String wechatQrImage;
    @Column(name="wechat_qr_size", nullable=false)
    private int wechatQrSize;
    @Column(name="contact_channels", columnDefinition="TEXT")
    private String contactChannels;
    @Column(name="qq_id", length=100)
    private String qqId;
    @Column(name="qq_link", length=500)
    private String qqLink;
    @Column(name="phone", length=50)
    private String phone;
    @Column(name="service_hours", length=100)
    private String serviceHours;
    @Column(name="taobao_url", length=500)
    private String taobaoUrl;
    @Column(name="xianyu_url", length=500)
    private String xianyuUrl;
    @Column(name="contact_notice", nullable=false, length=500)
    private String contactNotice;
    @Column(name="wecom_kefu_url", length=500)
    private String wecomKefuUrl;
    @Column(name="wecom_kefu_note", length=300)
    private String wecomKefuNote;
    @Column(name="quick_contact_title", length=40)
    private String quickContactTitle;
    @Column(name="taobao_label", length=40)
    private String taobaoLabel;
    @Column(name="xianyu_label", length=40)
    private String xianyuLabel;
    @Column(name="wecom_kefu_label", length=40)
    private String wecomKefuLabel;
    @Column(name="wecom_kefu_button", length=40)
    private String wecomKefuButton;
    @Column(name="quote_success_text", nullable=false, length=300)
    private String quoteSuccessText;
    @Column(name="order_help_text", nullable=false, length=300)
    private String orderHelpText;
    @Column(name="footer_text", nullable=false, length=200)
    private String footerText;
    @Column(name="seo_title", length=160)
    private String seoTitle;
    @Column(name="seo_description", length=300)
    private String seoDescription;
    @Column(name="tool_updated_note", length=120)
    private String toolUpdatedNote;
    @Column(name="tool_public_enabled", nullable=false)
    private boolean toolPublicEnabled;
    @Column(name="updated_at", nullable=false)
    private Instant updatedAt;
    @Version
    private long version;

    @Generated
    public static SiteConfigBuilder builder() {
        return new SiteConfigBuilder();
    }

    @Generated
    public Long getId() {
        return this.id;
    }

    @Generated
    public String getBrandName() {
        return this.brandName;
    }

    @Generated
    public String getLegalEntityName() {
        return this.legalEntityName;
    }

    @Generated
    public ServiceStatus getServiceStatus() {
        return this.serviceStatus;
    }

    @Generated
    public String getStatusShortText() {
        return this.statusShortText;
    }

    @Generated
    public String getPrimaryButtonLabel() {
        return this.primaryButtonLabel;
    }

    @Generated
    public boolean isQuoteEnabled() {
        return this.quoteEnabled;
    }

    @Generated
    public boolean isOrderQueryEnabled() {
        return this.orderQueryEnabled;
    }

    @Generated
    public String getHeroTitle() {
        return this.heroTitle;
    }

    @Generated
    public String getHeroSubtitle() {
        return this.heroSubtitle;
    }

    @Generated
    public StartingPriceMode getStartingPriceMode() {
        return this.startingPriceMode;
    }

    @Generated
    public BigDecimal getStartingPrice() {
        return this.startingPrice;
    }

    @Generated
    public boolean isShowStartingPrice() {
        return this.showStartingPrice;
    }

    @Generated
    public int getHomeServiceLimit() {
        return this.homeServiceLimit;
    }

    @Generated
    public String getHeroImage() {
        return this.heroImage;
    }

    @Generated
    public int getHeroImageHeight() {
        return this.heroImageHeight;
    }

    @Generated
    public String getHeroImageFit() {
        return this.heroImageFit;
    }

    @Generated
    public int getHeroImagePositionX() {
        return this.heroImagePositionX;
    }

    @Generated
    public int getHeroImagePositionY() {
        return this.heroImagePositionY;
    }

    @Generated
    public String getHeroBackgroundImage() {
        return this.heroBackgroundImage;
    }

    @Generated
    public int getHeroBackgroundOpacity() {
        return this.heroBackgroundOpacity;
    }

    @Generated
    public String getHeroBackgroundFit() {
        return this.heroBackgroundFit;
    }

    @Generated
    public int getHeroBackgroundPositionX() {
        return this.heroBackgroundPositionX;
    }

    @Generated
    public int getHeroBackgroundPositionY() {
        return this.heroBackgroundPositionY;
    }

    @Generated
    public int getHeroBackgroundBlur() {
        return this.heroBackgroundBlur;
    }

    @Generated
    public int getServiceCoverHeight() {
        return this.serviceCoverHeight;
    }

    @Generated
    public String getServiceCoverFit() {
        return this.serviceCoverFit;
    }

    @Generated
    public int getServiceCoverPositionX() {
        return this.serviceCoverPositionX;
    }

    @Generated
    public int getServiceCoverPositionY() {
        return this.serviceCoverPositionY;
    }

    @Generated
    public String getTrustTags() {
        return this.trustTags;
    }

    @Generated
    public String getTrustCases() {
        return this.trustCases;
    }

    @Generated
    public String getTrustFaq() {
        return this.trustFaq;
    }

    @Generated
    public boolean isAnnouncementEnabled() {
        return this.announcementEnabled;
    }

    @Generated
    public String getAnnouncementText() {
        return this.announcementText;
    }

    @Generated
    public String getContactTitle() {
        return this.contactTitle;
    }

    @Generated
    public String getContactIntro() {
        return this.contactIntro;
    }

    @Generated
    public String getWechatId() {
        return this.wechatId;
    }

    @Generated
    public String getWechatQrImage() {
        return this.wechatQrImage;
    }

    @Generated
    public int getWechatQrSize() {
        return this.wechatQrSize;
    }

    @Generated
    public String getContactChannels() {
        return this.contactChannels;
    }

    @Generated
    public String getQqId() {
        return this.qqId;
    }

    @Generated
    public String getQqLink() {
        return this.qqLink;
    }

    @Generated
    public String getPhone() {
        return this.phone;
    }

    @Generated
    public String getServiceHours() {
        return this.serviceHours;
    }

    @Generated
    public String getTaobaoUrl() {
        return this.taobaoUrl;
    }

    @Generated
    public String getXianyuUrl() {
        return this.xianyuUrl;
    }

    @Generated
    public String getContactNotice() {
        return this.contactNotice;
    }

    @Generated
    public String getWecomKefuUrl() {
        return this.wecomKefuUrl;
    }

    @Generated
    public String getWecomKefuNote() {
        return this.wecomKefuNote;
    }

    @Generated
    public String getQuickContactTitle() {
        return this.quickContactTitle;
    }

    @Generated
    public String getTaobaoLabel() {
        return this.taobaoLabel;
    }

    @Generated
    public String getXianyuLabel() {
        return this.xianyuLabel;
    }

    @Generated
    public String getWecomKefuLabel() {
        return this.wecomKefuLabel;
    }

    @Generated
    public String getWecomKefuButton() {
        return this.wecomKefuButton;
    }

    @Generated
    public String getQuoteSuccessText() {
        return this.quoteSuccessText;
    }

    @Generated
    public String getOrderHelpText() {
        return this.orderHelpText;
    }

    @Generated
    public String getFooterText() {
        return this.footerText;
    }

    @Generated
    public String getSeoTitle() {
        return this.seoTitle;
    }

    @Generated
    public String getSeoDescription() {
        return this.seoDescription;
    }

    @Generated
    public String getToolUpdatedNote() {
        return this.toolUpdatedNote;
    }

    @Generated
    public boolean isToolPublicEnabled() {
        return this.toolPublicEnabled;
    }

    @Generated
    public Instant getUpdatedAt() {
        return this.updatedAt;
    }

    @Generated
    public long getVersion() {
        return this.version;
    }

    @Generated
    public void setId(Long id) {
        this.id = id;
    }

    @Generated
    public void setBrandName(String brandName) {
        this.brandName = brandName;
    }

    @Generated
    public void setLegalEntityName(String legalEntityName) {
        this.legalEntityName = legalEntityName;
    }

    @Generated
    public void setServiceStatus(ServiceStatus serviceStatus) {
        this.serviceStatus = serviceStatus;
    }

    @Generated
    public void setStatusShortText(String statusShortText) {
        this.statusShortText = statusShortText;
    }

    @Generated
    public void setPrimaryButtonLabel(String primaryButtonLabel) {
        this.primaryButtonLabel = primaryButtonLabel;
    }

    @Generated
    public void setQuoteEnabled(boolean quoteEnabled) {
        this.quoteEnabled = quoteEnabled;
    }

    @Generated
    public void setOrderQueryEnabled(boolean orderQueryEnabled) {
        this.orderQueryEnabled = orderQueryEnabled;
    }

    @Generated
    public void setHeroTitle(String heroTitle) {
        this.heroTitle = heroTitle;
    }

    @Generated
    public void setHeroSubtitle(String heroSubtitle) {
        this.heroSubtitle = heroSubtitle;
    }

    @Generated
    public void setStartingPriceMode(StartingPriceMode startingPriceMode) {
        this.startingPriceMode = startingPriceMode;
    }

    @Generated
    public void setStartingPrice(BigDecimal startingPrice) {
        this.startingPrice = startingPrice;
    }

    @Generated
    public void setShowStartingPrice(boolean showStartingPrice) {
        this.showStartingPrice = showStartingPrice;
    }

    @Generated
    public void setHomeServiceLimit(int homeServiceLimit) {
        this.homeServiceLimit = homeServiceLimit;
    }

    @Generated
    public void setHeroImage(String heroImage) {
        this.heroImage = heroImage;
    }

    @Generated
    public void setHeroImageHeight(int heroImageHeight) {
        this.heroImageHeight = heroImageHeight;
    }

    @Generated
    public void setHeroImageFit(String heroImageFit) {
        this.heroImageFit = heroImageFit;
    }

    @Generated
    public void setHeroImagePositionX(int heroImagePositionX) {
        this.heroImagePositionX = heroImagePositionX;
    }

    @Generated
    public void setHeroImagePositionY(int heroImagePositionY) {
        this.heroImagePositionY = heroImagePositionY;
    }

    @Generated
    public void setHeroBackgroundImage(String heroBackgroundImage) {
        this.heroBackgroundImage = heroBackgroundImage;
    }

    @Generated
    public void setHeroBackgroundOpacity(int heroBackgroundOpacity) {
        this.heroBackgroundOpacity = heroBackgroundOpacity;
    }

    @Generated
    public void setHeroBackgroundFit(String heroBackgroundFit) {
        this.heroBackgroundFit = heroBackgroundFit;
    }

    @Generated
    public void setHeroBackgroundPositionX(int heroBackgroundPositionX) {
        this.heroBackgroundPositionX = heroBackgroundPositionX;
    }

    @Generated
    public void setHeroBackgroundPositionY(int heroBackgroundPositionY) {
        this.heroBackgroundPositionY = heroBackgroundPositionY;
    }

    @Generated
    public void setHeroBackgroundBlur(int heroBackgroundBlur) {
        this.heroBackgroundBlur = heroBackgroundBlur;
    }

    @Generated
    public void setServiceCoverHeight(int serviceCoverHeight) {
        this.serviceCoverHeight = serviceCoverHeight;
    }

    @Generated
    public void setServiceCoverFit(String serviceCoverFit) {
        this.serviceCoverFit = serviceCoverFit;
    }

    @Generated
    public void setServiceCoverPositionX(int serviceCoverPositionX) {
        this.serviceCoverPositionX = serviceCoverPositionX;
    }

    @Generated
    public void setServiceCoverPositionY(int serviceCoverPositionY) {
        this.serviceCoverPositionY = serviceCoverPositionY;
    }

    @Generated
    public void setTrustTags(String trustTags) {
        this.trustTags = trustTags;
    }

    @Generated
    public void setTrustCases(String trustCases) {
        this.trustCases = trustCases;
    }

    @Generated
    public void setTrustFaq(String trustFaq) {
        this.trustFaq = trustFaq;
    }

    @Generated
    public void setAnnouncementEnabled(boolean announcementEnabled) {
        this.announcementEnabled = announcementEnabled;
    }

    @Generated
    public void setAnnouncementText(String announcementText) {
        this.announcementText = announcementText;
    }

    @Generated
    public void setContactTitle(String contactTitle) {
        this.contactTitle = contactTitle;
    }

    @Generated
    public void setContactIntro(String contactIntro) {
        this.contactIntro = contactIntro;
    }

    @Generated
    public void setWechatId(String wechatId) {
        this.wechatId = wechatId;
    }

    @Generated
    public void setWechatQrImage(String wechatQrImage) {
        this.wechatQrImage = wechatQrImage;
    }

    @Generated
    public void setWechatQrSize(int wechatQrSize) {
        this.wechatQrSize = wechatQrSize;
    }

    @Generated
    public void setContactChannels(String contactChannels) {
        this.contactChannels = contactChannels;
    }

    @Generated
    public void setQqId(String qqId) {
        this.qqId = qqId;
    }

    @Generated
    public void setQqLink(String qqLink) {
        this.qqLink = qqLink;
    }

    @Generated
    public void setPhone(String phone) {
        this.phone = phone;
    }

    @Generated
    public void setServiceHours(String serviceHours) {
        this.serviceHours = serviceHours;
    }

    @Generated
    public void setTaobaoUrl(String taobaoUrl) {
        this.taobaoUrl = taobaoUrl;
    }

    @Generated
    public void setXianyuUrl(String xianyuUrl) {
        this.xianyuUrl = xianyuUrl;
    }

    @Generated
    public void setContactNotice(String contactNotice) {
        this.contactNotice = contactNotice;
    }

    @Generated
    public void setWecomKefuUrl(String wecomKefuUrl) {
        this.wecomKefuUrl = wecomKefuUrl;
    }

    @Generated
    public void setWecomKefuNote(String wecomKefuNote) {
        this.wecomKefuNote = wecomKefuNote;
    }

    @Generated
    public void setQuickContactTitle(String quickContactTitle) {
        this.quickContactTitle = quickContactTitle;
    }

    @Generated
    public void setTaobaoLabel(String taobaoLabel) {
        this.taobaoLabel = taobaoLabel;
    }

    @Generated
    public void setXianyuLabel(String xianyuLabel) {
        this.xianyuLabel = xianyuLabel;
    }

    @Generated
    public void setWecomKefuLabel(String wecomKefuLabel) {
        this.wecomKefuLabel = wecomKefuLabel;
    }

    @Generated
    public void setWecomKefuButton(String wecomKefuButton) {
        this.wecomKefuButton = wecomKefuButton;
    }

    @Generated
    public void setQuoteSuccessText(String quoteSuccessText) {
        this.quoteSuccessText = quoteSuccessText;
    }

    @Generated
    public void setOrderHelpText(String orderHelpText) {
        this.orderHelpText = orderHelpText;
    }

    @Generated
    public void setFooterText(String footerText) {
        this.footerText = footerText;
    }

    @Generated
    public void setSeoTitle(String seoTitle) {
        this.seoTitle = seoTitle;
    }

    @Generated
    public void setSeoDescription(String seoDescription) {
        this.seoDescription = seoDescription;
    }

    @Generated
    public void setToolUpdatedNote(String toolUpdatedNote) {
        this.toolUpdatedNote = toolUpdatedNote;
    }

    @Generated
    public void setToolPublicEnabled(boolean toolPublicEnabled) {
        this.toolPublicEnabled = toolPublicEnabled;
    }

    @Generated
    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Generated
    public void setVersion(long version) {
        this.version = version;
    }

    @Generated
    public SiteConfig() {
    }

    @Generated
    public SiteConfig(Long id, String brandName, String legalEntityName, ServiceStatus serviceStatus, String statusShortText, String primaryButtonLabel, boolean quoteEnabled, boolean orderQueryEnabled, String heroTitle, String heroSubtitle, StartingPriceMode startingPriceMode, BigDecimal startingPrice, boolean showStartingPrice, int homeServiceLimit, String heroImage, int heroImageHeight, String heroImageFit, int heroImagePositionX, int heroImagePositionY, String heroBackgroundImage, int heroBackgroundOpacity, String heroBackgroundFit, int heroBackgroundPositionX, int heroBackgroundPositionY, int heroBackgroundBlur, int serviceCoverHeight, String serviceCoverFit, int serviceCoverPositionX, int serviceCoverPositionY, String trustTags, String trustCases, String trustFaq, boolean announcementEnabled, String announcementText, String contactTitle, String contactIntro, String wechatId, String wechatQrImage, int wechatQrSize, String contactChannels, String qqId, String qqLink, String phone, String serviceHours, String taobaoUrl, String xianyuUrl, String contactNotice, String quoteSuccessText, String orderHelpText, String footerText, String seoTitle, String seoDescription, String toolUpdatedNote, boolean toolPublicEnabled, Instant updatedAt, long version) {
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

    @Generated
    public static class SiteConfigBuilder {
        @Generated
        private Long id;
        @Generated
        private String brandName;
        @Generated
        private String legalEntityName;
        @Generated
        private ServiceStatus serviceStatus;
        @Generated
        private String statusShortText;
        @Generated
        private String primaryButtonLabel;
        @Generated
        private boolean quoteEnabled;
        @Generated
        private boolean orderQueryEnabled;
        @Generated
        private String heroTitle;
        @Generated
        private String heroSubtitle;
        @Generated
        private StartingPriceMode startingPriceMode;
        @Generated
        private BigDecimal startingPrice;
        @Generated
        private boolean showStartingPrice;
        @Generated
        private int homeServiceLimit;
        @Generated
        private String heroImage;
        @Generated
        private int heroImageHeight;
        @Generated
        private String heroImageFit;
        @Generated
        private int heroImagePositionX;
        @Generated
        private int heroImagePositionY;
        @Generated
        private String heroBackgroundImage;
        @Generated
        private int heroBackgroundOpacity;
        @Generated
        private String heroBackgroundFit;
        @Generated
        private int heroBackgroundPositionX;
        @Generated
        private int heroBackgroundPositionY;
        @Generated
        private int heroBackgroundBlur;
        @Generated
        private int serviceCoverHeight;
        @Generated
        private String serviceCoverFit;
        @Generated
        private int serviceCoverPositionX;
        @Generated
        private int serviceCoverPositionY;
        @Generated
        private String trustTags;
        @Generated
        private String trustCases;
        @Generated
        private String trustFaq;
        @Generated
        private boolean announcementEnabled;
        @Generated
        private String announcementText;
        @Generated
        private String contactTitle;
        @Generated
        private String contactIntro;
        @Generated
        private String wechatId;
        @Generated
        private String wechatQrImage;
        @Generated
        private int wechatQrSize;
        @Generated
        private String contactChannels;
        @Generated
        private String qqId;
        @Generated
        private String qqLink;
        @Generated
        private String phone;
        @Generated
        private String serviceHours;
        @Generated
        private String taobaoUrl;
        @Generated
        private String xianyuUrl;
        @Generated
        private String contactNotice;
        @Generated
        private String quoteSuccessText;
        @Generated
        private String orderHelpText;
        @Generated
        private String footerText;
        @Generated
        private String seoTitle;
        @Generated
        private String seoDescription;
        @Generated
        private String toolUpdatedNote;
        @Generated
        private boolean toolPublicEnabled;
        @Generated
        private Instant updatedAt;
        @Generated
        private long version;

        @Generated
        SiteConfigBuilder() {
        }

        @Generated
        public SiteConfigBuilder id(Long id) {
            this.id = id;
            return this;
        }

        @Generated
        public SiteConfigBuilder brandName(String brandName) {
            this.brandName = brandName;
            return this;
        }

        @Generated
        public SiteConfigBuilder legalEntityName(String legalEntityName) {
            this.legalEntityName = legalEntityName;
            return this;
        }

        @Generated
        public SiteConfigBuilder serviceStatus(ServiceStatus serviceStatus) {
            this.serviceStatus = serviceStatus;
            return this;
        }

        @Generated
        public SiteConfigBuilder statusShortText(String statusShortText) {
            this.statusShortText = statusShortText;
            return this;
        }

        @Generated
        public SiteConfigBuilder primaryButtonLabel(String primaryButtonLabel) {
            this.primaryButtonLabel = primaryButtonLabel;
            return this;
        }

        @Generated
        public SiteConfigBuilder quoteEnabled(boolean quoteEnabled) {
            this.quoteEnabled = quoteEnabled;
            return this;
        }

        @Generated
        public SiteConfigBuilder orderQueryEnabled(boolean orderQueryEnabled) {
            this.orderQueryEnabled = orderQueryEnabled;
            return this;
        }

        @Generated
        public SiteConfigBuilder heroTitle(String heroTitle) {
            this.heroTitle = heroTitle;
            return this;
        }

        @Generated
        public SiteConfigBuilder heroSubtitle(String heroSubtitle) {
            this.heroSubtitle = heroSubtitle;
            return this;
        }

        @Generated
        public SiteConfigBuilder startingPriceMode(StartingPriceMode startingPriceMode) {
            this.startingPriceMode = startingPriceMode;
            return this;
        }

        @Generated
        public SiteConfigBuilder startingPrice(BigDecimal startingPrice) {
            this.startingPrice = startingPrice;
            return this;
        }

        @Generated
        public SiteConfigBuilder showStartingPrice(boolean showStartingPrice) {
            this.showStartingPrice = showStartingPrice;
            return this;
        }

        @Generated
        public SiteConfigBuilder homeServiceLimit(int homeServiceLimit) {
            this.homeServiceLimit = homeServiceLimit;
            return this;
        }

        @Generated
        public SiteConfigBuilder heroImage(String heroImage) {
            this.heroImage = heroImage;
            return this;
        }

        @Generated
        public SiteConfigBuilder heroImageHeight(int heroImageHeight) {
            this.heroImageHeight = heroImageHeight;
            return this;
        }

        @Generated
        public SiteConfigBuilder heroImageFit(String heroImageFit) {
            this.heroImageFit = heroImageFit;
            return this;
        }

        @Generated
        public SiteConfigBuilder heroImagePositionX(int heroImagePositionX) {
            this.heroImagePositionX = heroImagePositionX;
            return this;
        }

        @Generated
        public SiteConfigBuilder heroImagePositionY(int heroImagePositionY) {
            this.heroImagePositionY = heroImagePositionY;
            return this;
        }

        @Generated
        public SiteConfigBuilder heroBackgroundImage(String heroBackgroundImage) {
            this.heroBackgroundImage = heroBackgroundImage;
            return this;
        }

        @Generated
        public SiteConfigBuilder heroBackgroundOpacity(int heroBackgroundOpacity) {
            this.heroBackgroundOpacity = heroBackgroundOpacity;
            return this;
        }

        @Generated
        public SiteConfigBuilder heroBackgroundFit(String heroBackgroundFit) {
            this.heroBackgroundFit = heroBackgroundFit;
            return this;
        }

        @Generated
        public SiteConfigBuilder heroBackgroundPositionX(int heroBackgroundPositionX) {
            this.heroBackgroundPositionX = heroBackgroundPositionX;
            return this;
        }

        @Generated
        public SiteConfigBuilder heroBackgroundPositionY(int heroBackgroundPositionY) {
            this.heroBackgroundPositionY = heroBackgroundPositionY;
            return this;
        }

        @Generated
        public SiteConfigBuilder heroBackgroundBlur(int heroBackgroundBlur) {
            this.heroBackgroundBlur = heroBackgroundBlur;
            return this;
        }

        @Generated
        public SiteConfigBuilder serviceCoverHeight(int serviceCoverHeight) {
            this.serviceCoverHeight = serviceCoverHeight;
            return this;
        }

        @Generated
        public SiteConfigBuilder serviceCoverFit(String serviceCoverFit) {
            this.serviceCoverFit = serviceCoverFit;
            return this;
        }

        @Generated
        public SiteConfigBuilder serviceCoverPositionX(int serviceCoverPositionX) {
            this.serviceCoverPositionX = serviceCoverPositionX;
            return this;
        }

        @Generated
        public SiteConfigBuilder serviceCoverPositionY(int serviceCoverPositionY) {
            this.serviceCoverPositionY = serviceCoverPositionY;
            return this;
        }

        @Generated
        public SiteConfigBuilder trustTags(String trustTags) {
            this.trustTags = trustTags;
            return this;
        }

        @Generated
        public SiteConfigBuilder trustCases(String trustCases) {
            this.trustCases = trustCases;
            return this;
        }

        @Generated
        public SiteConfigBuilder trustFaq(String trustFaq) {
            this.trustFaq = trustFaq;
            return this;
        }

        @Generated
        public SiteConfigBuilder announcementEnabled(boolean announcementEnabled) {
            this.announcementEnabled = announcementEnabled;
            return this;
        }

        @Generated
        public SiteConfigBuilder announcementText(String announcementText) {
            this.announcementText = announcementText;
            return this;
        }

        @Generated
        public SiteConfigBuilder contactTitle(String contactTitle) {
            this.contactTitle = contactTitle;
            return this;
        }

        @Generated
        public SiteConfigBuilder contactIntro(String contactIntro) {
            this.contactIntro = contactIntro;
            return this;
        }

        @Generated
        public SiteConfigBuilder wechatId(String wechatId) {
            this.wechatId = wechatId;
            return this;
        }

        @Generated
        public SiteConfigBuilder wechatQrImage(String wechatQrImage) {
            this.wechatQrImage = wechatQrImage;
            return this;
        }

        @Generated
        public SiteConfigBuilder wechatQrSize(int wechatQrSize) {
            this.wechatQrSize = wechatQrSize;
            return this;
        }

        @Generated
        public SiteConfigBuilder contactChannels(String contactChannels) {
            this.contactChannels = contactChannels;
            return this;
        }

        @Generated
        public SiteConfigBuilder qqId(String qqId) {
            this.qqId = qqId;
            return this;
        }

        @Generated
        public SiteConfigBuilder qqLink(String qqLink) {
            this.qqLink = qqLink;
            return this;
        }

        @Generated
        public SiteConfigBuilder phone(String phone) {
            this.phone = phone;
            return this;
        }

        @Generated
        public SiteConfigBuilder serviceHours(String serviceHours) {
            this.serviceHours = serviceHours;
            return this;
        }

        @Generated
        public SiteConfigBuilder taobaoUrl(String taobaoUrl) {
            this.taobaoUrl = taobaoUrl;
            return this;
        }

        @Generated
        public SiteConfigBuilder xianyuUrl(String xianyuUrl) {
            this.xianyuUrl = xianyuUrl;
            return this;
        }

        @Generated
        public SiteConfigBuilder contactNotice(String contactNotice) {
            this.contactNotice = contactNotice;
            return this;
        }

        @Generated
        public SiteConfigBuilder quoteSuccessText(String quoteSuccessText) {
            this.quoteSuccessText = quoteSuccessText;
            return this;
        }

        @Generated
        public SiteConfigBuilder orderHelpText(String orderHelpText) {
            this.orderHelpText = orderHelpText;
            return this;
        }

        @Generated
        public SiteConfigBuilder footerText(String footerText) {
            this.footerText = footerText;
            return this;
        }

        @Generated
        public SiteConfigBuilder seoTitle(String seoTitle) {
            this.seoTitle = seoTitle;
            return this;
        }

        @Generated
        public SiteConfigBuilder seoDescription(String seoDescription) {
            this.seoDescription = seoDescription;
            return this;
        }

        @Generated
        public SiteConfigBuilder toolUpdatedNote(String toolUpdatedNote) {
            this.toolUpdatedNote = toolUpdatedNote;
            return this;
        }

        @Generated
        public SiteConfigBuilder toolPublicEnabled(boolean toolPublicEnabled) {
            this.toolPublicEnabled = toolPublicEnabled;
            return this;
        }

        @Generated
        public SiteConfigBuilder updatedAt(Instant updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        @Generated
        public SiteConfigBuilder version(long version) {
            this.version = version;
            return this;
        }

        @Generated
        public SiteConfig build() {
            return new SiteConfig(this.id, this.brandName, this.legalEntityName, this.serviceStatus, this.statusShortText, this.primaryButtonLabel, this.quoteEnabled, this.orderQueryEnabled, this.heroTitle, this.heroSubtitle, this.startingPriceMode, this.startingPrice, this.showStartingPrice, this.homeServiceLimit, this.heroImage, this.heroImageHeight, this.heroImageFit, this.heroImagePositionX, this.heroImagePositionY, this.heroBackgroundImage, this.heroBackgroundOpacity, this.heroBackgroundFit, this.heroBackgroundPositionX, this.heroBackgroundPositionY, this.heroBackgroundBlur, this.serviceCoverHeight, this.serviceCoverFit, this.serviceCoverPositionX, this.serviceCoverPositionY, this.trustTags, this.trustCases, this.trustFaq, this.announcementEnabled, this.announcementText, this.contactTitle, this.contactIntro, this.wechatId, this.wechatQrImage, this.wechatQrSize, this.contactChannels, this.qqId, this.qqLink, this.phone, this.serviceHours, this.taobaoUrl, this.xianyuUrl, this.contactNotice, this.quoteSuccessText, this.orderHelpText, this.footerText, this.seoTitle, this.seoDescription, this.toolUpdatedNote, this.toolPublicEnabled, this.updatedAt, this.version);
        }

        @Generated
        public String toString() {
            return "SiteConfig.SiteConfigBuilder(id=" + this.id + ", brandName=" + this.brandName + ", legalEntityName=" + this.legalEntityName + ", serviceStatus=" + String.valueOf((Object)this.serviceStatus) + ", statusShortText=" + this.statusShortText + ", primaryButtonLabel=" + this.primaryButtonLabel + ", quoteEnabled=" + this.quoteEnabled + ", orderQueryEnabled=" + this.orderQueryEnabled + ", heroTitle=" + this.heroTitle + ", heroSubtitle=" + this.heroSubtitle + ", startingPriceMode=" + String.valueOf((Object)this.startingPriceMode) + ", startingPrice=" + String.valueOf(this.startingPrice) + ", showStartingPrice=" + this.showStartingPrice + ", homeServiceLimit=" + this.homeServiceLimit + ", heroImage=" + this.heroImage + ", heroImageHeight=" + this.heroImageHeight + ", heroImageFit=" + this.heroImageFit + ", heroImagePositionX=" + this.heroImagePositionX + ", heroImagePositionY=" + this.heroImagePositionY + ", heroBackgroundImage=" + this.heroBackgroundImage + ", heroBackgroundOpacity=" + this.heroBackgroundOpacity + ", heroBackgroundFit=" + this.heroBackgroundFit + ", heroBackgroundPositionX=" + this.heroBackgroundPositionX + ", heroBackgroundPositionY=" + this.heroBackgroundPositionY + ", heroBackgroundBlur=" + this.heroBackgroundBlur + ", serviceCoverHeight=" + this.serviceCoverHeight + ", serviceCoverFit=" + this.serviceCoverFit + ", serviceCoverPositionX=" + this.serviceCoverPositionX + ", serviceCoverPositionY=" + this.serviceCoverPositionY + ", trustTags=" + this.trustTags + ", trustCases=" + this.trustCases + ", trustFaq=" + this.trustFaq + ", announcementEnabled=" + this.announcementEnabled + ", announcementText=" + this.announcementText + ", contactTitle=" + this.contactTitle + ", contactIntro=" + this.contactIntro + ", wechatId=" + this.wechatId + ", wechatQrImage=" + this.wechatQrImage + ", wechatQrSize=" + this.wechatQrSize + ", contactChannels=" + this.contactChannels + ", qqId=" + this.qqId + ", qqLink=" + this.qqLink + ", phone=" + this.phone + ", serviceHours=" + this.serviceHours + ", taobaoUrl=" + this.taobaoUrl + ", xianyuUrl=" + this.xianyuUrl + ", contactNotice=" + this.contactNotice + ", quoteSuccessText=" + this.quoteSuccessText + ", orderHelpText=" + this.orderHelpText + ", footerText=" + this.footerText + ", seoTitle=" + this.seoTitle + ", seoDescription=" + this.seoDescription + ", toolUpdatedNote=" + this.toolUpdatedNote + ", toolPublicEnabled=" + this.toolPublicEnabled + ", updatedAt=" + String.valueOf(this.updatedAt) + ", version=" + this.version + ")";
        }
    }

    public static enum ServiceStatus {
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

    public static enum StartingPriceMode {
        MANUAL,
        AUTO;

    }
}
