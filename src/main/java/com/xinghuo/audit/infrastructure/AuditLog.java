package com.xinghuo.audit.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Generated;

@Entity
@Table(name="audit_logs")
public class AuditLog {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;
    @Column(nullable=false, length=64)
    private String action;
    @Column(name="target_type", nullable=false, length=64)
    private String targetType;
    @Column(name="target_id", nullable=false, length=100)
    private String targetId;
    @Column(name="operator_name", nullable=false, length=100)
    private String operatorName;
    @Column(name="before_json", columnDefinition="TEXT")
    private String beforeJson;
    @Column(name="after_json", columnDefinition="TEXT")
    private String afterJson;
    @Column(length=500)
    private String reason;
    @Column(name="ip_address", length=64)
    private String ipAddress;
    @Column(name="request_id", length=100)
    private String requestId;
    @Column(name="created_at", nullable=false)
    private Instant createdAt;

    @Generated
    public static AuditLogBuilder builder() {
        return new AuditLogBuilder();
    }

    @Generated
    public Long getId() {
        return this.id;
    }

    @Generated
    public String getAction() {
        return this.action;
    }

    @Generated
    public String getTargetType() {
        return this.targetType;
    }

    @Generated
    public String getTargetId() {
        return this.targetId;
    }

    @Generated
    public String getOperatorName() {
        return this.operatorName;
    }

    @Generated
    public String getBeforeJson() {
        return this.beforeJson;
    }

    @Generated
    public String getAfterJson() {
        return this.afterJson;
    }

    @Generated
    public String getReason() {
        return this.reason;
    }

    @Generated
    public String getIpAddress() {
        return this.ipAddress;
    }

    @Generated
    public String getRequestId() {
        return this.requestId;
    }

    @Generated
    public Instant getCreatedAt() {
        return this.createdAt;
    }

    @Generated
    public void setId(Long id) {
        this.id = id;
    }

    @Generated
    public void setAction(String action) {
        this.action = action;
    }

    @Generated
    public void setTargetType(String targetType) {
        this.targetType = targetType;
    }

    @Generated
    public void setTargetId(String targetId) {
        this.targetId = targetId;
    }

    @Generated
    public void setOperatorName(String operatorName) {
        this.operatorName = operatorName;
    }

    @Generated
    public void setBeforeJson(String beforeJson) {
        this.beforeJson = beforeJson;
    }

    @Generated
    public void setAfterJson(String afterJson) {
        this.afterJson = afterJson;
    }

    @Generated
    public void setReason(String reason) {
        this.reason = reason;
    }

    @Generated
    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    @Generated
    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    @Generated
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    @Generated
    public AuditLog() {
    }

    @Generated
    public AuditLog(Long id, String action, String targetType, String targetId, String operatorName, String beforeJson, String afterJson, String reason, String ipAddress, String requestId, Instant createdAt) {
        this.id = id;
        this.action = action;
        this.targetType = targetType;
        this.targetId = targetId;
        this.operatorName = operatorName;
        this.beforeJson = beforeJson;
        this.afterJson = afterJson;
        this.reason = reason;
        this.ipAddress = ipAddress;
        this.requestId = requestId;
        this.createdAt = createdAt;
    }

    @Generated
    public static class AuditLogBuilder {
        @Generated
        private Long id;
        @Generated
        private String action;
        @Generated
        private String targetType;
        @Generated
        private String targetId;
        @Generated
        private String operatorName;
        @Generated
        private String beforeJson;
        @Generated
        private String afterJson;
        @Generated
        private String reason;
        @Generated
        private String ipAddress;
        @Generated
        private String requestId;
        @Generated
        private Instant createdAt;

        @Generated
        AuditLogBuilder() {
        }

        @Generated
        public AuditLogBuilder id(Long id) {
            this.id = id;
            return this;
        }

        @Generated
        public AuditLogBuilder action(String action) {
            this.action = action;
            return this;
        }

        @Generated
        public AuditLogBuilder targetType(String targetType) {
            this.targetType = targetType;
            return this;
        }

        @Generated
        public AuditLogBuilder targetId(String targetId) {
            this.targetId = targetId;
            return this;
        }

        @Generated
        public AuditLogBuilder operatorName(String operatorName) {
            this.operatorName = operatorName;
            return this;
        }

        @Generated
        public AuditLogBuilder beforeJson(String beforeJson) {
            this.beforeJson = beforeJson;
            return this;
        }

        @Generated
        public AuditLogBuilder afterJson(String afterJson) {
            this.afterJson = afterJson;
            return this;
        }

        @Generated
        public AuditLogBuilder reason(String reason) {
            this.reason = reason;
            return this;
        }

        @Generated
        public AuditLogBuilder ipAddress(String ipAddress) {
            this.ipAddress = ipAddress;
            return this;
        }

        @Generated
        public AuditLogBuilder requestId(String requestId) {
            this.requestId = requestId;
            return this;
        }

        @Generated
        public AuditLogBuilder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        @Generated
        public AuditLog build() {
            return new AuditLog(this.id, this.action, this.targetType, this.targetId, this.operatorName, this.beforeJson, this.afterJson, this.reason, this.ipAddress, this.requestId, this.createdAt);
        }

        @Generated
        public String toString() {
            return "AuditLogBuilder(id=" + this.id + ", action=" + this.action + ", targetType=" + this.targetType + ", targetId=" + this.targetId + ", operatorName=" + this.operatorName + ", beforeJson=" + this.beforeJson + ", afterJson=" + this.afterJson + ", reason=" + this.reason + ", ipAddress=" + this.ipAddress + ", requestId=" + this.requestId + ", createdAt=" + String.valueOf(this.createdAt) + ")";
        }
    }
}
