# 星火网站 VNext 全量重构与生产部署 SOP

> 状态：方案 A 已建立隔离预览。`/vnext/` 可随生产包发布，但在 UI 体系审核通过前，根路径 `/` 和现有 Thymeleaf 页面保持不变。
>
> 当前运维策略：自动 CI 暂停，`CI` 只通过 `workflow_dispatch` 手动运行。最终验收顺序以 `docs/MANUAL-REFACTOR-VALIDATION.md` 为准。

## 1. 重构边界

### 保留不动

- Spring Boot 3.5.5 / Java 21 后端
- MySQL / Flyway 数据层
- 现有公开 API、账号 API、后台 API 和权限边界
- 现有主图、背景图、透明度、位置、模糊等视觉配置能力
- 当前生产首页和现有 Thymeleaf 页面

### VNext 新增

- `frontend-vnext/`：React + TypeScript + Vite + Tailwind 前端工作区
- shadcn 风格的本地可控 UI primitives；复杂交互底层使用 Radix
- `/vnext/`：隔离预览入口
- 手机 / 平板 / PC / 超宽屏 / 微信 WebView 的响应式环境层
- 新首页、服务中心、三步询价、进度查询、用户中心、后台框架预览
- VNext Playwright 与构建契约测试

### 暂不启用

- 不把 `/` 切到 React
- 不替换现网 UI 设计体系
- 不让 VNext 询价预览写入真实 `/api/quote`
- 不做破坏性数据库迁移

---

## 2. 五阶段执行结果

### 阶段 1：Design System / Foundation

建立可替换的 Token 与组件边界，而不是锁死视觉：

- 颜色、间距、圆角、边框、字体尺寸、触控尺寸统一变量化
- Button / Card / Badge / Skeleton / Sheet 等基础组件
- `prefers-reduced-motion`、forced-colors 与键盘 focus 规则
- UI 审核前仅在 `/vnext/` 生效

### 阶段 2：跨端框架

- 流式容器 + CSS Grid + `clamp()`
- 320 / 375 / 390 / 430 / 768 / 1024 / 1280 / 1440 / 1536 / 1920 / 2560 设计范围
- `safe-area-inset-*`
- `visualViewport` 高度同步
- MicroMessenger / iOS / mobile 环境识别
- 移动端底部导航与抽屉菜单
- 1920+ 大屏扩展画布，避免遗留窄画布/黑边观感

### 阶段 3：核心业务流程

- 服务列表复用 `/api/services`
- 站点配置复用 `/api/site-config`
- CODMW4 状态复用 `/api/public/game-status`
- 业务进度复用 `/api/records/{businessNo}`
- 三步询价流程已做 UI 预览，但生产写入保持关闭；确认 UI 后再绑定 `/api/quote`

### 阶段 4：用户中心与后台

- 用户中心完成新的响应式信息架构预览，并暂时链接现有业务页
- 后台完成响应式 Shell、导航和指标卡框架
- 不修改现有管理员权限、订单逻辑和生产数据

### 阶段 5：测试与发布门禁

- VNext 合同测试：确保 `/vnext/` 隔离、不接管 `/`、不误写真实询价
- Playwright：Chromium + WebKit
- 重点尺寸：320、375 WeChat、390 iPhone、430 WeChat、768、1440、1536、1920、2560
- 检查：横向溢出、44px 触控区、iOS 输入框缩放、移动底栏、超宽屏画布
- `LegacyBridgePlaywrightTest` 覆盖仍依赖 compatibility bridge 的关键 legacy 页面
- 生产 smoke test 检查 `/vnext/`、Vite JS/CSS 以及仍在使用的 legacy bridge CSS 资源

---

## 3. 唯一正确的源码与目录

### GitHub

正式源码唯一基线：

```text
Reid-YM/xinghuo-dailian-wangyou
main
```

### 服务器正式 Git 工作区

```text
/srv/xinghuo-dev/work/xinghuo-v2
```

### 生产运行目录

```text
/opt/xinghuo-v2
```

### 正式服务

```text
xinghuo-v2.service
```

### 禁止再使用

不要把以下旧路径当正式源码：

```text
/opt/xinghuo-v2/source-current
```

---

## 4. 构建流程（重新统一）

VNext 已接入 Maven 生命周期，正常发布只执行一个构建入口：

```bash
cd /srv/xinghuo-dev/work/xinghuo-v2
mvn -B -ntp clean package
```

Maven 会自动完成：

1. 安装项目固定 Node 版本到 `.frontend/`
2. 在 `frontend-vnext/` 安装前端依赖
3. TypeScript typecheck
4. Vite production build
5. 输出到 `target/classes/static/vnext/`
6. Java 编译与测试
7. 打入同一个 Spring Boot JAR

正式 JAR：

```text
target/xinghuo-v2-1.0.0.jar
```

服务器不需要单独全局安装 Node，也不要手工复制 `dist/`。

---

## 5. Git / PR / 手动 CI 流程

所有生产代码改动都走：

```text
feature branch
   ↓
Pull Request
   ↓
merge 到 main
   ↓
锁定当前 main SHA
   ↓
在 main 上手动运行 CI（workflow_dispatch）
   ↓
Java 21 / Maven Build 全绿
+ Chromium + WebKit UI Regression 全绿
   ↓
Deploy Production
```

当前 `CI` workflow 不会因 PR 或 push 自动运行，这是刻意的运维决定。发布门禁必须找到**同一个待发布 main SHA** 的成功手动 CI，不能拿旧 SHA 或其他事件的 CI 结果替代。

禁止：

