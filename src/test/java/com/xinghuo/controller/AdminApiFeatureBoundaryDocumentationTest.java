package com.xinghuo.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class AdminApiFeatureBoundaryDocumentationTest {
    @Test
    void retirementDocumentationExists() {
        assertThat(Files.exists(Path.of("docs/admin-api-feature-boundary.md"))).isTrue();
    }
}
