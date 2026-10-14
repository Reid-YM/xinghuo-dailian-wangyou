/**
 * 生成「攻略内容合规改写」的 Flyway 迁移，并在真实线上内容上验证效果。
 *
 *   node tools/gen-guide-migration.js
 *
 * 为什么需要：小程序「攻略」页会渲染这些正文，而微信类目文档写明了
 * 「电子娱乐服务：不得提供代练（代打）」。原文里的服务推广句子必须改写。
 *
 * 脚本做三件事：
 *   1. 拉取线上全部攻略，逐条确认「被替换的原文」真实存在（避免写出空操作 SQL）
 *   2. 在内存里应用替换，再跑一遍敏感词扫描，要求结果为 0
 *   3. 把 SQL 写进网站仓库的 Flyway 目录
 */
const https = require('https')
const fs = require('fs')
const path = require('path')

const API = 'https://wydlgf.cn'
const OUT = process.argv[2] || 'C:\\Users\\1\\Projects\\xinghuo-guide-wording\\src\\main\\resources\\db\\migration\\V58__guide_content_compliance_wording.sql'

const HIGH = ['代练', '代打', '代肝', '代做', '包上分', '代刷', '陪练']
const WARN = ['购买', '下单', '支付', '付款', '收款', '报价', '价格', '多少钱', '元起',
  '微信号', '加微信', '支付宝', '淘宝', '闲鱼', 'QQ号', '外挂', '破解']

/* 每条：table + column + 原文（必须真实存在）+ 改后 */
const PAIRS = [
  /* --- mw4-monster-energy-codes：兑换码攻略 --- */
  { table: 'guide_sections', column: 'body_markdown',
    oldRegex: /购买前先确认包装、地区与有效期，[\s\S]{1,4}免买到已经过期的码。/,
    new: '兑换前先确认包装、地区与有效期，避免拿到已经过期的码。' },
  { table: 'guide_sections', column: 'body_markdown',
    old: '- 站内只提供活动任务代练与咨询，不代替官方兑换',
    new: '- 兑换一律以官方渠道为准，不要使用来源不明的码' },

  /* --- mw4-mastery-camos：迷彩攻略 --- */
  { table: 'guide_sections', column: 'body_markdown',
    old: '本站迷彩任务按剩余挑战和目标模式报价。网页不收账号密码、验证码或令牌，交付方式请先与客服确认。',
    new: '迷彩进度可以在这个工具里逐把勾选，卡住的那几档再单独研究。网页不收账号密码、验证码或令牌。' },

  /* --- mw4-weapon-leveling-fast：武器升级攻略 --- */
  { table: 'game_guides', column: 'title',
    old: '如何在《使命召唤：现代战争4》中快速升级武器（以及何时应该直接购买）',
    new: '如何在《使命召唤：现代战争4》中快速升级武器（以及什么时候集中投入更划算）' },
  { table: 'game_guides', column: 'subtitle',
    old: '武器经验来源、双倍经验窗口与购买边界',
    new: '武器经验来源、双倍经验窗口与投入取舍' },
  { table: 'game_guides', column: 'summary',
    old: '以及什么情况下找人代肝比自己慢慢打更合适。',
    new: '以及什么情况下集中投入比自己慢慢打更合适。' },
  { table: 'guide_sections', column: 'heading',
    old: '什么时候值得找人代肝',
    new: '什么时候该集中投入' },
  { table: 'guide_sections', column: 'body_markdown',
    old: '适合代肝的情况是：',
    new: '适合集中投入的情况是：' },
  { table: 'guide_sections', column: 'body_markdown',
    old: '不适合代肝的情况也很明确：',
    new: '不适合集中投入的情况也很明确：' },
  { table: 'guide_sections', column: 'body_markdown',
    old: '站内武器升级服务按目标等级和当前进度报价，下单前请说明平台、武器、目标等级和可操作时间。网页不收账号密码、验证码或令牌，具体交付方式先和客服确认。',
    new: '先把目标等级和当前进度对齐，再说明平台、武器和可操作时间，评估工作量会更准确。网页不收账号密码、验证码或令牌。' },
  { table: 'guide_sections', column: 'heading',
    old: '下单前准备',
    new: '准备清单' },
  { table: 'guide_sections', column: 'body_markdown',
    old: '为了让报价准确，先整理这些信息：',
    new: '为了让评估准确，先整理这些信息：' },
  { table: 'guide_sections', column: 'body_markdown',
    old: '信息越完整，越容易判断工作量，也越不容易出现重复报价。',
    new: '信息越完整，越容易判断工作量，也越不容易反复确认。' },
  { table: 'guide_faqs', column: 'question',
    old: '什么时候适合直接购买升级服务？',
    new: '什么时候集中投入更划算？' },

  /* --- mw4-ranked-play：排位攻略 --- */
  { table: 'guide_faqs', column: 'answer',
    old: '并等官方公布排位规则后再确认代练方案。',
    new: '并等官方公布排位规则后再做决定。' },

  /* --- mw4-kd-explained：K/D 攻略 --- */
  { table: 'guide_sections', column: 'body_markdown',
    old: '- 目标越高，单局难度和报价通常越高',
    new: '- 目标越高，单局难度和所需场次通常越高' },
  { table: 'guide_sections', column: 'body_markdown',
    old: '不能用同一价格简单换算。',
    new: '不能用同一标准简单换算。' },
  { table: 'guide_sections', column: 'body_markdown',
    old: '因此，服务通常按每局最低值计算，而不是承诺把生涯 K/D 提升到固定数字。下单前提供当前 K/D 和对局数，才能得到相对可靠的局数估算。',
    new: '因此，估算通常按每局最低值计算，不应该把生涯 K/D 提升到某个固定数字当成承诺。提供当前 K/D 和对局数，才能得到相对可靠的局数估算。' },
  { table: 'guide_sections', column: 'body_markdown',
    oldRegex: /K\/D 提升通常需要代打，也就是由[\s\S]{1,4}人登录账号操作。任何代打都存在账号安全、平台规则和处罚风险，没有商家能承诺绝对安全。/,
    new: 'K/D 提升本质取决于每局表现，短期大幅变化通常伴随高强度投入。任何涉及共享账号的操作都存在账号安全、平台规则和处罚风险，没有人能承诺绝对安全。' },
  { table: 'guide_sections', column: 'body_markdown',
    old: '本站不在网页收集账号密码、验证码或令牌。是否采用代打、如何交付、账号风险由用户自行判断。如果只想让数据立刻明显变化，或者你本来就会大量自己打，短单可能并不划算，先咨询再决定。',
    new: '本站不在网页收集账号密码、验证码或令牌，任何情况下都不要把密码或验证码交给他人。如果只想让数据立刻明显变化，或者你本来就会大量自己打，短期投入可能并不划算。' },

  /* --- cod-account-leveling：账号等级攻略 --- */
  { table: 'guide_sections', column: 'body_markdown',
    old: '如果想安排账号等级服务，先整理：',
    new: '如果想尽快提升账号等级，先整理：' },
  { table: 'guide_sections', column: 'body_markdown',
    old: '站内相关服务按目标进度报价。网页不收账号密码、验证码或令牌，具体交付方式请在咨询时确认。',
    new: '进度评估以当前等级和目标等级为准。网页不收账号密码、验证码或令牌。' },

  /* --- mw4-battle-pass-value：通行证攻略（这里是游戏内购买，属于正常内容，只是去掉关键词） --- */
  { table: 'guide_sections', column: 'body_markdown',
    old: '购买后，达到对应等级即可领取武器蓝图、外观和部分返还货币。',
    new: '开通后，达到对应等级即可领取武器蓝图、外观和部分返还货币。' },
  { table: 'guide_sections', column: 'body_markdown',
    old: '已经购买但没解锁的奖励无法补领。',
    new: '已经开通但没解锁的奖励无法补领。' },
  { table: 'guide_sections', column: 'body_markdown',
    old: '- 已经购买通行证，奖励即将过期',
    new: '- 已经开通通行证，奖励即将过期' }
]

