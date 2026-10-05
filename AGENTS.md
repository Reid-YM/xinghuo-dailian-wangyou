# 星火网站 — 人 + AI 协作开发规范

本文件是星火网站仓库级开发规则，适用于两位开发者及其使用的 AI 工具，包括 OpenAI Codex、DeepSeek、GitHub Copilot 及后续接入的其他 AI。

任何人或 AI 在分析、修改、提交代码前，应优先阅读并遵守本文件。

## 1. 正式项目基线

- 正式 GitHub 仓库：`Reid-YM/xinghuo-dailian-wangyou`
- 正式主分支：`main`
- 2026-10-05 生产源码恢复基线：`d127eaae39b0f9b93c940e3f4a330d6b0e654c1a`
- 恢复标签：`production-recovered-20261005`
- 服务器正式源码目录：`/opt/xinghuo-v2/source-current`
- 生产服务：`xinghuo-v2.service`

恢复基线 SHA 只是历史恢复起点，不代表以后 `main` 永远停留在该提交。`main` 会随着正常开发继续前进；恢复标签用于历史锚定，不应删除或改写。

## 2. 开发架构

GitHub 是正式代码协作和版本管理中心。

生产服务器主要用于：

- 拉取正式代码
- 构建
- 测试
- 部署
- 查看日志
- 故障排查

生产服务器不作为两位开发者的日常共同编码工作区。两位开发者应分别使用自己电脑上的独立 Git Clone。

## 3. 核心协作原则

两位开发者权限平等，都可以独立：

- 提出需求
- 使用 AI 分析代码
- 创建开发分支
- 修改代码
- 测试
- Commit
- Push
- 创建 Pull Request
- 检查自己的 PR
- 合并进入 `main`

当前不强制另一位开发者审批。

**`main` 是正式主线，不是日常开发工作区。**

正常开发时，人和 AI 都不应直接在 `main` 上修改并推送业务代码。

## 4. 每个任务必须独立分支

开始任务前先同步最新主线：

```bash
git fetch origin
git switch main
git pull --ff-only
```

然后创建独立任务分支：

- 新功能：`feat/<功能名>`
- Bug 修复：`fix/<问题名>`
- 重构：`refactor/<模块名>`
- 文档：`docs/<内容>`

示例：

```text
feat/wechat-login
feat/order-center
fix/mobile-layout
fix/admin-upload
refactor/site-config
docs/deployment-guide
```

两个人同时开发时，必须使用各自独立分支，不要多人和多个 AI 无序共用同一个开发分支。

## 5. AI 开始修改前必须检查 Git 状态

Codex、DeepSeek 或其他 AI 在修改代码前，应先检查：

```bash
git status --short --branch
git branch --show-current
```

如果当前分支是 `main`，正常开发情况下不要直接修改，应先创建 `feat/*`、`fix/*`、`refactor/*` 或 `docs/*` 分支。

## 6. main 禁止操作

正常开发情况下禁止：

```bash
git push origin main
git push --force
git push --force-with-lease
```

同时禁止：

- 强制覆盖或重写 `main` 历史
- 删除 `main`
- 删除或改写 `production-recovered-20261005` 标签
- 把实验性代码直接提交到 `main`
- AI 自动把大量未经检查的修改直接送入 `main`

只有在明确的仓库恢复、事故处理等特殊场景，并得到项目负责人明确授权后，才允许例外。

## 7. 标准开发流程

```text
最新 main
  ↓
创建 feat/* / fix/* / refactor/* / docs/*
  ↓
人 + AI 开发
  ↓
检查 git diff
  ↓
构建 / 测试
  ↓
Commit
  ↓
Push 当前开发分支
  ↓
创建 Pull Request
  ↓
开发者本人检查
  ↓
CI 通过
  ↓
Merge main
  ↓
生产服务器更新 main
  ↓
构建 / 18080 冒烟 / 零停机部署 / 验证
```

## 8. 合并前最低检查要求

即使不要求另一位开发者审批，PR 合并前本人也必须确认：

