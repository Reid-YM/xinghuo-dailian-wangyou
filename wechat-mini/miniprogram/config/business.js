/**
 * 星火迷彩 · 集中配置
 *
 * 品牌名、客服文案、审核策略开关都放这里，页面代码不写死文案，
 * 以后要改只改这一个文件，两个页面一起生效。
 */
const BRAND = {
  name: '星火迷彩追踪器',
  full: '星火网游 · SPARK TEAM'
}

// 与开发者工具上传的版本号保持一致，方便用户报问题时定位是哪一版
const VERSION = '1.0.22'

/*
 * 工信部 ICP 备案号：备案通过后填写，展示在「我的 → 关于本工具」。
 * 形如 '蜀ICP备2026000000号-2'；留空则不展示这一行（未填真实备案号前不要写占位符）。
 */
const ICP = '蜀ICP备2026047129号'

/* 攻略源内容已在网站后台整理；此过滤器保留为内容回归保护。 */
const CONTENT_FILTER = {
  enabled: true,

  // 标题 / 摘要 / 标签命中就直接不收录这篇攻略
  // compliance-ignore-start（这里是词表本身，不是给人看的文案）
  guideWords: ['代练', '代打', '代肝', '代做', '包上分', '代刷', '陪练'],
  // 正文段落命中就把这一段剔掉
  blockWords: ['代练', '代打', '代肝', '代做', '包上分', '代刷', '陪练',
    '报价', '下单', '购买', '支付', '价格', '多少钱', '微信号', '加微信'],
  // compliance-ignore-end

  // 一篇里被剔掉的段落超过这个比例，就整篇不展示（说明它主要是服务推广）
  maxDroppedRatio: 0.6
}

// 星火网站：小程序的数据源。
// 小程序启动时优先读这个接口，拿不到就退回本地快照（data/weapons.js）。
// 改域名只改这里 —— 但要记得在「小程序后台 → 开发设置 → 服务器域名」里
// 把 request 和 downloadFile 合法域名都加上这个域名，否则正式版请求会被拦。
const SITE = {
  base: 'https://wydlgf.cn',
  timeout: 8000,
  // 武器/迷彩图标开关。域名没配好之前可以先关掉，列表就不会出现裂图。
  showImages: true,

  // 图片从哪来：
  //   'local'  —— 用打包在小程序里的图（离线可用，不需要域名）
  //   'remote' —— 直接读官网地址（和你网站上看到的一样；需要把域名加进
  //               downloadFile 合法域名）
  // 如果手机上包内图显示不出来，把这里改成 'remote' 并配好域名即可。
  imageMode: 'local'
}

// 云开发环境 ID。留空 = 用当前小程序的默认环境（推荐，少一处要改的地方）。
const CLOUD_ENV = 'cloud1-d7gh84cnd49ae119a'

/* 客服入口统一使用工具咨询文案。 */
const SERVICE = {
  enabled: true,
  copy: {
    floatText: '客服',
    cardTitle: '需要帮助',
    cardDesc: '迷彩、活动、周常、DMZ，不想自己打、时间不够，或者卡在某个挑战上，都可以找客服，让我们来帮你。',
    buttonText: '联系客服',
    sessionPrefix: '工具咨询'
  }
}

/* ---------------------------------------------------------------------------
   一对一咨询：直接打开「微信客服」会话 —— 和你网站上那套客服是同一个。

   ⚠️ 需要填 corpId（企业微信的企业ID）才能启用：
      企业微信管理后台 → 我的企业 → 企业信息 → 企业ID，形如 ww1234567890abcdef
      填上之后，小程序里的客服按钮会变成「一对一咨询」，点一下直接跳进客服会话
      （用 wx.openCustomerServiceChat，不需要域名白名单，也不走「小程序客服消息」那套留言机制）

   corpId 留空时自动退回小程序自带的客服会话，功能不受影响。
   --------------------------------------------------------------------------- */
const KF = {
  corpId: 'wwa474532de156197a',
  // 白班 / 夜班，取自网站 /api/site-config 的 contactChannels
  channels: [
    { label: '白班客服', url: 'https://work.weixin.qq.com/kfid/kfc392f40b584c2d7a1' },
    { label: '夜班客服', url: 'https://work.weixin.qq.com/kfid/kfc03e31c506368bb68' }
  ],
  buttonText: '一对一咨询'
}

// 分享文案：这是免费的推广位，好友/朋友圈点进来直接落在追踪器上
const SHARE = {
  title: '星火迷彩追踪器 · MW4 全武器进度一键勾选',
  timelineTitle: 'MW4 全武器迷彩进度追踪，免登录就能用'
}

// 分享码最多携带多少条进度（太长在聊天框里粘贴体验差）
const SHARE_CODE_LIMIT = 400

// 当前应该用哪一套客服文案
function serviceCopy() {
  return SERVICE.copy
}

// 客服会话里带过去的上下文，让客服一眼知道用户卡在哪把武器上
function sessionFrom(context) {
  const copy = serviceCopy()
  const parts = [copy.sessionPrefix]
  if (context && context.weapon) parts.push('武器:' + context.weapon)
  if (context && context.mode) parts.push('模式:' + context.mode)
  if (context && typeof context.pct === 'number') parts.push('进度:' + context.pct + '%')
  if (context && context.note) parts.push(context.note)
  return parts.join('|')
}

module.exports = {
  BRAND,
  VERSION,
  ICP,
  SITE,
  KF,
  CONTENT_FILTER,
  CLOUD_ENV,
  SERVICE,
  SHARE,
  SHARE_CODE_LIMIT,
  serviceCopy,
  sessionFrom
}
