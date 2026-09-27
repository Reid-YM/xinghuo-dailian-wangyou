# 开发者接入

## 管理员操作

1. 将开发者公钥保存到服务器临时文件。
2. 执行：

```bash
sudo /srv/xinghuo-dev/onboarding/add-developer.sh alice /tmp/alice.pub
```

3. 将输出中的用户名和克隆地址发给开发者。
4. 查看成员：

```bash
sudo /srv/xinghuo-dev/onboarding/list-developers.sh
```

## 开发者操作

先用管理员分配的用户名测试 SSH：

```bash
ssh alice@82.156.209.250
```

从外部电脑可以直接克隆；已经登录服务器时把地址换成 `/srv/git/xinghuo-v2.git`：

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