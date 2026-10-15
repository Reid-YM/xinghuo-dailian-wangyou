# V4L 售后待跟进优先级排序

日期：2026-10-16

状态：已上线，Stable Production 完成。

## 目标

V4K 已上线只读待跟进时效队列，可按全部需跟进、24 小时内、24–72 小时、72 小时以上进入独立队列。V4L 在不改变现有默认行为的前提下，为同一筛选结果增加可分享的优先级排序：客服既可以继续查看最新待跟进，也可以切换到最久未响应优先，从而优先处理等待时间最长的客户记录。

## 本阶段实现

- `GET /api/admin/xingwei/business/after-sales/cases/attention-queue` 新增可选 `sort` 参数。
- 支持：
  - `NEWEST_FIRST`：最新待跟进优先；
  - `OLDEST_FIRST`：最久未响应优先。
- `sort` 缺失时默认 `NEWEST_FIRST`，旧 V4K 链接、旧调用和默认列表顺序保持兼容。
- 排序基准统一使用 V4J/V4K 已定义的 `attention_at_ms`：
  - 有尚未确认的新客户补充时，使用最新补充时间；
  - 否则 OPEN 记录使用原始售后提交时间。
- 不再使用与待响应语义不完全一致的历史活动最大时间作为排序依据。
- 独立时效队列页面增加“优先顺序”筛选项。
- URL 保存 `sort`，刷新、浏览器前进/后退和分页继续保持排序条件。
- 资产缓存更新为 `admin-xingwei-after-sales-attention.js?v=20261016v4l`。

## 功能合并与精确主线验证

- 功能 PR #484：`V4L: 售后待跟进优先级排序`。
- PR #484 的 Java/Maven、版本真值守卫、Chromium + WebKit 实际交互回归全部通过。
- PR #484 已 squash 合入 `main`。
- 功能主线 SHA：`ffc9c17da74d21dcc1724deb00a04b0e0b0b84e7`。
- 精确功能主线 CI #1149 已通过 Java/Maven、Xingwei release-state、main build artifact 与 Chromium + WebKit。

## 统一 Release Candidate

- RC 分支：`feat/v4l-unified-release`。
- 统一候选 PR #485 已通过 Java/Maven、版本真值守卫、Chromium + WebKit 双 CI并 squash 合入 `main`。
- RC main SHA：`f70f64771254dd56b9c1d4b378cbaa81d4d40af8`。
- 精确 RC main CI #1151：Java/Maven、release-state、main build artifact、Chromium + WebKit 全部通过。
- 候选真值：`20261016-v4l / V4L · RELEASE CANDIDATE / V4L 待发布`。
- V4K 继续保持 `已上线`，V3H 继续 `待真机验收`。

## 候选生产门禁

- Deploy Production #211：成功。
- 部署目标：`f70f64771254dd56b9c1d4b378cbaa81d4d40af8`。
- 服务器 Maven：472 tests / 0 failures / 0 errors / 88 skipped。
- 18080 预发布完整冒烟：通过。
- 8081 金丝雀完整冒烟：通过。
- 8080 正式完整冒烟：通过。
- Nginx 最终恢复到 8080，8081 金丝雀已停止。
- 候选生产 JAR SHA256：`0f4afe01e504489dadabfe5b6fff82c832da4b5634bb7185f054251405d360c6`。
- 候选生产 release：`/opt/xinghuo-v2/releases/xinghuo-v2-standard-20261016045351.jar`。

## Stable 状态收口

- Stable 分支：`release/v4l-stable-production`。
- Stable PR #486：`V4L: 同步生产上线状态`。
- PR #486 仅修改 9 个状态 / 页面 / 契约 / 文档文件，无业务逻辑、数据库或生产配置变更。
- PR #486 的 Java/Maven、版本真值守卫、Chromium + WebKit 双 CI全部通过。
- Stable main SHA：`1c20db9df1da7d7d88ac7d2be51e2c0611e62dbb`。
- 精确 Stable main CI #1153 全绿：Java/Maven、release-state、main build artifact、Chromium + WebKit 均通过。
- 最终生产真值：`20261016-v4l / V4L · STABLE PRODUCTION / V4L 已上线`。

## 最终 Stable Production 发布

- Deploy Production #212：成功。
- 部署目标：`1c20db9df1da7d7d88ac7d2be51e2c0611e62dbb`。
- 生产前置门禁：merged PR origin、精确 main CI、部署脚本、SSH 全部通过。
- Flyway 安全阀：无新增 migration。
- 服务器 Maven：472 tests / 0 failures / 0 errors / 88 skipped。
- 18080 预发布完整冒烟：通过。
- 8081 金丝雀完整冒烟：通过。
- Nginx 金丝雀切流：通过。
- 8080 正式完整冒烟：通过。
- Nginx 最终恢复到 `127.0.0.1:8080`。
- 8081 金丝雀已停止。
- 发布后核验：通过。
- 最终生产 JAR SHA256：`9e2ee6d4ac53ef54fb1e008f5add885b4bf352e4a48d992d51da9c61e1a8f09c`。
- 当前生产 release：`/opt/xinghuo-v2/releases/xinghuo-v2-standard-20261016051125.jar`。
- 上一版可回滚 release：`/opt/xinghuo-v2/releases/xinghuo-v2-standard-20261016045351.jar`。

## 风险边界

- 无数据库迁移。
- 无生产配置变更。
- 不修改订单、售后状态或售后处理写入逻辑。
- 不修改退款、收款、微信支付或企业微信写入。
- 不启用 V3H 小程序真实支付。
- 默认排序保持 V4K 原有“最新待跟进优先”。
- V3H 继续 `待真机验收`，PR #384 不混入本阶段。
- PR #372 企业微信写入继续独立验收。
- 旧后台、原售后处理页与上一版 production release 继续保留。

## 测试范围

- 服务层：默认排序继续保持 NEWEST_FIRST。
- 服务层：OLDEST_FIRST 按 `attention_at_ms` 升序返回。
- 服务层：非法 sort 返回参数错误。
- 控制器：sort 参数透传，同时保持现有 QueueSnapshot JSON 形状。
- Chromium：URL 恢复 `sort=OLDEST_FIRST`，请求参数、分页 URL 与前进/后退保持排序条件。
- WebKit：V4K 概览快捷入口继续保留原筛选条件，未显式设置 sort 时由 V4L 页面回落到 NEWEST_FIRST。
- Stable 版本契约：V4L 必须为 `STABLE PRODUCTION / 已上线`，V3H 必须继续待真机验收。
- 运行时契约：页面声明与 `/api/admin/xingwei/health` 返回 V4L Stable 真值时匹配；旧运行版本仍触发版本漂移。

## 结论

V4L 已完成“功能 PR → 功能 main CI → 统一 RC PR → 精确 RC main CI → 候选生产门禁 → Stable 状态 PR → 精确 Stable main CI → Stable Production 发布”的完整闭环。当前线上正式端口为 8080，V4L 生产真值与仓库版本真值一致，可以进入下一阶段开发。
