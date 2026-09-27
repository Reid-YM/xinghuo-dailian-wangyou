/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.xinghuo.dto.CustomerDtos$CustomerDashboardResponse
 *  com.xinghuo.dto.CustomerDtos$CustomerLoginRequest
 *  com.xinghuo.dto.CustomerDtos$CustomerProfileResponse
 *  com.xinghuo.dto.CustomerDtos$CustomerRecordSummary
 *  com.xinghuo.dto.CustomerDtos$CustomerRegisterRequest
 *  com.xinghuo.exception.BadRequestException
 *  com.xinghuo.model.CustomerAccount
 *  com.xinghuo.model.CustomerAccount$Role
 *  com.xinghuo.model.CustomerOrder
 *  com.xinghuo.model.Quote
 *  com.xinghuo.repository.CustomerAccountRepository
 *  com.xinghuo.repository.CustomerOrderRepository
 *  com.xinghuo.repository.QuoteRepository
 *  com.xinghuo.service.CustomerAccountService
 *  com.xinghuo.service.CustomerAccountService$1
 *  jakarta.servlet.http.HttpServletRequest
 *  jakarta.servlet.http.HttpSession
 *  lombok.Generated
 *  org.springframework.security.crypto.password.PasswordEncoder
 *  org.springframework.stereotype.Service
 *  org.springframework.transaction.annotation.Transactional
 */
package com.xinghuo.service;

