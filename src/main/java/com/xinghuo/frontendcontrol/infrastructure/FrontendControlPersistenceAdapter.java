package com.xinghuo.frontendcontrol.infrastructure;

import com.xinghuo.frontendcontrol.application.FrontendControlPort;
import com.xinghuo.model.SiteSetting;
import com.xinghuo.repository.SiteSettingRepository;
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
    public void put(String key, String value) {
        SiteSetting setting = repo.findById(key)
                .orElseGet(() -> SiteSetting.builder().settingKey(key).build());
        setting.setSettingValue(value);
        setting.setUpdatedAt(Instant.now());
        repo.save(setting);
    }
}
