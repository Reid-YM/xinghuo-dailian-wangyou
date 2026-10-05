package com.xinghuo.account.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
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
 * 一次性令牌：邮箱验证、找回密码、换绑邮箱。
 *
 * <p>明文令牌只出现在发给用户的邮件链接里，数据库只存 SHA-256 哈希。
 * 这样即使数据库泄露，攻击者也无法用其中的记录直接重置他人密码。
 */
@Entity
@Table(name = "user_token")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private Purpose purpose;

    @Column(name = "token_hash", nullable = false, length = 64)
    private String tokenHash;

    @Column(length = 200)
    private String payload;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "used_at")
    private Instant usedAt;

    @Column(name = "request_ip", length = 45)
    private String requestIp;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    /** 令牌此刻是否可用：未使用、未过期。 */
    public boolean isUsable() {
        return usedAt == null && expiresAt != null && expiresAt.isAfter(Instant.now());
    }

    public enum Purpose {
        VERIFY_EMAIL,
        RESET_PASSWORD,
        CHANGE_EMAIL,
        REGISTER_EMAIL
    }

    @Generated
    public static class AccountTokenBuilder {
        @Generated
        private Long id;
        @Generated
        private Long userId;
        @Generated
        private Purpose purpose;
        @Generated
        private String tokenHash;
        @Generated
        private String payload;
        @Generated
        private Instant expiresAt;
        @Generated
        private Instant usedAt;
        @Generated
        private String requestIp;
        @Generated
        private Instant createdAt;

        @Generated
        AccountTokenBuilder() {
        }

        @Generated
        public AccountTokenBuilder id(Long l) {
            this.id = l;
            return this;
        }

        @Generated
        public AccountTokenBuilder userId(Long l) {
            this.userId = l;
            return this;
        }

        @Generated
        public AccountTokenBuilder purpose(Purpose purpose) {
            this.purpose = purpose;
            return this;
        }

        @Generated
        public AccountTokenBuilder tokenHash(String string) {
            this.tokenHash = string;
            return this;
        }

        @Generated
        public AccountTokenBuilder payload(String string) {
            this.payload = string;
            return this;
        }

        @Generated
        public AccountTokenBuilder expiresAt(Instant instant) {
            this.expiresAt = instant;
            return this;
        }

        @Generated
        public AccountTokenBuilder usedAt(Instant instant) {
            this.usedAt = instant;
            return this;
        }

        @Generated
        public AccountTokenBuilder requestIp(String string) {
            this.requestIp = string;
            return this;
        }

        @Generated
        public AccountTokenBuilder createdAt(Instant instant) {
            this.createdAt = instant;
            return this;
        }

        @Generated
        public AccountToken build() {
            return new AccountToken(this.id, this.userId, this.purpose, this.tokenHash, this.payload, this.expiresAt, this.usedAt, this.requestIp, this.createdAt);
        }

        @Generated
        public String toString() {
            return "AccountTokenBuilder(id=" + this.id + ", userId=" + this.userId + ", purpose=" + String.valueOf(this.purpose) + ", tokenHash=" + this.tokenHash + ", payload=" + this.payload + ", expiresAt=" + String.valueOf(this.expiresAt) + ", usedAt=" + String.valueOf(this.usedAt) + ", requestIp=" + this.requestIp + ", createdAt=" + String.valueOf(this.createdAt) + ")";
        }
    }
}
