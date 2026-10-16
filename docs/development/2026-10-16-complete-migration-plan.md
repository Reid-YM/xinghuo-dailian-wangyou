# 完整迁移替换开发清单

用户要求：完整迁移替换，并检查之前替换不生效的修复。此清单取代此前“只完成后台核心迁移”的范围，不把保留旧入口视为完全迁移。开发与 CI 可以分 PR 集成，集中验证后统一发布；保留明确的回退方式，不逐阶段部署。

## 本轮开始时确认的阻断

1. 正式 /、/services、/quote、/account 仍返回旧模板；/next 只是并行入口。
2. 并行 HTML 的 go() 仅读取 hash；直接访问 /next/services、/next/quote、/next/account 时初始化为首页。
3. 多个新版账号/业务脚本使用 XMLHttpRequest 自定义头，而实际账号拦截器只接受 fetch；真实写请求可能 403，已有 browser fixture 未执行真实拦截器。
4. Next runtime 注入没有内容版本，HTML no-store 不能解决长期缓存的 JS 更新问题。
5. 新版服务读取失败沿用演示商品/价格，不能直接作为正式替换后的数据回退。
6. Next 的站点配置、客服、服务详情仍有占位行为，需补齐真实配置闭环。

复选框标记开发和自动化验证进度；生产发布以最后的发布核验项为准，不能据此宣称已上线。

## 必须完成

- [x] 统一账号自定义头，保留服务端校验；浏览器请求经过真实 AccountAuthInterceptor。
- [x] 修复路径/查询参数/hash 路由，直接访问、刷新、前进后退与安全登录回跳。
- [x] 内容指纹资源版本、真实 HTTP 路由和 CSP 验证。
- [x] 正式入口映射、清晰旧版回退，保留原网站账号认证检查。
- [x] 前端真实配置、客服、服务详情、空/错/重试，去除正式页面演示业务数据。
- [x] 账号头像上传、联系方式、注册/找回、业务/进度/售后、收藏/投稿等逐项功能对齐。
- [x] 会员、社区/投稿、反馈、首页展示位、CRM 迁入新后台，权限和审计覆盖。
- [x] 员工后台账号生命周期界面（受控创建、停用、恢复/密码重置、防锁死）。
- [x] /admin 默认新版与正式前端入口替换，功能清单中不再依赖旧主入口。
- [x] MW4 工具资料编辑、行为统计和旧概览指标全部进入新版，补齐正式前台事件/来源。
- [x] 服务封面/有效期/热度、游戏完整资料/图片/平台、业务公开案例与管理删除项的接口级功能对齐。
- [x] 原有 UI 补丁退休、后台导航空白保护等历史修复未回归。
- [ ] Java clean package、实际 Chromium/WebKit、手机/桌面、主线 CI、最后一次统一发布核验。

真实支付、外部退款、企业微信目录写入及真实员工/小程序真机验收继续保持独立授权和验收边界。完整迁移不能用自动化结果替代这些外部验收，也不会为完成迁移擅自启用资金或目录写入。

开发进度：PR #498–#506 均已合并，Java 与 Chromium/WebKit 已通过；本轮仍未部署。完整资料与业务维护补录项已通过真实 Chromium/WebKit、Java 和权限/事务测试。V6A 统一版本与发布冒烟增强正在做最终 CI，之后检查精确主线并集中发布。

## 旧菜单核对表

| 旧入口/能力 | 新版对应入口 |
| --- | --- |
| 运营概览及事件漏斗 | /admin/xingwei/statistics（全站）；业务工作台保留按范围的统计 |
| 站点配置、图片、客服、FAQ、SEO | /admin/xingwei/site |
| 导航/展示开关/主题 | /admin/xingwei/frontend-controls |
| 首页推荐位 | /admin/xingwei/operations?module=display |
| 服务项目 | /admin/xingwei/catalog；完整字段/图片/有效期 /admin/xingwei/catalog-details |
| 游戏/分类启停及新增 | /admin/xingwei/taxonomy |
| 游戏完整资料/图片/平台/批量，无依赖游戏分类删除 | /admin/xingwei/catalog-details |
| 攻略编辑和预览 | /admin/xingwei/guides |
| MW4 资料与图片 | /admin/xingwei/tool-station |
| 询价、转单、归属 | /admin/xingwei/business?tab=quotes |
| CRM 跟进 | /admin/xingwei/operations?module=crm |
| 订单、进度、派单、附件 | /admin/xingwei/business?tab=orders |
| 订单公开案例与管理删除、询价管理删除 | /admin/xingwei/maintenance |
| 支付单查询、确认金额、关闭 | /admin/xingwei/payments（开关和外部验证边界保持） |
| 售后 | /admin/xingwei/after-sales/cases |
| 网站会员及登录日志 | /admin/xingwei/operations?module=members |
| 评论/投稿审核 | /admin/xingwei/operations?module=comments 或 submissions |
| 测试反馈 | /admin/xingwei/operations?module=feedback |
| 审计记录 | /admin/xingwei/audits |
| 后台员工登录生命周期 | /admin/xingwei/security |

原 /admin?legacy=1 仅作 ADMIN 恢复，不承载普通员工的必经操作。来源中保留旧实现用于恢复，不把删除历史实现当成迁移完成条件。

## 后续细分复核

上一轮 V6A 已由 Deploy Production #217（运行 38898438359）完成发布。本清单上方“仍未部署”和未勾选的最后发布核验项是发布前记录。后续复核发现攻略虽使用新版接口，仍渲染旧模板，不能据此宣称独立界面完全迁移；本次修复、中文化与下一阶段逐项清单见 [新版后台细分复核](2026-10-16-native-admin-detail-and-chinese.md)。新修复的代码合并和生产发布分别记录，不沿用上一轮上线结论。

## 后续全量复核

此前版本已由生产发布 #218 完成上线。本次进一步审查发现的字段、日志、筛选和旧布局依赖修复，以 [新后台全量迁移审查](2026-10-16-native-admin-full-audit.md) 及 82 接口机器清单为准；此轮代码验证/合并不等于已再次部署。

## 当前生产边界与下一阶段

PR #512 与正式前端回退 PR #513 已由生产发布 #219（主线 6b81a246）完成。正式前端现使用旧版，新版后台保留；上方正式前端替换记录是历史开发/发布过程，不是当前启用要求。再次切换正式前端必须单独获得明确授权。

下一阶段见 [员工运营流程连续性](2026-10-16-operations-workflow-continuity.md)：先处理未保存保护、过期请求和员工角色流程，再依据真实员工反馈继续优化。本阶段开发/CI 不等于自动获得新的生产发布授权。

PR #514 运营流程修复已由生产发布 #220 完成。按用户“部署完开始下一阶段”要求，随后推进 [业务派单与订单编辑连续性](2026-10-16-business-workflow-continuity.md)；本阶段未改变旧版正式前端默认设置。

PR #515 业务派单修复已由生产发布 #221 完成；PR #516 版本与交付信息收纳已由生产发布 #222 完成。按用户授权先部署后继续，随后进入 [目录完整资料编辑连续性](2026-10-17-catalog-details-workflow.md)，处理目录维护中的草稿、图片、重复提交和读取异常；正式前端仍保持旧版。
