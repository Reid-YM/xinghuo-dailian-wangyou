package com.xinghuo.sitesetting.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class SiteSettingPersistenceOwnershipContractTest {
    @Test
    void siteSettingPersistenceLivesInsideFeatureInfrastructure() throws IOException {
        assertThat(Files.exists(Path.of("src/main/java/com/xinghuo/sitesetting/infrastructure/SiteSetting.java"))).isTrue();
        assertThat(Files.exists(Path.of("src/main/java/com/xinghuo/sitesetting/infrastructure/SiteSettingRepository.java"))).isTrue();
        assertThat(Files.exists(Path.of("src/main/java/com/xinghuo/model/SiteSetting.java"))).isFalse();
        assertThat(Files.exists(Path.of("src/main/java/com/xinghuo/repository/SiteSettingRepository.java"))).isFalse();

        try (Stream<Path> files = Files.walk(Path.of("src/main/java/com/xinghuo/sitesetting"))) {
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
                            .doesNotContain("com.xinghuo.model.SiteSetting")
                            .doesNotContain("com.xinghuo.repository.SiteSettingRepository"));
        }
    }
}
