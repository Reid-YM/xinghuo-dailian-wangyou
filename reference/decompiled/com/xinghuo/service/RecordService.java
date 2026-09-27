/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.xinghuo.dto.ApiDtos$RecordPublicResponse
 *  com.xinghuo.exception.NotFoundException
 *  com.xinghuo.model.CustomerOrder
 *  com.xinghuo.model.Quote
 *  com.xinghuo.repository.CustomerOrderRepository
 *  com.xinghuo.repository.QuoteRepository
 *  com.xinghuo.service.RecordService
 *  com.xinghuo.service.RecordService$1
 *  lombok.Generated
 *  org.springframework.stereotype.Service
 *  org.springframework.transaction.annotation.Transactional
 */
package com.xinghuo.service;

import com.xinghuo.dto.ApiDtos;
import com.xinghuo.exception.NotFoundException;
import com.xinghuo.model.CustomerOrder;
import com.xinghuo.model.Quote;
import com.xinghuo.repository.CustomerOrderRepository;
import com.xinghuo.repository.QuoteRepository;
import com.xinghuo.service.RecordService;
import java.util.Locale;
import java.util.Optional;
import lombok.Generated;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RecordService {
    private final CustomerOrderRepository orders;
    private final QuoteRepository quotes;

    @Transactional(readOnly=true)
    public ApiDtos.RecordPublicResponse get(String rawNo) {
        Optional quote;
        Optional order;
        String no = this.normalize(rawNo);
        if (!this.validBusinessNo(no) && !this.validLegacyOrderNo(no)) {
            throw new NotFoundException();
        }
        Optional optional = order = this.validBusinessNo(no) ? this.orders.findByBusinessNo(no) : this.orders.findByOrderNo(no);
        if (order.isPresent()) {
            return this.fromOrder((CustomerOrder)order.get());
        }
        if (this.validBusinessNo(no) && (quote = this.quotes.findByBusinessNo(no)).isPresent()) {
            return this.fromQuote((Quote)quote.get());
        }
        throw new NotFoundException();
    }

    private ApiDtos.RecordPublicResponse fromQuote(Quote q) {
        String progress = switch (1.$SwitchMap$com$xinghuo$model$Quote$Status[q.getStatus().ordinal()]) {
            default -> throw new MatchException(null, null);
            case 1 -> "\u8be2\u4ef7\u5df2\u63d0\u4ea4\uff0c\u7b49\u5f85\u5ba2\u670d\u67e5\u770b";
            case 2 -> "\u5ba2\u670d\u5df2\u8054\u7cfb\uff0c\u6b63\u5728\u786e\u8ba4\u9700\u6c42";
            case 3 -> "\u5df2\u5b8c\u6210\u62a5\u4ef7\uff0c\u8bf7\u4e0e\u5ba2\u670d\u786e\u8ba4\u65b9\u6848";
            case 4 -> "\u5df2\u8f6c\u4e3a\u8ba2\u5355\uff0c\u6b63\u5728\u8fdb\u5165\u5c65\u7ea6\u6d41\u7a0b";
            case 5 -> "\u8be2\u4ef7\u5df2\u5173\u95ed";
            case 6 -> "\u8be2\u4ef7\u5df2\u53d6\u6d88";
        };
        return new ApiDtos.RecordPublicResponse(q.getBusinessNo(), "QUOTE", q.getStatus().name(), q.getGame() + " \u00b7 " + q.getDemand(), q.getExpectedTime(), null, progress, q.getCreatedAt(), q.getUpdatedAt(), false, null);
    }

    private ApiDtos.RecordPublicResponse fromOrder(CustomerOrder o) {
        Optional quote;
        Object title = "\u670d\u52a1\u8ba2\u5355";
        String expectedTime = null;
        if (o.getSourceQuoteId() != null && (quote = this.quotes.findById((Object)o.getSourceQuoteId())).isPresent()) {
            Quote q = (Quote)quote.get();
            title = q.getGame() + " \u00b7 " + q.getDemand();
            expectedTime = q.getExpectedTime();
        }
        return new ApiDtos.RecordPublicResponse(o.getBusinessNo(), "ORDER", o.getStatus().name(), (String)title, expectedTime, o.getProgressPercent(), o.getProgressText(), o.getCreatedAt(), o.getUpdatedAt(), o.getProgressImageAssetId() != null, o.getCustomerNote());
    }

    private String normalize(String s) {
        return s == null ? null : s.trim().toUpperCase(Locale.ROOT);
    }

    private boolean validBusinessNo(String s) {
        return s != null && s.matches("XH[A-Z0-9]{10}");
    }

    private boolean validLegacyOrderNo(String s) {
        return s != null && s.matches("XH-[A-Z2-9]{24}");
    }

    @Generated
    public RecordService(CustomerOrderRepository orders, QuoteRepository quotes) {
        this.orders = orders;
        this.quotes = quotes;
    }
}

