# 星火网站真正重构路线图

## 目标

这轮重构不再以“继续叠页面样式”为核心，而是把现有系统从可运行的混合结构，逐步整理为可长期演进、可测试、可回滚的模块化系统。

原则：

1. 不做一次性大爆炸重写，始终保持 `main` 可构建、生产可发布。
2. 每一阶段都必须有结构门禁、行为回归和生产 smoke。
3. 先建立模块边界，再迁移业务逻辑，最后删除旧实现。
4. API、数据库、鉴权、支付等高风险边界不在同一个 PR 里同时重写。
5. 旧实现只有在新实现经过生产验证后才允许删除。

## 阶段顺序

### #125 VNext 应用架构拆分

- `main.tsx` 只负责 React 启动。
- `app/` 负责应用壳与导航。
- `pages/` 负责页面级组件。
- `components/` 负责跨页面 UI/展示逻辑。
- 保持现有 URL、API、DOM 关键 class、业务行为不变。
- 新增架构契约测试，禁止入口重新膨胀。

### #126 前端业务领域拆分

重点从页面中继续抽离业务逻辑：

- `features/quote/`：询价状态机、校验、提交、截图上传、幂等键。
- `features/records/`：业务编号查询与结果映射。
- `features/services/`：服务筛选、价格显示、服务模型。
- `features/account/`：账号入口模型。
- `shared/api/`：HTTP client、错误映射、AbortSignal、统一响应处理。

目标是让页面只负责组合 UI，不直接承担复杂请求和业务状态。

### #127 后端按业务域拆包

逐步替代当前 `controller / service / repository / model / dto` 横向大包结构，迁移为 package-by-feature：

- `quote/`
- `order/`
- `catalog/`
- `account/`
- `guide/`
- `site/`
- `admin/`
- `integration/`

每个领域内部再按 `web / application / domain / infrastructure` 分层。迁移期间允许旧包与新包并存，但新代码不得继续扩大旧横向包。

### #128 API 与应用服务收口

- Controller 只处理协议转换、鉴权上下文、参数校验。
- Application service 负责编排用例。
- Domain 负责核心规则和状态变化。
- Repository interface 面向领域，JPA/数据库实现放 infrastructure。
- DTO 与 Entity 分离，禁止直接把持久化实体暴露给公开 API。
- 为公开 API 建稳定 contract tests。

### #129 旧前端/模板/样式收敛

- 建立现有 React、Thymeleaf、legacy JS/CSS 的责任清单。
- 将仍有业务价值的旧页面迁到统一组件和 token。
- 删除已被新实现完全替代的模板、JS、CSS。
- 合并 v2/v3/v4/v5/v7/v8 覆盖层前必须有真实 Chromium + WebKit 回归证明等价。

### #130 数据与基础设施重构

- 梳理 Flyway、表关系、索引、状态字段和审计字段。
- 清理重复配置和隐式默认值。
- 统一日志、业务编号、trace/correlation id、异常响应。
- 完善健康检查、发布指标、回滚验证。
- 支付、微信登录等外部集成通过 adapter 隔离，业务层不直接依赖第三方 SDK/HTTP 细节。

### #131 最终清理

只有在上述模块全部经过生产验证后：

- 删除旧横向包残留。
- 删除无引用模板和静态资源。
- 删除兼容桥接代码。
- 更新开发手册与部署说明。
- 运行完整 Maven、Chromium、WebKit、18080、8081、8080 门禁。

## 完成标准

“真正意义上的重构完成”不是代码看起来更新，而是满足：

- 前端入口无业务逻辑。
- 页面无底层 HTTP 细节。
- 后端业务域拥有明确边界。
- Controller / application / domain / infrastructure 职责可区分。
- Entity 不直接作为公开 API contract。
- 旧实现有明确退役清单且绝大部分已删除。
- 核心业务流有自动化行为回归。
- 任一阶段都可独立发布与回滚。
