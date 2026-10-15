package com.xinghuo.controller;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * 星维网络服务工作室新版后台入口。
 *
 * <p>版本号和首页阶段状态集中维护在本类，避免功能已更新但后台仍显示旧阶段。
 * 每个可交付阶段同时更新 RELEASE_HISTORY、对应模块版本、页面阶段说明与状态契约测试。
 * V3K 把版本/阶段/更新记录一致性检查下沉到 CI；V3L 再由浏览器核对页面声明与实际运行后端版本。
 */
@Controller
public class XingweiAdminController {
    static final String CONSOLE_VERSION = "20261015-v4j";
    static final String CONSOLE_STAGE = "V4J · RELEASE CANDIDATE";
    static final String BUSINESS_VERSION = "20261015-v4j";
    static final String WORK_VERSION = "20261013-v2f";
    static final String WECOM_VERSION = "20261013-v2g";
    static final String CATALOG_VERSION = "20261014-v3f";
    static final String TAXONOMY_VERSION = "20261014-v3i";

    private static final List<ReleaseItem> RELEASE_HISTORY = List.of(
            new ReleaseItem("2026-10-15", "V4J", "待发布",
                    "售后待跟进时效分布",
                    "在 V4I 售后概览基础上增加按当前账号数据范围统计的需跟进总数，以及 24 小时内、24–72 小时、72 小时以上三档互斥时效分布。需跟进仅包含尚未开始处理的 OPEN 记录，或存在客服尚未确认到最新客户补充的记录；有新补充时从最新补充时间计时，否则从原始售后提交时间计时。继续受 order:view 和 ALL / DEPARTMENT / SELF 数据范围约束，仅提供只读运营提示，不新增数据库迁移，不改变订单、售后处理、退款、真实收款或企业微信写入。功能 PR #476 已通过 Java/Maven、版本真值守卫及 Chromium/WebKit 双 CI并合入主线；本统一版本仍需生产门禁通过后才能标记已上线。"),
            new ReleaseItem("2026-10-15", "V4I", "已上线",
                    "售后问题类型分布与快捷筛选",
                    "在 V4H 状态分布基础上增加交付或进度、取消服务、退款核对、其他问题四类按权限范围统计；点击问题类型可直接筛选售后队列并保留处理状态、待跟进与搜索条件。分类统计继续受 order:view 和 ALL / DEPARTMENT / SELF 数据范围约束；功能 PR #470 与统一版本 PR #474 均通过双 CI，主线 3e1870701a158fe0cd21703864de939b1dc221d0 已通过 18080、8081、8080 零停机生产门禁及发布后核验。无新增数据库迁移，不改变订单、售后状态、退款、真实收款或企业微信写入。"),
            new ReleaseItem("2026-10-15", "V4H", "已上线",
                    "售后处理状态分布与快捷筛选",
                    "后台售后概览增加待处理、处理中、已解决三种状态的按权限范围计数；点击状态可直接筛选队列并生成可分享的网址。计数保留 order:view 和 ALL / DEPARTMENT / SELF 数据范围，未建立处理状态的记录计入待处理。功能 PR #465、#466 与统一版本 PR #468 均通过双 CI、主线双 CI 和 18080/8081/8080 生产门禁；仅提供读取与导航，不自动改变售后状态、订单、退款、真实收款或企业微信写入。"),
            new ReleaseItem("2026-10-15", "V4G", "已上线",
                    "售后跟进状态与客服概览",
                    "客户订单详情只返回售后补充是否待客服跟进，不暴露内部处理游标或说明；个人中心明确提示待跟进和已解决记录的补充规则。新版售后工作台增加按权限范围汇总的售后总数、待跟进数，并支持筛选网址分享及浏览器返回恢复。汇总继续受 order:view 和 ALL / DEPARTMENT / SELF 数据范围约束；处理仍要求 order:manage。三条功能线与统一版本均通过双 CI、主线双 CI 和 18080/8081/8080 生产门禁；不改变订单或售后状态，不触发退款、真实收款或企业微信写入。"),
            new ReleaseItem("2026-10-15", "V4F", "已上线",
                    "客户补充待跟进筛选与处理确认",
                    "客服可按待跟进客户补充筛选售后队列，查看后在原有版本保护的处理动作中确认已看到的补充编号；处理期间新到的补充继续待跟进。V74 只为处理状态增加可兼容的游标列；查询保留 order:view 和 ALL / DEPARTMENT / SELF 数据范围，确认仍要求 order:manage。不会自动改售后或订单状态，也不触发退款、真实收款或企业微信写入。功能 PR #455、主线双 CI、V74 哈希审核、18080/8081/8080 生产门禁及发布后核验已完成。"),
            new ReleaseItem("2026-10-15", "V4E", "已上线",
                    "客户售后补充与员工跟进队列",
                    "客户可在归属校验后对原 V70 售后记录补充说明；补充内容写入 V73 独立表并显示于客户与后台历史。员工队列按最近客户补充时间排序，保持 order:view 及数据范围约束。补充不会自动重开已解决记录，也不会更改订单状态、触发退款、收款或企业微信写入。功能 PR #453、主线双 CI、V73 哈希审核、18080/8081/8080 生产门禁及发布后核验已完成。"),
            new ReleaseItem("2026-10-15", "V4D", "已上线",
                    "售后连续回复与客户回复历史",
                    "客服可在处理中或已解决状态追加明确的客户回复，无须切换状态；仍要求内部处理说明与预期版本号，写入 V72 事件并审计。客户订单详情在归属校验后按时间展示全部公开回复，保留最新回复字段兼容旧页面，内部说明和处理人不进入客户响应。PR #451、main 双 CI、18080/8081/8080 生产门禁及发布后核验已完成。无新数据库迁移，不触发退款、真实收款或企业微信写入。"),
            new ReleaseItem("2026-10-15", "V4C", "已上线",
                    "售后处理闭环与客户可见回复",
                    "售后状态变更写入独立事件历史，员工按原有权限查看并填写可选客户回复；客户订单详情仅展示状态和明确回复，不暴露内部处理说明。统一售后工作台提供筛选、历史和受控处理。V72 增量迁移已通过内容校验，PR #449、主线双 CI、18080/8081/8080 生产门禁与发布后核验已完成；退款、真实收款和企业微信写入继续保持独立。"),
            new ReleaseItem("2026-10-15", "V4B", "已上线",
                    "后台售后处理队列",
                    "按单条 V70 售后流水提供员工队列，支持 OPEN、IN_PROGRESS、RESOLVED 状态、问题类型、关键词和分页。查询与计数均在 order:view 及 ALL / DEPARTMENT / SELF 数据范围内执行；未处理流水默认 OPEN，可衔接 V4A 处理接口。功能 PR #446、版本 PR #447 与 18080/8081/8080 生产门禁均已完成。无新数据库迁移，不触发退款或支付。"),
            new ReleaseItem("2026-10-14", "V4A", "已上线",
                    "后台售后记录处理状态与审计",
                    "V71 为售后流水增加独立处理状态。员工可在 order:view 和业务归属范围内读取记录；持有 order:manage 的员工可填写处理说明，按版本号推进处理中、已解决或重新处理中。并发冲突返回 409，处理动作写入审计。功能 PR #443、版本 PR #444 和生产门禁均已完成；18080、8081、8080 冒烟通过。客户原文、退款、真实收款及企业微信写入均不变。"),
            new ReleaseItem("2026-10-14", "V3Z", "已上线",
                    "售后问题记录独立存储",
                    "V70 新增售后记录表；新提交在同一事务写入记录，订单备注有空间时兼容双写，备注已满仍可提交。客户详情与后台受权限范围约束的列表合并旧备注和新表记录，后台检索、分类、计数与分页覆盖两种来源。功能 PR #440、版本 PR #441 与生产门禁均已完成；不启用真实收款或企业微信写入。"),
            new ReleaseItem("2026-10-14", "V3Y", "已上线",
                    "后台售后记录结构化接口",
                    "后台售后列表在现有权限、数据范围、检索、分类和分页下，为每条可见订单新增 afterSalesRecords；与客户订单详情共用规范售后记录解析规则。保留 customerNote 等原字段，不改变前端、数据库、支付或企业微信写入。后端 PR #437、版本 PR #438 与生产门禁均已完成。"),
            new ReleaseItem("2026-10-14", "V3X", "已上线",
                    "客户售后历史结构化回显",
                    "客户订单详情在原有 order、sourceQuote、timeline 字段之外，增加经订单归属校验的 afterSalesRecords 只读列表，包含问题类型、提交时间和正文。个人中心优先使用结构化记录，旧响应仍兼容；空列表明确显示暂无记录，提交后重新读取，正文按纯文本展示。后端 PR #434、前端 PR #433、统一版本 PR #435 与生产门禁均已完成。无数据库或配置变更，不改变售后处理、退款、支付或企业微信写入状态。"),
            new ReleaseItem("2026-10-14", "V3W", "已上线",
                    "服务大厅筛选链接与售后说明校验",
                    "前端服务大厅将游戏、平台、类型和关键词筛选写入可分享网址，刷新及浏览器返回时恢复选择；后端在客户订单补充说明与售后提交入口拒绝正文中的系统保留售后标记，避免普通说明伪装成售后记录或伪造分类。前端 PR #429、后端 PR #430、统一版本 PR #431 与生产门禁均已完成。无数据库或配置变更，不启用真实收款和企业微信写入。"),
            new ReleaseItem("2026-10-14", "V3V", "已上线",
                    "售后问题记录详情查看",
                    "新版业务工作台可从已授权售后列表打开只读详情，完整查看业务编号、订单号、状态、更新时间、客户说明及内部备注；长内容保留换行并作为纯文本显示。只读角色可查看但不能调整归属，沿用 order:view 与既有数据范围。功能 PR #426、统一版本 PR #427 和生产门禁已完成。此页面不改变订单、退款、收款或企业微信写入。"),
            new ReleaseItem("2026-10-14", "V3U", "已上线",
                    "售后问题记录按类型筛选",
                    "新版业务工作台按客户提交时的交付或进度、取消服务、退款核对、其他问题四类筛选售后记录；类型与关键词、order:view 和 ALL / DEPARTMENT / SELF 数据范围一起进入数据库计数与分页。功能 PR #418、统一版本 PR #420 及生产门禁已完成。此筛选不是工单处理状态，不触发退款或收款，V3H 小程序支付与企业微信写入继续独立验收。"),
            new ReleaseItem("2026-10-14", "V3T", "已上线",
                    "售后问题记录全范围检索与分页",
                    "新版业务工作台在 order:view 和 ALL / DEPARTMENT / SELF 数据范围内，按业务编号、订单号或客户说明检索售后问题记录；数据库先过滤并计数，再按更新时间分页，每页 50 条。客户说明仍作为纯文本展示。功能 PR #415、统一版本 PR #416 与生产门禁已完成。订单、退款、支付和企业微信写入行为不变。"),
            new ReleaseItem("2026-10-14", "V3S", "已上线",
                    "售后问题记录与客户订单回显",
                    "新版业务工作台按 order:view 与 ALL / DEPARTMENT / SELF 范围集中查看带售后说明标记的订单；数据库先过滤再截取最近 100 条。客户订单详情显示已提交的售后说明，提交后重新读取授权详情并即时回显。记录不代表工单处理或退款状态；真实收款与企业微信写入继续独立验收。"),
            new ReleaseItem("2026-10-14", "V3R", "已上线",
                    "异常订单、客户售后说明与服务大厅恢复体验",
                    "业务工作台按 order:view、ALL / DEPARTMENT / SELF 范围和 EXCEPTION 状态独立查询异常订单，并分别显示客户说明与内部备注。客户可在个人中心提交分类售后问题，由客服核对；服务端校验订单归属、备注容量并记审计，不自动取消订单或发起退款。服务大厅数据读取失败时保留已有内容并可重试，筛选无结果时可清除条件。真实收款与企业微信写入仍独立验收。"),
            new ReleaseItem("2026-10-14", "V3Q", "已上线",
                    "微信支付后台操作权限收口",
                    "网站 Native 支付单读取要求 payment:view；客服确认金额后建单与关闭待支付单要求 payment:manage。旧版 ADMIN 保留全权，普通 OPERATOR 默认只读，明确授予支付管理权限的角色才可写入。真实收款开关保持关闭，小程序支付仍独立验收。"),
            new ReleaseItem("2026-10-14", "V3P", "已上线",
                    "网站微信支付只读运营概览",
                    "新版集成配置页增加网站微信支付单状态汇总与最近记录；仅查询数据库，不调用支付接口、不读取二维码或回调原文，也不提供创建、改价、关闭或启用收款入口。V3O 已通过零停机门禁上线；企业微信写入与小程序支付继续独立验收。"),
            new ReleaseItem("2026-10-14", "V3O", "已上线",
                    "企业微信与微信支付集成配置工作台",
                    "新版后台增加受权限保护的服务器配置检查，逐项显示企业微信读取与微信 Native 支付的配置状态，不返回密钥、证书内容或路径。员工写入 PR #372 和小程序支付 PR #384 仍独立待验收；本阶段不启用真实收款、不发布小程序。"),
            new ReleaseItem("2026-10-14", "V3N", "已上线",
                    "Frontend Next 与后台韧性统一交付",
                    "整合 Frontend Next A1–A4 的公共壳层、首页、服务、询价/记录、登录与个人中心，以及 V3M 后台运行时韧性；接收星火迷彩追踪器 1.0.13 源码并补全网站二维码扫码入口。前后端总联调已覆盖公开与后台边界、响应式、SEO 和版本漂移，并通过 PR、主线 CI 与 18080/8081/8080 零停机门禁上线。预览版真机扫码与原网页登录主链路已验证；小程序正式版发布、异常与跨设备边界仍待单独验收。V3H 真实收款与 PR #372 企业微信写入不在本版范围。"),
            new ReleaseItem("2026-10-14", "V3M", "已上线",
                    "新版后台运行时韧性",
                    "增加请求超时、周期复核、页面可见与网络恢复复核、stale 降级和权限失败关闭；与 Frontend Next 在 PR #393 完成组合验证。"),
            new ReleaseItem("2026-10-14", "V3L", "已上线",
                    "后台运行版本自校验",
                    "新版后台会把页面渲染的版本/阶段与 /api/admin/xingwei/health 返回的实际运行版本/阶段逐项核对；一致时显示运行版本已核对，浏览器缓存或异常发布造成不一致时立即显示版本漂移告警，并禁止再把单纯 HTTP 200 误判为完整健康。"),
            new ReleaseItem("2026-10-14", "V3K", "已上线",
                    "版本与更新记录自动巡检",
                    "新增 CI 版本真值校验：自动读取控制台版本/阶段并核对后台首页、更新记录、当前 ReleaseItem、V3H 待验收状态和已启用模块的 planned 标记；任一处遗漏同步都会阻断 PR/主线 CI。"),
            new ReleaseItem("2026-10-14", "V3J", "已上线",
                    "新版后台生产状态一致性与版本真值守卫",
                    "将已部署的 V3I 从发布候选同步为生产已上线；微信支付保留待真机验收状态，生产发布流水线标记为已启用。首页阶段标签改为集中版本真源，并由契约测试阻止已上线能力再次显示为候选或规划中。"),
            new ReleaseItem("2026-10-14", "V3I", "已上线",
                    "游戏/分类前台可见性确认与过期预览防护",
                    "启停前显示当前与操作后的前台服务数量、父级游戏阻断和风险提示；提交携带影响快照 token，预览后数据发生变化会拒绝旧确认并要求重新查看。Java/Maven 与 Chromium/WebKit 自动化验证通过，并已通过零停机生产发布流程上线。"),
            new ReleaseItem("2026-10-14", "V3H", "待真机验收",
                    "小程序微信支付闭环",
                    "JSAPI/小程序支付、直接 openid 绑定、Native/小程序通道互斥、支付状态服务端核验已完成；PR #384 保持未合并，待 AppID/商户绑定和小额真机联调。"),
            new ReleaseItem("2026-10-14", "V3G", "已上线",
                    "网站侧小程序登录接口",
                    "网站登录码、二维码和原浏览器二次确认接口已上线。实际客户端改以星火迷彩追踪器为准，星火迷彩追踪器源码已随 V3N 纳入仓库；预览版真机扫码主链路已验证，正式小程序发布和异常边界仍待单独验收。"),
            new ReleaseItem("2026-10-14", "V3F", "已上线",
                    "游戏与分类受控新增及资料编辑",
                    "新增游戏/分类默认停用；资料编辑不改变 code、归属和启停状态；启停继续走影响预览、原因和审计。"),
            new ReleaseItem("2026-10-13", "V3E", "已上线",
                    "游戏与分类影响预览及受控启停",
                    "启停前展示分类、服务和公开候选影响，要求填写原因并立即刷新公开可见性缓存。"),
            new ReleaseItem("2026-10-13", "V3A-V3D", "已上线",
                    "新版服务目录工作台",
                    "完成服务目录只读工作台、基础编辑、服务新增、恢复上架发布检查，并继续复用现有业务真源。"),
            new ReleaseItem("2026-10-13", "V2E-V2G", "已上线",
                    "业务工作台、员工工作台与企业微信目录",
                    "询价/订单进入新版数据范围工作台，员工领取与履约入口上线，企业微信通讯录只读接入。"),
            new ReleaseItem("2026-10-13", "V2A-V2C", "已上线",
                    "组织、RBAC 与数据范围底座",
                    "原控制台版本 20261013-v2c：七大中心、155 岗位、角色权限与 ALL / DEPARTMENT / SELF 数据范围成为新版后台授权基础。")
    );

