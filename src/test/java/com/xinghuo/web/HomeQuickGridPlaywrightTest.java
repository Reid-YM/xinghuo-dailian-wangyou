package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

@EnabledIfEnvironmentVariable(named = "PLAYWRIGHT_UI_TESTS", matches = "1")
class HomeQuickGridPlaywrightTest {
    private static final Path ASSETS = Path.of("src/main/resources/static/assets");

    @Test
    void quickActionsNeverOverlapAcrossDesktopBreakpoints() {
        try (Playwright playwright = Playwright.create();
             Browser chromium = playwright.chromium().launch();
             Browser webkit = playwright.webkit().launch()) {
            assertWideTwoColumnLayout(chromium, 1920, "Chromium 1920");
            assertStackedIntermediateLayout(chromium, 1100, "Chromium 1100");
            assertStackedIntermediateLayout(webkit, 1100, "WebKit 1100");
        }
    }

    private static void assertWideTwoColumnLayout(Browser browser, int width, String label) {
        Page page = browser.newPage(new Browser.NewPageOptions().setViewportSize(width, 900));
        renderFixture(page);

        double[] quote = rect(page, ".quote-card-home");
        double[] lookup = rect(page, ".desktop-secondary-panel");
        double[] contact = rect(page, ".quick-contact-card");

        assertTrue(quote[2] <= lookup[0] + 1,
                () -> label + " quote card crossed into the right column");
        assertTrue(Math.abs(lookup[0] - contact[0]) <= 2,
                () -> label + " right-column cards no longer share the same left edge");
        assertTrue(Math.abs(lookup[2] - contact[2]) <= 2,
                () -> label + " contact card escaped the right-column width");
        assertTrue(contact[1] >= lookup[3] - 1,
                () -> label + " contact card overlaps the business lookup card");
        assertNoAreaIntersection(quote, contact, label + " quote/contact");
        assertNoHorizontalOverflow(page, label);
        page.close();
    }

    private static void assertStackedIntermediateLayout(Browser browser, int width, String label) {
        Page page = browser.newPage(new Browser.NewPageOptions().setViewportSize(width, 900));
        renderFixture(page);

        double[] quote = rect(page, ".quote-card-home");
        double[] contact = rect(page, ".quick-contact-card");
        String lookupDisplay = String.valueOf(page.evaluate(
                "() => getComputedStyle(document.querySelector('.desktop-secondary-panel')).display"));

        assertTrue("none".equals(lookupDisplay),
                () -> label + " must preserve the existing hidden business lookup panel at <=1180px");
        assertTrue(Math.abs(quote[0] - contact[0]) <= 2,
                () -> label + " contact card created an implicit second column");
        assertTrue(Math.abs(quote[2] - contact[2]) <= 2,
                () -> label + " contact card escaped the single-column grid width");
        assertTrue(contact[1] >= quote[3] - 1,
                () -> label + " contact card overlaps the quote card");
        assertNoAreaIntersection(quote, contact, label + " quote/contact");
        assertNoHorizontalOverflow(page, label);
        page.close();
    }

    private static void renderFixture(Page page) {
        page.setContent("""
                <body data-page="home">
                  <main class="container">
                    <section class="section quick-grid quick-grid-r3" id="quick-actions">
                      <div class="card quick-card quote-card quote-card-home">
                        <div class="quote-card-head"><span class="eyebrow">快速询价</span><h2>选择需求，获取报价</h2></div>
                        <form>
                          <div class="linked-selects four-step">
                            <div class="field"><label>游戏</label><select><option>请选择游戏</option></select></div>
                            <div class="field"><label>游戏平台</label><select><option>请选择平台</option></select></div>
                            <div class="field"><label>需求类型</label><select><option>请选择需求</option></select></div>
                            <div class="field"><label>具体服务</label><select><option>请选择服务</option></select></div>
                          </div>
                          <div class="quote-extras-content quote-extras-home">
                            <div class="field"><label>当前进度</label><select><option>不确定</option></select></div>
                            <div class="field"><label>联系方式</label><input value="微信号"></div>
                            <div class="field upload-field"><label>任务截图（可选）</label><div class="upload-zone">上传任务截图</div></div>
                          </div>
                        </form>
                      </div>
                      <div class="card quick-card desktop-secondary-panel">
                        <div class="quote-card-head"><span class="eyebrow">业务进度</span><h2>查询业务编号</h2></div>
                        <div class="field"><label>业务编号</label><input value="XH123456"></div>
                        <button class="btn ghost btn-wide">查询进度</button>
                      </div>
                      <div class="card quick-contact-card">
                        <div class="quick-contact-head"><span class="eyebrow">客服联系</span></div>
                        <div class="quick-contact-row"><span>微信</span><strong>AAC9878</strong><button class="btn small-btn">复制</button></div>
                        <div class="quick-contact-time"><span>在线时间</span><span>10:00–24:00</span></div>
                      </div>
                    </section>
                  </main>
                </body>
                """);
        page.addStyleTag(new Page.AddStyleTagOptions().setPath(ASSETS.resolve("app.css")));
        page.addStyleTag(new Page.AddStyleTagOptions().setPath(ASSETS.resolve("home-quick-grid.css")));
    }

    private static double[] rect(Page page, String selector) {
        Object value = page.evaluate("""
                selector => {
                  const r = document.querySelector(selector).getBoundingClientRect();
                  return [r.left, r.top, r.right, r.bottom];
                }
                """, selector);
        java.util.List<?> list = (java.util.List<?>) value;
        return new double[] {
                ((Number) list.get(0)).doubleValue(),
                ((Number) list.get(1)).doubleValue(),
                ((Number) list.get(2)).doubleValue(),
                ((Number) list.get(3)).doubleValue()
        };
    }

    private static void assertNoHorizontalOverflow(Page page, String label) {
        Number scrollWidth = (Number) page.evaluate("() => document.documentElement.scrollWidth");
        Number clientWidth = (Number) page.evaluate("() => document.documentElement.clientWidth");
        assertTrue(scrollWidth.doubleValue() <= clientWidth.doubleValue() + 1,
                () -> label + " overflowed horizontally: scrollWidth=" + scrollWidth
                        + ", clientWidth=" + clientWidth);
    }

    private static void assertNoAreaIntersection(double[] a, double[] b, String label) {
        double width = Math.max(0, Math.min(a[2], b[2]) - Math.max(a[0], b[0]));
        double height = Math.max(0, Math.min(a[3], b[3]) - Math.max(a[1], b[1]));
        assertTrue(width * height <= 1,
                () -> label + " cards overlap by " + (width * height) + "px²");
    }
}
