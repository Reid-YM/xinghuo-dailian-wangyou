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
| N5–N6 账号业务与设置 | 原业务列表/详情/售后与资料/安全 API；离页取消只读请求并停止挂载/轮询；写请求不取消，恢复后重新读取 | N5Business/N5Actions/N6Security；AccountContext、CanonicalRoutes、RealSession 完整文档 |
| N8–N9 整合和灰度 | 原控制器先鉴权/校验，再按关闭的开关决定转发；显式 Legacy 优先 | N8Integration、CanonicalRouteContract、CanonicalSecurity；不得触及 admin/API/POST/专用游客入口 |
| N10–N12 服务详情、推荐、询价上下文 | 真实服务字段、可接状态、相关推荐；安全文本、刷新/返回/重试 | ServicePages、CanonicalRoutes、N10–N12 契约 |
| N13 记录与游客业务 | 公开脱敏记录；共享 `assets/guest-business.js` 提供报价接受、支付状态、履约信息 | GuestBusiness 双引擎与原 GuestUnpaidOrderCheckout/PaidGuestOrderCompletion；403/503/重试、迟到响应、路由离开、报价取消确认；POST 不发送客户端金额 |
| N14 路由与资源 | 预览/正式路径由 shell 统一转换；每个动态资源独立 SHA256 | AssetVersion、ServerMetadata、CanonicalRoutes；脚本失败可重试，明确 Legacy 保留 |
| N16–N20 账号深链与上下文 | 询价/订单详情，前后条，来源报价，状态/排序/返回参数与刷新恢复 | 各阶段契约、CanonicalRoutes、AccountContext；同 tab 不重复 history；首次从公共页面单页进入账号时延迟初始化且只初始化一次，缓存恢复重新连接观察器；计数/空态无变化时不重写 DOM |
| N21 收藏、进度与投稿列表 | 分页 URL、资产独立路线、已发布内容、正常工具/撰写入口 | AccountContext：20+1 分页、刷新/返回、投稿→攻略→返回→撰写；明确旧版链接保留 |
| N22 攻略详情 | 正文/章节、相关服务、FAQ、相关推荐；共用 guide-community 收藏/评论/回复/举报 | N22 契约、N30GuideCommunity 连续三轮、CanonicalRoutes；普通面包屑新版，明确旧版入口 `legacy=1` |
| N23 攻略中心 | 当前 q/game/sort 发送原公开接口；服务端搜索和排序先于 limit；保留工具栏、错误重试、过期响应保护 | PublicGuideSearch：默认窗口外结果、中文/英文游戏名/标签/副标题、发布门禁、排序白名单；GuideSearch 双引擎延迟响应/错误重试/详情返回 |
| N24 MW4 工具站 | 与原本机存档兼容；大师按模式统一解锁，完整枪械挑战独立本机保存；云端首次读取、冲突选择、迟到响应与离页保存 | MasteryParity 真实目录/图片/旧记录/重置/多视口、N30MasterySync 四组双引擎，N24 契约；不得静默覆盖本机/云端进度 |
| N25 投稿 | 本机即时草稿、失败保留、提交清理、正文预览、状态/撤回/发布跳转 | N30SubmissionDraft、N29SubmissionCatalogRecovery、AccountContext；无后端编辑能力时不伪造已提交稿编辑 |
| N26 记录中心 | 共享本机记录键；实际编号查询、单条详情/返回、清除本机索引 | N30QuoteRecordJourney、N30DocumentJourney、CanonicalRoutes |
| N27 客服与保障 | 公开配置和脱敏案例；明确空/错/重试 | N29PublicCasesRecovery、N30DocumentJourney；不展示未公开业务信息 |
| N28 中英文规则/隐私 | 四份正式 HTML 单一来源；安全重建、正式源请求带 legacy 防递归 | N30LegalParity、N28LegalSingleSource；逐段比较正文、主体/客服、语言/索引/回跳 |
| N29 移动、键盘与恢复 | 底栏/安全区、地标/名称、标签键盘、弹窗焦点、必填定位、长内容换行 | N30Accessibility、N29MobileNavigation/LongContent/DataRecovery；320/390/1280px，Chromium/WebKit |
| N30 全站正式路径 | 公开/账号路线、游戏、深链、真实302、刷新/返回、登录回跳、明确Legacy；保留原服务/攻略SEO/JSONLD | CanonicalRoutes、CanonicalRouteContract、CanonicalSecurity；实际 HTTP + MVC 文档，不只单独注入脚本 |
| 独立游戏资料 | 服务端公开游戏校验、介绍/平台/服务、可接/维护/下架、SEO/OG/Breadcrumb | NextGameController/NextGamePlaywright；首页→游戏→服务→返回→询价，缺失404 |
| 安全与生产边界 | Session/归属、CSRF/请求头、公开可见性、服务端金额与原业务状态机继续生效 | Java 安全/业务回归；`app.frontend-next.default-enabled` 默认 false，无数据库迁移、生产密钥/配置/部署变更 |

## 开发验收证据与合并门禁