    @GetMapping("/admin/xingwei")
    String console(Model model, CsrfToken csrf, Authentication authentication) {
        common(model, csrf, authentication);
        model.addAttribute("consoleVersion", CONSOLE_VERSION);
        model.addAttribute("consoleStage", CONSOLE_STAGE);
        model.addAttribute("releaseHistory", RELEASE_HISTORY);
        return "admin-xingwei";
    }

    @GetMapping("/admin/xingwei/releases")
    String releases(Model model, CsrfToken csrf, Authentication authentication) {
        common(model, csrf, authentication);
        model.addAttribute("consoleVersion", CONSOLE_VERSION);
        model.addAttribute("releaseHistory", RELEASE_HISTORY);
        return "admin-xingwei-releases";
    }

    @GetMapping("/admin/xingwei/business")
    String businessWorkbench(Model model, CsrfToken csrf, Authentication authentication) {
        common(model, csrf, authentication);
        model.addAttribute("businessVersion", BUSINESS_VERSION);
        return "admin-xingwei-business";
    }

    @GetMapping("/admin/xingwei/after-sales/cases")
    String afterSalesCases(Model model, CsrfToken csrf, Authentication authentication) {
        common(model, csrf, authentication);
        return "admin-xingwei-after-sales";
    }

    @GetMapping("/admin/xingwei/work")
    String employeeWorkbench(Model model, CsrfToken csrf, Authentication authentication) {
        common(model, csrf, authentication);
        model.addAttribute("workVersion", WORK_VERSION);
        return "admin-xingwei-work";
    }

