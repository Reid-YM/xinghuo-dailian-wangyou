package com.xinghuo.siteconfig.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class SiteConfigRepositoryOwnershipContractTest {
    @Test
    void siteConfigPersistenceLivesInsideSiteConfigInfrastructure() throws IOException {
        Path main = Path.of("src/main/java/com/xinghuo");
        assertThat(Files.exists(main.resolve("siteconfig/infrastructure/SiteConfig.java"))).isTrue();
        assertThat(Files.exists(main.resolve("siteconfig/infrastructure/SiteConfigRepository.java"))).isTrue();
        assertThat(Files.exists(main.resolve("model/SiteConfig.java"))).isFalse();
        assertThat(Files.exists(main.resolve("repository/SiteConfigRepository.java"))).isFalse();

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
                            .doesNotContain("com.xinghuo.model.SiteConfig")
                            .doesNotContain("com.xinghuo.repository.SiteConfigRepository"));
        }
    }
}
