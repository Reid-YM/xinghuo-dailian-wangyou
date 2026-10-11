# 重构最终手动验收 Runbook

> 适用状态：自动 CI 暂停，`.github/workflows/ci.yml` 仅保留 `workflow_dispatch`。
>
> 原则：代码结构收口不等于生产验收完成。兼容 CSS 本体只有在浏览器与生产端口门禁全部通过后才允许继续退役。

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

因此项目已从“等待生产验收”进入 **#131 小批次兼容资产退役阶段**。后续每一个删除/迁移 PR 都必须重新建立自己的同 SHA 证据，不能把上述基线结果当成新提交的最终验收结果。

## 1. 锁定唯一验收 SHA

验收前先确认 GitHub 当前 `main` 或待验收清理分支的 40 位 SHA，并在整个验收过程中保持不变。

所有后续证据都必须指向同一个 SHA：

- 手动 CI；
- Chromium / WebKit 回归；
- 生产部署目标；
- 18080 / 8081 / 8080 smoke；
- 最终 release manifest。

如果 `main` 在验收过程中又合入新 PR，旧 SHA 的通过结果不能替代新 SHA，必须从手动 CI 重新开始。

## 2. 手动 CI 门禁

在 GitHub Actions 中，从目标分支手动运行 `CI` workflow；合并到 `main` 后用于生产发布时，必须再以最终 `main` SHA 为准。

必须同时成功：

1. `Java 21 / Maven Build`
   - shell 脚本语法检查；
   - `mvn -B -ntp clean package`；
   - Java 21 编译与完整 Maven 测试；
   - VNext 前端构建随 Maven 生命周期完成。

2. `Chromium + WebKit UI Regression`
   - Playwright Chromium；
   - Playwright WebKit；
   - `PLAYWRIGHT_UI_TESTS=1`；
   - 全部 `*PlaywrightTest`，包括 `LegacyBridgePlaywrightTest`。

验收记录至少保留：workflow run URL / run id、head SHA、两个 job 的成功状态。

## 3. 浏览器兼容重点

共享 `vnext-legacy-bridge-v3.css` 仍是生产依赖，不得仅凭静态搜索删除。

重点验证：

- service detail：1920 宽屏画布、手机无横向溢出；
- record / order detail：声明式 bridge 正常加载；
- account center：桌面宽屏、390 iPhone、输入框 16px；
- account submit：桌面宽屏、390 iPhone、编辑器移动端单列、输入框不触发 iOS 自动缩放；
- 后台：`admin-brand-layout-fixes.css` 单一路径仍能提供 legacy bridge；
- 微信 / Safari / WebKit 下安全区与底部固定区域无遮挡。

`vnext-account-submit-v3.css` 已进入首批退役：其视觉规则不删除，而是原样迁移为业务页面自有的 `/assets/account-submit.css`。这一迁移只有在新的 Maven + Chromium + WebKit + production smoke 全部通过后才算正式完成。

`vnext-legacy-bridge-v3.css` 仍继续保留，必须单独建立替代 owner 和无 bridge 浏览器证据后才能进入下一批删除。

## 4. 生产 SSH 只读安全验证

如需先验证 GitHub Secrets 与受限 SSH key，可手动运行 `Test Production SSH`。

该 workflow 只发送一个不允许的普通命令来确认 forced-command 限制，预期结果是拒绝普通 shell；它不会发送 `deploy <sha>`，也不会部署。

## 5. 生产发布入口

当前有两条受控入口，最终都会执行同一套生产门禁：

### A. `main` 手动入口

1. 当前 `main` SHA 已完成第 2 节手动 CI，两个 job 全绿；
2. 从 `main` 手动运行 `Deploy Production`；
3. 勾选 `confirm=true`；
4. Deploy workflow 再次验证：
   - 目标就是当前 `main`；
   - 目标提交来自已合并 PR；
   - 同一个 SHA 存在成功的 `CI` `workflow_dispatch` run；
   - 生产 SSH secrets 可用。

