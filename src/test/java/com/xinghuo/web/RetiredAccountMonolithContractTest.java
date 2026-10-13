package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class RetiredAccountMonolithContractTest {
    private static final Path MAIN = Path.of("src/main");
    private static final Path ASSETS = MAIN.resolve("resources/static/assets");

    @Test
    void legacyAccountMonolithStaysDeleted() {
        assertFalse(
                Files.exists(ASSETS.resolve("account.js")),
                "account.js has been replaced by account-core/auth/center/submit/settings runtimes and must not be restored");
    }

    @Test
    void noRuntimeSurfaceReferencesRetiredAccountMonolith() throws IOException {
        try (Stream<Path> files = Files.walk(MAIN)) {
            files.filter(Files::isRegularFile)
                    .filter(path -> {
                        String name = path.getFileName().toString();
                        return name.endsWith(".html") || name.endsWith(".js");
                    })
                    .forEach(path -> {
                        try {
                            String content = Files.readString(path);
                            assertFalse(
                                    content.contains("/assets/account.js"),
                                    "runtime surface still references retired account.js: " + path);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
        }
    }
}
