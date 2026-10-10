package com.xinghuo.audit.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class AuditPersistenceOwnershipContractTest {
    @Test
    void auditPersistenceLivesInsideAuditInfrastructure() throws IOException {
        assertThat(Files.exists(Path.of("src/main/java/com/xinghuo/audit/infrastructure/AuditLog.java"))).isTrue();
        assertThat(Files.exists(Path.of("src/main/java/com/xinghuo/audit/infrastructure/AuditLogRepository.java"))).isTrue();
        assertThat(Files.exists(Path.of("src/main/java/com/xinghuo/model/AuditLog.java"))).isFalse();
        assertThat(Files.exists(Path.of("src/main/java/com/xinghuo/repository/AuditLogRepository.java"))).isFalse();

        try (Stream<Path> files = Files.walk(Path.of("src/main/java/com/xinghuo/audit"))) {
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
                            .doesNotContain("com.xinghuo.model.AuditLog")
                            .doesNotContain("com.xinghuo.repository.AuditLogRepository"));
        }
    }
}