### B. `deploy/production` 受控发布请求入口

`deploy/production` 可通过只修改 `.deploy-request` 的单父提交触发发布。请求中的 `deploy_sha` 必须等于当前 `main`，并同样要求已合并 PR 来源和同 SHA 成功的手动 CI。

不要用其他 SHA 的旧 CI 结果替代当前 SHA。

## 6. `deploy/production` 入口注意事项

`deploy/production` 是长期受控发布请求分支，不是正式源码分支。

重要限制：GitHub 对该分支的 push 会使用该分支自己保存的 workflow 定义。只要 `main` 上的 `.github/workflows/deploy-production.yml` 发生过变更，就必须先确认 `deploy/production` 已按受控流程刷新到同一版本，才能继续使用发布请求入口。

在 workflow 定义未同步时：

- 不要直接提交 `.deploy-request`；
- 先受控同步 workflow 定义；
- 同步本身不应满足 `.deploy-request` 校验，因此不会进入实际服务器部署；
- 确认安全失败后，再单独提交只修改 `.deploy-request` 的正常发布请求。

## 7. 服务器三端口门禁

生产服务器发布链固定为：

### 18080 — 候选预发布

`deploy-from-github.sh`：

- 从当前 `main` SHA 完整 Maven 构建候选 JAR；
- 独立启动候选 JAR 到 18080；
- 执行 `scripts/smoke_test.sh 18080`；
- 失败时不切生产流量。

### 8081 — 金丝雀

`deploy-zero-downtime.sh`：

- 同一候选 JAR 启动到 8081；
- 执行完整 `run_smoke 8081`；
- 通过后 nginx 才允许临时切到 8081。

### 8080 — 新正式实例

- 原子切换 `current.jar`；
- 启动正式 8080；
- 执行完整 `run_smoke 8080`；
- 通过后 nginx 切回 8080；
- 停止 8081 金丝雀。

共享 smoke 当前验证：

- `/assets/vnext-legacy-bridge-v3.css`：仍在使用的共享兼容层；
- `/assets/account-submit.css`：攻略投稿页业务自有 canonical style。

旧 `/assets/vnext-account-submit-v3.css` 不应再成为 smoke 依赖。

## 8. 发布后必须确认

- `xinghuo-v2.service` active；
- nginx active 且 `nginx -t` 成功；
- nginx upstream 最终指向 8080；
- 8081 不再监听；
- 18080 不再监听；
- `current.jar` SHA256 与候选 JAR 一致；
- `current-release.env` 中 Git SHA 与 JAR SHA256 正确；
- 首页、服务、询价、记录、账号登录、后台登录与核心 API smoke 正常。

## 9. 回滚证据

发布完成前必须明确上一正式 JAR 路径。

若新 8080 健康或 smoke 失败，`deploy-zero-downtime.sh` 会尝试恢复旧 `current.jar`。如果 nginx 仍指向健康的 8081 金丝雀，脚本会优先保留金丝雀承载流量，避免因清理动作造成中断。

人工回滚时必须先让 8080 通过 smoke，再将 nginx upstream 恢复到 8080；不要先停掉正在承载生产流量的 8081。

## 10. compatibility CSS 的最终删除条件

只有以下证据全部属于同一个已验收版本时，才进入 bridge CSS 删除 PR：

- Maven 全量构建与测试通过；
- Chromium 全量回归通过；
- WebKit 全量回归通过；
- 18080 smoke 通过；
- 8081 smoke 通过；
- 8080 smoke 通过；
- 回滚路径已确认；
- 替代样式 owner 与受影响路由有明确清单。

删除必须继续小批次进行，不能把 bridge CSS 删除与 API、鉴权、数据库迁移或生产路由切换混在同一个 PR。

第一批策略是“先改 owner、后删共享层”：页面专属规则迁到业务命名资产并保持规则等价，再由 Playwright 和三端口 smoke 证明路径变更没有造成视觉/运行回归。共享 `vnext-legacy-bridge-v3.css` 最后处理。