1. 修改符合当前需求。
2. 没有误删已有功能。
3. 已检查 `git diff`。
4. 没有无关的大规模修改。
5. Maven 构建成功，或明确说明为什么没有执行。
6. 必要测试已执行。
7. GitHub CI 已完成且没有失败检查，或明确说明为什么没有 CI 结果。
8. 没有提交密码、Token、Cookie、私钥等敏感信息。
9. 数据库变更已确认迁移方案。
10. 生产配置没有被意外修改。
11. 已明确回滚方式。

## 9. AI 修改原则

所有 AI 应遵循：

- 优先最小改动。
- 优先修复根因。
- 不做与当前任务无关的大规模重构。
- 不擅自删除现有业务能力。
- 不擅自改变核心业务逻辑。
- 不因“代码更漂亮”而扩大修改范围。
- 修改前先理解相关调用链。
- 对不确定的信息先检查源码，不猜测。
- 修改后明确说明修改了哪些文件、为什么修改。
- 有多种方案时，优先选择低风险、易验证、易回滚方案。

除非需求明确要求，否则应尽量保持现有行为兼容。

## 10. 两个人同时修改同一区域

如果两个开发分支同时修改同一个文件或同一业务模块：

1. 获取最新 `main`。
2. 比较双方改动。
3. 理解冲突双方的业务目的。
4. 手工合并正确逻辑。
5. 重新构建和测试。
6. 再合并。

AI 不得遇到 Git 冲突后直接用 `ours` 或 `theirs` 粗暴覆盖整个文件，除非用户明确确认。

## 11. Java / Maven 项目要求

当前项目主要技术基线：

- Java 21
- Spring Boot 3.5.5
- Maven
- MySQL 8
- Flyway

常规代码改动完成后优先执行：

```bash
mvn clean package
```

如果因为数据库、外部服务或测试环境原因不能完成完整验证，应明确说明：

- 哪一步没有验证
- 为什么没有验证
- 可能风险是什么

不得把“代码已修改”等同于“功能已验证”。

## 12. Flyway / 数据库规则

2026-10-05 恢复基线包含 Flyway `V1 ~ V55`。

已经进入生产的历史 migration 原则上不修改。新增数据库变更应新增更高版本，例如：

```text
V56__xxxx.sql
V57__xxxx.sql
```

禁止为了新需求回头修改已进入生产的旧 migration。

涉及以下操作必须特别谨慎并在执行前明确说明风险：

- 删除表或列
- 大规模 `UPDATE` / `DELETE`
- 数据类型变更
- 唯一索引调整
- 用户认证数据
- 订单数据
- 支付相关数据

零停机部署期间，18080 / 8081 候选实例可能连接生产数据库并触发 Flyway。若存在新 migration，必须先确认迁移与旧版 8080 可并行兼容；破坏性、不可逆或兼容性不明确的迁移不得直接套用普通零停机发布流程。

## 13. 敏感信息禁止进入 Git

禁止提交：

- 数据库真实密码
- GitHub Token
- API Key
- Cookie / Session
- Secret
- SSH 私钥
- 第三方服务密钥
- 管理员真实密码
- 生产环境认证信息

示例配置使用 `CHANGE_ME`、`YOUR_API_KEY`、`YOUR_WECHAT_ID` 等占位值。真实 `.env` 和生产凭据不得提交。

## 14. 生产环境规则

禁止 AI 直接修改：

```text
/opt/xinghuo-v2/current.jar
```

也不要直接篡改 `/opt/xinghuo-v2/releases/` 中已经部署过的历史正式 JAR。

正常功能修改必须回到源码完成；禁止通过解压、修改、重新打包运行中的生产 JAR 作为日常开发方式。

只有在用户明确说出“确认部署”或同等明确的生产发布授权后，AI 才能进入会改变生产运行状态的部署阶段。开发完成、Push、PR 创建或 PR 合并本身都不等于部署授权。

## 15. 正式部署原则

生产部署应来自已经确认进入 `main` 的代码。

当前正式生产链路是：

