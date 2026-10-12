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
        while (positionCodes.find()) {
            count++;
        }
        assertEquals(155, count, "V63 must seed the complete canonical 155-position dictionary exactly once");

        for (String position : List.of(
                "工作室负责人",
                "淘宝运营专员",
                "售前客服",
                "MW4 代练员",
                "微信支付开发工程师",
                "SEO 专员",
                "权限管理员")) {
            assertTrue(migration.contains("'" + position + "'"), "missing representative canonical position " + position);
        }
    }

    @Test
    void v2aIsAdditiveAndLeavesLegacyAdminAuthenticationUntouched() throws Exception {
        String migration = read("resources/db/migration/V63__xingwei_org_rbac_foundation.sql");
        String security = read("java/com/xinghuo/security/SecurityConfig.java");
        String service = read("java/com/xinghuo/service/XingweiOrgRbacService.java");

        for (String table : List.of(
                "xw_departments",
                "xw_positions",
                "xw_rbac_roles",
                "xw_permissions",
                "xw_role_permissions",
                "xw_admin_profiles",
                "xw_admin_roles")) {
            assertTrue(migration.contains("CREATE TABLE " + table), "missing additive table " + table);
        }
        assertFalse(migration.contains("ALTER TABLE admin_users"), "V2A must not change the legacy admin login table");
        assertFalse(migration.contains("UPDATE admin_users"), "V2A must not rewrite legacy admin roles");
        assertTrue(security.contains(".hasAnyRole(\"ADMIN\", \"OPERATOR\")"),
                "legacy ADMIN/OPERATOR must remain the actual Spring Security gate in V2A");
        assertTrue(service.contains("result.put(\"shadowMode\", true)"));
        assertTrue(service.contains("result.put(\"enforcement\", \"LEGACY_ADMIN_ROLE\")"));
    }

    @Test
    void allV2aWritesRequireLegacyAdminAndAreAudited() throws Exception {
        String controller = read("java/com/xinghuo/controller/XingweiOrgRbacController.java");

        assertTrue(controller.contains("@GetMapping(\"/snapshot\")"));
        assertTrue(controller.contains("@PutMapping(\"/employees/{adminUserId}/profile\")"));
        assertTrue(controller.contains("@PutMapping(\"/employees/{adminUserId}/roles\")"));
        assertTrue(controller.contains("@PutMapping(\"/roles/{roleCode}/permissions\")"));
        assertEquals(3, occurrences(controller, "@PreAuthorize(\"hasRole('ADMIN')\")"),
                "every V2A mutation endpoint must remain legacy-ADMIN-only");
        assertTrue(controller.contains("XW_EMPLOYEE_PROFILE_UPDATE"));
        assertTrue(controller.contains("XW_EMPLOYEE_ROLES_UPDATE"));
        assertTrue(controller.contains("XW_ROLE_PERMISSIONS_UPDATE"));
        assertTrue(controller.contains("audits.log("));
    }

    @Test
    void consoleLoadsV2aAssetsAndMakesShadowModeExplicit() throws Exception {
        String template = read("resources/templates/admin-xingwei.html");
        String js = read("resources/static/assets/admin-xingwei-org.js");
        String controller = read("java/com/xinghuo/controller/XingweiAdminController.java");

        assertTrue(template.contains("/assets/admin-xingwei-org.css?v=20261013v2a"));
        assertTrue(template.contains("/assets/admin-xingwei-org.js?v=20261013v2a"));
        assertTrue(template.contains("影子权限模式"));
        assertTrue(template.contains("155 岗位正式词库"));
        assertTrue(js.contains("/api/admin/xingwei/org/snapshot"));
        assertTrue(js.contains("XW_MANAGEMENT"));
        assertTrue(controller.contains("20261013-v2a"));
    }

    private static int occurrences(String source, String needle) {
        int count = 0;
        int index = 0;
        while ((index = source.indexOf(needle, index)) >= 0) {
            count++;
            index += needle.length();
        }
        return count;
    }

    private static String read(String relative) throws Exception {
        return Files.readString(ROOT.resolve(relative));
    }
}
