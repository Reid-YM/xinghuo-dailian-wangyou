# AI 开发快速启动

本文件用于减少重复提示词。仓库级规则仍以根目录 `AGENTS.md` 为准。

## 开发者以后只需要说什么

正常开发时，开发者只需要直接描述需求，例如：

```text
开发微信扫码登录功能。
```

或：

```text
修复移动端订单页面按钮挤压问题。
```

不需要每次重复粘贴 Git、CI、Flyway、生产部署等固定规则。

## AI 收到开发需求后必须自动完成

1. 阅读根目录 `AGENTS.md`。
2. 如果任务涉及 UI、布局、视觉、交互、响应式、前台可配置能力、Dashboard、表格、表单、列表或详情页，同时阅读 `docs/XINGHUO-UI-TASTE.md`，先确定页面主任务和 Visual Thesis，再开始修改。
3. 检查 `git status --short --branch`、当前分支和远程仓库。
4. 确认仓库为 `Reid-YM/xinghuo-dailian-wangyou`。
5. 若存在不明未提交修改，停止并报告，不覆盖现有工作。
6. 若工作区干净，执行 `git fetch origin`，切换 `main`，并以 `git pull --ff-only origin main` 同步最新主线。
7. 根据任务创建独立的 `feat/*`、`fix/*`、`refactor/*`、`docs/*` 或 `ci/*` 分支。
8. 阅读相关代码和调用链后再修改，不猜测现有行为。
9. 完成后执行必要测试，Java 常规改动至少执行 `mvn clean package`。
10. 检查 `git diff`，提交并 Push 当前任务分支。
11. 创建 Pull Request 到 `main`，等待 CI。
12. 按 `AGENTS.md` 的交付格式汇报；明显 UI 改版同时按 `docs/XINGHUO-UI-TASTE.md` 汇报 Visual Thesis、响应式与状态验证结果。

## 绝对不要自动做的事

- 不直接修改或 Push `main`。
- 不 Force Push。
- 不删除分支或 Tag。
- 不修改已进入生产的历史 Flyway migration。
- 不把密钥、Token、密码、Cookie、`.env` 等提交到 Git。
- 不把生产服务器当成日常开发环境。
- 不因为 PR 合并或 CI 成功就自动部署生产。

生产部署只有在项目负责人明确说出“确认部署”或同等明确授权后才允许执行，并优先通过 GitHub Actions 的 `Deploy Production` 工作流完成。

## 什么时候需要向负责人提问

正常、低风险、可逆的代码开发不需要每一步都等待确认，可以按上述流程连续完成。

只有遇到以下情况时再停下来询问：

- 需求存在会明显影响实现结果的关键歧义；
- 需要删除已有核心功能；
- 需要不可逆或破坏性数据库变更；
- 需要修改生产密钥、权限或生产基础设施；
- 需要 Force Push、重写历史、删除分支或 Tag；
- 需要进入生产部署阶段但尚未获得明确授权。