```text
GitHub main
  ↓
/opt/xinghuo-v2/source-current
  ↓
mvn clean package
  ↓
候选 JAR 在 18080 预发布冒烟
  ↓
校验候选 JAR SHA256
  ↓
scripts/deploy-zero-downtime.sh
  ↓
8081 金丝雀完整冒烟
  ↓
nginx 临时切流到 8081
  ↓
current.jar 原子切换
  ↓
正式 8080 完整冒烟
  ↓
nginx 切回 8080
```

端口职责：

- `18080`：正式发布前的候选包预发布冒烟，不接 nginx 公网流量。
- `8081`：零停机切流过程中的临时金丝雀。
- `8080`：正式生产服务端口。

正式 release 命名统一为：

```text
/opt/xinghuo-v2/releases/xinghuo-v2-standard-YYYYMMDDHHMMSS.jar
```

`/opt/xinghuo-v2/current.jar` 必须通过 `current.jar.next + mv` 原子切换到新的 release。

以下旧方式已经退出正式生产流程，不得重新作为默认发布方案：

```text
/srv/xinghuo-dev/base/xinghuo-v2-base.jar
/srv/xinghuo-dev/work/xinghuo-v2
基线 JAR + 覆盖源码
rsync 覆盖源码后 dev_build.sh 拼包
/opt/xinghuo-v2/bin/deploy-from-source.sh 直接 stop/start 替换固定 JAR
```

当前 `scripts/deploy-zero-downtime.sh` 的职责仅是发布已经 Maven 构建并完成 18080 冒烟的候选 JAR，不应重新构建源码或依赖旧 baseline JAR。

每次部署应能明确对应：

- Git Commit SHA
- Git 分支（生产部署必须是 `main`）
- 候选 JAR SHA256
- 构建时间
- JAR 文件名
- 部署时间
- 上一个可回滚版本

部署后至少检查：

- `xinghuo-v2.service`
- `current.jar` 实际指向
- 8080 / 8081 / 18080 端口状态
- 网站 HTTP 状态
- Nginx 配置与最终 upstream
- 应用日志
- 核心页面
- 本次变更涉及的功能

出现严重异常时优先回滚已知正常版本，不要直接在生产环境大范围临时修改。

JAR 回滚不等于数据库回滚。若新版本已经执行不可逆 Flyway migration，必须单独判断数据库恢复方案，不得假设切回旧 JAR 即可恢复。

详细生产步骤以仓库根目录 `源码包部署说明.md` 为当前部署手册。

## 16. AI 高风险操作限制

以下操作不得由 AI 未经确认直接执行：

- Force Push
- 删除 Git 分支或 Tag
- 重写 `main` 历史
- 删除数据库数据、表或列
- 批量修改生产数据
- 修改生产密钥或数据库密码
- 删除生产 JAR
- 大范围覆盖生产配置
- 停止关键生产服务
- 其他不可逆生产操作

遇到高风险操作，AI 应先说明：要做什么、为什么需要、风险是什么、是否可回滚，再等待明确确认。

## 17. AI 完成任务后的交付要求

AI 完成代码任务后，应明确说明：

- 修改了哪些文件
- 每个文件做了什么
- 为什么这样修改
- 是否涉及数据库
- 是否涉及配置
- 是否涉及安全 / 认证
- 执行了哪些测试
- Maven 构建结果
- CI 结果
- 是否存在未验证部分
- 当前 Git 分支
- 推荐下一步

不得只回复“已经完成”或“已经修复”而没有验证信息。

## 18. 冲突优先级

若旧 README、旧开发文档、历史恢复资料与本文件冲突：

- 开发流程和 AI 协作规则以当前 `AGENTS.md` 为准。
- 当前生产部署流程以 `AGENTS.md` 与 `源码包部署说明.md` 为准。
- 当前代码事实以 GitHub `main` 为准。
- 当前生产事实以实时服务器状态为准。

不要因为历史文档重新使用已经淘汰的 `/srv/xinghuo-dev`、旧 baseline JAR 覆盖开发模式或旧的直接 stop/start 固定 JAR 发布方式。
