/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.xinghuo.model.CustomerAccount
 *  com.xinghuo.repository.CustomerAccountRepository
 *  org.springframework.data.jpa.repository.JpaRepository
 */
package com.xinghuo.repository;

import com.xinghuo.model.CustomerAccount;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerAccountRepository
extends JpaRepository<CustomerAccount, Long> {
    public Optional<CustomerAccount> findByContactMethodAndContactValue(String var1, String var2);
}

