# N21 个人资产路由连续性

## 目标

N21 继续收口 Frontend Next 个人中心中的「工具存档、收藏、投稿」区域，不新建第二套个人资产数据或 API。

本阶段新增显式预览路由：

- `/next/account/favorites`
- `/next/account/submissions`

已有工具存档继续使用：

- `/next/account/progress`

三者都复用 N5 已存在的真实账号数据和操作。

## 现有真实能力

N5 当前已经读取：

- `GET /api/account/progress`
- `GET /api/account/favorites?page={page}&size=20`
- `GET /api/account/submissions?page={page}&size=20`

收藏取消和投稿撤回仍由 N5 原实现处理。N21 本身不新增 fetch，也不复制这些请求。

收藏和投稿现有分页是 N5 内部真实分页：每页 20 条，上一页 / 下一页会调用同一账号接口。N21 仅把当前页同步到 URL，并在刷新或浏览器前进后退时驱动现有分页按钮恢复页面。

## URL 状态

收藏和投稿使用 1-based `page`：

- `/next/account/favorites?page=2`
- `/next/account/submissions?page=3`

规则：

- 第 1 页会规范为无 `page` 参数；
- 只接受 `1..1000` 的整数；
- 非法、越界或无法到达的页码会回退到真实可到达页面；
- 不使用 localStorage / sessionStorage；
- 未登录时 N16 会把完整 Next 子路由和查询参数继续放入安全 `next` 回跳目标。

## 个人资产子导航

N21 在现有 `#n5PersonalAssets` 内增加三个入口：

- 工具存档
- 我的收藏
- 我的投稿

不扩充 N17 顶部五项主导航。`favorites` 和 `submissions` 在 N17 中继续归属于现有「工具存档」一级区域，具体位置由 N21 子导航高亮。

## 链接迁移边界

N21 不为尚不存在的页面伪造 Next 路由。

明确继续使用旧版入口：

- 工具站 `/mastery`
- 投稿编辑 `/account/submit`
- 攻略详情 `/guides/{slug}`

这些链接会标记为显式 legacy，避免被 Next 路由归一逻辑误处理。

收藏目标中：

- `SERVICE /service/{slug}`：交给现有 N14 归一到 `/next/service/{slug}`；
- `GAME /game/{seoSlug}`：只有 N14 能用真实 N2 catalog 找到 `seoSlug -> game code` 时才进入 `/next/services?game=...`；否则保留原旧版链接，并允许 catalog 后续加载后再次尝试；
- `GUIDE`：当前没有 Next 攻略详情页，继续旧版。

## 后端与安全边界

N21：

- 不新增后端 API；
- 不新增 fetch；
- 不新增业务写操作；
- 不改变收藏取消、投稿撤回语义；
- 不改变 Session / CSRF / 账号归属校验；
- 不新增客户端持久化；
- 不开放支付、退款或其它业务动作。

## 代码范围

核心 runtime：

`src/main/resources/static/next/n21-account-personal-assets.js`

同时扩展：

- `FrontendNextPreviewController`：新增显式预览路由和 N21 runtime / 指纹；
- `n16-account-deep-links.js`：识别并定位收藏、投稿；
- `n17-account-navigation.js`：让收藏、投稿继续归属个人资产一级导航；
- `FrontendNextN21AccountPersonalAssetsContractTest`：锁定路由、分页、legacy 边界和无新增网络写入约束。

## 发布状态

N21 只在 Frontend Next 开发分支开发。

- 不部署；
- 不合并 `main`；
- 不切正式流量；
- 旧正式前端继续保留；
- `app.frontend-next.default-enabled=false` 不变。

静态契约已写入仓库，但本阶段不把“存在测试文件”等同于 Maven / Playwright / CI 已实际执行通过。
