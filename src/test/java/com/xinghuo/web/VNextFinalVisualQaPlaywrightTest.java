package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

@EnabledIfEnvironmentVariable(named = "PLAYWRIGHT_UI_TESTS", matches = "1")
class VNextFinalVisualQaPlaywrightTest {
    private static final Path ASSETS = Path.of("src/main/resources/static/assets");
    private static final List<String> PAGES = List.of(
            "account-login",
            "account-register",
            "account-reset",
            "guides",
            "guide-detail",
            "mastery",
            "rules",
            "privacy",
            "contact",
            "trust",
            "order");

    @Test
    void chromiumFinalQaCoversMobileAndWideProductionSurfaces() throws Exception {
        try (Playwright playwright = Playwright.create(); Browser browser = playwright.chromium().launch()) {
            verifySuite(browser, 375, 812, true, true, "Chromium WeChat 375");
            verifySuite(browser, 1440, 900, false, false, "Chromium desktop 1440");
            verifySuite(browser, 1920, 1080, false, false, "Chromium desktop 1920");
            verifySuite(browser, 2560, 1440, false, false, "Chromium desktop 2560");
        }
    }

    @Test
    void webkitFinalQaCoversIphoneAndWechatSurfaces() throws Exception {
        try (Playwright playwright = Playwright.create(); Browser browser = playwright.webkit().launch()) {
            verifySuite(browser, 390, 844, true, false, "iPhone Safari 390");
            verifySuite(browser, 430, 932, true, true, "iPhone WeChat 430");
        }
    }

    private static void verifySuite(Browser browser, int width, int height, boolean mobile, boolean wechat, String label)
            throws Exception {
        for (String pageName : PAGES) {
            verifyPage(browser, pageName, width, height, mobile, wechat, label + " / " + pageName);
        }
    }

    private static void verifyPage(
            Browser browser, String pageName, int width, int height, boolean mobile, boolean wechat, String label)
            throws Exception {
        String userAgent = wechat
                ? "Mozilla/5.0 (iPhone; CPU iPhone OS 19_0 like Mac OS X) AppleWebKit/605.1.15 MicroMessenger/8.0.60 Mobile/15E148"
                : mobile
                    ? "Mozilla/5.0 (iPhone; CPU iPhone OS 19_0 like Mac OS X) AppleWebKit/605.1.15 Version/19.0 Mobile/15E148 Safari/604.1"
                    : "Mozilla/5.0 AppleWebKit/537.36 Chrome/141 Safari/537.36";

        Page page = browser.newPage(new Browser.NewPageOptions()
                .setViewportSize(width, height)
                .setUserAgent(userAgent));
        page.setContent(fixture(pageName, wechat));
        addStyles(page, pageName);

        assertNoHorizontalOverflow(page, label);
        assertCriticalContentInsideViewport(page, label);

        Number mainWidth = number(page, "document.querySelector('main.container').getBoundingClientRect().width");
        if (width >= 2400) {
            assertTrue(mainWidth.doubleValue() >= 1900.0,
                    () -> label + " regressed to a narrow 2560 canvas: " + mainWidth + "px");
        } else if (width >= 1920) {
            assertTrue(mainWidth.doubleValue() >= 1700.0,
                    () -> label + " regressed to a narrow 1920 canvas: " + mainWidth + "px");
        }

        if (mobile && width <= 720) {
            assertEquals("grid", string(page, "getComputedStyle(document.querySelector('.mobile-dock')).display"),
                    label + " mobile dock must remain visible");
            Number dockTargetHeight = number(page, "document.querySelector('.mobile-dock a').getBoundingClientRect().height");
            assertTrue(dockTargetHeight.doubleValue() >= 44.0,
                    () -> label + " mobile dock target below 44px: " + dockTargetHeight);
            Number bottomPadding = number(page,
                    "parseFloat(getComputedStyle(document.body).paddingBottom) || 0");
            assertTrue(bottomPadding.doubleValue() >= 70.0,
                    () -> label + " body does not reserve space for floating dock: " + bottomPadding);

            Number smallInputs = number(page, """
                    Array.from(document.querySelectorAll('input,select,textarea'))
                      .filter(el => getComputedStyle(el).display !== 'none')
                      .filter(el => parseFloat(getComputedStyle(el).fontSize) < 16).length
                    """);
            assertEquals(0, smallInputs.intValue(), label + " contains an input below the iOS 16px zoom threshold");
        }

        Number cards = number(page,
                "Array.from(document.querySelectorAll('main .card')).filter(el => getComputedStyle(el).display !== 'none').length");
        assertTrue(cards.intValue() > 0, label + " fixture lost its primary card surface");
        page.close();
    }

