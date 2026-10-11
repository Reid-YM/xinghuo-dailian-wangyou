package com.xinghuo.dashboard.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class DashboardWebContractOwnershipTest {
    private static final Path ROOT = Path.of(System.getProperty("user.dir"));

    @Test
    void dashboardResponseIsOwnedByDashboardWeb() throws IOException {
        Path main = ROOT.resolve("src/main/java/com/xinghuo");
        String response = Files.readString(main.resolve("dashboard/web/DashboardResponse.java"));
        String controller = Files.readString(main.resolve("dashboard/web/AdminDashboardController.java"));
        String mapper = Files.readString(main.resolve("dashboard/web/DashboardWebMapper.java"));

        assertTrue(response.contains("long totalServices"));
        assertTrue(response.contains("long newQuotes"));
        assertTrue(response.contains("long inProgressOrders"));
        assertTrue(response.contains("long completedOrders"));
        assertTrue(response.contains("long trackingEvents"));
        assertTrue(response.contains("long newFeedback"));
        assertFalse(controller.contains("ApiDtos.DashboardResponse"));
        assertFalse(mapper.contains("ApiDtos.DashboardResponse"));
    }
}
