package com.xinghuo.siteconfig.infrastructure;

import com.xinghuo.model.SiteConfig;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SiteConfigRepository extends JpaRepository<SiteConfig, Long> {
}
