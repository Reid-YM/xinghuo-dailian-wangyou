# 完整迁移替换开发清单

用户要求：完整迁移替换，并检查之前替换不生效的修复。此清单取代此前“只完成后台核心迁移”的范围，不把保留旧入口视为完全迁移。开发与 CI 可以分 PR 集成，集中验证后统一发布；保留明确的回退方式，不逐阶段部署。

## 已确认的阻断

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
- [ ] /admin 默认新版与正式前端入口真正替换；功能清单中不再依赖旧主入口（默认切换已通过，完整字段级功能仍在核对）。
- [x] MW4 工具资料编辑、行为统计和旧概览指标全部进入新版，补齐正式前台事件/来源。
- [ ] 服务封面/有效期/热度、游戏完整资料/图片/平台、业务公开案例与管理删除项的接口级功能对齐。
- [ ] 原有 UI 补丁退休、后台导航空白保护等历史修复未回归。
- [ ] Java clean package、实际 Chromium/WebKit、手机/桌面、主线 CI、最后一次统一发布核验。

真实支付、外部退款、企业微信目录写入及真实员工/小程序真机验收继续保持独立授权和验收边界。完整迁移不能用自动化结果替代这些外部验收，也不会为完成迁移擅自启用资金或目录写入。

开发进度：PR #498–#505 均已合并，Java 与 Chromium/WebKit 已通过；本轮仍未部署。完整资料与业务维护补录项已实现，正在完成最终浏览器与 CI 验证，之后统一版本核对、主线 CI 和一次集中发布。

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
