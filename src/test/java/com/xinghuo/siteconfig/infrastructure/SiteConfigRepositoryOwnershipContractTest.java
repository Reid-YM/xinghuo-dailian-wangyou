package com.xinghuo.siteconfig.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class SiteConfigRepositoryOwnershipContractTest {
    @Test
    void siteConfigRepositoryLivesInsideSiteConfigInfrastructure() throws IOException {
        assertThat(Files.exists(Path.of("src/main/java/com/xinghuo/siteconfig/infrastructure/SiteConfigRepository.java"))).isTrue();
        assertThat(Files.exists(Path.of("src/main/java/com/xinghuo/repository/SiteConfigRepository.java"))).isFalse();
        assertThat(Files.exists(Path.of("src/main/java/com/xinghuo/model/SiteConfig.java"))).isTrue();

        try (Stream<Path> files = Files.walk(Path.of("src/main/java/com/xinghuo/siteconfig"))) {
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
                            .doesNotContain("com.xinghuo.repository.SiteConfigRepository"));
        }
    }
}
