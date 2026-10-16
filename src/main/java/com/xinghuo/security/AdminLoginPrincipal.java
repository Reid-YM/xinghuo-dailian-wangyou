package com.xinghuo.security;

import java.util.List;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;

/** Keeps only the login revision after Spring erases the password credentials. */
public final class AdminLoginPrincipal extends User {
    private static final long serialVersionUID = 1L;
    private final long authVersion;

    public AdminLoginPrincipal(String username,String password,String role,boolean enabled,long authVersion) {
        super(username,password,enabled,true,true,true,List.of(new SimpleGrantedAuthority("ROLE_"+role)));
        this.authVersion=authVersion;
    }
    public long authVersion(){return authVersion;}
}
