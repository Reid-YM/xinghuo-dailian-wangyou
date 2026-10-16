package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class XingweiBusinessWorkbenchContractTest {
    private static final Path ROOT = Path.of("src/main");

    @Test
    void v2eWorkbenchUsesOnlyScopedBusinessApisAndCsrfProtectedOwnershipWrites() throws Exception {
        String controller = read("java/com/xinghuo/controller/XingweiAdminController.java");
        String template = read("resources/templates/admin-xingwei-business.html");
        String js = read("resources/static/assets/admin-xingwei-business.js");
        String shell = read("resources/templates/admin-xingwei.html");

        assertTrue(controller.contains("@GetMapping(\"/admin/xingwei/business\")"));
        assertTrue(controller.contains("return \"admin-xingwei-business\""));
        assertTrue(template.contains("admin-xingwei-business.js?v=20261016cn1"));
        assertTrue(template.contains("meta name=\"_csrf\""));
        assertTrue(js.contains("/api/admin/xingwei/business/${endpoint}"));
        assertTrue(js.contains("/api/admin/xingwei/business/ownership-candidates"));
        assertTrue(js.contains("/ownership`"));
        assertTrue(js.contains("headers[csrfHeader] = csrfToken"));
        assertTrue(js.contains("textContent"));
        assertFalse(js.contains(".innerHTML"), "customer-controlled values must not be assembled with innerHTML");
        assertFalse(js.contains("insertAdjacentHTML"));

        assertTrue(shell.contains("href=\"/admin/xingwei/business?tab=quotes\""));
        assertTrue(shell.contains("href=\"/admin/xingwei/business?tab=orders\""));
        assertTrue(shell.contains("新版工作台 →"));
    }

    @Test
    void selfScopeNeverOffersDepartmentQueueThatBackendWouldReject() throws Exception {
        String js = read("resources/static/assets/admin-xingwei-business.js");

        assertTrue(js.contains("access?.dataScope === 'SELF' ? '请选择本人' : '请选择负责人或中心队列'"));
        assertTrue(js.contains("if (access?.dataScope !== 'SELF')"));
    }

    @Test
    void workbenchKeepsLegacyAdminRecoveryLinkAndResponsiveTableContainment() throws Exception {
        String template = read("resources/templates/admin-xingwei-business.html");
        String css = read("resources/static/assets/admin-xingwei-business.css");

        assertTrue(template.contains("href=\"/admin?legacy=1\""));
        assertTrue(template.contains("href=\"/admin/xingwei\">返回星维后台</a>"));
        assertTrue(template.contains("xw-table-wrap"));
        assertTrue(css.contains("@media(max-width:680px)"));
        assertTrue(css.contains(".xwb-table{min-width:1080px}"));
    }

    private static String read(String relative) throws Exception {
        return Files.readString(ROOT.resolve(relative));
    }
}
