# 星火前端资产职责边界

> 目的：阻止前端继续通过 `*-fixes.*`、`*-refine.*`、动态注入样式和轮询脚本叠加解决问题。
> 规则：新需求必须落在明确的模块所有者中；临时兼容代码必须有退出条件和契约测试。

## 1. 公共前台壳层

### `site-experience-v2.css`
负责：
- Header / Footer / 公共容器与层级；
- 公共响应式行为；
- 全站浮动客服、导航抽屉、横向滚动提示的视觉状态；
- 公共无障碍状态和跨浏览器体验。

不得负责：
- 首页独有布局；
- 工具站、攻略、账号中心的业务组件内部样式；
- 后台运营界面。

### `site-shell.js`
负责：
- 品牌 favicon 和公共品牌资源；
- 浮动客服；
- 公共公告条；
- 移动端主导航；
- 公共横向滚动提示；
- 顶部账号入口的登录态展示。

不得负责：
- 首页推荐数据；
- 联系页渠道布局；
- MW4 工具站武器/迷彩业务逻辑；
- 后台面板导航。

## 2. 公共数据与页面逻辑

### `public.js`
当前负责公共 API、站点配置、服务/分类数据、询价辅助和公共工具函数。

长期目标：逐步拆出 API、taxonomy、tracking 等基础模块；在拆分完成前，不再向其中加入页面专属 DOM 修补。

### `pages.js`
负责按 `body[data-page]` 分发的普通公开页面逻辑，例如：
- 首页；
- 服务大厅；
- 服务详情；
- 询价；
- 业务记录；
- 联系页。

首页 Hero 推荐与联系页渠道标签/二维码布局已经从 `ui-fixes.js` 回收到这里。页面专属交互优先进入本文件或独立页面模块，不进入 `site-shell.js`。

## 3. 业务模块

### 账号中心
- `account.js`：当前账号模块主脚本；
- `account-center.js` / `account-profile-controller.js`：账号中心增强职责。

后续重构目标：按登录、资料、安全、设备、订单/进度拆分，保持 API 契约不变。

### MW4 工具站
- `mw4-camo-tracker.js`：当前 `/mastery` 权威脚本；
- `mw4-camo-tracker.css`：当前 `/mastery` 权威样式。

旧 `weaponList`、`progressSuggest`、`.camo-mode-preview-art`、`.camo-check` 补丁已退出当前 DOM，不得重新作为兼容逻辑引入。

### 攻略
- `guide.css` / `guide-render.js` / `guide-community.*`：攻略前台；
- `admin-guides-workspace.css`：攻略后台主结构。

## 4. 后台

当前权威结构：
- `admin.js`：业务主交互；
- `admin-control-center-v2.js/css`：运营工作台结构与导航增强；
- `admin-light-theme.css`：历史组件兼容桥；
- `admin-brand-layout-fixes.css`：仍在使用的品牌布局兼容层。

后续顺序：先修正统一 panel router，再逐步拆 `admin.js`，最后合并兼容 CSS。不得在根因未解决前新增 `admin-*-fixes-vN.*`。

## 5. 退休兼容资产

### `ui-fixes.js`
状态：**运行时职责已退休，仅保留空兼容桩**。

所有原有职责已经迁移：
- 公共导航、账号入口、横向溢出提示 → `site-shell.js`；
- 首页 Hero 推荐、联系页兼容 → `pages.js`；
- MW4 工具行为 → `mw4-camo-tracker.js`；
- 本地业务记录清理确认 → `pages.js` 既有正式逻辑。

该文件不得再加入任何运行时代码。之所以暂时保留，是因为历史模板仍有多处 `<script src="/assets/ui-fixes.js">` 引用；下一阶段只做模板引用清理，全部引用清零后直接删除该文件。

## 6. 新代码准入规则

新增前端代码前必须回答：
1. 这是公共壳层、页面逻辑还是业务模块？
2. 已有哪个文件拥有该职责？
3. 是否在覆盖旧错误，而不是修复根因？
4. 是否会引入第二份数据源？
5. 是否需要 `setInterval` / 全局 `MutationObserver`？若需要，必须说明为什么事件驱动无法解决。
6. 能否通过契约测试或浏览器回归证明旧补丁可以删除？

如果无法明确归属，不应创建新的 `fix/refine/patch` 文件。
