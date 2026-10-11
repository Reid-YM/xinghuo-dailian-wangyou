# 生产发布治理

## 目标

生产发布只允许针对当前 `main`，目标提交必须来自已合并 Pull Request，并且**同一个 `main` SHA** 的完整 `CI` 必须成功。发布仍由 GitHub Actions `Deploy Production` 进入服务器零停机链路。

仓库公开后恢复自动 CI：Pull Request 指向 `main` 时自动运行完整 CI，PR 合并后对新的 `main` SHA 再自动运行一次 `push` CI。`workflow_dispatch` 继续保留为人工补跑/故障恢复入口，但不再是生产门禁唯一认可的 CI 事件。

生产门禁认可同一个目标 SHA 上成功的 `push` CI，或人工补跑成功的 `workflow_dispatch` CI；不会拿其他 SHA、其他分支或 PR merge ref 的结果替代当前 `main` SHA。

## CI 自动验证

`.github/workflows/ci.yml` 当前触发策略：

- `pull_request` → `main`：自动运行 Java/Maven 和 Chromium + WebKit 回归，作为合并前验证；
- `push` → `main`：PR 合并后自动再次运行完整 CI，生成生产发布所需的同 SHA 证据；
- `workflow_dispatch`：保留人工补跑入口。

完整 CI 必须同时通过：

- `Java 21 / Maven Build`
- `Chromium + WebKit UI Regression`

`main` 的 push Job 仍有代码层防线：提交必须能追溯到已合并到 `main` 的 Pull Request，否则 CI 会主动失败，防止把直接 push 当作正常发布来源。

## 推荐生产入口

等待当前 `main` SHA 的自动 `push` CI 两个 Job 全绿后，再从 `main` 手动触发 `Deploy Production`，并勾选 `confirm=true`。CI 自动化不等于自动上线；生产发布继续保留明确人工确认。

当自动化客户端没有 `workflow_dispatch` 写权限时，也允许使用长期分支 `deploy/production` 作为受控发布请求队列：

1. `.deploy-request` 中必须且只能有一个 `deploy_sha=<40位SHA>` 目标。
2. `deploy_sha` 必须等于触发时 GitHub 当前 `main` SHA。
3. 每个请求提交必须是普通单父提交。
4. 请求提交相对其直接父提交只能修改 `.deploy-request`。
5. 工作流随后仍会验证目标 SHA 来自合并到 `main` 的 PR，并验证**同一个目标 SHA** 的成功 `main` CI。

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
- Nginx 恢复指向 8080 并停止金丝雀；
- 保留上一正式版本用于回滚。

## main 保护

仓库代码层已有两道防线：

- 所有生产代码改动继续要求通过 Pull Request 合并到 `main`；
- `Deploy Production` 只接受由合并 PR 产生、且**同一 SHA 的成功 `main` CI**已经完成的目标。

自动 CI 恢复后，推荐将 GitHub 平台级 Branch Protection / Repository Ruleset 配置为：

- 必须通过 Pull Request 合并；
- 将 `Java 21 / Maven Build` 与 `Chromium + WebKit UI Regression` 设为 required checks；
- 禁止 force push；
- 禁止删除 `main`；
- 保持 `production-recovered-20261005` 历史标签不可移动。

平台级规则需要仓库 Administration 权限。若当前自动化连接没有 Administration 写权限，应保持代码层防线并由仓库管理员在 GitHub 设置中完成平台级规则。
