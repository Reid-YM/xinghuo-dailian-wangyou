package com.xinghuo.sitesetting.infrastructure;

import com.xinghuo.sitesetting.application.SiteSettingPort;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class SiteSettingPersistenceAdapter implements SiteSettingPort {
    private final SiteSettingRepository repo;

    public SiteSettingPersistenceAdapter(SiteSettingRepository repo) {
        this.repo = repo;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, String> loadAll() {
        LinkedHashMap<String, String> out = new LinkedHashMap<>();
        for (SiteSetting setting : repo.findAll()) {
            out.put(setting.getSettingKey(), setting.getSettingValue());
        }
        return out;
    }

    @Override
    @Transactional(readOnly = true)
    public String get(String key) {
        return repo.findById(key).map(SiteSetting::getSettingValue).orElse(null);
    }

    @Override
    @Transactional
    public void put(String key, String value) {
        save(key, value);
    }

    @Override
    @Transactional
    public void putAll(Map<String, String> values) {
        for (Map.Entry<String, String> entry : values.entrySet()) {
            save(entry.getKey(), entry.getValue());
        }
    }

    private void save(String key, String value) {
        SiteSetting setting = repo.findById(key)
                .orElseGet(() -> SiteSetting.builder().settingKey(key).build());
        setting.setSettingValue(value);
        setting.setUpdatedAt(Instant.now());
        repo.save(setting);
    }
}
