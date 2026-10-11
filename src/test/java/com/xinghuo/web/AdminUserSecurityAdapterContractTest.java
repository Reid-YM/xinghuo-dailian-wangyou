package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class AdminUserSecurityAdapterContractTest {
    private static final Path ROOT = Path.of(System.getProperty("user.dir"));

    private static String read(String relative) throws IOException {
        return Files.readString(ROOT.resolve(relative));
    }

    @Test
    void adminUserDetailsLivesInSecurityInfrastructure() throws IOException {
        String adapter = read("src/main/java/com/xinghuo/security/infrastructure/AdminUserDetailsService.java");
        assertTrue(adapter.contains("implements UserDetailsService"));
        assertTrue(adapter.contains("private final AdminUserRepository repo;"));
        assertTrue(adapter.contains("AdminUser user = repo.findByUsername(username)"));
        assertFalse(adapter.contains("com.xinghuo.repository.AdminUserRepository"));
        assertFalse(adapter.contains("com.xinghuo.model.AdminUser"));
        assertTrue(adapter.contains("roles(user.getRole().name())"));
        assertTrue(adapter.contains("disabled(!user.isEnabled())"));
    }

    @Test
    void securityConfigUsesNewAdapterWithoutChangingSecurityPolicy() throws IOException {
        String security = read("src/main/java/com/xinghuo/security/SecurityConfig.java");
        assertTrue(security.contains("com.xinghuo.security.infrastructure.AdminUserDetailsService"));
        assertFalse(security.contains("com.xinghuo.service.AdminUserService"));
        assertTrue(security.contains("new BCryptPasswordEncoder(12)"));
        assertTrue(security.contains("hasAnyRole(\"ADMIN\", \"OPERATOR\")"));
        assertTrue(security.contains(".loginProcessingUrl(\"/login\")"));
        assertTrue(security.contains(".anyRequest().denyAll()"));
    }

    @Test
    void legacyAdminUserServiceStaysRetired() throws IOException {
        assertFalse(Files.exists(ROOT.resolve("src/main/java/com/xinghuo/service/AdminUserService.java")));
        try (Stream<Path> files = Files.walk(ROOT.resolve("src/main/java"))) {
            for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                assertFalse(Files.readString(file).contains("com.xinghuo.service.AdminUserService"),
                        file + " still references retired AdminUserService");
            }
        }
    }
}
