# 重构最终验收与发布 Runbook

> 当前状态：CI 已恢复自动触发。`pull_request -> main` 与 `push -> main` 都会运行完整 CI，`workflow_dispatch` 仅作为人工补跑入口。
>
> 原则：结构收口、自动 CI 通过与生产发布是三层不同证据；任何生产发布仍必须针对最终 `main` SHA 重新完成同 SHA 门禁。

## 0. 已完成的生产基线

2026-10-11，`main` SHA `73dd113bc52b4ddf1c4d80b602f510ee036cde65` 已完成：

- `Java 21 / Maven Build`；
- `Chromium + WebKit UI Regression`；
- 18080 候选完整 smoke；
- 8081 金丝雀完整 smoke；
- 8080 正式实例完整 smoke；
- nginx 最终回到 8080；
- 8081 金丝雀停止；
- release SHA256 与 release manifest 校验；
- 上一正式 JAR 与回滚路径确认。

这仍是当前已确认的生产基线。其后的 #251–#256 属于兼容资产退役清理，已经分别通过 PR CI 与 `main` 自动 CI，但尚不能用旧生产基线替代新的生产发布验证。

## 1. 自动 CI 门禁

CI workflow 当前触发策略：

- Pull Request 指向 `main`：自动运行；
- 合并后 push 到 `main`：自动再次运行；
- `workflow_dispatch`：保留为人工补跑入口。

每一次清理 PR 合并前必须同时成功：

1. `Java 21 / Maven Build`
   - shell 脚本语法检查；
   - `mvn -B -ntp clean package`；
   - Java 21 编译与完整 Maven 测试；
   - VNext 前端构建随 Maven 生命周期完成。

2. `Chromium + WebKit UI Regression`
   - Playwright Chromium；
   - Playwright WebKit；
   - `PLAYWRIGHT_UI_TESTS=1`；
   - 全部 `*PlaywrightTest`，包括历史命名的 `LegacyBridgePlaywrightTest` 与最终独立后台验证。

合并后必须再确认最终 `main` SHA 的自动 push CI 同样全绿。

## 2. compatibility CSS 退役状态

退役采用“先改 owner、再删共享层”的小批次策略，不能与 API、鉴权、数据库或生产路由变更混在同一个 PR。

已完成迁移：

- `vnext-account-submit-v3.css`：已进入首批退役并完成，规则迁移到 `/assets/account-submit.css`；
- account center：规则归属 `/assets/account-center.css` + `/assets/account-center-refine.css`；
- service detail：归属 `/assets/service-detail.css`；
- record / order detail：归属 `/assets/record-detail.css`；
- frontend control center：归属 `/assets/admin-frontend-controls.css`；
- 主运营后台：由 `/assets/admin-control-center-v2.css` + `/assets/admin-light-theme.css` 负责；
- 独立攻略后台：由 `/assets/admin-guides-workspace.css` + `/assets/admin-light-theme.css` 负责；
- 品牌与窄屏修复：由 `/assets/admin-brand-layout-fixes.css` 负责。

`vnext-legacy-bridge-v3.css` 已进入最终退役批次：删除后生产 smoke 必须验证该旧资产返回 404，同时所有 canonical replacement assets 返回 200。

## 3. 浏览器重点

最终清理必须继续验证：

- service detail：1920 宽屏画布、WebKit 手机无横向溢出；
- record / order detail：feature-owned `record-detail.css` 独立工作；
- account center：桌面宽屏、390 iPhone、输入框 16px；
- account submit：桌面宽屏、390 iPhone、编辑器移动端单列，不依赖 `vnext-legacy-bridge-v3.css`；
- frontend control center：业务自有 CSS 直接提供当前生产深色宽屏/移动端效果；
- 主运营后台：Control Center V2 浅色工作台保持稳定；
- 独立攻略后台：`app.css -> admin-guides-workspace.css -> admin-light-theme.css -> admin-brand-layout-fixes.css` 的真实顺序下保持浅色、宽屏和手机无溢出；
- 微信 / Safari / WebKit 下安全区与底部固定区域无遮挡。

## 4. 生产发布资格

生产发布目标必须满足：