    private static void addStyles(Page page, String pageName) throws Exception {
        page.addStyleTag(new Page.AddStyleTagOptions().setPath(ASSETS.resolve("app.css")));
        if (pageName.startsWith("account-")) {
            page.addStyleTag(new Page.AddStyleTagOptions().setPath(ASSETS.resolve("account.css")));
        }
        if (pageName.startsWith("guide")) {
            page.addStyleTag(new Page.AddStyleTagOptions().setPath(ASSETS.resolve("guide.css")));
        }
        page.addStyleTag(new Page.AddStyleTagOptions().setPath(ASSETS.resolve("site-experience-v2.css")));
        page.addStyleTag(new Page.AddStyleTagOptions().setPath(ASSETS.resolve("vnext-public-finish-v4.css")));
        page.addStyleTag(new Page.AddStyleTagOptions().setPath(ASSETS.resolve("vnext-final-qa-fix-v5.css")));
    }

    private static void assertNoHorizontalOverflow(Page page, String label) {
        Number scrollWidth = number(page, "document.documentElement.scrollWidth");
        Number clientWidth = number(page, "document.documentElement.clientWidth");
        assertTrue(scrollWidth.doubleValue() <= clientWidth.doubleValue() + 1,
                () -> label + " horizontal overflow: " + scrollWidth + " > " + clientWidth);
    }

    private static void assertCriticalContentInsideViewport(Page page, String label) {
        Number offenders = number(page, """
                (() => Array.from(document.querySelectorAll('main .card, main input, main select, main textarea, main .btn'))
                  .filter(el => {
                    const style = getComputedStyle(el);
                    if (style.display === 'none' || style.visibility === 'hidden') return false;
                    const r = el.getBoundingClientRect();
                    if (r.width === 0 || r.height === 0) return false;
                    return r.left < -1 || r.right > innerWidth + 1;
                  }).length)()
                """);
        assertEquals(0, offenders.intValue(), label + " has clipped cards or controls");
    }

    private static Number number(Page page, String expression) {
        return (Number) page.evaluate("() => " + expression);
    }

    private static String string(Page page, String expression) {
        return String.valueOf(page.evaluate("() => " + expression));
    }

    private static String fixture(String pageName, boolean wechat) {
        return """
                <html class="%s">
                <body data-page="%s">
                  %s
                  %s
                  <nav class="mobile-dock" aria-label="手机快捷操作"><a>首页</a><a>业务记录</a><a class="primary">提前询价</a></nav>
                  %s
                </body></html>
                """.formatted(
                wechat ? "is-wechat is-mobile is-ios" : "",
                pageName,
                header(),
                main(pageName),
                footer());
    }

    private static String header() {
        return """
                <header class="site-header" data-experience-shell="v3" style="-webkit-backdrop-filter:none!important;backdrop-filter:none!important;transition:none!important">
                  <div class="container topbar">
                    <a class="brand brand-mark"><span>星火网游代练平台</span></a>
                    <nav class="nav"><a>服务</a><a>攻略</a><a>MW4 工具站</a><a>业务记录</a><a>一对一咨询</a><a>个人中心</a><a class="btn primary desktop-keep">提前询价</a></nav>
                    <button class="nav-toggle" aria-label="打开导航"><span></span><span></span><span></span></button>
                  </div>
                </header>
                """;
    }

    private static String footer() {
        return """
                <footer class="site-footer"><div class="container footer-row"><a class="footer-credential">星维网络服务工作室</a><nav class="footer-links"><a>服务大厅</a><a>业务记录</a><a>服务规则</a><a>隐私说明</a></nav></div><div class="container beian-row">蜀ICP备2026047129号</div></footer>
                """;
    }

