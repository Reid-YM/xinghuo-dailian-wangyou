/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.xinghuo.model.SiteSetting
 *  com.xinghuo.repository.SiteSettingRepository
 *  com.xinghuo.service.SiteSettingService
 *  lombok.Generated
 *  org.springframework.stereotype.Service
 *  org.springframework.transaction.annotation.Transactional
 */
package com.xinghuo.service;

import com.xinghuo.model.SiteSetting;
import com.xinghuo.repository.SiteSettingRepository;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import lombok.Generated;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SiteSettingService {
    private static final Set<String> PUBLIC_KEYS = Set.of("legalEntityName", "brandName", "wechat", "serviceHours", "icpNumber", "publicSecurityNumber", "siteTitleSuffix", "defaultOgImage", "faviconPath");
    private final SiteSettingRepository repo;

    @Transactional(readOnly=true)
    public Map<String, String> publicSettings() {
        LinkedHashMap<String, String> out = new LinkedHashMap<String, String>();
        for (SiteSetting s : this.repo.findAll()) {
            if (!PUBLIC_KEYS.contains(s.getSettingKey())) continue;
            out.put(s.getSettingKey(), s.getSettingValue());
        }
        return out;
    }

    @Transactional(readOnly=true)
    public String get(String key, String fallback) {
        String v = this.repo.findById((Object)key).map(SiteSetting::getSettingValue).orElse(null);
        return v == null || v.isBlank() ? fallback : v;
    }

    @Transactional
    public void put(String key, String value) {
        if (!PUBLIC_KEYS.contains(key)) {
            return;
        }
        SiteSetting s = this.repo.findById((Object)key).orElseGet(() -> SiteSetting.builder().settingKey(key).build());
        s.setSettingValue(value);
        s.setUpdatedAt(Instant.now());
        this.repo.save((Object)s);
    }

    @Generated
    public SiteSettingService(SiteSettingRepository repo) {
        this.repo = repo;
    }
}