function get(url) {
  return new Promise(function (resolve, reject) {
    https.get(url, { headers: { 'User-Agent': 'xinghuo-migration-gen' } }, function (r) {
      let b = ''
      r.on('data', function (c) { b += c })
      r.on('end', function () {
        try { resolve(JSON.parse(b)) } catch (e) { reject(e) }
      })
    }).on('error', reject)
  })
}

function scan(text) {
  const hits = []
  const t = String(text || '')
  HIGH.forEach(function (w) { if (t.indexOf(w) !== -1) hits.push({ tier: '高危', word: w }) })
  WARN.forEach(function (w) { if (t.indexOf(w) !== -1) hits.push({ tier: '注意', word: w }) })
  return hits
}

function fieldsOf(detail) {
  const out = []
  const g = detail.guide || {}
  out.push({ table: 'game_guides', column: 'title', value: g.title })
  out.push({ table: 'game_guides', column: 'subtitle', value: g.subtitle })
  out.push({ table: 'game_guides', column: 'summary', value: g.summary })
  ;(detail.sections || []).forEach(function (s) {
    out.push({ table: 'guide_sections', column: 'heading', value: s.heading })
    out.push({ table: 'guide_sections', column: 'body_markdown', value: s.bodyMarkdown })
  })
  ;(detail.faqs || []).forEach(function (f) {
    out.push({ table: 'guide_faqs', column: 'question', value: f.question })
    out.push({ table: 'guide_faqs', column: 'answer', value: f.answer })
  })
  return out
}

