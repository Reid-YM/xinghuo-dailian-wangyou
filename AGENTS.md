# AGENTS.md

# 星火网站 — 人 + AI 协作开发规范

本文件是星火网站仓库级开发规则。

适用于所有参与本项目开发的人和 AI，包括但不限于：

- 两位项目开发者
- OpenAI Codex
- DeepSeek
- GitHub Copilot
- 其他后续接入的 AI 开发工具

任何人或 AI 在分析、修改、提交代码前，应优先阅读并遵守本文件。

---

## 1. 正式项目基线

正式 GitHub 仓库：

`Reid-YM/xinghuo-dailian-wangyou`

正式主分支：

`main`

2026-10-05 生产源码恢复基线：

`d127eaae39b0f9b93c940e3f4a330d6b0e654c1a`

恢复标签：

`production-recovered-20261005`

注意：

上述 SHA 是“生产源码恢复起点”，不是以后永远不变的 main 版本。

随着正常开发继续进行，main 会产生新的 Commit。

`production-recovered-20261005` 作为历史恢复锚点保留，不应删除或重写。

---

## 2. 当前正式开发架构

GitHub 是正式代码协作和版本管理中心。

生产服务器正式源码目录：

`/opt/xinghuo-v2/source-current`

生产服务：

`xinghuo-v2.service`

生产服务器主要用于：

- 拉取正式代码
- 构建
- 测试
- 部署
- 查看日志
- 故障排查

生产服务器不作为两位开发者的日常共同编码工作区。

两位开发者应分别使用各自电脑上的独立 Git Clone。

---

## 3. 核心协作原则

两位开发者权限平等。

每位开发者都可以独立：

- 提出需求
- 使用 AI 分析代码
- 创建开发分支
- 修改代码
- 测试
- Commit
- Push
- 创建 Pull Request
- 检查自己的 PR
- 合并进入 main

当前不强制另一位开发者审批。

但是：

**main 是正式主线，不是日常开发工作区。**

任何人或 AI 都不应在正常开发过程中直接修改 main。

---

## 4. 每个任务必须独立分支

开始开发前：

```bash
git fetch origin
git switch main
git pull --ff-only
