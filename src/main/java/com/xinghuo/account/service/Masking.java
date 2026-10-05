package com.xinghuo.account.service;

import com.xinghuo.account.model.Account;

/**
 * 脱敏工具。
 *
 * <p>出现脱敏需求的地方只有两类：给用户看的列表（订单候选、会话列表）和审计日志。
 * 两处都必须走这里，不要在业务代码里手写字符串截取。
 */
public final class Masking {

    private Masking() {
    }

    /** 联系方式脱敏：微信号/QQ 保留首尾，手机号保留前 3 后 2。 */
    public static String contact(Account.ContactType type, String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String v = value.trim();
        if (type == Account.ContactType.PHONE) {
            if (v.length() <= 5) {
                return "***";
            }
            return v.substring(0, 3) + "****" + v.substring(v.length() - 2);
        }
        if (v.length() <= 2) {
            return "*";
        }
        if (v.length() <= 4) {
            return v.charAt(0) + "**" + v.charAt(v.length() - 1);
        }
        return v.substring(0, 2) + "****" + v.substring(v.length() - 2);
    }

    /** 邮箱脱敏：保留首字符与域名。 */
    public static String email(String email) {
        if (email == null || email.isBlank()) {
            return "";
        }
        String v = email.trim();
        int at = v.indexOf('@');
        if (at <= 0) {
            return "***";
        }
        String local = v.substring(0, at);
        String domain = v.substring(at);
        String head = local.substring(0, 1);
        return head + "***" + domain;
    }

    /** 订单号脱敏：保留前 6 后 2，用于「可认领订单」候选列表。 */
    public static String orderNo(String orderNo) {
        if (orderNo == null || orderNo.length() <= 8) {
            return "****";
        }
        return orderNo.substring(0, 6) + "****" + orderNo.substring(orderNo.length() - 2);
    }

    /** 金额脱敏：只保留数量级，用于候选列表。 */
    public static String amount(String amountText) {
        if (amountText == null || amountText.isBlank()) {
            return "***";
        }
        return "¥***";
    }

    /** IP 脱敏：IPv4 只保留前两段；IPv6 只保留前两组。 */
    public static String ip(String ip) {
        if (ip == null || ip.isBlank()) {
            return "未知网络";
        }
        String value = ip.trim();
        if (value.contains(".")) {
            String[] parts = value.split("\\.");
            if (parts.length == 4) {
                return parts[0] + "." + parts[1] + ".*.*";
            }
        }
        if (value.contains(":")) {
            String[] parts = value.split(":", -1);
            String first = parts.length > 0 && !parts[0].isBlank() ? parts[0] : "*";
            String second = parts.length > 1 && !parts[1].isBlank() ? parts[1] : "*";
            return first + ":" + second + ":*";
        }
        return "***";
    }
}
