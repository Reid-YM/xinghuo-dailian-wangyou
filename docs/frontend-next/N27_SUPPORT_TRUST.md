# N27 公共支持与信任页 Next 预览

## 目标

N27 继续 Frontend Next 最终替换阶段，把高频公共辅助入口中的“一对一咨询”和“信任中心”留在 `/next/**` 体验内，同时坚持复用现有后台配置和公开数据，不复制另一套客服、保障或案例数据。

## 新增预览路由

- `/next/contact`：一对一咨询
- `/next/trust`：信任中心

两个路由都委托给既有 `FrontendNextPreviewController` 共享文档；正式 `/contact` 与 `/trust` 不被替换。

## `/next/contact`

直接复用 `/api/site-config` 已公开配置：

- `contactChannels`
- `wecomKefuUrl`
- `serviceStatus`
- `statusShortText`
- `quoteEnabled`

页面展示当前真实客服渠道和服务状态，并提供 `/next/quote` 询价入口。外部客服链接只允许无账号密码的 HTTPS URL，站内链接只允许单斜杠相对路径；可直接联系的链接继续带 `data-support-direct`，复用现有 `contact_click` 埋点。

## `/next/trust`

直接复用 `/api/site-config` 中：

- `trustTags`
- `trustCases`
- `trustFaq`

并只读现有 `GET /api/public/cases`，最多展示 6 条服务端明确允许公开的脱敏完成案例。公开案例读取失败只退化为空状态，不影响信任页主体。

## 路由连续性

N14 在 Next 环境中新增：

- `/contact` -> `/next/contact`
- `/trust` -> `/next/trust`

N27 页面上的旧版对照入口使用 `data-n27-legacy="1"`，因此仍可明确返回正式旧页。

N27 runtime 继续通过 N14 的 64 位内容指纹 manifest 按需加载，不使用日期版本字符串。

## 法律内容边界

N27 **不复制** `/rules` 或 `/privacy` 正文，也不把这两个链接归一为 Next。

服务规则和隐私政策仍以正式页面为唯一内容源，避免同一法律文本在 Legacy 与 Next 两份实现中长期漂移。法律页单一来源改造留给 N28。

## 安全边界

- 不切换正式默认前端；
- 不替换正式 `/contact`、`/trust`；
- 不新增客服、业务记录、订单、支付等写 API；
- N27 唯一额外请求为只读 `GET /api/public/cases`；
- 页面使用 `textContent` / DOM 节点渲染，不使用 `innerHTML` 或 `insertAdjacentHTML`；
- `/next/contact`、`/next/trust` canonical 分别指回正式 `/contact`、`/trust`，预览页使用 `noindex,follow`；
- `/rules` 与 `/privacy` 继续作为正式法律内容源。

## 验证要求

N27 合约测试锁定：

1. 两个显式预览路由委托共享 Next 文档；
2. 客服和信任数据只来自既有公开配置；
3. 公开案例请求只读；
4. 不出现 POST / PUT / PATCH / DELETE；
5. 不使用 HTML 注入式渲染；
6. Contact / Trust 留在 Next，Rules / Privacy 不被归一；
7. N27 runtime 使用 N14 的阶段内容指纹机制。

N27 仍属于开发验证阶段，在合并与部署前不能视为生产 `/next/**` 已发布能力。
