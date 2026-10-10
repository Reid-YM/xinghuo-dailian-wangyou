package com.xinghuo.siteconfig.application;

import com.xinghuo.gamestatus.application.GameStatusApplicationService;
import com.xinghuo.gamestatus.application.GameStatusView;
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
        SiteRuntimeState runtime = port.load();
        GameStatusView mw4 = gameStatuses.resolve("CODMW4", null);
        return mw4 == null ? runtime.acceptingOrders() : mw4.getStatusCode().acceptingOrders();
    }
}
