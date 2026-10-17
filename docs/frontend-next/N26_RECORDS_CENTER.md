# N26 业务记录中心 Next 预览

N26 补齐 N13 单条业务记录详情之前缺少的“列表 / 查询入口”，新增显式 `/next/records`，并让 Frontend Next 与旧 `/records` 共用同一份浏览器业务编号历史。

## 目标

- 新增 `/next/records` 显式预览入口，不替换正式 `/records`。
- 支持输入统一业务编号查询现有公开业务记录。
- 展示当前浏览器最近保存的业务编号与最新公开状态。
- 卡片继续进入 N13 `/next/record/{businessNo}`。
- 与旧站共用 `localStorage` 键 `xh_client_records_v1`，不复制第二份历史。
- Next 询价成功和 N13 记录详情自动把业务编号写入同一历史键。
- 清除动作只删除当前浏览器编号索引，不删除服务器业务数据。

## 数据与安全边界

N26 只读取现有公开接口：

- `GET /api/records/{businessNo}`

业务编号继续使用现有格式：

- `XH` + 10 位大写字母或数字；
- `XH-` + 24 位大写字母 / `2-9` 数字。

N26 不新增订单、支付、退款、售后或记录写 API。

## 本机历史连续性

旧版 `public.js` 使用：

- `xh_client_records_v1`

N26 的记录中心和历史桥接 runtime 继续使用完全相同的 key，并保持最多最近 30 条。因此：

- 旧版已有记录会直接显示在 `/next/records`；
- Next 询价成功后出现 `/next/record/{businessNo}` 链接时会记录编号；
- 打开 N13 详情也会再次确认该编号进入本机历史；
- 切回旧 `/records` 时仍能看到同一批编号。

## N26 history bridge

`n26-record-history-bridge.js` 仅在：

- `/next/quote`
- `/next/record/{businessNo}`

加载。

它负责：

1. 监听 N3 提交成功生成的 `/next/record/...` 链接并保存业务编号；
2. N13 详情成功渲染后保存当前业务编号；
3. 在 N13 的操作区增加“业务记录中心”入口。

桥接脚本不读取或写入服务器业务数据，只维护本机编号索引。

## 路由连续性

N14 从 N26 起增加：

- `/records` -> `/next/records`
- `/record/{businessNo}` -> `/next/record/{businessNo}`（此前 N13/N14 已有）

N26 页面自身使用 `data-n26-legacy="1"` 明确保留旧 `/records` 对照入口。

## SEO 与回退

- canonical: `https://wydlgf.cn/records`
- robots: `noindex,nofollow`
- 正式 `/records` 始终保留。
- 当前阶段只在叠加开发分支验证；未经合并和生产预览发布前不属于线上能力。

## 后续

N26 完成后，核心交易浏览链已经具备服务、询价、记录查询、记录详情、账号订单 / 询价详情的 Next 连续性。后续重点转向 Contact / Trust / Legal 等公共辅助页统一，以及全站移动端、错误态、空状态和正式切换前全链路验收。
