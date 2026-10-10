package com.xinghuo.toolstation.web;

import com.xinghuo.page.web.PublicPageModelSupport;
import lombok.Generated;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;

@Controller
public class MasteryPageController {
    private final PublicPageModelSupport pageModel;

    @ModelAttribute
    void fillCommon(Model model) {
        this.pageModel.fill(model);
    }

    @GetMapping(value={"/mastery"})
    String mastery() {
        return "mw4-camo-tracker";
    }

    @Generated
    public MasteryPageController(PublicPageModelSupport pageModel) {
        this.pageModel = pageModel;
    }
}
