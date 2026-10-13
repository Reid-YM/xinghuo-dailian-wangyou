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
    void currentVersionsStageAndReleaseHistoryAreCentralized() throws Exception {
        String controller = read("java/com/xinghuo/controller/XingweiAdminController.java");
        assertTrue(controller.contains("static final String CONSOLE_VERSION = \"20261014-v3j\""));
        assertTrue(controller.contains("static final String CONSOLE_STAGE = \"V3J · STABLE PRODUCTION\""));
        assertTrue(controller.contains("static final String TAXONOMY_VERSION = \"20261014-v3i\""));
        assertTrue(controller.contains("model.addAttribute(\"consoleStage\", CONSOLE_STAGE)"));
        assertTrue(controller.contains("@GetMapping(\"/admin/xingwei/releases\")"));
        assertTrue(controller.contains("\"V3G\", \"已上线\""));
        assertTrue(controller.contains("\"V3H\", \"待真机验收\""));
        assertTrue(controller.contains("\"V3I\", \"已上线\""));
        assertTrue(controller.contains("\"V3J\", \"已上线\""));
        assertTrue(controller.contains("影响快照 token"));
        assertTrue(controller.contains("零停机生产发布流程上线"));
        assertTrue(controller.contains("result.put(\"version\", CONSOLE_VERSION)"));
        assertTrue(controller.contains("result.put(\"stage\", CONSOLE_STAGE)"));
        assertFalse(controller.contains("\"V3I\", \"开发完成\""));
    }

    @Test
    void releasePageDocumentsTheUpdateRuleAndCurrentFallback() throws Exception {
        String template = read("resources/templates/admin-xingwei-releases.html");
        assertTrue(template.contains("版本与更新记录"));
        assertTrue(template.contains("后续固定规则"));
        assertTrue(template.contains("未同步则不算阶段收口"));
        assertTrue(template.contains("th:each=\"item : ${releaseHistory}\""));
        assertTrue(template.contains(">20261014-v3j</span>"));
    }

    @Test
    void dashboardStatusMatchesStableProductionTruthAndPendingPayment() throws Exception {
        String dashboard = read("resources/templates/admin-xingwei.html");
        assertTrue(dashboard.contains("href=\"/admin/xingwei/releases\">更新记录</a>"));
        assertTrue(dashboard.contains("th:text=\"${consoleStage}\""));
        assertTrue(dashboard.contains("V3J · STABLE PRODUCTION"));
        assertTrue(dashboard.contains("20261014-v3j"));
        assertTrue(dashboard.contains("V3G 已上线"));
        assertTrue(dashboard.contains("V3H 待真机验收"));
        assertTrue(dashboard.contains("V3I 已上线"));
        assertTrue(dashboard.contains("impactToken"));
        assertTrue(dashboard.contains("生产流水线已启用"));
        assertFalse(dashboard.contains("V3I · RELEASE CANDIDATE"));
        assertFalse(dashboard.contains("作为本次发布候选"));
        assertFalse(dashboard.contains("<article class=\"xw-module xw-module-planned\" data-search=\"微信支付"));
        assertFalse(dashboard.contains("<article class=\"xw-module xw-module-planned\" data-search=\"发布 部署"));
        assertFalse(dashboard.contains("<span>V4 整合</span>"));
        assertFalse(dashboard.contains("PHASE 2C · DATA SCOPE ENFORCEMENT"));
    }
}
