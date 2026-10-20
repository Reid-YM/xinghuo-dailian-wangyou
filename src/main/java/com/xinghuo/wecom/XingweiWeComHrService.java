package com.xinghuo.wecom;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.xinghuo.service.XingweiOrgRbacService;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/** Validated, short-lived HR plans; no request can supply an arbitrary vendor payload. */
@Service
public class XingweiWeComHrService {
    private final XingweiWeComWriteClient client;
    private final XingweiOrgRbacService org;
    private final ObjectMapper mapper;

    public XingweiWeComHrService(XingweiWeComWriteClient client, XingweiOrgRbacService org, ObjectMapper mapper) {
        this.client = client;
        this.org = org;
        this.mapper = mapper;
    }

    public boolean ready() { return client.ready(); }

    public Plan prepare(Command command) {
        requireReady();
        if (command == null) throw bad("请填写变更资料");
        String action = clean(command.action());
        ObjectNode payload = mapper.createObjectNode();
        String id;
        String name;
        String before = "";
        String departmentName = "";
        String positionName = "";
        if ("CREATE_MEMBER".equals(action)) {
            name = name(command.name());
            String mobile = clean(command.mobile());
            String email = clean(command.email());
            if (mobile.isBlank() && email.isBlank()) throw bad("请填写手机号或邮箱");
            if (!mobile.isBlank() && !mobile.matches("[+0-9][0-9 -]{4,31}")) throw bad("请核对手机号格式");
            if (!email.isBlank() && (email.length() > 128 || !email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")))
                throw bad("请核对邮箱格式");
            Assignment assignment = assignment(command.departmentId(), command.positionCode());
            departmentName = assignment.departmentName();
            positionName = assignment.positionName();
            id = "xh_" + UUID.randomUUID().toString().replace("-", "");
            payload.put("userid", id).put("name", name).put("position", positionName).put("enable", 1);
            payload.putArray("department").add(assignment.departmentId());
            payload.put("main_department", assignment.departmentId());
            if (!mobile.isBlank()) payload.put("mobile", mobile);
            if (!email.isBlank()) payload.put("email", email);
        } else if ("TRANSFER_MEMBER".equals(action) || "DISABLE_MEMBER".equals(action)) {
            id = memberId(command.userId());
            JsonNode current = member(id);
            name = name(current.path("name").asText(""));
            before = fingerprint(current);
            payload.put("userid", id);
            if ("TRANSFER_MEMBER".equals(action)) {
                Assignment assignment = assignment(command.departmentId(), command.positionCode());
                departmentName = assignment.departmentName();
                positionName = assignment.positionName();
                payload.putArray("department").add(assignment.departmentId());
                payload.put("main_department", assignment.departmentId());
                payload.put("position", positionName);
            } else payload.put("enable", 0);
        } else if ("CREATE_DEPARTMENT".equals(action)) {
            name = name(command.name());
            JsonNode parentDepartment = department(command.departmentId());
            long parent = parentDepartment.path("id").asLong();
            before = fingerprintDepartment(parentDepartment);
            ensureUniqueDepartmentName(name, parent, null);
            id = "";
            payload.put("name", name).put("parentid", parent);
            departmentName = parentDepartment.path("name").asText();
        } else if ("RENAME_DEPARTMENT".equals(action)) {
            JsonNode current = department(command.departmentId());
            if (current.path("id").asLong() == 1) throw bad("企业根部门不能在此改名");
            id = String.valueOf(current.path("id").asLong());
            name = name(command.name());
            before = fingerprintDepartment(current);
            departmentName = current.path("name").asText();
            ensureUniqueDepartmentName(name, current.path("parentid").asLong(), current.path("id").asLong());
            payload.put("id", current.path("id").asLong()).put("name", name);
        } else throw bad("请选择支持的通讯录操作");
        return new Plan(action, id, name, departmentName, positionName, clean(command.positionCode()),
                before, payload, Instant.now().plusSeconds(300));
    }

    /** Re-read and validate immediately before the one-shot write. No automatic retries. */
    public void validateCurrent(Plan plan) {
        requireReady();
        if (plan == null || !Instant.now().isBefore(plan.expiresAt())) throw conflict("预览已过期，请重新核对");
        if (plan.action().equals("TRANSFER_MEMBER") || plan.action().equals("DISABLE_MEMBER")) {
            if (!plan.before().equals(fingerprint(member(plan.targetId()))))
                throw conflict("成员资料已变化，请刷新后重新预览");
        }
        if (plan.action().equals("CREATE_MEMBER") || plan.action().equals("TRANSFER_MEMBER")) {
            Assignment current = assignment(plan.payload().path("department").get(0).asLong(), plan.positionCode());
            if (!current.positionName().equals(plan.positionName()) || !current.departmentName().equals(plan.departmentName()))
                throw conflict("部门或岗位资料已变化，请重新预览");
        }
        if (plan.action().equals("CREATE_DEPARTMENT")) {
            if (!plan.before().equals(fingerprintDepartment(department(plan.payload().path("parentid").asLong()))))
                throw conflict("上级部门资料已变化，请重新预览");
            ensureUniqueDepartmentName(plan.name(), plan.payload().path("parentid").asLong(), null);
        }
        if (plan.action().equals("RENAME_DEPARTMENT")) {
            JsonNode current = department(Long.valueOf(plan.targetId()));
            if (!plan.before().equals(fingerprintDepartment(current)))
                throw conflict("部门资料已变化，请刷新后重新预览");
            ensureUniqueDepartmentName(plan.name(), current.path("parentid").asLong(), Long.valueOf(plan.targetId()));
        }
    }

    public void execute(Plan plan) {
        Long createdDepartmentId = null;
        switch (plan.action()) {
            case "CREATE_MEMBER" -> client.createMember(plan.payload());
            case "TRANSFER_MEMBER", "DISABLE_MEMBER" -> client.updateMember(plan.payload());
            case "CREATE_DEPARTMENT" -> {
                long id = client.createDepartment(plan.payload()).path("id").asLong(0);
                if (id < 1) throw pending();
                createdDepartmentId = id;
            }
            case "RENAME_DEPARTMENT" -> client.updateDepartment(plan.payload());
            default -> throw bad("不支持的通讯录操作");
        }
        // Successful POST is not enough: verify the actual directory state.
        if (plan.action().endsWith("MEMBER")) {
            JsonNode current = member(plan.targetId());
            boolean confirmed = switch (plan.action()) {
                case "CREATE_MEMBER" -> current.path("enable").asInt(1) != 0 && current.path("status").asInt(-1) != 2
                        && plan.name().equals(current.path("name").asText())
                        && assigned(current, plan.payload());
                case "TRANSFER_MEMBER" -> assigned(current, plan.payload());
                case "DISABLE_MEMBER" -> current.path("enable").asInt(-1) == 0 || current.path("status").asInt(-1) == 2;
                default -> false;
            };
            if (!confirmed) throw pending();
        } else {
            boolean confirmed = false;
            for (JsonNode row : client.departments().path("department")) {
                if (!plan.name().equals(row.path("name").asText())) continue;
                if (plan.action().equals("RENAME_DEPARTMENT") && plan.targetId().equals(row.path("id").asText())) confirmed = true;
                if (plan.action().equals("CREATE_DEPARTMENT") && createdDepartmentId != null
                        && createdDepartmentId == row.path("id").asLong()
                        && row.path("parentid").asLong() == plan.payload().path("parentid").asLong()) confirmed = true;
            }
            if (!confirmed) throw pending();
        }
    }

    private boolean assigned(JsonNode member, JsonNode payload) {
        List<Long> ids = new ArrayList<>();
        for (JsonNode id : member.path("department")) ids.add(id.asLong());
        return ids.equals(List.of(payload.path("department").get(0).asLong()))
                && payload.path("position").asText().equals(member.path("position").asText());
    }

    private JsonNode member(String id) {
        JsonNode result = client.member(id);
        if (!id.equals(result.path("userid").asText())) throw conflict("成员资料暂时无法核对，请刷新");
        return result;
    }

    private JsonNode department(Long id) {
        if (id == null || id < 1) throw bad("请选择部门");
        for (JsonNode row : client.departments().path("department")) if (id == row.path("id").asLong()) return row;
        throw conflict("部门已变化或不在管理范围，请刷新");
    }

    private Assignment assignment(Long departmentId, String positionCode) {
        JsonNode department = department(departmentId);
        boolean centerEnabled = org.departments().stream().anyMatch(center -> Boolean.TRUE.equals(center.get("enabled"))
                && department.path("name").asText().equals(center.get("name")));
        if (!centerEnabled) throw bad("请选择已启用的正式中心对应部门");
        for (Map<String, Object> position : org.positions()) {
            if (clean(positionCode).equals(position.get("code")) && Boolean.TRUE.equals(position.get("enabled"))
                    && department.path("name").asText().equals(position.get("departmentName"))) {
                return new Assignment(departmentId, department.path("name").asText(), String.valueOf(position.get("name")));
            }
        }
        throw bad("请选择当前部门所属的正式岗位");
    }

    private void ensureUniqueDepartmentName(String name, long parent, Long excludedId) {
        for (JsonNode row : client.departments().path("department")) {
            if (name.equals(row.path("name").asText()) && parent == row.path("parentid").asLong()
                    && (excludedId == null || excludedId != row.path("id").asLong()))
                throw conflict("同级部门名称已存在，请刷新核对");
        }
    }

    private static String fingerprint(JsonNode node) {
        List<Long> ids = new ArrayList<>();
        for (JsonNode id : node.path("department")) ids.add(id.asLong());
        ids.sort(Long::compareTo);
        return hash(List.of(node.path("userid").asText(), node.path("name").asText(), ids,
                node.path("position").asText(), node.path("status").asInt(-1), node.path("enable").asInt(-1)));
    }

    private static String fingerprintDepartment(JsonNode node) {
        return hash(List.of(node.path("id").asLong(), node.path("name").asText(),
                node.path("parentid").asLong(), node.path("order").asLong()));
    }

    private static String hash(Object value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(new ObjectMapper().writeValueAsBytes(value))); }
        catch (Exception ex) { throw new IllegalStateException("无法生成变更校验"); }
    }

