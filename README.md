# 星火 V2 共享开发仓库

这是星火网游代练平台当前线上版本的共享开发区。仓库采用“固定基线 JAR + 可编译覆盖源码”的方式，当前线上版可以直接重建，新开发者可以从这里继续接手前后端开发。

## 目录职责

- `src/main/java/`：新增或需要覆盖的 Java 类。构建时优先于基线 JAR。
- `src/main/resources/`：需要覆盖或新增的模板、静态资源、Flyway 迁移。
- `scripts/`：构建、冒烟测试、资源抓取和清理脚本。
- `reference/decompiled/`：当前基线 JAR 的反编译参考代码。只用于阅读，不以它直接构建。
- `/srv/xinghuo-dev/base/xinghuo-v2-base.jar`：不可变的当前线上基线 JAR。
- `/srv/xinghuo-dev/repo/`：服务器上的共享工作克隆。
- `/srv/git/xinghuo-v2.git`：共享裸仓库，开发者从这里克隆和推送。

## 开发者快速开始

外部电脑先通过 SSH 克隆（把 `alice` 换成管理员分配的账号）：

```bash
git clone alice@82.156.209.250:/srv/git/xinghuo-v2.git ~/xinghuo-v2
```

已经登录服务器时，也可以使用本地路径：

```bash
git clone /srv/git/xinghuo-v2.git ~/xinghuo-v2
```

首次初始化：

```bash
cd ~/xinghuo-v2
git config user.name "你的名字"
git config user.email "你的邮箱"
cp .env.example .env.dev
chmod 600 .env.dev
./scripts/dev_build.sh
```

构建产物默认生成在 `build/xinghuo-v2-dev.jar`。

本地启动示例：

```bash
set -a
. ./.env.dev
set +a
java -jar build/xinghuo-v2-dev.jar
```

## 重要约束

- 不要在仓库内提交 `.env`、数据库密码、SMTP/API Key、私钥或生产后台密码。
- 不要直接修改 `/opt/xinghuo-v2/xinghuo-v2-1.0.0.jar`。
- 生产部署需要使用 `/srv/xinghuo-dev/base` 的固定基线，并由有权限的维护者执行。
- 反编译参考代码可能包含 CFR 的泛型差异；需要修改已有核心类时，把它从参考目录复制到 `src/main/java` 后再修编译问题。
- 数据库结构变更必须新增 Flyway `V*__*.sql`，不要修改已经上线的历史迁移文件。
- `main` 分支禁止强推和删除；功能分支可以自由推送、强制更新和删除。
- 裸仓库的 `pre-receive` 钩子会拒绝提交 `.env`、私钥、凭据等敏感文件。

## 常用命令

```bash
./scripts/dev_build.sh
# 先启动构建产物并监听 18080，再运行：
./scripts/smoke_test.sh 18080
./scripts/fetch_market_images.py /srv/xinghuo-dev/base/games.tsv /tmp/market /tmp/market.json /tmp/V31.sql
```

详见 `docs/DEVELOPMENT.md` 和 `docs/ARCHITECTURE.md`。