package com.xinghuo.service;

import com.xinghuo.security.XingweiPermissionGuard;
import com.xinghuo.security.XingweiPermissionGuard.AccessSnapshot;
import jakarta.servlet.http.HttpServletRequest;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Internal operational records; never calls external marketplaces or changes orders/payments. */
@Service
public class XingweiChannelOperationsService {
    private static final int PAGE_SIZE = 30;
    private static final List<String> CHANNELS = List.of("WEBSITE", "TAOBAO", "XIANYU");
    private static final List<String> STATUSES = List.of("TODO", "IN_PROGRESS", "DONE", "CANCELLED");
    private final JdbcTemplate jdbc;
    private final XingweiPermissionGuard guard;
    private final AuditService audits;

    public XingweiChannelOperationsService(JdbcTemplate jdbc, XingweiPermissionGuard guard, AuditService audits) {
        this.jdbc = jdbc;
        this.guard = guard;
        this.audits = audits;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> snapshot(Authentication actor, String channel, String status, String q, int page) {
        AccessSnapshot access = require(actor, false);
        if (page < 0 || page > 10000) throw new IllegalArgumentException("页码超出范围");
        channel = optionalChoice(channel, CHANNELS, "渠道");
        status = optionalChoice(status, STATUSES, "处理状态");
        q = text(q, 80, false, "搜索内容");
        Clause scope = scope(access, "t.owner_admin_user_id", "t.department_code");
        String where = scope.sql();
        List<Object> args = new ArrayList<>(scope.args());
        if (!channel.isEmpty()) { where += " AND t.channel_code=?"; args.add(channel); }
        if (!status.isEmpty()) { where += " AND t.status=?"; args.add(status); }
        if (!q.isEmpty()) {
            where += " AND (t.title LIKE ? ESCAPE '!' OR t.details LIKE ? ESCAPE '!')";
            String pattern = "%" + q.replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
            args.add(pattern); args.add(pattern);
        }
        long total = jdbc.queryForObject("SELECT COUNT(*) FROM xw_channel_tasks t WHERE " + where,
                Long.class, args.toArray());
        List<Object> listArgs = new ArrayList<>(args);
        listArgs.add(PAGE_SIZE); listArgs.add(page * PAGE_SIZE);
        List<Task> items = jdbc.query(taskSelect() + " WHERE " + where
                + " ORDER BY t.updated_at DESC,t.id LIMIT ? OFFSET ?", this::taskRow, listArgs.toArray());
        List<ChannelState> channels = jdbc.query("SELECT * FROM xw_channel_status ORDER BY channel_code", this::stateRow);
        Clause owners = scope(access, "u.id", "d.code");
        List<Owner> candidates = jdbc.query("""
                SELECT u.id, COALESCE(NULLIF(p.display_name,''),u.username) AS name,d.code AS department_code
                FROM admin_users u LEFT JOIN xw_admin_profiles p ON p.admin_user_id=u.id
                LEFT JOIN xw_departments d ON d.id=p.department_id
                WHERE u.enabled=TRUE AND (p.status='ACTIVE' OR p.status IS NULL) AND
                """ + owners.sql() + " ORDER BY u.id", (rs, n) -> new Owner(rs.getLong("id"),
                rs.getString("name"), rs.getString("department_code")), owners.args().toArray());
        boolean canManage = guard.has(actor, "channel:manage");
        return Map.of("items", items, "channels", channels, "owners", canManage ? candidates : List.of(),
                "totalCount", total, "page", page, "pageSize", PAGE_SIZE,
                "canManage", canManage, "canManageChannels", canManage && global(access), "dataScope", access.dataScope());
    }

    @Transactional
    public Task create(Authentication actor, TaskInput input, HttpServletRequest request) {
        AccessSnapshot access = require(actor, true);
        ValidTask valid = validate(input);
        Owner owner = owner(access, input.ownerAdminUserId() == null ? access.adminUserId() : input.ownerAdminUserId());
        String id = UUID.randomUUID().toString();
        jdbc.update("""
                INSERT INTO xw_channel_tasks(id,channel_code,title,details,status,owner_admin_user_id,
                department_code,due_date,created_by,updated_by) VALUES(?,?,?,?,?,?,?,?,?,?)
                """, id, valid.channel(), valid.title(), valid.details(), valid.status(), owner.id(),
                owner.departmentCode(), input.dueDate(), actor.getName(), actor.getName());
        Task after = find(id, access);
        audits.log("CHANNEL_TASK_CREATE", "CHANNEL_TASK", id, actor.getName(), null, after, "新增渠道任务", request);
        return after;
    }

    @Transactional
    public Task update(String id, Authentication actor, TaskInput input, HttpServletRequest request) {
        AccessSnapshot access = require(actor, true);
        Task before = find(id, access);
        ValidTask valid = validate(input);
        if (input.version() == null || input.version() < 0) throw new IllegalArgumentException("缺少有效版本号");
        Owner owner = owner(access, input.ownerAdminUserId() == null ? before.ownerAdminUserId() : input.ownerAdminUserId());
        int changed = jdbc.update("""
                UPDATE xw_channel_tasks SET channel_code=?,title=?,details=?,status=?,owner_admin_user_id=?,
                department_code=?,due_date=?,version=version+1,updated_by=?,updated_at=CURRENT_TIMESTAMP(6)
                WHERE id=? AND version=?
                """, valid.channel(), valid.title(), valid.details(), valid.status(), owner.id(),
                owner.departmentCode(), input.dueDate(), actor.getName(), id, input.version());
        if (changed != 1) throw new OptimisticLockingFailureException("记录已被其他人更新，请刷新后重新编辑");
        Task after = find(id, access);
        audits.log("CHANNEL_TASK_UPDATE", "CHANNEL_TASK", id, actor.getName(), before, after, "更新渠道任务", request);
        return after;
    }

    @Transactional
    public ChannelState updateChannel(String channel, Authentication actor, StateInput input, HttpServletRequest request) {
        AccessSnapshot access = require(actor, true);
        if (!global(access)) throw new AccessDeniedException("只有全部数据范围的渠道管理员可维护统一服务状态");
        channel = choice(channel, CHANNELS, "渠道");
        String state = choice(input.serviceState(), List.of("UNKNOWN", "AVAILABLE", "PAUSED"), "服务状态");
        String note = text(input.note(), 500, false, "状态说明");
        if (input.version() == null || input.version() < 0) throw new IllegalArgumentException("缺少有效版本号");
        ChannelState before = jdbc.queryForObject("SELECT * FROM xw_channel_status WHERE channel_code=?", this::stateRow, channel);
        int changed = jdbc.update("""
                UPDATE xw_channel_status SET service_state=?,note=?,version=version+1,
                updated_by=?,updated_at=CURRENT_TIMESTAMP(6) WHERE channel_code=? AND version=?
                """, state, note, actor.getName(), channel, input.version());
        if (changed != 1) throw new OptimisticLockingFailureException("渠道状态已更新，请刷新后重试");
        ChannelState after = jdbc.queryForObject("SELECT * FROM xw_channel_status WHERE channel_code=?", this::stateRow, channel);
        audits.log("CHANNEL_STATE_UPDATE", "CHANNEL", channel, actor.getName(), before, after, "维护内部渠道服务状态", request);
        return after;
    }

    private AccessSnapshot require(Authentication actor, boolean write) {
        AccessSnapshot access = guard.resolve(actor);
        if ("DENIED".equals(access.mode())
                || (!global(access) && !List.of("DEPARTMENT", "SELF").contains(access.dataScope()))
                || !guard.has(actor, write ? "channel:manage" : "channel:view"))
            throw new AccessDeniedException("没有渠道" + (write ? "管理" : "查看") + "权限");
        return access;
    }

    private Owner owner(AccessSnapshot access, Long id) {
        if (id == null) throw new IllegalArgumentException("请选择有效负责人");
        List<Owner> owners = jdbc.query("""
                SELECT u.id,COALESCE(NULLIF(p.display_name,''),u.username) AS name,d.code AS department_code
                FROM admin_users u LEFT JOIN xw_admin_profiles p ON p.admin_user_id=u.id
                LEFT JOIN xw_departments d ON d.id=p.department_id
                WHERE u.id=? AND u.enabled=TRUE AND (p.status='ACTIVE' OR p.status IS NULL)
                """, (rs, n) -> new Owner(rs.getLong("id"), rs.getString("name"), rs.getString("department_code")), id);
        if (owners.isEmpty()) throw new IllegalArgumentException("负责人不存在或已停用");
        Owner owner = owners.getFirst();
        if (!global(access) && !id.equals(access.adminUserId())
                && !("DEPARTMENT".equals(access.dataScope()) && access.departmentCode() != null
                    && access.departmentCode().equals(owner.departmentCode())))
            throw new AccessDeniedException("负责人超出当前数据范围");
        return owner;
    }

    private Task find(String id, AccessSnapshot access) {
        Clause scope = scope(access, "t.owner_admin_user_id", "t.department_code");
        List<Object> args = new ArrayList<>(scope.args()); args.add(id);
        List<Task> rows = jdbc.query(taskSelect() + " WHERE " + scope.sql() + " AND t.id=?", this::taskRow, args.toArray());
        if (rows.isEmpty()) throw new AccessDeniedException("记录不存在或超出当前数据范围");
        return rows.getFirst();
    }

    private static String taskSelect() {
        return "SELECT t.*,COALESCE(NULLIF(p.display_name,''),u.username) AS owner_name FROM xw_channel_tasks t"
                + " LEFT JOIN admin_users u ON u.id=t.owner_admin_user_id"
                + " LEFT JOIN xw_admin_profiles p ON p.admin_user_id=u.id";
    }

    private Task taskRow(ResultSet rs, int row) throws SQLException {
        return new Task(rs.getString("id"), rs.getString("channel_code"), rs.getString("title"),
                rs.getString("details"), rs.getString("status"), rs.getLong("owner_admin_user_id"),
                rs.getString("owner_name"), rs.getString("department_code"),
                rs.getDate("due_date") == null ? null : rs.getDate("due_date").toLocalDate(),
                rs.getLong("version"), rs.getTimestamp("updated_at").toInstant().toString());
    }

    private ChannelState stateRow(ResultSet rs, int row) throws SQLException {
        return new ChannelState(rs.getString("channel_code"), rs.getString("service_state"),
                rs.getString("note"), rs.getLong("version"), rs.getString("updated_by"),
                rs.getTimestamp("updated_at").toInstant().toString());
    }

    private static boolean global(AccessSnapshot access) { return access.allowAll() || "ALL".equals(access.dataScope()); }
    private static Clause scope(AccessSnapshot access, String owner, String department) {
        if ("DENIED".equals(access.mode())) return new Clause("1=0", List.of());
        if (global(access)) return new Clause("1=1", List.of());
        if (access.adminUserId() == null) return new Clause("1=0", List.of());
        if ("DEPARTMENT".equals(access.dataScope()) && access.departmentCode() != null)
            return new Clause("(" + owner + "=? OR " + department + "=?)", List.of(access.adminUserId(), access.departmentCode()));
        if ("SELF".equals(access.dataScope()) || "DEPARTMENT".equals(access.dataScope()))
            return new Clause(owner + "=?", List.of(access.adminUserId()));
        return new Clause("1=0", List.of());
    }

    private static ValidTask validate(TaskInput input) {
        if (input == null) throw new IllegalArgumentException("任务内容不能为空");
        return new ValidTask(choice(input.channelCode(), CHANNELS, "渠道"), text(input.title(),120,true,"任务标题"),
                text(input.details(),2000,false,"任务说明"), choice(input.status(),STATUSES,"处理状态"));
    }
    private static String text(String value, int max, boolean required, String label) {
        value = value == null ? "" : value.trim();
        if ((required && value.isEmpty()) || value.length() > max) throw new IllegalArgumentException(label + "不能为空且不能超过" + max + "字");
        return value;
    }
    private static String optionalChoice(String value, List<String> allowed, String label) {
        return value == null || value.isEmpty() ? "" : choice(value, allowed, label);
    }
    private static String choice(String value, List<String> allowed, String label) {
        if (!allowed.contains(value == null ? "" : value)) throw new IllegalArgumentException(label + "无效");
        return value;
    }
    private record Clause(String sql, List<Object> args) {}
    private record ValidTask(String channel, String title, String details, String status) {}
    public record Owner(long id, String name, String departmentCode) {}
    public record Task(String id, String channelCode, String title, String details, String status,
                       long ownerAdminUserId, String ownerName, String departmentCode, LocalDate dueDate,
                       long version, String updatedAt) {}
    public record TaskInput(String channelCode, String title, String details, String status,
                            Long ownerAdminUserId, LocalDate dueDate, Long version) {}
    public record ChannelState(String channelCode, String serviceState, String note, long version,
                               String updatedBy, String updatedAt) {}
    public record StateInput(String serviceState, String note, Long version) {}
}
