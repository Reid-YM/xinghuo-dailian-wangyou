# 星火网游代练平台

本仓库是星火网站唯一正式源码基线。生产代码必须通过 GitHub 分支、Pull Request、CI 和 `Deploy Production` 工作流进入线上环境。

## 目录说明

- `src/main/java/`：Spring Boot / Java 业务、账号、后台、安全配置与控制器。
- `src/main/resources/templates/`：当前生产 Thymeleaf 页面。
- `src/main/resources/static/`：当前生产静态资源。
- `frontend-vnext/`：VNext React + TypeScript + Vite 响应式前端；UI 审核前仅通过 `/vnext/` 隔离预览。
- `src/main/resources/db/migration/`：Flyway 数据库迁移。
- `scripts/`：构建、冒烟、GitHub 部署和零停机发布脚本。
- `docs/VNEXT_REBUILD_AND_DEPLOY.md`：VNext 重构范围、构建、发布、验证和回滚的统一 SOP。
- `.env.example`：环境变量示例，不包含真实密钥。

## 正式源码与服务器目录

GitHub 正式分支：

```text
Reid-YM/xinghuo-dailian-wangyou
main
```

服务器正式 Git 工作区：

```text
/srv/xinghuo-dev/work/xinghuo-v2
```

生产运行目录：

```text
/opt/xinghuo-v2
```

生产服务：

```text
xinghuo-v2.service
```

> `/opt/xinghuo-v2/source-current`、旧 base JAR 覆盖构建、`scripts/dev_build.sh` 手工发布均不再是正式生产流程，不得作为日常发布入口。

## 本地 / CI 构建

统一使用 Maven：

```bash
mvn -B -ntp clean package
```

VNext 已接入 Maven 生命周期。该命令会同时完成：

1. Java / Spring Boot 构建与测试
2. 项目固定 Node 环境准备
3. `frontend-vnext/` 依赖安装、TypeScript typecheck、Vite production build
4. VNext 输出到 JAR 内 `/vnext/`
5. 生成 `target/xinghuo-v2-1.0.0.jar`

服务器无需手工全局安装 Node，也不要单独复制 Vite `dist`。

## 正式开发与合并流程

```text
feature branch
  ↓
Pull Request
  ↓
CI（Java/Maven + Chromium/WebKit）
  ↓
merge main
  ↓
main CI 全绿
  ↓
Deploy Production
```

禁止直接 push `main`，禁止从 feature branch 直接上生产，禁止在服务器直接修改正式源码后部署。

## 正式生产发布

生产发布统一由：

```text
.github/workflows/deploy-production.yml
```

发起。工作流会确认：

- 发布目标是当前 `main`
- commit 来自已合并 PR
- 对应 main CI 成功
- 生产 SSH 配置可用

随后在服务器执行标准部署入口：

```text
deploy <MAIN_GIT_SHA>
```

部署链：

```text
GitHub main
  ↓
/srv/xinghuo-dev/work/xinghuo-v2
  ↓
Flyway 安全阀
  ↓
mvn clean package
  ↓
18080 预发布完整冒烟
  ↓
8081 金丝雀完整冒烟
  ↓
Nginx 热切流
  ↓
原子切换 /opt/xinghuo-v2/current.jar
  ↓
8080 正式完整冒烟
  ↓
Nginx 切回 8080
```

发布失败时会优先保持旧版本；新正式实例失败时会尝试自动回滚到部署前 JAR。

完整命令、检查条件和人工回滚步骤见：

```text
docs/VNEXT_REBUILD_AND_DEPLOY.md
```

## 生产冒烟

服务器本机：

```bash
cd /srv/xinghuo-dev/work/xinghuo-v2
bash scripts/smoke_test.sh 8080
```

冒烟范围包括现有公开页面、账号权限、后台权限，以及 VNext `/vnext/` 和其 Vite JS/CSS bundle。

## VNext 当前边界

当前 VNext 只作为架构和 UI 审核预览：

```text
/vnext/
```

在 UI 体系确认前：

- `/` 不切换到 React
- 现有 Thymeleaf 生产 UI 不替换
- VNext 询价不会写入真实 `/api/quote`
- 用户中心和后台仍保留现有正式业务页面

UI 审核完成后，正式根路由迁移必须走独立 PR 和独立生产发布。

## 2026-10-04 v024 补充

- 工具站 `/mastery` 桌面端改为动态响应：宽屏三列，中屏两列且进度区跨整行，窄屏单列，避免大窗口右侧空白或面板被挤压。
- 联系方式后台的“微信号”兼容填写微信跳转链接，例如二维码解析出的 `https://u.wechat.com/...`；前台联系页和提交成功卡片会显示“打开微信”并直接跳转。
- 后台上传客服二维码时会尝试使用浏览器原生 `BarcodeDetector` 识别二维码链接；浏览器不支持时，可手动把二维码链接粘贴到“微信号 / 跳转链接”。
- 本次不新增数据表和字段，不需要手动执行数据库迁移。
