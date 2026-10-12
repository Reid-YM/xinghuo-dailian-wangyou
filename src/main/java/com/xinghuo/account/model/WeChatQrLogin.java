package com.xinghuo.account.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 网站浏览器与微信小程序之间的一次性扫码登录会话。 */
@Entity
@Table(name = "wechat_qr_login")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeChatQrLogin {

    @Id
    @Column(length = 32)
    private String id;

    @Column(name = "browser_secret_hash", nullable = false, length = 64)
    private String browserSecretHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Status status;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "return_path", nullable = false, length = 1000)
    private String returnPath;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    @Column(name = "consumed_at")
    private Instant consumedAt;

    @Version
    @Column(nullable = false)
    private int version;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (status == null) {
            status = Status.PENDING;
        }
        if (returnPath == null || returnPath.isBlank()) {
            returnPath = "/account";
        }
    }

    public boolean isExpired() {
        return expiresAt == null || !expiresAt.isAfter(Instant.now());
    }

    public enum Status {
        PENDING,
        CONFIRMED,
        CONSUMED,
        EXPIRED
    }
}
