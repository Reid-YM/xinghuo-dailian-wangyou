package com.xinghuo.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class AdminApiFeatureAuditBoundaryContractTest {
    @Test
    void auditWebDoesNotDependDirectlyOnRepositoryPackage() throws Exception {
        String controller = Files.readString(Path.of("src/main/java/com/xinghuo/audit/web/AdminAuditController.java"));
        assertThat(controller).doesNotContain("com.xinghuo.repository");
    }
}
