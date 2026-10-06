package com.xinghuo.security;

import com.xinghuo.service.AdminUserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;

/**
 * 安全配置。
 *
 * <p>相对基线版本只有一处行为变更，见 {@link #chain} 中的 exceptionHandling：
 * 未认证访问 /api/** 时返回 401 而不是 302 跳到登录页。
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
     * {@code @RequireAccount} 注解负责鉴权（已逐接口核对覆盖完整）。
     */
    private static final String[] PERMIT_ALL = {
            "/", "/services", "/service/**", "/game/**", "/guides", "/guides/**", "/mastery", "/quote",
            "/records", "/record/**", "/order", "/order/**", "/contact", "/support", "/support/**", "/customer-service/**",
            "/account", "/account/login", "/account/register", "/account/reset",
            "/account/settings", "/account/submit", "/console", "/console/**",
            "/rules", "/privacy", "/trust",
            "/robots.txt", "/sitemap.xml", "/favicon.ico", "/favicon.svg",
            "/apple-touch-icon.png", "/og.png", "/assets/**",
            "/api/site-config", "/api/site-assets/**", "/api/public/**",
            "/api/services", "/api/catalog", "/api/quote", "/api/records/**",
            "/api/orders/**", "/api/uploads/quote", "/api/track",
            "/api/tool-station", "/api/guides", "/api/guides/**", "/api/account/**", "/api/console/**",
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
                        // 必须排在 permitAll 之前：requestMatchers 先匹配先赢，
                        // 放到后面就会被上面的通配规则吃掉，等于没写。
                        .requestMatchers(PERMIT_ALL).permitAll()
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
                // 未认证访问接口时返回 401，而不是 302 跳到登录页。
                //
                // 原因：/api/admin/** 的调用方是 fetch。默认的
                // LoginUrlAuthenticationEntryPoint 会 302 到 /admin/login，
                // fetch 自动跟随跳转后拿到的是 HTML 登录页且状态码为 200，
                // 于是 admin.js 里 response.ok 为真、JSON 解析失败被 catch 吞掉，
                // 最终返回 null —— 调用方对 null 取属性抛 TypeError，
                // 运营看到的是莫名其妙的报错，而不是「登录已失效」。
                // 返回 401 后 admin.js 的既有分支即可正确跳转登录页。
                .exceptionHandling(e -> e.defaultAuthenticationEntryPointFor(
                        new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                        PathPatternRequestMatcher.withDefaults().matcher("/api/**")))
                // 公开写接口没有 Spring CSRF token，使用自定义头与服务端限流做防护。
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
}
