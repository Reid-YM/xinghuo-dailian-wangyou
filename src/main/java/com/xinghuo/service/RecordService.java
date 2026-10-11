package com.xinghuo.service;

import com.xinghuo.dto.ApiDtos;
import com.xinghuo.exception.NotFoundException;
import com.xinghuo.model.CustomerOrder;
import com.xinghuo.model.Quote;
import com.xinghuo.repository.CustomerOrderRepository;
import com.xinghuo.repository.QuoteRepository;
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
        String progress = switch (q.getStatus()) {
            case NEW -> "询价已提交，等待客服查看";
            case CONTACTED -> "客服已联系，正在确认需求";
            case QUOTED -> "已完成报价，请与客服确认方案";
            case CONVERTED -> "已转为订单，正在进入履约流程";
            case CLOSED -> "询价已关闭";
            case CANCELLED -> "询价已取消";
        };
        return new ApiDtos.RecordPublicResponse(q.getBusinessNo(), "QUOTE", q.getStatus().name(), q.getGame() + " \u00b7 " + q.getDemand(), q.getExpectedTime(), null, progress, q.getCreatedAt(), q.getUpdatedAt(), false, null);
    }

    private ApiDtos.RecordPublicResponse fromOrder(CustomerOrder o) {
        Optional quote;
        Object title = "\u670d\u52a1\u8ba2\u5355";
        String expectedTime = null;
        if (o.getSourceQuoteId() != null && (quote = this.quotes.findById(o.getSourceQuoteId())).isPresent()) {
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

