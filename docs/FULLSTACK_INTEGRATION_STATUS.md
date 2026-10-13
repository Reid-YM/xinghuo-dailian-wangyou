# Frontend Next + Xingwei V3M 总联调状态

更新时间：2026-10-14

## 当前性质

本分支 `integration/frontend-v3m-total` 是前后端集中总联调分支，不是生产发布分支，也不代表 V3M 已上线。

## 输入线

- Frontend Next Draft PR #391：A1–A4 核心用户链路已独立通过 Java/Maven 与 Chromium/WebKit。
- Xingwei V3M Draft PR #392：后台运行时韧性已独立通过 Java/Maven 与 Chromium/WebKit。
- 当前基线 `main`：V3L。

## 本轮联调范围

1. 公共壳层 / 首页 / 24 寸宽屏 / 手机端。
2. 服务大厅、服务详情。
3. 询价、业务记录、记录详情。
4. 邮箱登录、星维网络小程序扫码登录页面、个人中心。
5. 新版后台 RBAC / 数据范围 / 运行版本核验。
6. V3M 请求超时、周期复核、visibility / online / offline 复核、权限 fail-closed。
7. Chromium + WebKit，1920 桌面、1440 桌面、常见手机与 320px 窄屏。
8. 公开 API 与后台受保护 API 的安全边界。
9. SEO / canonical / JSON-LD / robots / sitemap 原有契约不得退化。
10. 缓存和版本漂移检测不得被 Frontend Next 静态资源层绕过。

## 明确排除

- V3H 小程序微信支付真实收款：继续保持未合并、待真机 AppID / 商户绑定 / 小额联调。
- V68 小程序支付数据库迁移：不得进入本轮联调分支。
- PR #372 企业微信协作者写入能力：不在本轮联调中强行合并。
- 不直接修改生产环境，不触发 `.deploy-request`。

## 收口门槛

- 本分支 Java/Maven 全绿。
- Chromium + WebKit UI Regression 全绿。
- Frontend Next 与 V3M 无文件级冲突或安全边界倒退。
- 现有支付待验收状态、旧后台兜底、RBAC、SEO 和发布门禁均保持原有真值。
- 达到以上条件后，才整理最终版本号、后台更新记录和主线合并方案。
