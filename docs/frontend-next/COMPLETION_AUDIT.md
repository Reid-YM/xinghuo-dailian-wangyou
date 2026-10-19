# 前端完整开发验收清单

## 当前结论（2026-10-19）

N1–N30、独立游戏资料页、正式路径适配及游客业务模块已进入同一开发分支 `feat/frontend-completion-20261019`，PR #575。开发验收由下列逐项证据、完整构建及 [PR #575](https://github.com/Reid-YM/xinghuo-dailian-wangyou/pull/575) 精确提交的默认 Chromium/WebKit CI 共同构成；仅在全部门禁成功后合并。生产部署及正式流量切换不在本次授权内。

原分阶段文档记录各阶段初始状态；其中“后续实现”“仍回旧版”和旧测试数量属于历史。本清单描述当前整合范围，不以早期 CI 绿灯代替最新代码验证。

## 需求与可核查证据

| 范围 | 当前实现 | 验证来源及最终验收要求 |
| --- | --- | --- |
| N1–N2 首页、目录、客服、服务筛选 | `shell.js` / `n2.js`，读取真实公开配置/目录；失败可重试 | `FrontendNextConversionPlaywrightTest`、`FrontendNextServicePagesPlaywrightTest`、N8 整合、N30 完整文档；手机/桌面、空/错/重试 |
| N3 询价 | 原询价接口、附件、幂等键；不由客户端确认收费金额 | `FrontendNextN3QuotePlaywrightTest`、N30QuoteRecordJourney；提交失败保留及重复提交边界 |
| N4、N7、N15 认证 | 密码/注册/重置、微信/小程序入口、安全回跳 | N4/N4Mini/N7/AuthReturn 专项；`FrontendRealSessionPlaywrightTest` 实际 BCrypt、Session 签发/撤销、HTTP Cookie、刷新/登出/失效 |
| N5–N6 账号业务与设置 | 原业务列表/详情/售后与资料/安全 API；离页停止挂载和轮询，恢复后重新挂载 | N5Business/N5Actions/N6Security；AccountContext、CanonicalRoutes、RealSession 完整文档 |
| N8–N9 整合和灰度 | 原控制器先鉴权/校验，再按关闭的开关决定转发；显式 Legacy 优先 | N8Integration、CanonicalRouteContract、CanonicalSecurity；不得触及 admin/API/POST/专用游客入口 |
| N10–N12 服务详情、推荐、询价上下文 | 真实服务字段、可接状态、相关推荐；安全文本、刷新/返回/重试 | ServicePages、CanonicalRoutes、N10–N12 契约 |
| N13 记录与游客业务 | 公开脱敏记录；共享 `assets/guest-business.js` 提供报价接受、支付状态、履约信息 | GuestBusiness 双引擎与原 GuestUnpaidOrderCheckout/PaidGuestOrderCompletion；403/503/重试、迟到响应、路由离开、报价取消确认；POST 不发送客户端金额 |
| N14 路由与资源 | 预览/正式路径由 shell 统一转换；每个动态资源独立 SHA256 | AssetVersion、ServerMetadata、CanonicalRoutes；脚本失败可重试，明确 Legacy 保留 |
| N16–N20 账号深链与上下文 | 询价/订单详情，前后条，来源报价，状态/排序/返回参数与刷新恢复 | 各阶段契约、CanonicalRoutes、AccountContext；同 tab 不重复 history |
| N21 收藏、进度与投稿列表 | 分页 URL、资产独立路线、已发布内容、正常工具/撰写入口 | AccountContext：20+1 分页、刷新/返回、投稿→攻略→返回→撰写；明确旧版链接保留 |
| N22 攻略详情 | 正文/章节、相关服务、FAQ、相关推荐；共用 guide-community 收藏/评论/回复/举报 | N22 契约、N30GuideCommunity 连续三轮、CanonicalRoutes；普通面包屑新版，明确旧版入口 `legacy=1` |
| N23 攻略中心 | 当前 q/game/sort 发送原公开接口；服务端搜索和排序先于 limit；保留工具栏、错误重试、过期响应保护 | PublicGuideSearch：默认窗口外结果、中文/英文游戏名/标签/副标题、发布门禁、排序白名单；GuideSearch 双引擎延迟响应/错误重试/详情返回 |
| N24 MW4 工具站 | 与原本机存档兼容；云端首次读取、冲突选择、迟到响应与离页保存 | N30MasterySync 四组双引擎，N24 契约；不得静默覆盖本机/云端进度 |
| N25 投稿 | 本机即时草稿、失败保留、提交清理、正文预览、状态/撤回/发布跳转 | N30SubmissionDraft、N29SubmissionCatalogRecovery、AccountContext；无后端编辑能力时不伪造已提交稿编辑 |
| N26 记录中心 | 共享本机记录键；实际编号查询、单条详情/返回、清除本机索引 | N30QuoteRecordJourney、N30DocumentJourney、CanonicalRoutes |
| N27 客服与保障 | 公开配置和脱敏案例；明确空/错/重试 | N29PublicCasesRecovery、N30DocumentJourney；不展示未公开业务信息 |
| N28 中英文规则/隐私 | 四份正式 HTML 单一来源；安全重建、正式源请求带 legacy 防递归 | N30LegalParity、N28LegalSingleSource；逐段比较正文、主体/客服、语言/索引/回跳 |
| N29 移动、键盘与恢复 | 底栏/安全区、地标/名称、标签键盘、弹窗焦点、必填定位、长内容换行 | N30Accessibility、N29MobileNavigation/LongContent/DataRecovery；320/390/1280px，Chromium/WebKit |
| N30 全站正式路径 | 公开/账号路线、游戏、深链、真实302、刷新/返回、登录回跳、明确Legacy；保留原服务/攻略SEO/JSONLD | CanonicalRoutes、CanonicalRouteContract、CanonicalSecurity；实际 HTTP + MVC 文档，不只单独注入脚本 |
| 独立游戏资料 | 服务端公开游戏校验、介绍/平台/服务、可接/维护/下架、SEO/OG/Breadcrumb | NextGameController/NextGamePlaywright；首页→游戏→服务→返回→询价，缺失404 |
| 安全与生产边界 | Session/归属、CSRF/请求头、公开可见性、服务端金额与原业务状态机继续生效 | Java 安全/业务回归；`app.frontend-next.default-enabled` 默认 false，无数据库迁移、生产密钥/配置/部署变更 |

