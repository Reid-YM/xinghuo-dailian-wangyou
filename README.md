# 星火网游平台

这是星火网站与微信小程序的正式源码仓库。**开发仓库提交、生产运行 JAR、微信小程序发布版本是三个不同的版本事实；不能因为合并 main 就宣称线上已更新。**

## 正式入口和技术架构

- 生产网站：<https://wydlgf.cn/>
- 代码主线：`main`，日常工作使用独立 `feat/*`、`fix/*`、`docs/*` 分支与 Pull Request。
- Java 21 / Spring Boot 3.5.5 / Thymeleaf / JavaScript / MySQL / Flyway。
- 新版前台默认按 `FrontendMigrationConfig` 映射正式路由；不再承诺用户通过 `?legacy=1` 选择旧前台。全站紧急回退需遵循运维审核流程，不能把旧版公开参数视为受支持功能。
- 新版后台入口：`/admin/xingwei`，经后台登录及 RBAC 验权。历史页面和接口的访问限制以当前 `SecurityConfig` 为准。
- 微信网站扫码、小程序扫码互通、Native 支付分别有独立配置及验收边界；接口存在不等于已在生产启用。

## 源码目录

- `src/main/java/`：控制器、业务服务、账号与权限、支付、企业微信等后端。
- `src/main/resources/templates/`：Thymeleaf 模板。
- `src/main/resources/static/`：前台、后台、MW4 工具站与小程序共享素材。
- `src/main/resources/db/migration/`：Flyway 迁移。
- `wechat-mini/`：微信小程序客户端源码；发布到微信仍需独立执行与验收。
- `src/test/`：Java / Spring / Playwright 回归测试。
- `.github/workflows/`：CI 与受控生产发布。
- `scripts/`：生产候选检查、冒烟、零停机发布脚本。

## 开发、测试和生产发布

1. 阅读 [AGENTS.md](AGENTS.md) 并确认仓库与当前主线，创建独立任务分支；不要直接向 `main` 推送。
2. 执行 `mvn clean package`、相关回归，并创建 PR；CI 必须针对最终准确提交通过。
3. 经授权合并到 `main` 后，取得**明确生产部署授权**再进入发布流程。
4. 优先使用 GitHub Actions [Deploy Production](.github/workflows/deploy-production.yml) 的受控门禁，而不是在服务器手工复制、覆盖或编辑 JAR。
5. 服务器使用标准 Maven 全量构建与 `18080` 候选冒烟、`8081` 金丝雀、`8080` 正式验证，并记录 Git SHA、JAR SHA256、迁移版本与上一版回滚目标。
6. 数据库迁移、企业微信真实员工写入、微信真实付款/退款、小程序真机发布分别进行审批和外部验收。

**禁止恢复使用**基线 JAR + 覆盖编译、`scripts/dev_build.sh` 拼包、`rsync --delete` 覆盖生产工作区或直接 stop/start 替换固定 JAR 等已退役操作。

## 运维与验收权威资料

- [仓库协作与生产门禁](AGENTS.md)
- [现行源码与零停机部署说明](源码包部署说明.md)
- [一键生产部署及 SSH 受限通道](一键部署说明.md)
- [微信小程序账号互通](docs/WECHAT-MINI-BRIDGE.md)
- [微信 Native 支付上线与验收](docs/WECHAT_NATIVE_PAYMENT.md)

生产密钥、真实 `.env`、私钥、用户上传目录和历史 release JAR **不在 Git 中**。故障排查只能读取必要状态，不能把密码或完整环境配置打印到日志、PR、聊天中。

> 历史 V6A、Frontend Next、旧版回退及旧部署步骤属于阶段记录，只有与当前源码和 [AGENTS.md](AGENTS.md) 一致时才可执行。
