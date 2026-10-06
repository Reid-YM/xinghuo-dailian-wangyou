package com.xinghuo.account.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinghuo.account.model.Account;
import com.xinghuo.account.repo.AccountRepository;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 账号的「询价联系方式」。
 *
 * <p>这些联系方式只用于客服回访，与登录凭据（contact_type / contact_value）完全分开：
 * 按需求允许免验证绑定，因此绝不能拿它做身份判定或登录匹配。
 *
 * <p>账号可绑定多条（手机号 / 微信号 / QQ 号各一条即可），整体读写，
 * 存储形态是 customer_accounts.quote_contacts 里的 JSON 数组。
 */
@Service
public class QuoteContactService {

    /** 单账号最多绑定几条，避免把这里当成通讯录。 */
    public static final int MAX_CONTACTS = 3;

    private static final int MAX_VALUE_LENGTH = 120;

    /** 手机号：中国大陆 11 位。 */
    private static final Pattern PHONE = Pattern.compile("1[3-9]\\d{9}");
    /** 微信号：6-20 位，字母开头，可含字母数字下划线连字符。 */
    private static final Pattern WECHAT = Pattern.compile("[A-Za-z][A-Za-z0-9_-]{5,19}");
    /** QQ 号：5-12 位，不以 0 开头。 */
    private static final Pattern QQ = Pattern.compile("[1-9]\\d{4,11}");

    private static final TypeReference<List<StoredContact>> LIST_TYPE = new TypeReference<>() {};

    private final AccountRepository accounts;
    private final ObjectMapper json;

    public QuoteContactService(AccountRepository accounts, ObjectMapper json) {
        this.accounts = accounts;
        this.json = json;
    }

    /** 已绑定的联系方式，按绑定顺序返回；载荷损坏时按空处理而不是抛错。 */
    public List<StoredContact> list(Account account) {
        String raw = account == null ? null : account.getQuoteContacts();
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        try {
            List<StoredContact> parsed = json.readValue(raw, LIST_TYPE);
            return parsed == null ? List.of() : parsed.stream().filter(StoredContact::isUsable).toList();
        } catch (Exception ignored) {
            return List.of();
        }
    }

    /**
     * 询价时优先使用的联系方式：先看绑定的第一条，
     * 否则在登录凭据本身就是手机/微信/QQ 时复用（邮箱是找回方式，不适合回访）。
     */
    public Optional<StoredContact> preferred(Account account) {
        List<StoredContact> bound = list(account);
        if (!bound.isEmpty()) {
            return Optional.of(bound.get(0));
        }
        if (account == null) {
            return Optional.empty();
        }
        Account.ContactType type = account.getContactType();
        if (type == Account.ContactType.PHONE || type == Account.ContactType.WECHAT
                || type == Account.ContactType.QQ) {
            String value = account.getContactValue() == null ? "" : account.getContactValue().trim();
            if (!value.isEmpty()) {
                return Optional.of(new StoredContact(type.name(), value));
            }
        }
        return Optional.empty();
    }

    /** 追加一条；重复或格式不合法直接抛 400，不做静默过滤。 */
    @Transactional
    public Account add(Account account, String type, String value) {
        List<StoredContact> next = new ArrayList<>(list(account));
        next.add(new StoredContact(type, value));
        return save(account, next);
    }

    /** 按绑定顺序删除第 index 条。 */
    @Transactional
    public Account removeAt(Account account, int index) {
        List<StoredContact> current = new ArrayList<>(list(account));
        if (index < 0 || index >= current.size()) {
            throw AccountException.badRequest("QUOTE_CONTACT_NOT_FOUND", "要删除的联系方式不存在");
        }
        current.remove(index);
        return save(account, current);
    }

    private Account save(Account account, List<StoredContact> incoming) {
        List<StoredContact> normalized = validateAndNormalize(incoming);
        try {
            account.setQuoteContacts(normalized.isEmpty() ? null : json.writeValueAsString(normalized));
        } catch (Exception e) {
            throw AccountException.badRequest("QUOTE_CONTACT_SERIALIZE_FAILED", "联系方式保存失败");
        }
        return accounts.save(account);
    }

    private List<StoredContact> validateAndNormalize(List<StoredContact> incoming) {
        if (incoming == null || incoming.isEmpty()) {
            return List.of();
        }
        if (incoming.size() > MAX_CONTACTS) {
            throw AccountException.badRequest("QUOTE_CONTACT_LIMIT",
                    "最多绑定 " + MAX_CONTACTS + " 条询价联系方式");
        }
        List<StoredContact> result = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (StoredContact raw : incoming) {
            Account.ContactType type = parseType(raw == null ? null : raw.type());
            String value = raw == null || raw.value() == null ? "" : raw.value().trim();
            if (value.isEmpty()) {
                throw AccountException.badRequest("QUOTE_CONTACT_EMPTY", "请填写联系方式");
            }
            if (value.length() > MAX_VALUE_LENGTH) {
                throw AccountException.badRequest("QUOTE_CONTACT_TOO_LONG", "联系方式过长");
            }
            // 「不能瞎填」：按类型做严格格式校验，不做真实性验证
            switch (type) {
                case PHONE -> {
                    if (!PHONE.matcher(value.replaceAll("[\\s\\-()]", "")).matches()) {
                        throw AccountException.badRequest("QUOTE_CONTACT_INVALID_PHONE",
                                "手机号格式不正确，请填写 11 位中国大陆手机号");
                    }
                }
                case WECHAT -> {
                    if (!WECHAT.matcher(value).matches()) {
                        throw AccountException.badRequest("QUOTE_CONTACT_INVALID_WECHAT",
                                "微信号格式不正确：6-20 位，字母开头，只能包含字母、数字、下划线或连字符");
                    }
                }
                case QQ -> {
                    if (!QQ.matcher(value).matches()) {
                        throw AccountException.badRequest("QUOTE_CONTACT_INVALID_QQ",
                                "QQ 号格式不正确：5-12 位数字，且不以 0 开头");
                    }
                }
                default -> throw AccountException.badRequest("QUOTE_CONTACT_INVALID_TYPE",
                        "只支持手机号 / 微信号 / QQ 号");
            }
            String norm = ContactNormalizer.normalize(type, value);
            if (!seen.add(type.name() + ":" + norm)) {
                throw AccountException.badRequest("QUOTE_CONTACT_DUPLICATE", "同一种联系方式不要重复填写");
            }
            result.add(new StoredContact(type.name(), value));
        }
        return result;
    }

    private Account.ContactType parseType(String raw) {
        String value = raw == null ? "" : raw.trim().toUpperCase(java.util.Locale.ROOT);
        try {
            Account.ContactType type = Account.ContactType.valueOf(value);
            return type == Account.ContactType.EMAIL ? null : type;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** 落库形态；type 为 PHONE / WECHAT / QQ。 */
    public record StoredContact(String type, String value) {
        boolean isUsable() {
            return type != null && !type.isBlank() && value != null && !value.isBlank();
        }
    }
}
