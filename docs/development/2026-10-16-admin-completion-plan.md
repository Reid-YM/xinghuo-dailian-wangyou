# 新版后台整体开发验收清单

范围：用户于 2026-10-16 确认先完成新版后台；新版前端替换不在本轮。保留旧后台回退。依据 docs/xingwei-admin-migration.md 与当前首页模块逐项验收，而不是以某个版本号作为整体完成标准。

| 能力 | 开发及自动化验收证据 | 发布状态 |
| --- | --- | --- |
| 组织、岗位、员工档案、RBAC、ALL/DEPARTMENT/SELF | V63、XingweiPermissionGuardTest、OrgRbacContractTest、AdminPlaywrightTest；ADMIN 恢复与 OPERATOR 兼容保留 | 原能力已上线；本次整体回归 |
| 询价、订单、派单、员工工作台 | PR #494；BusinessScope/MutationServiceTest、WorkServiceTest、Business/Actions/Work Playwright；归属、转单、版本冲突、派单 | 纳入最后统一发布 |
| 服务、游戏、分类、可见性发布检查 | ServicePublishReviewServiceTest、TaxonomyControlServiceTest、Catalog/PublishReview/Taxonomy Playwright | 原能力已上线；本次整体回归 |
| 售后处理、历史、客户补充、待跟进与时效 | V4A–V4M；AfterSalesCase/Attention/Category/Age ServiceTest 和 Playwright；V4M Stable #214 已发布 | 原能力已上线；本次整体回归 |
| 官网/淘宝/闲鱼渠道运营 | PR #491，CI 38839725481 双绿；ChannelOperationsService/Security/PlaywrightTest | 纳入最后统一发布 |
| 攻略内容 | PR #495，CI 38845063830 双绿；GuideWorkspaceServiceTest、GuideWorkspacePlaywrightTest | 纳入最后统一发布 |
| 前台控制、站点/SEO | PR #493，CI 38842249630 双绿；SiteControlService/Persistence/Template/PlaywrightTest | 纳入最后统一发布 |
| 账号安全 | 新安全页与 SecurityPageTest；保留原认证，员工与会员边界见 account-and-tool-boundaries 文档 | 纳入最后统一发布；不新增认证写入 |
| 操作审计 | PR #492，CI 38840381831 双绿；AuditQueryService/PlaywrightTest，按范围隐藏敏感变更 | 纳入最后统一发布 |
| 发布治理与回滚 | PR/CI/精确SHA/18080/8081/8080；V5A 版本真值及实际校验器负向测试 | 最终精确主线 CI 与统一发布待执行 |
| 代码生成/字典工具 | 已完成评估：参数复用现有配置；代码生成不适配既有权限/审计与数据层，暂不引入 | 评估完成，无生产工具新增 |


支付真实收款、退款外部交易、企业微信写入、真实员工/小程序真机验收按用户本轮确认的独立边界处理。不得将其描述为已完成或误开启。上述覆盖原迁移计划的后台范围；会员/社区等辅助模块保留旧入口的边界已逐项写明，不将其声称为原生迁移。当前代码待最后 CI、统一生产发布与发布后核验，尚不能宣称整体交付完成。

## 渠道运营验收规则

- 仅新增内部运营数据，不调用淘宝/闲鱼写接口，不代替平台真实经营状态。
- 三个渠道：官网、淘宝、闲鱼；状态默认“待核对”，由有 channel:manage 且 ALL 范围人员统一维护。
- 任务支持新增、编辑、负责人、所属中心、截止日期、待处理/进行中/已完成/已取消；取消保留记录。
- channel:view/manage + ALL/DEPARTMENT/SELF 在服务端执行；操作员兼容模式仅查看。
- 所有写入使用版本号并记录审计；冲突明确提示，表单保留输入；失败不伪报成功。
- 筛选、搜索、分页、URL恢复；空、错、权限、加载状态；手机与桌面可用。
- 新增 V75 两张表，既有表和迁移保持不变，精确内容评审后发布。JAR 回退保留新增表，不声称回滚数据库。

## 发布节奏

用户追加要求：当前已发起的 V4M Stable #214 完成后，不再按子阶段部署；后续全部开发与联调通过后只做最后的统一生产发布。开发中可正常创建 PR、运行 CI 和整合 main，不更新 deploy/production。

V4M Stable 已由 Deploy Production #214 于 2026-10-16 10:09 完成，提交 0d098f0e76ada4ddb3b334d5db1c95bbf0bf6eeb；18080、8081、8080、发布后核验通过，Nginx=8080、8081停止。release=/opt/xinghuo-v2/releases/xinghuo-v2-standard-20261016100747.jar，SHA256=b46d1d4c8e7cb387a0333cd919ca4170d4a5d2bfec26c3a0b93b2786b16640bc。

## V5A 统一交付方式

当前包使用 V5A · UNIFIED DELIVERY 和“本版本”描述功能范围，不提前声称生产门禁成功。CI 仍校验版本、页面回退、运行时健康与记录一致；新增交付模式只接受对应“本版本”记录，未知阶段直接失败。实际生产状态以部署流水线精确 SHA/端口门禁为准。这样不需要为把待发布改为已上线再做第二次部署。

最终发布只新增 V75 两张渠道表。精确 blob ba2638d5b1b0354fd133c67a43cbbc38ee95745d 已登记；所有历史 migration 不变。回滚使用已验证 V4M release `/opt/xinghuo-v2/releases/xinghuo-v2-standard-20261016100747.jar`，保留新增表，不将 JAR 回退描述为数据库回退。
