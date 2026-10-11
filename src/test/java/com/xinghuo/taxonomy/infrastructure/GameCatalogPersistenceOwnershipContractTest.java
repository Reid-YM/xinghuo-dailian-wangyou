package com.xinghuo.taxonomy.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class GameCatalogPersistenceOwnershipContractTest {
    @Test
    void gameCatalogPersistenceLivesInsideTaxonomyInfrastructure() throws IOException {
        assertThat(Files.exists(Path.of("src/main/java/com/xinghuo/taxonomy/infrastructure/GameCatalog.java"))).isTrue();
        assertThat(Files.exists(Path.of("src/main/java/com/xinghuo/taxonomy/infrastructure/GameCatalogRepository.java"))).isTrue();
        assertThat(Files.exists(Path.of("src/main/java/com/xinghuo/model/GameCatalog.java"))).isFalse();
        assertThat(Files.exists(Path.of("src/main/java/com/xinghuo/repository/GameCatalogRepository.java"))).isFalse();

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
                            .doesNotContain("com.xinghuo.model.GameCatalog")
                            .doesNotContain("com.xinghuo.repository.GameCatalogRepository"));
        }
    }
}
