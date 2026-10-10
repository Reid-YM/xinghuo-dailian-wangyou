package com.xinghuo.page.web;

import java.util.Locale;
import lombok.Generated;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;

@Controller
public class StaticPageController {
    private final PublicPageModelSupport pageModel;

    @ModelAttribute
    void fillCommon(Model model) {
        this.pageModel.fill(model);
    }

    @GetMapping(value={"/contact"})
    String contact() {
        return "contact";
    }

    @GetMapping(value={"/rules"})
    String rules(Locale locale) {
        return Locale.ENGLISH.getLanguage().equals(locale.getLanguage()) ? "rules-en" : "rules";
    }

    @GetMapping(value={"/privacy"})
    String privacy(Locale locale) {
        return Locale.ENGLISH.getLanguage().equals(locale.getLanguage()) ? "privacy-en" : "privacy";
    }

    @GetMapping(value={"/rules-en"})
    String rulesEn() {
        return "rules-en";
    }

    @GetMapping(value={"/privacy-en"})
    String privacyEn() {
        return "privacy-en";
    }

    @GetMapping(value={"/trust"})
    String trust() {
        return "trust";
    }

    @Generated
    public StaticPageController(PublicPageModelSupport pageModel) {
        this.pageModel = pageModel;
    }
}
