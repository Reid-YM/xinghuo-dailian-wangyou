package com.xinghuo.security;

import com.xinghuo.service.AdminUserService;
import java.util.LinkedHashMap;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.RequestMatcherDelegatingAuthenticationEntryPoint;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

/**
 * 安全配置。
 *
 * <p>未认证访问 /api/** 时返回 401；未认证访问后台网页时明确跳转
 * /admin/login，避免默认入口选择因 Accept 头不同而把 /admin 当成 API 返回 401。
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
     */
    private static final String[] PERMIT_ALL = {
            "/", "/services", "/service/**", "/game/**", "/guides", "/guides/**", "/mastery", "/quote",
            "/records", "/record/**", "/order", "/order/**", "/contact",
            "/account", "/account/login", "/account/register", "/account/reset",
            "/account/settings", "/account/submit",
            "/account/requests", "/account/orders", "/account/progress",
            "/account/quotes", "/account/demands", "/account/order", "/account/order-list",
            "/account/tools", "/account/tool-progress", "/account/profile", "/account/contact",
            "/account/security", "/account/devices", "/account/activity",
            "/account/settings/profile", "/account/settings/contact", "/account/settings/security",
            "/console", "/console/**",
            "/rules", "/privacy", "/trust", "/loadouts",
            "/robots.txt", "/sitemap.xml", "/favicon.ico", "/favicon.svg",
            "/apple-touch-icon.png", "/og.png", "/assets/**",
            "/api/site-config", "/api/site-assets/**", "/api/public/**",
            "/api/services", "/api/catalog", "/api/quote", "/api/records/**",
            "/api/orders/**", "/api/uploads/quote", "/api/track",
            "/api/tool-station", "/api/guides", "/api/guides/**", "/api/loadouts", "/api/account/**", "/api/console/**",
            "/admin/login", "/error"
    };

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    SecurityFilterChain chain(HttpSecurity http, AdminUserService users) throws Exception {
        return http
                .userDetailsService((UserDetailsService) users)
                .authorizeHttpRequests(a -> a
                        .requestMatchers(PERMIT_ALL).permitAll()
                        .requestMatchers("/admin")
                        .hasAnyRole("ADMIN", "OPERATOR")
                        .requestMatchers("/admin/**", "/api/admin/**")
                        .hasAnyRole("ADMIN", "OPERATOR")
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
                .exceptionHandling(e -> e.authenticationEntryPoint(authenticationEntryPoint()))
                .csrf(c -> c.ignoringRequestMatchers(
                        "/api/quote", "/api/uploads/quote", "/api/track", "/api/account/**", "/api/console/**"))
                .headers(h -> h
                        .cacheControl(c -> c.disable())
                        .contentSecurityPolicy(c -> c.policyDirectives(
                                "default-src 'self'; img-src 'self' data: https:; "
                                        + "style-src 'self' 'unsafe-inline'; script-src 'self'; "
                                        + "connect-src 'self'; object-src 'none'; "
                                        + "frame-ancestors 'none'; base-uri 'self'; form-action 'self'"))
                        .referrerPolicy(r -> r.policy(
                                ReferrerPolicyHeaderWriter.ReferrerPolicy.SAME_ORIGIN))
                        .permissionsPolicyHeader(p -> p.policy(
                                "camera=(), microphone=(), geolocation=()")))
                .build();
    }

    static AuthenticationEntryPoint authenticationEntryPoint() {
        LinkedHashMap<RequestMatcher, AuthenticationEntryPoint> entryPoints = new LinkedHashMap<>();
        entryPoints.put(
                PathPatternRequestMatcher.withDefaults().matcher("/api/**"),
                new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED));

        RequestMatcherDelegatingAuthenticationEntryPoint delegating =
                new RequestMatcherDelegatingAuthenticationEntryPoint(entryPoints);
        delegating.setDefaultEntryPoint(new LoginUrlAuthenticationEntryPoint("/admin/login"));
        return delegating;
    }
}
