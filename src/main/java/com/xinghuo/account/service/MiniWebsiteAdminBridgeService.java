package com.xinghuo.account.service;

import com.xinghuo.dto.ApiDtos;
import com.xinghuo.security.XingweiPermissionGuard;
import com.xinghuo.service.AuditService;
import com.xinghuo.service.OrderService;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Explicit browser-confirmed binding; never relies on mini program cloud admin flags. */
@Service
public class MiniWebsiteAdminBridgeService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private final JdbcTemplate jdbc;
    private final WeChatMiniBridgeService wechat;
    private final XingweiPermissionGuard permission;
    private final OrderService orders;
    private final AuditService audits;

    public MiniWebsiteAdminBridgeService(JdbcTemplate jdbc, WeChatMiniBridgeService wechat,
            XingweiPermissionGuard permission, OrderService orders, AuditService audits) {
        this.jdbc = jdbc;
        this.wechat = wechat;
        this.permission = permission;
        this.orders = orders;
        this.audits = audits;
    }

    public record Challenge(String code, int expiresInSeconds) {}
    public record MiniAdminView(boolean linked, String username, boolean canViewOrders,
                                boolean canManageOrders, String dataScope) {}
    public record MiniOrderView(String orderNo, String businessNo, String status,
                                Integer progressPercent, String progressText, String customerNote,
                                String updatedAt, long version) {
        static MiniOrderView from(ApiDtos.AdminOrderResponse x) {
            return new MiniOrderView(x.orderNo(), x.businessNo(), x.status(),
                    x.progressPercent(), x.progressText(), x.customerNote(),
                    String.valueOf(x.updatedAt()), x.version());
        }
    }
    private record BoundAdmin(long id, String username, String role, long authVersion,
                              Authentication identity) {}

    /** Browser admin must separately confirm the code within five minutes. */
    @Transactional
    public Challenge challenge(String loginCode) {
        String digest = sha(wechat.verifiedMiniOpenId(loginCode));
        jdbc.update("DELETE FROM xw_mini_admin_challenges WHERE mini_openid_hash = ? AND consumed_at IS NULL",
                digest);
        for (int attempt = 0; attempt < 10; attempt++) {
            String code = String.format(java.util.Locale.ROOT, "%08d", RANDOM.nextInt(100_000_000));
            try {
                jdbc.update("INSERT INTO xw_mini_admin_challenges (code_hash, mini_openid_hash, expires_at) " +
                        "VALUES (?, ?, CURRENT_TIMESTAMP + INTERVAL '5 minutes')", sha(code), digest);
                return new Challenge(code, 300);
            } catch (DataIntegrityViolationException collision) {
                // Extremely unlikely eight-digit collision. Generate a different one.
            }
        }
        throw new ResponseStatusException(HttpStatus.CONFLICT, "绑定码生成失败，请重试");
    }

    @Transactional
    public void confirm(String code, Authentication browserAdmin, HttpServletRequest request) {
        if (code == null || !code.matches("[0-9]{8}")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请输入八位绑定码");
        }
        XingweiPermissionGuard.AccessSnapshot snapshot = permission.resolve(browserAdmin);
        if (snapshot.adminUserId() == null || !permission.has(browserAdmin, "order:view")
                || !"ALL".equals(snapshot.dataScope())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "当前网站管理员无全局订单权限");
        }
        List<String> found = jdbc.query(
                "SELECT mini_openid_hash FROM xw_mini_admin_challenges " +
                "WHERE code_hash=? AND consumed_at IS NULL AND expires_at>CURRENT_TIMESTAMP FOR UPDATE",
                (rs, n) -> rs.getString(1), sha(code));
        if (found.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "绑定码无效或已过期");
        Long revision = jdbc.queryForObject(
                "SELECT auth_version FROM admin_users WHERE id=? AND enabled=TRUE", Long.class,
                snapshot.adminUserId());
        if (revision == null) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "账号已失效");
        try {
            jdbc.update("INSERT INTO xw_mini_admin_bindings " +
                    "(admin_user_id,mini_openid_hash,admin_auth_version) VALUES (?,?,?)",
                    snapshot.adminUserId(), found.get(0), revision);
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "该管理员或微信已绑定；请先在原网站后台解绑");
        }
        jdbc.update("UPDATE xw_mini_admin_challenges SET consumed_at=CURRENT_TIMESTAMP WHERE code_hash=?",
                sha(code));
        audits.log("MINI_ADMIN_BOUND", "ADMIN", String.valueOf(snapshot.adminUserId()),
                browserAdmin.getName(), null, null, "网站后台确认微信管理身份绑定", request);
    }

    @Transactional
    public void revoke(Authentication browserAdmin, HttpServletRequest request) {
        XingweiPermissionGuard.AccessSnapshot actor = permission.resolve(browserAdmin);
        if (actor.adminUserId() == null) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        jdbc.update("DELETE FROM xw_mini_admin_bindings WHERE admin_user_id=?", actor.adminUserId());
        audits.log("MINI_ADMIN_UNBOUND", "ADMIN", String.valueOf(actor.adminUserId()),
                browserAdmin.getName(), null, null, "网站后台主动解除微信管理身份", request);
    }

    public MiniAdminView me(String loginCode) {
        BoundAdmin bound = lookup(loginCode);
        if (bound == null) return new MiniAdminView(false, "", false, false, "NONE");
        XingweiPermissionGuard.AccessSnapshot scope = permission.resolve(bound.identity());
        boolean all = "ALL".equals(scope.dataScope());
        return new MiniAdminView(true, bound.username(),
                all && permission.has(bound.identity(), "order:view"),
                all && permission.has(bound.identity(), "order:manage"), scope.dataScope());
    }

    public List<MiniOrderView> listOrders(String loginCode) {
        BoundAdmin bound = require(loginCode, "order:view");
        return orders.list().stream().limit(100).map(MiniOrderView::from).toList();
    }

    @Transactional
    public MiniOrderView update(String loginCode, String orderNo, ApiDtos.UpdateOrderRequest update,
                                HttpServletRequest request) {
        BoundAdmin bound = require(loginCode, "order:manage");
        if (orderNo == null || orderNo.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "订单编号不能为空");
        }
        if (update == null || update.reason() == null || update.reason().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "必须填写变更原因");
        }
        // Mini admin UI cannot read or overwrite internal notes. Website order service
        // handles optimistic version locks, valid state transitions and payment gates.
        ApiDtos.UpdateOrderRequest safe = new ApiDtos.UpdateOrderRequest(
                update.version(), update.status(), update.progressPercent(),
                update.progressText(), update.customerNote(), null, update.reason());
        ApiDtos.AdminOrderResponse result = orders.update(
                orderNo, safe, "mini:" + bound.username(), request);
        return MiniOrderView.from(result);
    }

    private BoundAdmin require(String code, String needed) {
        BoundAdmin bound = lookup(code);
        if (bound == null || !permission.has(bound.identity(), needed)
                || !"ALL".equals(permission.resolve(bound.identity()).dataScope())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "尚未绑定或没有网站订单权限");
        }
        return bound;
    }

    private BoundAdmin lookup(String code) {
        String digest = sha(wechat.verifiedMiniOpenId(code));
        List<Map<String,Object>> rows = jdbc.queryForList(
                "SELECT u.id,u.username,u.role,u.auth_version " +
                "FROM xw_mini_admin_bindings b JOIN admin_users u ON u.id=b.admin_user_id " +
                "WHERE b.mini_openid_hash=? AND u.enabled=TRUE " +
                "AND b.admin_auth_version=u.auth_version", digest);
        if (rows.isEmpty()) return null;
        Map<String,Object> row = rows.get(0);
        String username = String.valueOf(row.get("username"));
        String role = String.valueOf(row.get("role"));
        Authentication synthetic = new UsernamePasswordAuthenticationToken(username, null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role)));
        return new BoundAdmin(((Number)row.get("id")).longValue(), username, role,
                ((Number)row.get("auth_version")).longValue(), synthetic);
    }

    private static String sha(String source) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(source.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }
}