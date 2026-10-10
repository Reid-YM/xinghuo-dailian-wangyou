package com.xinghuo.order.web;

import com.xinghuo.page.web.PublicPageModelSupport;
import lombok.Generated;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class OrderPageController {
    private final PublicPageModelSupport pageModel;

    @ModelAttribute
    void fillCommon(Model model) {
        this.pageModel.fill(model);
    }

    @GetMapping(value={"/records"})
    String records() {
        return "records";
    }

    @GetMapping(value={"/record/{businessNo}"})
    String recordDetail(@PathVariable String businessNo, Model model) {
        model.addAttribute("businessNo", businessNo);
        return "record-detail";
    }

    @GetMapping(value={"/order"})
    String order() {
        return "order";
    }

    @GetMapping(value={"/order/{orderNo}"})
    String legacyOrderDetail(@PathVariable String orderNo, Model model) {
        model.addAttribute("businessNo", orderNo);
        return "record-detail";
    }

    @Generated
    public OrderPageController(PublicPageModelSupport pageModel) {
        this.pageModel = pageModel;
    }
}
