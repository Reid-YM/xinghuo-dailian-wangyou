package com.xinghuo.quote.web;

import com.xinghuo.account.model.Account;
import com.xinghuo.account.service.AccountSessionService;
import com.xinghuo.account.service.QuoteContactService;
import com.xinghuo.dto.ApiDtos;
import com.xinghuo.dto.PublicQuoteRequest;
import com.xinghuo.exception.BadRequestException;
import com.xinghuo.quote.application.QuoteApplicationService;
import com.xinghuo.siteconfig.application.SiteRuntimeApplicationService;
import com.xinghuo.util.HashUtil;
import com.xinghuo.util.RequestUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Locale;
import java.util.Optional;
import lombok.Generated;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value={"/api/quote"})
public class PublicQuoteController {
    private final SiteRuntimeApplicationService runtimeConfig;
    private final QuoteApplicationService quotes;
    private final AccountSessionService accountSessions;
    private final QuoteContactService quoteContacts;

    @PostMapping
    ResponseEntity<ApiDtos.QuoteResponse> quote(@Valid @RequestBody PublicQuoteRequest body,
                                                @RequestHeader(value="Idempotency-Key") String key,
                                                HttpServletRequest req) {
        if (!this.runtimeConfig.quoteEnabled()) {
            throw new BadRequestException("当前已关闭网页询价，请通过客服渠道联系");
        }
        Optional<Account> current = this.accountSessions.current(req);
        Long accountId = current.map(Account::getId).orElse(null);
        PublicQuoteRequest payload = body;
        if (current.isPresent() && this.isBlankContact(body)) {
            Optional<QuoteContactService.StoredContact> preferred = this.quoteContacts.preferred(current.get());
            if (preferred.isPresent()) {
                QuoteContactService.StoredContact contact = preferred.get();
                payload = body.withCustomerContact(contact.value(), quoteContactMethod(contact.type()));
            }
        }
        ApiDtos.QuoteResponse out = QuoteWebMapper.toResponse(this.quotes.create(QuoteWebMapper.toCommand(
                payload,
                key,
                HashUtil.sha256(RequestUtil.clientIp(req)),
                accountId)));
        return ResponseEntity.status((HttpStatusCode) HttpStatus.CREATED).body(out);
    }

    private boolean isBlankContact(PublicQuoteRequest body) {
        return body.customerContact() == null || body.customerContact().isBlank();
    }

    private static String quoteContactMethod(String typeName) {
        if (typeName == null) {
            return null;
        }
        return switch (typeName.toUpperCase(Locale.ROOT)) {
            case "PHONE" -> "手机";
            case "WECHAT" -> "微信";
            case "QQ" -> "QQ";
            default -> null;
        };
    }

    @Generated
    public PublicQuoteController(SiteRuntimeApplicationService runtimeConfig,
                                 QuoteApplicationService quotes,
                                 AccountSessionService accountSessions,
                                 QuoteContactService quoteContacts) {
        this.runtimeConfig = runtimeConfig;
        this.quotes = quotes;
        this.accountSessions = accountSessions;
        this.quoteContacts = quoteContacts;
    }
}
