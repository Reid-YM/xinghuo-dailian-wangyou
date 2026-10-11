package com.xinghuo.upload.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class UploadAssetRepositoryOwnershipContractTest {
    @Test
    void uploadAssetPersistenceLivesInsideUploadInfrastructure() throws IOException {
        Path main = Path.of("src/main/java/com/xinghuo");
        assertThat(Files.exists(main.resolve("upload/infrastructure/UploadAsset.java"))).isTrue();
        assertThat(Files.exists(main.resolve("upload/infrastructure/UploadAssetRepository.java"))).isTrue();
        assertThat(Files.exists(main.resolve("model/UploadAsset.java"))).isFalse();
        assertThat(Files.exists(main.resolve("repository/UploadAssetRepository.java"))).isFalse();

        try (Stream<Path> files = Files.walk(main)) {
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
                            .doesNotContain("com.xinghuo.model.UploadAsset")
                            .doesNotContain("com.xinghuo.repository.UploadAssetRepository"));
        }
    }
}
