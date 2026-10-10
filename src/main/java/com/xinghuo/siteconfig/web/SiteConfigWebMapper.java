package com.xinghuo.siteconfig.web;

import com.xinghuo.dto.ApiDtos;
import com.xinghuo.siteconfig.application.SiteConfigPublicSnapshot;
import com.xinghuo.siteconfig.application.SiteConfigPublicView;
import com.xinghuo.slot.web.SlotWebMapper;

public final class SiteConfigWebMapper {
    private SiteConfigWebMapper() {
    }

    public static ApiDtos.SiteConfigResponse toResponse(SiteConfigPublicView view) {
        SiteConfigPublicSnapshot c = view.config();
        return new ApiDtos.SiteConfigResponse(
                c.brandName(),
                c.legalEntityName(),
                view.operatingStatus().publicCode(),
                view.operatingStatus().shortText(),
                view.operatingStatus().ctaLabel(),
                c.quoteEnabled(),
                c.orderQueryEnabled(),
                c.heroTitle(),
                c.heroSubtitle(),
                c.startingPriceMode(),
                c.startingPrice(),
                c.showStartingPrice(),
                c.homeServiceLimit(),
                c.homeServiceMobileLimit(),
                c.homeServiceDesktopColumns(),
                c.homeServiceMobileColumns(),
                c.serviceCardDensity(),
                c.serviceCardMobileDensity(),
                c.heroImage(),
                c.heroImageHeight(),
                c.heroImageFit(),
                c.heroImagePositionX(),
                c.heroImagePositionY(),
                c.heroImageMobileHeight(),
                c.heroImageMobileFit(),
                c.heroImageMobilePositionX(),
                c.heroImageMobilePositionY(),
                c.heroBackgroundImage(),
                c.heroBackgroundMobileImage(),
                c.heroBackgroundMobileHeight(),
                c.heroBackgroundOpacity(),
                c.heroBackgroundFit(),
                c.heroBackgroundPositionX(),
                c.heroBackgroundPositionY(),
                c.heroBackgroundBlur(),
                c.heroBackgroundMobileFit(),
                c.heroBackgroundMobilePositionX(),
                c.heroBackgroundMobilePositionY(),
                c.serviceCoverHeight(),
                c.serviceCoverFit(),
                c.serviceCoverPositionX(),
                c.serviceCoverPositionY(),
                c.serviceCoverMobileHeight(),
                c.serviceCoverMobileFit(),
                c.serviceCoverMobilePositionX(),
                c.serviceCoverMobilePositionY(),
                c.trustTags(),
                c.trustCases().stream().map(SiteConfigWebMapper::toResponse).toList(),
                c.trustCaseHomeLimit(),
                c.trustCaseHomeMobileLimit(),
                c.trustFaq().stream().map(SiteConfigWebMapper::toResponse).toList(),
                c.trustFaqHomeLimit(),
                c.trustFaqHomeMobileLimit(),
                c.announcementEnabled(),
                c.announcementText(),
                c.contactTitle(),
                c.contactIntro(),
                c.wechatId(),
                c.wechatQrImage(),
                c.wechatQrSize(),
                c.contactChannels().stream().map(SiteConfigWebMapper::toResponse).toList(),
                c.homeContactLimit(),
                c.homeContactMobileLimit(),
                c.qqId(),
                c.qqLink(),
                c.phone(),
                c.serviceHours(),
                c.taobaoUrl(),
                c.xianyuUrl(),
                c.contactNotice(),
                c.wecomKefuUrl(),
                c.wecomKefuNote(),
                c.quickContactTitle(),
                c.taobaoLabel(),
                c.xianyuLabel(),
                c.wecomKefuLabel(),
                c.wecomKefuButton(),
                c.quoteSuccessText(),
                c.orderHelpText(),
                c.footerText(),
                c.seoTitle(),
                c.seoDescription(),
                c.toolUpdatedNote(),
                c.toolPublicEnabled(),
                SlotWebMapper.toPublicResponses(view.hotServices()));
    }

    private static ApiDtos.ContactChannelResponse toResponse(SiteConfigPublicSnapshot.ContactChannelView view) {
        return new ApiDtos.ContactChannelResponse(
                view.label(), view.wechatId(), view.qrImage(), view.note(), view.jumpUrl());
    }

    private static ApiDtos.TrustCaseResponse toResponse(SiteConfigPublicSnapshot.TrustCaseView view) {
        return new ApiDtos.TrustCaseResponse(view.tag(), view.title(), view.metric(), view.detail());
    }

    private static ApiDtos.TrustFaqResponse toResponse(SiteConfigPublicSnapshot.TrustFaqView view) {
        return new ApiDtos.TrustFaqResponse(view.question(), view.answer());
    }
}
