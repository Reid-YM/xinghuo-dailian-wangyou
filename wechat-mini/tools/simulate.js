/**
 * 小程序逻辑模拟器：在没有微信开发者工具的环境里跑一遍页面逻辑。
 * 只验证 JS 层（数据流、统计、勾选、同步降级、接口兜底），不渲染 WXML。
 *
 * 用法：node tools/simulate.js
 */
const path = require('path')

const ROOT = path.join(__dirname, '..', 'miniprogram')

/* ---------------------------------------------------------- wx mock */

const store = new Map()
const calls = { toast: [], modal: [], request: 0, nav: [] }
let requestMode = 'off' // off = 没有 wx.request（模拟域名没配/断网）, ok = 正常返回

global.wx = {
  getStorageSync(key) {
    return store.has(key) ? store.get(key) : ''
  },
  setStorageSync(key, value) {
    store.set(key, value)
  },
  removeStorageSync(key) {
    store.delete(key)
  },
  showToast(opts) {
    calls.toast.push(opts && opts.title)
  },
  showLoading() {},
  hideLoading() {},
  showModal(opts) {
    const confirm = !!global.__modalConfirm
    global.__modalConfirm = false
    if (opts && typeof opts.success === 'function') {
      opts.success({ confirm: confirm, content: 'MULTIPLAYER::M4::1|MULTIPLAYER::M4::2' })
    }
  },
  stopPullDownRefresh() {},
  openCustomerServiceChat(opts) {
    global.__kfOpened = { url: opts.extInfo && opts.extInfo.url, corpId: opts.corpId }
    if (opts.success) opts.success({ errMsg: 'openCustomerServiceChat:ok' })
  },
  getImageInfo(opts) {
    // 模拟手机上能读到包内图片
    opts.success({ width: 512, height: 512 });
  },
  setNavigationBarTitle() {},
  navigateTo(opts) {
    calls.nav.push(opts && opts.url)
  },
  switchTab(opts) {
    calls.nav.push(opts && opts.url)
  },
  redirectTo(opts) {
    calls.nav.push(opts && opts.url)
  },
  previewImage() {},
  scanCode() {},
  setClipboardData(opts) {
    if (opts && typeof opts.success === 'function') opts.success()
  }
}

/* ---------------------------------------------------------- helpers */

let failures = 0

function check(label, actual, expected) {
  const ok = JSON.stringify(actual) === JSON.stringify(expected)
  if (!ok) {
    failures++
    console.log('  ✗ ' + label + '  期望 ' + JSON.stringify(expected) + '，实际 ' + JSON.stringify(actual))
  } else {
    console.log('  ✓ ' + label)
  }
}

function assert(label, cond, detail) {
  if (!cond) {
    failures++
    console.log('  ✗ ' + label + (detail ? '  ' + detail : ''))
  } else {
    console.log('  ✓ ' + label)
  }
}

function instantiate(def) {
  const inst = Object.create(def)
  inst.data = JSON.parse(JSON.stringify(def.data))
  inst.setData = function (patch) {
    Object.assign(inst.data, patch)
  }
  return inst
}

function loadPage(rel) {
  delete global.__page
  require(path.join(ROOT, rel))
  return global.__page
}

global.App = function (def) {
  global.__app = def
}
global.Page = function (def) {
  global.__page = def
}

