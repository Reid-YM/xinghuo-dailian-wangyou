package com.xinghuo.guide.web;

import com.xinghuo.guide.application.GuideWriteModels;
import java.util.List;

public final class GuideWriteWebMapper {
    private GuideWriteWebMapper() {
    }

    public static GuideWriteModels.GuideUpsertCommand toCommand(GuideAdminRequests.GuideUpsert input) {
        if (input == null) {
            return null;
        }
        return new GuideWriteModels.GuideUpsertCommand(
                input.gameCode(),
                input.slug(),
                input.title(),
                input.subtitle(),
                input.summary(),
                input.coverImage(),
                input.difficulty(),
                input.estimatedMinutes(),
                input.language(),
                input.authorName(),
                input.sourceName(),
                input.sourceUrl(),
                input.status(),
                input.featured(),
                input.sortOrder(),
                input.tags(),
                input.seoTitle(),
                input.seoDescription(),
                input.canonicalUrl(),
                sections(input.sections()),
                products(input.products()),
                faqs(input.faqs()),
                input.relatedGuideIds(),
                input.reason());
    }

    public static GuideWriteModels.GuideImportCommand toCommand(GuideAdminRequests.GuideImportRequest input) {
        return input == null
                ? null
                : new GuideWriteModels.GuideImportCommand(input.publish(), input.refresh(), input.reason());
    }

    private static List<GuideWriteModels.SectionInput> sections(List<GuideAdminRequests.SectionInput> values) {
        return values == null
                ? null
                : values.stream()
                        .map(value -> value == null
                                ? null
                                : new GuideWriteModels.SectionInput(
                                        value.sectionKey(),
                                        value.heading(),
                                        value.bodyMarkdown(),
                                        value.imageUrl(),
                                        value.imageAlt(),
                                        value.sortOrder()))
                        .toList();
    }

    private static List<GuideWriteModels.ProductInput> products(List<GuideAdminRequests.ProductInput> values) {
        return values == null
                ? null
                : values.stream()
                        .map(value -> value == null
                                ? null
                                : new GuideWriteModels.ProductInput(
                                        value.serviceId(),
                                        value.label(),
                                        value.description(),
                                        value.imageUrl(),
                                        value.featured(),
                                        value.sortOrder()))
                        .toList();
    }

    private static List<GuideWriteModels.FaqInput> faqs(List<GuideAdminRequests.FaqInput> values) {
        return values == null
                ? null
                : values.stream()
                        .map(value -> value == null
                                ? null
                                : new GuideWriteModels.FaqInput(
                                        value.question(), value.answer(), value.sortOrder()))
                        .toList();
    }
}
