package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class PackageByFeatureBoundaryContractTest {
    private static final Path ROOT = Path.of(System.getProperty("user.dir"));

    @Test
    void retiredTopLevelHorizontalBusinessPackagesStayAbsent() {
        Path root = ROOT.resolve("src/main/java/com/xinghuo");
        for (String retired : List.of("service", "repository", "model", "dto", "web", "controller")) {
            assertFalse(Files.exists(root.resolve(retired)),
                    "top-level horizontal package must stay retired: com.xinghuo." + retired);
        }
    }
}
