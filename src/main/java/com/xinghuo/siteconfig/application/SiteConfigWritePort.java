package com.xinghuo.siteconfig.application;

import java.util.Map;

/** Mutation boundary for SiteConfig persistence. */
public interface SiteConfigWritePort {
    void updateSiteImage(String slot, String url);

    void update(SiteConfigFullUpdateCommand command);

    void updateSection(String section, Map<String, Object> values);
}
