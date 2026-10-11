package com.xinghuo.order.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class CustomerOrderPersistenceOwnershipContractTest {
    @Test
    void customerOrderPersistenceLivesInsideOrderInfrastructure() throws IOException {
        assertThat(Files.exists(Path.of("src/main/java/com/xinghuo/order/infrastructure/CustomerOrder.java"))).isTrue();
        assertThat(Files.exists(Path.of("src/main/java/com/xinghuo/order/infrastructure/CustomerOrderRepository.java"))).isTrue();
        assertThat(Files.exists(Path.of("src/main/java/com/xinghuo/model/CustomerOrder.java"))).isFalse();
        assertThat(Files.exists(Path.of("src/main/java/com/xinghuo/repository/CustomerOrderRepository.java"))).isFalse();

        try (Stream<Path> files = Files.walk(Path.of("src/main/java/com/xinghuo/order"))) {
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
                            .doesNotContain("com.xinghuo.model.CustomerOrder")
                            .doesNotContain("com.xinghuo.repository.CustomerOrderRepository"));
        }
    }
}
