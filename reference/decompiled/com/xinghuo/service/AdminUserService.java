/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.xinghuo.model.AdminUser
 *  com.xinghuo.repository.AdminUserRepository
 *  com.xinghuo.service.AdminUserService
 *  lombok.Generated
 *  org.springframework.security.core.userdetails.User
 *  org.springframework.security.core.userdetails.UserDetails
 *  org.springframework.security.core.userdetails.UserDetailsService
 *  org.springframework.security.core.userdetails.UsernameNotFoundException
 *  org.springframework.stereotype.Service
 */
package com.xinghuo.service;

import com.xinghuo.model.AdminUser;
import com.xinghuo.repository.AdminUserRepository;
import lombok.Generated;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class AdminUserService
implements UserDetailsService {
    private final AdminUserRepository repo;

    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        AdminUser u = (AdminUser)this.repo.findByUsername(username).orElseThrow(() -> new UsernameNotFoundException("user not found"));
        return User.withUsername((String)u.getUsername()).password(u.getPasswordHash()).roles(new String[]{u.getRole().name()}).disabled(!u.isEnabled()).build();
    }

    @Generated
    public AdminUserService(AdminUserRepository repo) {
        this.repo = repo;
    }
}

