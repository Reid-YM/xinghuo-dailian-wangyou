package com.xinghuo.account.web;

import com.xinghuo.account.dto.AccountDtos;
import com.xinghuo.account.model.Account;
import com.xinghuo.account.service.AccountAuditService;
import com.xinghuo.account.service.OrderLinkService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 订单归属：我的订单、可认领候选、认领。 */
@RestController
@RequestMapping("/api/account/orders")
public class AccountOrdersApiController {

    private static final int MAX_PAGE_SIZE = 50;

    private final OrderLinkService orders;
    private final AccountAuditService audit;

    public AccountOrdersApiController(OrderLinkService orders, AccountAuditService audit) {
        this.orders = orders;
        this.audit = audit;
    }

    @GetMapping
    @RequireAccount
    public AccountDtos.OrderListResponse list(@RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "10") int size,
                                              HttpServletRequest request) {
        Account account = AccountAuthInterceptor.requireAccount(request);
        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), MAX_PAGE_SIZE);
        List<AccountDtos.OrderItem> items = orders.listOrders(account.getId(), safePage, safeSize)
                .stream()
                .map(AccountDtos.OrderItem::of)
                .toList();
        return new AccountDtos.OrderListResponse(
                items, orders.countOrders(account.getId()), safePage, safeSize);
    }

    @GetMapping("/candidates")
    @RequireAccount
    public List<AccountDtos.OrderItem> candidates(HttpServletRequest request) {
        Account account = AccountAuthInterceptor.requireAccount(request);
        // 候选是「可能属于你」的历史订单，businessNo 只回脱敏值（D-1）；
        // 认领成功后才把完整单号给本人。
        return orders.listCandidates(account).stream()
                .map(AccountDtos.OrderItem::ofMasked)
                .toList();
    }

    @PostMapping("/claim")
    @RequireAccount
    public AccountDtos.OrderItem claim(@Valid @RequestBody AccountDtos.ClaimRequest body,
                                       HttpServletRequest request) {
        Account account = AccountAuthInterceptor.requireAccount(request);
        OrderLinkService.OrderRow row = orders.claim(account, body.businessNo());
        // 归属于敏感操作：记下账号、订单号与来源（订单号不是个人信息，可入日志）
        audit.record(account.getId(), "ORDER_CLAIM", "orderNo=" + row.orderNo(), request);
        return AccountDtos.OrderItem.of(row);
    }
}
