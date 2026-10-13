package com.xinghuo.wecom;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class WeComHrService {

    private final WeComClient client;
    private final ObjectMapper mapper;

    /**
     * 固定155职务体系。
     *
     * 数据来自：
     * classpath:wecom/positions.json
     */
    private final JsonNode positions;

    public WeComHrService(
            WeComClient client,
            ObjectMapper mapper) {

        this.client = client;
        this.mapper = mapper;
        this.positions = loadPositions(mapper);
    }

    /**
     * 企业微信实时成员列表。
     *
     * 这里使用自建应用 Secret 的只读能力。
     */
    public EmployeeListResponse employees() {

        JsonNode departmentResponse =
                client.listDepartments();

        Map<Long, String> departmentNames =
                departmentNameMap(
                        departmentResponse
                );

        JsonNode userResponse =
                client.listUsers();

        List<EmployeeItem> items =
                new ArrayList<>();

        JsonNode users =
                userResponse.path("userlist");

        if (users.isArray()) {

            for (JsonNode user : users) {

                List<Long> departmentIds =
                        longList(
                                user.path(
                                        "department"
                                )
                        );

                List<String> names =
                        departmentIds.stream()
                                .map(id ->
                                        departmentNames
                                                .getOrDefault(
                                                        id,
                                                        "未知部门ID "
                                                                + id
                                                )
                                )
                                .toList();

                items.add(
                        new EmployeeItem(
                                text(
                                        user,
                                        "userid"
                                ),
                                text(
                                        user,
                                        "name"
                                ),
                                text(
                                        user,
                                        "position"
                                ),
                                text(
                                        user,
                                        "mobile"
                                ),
                                text(
                                        user,
                                        "email"
                                ),
                                text(
                                        user,
                                        "biz_mail"
                                ),
                                user.path(
                                        "status"
                                ).asInt(0),
                                departmentIds,
                                names
                        )
                );
            }
        }

        return new EmployeeListResponse(
                client.readConfigured(),
                client.writeConfigured(),
                items.size(),
                items
        );
    }

    /**
     * 企业微信实时部门结构。
     */
    public DepartmentListResponse departments() {

        JsonNode response =
                client.listDepartments();

        List<DepartmentItem> items =
                new ArrayList<>();

        JsonNode departments =
                response.path(
                        "department"
                );

        if (departments.isArray()) {

            for (JsonNode dep : departments) {

                items.add(
                        new DepartmentItem(
                                dep.path(
                                        "id"
                                ).asLong(),
                                dep.path(
                                        "parentid"
                                ).asLong(),
                                text(
                                        dep,
                                        "name"
                                ),
                                dep.path(
                                        "order"
                                ).asLong()
                        )
                );
            }
        }

        return new DepartmentListResponse(
                items.size(),
                items
        );
    }

    /**
     * 星维唯一正式职务体系。
     *
     * 当前只读取，不允许网页端修改。
     */
    public JsonNode positions() {
        return positions.deepCopy();
    }

    public void createEmployee(NewEmployeeRequest request) {
        if (request == null) throw bad("请填写员工资料");
        String userId = clean(request.userId());
        String name = clean(request.name());
        String position = clean(request.position());
        String mobile = clean(request.mobile());
        String email = clean(request.email());
        if (!userId.matches("[A-Za-z0-9_.-]{1,64}")) throw bad("UserID 只能包含字母、数字、点、下划线和连字符，最长64位");
        if (name.isBlank() || name.length() > 64) throw bad("员工姓名不能为空且不能超过64字");
        if (mobile.isBlank() && email.isBlank()) throw bad("手机号或邮箱至少填写一项");
        if (!email.isBlank() && !email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")) throw bad("邮箱格式错误");
        long departmentId = checkedDepartment(request.departmentId());
        checkedPosition(position);
        ObjectNode body = mapper.createObjectNode();
        body.put("userid", userId);
        body.put("name", name);
        body.put("position", position);
        body.put("enable", 1);
        ArrayNode departments = body.putArray("department");
        departments.add(departmentId);
        if (!mobile.isBlank()) body.put("mobile", mobile);
        if (!email.isBlank()) body.put("email", email);
        client.createUser(body);
    }

    public void transferEmployee(String userId, AssignmentRequest request) {
        if (request == null) throw bad("请填写调岗资料");
        String id = clean(userId);
        if (id.isBlank()) throw bad("UserID 不能为空");
        String position = clean(request.position());
        long departmentId = checkedDepartment(request.departmentId());
        checkedPosition(position);
        client.getUser(id);
        ObjectNode body = mapper.createObjectNode();
        body.put("userid", id);
        body.put("position", position);
        body.putArray("department").add(departmentId);
        client.updateUser(body);
    }

    /** 离职只停用成员账号，保留通讯录记录以便恢复。 */
    public void disableEmployee(String userId) {
        String id = clean(userId);
        if (id.isBlank()) throw bad("UserID 不能为空");
        client.getUser(id);
        ObjectNode body = mapper.createObjectNode();
        body.put("userid", id);
        body.put("enable", 0);
        client.updateUser(body);
    }

    private long checkedDepartment(Long id) {
        if (id == null || id < 1) throw bad("请选择部门");
        for (JsonNode item : client.listDepartments().path("department")) {
            if (item.path("id").asLong() == id) return id;
        }
        throw bad("部门不存在，请刷新后重试");
    }

    private void checkedPosition(String position) {
        if (position.isBlank()) throw bad("请选择正式职务");
        var centers = positions.fields();
        while (centers.hasNext()) {
            for (JsonNode job : centers.next().getValue()) {
                if (position.equals(job.asText())) return;
            }
        }
        throw bad("职务不在正式职务表中");
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private ResponseStatusException bad(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    public record NewEmployeeRequest(String userId, String name, String mobile,
                                     String email, Long departmentId, String position) {}
    public record AssignmentRequest(Long departmentId, String position) {}

    private Map<Long, String> departmentNameMap(
            JsonNode response) {

        Map<Long, String> result =
                new HashMap<>();

        JsonNode departments =
                response.path(
                        "department"
                );

        if (!departments.isArray()) {
            return result;
        }

        for (JsonNode dep : departments) {

            result.put(
                    dep.path(
                            "id"
                    ).asLong(),
                    text(
                            dep,
                            "name"
                    )
            );
        }

        return result;
    }

    private List<Long> longList(
            JsonNode node) {

        List<Long> result =
                new ArrayList<>();

        if (!node.isArray()) {
            return result;
        }

        for (JsonNode value : node) {

            result.add(
                    value.asLong()
            );
        }

        return result;
    }

    private String text(
            JsonNode node,
            String field) {

        JsonNode value =
                node.get(field);

        if (value == null
                || value.isNull()) {

            return "";
        }

        return value.asText("");
    }

    private JsonNode loadPositions(
            ObjectMapper mapper) {

        ClassPathResource resource =
                new ClassPathResource(
                        "wecom/positions.json"
                );

        try (
                InputStream input =
                        resource.getInputStream()
        ) {

            JsonNode json =
                    mapper.readTree(
                            input
                    );

            validatePositions(json);

            return json;

        } catch (IOException e) {

            throw new IllegalStateException(
                    "无法读取正式职务配置",
                    e
            );
        }
    }

    /**
     * 应用启动时再次验证职务数量。
     *
     * 避免后续误修改 positions.json，
     * 导致正式职务体系悄悄发生变化。
     */
    private void validatePositions(
            JsonNode json) {

        if (json == null
                || !json.isObject()) {

            throw new IllegalStateException(
                    "正式职务配置格式错误"
            );
        }

        int total = 0;

        var fields =
                json.fields();

        while (fields.hasNext()) {

            Map.Entry<String, JsonNode> entry =
                    fields.next();

            JsonNode jobs =
                    entry.getValue();

            if (!jobs.isArray()) {

                throw new IllegalStateException(
                        "职务中心格式错误："
                                + entry.getKey()
                );
            }

            total += jobs.size();
        }

        if (total != 155) {

            throw new IllegalStateException(
                    "正式职务数量异常，应为155，实际为"
                            + total
            );
        }
    }

    public record EmployeeListResponse(
            boolean readConfigured,
            boolean writeConfigured,
            int total,
            List<EmployeeItem> items) {
    }

    public record EmployeeItem(
            String userId,
            String name,
            String position,
            String mobile,
            String email,
            String businessEmail,
            int status,
            List<Long> departmentIds,
            List<String> departments) {
    }

    public record DepartmentListResponse(
            int total,
            List<DepartmentItem> items) {
    }

    public record DepartmentItem(
            long id,
            long parentId,
            String name,
            long order) {
    }
}
