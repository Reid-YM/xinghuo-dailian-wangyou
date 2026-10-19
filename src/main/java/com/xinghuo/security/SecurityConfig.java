package com.xinghuo.security;

import com.xinghuo.service.AdminUserService;
import com.xinghuo.wecom.XingweiWeComBindingService;
import com.xinghuo.wecom.XingweiWeComProperties;
import com.xinghuo.wecom.XingweiWeComSessionFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;

/**
 * 安全配置。
 *
 * <p>未认证访问 /api/** 时返回 401 而不是 302 跳到登录页。
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    /**
     * 免登录可访问的路径。
     *
     * <p>注意 {@code /api/account/**} 在这里是 permitAll：账号模块的登录态不是
     * Spring Security 的 SecurityContext，而是自建会话表 + Cookie，
     * 由 {@code com.xinghuo.account.web.AccountAuthInterceptor} 与
     * {@code @RequireAccount} 注解负责鉴权。用户中心页面同样必须先通过这里，
     * 才能由拦截器把未登录访问统一 302 到账号登录页；这里的 permitAll
     * 并不代表业务页面免登录。
     *
     * <p>{@code /api/guest/**} 与 {@code /guest/access/**} 同理：它们不使用 Spring
     * 登录态，而是由服务端生成的高熵 capability 授权。浏览器业务 Cookie 为
     * host-only / HttpOnly / SameSite=Lax，数据库仅保存 SHA-256 哈希；业务接口仍会
     * 逐条校验 guest session 与 business access，因此 permitAll 只表示“不要求后台账号登录”。
     */
    private static final String[] PERMIT_ALL = {
            "/", "/services", "/service/**", "/game/**", "/guides", "/guides/**", "/mastery", "/quote",
            "/records", "/record/**", "/order", "/order/**", "/contact", "/language",
            "/guest/access/**",
            "/account", "/account/login", "/account/register", "/account/reset",
            "/account/settings", "/account/submit",
            "/account/requests", "/account/orders", "/account/progress",
            "/account/requests/*", "/account/orders/*", "/account/favorites", "/account/submissions",
            "/account/quotes", "/account/demands", "/account/order", "/account/order-list",
            "/account/tools", "/account/tool-progress", "/account/profile", "/account/contact",
            "/account/security", "/account/devices", "/account/activity",
            "/account/settings/profile", "/account/settings/contact", "/account/settings/security",
            "/console", "/console/**",
            "/next", "/next/**",
            "/rules", "/rules-en", "/privacy", "/privacy-en", "/trust", "/loadouts",
            "/robots.txt", "/sitemap.xml", "/favicon.ico", "/favicon.svg",
            "/apple-touch-icon.png", "/og.png", "/assets/**",
            "/api/site-config", "/api/site-assets/**", "/api/public/**",
            "/api/services", "/api/catalog", "/api/quote", "/api/records/**",
            "/api/orders/**", "/api/uploads/quote", "/api/track",
            "/api/tool-station", "/api/guides", "/api/guides/**", "/api/loadouts",
            "/api/account/**", "/api/console/**", "/api/guest/**",
            "/api/wechat-pay/notify",
            "/admin/login", "/admin/xingwei/wecom/login", "/admin/xingwei/wecom/callback",
            "/admin/xingwei/wecom/pending", "/error"
    };

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    SecurityFilterChain chain(HttpSecurity http, AdminUserService users,
            XingweiWeComBindingService bindings, XingweiWeComProperties weComProperties) throws Exception {
        return http
                .addFilterBefore(new XingweiWeComSessionFilter(bindings, weComProperties), AuthorizationFilter.class)
                .addFilterBefore(new AdminSessionStatusFilter(users), AuthorizationFilter.class)
                .userDetailsService((UserDetailsService) users)
                .authorizeHttpRequests(a -> a
                        .requestMatchers("/assets/admin.js").denyAll()
                        .requestMatchers(PERMIT_ALL).permitAll()
                        .requestMatchers("/admin", "/admin/guides", "/admin/frontend-controls", "/admin/xingwei", "/admin/xingwei/**", "/api/admin/xingwei", "/api/admin/xingwei/**")
                        .hasAnyRole("ADMIN", "OPERATOR")
                        // Retired pages and APIs stay closed for password and WeCom sessions alike.
                        .requestMatchers("/admin/**", "/api/admin", "/api/admin/**")
                        .denyAll()
                        .anyRequest().denyAll())
                .formLogin(f -> f
                        .loginPage("/admin/login")
                        .loginProcessingUrl("/login")
                        .defaultSuccessUrl("/admin", true)
                        .failureUrl("/admin/login?error")
                        .permitAll())
                .logout(l -> l
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/admin/login?logout")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID"))
                .exceptionHandling(e -> e.defaultAuthenticationEntryPointFor(
                        new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                        PathPatternRequestMatcher.withDefaults().matcher("/api/**")))
                .csrf(c -> c.ignoringRequestMatchers(
                        "/api/quote", "/api/uploads/quote", "/api/track", "/api/account/**", "/api/console/**",
                        "/api/guest/**", "/api/wechat-pay/notify"))
                .headers(h -> h
                        .cacheControl(c -> c.disable())
                        .contentSecurityPolicy(c -> c.policyDirectives(
                                "default-src 'self'; img-src 'self' data: https:; "
                                        + "style-src 'self' 'unsafe-inline'; "
                                        + "script-src 'self' 'sha256-Ua8FBICmLZGVLagGDb98azul9qj4MC8YNXHvKEOHidg='; "
                                        + "connect-src 'self'; object-src 'none'; "
                                        + "frame-ancestors 'none'; base-uri 'self'; form-action 'self'"))
                        .referrerPolicy(r -> r.policy(
                                ReferrerPolicyHeaderWriter.ReferrerPolicy.SAME_ORIGIN))
                        .permissionsPolicyHeader(p -> p.policy(
                                "camera=(), microphone=(), geolocation=()")))
                .build();
    }
}
