package com.xinghuo.siteconfig.web;

import com.xinghuo.audit.infrastructure.AuditService;
import com.xinghuo.dto.ApiDtos;
import com.xinghuo.siteconfig.application.SiteConfigPublicApplicationService;
import com.xinghuo.siteconfig.application.SiteConfigWriteApplicationService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** HTTP-facing transactional orchestration for SiteConfig writes and legacy-compatible audit snapshots. */
@Service
public class SiteConfigWriteWebService {
    private final SiteConfigWriteApplicationService writes;
    private final SiteConfigPublicApplicationService query;
    private final AuditService audit;

    public SiteConfigWriteWebService(
            SiteConfigWriteApplicationService writes,
            SiteConfigPublicApplicationService query,
            AuditService audit) {
        this.writes = writes;
        this.query = query;
        this.audit = audit;
    }

    @Transactional
    public ApiDtos.SiteConfigResponse updateSiteImage(
            String slot,
            String url,
            String operator,
            HttpServletRequest request) {
        String key = slot == null ? "" : slot.trim().toUpperCase(Locale.ROOT);
        ApiDtos.SiteConfigResponse before = current();
        writes.updateSiteImage(slot, url);
        ApiDtos.SiteConfigResponse after = current();
        audit.log("SITE_IMAGE_UPDATE", "SITE_CONFIG", key, operator, before, after, "更新站点图片", request);
        return after;
    }

    @Transactional
    public ApiDtos.SiteConfigResponse update(
            ApiDtos.SiteConfigUpdate update,
            String operator,
            HttpServletRequest request) {
        ApiDtos.SiteConfigResponse before = current();
        writes.update(SiteConfigWriteWebMapper.toCommand(update));
        ApiDtos.SiteConfigResponse after = current();
        audit.log("SITE_CONFIG_UPDATE", "SITE_CONFIG", "1", operator, before, after, update.reason(), request);
        return after;
    }

    @Transactional
    public ApiDtos.SiteConfigResponse updateSection(
            String section,
            Map<String, Object> values,
            String operator,
            HttpServletRequest request) {
        String key = section == null ? "" : section.trim().toLowerCase(Locale.ROOT);
        ApiDtos.SiteConfigResponse before = current();
        writes.updateSection(section, values);
        ApiDtos.SiteConfigResponse after = current();
        audit.log(
                "SITE_CONFIG_" + key.toUpperCase(Locale.ROOT),
                "SITE_CONFIG",
                key,
                operator,
                before,
                after,
                String.valueOf(values.getOrDefault("reason", "后台分区保存")),
                request);
        return after;
    }

    private ApiDtos.SiteConfigResponse current() {
        return SiteConfigWebMapper.toResponse(query.get());
    }
}
