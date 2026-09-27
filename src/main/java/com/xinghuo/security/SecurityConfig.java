/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.xinghuo.security.SecurityConfig
 *  com.xinghuo.service.AdminUserService
 *  org.springframework.context.annotation.Bean
 *  org.springframework.context.annotation.Configuration
 *  org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
 *  org.springframework.security.config.annotation.web.builders.HttpSecurity
 *  org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer$AuthorizedUrl
 *  org.springframework.security.config.annotation.web.configurers.FormLoginConfigurer
 *  org.springframework.security.core.userdetails.UserDetailsService
 *  org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
 *  org.springframework.security.crypto.password.PasswordEncoder
 *  org.springframework.security.web.SecurityFilterChain
 *  org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter$ReferrerPolicy
 */
package com.xinghuo.security;

import com.xinghuo.service.AdminUserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.security.config.annotation.web.configurers.FormLoginConfigurer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    SecurityFilterChain chain(HttpSecurity http, AdminUserService users) throws Exception {
        return (SecurityFilterChain)http.userDetailsService((UserDetailsService)users).authorizeHttpRequests(a -> ((AuthorizeHttpRequestsConfigurer.AuthorizedUrl)((AuthorizeHttpRequestsConfigurer.AuthorizedUrl)((AuthorizeHttpRequestsConfigurer.AuthorizedUrl)a.requestMatchers(new String[]{"/", "/services", "/service/**", "/game/**", "/mastery", "/quote", "/records", "/record/**", "/order", "/order/**", "/contact", "/rules", "/privacy", "/trust", "/robots.txt", "/sitemap.xml", "/favicon.ico", "/favicon.svg", "/apple-touch-icon.png", "/og.png", "/assets/**", "/api/site-config", "/api/system/version", "/api/site-assets/**", "/api/services", "/api/catalog", "/api/quote", "/api/records/**", "/api/orders/**", "/api/uploads/quote", "/api/track", "/api/tool-station", "/admin/login", "/error"})).permitAll().requestMatchers(new String[]{"/admin/**", "/api/admin/**"})).hasAnyRole(new String[]{"ADMIN", "OPERATOR"}).anyRequest()).permitAll()).formLogin(f -> ((FormLoginConfigurer)((FormLoginConfigurer)((FormLoginConfigurer)f.loginPage("/admin/login").loginProcessingUrl("/login")).defaultSuccessUrl("/admin", true)).failureUrl("/admin/login?error")).permitAll()).logout(l -> l.logoutUrl("/logout").logoutSuccessUrl("/admin/login?logout").invalidateHttpSession(true).deleteCookies(new String[]{"JSESSIONID"})).csrf(c -> c.ignoringRequestMatchers(new String[]{"/api/quote", "/api/uploads/quote", "/api/track"})).headers(h -> h.cacheControl(c -> c.disable()).contentSecurityPolicy(c -> c.policyDirectives("default-src 'self'; img-src 'self' data: https:; style-src 'self' 'unsafe-inline'; script-src 'self'; connect-src 'self'; object-src 'none'; frame-ancestors 'none'; base-uri 'self'; form-action 'self'")).referrerPolicy(r -> r.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.SAME_ORIGIN)).permissionsPolicyHeader(p -> p.policy("camera=(), microphone=(), geolocation=()"))).build();
    }
}

