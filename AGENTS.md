# AGENTS.md

本仓库遵循“固定基线 JAR + 可编译覆盖源码”的既有架构。任何代理或开发者开始工作前必须按顺序执行：

1. UI/视觉设计任务先访问 `https://github.com/VoltAgent/awesome-design-md`，读取与任务相关的 PlayStation、NVIDIA、Binance、Linear 或 Vercel 设计规范，并记录所用提交。
2. 如果无法访问 GitHub，必须明确说明无法读取，不得假装已经参考。
3. 阅读仓库根目录 `DESIGN.md` 和本文件。
4. 阅读 `docs/DEVELOPMENT.md`、`docs/ARCHITECTURE.md` 和 `docs/DEPLOYMENT.md`。
5. 运行 `git status -sb`，确认工作树状态和当前分支。
6. 不在 `main` 上直接开发，不强制推送或删除 `main`。

外部设计仓库只用于提炼规则，不复制品牌 Logo、商标、专有字体、完整页面或独特组件。所有最终规则必须收敛到星火自己的 `DESIGN.md`。

## 项目身份

- 站点：星火网游工作室。
- 核心游戏：CODMW4。
- 业务：代练、陪玩、咨询、商品展示、询价、订单记录与后台管理。
- 视觉：黑黄游戏科技风，专业、可信、克制，不做低价地摊风。
- 详细规范以 `DESIGN.md` 为准。

## 架构约束

- `/srv/xinghuo-dev/base/xinghuo-v2-base.jar` 是不可变基线 JAR。
- 新功能或覆盖类放在 `src/main/java/`。
- 模板和静态资源放在 `src/main/resources/`。
- 修改已有类时，先从 `reference/decompiled/` 找到对应类，再复制到 `src/main/java/` 相同包路径并修正编译差异。
- Flyway 迁移只能新增 `src/main/resources/db/migration/V*__*.sql`，不得修改已上线历史迁移。
- 不直接修改 `/opt/xinghuo-v2/xinghuo-v2-1.0.0.jar` 作为开发手段。
- 不引入 Docker、PM2、Kubernetes 或第二套发布体系，除非用户明确要求并完成设计评审。
- 不在仓库提交 `.env`、数据库密码、SMTP/API Key、私钥、Token、生产后台密码、备份 JAR 或数据库导出。

## 开发流程

```bash
cd /srv/xinghuo-dev/repo
git switch -c feat/<short-name>
./scripts/dev_build.sh build/current.jar
APP_PORT=18080 java -jar build/current.jar --server.port=18080
./scripts/smoke_test.sh 18080
git diff --check
git status --short
```

要求：

- 共享仓库的服务器远端为 `/srv/git/xinghuo-v2.git`。
- GitHub 远端为 `Reid-YM/xinghuo-dailian-wangyou`。
- 功能分支可以自由提交和推送；`main` 只接收经过验证的合并。
- 提交应聚焦单一目标，不混入无关格式化或大范围重写。
- 前端改动必须递增相关静态资源版本查询参数，避免浏览器缓存旧文件。

## UI 工作规则

- 先审计现状，再实施最小必要修改。
- 前台与后台必须分别验证。
- 视觉升级不得删除或隐藏功能。
- 不删除商品、询价记录、订单记录。
- 不隐藏客服入口、商品入口、后台菜单或危险操作确认。
- 不改变价格逻辑或业务流程，除非任务明确要求。
- 不使用测试域名、临时图片 URL 或硬编码占位图。
- 图片必须有 PC/移动端裁切策略，不能遮挡文字。
- 所有响应式改动必须检查手机、平板、普通电脑、24 寸和宽屏。
- 后台优先稳定、清晰和可操作性。
- 文案必须符合 `DESIGN.md`，禁止夸大承诺。

## 测试要求

至少执行：

```bash
./scripts/dev_build.sh build/current.jar
APP_PORT=18080 java -jar build/current.jar --server.port=18080
./scripts/smoke_test.sh 18080
```

同时验证：

- 首页 Hero 文案、图片和 CTA。
- 商品卡片完整信息与移动端单列布局。
- 询价、记录、咨询入口。
- 后台登录、商品管理、询价记录、订单记录、分类、图片配置和联系方式配置。
- 页面无横向溢出。
- 浏览器控制台无新增严重错误。

如果环境支持，使用 375、768、1280、1920、2560 等宽度进行截图对比。

## 发布流程

优先使用现有 GitHub Actions：

```text
push main
→ Actions checkout
→ SSH/rsync 到 /srv/xinghuo-dev/actions-workdir/xinghuo-v2
→ scripts/deploy_from_actions.sh
→ 备份当前 JAR
→ 生成 release
→ 更新 /opt/xinghuo-v2/xinghuo-v2-1.0.0.jar
→ systemctl restart xinghuo-v2
```

维护者手动发布时严格遵循 `docs/DEPLOYMENT.md`：

1. 备份当前 JAR 和必要配置。
2. 使用固定基线构建候选 JAR。
3. 在 `18080` 启动并完成冒烟测试。
4. 安装到 `/opt/xinghuo-v2/xinghuo-v2-1.0.0.jar`。
5. 保持 `xinghuo:xinghuo`、`0640` 权限。
6. 重启 `xinghuo-v2.service`。
7. 检查 systemd、日志、公网首页、核心 API 和后台入口。
8. 保留 release 与 backup 用于回滚。

不得在服务器无法访问 GitHub HTTPS 时改变部署架构；现有 Actions 由 GitHub Runner 主动 SSH/rsync 到服务器，不要求服务器主动拉取 GitHub。

## 完成报告

每次任务完成必须报告：

- 修改的文件及每项改动。
- 是否涉及数据库。
- 是否影响运行服务。
- 使用的测试命令与结果。
- 是否仍未验证的内容。
- 部署所需命令，但不得未经明确授权直接发布生产。
