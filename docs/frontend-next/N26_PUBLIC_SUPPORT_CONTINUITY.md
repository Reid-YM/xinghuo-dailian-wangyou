# N26 公开辅助页面 Next 连续性

## 目标

收口三个仍会明显把用户从 Frontend Next 带回 Legacy 的公开入口：

- 业务记录入口 `/records`
- 一对一咨询 `/contact`
- 信任与 FAQ `/trust`

N26 新增对应显式预览路由：

- `/next/records`
- `/next/contact`
- `/next/trust`

正式旧页面继续保留，不改变默认前端。

## 业务记录 `/next/records`

N13 已有 `/next/record/{businessNo}` 详情页，N26 补齐查询入口与浏览器历史列表。

为保证旧版与 Next 不分叉，继续使用 Legacy 已有本机记录 key：

`xh_client_records_v1`

行为：

- 读取同一浏览器最多 30 条最近业务编号；
- 支持 `XH[A-Z0-9]{10}` 与 `XH-[A-Z2-9]{24}` 两种正式编号；
- 查询继续使用只读 `GET /api/records/{businessNo}`；
- 查询成功后更新同一份本机最近记录；
- 进入 `/next/record/{businessNo}?from=records`；
- 最近记录直接链接 N13 Next 详情。

因此用户从旧 `/records` 切到 Next 后仍能看到相同的本机历史编号。

## 联系页 `/next/contact`

继续读取现有站点公开配置，不建立第二份客服配置：

- `contactIntro`
- `contactChannels`
- `wechatId` / `wechatQrImage`
- 企业微信客服 URL、标签、说明与按钮文案
- QQ / 电话 / 客服时间
- 淘宝 / 闲鱼入口与标签
- 主体名称与联系安全提示

配置优先复用 `XHFrontendShell.configurationReady`，必要时只读 `GET /api/site-config`。

外部链接仅接受 HTTP/HTTPS 或明确的微信协议；二维码、联系方式和动态文案均通过安全 DOM API 渲染。

## 信任与 FAQ `/next/trust`

继续读取现有 `SiteConfigService.publicConfig()` 中的：

- `legalEntityName`
- `trustCases`
- `trustFaq`

同时保留现有公开备案核验信息：

- 蜀ICP备2026047129号
- 川公网安备51162102000262号

业务编号核验入口改为 `/next/records`，使用户继续留在 Next 路径族。

## 路由连续性

N14 从 N26 起增加：

- `/records` -> `/next/records`
- `/contact` -> `/next/contact`
- `/trust` -> `/next/trust`

显式 `legacy=1` 或 `data-n26-legacy="1"` 链接继续留在旧版。

## 数据与安全边界

N26 本身没有业务写 API：

- 业务记录查询是 GET；
- 联系页与信任页只读公开站点配置；
- 不新增 POST / PUT / PATCH / DELETE；
- 不新增数据库 migration；
- 不修改订单、支付、退款、售后。

唯一浏览器持久化是 `/records` 为兼容旧版而有意复用的 `xh_client_records_v1`，不会创建新的第二份记录存储。

## SEO 与回退

Next 辅助页都作为并行预览：

- `/next/records` canonical -> `https://wydlgf.cn/records`
- `/next/contact` canonical -> `https://wydlgf.cn/contact`
- `/next/trust` canonical -> `https://wydlgf.cn/trust`
- robots 均为 `noindex,follow`

旧页面仍通过 `?legacy=1` 明确保留。

## 后续

N27 进入剩余账号辅助入口与跨页连续性收口，随后 N28 做全站视觉、加载、空状态、错误状态和移动端一致性统一。