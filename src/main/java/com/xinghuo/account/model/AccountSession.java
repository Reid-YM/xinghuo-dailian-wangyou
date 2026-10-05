package com.xinghuo.account.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Generated;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 持久化登录会话。
 *
 * <p>存在的唯一理由是蓝绿发布：原实现把登录态放在 JVM 内存 Session 里，
 * 每次发布新实例都会让全站用户掉线。会话落到数据库后，发布不再影响登录态，
 * 同时天然获得「我的设备」与远程下线能力。
 *
 * <p>Cookie（XH_SID）里只放这里的随机 id，不放任何用户信息。
 */
@Entity
@Table(name = "user_session")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountSession {

    @Id
    @Column(length = 64)
    private String id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(length = 45)
    private String ip;

    @Column(name = "user_agent", length = 300)
    private String userAgent;

    @Column(name = "device_label", length = 80)
    private String deviceLabel;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "last_seen_at", nullable = false)
    private Instant lastSeenAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (lastSeenAt == null) {
            lastSeenAt = now;
        }
    }

    /** 会话此刻是否可用：未吊销、未过期。 */
    public boolean isUsable() {
        return revokedAt == null && expiresAt != null && expiresAt.isAfter(Instant.now());
    }

    @Generated
    public static class AccountSessionBuilder {
        @Generated
        private String id;
        @Generated
        private Long userId;
        @Generated
        private String ip;
        @Generated
        private String userAgent;
        @Generated
        private String deviceLabel;
        @Generated
        private Instant createdAt;
        @Generated
        private Instant lastSeenAt;
        @Generated
        private Instant expiresAt;
        @Generated
        private Instant revokedAt;

        @Generated
        AccountSessionBuilder() {
        }

        @Generated
        public AccountSessionBuilder id(String string) {
            this.id = string;
            return this;
        }

        @Generated
        public AccountSessionBuilder userId(Long l) {
            this.userId = l;
            return this;
        }

        @Generated
        public AccountSessionBuilder ip(String string) {
            this.ip = string;
            return this;
        }

        @Generated
        public AccountSessionBuilder userAgent(String string) {
            this.userAgent = string;
            return this;
        }

        @Generated
        public AccountSessionBuilder deviceLabel(String string) {
            this.deviceLabel = string;
            return this;
        }

        @Generated
        public AccountSessionBuilder createdAt(Instant instant) {
            this.createdAt = instant;
            return this;
        }

        @Generated
        public AccountSessionBuilder lastSeenAt(Instant instant) {
            this.lastSeenAt = instant;
            return this;
        }

        @Generated
        public AccountSessionBuilder expiresAt(Instant instant) {
            this.expiresAt = instant;
            return this;
        }

        @Generated
        public AccountSessionBuilder revokedAt(Instant instant) {
            this.revokedAt = instant;
            return this;
        }

        @Generated
        public AccountSession build() {
            return new AccountSession(this.id, this.userId, this.ip, this.userAgent, this.deviceLabel, this.createdAt, this.lastSeenAt, this.expiresAt, this.revokedAt);
        }

        @Generated
        public String toString() {
            return "AccountSessionBuilder(id=" + this.id + ", userId=" + this.userId + ", ip=" + this.ip + ", userAgent=" + this.userAgent + ", deviceLabel=" + this.deviceLabel + ", createdAt=" + String.valueOf(this.createdAt) + ", lastSeenAt=" + String.valueOf(this.lastSeenAt) + ", expiresAt=" + String.valueOf(this.expiresAt) + ", revokedAt=" + String.valueOf(this.revokedAt) + ")";
        }
    }
}
