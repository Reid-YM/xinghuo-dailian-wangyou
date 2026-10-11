package com.xinghuo.security.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class AdminUserPersistenceOwnershipContractTest {
    @Test
    void adminUserPersistenceLivesInsideSecurityInfrastructure() throws IOException {
        assertThat(Files.exists(Path.of("src/main/java/com/xinghuo/security/infrastructure/AdminUser.java"))).isTrue();
        assertThat(Files.exists(Path.of("src/main/java/com/xinghuo/security/infrastructure/AdminUserRepository.java"))).isTrue();
        assertThat(Files.exists(Path.of("src/main/java/com/xinghuo/model/AdminUser.java"))).isFalse();
        assertThat(Files.exists(Path.of("src/main/java/com/xinghuo/repository/AdminUserRepository.java"))).isFalse();

        try (Stream<Path> files = Files.walk(Path.of("src/main/java/com/xinghuo/security"))) {
            assertThat(files
                    .filter(path -> path.toString().endsWith(".java"))
                    .map(path -> {
                        try {
                            return Files.readString(path);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .toList())
                    .allSatisfy(source -> assertThat(source)
                            .doesNotContain("com.xinghuo.model.AdminUser")
                            .doesNotContain("com.xinghuo.repository.AdminUserRepository"));
        }
    }
}
