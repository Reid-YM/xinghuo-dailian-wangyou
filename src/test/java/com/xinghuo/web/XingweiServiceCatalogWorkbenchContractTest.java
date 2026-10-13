package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class XingweiServiceCatalogWorkbenchContractTest {
    private static final Path MAIN = Path.of("src/main");

    @Test
    void v3aUsesExistingCatalogServicesAndIsStrictlyReadOnly() throws Exception {
        String controller = read("java/com/xinghuo/controller/XingweiServiceCatalogController.java");
        String js = read("resources/static/assets/admin-xingwei-catalog.js");
        assertTrue(controller.contains("services.adminList()"));
        assertTrue(controller.contains("taxonomy.catalog(true)"));
        assertTrue(controller.contains("'service:view'"));
        assertTrue(controller.contains("@GetMapping(\"/snapshot\")"));
        assertFalse(controller.contains("@PostMapping"));
        assertFalse(controller.contains("@PutMapping"));
        assertFalse(controller.contains("@PatchMapping"));
        assertFalse(controller.contains("@DeleteMapping"));
        assertTrue(js.contains("/api/admin/xingwei/catalog/snapshot"));
        assertFalse(js.contains("innerHTML"));
        assertFalse(js.contains("insertAdjacentHTML"));
    }

    @Test
    void v3aHasDedicatedPageRecoveryLinkAndConsoleEntry() throws Exception {
        String pageController = read("java/com/xinghuo/controller/XingweiAdminController.java");
        String template = read("resources/templates/admin-xingwei-catalog.html");
        String shell = read("resources/templates/admin-xingwei.html");
        assertTrue(pageController.contains("@GetMapping(\"/admin/xingwei/catalog\")"));
        assertTrue(pageController.contains("return \"admin-xingwei-catalog\""));
        assertTrue(template.contains("href=\"/admin\">旧后台编辑</a>"));
        assertTrue(template.contains("V3A · READ ONLY MIGRATION"));
        assertTrue(shell.contains("href=\"/admin/xingwei/catalog\""));
        assertTrue(shell.contains("B03"));
    }

    @Test
    void v3aResponsiveTableIsContainedAndAddsNoMigration() throws Exception {
        String css = read("resources/static/assets/admin-xingwei-catalog.css");
        String template = read("resources/templates/admin-xingwei-catalog.html");
        assertTrue(css.contains("@media(max-width:680px)"));
        assertTrue(css.contains(".xcat-service-table{min-width:1040px}"));
        assertTrue(template.contains("xw-table-wrap"));
    }

    private static String read(String relative) throws Exception {
        return Files.readString(MAIN.resolve(relative));
    }
}
