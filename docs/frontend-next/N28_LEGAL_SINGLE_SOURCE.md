# N28 法律内容单一来源 Next 预览

## 目标

N28 补齐 `/next/rules` 与 `/next/privacy`，但不复制第二份服务规则或隐私正文。

正式 `/rules` 与 `/privacy`（以及其现有语言解析）继续作为唯一法律内容源；Frontend Next 只负责读取正式 HTML、提取允许结构并安全重建展示。

## 新增路由

- `/next/rules`
- `/next/privacy`

正式 `/rules`、`/privacy` 原样保留，不被替换。

## 单一来源机制

N28 根据当前文档只读：

- `/next/rules` -> `GET /rules`
- `/next/privacy` -> `GET /privacy`

响应按 `text/html` 读取，经 `DOMParser` 解析后只查找正式页面的 `.rules-card`。Next runtime 本身不保存规则正文、保存周期、退款条款等正式法律文案。

因此更新法律内容时仍只修改正式源页面；Next 在下一次读取时自动展示新正文。

## 安全重建

N28 **不会**把抓取到的 HTML 赋给 `innerHTML`。

允许重建的结构只有：

- 文本节点；
- 标题；
- 段落；
- `ul` / `ol` / `li`；
- `b` / `strong` / `em` / `code`；
- `br`；
- 经过安全校验的链接。

链接规则：

- 站内仅接受单斜杠相对路径；
- 外部仅接受无账号密码的 HTTPS URL；
- 正式源中的 `/rules`、`/privacy`、`/contact`、`/trust`、`/records`、`/quote` 在 Next 展示中映射到对应 `/next/**`；
- 页面顶部“打开正式原文”使用 `data-n28-legacy="1"`，明确绕过 N14 归一。

源 HTML 中的 `script`、样式和未知标签不会作为可执行/可注入节点进入 Next。

## SEO 与正式内容权属

- Next 预览使用 `noindex,follow`；
- canonical 优先读取正式源页面自己的 canonical，并限制为 `wydlgf.cn`；
- 正式源不可读取时，Next 不使用备用法律副本，只提供重试和“打开正式原文”；
- 语言版本继续由正式 `/rules`、`/privacy` 的现有 Locale 机制决定，N28 不维护第二套翻译。

## 路由连续性

N14 新增：

- `/rules` -> `/next/rules`
- `/privacy` -> `/next/privacy`

并按需加载 `n28-legal-single-source.js`，继续复用 N14 64 位内容指纹 manifest。

## 验证

N28 包含两层验证：

1. 合约测试确认正式模板拥有实际法律正文、N28 runtime 不包含对应正文，不存在写请求或 HTML 注入；
2. Playwright 真实浏览器测试用带恶意 `<script>` 的模拟正式 HTML 验证：脚本不会进入 Next DOM 或执行，法律交叉链接正确归一，canonical/noindex 和正式原文逃生口均正确。

组合 CI 临时以最新 `main` `e45d0df4e65be005655d8ec98c7291dab4c902d9` 为目标验证 N23–N28 与当前生产治理、V78 数据库迁移基线的兼容性；验证结束后 PR 恢复为 N28 -> N27 的叠加审查关系。

N28 仍属于开发验证阶段；未合并、未部署前不能视为线上 `/next/rules`、`/next/privacy` 已发布。

## 2026-10-19 双语与内容完整性

增加 `/next/rules-en`、`/next/privacy-en`；页内语言切换、同站链接和 N15 安全认证返回继续对应语言。正式 `/rules[-en]`、`/privacy[-en]` 仍是唯一正文来源，新版不保存第二份法律正文。

补查发现原渲染器只保留首条 `.rule-alert`，现保留全部提示；主体名称和客服微信按旧正式页同样的公开配置绑定；正文忽略 script/style/iframe 等非正文内容。四个正式模板的全部提示、章节标题、段落及列表文本由真实浏览器逐段核对，英文条款提示不会因迁移遗漏。英语内容与操作区域标记语言，正式原文仍可直接打开。生产发布和正式切换继续独立记录。
