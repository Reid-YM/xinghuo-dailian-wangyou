package com.xinghuo.account.service;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * 密码策略。
 *
 * <p>上限 72 字节不是随意定的：BCrypt 只取前 72 字节，超出部分被静默丢弃。
 * 若不拦住，用户以为设了 100 位长密码，实际只有前 72 字节生效。
 */
public final class PasswordPolicy {

    /** BCrypt 的有效输入上限。 */
    public static final int MAX_BYTES = 72;

    private static final int MIN_LENGTH = 8;

    private PasswordPolicy() {
    }

    /**
     * 校验密码强度：至少 8 位，且至少包含两类字符（小写/大写/数字/符号）。
     *
     * @param password  明文密码
     * @param forbidden 禁止与密码相同的值（通常是用户的联系方式或显示名）
     */
    public static void validate(String password, String... forbidden) {
        if (password == null || password.isEmpty()) {
            throw AccountException.badRequest("WEAK_PASSWORD", "请填写密码");
        }
        if (password.length() < MIN_LENGTH) {
            throw AccountException.badRequest("WEAK_PASSWORD", "密码至少 8 位");
        }
        if (password.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES) {
            throw AccountException.badRequest("WEAK_PASSWORD", "密码过长，请控制在 72 字节以内");
        }
        int kinds = 0;
        if (password.chars().anyMatch(Character::isLowerCase)) {
            kinds++;
        }
        if (password.chars().anyMatch(Character::isUpperCase)) {
            kinds++;
        }
        if (password.chars().anyMatch(Character::isDigit)) {
            kinds++;
        }
        if (password.chars().anyMatch(c -> !Character.isLetterOrDigit(c))) {
            kinds++;
        }
        if (kinds < 2) {
            throw AccountException.badRequest("WEAK_PASSWORD", "密码需包含字母、数字、符号中的至少两类");
        }
        if (forbidden != null) {
            String lower = password.toLowerCase(Locale.ROOT);
            for (String f : forbidden) {
                if (f != null && !f.isBlank() && lower.equals(f.trim().toLowerCase(Locale.ROOT))) {
                    throw AccountException.badRequest("WEAK_PASSWORD", "密码不能与联系方式相同");
                }
            }
        }
    }
}
