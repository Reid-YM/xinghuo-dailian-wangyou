# N24 账号投稿控制台 Next 化

## 目标

把 N21 个人资产区中仍固定到 Legacy 的 `/account/submit` 投稿入口纳入 Frontend Next，形成 `/next/account/submit` 投稿撰写、状态查看与撤回闭环。

N24 只迁移现有后端已经真实支持的能力，不伪造“编辑已提交投稿”。

## 真实后端能力

继续复用 `AccountSubmissionApiController`：

- `GET /api/account/submissions?size=50`：读取本人投稿；
- `POST /api/account/submissions`：创建新投稿；
- `POST /api/account/submissions/{id}/withdraw`：撤回处于允许状态的本人投稿；
- `GET /api/account/me`：确认账号状态；
- `GET /api/catalog`：读取可投稿游戏目录。

现有控制器没有 `PUT` / `PATCH` 投稿更新接口，所以 N24 不提供虚假的“修改已提交投稿”能力。Legacy 页实际核心能力同样是撰写新稿、查看审核状态和撤回。

## 新增路由

- `/next/account/submit`
- `/next/account/submit/`

N5 个人资产区原“撰写与管理投稿”链接会在 Next 环境内改为 `/next/account/submit`。普通 `/account/submit` 链接也由 N14 归一到 Next，除非显式 `legacy=1` 或带 N24 legacy 标记。

## 页面能力

- 游戏选择；
- 标题，最多 180 字符；
- 摘要，最多 500 字符；
- Markdown 正文；
- 只使用安全 DOM 的结构预览，不执行 Markdown HTML；
- 创建投稿；
- 查看最近投稿状态与审核意见；
- 对仍在审核流程中的投稿执行撤回；
- 已发布投稿继续进入 `/next/guides/{slug}`；
- 返回 `/next/account/submissions`；
- 显式旧版入口 `/account/submit?legacy=1`。

## 账号与安全

所有账号 API 请求继续：

- `credentials: 'same-origin'`；
- 写请求带 `X-Requested-With: fetch`；
- 由现有 `AccountAuthInterceptor` 校验账号归属与写请求安全头；
- 不放宽 Spring Security / Session / 账号数据范围。

未登录访问 `/next/account/submit` 时，N24 保留现有 Next 登录界面并写入安全返回目标 `next=/next/account/submit`，登录后回到投稿控制台。

## 草稿策略

Legacy 投稿页曾使用 `localStorage` 保存本机草稿。N24 当前不复制该持久化行为：

- 输入仅保留在当前页面内存与 DOM；
- 不向浏览器持久存储投稿正文；
- 不引入第二套草稿后端。

后续如确实需要跨刷新草稿，应单独设计账号云草稿模型与隐私/生命周期规则，而不是直接复制本机长期存储。

## SEO 与回退

- canonical：`https://wydlgf.cn/account/submit`；
- robots：`noindex,nofollow`；
- 正式 Legacy `/account/submit` 继续存在；
- `app.frontend-next.default-enabled=false` 保持不变。

## 后续

N25 继续处理工具站 / Mastery 等目前最大的公开 Legacy 功能入口。