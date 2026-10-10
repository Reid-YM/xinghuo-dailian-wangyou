package com.xinghuo.siteconfig.infrastructure;

import com.xinghuo.model.SiteConfig;
import com.xinghuo.repository.SiteConfigRepository;
import com.xinghuo.siteconfig.application.SiteRuntimePort;
import com.xinghuo.siteconfig.domain.SiteOperatingStatusCode;
import com.xinghuo.siteconfig.domain.SiteRuntimeState;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class SiteRuntimePersistenceAdapter implements SiteRuntimePort {
    private final SiteConfigRepository repo;

    public SiteRuntimePersistenceAdapter(SiteConfigRepository repo) {
        this.repo = repo;
    }

    @Override
    @Transactional(readOnly = true)
    public SiteRuntimeState load() {
        SiteConfig config = repo.findById(1L).orElseThrow();
        return new SiteRuntimeState(
                config.isQuoteEnabled(),
                config.isOrderQueryEnabled(),
                SiteOperatingStatusCode.valueOf(config.getServiceStatus().name()),
                config.getStatusShortText(),
                config.getPrimaryButtonLabel());
    }
}
