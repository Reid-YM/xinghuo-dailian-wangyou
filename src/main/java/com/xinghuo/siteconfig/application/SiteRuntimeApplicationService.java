package com.xinghuo.siteconfig.application;

import org.springframework.stereotype.Service;

/** Application boundary for public runtime feature switches. */
@Service
public class SiteRuntimeApplicationService {
    private final SiteRuntimePort port;

    public SiteRuntimeApplicationService(SiteRuntimePort port) {
        this.port = port;
    }

    public boolean quoteEnabled() {
        return port.load().quoteEnabled();
    }

    public boolean orderQueryEnabled() {
        return port.load().orderQueryEnabled();
    }
}
