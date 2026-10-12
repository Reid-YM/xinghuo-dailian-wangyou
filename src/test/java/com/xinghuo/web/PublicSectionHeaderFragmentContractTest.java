package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class PublicSectionHeaderFragmentContractTest {
    private static final Path TEMPLATES = Path.of("src/main/resources/templates");

    @Test
    void pageFragmentsOwnReusableSectionHeaderStructure() throws Exception {
        String page = Files.readString(TEMPLATES.resolve("fragments/page.html"));
        assertTrue(page.contains("th:fragment=\"sectionHeader(eyebrow,title,linkHref,linkLabel,extraClass)\""));
        assertTrue(page.contains("class=\"section-title\""));
        assertTrue(page.contains("class=\"text-link\""));
        assertTrue(page.contains("th:if=\"${linkHref != null and !#strings.isEmpty(linkHref)}\""));
    }

    @Test
    void guideUsesSharedSectionHeadersForProductsAndRelatedGuides() throws Exception {
        String guide = Files.readString(TEMPLATES.resolve("guide.html"));
        assertTrue(guide.contains("sectionHeader('Recommended Products','推荐商品','/services','查看全部服务','section-title-rich')"));
        assertTrue(guide.contains("sectionHeader('Related','相关攻略',null,null,'section-title-rich')"));
        assertFalse(guide.contains("<div class=\"section-title section-title-rich\"><div><span class=\"eyebrow\">Recommended Products</span>"));
        assertFalse(guide.contains("<div class=\"section-title section-title-rich\"><div><span class=\"eyebrow\">Related</span>"));
    }

    @Test
    void serviceDetailUsesSharedSectionHeaderForRelatedTools() throws Exception {
        String detail = Files.readString(TEMPLATES.resolve("service-detail.html"));
        assertTrue(detail.contains("sectionHeader('相关工具','先自己看看，再决定要不要下单','/mastery','MW4 工具站','section-title-rich')"));
        assertFalse(detail.contains("<div class=\"section-title section-title-rich\"><div><span class=\"eyebrow\">相关工具</span>"));
        assertTrue(detail.contains("<span class=\"eyebrow\">相关服务</span>"),
                "dynamic related-services header stays local until its parameter shape is proven separately");
    }
}
