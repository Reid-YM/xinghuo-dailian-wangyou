package com.xinghuo.model;

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

    @Generated
    public static class SiteSettingBuilder {
        @Generated
        private String settingKey;
        @Generated
        private String settingValue;
        @Generated
        private Instant updatedAt;

        @Generated
        SiteSettingBuilder() {
        }

        @Generated
        public SiteSettingBuilder settingKey(String settingKey) {
            this.settingKey = settingKey;
            return this;
        }

        @Generated
        public SiteSettingBuilder settingValue(String settingValue) {
            this.settingValue = settingValue;
            return this;
        }

        @Generated
        public SiteSettingBuilder updatedAt(Instant updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        @Generated
        public SiteSetting build() {
            return new SiteSetting(this.settingKey, this.settingValue, this.updatedAt);
        }

        @Generated
        public String toString() {
            return "SiteSettingBuilder(settingKey=" + this.settingKey + ", settingValue=" + this.settingValue + ", updatedAt=" + String.valueOf(this.updatedAt) + ")";
        }
    }
}

