/**
 * 攻略数据层：走网站公开接口
 *   GET /api/guides            列表
 *   GET /api/guides/{slug}     详情
 *
 * 数据进入页面前的处理：
 *
 * 1. **丢掉 products / sourceUrl / canonicalUrl**
 *    products 是网站业务服务数据，攻略页只展示游戏内容。
 *    站外链接也不进入小程序页面模型。
 *
 * 2. **正文过一遍内容过滤**（config/business.js 的 CONTENT_FILTER）
 *    网站内容源已经整理；运行时仍保留过滤作为回归保护。
 *    命中的段落不进入页面模型，具体数量供诊断脚本检查。
 *
 * 3. **正文用 markdown.js 解析成块**，链接只留文字、不留 URL。
 *
 * 缓存 6 小时；接口挂了会 reject，由页面给提示 + 重试。
 */
const api = require('./api')
const markdown = require('./markdown')
const business = require('../config/business')

const LIST_KEY = 'xh_guides_list_v2'
const DETAIL_KEY = 'xh_guides_detail_v2'
const TTL = 6 * 3600 * 1000

const DIFFICULTY = {
  BEGINNER: '入门',
  INTERMEDIATE: '进阶',
  ADVANCED: '高级'
}

function difficultyText(code) {
  return DIFFICULTY[code] || ''
}

/* ------------------------------------------------------------------ 内容过滤 */

function filterConfig() {
  const f = business.CONTENT_FILTER || {}
  const merged = (f.guideWords || []).concat(f.blockWords || [])
  return {
    allWords: merged,
    enabled: f.enabled !== false,
    guideWords: f.guideWords || [],
    blockWords: f.blockWords || [],
    maxDroppedRatio: typeof f.maxDroppedRatio === 'number' ? f.maxDroppedRatio : 0.6
  }
}

function hitsAny(text, words) {
  const s = String(text || '')
  for (let i = 0; i < words.length; i++) {
    if (s.indexOf(words[i]) !== -1) return words[i]
  }
  return ''
}

function runsText(runs) {
  return (runs || []).map(function (r) { return r.text }).join('')
}

function blockText(b) {
  if (!b) return ''
  if (b.type === 'list') return (b.items || []).map(runsText).join(' ')
  return runsText(b.runs)
}

/* ------------------------------------------------------------------ 清洗 */

function cleanSummary(g) {
  const cfg = filterConfig()
  const item = {
    slug: g.slug,
    title: g.title || '',
    subtitle: g.subtitle || '',
    summary: g.summary || '',
    cover: api.asset(g.coverImage),
    difficulty: difficultyText(g.difficulty),
    minutes: g.estimatedMinutes || 0,
    author: g.authorName || '',
    tags: Array.isArray(g.tags) ? g.tags.slice(0, 3) : [],
    featured: !!g.featured,
    updatedAt: g.updatedAt || ''
  }
  if (cfg.enabled) {
    const hay = [item.title, item.subtitle, item.summary, item.tags.join(' ')].join(' ')
    if (hitsAny(hay, cfg.guideWords)) return null // 整篇不收录
  }
  return item
}

