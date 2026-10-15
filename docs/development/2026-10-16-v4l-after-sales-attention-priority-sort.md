# V4L 售后待跟进优先级排序

日期：2026-10-16

状态：功能开发中，生产版本继续保持 V4K · STABLE PRODUCTION。

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
- 当前统一后台版本真值仍为 `20261016-v4k / V4K · STABLE PRODUCTION`；V4L 功能 PR 不提前修改 Release History。

## 风险边界

- 无数据库迁移。
- 无生产配置变更。
- 不修改订单、售后状态或售后处理写入逻辑。
- 不修改退款、收款、微信支付或企业微信写入。
- 不启用 V3H 小程序真实支付。
- 默认排序保持 V4K 原有“最新待跟进优先”。
- 旧后台、原售后处理页与 V4K 生产 release 继续保留。

## 测试范围

- 服务层：默认排序继续保持 NEWEST_FIRST。
- 服务层：OLDEST_FIRST 按 `attention_at_ms` 升序返回。
- 服务层：非法 sort 返回参数错误。
- 控制器：sort 参数透传，同时保持现有 QueueSnapshot JSON 形状。
- Chromium：URL 恢复 `sort=OLDEST_FIRST`，请求参数、分页 URL 与前进/后退保持排序条件。
- WebKit：V4K 概览快捷入口继续保留原筛选条件，未显式设置 sort 时由 V4L 页面回落到 NEWEST_FIRST。
- 版本契约：V4K 继续保持 Stable Production，仅 V4L 页面资产缓存前进。

## 下一步

1. 完成功能 PR 的 Java/Maven、版本真值守卫与 Chromium/WebKit 双 CI。
2. 功能 PR 合入 main 后，对精确 main SHA 再跑主线双 CI。
3. 单独创建 V4L 统一 Release Candidate：版本真值、更新记录、后台首页和售后页面同步到 `20261016-v4l / V4L · RELEASE CANDIDATE`。
4. 候选双 CI 全绿后走 18080 → 8081 → 8080 受控生产门禁。
5. 生产门禁成功后再用状态-only PR 收口为 `V4L · STABLE PRODUCTION / 已上线`。
