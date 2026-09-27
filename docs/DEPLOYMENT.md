# 发布流程（仅维护者）

1. 拉取 `main`，确认工作树干净。
2. 固定基线 JAR 并记录 SHA-256。
3. 构建候选包：

```bash
BASE_JAR=/srv/xinghuo-dev/base/xinghuo-v2-base.jar ./scripts/dev_build.sh build/current.jar
```

4. 在备用端口验证：

```bash
APP_PORT=18080 java -jar build/current.jar --server.port=18080
./scripts/smoke_test.sh 18080
```

5. 备份当前生产 JAR 和环境文件。
6. 安装候选 JAR，保证 `xinghuo` 用户可读，然后重启服务：

```bash
sudo install -m 0640 -o xinghuo -g xinghuo build/current.jar /opt/xinghuo-v2/xinghuo-v2-1.0.0.jar
sudo systemctl restart xinghuo-v2
```

7. 检查 `systemctl status`、应用日志、公网页面和核心接口。

禁止在共享仓库保存生产 `.env`。共享仓库中的发布文档只是流程说明，不会自动获得生产权限。