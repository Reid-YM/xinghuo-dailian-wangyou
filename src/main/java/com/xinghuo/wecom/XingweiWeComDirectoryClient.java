package com.xinghuo.wecom;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * 企业微信通讯录只读客户端。
 *
 * <p>仅包含 gettoken、department/list、user/list、user/get 和 OAuth user/getuserinfo 的 GET 能力；
 * 不实现成员创建、更新、删除、停用等写接口。
 */
@Component
public class XingweiWeComDirectoryClient {
    private static final String API_BASE = "https://qyapi.weixin.qq.com/cgi-bin";

    private final XingweiWeComProperties properties;
    private final ObjectMapper mapper;
    private final HttpClient http;
    private TokenState tokenState;

    public XingweiWeComDirectoryClient(XingweiWeComProperties properties, ObjectMapper mapper) {
        this.properties = properties;
        this.mapper = mapper;
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    }

    public boolean configured() {
        return properties.configured();
    }

    public JsonNode listDepartments() {
        ensureConfigured();
        Map<String, String> query = new LinkedHashMap<>();
        query.put("access_token", accessToken());
        return get("/department/list", query);
    }

    public JsonNode listUsers() {
        ensureConfigured();
        Map<String, String> query = new LinkedHashMap<>();
        query.put("access_token", accessToken());
        query.put("department_id", "1");
        query.put("fetch_child", "1");
        return get("/user/list", query);
    }

    /** 用一次性 OAuth code 换取企业成员 UserID；不接受非成员 OpenID。 */
    public String userIdForCode(String code) {
        ensureConfigured();
        Map<String, String> query = new LinkedHashMap<>();
        query.put("access_token", accessToken());
        query.put("code", code);
        JsonNode response = get("/user/getuserinfo", query);
        String userId = response.path("UserId").asText("").trim();
        if (userId.isBlank() || userId.length() > 64) {
            throw new IllegalStateException("企业微信未返回有效成员身份");
        }
        return userId;
    }

    /** Confirm the member remains visible to this app and read the exact current title. */
    public Member member(String userId) {
        ensureConfigured();
        Map<String, String> query = new LinkedHashMap<>();
        query.put("access_token", accessToken());
        query.put("userid", userId);
        JsonNode response = get("/user/get", query);
        if (!userId.equals(response.path("userid").asText(""))) {
            throw new IllegalStateException("企业微信成员身份不一致");
        }
        return new Member(userId, response.path("name").asText(userId).trim(),
                response.path("position").asText("").trim());
    }

    public record Member(String userId, String name, String position) {}

    private synchronized String accessToken() {
        if (tokenState != null && tokenState.valid()) {
            return tokenState.value();
        }
        Map<String, String> query = new LinkedHashMap<>();
        query.put("corpid", properties.corpId());
        query.put("corpsecret", properties.appSecret());
        JsonNode json = getRaw("/gettoken", query);
        String token = json.path("access_token").asText("");
        if (token.isBlank()) {
            throw new IllegalStateException("企业微信未返回 access_token");
        }
        long safeSeconds = Math.max(60, json.path("expires_in").asLong(7200) - 300);
        tokenState = new TokenState(token, Instant.now().plusSeconds(safeSeconds));
        return token;
    }

    private JsonNode get(String path, Map<String, String> query) {
        JsonNode json = getRaw(path, query);
        int errcode = json.path("errcode").asInt(0);
        if (errcode != 0) {
            throw new IllegalStateException("企业微信 API 调用失败：" + errcode + " "
                    + json.path("errmsg").asText("unknown error"));
        }
        return json;
    }

    private JsonNode getRaw(String path, Map<String, String> query) {
        URI uri = URI.create(API_BASE + path + "?" + queryString(query));
        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .timeout(Duration.ofSeconds(15))
                .GET()
                .build();
        try {
            HttpResponse<String> response = http.send(
                    request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("企业微信 HTTP 请求失败：" + response.statusCode());
            }
            JsonNode json = mapper.readTree(response.body());
            int errcode = json.path("errcode").asInt(0);
            if (errcode != 0) {
                throw new IllegalStateException("企业微信 API 调用失败：" + errcode + " "
                        + json.path("errmsg").asText("unknown error"));
            }
            return json;
        } catch (IOException e) {
            throw new IllegalStateException("企业微信响应解析失败", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("企业微信请求被中断", e);
        }
    }

    private String queryString(Map<String, String> query) {
        StringBuilder result = new StringBuilder();
        for (Map.Entry<String, String> entry : query.entrySet()) {
            if (!result.isEmpty()) {
                result.append('&');
            }
            result.append(encode(entry.getKey())).append('=').append(encode(entry.getValue()));
        }
        return result.toString();
    }

    private String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    private void ensureConfigured() {
        if (!properties.configured()) {
            throw new IllegalStateException("企业微信只读通讯录尚未配置");
        }
    }

    private record TokenState(String value, Instant expiresAt) {
        boolean valid() {
            return value != null && !value.isBlank() && expiresAt != null && Instant.now().isBefore(expiresAt);
        }
    }
}
