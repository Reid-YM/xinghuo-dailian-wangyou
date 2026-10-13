package com.xinghuo.wecom;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

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

@Component
public class WeComClient {

    private static final String API_BASE =
            "https://qyapi.weixin.qq.com/cgi-bin";

    private final WeComProperties properties;
    private final ObjectMapper mapper;
    private final HttpClient http;

    private TokenState readToken;
    private TokenState writeToken;

    public WeComClient(
            WeComProperties properties,
            ObjectMapper mapper) {

        this.properties = properties;
        this.mapper = mapper;

        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    /**
     * 企业微信部门列表。
     */
    public JsonNode listDepartments() {
        ensureReadConfigured();

        Map<String, String> query =
                new LinkedHashMap<>();

        query.put(
                "access_token",
                getReadToken()
        );

        return get(
                "/department/list",
                query
        );
    }

    /**
     * 从根部门递归读取全部成员。
     */
    public JsonNode listUsers() {
        ensureReadConfigured();

        Map<String, String> query =
                new LinkedHashMap<>();

        query.put(
                "access_token",
                getReadToken()
        );

        query.put(
                "department_id",
                "1"
        );

        query.put(
                "fetch_child",
                "1"
        );

        return get(
                "/user/list",
                query
        );
    }

    /**
     * 读取单个企业微信成员。
     */
    public JsonNode getUser(String userId) {
        ensureReadConfigured();

        if (userId == null
                || userId.isBlank()) {

            throw new IllegalArgumentException(
                    "UserID 不能为空"
            );
        }

        Map<String, String> query =
                new LinkedHashMap<>();

        query.put(
                "access_token",
                getReadToken()
        );

        query.put(
                "userid",
                userId.trim()
        );

        return get(
                "/user/get",
                query
        );
    }

    public void createUser(JsonNode body) {
        postWrite("/user/create", body);
    }

    public void updateUser(JsonNode body) {
        postWrite("/user/update", body);
    }

    private synchronized String getWriteToken() {
        ensureWriteConfigured();
        if (writeToken != null && writeToken.valid()) return writeToken.value();
        JsonNode json = requestToken(properties.contactSecret());
        String token = json.path("access_token").asText("");
        if (token.isBlank()) throw new IllegalStateException("企业微信未返回写入凭证");
        long seconds = Math.max(60, json.path("expires_in").asLong(7200) - 300);
        writeToken = new TokenState(token, Instant.now().plusSeconds(seconds));
        return token;
    }

    private void postWrite(String path, JsonNode body) {
        String token = getWriteToken();
        URI uri = URI.create(API_BASE + path + "?access_token=" + encode(token));
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(uri)
                    .timeout(Duration.ofSeconds(15))
                    .header("Content-Type", "application/json; charset=utf-8")
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body), StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = http.send(request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("企业微信写入请求失败：HTTP " + response.statusCode());
            }
            JsonNode json = mapper.readTree(response.body());
            int errcode = json.path("errcode").asInt(-1);
            if (errcode != 0) {
                throw new IllegalStateException("企业微信写入失败：" + errcode + " "
                        + json.path("errmsg").asText("unknown error"));
            }
        } catch (IOException e) {
            throw new IllegalStateException("企业微信写入响应解析失败", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("企业微信写入请求被中断", e);
        }
    }

    public boolean readConfigured() {
        return properties.readConfigured();
    }

    public boolean writeConfigured() {
        return properties.writeConfigured();
    }

    private synchronized String getReadToken() {

        if (readToken != null
                && readToken.valid()) {

            return readToken.value();
        }

        JsonNode json =
                requestToken(
                        properties.appSecret()
                );

        String token =
                json.path(
                        "access_token"
                ).asText("");

        long expiresIn =
                json.path(
                        "expires_in"
                ).asLong(7200);

        if (token.isBlank()) {
            throw new IllegalStateException(
                    "企业微信未返回 access_token"
            );
        }

        /*
         * 提前 5 分钟失效，
         * 避免请求刚好撞上 token 过期。
         */
        long safeSeconds =
                Math.max(
                        60,
                        expiresIn - 300
                );

        readToken =
                new TokenState(
                        token,
                        Instant.now()
                                .plusSeconds(
                                        safeSeconds
                                )
                );

        return token;
    }

