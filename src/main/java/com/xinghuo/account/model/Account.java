package com.xinghuo.account.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Generated;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 用户账号主表（V35 重建后的 customer_accounts）。
 *
 * <p>表名沿用 {@code customer_accounts} 是刻意的：{@code quotes.customer_account_id}、
 * {@code customer_orders.customer_account_id} 以及 V34 的 seller_profiles /
 * booster_profiles 都以它为身份锚点，沿用表名可让这些既有引用保持语义正确。
 *
 * <p>本类取代基线 jar 中的 {@code com.xinghuo.model.CustomerAccount}。
 * 旧类的关联关系已核对：Quote / CustomerOrder 只持有普通 Long customerAccountId，
 * 不是 @ManyToOne，因此替换实体不会影响它们的映射。
 */
@Entity
@Table(name = "customer_accounts",
        uniqueConstraints = {
            @UniqueConstraint(name = "uk_customer_contact_norm",
                    columnNames = {"contact_type", "contact_norm"}),
            @UniqueConstraint(name = "uk_customer_email", columnNames = {"email_norm"})
        })
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "contact_type", nullable = false, length = 16)
    private ContactType contactType;

    @Column(name = "contact_value", nullable = false, length = 120)
    private String contactValue;

    @Column(name = "contact_norm", nullable = false, length = 120)
    private String contactNorm;

    @Column(name = "contact_verified", nullable = false)
    private boolean contactVerified;

    /**
     * 询价联系方式（可多条），与登录凭据解耦（见 V56 / V57 迁移说明）。
     *
     * <p>contact_* 是登录凭据，改它等于换登录方式；quote_contacts 只用于
     * 客服回访，可在后台免验证绑定，因此不能拿它做身份判定。
     * 形如 [{"type":"PHONE","value":"13800000000"}]，解析与校验见 QuoteContactService。
     */
    @Column(name = "quote_contacts", columnDefinition = "TEXT")
    private String quoteContacts;

    @Column(name = "public_user_id", nullable = false, length = 24, unique = true)
    private String publicUserId;

    @Column(length = 160)
    private String email;

    @Column(name = "email_norm", length = 160)
    private String emailNorm;

    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(name = "display_name", nullable = false, length = 60)
    private String displayName;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    /** 与 marketplace 对齐的角色：BUYER / SELLER / BOOSTER。 */
    @Column(nullable = false, length = 24)
    private String role;

    /** 会员等级，权益模型落地后使用。 */
    @Column(name = "level_code", nullable = false, length = 24)
    private String levelCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private AccountStatus status;

    @Column(name = "failed_attempts", nullable = false)
    private int failedAttempts;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @Column(name = "last_login_ip", length = 45)
    private String lastLoginIp;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
        if (status == null) {
            status = AccountStatus.ACTIVE;
        }
        if (role == null) {
            role = "BUYER";
        }
        if (levelCode == null) {
            levelCode = "NEW";
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    /** 账号是否处于可登录状态。 */
    public boolean isLoginable() {
        return status == AccountStatus.ACTIVE;
    }

    /** 当前是否处于失败锁定窗口内。 */
    public boolean isLockedNow() {
        return lockedUntil != null && lockedUntil.isAfter(Instant.now());
    }

    public enum ContactType {
        EMAIL,
        WECHAT,
        QQ,
        PHONE
    }

    public enum AccountStatus {
        ACTIVE,
        LOCKED,
        DISABLED,
        DELETED
    }

    @Generated
    public static class AccountBuilder {
        @Generated
        private Long id;
        @Generated
        private ContactType contactType;
        @Generated
        private String contactValue;
        @Generated
        private String contactNorm;
        @Generated
        private boolean contactVerified;
        @Generated
        private String quoteContacts;
        @Generated
        private String publicUserId;
        @Generated
        private String email;
        @Generated
        private String emailNorm;
        @Generated
        private boolean emailVerified;
        @Generated
        private String passwordHash;
        @Generated
        private String displayName;
        @Generated
        private String avatarUrl;
        @Generated
        private String role;
        @Generated
        private String levelCode;
        @Generated
        private AccountStatus status;
        @Generated
        private int failedAttempts;
        @Generated
        private Instant lockedUntil;
        @Generated
        private Instant lastLoginAt;
        @Generated
        private String lastLoginIp;
        @Generated
        private Instant createdAt;
        @Generated
        private Instant updatedAt;
        @Generated
        private Instant deletedAt;

        @Generated
        AccountBuilder() {
        }

        @Generated
        public AccountBuilder id(Long l) {
            this.id = l;
            return this;
        }

        @Generated
        public AccountBuilder contactType(ContactType contactType) {
            this.contactType = contactType;
            return this;
        }

        @Generated
        public AccountBuilder contactValue(String string) {
            this.contactValue = string;
            return this;
        }

        @Generated
        public AccountBuilder contactNorm(String string) {
            this.contactNorm = string;
            return this;
        }

        @Generated
        public AccountBuilder contactVerified(boolean bl) {
            this.contactVerified = bl;
            return this;
        }

        @Generated
        public AccountBuilder publicUserId(String string) {
            this.publicUserId = string;
            return this;
        }

        @Generated
        public AccountBuilder email(String string) {
            this.email = string;
            return this;
        }

        @Generated
        public AccountBuilder emailNorm(String string) {
            this.emailNorm = string;
            return this;
        }

        @Generated
        public AccountBuilder emailVerified(boolean bl) {
            this.emailVerified = bl;
            return this;
        }

        @Generated
        public AccountBuilder passwordHash(String string) {
            this.passwordHash = string;
            return this;
        }

        @Generated
        public AccountBuilder displayName(String string) {
            this.displayName = string;
            return this;
        }

        @Generated
        public AccountBuilder avatarUrl(String string) {
            this.avatarUrl = string;
            return this;
        }

        @Generated
        public AccountBuilder role(String string) {
            this.role = string;
            return this;
        }

        @Generated
        public AccountBuilder levelCode(String string) {
            this.levelCode = string;
            return this;
        }

        @Generated
        public AccountBuilder status(AccountStatus accountStatus) {
            this.status = accountStatus;
            return this;
        }

        @Generated
        public AccountBuilder failedAttempts(int n) {
            this.failedAttempts = n;
            return this;
        }

        @Generated
        public AccountBuilder lockedUntil(Instant instant) {
            this.lockedUntil = instant;
            return this;
        }

        @Generated
        public AccountBuilder lastLoginAt(Instant instant) {
            this.lastLoginAt = instant;
            return this;
        }

        @Generated
        public AccountBuilder lastLoginIp(String string) {
            this.lastLoginIp = string;
            return this;
        }

        @Generated
        public AccountBuilder createdAt(Instant instant) {
            this.createdAt = instant;
            return this;
        }

        @Generated
        public AccountBuilder updatedAt(Instant instant) {
            this.updatedAt = instant;
            return this;
        }

        @Generated
        public AccountBuilder deletedAt(Instant instant) {
            this.deletedAt = instant;
            return this;
        }

        @Generated
        public AccountBuilder quoteContacts(String string) {
            this.quoteContacts = string;
            return this;
        }

        @Generated
        public Account build() {
            return new Account(this.id, this.contactType, this.contactValue, this.contactNorm, this.contactVerified, this.quoteContacts, this.publicUserId, this.email, this.emailNorm, this.emailVerified, this.passwordHash, this.displayName, this.avatarUrl, this.role, this.levelCode, this.status, this.failedAttempts, this.lockedUntil, this.lastLoginAt, this.lastLoginIp, this.createdAt, this.updatedAt, this.deletedAt);
        }

        @Generated
        public String toString() {
            return "AccountBuilder(id=" + this.id + ", contactType=" + String.valueOf(this.contactType) + ", contactValue=" + this.contactValue + ", contactNorm=" + this.contactNorm + ", contactVerified=" + this.contactVerified + ", publicUserId=" + this.publicUserId + ", email=" + this.email + ", emailNorm=" + this.emailNorm + ", emailVerified=" + this.emailVerified + ", passwordHash=" + this.passwordHash + ", displayName=" + this.displayName + ", avatarUrl=" + this.avatarUrl + ", role=" + this.role + ", levelCode=" + this.levelCode + ", status=" + String.valueOf(this.status) + ", failedAttempts=" + this.failedAttempts + ", lockedUntil=" + String.valueOf(this.lockedUntil) + ", lastLoginAt=" + String.valueOf(this.lastLoginAt) + ", lastLoginIp=" + this.lastLoginIp + ", createdAt=" + String.valueOf(this.createdAt) + ", updatedAt=" + String.valueOf(this.updatedAt) + ", deletedAt=" + String.valueOf(this.deletedAt) + ")";
        }
    }
}
