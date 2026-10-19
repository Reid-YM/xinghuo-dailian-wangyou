package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class XingweiWorkWorkbenchContractTest {
    private static final Path ROOT = Path.of("src/main");

    @Test
    void v2fWorkApisSeparateViewManageAndAssignPermissions() throws Exception {
        String controller = read("java/com/xinghuo/controller/XingweiWorkController.java");
        String service = read("java/com/xinghuo/service/XingweiWorkService.java");

        assertTrue(controller.contains("@RequestMapping(\"/api/admin/xingwei/work\")"));
        assertTrue(controller.contains("'order:view'"));
        assertTrue(controller.contains("'order:manage'"));
        assertTrue(controller.contains("'order:assign'"));
        assertTrue(controller.contains("@PutMapping(\"/orders/{businessNo}/claim\")"));
        assertTrue(controller.contains("@PatchMapping(\"/orders/{businessNo}\")"));

        assertTrue(service.contains("XINGWEI_V2F_WORKBENCH"));
        assertTrue(service.contains("own.owner_admin_user_id = ? OR (own.owner_admin_user_id IS NULL AND d.code = ?)"));
        assertTrue(service.contains("员工工作台只允许修改本人已领取任务"));
        assertTrue(service.contains("businessScope.canAccessBusiness(authentication, normalized)"));
        assertTrue(service.contains("orders.update(orderNo, update, operator(authentication), request)"));
        assertTrue(service.contains("businessScope.assignOrder("));
        assertTrue(service.contains("lockOrder(normalized)"));
        assertTrue(service.contains("ownershipForUpdate(normalized)"));
        assertTrue(service.contains("private Ownership ownershipForUpdate"));
        assertTrue(service.contains("FOR UPDATE"), "claim must serialize both the order row and current ownership read");
    }

    @Test
    void v2fPageUsesCsrfScopedApisAndSafeDomRendering() throws Exception {
        String adminController = read("java/com/xinghuo/controller/XingweiAdminController.java");
        String template = read("resources/templates/admin-xingwei-work.html");
        String js = read("resources/static/assets/admin-xingwei-work.js");
        String business = read("resources/templates/admin-xingwei-business.html");

        assertTrue(adminController.contains("@GetMapping(\"/admin/xingwei/work\")"));
        assertTrue(adminController.contains("return \"admin-xingwei-work\""));
        assertTrue(template.contains("meta name=\"_csrf\""));
        assertTrue(template.contains("admin-xingwei-work.js?v=20261019cutover"));
        assertTrue(template.contains("WAITING_CONFIRMATION"));
        assertTrue(template.contains("IN_PROGRESS"));
        assertTrue(template.contains("COMPLETED"));
        assertTrue(template.contains("EXCEPTION"));
        assertTrue(js.contains("/api/admin/xingwei/work"));
        assertTrue(js.contains("/claim`"));
        assertTrue(js.contains("method: 'PATCH'"));
        assertTrue(js.contains("headers[csrfHeader] = csrfToken"));
        assertTrue(js.contains("textContent"));
        assertFalse(js.contains(".innerHTML"));
        assertFalse(js.contains("insertAdjacentHTML"));
        assertTrue(business.contains("href=\"/admin/xingwei/work\">我的工作</a>"));
    }

    @Test
    void mobileLayoutKeepsWideTaskTableInsideScrollContainer() throws Exception {
        String template = read("resources/templates/admin-xingwei-work.html");
        String css = read("resources/static/assets/admin-xingwei-work.css");

        assertTrue(template.contains("xw-table-wrap"));
        assertTrue(css.contains(".xww-table{min-width:1100px}"));
        assertTrue(css.contains("@media(max-width:680px)"));
        assertTrue(css.contains("flex-wrap:wrap"));
    }

    private static String read(String relative) throws Exception {
        return Files.readString(ROOT.resolve(relative));
    }
}
