package com.xinghuo.catalog.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class ServiceItemRepositoryOwnershipContractTest {
    @Test
    void serviceItemRepositoryLivesInsideCatalogInfrastructure() throws IOException {
        assertThat(Files.exists(Path.of("src/main/java/com/xinghuo/catalog/infrastructure/ServiceItemRepository.java"))).isTrue();
        assertThat(Files.exists(Path.of("src/main/java/com/xinghuo/repository/ServiceItemRepository.java"))).isFalse();
        assertThat(Files.exists(Path.of("src/main/java/com/xinghuo/model/ServiceItem.java"))).isTrue();

        try (Stream<Path> files = Files.walk(Path.of("src/main/java"))) {
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
                            .doesNotContain("com.xinghuo.repository.ServiceItemRepository"));
        }
    }
}
