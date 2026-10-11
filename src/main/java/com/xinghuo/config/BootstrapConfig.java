package com.xinghuo.config;

import com.xinghuo.security.infrastructure.AdminUser;
import com.xinghuo.security.infrastructure.AdminUserRepository;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class BootstrapConfig {
    private static final Logger log = LoggerFactory.getLogger(BootstrapConfig.class);

    @Bean
    CommandLineRunner bootstrapAdmin(AdminUserRepository repo, PasswordEncoder encoder, @Value(value="${app.bootstrap-admin.username}") String user, @Value(value="${app.bootstrap-admin.password}") String pass) {
        return args -> {
            if (repo.count() != 0L) {
                return;
            }
            if (pass == null || pass.isBlank() || "ChangeMe123!".equals(pass) || "CHANGE_ME_ADMIN_PASSWORD".equals(pass)) {
                throw new IllegalStateException("\u9996\u6b21\u542f\u52a8\u524d\u5fc5\u987b\u901a\u8fc7 ADMIN_PASSWORD \u8bbe\u7f6e\u5f3a\u540e\u53f0\u5bc6\u7801");
            }
            Instant now = Instant.now();
            repo.save(AdminUser.builder().username(user).passwordHash(encoder.encode((CharSequence)pass)).role(AdminUser.Role.ADMIN).enabled(true).createdAt(now).updatedAt(now).build());
            log.info("Bootstrap admin account created: {}", user);
        };
    }
}
