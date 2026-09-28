# 架构说明

## 技术栈

- Spring Boot 3.5.5
- Java 21
- MySQL 8
- Spring MVC + Thymeleaf
- Spring Data JPA + JdbcTemplate
- Flyway
- Nginx + systemd

## 构建方式

线上 JAR 依赖较多，且当前工作区没有完整 Maven 工程。因此共享仓库采用基线覆盖构建：

1. 以 `/srv/xinghuo-dev/base/xinghuo-v2-base.jar` 为固定基线。
2. 将 `src/main/resources` 覆盖进基线 JAR。
3. 用 Java 21 编译 `src/main/java`，编译时读取基线 JAR 的全部三方依赖。
4. 把编译产物写回 JAR。
5. 按 `scripts/delete-jar-entries.txt` 删除已废弃类、模板和静态文件。
6. 生成 `build/xinghuo-v2-dev.jar`。

这样既能保证开发环境可重复构建，也不会因为反编译代码的泛型差异回退线上功能。

## 主要业务模块

- 游戏资料：`game_catalog`、`service_categories`、`game_platforms`、`platform_catalog`
- 服务商品：`services`
- 询价与订单：`quotes`、`customer_orders`
- 展示配置：`site_config`、`site_settings`、`hot_slots`

## 主要路由

- 前台：`/`、`/services`、`/service/{slug}`、`/game/{slug}`、`/mastery`
- 公开 API：`/api/services`、`/api/catalog`、`/api/tool-station`、`/api/site-config`
- 后台：`/admin`
- 后台 API：`/api/admin/**`

## 部署基线

- 生产 JAR：`/opt/xinghuo-v2/xinghuo-v2-1.0.0.jar`
- 服务：`systemd` 单元 `xinghuo-v2`
- 生产环境变量：`/opt/xinghuo-v2/.env`
- 上传目录：`/opt/xinghuo-v2/uploads`
- Nginx：反向代理至 `127.0.0.1:8080`

生产环境变量和数据库凭据不属于共享仓库内容。