import com.xinghuo.dto.CustomerDtos;
import com.xinghuo.exception.BadRequestException;
import com.xinghuo.model.CustomerAccount;
import com.xinghuo.model.CustomerOrder;
import com.xinghuo.model.Quote;
import com.xinghuo.repository.CustomerAccountRepository;
import com.xinghuo.repository.CustomerOrderRepository;
import com.xinghuo.repository.QuoteRepository;
import com.xinghuo.service.CustomerAccountService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import lombok.Generated;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerAccountService {
    public static final String SESSION_KEY = "CUSTOMER_ACCOUNT_ID";
    private static final String LOGIN_FAILS_KEY = "CUSTOMER_LOGIN_FAILS";
    private static final int MAX_LOGIN_FAILS = 8;
    private final CustomerAccountRepository accounts;
    private final QuoteRepository quotes;
    private final CustomerOrderRepository orders;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public CustomerDtos.CustomerProfileResponse register(CustomerDtos.CustomerRegisterRequest r, HttpServletRequest req) {
        String method = this.cleanMethod(r.contactMethod());
        String value = this.cleanContact(r.contactValue());
        this.validatePassword(r.password());
        this.accounts.findByContactMethodAndContactValue(method, value).ifPresent(x -> {
            throw new BadRequestException("\u8be5\u8054\u7cfb\u65b9\u5f0f\u5df2\u6ce8\u518c\uff0c\u8bf7\u76f4\u63a5\u767b\u5f55\u3002");
        });
        Instant now = Instant.now();
        CustomerAccount account = CustomerAccount.builder().displayName(this.trim(r.displayName(), 80)).contactMethod(method).contactValue(value).passwordHash(this.passwordEncoder.encode((CharSequence)r.password())).role(CustomerAccount.Role.BUYER).levelCode("NEW").enabled(true).createdAt(now).updatedAt(now).build();
        this.accounts.save((Object)account);
        this.bindSession(req, account.getId());
        return this.profile(account);
    }

    @Transactional
    public CustomerDtos.CustomerProfileResponse login(CustomerDtos.CustomerLoginRequest r, HttpServletRequest req) {
        this.guardLoginAttempts(req);
        CustomerAccount account = (CustomerAccount)this.accounts.findByContactMethodAndContactValue(this.cleanMethod(r.contactMethod()), this.cleanContact(r.contactValue())).orElseThrow(() -> {
            this.markLoginFailure(req);
            return new BadRequestException("\u8d26\u53f7\u6216\u5bc6\u7801\u4e0d\u6b63\u786e\u3002");
        });
        if (!account.isEnabled() || !this.passwordEncoder.matches((CharSequence)r.password(), account.getPasswordHash())) {
            this.markLoginFailure(req);
            throw new BadRequestException("\u8d26\u53f7\u6216\u5bc6\u7801\u4e0d\u6b63\u786e\u3002");
        }
        account.setLastLoginAt(Instant.now());
        account.setUpdatedAt(Instant.now());
        this.bindSession(req, account.getId());
        return this.profile(account);
    }

    public void logout(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }

    @Transactional(readOnly=true)
    public Optional<CustomerAccount> current(HttpServletRequest req) {
        Object id;
        HttpSession session = req.getSession(false);
        Object object = id = session == null ? null : session.getAttribute(SESSION_KEY);
        if (!(id instanceof Long)) {
            return Optional.empty();
        }
        Long accountId = (Long)id;
        return this.accounts.findById((Object)accountId).filter(CustomerAccount::isEnabled);
    }

    @Transactional(readOnly=true)
    public Optional<CustomerDtos.CustomerDashboardResponse> dashboard(HttpServletRequest req) {
        Optional found = this.current(req);
        if (found.isEmpty()) {
            return Optional.empty();
        }
        CustomerAccount account = (CustomerAccount)found.get();
        ArrayList<CustomerDtos.CustomerRecordSummary> rows = new ArrayList<CustomerDtos.CustomerRecordSummary>();
        for (CustomerOrder o : this.orders.findTop50ByCustomerAccountIdOrderByUpdatedAtDesc(account.getId())) {
            rows.add(new CustomerDtos.CustomerRecordSummary(o.getBusinessNo(), "ORDER", o.getStatus().name(), "\u670d\u52a1\u8ba2\u5355", o.getProgressPercent(), o.getProgressText(), o.getUpdatedAt()));
        }
        for (Quote q : this.quotes.findTop50ByCustomerAccountIdOrderByUpdatedAtDesc(account.getId())) {
            rows.add(new CustomerDtos.CustomerRecordSummary(q.getBusinessNo(), "QUOTE", q.getStatus().name(), q.getGame() + " \u00b7 " + q.getDemand(), null, this.quoteProgress(q), q.getUpdatedAt()));
        }
        rows.sort(Comparator.comparing(CustomerDtos.CustomerRecordSummary::updatedAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed());
        return Optional.of(new CustomerDtos.CustomerDashboardResponse(this.profile(account), rows.stream().limit(50L).toList()));
    }

    public CustomerDtos.CustomerProfileResponse profile(CustomerAccount account) {
        return new CustomerDtos.CustomerProfileResponse(account.getId(), account.getDisplayName(), account.getContactMethod(), this.mask(account.getContactValue()), account.getRole().name(), account.getLevelCode(), account.getCreatedAt());
    }

    private String quoteProgress(Quote q) {
        return switch (1.$SwitchMap$com$xinghuo$model$Quote$Status[q.getStatus().ordinal()]) {
            default -> throw new MatchException(null, null);
            case 1 -> "\u8be2\u4ef7\u5df2\u63d0\u4ea4\uff0c\u7b49\u5f85\u5ba2\u670d\u786e\u8ba4";
            case 2 -> "\u5ba2\u670d\u5df2\u8054\u7cfb";
            case 3 -> "\u5df2\u5b8c\u6210\u62a5\u4ef7";
            case 4 -> "\u5df2\u8f6c\u4e3a\u8ba2\u5355";
            case 5 -> "\u8be2\u4ef7\u5df2\u5173\u95ed";
            case 6 -> "\u8be2\u4ef7\u5df2\u53d6\u6d88";
        };
    }

    private String cleanMethod(String s) {
        String v = this.trim(s, 40);
        if (!List.of("\u5fae\u4fe1", "QQ", "\u624b\u673a", "\u5176\u4ed6").contains(v)) {
            throw new BadRequestException("\u8bf7\u9009\u62e9\u6b63\u786e\u7684\u8054\u7cfb\u65b9\u5f0f\u3002");
        }
        return v;
    }

    private String cleanContact(String s) {
        String v = this.trim(s, 120);
        if (v.length() < 3) {
            throw new BadRequestException("\u8054\u7cfb\u53f7\u8fc7\u77ed\u3002");
        }
        return v;
    }

    private void validatePassword(String password) {
        if (password == null || password.length() < 8) {
            throw new BadRequestException("\u5bc6\u7801\u81f3\u5c11\u9700\u8981 8 \u4f4d\u3002");
        }
        if (password.length() > 72) {
            throw new BadRequestException("\u5bc6\u7801\u957f\u5ea6\u4e0d\u80fd\u8d85\u8fc7 72 \u4f4d\u3002");
        }
        if (password.isBlank()) {
            throw new BadRequestException("\u5bc6\u7801\u4e0d\u80fd\u5168\u4e3a\u7a7a\u683c\u3002");
        }
    }

    private void bindSession(HttpServletRequest req, Long accountId) {
        HttpSession old = req.getSession(false);
        if (old != null) {
            old.invalidate();
        }
        req.getSession(true).setAttribute(SESSION_KEY, (Object)accountId);
    }

    private void guardLoginAttempts(HttpServletRequest req) {
        int fails;
        HttpSession session = req.getSession(true);
        Object raw = session.getAttribute(LOGIN_FAILS_KEY);
        if (raw instanceof Integer) {
            Integer n = (Integer)raw;
            v0 = n;
        } else {
            v0 = fails = 0;
        }
        if (fails >= 8) {
            throw new BadRequestException("\u767b\u5f55\u5c1d\u8bd5\u6b21\u6570\u8fc7\u591a\uff0c\u8bf7\u7a0d\u540e\u518d\u8bd5\u3002");
        }
    }

    private void markLoginFailure(HttpServletRequest req) {
        int n;
        HttpSession session = req.getSession(true);
        Object raw = session.getAttribute(LOGIN_FAILS_KEY);
        if (raw instanceof Integer) {
            Integer n2 = (Integer)raw;
            n = n2;
        } else {
            n = 0;
        }
        int fails = n;
        session.setAttribute(LOGIN_FAILS_KEY, (Object)(fails + 1));
    }

    private String trim(String s, int max) {
        if (s == null || s.isBlank()) {
            throw new BadRequestException("\u8bf7\u586b\u5199\u5b8c\u6574\u4fe1\u606f\u3002");
        }
        String v = s.trim();
        return v.length() > max ? v.substring(0, max) : v;
    }

    private String mask(String s) {
        if (s == null || s.length() <= 4) {
            return "****";
        }
        return s.substring(0, 2) + "****" + s.substring(s.length() - 2);
    }

    @Generated
    public CustomerAccountService(CustomerAccountRepository accounts, QuoteRepository quotes, CustomerOrderRepository orders, PasswordEncoder passwordEncoder) {
        this.accounts = accounts;
        this.quotes = quotes;
        this.orders = orders;
        this.passwordEncoder = passwordEncoder;
    }
}

