# V5A 新版后台统一交付

## 范围及结果

本轮按 docs/xingwei-admin-migration.md 的后台迁移计划完成原生业务工作台、渠道任务、攻略、审计、站点/SEO 与前台控制，并沿用已上线的组织权限、员工工作台、服务目录和售后闭环。具体逐项证据见 2026-10-16-admin-completion-plan.md。新版前端替换不在本轮。

补充开发 PR：#491 渠道、#492 审计、#493 站点控制、#494 业务动作、#495 攻略、#496 账号安全说明。原认证及旧后台继续保留；会员、社区、反馈等辅助管理仍由旧后台承载，不声称已经原生迁移。账号与工具边界详见 2026-10-16-account-and-tool-boundaries.md。

## 验证

- 本机 Java 21 全量 clean package 通过；最后增加发布模式负向测试后 package 通过：525 tests，0 failures，0 errors，100 skipped。
- 本机 macOS 13 不支持当前 Playwright 浏览器；全部 `*PlaywrightTest` 由 Linux CI 实际执行 Chromium/WebKit，不能将本机跳过记为浏览器通过。
- 新版数据范围、写权限、并发冲突、审计事务、草稿保留、旧入口兼容和手机布局分别由各模块服务/控制器/模板/浏览器检查覆盖；最终以本 PR 及精确 main CI 双绿为发布前置条件。
- 实际 release-state 脚本验证：统一交付版本需要“本版本”记录；伪标已上线及未知阶段均拒绝。旧 STABLE PRODUCTION / RELEASE CANDIDATE 的对应状态门禁继续保留。

## 数据与回退

相对最后生产 V4M 0d098f0e76ada4ddb3b334d5db1c95bbf0bf6eeb，唯一新增数据库文件为 V75__xingwei_channel_operations.sql。该迁移只新增内部渠道状态与任务表，初始三渠道 UNKNOWN；不修改旧表，不改变官网服务开关，不调用淘宝/闲鱼 API。精确 Git blob ba2638d5b1b0354fd133c67a43cbbc38ee95745d 已在评审清单及测试登记。

旧 JAR 不依赖两张新表，故允许候选/正式实例并行。回退目标为 `/opt/xinghuo-v2/releases/xinghuo-v2-standard-20261016100747.jar`，SHA256 b46d1d4c8e7cb387a0333cd919ca4170d4a5d2bfec26c3a0b93b2786b16640bc。回退保留 V75 表及已写任务；不尝试删除表或假定 JAR 回退等于数据库回退。

## 一次统一发布

用户已授权整体完成后最终部署。本 PR 和精确 main CI 全通过后才更新 deploy/production 的唯一 .deploy-request，运行既有 Deploy Production：服务器全量构建、18080 预发布、8081 金丝雀、8080 正式及发布后核验。V5A · UNIFIED DELIVERY 表示运行包范围，不提前断言这些门禁成功；部署证据单独归档，无需为状态文字再部署。

不启用真实收款、退款外部交易、企业微信写入，不将真实员工或小程序真机验收记为已完成。没有生产配置、凭据或认证数据改动。
