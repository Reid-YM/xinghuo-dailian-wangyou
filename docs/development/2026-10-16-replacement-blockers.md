# 替换未生效排查与修复

## 账号请求实际被拦截

AccountAuthInterceptor 对所有账号/console 写请求要求 X-Requested-With: fetch，且登录和注册不豁免。N4、N5、N5 Actions、N6、N7 曾使用 XMLHttpRequest，实际请求会在服务端被拒绝。修复客户端值，不放宽服务端防护。

增加 Chromium/WebKit 互操作检查：真实浏览器请求通过 MockMvc 的实际 AccountAuthInterceptor 和 AccountExceptionHandler，再进入专用测试端点。保留 XMLHttpRequest 的负向请求必须 403；新版 N4 登录和 N7 注册/找回请求必须通过策略；匿名私有探针仍返回 401。该测试聚焦协议与鉴权边界，不假称已验证真实邮箱、真实密码或生产会话。

## HTML 更新不等于 JS 更新

Next HTML 原已 no-store，但注入的 runtime URL 没有版本。现在每个 runtime 使用实际内容的 SHA256 作为 v 参数；同一内容稳定、内容改变自动换 URL，页面清单仍 no-store。测试根据实际文件重新计算摘要，避免只检查固定版本字符串。

## 仍需完成的入口替换

当前正式首页/服务/询价/账号仍是旧模板，Next shell 又只根据 hash 初始化。正式路由映射和路径路由修复属于下一组改动；在该组及功能对齐完成前，不把本次协议修复声称为完整迁移。

## 历史修复核对

- 退休 ui-fixes.js 未恢复，HTML/JS 无引用。
- admin-control-center-v2 的 ensureVisiblePanel 仍在，#317 的后台导航空白防护测试保留。
- 不通过新增补丁层修复路由；不删除原账号拦截器或关闭 CSRF 自定义头校验。

本组没有迁移、没有生产配置或业务数据写入。开发/CI 可先集成，等全部迁移完成后统一发布。
