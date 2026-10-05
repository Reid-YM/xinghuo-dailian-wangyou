package com.xinghuo.account.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * 用户系统审计日志。
 *
 * <p>detail 字段必须已脱敏：手机号、邮箱、微信号、QQ 号一律不得写入明文。
 * 写入统一走 {@link com.xinghuo.account.service.AccountAuditService}，
 * 由它负责脱敏，调用方不要直接 new。
 */
@Entity
@Table(name = "user_audit_log")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(nullable = false, length = 40)
    private String action;

    @Column(length = 500)
    private String detail;

    @Column(length = 45)
    private String ip;

    @Column(name = "user_agent", length = 300)
    private String userAgent;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    @Generated
    public static class AccountAuditBuilder {
        @Generated
        private Long id;
        @Generated
        private Long userId;
        @Generated
        private String action;
        @Generated
        private String detail;
        @Generated
        private String ip;
        @Generated
        private String userAgent;
        @Generated
        private Instant createdAt;

        @Generated
        AccountAuditBuilder() {
        }

        @Generated
        public AccountAuditBuilder id(Long l) {
            this.id = l;
            return this;
        }

        @Generated
        public AccountAuditBuilder userId(Long l) {
            this.userId = l;
            return this;
        }

        @Generated
        public AccountAuditBuilder action(String string) {
            this.action = string;
            return this;
        }

        @Generated
        public AccountAuditBuilder detail(String string) {
            this.detail = string;
            return this;
        }

        @Generated
        public AccountAuditBuilder ip(String string) {
            this.ip = string;
            return this;
        }

        @Generated
        public AccountAuditBuilder userAgent(String string) {
            this.userAgent = string;
            return this;
        }

        @Generated
        public AccountAuditBuilder createdAt(Instant instant) {
            this.createdAt = instant;
            return this;
        }

        @Generated
        public AccountAudit build() {
            return new AccountAudit(this.id, this.userId, this.action, this.detail, this.ip, this.userAgent, this.createdAt);
        }

        @Generated
        public String toString() {
            return "AccountAuditBuilder(id=" + this.id + ", userId=" + this.userId + ", action=" + this.action + ", detail=" + this.detail + ", ip=" + this.ip + ", userAgent=" + this.userAgent + ", createdAt=" + String.valueOf(this.createdAt) + ")";
        }
    }
}
