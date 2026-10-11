package com.xinghuo.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class AdminApiFeatureNoGlobalAggregationContractTest {
    @Test
    void retiredGlobalControllerPackageStaysAbsent() {
        assertThat(Path.of("src/main/java/com/xinghuo/controller")).doesNotExist();
    }
}
