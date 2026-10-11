package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class GlobalDtoRetirementContractTest {
    private static final Path ROOT = Path.of(System.getProperty("user.dir"));

    @Test
    void globalApiDtosStayRetiredAndProductionCodeDoesNotReferenceThem() throws Exception {
        Path retired = ROOT.resolve("src/main/java/com/xinghuo/dto/ApiDtos.java");
        assertFalse(Files.exists(retired));

        try (Stream<Path> files = Files.walk(ROOT.resolve("src/main/java"))) {
            for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                String source = Files.readString(file);
                assertFalse(source.contains("import com.xinghuo.dto.ApiDtos;"),
                        file + " still imports retired ApiDtos");
                assertFalse(source.contains("ApiDtos."),
                        file + " still references retired ApiDtos members");
            }
        }
    }
}
