package com.xinghuo.wecom;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class WeComProperties {

    private final String corpId;
    private final String appSecret;
    private final String contactSecret;

    public WeComProperties(
            @Value("${wecom.corp-id:}") String corpId,
            @Value("${wecom.app-secret:}") String appSecret,
            @Value("${wecom.contact-secret:}") String contactSecret) {

        this.corpId = clean(corpId);
        this.appSecret = clean(appSecret);
        this.contactSecret = clean(contactSecret);
    }

    public String corpId() {
        return corpId;
    }

    /**
     * 自建应用 Secret：
     * 当前用于读取成员详情、成员列表、部门等。
     */
    public String appSecret() {
        return appSecret;
    }

    /**
     * 通讯录同步 Secret：
     * 仅用于新增、调岗、离职停用等写操作。
     */
    public String contactSecret() {
        return contactSecret;
    }

    public boolean readConfigured() {
        return !corpId.isBlank()
                && !appSecret.isBlank();
    }

    public boolean writeConfigured() {
        return !corpId.isBlank()
                && !contactSecret.isBlank();
    }

    private static String clean(String value) {
        return value == null
                ? ""
                : value.trim();
    }
}
