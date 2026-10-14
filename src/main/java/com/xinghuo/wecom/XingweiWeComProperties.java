package com.xinghuo.wecom;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** 企业微信自建应用配置；Secret 只从服务端环境变量注入。 */
@Component
public class XingweiWeComProperties {
    private final String corpId;
    private final String appSecret;
    private final String agentId;
    private final boolean ssoEnabled;

    public XingweiWeComProperties(
            @Value("${wecom.corp-id:}") String corpId,
            @Value("${wecom.app-secret:}") String appSecret,
            @Value("${wecom.agent-id:}") String agentId,
            @Value("${wecom.sso-enabled:false}") boolean ssoEnabled) {
        this.corpId = clean(corpId);
        this.appSecret = clean(appSecret);
        this.agentId = clean(agentId);
        this.ssoEnabled = ssoEnabled;
    }

    public String corpId() {
        return corpId;
    }

    public String appSecret() {
        return appSecret;
    }

    public String agentId() {
        return agentId;
    }

    public boolean ssoReady() {
        return ssoEnabled && configured() && agentId.matches("[0-9]{1,20}");
    }

    public boolean configured() {
        return !corpId.isBlank() && !appSecret.isBlank();
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