    @GetMapping("/admin/xingwei/wecom")
    String weComDirectory(Model model, CsrfToken csrf, Authentication authentication) {
        common(model, csrf, authentication);
        model.addAttribute("weComVersion", WECOM_VERSION);
        return "admin-xingwei-wecom";
    }

    @GetMapping("/admin/xingwei/catalog")
    String serviceCatalog(Model model, CsrfToken csrf, Authentication authentication) {
        common(model, csrf, authentication);
        model.addAttribute("catalogVersion", CATALOG_VERSION);
        return "admin-xingwei-catalog";
    }

    @GetMapping("/admin/xingwei/taxonomy")
    String taxonomyControl(Model model, CsrfToken csrf, Authentication authentication) {
        common(model, csrf, authentication);
        model.addAttribute("taxonomyVersion", TAXONOMY_VERSION);
        return "admin-xingwei-taxonomy";
    }

    @GetMapping("/api/admin/xingwei/health")
    @ResponseBody
    Map<String, Object> health(Authentication authentication) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("ok", true);
        result.put("workspace", "星维网络服务工作室");
        result.put("version", CONSOLE_VERSION);
        result.put("stage", CONSOLE_STAGE);
        result.put("user", authentication == null ? "unknown" : authentication.getName());
        result.put("serverTime", OffsetDateTime.now().toString());
        return result;
    }

    private void common(Model model, CsrfToken csrf, Authentication authentication) {
        model.addAttribute("csrf", csrf);
        model.addAttribute("username", authentication == null ? "admin" : authentication.getName());
        model.addAttribute("workspaceName", "星维网络服务工作室");
    }

    public record ReleaseItem(String date, String version, String status, String title, String detail) {}
}