## 开发验收证据与合并门禁

- 最终 Java 21 `mvn -B -ntp clean package`：928 项，零失败、零错误，206 项浏览器条件跳过。源码、测试和资源在运行期间保持一致；浏览器由独立 UI job 执行。
- 账号生命周期修复后的 CanonicalRoutes + AccountContext：6 项，零失败/错误/跳过。实际测试引擎包括 Chromium 与 WebKit。
- 攻略 SQL/API 与浏览器专项（含账号关联）9 项通过；完整 `Frontend*PlaywrightTest` 本机回归 81 项，零失败/错误/跳过。随后补上搜索输入/中文输入法与迟到游戏目录的处理，相关最终专项 14 项全部通过（包含 SQL/API、搜索输入、整页、可达性、N8 和 N23 契约）。最终完整构建与 CI 另见下文，不能用专项结果替代。
- 已同步 `main@823f06fb936ab9505099814713022db06584f713` 的支付终态、事务与查询身份修复；相对主线无支付服务差异。最后清理残留的用户可见研发阶段文案。
- `git diff --check`、28 个修改脚本语法、409 条后台搜索索引、小程序静态/回归与发布标记检查通过；无凭证模式匹配。
- 精确提交的默认 Playwright 1.63 全部浏览器与 Java 21 两项检查以 [PR #575 的状态检查](https://github.com/Reid-YM/xinghuo-dailian-wangyou/pull/575/checks) 为准。仅在全部成功后合并，并确认主线包含目标提交；构建或合并均不授权生产发布。

## 验证环境及对外可用性

此 Mac 为 macOS 13.7.8；本地使用命令覆盖 Playwright 1.48，仓库 pom 保持 1.63。最终 Chromium/WebKit 组合以 CI 默认 1.63 为准。

浏览器回归使用隔离 HTTP/MVC、业务响应夹具及测试账号存储。RealSession 使用真实认证与 Session 实现，但持久层为测试替身；游客支付仅模拟服务端正式状态，不创建生产收费。真实微信/商户/设备授权、生产订单履约和线上外部交易可用性不由这些结果证明。

## 发布与回退

本次开发不部署、不启用正式 Next。整体回退保持 `app.frontend-next.default-enabled=false`，页面级回退使用 `?legacy=1`，保留原视图和鉴权。后续若单独授权部署，应先在候选环境验证目标提交、原接口/权限及页面，监控错误率/认证/询价/支付状态，再按正式发布门禁执行。JAR 回退不能被当作数据库回滚；本次没有新增 migration。
