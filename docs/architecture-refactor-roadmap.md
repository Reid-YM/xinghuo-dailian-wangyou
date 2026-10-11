# 星火网站真正重构路线图

## 当前状态（2026-10-11）

代码结构重构与第一轮生产级验收已经完成，项目处于 **#131 最终清理 / 兼容资产退役收口阶段**。

已完成的主架构工作：

- 后端已按业务域 package-by-feature 收口，顶层旧横向业务包 `controller / service / repository / model / dto / web` 已退役，并有架构守卫防止回流。
- 全局 `com.xinghuo.dto.ApiDtos` 已删除；公开/后台 HTTP contract 已迁回各业务域 `web` 边界，并有 contract tests 锁定字段与边界。
- Controller、application、domain、infrastructure 的业务域边界已经建立，旧 Legacy Service 的逐域退役已完成主体收口。
- 公共详情页、业务进度页、个人中心与攻略投稿页均已迁移到明确的 feature-owned CSS owner；`site-image-controls.js` 不再动态注入兼容 CSS。
- 前端控制中心已在 #256 脱离 shared bridge，由 `admin-frontend-controls.css` 独立负责当前生产视觉。
- 主运营后台由 `admin-control-center-v2.css + admin-light-theme.css` 接管；独立攻略后台由 `admin-guides-workspace.css + admin-light-theme.css` 接管；`admin-brand-layout-fixes.css` 只保留 Logo 与窄屏修复职责。
- 数据/观测/发布基础设施重构及零消费者资产清理已合入主线；历史补丁构建脚本不再参与正式生产发布。
- CI 已恢复自动化：Pull Request 指向 `main` 自动跑完整 CI，合并到 `main` 后 push 再自动跑一次，`workflow_dispatch` 保留为备用入口。
- `main` SHA `73dd113bc52b4ddf1c4d80b602f510ee036cde65` 已完成同 SHA `Java 21 / Maven Build`、Chromium、WebKit、18080、8081、8080、nginx 回切和回滚路径验证，并成功发布到生产。

因此当前状态定义为：**主架构重构已经完成生产级闭环；剩余是最终兼容资产退役、文档收尾，以及对最终清理 SHA 的生产再验收。**

### #131 当前最终清理批次

已经完成的小批次：

- #251：`vnext-account-submit-v3.css` → `account-submit.css`；
- #253：account center → `account-center.css / account-center-refine.css`；
- #254：service detail → `service-detail.css`；
- #255：record / order detail → `record-detail.css`；
- #256：frontend control center → `admin-frontend-controls.css`。

当前 #257 最终批次：

- 取消 `admin-brand-layout-fixes.css` 对 `vnext-legacy-bridge-v3.css` 的 `@import`；
- 删除无消费者的 `deepPageBridge`；
- 删除 `vnext-legacy-bridge-v3.css` 本体；
- 保留账号投稿页真正需要的 `account-submit.css`；
- 新增独立攻略后台无 bridge 的 Chromium / WebKit 视觉回归；
- production smoke 将旧 bridge 从“必须 200”改为“必须 404”，并同时要求全部 canonical replacement assets 200。

这批只处理 CSS owner 与验证，不修改 API、鉴权、数据库、业务路由或生产部署逻辑。

## 目标

这轮重构不再以“继续叠页面样式”为核心，而是把现有系统从可运行的混合结构整理为可长期演进、可测试、可回滚的模块化系统。

原则：

1. 不做一次性大爆炸重写，始终保持 `main` 可构建、生产可发布。
2. 每一阶段都必须有结构门禁、行为回归和生产 smoke。
3. 先建立模块边界，再迁移业务逻辑，最后删除旧实现。
4. API、数据库、鉴权、支付等高风险边界不在同一个 PR 里同时重写。
5. 旧实现只有在新实现经过验证后才允许删除。

## 阶段顺序

### #125 VNext 应用架构拆分

- `main.tsx` 只负责 React 启动。
- `app/` 负责应用壳与导航。
- `pages/` 负责页面级组件。
- `components/` 负责跨页面 UI/展示逻辑。
- 保持现有 URL、API、DOM 关键 class、业务行为不变。
- 新增架构契约测试，禁止入口重新膨胀。

### #126 前端业务领域拆分

- `features/quote/`：询价状态机、校验、提交、截图上传、幂等键。
- `features/records/`：业务编号查询与结果映射。
- `features/services/`：服务筛选、价格显示、服务模型。
- `features/account/`：账号入口模型。
- `shared/api/`：HTTP client、错误映射、AbortSignal、统一响应处理。

目标是让页面只负责组合 UI，不直接承担复杂请求和业务状态。

### #127 后端按业务域拆包

迁移为 package-by-feature：

- `quote/`
- `order/`
- `catalog/`
- `account/`
- `guide/`
- `site/`
- `admin/`
- `integration/`

每个领域内部按 `web / application / domain / infrastructure` 分层，新代码不得重新扩大旧横向包。

### #128 API 与应用服务收口

- Controller 只处理协议转换、鉴权上下文、参数校验。
- Application service 负责编排用例。
- Domain 负责核心规则和状态变化。
- Repository interface 面向领域，JPA/数据库实现放 infrastructure。
- DTO 与 Entity 分离，禁止直接把持久化实体暴露给公开 API。
- 为公开 API 建稳定 contract tests。

### #129 旧前端/模板/样式收敛

- 建立 React、Thymeleaf、legacy JS/CSS 的责任清单。
- 将仍有业务价值的旧页面迁到统一组件和 token。
- 删除已被新实现完全替代的模板、JS、CSS。
- 合并/删除兼容层前必须有真实 Chromium + WebKit 回归证明。

### #130 数据与基础设施重构

- 梳理 Flyway、表关系、索引、状态字段和审计字段。
- 清理重复配置和隐式默认值。
- 统一日志、业务编号、trace/correlation id、异常响应。
- 完善健康检查、发布指标、回滚验证。
- 支付、微信登录等外部集成通过 adapter 隔离，业务层不直接依赖第三方 SDK/HTTP 细节。

### #131 最终清理

当前阶段，只允许小批次、可回滚的退役工作：

- 删除旧横向包残留。
- 删除无引用模板和静态资源。
- 将页面专属兼容规则迁移到明确业务 owner。
- 删除最后的共享兼容桥接代码。
- 更新开发手册与部署说明。
- 每一批都重新运行完整 Maven、Chromium、WebKit；合入 `main` 后自动再跑 push CI；生产发布仍走 18080、8081、8080 门禁。

## 完成标准

“真正意义上的重构完成”必须满足：

- 前端入口无业务逻辑。
- 页面无底层 HTTP 细节。
- 后端业务域拥有明确边界。
- Controller / application / domain / infrastructure 职责可区分。
- Entity 不直接作为公开 API contract。
- 旧实现有明确退役清单且绝大部分已删除。
- 核心业务流有自动化行为回归。
- 任一阶段都可独立发布与回滚。
- `vnext-legacy-bridge-v3.css` 最后消费者已经迁走，最终代码中该资产不存在。
- 无 bridge 版本通过 Maven、Chromium、WebKit，并在生产发布时通过 18080 / 8081 / 8080 smoke，旧 bridge 路径返回 404、canonical assets 返回 200。
