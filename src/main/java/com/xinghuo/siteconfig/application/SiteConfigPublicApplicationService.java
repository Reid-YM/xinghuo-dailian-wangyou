package com.xinghuo.siteconfig.application;

import com.xinghuo.slot.application.SlotApplicationService;
import org.springframework.stereotype.Service;

@Service
public class SiteConfigPublicApplicationService {
    private final SiteConfigPublicPort port;
    private final SiteRuntimeApplicationService runtimeConfig;
    private final SlotApplicationService slots;

    public SiteConfigPublicApplicationService(
            SiteConfigPublicPort port,
            SiteRuntimeApplicationService runtimeConfig,
            SlotApplicationService slots) {
        this.port = port;
        this.runtimeConfig = runtimeConfig;
        this.slots = slots;
    }

    public SiteConfigPublicView get() {
        return new SiteConfigPublicView(
                port.load(),
                runtimeConfig.operatingStatus(),
                slots.publicHotServices());
    }
}
