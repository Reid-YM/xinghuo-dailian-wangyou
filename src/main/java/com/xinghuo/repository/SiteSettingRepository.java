package com.xinghuo.repository;

import com.xinghuo.model.SiteSetting;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SiteSettingRepository
extends JpaRepository<SiteSetting, String> {
}

