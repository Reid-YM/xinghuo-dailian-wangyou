package com.xinghuo.security.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Generated;

@Entity
@Table(name="admin_users")
public class AdminUser {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;
    @Column(nullable=false, unique=true, length=64)
    private String username;
    @Column(name="password_hash", nullable=false)
    private String passwordHash;
    @Enumerated(value=EnumType.STRING)
    @Column(nullable=false, length=32)
    private Role role;
    @Column(nullable=false)
    private boolean enabled;
    @Column(name="created_at", nullable=false)
    private Instant createdAt;
    @Column(name="updated_at", nullable=false)
    private Instant updatedAt;

    @Generated
    public static AdminUserBuilder builder() {
        return new AdminUserBuilder();
    }

    @Generated
    public Long getId() {
        return this.id;
    }

    @Generated
    public String getUsername() {
        return this.username;
    }

    @Generated
    public String getPasswordHash() {
        return this.passwordHash;
    }

    @Generated
    public Role getRole() {
        return this.role;
    }

    @Generated
    public boolean isEnabled() {
        return this.enabled;
    }

    @Generated
    public Instant getCreatedAt() {
        return this.createdAt;
    }

    @Generated
    public Instant getUpdatedAt() {
        return this.updatedAt;
    }

    @Generated
    public void setId(Long id) {
        this.id = id;
    }

    @Generated
    public void setUsername(String username) {
        this.username = username;
    }

    @Generated
    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    @Generated
    public void setRole(Role role) {
        this.role = role;
    }

    @Generated
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Generated
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    @Generated
    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Generated
    public AdminUser() {
    }

    @Generated
    public AdminUser(Long id, String username, String passwordHash, Role role, boolean enabled, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
        this.enabled = enabled;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    @Generated
    public static class AdminUserBuilder {
        @Generated
        private Long id;
        @Generated
        private String username;
        @Generated
        private String passwordHash;
        @Generated
        private Role role;
        @Generated
        private boolean enabled;
        @Generated
        private Instant createdAt;
        @Generated
        private Instant updatedAt;

        @Generated
        AdminUserBuilder() {
        }

        @Generated
        public AdminUserBuilder id(Long id) {
            this.id = id;
            return this;
        }

        @Generated
        public AdminUserBuilder username(String username) {
            this.username = username;
            return this;
        }

        @Generated
        public AdminUserBuilder passwordHash(String passwordHash) {
            this.passwordHash = passwordHash;
            return this;
        }

        @Generated
        public AdminUserBuilder role(Role role) {
            this.role = role;
            return this;
        }

        @Generated
        public AdminUserBuilder enabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        @Generated
        public AdminUserBuilder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        @Generated
        public AdminUserBuilder updatedAt(Instant updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        @Generated
        public AdminUser build() {
            return new AdminUser(this.id, this.username, this.passwordHash, this.role, this.enabled, this.createdAt, this.updatedAt);
        }

        @Generated
        public String toString() {
            return "AdminUserBuilder(id=" + this.id + ", username=" + this.username + ", passwordHash=" + this.passwordHash + ", role=" + String.valueOf(this.role) + ", enabled=" + this.enabled + ", createdAt=" + String.valueOf(this.createdAt) + ", updatedAt=" + String.valueOf(this.updatedAt) + ")";
        }
    }

    public static enum Role {
        ADMIN,
        OPERATOR;

    }
}
