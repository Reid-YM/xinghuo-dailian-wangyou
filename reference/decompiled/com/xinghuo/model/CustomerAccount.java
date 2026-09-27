/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.xinghuo.model.CustomerAccount
 *  com.xinghuo.model.CustomerAccount$CustomerAccountBuilder
 *  com.xinghuo.model.CustomerAccount$Role
 *  jakarta.persistence.Column
 *  jakarta.persistence.Entity
 *  jakarta.persistence.EnumType
 *  jakarta.persistence.Enumerated
 *  jakarta.persistence.GeneratedValue
 *  jakarta.persistence.GenerationType
 *  jakarta.persistence.Id
 *  jakarta.persistence.Table
 *  jakarta.persistence.UniqueConstraint
 *  lombok.Generated
 */
package com.xinghuo.model;

import com.xinghuo.model.CustomerAccount;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.Generated;

@Entity
@Table(name="customer_accounts", uniqueConstraints={@UniqueConstraint(name="uk_customer_contact", columnNames={"contact_method", "contact_value"})})
public class CustomerAccount {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;
    @Column(name="display_name", nullable=false, length=80)
    private String displayName;
    @Column(name="contact_method", nullable=false, length=40)
    private String contactMethod;
    @Column(name="contact_value", nullable=false, length=120)
    private String contactValue;
    @Column(name="password_hash", nullable=false)
    private String passwordHash;
    @Enumerated(value=EnumType.STRING)
    @Column(nullable=false, length=24)
    private Role role;
    @Column(name="level_code", nullable=false, length=24)
    private String levelCode;
    @Column(nullable=false)
    private boolean enabled;
    @Column(name="verified_at")
    private Instant verifiedAt;
    @Column(name="last_login_at")
    private Instant lastLoginAt;
    @Column(name="created_at", nullable=false)
    private Instant createdAt;
    @Column(name="updated_at", nullable=false)
    private Instant updatedAt;

    @Generated
    public static CustomerAccountBuilder builder() {
        return new CustomerAccountBuilder();
    }

    @Generated
    public Long getId() {
        return this.id;
    }

    @Generated
    public String getDisplayName() {
        return this.displayName;
    }

    @Generated
    public String getContactMethod() {
        return this.contactMethod;
    }

    @Generated
    public String getContactValue() {
        return this.contactValue;
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
    public String getLevelCode() {
        return this.levelCode;
    }

    @Generated
    public boolean isEnabled() {
        return this.enabled;
    }

    @Generated
    public Instant getVerifiedAt() {
        return this.verifiedAt;
    }

    @Generated
    public Instant getLastLoginAt() {
        return this.lastLoginAt;
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
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    @Generated
    public void setContactMethod(String contactMethod) {
        this.contactMethod = contactMethod;
    }

    @Generated
    public void setContactValue(String contactValue) {
        this.contactValue = contactValue;
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
    public void setLevelCode(String levelCode) {
        this.levelCode = levelCode;
    }

    @Generated
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Generated
    public void setVerifiedAt(Instant verifiedAt) {
        this.verifiedAt = verifiedAt;
    }

    @Generated
    public void setLastLoginAt(Instant lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
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
    public CustomerAccount() {
    }

    @Generated
    public CustomerAccount(Long id, String displayName, String contactMethod, String contactValue, String passwordHash, Role role, String levelCode, boolean enabled, Instant verifiedAt, Instant lastLoginAt, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.displayName = displayName;
        this.contactMethod = contactMethod;
        this.contactValue = contactValue;
        this.passwordHash = passwordHash;
        this.role = role;
        this.levelCode = levelCode;
        this.enabled = enabled;
        this.verifiedAt = verifiedAt;
        this.lastLoginAt = lastLoginAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}