function cleanDetail(raw) {
  const cfg = filterConfig()
  const g = (raw && raw.guide) || {}
  let dropped = 0
  let counted = 0

  const sections = []
  ;((raw && raw.sections) || []).forEach(function (s) {
    const heading = s.heading || ''
    // 章节标题命中（含「下单前准备」这类）就把整段丢掉，否则标题本身会漏出去
    const headingHit = cfg.enabled && !!hitsAny(heading, cfg.allWords)
    const blocks = []

    markdown.parse(s.bodyMarkdown).forEach(function (b) {
      if (cfg.enabled && b.type !== 'divider') {
        counted++
        if (headingHit || hitsAny(blockText(b), cfg.blockWords)) {
          dropped++
          return
        }
      }
      blocks.push(b)
    })

    if (headingHit) {
      // 标题本身也算一条剔除：只丢正文不丢标题的话，标题还是会漏到页面上
      dropped++
      return
    }
    if (!heading && !blocks.length) return

    sections.push({
      heading: heading,
      image: api.asset(s.imageUrl),
      imageAlt: s.imageAlt || '',
      blocks: blocks
    })
  })

  const faqs = []
  ;((raw && raw.faqs) || []).forEach(function (f) {
    const item = { question: f.question || '', answer: f.answer || '' }
    if (cfg.enabled) {
      counted++
      if (hitsAny(item.question + ' ' + item.answer, cfg.blockWords)) {
        dropped++
        return
      }
    }
    faqs.push(item)
  })

  const related = []
  ;((raw && raw.relatedGuides) || []).forEach(function (r) {
    const item = { slug: r.slug, title: r.title || '', cover: api.asset(r.coverImage) }
    if (cfg.enabled && hitsAny(item.title, cfg.allWords)) return
    related.push(item)
  })

  // 标题本身就命中，或者被剔掉的段落太多 → 整篇不展示
  const titleHit = cfg.enabled && !!hitsAny([g.title, g.subtitle, g.summary].join(' '), cfg.guideWords)
  const ratio = counted ? dropped / counted : 0
  const hidden = titleHit || (cfg.enabled && counted > 0 && ratio > cfg.maxDroppedRatio)

  return {
    slug: g.slug,
    title: g.title || '',
    subtitle: g.subtitle || '',
    summary: g.summary || '',
    cover: api.asset(g.coverImage),
    difficulty: difficultyText(g.difficulty),
    minutes: g.estimatedMinutes || 0,
    author: g.authorName || '',
    tags: Array.isArray(g.tags) ? g.tags.slice(0, 8) : [],
    updatedAt: g.updatedAt || '',
    hidden: hidden,
    droppedBlocks: dropped,
    totalBlocks: counted,
    sections: hidden ? [] : sections,
    faqs: hidden ? [] : faqs,
    related: hidden ? [] : related
    // products / sourceUrl / canonicalUrl / seo* 一律不带出来
  }
}

/* ------------------------------------------------------------------ 缓存 */

function readCache(key) {
  try {
    const raw = wx.getStorageSync(key)
    return raw && raw.fetchedAt ? raw : null
  } catch (e) {
    return null
  }
}

function writeCache(key, payload) {
  try {
    wx.setStorageSync(key, { fetchedAt: Date.now(), payload: payload })
  } catch (e) {}
}

/* ------------------------------------------------------------------ 对外 */

/** 攻略列表。filteredOut = 因为合规过滤没收录的篇数 */
function loadList(options) {
  const opts = options || {}
  const cached = readCache(LIST_KEY)
  const cachedList = cached && (Array.isArray(cached.payload)
    ? { items: cached.payload, filteredOut: 0 }
    : (cached.payload && Array.isArray(cached.payload.items) ? cached.payload : null))
  if (!opts.force && cachedList && Date.now() - cached.fetchedAt < TTL) {
    return Promise.resolve({ items: cachedList.items, fetchedAt: cached.fetchedAt, cached: true, filteredOut: cachedList.filteredOut || 0 })
  }
  return api.request('/api/guides?limit=100').then(function (list) {
    const all = Array.isArray(list) ? list : []
    const items = all.map(cleanSummary).filter(Boolean)
    writeCache(LIST_KEY, { items: items, filteredOut: all.length - items.length })
    return {
      items: items,
      fetchedAt: Date.now(),
      cached: false,
      filteredOut: all.length - items.length
    }
  }).catch(function (err) {
    if (cachedList) {
      return { items: cachedList.items, fetchedAt: cached.fetchedAt, cached: true, filteredOut: cachedList.filteredOut || 0 }
    }
    throw err
  })
}

function loadDetail(slug, options) {
  const opts = options || {}
  const cache = readCache(DETAIL_KEY)
  const bySlug = (cache && cache.payload) || {}
  if (!opts.force && bySlug[slug] && Date.now() - cache.fetchedAt < TTL) {
    return Promise.resolve(bySlug[slug])
  }
  return api.request('/api/guides/' + encodeURIComponent(slug)).then(function (raw) {
    const detail = cleanDetail(raw)
    bySlug[slug] = detail
    writeCache(DETAIL_KEY, bySlug)
    return detail
  }).catch(function (err) {
    if (bySlug[slug]) return bySlug[slug]
    throw err
  })
}

/** 本地筛选：11 篇的量级，前端过滤比每次都打接口快得多 */
function filterList(items, keyword) {
  const kw = String(keyword || '').trim().toLowerCase()
  if (!kw) return items
  return items.filter(function (g) {
    const hay = [g.title, g.subtitle, g.summary, g.tags.join(' ')].join(' ').toLowerCase()
    return hay.indexOf(kw) !== -1
  })
}

module.exports = {
  TTL,
  LIST_KEY,
  DETAIL_KEY,
  loadList,
  loadDetail,
  filterList,
  difficultyText,
  filterConfig,
  hitsAny,
  cleanSummary,
  cleanDetail
}
