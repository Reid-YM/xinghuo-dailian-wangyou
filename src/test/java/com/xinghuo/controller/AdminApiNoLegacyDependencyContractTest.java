package com.xinghuo.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class AdminApiNoLegacyDependencyContractTest {
    @Test
    void productionSourcesDoNotReferenceRetiredAdminApiController() throws IOException {
        try (Stream<Path> files = Files.walk(Path.of("src/main/java"))) {
            assertThat(files
                    .filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> {
                        try {
                            return Files.readString(path).contains("AdminApiController");
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .toList()).isEmpty();
        }
    }
}
