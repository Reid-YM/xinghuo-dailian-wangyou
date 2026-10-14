package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class XingweiBusinessScopeContractTest {
    private static final Path ROOT = Path.of("src/main");

    @Test
    void v64IsAdditiveAndDoesNotRewriteCoreBusinessTables() throws Exception {
        String migration = read("resources/db/migration/V64__xingwei_business_ownership.sql");
        assertTrue(migration.contains("CREATE TABLE xw_business_ownership"));
        assertTrue(migration.contains("PRIMARY KEY (business_no)"));
        assertTrue(migration.contains("REFERENCES admin_users (id) ON DELETE SET NULL"));
        assertTrue(migration.contains("REFERENCES xw_departments (id) ON DELETE RESTRICT"));
        assertFalse(migration.contains("ALTER TABLE quotes"));
        assertFalse(migration.contains("ALTER TABLE customer_orders"));
        assertFalse(migration.contains("ALTER TABLE admin_users"));
    }

    @Test
    void v2dUsesNewXingweiEndpointsAndKeepsLegacyBusinessApisUntouched() throws Exception {
        String controller = read("java/com/xinghuo/controller/XingweiBusinessScopeController.java");
        String service = read("java/com/xinghuo/service/XingweiBusinessScopeService.java");
        String legacy = read("java/com/xinghuo/controller/AdminApiController.java");

        assertTrue(controller.contains("@RequestMapping(\"/api/admin/xingwei/business\")"));
        assertTrue(controller.contains("'quote:view'"));
        assertTrue(controller.contains("'quote:manage'"));
        assertTrue(controller.contains("'order:view'"));
        assertTrue(controller.contains("'order:assign'"));
        assertTrue(service.contains("LEFT JOIN xw_business_ownership"));
        assertTrue(service.contains("DEPARTMENT"));
        assertTrue(service.contains("SELF"));
        assertTrue(service.contains("当前业务未归属到你的数据范围"));
        assertTrue(service.contains("XW_BUSINESS_OWNERSHIP"));

        assertTrue(legacy.contains("@GetMapping(value={\"/quotes\"})"));
        assertTrue(legacy.contains("@GetMapping(value={\"/orders\"})"));
        assertFalse(legacy.contains("XingweiBusinessScopeService"),
                "V2D must not tighten legacy /api/admin business endpoints yet");
    }

    @Test
    void exceptionQueueFiltersBeforeLimitAndRequiresOrderView() throws Exception {
        String controller = read("java/com/xinghuo/controller/XingweiBusinessScopeController.java");
        String service = read("java/com/xinghuo/service/XingweiBusinessScopeService.java");

        assertTrue(controller.contains("@GetMapping(\"/orders/exceptions\")\n    @PreAuthorize(\"@xingweiPermissionGuard.has(authentication, 'order:view')\")"));
        assertTrue(service.contains("case EXCEPTIONS -> \" AND o2.status = 'EXCEPTION'\""));
        assertTrue(service.contains("WHERE %s\n                ORDER BY o2.updated_at DESC\n                LIMIT %d"));
        assertTrue(service.contains("ScopeClause scope = scopeClause(access)"));
    }

    @Test
    void afterSalesRecordListRequiresOrderViewAndFiltersInsideScope() throws Exception {
        String controller = read("java/com/xinghuo/controller/XingweiBusinessScopeController.java");
        String service = read("java/com/xinghuo/service/XingweiBusinessScopeService.java");

        assertTrue(controller.contains("@GetMapping(\"/orders/after-sales\")\n    @PreAuthorize(\"@xingweiPermissionGuard.has(authentication, 'order:view')\")"));
        assertTrue(service.contains("case AFTER_SALES -> \" AND LOCATE('[售后问题 ·', COALESCE(o2.customer_note, '')) > 0\""));
    }

    private static String read(String relative) throws Exception {
        return Files.readString(ROOT.resolve(relative));
    }
}
