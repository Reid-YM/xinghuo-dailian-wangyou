package com.xinghuo.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.xinghuo.account.web.RequireAccount;
import com.xinghuo.model.GameCatalog;
import com.xinghuo.model.ServiceItem;
import com.xinghuo.service.ServiceCatalogService;
import java.time.LocalDate;
import com.xinghuo.service.SiteConfigService;
import com.xinghuo.service.TaxonomyService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.Generated;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class PageController {
    private final ServiceCatalogService services;
    private final TaxonomyService taxonomy;
    private final ObjectMapper objectMapper;
    private final SiteConfigService config;
    private static final String DEFAULT_WECHAT = "AAC9878";
    private static final List<String> PLATFORM_LABELS = List.of("Steam", "Battle.net", "PlayStation", "Xbox", "Switch 2");

    @ModelAttribute
    void fillCommon(Model m) {
        m.addAttribute("contactFallback", this.contactFallback());
        m.addAttribute("homeServices", this.services.publicItems().stream().limit(6L).toList());
    }

    private String contactFallback() {
        return DEFAULT_WECHAT;
    }

    @GetMapping(value={"/"})
    String home(Model m) {
        m.addAttribute("acceptingOrders", this.acceptingOrders());
        return "index";
    }

    private boolean acceptingOrders() {
        return "open".equals(this.config.publicConfig().serviceStatus());
    }

    @GetMapping(value={"/services"})
    String services(Model m) {
        m.addAttribute("hallServices", this.services.publicItems());
        m.addAttribute("acceptingOrders", this.acceptingOrders());
        return "services";
    }

    @GetMapping(value={"/mastery"})
    String mastery() {
        return "mw4-camo-tracker";
    }

    @GetMapping(value={"/service/{slug}"})
    String serviceDetail(@PathVariable String slug, Model m) {
        ServiceItem service = this.services.publicBySlug(slug);
        GameCatalog game = this.taxonomy.publicGames().stream().filter(x -> x.getCode().equals(service.getGameCode())).findFirst().orElse(null);
        String gameName = game == null ? service.getGameCode() : game.getName();
        String h1 = gameName + " " + service.getName();
        m.addAttribute("serviceSlug", slug);
        m.addAttribute("service", service);
        m.addAttribute("game", game);
        m.addAttribute("gameName", gameName);
        m.addAttribute("serviceH1", h1);
        m.addAttribute("serviceTitle", (h1 + " | \u661f\u706b\u7f51\u6e38\u4ee3\u7ec3\u5e73\u53f0"));
        // SEO-J：基线 ServiceItem 未提供 getSeoDescription()，
        // 编译期已证实不可用，回退到基线的 shortDesc 拼接文案（原逻辑）。
        String serviceDescription = gameName + service.getShortDesc() + "\u3002\u6309\u8fdb\u5ea6\u62a5\u4ef7\uff0c\u7f51\u9875\u4e0d\u6536\u8d26\u53f7\u5bc6\u7801\u3002";
        m.addAttribute("serviceDescription", serviceDescription);
        m.addAttribute("canonicalUrl", ("https://wydlgf.cn/service/" + service.getSlug()));
        m.addAttribute("ogImage", this.rasterOgImage(service.getCoverImage()));
        m.addAttribute("supportItems", this.supportItems(service));
        m.addAttribute("quoteFactors", this.quoteFactors(service));
        m.addAttribute("relatedServices", this.relatedServices(service));
        List faqs = this.serviceFaqs(service, gameName);
        m.addAttribute("serviceFaqs", faqs);
        m.addAttribute("faqJsonLd", jsonLdSafe(this.faqJsonLd(faqs)));
        m.addAttribute("breadcrumbJsonLd", jsonLdSafe(this.breadcrumbJsonLd(gameName, service)));
        // SEO-A：商详页命中商品意图，用 Product + 真实服务端价格出 offers，
        // 价格字段只在后端计算，绝不经浏览器/模板。
        m.addAttribute("productJsonLd", jsonLdSafe(this.productJsonLd(service, h1, serviceDescription)));
        return "service-detail";
    }

    /**
     * SEO-A：为商详页输出 Product 结构化数据（含真实价格 Offer）。
     *
     * <p>价格只从服务端 {@code ServiceItem} 读取：FIXED 给 {@code price}，RANGE 给
     * {@code PriceSpecification(minPrice/maxPrice)}，QUOTE 不给价格仅给 {@code url} 与
     * {@code availability}。价格币种全站为人民币（CNY，模板同样以 ¥ 呈现）。
     */
    private String productJsonLd(ServiceItem service, String name, String description) {
        ObjectNode root = this.objectMapper.createObjectNode();
        root.put("@context", "https://schema.org");
        root.put("@type", "Product");
        root.put("name", name);
        root.put("image", this.rasterOgImage(service.getCoverImage()));
        root.put("description", description);
        ObjectNode brand = this.objectMapper.createObjectNode();
        brand.put("@type", "Organization");
        brand.put("name", "\u661f\u706b\u7f51\u6e38\u4ee3\u7ec3\u5e73\u53f0");
        root.set("brand", (JsonNode)brand);
        root.set("offers", (JsonNode)this.offerNode(service));
        return root.toString();
    }

    private ObjectNode offerNode(ServiceItem service) {
        ObjectNode offer = this.objectMapper.createObjectNode();
        offer.put("@type", "Offer");
        offer.put("priceCurrency", "CNY");
        // 页面详情在售时按库内 status 给出 availability；不可直接购买/待预约按 PreOrder。
        offer.put("availability",
                service.getStatus() != null && "AVAILABLE".equals(service.getStatus().name())
                        ? "InStock" : "PreOrder");
        offer.put("url", "https://wydlgf.cn/service/" + service.getSlug());
        // 价格有效性到今年年底，防止陈旧价格被长期展示。
        offer.put("priceValidUntil",
                LocalDate.of(LocalDate.now().getYear(), 12, 31).toString());
        if (service.getPriceMode() == ServiceItem.PriceMode.FIXED && service.getPriceFixed() != null) {
            offer.put("price", plainPrice(service.getPriceFixed()));
        } else if (service.getPriceMode() == ServiceItem.PriceMode.RANGE) {
            ObjectNode spec = this.objectMapper.createObjectNode();
            spec.put("@type", "PriceSpecification");
            spec.put("priceCurrency", "CNY");
            if (service.getPriceMin() != null) {
                spec.put("minPrice", plainPrice(service.getPriceMin()));
            }
            if (service.getPriceMax() != null) {
                spec.put("maxPrice", plainPrice(service.getPriceMax()));
            }
            offer.set("priceSpecification", (JsonNode)spec);
        }
        // QUOTE 人工报价：不给出价格，避免误导（SEO 清单要求只给 url/availability）。
        return offer;
    }

    private static String plainPrice(Object value) {
        if (value == null) {
            return null;
        }
        return java.math.BigDecimal.valueOf(((Number) value).doubleValue())
                .setScale(2, java.math.RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }

    private Map<String, String> supportItems(ServiceItem service) {
        String unit = switch (service.getCategory() == null ? "" : service.getCategory().toUpperCase(Locale.ROOT)) {
            case "WEAPON" -> "\u6307\u5b9a\u6b66\u5668\u3001\u76ee\u6807\u7b49\u7ea7\u3001\u9700\u8981\u89e3\u9501\u7684\u914d\u4ef6";
            case "CAMO" -> "\u76ee\u6807\u8ff7\u5f69\u7cfb\u5217\u4e0e\u5269\u4f59\u6311\u6218\u9879\uff08\u5177\u4f53\u540d\u79f0\u4ee5\u5b98\u65b9\u6b63\u5f0f\u7248\u4e3a\u51c6\uff09";
            case "DMZ" -> "\u64a4\u79bb\u4efb\u52a1\u3001\u5c01\u9501\u533a\u76ee\u6807\u4e0e\u9700\u8981\u4ea4\u4ed8\u7684\u7269\u8d44";
            case "ACTIVITY", "RANK" -> "\u76ee\u6807\u6d3b\u52a8\u3001\u5269\u4f59\u5956\u52b1\u6216\u76ee\u6807\u6bb5\u4f4d";
            default -> "\u5177\u4f53\u76ee\u6807\u3001\u6570\u91cf\u4e0e\u5f53\u524d\u8fdb\u5ea6";
        };
        LinkedHashMap<String, String> items = new LinkedHashMap<String, String>();
        items.put("\u53ef\u63a5\u76ee\u6807", unit);
        items.put("\u652f\u6301\u5e73\u53f0", "Steam \u00b7 Battle.net \u00b7 PlayStation \u00b7 Xbox \u00b7 Switch 2");
        items.put("\u8fdb\u5ea6\u53e3\u5f84", "\u4ee5\u4efb\u52a1\u622a\u56fe\u6838\u5bf9\u4e3a\u51c6\uff0c\u5df2\u6709\u8fdb\u5ea6\u53ef\u62b5\u6263");
        items.put("\u7279\u6b8a\u60c5\u51b5", "\u7279\u6b8a\u73a9\u6cd5\u3001\u9650\u65f6\u6a21\u5f0f\u6216\u672a\u5217\u51fa\u7684\u76ee\u6807\u53ef\u8d70\u4eba\u5de5\u786e\u8ba4");
        return items;
    }

    private Map<String, String> quoteFactors(ServiceItem service) {
        LinkedHashMap<String, String> factors = new LinkedHashMap<String, String>();
        factors.put("\u76ee\u6807\u5dee\u8ddd", "\u8d77\u59cb\u76ee\u6807\u4e0e\u6700\u7ec8\u76ee\u6807\u7684\u8ddd\u79bb\u8d8a\u5927\uff0c\u5de5\u65f6\u8d8a\u957f");
        factors.put("\u5f53\u524d\u8fdb\u5ea6", "\u5df2\u6709\u8fdb\u5ea6\u53ef\u62b5\u6263\uff0c\u5177\u4f53\u4ee5\u622a\u56fe\u6838\u5bf9\u4e3a\u51c6");
        factors.put("\u6e38\u620f\u5e73\u53f0", "\u4e3b\u673a\u4e0e PC \u7684\u64cd\u4f5c\u65b9\u5f0f\u4e0d\u540c\uff0c\u62a5\u4ef7\u4f1a\u5206\u5f00\u786e\u8ba4");
        factors.put("\u65f6\u95f4\u8981\u6c42", "\u52a0\u6025\u9700\u6c42\u4f1a\u5355\u72ec\u786e\u8ba4\u53ef\u884c\u6027\uff0c\u4e0d\u627f\u8bfa\u4e0d\u786e\u5b9a\u7684\u65f6\u6548");
        if (service.getPriceMode() == ServiceItem.PriceMode.QUOTE) {
            factors.put("\u4eba\u5de5\u8bc4\u4f30", "\u8be5\u9879\u4e3a\u4eba\u5de5\u62a5\u4ef7\uff0c\u6700\u7ec8\u91d1\u989d\u4ee5\u5ba2\u670d\u786e\u8ba4\u4e3a\u51c6");
        } else {
            factors.put("\u53c2\u8003\u533a\u95f4", "\u9875\u9762\u4ef7\u683c\u4e3a\u53c2\u8003\u533a\u95f4\uff0c\u6700\u7ec8\u4ee5\u4eba\u5de5\u786e\u8ba4\u4e3a\u51c6");
        }
        return factors;
    }

    private List<ServiceItem> relatedServices(ServiceItem service) {
        return this.services.publicItems().stream().filter(x -> x.getGameCode().equals(service.getGameCode())).filter(x -> !x.getSlug().equals(service.getSlug())).limit(4L).toList();
    }

    private List<FaqItem> serviceFaqs(ServiceItem service, String gameName) {
        String name = service.getName();
        return List.of(new FaqItem(name + "\u5927\u6982\u9700\u8981\u591a\u4e45\uff1f", "\u53c2\u8003\u5de5\u65f6\u89c1\u672c\u9875\u201c\u9884\u8ba1\u65f6\u95f4\u201d\u4e00\u680f\u3002\u5b9e\u9645\u7528\u65f6\u53d6\u51b3\u4e8e\u5f53\u524d\u8fdb\u5ea6\u548c\u76ee\u6807\uff0c\u5ba2\u670d\u6838\u5bf9\u622a\u56fe\u540e\u4f1a\u7ed9\u51fa\u66f4\u51c6\u786e\u7684\u4f30\u8ba1\u3002"), new FaqItem("\u4ef7\u683c\u662f\u600e\u4e48\u7b97\u7684\uff1f", "\u6309\u5f53\u524d\u8fdb\u5ea6\u5230\u76ee\u6807\u8fdb\u5ea6\u7684\u5dee\u8ddd\u8ba1\u7b97\u3002\u63d0\u4ea4\u65f6\u53ef\u5148\u770b\u5230\u53c2\u8003\u4ef7\uff0c\u6700\u7ec8\u91d1\u989d\u4ee5\u5ba2\u670d\u786e\u8ba4\u4e3a\u51c6\uff0c\u4e0d\u4f1a\u5728\u786e\u8ba4\u524d\u5f00\u5de5\u3002"), new FaqItem("\u9700\u8981\u63d0\u4f9b\u8d26\u53f7\u5bc6\u7801\u5417\uff1f", "\u4e0d\u9700\u8981\u3002\u7f51\u9875\u3001\u67e5\u8be2\u9875\u548c\u622a\u56fe\u90fd\u4e0d\u8981\u586b\u5199\u8d26\u53f7\u5bc6\u7801\u3001\u9a8c\u8bc1\u7801\u6216\u4ee4\u724c\u3002\u5177\u4f53\u4e0a\u53f7\u65b9\u5f0f\u52a0\u5ba2\u670d\u540e\u5355\u72ec\u786e\u8ba4\u3002"), new FaqItem("\u600e\u4e48\u4ed8\u6b3e\uff1f", "\u786e\u8ba4\u62a5\u4ef7\u540e\u8d70\u6dd8\u5b9d\u6216\u95f2\u9c7c\u62c5\u4fdd\u4ea4\u6613\uff0c\u7f51\u7ad9\u672c\u8eab\u4e0d\u6536\u53d6\u4efb\u4f55\u8d39\u7528\u3002"), new FaqItem("\u53ef\u4ee5\u67e5\u5230\u8fdb\u5ea6\u5417\uff1f", "\u53ef\u4ee5\u3002\u63d0\u4ea4\u540e\u4f1a\u751f\u6210\u4e1a\u52a1\u7f16\u53f7\uff0c\u5728\u201c\u4e1a\u52a1\u8bb0\u5f55\u201d\u9875\u8f93\u5165\u7f16\u53f7\u5373\u53ef\u67e5\u770b\u5f53\u524d\u72b6\u6001\u548c\u5ba2\u670d\u5907\u6ce8\u3002"), new FaqItem(gameName + "\u7684\u670d\u52a1\u53ef\u4ee5\u9884\u7ea6\u5417\uff1f", "\u53ef\u4ee5\u3002\u7ad9\u70b9\u9876\u90e8\u4f1a\u663e\u793a\u5f53\u524d\u8fd0\u8425\u72b6\u6001\uff1b\u5904\u4e8e\u9884\u7ea6\u4e2d\u65f6\u63d0\u4ea4\u7684\u662f\u63d0\u524d\u8be2\u4ef7\uff0c\u5f00\u653e\u540e\u4f1a\u6309\u987a\u5e8f\u5b89\u6392\u3002"));
    }

    private String faqJsonLd(List<FaqItem> faqs) {
        ArrayNode items = this.objectMapper.createArrayNode();
        for (FaqItem f : faqs) {
            ObjectNode question = this.objectMapper.createObjectNode();
            question.put("@type", "Question");
            question.put("name", f.question());
            ObjectNode answer = this.objectMapper.createObjectNode();
            answer.put("@type", "Answer");
            answer.put("text", f.answer());
            question.set("acceptedAnswer", (JsonNode)answer);
            items.add((JsonNode)question);
        }
        ObjectNode root = this.objectMapper.createObjectNode();
        root.put("@context", "https://schema.org");
        root.put("@type", "FAQPage");
        root.set("mainEntity", (JsonNode)items);
        return root.toString();
    }

    private String breadcrumbJsonLd(String gameName, ServiceItem service) {
        ArrayNode items = this.objectMapper.createArrayNode();
        items.add((JsonNode)this.crumb(1, "\u9996\u9875", "https://wydlgf.cn/"));
        items.add((JsonNode)this.crumb(2, "\u670d\u52a1\u5927\u5385", "https://wydlgf.cn/services"));
        items.add((JsonNode)this.crumb(3, gameName + " " + service.getName(), "https://wydlgf.cn/service/" + service.getSlug()));
        ObjectNode root = this.objectMapper.createObjectNode();
        root.put("@context", "https://schema.org");
        root.put("@type", "BreadcrumbList");
        root.set("itemListElement", (JsonNode)items);
        return root.toString();
    }

    private ObjectNode crumb(int position, String name, String url) {
        ObjectNode node = this.objectMapper.createObjectNode();
        node.put("@type", "ListItem");
        node.put("position", position);
        node.put("name", name);
        node.put("item", url);
        return node;
    }

    @GetMapping(value={"/game/{seoSlug}"})
    String gameDetail(@PathVariable String seoSlug, Model m) {
        GameCatalog game = this.taxonomy.publicGameBySeoSlug(seoSlug);
        m.addAttribute("game", game);
        m.addAttribute("gameServices", this.services.publicItems().stream().filter(x -> x.getGameCode().equals(game.getCode())).toList());
        m.addAttribute("canonicalUrl", ("https://wydlgf.cn/game/" + seoSlug));
        m.addAttribute("acceptingOrders", this.acceptingOrders());
        m.addAttribute("gameOgImage", this.rasterOgImage(game.getCoverImage()));
        // SEO-K：game.html 缺结构化数据，补充 BreadcrumbList（首页 > 服务大厅 > 游戏）。
        m.addAttribute("breadcrumbJsonLd", jsonLdSafe(this.gameBreadcrumbJsonLd(game)));
        return "game";
    }

    /** SEO-K：游戏页面包屑，与 service-detail 的 BreadcrumbList 同构。 */
    private String gameBreadcrumbJsonLd(GameCatalog game) {
        ArrayNode items = this.objectMapper.createArrayNode();
        items.add((JsonNode)this.crumb(1, "\u9996\u9875", "https://wydlgf.cn/"));
        items.add((JsonNode)this.crumb(2, "\u670d\u52a1\u5927\u5385", "https://wydlgf.cn/services"));
        items.add((JsonNode)this.crumb(3, game.getName(), "https://wydlgf.cn/game/" + game.getSeoSlug()));
        ObjectNode root = this.objectMapper.createObjectNode();
        root.put("@context", "https://schema.org");
        root.put("@type", "BreadcrumbList");
        root.set("itemListElement", (JsonNode)items);
        return root.toString();
    }

    @GetMapping(value={"/quote"})
    String quote() {
        return "quote";
    }

    @GetMapping(value={"/account/login"})
    String customerLogin() {
        return "account-login";
    }

    @GetMapping(value={"/account/register"})
    String customerRegister() {
        return "account-register";
    }

    @RequireAccount
    @GetMapping(value={"/account"})
    String customerAccount() {
        return "account";
    }

    @RequireAccount
    @GetMapping(value={"/account/settings"})
    String customerAccountSettings() {
        return "account-settings";
    }

    /** 投稿页。需要登录：投稿必须归属到账号，否则无法展示审核状态与撤回。 */
    @RequireAccount
    @GetMapping(value={"/account/submit"})
    String customerAccountSubmit() {
        return "account-submit";
    }

    /**
     * 密码重置确认页。刻意不加 {@link RequireAccount}：用户从邮件链接进入时
     * 必然是未登录状态，要求登录会让重置流程直接死锁。
     */
    @GetMapping(value={"/account/reset"})
    String customerAccountReset() {
        return "account-reset";
    }

    @GetMapping(value={"/records"})
    String records() {
        return "records";
    }

    @GetMapping(value={"/record/{businessNo}"})
    String recordDetail(@PathVariable String businessNo, Model m) {
        m.addAttribute("businessNo", businessNo);
        return "record-detail";
    }

    @GetMapping(value={"/order"})
    String order() {
        return "order";
    }

    @GetMapping(value={"/order/{orderNo}"})
    String legacyOrderDetail(@PathVariable String orderNo, Model m) {
        m.addAttribute("businessNo", orderNo);
        return "record-detail";
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

    // 英文版是独立模板（rules-en.html / privacy-en.html），原来只能靠 /rules
    // 的 locale 切换才能拿到，直接访问 /rules-en 会 404。
    // 补上显式路由，让英文页有稳定、可被搜索引擎收录的固定地址。
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

    @GetMapping(value={"/admin/login"})
    String login() {
        return "admin-login";
    }

    @GetMapping(value={"/admin"})
    String admin(Model m, CsrfToken csrf, Authentication auth) {
        m.addAttribute("csrf", csrf);
        m.addAttribute("username", auth.getName());
        return "admin";
    }

    private String rasterOgImage(String image) {
        if (image == null || image.isBlank()) {
            return "https://wydlgf.cn/og.png";
        }
        String lower = image.toLowerCase();
        if (!(lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".webp"))) {
            return "https://wydlgf.cn/og.png";
        }
        if (lower.startsWith("https://")) {
            return image;
        }
        if (lower.startsWith("/")) {
            return "https://wydlgf.cn" + image;
        }
        return "https://wydlgf.cn/" + image;
    }

    @Generated
    public PageController(ServiceCatalogService services, TaxonomyService taxonomy, ObjectMapper objectMapper, SiteConfigService config) {
        this.services = services;
        this.taxonomy = taxonomy;
        this.objectMapper = objectMapper;
        this.config = config;
    }

    public record FaqItem(String question, String answer) {
    }

    /**
     * JSON-LD 安全化。
     *
     * <p>这些值经 {@code th:utext} 不转义写入 {@code <script type="application/ld+json">}，
     * 而 Jackson 的 {@code toString()} 只转义引号、反斜杠与控制字符，<b>不转义 {@code <}</b>。
     * 只要标题、摘要或 FAQ 里出现 {@code </script>}，就能提前闭合脚本元素并注入 HTML。
     * CSP 会挡掉注入的内联脚本执行，但 {@code meta refresh} 跳转与样式注入仍然生效。
     *
     * <p>把三个 HTML 敏感字符换成 JSON 的 Unicode 转义（反斜杠 + u + 四位十六进制）：
     * JSON 语义完全不变，
     * 但内容再也无法闭合标签。
     */
    private static String jsonLdSafe(String json) {
        if (json == null || json.isBlank()) {
            return "{}";
        }
        return json.replace("<", "\\u003c")
                   .replace(">", "\\u003e")
                   .replace("&", "\\u0026");
    }
}

