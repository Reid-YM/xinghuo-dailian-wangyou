package com.xinghuo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import com.xinghuo.security.XingweiPermissionGuard;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

class XingweiBusinessScopeServiceTest {
    private JdbcTemplate jdbc;
    private XingweiBusinessScopeService service;

    @BeforeEach
    void setUp() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:xw_business_" + System.nanoTime() + ";MODE=MySQL;DB_CLOSE_DELAY=-1",
                "sa",
                "");
        jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("CREATE TABLE admin_users (id BIGINT PRIMARY KEY, username VARCHAR(64) UNIQUE, enabled BOOLEAN NOT NULL)");
        jdbc.execute("CREATE TABLE xw_departments (id BIGINT PRIMARY KEY, code VARCHAR(16), name VARCHAR(80), enabled BOOLEAN, sort_order INT)");
        jdbc.execute("CREATE TABLE xw_positions (id BIGINT PRIMARY KEY, code VARCHAR(32), name VARCHAR(80), enabled BOOLEAN)");
        jdbc.execute("CREATE TABLE xw_admin_profiles (admin_user_id BIGINT PRIMARY KEY, display_name VARCHAR(80), employee_no VARCHAR(32), department_id BIGINT, position_id BIGINT, status VARCHAR(24))");
        jdbc.execute("CREATE TABLE xw_rbac_roles (id BIGINT PRIMARY KEY, code VARCHAR(48), data_scope VARCHAR(32), enabled BOOLEAN NOT NULL, sort_order INT NOT NULL)");
        jdbc.execute("CREATE TABLE xw_permissions (id BIGINT PRIMARY KEY, code VARCHAR(64))");
        jdbc.execute("CREATE TABLE xw_admin_roles (admin_user_id BIGINT, role_id BIGINT, is_primary BOOLEAN NOT NULL)");
        jdbc.execute("CREATE TABLE xw_role_permissions (role_id BIGINT, permission_id BIGINT)");
        jdbc.execute("CREATE TABLE xw_business_ownership (business_no VARCHAR(20) PRIMARY KEY, owner_admin_user_id BIGINT, department_id BIGINT NOT NULL, created_by VARCHAR(64), updated_by VARCHAR(64), created_at TIMESTAMP, updated_at TIMESTAMP)");
        jdbc.execute("CREATE TABLE quotes (business_no VARCHAR(20) PRIMARY KEY, game VARCHAR(100), demand VARCHAR(200), current_progress VARCHAR(500), customer_supplement CLOB, screenshot_asset_id VARCHAR(64), platform VARCHAR(50), expected_time VARCHAR(100), contact_method VARCHAR(40), customer_contact VARCHAR(120), customer_ip_hash VARCHAR(128), status VARCHAR(24), created_at TIMESTAMP)");
        jdbc.execute("CREATE TABLE customer_orders (order_no VARCHAR(40) PRIMARY KEY, business_no VARCHAR(20), status VARCHAR(32), progress_percent INT, progress_text VARCHAR(240), progress_image_asset_id VARCHAR(64), customer_note VARCHAR(500), internal_note VARCHAR(1200), updated_at TIMESTAMP, version BIGINT)");

        jdbc.update("INSERT INTO admin_users VALUES (1,'admin',TRUE),(2,'dept',TRUE),(3,'self',TRUE),(4,'agent03',TRUE),(5,'agent04',TRUE)");
        jdbc.update("INSERT INTO xw_departments VALUES (3,'03','03 客服与订单中心',TRUE,3),(4,'04','04 游戏业务中心',TRUE,4)");
        jdbc.update("INSERT INTO xw_admin_profiles VALUES (2,'部门主管','D02',3,NULL,'ACTIVE'),(3,'本人','S03',3,NULL,'ACTIVE'),(4,'客服甲','A04',3,NULL,'ACTIVE'),(5,'游戏甲','A05',4,NULL,'ACTIVE')");
        jdbc.update("INSERT INTO xw_rbac_roles VALUES (1,'XW_CS','DEPARTMENT',TRUE,1),(2,'XW_SELF','SELF',TRUE,2)");
        jdbc.update("INSERT INTO xw_permissions VALUES (1,'quote:view'),(2,'quote:manage'),(3,'order:view'),(4,'order:assign')");
        jdbc.update("INSERT INTO xw_admin_roles VALUES (2,1,TRUE),(3,2,TRUE)");
        jdbc.update("INSERT INTO xw_role_permissions VALUES (1,1),(1,2),(1,3),(1,4),(2,1),(2,2),(2,3),(2,4)");

        jdbc.update("INSERT INTO quotes VALUES ('XHDEPT03','MW4','需求A',NULL,NULL,NULL,'PC',NULL,'WECHAT','a',NULL,'NEW',CURRENT_TIMESTAMP)");
        jdbc.update("INSERT INTO quotes VALUES ('XHDEPT04','MW4','需求B',NULL,NULL,NULL,'PC',NULL,'WECHAT','b',NULL,'NEW',CURRENT_TIMESTAMP)");
        jdbc.update("INSERT INTO quotes VALUES ('XHSELF03','MW4','需求C',NULL,NULL,NULL,'PC',NULL,'WECHAT','c',NULL,'NEW',CURRENT_TIMESTAMP)");
        jdbc.update("INSERT INTO quotes VALUES ('XHUNASSIGNED','MW4','需求D',NULL,NULL,NULL,'PC',NULL,'WECHAT','d',NULL,'NEW',CURRENT_TIMESTAMP)");
        jdbc.execute("ALTER TABLE quotes ADD COLUMN customer_account_id BIGINT NULL");
        jdbc.update("INSERT INTO customer_orders VALUES ('XHDEPT03','XHDEPT03','IN_PROGRESS',20,'处理中',NULL,NULL,NULL,CURRENT_TIMESTAMP,0)");
        jdbc.update("INSERT INTO customer_orders VALUES ('XHDEPT04','XHDEPT04','IN_PROGRESS',30,'处理中',NULL,NULL,NULL,CURRENT_TIMESTAMP,0)");
        jdbc.update("INSERT INTO customer_orders VALUES ('XHSELF03','XHSELF03','IN_PROGRESS',40,'处理中',NULL,NULL,NULL,CURRENT_TIMESTAMP,0)");
        jdbc.update("INSERT INTO customer_orders VALUES ('XHUNASSIGNED','XHUNASSIGNED','WAITING_CONFIRMATION',NULL,'待确认',NULL,NULL,NULL,CURRENT_TIMESTAMP,0)");
        jdbc.update("INSERT INTO xw_business_ownership VALUES ('XHDEPT03',4,3,'admin','admin',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)");
        jdbc.update("INSERT INTO xw_business_ownership VALUES ('XHDEPT04',5,4,'admin','admin',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)");
        jdbc.update("INSERT INTO xw_business_ownership VALUES ('XHSELF03',3,3,'admin','admin',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)");
        // The real production schema has quote lineage and payment state fields.
        jdbc.execute("ALTER TABLE quotes ADD COLUMN id VARCHAR(36)");
        jdbc.execute("ALTER TABLE customer_orders ADD COLUMN source_quote_id VARCHAR(36)");
        jdbc.execute("ALTER TABLE customer_orders ADD COLUMN customer_account_id BIGINT");
        jdbc.execute("CREATE TABLE payment_order (business_no VARCHAR(20), provider VARCHAR(24), status VARCHAR(24))");

        XingweiPermissionGuard guard = new XingweiPermissionGuard(jdbc);
        service = new XingweiBusinessScopeService(jdbc, guard, mock(AuditService.class));
    }

    @Test
    void allScopeSeesAssignedAndUnassignedBusiness() {
        Map<String, Object> quotes = service.quoteSnapshot(authentication("admin", "ROLE_ADMIN"));
        assertEquals("ALL", quotes.get("dataScope"));
        assertEquals(4, quotes.get("visibleCount"));
        assertEquals(1L, quotes.get("unassignedCount"));
        assertTrue((Boolean) quotes.get("unassignedVisible"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void offerActionsOnlyAppearForOpenAnonymousAuthorizedQuotes() {
        jdbc.update("UPDATE quotes SET customer_account_id=77 WHERE business_no='XHDEPT04'");
        jdbc.update("UPDATE quotes SET status='CLOSED' WHERE business_no='XHSELF03'");
        Map<String, Object> snapshot = service.quoteSnapshot(authentication("admin", "ROLE_ADMIN"));
        List<Map<String, Object>> items = (List<Map<String, Object>>) snapshot.get("items");
        Map<String, Object> activeGuest = items.stream()
                .filter(row -> "XHDEPT03".equals(row.get("businessNo"))).findFirst().orElseThrow();
        Map<String, Object> account = items.stream()
                .filter(row -> "XHDEPT04".equals(row.get("businessNo"))).findFirst().orElseThrow();
        Map<String, Object> closed = items.stream()
                .filter(row -> "XHSELF03".equals(row.get("businessNo"))).findFirst().orElseThrow();

        assertEquals("GUEST", activeGuest.get("customerType"));
        assertEquals(true, activeGuest.get("canOffer"));
        assertEquals("ACCOUNT", account.get("customerType"));
        assertEquals(false, account.get("canOffer"));
        assertEquals(false, closed.get("canOffer"));
        List<Map<String, Object>> limited = (List<Map<String, Object>>) service
                .quoteSnapshot(authentication("self", "ROLE_OPERATOR")).get("items");
        assertTrue(limited.stream().noneMatch(row -> Boolean.TRUE.equals(row.get("canOffer"))));
    }

    @Test
    @SuppressWarnings("unchecked")
    void manuallyConvertedUnpaidGuestOrderCanStillBeQuotedButPaidCannot() {
        jdbc.update("UPDATE quotes SET id='quote-dept-03',status='CONVERTED' WHERE business_no='XHDEPT03'");
        jdbc.update("UPDATE customer_orders SET source_quote_id='quote-dept-03' WHERE business_no='XHDEPT03'");
        List<Map<String, Object>> quoteRows = (List<Map<String, Object>>) service
                .quoteSnapshot(authentication("admin", "ROLE_ADMIN")).get("items");
        var quote = quoteRows.stream().filter(row -> "XHDEPT03".equals(row.get("businessNo")))
                .findFirst().orElseThrow();
        assertEquals(true, quote.get("canOffer"));
        List<Map<String, Object>> orderRows = (List<Map<String, Object>>) service
                .orderSnapshot(authentication("admin", "ROLE_ADMIN")).get("items");
        var order = orderRows.stream().filter(row -> "XHDEPT03".equals(row.get("businessNo")))
                .findFirst().orElseThrow();
        assertEquals(true, order.get("guestQuoteLinked"));
        assertEquals(true, order.get("canOffer"));

        jdbc.update("INSERT INTO payment_order VALUES('XHDEPT03','WECHAT','CREATED')");
        quoteRows = (List<Map<String, Object>>) service
                .quoteSnapshot(authentication("admin", "ROLE_ADMIN")).get("items");
        quote = quoteRows.stream().filter(row -> "XHDEPT03".equals(row.get("businessNo")))
                .findFirst().orElseThrow();
        assertEquals(false, quote.get("canOffer"));
        orderRows = (List<Map<String, Object>>) service
                .orderSnapshot(authentication("admin", "ROLE_ADMIN")).get("items");
        order = orderRows.stream().filter(row -> "XHDEPT03".equals(row.get("businessNo")))
                .findFirst().orElseThrow();
        assertEquals(false, order.get("canOffer"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void orphanedSourceQuoteNeverBreaksOrderListOrExposesGuestOffer() {
        jdbc.update("UPDATE customer_orders SET source_quote_id='orphan' WHERE business_no='XHDEPT04'");
        List<Map<String, Object>> orders = (List<Map<String, Object>>) service
                .orderSnapshot(authentication("admin", "ROLE_ADMIN")).get("items");
        Map<String, Object> orphan = orders.stream()
                .filter(row -> "XHDEPT04".equals(row.get("businessNo"))).findFirst().orElseThrow();
        assertFalse(orphan.containsKey("guestQuoteLinked"));
        assertFalse(Boolean.TRUE.equals(orphan.get("canOffer")));
    }

    @Test
    void departmentScopeOnlySeesItsOwnedDepartment() {
        Map<String, Object> quotes = service.quoteSnapshot(authentication("dept", "ROLE_OPERATOR"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) quotes.get("items");

        assertEquals("DEPARTMENT", quotes.get("dataScope"));
        assertEquals(2, items.size());
        assertTrue(items.stream().anyMatch(row -> "XHDEPT03".equals(row.get("businessNo"))));
        assertTrue(items.stream().anyMatch(row -> "XHSELF03".equals(row.get("businessNo"))));
        assertFalse(items.stream().anyMatch(row -> "XHDEPT04".equals(row.get("businessNo"))));
        assertFalse(items.stream().anyMatch(row -> "XHUNASSIGNED".equals(row.get("businessNo"))));
    }

    @Test
    void selfScopeOnlySeesBusinessOwnedBySelf() {
        Map<String, Object> orders = service.orderSnapshot(authentication("self", "ROLE_OPERATOR"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) orders.get("items");

        assertEquals("SELF", orders.get("dataScope"));
        assertEquals(1, items.size());
        assertEquals("XHSELF03", items.getFirst().get("businessNo"));
    }

    @Test
    void scopedOperatorCannotClaimUnassignedOrCrossDepartmentBusiness() {
        Authentication dept = authentication("dept", "ROLE_OPERATOR");
        assertThrows(AccessDeniedException.class,
                () -> service.assignQuote("XHUNASSIGNED", 4L, null, dept, null));
        assertThrows(AccessDeniedException.class,
                () -> service.assignQuote("XHDEPT03", 5L, null, dept, null));
    }

    @Test
    void departmentOperatorCanReassignOwnedBusinessInsideDepartment() {
        Authentication dept = authentication("dept", "ROLE_OPERATOR");
        Map<String, Object> ownership = service.assignQuote("XHDEPT03", 4L, null, dept, null);
        assertEquals("03", ownership.get("departmentCode"));
        assertEquals(4L, ownership.get("ownerAdminUserId"));
    }

    private static Authentication authentication(String username, String authority) {
        return new UsernamePasswordAuthenticationToken(
                username,
                "n/a",
                List.of(new SimpleGrantedAuthority(authority)));
    }
}
