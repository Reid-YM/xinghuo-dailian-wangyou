# 星火项目协作流程

本仓库以 GitHub `main` 分支作为正式发布入口。代码合入 `main` 后，会自动触发 GitHub Actions，将代码同步到腾讯服务器、构建 JAR、替换线上包并重启服务。

## 角色建议

- Owner：仓库所有者，负责邀请成员、配置权限、处理紧急回滚。
- Core Maintainer：核心开发者，建议给 `Maintain` 权限，可以管理 PR、分支和日常协作，但不轻易改仓库安全配置。
- Contributor：普通协作者，建议给 `Write` 权限，通过分支和 PR 提交代码。

## 推荐权限

核心开发者：

- Repository role：`Maintain`
- 不建议默认给 `Admin`
- 不共享服务器密码、GitHub token、`.env`、数据库密码

普通开发者：

- Repository role：`Write`
- 只能通过分支和 Pull Request 合入

## 日常开发流程

1. 更新本地 `main`：

```bash
git checkout main
git pull origin main
```

2. 创建功能分支：

```bash
git checkout -b feature/brief-name
```

分支命名建议：

- `feature/xxx`：新功能
- `fix/xxx`：问题修复
- `hotfix/xxx`：线上紧急修复
- `docs/xxx`：文档

3. 修改代码后提交：

```bash
git status
git add .
git commit -m "简短说明这次修改"
```

4. 推送分支：

```bash
git push origin feature/brief-name
```

5. 在 GitHub 打开 Pull Request：

- base：`main`
- compare：你的功能分支
- 填写 PR 模板
- 等待 `Validate Xinghuo V2` 检查通过
- 至少让另一位核心者 Review

6. 合并 PR：

合并到 `main` 后会自动部署线上服务器。

## 自动化规则

当前有两条 GitHub Actions：

- `Validate Xinghuo V2`：PR 校验，只构建，不部署。
- `Deploy Xinghuo V2`：`main` 更新后部署到腾讯服务器。

因此：

- PR 阶段：只验证代码能否构建。
- 合并到 `main`：才会自动上线。

## 不要提交的内容

不要把下面这些文件或内容提交进 GitHub：

- `.env`
- 数据库密码
- SMTP/API Key
- GitHub token
- SSH 私钥
- 生产后台密码
- `build/` 里的 JAR 包
- 临时截图、日志、导出数据

## 服务器修改后的处理

如果临时在腾讯服务器 `/srv/xinghuo-dev/repo` 修改了代码，不会自动上传 GitHub。需要手动提交并推送：

```bash
git status
git add .
git commit -m "说明服务器上的修改"
git push github main
```

推荐仍然优先在本地或 GitHub 分支开发，通过 PR 合并。

## 紧急修复

线上故障时可以走 `hotfix/xxx` 分支：

```bash
git checkout main
git pull origin main
git checkout -b hotfix/brief-name
```

修复后开 PR。只有在极端情况下才直接推 `main`，并且修复后补一个说明 PR 或记录。

## 建议开启的 GitHub 分支保护

进入：

`Settings` -> `Branches` -> `Add branch protection rule`

配置：

- Branch name pattern：`main`
- Require a pull request before merging：开启
- Required approvals：`1`
- Dismiss stale pull request approvals when new commits are pushed：开启
- Require status checks to pass before merging：开启
- 选择状态检查：`validate`
- Require branches to be up to date before merging：开启
- Do not allow bypassing the above settings：建议开启
- Allow force pushes：关闭
- Allow deletions：关闭

这能保证 `main` 不被随意直推，只有 PR 校验通过并被审核后才会触发正式部署。