```text
direct push main
feature branch 直接上生产
服务器直接改源码后发布
绕过手动 CI 手工替换 current.jar
用其他 SHA 的成功 CI 作为当前版本发布证明
```

完整人工验收记录格式见：

```text
docs/MANUAL-REFACTOR-VALIDATION.md
```

---

## 6. 生产部署标准流程

生产工作流使用 `.github/workflows/deploy-production.yml`。

### 触发条件

- 目标必须是当前 `main`
- 目标 commit 必须来自已合并 Pull Request
- 对应**同一个 `main` SHA** 的手动 `CI` `workflow_dispatch` 必须成功
- GitHub Actions 必须取得生产 SSH secrets

优先从 `main` 手动运行 `Deploy Production` 并勾选 `confirm=true`。

`deploy/production` 是备用的长期发布请求分支。只要 `main` 上的 `deploy-production.yml` 发生过变更，在该分支完成受控 workflow 同步前不要使用备用入口；GitHub 会使用触发分支自己的 workflow 定义，旧定义可能导致错误门禁。不要为了同步 workflow 随意向该分支 push，因为 push 本身会触发生产部署工作流。

工作流最终在生产服务器调用：

```bash
deploy <MAIN_GIT_SHA>
```

服务器 deploy gateway 再进入：

```text
scripts/deploy-from-github.sh
```

### 服务器实际 8 步

1. **锁定 GitHub main**
   - `/srv/xinghuo-dev/work/xinghuo-v2` 必须在 `main`
   - worktree 必须干净
   - `origin/main` 必须等于待发布 SHA

2. **Flyway 安全阀**
   - 比较当前线上 JAR 与待发布源码的 migration 清单
   - 没有新增 migration：自动继续
   - 有新增 migration：默认拒绝
   - 只有经过单独评审后才允许 `ALLOW_REVIEWED_MIGRATION=YES`

3. **生产前置检查**
   - `xinghuo-v2.service` active
   - nginx active
   - `nginx -t` 成功
   - 8080 `/`、`/services` 健康
   - 8081 / 18080 未被占用
   - nginx upstream 当前必须指向 8080

4. **完整构建**
   - `mvn -B -ntp clean package`
   - 同时构建 Java + VNext
   - 计算 JAR SHA256

5. **安装候选包**
   - 放入 `/opt/xinghuo-v2/candidates/`
   - 再次验证 SHA256 与运行用户可读性

6. **18080 预发布冒烟**
   - 候选 JAR 独立启动在 18080
   - 执行 `scripts/smoke_test.sh 18080`
   - VNext `/vnext/`、JS/CSS bundle 以及仍在使用的 compatibility bridge 资源必须成功
   - 失败时生产流量完全不受影响

7. **8081 → 8080 蓝绿发布**
   - 候选版本先运行在 8081
   - 8081 完整 smoke 通过
   - nginx 热切到 8081
   - 原子替换 `/opt/xinghuo-v2/current.jar`
   - 新正式实例启动 8080
   - 8080 再次完整 smoke
   - nginx 切回 8080
   - 停止 8081 金丝雀

8. **发布后核验**
   - 正式 service active
   - nginx active
   - upstream 回到 8080
   - 8081 / 18080 无残留监听
   - `current.jar` SHA256 与候选包一致

---

## 7. 自动回滚机制

如果以下任一步失败：

- 8081 金丝雀启动失败
- 8081 smoke 失败
- 新 8080 启动失败
- 新 8080 smoke 失败

发布脚本会优先保持旧版本或自动把 `current.jar` 原子切回部署前 JAR。

脚本每次发布结束都会打印上一版本路径与回滚命令。

### 人工紧急回滚模板

先确认上一版本真实路径：

```bash
readlink -f /opt/xinghuo-v2/current.jar
ls -lt /opt/xinghuo-v2/releases/ | head
```

确认要回滚的 JAR 后：

```bash
PREVIOUS_JAR=/opt/xinghuo-v2/releases/<上一版本.jar>

sudo systemctl stop xinghuo-v2.service
sudo ln -sfn "$PREVIOUS_JAR" /opt/xinghuo-v2/current.jar.next
sudo mv -Tf /opt/xinghuo-v2/current.jar.next /opt/xinghuo-v2/current.jar
sudo systemctl start xinghuo-v2.service

sudo systemctl is-active xinghuo-v2.service
bash /srv/xinghuo-dev/work/xinghuo-v2/scripts/smoke_test.sh 8080
sudo nginx -t
```

如果 nginx 在异常现场仍指向 8081，不要直接停止金丝雀；先确认 8080 已通过 smoke，再把 upstream 恢复到 8080。

---

## 8. 发布后的人工检查清单

### 现网保持不变

```text
/
/services
/quote
/account/*
/admin/*
```

### VNext 预览

```text
/vnext/
```

重点检查：

- PC Chrome / Edge
- macOS / iOS Safari
- Android Chrome
- 微信 Android WebView
- 微信 iOS WebView
- 320px 小屏
- 375 / 390 / 430px 常用手机
- 768px 平板
- 1440 / 1536 PC
- 1920 / 2560 大屏

---

## 9. UI 审核通过后的下一次切换

当前部署只发布 VNext 预览，不切正式 UI。

UI 体系确认后，单独新建 PR 完成：

1. 正式路由迁移策略
2. 真实询价 `/api/quote` 接入
3. 用户中心真实 React 页面逐页接管
4. 后台真实页面逐页接管
5. SEO / SSR / fallback 策略确认
6. 根路径 `/` 切换
7. 旧 Thymeleaf 前端保留一个发布周期作为回退

根路径切换必须独立发布，不能和本次预览基础设施混在一起。
