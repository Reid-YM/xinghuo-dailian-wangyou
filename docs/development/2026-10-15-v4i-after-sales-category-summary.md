# V4I 售后问题类型分布与快捷筛选

日期：2026-10-15

状态：统一 Release Candidate，未发布生产。

## 本阶段目标

在 V4H 已上线的售后状态分布基础上，增加按当前账号数据范围统计的问题类型分布，并允许客服从概览卡片直接切换问题类型筛选。旧售后处理、订单、退款、支付和企业微信写入逻辑保持不变。

## 已完成

- 新增只读分类汇总服务，统计：交付或进度、取消服务、退款核对、其他问题。
- 分类统计沿用 `order:view` 权限与 `ALL / DEPARTMENT / SELF / DENIED` 数据范围语义。
- 新增 `GET /api/admin/xingwei/business/after-sales/cases/category-summary`。
- 售后页面新增四个问题类型概览卡片。
- 点击问题类型卡片后只更新 `category` 筛选，保留处理状态、客户补充和搜索条件，并把页码重置为第一页。
- 浏览器返回/前进继续通过 URL 恢复筛选状态。
- 分类汇总读取失败时只禁用分类卡片，不影响原售后队列和 V4H 状态概览使用。
- 功能 PR #470 已通过 Java/Maven、版本真值校验和 Chromium/WebKit CI，并 squash 合入 `main`。
- 统一版本分支已快进到包含 Frontend Next N5/N6 的最新 `main`，避免覆盖并行前端开发。
- 后台统一版本已切为 `20261015-v4i` / `V4I · RELEASE CANDIDATE`；V4I Release History 保持“待发布”，V4H 保持“已上线”。
- 售后页 CSS/JS 缓存参数已提升到 `20261015v4i`。

## 修改文件

功能阶段：

- `src/main/java/com/xinghuo/service/XingweiAfterSalesCategorySummaryService.java`
- `src/main/java/com/xinghuo/controller/XingweiAfterSalesCategorySummaryController.java`
- `src/main/resources/templates/admin-xingwei-after-sales.html`
- `src/main/resources/static/assets/admin-xingwei-after-sales.js`
- `src/main/resources/static/assets/admin-xingwei-after-sales.css`
- `src/test/java/com/xinghuo/service/XingweiAfterSalesCategorySummaryServiceTest.java`
- `src/test/java/com/xinghuo/controller/XingweiAfterSalesCategorySummaryControllerTest.java`
- `src/test/java/com/xinghuo/web/XingweiAfterSalesCategorySummaryPlaywrightTest.java`

统一版本阶段：

- `src/main/java/com/xinghuo/controller/XingweiAdminController.java`
- `src/main/resources/templates/admin-xingwei.html`
- `src/main/resources/templates/admin-xingwei-business.html`
- `src/main/resources/templates/admin-xingwei-after-sales.html`
- `src/main/resources/templates/admin-xingwei-releases.html`
- 对应版本真值、RBAC、运行时韧性和前后端总联调契约测试。

## 风险边界

- 无数据库迁移。
- 无生产配置变更。
- 无订单、售后状态、退款、支付或企业微信写入变更。
- 全部新增业务能力为只读统计和前端筛选入口。
- 当前只切 Release Candidate，不把 V4I 标记为“已上线”；生产门禁完成前 V4I 必须保持“待发布”。
- 旧后台继续保留，不替换现有回退入口。

## 测试覆盖

- 服务层：SELF / DEPARTMENT / ALL / DENIED 范围与 `order:view` 权限。
- 控制器：四个稳定 JSON 字段。
- 浏览器：分类数字渲染、分类快捷筛选、其他筛选条件保留、页码归零。
- 统一版本：CONSOLE_VERSION / CONSOLE_STAGE / BUSINESS_VERSION、更新记录、后台首页、售后资产缓存版本、运行版本漂移与 RBAC 契约。

## 下一步

1. 为统一 V4I Release Candidate 创建 PR。
2. 跑 Java/Maven、`validate-xingwei-release-state.sh` 与 Chromium/WebKit 双 CI。
3. CI 全绿后合入 `main`，再对精确主线 SHA 复核双 CI。
4. 只有生产 18080 / 8081 / 8080 门禁和发布后核验完成后，才单独把 V4I 从“待发布”同步为“已上线”。
