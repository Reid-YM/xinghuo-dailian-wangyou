package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class XingweiOrgRbacContractTest {
    private static final Path ROOT = Path.of("src/main");

    @Test
    void v63SeedsExactSevenCentersAndAll155CanonicalPositions() throws Exception {
        String migration = read("resources/db/migration/V63__xingwei_org_rbac_foundation.sql");

        for (String department : List.of(
                "01 管理层",
                "02 运营中心",
                "03 客服与订单中心",
                "04 游戏业务中心",
                "05 技术产品中心",
                "06 市场与渠道中心",
                "07 综合支持")) {
            assertTrue(migration.contains("'" + department + "'"), "missing fixed center " + department);
        }

        Matcher positionCodes = Pattern.compile("'XW-LC-\\d{2}-\\d{3}'").matcher(migration);
        int count = 0;
        while (positionCodes.find()) count++;
        assertEquals(155, count, "V63 must seed the complete canonical 155-position dictionary exactly once");

        for (String position : List.of("工作室负责人", "淘宝运营专员", "售前客服", "MW4 代练员", "微信支付开发工程师", "SEO 专员", "权限管理员")) {
            assertTrue(migration.contains("'" + position + "'"), "missing representative canonical position " + position);
        }
    }

    @Test
    void v63RemainsAdditiveAndLegacyAdminBoundaryStillExists() throws Exception {
        String migration = read("resources/db/migration/V63__xingwei_org_rbac_foundation.sql");
        String security = read("java/com/xinghuo/security/SecurityConfig.java");
        for (String table : List.of("xw_departments", "xw_positions", "xw_rbac_roles", "xw_permissions", "xw_role_permissions", "xw_admin_profiles", "xw_admin_roles")) {
            assertTrue(migration.contains("CREATE TABLE " + table), "missing additive table " + table);
        }
        assertFalse(migration.contains("ALTER TABLE admin_users"), "organization migration must not change legacy login table");
        assertFalse(migration.contains("UPDATE admin_users"), "organization migration must not rewrite legacy roles");
        assertTrue(security.contains(".requestMatchers(\"/admin/**\", \"/api/admin/**\")"));
        assertTrue(security.contains(".hasAnyRole(\"ADMIN\", \"OPERATOR\")"), "legacy ADMIN/OPERATOR remains the outer Spring Security boundary");
    }

    @Test
    void v2cOrganizationEndpointsUsePermissionDataScopeAndEscalationGuards() throws Exception {
        String controller = read("java/com/xinghuo/controller/XingweiOrgRbacController.java");
        String guard = read("java/com/xinghuo/security/XingweiPermissionGuard.java");
        assertTrue(controller.contains("@PreAuthorize(\"@xingweiPermissionGuard.hasAny(authentication, 'org:view', 'employee:view', 'rbac:view')\")"));
        assertTrue(controller.contains("@PreAuthorize(\"@xingweiPermissionGuard.has(authentication, 'employee:manage')\")"));
        assertEquals(2, occurrences(controller, "@PreAuthorize(\"@xingweiPermissionGuard.has(authentication, 'rbac:manage')\")"));
        assertFalse(controller.contains("@PreAuthorize(\"hasRole('ADMIN')\")"), "organization API must use the Xingwei guard, not UI-only filtering");
        assertTrue(controller.contains("scopedEmployees(authentication"));
        assertTrue(controller.contains("permissions.canAccessEmployee("));
        assertTrue(controller.contains("permissions.canManageEmployeeProfile("));
        assertTrue(controller.contains("permissions.canAssignEmployeeRoles("));
        assertTrue(controller.contains("permissions.canManageGlobalRbac(authentication)"));
        assertTrue(controller.contains("@ResponseStatus(HttpStatus.FORBIDDEN)"));
        assertTrue(controller.contains("XINGWEI_V2C_DATA_SCOPE"));
        assertTrue(controller.contains("result.put(\"dataScope\", access.dataScope())"));
        assertTrue(controller.contains("result.put(\"scopeDepartmentCode\", access.departmentCode())"));
        assertTrue(guard.contains("LEGACY_ADMIN_OVERRIDE"));
        assertTrue(guard.contains("LEGACY_OPERATOR_COMPAT"));
        assertTrue(guard.contains("SHADOW_RBAC"));
        assertTrue(guard.contains("effectiveDataScope"));
        assertTrue(guard.contains("\"ALL\", \"DEPARTMENT\", \"SELF\""));
        assertTrue(guard.contains("canAccessEmployee("));
        assertTrue(guard.contains("canManageEmployeeProfile("));
        assertTrue(guard.contains("canAssignEmployeeRoles("));
        assertTrue(guard.contains("scopeCanGrant("));
        assertTrue(guard.contains("canManageGlobalRbac("));
        assertTrue(guard.contains("departmentFallbackToSelf"));
        assertTrue(controller.contains("XW_EMPLOYEE_PROFILE_UPDATE"));
        assertTrue(controller.contains("XW_EMPLOYEE_ROLES_UPDATE"));
        assertTrue(controller.contains("XW_ROLE_PERMISSIONS_UPDATE"));
        assertTrue(controller.contains("audits.log("));
    }

    @Test
    void consoleKeepsV2cOrganizationScopeWhileDashboardAdvancesToV4m() throws Exception {
        String template = read("resources/templates/admin-xingwei.html");
        String orgJs = read("resources/static/assets/admin-xingwei-org.js");
        String mainJs = read("resources/static/assets/admin-xingwei.js");
        String controller = read("java/com/xinghuo/controller/XingweiAdminController.java");
        String accessController = read("java/com/xinghuo/controller/XingweiPermissionController.java");
        assertTrue(template.contains("/assets/admin-xingwei-org.css?v=20261013v2a"));
        assertTrue(template.contains("/assets/admin-xingwei.js?v=20261014v3x"));
        assertTrue(template.contains("/assets/admin-xingwei-org.js?v=20261013v2c"));
        assertTrue(template.contains("V4M · STABLE PRODUCTION"));
        assertTrue(template.contains("V2C · DATA SCOPE"));
        assertTrue(template.contains("ADMIN 全量 · OPERATOR 兼容 · RBAC 执行 ALL / DEPARTMENT / SELF"));
        assertTrue(orgJs.contains("/api/admin/xingwei/org/snapshot"));
        assertTrue(orgJs.contains("form.dataset.canProfile"));
        assertTrue(orgJs.contains("form.dataset.canRoles"));
        assertTrue(orgJs.contains("dataScopeLabel(scope"));
        assertTrue(orgJs.contains("availableEmployeeDepartments"));
        assertTrue(orgJs.contains("V2C 数据范围"));
        assertTrue(mainJs.contains("/api/admin/xingwei/access"));
        assertTrue(mainJs.contains("MODULE_PERMISSIONS"));
        assertTrue(mainJs.contains("employee:manage"));
        assertTrue(mainJs.contains("rbac:manage"));
        assertTrue(mainJs.contains("scopeLabel(snapshot)"));
        assertTrue(mainJs.contains("case 'DEPARTMENT'"));
        assertTrue(mainJs.contains("case 'SELF'"));
        assertTrue(mainJs.contains("dataScope: 'NONE'"));
        assertTrue(mainJs.contains("xw:runtime-health"));
        assertTrue(accessController.contains("@GetMapping(\"/access\")"));
        assertTrue(controller.contains("20261013-v2c"));
        assertTrue(controller.contains("20261016-v4m"));
    }

    private static int occurrences(String source, String needle) {
        int count = 0, index = 0;
        while ((index = source.indexOf(needle, index)) >= 0) { count++; index += needle.length(); }
        return count;
    }

    private static String read(String relative) throws Exception { return Files.readString(ROOT.resolve(relative)); }
}
