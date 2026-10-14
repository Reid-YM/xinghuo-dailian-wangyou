package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class XingweiTaxonomyControlContractTest {
    private static final Path MAIN = Path.of("src/main");

    @Test
    void impactAndActiveToggleRemainSeparateFromProfileManagement() throws Exception {
        String controller = read("java/com/xinghuo/controller/XingweiServiceCatalogController.java");
        String service = read("java/com/xinghuo/service/XingweiTaxonomyControlService.java");
        assertTrue(controller.contains("/taxonomy/games/{code}/impact"));
        assertTrue(controller.contains("/taxonomy/categories/{id}/impact"));
        assertTrue(controller.contains("/taxonomy/games/{code}/active"));
        assertTrue(controller.contains("/taxonomy/categories/{id}/active"));
        assertTrue(controller.contains("'service:view'"));
        assertTrue(controller.contains("'service:manage'"));
        assertTrue(controller.contains("request.impactToken()"));
        assertTrue(controller.contains("@NotBlank @Size(max = 128) String impactToken"));
        assertTrue(service.contains("requireFreshImpact(expectedImpactToken, impact)"));
        assertTrue(service.contains("taxonomy.updateGame"));
        assertTrue(service.contains("taxonomy.updateCategory"));
        assertTrue(service.contains("taxonomy.invalidateVisibilityCache()"));
        assertFalse(controller.contains("/taxonomy/games/{code}/delete"));
        assertFalse(controller.contains("/taxonomy/categories/{id}/delete"));
        assertFalse(service.contains("deleteGame"));
        assertFalse(service.contains("deleteCategory"));
        assertFalse(service.contains("addGame"));
        assertFalse(service.contains("addCategory"));
    }

    @Test
    void v3iPageExplainsVisibilityAndRequiresExplicitAcknowledgement() throws Exception {
        String pageController = read("java/com/xinghuo/controller/XingweiAdminController.java");
        String taxonomyTemplate = read("resources/templates/admin-xingwei-taxonomy.html");
        String catalogTemplate = read("resources/templates/admin-xingwei-catalog.html");
        assertTrue(pageController.contains("static final String TAXONOMY_VERSION = \"20261014-v3i\""));
        assertTrue(pageController.contains("@GetMapping(\"/admin/xingwei/taxonomy\")"));
        assertTrue(taxonomyTemplate.contains("V3I · VISIBILITY CONFIRM"));
        assertTrue(taxonomyTemplate.contains("当前前台服务"));
        assertTrue(taxonomyTemplate.contains("操作后前台服务"));
        assertTrue(taxonomyTemplate.contains("id=\"xtaxVisibilitySummary\""));
        assertTrue(taxonomyTemplate.contains("id=\"xtaxAcknowledge\""));
        assertTrue(taxonomyTemplate.contains("影响快照 token"));
        assertTrue(taxonomyTemplate.contains("href=\"/admin/xingwei/releases\""));
        assertTrue(catalogTemplate.contains("href=\"/admin/xingwei/taxonomy\""));
    }

    @Test
    void frontendRequiresPreviewReasonAcknowledgementTokenAndCsrfWithoutUnsafeHtml() throws Exception {
        String js = read("resources/static/assets/admin-xingwei-taxonomy.js");
        assertTrue(js.contains("/impact`"));
        assertTrue(js.contains("/active`"));
        assertTrue(js.contains("reason.value.trim()"));
        assertTrue(js.contains("acknowledge.checked"));
        assertTrue(js.contains("impactToken:currentImpact.impactToken"));
        assertTrue(js.contains("影响数据已变化"));
        assertTrue(js.contains("method:'PUT'"));
        assertTrue(js.contains("headers[csrfHeader]=csrfToken"));
        assertTrue(js.contains("service:manage"));
        assertFalse(js.contains("window.confirm"));
        assertFalse(js.contains("innerHTML"));
        assertFalse(js.contains("insertAdjacentHTML"));
    }

    @Test
    void mobileTablesAndVisibilityDialogStayContained() throws Exception {
        String css = read("resources/static/assets/admin-xingwei-taxonomy.css");
        String v3iCss = read("resources/static/assets/admin-xingwei-taxonomy-v3i.css");
        assertTrue(css.contains(".xtax-table{min-width:720px}"));
        assertTrue(css.contains(".xtax-dialog{width:min(700px,calc(100vw - 32px))"));
        assertTrue(css.contains("@media(max-width:680px)"));
        assertTrue(v3iCss.contains(".xtax-visibility"));
        assertTrue(v3iCss.contains(".xtax-ack"));
        assertTrue(v3iCss.contains("@media(max-width:680px)"));
    }

    private static String read(String relative) throws Exception {
        return Files.readString(MAIN.resolve(relative));
    }
}