    private static String main(String pageName) {
        if (pageName.startsWith("account-")) {
            String extra = pageName.equals("account-register")
                    ? "<ol class='auth-steps'><li><b>1</b><span>填写邮箱</span></li><li><b>2</b><span>设置密码</span></li></ol>"
                    : "<div class='auth-value-grid'><div><strong>记录归档</strong><span>业务状态统一留痕。</span></div><div><strong>进度同步</strong><span>随时查看交付状态。</span></div><div><strong>设备可控</strong><span>安全管理登录设备。</span></div></div>";
            return """
                    <main class="container section auth-shell">
                      <section class="auth-value-panel"><span class="eyebrow">账号中心</span><h1>集中管理业务编号与交付进度</h1><p>不在网页提交游戏账号密码、验证码或令牌。</p>%s<div class="detail-list"><strong>安全说明</strong><span>需要帮助时联系人工客服。</span></div></section>
                      <section class="auth-card card"><div class="auth-card-head"><span class="eyebrow">账号</span><h2>继续使用服务</h2></div><form class="account-form"><div class="account-block"><label class="field">邮箱<input type="email" value="test@example.com"></label><label class="field">密码<input type="password" value="Password123!"></label></div><button class="btn primary auth-submit" type="button">确认</button></form><div class="auth-card-foot"><a>联系客服</a></div></section>
                    </main>
                    """.formatted(extra);
        }
        if (pageName.equals("guides") || pageName.equals("guide-detail")) {
            return """
                    <main class="container guide-shell"><section class="guide-hero card"><div><span class="eyebrow">星火攻略中心</span><h1>多游戏攻略与任务路径</h1><p>查看当前任务、推荐推进顺序与安全说明。</p></div><div class="guide-hero-stat"><strong>12</strong><span>篇攻略</span></div></section><form class="guide-search card"><label>搜索<input value="MW4"></label><label>游戏代码<input value="CODMW4"></label><button class="btn primary" type="button">筛选攻略</button></form><section class="guide-grid"><a class="card guide-card"><div class="guide-card-body"><span class="tag">MW4</span><h2>赛季任务攻略</h2><p>任务路径与完成建议。</p></div></a><a class="card guide-card"><div class="guide-card-body"><span class="tag">MW4</span><h2>武器升级攻略</h2><p>配件与挑战路线。</p></div></a></section></main>
                    """;
        }
        if (pageName.equals("rules") || pageName.equals("privacy")) {
            return """
                    <main class="container section page-shell"><div class="card rules-card rules-console"><span class="eyebrow">说明</span><h1>服务规则与信息处理</h1><section><h2>报价与确认</h2><p>提交需求后按实际任务确认服务范围。</p></section><section><h2>信息安全</h2><p>网页不收账号密码、验证码或令牌。</p></section><section><h2>售后与联系</h2><p>如需处理问题，请通过业务编号联系客服。</p></section></div></main>
                    """;
        }
        if (pageName.equals("contact")) {
            return """
                    <main class="container section page-shell"><div class="card contact-grid contact-console"><span class="eyebrow">人工客服</span><h1>一对一咨询</h1><p class="contact-intro">发送业务编号或截图说明需求。</p><div class="contact-channel-grid"><article class="contact-channel-card card"><strong>企业微信客服</strong><p>确认需求和交付时间。</p><button class="btn primary">联系客服</button></article></div><div class="contact-rows-grid"><div class="contact-row"><span>微信</span><span class="contact-value">AAC9878</span><button class="btn">复制</button></div></div><div class="notice contact-safety">网页不要发送密码或验证码。</div></div></main>
                    """;
        }
        if (pageName.equals("trust")) {
            return """
                    <main class="container section page-shell trust-page"><div class="page-hero page-hero-compact"><div><span class="eyebrow">服务说明</span><h1>备案信息与常见问题</h1></div><div class="page-hero-actions"><a class="btn primary">提前询价</a></div></div><section class="trust-verify"><div><strong>经营主体</strong><span>星维网络服务工作室</span></div><div><strong>网站备案</strong><span>蜀ICP备2026047129号</span></div></section><section class="trust-grid"><article class="card trust-case"><span class="tag">武器升级</span><h2>MW4 武器升级</h2><p>按当前进度确认。</p></article><article class="card trust-case"><span class="tag">活动任务</span><h2>赛季任务</h2><p>按任务报价。</p></article></section><section class="card rules-console faq-console"><span class="eyebrow">FAQ</span><section><h2>怎么查进度？</h2><p>通过业务编号查询。</p></section></section></main>
                    """;
        }
        if (pageName.equals("order")) {
            return """
                    <main class="container section page-shell"><div class="card order-card task-card"><span class="eyebrow">业务进度</span><h1>查询业务编号</h1><form id="orderLookupForm"><div class="field"><label>业务编号<input value="XH7KM4Q2AZ8P"></label></div><button class="btn primary btn-wide" type="button">查询进度</button></form><div class="info-strip"><div><strong>统一编号</strong></div><div><strong>无需注册</strong></div><div><strong>客服同步</strong></div></div></div></main>
                    """;
        }
        return """
                <main class="container section page-shell"><section class="card"><span class="eyebrow">MW4 工具站</span><h1>任务与精通进度工具</h1><p>在一个页面查看挑战、任务和进度。</p><div class="field"><label>搜索任务<input value="迷彩挑战"></label></div><div class="grid4"><article class="card"><h2>武器任务</h2><p>当前进度</p></article><article class="card"><h2>赛季任务</h2><p>当前进度</p></article></div></section></main>
                """;
    }
}
