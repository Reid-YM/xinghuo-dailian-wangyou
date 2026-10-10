package com.xinghuo.order.web;

import com.xinghuo.order.application.OrderApplicationService;
import java.util.List;
import java.util.Map;
import lombok.Generated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value={"/api/public/cases"})
public class PublicOrderCaseController {
    private final OrderApplicationService orders;

    @GetMapping
    List<Map<String, Object>> publicCases() {
        return OrderWebMapper.toCaseResponses(this.orders.publicCases());
    }

    @Generated
    public PublicOrderCaseController(OrderApplicationService orders) {
        this.orders = orders;
    }
}
