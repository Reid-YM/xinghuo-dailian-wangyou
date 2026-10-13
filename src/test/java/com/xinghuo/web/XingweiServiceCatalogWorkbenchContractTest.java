package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class XingweiServiceCatalogWorkbenchContractTest {
    private static final Path MAIN = Path.of("src/main");

    @Test
    void v3dReusesExistingCatalogServicesAndKeepsTaxonomyReadOnly() throws Exception {
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
        assertTrue(controller.contains("services.create(request"));
        assertTrue(controller.contains("services.update(id, request"));
        assertTrue(controller.contains("services.delete(id"));
        assertTrue(controller.contains("publishReview.requireReady(id)"));
        assertTrue(controller.contains("services.restore(id"));
        assertFalse(controller.contains("@PatchMapping"));
        assertFalse(controller.contains("taxonomy.add"));
        assertFalse(controller.contains("taxonomy.update"));
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
    void v3dHasSafeCreateControlledEditorAndReleasePreflight() throws Exception {
        String pageController = read("java/com/xinghuo/controller/XingweiAdminController.java");
        String template = read("resources/templates/admin-xingwei-catalog.html");
        String shell = read("resources/templates/admin-xingwei.html");
        assertTrue(pageController.contains("@GetMapping(\"/admin/xingwei/catalog\")"));
        assertTrue(pageController.contains("catalogVersion\", \"20261013-v3d\""));
        assertTrue(template.contains("href=\"/admin\">旧后台完整编辑</a>"));
        assertTrue(template.contains("V3D · RELEASE PREFLIGHT"));
        assertTrue(template.contains("id=\"xcatCreateOpen\""));
        assertTrue(template.contains("id=\"xcatCreateDialog\""));
        assertTrue(template.contains("OFFLINE + 停用状态创建"));
        assertTrue(template.contains("id=\"xcatEditDialog\""));
        assertTrue(template.contains("id=\"xcatReviewDialog\""));
        assertTrue(template.contains("id=\"xcatReviewPublish\""));
        assertTrue(template.contains("阻断项必须全部通过才允许上架"));
        assertTrue(template.contains("新增原因"));
        assertTrue(template.contains("SERVICE_CREATE"));
        assertTrue(shell.contains("href=\"/admin/xingwei/catalog\""));
        assertTrue(shell.contains("B03"));
    }

    @Test
    void v3dPublishReviewHasNoSecondApprovalStateOrMigrationDependency() throws Exception {
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
        assertTrue(controller.contains("EXISTING_SERVICE_CREATE_UPDATE_WITH_PUBLISH_PREFLIGHT"));
    }

    @Test
    void v3dResponsiveTablesDialogsAndChecksStayContained() throws Exception {
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
