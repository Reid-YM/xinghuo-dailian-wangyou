package com.xinghuo.catalog.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.xinghuo.catalog.application.CatalogApplicationService;
import com.xinghuo.catalog.application.CatalogPageServiceView;
import com.xinghuo.taxonomy.application.TaxonomyCatalogView;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.Generated;
import org.springframework.stereotype.Component;

@Component
public class CatalogPagePresentation {
    private final CatalogApplicationService services;
    private final ObjectMapper objectMapper;

    public Map<String, String> supportItems(CatalogPageServiceView service) {
        String unit = switch (service.getCategory() == null ? "" : service.getCategory().toUpperCase(Locale.ROOT)) {
            case "WEAPON" -> "指定武器、目标等级、需要解锁的配件";
            case "CAMO" -> "目标迷彩系列与剩余挑战项（具体名称以官方正式版为准）";
            case "DMZ" -> "撤离任务、封锁区目标与需要交付的物资";
            case "ACTIVITY", "RANK" -> "目标活动、剩余奖励或目标段位";
            default -> "具体目标、数量与当前进度";
        };
        LinkedHashMap<String, String> items = new LinkedHashMap<>();
        items.put("可接目标", unit);
        items.put("支持平台", "Steam · Battle.net · PlayStation · Xbox · Switch 2");
        items.put("进度口径", "以任务截图核对为准，已有进度可抵扣");
        items.put("特殊情况", "特殊玩法、限时模式或未列出的目标可走人工确认");
        return items;
    }

    public Map<String, String> quoteFactors(CatalogPageServiceView service) {
        LinkedHashMap<String, String> factors = new LinkedHashMap<>();
        factors.put("目标差距", "起始目标与最终目标的距离越大，工时越长");
        factors.put("当前进度", "已有进度可抵扣，具体以截图核对为准");
        factors.put("游戏平台", "主机与 PC 的操作方式不同，报价会分开确认");
        factors.put("时间要求", "加急需求会单独确认可行性，不承诺不确定的时效");
        if (service.getPriceMode() == CatalogPageServiceView.PriceMode.QUOTE) {
            factors.put("人工评估", "该项为人工报价，最终金额以客服确认为准");
        } else {
            factors.put("参考区间", "页面价格为参考区间，最终以人工确认为准");
        }
        return factors;
    }

    public List<CatalogPageServiceView> relatedServices(CatalogPageServiceView service) {
        return this.services.publicPageItems().stream()
                .filter(x -> x.getGameCode().equals(service.getGameCode()))
                .filter(x -> !x.getSlug().equals(service.getSlug()))
                .limit(4L)
                .toList();
    }

    public List<FaqItem> serviceFaqs(CatalogPageServiceView service, String gameName) {
        String name = service.getName();
        return List.of(
                new FaqItem(name + "大概需要多久？", "参考工时见本页“预计时间”一栏。实际用时取决于当前进度和目标，客服核对截图后会给出更准确的估计。"),
                new FaqItem("价格是怎么算的？", "按当前进度到目标进度的差距计算。提交时可先看到参考价，最终金额以客服确认为准，不会在确认前开工。"),
                new FaqItem("需要提供账号密码吗？", "不需要。网页、查询页和截图都不要填写账号密码、验证码或令牌。具体上号方式加客服后单独确认。"),
                new FaqItem("怎么付款？", "确认报价后走淘宝或闲鱼担保交易，网站本身不收取任何费用。"),
                new FaqItem("可以查到进度吗？", "可以。提交后会生成业务编号，在“业务记录”页输入编号即可查看当前状态和客服备注。"),
                new FaqItem(gameName + "的服务可以预约吗？", "可以。站点顶部会显示当前运营状态；处于预约中时提交的是提前询价，开放后会按顺序安排。"));
    }

    public String productJsonLd(CatalogPageServiceView service, String name, String description) {
        ObjectNode root = this.objectMapper.createObjectNode();
        root.put("@context", "https://schema.org");
        root.put("@type", "Product");
        root.put("name", name);
        root.put("image", this.rasterOgImage(service.getCoverImage()));
        root.put("description", description);
        ObjectNode brand = this.objectMapper.createObjectNode();
        brand.put("@type", "Organization");
        brand.put("name", "星火网游代练平台");
        root.set("brand", brand);
        root.set("offers", this.offerNode(service));
        return jsonLdSafe(root.toString());
    }

