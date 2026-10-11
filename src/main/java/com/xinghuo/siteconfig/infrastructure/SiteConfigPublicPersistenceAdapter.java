package com.xinghuo.siteconfig.infrastructure;

import com.xinghuo.catalog.infrastructure.ServiceItem;
import com.xinghuo.catalog.infrastructure.ServiceItemRepository;
import com.xinghuo.exception.BadRequestException;
import com.xinghuo.siteconfig.application.SiteConfigPublicPort;
import com.xinghuo.siteconfig.application.SiteConfigPublicSnapshot;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class SiteConfigPublicPersistenceAdapter implements SiteConfigPublicPort {
    private final SiteConfigRepository repo;
    private final ServiceItemRepository serviceRepo;

    public SiteConfigPublicPersistenceAdapter(
            SiteConfigRepository repo,
            ServiceItemRepository serviceRepo) {
        this.repo = repo;
        this.serviceRepo = serviceRepo;
    }

    @Override
    @Transactional(readOnly = true)
    public SiteConfigPublicSnapshot load() {
        SiteConfig c = repo.findById(1L).orElseThrow();
        return new SiteConfigPublicSnapshot(
                c.getBrandName(),
                c.getLegalEntityName(),
                c.isQuoteEnabled(),
                c.isOrderQueryEnabled(),
                c.getHeroTitle(),
                c.getHeroSubtitle(),
                c.getStartingPriceMode().name().toLowerCase(Locale.ROOT),
                displayStartingPrice(c),
                c.isShowStartingPrice(),
                c.getHomeServiceLimit(),
                c.getHomeServiceMobileLimit(),
                c.getHomeServiceDesktopColumns(),
                c.getHomeServiceMobileColumns(),
                normalizeDensity(c.getServiceCardDensity()).toLowerCase(Locale.ROOT),
                normalizeDensity(c.getServiceCardMobileDensity()).toLowerCase(Locale.ROOT),
                c.getHeroImage(),
                c.getHeroImageHeight(),
                c.getHeroImageFit().toLowerCase(Locale.ROOT),
                c.getHeroImagePositionX(),
                c.getHeroImagePositionY(),
                c.getHeroImageMobileHeight(),
                c.getHeroImageMobileFit().toLowerCase(Locale.ROOT),
                c.getHeroImageMobilePositionX(),
                c.getHeroImageMobilePositionY(),
                c.getHeroBackgroundImage(),
                c.getHeroBackgroundMobileImage(),
                c.getHeroBackgroundMobileHeight(),
                c.getHeroBackgroundOpacity(),
                c.getHeroBackgroundFit().toLowerCase(Locale.ROOT),
                c.getHeroBackgroundPositionX(),
                c.getHeroBackgroundPositionY(),
                c.getHeroBackgroundBlur(),
                c.getHeroBackgroundMobileFit().toLowerCase(Locale.ROOT),
                c.getHeroBackgroundMobilePositionX(),
                c.getHeroBackgroundMobilePositionY(),
                c.getServiceCoverHeight(),
                c.getServiceCoverFit().toLowerCase(Locale.ROOT),
                c.getServiceCoverPositionX(),
                c.getServiceCoverPositionY(),
                c.getServiceCoverMobileHeight(),
                c.getServiceCoverMobileFit().toLowerCase(Locale.ROOT),
                c.getServiceCoverMobilePositionX(),
                c.getServiceCoverMobilePositionY(),
                splitTags(c.getTrustTags()),
                parseTrustCases(c),
                c.getTrustCaseHomeLimit(),
                c.getTrustCaseHomeMobileLimit(),
                parseTrustFaq(c),
                c.getTrustFaqHomeLimit(),
                c.getTrustFaqHomeMobileLimit(),
                c.isAnnouncementEnabled(),
                c.getAnnouncementText(),
                c.getContactTitle(),
                c.getContactIntro(),
                c.getWechatId(),
                c.getWechatQrImage(),
                c.getWechatQrSize(),
                parseContactChannels(c),
                c.getHomeContactLimit(),
                c.getHomeContactMobileLimit(),
                c.getQqId(),
                c.getQqLink(),
                c.getPhone(),
                c.getServiceHours(),
                c.getTaobaoUrl(),
                c.getXianyuUrl(),
                c.getContactNotice(),
                c.getWecomKefuUrl(),
                c.getWecomKefuNote(),
                c.getQuickContactTitle(),
                c.getTaobaoLabel(),
                c.getXianyuLabel(),
                c.getWecomKefuLabel(),
                c.getWecomKefuButton(),
                c.getQuoteSuccessText(),
                c.getOrderHelpText(),
                c.getFooterText(),
                c.getSeoTitle(),
                c.getSeoDescription(),
                c.getToolUpdatedNote(),
                c.isToolPublicEnabled());
    }

    private BigDecimal displayStartingPrice(SiteConfig c) {
        if (c.getStartingPriceMode() != SiteConfig.StartingPriceMode.AUTO) {
            return c.getStartingPrice();
        }
        return serviceRepo.findPublic(Instant.now()).stream()
                .map(this::entryPrice)
                .filter(Objects::nonNull)
                .min(BigDecimal::compareTo)
                .orElse(c.getStartingPrice());
    }

    private BigDecimal entryPrice(ServiceItem s) {
        if (s.getPriceMode() == ServiceItem.PriceMode.FIXED) {
            return s.getPriceFixed();
        }
        if (s.getPriceMode() == ServiceItem.PriceMode.RANGE) {
            return s.getPriceMin();
        }
        return null;
    }

    private List<String> splitTags(String raw) {
        return raw == null || raw.isBlank()
                ? List.of()
                : Arrays.stream(raw.split("\\|"))
                        .map(String::trim)
                        .filter(value -> !value.isBlank())
                        .toList();
    }

    private List<SiteConfigPublicSnapshot.ContactChannelView> parseContactChannels(SiteConfig c) {
        String raw = c.getContactChannels();
        ArrayList<SiteConfigPublicSnapshot.ContactChannelView> rows = new ArrayList<>();
        if (raw != null && !raw.isBlank()) {
            for (String line : raw.split("\\R")) {
                if (line == null || line.isBlank()) {
                    continue;
                }
                String[] parts = line.split("\\t", -1);
                String label = clean(parts.length > 0 ? parts[0] : "", 40);
                String wechat = clean(parts.length > 1 ? parts[1] : "", 100);
                String qr = clean(parts.length > 2 ? parts[2] : "", 500);
                String note = clean(parts.length > 3 ? parts[3] : "", 120);
                String jump = clean(parts.length > 4 ? parts[4] : "", 500);
                if (label == null && wechat == null && qr == null) {
                    continue;
                }
                rows.add(new SiteConfigPublicSnapshot.ContactChannelView(
                        label == null ? "客服" + (rows.size() + 1) : label,
                        wechat,
                        qr,
                        note,
                        jump));
                if (rows.size() >= 6) {
                    break;
                }
            }
        }
        if (rows.isEmpty() && (c.getWechatId() != null || c.getWechatQrImage() != null)) {
            rows.add(new SiteConfigPublicSnapshot.ContactChannelView(
                    "主客服", c.getWechatId(), c.getWechatQrImage(), "发送业务编号确认报价", null));
        }
        if (rows.isEmpty()) {
            rows.add(new SiteConfigPublicSnapshot.ContactChannelView(
                    "主客服", "AAC9878", null, "发送业务编号确认报价", null));
        }
        return rows;
    }

    private List<SiteConfigPublicSnapshot.TrustCaseView> parseTrustCases(SiteConfig c) {
        String raw = c.getTrustCases();
        ArrayList<SiteConfigPublicSnapshot.TrustCaseView> rows = new ArrayList<>();
        if (raw != null && !raw.isBlank()) {
            for (String line : raw.split("\\R")) {
                if (line == null || line.isBlank()) {
                    continue;
                }
                String[] parts = line.split("\\t", -1);
                String tag = clean(parts.length > 0 ? parts[0] : "", 40);
                String title = clean(parts.length > 1 ? parts[1] : "", 100);
                String metric = clean(parts.length > 2 ? parts[2] : "", 120);
                String detail = clean(parts.length > 3 ? parts[3] : "", 240);
                if (title == null && metric == null && detail == null) {
                    continue;
                }
                rows.add(new SiteConfigPublicSnapshot.TrustCaseView(
                        tag == null ? "服务示例" : tag,
                        title == null ? "服务示例" : title,
                        metric == null ? "按进度确认" : metric,
                        detail == null ? "" : detail));
                if (rows.size() >= 8) {
                    break;
                }
            }
        }
        return rows.isEmpty() ? defaultTrustCases() : rows;
    }

    private List<SiteConfigPublicSnapshot.TrustFaqView> parseTrustFaq(SiteConfig c) {
        String raw = c.getTrustFaq();
        ArrayList<SiteConfigPublicSnapshot.TrustFaqView> rows = new ArrayList<>();
        if (raw != null && !raw.isBlank()) {
            for (String line : raw.split("\\R")) {
                if (line == null || line.isBlank()) {
                    continue;
                }
                String[] parts = line.split("\\t", -1);
                String question = clean(parts.length > 0 ? parts[0] : "", 100);
                String answer = clean(parts.length > 1 ? parts[1] : "", 400);
                if (question == null || answer == null) {
                    continue;
                }
                rows.add(new SiteConfigPublicSnapshot.TrustFaqView(question, answer));
                if (rows.size() >= 12) {
                    break;
                }
            }
        }
        return rows.isEmpty() ? defaultTrustFaq() : rows;
    }

    private List<SiteConfigPublicSnapshot.TrustCaseView> defaultTrustCases() {
        return List.of(
                new SiteConfigPublicSnapshot.TrustCaseView(
                        "武器升级", "MW4 武器 1→满级配件", "约 50–90 分钟 · ¥35–60",
                        "按当前等级抵扣，完成后可在业务记录查看进度说明。"),
                new SiteConfigPublicSnapshot.TrustCaseView(
                        "活动任务", "赛季限时任务清单", "约 3–6 小时 · 按任务报价",
                        "先核对剩余任务与截止时间，再确认是否开工。"),
                new SiteConfigPublicSnapshot.TrustCaseView(
                        "迷彩挑战", "剩余挑战补齐", "当天可交付 · 按截图估价",
                        "按剩余迷彩挑战项目确认，正式名称以官方公布为准；网页不收密码。"),
                new SiteConfigPublicSnapshot.TrustCaseView(
                        "排位/段位", "当前段到目标段", "按段差报价 · 异常即停",
                        "开工前确认目标、平台与可操作时间，异常及时沟通。"));
    }

    private List<SiteConfigPublicSnapshot.TrustFaqView> defaultTrustFaq() {
        return List.of(
                new SiteConfigPublicSnapshot.TrustFaqView(
                        "怎么查进度？", "提交后会生成业务编号，在「业务记录」输入即可查看当前状态和客服备注。"),
                new SiteConfigPublicSnapshot.TrustFaqView(
                        "要不要给密码？", "网页、查询页、截图都不要发密码、验证码或令牌。具体上号方式加微信后单独确认。"),
                new SiteConfigPublicSnapshot.TrustFaqView(
                        "怎么付款？", "确认报价后走淘宝或闲鱼担保，网页不收钱。"),
                new SiteConfigPublicSnapshot.TrustFaqView(
                        "能取消吗？", "未开工可取消；已开工按完成进度核对后退剩余部分。"),
                new SiteConfigPublicSnapshot.TrustFaqView(
                        "支持哪些平台？", "Steam、Battle.net、PlayStation、Xbox、Switch 2 等平台按具体游戏确认。"),
                new SiteConfigPublicSnapshot.TrustFaqView(
                        "排位会不会掉？", "异常立即停单并沟通，不硬冲。排位类服务会先确认当前段位、目标段位和风险。"),
                new SiteConfigPublicSnapshot.TrustFaqView(
                        "截图怎么发？", "询价页可上传任务截图，也可以复制业务编号后加微信补发。截图里不要包含密码或验证码。"),
                new SiteConfigPublicSnapshot.TrustFaqView(
                        "多久回复？", "客服时间通常为 10:00–24:00，高峰期会按业务编号顺序处理。"));
    }

    private String clean(String value, int max) {
        if (value == null) {
            return null;
        }
        String out = value.trim().replace("\t", " ").replace("\r", " ").replace("\n", " ");
        if (out.isBlank()) {
            return null;
        }
        if (out.length() > max) {
            throw new BadRequestException("客服渠道字段过长");
        }
        return out;
    }

    private String normalizeDensity(String value) {
        String normalized = value == null ? "COMFORTABLE" : value.trim().toUpperCase(Locale.ROOT);
        if (!Set.of("COMPACT", "COMFORTABLE", "SPACIOUS").contains(normalized)) {
            throw new BadRequestException("非法组件密度");
        }
        return normalized;
    }
}
