package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class XingweiTaxonomyManagementContractTest {
    private String read(String path) throws Exception {
        return Files.readString(Path.of("src/main", path));
    }

    @Test void v3fEndpointsRequireManageAndPreserveActiveState() throws Exception {
        String source = read("java/com/xinghuo/controller/XingweiServiceCatalogController.java");
        assertTrue(source.contains("@PostMapping(\"/taxonomy/games\")"));
        assertTrue(source.contains("@PostMapping(\"/taxonomy/categories\")"));
        assertTrue(source.contains("@PutMapping(\"/taxonomy/games/{code}/profile\")"));
        assertTrue(source.contains("@PutMapping(\"/taxonomy/categories/{id}/profile\")"));
        assertTrue(source.contains("new ApiDtos.CatalogNameRequest("));
        assertTrue(source.contains("new ApiDtos.CategoryCreateRequest("));
        assertTrue(source.contains("null, request.reason()"));
        assertTrue(source.contains("false, request.reason()"));
        assertTrue(source.contains("'service:manage'"));
        assertFalse(source.contains("/taxonomy/games/{code}/delete"));
        assertFalse(source.contains("/taxonomy/categories/{id}/delete"));
    }

    @Test void v3fEditorRequiresCsrfAndAvoidsHtmlInjection() throws Exception {
        String js = read("resources/static/assets/admin-xingwei-taxonomy.js");
        String html = read("resources/templates/admin-xingwei-taxonomy.html");
        assertTrue(js.contains("function openEditor("));
        assertTrue(js.contains("async function saveEditor("));
        assertTrue(js.contains("headers[csrfHeader]=csrfToken"));
        assertTrue(js.contains("if(!canManage||!editTarget)return"));
        assertFalse(js.contains("innerHTML"));
        assertTrue(html.contains("id=\"xtaxEditDialog\""));
        assertTrue(html.contains("id=\"xtaxEditReason\""));
        assertTrue(html.contains("id=\"xtaxAddGame\""));
        assertTrue(html.contains("id=\"xtaxAddCategory\""));
    }
}
