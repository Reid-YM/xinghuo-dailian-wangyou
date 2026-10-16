package com.xinghuo.service;

import com.xinghuo.exception.BadRequestException;
import com.xinghuo.model.SiteSetting;
import com.xinghuo.repository.SiteSettingRepository;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SiteSettingService {
    private static final Map<String, String> GLOBAL_UI_DEFAULTS = Map.ofEntries(
            Map.entry("navServicesLabel", "服务"),
            Map.entry("navServicesVisible", "true"),
            // N9 rollout gate: default keeps all public traffic on the legacy hall.
            // Administrators can only switch between two known in-site targets.
            Map.entry("servicesEntryMode", "legacy"),
            Map.entry("navGuidesLabel", "攻略"),
            Map.entry("navGuidesVisible", "true"),
            Map.entry("navMasteryLabel", "MW4 工具站"),
            Map.entry("navMasteryVisible", "true"),
            Map.entry("navRecordsLabel", "业务记录"),
            Map.entry("navRecordsVisible", "true"),
            Map.entry("navContactLabel", "一对一咨询"),
            Map.entry("navContactVisible", "true"),
            Map.entry("navAccountLabel", "个人中心"),
            Map.entry("navAccountVisible", "true"),
            Map.entry("headerQuoteVisible", "true"),
            Map.entry("footerServicesLabel", "服务大厅"),
            Map.entry("footerServicesVisible", "true"),
            Map.entry("footerRecordsLabel", "业务记录"),
            Map.entry("footerRecordsVisible", "true"),
            Map.entry("footerConsoleLabel", "个人中心"),
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
            Map.entry("publicSecurityVisible", "true"));

    private static final Set<String> BOOLEAN_KEYS = Set.of(
            "navServicesVisible", "navGuidesVisible", "navMasteryVisible", "navRecordsVisible",
            "navContactVisible", "navAccountVisible", "headerQuoteVisible",
            "footerServicesVisible", "footerRecordsVisible", "footerConsoleVisible",
            "footerRulesVisible", "footerPrivacyVisible", "footerContactVisible",
            "homeQuickContactVisible", "icpVisible", "publicSecurityVisible");

    private static final Set<String> ROLLOUT_MODE_KEYS = Set.of("servicesEntryMode");
    private static final Set<String> ALLOWED_SERVICE_ENTRY_MODES = Set.of("legacy", "next");

    private static final Set<String> GLOBAL_UI_KEYS = GLOBAL_UI_DEFAULTS.keySet();

    private static final Set<String> PUBLIC_KEYS = Set.of(
            "legalEntityName", "brandName", "wechat", "serviceHours", "icpNumber",
            "publicSecurityNumber", "siteTitleSuffix", "defaultOgImage", "faviconPath",
            "navServicesLabel", "navServicesVisible", "servicesEntryMode",
            "navGuidesLabel", "navGuidesVisible",
            "navMasteryLabel", "navMasteryVisible", "navRecordsLabel", "navRecordsVisible",
            "navContactLabel", "navContactVisible", "navAccountLabel", "navAccountVisible",
            "headerQuoteVisible", "footerServicesLabel", "footerServicesVisible",
            "footerRecordsLabel", "footerRecordsVisible", "footerConsoleLabel", "footerConsoleVisible",
            "footerRulesLabel", "footerRulesVisible", "footerPrivacyLabel", "footerPrivacyVisible",
            "footerContactLabel", "footerContactVisible", "homeQuickContactVisible",
            "icpVisible", "publicSecurityVisible");

    private final SiteSettingRepository repo;

    public SiteSettingService(SiteSettingRepository repo) {
        this.repo = repo;
    }

    @Transactional(readOnly = true)
    public Map<String, String> publicSettings() {
        LinkedHashMap<String, String> out = new LinkedHashMap<>();
        out.putAll(GLOBAL_UI_DEFAULTS);
        for (SiteSetting setting : this.repo.findAll()) {
            if (!PUBLIC_KEYS.contains(setting.getSettingKey())) {
                continue;
            }
            String value = setting.getSettingValue();
            if (value != null && !value.isBlank()) {
                out.put(setting.getSettingKey(), value);
            }
        }
        normalizeLegacyAccountLabels(out);
        normalizeRolloutModes(out);
        return out;
    }

    @Transactional(readOnly = true)
    public Map<String, String> globalUiSettings() {
        LinkedHashMap<String, String> out = new LinkedHashMap<>();
        out.putAll(GLOBAL_UI_DEFAULTS);
        for (SiteSetting setting : this.repo.findAll()) {
            if (!GLOBAL_UI_KEYS.contains(setting.getSettingKey())) {
                continue;
            }
            String value = setting.getSettingValue();
            if (value != null && !value.isBlank()) {
                out.put(setting.getSettingKey(), value);
            }
        }
        normalizeLegacyAccountLabels(out);
        normalizeRolloutModes(out);
        return out;
    }

    /**
     * 旧版本把同一账号体系暴露成“登录”和“用户控制台”两个概念。
     * 新信息架构统一为“个人中心”；仅对这两个精确的历史默认值做读取期归一，
     * 其他管理员自定义文案仍原样保留。
     */
    private static void normalizeLegacyAccountLabels(Map<String, String> settings) {
        if ("登录".equals(settings.get("navAccountLabel"))) {
            settings.put("navAccountLabel", "个人中心");
        }
        if ("用户控制台".equals(settings.get("footerConsoleLabel"))) {
            settings.put("footerConsoleLabel", "个人中心");
        }
    }

    /**
     * Production safety belt for rollout flags persisted by older/manual tooling.
     * An unknown value must never redirect traffic to an arbitrary path; fall back to legacy.
     */
    private static void normalizeRolloutModes(Map<String, String> settings) {
        String value = settings.get("servicesEntryMode");
        if (!ALLOWED_SERVICE_ENTRY_MODES.contains(value)) {
            settings.put("servicesEntryMode", "legacy");
        }
    }

    @Transactional
    public Map<String, String> updateGlobalUiSettings(Map<String, Object> values) {
        if (values == null) {
            throw new BadRequestException("全局前端配置不能为空");
        }
        for (String key : GLOBAL_UI_KEYS) {
            if (!values.containsKey(key)) {
                continue;
            }
            String raw = values.get(key) == null ? "" : String.valueOf(values.get(key)).trim();
            String normalized;
            if (BOOLEAN_KEYS.contains(key)) {
                if (!"true".equalsIgnoreCase(raw) && !"false".equalsIgnoreCase(raw)) {
                    throw new BadRequestException("显示开关必须为 true 或 false：" + key);
                }
                normalized = String.valueOf(Boolean.parseBoolean(raw));
            } else if (ROLLOUT_MODE_KEYS.contains(key)) {
                normalized = raw.isBlank() ? GLOBAL_UI_DEFAULTS.get(key) : raw.toLowerCase();
                if (!ALLOWED_SERVICE_ENTRY_MODES.contains(normalized)) {
                    throw new BadRequestException("服务入口目标仅支持 legacy 或 next");
                }
            } else {
                if (raw.length() > 120) {
                    throw new BadRequestException("全局前端配置内容过长：" + key);
                }
                normalized = raw.isBlank() ? GLOBAL_UI_DEFAULTS.get(key) : raw;
            }
            this.putInternal(key, normalized);
        }
        return this.globalUiSettings();
    }

    @Transactional(readOnly = true)
    public String get(String key, String fallback) {
        String value = this.repo.findById(key).map(SiteSetting::getSettingValue).orElse(null);
        return value == null || value.isBlank() ? fallback : value;
    }

    @Transactional
    public void put(String key, String value) {
        if (!PUBLIC_KEYS.contains(key)) {
            return;
        }
        this.putInternal(key, value);
    }

    private void putInternal(String key, String value) {
        SiteSetting setting = this.repo.findById(key)
                .orElseGet(() -> SiteSetting.builder().settingKey(key).build());
        setting.setSettingValue(value);
        setting.setUpdatedAt(Instant.now());
        this.repo.save(setting);
    }
}
