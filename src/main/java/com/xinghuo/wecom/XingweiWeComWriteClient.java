package com.xinghuo.wecom;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/** Dedicated directory writer: never shares the application's read/SSO token. */
@Component
public class XingweiWeComWriteClient {
    private final Environment environment;
    private final ObjectMapper mapper;
    private final HttpClient http;
    private final URI base;
    private String token;
    private Instant tokenExpires = Instant.EPOCH;

    @Autowired
    public XingweiWeComWriteClient(Environment environment, ObjectMapper mapper) {
        this(environment, mapper, URI.create("https://qyapi.weixin.qq.com/cgi-bin"));
    }

    XingweiWeComWriteClient(Environment environment, ObjectMapper mapper, URI base) {
        this.environment = environment;
        this.mapper = mapper;
        this.base = base;
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    }

    public boolean ready() {
        return environment.getProperty("wecom.hr-write-enabled", Boolean.class, false)
                && !value("wecom.corp-id").isBlank() && !value("wecom.write-secret").isBlank();
    }

    public JsonNode departments() { return get("/department/list"); }
    public JsonNode member(String userId) { return get("/user/get", "userid=" + encode(userId)); }
    public JsonNode createMember(JsonNode body) { return post("/user/create", body); }
    public JsonNode updateMember(JsonNode body) { return post("/user/update", body); }
    public JsonNode createDepartment(JsonNode body) { return post("/department/create", body); }
    public JsonNode updateDepartment(JsonNode body) { return post("/department/update", body); }

    private JsonNode get(String path) { return get(path, ""); }

    private JsonNode get(String path, String query) {
        String suffix = query.isEmpty() ? "" : "&" + query;
        return send(path + "?access_token=" + encode(accessToken()) + suffix, null);
    }

    private JsonNode post(String path, JsonNode body) {
        return send(path + "?access_token=" + encode(accessToken()), body);
    }

    private synchronized String accessToken() {
        if (!ready()) throw new ResponseStatusException(HttpStatus.CONFLICT, "通讯录管理尚未启用");
        if (token != null && Instant.now().isBefore(tokenExpires)) return token;
        JsonNode result = send("/gettoken?corpid=" + encode(value("wecom.corp-id"))
                + "&corpsecret=" + encode(value("wecom.write-secret")), null);
        String candidate = result.path("access_token").asText("");
        long seconds = result.path("expires_in").asLong(0);
        if (candidate.isBlank() || seconds < 1) throw unavailable(false);
        token = candidate;
        tokenExpires = Instant.now().plusSeconds(Math.max(1, seconds - Math.min(300, seconds / 2)));
        return token;
    }

    private JsonNode send(String relative, JsonNode body) {
        boolean write = body != null;
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(base + relative))
                    .timeout(Duration.ofSeconds(15)).header("Accept", "application/json");
            if (write) builder.header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)));
            else builder.GET();
            HttpResponse<String> response = http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) throw unavailable(write);
            JsonNode result = mapper.readTree(response.body());
            if (result == null || !result.isObject()) throw unavailable(write);
            if (!result.path("errcode").isIntegralNumber()) throw unavailable(write);
            int code = result.path("errcode").asInt(-1);
            if (code != 0) {
                if (code == 40014 || code == 42001) {
                    synchronized (this) { token = null; tokenExpires = Instant.EPOCH; }
                }
                // Do not propagate vendor errmsg, request URLs, contacts or tokens.
                throw unavailable(write);
            }
            return result;
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw unavailable(write);
        } catch (Exception ex) {
            // The original exception may contain a credential-bearing URI.
            throw unavailable(write);
        }
    }

    private static ResponseStatusException unavailable(boolean write) {
        return new ResponseStatusException(HttpStatus.BAD_GATEWAY, write
                ? "企业微信未确认操作结果，请先刷新通讯录核实，勿重复提交"
                : "企业微信通讯录暂时无法读取，请检查管理授权后刷新");
    }

    private String value(String key) { return environment.getProperty(key, "").trim(); }
    private static String encode(String value) { return URLEncoder.encode(value, StandardCharsets.UTF_8); }
}