    private void requireReady() { if (!ready()) throw conflict("通讯录管理尚未启用"); }
    private static String memberId(String value) {
        String id = clean(value);
        if (!id.matches("[A-Za-z0-9_@.\\-]{1,64}")) throw bad("请选择有效成员");
        return id;
    }
    private static String name(String value) {
        String text = clean(value);
        if (text.isBlank() || text.length() > 64 || text.chars().anyMatch(Character::isISOControl)) throw bad("请填写不超过64字的有效名称");
        return text;
    }
    private static String clean(String value) { return value == null ? "" : value.trim(); }
    private static ResponseStatusException bad(String message) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, message); }
    private static ResponseStatusException conflict(String message) { return new ResponseStatusException(HttpStatus.CONFLICT, message); }
    private static ResponseStatusException pending() { return new ResponseStatusException(HttpStatus.BAD_GATEWAY, "操作已提交，通讯录尚未核对成功，请刷新核实，勿重复提交"); }

    private record Assignment(long departmentId, String departmentName, String positionName) {}
    public record Command(String action, String userId, String name, String mobile, String email, Long departmentId, String positionCode) {
        @Override public String toString() { return "Command[private fields redacted]"; }
    }
    public record Plan(String action, String targetId, String name, String departmentName, String positionName,
                       String positionCode, String before, ObjectNode payload, Instant expiresAt) {
        @Override public String toString() { return "Plan[private fields redacted]"; }
    }
}
