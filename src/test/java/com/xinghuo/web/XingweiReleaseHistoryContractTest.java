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
        assertTrue(controller.contains("static final String CONSOLE_VERSION = \"20261016-v6a\""));
        assertTrue(controller.contains("static final String CONSOLE_STAGE = \"V6A · UNIFIED DELIVERY\""));
        assertTrue(controller.contains("static final String BUSINESS_VERSION = \"20261016-v6a\""));
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
        assertTrue(controller.contains("\"V3Z\", \"已上线\""));
        assertTrue(controller.contains("\"V4A\", \"已上线\""));
        assertTrue(controller.contains("\"V4B\", \"已上线\""));
        assertTrue(controller.contains("\"V4C\", \"已上线\""));
        assertTrue(controller.contains("\"V4D\", \"已上线\""));
        assertTrue(controller.contains("\"V4E\", \"已上线\""));
        assertTrue(controller.contains("\"V4F\", \"已上线\""));
        assertTrue(controller.contains("\"V4G\", \"已上线\""));
        assertTrue(controller.contains("\"V4H\", \"已上线\""));
        assertTrue(controller.contains("\"V4I\", \"已上线\""));
        assertTrue(controller.contains("\"V4J\", \"已上线\""));
        assertTrue(controller.contains("\"V4K\", \"已上线\""));
        assertTrue(controller.contains("\"V4L\", \"已上线\""));
        assertTrue(controller.contains("\"V4M\", \"已上线\""));
        assertTrue(controller.contains("售后待跟进等待时长与时效标记"));
        assertTrue(controller.contains("售后待跟进优先级排序"));
        assertTrue(controller.contains("售后待跟进时效快捷队列"));
        assertTrue(controller.contains("售后待跟进时效分布"));
        assertTrue(controller.contains("售后问题类型分布与快捷筛选"));
        assertTrue(controller.contains("售后问题记录详情查看"));
        assertTrue(controller.contains("售后问题记录按类型筛选"));
        assertTrue(controller.contains("售后问题记录全范围检索与分页"));
        assertTrue(controller.contains("售后问题记录与客户订单回显"));
        assertTrue(controller.contains("异常订单、客户售后说明与服务大厅恢复体验"));
        assertTrue(controller.contains("\"V3M\", \"已上线\""));
        assertTrue(controller.contains("Frontend Next 与后台韧性统一交付"));
        assertTrue(controller.contains("后台运行版本自校验"));
        assertTrue(controller.contains("版本漂移告警"));
        assertTrue(controller.contains("PR #488"));
        assertTrue(controller.contains("PR #489"));
        assertTrue(controller.contains("Deploy Production #213"));
        assertTrue(controller.contains("472 tests / 0 failures / 0 errors / 88 skipped"));
        assertTrue(controller.contains("10f62885752346a929efe7ec8c248ad03fcafb31a77335ddda2575d0ae6411ef"));
        assertTrue(controller.contains("PR #486"));
        assertTrue(controller.contains("Deploy Production #212"));
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
        assertTrue(template.contains(">20261016-v6a</span>"));
        assertTrue(template.contains(">V6A</strong>"));
    }

    @Test
    void dashboardStatusMatchesReleaseTruthAndPendingPayment() throws Exception {
        String dashboard = read("resources/templates/admin-xingwei.html");
        String afterSales = read("resources/templates/admin-xingwei-after-sales.html");
        String attention = read("resources/templates/admin-xingwei-after-sales-attention.html");
        assertTrue(dashboard.contains("href=\"/admin/xingwei/releases\">更新记录</a>"));
        assertTrue(dashboard.contains("th:text=\"${consoleStage}\""));
        assertTrue(dashboard.contains("V6A · UNIFIED DELIVERY"));
        assertTrue(dashboard.contains("新版后台统一工作台"));
        assertTrue(dashboard.contains("<strong>V4J</strong>"));
        assertTrue(dashboard.contains("<strong>V4K</strong>"));
        assertTrue(dashboard.contains("<strong>V4L</strong>"));
        assertTrue(dashboard.contains("<strong>V4M</strong>"));
        assertTrue(dashboard.contains("20261016-v6a"));
        assertTrue(read("resources/templates/admin-xingwei-business.html").contains("th:text=\"${businessVersion}\">20261016-v6a"));
        assertTrue(afterSales.contains("V4M · STABLE PRODUCTION"));
        assertTrue(afterSales.contains("admin-xingwei-after-sales.js?v=20261016v4k"));
        assertTrue(afterSales.contains("admin-xingwei-after-sales-v4k-links.js?v=20261016v4k"));
        assertTrue(attention.contains("V4M · STABLE PRODUCTION"));
        assertTrue(attention.contains("admin-xingwei-after-sales-attention.js?v=20261016v4m"));
        assertTrue(attention.contains("等待时长"));
        assertTrue(dashboard.contains("/assets/admin-xingwei.js?v=20261016v6a1"));
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
        assertTrue(dashboard.contains("<strong>V3Z</strong>"));
        assertTrue(dashboard.contains("客户订单售后说明回显"));
        assertTrue(dashboard.contains("实际部署状态以发布门禁为准"));
        assertTrue(dashboard.contains("已上线：企业微信与微信支付集成配置检查"));
        assertTrue(dashboard.contains("生产流水线已启用"));
        assertTrue(dashboard.contains("<strong>V3L</strong>"));
        assertTrue(dashboard.contains("已上线：Frontend Next A1–A4"));
        assertTrue(dashboard.contains("V4M 已上线 →"));
        assertTrue(dashboard.contains("/admin/xingwei/payments"));
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
