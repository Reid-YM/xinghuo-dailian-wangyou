package com.xinghuo.service;

import com.xinghuo.exception.BadRequestException;
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
    private static final Map<String, String> GLOBAL_UI_DEFAULTS = Map.ofEntries(
            Map.entry("navServicesLabel", "服务"),
            Map.entry("navServicesVisible", "true"),
            Map.entry("navGuidesLabel", "攻略"),
            Map.entry("navGuidesVisible", "true"),
            Map.entry("navMasteryLabel", "MW4 工具站"),
            Map.entry("navMasteryVisible", "true"),
            Map.entry("navRecordsLabel", "业务记录"),
            Map.entry("navRecordsVisible", "true"),
            Map.entry("navContactLabel", "一对一咨询"),
            Map.entry("navContactVisible", "true"),
            Map.entry("navAccountLabel", "登录"),
            Map.entry("navAccountVisible", "true"),
            Map.entry("headerQuoteVisible", "true"),
            Map.entry("footerServicesLabel", "服务大厅"),
            Map.entry("footerServicesVisible", "true"),
            Map.entry("footerRecordsLabel", "业务记录"),
            Map.entry("footerRecordsVisible", "true"),
            Map.entry("footerConsoleLabel", "用户控制台"),
            Map.entry("footerConsoleVisible", "true"),
            Map.entry("footerRulesLabel", "服务规则"),
            Map.entry("footerRulesVisible", "true"),
            Map.entry("footerPrivacyLabel", "隐私说明"),
            Map.entry("footerPrivacyVisible", "true"),
            Map.entry("footerContactLabel", "一对一咨询"),
            Map.entry("footerContactVisible", "true"),
            Map.entry("homeQuickContactVisible", "true"),
            Map.entry("icpNumber", "蜀ICP备2026047129号"),
            Map.entry("icpVisible", "true"),
            Map.entry("publicSecurityNumber", "川公网安备51162102000262号"),
            Map.entry("publicSecurityVisible", "true")
    );

    private static final Set<String> BOOLEAN_KEYS = Set.of(
            "navServicesVisible", "navGuidesVisible", "navMasteryVisible", "navRecordsVisible",
            "navContactVisible", "navAccountVisible", "headerQuoteVisible",
            "footerServicesVisible", "footerRecordsVisible", "footerConsoleVisible",
            "footerRulesVisible", "footerPrivacyVisible", "footerContactVisible",
            "homeQuickContactVisible", "icpVisible", "publicSecurityVisible"
    );

    private static final Set<String> GLOBAL_UI_KEYS = GLOBAL_UI_DEFAULTS.keySet();

    private static final Set<String> PUBLIC_KEYS = Set.of(
            "legalEntityName", "brandName", "wechat", "serviceHours", "icpNumber",
            "publicSecurityNumber", "siteTitleSuffix", "defaultOgImage", "faviconPath",
            "navServicesLabel", "navServicesVisible", "navGuidesLabel", "navGuidesVisible",
            "navMasteryLabel", "navMasteryVisible", "navRecordsLabel", "navRecordsVisible",
            "navContactLabel", "navContactVisible", "navAccountLabel", "navAccountVisible",
            "headerQuoteVisible", "footerServicesLabel", "footerServicesVisible",
            "footerRecordsLabel", "footerRecordsVisible", "footerConsoleLabel", "footerConsoleVisible",
            "footerRulesLabel", "footerRulesVisible", "footerPrivacyLabel", "footerPrivacyVisible",
            "footerContactLabel", "footerContactVisible", "homeQuickContactVisible",
            "icpVisible", "publicSecurityVisible"
    );

    private final SiteSettingRepository repo;

    @Transactional(readOnly=true)
    public Map<String, String> publicSettings() {
        LinkedHashMap<String, String> out = new LinkedHashMap<>();
        out.putAll(GLOBAL_UI_DEFAULTS);
        for (SiteSetting s : this.repo.findAll()) {
            if (!PUBLIC_KEYS.contains(s.getSettingKey())) continue;
            String value = s.getSettingValue();
            if (value != null && !value.isBlank()) out.put(s.getSettingKey(), value);
        }
        return out;
    }

    @Transactional(readOnly=true)
    public Map<String, String> globalUiSettings() {
        LinkedHashMap<String, String> out = new LinkedHashMap<>();
        out.putAll(GLOBAL_UI_DEFAULTS);
        for (SiteSetting s : this.repo.findAll()) {
            if (!GLOBAL_UI_KEYS.contains(s.getSettingKey())) continue;
            String value = s.getSettingValue();
            if (value != null && !value.isBlank()) out.put(s.getSettingKey(), value);
        }
        return out;
    }

    @Transactional
    public Map<String, String> updateGlobalUiSettings(Map<String, Object> values) {
        if (values == null) throw new BadRequestException("全局前端配置不能为空");
        for (String key : GLOBAL_UI_KEYS) {
            if (!values.containsKey(key)) continue;
            String raw = values.get(key) == null ? "" : String.valueOf(values.get(key)).trim();
            String normalized;
            if (BOOLEAN_KEYS.contains(key)) {
                if (!"true".equalsIgnoreCase(raw) && !"false".equalsIgnoreCase(raw)) {
                    throw new BadRequestException("显示开关必须为 true 或 false：" + key);
                }
                normalized = String.valueOf(Boolean.parseBoolean(raw));
            } else {
                if (raw.length() > 120) throw new BadRequestException("全局前端配置内容过长：" + key);
                normalized = raw.isBlank() ? GLOBAL_UI_DEFAULTS.get(key) : raw;
            }
            this.putInternal(key, normalized);
        }
        return this.globalUiSettings();
    }

    @Transactional(readOnly=true)
    public String get(String key, String fallback) {
        String v = this.repo.findById(key).map(SiteSetting::getSettingValue).orElse(null);
        return v == null || v.isBlank() ? fallback : v;
    }

    @Transactional
    public void put(String key, String value) {
        if (!PUBLIC_KEYS.contains(key)) return;
        this.putInternal(key, value);
    }

    private void putInternal(String key, String value) {
        SiteSetting s = this.repo.findById(key).orElseGet(() -> SiteSetting.builder().settingKey(key).build());
        s.setSettingValue(value);
        s.setUpdatedAt(Instant.now());
        this.repo.save(s);
    }

    @Generated
    public SiteSettingService(SiteSettingRepository repo) {
        this.repo = repo;
    }
}
