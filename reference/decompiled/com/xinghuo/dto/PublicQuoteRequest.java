/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.xinghuo.dto.PublicQuoteRequest
 *  jakarta.validation.constraints.AssertTrue
 *  jakarta.validation.constraints.NotBlank
 *  jakarta.validation.constraints.NotNull
 *  jakarta.validation.constraints.Size
 */
package com.xinghuo.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record PublicQuoteRequest(@NotBlank @Size(max=100) @NotBlank @Size(max=100) String game, @NotBlank @Size(max=200) @NotBlank @Size(max=200) String demand, @Size(max=500) @Size(max=500) String currentProgress, UUID screenshotAssetId, @Size(max=50) @Size(max=50) String platform, @Size(max=100) @Size(max=100) String expectedTime, @Size(max=40) @Size(max=40) String contactMethod, @Size(max=120) @Size(max=120) String customerContact, @NotNull @AssertTrue(message="\u8bf7\u5148\u9605\u8bfb\u5e76\u540c\u610f\u670d\u52a1\u89c4\u5219\u4e0e\u9690\u79c1\u8bf4\u660e") @NotNull @AssertTrue(message="\u8bf7\u5148\u9605\u8bfb\u5e76\u540c\u610f\u670d\u52a1\u89c4\u5219\u4e0e\u9690\u79c1\u8bf4\u660e") Boolean privacyConsent, @Size(max=64) @Size(max=64) String firstTouchSource, @Size(max=64) @Size(max=64) String utmSource, @Size(max=64) @Size(max=64) String utmMedium, @Size(max=64) @Size(max=64) String utmCampaign, @Size(max=300) @Size(max=300) String referrer) {
    @NotBlank
    @Size(max=100)
    private final @NotBlank @Size(max=100) String game;
    @NotBlank
    @Size(max=200)
    private final @NotBlank @Size(max=200) String demand;
    @Size(max=500)
    private final @Size(max=500) String currentProgress;
    private final UUID screenshotAssetId;
    @Size(max=50)
    private final @Size(max=50) String platform;
    @Size(max=100)
    private final @Size(max=100) String expectedTime;
    @Size(max=40)
    private final @Size(max=40) String contactMethod;
    @Size(max=120)
    private final @Size(max=120) String customerContact;
    @NotNull
    @AssertTrue(message="\u8bf7\u5148\u9605\u8bfb\u5e76\u540c\u610f\u670d\u52a1\u89c4\u5219\u4e0e\u9690\u79c1\u8bf4\u660e")
    private final @NotNull @AssertTrue(message="\u8bf7\u5148\u9605\u8bfb\u5e76\u540c\u610f\u670d\u52a1\u89c4\u5219\u4e0e\u9690\u79c1\u8bf4\u660e") Boolean privacyConsent;
    @Size(max=64)
    private final @Size(max=64) String firstTouchSource;
    @Size(max=64)
    private final @Size(max=64) String utmSource;
    @Size(max=64)
    private final @Size(max=64) String utmMedium;
    @Size(max=64)
    private final @Size(max=64) String utmCampaign;
    @Size(max=300)
    private final @Size(max=300) String referrer;

    public PublicQuoteRequest(@NotBlank @Size(max=100) @NotBlank @Size(max=100) String game, @NotBlank @Size(max=200) @NotBlank @Size(max=200) String demand, @Size(max=500) @Size(max=500) String currentProgress, UUID screenshotAssetId, @Size(max=50) @Size(max=50) String platform, @Size(max=100) @Size(max=100) String expectedTime, @Size(max=40) @Size(max=40) String contactMethod, @Size(max=120) @Size(max=120) String customerContact, @NotNull @AssertTrue(message="\u8bf7\u5148\u9605\u8bfb\u5e76\u540c\u610f\u670d\u52a1\u89c4\u5219\u4e0e\u9690\u79c1\u8bf4\u660e") @NotNull @AssertTrue(message="\u8bf7\u5148\u9605\u8bfb\u5e76\u540c\u610f\u670d\u52a1\u89c4\u5219\u4e0e\u9690\u79c1\u8bf4\u660e") Boolean privacyConsent, @Size(max=64) @Size(max=64) String firstTouchSource, @Size(max=64) @Size(max=64) String utmSource, @Size(max=64) @Size(max=64) String utmMedium, @Size(max=64) @Size(max=64) String utmCampaign, @Size(max=300) @Size(max=300) String referrer) {
        this.game = game;
        this.demand = demand;
        this.currentProgress = currentProgress;
        this.screenshotAssetId = screenshotAssetId;
        this.platform = platform;
        this.expectedTime = expectedTime;
        this.contactMethod = contactMethod;
        this.customerContact = customerContact;
        this.privacyConsent = privacyConsent;
        this.firstTouchSource = firstTouchSource;
        this.utmSource = utmSource;
        this.utmMedium = utmMedium;
        this.utmCampaign = utmCampaign;
        this.referrer = referrer;
    }

    @NotBlank
    @Size(max=100)
    public @NotBlank @Size(max=100) String game() {
        return this.game;
    }

    @NotBlank
    @Size(max=200)
    public @NotBlank @Size(max=200) String demand() {
        return this.demand;
    }

    @Size(max=500)
    public @Size(max=500) String currentProgress() {
        return this.currentProgress;
    }

    public UUID screenshotAssetId() {
        return this.screenshotAssetId;
    }

    @Size(max=50)
    public @Size(max=50) String platform() {
        return this.platform;
    }

    @Size(max=100)
    public @Size(max=100) String expectedTime() {
        return this.expectedTime;
    }

    @Size(max=40)
    public @Size(max=40) String contactMethod() {
        return this.contactMethod;
    }

    @Size(max=120)
    public @Size(max=120) String customerContact() {
        return this.customerContact;
    }

    @NotNull
    @AssertTrue(message="\u8bf7\u5148\u9605\u8bfb\u5e76\u540c\u610f\u670d\u52a1\u89c4\u5219\u4e0e\u9690\u79c1\u8bf4\u660e")
    public @NotNull @AssertTrue(message="\u8bf7\u5148\u9605\u8bfb\u5e76\u540c\u610f\u670d\u52a1\u89c4\u5219\u4e0e\u9690\u79c1\u8bf4\u660e") Boolean privacyConsent() {
        return this.privacyConsent;
    }

    @Size(max=64)
    public @Size(max=64) String firstTouchSource() {
        return this.firstTouchSource;
    }

    @Size(max=64)
    public @Size(max=64) String utmSource() {
        return this.utmSource;
    }

    @Size(max=64)
    public @Size(max=64) String utmMedium() {
        return this.utmMedium;
    }

    @Size(max=64)
    public @Size(max=64) String utmCampaign() {
        return this.utmCampaign;
    }

    @Size(max=300)
    public @Size(max=300) String referrer() {
        return this.referrer;
    }
}

