/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.xinghuo.model.AdminUser
 *  com.xinghuo.repository.AdminUserRepository
 *  org.springframework.data.jpa.repository.JpaRepository
 */
package com.xinghuo.repository;

import com.xinghuo.model.AdminUser;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminUserRepository
extends JpaRepository<AdminUser, Long> {
    public Optional<AdminUser> findByUsername(String var1);
}

