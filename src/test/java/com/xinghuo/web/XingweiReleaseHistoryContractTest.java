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
        assertTrue(controller.contains("static final String CONSOLE_VERSION = \"20261014-v3k\""));
        assertTrue(controller.contains("static final String CONSOLE_STAGE = \"V3K · STABLE PRODUCTION\""));
        assertTrue(controller.contains("static final String TAXONOMY_VERSION = \"20261014-v3i\""));
        assertTrue(controller.contains("model.addAttribute(\"consoleStage\", CONSOLE_STAGE)"));
        assertTrue(controller.contains("@GetMapping(\"/admin/xingwei/releases\")"));
        assertTrue(controller.contains("\"V3G\", \"已上线\""));
        assertTrue(controller.contains("\"V3H\", \"待真机验收\""));
        assertTrue(controller.contains("\"V3I\", \"已上线\""));
        assertTrue(controller.contains("\"V3J\", \"已上线\""));
        assertTrue(controller.contains("\"V3K\", \"已上线\""));
        assertTrue(controller.contains("版本与更新记录自动巡检"));
        assertTrue(controller.contains("遗漏同步都会阻断 PR/主线 CI"));
        assertTrue(controller.contains("result.put(\"version\", CONSOLE_VERSION)"));
        assertTrue(controller.contains("result.put(\"stage\", CONSOLE_STAGE)"));
        assertFalse(controller.contains("\"V3I\", \"开发完成\""));
    }

    @Test
    void releasePageDocumentsTheCiEnforcedUpdateRuleAndCurrentFallback() throws Exception {
        String template = read("resources/templates/admin-xingwei-releases.html");
        assertTrue(template.contains("版本与更新记录"));
        assertTrue(template.contains("后续固定规则 · CI 强制"));
        assertTrue(template.contains("未同步会由 CI 直接阻断"));
        assertTrue(template.contains("th:each=\"item : ${releaseHistory}\""));
        assertTrue(template.contains(">20261014-v3k</span>"));
        assertTrue(template.contains(">V3K</strong>"));
    }

    @Test
    void dashboardStatusMatchesStableProductionTruthAndPendingPayment() throws Exception {
        String dashboard = read("resources/templates/admin-xingwei.html");
        assertTrue(dashboard.contains("href=\"/admin/xingwei/releases\">更新记录</a>"));
        assertTrue(dashboard.contains("th:text=\"${consoleStage}\""));
        assertTrue(dashboard.contains("V3K · STABLE PRODUCTION"));
        assertTrue(dashboard.contains("20261014-v3k"));
        assertTrue(dashboard.contains("V3G 已上线"));
        assertTrue(dashboard.contains("V3H 待真机验收"));
        assertTrue(dashboard.contains("V3I 已上线"));
        assertTrue(dashboard.contains("impactToken"));
        assertTrue(dashboard.contains("版本记录同步已从人工约定升级为 CI 强制门禁"));
        assertTrue(dashboard.contains("生产流水线已启用"));
        assertTrue(dashboard.contains("<strong>V3K</strong>"));
        assertFalse(dashboard.contains("RELEASE CANDIDATE"));
        assertFalse(dashboard.contains("作为本次发布候选"));
        assertFalse(dashboard.contains("<article class=\"xw-module xw-module-planned\" data-search=\"微信支付"));
        assertFalse(dashboard.contains("<article class=\"xw-module xw-module-planned\" data-search=\"发布 部署"));
        assertFalse(dashboard.contains("<span>V4 整合</span>"));
        assertFalse(dashboard.contains("PHASE 2C · DATA SCOPE ENFORCEMENT"));
    }

    @Test
    void ciRunsTheDynamicReleaseStateValidator() throws Exception {
        String workflow = Files.readString(Path.of(".github/workflows/ci.yml"));
        String validator = Files.readString(Path.of("scripts/validate-xingwei-release-state.sh"));
        assertTrue(workflow.contains("Validate Xingwei release state"));
        assertTrue(workflow.contains("bash scripts/validate-xingwei-release-state.sh"));
        assertTrue(validator.contains("extract_constant CONSOLE_VERSION"));
        assertTrue(validator.contains("version/stage mismatch"));
        assertTrue(validator.contains("stable production stage"));
        assertTrue(validator.contains("V3H payment is implemented/pending validation"));
    }
}
