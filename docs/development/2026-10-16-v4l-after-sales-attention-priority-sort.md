# V4L 售后待跟进优先级排序

日期：2026-10-16

状态：统一 Release Candidate，尚未发布生产。

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
- 精确功能主线 CI #1149 已再次通过：
  - Java 21 / Maven Build：通过；
  - Xingwei release-state：通过；
  - main build artifact：上传成功；
  - Chromium + WebKit UI Regression：通过。

## 统一 Release Candidate

- RC 分支：`feat/v4l-unified-release`，精确基于功能主线 `ffc9c17da74d21dcc1724deb00a04b0e0b0b84e7`。
- `CONSOLE_VERSION`：`20261016-v4l`。
- `CONSOLE_STAGE`：`V4L · RELEASE CANDIDATE`。
- `BUSINESS_VERSION`：`20261016-v4l`。
- Release History 新增 V4L，状态保持 `待发布`。
- V4K 继续保持 `已上线`，不回退既有生产真值。
- 后台首页、业务工作台、更新记录、售后处理页、独立时效队列与版本/运行时/RBAC/总联调契约同步到 V4L 候选真值。
- V4L 候选尚未经过生产门禁，因此不能标记 `已上线`。

## 风险边界

- 无数据库迁移。
- 无生产配置变更。
- 不修改订单、售后状态或售后处理写入逻辑。
- 不修改退款、收款、微信支付或企业微信写入。
- 不启用 V3H 小程序真实支付。
- 默认排序保持 V4K 原有“最新待跟进优先”。
- V3H 继续 `待真机验收`，PR #384 不混入本阶段。
- PR #372 企业微信写入继续独立验收。
- 旧后台、原售后处理页与 V4K 生产 release 继续保留。

## 测试范围

- 服务层：默认排序继续保持 NEWEST_FIRST。
- 服务层：OLDEST_FIRST 按 `attention_at_ms` 升序返回。
- 服务层：非法 sort 返回参数错误。
- 控制器：sort 参数透传，同时保持现有 QueueSnapshot JSON 形状。
- Chromium：URL 恢复 `sort=OLDEST_FIRST`，请求参数、分页 URL 与前进/后退保持排序条件。
- WebKit：V4K 概览快捷入口继续保留原筛选条件，未显式设置 sort 时由 V4L 页面回落到 NEWEST_FIRST。
- 版本契约：V4L 当前必须为 Release Candidate / 待发布，V4K 必须继续为已上线。
- 运行时契约：页面声明与 `/api/admin/xingwei/health` 返回 V4L 候选真值时匹配；旧运行版本仍触发版本漂移。

## 下一步

1. 为统一候选创建独立 PR，并通过 Java/Maven、版本真值守卫、Chromium + WebKit 双 CI。
2. 合入后对精确 V4L RC `main` SHA 再跑主线双 CI。
3. 只有精确 RC main 双 CI 全绿后，才通过现有 `deploy/production` 受控链路执行 18080 → 8081 → 8080 生产门禁。
4. 生产门禁成功后，另开状态-only PR 将 V4L 同步为 `V4L · STABLE PRODUCTION / 已上线`。
5. Stable PR 合入并通过精确 main 双 CI 后，再执行一次状态-only 生产发布，使仓库真值与生产运行真值完全一致。
