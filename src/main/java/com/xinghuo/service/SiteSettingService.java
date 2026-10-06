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
            Map.entry("floatingCustomerServiceEnabled", "true"),
            Map.entry("navServicesLabel", "服务"),
            Map.entry("navGuidesLabel", "攻略"),
            Map.entry("navMasteryLabel", "MW4 工具站"),
            Map.entry("navRecordsLabel", "业务记录"),
            Map.entry("navAccountLabel", "个人空间"),
            Map.entry("footerServicesLabel", "服务大厅"),
            Map.entry("footerRecordsLabel", "业务记录"),
            Map.entry("footerAccountLabel", "个人空间"),
            Map.entry("footerRulesLabel", "服务规则"),
            Map.entry("footerPrivacyLabel", "隐私说明"),
            Map.entry("floatingCustomerServiceLabel", "在线客服"),
            Map.entry("floatingCustomerServiceTitle", "需要帮助？"),
            Map.entry("floatingCustomerServiceIntro", "先选择问题类型，我们会给你对应的准备提示，再开始在线咨询。"),
            Map.entry("floatingCustomerServiceNote", "请勿发送密码、验证码或令牌"),
            Map.entry("icpNumber", "蜀ICP备2026047129号"),
            Map.entry("publicSecurityNumber", "川公网安备51162102000262号")
    );

    private static final Set<String> PUBLIC_KEYS = Set.of(
            "legalEntityName", "brandName", "wechat", "serviceHours", "icpNumber",
            "publicSecurityNumber", "siteTitleSuffix", "defaultOgImage", "faviconPath",
            "floatingCustomerServiceEnabled", "navServicesLabel", "navGuidesLabel",
            "navMasteryLabel", "navRecordsLabel", "navAccountLabel", "footerServicesLabel",
            "footerRecordsLabel", "footerAccountLabel", "footerRulesLabel", "footerPrivacyLabel",
            "floatingCustomerServiceLabel", "floatingCustomerServiceTitle",
            "floatingCustomerServiceIntro", "floatingCustomerServiceNote"
    );

    private static final Set<String> GLOBAL_UI_KEYS = GLOBAL_UI_DEFAULTS.keySet();
    private final SiteSettingRepository repo;

    @Transactional(readOnly=true)
    public Map<String, String> publicSettings() {
        LinkedHashMap<String, String> out = new LinkedHashMap<String, String>();
        out.putAll(GLOBAL_UI_DEFAULTS);
        for (SiteSetting s : this.repo.findAll()) {
            if (!PUBLIC_KEYS.contains(s.getSettingKey())) continue;
            out.put(s.getSettingKey(), s.getSettingValue());
        }
        return out;
    }

    @Transactional(readOnly=true)
    public Map<String, String> globalUiSettings() {
        LinkedHashMap<String, String> out = new LinkedHashMap<String, String>();
        out.putAll(GLOBAL_UI_DEFAULTS);
        for (SiteSetting s : this.repo.findAll()) {
            if (!GLOBAL_UI_KEYS.contains(s.getSettingKey())) continue;
            out.put(s.getSettingKey(), s.getSettingValue());
        }
        return out;
    }

    @Transactional
    public Map<String, String> updateGlobalUiSettings(Map<String, Object> values) {
        if (values == null) {
            throw new BadRequestException("全局前端配置不能为空");
        }
        for (String key : GLOBAL_UI_KEYS) {
            if (!values.containsKey(key)) continue;
            String raw = values.get(key) == null ? "" : String.valueOf(values.get(key)).trim();
            String normalized;
            if ("floatingCustomerServiceEnabled".equals(key)) {
                if (!"true".equalsIgnoreCase(raw) && !"false".equalsIgnoreCase(raw)) {
                    throw new BadRequestException("悬浮客服开关必须为 true 或 false");
                }
                normalized = String.valueOf(Boolean.parseBoolean(raw));
            } else {
                int max = key.endsWith("Intro") || key.endsWith("Note") ? 300 : 100;
                if (raw.length() > max) {
                    throw new BadRequestException("全局前端配置内容过长：" + key);
                }
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
        if (!PUBLIC_KEYS.contains(key)) {
            return;
        }
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
