# Frontend Next N7：统一注册 / 登录 / 找回

日期：2026-10-15

## 目标

在 N4–N6 已上线的账号、业务与安全能力基础上，统一 Frontend Next 的游客认证入口，修复“前端展示多种账号密码登录方式，但当前后端账号密码体系只支持邮箱”的不一致。

## 本阶段能力

- `/next/account` 游客状态提供“登录 / 注册 / 找回密码”三态切换。
- 账号密码登录强制收口为邮箱，不再展示手机号、QQ、微信号作为账号密码登录方式。
- 微信开放平台扫码登录继续保留。
- “星火迷彩追踪器”小程序扫码登录继续保留，并继续复用 N4 Mini runtime。
- 注册复用现有：
  - `POST /api/account/register/verify/request`
  - `POST /api/account/register`
- 找回复用现有：
  - `POST /api/account/password/reset/request`
  - `POST /api/account/password/reset`
- 密码提示与服务端策略保持一致：至少 8 位，至少两类字符，BCrypt 有效输入上限 72 字节。
- 注册验证码按现有流程使用 6 位邮箱验证码；找回密码使用邮箱 + 验证码 + 新密码。

## 数据与安全边界

- 不新增数据库迁移。
- 不建立第二套账号模型或第二套会话。
- 所有写请求继续携带 `X-Requested-With: XMLHttpRequest`。
- 注册成功仍由现有 AccountService 建立正式网站会话。
- 找回密码继续使用“无论邮箱是否存在均返回相同提示”的防账号枚举策略。
- 不使用 `innerHTML`、`localStorage`、`sessionStorage` 保存认证数据。
- 不在测试或前端源码中写入真实密码、令牌或生产凭据。

## 回退

移除 `/next/n7-auth.js` runtime 即可恢复 N6 游客登录界面；旧 `/account/login`、`/account/register`、`/account/reset` 全部继续保留。

## 测试

- Java 合约：runtime 顺序、邮箱唯一账号密码模式、复用现有注册与找回端点、安全请求头与回退入口。
- Chromium：邮箱登录归一化、发送注册验证码、注册请求 payload 与请求头。
- WebKit：找回密码申请、验证码重置、新密码成功后返回新版登录态。

## 后续

N7 通过 PR CI、合并后主分支 CI 与生产门禁后，进入 Frontend Next 替换前总联调：游客认证、登录态、业务记录、售后、安全设置、微信登录、小程序扫码登录与移动端统一回归。