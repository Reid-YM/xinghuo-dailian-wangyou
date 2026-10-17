package com.xinghuo.account.web;

import com.xinghuo.account.dto.AccountDtos;
import com.xinghuo.account.model.Account;
import com.xinghuo.account.service.AccountAuditService;
import com.xinghuo.account.service.AutomaticOrderAccountLinkService;
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

/** 订单归属：我的订单、自动归属修复、历史候选与人工认领兜底。 */
@RestController
@RequestMapping("/api/account/orders")
public class AccountOrdersApiController {

    private static final int MAX_PAGE_SIZE = 50;

    private final OrderLinkService orders;
    private final AccountAuditService audit;
    private final AutomaticOrderAccountLinkService automaticLinks;

    public AccountOrdersApiController(
            OrderLinkService orders,
            AccountAuditService audit,
            AutomaticOrderAccountLinkService automaticLinks) {
        this.orders = orders;
        this.audit = audit;
        this.automaticLinks = automaticLinks;
    }

    @GetMapping
    @RequireAccount
    public AccountDtos.OrderListResponse list(@RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "10") int size,
                                              HttpServletRequest request) {
        Account account = AccountAuthInterceptor.requireAccount(request);
        reconcile(account, request);
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
        reconcile(account, request);
        // 自动归属之后仍保留候选接口作为历史数据和客服介入场景的安全兜底。
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

    private void reconcile(Account account, HttpServletRequest request) {
        int linked = automaticLinks.reconcile(account);
        if (linked > 0) {
            audit.record(account.getId(), "ORDER_AUTO_LINK", "count=" + linked, request);
        }
    }
}
