package com.xinghuo.siteconfig.application;

import com.xinghuo.siteconfig.domain.SiteRuntimeState;

public interface SiteRuntimePort {
    SiteRuntimeState load();
}
