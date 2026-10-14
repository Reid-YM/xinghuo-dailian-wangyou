# V4I 售后问题类型分布与快捷筛选

日期：2026-10-15

状态：开发候选，未发布生产。

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

## 修改文件

- `src/main/java/com/xinghuo/service/XingweiAfterSalesCategorySummaryService.java`
- `src/main/java/com/xinghuo/controller/XingweiAfterSalesCategorySummaryController.java`
- `src/main/resources/templates/admin-xingwei-after-sales.html`
- `src/main/resources/static/assets/admin-xingwei-after-sales.js`
- `src/main/resources/static/assets/admin-xingwei-after-sales.css`
- `src/test/java/com/xinghuo/service/XingweiAfterSalesCategorySummaryServiceTest.java`
- `src/test/java/com/xinghuo/controller/XingweiAfterSalesCategorySummaryControllerTest.java`
- `src/test/java/com/xinghuo/web/XingweiAfterSalesCategorySummaryPlaywrightTest.java`

## 风险边界

- 无数据库迁移。
- 无生产配置变更。
- 无订单、售后状态、退款、支付或企业微信写入变更。
- 全部新增能力为只读统计和前端筛选入口。
- 在正式切 V4I Release 前继续保留 V4H 页面版本与资产契约，避免把未发布功能误标为稳定生产。

## 测试覆盖

- 服务层：SELF / DEPARTMENT / ALL / DENIED 范围与 `order:view` 权限。
- 控制器：四个稳定 JSON 字段。
- 浏览器：分类数字渲染、分类快捷筛选、其他筛选条件保留、页码归零。

## 下一步

1. 创建 PR 并跑 Java/Maven 与 Playwright CI。
2. CI 通过后检查与当前 main 的差异和 release-state 守卫。
3. 功能 PR 合并后，再按现有 V4 系列流程切统一 V4I Release Candidate；届时统一更新版本、资产缓存参数、Release History 和生产发布记录。