;(async function () {
  /* ---------------------------------------------------------- 1. app.js */

  console.log('\n[1] app.js 启动（没云开发、没配域名 → 应该优雅降级）')
  require(path.join(ROOT, 'app.js'))
  assert('App 注册成功', !!global.__app)
  global.__app.onLaunch()
  // 页面里用 getApp() 取全局数据（跨 tab 传参）
  global.__appInstance = { globalData: global.__app.globalData }
  global.getApp = function () { return global.__appInstance }
  const cloud = require(path.join(ROOT, 'utils', 'cloud.js'))
  const st = cloud.getState()
  check('云能力标记为不可用', st.available, false)
  assert('给出了人话提示', /云开发|本机/.test(st.hint), '实际 hint=' + st.hint)
  // 自动登录用的是静默模式：失败不能写状态提示（否则新用户一进来就看到报错）
  const hintBefore = cloud.getState().hint
  let silentErr = null
  await cloud.login({ silent: true }).catch(function (e) { silentErr = e })
  assert('云开发不可用时静默登录会失败', !!silentErr, '')
  check('静默失败不写状态提示', cloud.getState().hint, hintBefore)
  check('失败后没有变成已登录', cloud.getState().loggedIn, false)

  /* ---------------------------------------------------------- 2. 数据层 */

  console.log('\n[2] 数据层（官网接口不通 → 必须退回本地快照，不能白屏）')
  const catalog = require(path.join(ROOT, 'utils', 'catalog.js'))
  const builtinPayload = require(path.join(ROOT, 'data', 'weapons.js'))

  const cat1 = await catalog.load()
  check('数据来源标记为快照', cat1.source, 'builtin')
  check('武器数', cat1.weapons.length, 22)
  check('职业数', cat1.classes.length, 9)
  check('职业是中文', cat1.classes[0], '突击步枪')
  check('枪械模式数（不含 Event）', cat1.modes.length, 3)
  check('每模式三档', cat1.modes[0].tiers.length, 3)
  check('进度项总数', cat1.total, 202)
  check('第一把武器中文名', cat1.weapons[0].nameZh, '汉86')
  check('第一把武器 code', cat1.weapons[0].code, 'HAN86')
  check('迷彩中文名', cat1.modes[0].tiers[0].nameZh, '水银漂移')
  assert('挑战要求是中文', /不死|击杀/.test(cat1.modes[0].tiers[0].requirement), cat1.modes[0].tiers[0].requirement)
  assert('武器图解析为包内路径', /^\/images\/weapon\//.test(cat1.weapons[0].image), cat1.weapons[0].image)
  assert('迷彩图解析为包内路径', /^\/images\/camo\//.test(cat1.modes[0].tiers[0].image), cat1.modes[0].tiers[0].image)
  assert('Event 迷彩存在', !!cat1.eventMode && cat1.eventMode.items.length === 4)
  check('Event 用官网的名字', cat1.eventMode.items[0].nameZh, '火山碎屑')

  /* ---------------------------------------------------------- 3. 接口通了 */

  console.log('\n[3] 官网接口通了（模拟 wx.request 返回真实结构）')
  requestMode = 'ok'
  global.wx.request = function (opts) {
    calls.request++
    opts.success({ statusCode: 200, data: builtinPayload })
  }
  catalog.resetMemory()
  const cat2 = await catalog.load({ force: true })
  check('数据来源标记为官网', cat2.source, 'remote')
  check('接口被调用次数', calls.request, 1)
  check('武器数一致', cat2.weapons.length, cat1.weapons.length)
  check('进度项总数一致', cat2.total, cat1.total)
  check('camoTracks 版本带过来了', cat2.version, cat1.version)

  // 缓存命中：再 load 一次不应该再打接口
  const savedCatalog = wx.getStorageSync(catalog.CACHE_KEY)
  savedCatalog.fetchedAt = Date.now() - 3600000
  wx.setStorageSync(catalog.CACHE_KEY, savedCatalog)
  catalog.resetMemory()
  const cat3 = await catalog.load()
  check('走本机缓存', cat3.source, 'cache')
  check('缓存保留实际拉取时间', cat3.fetchedAt, savedCatalog.fetchedAt)
  check('接口没被重复调用', calls.request, 1)
  global.wx.request = function (opts) {
    calls.request++
    opts.fail({ errMsg: 'offline' })
  }
  const offlineCat = await catalog.load({ force: true })
  check('手动刷新断网仍使用已有缓存', offlineCat.source, 'cache')
  global.wx.request = function (opts) {
    calls.request++
    opts.success({ statusCode: 200, data: builtinPayload })
  }

  /* ---------------------------------------------------------- 4. 首页 */

  console.log('\n[4] 首页 pages/index/index')
  store.clear()
  catalog.resetMemory()
  const indexDef = loadPage('pages/index/index.js')
  const index = instantiate(indexDef)
  index.onLoad()
  await index.ready

  check('已退出 loading', index.data.loading, false)
  check('武器总数', index.data.weaponCount, 22)
  check('总项目数', index.data.stats.total, 202)
  check('初始完成 0', index.data.stats.count, 0)
  check('模式切换项是中文简称', index.data.modes[0].short, '多人')
  check('第一个分组是中文职业', index.data.groups[0].cls, '突击步枪')
  check('突击步枪 6 把', index.data.groups[0].total, 6)
  assert('显示了数据来源', /官网|缓存|快照/.test(index.data.sourceText), index.data.sourceText)

  // 勾选 M4 三档（新键格式）
  const keys = ['MULTIPLAYER::M4::1', 'MULTIPLAYER::M4::2', 'MULTIPLAYER::M4::3']
  keys.forEach(function (k) {
    index.onToggleTier({ currentTarget: { dataset: { key: k } } })
  })
  const m4 = index.data.groups[0].weapons.filter(function (w) { return w.code === 'M4' })[0]
  check('M4 完成三档', m4.count, 3)
  check('M4 标记完成', m4.complete, true)
  check('总完成数 3', index.data.stats.count, 3)
  check('进度百分比', index.data.stats.pct, Math.round((3 / 202) * 100))
  check('多人模式统计', index.data.modeStats[0].count, 3)
  check('本机存了 3 条', JSON.parse(JSON.stringify(store.get('mw4_camo_v1'))).length, 3)
  check('档位按钮显示 1/2/3', m4.tiers.map(function (t) { return t.tier }).join(','), '1,2,3')
  check('档位显示中文迷彩名', m4.tiers[0].name, '水银漂移')

  // 三档迷彩概览的计数
  check('第一档迷彩已达成 1 把', index.data.tierCounts[0].count, 1)

  // 取消 / 收藏
  index.onToggleTier({ currentTarget: { dataset: { key: 'MULTIPLAYER::M4::3' } } })
  check('取消后剩 2', index.data.stats.count, 2)
  index.onToggleTier({ currentTarget: { dataset: { key: 'MULTIPLAYER::M4::3' } } })

  index.onToggleFav({ currentTarget: { dataset: { weapon: 'AXION' } } })
  const axion = index.data.groups[0].weapons.filter(function (w) { return w.code === 'AXION' })[0]
  check('Axion 已收藏', axion.fav, true)

  // 中文搜索
  index.onSearch({ detail: { value: '汉86' } })
  await new Promise(function (r) { setTimeout(r, 200) })
  check('中文搜索「汉86」命中 1 把', index.data.groups.length === 1 ? index.data.groups[0].total : -1, 1)
  index.onSearch({ detail: { value: 'kastov' } })
  await new Promise(function (r) { setTimeout(r, 200) })
  check('英文搜索 kastov 命中 1 把', index.data.groups.length === 1 ? index.data.groups[0].total : -1, 1)
  index.onSearch({ detail: { value: '' } })

  // 筛选
  index.onStatus({ currentTarget: { dataset: { v: 'fav' } } })
  check('收藏筛选剩 1 把', index.data.groups[0].total, 1)
  check('收藏筛选不显示活动项目', index.data.eventList.length, 0)
  index.onStatus({ currentTarget: { dataset: { v: 'done' } } })
  check('已完成筛选剩 1 把（M4）', index.data.groups[0].total, 1)
  index.onStatus({ currentTarget: { dataset: { v: 'all' } } })
  index.onClassChange({ detail: { value: 2 } })
  check('职业筛选只显示一个分组', index.data.groups.length, 1)
  index.onClassChange({ detail: { value: 0 } })
  index.onSearch({ detail: { value: '不存在的武器' } })
  await new Promise(function (r) { setTimeout(r, 200) })
  check('空结果数量为零', index.data.visibleCount, 0)
  check('空结果提示可清除筛选', index.data.hasActiveFilters, true)
  index.onResetFilters()
  check('清除后恢复全部项目', index.data.visibleCount, 26)

  // 模式切换
  index.onMode({ currentTarget: { dataset: { key: 'WARZONE' } } })
  check('切到战区', index.data.activeMode, 'WARZONE')
  check('战区里 M4 还没完成', index.data.modeStats[1].count, 0)
  assert('战区迷彩名是中文', index.data.tierCounts[0].name === '视差', index.data.tierCounts[0].name)
  index.onMode({ currentTarget: { dataset: { key: 'MULTIPLAYER' } } })

  // Event 迷彩
  index.onToggleTier({ currentTarget: { dataset: { key: 'PYROCLAST::PYROCLAST_VERGE' } } })
  const ev = index.data.eventList.filter(function (e) { return e.code === 'PYROCLAST_VERGE' })[0]
  check('Event 迷彩已勾选', ev.on, true)

  // 展开/收起/说明
  index.onToggleExpand({ currentTarget: { dataset: { weapon: 'M4' } } })
  assert('展开状态写入', index.data.expanded.M4 === true)
  index.onToggleClass({ currentTarget: { dataset: { cls: '手枪' } } })
  assert('收起状态写入', index.data.collapsed['手枪'] === true)
  index.onToggleGuide()
  check('说明面板展开', index.data.guideOpen, true)
  check('客服文案（安全模式）', index.data.copy.buttonText, '联系客服')
  assert('客服上下文带模式名', /工具咨询|模式:/.test(index.data.sessionFrom), index.data.sessionFrom)

  // 分享
  const share = index.onShareAppMessage()
  assert('分享标题可用', /MW4/.test(share.title), share.title)
  check('分享落地在追踪器', share.path, '/pages/index/index')
  assert('朋友圈分享可用', !!index.onShareTimeline().title)

  /* ---------------------------------------------------------- 5. 我的页 */

  console.log('\n[5] 我的页 pages/me/me')
  store.clear()
  const meDef = loadPage('pages/me/me.js')
  const me = instantiate(meDef)
  me.onLoad()
  await me.ready
  check('初始进度 0', me.data.summary.count, 0)
  check('进度总数', me.data.summary.total, 202)
  check('Event 总数带过来了', me.data.summary.eventTotal, 4)
  check('武器数', me.data.weaponCount, 22)
  assert('显示数据来源', !!me.data.dataSource, me.data.dataSource)
  me.onShow()
  assert('未登录有提示', !!me.data.cloud.hint || !!me.data.copy.cardTitle)
  require(path.join(ROOT, 'utils', 'storage.js')).saveDone({ 'MULTIPLAYER::M4::1': true })
  me.refresh()
  check('我的页显示已完成进度', me.data.summary.count, 1)
  global.__modalConfirm = false
  me.onClear()
  check('我的页取消清空后保留进度', me.data.summary.count, 1)
  global.__modalConfirm = true
  me.onClear()
  check('我的页确认后清空进度', me.data.summary.count, 0)

  /* ---------------------------------------------------------- 6. 进度键校验 */

  console.log('\n[6] 分享码校验')
  const progressUtil = require(path.join(ROOT, 'utils', 'progress.js'))
  check('新格式 3 段', progressUtil.isValidKey('MULTIPLAYER::M4::1'), true)
  check('新格式 Event 2 段', progressUtil.isValidKey('PYROCLAST::PYROCLAST_VERGE'), true)
  check('乱码拒绝', progressUtil.isValidKey('乱码::x'), false)
  check('缺档位拒绝', progressUtil.isValidKey('MULTIPLAYER::M4'), false)
  check('模式名不对拒绝', progressUtil.isValidKey('ZZZ::M4::1'), false)
  check('Event 模式名不对拒绝', progressUtil.isValidKey('MULTIPLAYER::FOO'), false)
  check('旧格式拒绝', progressUtil.isValidKey('mp::M4::0'), false)
  check('档位从 0 开始拒绝', progressUtil.isValidKey('MULTIPLAYER::M4::0'), false)

  /* ---------------------------------------------------------- 7. 一致性 */

  console.log('\n[7] 一致性')
  const summary = require(path.join(ROOT, 'utils', 'stats.js')).computeStats(cat1, {})
  check('空进度时 total', summary.total, 202)
  check('空进度时 count', summary.count, 0)
  check('三个模式', summary.modes.map(function (m) { return m.code }).join(','), 'MULTIPLAYER,WARZONE,DMZ')
  const allDone = {}
  cat1.weapons.forEach(function (w) {
    cat1.modes.forEach(function (m) {
      m.tiers.forEach(function (t) {
        allDone[progressUtil.tierKey(m.code, w.code, t.tier)] = true
      })
    })
  })
  cat1.eventMode.items.forEach(function (it) {
    allDone[progressUtil.eventKey(cat1.eventMode.code, it.code)] = true
  })
  const full = require(path.join(ROOT, 'utils', 'stats.js')).computeStats(cat1, allDone)
  check('全勾满时 100%', full.pct, 100)
  check('全勾满时数量', full.count, 202)

  /* ---------------------------------------------------------- 8. 业务进度查询 */

  console.log('\n[8] 业务进度查询页 pages/record/record')
  store.clear()
  const RECORD_OK = {
    businessNo: 'XH20261012001',
    stage: 'ORDER',
    status: 'IN_PROGRESS',
    title: 'CODMW4 · 迷彩全套收尾',
    expectedTime: '约 2 小时',
    progressPercent: 60,
    progressText: 'MP 三档已收尾，正在补战区和 DMZ',
    createdAt: '2026-10-12T02:00:00Z',
    updatedAt: '2026-10-12T04:00:00Z',
    hasProgressImage: true,
    customerNote: '账号使用中，请勿登录'
  }
  global.wx.request = function (opts) {
    calls.request++
    const no = decodeURIComponent(String(opts.url).split('/').pop())
    if (no === 'XH20261012001') return opts.success({ statusCode: 200, data: RECORD_OK })
    if (no === 'MAINT') return opts.success({ statusCode: 400, data: { message: '进度查询正在维护，请联系客服查询' } })
    return opts.success({ statusCode: 404, data: { message: 'not found' } })
  }

  const recDef = loadPage('pages/record/record.js')
  const rec = instantiate(recDef)
  rec.onLoad()
  check('初始没有结果', rec.data.result, null)
  assert('客服文案就位', !!rec.data.copy.buttonText, '')

  rec.setData({ no: 'xh20261012001' })
  await rec.query()
  check('查到了', !!rec.data.result, true)
  check('单号自动大写规范', rec.data.result.businessNo, 'XH20261012001')
  check('状态码转中文', rec.data.result.statusText, '进行中')
  check('阶段转中文', rec.data.result.stageText, '服务订单')
  check('进度百分比', rec.data.result.progressPercent, 60)
  check('百分比标记', rec.data.result.hasPercent, true)
  assert('进度图是绝对地址', /^https:\/\/.+\/api\/records\/.+\/image$/.test(rec.data.result.imageUrl), rec.data.result.imageUrl)
  assert('下单时间已格式化', /^\d{4}-\d{2}-\d{2} \d{2}:\d{2}$/.test(rec.data.result.createdText), rec.data.result.createdText)
  assert('客服上下文带单号', /单号:XH20261012001/.test(rec.data.sessionFrom), rec.data.sessionFrom)
  check('写入查询历史', rec.data.history[0], 'XH20261012001')

  rec.setData({ no: 'XH00000000000' })
  await rec.query()
  check('未知单号不返回结果', rec.data.result, null)
  assert('未知单号提示带单号', rec.data.error.indexOf('XH00000000000') !== -1, rec.data.error)

  rec.setData({ no: 'maint' })
  await rec.query()
  check('维护中显示服务端文案', rec.data.error, '进度查询正在维护，请联系客服查询')

  rec.setData({ no: '   ' })
  await rec.query()
  assert('空输入给提示', String(calls.toast[calls.toast.length - 1]).indexOf('业务单号') !== -1, String(calls.toast[calls.toast.length - 1]))

  ;['A1', 'A2', 'A3', 'A4', 'A5', 'A6', 'A3'].forEach(function (x) { rec.pushHistory(x) })
  check('历史上限 5 条', rec.data.history.length, 5)
  check('最近查的排最前', rec.data.history[0], 'A3')
  check('历史不重复', rec.data.history.filter(function (x) { return x === 'A3' }).length, 1)
  const pendingQueries = []
  global.wx.request = function (opts) { pendingQueries.push(opts) }
  rec.setData({ no: 'SAME' })
  const same = rec.query()
  rec.query()
  check('同一单号加载中不重复请求', pendingQueries.length, 1)
  pendingQueries.shift().success({ statusCode: 404, data: { message: 'not found' } })
  await same
  rec.setData({ no: 'SLOW' })
  const slow = rec.query()
  rec.setData({ no: 'FAST' })
  const fast = rec.query()
  pendingQueries[1].success({ statusCode: 200, data: Object.assign({}, RECORD_OK, { businessNo: 'FAST' }) })
  await fast
  pendingQueries[0].success({ statusCode: 404, data: { message: 'not found' } })
  await slow
  check('较慢的旧查询不会覆盖新结果', rec.data.result.businessNo, 'FAST')

  /* ---------------------------------------------------------- 9. tabBar */

  console.log('\n[9] tabBar / 页面注册')
  const appJson = require(path.join(ROOT, 'app.json'))
  check('注册了 5 个页面', appJson.pages.length, 5)
  check('查单页已注册', appJson.pages.indexOf('pages/record/record') !== -1, true)
  check('攻略页已注册', appJson.pages.indexOf('pages/guides/guides') !== -1, true)
  check('tabBar 四项', appJson.tabBar.list.map(function (x) { return x.text }).join('/'), '迷彩进度/攻略/查单/我的')
  check('图鉴页已下线', appJson.pages.indexOf('pages/camos/camos') === -1, true)

  /* ---------------------------------------------------------- 10. 攻略模块 */

  console.log('\n[10] 攻略模块（markdown 解析 + 数据层 + 两个页面）')

  const markdown = require(path.join(ROOT, 'utils', 'markdown.js'))
  const mdBlocks = markdown.parse([
    '# 一级标题',
    '',
    '第一行普通文字',
    '第二行接上一段',
    '',
    '- 列表项 A',
    '- 列表项 **加粗** 内容',
    '',
    '1. 有序一',
    '2. 有序二',
    '',
    '> 引用一句话',
    '',
    '---',
    '',
    '带 `code` 和 [链接文字](https://example.com/outside) 的段落',
    '',
    '![图](https://example.com/a.png)'
  ].join('\r\n'))

  check('解析出 7 个块', mdBlocks.length, 7)
  check('标题块', mdBlocks[0].type, 'heading')
  check('标题级别', mdBlocks[0].level, 1)
  check('连续两行合并成一段', mdBlocks[1].runs.map(function (r) { return r.text }).join(''), '第一行普通文字 第二行接上一段')
  check('无序列表', mdBlocks[2].type + '/' + mdBlocks[2].ordered, 'list/false')
  check('无序列表两条', mdBlocks[2].items.length, 2)
  check('列表里的粗体 run', mdBlocks[2].items[1].some(function (r) { return r.bold === true }), true)
  check('有序列表', mdBlocks[3].type + '/' + mdBlocks[3].ordered, 'list/true')
  check('引用块', mdBlocks[4].type, 'quote')
  check('分隔线', mdBlocks[5].type, 'divider')
  assert('行内代码保留', mdBlocks[6].runs.some(function (r) { return r.code === true && r.text === 'code' }))
  const lastText = mdBlocks[6].runs.map(function (r) { return r.text }).join('')
  assert('链接只留文字、URL 被丢掉', lastText.indexOf('链接文字') !== -1 && lastText.indexOf('example.com') === -1, lastText)
  assert('行内图片被丢弃', lastText.indexOf('![') === -1 && lastText.indexOf('.png') === -1, lastText)

  store.clear()
  const GUIDE_LIST = [
    { slug: 'a', title: 'A 迷彩攻略', subtitle: 'x', summary: '关于迷彩', coverImage: '/assets/1.jpg', difficulty: 'BEGINNER', estimatedMinutes: 5, tags: ['迷彩', 'a', 'b', 'c', 'd'], authorName: '星火攻略组', sourceName: 'UPBLAZED', sourceUrl: 'https://upblazed.com/x', featured: true, updatedAt: '2026-10-01T00:00:00Z' },
    { slug: 'b', title: 'B 设置攻略', subtitle: '', summary: '关于设置', coverImage: '', difficulty: 'ADVANCED', estimatedMinutes: 9, tags: [], authorName: '', sourceName: '', featured: false, updatedAt: '' },
    { slug: 'c', title: '找人代肝划算吗', subtitle: '', summary: '关于代肝', coverImage: '', difficulty: 'BEGINNER', estimatedMinutes: 6, tags: ['代肝'], authorName: '', sourceName: '', featured: false, updatedAt: '' }
  ]
  const GUIDE_DETAIL = {
    guide: { slug: 'a', title: 'A 迷彩攻略', subtitle: '副标题', coverImage: '/assets/1.jpg', difficulty: 'BEGINNER', estimatedMinutes: 5, authorName: '星火攻略组', sourceName: 'UPBLAZED', sourceUrl: 'https://upblazed.com/x', canonicalUrl: 'https://wydlgf.cn/x', tags: ['迷彩'], updatedAt: '2026-10-01T00:00:00Z' },
    sections: [{ heading: '正文', bodyMarkdown: '# 小标题\n\n段落内容\n\n- 要点一\n\n站内提供代练服务，按剩余进度报价', imageUrl: '/assets/2.jpg', imageAlt: '图' }],
    products: [{ slug: 'camo-task', name: 'MW4 全迷彩代练', priceMin: 80, durationText: '按进度' }],
    faqs: [{ question: '多少钱', answer: '问客服' }, { question: '什么时候更新', answer: '官方公布后' }],
    relatedGuides: [{ slug: 'b', title: 'B 设置攻略', coverImage: '/assets/3.jpg' }, { slug: 'c', title: '找人代肝划算吗', coverImage: '' }]
  }
  const GUIDE_RISKY = {
    guide: { slug: 'risky', title: '找人代肝划算吗', subtitle: '', summary: '关于代肝', coverImage: '', difficulty: 'BEGINNER', estimatedMinutes: 6, authorName: '', sourceName: '', tags: [], updatedAt: '' },
    sections: [{ heading: '为什么要代肝', bodyMarkdown: '正文内容' }],
    faqs: [],
    relatedGuides: []
  }
  global.wx.request = function (opts) {
    calls.request++
    const url = String(opts.url)
    if (url.indexOf('/api/guides/risky') !== -1) return opts.success({ statusCode: 200, data: GUIDE_RISKY })
    if (url.indexOf('/api/guides/') !== -1) return opts.success({ statusCode: 200, data: GUIDE_DETAIL })
    if (url.indexOf('/api/guides') !== -1) return opts.success({ statusCode: 200, data: GUIDE_LIST })
    return opts.success({ statusCode: 404, data: {} })
  }

  const guidesMod = require(path.join(ROOT, 'utils', 'guides.js'))
  let gFiltered = 0
  const listRes = await guidesMod.loadList({ force: true })
  gFiltered = listRes.filteredOut
  check('列表条数（含服务词的被过滤掉）', listRes.items.length, 2)
  check('难度转中文（入门）', listRes.items[0].difficulty, '入门')
  check('难度转中文（高级）', listRes.items[1].difficulty, '高级')
  check('封面转绝对地址', /^https:\/\/.+\.jpg$/.test(listRes.items[0].cover), true)
  check('列表标签最多 3 个', listRes.items[0].tags.length, 3)
  assert('列表项不带站外链接字段', listRes.items[0].sourceUrl === undefined)
  check('本地搜索命中', guidesMod.filterList(listRes.items, '设置').length, 1)
  check('本地搜索不命中', guidesMod.filterList(listRes.items, '不存在的东西').length, 0)

  const detail = await guidesMod.loadDetail('a', { force: true })
  check('详情标题', detail.title, 'A 迷彩攻略')
  check('正文解析成 3 个块', detail.sections[0].blocks.length, 3)
  assert('section 图是绝对地址', /^https:\/\//.test(detail.sections[0].image), detail.sections[0].image)
  check('FAQ 过滤掉含服务字眼的，剩 1 条', detail.faqs.length, 1)
  check('含服务字眼的段落被剔除', detail.droppedBlocks, 2)
  check('剔除比例不高 → 仍展示', detail.hidden, false)
  check('相关攻略滤掉含服务词的那条，剩 1 条', detail.related.length, 1)
  check('列表页报告被过滤的篇数', gFiltered, 1)
  assert('products 被丢掉（合规关键）', detail.products === undefined && JSON.stringify(detail).indexOf('代练') === -1, JSON.stringify(detail).slice(0, 100))
  assert('站外链接字段被丢掉', JSON.stringify(detail).indexOf('sourceUrl') === -1 && JSON.stringify(detail).indexOf('canonicalUrl') === -1)
  global.wx.request = function (opts) { opts.fail({ errMsg: 'offline' }) }
  const offlineList = await guidesMod.loadList({ force: true })
  check('攻略强制刷新断网仍显示缓存', offlineList.items.length, 2)
  check('攻略缓存保留过滤数量', offlineList.filteredOut, 1)
  const offlineDetail = await guidesMod.loadDetail('a', { force: true })
  check('攻略详情断网仍显示缓存', offlineDetail.title, 'A 迷彩攻略')
  global.wx.request = function (opts) {
    calls.request++
    const url = String(opts.url)
    if (url.indexOf('/api/guides/risky') !== -1) return opts.success({ statusCode: 200, data: GUIDE_RISKY })
    if (url.indexOf('/api/guides/') !== -1) return opts.success({ statusCode: 200, data: GUIDE_DETAIL })
    if (url.indexOf('/api/guides') !== -1) return opts.success({ statusCode: 200, data: GUIDE_LIST })
    return opts.success({ statusCode: 404, data: {} })
  }

  const gListDef = loadPage('pages/guides/guides.js')
  const gList = instantiate(gListDef)
  gList.onLoad()
  await gList.ready
  check('列表页加载完成', gList.data.loading, false)
  check('列表页展示 2 条', gList.data.shown.length, 2)
  gList.onSearch({ detail: { value: '设置' } })
  check('列表页搜索过滤', gList.data.shown.length, 1)
  gList.onSearch({ detail: { value: '' } })
  gList.onTap({ currentTarget: { dataset: { slug: 'a' } } })
  assert('点击跳详情', /pages\/guides\/detail\?slug=a/.test(String(calls.nav[calls.nav.length - 1])), String(calls.nav[calls.nav.length - 1]))

  const gDetailDef = loadPage('pages/guides/detail.js')
  const gDetail = instantiate(gDetailDef)
  gDetail.onLoad({ slug: 'a' })
  await gDetail.ready
  check('详情页加载完成', gDetail.data.loading, false)
  check('详情页标题', gDetail.data.guide.title, 'A 迷彩攻略')
  gDetail.onToggleFaq({ currentTarget: { dataset: { i: 0 } } })
  check('FAQ 展开', gDetail.data.openFaq[0], true)
  gDetail.onToggleFaq({ currentTarget: { dataset: { i: 0 } } })
  check('FAQ 收起', gDetail.data.openFaq[0], false)
  gDetail.onTapRelated({ currentTarget: { dataset: { slug: 'b' } } })
  assert('相关攻略用 redirectTo', /slug=b/.test(String(calls.nav[calls.nav.length - 1])), String(calls.nav[calls.nav.length - 1]))
  assert('客服上下文带攻略名', /攻略:A 迷彩攻略/.test(gDetail.data.sessionFrom), gDetail.data.sessionFrom)
  assert('详情分享落地在本篇', /slug=a/.test(gDetail.onShareAppMessage().path), gDetail.onShareAppMessage().path)

  const risky = await guidesMod.loadDetail('risky', { force: true })
  check('标题命中词表 → 整篇隐藏', risky.hidden, true)
  check('整篇隐藏时不返回正文', risky.sections.length, 0)
  check('整篇隐藏时不返回 FAQ', risky.faqs.length, 0)

  const gHidden = instantiate(gDetailDef)
  gHidden.onLoad({ slug: 'risky' })
  await gHidden.ready
  check('详情页识别整篇隐藏', gHidden.data.guide.hidden, true)

  const gBad = instantiate(gDetailDef)
  gBad.onLoad({})
  check('缺 slug 不算加载中', gBad.data.loading, false)
  assert('缺 slug 有错误文案', !!gBad.data.error, gBad.data.error)

  /* ---------------------------------------------------------- 11. 迷彩图进了追踪器 */

  console.log('\n[11] 迷彩图与模式切换（追踪器页内）')
  store.clear()
  catalog.resetMemory()
  // 首页定义第 4 节已加载过（require 有缓存）
  const idx = instantiate(indexDef)
  idx.onLoad()
  await idx.ready

  check('三档迷彩带图片字段', typeof idx.data.tierCounts[0].image, 'string')
  assert('三档迷彩图用包内路径', /^\/images\/camo\/.+\.webp$/.test(idx.data.tierCounts[0].image), idx.data.tierCounts[0].image)
  check('三档迷彩带要求文案', typeof idx.data.tierCounts[0].requirement, 'string')
  check('默认模式是多人', idx.data.activeMode, 'MULTIPLAYER')
  check('多人第一档名', idx.data.tierCounts[0].name, '水银漂移')

  // 点模式卡片切换（就是「点多人显示哪 3 个迷彩」那个交互）
  idx.onMode({ currentTarget: { dataset: { key: 'WARZONE' } } })
  check('点战区卡片后切到战区', idx.data.activeMode, 'WARZONE')
  check('战区第一档名', idx.data.tierCounts[0].name, '视差')
  check('模式统计高亮项对得上', idx.data.modeStats.filter(function (m) { return m.code === 'WARZONE' })[0].short, '战区')
  idx.onMode({ currentTarget: { dataset: { key: 'DMZ' } } })
  assert('点 DMZ 卡片后第一档换了名字', idx.data.tierCounts[0].name && idx.data.tierCounts[0].name !== '水银漂移', idx.data.tierCounts[0].name)
  idx.onMode({ currentTarget: { dataset: { key: 'NOPE' } } })
  check('非法模式不生效', idx.data.activeMode, 'DMZ')
  idx.onMode({ currentTarget: { dataset: { key: 'MULTIPLAYER' } } })

  // 包内图片读不出来（没打进上传包）时，整体自愈切到官网地址
  assert('初始用包内图', idx.data.tierCounts[0].image.indexOf('/images/') === 0, idx.data.tierCounts[0].image)
  idx.onImgError()
  check('触发一次后切成 remote', idx.data.useRemoteImages, true)
  assert('迷彩图变成官网地址', idx.data.tierCounts[0].image.indexOf('https://') === 0, idx.data.tierCounts[0].image)
  assert('武器图也换成官网地址', idx.data.groups[0].weapons[0].image.indexOf('https://') === 0, idx.data.groups[0].weapons[0].image)

  // 展开武器时每一档也带图
  idx.onToggleExpand({ currentTarget: { dataset: { weapon: 'M4' } } })
  const m4Row = idx.data.groups[0].weapons.filter(function (x) { return x.code === 'M4' })[0]
  check('展开后每档带图', typeof m4Row.tiers[0].image, 'string')

  /* ---------------------------------------------------------- 12. 一对一咨询（微信客服） */

  console.log('\n[12] 一对一咨询（微信客服跳转）')
  const kf = require(path.join(ROOT, 'utils', 'kf.js'))
  check('配置里有两个客服', kf.config().channels.length, 2)
  check('企业ID 已配置', kf.config().corpId, 'wwa474532de156197a')
  check('客服按钮文案', kf.config().buttonText, '一对一咨询')

  let sheetShown = 0
  global.wx.showActionSheet = function (opts) {
    sheetShown++
    opts.success({ tapIndex: global.__tapIndex || 0 })
  }
  const ch1 = await kf.choose()
  check('弹出了客服选择', sheetShown, 1)
  check('默认选白班', ch1.label, '白班客服')
  global.__tapIndex = 1
  const ch2 = await kf.choose()
  check('可以切到夜班', ch2.label, '夜班客服')
  global.__tapIndex = 0

  check('配好 corpId 后可用', kf.available(), true)
  check('contact 打开成功', await kf.contact(), true)
  check('默认传白班链接', global.__kfOpened.url, 'https://work.weixin.qq.com/kfid/kfc392f40b584c2d7a1')
  check('企业ID 传对了', global.__kfOpened.corpId, 'wwa474532de156197a')

  global.__tapIndex = 1
  await kf.contact()
  check('选夜班时传夜班链接', global.__kfOpened.url, 'https://work.weixin.qq.com/kfid/kfc03e31c506368bb68')
  global.__tapIndex = 0

  // 用户点取消时不应该报错
  const cancelImpl = global.wx.showActionSheet
  global.wx.showActionSheet = function (opts) { opts.fail({ errMsg: 'cancel' }) }
  check('取消选择时不报错', await kf.contact(), null)
  global.wx.showActionSheet = cancelImpl

  // 老微信没有这个接口时，给出明确提示
  const savedImpl = global.wx.openCustomerServiceChat
  delete global.wx.openCustomerServiceChat
  check('缺少接口时不可用', kf.available(), false)
  let kfErr2 = null
  await kf.open().catch(function (e) { kfErr2 = e })
  assert('老微信给出明确提示', !!kfErr2 && /微信版本/.test(kfErr2.message), kfErr2 && kfErr2.message)
  global.wx.openCustomerServiceChat = savedImpl

  // 页面：配置齐了应该走「一对一咨询」直达按钮
  check('首页走直达按钮', idx.data.kfReady, true)
  check('按钮文案', idx.data.kfButtonText, '一对一咨询')

  console.log('\n' + (failures === 0 ? '全部通过 ✅' : failures + ' 项失败 ❌'))
  process.exit(failures === 0 ? 0 : 1)
})()
