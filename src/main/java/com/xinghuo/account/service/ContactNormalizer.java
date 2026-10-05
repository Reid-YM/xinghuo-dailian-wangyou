package com.xinghuo.account.service;

import com.xinghuo.account.model.Account;
import java.util.Locale;

/**
 * 联系方式规范化与格式校验。
 *
 * <p>规范化值单独存一列（*_norm）并建唯一索引，是账号查重与「历史订单认领」
 * 匹配的基础。规则一旦上线就不能随意改动 —— 改动会让既有账号的唯一索引
 * 与匹配结果失真，需要配套数据订正。
 */
public final class ContactNormalizer {

    private ContactNormalizer() {
    }

    /**
     * 规范化联系方式。
     *
     * <p>手机号去掉空格、连字符与 +86 前缀；QQ 只保留数字；微信号统一小写
     * （微信 ID 不区分大小写，但用户输入习惯不一致）。
     */
    public static String normalize(Account.ContactType type, String raw) {
        if (raw == null) {
            return "";
        }
        String v = raw.trim();
        if (type == null) {
            return v.toLowerCase(Locale.ROOT);
        }
        switch (type) {
            case EMAIL:
                return normalizeEmail(v);
            case PHONE:
                v = v.replaceAll("[\\s\\-()]", "");
                if (v.startsWith("+86")) {
                    v = v.substring(3);
                } else if (v.startsWith("86") && v.length() > 11) {
                    v = v.substring(2);
                }
                return v;
            case QQ:
                return v.replaceAll("[^0-9]", "");
            case WECHAT:
            default:
                return v.toLowerCase(Locale.ROOT);
        }
    }

    /** 邮箱规范化：去空格 + 小写。 */
    public static String normalizeEmail(String raw) {
        if (raw == null) {
            return null;
        }
        String v = raw.trim().toLowerCase(Locale.ROOT);
        return v.isEmpty() ? null : v;
    }

    /**
     * 格式校验。只做「明显不合法」的拦截，不做真实性验证 ——
     * 微信号没有公开的格式规范，过度校验只会把真实用户挡在门外。
     */
    public static void validate(Account.ContactType type, String raw) {
        String v = raw == null ? "" : raw.trim();
        if (v.isEmpty()) {
            throw AccountException.badRequest("INVALID_CONTACT", "请填写联系方式");
        }
        if (v.length() > 120) {
            throw AccountException.badRequest("INVALID_CONTACT", "联系方式过长");
        }
        if (type == null) {
            throw AccountException.badRequest("INVALID_CONTACT", "请选择联系方式类型");
        }
        switch (type) {
            case EMAIL:
                validateEmail(v);
                break;
            case PHONE:
                String phone = normalize(type, v);
                if (!phone.matches("1[3-9]\\d{9}")) {
                    throw AccountException.badRequest("INVALID_CONTACT", "请填写有效的手机号");
                }
                break;
            case QQ:
                String qq = normalize(type, v);
                if (!qq.matches("[1-9]\\d{4,11}")) {
                    throw AccountException.badRequest("INVALID_CONTACT", "请填写有效的 QQ 号");
                }
                break;
            case WECHAT:
            default:
                if (v.length() < 2) {
                    throw AccountException.badRequest("INVALID_CONTACT", "请填写有效的微信号");
                }
                break;
        }
    }

    /** 基础邮箱格式校验；真实性靠验证邮件确认。 */
    public static void validateEmail(String raw) {
        String v = raw == null ? "" : raw.trim();
        if (v.isEmpty()) {
            throw AccountException.badRequest("INVALID_EMAIL", "请填写邮箱");
        }
        if (v.length() > 160 || !v.matches("^[^@\\s]+@[^@\\s.]+(\\.[^@\\s.]+)+$")) {
            throw AccountException.badRequest("INVALID_EMAIL", "请填写有效的邮箱地址");
        }
    }
}
