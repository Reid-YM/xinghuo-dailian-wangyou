package com.xinghuo.quote.web;

import com.xinghuo.page.web.PublicPageModelSupport;
import lombok.Generated;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;

@Controller
public class PublicQuotePageController {
    private final PublicPageModelSupport pageModel;

    @ModelAttribute
    void fillCommon(Model model) {
        this.pageModel.fill(model);
    }

    @GetMapping(value={"/quote"})
    String quote() {
        return "quote";
    }

    @Generated
    public PublicQuotePageController(PublicPageModelSupport pageModel) {
        this.pageModel = pageModel;
    }
}
