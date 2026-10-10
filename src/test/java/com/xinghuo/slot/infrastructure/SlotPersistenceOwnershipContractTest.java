package com.xinghuo.slot.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class SlotPersistenceOwnershipContractTest {
    @Test
    void hotSlotPersistenceLivesInsideSlotInfrastructure() throws IOException {
        assertThat(Files.exists(Path.of("src/main/java/com/xinghuo/slot/infrastructure/HotSlot.java"))).isTrue();
        assertThat(Files.exists(Path.of("src/main/java/com/xinghuo/slot/infrastructure/HotSlotRepository.java"))).isTrue();
        assertThat(Files.exists(Path.of("src/main/java/com/xinghuo/model/HotSlot.java"))).isFalse();
        assertThat(Files.exists(Path.of("src/main/java/com/xinghuo/repository/HotSlotRepository.java"))).isFalse();

        try (Stream<Path> files = Files.walk(Path.of("src/main/java/com/xinghuo/slot"))) {
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
                            .doesNotContain("com.xinghuo.model.HotSlot")
                            .doesNotContain("com.xinghuo.repository.HotSlotRepository"));
        }
    }
}
