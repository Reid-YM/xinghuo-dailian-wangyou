# 星火网站真正重构路线图

## 当前状态（2026-10-11）

代码层重构已经进入验收阶段，不能再把“结构迁移完成”和“生产验收完成”混为一谈。

已在 `main` 收口的代码层工作：

- 后端已按业务域 package-by-feature 收口，顶层旧横向业务包 `controller / service / repository / model / dto / web` 已退役，并有架构守卫防止回流。
- 全局 `com.xinghuo.dto.ApiDtos` 已删除；公开/后台 HTTP contract 已迁回各业务域 `web` 边界，并有 contract tests 锁定字段与边界。
- Controller、application、domain、infrastructure 的业务域边界已经建立，旧 Legacy Service 的逐域退役已完成主体收口。
- 公共详情页、业务进度页、个人中心旧子页与攻略投稿页的 VNext compatibility bridge 已改为模板声明式加载；`site-image-controls.js` 不再负责动态注入兼容 CSS，只保留图片布局能力。
- 后台 compatibility bridge 已收敛到单一后台样式入口，避免多条 JS 动态加载链路。
- 数据/观测/发布基础设施重构及最终零消费者资产清理已合入主线；仍被补丁构建流程消费的基线 jar 清理清单继续保留。
- 自动 CI 按当前运维决定保持暂停，仅保留 `workflow_dispatch` 手动触发。

尚未完成、因此暂不能宣告“生产级重构完成”的验收门：

1. 手动触发 Java 21 / Maven 全量构建与测试。
2. Chromium 回归。
3. WebKit 回归。
4. 18080 验证环境 smoke。
5. 8081 预发布/切换验证。
6. 8080 生产端口最终验证与回滚确认。
7. 只有上述浏览器与生产 smoke 证明等价后，才允许进一步删除仍在使用的 `vnext-legacy-bridge-v3.css`、`vnext-account-submit-v3.css` 等兼容样式本体。

因此当前状态定义为：**代码结构重构已基本收口，正在等待手动 CI + 浏览器 + 生产端口门禁验收；兼容 CSS 本体暂保留属于刻意设计，不是漏清理。**

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
