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

```bash
ssh alice@82.156.209.250
git clone /srv/git/xinghuo-v2.git ~/xinghuo-v2
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

合并到 `main` 前先运行构建和冒烟测试。