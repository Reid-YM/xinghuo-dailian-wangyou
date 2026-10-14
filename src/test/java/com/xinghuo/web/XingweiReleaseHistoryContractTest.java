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
        assertTrue(controller.contains("static final String CONSOLE_VERSION = \"20261014-v3y\""));
        assertTrue(controller.contains("static final String CONSOLE_STAGE = \"V3Y · STABLE PRODUCTION\""));
        assertTrue(controller.contains("static final String TAXONOMY_VERSION = \"20261014-v3i\""));
        assertTrue(controller.contains("model.addAttribute(\"consoleStage\", CONSOLE_STAGE)"));
        assertTrue(controller.contains("@GetMapping(\"/admin/xingwei/releases\")"));
        assertTrue(controller.contains("\"V3H\", \"待真机验收\""));
        assertTrue(controller.contains("\"V3I\", \"已上线\""));
        assertTrue(controller.contains("\"V3K\", \"已上线\""));
        assertTrue(controller.contains("\"V3L\", \"已上线\""));
        assertTrue(controller.contains("\"V3N\", \"已上线\""));
        assertTrue(controller.contains("\"V3O\", \"已上线\""));
        assertTrue(controller.contains("\"V3P\", \"已上线\""));
        assertTrue(controller.contains("\"V3Q\", \"已上线\""));
        assertTrue(controller.contains("\"V3R\", \"已上线\""));
        assertTrue(controller.contains("\"V3S\", \"已上线\""));
        assertTrue(controller.contains("\"V3T\", \"已上线\""));
        assertTrue(controller.contains("\"V3U\", \"已上线\""));
        assertTrue(controller.contains("\"V3V\", \"已上线\""));
        assertTrue(controller.contains("\"V3W\", \"已上线\""));
        assertTrue(controller.contains("\"V3X\", \"已上线\""));
        assertTrue(controller.contains("\"V3Y\", \"已上线\""));
        assertTrue(controller.contains("售后问题记录详情查看"));
        assertTrue(controller.contains("售后问题记录按类型筛选"));
        assertTrue(controller.contains("售后问题记录全范围检索与分页"));
        assertTrue(controller.contains("售后问题记录与客户订单回显"));
        assertTrue(controller.contains("异常订单、客户售后说明与服务大厅恢复体验"));
        assertTrue(controller.contains("\"V3M\", \"已上线\""));
        assertTrue(controller.contains("Frontend Next 与后台韧性统一交付"));
        assertTrue(controller.contains("后台运行版本自校验"));
        assertTrue(controller.contains("版本漂移告警"));
        assertTrue(controller.contains("result.put(\"version\", CONSOLE_VERSION)"));
        assertTrue(controller.contains("result.put(\"stage\", CONSOLE_STAGE)"));
        assertFalse(controller.contains("\"V3I\", \"开发完成\""));
    }

    @Test
    void releasePageDocumentsCiAndRuntimeDoubleVerification() throws Exception {
        String template = read("resources/templates/admin-xingwei-releases.html");
        assertTrue(template.contains("版本与更新记录"));
        assertTrue(template.contains("后续固定规则 · CI + 运行时双校验"));
        assertTrue(template.contains("浏览器核对页面版本与实际后端版本"));
        assertTrue(template.contains("th:each=\"item : ${releaseHistory}\""));
        assertTrue(template.contains(">20261014-v3y</span>"));
        assertTrue(template.contains(">V3Y</strong>"));
    }

    @Test
    void dashboardStatusMatchesProductionTruthAndPendingPayment() throws Exception {
        String dashboard = read("resources/templates/admin-xingwei.html");
        assertTrue(dashboard.contains("href=\"/admin/xingwei/releases\">更新记录</a>"));
        assertTrue(dashboard.contains("th:text=\"${consoleStage}\""));
        assertTrue(dashboard.contains("V3Y · STABLE PRODUCTION"));
        assertTrue(dashboard.contains("V3X 已上线"));
        assertTrue(dashboard.contains("20261014-v3y"));
        assertTrue(dashboard.contains("/assets/admin-xingwei.js?v=20261014v3x"));
        assertTrue(dashboard.contains("V3H 待真机验收"));
        assertTrue(dashboard.contains("V3I 已上线"));
        assertTrue(dashboard.contains("impactToken"));
        assertTrue(dashboard.contains("<strong>V3N</strong>"));
        assertTrue(dashboard.contains("<strong>V3Q</strong>"));
        assertTrue(dashboard.contains("<strong>V3R</strong>"));
        assertTrue(dashboard.contains("<strong>V3S</strong>"));
        assertTrue(dashboard.contains("<strong>V3T</strong>"));
        assertTrue(dashboard.contains("<strong>V3U</strong>"));
        assertTrue(dashboard.contains("<strong>V3V</strong>"));
        assertTrue(dashboard.contains("<strong>V3W</strong>"));
        assertTrue(dashboard.contains("<strong>V3X</strong>"));
        assertTrue(dashboard.contains("<strong>V3Y</strong>"));
        assertTrue(dashboard.contains("客户订单售后说明回显"));
        assertTrue(dashboard.contains("服务大厅支持读取失败重试"));
        assertTrue(dashboard.contains("已上线：企业微信与微信支付集成配置检查"));
        assertTrue(dashboard.contains("生产流水线已启用"));
        assertTrue(dashboard.contains("<strong>V3L</strong>"));
        assertTrue(dashboard.contains("<strong>V3N</strong>"));
        assertTrue(dashboard.contains("已上线：Frontend Next A1–A4"));
        assertTrue(dashboard.contains("/admin/xingwei/integrations"));
        assertFalse(dashboard.contains("<article class=\"xw-module xw-module-planned\" data-search=\"微信支付"));
        assertFalse(dashboard.contains("<article class=\"xw-module xw-module-planned\" data-search=\"发布 部署"));
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
        assertTrue(validator.contains("release candidate stage"));
        assertTrue(validator.contains("V3H payment is implemented/pending validation"));
    }

    @Test
    void runtimeHealthScriptComparesRenderedAndBackendTruth() throws Exception {
        String js = read("resources/static/assets/admin-xingwei.js");
        assertTrue(js.contains("declaredVersion"));
        assertTrue(js.contains("declaredStage"));
        assertTrue(js.contains("runtimeVersion"));
        assertTrue(js.contains("runtimeStage"));
        assertTrue(js.contains("versionMatches && stageMatches ? 'match' : 'mismatch'"));
        assertTrue(js.contains("版本漂移 · 请刷新并检查发布"));
        assertTrue(js.contains("cache: 'no-store'"));
        assertTrue(js.contains("xw:runtime-health"));
    }
}
