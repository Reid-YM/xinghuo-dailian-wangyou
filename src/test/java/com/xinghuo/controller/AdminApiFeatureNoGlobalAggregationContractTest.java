package com.xinghuo.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class AdminApiFeatureNoGlobalAggregationContractTest {
    @Test
    void controllerPackageDoesNotReintroduceAdminApiAggregator() throws Exception {
        try (Stream<Path> files = Files.list(Path.of("src/main/java/com/xinghuo/controller"))) {
            assertThat(files
                    .filter(path -> path.getFileName().toString().endsWith("Controller.java"))
                    .map(path -> path.getFileName().toString())
                    .toList()).doesNotContain("AdminApiController.java");
        }
    }
}
