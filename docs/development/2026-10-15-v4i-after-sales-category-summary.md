# V4I 售后问题类型分布与快捷筛选

日期：2026-10-15

状态：生产已发布，正在同步稳定生产真值。

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
- 统一版本 PR #474 已通过双 CI 并 squash 合入 `main`，候选主线 SHA 为 `3e1870701a158fe0cd21703864de939b1dc221d0`。
- 后台统一版本为 `20261015-v4i`；候选阶段已完成生产门禁，本状态分支只同步 `V4I · STABLE PRODUCTION` / “已上线”真值。
- 售后页 CSS/JS 缓存参数保持 `20261015v4i`。
- Deploy Production run #203 已成功发布候选主线 SHA `3e1870701a158fe0cd21703864de939b1dc221d0`。
- 服务器本地 Maven 构建共运行 449 个测试：0 Failure、0 Error；81 个需要浏览器环境的测试在该构建上下文跳过，完整浏览器回归已由 GitHub CI 单独通过。
- 18080 预发布完整冒烟通过。
- 8081 金丝雀完整冒烟通过，nginx 临时切流验证后已停止金丝雀实例。
- 正式 8080 完整冒烟通过，nginx 已切回 `127.0.0.1:8080`。
- 发布后确认 18080 / 8081 均不再监听，正式服务与 nginx 均正常。
- 生产候选 JAR SHA256：`1834201ba9c87dc50b168c0971f78493577534cd22a618cf3f77b19205acc03c`。
- 当前生产 release：`/opt/xinghuo-v2/releases/xinghuo-v2-standard-20261015145130.jar`。
- 上一生产 release 继续保留：`/opt/xinghuo-v2/releases/xinghuo-v2-standard-20261015065741.jar`，可按现有原子软链接流程回滚。

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

统一版本与生产真值阶段：

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
- 第一轮生产发布已完成；当前状态分支仅同步生产真值，不增加业务行为。
- 旧后台继续保留，不替换现有回退入口。
- 真实收款继续关闭；V3H 小程序支付与企业微信写入继续独立验收。

## 测试覆盖

- 服务层：SELF / DEPARTMENT / ALL / DENIED 范围与 `order:view` 权限。
- 控制器：四个稳定 JSON 字段。
- 浏览器：分类数字渲染、分类快捷筛选、其他筛选条件保留、页码归零。
- 统一版本：CONSOLE_VERSION / CONSOLE_STAGE / BUSINESS_VERSION、更新记录、后台首页、售后资产缓存版本、运行版本漂移与 RBAC 契约。
- 生产：main 双 CI + 18080 预发布 + 8081 金丝雀 + 8080 正式服务 + 发布后端口/JAR 哈希核验。

## 下一步

1. 状态-only 分支将 V4I 从 `RELEASE CANDIDATE / 待发布` 同步为 `STABLE PRODUCTION / 已上线`。
2. 状态 PR 跑 Java/Maven、版本真值守卫与 Chromium/WebKit 双 CI。
3. 合入后对新的精确 main SHA 再跑主线双 CI。
4. 使用同一受控 `deploy/production` 流程执行状态-only 第二次发布，使生产页面和 `/api/admin/xingwei/health` 与实际已上线状态完全一致。
