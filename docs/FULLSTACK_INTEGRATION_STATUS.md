# V3N 前后端统一交付与总联调

更新时间：2026-10-14

## 版本真值

`integration/frontend-v3m-total` / PR #393 已合并到主线，合并提交为 `e0daadc6f79d2efec55981bb8d013d9360aeea61`。V3N（`20261014-v3n`）经主线 CI #923 和 Deploy Production #156 发布到生产；后台阶段记为 `V3N · STABLE PRODUCTION`，更新记录状态为「已上线」。本次状态同步由后续 PR 再次经过 CI 与生产门禁发布。

## 整合输入与验证

- Frontend Next PR #391：A1–A4 公共壳层、首页、服务大厅与详情、询价和业务记录、登录与个人中心。
- Xingwei V3M PR #392：请求超时、周期复核、visibility / online / offline 复核、stale 降级和权限 fail-closed。
- 协作者“星火迷彩追踪器”1.0.13 源码包：替换原占位小程序客户端，保留迷彩、攻略、查单、云同步和网站账号功能；“我的”增加扫描网站登录二维码，保留手动 8 位码入口，客户端候选版本为 1.0.14。网站文案与实际小程序名称对齐。
- PR #393：公共前端与后台运行时无交叉加载；公开 API 与受保护后台 API 保持原有安全边界；账号链路、SEO、canonical、JSON-LD、robots、sitemap、缓存和版本漂移契约继续有效。
- 集中联调覆盖 Chromium / WebKit，1920 / 1440 桌面、常见手机与 320px 窄屏。PR #393 的 CI #920、V3N 收口提交 `1f702a2` 的 CI #921、合并前 CI #922 和主线 CI #923 均通过 Java 21 / Maven Build 与 Chromium + WebKit UI Regression。
- 源码包本地静态、页面模拟、同步、图片检查通过；小程序扫码接口回归通过。微信开发者工具使用实际 AppID 成功生成 1.0.14 预览包（103151 字节）。2026-10-14 用户反馈预览版扫码、微信确认及原网页完成登录成功。该反馈覆盖正常主链路；新账号/已关联账号分别验证、错误/过期/重复码、两台设备交叉登录和正式体验版发布仍待记录。生产登录接口返回 `enabled: true` 只说明网站入口已开启。

## 明确排除

- V3H PR #384 小程序微信支付保持未合并；不引入 V68 支付 migration，不启用真实收款。AppID / 商户绑定与小额真机联调单独验收。
- PR #372 企业微信协作者写入能力不强行合并；现有企业微信通讯录只读能力继续保留。
- 本次收口不修改数据库 schema 或生产配置。

## 交付记录与剩余验收

1. V3N 分支运行版本真值校验、Java 21 / Maven Build 和 Chromium / WebKit UI Regression 均通过；PR #393 经主线 PR 流程合并，主线 CI #923 成功。
2. 用户明确确认部署后，从 `main` 手动触发 Deploy Production #156；18080 候选、8081 金丝雀与切流、8080 正式冒烟均成功。部署产物为 `/opt/xinghuo-v2/releases/xinghuo-v2-standard-20261014085237.jar`；前一版本 `/opt/xinghuo-v2/releases/xinghuo-v2-standard-20261014052349.jar` 保留供回滚。公开首页、登录页、robots、sitemap 和小程序登录可用性接口均返回 200。
3. 实际小程序预览版扫码登录主链路已由用户反馈通过。正式版/体验版发布及未关联/已关联账号、错误/过期/重复码、手动码、跨设备场景继续补充验收记录。客户端上传与云函数发布独立验收，不能用 GitHub CI 或网站生产发布代替。
4. 后续生产变更继续经过主线 PR、主线 CI 和 Deploy Production / `deploy/production` 门禁，不直接推送 `main`，也不从 PR 分支发布；继续执行 18080 / 8081 / 8080 零停机检查。