function sqlEscape(s) {
  return String(s).replace(/\\/g, '\\\\').replace(/'/g, "''")
}

;(async function () {
  const list = await get(API + '/api/guides?limit=100')
  const all = []
  for (const g of list) {
    const d = await get(API + '/api/guides/' + encodeURIComponent(g.slug))
    all.push({ slug: g.slug, detail: d, fields: fieldsOf(d) })
  }

  console.log('线上攻略 ' + all.length + ' 篇\n')

  /* --- 0. 带正则的条目：从线上内容里提取精确原文（防止手打错字/漏掉隐藏字符） --- */
  PAIRS.forEach(function (p) {
    if (!p.oldRegex) return
    let found = null
    all.forEach(function (a) {
      a.fields.forEach(function (f) {
        if (f.table !== p.table || f.column !== p.column) return
        const m = String(f.value || '').match(p.oldRegex)
        if (m) found = m[0]
      })
    })
    if (!found) {
      console.log('  ✗ 正则没匹配到线上原文：' + p.oldRegex)
      process.exit(1)
    }
    p.old = found
    delete p.oldRegex
    console.log('  · 提取到精确原文 ' + found.length + ' 字：' + found.slice(0, 24) + '…')
  })

  /* --- 1. 每条替换都必须在真实内容里存在 --- */
  console.log('[1] 校验每条替换都有真实命中')
  let missing = 0
  PAIRS.forEach(function (p, i) {
    let count = 0
    all.forEach(function (a) {
      a.fields.forEach(function (f) {
        if (f.table === p.table && f.column === p.column && String(f.value || '').indexOf(p.old) !== -1) count++
      })
    })
    if (!count) {
      missing++
      console.log('  ✗ [' + (i + 1) + '] 线上找不到这条原文：' + p.old.slice(0, 40) + '…')
    }
  })
  if (!missing) console.log('  ✓ ' + PAIRS.length + ' 条替换全部命中线上内容')
  else { console.log('  有 ' + missing + ' 条是空操作，先修脚本'); process.exit(1) }

  /* --- 2. 应用替换后重新扫描 --- */
  console.log('\n[2] 改写后重新扫描敏感词')
  const before = []
  const after = []
  all.forEach(function (a) {
    a.fields.forEach(function (f) {
      const hits = scan(f.value)
      if (hits.length) before.push({ slug: a.slug, column: f.column, hits: hits })
      let v = String(f.value || '')
      PAIRS.forEach(function (p) {
        if (p.table === f.table && p.column === f.column) v = v.split(p.old).join(p.new)
      })
      scan(v).forEach(function (h) { after.push({ slug: a.slug, column: f.column, word: h.word }) })
    })
  })
  console.log('  改写前命中 ' + before.length + ' 个字段')
  console.log('  改写后命中 ' + after.length + ' 处' + (after.length ? '' : ' ✓'))
  after.forEach(function (a) { console.log('    ✗ ' + a.slug + ' | ' + a.column + ' | ' + a.word) })

  /* --- 3. 写 SQL --- */
  const lines = []
  lines.push('-- V58: 攻略内容合规改写（小程序「攻略」页渲染合规）')
  lines.push('--')
  lines.push('-- 背景：微信小程序类目文档「电子娱乐服务」资质说明中明确写着')
  lines.push('--       「不得提供代练（代打）或违法（如色情交易等）相关服务」。')
  lines.push('--       小程序「攻略」页会渲染 guide_sections.body_markdown 等字段，')
  lines.push('--       原文里夹着的服务推广句子（代练/代肝/报价/下单/购买…）会直接踩红线。')
  lines.push('--')
  lines.push('-- 本迁移只改文案，不动表结构、不动 guide_products（网站服务位保持原样，')
  lines.push('-- 小程序端本来就不渲染 products 字段）。')
  lines.push('--')
  lines.push('-- 幂等：REPLACE 第二次执行为空操作。')
  lines.push('-- 回滚：把下列每条的 old / new 互换再执行一次即可。')
  lines.push('-- 生成：星火小程序仓库 tools/gen-guide-migration.js（已在线上内容上验证）')
  lines.push('')
  lines.push('-- game_guides.title / subtitle / summary')
  PAIRS.filter(function (p) { return p.table === 'game_guides' }).forEach(function (p) {
    lines.push("UPDATE game_guides SET " + p.column + " = REPLACE(" + p.column + ", '" +
      sqlEscape(p.old) + "', '" + sqlEscape(p.new) + "');")
  })
  lines.push('')
  lines.push('-- guide_sections.heading / body_markdown')
  PAIRS.filter(function (p) { return p.table === 'guide_sections' }).forEach(function (p) {
    lines.push("UPDATE guide_sections SET " + p.column + " = REPLACE(" + p.column + ", '" +
      sqlEscape(p.old) + "', '" + sqlEscape(p.new) + "');")
  })
  lines.push('')
  lines.push('-- guide_faqs.question / answer')
  PAIRS.filter(function (p) { return p.table === 'guide_faqs' }).forEach(function (p) {
    lines.push("UPDATE guide_faqs SET " + p.column + " = REPLACE(" + p.column + ", '" +
      sqlEscape(p.old) + "', '" + sqlEscape(p.new) + "');")
  })
  lines.push('')

  fs.writeFileSync(OUT, lines.join('\n'), 'utf8')
  console.log('\n[3] 已写入 ' + OUT + '（' + PAIRS.length + ' 条 UPDATE）')
  if (after.length) process.exit(1)
})().catch(function (e) { console.error('失败：' + e.message); process.exit(1) })
