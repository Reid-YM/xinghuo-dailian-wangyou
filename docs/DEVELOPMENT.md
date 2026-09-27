# 开发说明

## 第一次拉取

```bash
git clone /srv/git/xinghuo-v2.git ~/xinghuo-v2
cd ~/xinghuo-v2
git config user.name "你的名字"
git config user.email "你的邮箱"
chmod +x scripts/*.sh
./scripts/dev_build.sh
```

## 修改已有 Java 类

1. 从 `reference/decompiled/com/xinghuo/` 找到对应类。
2. 复制到 `src/main/java/com/xinghuo/` 相同包路径。
3. 删除 CFR 注释，按当前项目版本修正泛型或 Kotlin/Lombok 反编译差异。
4. 执行 `./scripts/dev_build.sh`。
5. 启动构建产物并运行 `./scripts/smoke_test.sh`。

新增类可以直接放到 `src/main/java/com/xinghuo/`；构建脚本会把整个覆盖层编译进 JAR。

## 修改前端资源

模板放在 `src/main/resources/templates/`，静态资源放在 `src/main/resources/static/`。修改后建议递增页面里的静态资源查询版本，例如 `v=20260928v1`，避免浏览器继续使用旧脚本。

## 数据库迁移

- 在 `src/main/resources/db/migration/` 新增 `V数字__描述.sql`。
- 不要修改已经应用到生产数据库的历史迁移内容，否则 Flyway 会校验失败。
- 开发库请使用独立数据库，不要连接生产库。

## 提交前检查

```bash
./scripts/dev_build.sh
./scripts/smoke_test.sh
git diff --check
git status --short
```

不要在提交中包含 `.env`、密码、私钥、生产备份 JAR 或数据库导出文件。