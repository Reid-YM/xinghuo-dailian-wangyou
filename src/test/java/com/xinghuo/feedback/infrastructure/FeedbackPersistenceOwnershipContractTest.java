package com.xinghuo.feedback.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class FeedbackPersistenceOwnershipContractTest {
    @Test
    void feedbackPersistenceLivesInsideFeedbackInfrastructure() throws IOException {
        assertThat(Files.exists(Path.of("src/main/java/com/xinghuo/feedback/infrastructure/AdminTestFeedback.java"))).isTrue();
        assertThat(Files.exists(Path.of("src/main/java/com/xinghuo/feedback/infrastructure/AdminTestFeedbackRepository.java"))).isTrue();
        assertThat(Files.exists(Path.of("src/main/java/com/xinghuo/model/AdminTestFeedback.java"))).isFalse();
        assertThat(Files.exists(Path.of("src/main/java/com/xinghuo/repository/AdminTestFeedbackRepository.java"))).isFalse();

        try (Stream<Path> files = Files.walk(Path.of("src/main/java/com/xinghuo/feedback"))) {
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
                            .doesNotContain("com.xinghuo.model.AdminTestFeedback")
                            .doesNotContain("com.xinghuo.repository.AdminTestFeedbackRepository"));
        }

        String dashboard = Files.readString(Path.of("src/main/java/com/xinghuo/dashboard/infrastructure/DashboardPersistenceAdapter.java"));
        assertThat(dashboard)
                .contains("FeedbackApplicationService")
                .contains("feedback.countNew()")
                .doesNotContain("AdminTestFeedbackRepository")
                .doesNotContain("com.xinghuo.model.AdminTestFeedback");
    }
}
