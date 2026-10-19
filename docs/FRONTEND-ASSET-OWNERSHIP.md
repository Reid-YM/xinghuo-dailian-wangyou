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
负责：品牌 favicon、浮动客服、公共公告条、移动端主导航、横向滚动提示和顶部账号入口登录态。

不得负责：首页推荐数据、联系页渠道布局、MW4 工具业务逻辑和后台面板导航。

## 2. 公共数据与页面逻辑

### `public.js`
当前负责公共 API、站点配置、服务/分类数据、询价辅助和公共工具函数。长期目标是逐步拆出 API、taxonomy、tracking 等基础模块；在拆分完成前，不再加入页面专属 DOM 修补。

### `pages.js`
负责按 `body[data-page]` 分发的普通公开页面逻辑，例如首页、服务大厅、服务详情、询价、业务记录和联系页。页面专属交互优先进入本文件或独立页面模块，不进入 `site-shell.js`。

## 3. 业务模块

### 账号认证与账号中心
- `account-core.js`：账号模块共享底座；负责同源 API 请求、安全登录回跳、错误映射、密码策略、字段校验与通用认证控件，不负责具体页面启动。
- `account-auth.js`：登录、注册、邮箱验证码、忘记密码与密码重置；仅由 `account-login`、`account-register`、`account-reset` 加载，并依赖 `account-core.js`。
- `account-center.js`：个人中心概览、需求、订单、进度，以及设置页分区导航。
- `account-submit.js`：攻略投稿页；负责作者状态、编辑器、本机草稿、安全预览、审核列表与撤回，依赖 `account-core.js`。
- `account-settings.js`：设置页中除个人资料外的职责；负责账号只读信息、回访联系方式、密码、安全邮箱、在线设备、安全活动和退出登录，依赖 `account-core.js`。
- `account-profile-controller.js`：`account-settings` 的头像与昵称表单唯一所有者；负责头像预览/上传与资料保存，不与 `account-settings.js` 重复绑定提交事件。

所有账号页面模块应优先复用 `account-core.js`，不得各自复制 `request`、错误映射或密码策略。`account-core.js` 必须持续保留 `X-Requested-With: fetch`、同源 Cookie、同源登录回跳校验和与后端一致的密码策略。认证页为兼容既有排障入口，暂时继续提供只读 `window.XHAccount` 公共接口。

### MW4 工具站
- `mw4-camo-tracker.js`：当前 `/mastery` 权威脚本；
- `mw4-camo-tracker.css`：当前 `/mastery` 权威样式。

旧 `weaponList`、`progressSuggest`、`.camo-mode-preview-art`、`.camo-check` 补丁已退出当前 DOM，不得重新作为兼容逻辑引入。

### 攻略
- `guide.css` / `guide-render.js` / `guide-community.*`：攻略前台；
- `guide-community.js` 的 `XHGuideCommunity.mount(root)` 同时拥有旧攻略和 Next 详情的收藏、讨论、回复、举报请求；Next 的 `n22-guide-detail.js` 仅建立界面并显式挂载，不复制社区状态机。重复挂载同一 root 不重复绑定事件。
- `admin-guides-workspace.css`：攻略后台主结构。

## 4. 后台

默认后台现由 `admin-xingwei*.html`、`admin-xingwei.css` 和各工作台同名 JS/CSS 拥有；`admin-login.html` 使用新版登录样式。默认入口不加载旧 `app.css`、亮色主题或品牌修补层。

- `admin-xingwei-frontend-controls.html/css`：独立前台展示控制外壳；`fragments/frontend-control-workspace.html` 与恢复页共享表单，`admin-frontend-controls.js` 唯一处理配置读写及版本保护，只有恢复页加载旧品牌兼容层。
- `fragments/tool-station-editor.html`：工具资料共享编辑结构；新版以 `admin-xingwei-tools.css` 拥有样式，不再从旧 `admin.html` 或 `app.css` 获取组件。
- `admin-xingwei-audits.js`：员工审计与会员操作日志两种数据源；服务端分别执行数据范围与脱敏规则。
- `admin.js`、`admin-control-center-v2.js/css`、`admin-light-theme.css`、`admin-brand-layout-fixes.css` 仅服务明确的旧版恢复入口，不是新版布局或新功能的依赖。

