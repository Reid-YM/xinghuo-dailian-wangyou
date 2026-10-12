package com.xinghuo.service;

import com.xinghuo.dto.ApiDtos;
import com.xinghuo.exception.NotFoundException;
import com.xinghuo.model.CustomerOrder;
import com.xinghuo.model.Quote;
import com.xinghuo.repository.CustomerOrderRepository;
import com.xinghuo.repository.QuoteRepository;
import java.util.Locale;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RecordService {
    private final CustomerOrderRepository orders;
    private final QuoteRepository quotes;

    public RecordService(CustomerOrderRepository orders, QuoteRepository quotes) {
        this.orders = orders;
        this.quotes = quotes;
    }

    @Transactional(readOnly = true)
    public ApiDtos.RecordPublicResponse get(String rawNo) {
        String no = this.normalize(rawNo);
        boolean businessNo = this.validBusinessNo(no);
        if (!businessNo && !this.validLegacyOrderNo(no)) {
            throw new NotFoundException();
        }

        Optional<CustomerOrder> order = businessNo
                ? this.orders.findByBusinessNo(no)
                : this.orders.findByOrderNo(no);
        if (order.isPresent()) {
            return this.fromOrder(order.get());
        }

        if (businessNo) {
            Optional<Quote> quote = this.quotes.findByBusinessNo(no);
            if (quote.isPresent()) {
                return this.fromQuote(quote.get());
            }
        }
        throw new NotFoundException();
    }

    private ApiDtos.RecordPublicResponse fromQuote(Quote quote) {
        String progress = switch (quote.getStatus()) {
            case NEW -> "询价已提交，等待客服查看";
            case CONTACTED -> "客服已联系，正在确认需求";
            case QUOTED -> "已完成报价，请与客服确认方案";
            case CONVERTED -> "已转为订单，正在进入履约流程";
            case CLOSED -> "询价已关闭";
            case CANCELLED -> "询价已取消";
        };
        return new ApiDtos.RecordPublicResponse(
                quote.getBusinessNo(),
                "QUOTE",
                quote.getStatus().name(),
                quote.getGame() + " · " + quote.getDemand(),
                quote.getExpectedTime(),
                null,
                progress,
                quote.getCreatedAt(),
                quote.getUpdatedAt(),
                false,
                null);
    }

    private ApiDtos.RecordPublicResponse fromOrder(CustomerOrder order) {
        String title = "服务订单";
        String expectedTime = null;
        if (order.getSourceQuoteId() != null) {
            Optional<Quote> quote = this.quotes.findById(order.getSourceQuoteId());
            if (quote.isPresent()) {
                Quote sourceQuote = quote.get();
                title = sourceQuote.getGame() + " · " + sourceQuote.getDemand();
                expectedTime = sourceQuote.getExpectedTime();
            }
        }
        return new ApiDtos.RecordPublicResponse(
                order.getBusinessNo(),
                "ORDER",
                order.getStatus().name(),
                title,
                expectedTime,
                order.getProgressPercent(),
                order.getProgressText(),
                order.getCreatedAt(),
                order.getUpdatedAt(),
                order.getProgressImageAssetId() != null,
                order.getCustomerNote());
    }

    private String normalize(String value) {
        return value == null ? null : value.trim().toUpperCase(Locale.ROOT);
    }

    private boolean validBusinessNo(String value) {
        return value != null && value.matches("XH[A-Z0-9]{10}");
    }

    private boolean validLegacyOrderNo(String value) {
        return value != null && value.matches("XH-[A-Z2-9]{24}");
    }
}
