package com.xinghuo.upload.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class UploadAssetRepositoryOwnershipContractTest {
    @Test
    void uploadAssetRepositoryLivesInsideUploadInfrastructure() throws IOException {
        assertThat(Files.exists(Path.of("src/main/java/com/xinghuo/upload/infrastructure/UploadAssetRepository.java"))).isTrue();
        assertThat(Files.exists(Path.of("src/main/java/com/xinghuo/repository/UploadAssetRepository.java"))).isFalse();
        assertThat(Files.exists(Path.of("src/main/java/com/xinghuo/model/UploadAsset.java"))).isTrue();

        try (Stream<Path> files = Files.walk(Path.of("src/main/java/com/xinghuo/upload"))) {
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
                            .doesNotContain("com.xinghuo.repository.UploadAssetRepository"));
        }
    }
}
