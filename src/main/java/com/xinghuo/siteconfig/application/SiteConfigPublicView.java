package com.xinghuo.siteconfig.application;

import com.xinghuo.slot.application.SlotPublicServiceView;
import java.util.List;

public record SiteConfigPublicView(
        SiteConfigPublicSnapshot config,
        SiteOperatingStatusView operatingStatus,
        List<SlotPublicServiceView> hotServices) {
}
