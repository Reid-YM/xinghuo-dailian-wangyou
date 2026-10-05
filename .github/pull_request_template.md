## 变更说明

请简要说明本次修改解决了什么问题、采用了什么方案。

## 自检清单

- [ ] 本次修改只包含当前任务相关内容，没有无关的大规模改动
- [ ] 已检查 `git diff`，没有误删已有功能
- [ ] 已确认未提交密码、Token、Cookie、私钥或其他敏感信息
- [ ] 如涉及数据库，已使用新的 Flyway migration，未修改已上线历史 migration
- [ ] 已说明配置变化、数据库影响和回滚方式（如适用）
- [ ] GitHub Actions `CI / Java 21 / Maven Build` 已通过

## 测试结果

请填写实际执行结果，例如：

```text
GitHub Actions CI: success
mvn clean package: success
其他测试: 无 / 说明
```

> 当前仓库为 GitHub Free 私有仓库，GitHub 无法强制启用“required status checks”。因此在合并到 `main` 前，开发者和 AI 必须人工确认 CI 已成功；CI 失败或仍在运行时不得合并。
