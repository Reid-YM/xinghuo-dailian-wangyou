package com.xinghuo.siteconfig.application;

import com.xinghuo.gamestatus.application.GameStatusApplicationService;
import com.xinghuo.gamestatus.application.GameStatusView;
import com.xinghuo.siteconfig.domain.SiteOperatingStatusCode;
import com.xinghuo.siteconfig.domain.SiteRuntimeState;
import org.springframework.stereotype.Service;

/** Application boundary for public runtime feature switches and operating-state decisions. */
@Service
public class SiteRuntimeApplicationService {
    private final SiteRuntimePort port;
    private final GameStatusApplicationService gameStatuses;

    public SiteRuntimeApplicationService(
            SiteRuntimePort port,
            GameStatusApplicationService gameStatuses) {
        this.port = port;
        this.gameStatuses = gameStatuses;
    }

    public boolean quoteEnabled() {
        return port.load().quoteEnabled();
    }

    public boolean orderQueryEnabled() {
        return port.load().orderQueryEnabled();
    }

    public boolean acceptingOrders() {
        return operatingStatus().acceptingOrders();
    }

    public SiteOperatingStatusView operatingStatus() {
        SiteRuntimeState runtime = port.load();
        GameStatusView mw4 = gameStatuses.resolve("CODMW4", null);
        if (mw4 == null) {
            return new SiteOperatingStatusView(
                    runtime.operatingStatus(),
                    runtime.statusShortText(),
                    runtime.primaryButtonLabel());
        }

        SiteOperatingStatusCode status = switch (mw4.getStatusCode()) {
            case OPEN -> SiteOperatingStatusCode.OPEN;
            case PAUSED -> SiteOperatingStatusCode.SUSPENDED;
            case MAINTENANCE -> SiteOperatingStatusCode.MAINTENANCE;
            case LIMITED, PREORDER -> SiteOperatingStatusCode.RESERVATION;
        };
        String ctaLabel = mw4.getCtaLabel() == null
                ? runtime.primaryButtonLabel()
                : mw4.getCtaLabel();
        return new SiteOperatingStatusView(status, mw4.getPublicLabel(), ctaLabel);
    }
}
