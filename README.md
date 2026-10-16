# 星火网游代练平台源码包

本仓库是星火网站正式开发源码。当前开发版本为 V6A 完整迁移交付包，代码版本与生产运行版本分别核验；生产部署结果以发布流水线为准。

## V6A 完整迁移交付

正式首页、服务、询价与账号核心入口默认新版，`/admin` 默认进入 `/admin/xingwei`。新版覆盖原管理菜单；完整服务/游戏资料、会员社区、反馈、CRM、员工账号、工具站、统计、支付和公开案例均有原生入口。权限、数据范围、CSRF 与并发检查继续执行。`/admin?legacy=1` 仅保留恢复 ADMIN；前台可按页 `?legacy=1` 或 `app.frontend-next.default-enabled=false` 回退。

字段/入口清单、验证与发布状态见 [完整迁移计划](docs/development/2026-10-16-complete-migration-plan.md) 和 [统一交付核验](docs/development/2026-10-16-v6a-complete-replacement.md)。支付/外部退款、企业微信写入、真实员工及小程序真机验收继续单独处理，自动化不替代外部验收。

## 目录说明

- `src/main/java/`：Java 覆盖源码，包含用户系统、攻略系统、上传、页面控制器、安全配置等后续维护代码。
- `src/main/resources/templates/`：Thymeleaf 页面模板，前台、后台、用户中心、攻略、工具站页面都在这里。
- `src/main/resources/static/assets/`：CSS、JS、图片、MW4 工具站资源、游戏封面、服务封面等静态资源。
- `src/main/resources/db/migration/`：Flyway 数据库迁移脚本，新增表、字段、初始化攻略和服务数据都在这里。
- `scripts/`：构建、部署、冒烟测试、图片抓取、CSS 清理等脚本。
- `.env.example`：环境变量示例，不含真实密码。
- `开发手册.md`：字段、页面、接口、部署、资源维护说明。

## 不包含的内容

- 不包含真实 `.env` 密钥。
- 不包含 `build/` 临时构建产物。
- 不包含 `/tmp` 验证文件。
- 不包含线上 `uploads/` 用户上传目录。
- 不包含旧 release JAR。

## 当前部署模式

当前项目采用：

```text
服务器基线 JAR + 本源码包覆盖编译 = 新线上 JAR
```

服务器基线 JAR 路径：

```bash
/srv/xinghuo-dev/base/xinghuo-v2-base.jar
```

构建脚本：

```bash
scripts/dev_build.sh
```

部署时需要在服务器上设置 `BASE_JAR` 指向基线 JAR。

## 从本地源码包部署到服务器

下面假设本地目录是 `xinghuo-v2-source`，服务器是 `ubuntu@82.156.209.250`。

1. 本地修改源码。

2. 上传源码到服务器工作目录：

```bash
rsync -av --delete \
  --exclude build/ \
  --exclude .env \
  --exclude '*.jar' \
  ./xinghuo-v2-source/ \
  ubuntu@82.156.209.250:/srv/xinghuo-dev/work/xinghuo-v2/
```

3. 在服务器构建新 JAR：

```bash
ssh ubuntu@82.156.209.250
cd /srv/xinghuo-dev/work/xinghuo-v2
BASE_JAR=/srv/xinghuo-dev/base/xinghuo-v2-base.jar \
  bash scripts/dev_build.sh build/xinghuo-v2-$(date +%Y%m%d%H%M%S).jar
```

4. 安装为 release 并重启：

```bash
cd /srv/xinghuo-dev/work/xinghuo-v2
NEW_BUILD=$(ls -t build/xinghuo-v2-*.jar | head -n 1)
STAMP=$(date +%Y%m%d%H%M%S)
sudo install -o xinghuo -g xinghuo -m 0640 "$NEW_BUILD" "/opt/xinghuo-v2/releases/xinghuo-v2-source-${STAMP}.jar"
sudo ln -sfn "/opt/xinghuo-v2/releases/xinghuo-v2-source-${STAMP}.jar" /opt/xinghuo-v2/current.jar
sudo systemctl restart xinghuo-v2
sleep 15
systemctl is-active xinghuo-v2
curl -I https://wydlgf.cn/
```

5. 冒烟测试：

```bash
bash scripts/smoke_test.sh 8080
journalctl -u xinghuo-v2 -n 80 --no-pager
```

## 回滚

查看最近 release：

```bash
ls -lt /opt/xinghuo-v2/releases/*.jar | head
```

切回某个旧版本：

```bash
sudo ln -sfn /opt/xinghuo-v2/releases/目标版本.jar /opt/xinghuo-v2/current.jar
sudo systemctl restart xinghuo-v2
```

## 2026-10-04 v024 补充

- 工具站 `/mastery` 桌面端改为动态响应：宽屏三列，中屏两列且进度区跨整行，窄屏单列，避免大窗口右侧空白或面板被挤压。
- 联系方式后台的“微信号”兼容填写微信跳转链接，例如二维码解析出的 `https://u.wechat.com/...`；前台联系页和提交成功卡片会显示“打开微信”并直接跳转。
- 后台上传客服二维码时会尝试使用浏览器原生 `BarcodeDetector` 识别二维码链接；浏览器不支持时，可手动把二维码链接粘贴到“微信号 / 跳转链接”。
- 本次不新增数据表和字段，不需要手动执行数据库迁移。
