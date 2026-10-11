package com.xinghuo.record.infrastructure;

import com.xinghuo.exception.NotFoundException;
import com.xinghuo.order.infrastructure.CustomerOrder;
import com.xinghuo.order.infrastructure.CustomerOrderRepository;
import com.xinghuo.quote.infrastructure.Quote;
import com.xinghuo.quote.infrastructure.QuoteRepository;
import com.xinghuo.record.application.RecordPort;
import com.xinghuo.record.application.RecordPublicView;
import java.util.Locale;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Record-owned read adapter spanning quote and order persistence. */
@Component
public class RecordPersistenceAdapter implements RecordPort {
    private final CustomerOrderRepository orders;
    private final QuoteRepository quotes;

    public RecordPersistenceAdapter(CustomerOrderRepository orders, QuoteRepository quotes) {
        this.orders = orders;
        this.quotes = quotes;
    }

    @Override
    @Transactional(readOnly = true)
    public RecordPublicView get(String rawNo) {
        String no = normalize(rawNo);
        if (!validBusinessNo(no) && !validLegacyOrderNo(no)) {
            throw new NotFoundException();
        }

        Optional<CustomerOrder> order = validBusinessNo(no)
                ? orders.findByBusinessNo(no)
                : orders.findByOrderNo(no);
        if (order.isPresent()) {
            return fromOrder(order.get());
        }

        if (validBusinessNo(no)) {
            Optional<Quote> quote = quotes.findByBusinessNo(no);
            if (quote.isPresent()) {
                return fromQuote(quote.get());
            }
        }
        throw new NotFoundException();
    }

    private RecordPublicView fromQuote(Quote quote) {
        String progress = switch (quote.getStatus()) {
            case NEW -> "询价已提交，等待客服查看";
            case CONTACTED -> "客服已联系，正在确认需求";
            case QUOTED -> "已完成报价，请与客服确认方案";
            case CONVERTED -> "已转为订单，正在进入履约流程";
            case CLOSED -> "询价已关闭";
            case CANCELLED -> "询价已取消";
        };
        return new RecordPublicView(
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

    private RecordPublicView fromOrder(CustomerOrder order) {
        String title = "服务订单";
        String expectedTime = null;
        if (order.getSourceQuoteId() != null) {
            Optional<Quote> quote = quotes.findById(order.getSourceQuoteId());
            if (quote.isPresent()) {
                title = quote.get().getGame() + " · " + quote.get().getDemand();
                expectedTime = quote.get().getExpectedTime();
            }
        }
        return new RecordPublicView(
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
