package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class XingweiServiceCatalogWorkbenchContractTest {
    private static final Path MAIN = Path.of("src/main");

    @Test
    void v3cReusesExistingCatalogServicesAndKeepsTaxonomyReadOnly() throws Exception {
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
        assertTrue(controller.split("'service:manage'", -1).length >= 5);
        assertTrue(controller.contains("services.create(request"));
        assertTrue(controller.contains("services.update(id, request"));
        assertTrue(controller.contains("services.delete(id"));
        assertTrue(controller.contains("services.restore(id"));
        assertFalse(controller.contains("@PatchMapping"));
        assertFalse(controller.contains("taxonomy.add"));
        assertFalse(controller.contains("taxonomy.update"));
        assertFalse(controller.contains("taxonomy.delete"));
        assertTrue(js.contains("/api/admin/xingwei/catalog/snapshot"));
        assertTrue(js.contains("/api/admin/xingwei/access"));
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
    void v3cHasSafeCreateAndControlledEditorWithRecoveryLink() throws Exception {
        String pageController = read("java/com/xinghuo/controller/XingweiAdminController.java");
        String template = read("resources/templates/admin-xingwei-catalog.html");
        String shell = read("resources/templates/admin-xingwei.html");
        assertTrue(pageController.contains("@GetMapping(\"/admin/xingwei/catalog\")"));
        assertTrue(pageController.contains("catalogVersion\", \"20261013-v3c\""));
        assertTrue(template.contains("href=\"/admin\">旧后台完整编辑</a>"));
        assertTrue(template.contains("V3C · SAFE SERVICE CREATE"));
        assertTrue(template.contains("id=\"xcatCreateOpen\""));
        assertTrue(template.contains("id=\"xcatCreateDialog\""));
        assertTrue(template.contains("新服务固定以 OFFLINE + 停用状态创建"));
        assertTrue(template.contains("id=\"xcatEditDialog\""));
        assertTrue(template.contains("新增原因"));
        assertTrue(template.contains("SERVICE_CREATE"));
        assertTrue(shell.contains("href=\"/admin/xingwei/catalog\""));
        assertTrue(shell.contains("B03"));
    }

    @Test
    void v3cResponsiveTablesAndDialogsStayContained() throws Exception {
        String css = read("resources/static/assets/admin-xingwei-catalog.css");
        String template = read("resources/templates/admin-xingwei-catalog.html");
        assertTrue(css.contains("@media(max-width:680px)"));
        assertTrue(css.contains(".xcat-service-table{min-width:1180px}"));
        assertTrue(css.contains(".xcat-dialog{width:min(760px,calc(100vw - 32px))"));
        assertTrue(css.contains(".xcat-head-actions"));
        assertTrue(template.contains("xw-table-wrap"));
    }

    private static String read(String relative) throws Exception {
        return Files.readString(MAIN.resolve(relative));
    }
}
