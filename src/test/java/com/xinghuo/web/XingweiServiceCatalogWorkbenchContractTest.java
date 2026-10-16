package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class XingweiServiceCatalogWorkbenchContractTest {
    private static final Path MAIN = Path.of("src/main");

    @Test
    void v3dReusesExistingCatalogServicesAndKeepsTaxonomyStructuralWritesControlled() throws Exception {
        String controller = read("java/com/xinghuo/controller/XingweiServiceCatalogController.java");
        String js = read("resources/static/assets/admin-xingwei-catalog.js");
        assertTrue(controller.contains("services.adminList()"));
        assertTrue(controller.contains("taxonomy.catalog(true)"));
        assertTrue(controller.contains("@GetMapping(\"/snapshot\")"));
        assertTrue(controller.contains("'service:view'"));
        assertTrue(controller.contains("@PostMapping(\"/services\")"));
        assertTrue(controller.contains("@PutMapping(\"/services/{id}\")"));
        assertTrue(controller.contains("@DeleteMapping(\"/services/{id}\")"));
        assertTrue(controller.contains("@PostMapping(\"/services/{id}/restore\")"));
        assertTrue(controller.contains("@GetMapping(\"/services/{id}/publish-review\")"));
        assertTrue(controller.split("'service:manage'", -1).length >= 5);
        assertTrue(controller.contains("services.create(draft"));
        assertTrue(controller.contains("services.update(id,safe"));
        assertTrue(controller.contains("revisions.change(Kind.SERVICE"));
        assertTrue(controller.contains("services.delete(id"));
        assertTrue(controller.contains("publishReview.requireReady(id)"));
        assertTrue(controller.contains("services.restore(id"));
        assertFalse(controller.contains("@PatchMapping"));
        assertTrue(controller.contains("taxonomy.addGame("));
        assertTrue(controller.contains("taxonomy.addCategory("));
        assertTrue(controller.contains("taxonomy.updateGame("));
        assertTrue(controller.contains("taxonomy.updateCategory("));
        assertFalse(controller.contains("taxonomy.delete"));
        assertTrue(js.contains("/api/admin/xingwei/catalog/snapshot"));
        assertTrue(js.contains("/api/admin/xingwei/access"));
        assertTrue(js.contains("/publish-review"));
        assertTrue(js.contains("/restore`,'POST'"));
        assertTrue(js.contains("/api/admin/xingwei/catalog/services','POST'"));
        assertTrue(js.contains("status:'OFFLINE'"));
        assertTrue(js.contains("active:false"));
        assertTrue(js.contains("category.active&&category.gameCode===payload.gameCode"));
        assertTrue(js.contains("service:manage"));
        assertTrue(js.contains("meta[name=\"_csrf\"]"));
        assertTrue(js.contains("meta[name=\"_csrf_header\"]"));
        assertFalse(js.contains("innerHTML"));
        assertFalse(js.contains("insertAdjacentHTML"));
    }

    @Test
    void safeCreateEditorAndReleasePreflightRemainPresentAfterVersionSync() throws Exception {
        String pageController = read("java/com/xinghuo/controller/XingweiAdminController.java");
        String template = read("resources/templates/admin-xingwei-catalog.html");
        String shell = read("resources/templates/admin-xingwei.html");
        assertTrue(pageController.contains("@GetMapping(\"/admin/xingwei/catalog\")"));
        assertTrue(pageController.contains("static final String CATALOG_VERSION = \"20261014-v3f\""));
        assertTrue(pageController.contains("model.addAttribute(\"catalogVersion\", CATALOG_VERSION)"));
        assertTrue(template.contains("href=\"/admin?legacy=1\""));
        assertTrue(template.contains("V3D · 发布检查"));
        assertTrue(template.contains("href=\"/admin/xingwei/taxonomy\""));
        assertTrue(template.contains("id=\"xcatCreateOpen\""));
        assertTrue(template.contains("id=\"xcatCreateDialog\""));
        assertTrue(template.contains("已下线并停用状态创建"));
        assertTrue(template.contains("id=\"xcatEditDialog\""));
        assertTrue(template.contains("id=\"xcatReviewDialog\""));
        assertTrue(template.contains("id=\"xcatReviewPublish\""));
        assertTrue(template.contains("阻断项必须全部通过才允许上架"));
        assertTrue(template.contains("新增原因"));
        assertTrue(template.contains("记录服务创建审计"));
        assertTrue(shell.contains("href=\"/admin/xingwei/catalog\""));
        assertTrue(shell.contains("B03"));
    }

    @Test
    void publishReviewStillHasNoSecondApprovalStateWhileTaxonomyUsesVisibilityConfirm() throws Exception {
        String review = read("java/com/xinghuo/service/XingweiServicePublishReviewService.java");
        String controller = read("java/com/xinghuo/controller/XingweiServiceCatalogController.java");
        assertTrue(review.contains("taxonomy.publicAllowed"));
        assertTrue(review.contains("COVER_RECOMMENDED"));
        assertTrue(review.contains("WARNING"));
        assertTrue(review.contains("BLOCKER"));
        assertTrue(review.contains("NOT_EXPIRED"));
        assertTrue(review.contains("PRICE_VALID"));
        assertTrue(review.contains("throw new BadRequestException(\"上架前检查未通过：\""));
        assertFalse(review.contains("Repository approval"));
        assertFalse(review.contains("save("));
        assertTrue(controller.contains("SERVICE_MANAGE_WITH_PUBLISH_PREFLIGHT_AND_TAXONOMY_VISIBILITY_CONFIRM"));
        assertTrue(controller.contains("publishReview.requireReady(id)"));
    }

    @Test
    void responsiveTablesDialogsAndChecksStayContained() throws Exception {
        String css = read("resources/static/assets/admin-xingwei-catalog.css");
        String template = read("resources/templates/admin-xingwei-catalog.html");
        assertTrue(css.contains("@media(max-width:680px)"));
        assertTrue(css.contains(".xcat-service-table{min-width:1180px}"));
        assertTrue(css.contains(".xcat-dialog{width:min(760px,calc(100vw - 32px))"));
        assertTrue(css.contains(".xcat-head-actions"));
        assertTrue(css.contains(".xcat-review-summary"));
        assertTrue(css.contains(".xcat-check.blocker"));
        assertTrue(template.contains("xw-table-wrap"));
    }

    private static String read(String relative) throws Exception {
        return Files.readString(MAIN.resolve(relative));
    }
}
