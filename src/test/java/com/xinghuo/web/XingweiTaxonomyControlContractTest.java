package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class XingweiTaxonomyControlContractTest {
    private static final Path MAIN = Path.of("src/main");

    @Test
    void v3eOnlyExposesImpactAndActiveToggle() throws Exception {
        String controller = read("java/com/xinghuo/controller/XingweiServiceCatalogController.java");
        String service = read("java/com/xinghuo/service/XingweiTaxonomyControlService.java");
        assertTrue(controller.contains("/taxonomy/games/{code}/impact"));
        assertTrue(controller.contains("/taxonomy/categories/{id}/impact"));
        assertTrue(controller.contains("/taxonomy/games/{code}/active"));
        assertTrue(controller.contains("/taxonomy/categories/{id}/active"));
        assertTrue(controller.contains("'service:view'"));
        assertTrue(controller.contains("'service:manage'"));
        assertTrue(service.contains("taxonomy.updateGame"));
        assertTrue(service.contains("taxonomy.updateCategory"));
        assertTrue(service.contains("taxonomy.invalidateVisibilityCache()"));
        assertFalse(controller.contains("/taxonomy/games/{code}/delete"));
        assertFalse(controller.contains("/taxonomy/categories/{id}/delete"));
        assertFalse(controller.contains("/taxonomy/games/create"));
        assertFalse(controller.contains("/taxonomy/categories/create"));
        assertFalse(service.contains("deleteGame"));
        assertFalse(service.contains("deleteCategory"));
        assertFalse(service.contains("addGame"));
        assertFalse(service.contains("addCategory"));
    }

    @Test
    void v3eHasIndependentPageAndCatalogEntry() throws Exception {
        String pageController = read("java/com/xinghuo/controller/XingweiAdminController.java");
        String taxonomyTemplate = read("resources/templates/admin-xingwei-taxonomy.html");
        String catalogTemplate = read("resources/templates/admin-xingwei-catalog.html");
        assertTrue(pageController.contains("@GetMapping(\"/admin/xingwei/taxonomy\")"));
        assertTrue(pageController.contains("taxonomyVersion\", \"20261013-v3e\""));
        assertTrue(taxonomyTemplate.contains("V3E · IMPACT PREVIEW"));
        assertTrue(taxonomyTemplate.contains("id=\"xtaxDialog\""));
        assertTrue(taxonomyTemplate.contains("id=\"xtaxReason\""));
        assertTrue(taxonomyTemplate.contains("不会新增/删除 taxonomy"));
        assertTrue(catalogTemplate.contains("href=\"/admin/xingwei/taxonomy\""));
    }

    @Test
    void v3eFrontendRequiresPreviewReasonAndCsrfWithoutUnsafeHtml() throws Exception {
        String js = read("resources/static/assets/admin-xingwei-taxonomy.js");
        assertTrue(js.contains("/impact`"));
        assertTrue(js.contains("/active`"));
        assertTrue(js.contains("reason.value.trim()"));
        assertTrue(js.contains("method:'PUT'"));
        assertTrue(js.contains("headers[csrfHeader]=csrfToken"));
        assertTrue(js.contains("service:manage"));
        assertFalse(js.contains("innerHTML"));
        assertFalse(js.contains("insertAdjacentHTML"));
    }

    @Test
    void v3eMobileTablesAndDialogStayContained() throws Exception {
        String css = read("resources/static/assets/admin-xingwei-taxonomy.css");
        assertTrue(css.contains(".xtax-table{min-width:720px}"));
        assertTrue(css.contains(".xtax-dialog{width:min(700px,calc(100vw - 32px))"));
        assertTrue(css.contains("@media(max-width:680px)"));
    }

    private static String read(String relative) throws Exception {
        return Files.readString(MAIN.resolve(relative));
    }
}
