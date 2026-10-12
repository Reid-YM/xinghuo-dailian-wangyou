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
 * 第三方登录身份绑定。
 *
 * <p>open_id 按应用隔离，因此网站微信开放平台与微信小程序分别使用 WECHAT / WECHAT_MINI；
 * 唯一键仍是 (provider, open_id)。union_id 用于在同一微信开放平台主体下把不同应用身份
 * 归并到同一个星火账号。
 */
@Entity
@Table(name = "user_identity")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountIdentity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Provider provider;

    @Column(name = "open_id", nullable = false, length = 128)
    private String openId;

    @Column(name = "union_id", length = 128)
    private String unionId;

    @Column(length = 120)
    private String nickname;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public enum Provider {
        QQ,
        WECHAT,
        WECHAT_MINI
    }

    @Generated
    public static class AccountIdentityBuilder {
        @Generated
        private Long id;
        @Generated
        private Long userId;
        @Generated
        private Provider provider;
        @Generated
        private String openId;
        @Generated
        private String unionId;
        @Generated
        private String nickname;
        @Generated
        private String avatarUrl;
        @Generated
        private Instant createdAt;
        @Generated
        private Instant lastLoginAt;

        @Generated
        AccountIdentityBuilder() {
        }

        @Generated
        public AccountIdentityBuilder id(Long l) {
            this.id = l;
            return this;
        }

        @Generated
        public AccountIdentityBuilder userId(Long l) {
            this.userId = l;
            return this;
        }

        @Generated
        public AccountIdentityBuilder provider(Provider provider) {
            this.provider = provider;
            return this;
        }

        @Generated
        public AccountIdentityBuilder openId(String string) {
            this.openId = string;
            return this;
        }

        @Generated
        public AccountIdentityBuilder unionId(String string) {
            this.unionId = string;
            return this;
        }

        @Generated
        public AccountIdentityBuilder nickname(String string) {
            this.nickname = string;
            return this;
        }

        @Generated
        public AccountIdentityBuilder avatarUrl(String string) {
            this.avatarUrl = string;
            return this;
        }

        @Generated
        public AccountIdentityBuilder createdAt(Instant instant) {
            this.createdAt = instant;
            return this;
        }

        @Generated
        public AccountIdentityBuilder lastLoginAt(Instant instant) {
            this.lastLoginAt = instant;
            return this;
        }

        @Generated
        public AccountIdentity build() {
            return new AccountIdentity(this.id, this.userId, this.provider, this.openId, this.unionId, this.nickname, this.avatarUrl, this.createdAt, this.lastLoginAt);
        }

        @Generated
        public String toString() {
            return "AccountIdentityBuilder(id=" + this.id + ", userId=" + this.userId + ", provider=" + String.valueOf(this.provider) + ", openId=" + this.openId + ", unionId=" + this.unionId + ", nickname=" + this.nickname + ", avatarUrl=" + this.avatarUrl + ", createdAt=" + String.valueOf(this.createdAt) + ", lastLoginAt=" + String.valueOf(this.lastLoginAt) + ")";
        }
    }
}
