# N25 投稿工作台 Next 预览

N25 将旧 `/account/submit` 的真实能力迁入 Frontend Next，新增显式 `/next/account/submit`。当前用户侧后端只支持新建投稿、读取自己的投稿/详情和受控撤回，不提供“修改已提交稿件”接口，因此 N25 严格按真实能力实现，不伪造在线编辑已提交内容。

> 当前阶段为开发与组合 CI 验证态；在合并并执行生产预览发布前，正式 `/account/submit` 与当前线上 `/next` 均保持原状态。

## 目标

- 新增 `/next/account/submit` 显式预览入口。
- 复用现有账号投稿 API，不增加第二套投稿模型。
- 与旧投稿页共用本机草稿键 `xinghuo:submission-draft:v1`。
- 支持游戏选择、标题、摘要、Markdown 文本正文、本机自动草稿、安全预览。
- 支持查看本人投稿状态、审核意见、投稿正文、已发布攻略和可撤回状态。
- 已发布攻略直接进入 N22 `/next/guides/{slug}`。
- 未登录时引导进入 Next 账号登录流程，并携带返回 `/next/account/submit` 的目标。

## 真实接口

N25 继续使用：

- `GET /api/account/me`
- `GET /api/catalog`
- `GET /api/account/submissions?size=50`
- `GET /api/account/submissions/{id}`
- `POST /api/account/submissions`
- `POST /api/account/submissions/{id}/withdraw`

所有账号请求继续使用同源 cookie，并携带 `X-Requested-With: fetch`。投稿创建、详情和撤回仍由服务端账号归属校验保护。

## 草稿连续性

旧版和 N25 共用：

- `localStorage`: `xinghuo:submission-draft:v1`

因此旧版未提交的本机草稿可直接在 N25 恢复；N25 自动保存的草稿回到旧页也能继续读取。

成功提交后才清除草稿；接口失败时保留用户输入。

## 安全预览

预览只识别：

- `#` / `##` / `###` 标题；
- `-` / `*` / 数字列表；
- 普通段落。

全部内容通过 `textContent` 创建，不使用 `innerHTML` 或 `insertAdjacentHTML`，用户输入的 HTML 与脚本不会被执行。

## 投稿状态

继续展示服务端真实状态：

- `SUBMITTED`
- `IN_REVIEW`
- `APPROVED`
- `REJECTED`
- `PUBLISHED`
- `WITHDRAWN`

仅 `SUBMITTED` / `IN_REVIEW` 提供撤回动作。当前 API 没有用户侧更新已提交投稿，因此 N25 明确不提供虚假的“继续编辑”按钮。

## 路由连续性

N21 过去会给 `/account/submit` 打显式 legacy 标记。N25 在 N14 中仅对来自 N21 的该精确目标解除 legacy，让个人资产中的“撰写与管理投稿”进入 `/next/account/submit`。

N25 自身仍提供 `data-n25-legacy="1"` 的旧 `/account/submit` 对照入口。

## SEO 与部署边界

- canonical: `https://wydlgf.cn/account/submit`
- robots: `noindex,nofollow`
- 正式旧 `/account/submit` 不被替换。
- 当前阶段仅开发和 CI 验证；未合并/部署前不能视为线上 `/next` 能力。

## 后续

N25 完成后，最终替换阶段主要剩余全站辅助页面与体验统一、移动端/错误态/空状态收口、真实全链路验收和正式灰度切换准备。
