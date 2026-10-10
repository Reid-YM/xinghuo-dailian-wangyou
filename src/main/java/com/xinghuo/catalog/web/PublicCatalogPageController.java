package com.xinghuo.catalog.web;

import com.xinghuo.catalog.application.CatalogApplicationService;
import com.xinghuo.catalog.application.CatalogPageServiceView;
import com.xinghuo.page.web.PublicPageModelSupport;
import com.xinghuo.siteconfig.application.SiteRuntimeApplicationService;
import com.xinghuo.taxonomy.application.TaxonomyApplicationService;
import com.xinghuo.taxonomy.application.TaxonomyCatalogView;
import java.util.List;
import lombok.Generated;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class PublicCatalogPageController {
    private final CatalogApplicationService services;
    private final TaxonomyApplicationService taxonomy;
    private final SiteRuntimeApplicationService runtimeConfig;
    private final CatalogPagePresentation presentation;
    private final PublicPageModelSupport pageModel;

    @ModelAttribute
    void fillCommon(Model model) {
        this.pageModel.fill(model);
    }

    @GetMapping(value={"/"})
    String home(Model model) {
        model.addAttribute("acceptingOrders", this.acceptingOrders());
        return "index";
    }

    @GetMapping(value={"/services"})
    String services(Model model) {
        model.addAttribute("hallServices", this.services.publicPageItems());
        model.addAttribute("acceptingOrders", this.acceptingOrders());
        return "services";
    }

    @GetMapping(value={"/service/{slug}"})
    String serviceDetail(@PathVariable String slug, Model model) {
        CatalogPageServiceView service = this.services.publicPageBySlug(slug);
        TaxonomyCatalogView.GameView game = this.taxonomy.publicGames().stream()
                .filter(x -> x.getCode().equals(service.getGameCode()))
                .findFirst()
                .orElse(null);
        String gameName = game == null ? service.getGameCode() : game.getName();
        String h1 = gameName + " " + service.getName();
        String serviceDescription = gameName + service.getShortDesc() + "。按进度报价，网页不收账号密码。";
        List<CatalogPagePresentation.FaqItem> faqs = this.presentation.serviceFaqs(service, gameName);

        model.addAttribute("serviceSlug", slug);
        model.addAttribute("service", service);
        model.addAttribute("game", game);
        model.addAttribute("gameName", gameName);
        model.addAttribute("serviceH1", h1);
        model.addAttribute("serviceTitle", h1 + " | 星火网游代练平台");
        model.addAttribute("serviceDescription", serviceDescription);
        model.addAttribute("canonicalUrl", "https://wydlgf.cn/service/" + service.getSlug());
        model.addAttribute("ogImage", this.presentation.rasterOgImage(service.getCoverImage()));
        model.addAttribute("supportItems", this.presentation.supportItems(service));
        model.addAttribute("quoteFactors", this.presentation.quoteFactors(service));
        model.addAttribute("relatedServices", this.presentation.relatedServices(service));
        model.addAttribute("serviceFaqs", faqs);
        model.addAttribute("faqJsonLd", this.presentation.faqJsonLd(faqs));
        model.addAttribute("breadcrumbJsonLd", this.presentation.serviceBreadcrumbJsonLd(gameName, service));
        model.addAttribute("productJsonLd", this.presentation.productJsonLd(service, h1, serviceDescription));
        return "service-detail";
    }

    @GetMapping(value={"/game/{seoSlug}"})
    String gameDetail(@PathVariable String seoSlug, Model model) {
        TaxonomyCatalogView.GameView game = this.taxonomy.publicGameBySeoSlug(seoSlug);
        model.addAttribute("game", game);
        model.addAttribute("gameServices", this.services.publicPageItems().stream()
                .filter(x -> x.getGameCode().equals(game.getCode()))
                .toList());
        model.addAttribute("canonicalUrl", "https://wydlgf.cn/game/" + seoSlug);
        model.addAttribute("acceptingOrders", this.acceptingOrders());
        model.addAttribute("gameOgImage", this.presentation.rasterOgImage(game.getCoverImage()));
        model.addAttribute("breadcrumbJsonLd", this.presentation.gameBreadcrumbJsonLd(game));
        return "game";
    }

    private boolean acceptingOrders() {
        return this.runtimeConfig.acceptingOrders();
    }

    @Generated
    public PublicCatalogPageController(CatalogApplicationService services,
                                       TaxonomyApplicationService taxonomy,
                                       SiteRuntimeApplicationService runtimeConfig,
                                       CatalogPagePresentation presentation,
                                       PublicPageModelSupport pageModel) {
        this.services = services;
        this.taxonomy = taxonomy;
        this.runtimeConfig = runtimeConfig;
        this.presentation = presentation;
        this.pageModel = pageModel;
    }
}