    public String faqJsonLd(List<FaqItem> faqs) {
        ArrayNode items = this.objectMapper.createArrayNode();
        for (FaqItem f : faqs) {
            ObjectNode question = this.objectMapper.createObjectNode();
            question.put("@type", "Question");
            question.put("name", f.question());
            ObjectNode answer = this.objectMapper.createObjectNode();
            answer.put("@type", "Answer");
            answer.put("text", f.answer());
            question.set("acceptedAnswer", answer);
            items.add(question);
        }
        ObjectNode root = this.objectMapper.createObjectNode();
        root.put("@context", "https://schema.org");
        root.put("@type", "FAQPage");
        root.set("mainEntity", items);
        return jsonLdSafe(root.toString());
    }

    public String serviceBreadcrumbJsonLd(String gameName, CatalogPageServiceView service) {
        ArrayNode items = this.objectMapper.createArrayNode();
        items.add(this.crumb(1, "首页", "https://wydlgf.cn/"));
        items.add(this.crumb(2, "服务大厅", "https://wydlgf.cn/services"));
        items.add(this.crumb(3, gameName + " " + service.getName(), "https://wydlgf.cn/service/" + service.getSlug()));
        return jsonLdSafe(this.breadcrumbRoot(items).toString());
    }

    public String gameBreadcrumbJsonLd(TaxonomyCatalogView.GameView game) {
        ArrayNode items = this.objectMapper.createArrayNode();
        items.add(this.crumb(1, "首页", "https://wydlgf.cn/"));
        items.add(this.crumb(2, "服务大厅", "https://wydlgf.cn/services"));
        items.add(this.crumb(3, game.getName(), "https://wydlgf.cn/game/" + game.getSeoSlug()));
        return jsonLdSafe(this.breadcrumbRoot(items).toString());
    }

    public String rasterOgImage(String image) {
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

    private ObjectNode offerNode(CatalogPageServiceView service) {
        ObjectNode offer = this.objectMapper.createObjectNode();
        offer.put("@type", "Offer");
        offer.put("priceCurrency", "CNY");
        offer.put("availability",
                service.getStatus() != null && "AVAILABLE".equals(service.getStatus().name())
                        ? "InStock" : "PreOrder");
        offer.put("url", "https://wydlgf.cn/service/" + service.getSlug());
        offer.put("priceValidUntil", LocalDate.of(LocalDate.now().getYear(), 12, 31).toString());
        if (service.getPriceMode() == CatalogPageServiceView.PriceMode.FIXED && service.getPriceFixed() != null) {
            offer.put("price", plainPrice(service.getPriceFixed()));
        } else if (service.getPriceMode() == CatalogPageServiceView.PriceMode.RANGE) {
            ObjectNode spec = this.objectMapper.createObjectNode();
            spec.put("@type", "PriceSpecification");
            spec.put("priceCurrency", "CNY");
            if (service.getPriceMin() != null) {
                spec.put("minPrice", plainPrice(service.getPriceMin()));
            }
            if (service.getPriceMax() != null) {
                spec.put("maxPrice", plainPrice(service.getPriceMax()));
            }
            offer.set("priceSpecification", spec);
        }
        return offer;
    }

    private ObjectNode crumb(int position, String name, String url) {
        ObjectNode node = this.objectMapper.createObjectNode();
        node.put("@type", "ListItem");
        node.put("position", position);
        node.put("name", name);
        node.put("item", url);
        return node;
    }

    private ObjectNode breadcrumbRoot(ArrayNode items) {
        ObjectNode root = this.objectMapper.createObjectNode();
        root.put("@context", "https://schema.org");
        root.put("@type", "BreadcrumbList");
        root.set("itemListElement", items);
        return root;
    }

    private static String plainPrice(Object value) {
        if (value == null) {
            return null;
        }
        return java.math.BigDecimal.valueOf(((Number) value).doubleValue())
                .setScale(2, java.math.RoundingMode.HALF_UP)
                .stripTrailingZeros()
                .toPlainString();
    }

    private static String jsonLdSafe(String json) {
        if (json == null || json.isBlank()) {
            return "{}";
        }
        return json.replace("<", "\\u003c")
                .replace(">", "\\u003e")
                .replace("&", "\\u0026");
    }

    @Generated
    public CatalogPagePresentation(CatalogApplicationService services, ObjectMapper objectMapper) {
        this.services = services;
        this.objectMapper = objectMapper;
    }

    public record FaqItem(String question, String answer) {
    }
}
