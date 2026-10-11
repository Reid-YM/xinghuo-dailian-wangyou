# 生产发布治理

## 目标

生产发布只允许针对当前 `main`，目标提交必须来自已合并 Pull Request，并且该提交的 `main` CI 必须完整成功。发布仍由 GitHub Actions `Deploy Production` 进入服务器零停机链路。

## 推荐入口

优先使用 `workflow_dispatch` 从 `main` 手动触发，并勾选 `confirm=true`。这是最直观的人工生产发布入口。

当自动化客户端没有 `workflow_dispatch` 写权限时，允许使用长期分支 `deploy/production` 作为受控发布请求队列：

1. `.deploy-request` 中必须且只能有一个 `deploy_sha=<40位SHA>` 目标。
2. `deploy_sha` 必须等于触发时 GitHub 当前 `main` SHA。
3. 每个请求提交必须是普通单父提交。
4. 请求提交相对其直接父提交只能修改 `.deploy-request`。
5. 工作流随后仍会验证目标 SHA 来自合并到 `main` 的 PR，并验证该 SHA 的 `main` CI 已成功。

因此 `deploy/production` 可以线性追加新的 `.deploy-request` 提交，不需要为了每次发布 force reset 到 `main`。只有部署工作流本身发生变更、需要刷新该长期分支上的 workflow 定义时，才可能进行一次经过明确授权的受控重置。

## 生产服务器链路

GitHub Actions 通过生产 SSH 网关调用 `deploy <DEPLOY_SHA>`，服务器依次执行：

- 同步并锁定 `main` 目标 SHA；
- Flyway 安全检查；
- Maven 完整构建与测试；
- 18080 候选实例完整冒烟；
- 8081 金丝雀实例完整冒烟；
- Nginx 临时切流至 8081；
- 原子切换 `current.jar` 并启动正式 8080；
- 8080 完整冒烟；
- Nginx 恢复指向 8080并停止金丝雀；
- 保留上一正式版本用于回滚。

## main 保护

仓库代码层已有两道防线：

- `CI` 的 `Guard main against direct pushes` 会拒绝非 PR merge 产生的 `main` CI；
- `Deploy Production` 只接受由合并 PR 产生、且 `main` CI 成功的目标 SHA。

GitHub 平台级 Branch Protection / Repository Ruleset 仍应配置为：

- 必须通过 Pull Request 合并；
- 必须通过 `Java 21 / Maven Build` 和 `Chromium UI Regression`；
- 禁止 force push；
- 禁止删除 `main`；
- 保持 `production-recovered-20261005` 历史标签不可移动。

平台级规则需要仓库 Administration 权限。若当前自动化连接没有 Administration 写权限，应保持代码层防线并由仓库管理员在 GitHub 设置中完成平台级规则。
