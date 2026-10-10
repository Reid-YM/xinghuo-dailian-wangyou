package com.xinghuo.tracking.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class TrackingPersistenceOwnershipContractTest {
    @Test
    void trackingPersistenceLivesInsideTrackingInfrastructure() throws IOException {
        assertThat(Files.exists(Path.of("src/main/java/com/xinghuo/tracking/infrastructure/TrackingEvent.java"))).isTrue();
        assertThat(Files.exists(Path.of("src/main/java/com/xinghuo/tracking/infrastructure/TrackingEventRepository.java"))).isTrue();
        assertThat(Files.exists(Path.of("src/main/java/com/xinghuo/model/TrackingEvent.java"))).isFalse();
        assertThat(Files.exists(Path.of("src/main/java/com/xinghuo/repository/TrackingEventRepository.java"))).isFalse();

        try (Stream<Path> files = Files.walk(Path.of("src/main/java/com/xinghuo/tracking"))) {
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
                            .doesNotContain("com.xinghuo.model.TrackingEvent")
                            .doesNotContain("com.xinghuo.repository.TrackingEventRepository"));
        }

        String dashboard = Files.readString(Path.of("src/main/java/com/xinghuo/dashboard/infrastructure/DashboardPersistenceAdapter.java"));
        assertThat(dashboard)
                .contains("TrackingApplicationService")
                .contains("tracking.totalCount()")
                .doesNotContain("TrackingEventRepository");
    }
}