- 最终快照 Java 21 `mvn -B -ntp clean package`：939 项，零失败、零错误，215 项浏览器条件跳过。源码、测试及资源在构建期间冻结；浏览器由独立 UI job 执行。
- 账号生命周期及标准错误捕获更新后，本机完整 `Frontend*PlaywrightTest` 87 项中 86 项通过，1 项测试动态求值被页面 CSP 拦截；改用可见 DOM 等待后，对应 DefaultReplacement 四项全部通过。原 CSP 保持不变。
- 最新主线工具站同步后的 MasteryParity、N30MasterySync、N30Accessibility 与 AssetVersion 专项 11 项通过；随后活动重置追加断言的 MasteryParity 双引擎再次通过。攻略 SQL/API、搜索输入及迟到目录已有专项覆盖，最终全量浏览器门禁以精确提交 CI 为准。
- 已同步 `main@ffd25f55` 的支付终态/事务/查询身份/恢复公平性修复，以及最新工具数据、小程序及网站工具站更新；相对主线无支付服务差异。最后清理残留的用户可见研发阶段文案。
- `git diff --check`、33 个修改脚本语法、409 条后台搜索索引、小程序静态/回归与发布标记检查通过；无凭证模式匹配。
- 精确提交的默认 Playwright 1.63 全部浏览器与 Java 21 两项检查以 [PR #575 的状态检查](https://github.com/Reid-YM/xinghuo-dailian-wangyou/pull/575/checks) 为准。仅在全部成功后合并，并确认主线包含目标提交；构建或合并均不授权生产发布。

## 验证环境及对外可用性

此 Mac 为 macOS 13.7.8；本地使用命令覆盖 Playwright 1.48，仓库 pom 保持 1.63。最终 Chromium/WebKit 组合以 CI 默认 1.63 为准。

浏览器回归使用隔离 HTTP/MVC、业务响应夹具及测试账号存储。RealSession 使用真实认证与 Session 实现，但持久层为测试替身；游客支付仅模拟服务端正式状态，不创建生产收费。真实微信/商户/设备授权、生产订单履约和线上外部交易可用性不由这些结果证明。

## 发布与回退

本次开发不部署、不启用正式 Next。整体回退保持 `app.frontend-next.default-enabled=false`，页面级回退使用 `?legacy=1`，保留原视图和鉴权。后续若单独授权部署，应先在候选环境验证目标提交、原接口/权限及页面，监控错误率/认证/询价/支付状态，再按正式发布门禁执行。JAR 回退不能被当作数据库回滚；本次没有新增 migration。

## 账号跨页与恢复补查

- N16–N21 从公共首屏进入账号时初始化，后续离开/返回保持同一模块实例；不以“直接打开账号页”代替该旅程。N17 内部导航发布共享页面切换事件，刷新筛选和详情上下文。
- N5 只读 GET 使用 AbortController，离页或退出登录时取消未完成读取、移除旧业务面板，恢复后按当前 URL 重新读取；返回结果还校验账号对象与请求代次。POST 写入不因离页被客户端取消；迟到的读取不会覆盖新视图。
- N17–N21 在 pageshow.persisted 重新连接观察器；N20 计数和空态仅在内容变化时更新，避免 MutationObserver 自身循环导致恢复挂载饥饿。
- 新增真实点击旅程：首页→服务→账号→筛选空态→订单详情→返回→首页→再次进入账号→缓存生命周期恢复。验证同文档、同模块实例、未完成 GET 的原生失败事件及恢复重读，并检查计数空态停止变化；另有同一文档从账号 1 退出、取消旧订单读取并登录账号 2 的迟到响应隔离旅程。
- RealSession 的 HTTP 测试适配器把响应流断连与浏览器观测到的失败 GET 对应核查；MVC 异常仍断言为零。WebKit 会把部分已处理的网络诊断也报告为 PageError，因此使用标准 error/unhandledrejection 事件独立捕获所有未处理异常；原始诊断保留，其他 PageError 仍作为失败。`FrontendBrowserErrorTrackerPlaywrightTest` 在两种引擎上验证真正抛错、同网络诊断文案的抛错、未处理拒绝仍会失败，而已处理 Abort 不记作未捕获异常。最终结果以本提交 CI 为准。

## 最新工具站行为同步

- 同步 PR #583 的模式级大师勾选；写入仍使用原 `progress[mode][weapon][tier]` 存档，保留部分武器历史记录，不擅自迁移为全部完成。统一勾选才更新当前目录全部武器的对应档位，模式间独立。
- 枪械挑战展示同一 `camoGuide` 数据中的军事/特殊/完成主义、等级和要求，中文原文走安全文本；使用主线同一图片映射及其内容指纹。
- `guideDone[weapon::challenge]` 与收藏/大师进度分别保留，挑战只存本机，不计入大师统计，不随模式重置或云端选择清除；与旧版同一存储键兼容。
- 双引擎真实目录专项覆盖 33 把武器、汉86 8/3/4 挑战项、历史部分进度、统一开关、图片实际解码、刷新与重置、战区/活动独立性及 320/390/1440px 无溢出。

- 同步最新工具站后的专项：MasteryParity、N30MasterySync、N30Accessibility 与 AssetVersion 共 11 项，零失败/错误/跳过（本机兼容 Playwright 1.48，Chromium 与 WebKit）；最终默认版本全量 CI 另见 PR 检查。