新增旧管理接口必须更新 `docs/admin-migration-coverage.json` 并提供新版工作台闭环；覆盖契约同时扫描类前缀与方法级完整路径，不能只比较菜单名。

## 5. 已退休资产

### `ui-fixes.js`
状态：**已删除，不得恢复**。

原有职责已经迁移到 `site-shell.js`、`pages.js`、`mw4-camo-tracker.js` 等明确所有者。所有模板中的 `/assets/ui-fixes.js` 引用已清零，并由 `FrontendPatchDebtContractTest` 防止恢复。

### `account.js`
状态：**已删除，不得恢复**。

历史巨型账号脚本的运行时职责已经全部迁移：
- 登录 / 注册 / 重置 → `account-auth.js` + `account-core.js`；
- 个人中心 / 需求 / 订单 / 进度 → `account-center.js`；
- 攻略投稿 → `account-submit.js` + `account-core.js`；
- 设置页账号安全 / 邮箱 / 设备 / 回访方式 → `account-settings.js` + `account-core.js`；
- 设置页头像 / 昵称 → `account-profile-controller.js`。

`RetiredAccountMonolithContractTest` 强制保证历史文件不得恢复，且任何 HTML/JS 运行时资产不得重新引用 `/assets/account.js`。

## 6. 新代码准入规则

新增前端代码前必须回答：
1. 这是公共壳层、页面逻辑还是业务模块？
2. 已有哪个文件拥有该职责？
3. 是否在覆盖旧错误，而不是修复根因？
4. 是否会引入第二份数据源？
5. 是否需要 `setInterval` / 全局 `MutationObserver`？若需要，必须说明为什么事件驱动无法解决。
6. 能否通过契约测试或浏览器回归证明旧补丁可以删除？

如果无法明确归属，不应创建新的 `fix/refine/patch` 文件。
## 新版攻略与中文显示

- `admin-xingwei-guides.html/css`：新版攻略页面外壳与响应式布局，不加载旧后台亮色/品牌修补样式。
- `fragments/guide-workspace.html`：新旧攻略编辑区共享结构；`admin-guides.js` 继续唯一负责保存、预览、筛选及版本保护。
- `admin-xingwei-labels.js`：新版后台枚举的显式中文显示字典，不改写 API 值，不扫描/替换 DOM，不负责用户输入或权限判定。

- `scripts/generate-admin-search-index.py` 从新版模板与站点字段定义生成 `admin-xingwei-search-index.js`，只索引功能元数据，不包含业务记录；`--check` 检查生成物是否过期。主工作台 `admin-xingwei.js` 唯一负责权限过滤、关键词匹配和结果展示；静态表单沿用 URL 锚点，异步站点表单在自身 render 完成后定位，不使用轮询或 DOM 扫描补丁。

## Next 公共可达性

- `next/shell.js` 负责跳过导航和标签组通用键盘行为；各业务模块明确绑定自己的 tablist 并维护 tab/panel 的关联，不通过全局 DOM 修补观察器推断表单。
- `next/index.html` 拥有公共手机输入/触控/禁用状态与导航留白；控件名称、表单错误、业务重绘后的焦点和对话框归各表单模块负责。
- `next/n28-legal-single-source.js` 只安全展示正式中英文法律页面，并按与正式页一致的公开字段绑定主体/客服信息，不拥有第二份法律条款。

- `next/game-detail.js` 负责独立游戏资料及该游戏公开服务列表，复用 `/api/catalog`、`/api/services` 和公开站点配置；`FrontendNextGameController` 复用原公开游戏校验，`FrontendNextPreviewController` 唯一生成文档元信息与资源指纹。

- `next/shell.js` 唯一拥有正式/预览路径上下文和导航历史适配；各模块使用逻辑路由进行匹配，生成返回地址时使用同一命名空间，不改写全局 History API。
- `assets/guest-business.js` 唯一拥有游客正式报价确认、付款二维码及履约展示，暴露可销毁的 mount 供 Next N13 使用，原记录页继续自动挂载。离页停止轮询、迟到响应保护及失败重试由共享模块负责；前端不传金额。独立 `/guest/access/{token}` 仍由原安全确认表单交换 HttpOnly 凭证。
