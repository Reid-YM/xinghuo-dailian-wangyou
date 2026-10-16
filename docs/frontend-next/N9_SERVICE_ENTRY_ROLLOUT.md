# N9 · 服务入口灰度控制（仅开发）

状态：开发分支完成首版，未部署、未合并、未切换正式流量。

## 目标

在保持旧版正式前端为默认值的前提下，为公共“服务”入口增加一个可审计、可回退的双态控制：

- `legacy` → `/services`（默认）
- `next` → `/next/services`（候选预览）

N9 只改变公共页头、动态手机导航抽屉与页脚中的服务入口目标，不删除、不重定向旧 `/services` 路由。

## 开发实现

- `SiteSettingService` 新增 `servicesEntryMode`，默认 `legacy`，仅允许 `legacy` / `next`。
- 非法或历史脏值读取时强制归一为 `legacy`，写入非法值直接拒绝。
- `global-ui-controls.js` 根据公开设置统一更新页头、页脚与手机抽屉服务链接，同时保留现有 `xh:site-config` 事件联动。
- 新增 `/admin/xingwei/frontend-next` 管理页，从现有“前台展示控制”进入。
- 管理页复用 `/api/admin/global-ui-settings`，沿用现有 `GLOBAL_UI_SETTINGS_UPDATE` 审计，不新增业务写接口。
- 切换到 `next` 前要求二次确认；失败时恢复当前 UI 状态。

## 安全边界

- 不修改 `/`、`/quote`、`/account` 默认路由。
- 不修改服务详情深链。
- 不调用询价、订单、售后、支付、退款或企业微信写接口。
- 不新增数据库迁移。
- 不启用 `app.frontend-next.default-enabled`。
- 不包含部署、合并或正式切流动作。

## 验证

新增：

- `SiteSettingServiceRolloutTest`
- `FrontendNextN9ServiceEntryContractTest`

契约覆盖默认值、白名单、非法值拒绝、旧路由保留、审计接口复用以及禁止触碰业务写接口等边界。
