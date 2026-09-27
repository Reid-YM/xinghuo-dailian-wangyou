/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.xinghuo.model.SiteSetting
 *  com.xinghuo.model.SiteSetting$SiteSettingBuilder
 *  jakarta.persistence.Column
 *  jakarta.persistence.Entity
 *  jakarta.persistence.Id
 *  jakarta.persistence.Table
 *  lombok.Generated
 */
package com.xinghuo.model;

import com.xinghuo.model.SiteSetting;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Generated;

@Entity
@Table(name="site_settings")
public class SiteSetting {
    @Id
    @Column(name="setting_key", length=64)
    private String settingKey;
    @Column(name="setting_value", length=500)
    private String settingValue;
    @Column(name="updated_at", nullable=false)
    private Instant updatedAt;

    @Generated
    public static SiteSettingBuilder builder() {
        return new SiteSettingBuilder();
    }

    @Generated
    public String getSettingKey() {
        return this.settingKey;
    }

    @Generated
    public String getSettingValue() {
        return this.settingValue;
    }

    @Generated
    public Instant getUpdatedAt() {
        return this.updatedAt;
    }

    @Generated
    public void setSettingKey(String settingKey) {
        this.settingKey = settingKey;
    }

    @Generated
    public void setSettingValue(String settingValue) {
        this.settingValue = settingValue;
    }

    @Generated
    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Generated
    public SiteSetting() {
    }

    @Generated
    public SiteSetting(String settingKey, String settingValue, Instant updatedAt) {
        this.settingKey = settingKey;
        this.settingValue = settingValue;
        this.updatedAt = updatedAt;
    }
}

