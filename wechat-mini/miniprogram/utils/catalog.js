/**
 * 数据层：把「星火网站的接口数据」和「本地快照」统一成同一种结构给页面用。
 *
 * 取数优先级：
 *   1. 内存
 *   2. 本机缓存（12 小时以内）
 *   3. 线上接口 /api/tool-station   ← 你改网站，小程序立刻跟着变
 *   4. 过期的本机缓存
 *   5. 打包在 data/weapons.js 里的快照（装好就能用，离线也能用）
 *
 * 所以：域名没配、用户断网、接口挂了，小程序都不会白屏，只是数据停在快照那一版。
 */
const api = require('./api')
const builtinPayload = require('../data/weapons')
const imageMap = require('../data/image-map')
const business = require('../config/business')

/**
 * 图片解析：先看有没有打进包里（不需要域名、离线可用），没有才回退到官网地址。
 * 映射表由 tools/sync-images.js 生成。
 */
function resolveImage(p) {
  if (!p) return ''
  // remote 模式：直接读官网（和网页版一致），需要 downloadFile 合法域名
  if (business.SITE.imageMode === 'remote') return api.asset(p)
  const local = imageMap[p]
  if (local) return local
  // 没打进包的（例如攻略封面）才回退到官网地址 —— 那种需要 downloadFile 合法域名
  return api.asset(p)
}

const CACHE_KEY = 'xh_catalog_v1'
const TTL = 12 * 3600 * 1000

let memory = null
let loading = null

/* 各职业基础迷彩的通用要求，展开武器详情时当提示用（官网接口里没有这项，本地兜着） */
const CLASS_HINTS = {
  突击步枪: '基础迷彩：以爆头击杀为主，分阶段完成。',
  冲锋枪: '基础迷彩：腰射击杀与近距离击杀。',
  霰弹枪: '基础迷彩：贴脸击杀（point-blank）。',
  轻机枪: '基础迷彩：架枪或蹲伏状态下的击杀。',
  射手步枪: '基础迷彩：爆头击杀与远距离击杀。',
  狙击步枪: '基础迷彩：一枪击杀与远距离击杀。',
  手枪: '基础迷彩：爆头击杀与腰射击杀。',
  发射器: '基础迷彩：摧毁敌方装备、连杀奖励与载具。',
  近战: '基础迷彩：近战 / 贴脸击杀。'
}

function classHint(cls) {
  return CLASS_HINTS[cls] || '基础迷彩：分阶段完成的累积型挑战。'
}

/* ------------------------------------------------------------------ 归一化 */

function normalizeCamoGuide(raw) {
  if (!raw || !raw.classes) return null
  const classes = {}
  Object.keys(raw.classes).forEach(function (cls) {
    const c = raw.classes[cls] || {}
    const groups = []
    const military = c.military || {}
    if (military.text) {
      groups.push({
        key: 'military',
        title: '军事迷彩 · ' + (military.count || 8) + ' 档',
        items: [{ key: 'military', name: '按武器等级解锁', req: military.text, estimate: !!military.estimate }]
      })
    }
    const special = (c.special || []).map(function (it, i) {
      return { key: 'special-' + i, name: it.name || ('特种迷彩 ' + (i + 1)), req: it.text || '', estimate: !!it.estimate }
    }).filter(function (it) { return it.req })
    if (special.length) groups.push({ key: 'special', title: '特种迷彩 · ' + special.length + ' 档', items: special })
    const comp = (c.completionist || []).map(function (it, i) {
      return { key: 'comp-' + i, name: it.name || '', req: it.text || '', estimate: !!it.estimate }
    }).filter(function (it) { return it.req })
    if (comp.length) groups.push({ key: 'completionist', title: '完成主义 · ' + comp.length + ' 档', items: comp })
    classes[cls] = { groups: groups, note: raw.note || '' }
  })
  return { version: raw.version || '', note: raw.note || '', classes: classes }
}