- 等于当前 `main` SHA；
- 来自已合并到 `main` 的 Pull Request；
- 同一个 SHA 的 `CI` 已成功；
- CI 可以是自动 `push` run，也可以是备用 `workflow_dispatch` run；
- 生产 SSH secrets 可用。

不要使用其他 SHA 的旧 CI 结果替代当前 SHA。

## 5. 生产发布入口

### A. `main` 人工确认入口

从 `main` 运行 `Deploy Production` 并勾选 `confirm=true`。发布 workflow 会再次验证 PR 来源、同 SHA CI 和 SSH 配置。

### B. `deploy/production` 受控请求入口

当自动化客户端无法直接发起 `workflow_dispatch` 时，可在线性 `deploy/production` 分支提交只修改 `.deploy-request` 的单父提交：

- `.deploy-request` 只能包含目标 `deploy_sha`；
- `deploy_sha` 必须等于当前 `main`；
- 目标仍必须来自已合并 PR；
- 同 SHA CI 必须成功。

`deploy/production` 是受控发布请求分支，不是源码开发分支。

如果 `main` 上的发布 workflow 发生过修改，而 `deploy/production` 的 workflow 定义未同步：

- 不要直接提交正常发布请求；
- 先按受控流程同步 workflow；
- 同步提交本身应因 `.deploy-request` 门禁而安全失败；
- 确认没有进入实际部署后，再提交正常 `.deploy-request`。

## 6. 服务器三端口门禁

### 18080 — 候选预发布

`deploy-from-github.sh` 从目标 SHA 完整 Maven 构建候选 JAR，启动 18080 并执行 `scripts/smoke_test.sh 18080`。失败时不得切生产流量。

### 8081 — 金丝雀

同一候选 JAR 启动到 8081，执行完整 smoke。只有通过后 nginx 才允许临时切到 8081。

### 8080 — 正式实例

原子切换 `current.jar`，启动正式 8080，执行完整 smoke；通过后 nginx 回到 8080，并停止 8081。

## 7. 最终 bridge 退役 smoke

最终 shared bridge 删除版本必须在 18080、8081、8080 三道门同时验证：

- `/assets/vnext-legacy-bridge-v3.css` → **404**；
- `/assets/service-detail.css` → 200；
- `/assets/record-detail.css` → 200；
- `/assets/account-submit.css` → 200；
- `/assets/account-center.css` → 200；
- `/assets/account-center-refine.css` → 200；
- `/assets/admin-frontend-controls.css` → 200；
- `/assets/admin-control-center-v2.css` → 200；
- `/assets/admin-light-theme.css` → 200；
- `/assets/admin-guides-workspace.css` → 200；
- `/assets/admin-brand-layout-fixes.css` → 200。

旧 `/assets/vnext-account-submit-v3.css` 不再是运行或 smoke 依赖。

## 8. 发布后确认

- `xinghuo-v2.service` active；
- nginx active 且 `nginx -t` 成功；
- nginx upstream 最终指向 8080；
- 8081 不再监听；
- 18080 不再监听；
- `current.jar` SHA256 与候选 JAR 一致；
- `current-release.env` 中 Git SHA 与 JAR SHA256 正确；
- 首页、服务、询价、记录、账号登录、后台登录与核心 API smoke 正常。

## 9. 回滚证据

发布前必须明确上一正式 JAR 路径。若新 8080 健康或 smoke 失败，零停机脚本应恢复旧 `current.jar`；如果 nginx 正在由健康的 8081 金丝雀承载，则先保持金丝雀，不要为了清理动作中断生产流量。

人工回滚必须先让 8080 通过 smoke，再把 nginx upstream 恢复到 8080，最后停止 8081。

## 10. 最终完成标准

compatibility bridge 的退役只有在以下证据都属于同一个最终版本时才算闭环：

- Maven 全量构建与测试通过；
- Chromium 全量回归通过；
- WebKit 全量回归通过；
- 最终 `main` push CI 通过；
- 18080 smoke 通过；
- 8081 smoke 通过；
- 8080 smoke 通过；
- 旧 shared bridge 404；
- replacement canonical assets 全部 200；
- 回滚路径已确认。