    private JsonNode requestToken(
            String secret) {

        Map<String, String> query =
                new LinkedHashMap<>();

        query.put(
                "corpid",
                properties.corpId()
        );

        query.put(
                "corpsecret",
                secret
        );

        return getRaw(
                "/gettoken",
                query
        );
    }

    private JsonNode get(
            String path,
            Map<String, String> query) {

        JsonNode json =
                getRaw(
                        path,
                        query
                );

        int errcode =
                json.path(
                        "errcode"
                ).asInt(0);

        if (errcode != 0) {

            String errmsg =
                    json.path(
                            "errmsg"
                    ).asText(
                            "unknown error"
                    );

            throw new IllegalStateException(
                    "企业微信 API 调用失败："
                            + errcode
                            + " "
                            + errmsg
            );
        }

        return json;
    }

    private JsonNode getRaw(
            String path,
            Map<String, String> query) {

        URI uri =
                URI.create(
                        API_BASE
                                + path
                                + "?"
                                + queryString(
                                        query
                                )
                );

        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(uri)
                        .timeout(
                                Duration.ofSeconds(
                                        15
                                )
                        )
                        .GET()
                        .build();

        try {

            HttpResponse<String> response =
                    http.send(
                            request,
                            HttpResponse.BodyHandlers
                                    .ofString(
                                            StandardCharsets
                                                    .UTF_8
                                    )
                    );

            if (response.statusCode() < 200
                    || response.statusCode() >= 300) {

                throw new IllegalStateException(
                        "企业微信 HTTP 请求失败："
                                + response.statusCode()
                );
            }

            JsonNode json =
                    mapper.readTree(
                            response.body()
                    );

            /*
             * gettoken 本身也要检查 errcode。
             */
            int errcode =
                    json.path(
                            "errcode"
                    ).asInt(0);

            if (errcode != 0) {

                String errmsg =
                        json.path(
                                "errmsg"
                        ).asText(
                                "unknown error"
                        );

                throw new IllegalStateException(
                        "企业微信 API 调用失败："
                                + errcode
                                + " "
                                + errmsg
                );
            }

            return json;

        } catch (IOException e) {

            throw new IllegalStateException(
                    "企业微信响应解析失败",
                    e
            );

        } catch (InterruptedException e) {

            Thread.currentThread()
                    .interrupt();

            throw new IllegalStateException(
                    "企业微信请求被中断",
                    e
            );
        }
    }

    private String queryString(
            Map<String, String> query) {

        StringBuilder result =
                new StringBuilder();

        for (Map.Entry<String, String> entry
                : query.entrySet()) {

            if (!result.isEmpty()) {
                result.append('&');
            }

            result.append(
                    encode(
                            entry.getKey()
                    )
            );

            result.append('=');

            result.append(
                    encode(
                            entry.getValue()
                    )
            );
        }

        return result.toString();
    }

    private String encode(
            String value) {

        return URLEncoder.encode(
                value == null
                        ? ""
                        : value,
                StandardCharsets.UTF_8
        );
    }

    private void ensureWriteConfigured() {
        if (!properties.writeConfigured()) {
            throw new IllegalStateException("企业微信通讯录写入配置尚未完成");
        }
    }

    private void ensureReadConfigured() {

        if (!properties.readConfigured()) {

            throw new IllegalStateException(
                    "企业微信读取配置尚未完成"
            );
        }
    }

    private record TokenState(
            String value,
            Instant expiresAt) {

        boolean valid() {
            return value != null
                    && !value.isBlank()
                    && expiresAt != null
                    && Instant.now()
                    .isBefore(
                            expiresAt
                    );
        }
    }
}
