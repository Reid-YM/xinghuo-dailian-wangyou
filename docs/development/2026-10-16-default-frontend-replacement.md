# 正式前端入口替换与无效修复核对

## 实际入口

FrontendMigrationConfig 在原控制器和鉴权拦截器成功完成之后，将首页、服务大厅、询价和账号核心页面的 view forward 到 Next。不抢占 API，不绕过 RequireAccount，不改变旧控制器业务数据逻辑。默认启用新版视图；`?legacy=1` 可按页回退，app.frontend-next.default-enabled=false 可整体回退。发布前仍是代码状态，不声称线上已切换。

旧 /next 入口继续可用。共享 shell 按路径初始化，兼容原 #home/#services/#quote/#account；导航生成实际 URL，刷新、popstate 和 hashchange 不再误显示首页。注册/找回从原路径选择正确面板。回退链接明确带 legacy=1，避免指向新版自身。

## 实际配置与目录

Next 首页读取真实站点标题、品牌、公告、客服、图片和页脚；复用现有 global-ui-controls 与 frontend-controls-runtime，导航显示/标签及首页/主题开关继续消费原配置。路由变化会重应用首页配置，不能只在初次 load 时生效。

服务目录空列表显示为空；首次失败显示错误与重试，刷新失败保留上次真实目录。移除模拟商品、价格和账号记录，固定价读取 priceFixed。详情链接落到真实 /service/{slug}；按实际游戏/平台关系、类别、关键词和顺序筛选，URL 恢复。报价停用和目录不可用时不提交演示需求，服务端原校验保持。

当前 HTML 服务端输出转义后的品牌/SEO 和 canonical；账号页面 noindex。客户端路由同步标题、canonical 和账号索引状态。Next 与共享控制脚本使用真实文件摘要版本，删除旧 shell 内联演示脚本；不放宽 CSP。

## 验证

- 原生路由测试：GET view forwarding、显式回退、整体开关和未登录账号重定向。
- SSR metadata：配置转义、canonical 和私有账号 noindex。
- 资源摘要：直接计算实际脚本内容比对。
- Chromium：加载实际服务端 HTML 和同源脚本，执行严格 self CSP，直接路径/刷新/历史、真实配置、固定价与详情。
- WebKit/手机：目录失败重试无模拟记录、注册路径初始化和视口约束。

Visual thesis: 服务、需求与真实业务进度使用同一个可恢复导航；保留现有品牌配色，配置是实际内容来源。
Signature device: 询价到服务交付步骤及可追溯业务入口。
Density / motion: 服务目录紧凑、首页适中；减少动效设置继续生效。
Intentional tradeoff: 保留明确的兼容 URL，不删除旧控制器以保障可回退；辅助账号功能和后台模块继续按完整迁移清单补齐，不提前声明整体完成。

无新数据库迁移，无生产配置写入，无真实支付或目录写入操作。当前仍不部署，整体完成后统一发布。
