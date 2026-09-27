# 开发者接入

## 管理员操作（默认使用密码登录）

1. 创建开发者账号：

```bash
sudo /srv/xinghuo-dev/onboarding/add-developer-password.sh alice
```

2. 按提示输入两次登录密码。
3. 将服务器地址、端口、用户名和密码发给开发者。
4. 查看成员：

```bash
sudo /srv/xinghuo-dev/onboarding/list-developers.sh
```

如果开发者后续希望额外使用公钥登录，可以再执行：

```bash
sudo /srv/xinghuo-dev/onboarding/add-developer.sh alice /tmp/alice.pub
```

## 开发者操作

```bash
ssh alice@82.156.209.250
```

登录成功后会自动进入 `/srv/xinghuo-dev/repo` 共享开发区，并显示共享区路径。首次进入建议阅读：

```bash
less README.md
```

如需在自己的 home 下建立独立工作副本：

```bash
git clone alice@82.156.209.250:/srv/git/xinghuo-v2.git ~/xinghuo-v2
cd ~/xinghuo-v2
git config user.name "Alice"
git config user.email "alice@example.com"
./scripts/dev_build.sh
```

开发自己的改动：

```bash
git checkout -b feature/your-feature
git add ...
git commit -m "feat: ..."
git push origin feature/your-feature
```

合并到 `main` 前先运行构建和冒烟测试。`main` 禁止强推和删除；已合并或放弃的功能分支可以删除：

```bash
git push origin --delete feature/your-feature
```

如果开发库、密钥或其他敏感内容误入提交，推送钩子会拒绝上传。不要把生产 `.env` 复制进仓库。