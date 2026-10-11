package com.xinghuo.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class AdminApiNoLegacyDependencyContractTest {
    @Test
    void productionSourcesDoNotReintroduceRetiredAdminApiControllerFile() throws IOException {
        try (Stream<Path> files = Files.walk(Path.of("src/main/java"))) {
            assertThat(files
                    .filter(path -> path.getFileName() != null)
                    .filter(path -> path.getFileName().toString().equals("AdminApiController.java"))
                    .toList()).isEmpty();
        }
    }
}
