package com.xinghuo.account.web;

import com.xinghuo.account.service.AccountException;
import com.xinghuo.account.service.RateLimiter;
import com.xinghuo.account.service.RequestUtils;
import com.xinghuo.account.service.WeChatMiniBridgeService;
import com.xinghuo.dto.ApiDtos;
import com.xinghuo.dto.PublicQuoteRequest;
import com.xinghuo.exception.BadRequestException;
import com.xinghuo.model.Quote;
import com.xinghuo.repository.QuoteRepository;
import com.xinghuo.service.QuoteService;
import com.xinghuo.service.SiteConfigService;
import com.xinghuo.util.HashUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 小程序正式网站询价 API：用户身份由微信一次性登录码验证。
 * 不接受客户端提供的 OpenID、用户 ID、金额或管理员权限。
 * 创建的是网站询价记录，不代表自动报价已确认或支付完成。
 */
@RestController
@RequestMapping("/api/account/wechat-mini")
public class MiniQuoteApiController {
    private final WeChatMiniBridgeService bridge;
    private final QuoteService quotes;
    private final QuoteRepository quoteRepo;
    private final SiteConfigService settings;
    private final RateLimiter limiter;

    public MiniQuoteApiController(WeChatMiniBridgeService bridge, QuoteService quotes,
                                  QuoteRepository quoteRepo, SiteConfigService settings,
                                  RateLimiter limiter) {
        this.bridge = bridge;
        this.quotes = quotes;
        this.quoteRepo = quoteRepo;
        this.settings = settings;
        this.limiter = limiter;
    }

    public record MiniQuoteRequest(@NotBlank String loginCode,
                                   @Valid @NotNull PublicQuoteRequest quote) {}
    public record MiniLoginRequest(@NotBlank String loginCode) {}
    public record MiniQuoteItem(String businessNo, String game, String demand,
                                String status, Instant createdAt, Instant updatedAt) {
        public static MiniQuoteItem from(Quote quote) {
            return new MiniQuoteItem(quote.getBusinessNo(), quote.getGame(),
                    quote.getDemand(), quote.getStatus().name(),
                    quote.getCreatedAt(), quote.getUpdatedAt());
        }
    }

    @PostMapping("/quote")
    public ResponseEntity<ApiDtos.QuoteResponse> submit(
            @Valid @RequestBody MiniQuoteRequest body,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            HttpServletRequest request) {
        limiter.check("mini:quote:ip:" + RequestUtils.clientIp(request), 10, Duration.ofMinutes(15));
        if (!settings.isQuoteEnabled()) {
            throw new BadRequestException("网站当前未开放询价，请联系客服");
        }
        if (idempotencyKey == null || !idempotencyKey.matches("mini-[0-9a-f]{48}")) {
            throw AccountException.badRequest("INVALID_QUOTE_KEY", "订单请求标识不合法");
        }
        Long accountId = bridge.requireLinkedAccountId(body.loginCode());
        ApiDtos.QuoteResponse response = quotes.create(body.quote(), idempotencyKey,
                HashUtil.sha256(RequestUtils.clientIp(request)), accountId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/my-quotes")
    public List<MiniQuoteItem> myQuotes(@Valid @RequestBody MiniLoginRequest body,
                                        HttpServletRequest request) {
        limiter.check("mini:quotes:ip:" + RequestUtils.clientIp(request), 30, Duration.ofMinutes(15));
        Long accountId = bridge.requireLinkedAccountId(body.loginCode());
        return quoteRepo.findTop50ByCustomerAccountIdOrderByUpdatedAtDesc(accountId)
                .stream().map(MiniQuoteItem::from).toList();
    }
}
