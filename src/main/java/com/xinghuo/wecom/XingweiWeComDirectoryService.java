package com.xinghuo.wecom;

import com.fasterxml.jackson.databind.JsonNode;
import com.xinghuo.service.XingweiOrgRbacService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/** V2G：实时企业微信员工/部门 + V63 正式岗位，只读聚合。 */
@Service
public class XingweiWeComDirectoryService {
    private final XingweiWeComDirectoryClient client;
    private final XingweiOrgRbacService org;

    public XingweiWeComDirectoryService(
            XingweiWeComDirectoryClient client,
            XingweiOrgRbacService org) {
        this.client = client;
        this.org = org;
    }

    public Map<String, Object> snapshot() {
        List<Map<String, Object>> canonicalPositions = org.positions();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("configured", client.configured());
        result.put("readOnly", true);
        result.put("source", "WECOM_LIVE + XW_V63");
        result.put("positions", canonicalPositions);

        if (!client.configured()) {
            result.put("employees", List.of());
            result.put("departments", List.of());
            result.put("counts", counts(0, 0, canonicalPositions.size()));
            return result;
        }

        JsonNode departmentResponse = client.listDepartments();
        List<Map<String, Object>> departments = departments(departmentResponse);
        Map<Long, String> departmentNames = new HashMap<>();
        for (Map<String, Object> department : departments) {
            departmentNames.put(((Number) department.get("id")).longValue(), String.valueOf(department.get("name")));
        }
        List<Map<String, Object>> employees = employees(client.listUsers(), departmentNames);

        result.put("employees", employees);
        result.put("departments", departments);
        result.put("counts", counts(employees.size(), departments.size(), canonicalPositions.size()));
        return result;
    }

    private Map<String, Object> counts(int employees, int departments, int positions) {
        Map<String, Object> counts = new LinkedHashMap<>();
        counts.put("employees", employees);
        counts.put("departments", departments);
        counts.put("positions", positions);
        return counts;
    }

    private List<Map<String, Object>> departments(JsonNode response) {
        List<Map<String, Object>> result = new ArrayList<>();
        JsonNode rows = response.path("department");
        if (!rows.isArray()) {
            return result;
        }
        for (JsonNode row : rows) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", row.path("id").asLong());
            item.put("parentId", row.path("parentid").asLong());
            item.put("name", row.path("name").asText(""));
            item.put("order", row.path("order").asLong());
            result.add(item);
        }
        return result;
    }

    private List<Map<String, Object>> employees(JsonNode response, Map<Long, String> departmentNames) {
        List<Map<String, Object>> result = new ArrayList<>();
        JsonNode rows = response.path("userlist");
        if (!rows.isArray()) {
            return result;
        }
        for (JsonNode row : rows) {
            List<Long> departmentIds = new ArrayList<>();
            List<String> departments = new ArrayList<>();
            JsonNode ids = row.path("department");
            if (ids.isArray()) {
                for (JsonNode idNode : ids) {
                    long id = idNode.asLong();
                    departmentIds.add(id);
                    departments.add(departmentNames.getOrDefault(id, "未知部门 #" + id));
                }
            }
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("userId", row.path("userid").asText(""));
            item.put("name", row.path("name").asText(""));
            item.put("position", row.path("position").asText(""));
            item.put("status", row.path("status").asInt(0));
            item.put("departmentIds", departmentIds);
            item.put("departments", departments);
            result.add(item);
        }
        return result;
    }
}
