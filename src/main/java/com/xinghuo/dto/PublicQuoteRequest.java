package com.xinghuo.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record PublicQuoteRequest(
    @NotBlank @Size(max = 100) String game,
    @NotBlank @Size(max = 200) String demand,
    @Size(max = 500) String currentProgress,
    UUID screenshotAssetId,
    @Size(max = 50) String platform,
    @Size(max = 100) String expectedTime,
    @Size(max = 40) String contactMethod,
    @Size(max = 120) String customerContact,
    @NotNull @AssertTrue(message = "请先阅读并同意服务规则与隐私说明") Boolean privacyConsent,
    @Size(max = 64) String firstTouchSource,
    @Size(max = 64) String utmSource,
    @Size(max = 64) String utmMedium,
    @Size(max = 64) String utmCampaign,
    @Size(max = 300) String referrer
) {}
