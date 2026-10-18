# N28 Frontend Next 体验一致性收口

## 目标

N23–N27 已把剩余主要业务与公开路径纳入 Frontend Next。N28 不新增业务功能，而是统一多阶段叠加后产生的交互与可访问性表现，为真实生产验收做准备。

新增全局运行时：

`/next/n28-experience-consistency.js`

它只作用于 `/next/**`，不影响正式 Legacy 页面。

## 统一内容

### 键盘与主内容导航

- 增加“跳到主要内容”链接；
- 自动识别当前 active page 作为主要内容目标；
- 统一 `:focus-visible` 焦点轮廓；
- 保留可编程聚焦的主内容 `tabindex=-1`。

### 加载、禁用与状态区域

对 N23–N26 以及账号认证中的已知状态区域统一：

- `aria-live=polite`；
- 根据“正在 / 读取中 / 提交中 / 加载中 / 查询中 / 同步中”设置 `aria-busy`；
- 加载态使用统一轻量 spinner；
- disabled 按钮统一视觉反馈并同步 `aria-disabled`。

N28 不更改表单默认提交语义：只有不属于任何 form 的无 type 按钮才会补 `type=button`。

### 图片与外部链接

- 动态图片缺少 alt 时补空 alt，避免读屏器朗读文件名；
- lazy 图片补 `decoding=async`；
- `target=_blank` 链接统一补 `noopener noreferrer`。

### 移动端

在窄屏下：

- 按钮、选择器等主要触控目标最低 44px；
- 文本输入、下拉框、textarea 至少 16px 字号，避免移动浏览器聚焦时意外缩放；
- 路由状态提示位置避开底部区域。

### 系统辅助偏好

- `prefers-reduced-motion: reduce` 时关闭非必要动画与滚动动画；
- `forced-colors` 模式保留清晰焦点和跳过导航边界。

## 路由动态更新

N28 监听：

- `xh:pagechange`
- `popstate`
- 动态 DOM 变化

因此 N10–N27 动态创建的页面、按钮、图片和状态区也会得到一致性处理，而无需在每个阶段复制同样逻辑。

## 安全边界

N28 是纯表现/可访问性层：

- 不 fetch；
- 不读取或写入 localStorage / sessionStorage；
- 不发 POST / PUT / PATCH / DELETE；
- 不修改 Session、账号数据、订单、支付、退款、售后；
- 不执行任意 innerHTML；
- 不修改 Legacy 页面。

## 后续

N29 需要把 N23–N28 合并并部署到生产 `/next/**` 预览后，执行真实线上全链路验收与缺陷清零。N30 再准备正式切换治理、灰度计划和回滚演练；正式流量是否切换仍需单独明确授权。