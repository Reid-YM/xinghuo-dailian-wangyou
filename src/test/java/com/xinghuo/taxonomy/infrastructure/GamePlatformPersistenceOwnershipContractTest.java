package com.xinghuo.taxonomy.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class GamePlatformPersistenceOwnershipContractTest {
    @Test
    void gamePlatformPersistenceLivesInsideTaxonomyInfrastructure() throws IOException {
        assertThat(Files.exists(Path.of("src/main/java/com/xinghuo/taxonomy/infrastructure/GamePlatform.java"))).isTrue();
        assertThat(Files.exists(Path.of("src/main/java/com/xinghuo/taxonomy/infrastructure/GamePlatformRepository.java"))).isTrue();
        assertThat(Files.exists(Path.of("src/main/java/com/xinghuo/model/GamePlatform.java"))).isFalse();
        assertThat(Files.exists(Path.of("src/main/java/com/xinghuo/repository/GamePlatformRepository.java"))).isFalse();

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
                            .doesNotContain("com.xinghuo.model.GamePlatform")
                            .doesNotContain("com.xinghuo.repository.GamePlatformRepository"));
        }
    }
}
