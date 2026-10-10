package com.xinghuo.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class AdminApiRetirementContractTest {
    @Test
    void globalAdminApiControllerIsRetired() {
        assertThat(Files.exists(Path.of("src/main/java/com/xinghuo/controller/AdminApiController.java"))).isFalse();
    }
}
