package com.xinghuo.gamestatus.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class GameStatusPersistenceOwnershipContractTest {
    @Test
    void gameStatusPersistenceLivesInsideFeatureInfrastructure() throws IOException {
        assertThat(Files.exists(Path.of("src/main/java/com/xinghuo/gamestatus/infrastructure/GameStatus.java"))).isTrue();
        assertThat(Files.exists(Path.of("src/main/java/com/xinghuo/gamestatus/infrastructure/GameStatusRepository.java"))).isTrue();
        assertThat(Files.exists(Path.of("src/main/java/com/xinghuo/model/GameStatus.java"))).isFalse();
        assertThat(Files.exists(Path.of("src/main/java/com/xinghuo/repository/GameStatusRepository.java"))).isFalse();

        try (Stream<Path> files = Files.walk(Path.of("src/main/java/com/xinghuo/gamestatus"))) {
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
                            .doesNotContain("com.xinghuo.model.GameStatus")
                            .doesNotContain("com.xinghuo.repository.GameStatusRepository"));
        }
    }
}
