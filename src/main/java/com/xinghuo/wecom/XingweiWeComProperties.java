package com.xinghuo.wecom;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** V2G 企业微信只读通讯录配置。没有任何写入 Secret。 */
@Component
public class XingweiWeComProperties {
    private final String corpId;
    private final String appSecret;

    public XingweiWeComProperties(
            @Value("${wecom.corp-id:}") String corpId,
            @Value("${wecom.app-secret:}") String appSecret) {
        this.corpId = clean(corpId);
        this.appSecret = clean(appSecret);
    }

    public String corpId() {
        return corpId;
    }

    public String appSecret() {
        return appSecret;
    }

    public boolean configured() {
        return !corpId.isBlank() && !appSecret.isBlank();
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
