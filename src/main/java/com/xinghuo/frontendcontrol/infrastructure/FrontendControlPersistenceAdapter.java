package com.xinghuo.frontendcontrol.infrastructure;

import com.xinghuo.frontendcontrol.application.FrontendControlPort;
import com.xinghuo.sitesetting.infrastructure.SiteSetting;
import com.xinghuo.sitesetting.infrastructure.SiteSettingRepository;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class FrontendControlPersistenceAdapter implements FrontendControlPort {
    private final SiteSettingRepository repo;

    public FrontendControlPersistenceAdapter(SiteSettingRepository repo) {
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
    @Transactional
    public void putAll(Map<String, String> values) {
        for (Map.Entry<String, String> entry : values.entrySet()) {
            SiteSetting setting = repo.findById(entry.getKey())
                    .orElseGet(() -> SiteSetting.builder().settingKey(entry.getKey()).build());
            setting.setSettingValue(entry.getValue());
            setting.setUpdatedAt(Instant.now());
            repo.save(setting);
        }
    }
}
