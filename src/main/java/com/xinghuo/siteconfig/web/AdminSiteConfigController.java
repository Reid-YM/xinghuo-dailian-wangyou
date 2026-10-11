package com.xinghuo.siteconfig.web;

import com.xinghuo.exception.BadRequestException;
import com.xinghuo.siteconfig.application.SiteConfigPublicApplicationService;
import com.xinghuo.upload.infrastructure.UploadAsset;
import com.xinghuo.upload.infrastructure.UploadService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Locale;
import java.util.Map;
import lombok.Generated;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping(value={"/api/admin/site-config"})
public class AdminSiteConfigController {
    private final SiteConfigWriteWebService configWrites;
    private final SiteConfigPublicApplicationService siteConfigQuery;
    private final UploadService uploads;

    @GetMapping
    SiteConfigResponse getConfig() {
        return SiteConfigWebMapper.toResponse(this.siteConfigQuery.get());
    }

    @PatchMapping(value={"/section/{section}"})
    SiteConfigResponse updateConfigSection(@PathVariable String section, @RequestBody Map<String, Object> body, Authentication a, HttpServletRequest req) {
        return this.configWrites.updateSection(section, body, a.getName(), req);
    }

    @PatchMapping
    SiteConfigResponse updateConfig(@Valid @RequestBody SiteConfigUpdate r, Authentication a, HttpServletRequest req) {
        return this.configWrites.update(r, a.getName(), req);
    }

    @PostMapping(value={"/image/{slot}"})
    SiteImageUploadResponse uploadSiteImage(@PathVariable String slot, @RequestParam(value="file") MultipartFile file, Authentication a, HttpServletRequest req) {
        String key;
        UploadAsset.Kind kind = switch (key = slot.trim().toUpperCase(Locale.ROOT)) {
            case "HERO" -> UploadAsset.Kind.SITE_HERO;
            case "BACKGROUND" -> UploadAsset.Kind.SITE_BACKGROUND;
            case "BACKGROUND_MOBILE" -> UploadAsset.Kind.SITE_BACKGROUND;
            case "WECHAT_QR" -> UploadAsset.Kind.SITE_WECHAT_QR;
            case "CONTACT_QR" -> UploadAsset.Kind.SITE_WECHAT_QR;
            default -> throw new BadRequestException("非法图片位置");
        };
        UploadAsset asset = this.uploads.storeImage(file, kind);
        this.uploads.bind(asset.getId(), "SITE_CONFIG", key);
        String url = "/api/site-assets/" + asset.getId();
        if (!"CONTACT_QR".equals(key)) {
            this.configWrites.updateSiteImage(key, url, a.getName(), req);
        }
        return new SiteImageUploadResponse(url);
    }

    @DeleteMapping(value={"/image/{slot}"})
    SiteConfigResponse clearSiteImage(@PathVariable String slot, Authentication a, HttpServletRequest req) {
        return this.configWrites.updateSiteImage(slot, null, a.getName(), req);
    }

    @Generated
    public AdminSiteConfigController(SiteConfigWriteWebService configWrites, SiteConfigPublicApplicationService siteConfigQuery, UploadService uploads) {
        this.configWrites = configWrites;
        this.siteConfigQuery = siteConfigQuery;
        this.uploads = uploads;
    }
}
