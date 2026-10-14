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
        assertTrue(controller.contains("static final String CONSOLE_VERSION = \"20261014-v3n\""));
        assertTrue(controller.contains("static final String CONSOLE_STAGE = \"V3N · RELEASE CANDIDATE\""));
        assertTrue(controller.contains("static final String TAXONOMY_VERSION = \"20261014-v3i\""));
        assertTrue(controller.contains("model.addAttribute(\"consoleStage\", CONSOLE_STAGE)"));
        assertTrue(controller.contains("@GetMapping(\"/admin/xingwei/releases\")"));
        assertTrue(controller.contains("\"V3H\", \"待真机验收\""));
        assertTrue(controller.contains("\"V3I\", \"已上线\""));
        assertTrue(controller.contains("\"V3K\", \"已上线\""));
        assertTrue(controller.contains("\"V3L\", \"已上线\""));
        assertTrue(controller.contains("\"V3N\", \"待发布\""));
        assertTrue(controller.contains("\"V3M\", \"并入 V3N 待发布\""));
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
        assertTrue(template.contains(">20261014-v3n</span>"));
        assertTrue(template.contains(">V3N</strong>"));
    }

    @Test
    void dashboardStatusMatchesReleaseCandidateTruthAndPendingPayment() throws Exception {
        String dashboard = read("resources/templates/admin-xingwei.html");
        assertTrue(dashboard.contains("href=\"/admin/xingwei/releases\">更新记录</a>"));
        assertTrue(dashboard.contains("th:text=\"${consoleStage}\""));
        assertTrue(dashboard.contains("V3N · RELEASE CANDIDATE"));
        assertTrue(dashboard.contains("20261014-v3n"));
        assertTrue(dashboard.contains("/assets/admin-xingwei.js?v=20261014v3n"));
        assertTrue(dashboard.contains("V3H 待真机验收"));
        assertTrue(dashboard.contains("V3I 已上线"));
        assertTrue(dashboard.contains("impactToken"));
        assertTrue(dashboard.contains("V3N 统一交付候选"));
        assertTrue(dashboard.contains("生产流水线已启用"));
        assertTrue(dashboard.contains("<strong>V3L</strong>"));
        assertTrue(dashboard.contains("<strong>V3N</strong>"));
        assertTrue(dashboard.contains("待发布：Frontend Next A1–A4"));
        assertFalse(dashboard.contains("作为本次发布候选"));
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
