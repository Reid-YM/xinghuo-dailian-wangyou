package com.xinghuo.taxonomy.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class PlatformCatalogPersistenceOwnershipContractTest {
    @Test
    void platformCatalogPersistenceLivesInsideTaxonomyInfrastructure() throws IOException {
        assertThat(Files.exists(Path.of("src/main/java/com/xinghuo/taxonomy/infrastructure/PlatformCatalog.java"))).isTrue();
        assertThat(Files.exists(Path.of("src/main/java/com/xinghuo/taxonomy/infrastructure/PlatformCatalogRepository.java"))).isTrue();
        assertThat(Files.exists(Path.of("src/main/java/com/xinghuo/model/PlatformCatalog.java"))).isFalse();
        assertThat(Files.exists(Path.of("src/main/java/com/xinghuo/repository/PlatformCatalogRepository.java"))).isFalse();

        try (Stream<Path> files = Files.walk(Path.of("src/main/java/com/xinghuo/taxonomy"))) {
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
                            .doesNotContain("com.xinghuo.model.PlatformCatalog")
                            .doesNotContain("com.xinghuo.repository.PlatformCatalogRepository"));
        }
    }
}
