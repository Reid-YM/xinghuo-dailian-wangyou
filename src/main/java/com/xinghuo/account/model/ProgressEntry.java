package com.xinghuo.account.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * 攻略工具进度（按用户 + 工具唯一）。
 *
 * <p>载荷存 MEDIUMTEXT 而非 JSON 列：基线没有任何 JSON 列映射先例，而
 * {@code ddl-auto: validate} 会在启动时校验列类型，映射不当直接导致服务起不来。
 * JSON 合法性由 {@link com.xinghuo.account.service.ProgressService} 在写入前校验。
 */
@Entity
@Table(name = "user_progress",
        uniqueConstraints = @UniqueConstraint(name = "uk_progress_user_tool",
                columnNames = {"user_id", "tool_key"}))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProgressEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "tool_key", nullable = false, length = 40)
    private String toolKey;

    @Column(nullable = false, columnDefinition = "MEDIUMTEXT")
    private String payload;

    @Column(nullable = false)
    private int version;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    @Generated
    public static class ProgressEntryBuilder {
        @Generated
        private Long id;
        @Generated
        private Long userId;
        @Generated
        private String toolKey;
        @Generated
        private String payload;
        @Generated
        private int version;
        @Generated
        private Instant createdAt;
        @Generated
        private Instant updatedAt;

        @Generated
        ProgressEntryBuilder() {
        }

        @Generated
        public ProgressEntryBuilder id(Long l) {
            this.id = l;
            return this;
        }

        @Generated
        public ProgressEntryBuilder userId(Long l) {
            this.userId = l;
            return this;
        }

        @Generated
        public ProgressEntryBuilder toolKey(String string) {
            this.toolKey = string;
            return this;
        }

        @Generated
        public ProgressEntryBuilder payload(String string) {
            this.payload = string;
            return this;
        }

        @Generated
        public ProgressEntryBuilder version(int n) {
            this.version = n;
            return this;
        }

        @Generated
        public ProgressEntryBuilder createdAt(Instant instant) {
            this.createdAt = instant;
            return this;
        }

        @Generated
        public ProgressEntryBuilder updatedAt(Instant instant) {
            this.updatedAt = instant;
            return this;
        }

        @Generated
        public ProgressEntry build() {
            return new ProgressEntry(this.id, this.userId, this.toolKey, this.payload, this.version, this.createdAt, this.updatedAt);
        }

        @Generated
        public String toString() {
            return "ProgressEntryBuilder(id=" + this.id + ", userId=" + this.userId + ", toolKey=" + this.toolKey + ", payload=" + this.payload + ", version=" + this.version + ", createdAt=" + String.valueOf(this.createdAt) + ", updatedAt=" + String.valueOf(this.updatedAt) + ")";
        }
    }
}
