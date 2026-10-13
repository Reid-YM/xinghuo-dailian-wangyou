package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class XingweiReleaseHistoryContractTest {
    private static String read(String relative) throws Exception {
        return Files.readString(Path.of("src/main/", relative));
    }

    @Test
    void currentVersionsAndReleaseHistoryAreCentralized() throws Exception {
        String controller = read("java/com/xinghuo/controller/XingweiAdminController.java");
        assertTrue(controller.contains("static final String CONSOLE_VERSION = \"20261014-v3i\""));
        assertTrue(controller.contains("static final String TAXONOMY_VERSION = \"20261014-v3i\""));
        assertTrue(controller.contains("@GetMapping(\"/admin/xingwei/releases\")"));
        assertTrue(controller.contains("\"V3F\", \"已上线\""));
        assertTrue(controller.contains("\"V3G\", \"已上线\""));
        assertTrue(controller.contains("\"V3H\", \"待真机验收\""));
        assertTrue(controller.contains("\"V3I\", \"已上线\""));
        assertTrue(controller.contains("影响快照 token"));
        assertTrue(controller.contains("生产 18080→8081→8080 三阶段冒烟并上线"));
        assertTrue(controller.contains("result.put(\"version\", CONSOLE_VERSION)"));
    }

    @Test
    void releasePageDocumentsTheUpdateRule() throws Exception {
        String template = read("resources/templates/admin-xingwei-releases.html");
        assertTrue(template.contains("版本与更新记录"));
        assertTrue(template.contains("后续固定规则"));
        assertTrue(template.contains("未同步则不算阶段收口"));
        assertTrue(template.contains("th:each=\"item : ${releaseHistory}\""));
    }

    @Test
    void dashboardStatusMatchesV3iLiveAndOtherStages() throws Exception {
        String dashboard = read("resources/templates/admin-xingwei.html");
        assertTrue(dashboard.contains("href=\"/admin/xingwei/releases\">更新记录</a>"));
        assertTrue(dashboard.contains("V3I · CURRENT DELIVERY"));
        assertTrue(dashboard.contains("20261014-v3i"));
        assertTrue(dashboard.contains("V3G 已上线"));
        assertTrue(dashboard.contains("V3H 待真机验收"));
        assertTrue(dashboard.contains("impactToken"));
        assertTrue(dashboard.contains("V3I 新版工作台"));
        assertTrue(dashboard.contains("生产 18080、8081、8080 三阶段冒烟通过"));
        assertFalse(dashboard.contains("V3I · RELEASE CANDIDATE"));
        assertFalse(dashboard.contains("<span>V4 整合</span>"));
        assertFalse(dashboard.contains("PHASE 2C · DATA SCOPE ENFORCEMENT"));
    }
}