function normalize(payload, source, fetchedAt) {
  const games = (payload && payload.games) || []
  const game =
    games.filter(function (g) { return g.code === payload.defaultGameCode })[0] || games[0]
  if (!game || !game.camoTracks) throw new Error('数据结构不认识：缺少 games[] / camoTracks')

  const tracks = game.camoTracks
  const order = tracks.order || Object.keys(tracks.modes || {})
  const rawModes = order
    .map(function (code) { return tracks.modes[code] })
    .filter(Boolean)

  const perWeaponModes = rawModes.filter(function (m) { return m.mastery }).map(function (m) {
    return {
      code: m.code,
      name: m.name,
      short: m.shortName || m.name,
      mastery: true,
      finalCamo: m.finalCamo || '',
      finalCamoZh: m.finalCamoZh || '',
      tiers: (m.items || []).map(function (it) {
        return {
          tier: it.tier,
          code: it.code,
          name: it.name,
          nameZh: it.nameZh || it.name,
          requirement: it.requirement || '',
          image: resolveImage(it.image),
          imageRemote: api.asset(it.image)
        }
      }),
      previewImage: resolveImage(m.previewImage)
    }
  })

  const eventModeRaw = rawModes.filter(function (m) { return !m.mastery })[0] || null
  const eventMode = eventModeRaw
    ? {
        code: eventModeRaw.code,
        name: eventModeRaw.name,
        short: eventModeRaw.shortName || eventModeRaw.name,
        items: (eventModeRaw.items || []).map(function (it) {
          return {
            code: it.code,
            name: it.name,
            nameZh: it.nameZh || it.name,
            requirement: it.requirement || '',
            image: resolveImage(it.image),
          imageRemote: api.asset(it.image)
          }
        })
      }
    : null

  const weapons = (game.weapons || []).map(function (w) {
    return {
      code: w.code,
      name: w.name,
      nameZh: w.nameZh || w.name,
      cls: w.weaponType || '其他',
      image: resolveImage(w.image),
      imageRemote: api.asset(w.image),
      maxLevel: w.maxLevel || 0,
      hint: classHint(w.weaponType || '')
    }
  })

  const classes = []
  weapons.forEach(function (w) {
    if (classes.indexOf(w.cls) === -1) classes.push(w.cls)
  })



  const tiersPerMode = tracks.tiersPerMode || 3
  const camoGuide = normalizeCamoGuide(game.camoGuide)
  const total =
    weapons.length * perWeaponModes.length * tiersPerMode +
    (eventMode ? eventMode.items.length : 0)

  return {
    source: source,
    version: tracks.version || '',
    storageVersion: payload.storageVersion || '',
    fetchedAt: fetchedAt || Date.now(),
    game: {
      code: game.code,
      name: game.name,
      short: game.shortName,
      accent: game.accent || '',
      dataStatus: game.dataStatus || '',
      // 官方数据还在占位阶段时给用户一个提醒
      placeholder: perWeaponModes.some(function (m) {
        const raw = tracks.modes[m.code] || {}
        return raw.status === 'placeholder'
      })
    },
    modes: perWeaponModes,
    eventMode: eventMode,
    weapons: weapons,
    classes: classes,
    tiersPerMode: tiersPerMode,
    camoGuide: camoGuide,
    total: total
  }
}

/* ------------------------------------------------------------------ 缓存 */

function readCache() {
  try {
    const raw = wx.getStorageSync(CACHE_KEY)
    if (!raw || !raw.payload) return null
    return { payload: raw.payload, fetchedAt: raw.fetchedAt || 0 }
  } catch (e) {
    return null
  }
}

function writeCache(payload) {
  try {
    wx.setStorageSync(CACHE_KEY, { payload: payload, fetchedAt: Date.now() })
  } catch (e) {}
}

/* ------------------------------------------------------------------ 对外 */

/**
 * 取数据。返回 Promise，永远 resolve，不会让页面白屏。
 * 结果里的 source 告诉你数据是哪来的：remote / cache / builtin。
 */
function load(options) {
  const opts = options || {}
  if (memory && !opts.force) return Promise.resolve(memory)
  if (loading && !opts.force) return loading

  const cached = readCache()
  const previous = memory
  if (!opts.force && cached && Date.now() - cached.fetchedAt < TTL) {
    try {
      memory = normalize(cached.payload, 'cache', cached.fetchedAt)
      return Promise.resolve(memory)
    } catch (e) {}
  }

  loading = api
    .fetchToolStation()
    .then(function (payload) {
      const cat = normalize(payload, 'remote')
      memory = cat
      writeCache(payload)
      return cat
    })
    .catch(function (err) {
      if (cached) {
        try {
          memory = normalize(cached.payload, 'cache', cached.fetchedAt)
          return memory
        } catch (e) {}
      }
      if (previous) {
        memory = Object.assign({}, previous, { source: 'cache' })
        return memory
      }
      const cat = normalize(builtinPayload, 'builtin')
      cat.error = (err && err.message) || '接口不可用'
      memory = cat
      return cat
    })
    .then(function (cat) {
      loading = null
      return cat
    })

  return loading
}

/** 进度键：模式::武器code::档位（例 MULTIPLAYER::M4::1） */
function tierKey(modeCode, weaponCode, tier) {
  return modeCode + '::' + weaponCode + '::' + tier
}

/** Event 类迷彩的键：模式::迷彩code（例 PYROCLAST::PYROCLAST_VERGE） */
function eventKey(modeCode, itemCode) {
  return modeCode + '::' + itemCode
}

function resetMemory() {
  memory = null
  loading = null
}

/** 云同步用的当前总项目数；不为此额外发网络请求。 */
function currentTotal() {
  if (memory) return memory.total
  const cached = readCache()
  if (cached) {
    try { return normalize(cached.payload, 'cache').total } catch (e) {}
  }
  return normalize(builtinPayload, 'builtin').total
}

module.exports = {
  CACHE_KEY,
  TTL,
  load,
  normalize,
  classHint,
  tierKey,
  eventKey,
  currentTotal,
  resetMemory
}
