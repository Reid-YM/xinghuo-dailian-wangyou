/**
 * 合规体检：扫描小程序里会不会出现「被微信驳回」的字眼。
 *
 *   node tools/compliance.js
 *
 * 扫两处：
 *   A. 小程序自己的代码文案（页面、配置）—— 这些是我们能直接改的
 *   B. 官网接口返回的**会渲染到页面上的内容**（攻略标题/正文/FAQ、武器与迷彩名）
 *      —— 这些来自你网站，要改得去网站后台改
 *
 * 分级：
 *   【高危】微信类目文档明确禁止的（代练/代打类）—— 出现在我们自己的代码里就直接判定失败
 *   【注意】可能被审核质疑导流/交易的（价格、下单、微信/QQ/淘宝等）
 */
const fs = require('fs')
const path = require('path')
const https = require('https')

const ROOT = path.join(__dirname, '..')
const MP = path.join(ROOT, 'miniprogram')

const HIGH = ['代练', '代打', '代肝', '代做', '包上分', '代刷', '陪练']
const WARN = ['购买', '下单', '支付', '付款', '收款', '价格', '报价', '多少钱', '元起',
  '微信号', '加微信', '支付宝', '淘宝', '闲鱼', 'QQ号', '外挂', '破解']

let problems = 0
let notices = 0

/** 支持 // compliance-ignore-start ... // compliance-ignore-end 把词表之类排除掉 */
function stripIgnored(text) {
  return text.replace(/\/\/\s*compliance-ignore-start[\s\S]*?\/\/\s*compliance-ignore-end/g, ' ')
}

/** 注释永远不会渲染到页面上，扫描前先去掉（注意别把 https:// 里的 // 当注释） */
function stripComments(text) {
  return text
    .replace(/<!--[\s\S]*?-->/g, ' ')
    .replace(/\/\*[\s\S]*?\*\//g, ' ')
    .replace(/(^|[^:])\/\/[^\n]*/g, '$1')
}

function scanText(text, where, tier) {
  const hits = []
  ;(tier === 'high' ? HIGH : WARN).forEach(function (w) {
    let i = text.indexOf(w)
    while (i !== -1) {
      const ctx = text.slice(Math.max(0, i - 20), i + w.length + 20).replace(/\s+/g, ' ')
      hits.push({ word: w, ctx: ctx })
      i = text.indexOf(w, i + w.length)
    }
  })
  return hits
}

function walk(dir, exts, out) {
  fs.readdirSync(dir, { withFileTypes: true }).forEach(function (d) {
    const full = path.join(dir, d.name)
    if (d.isDirectory()) {
      if (d.name === 'node_modules') return
      walk(full, exts, out)
    } else if (exts.indexOf(path.extname(d.name)) !== -1) {
      out.push(full)
    }
  })
  return out
}

function getJsonOnce(url) {
  return new Promise(function (resolve, reject) {
    https.get(url, { headers: { 'User-Agent': 'xinghuo-compliance' } }, function (r) {
      let b = ''
      r.on('data', function (c) { b += c })
      r.on('end', function () {
        try {
          resolve(JSON.parse(b))
        } catch (e) {
          reject(new Error('返回不是 JSON'))
        }
      })
    }).on('error', reject)
  })
}

/** 网络偶尔会抖，重试三次再放弃 */
function getJson(url) {
  let lastErr
  return (async function () {
    for (let i = 0; i < 3; i++) {
      try {
        return await getJsonOnce(url)
      } catch (e) {
        lastErr = e
        await new Promise(function (r) { setTimeout(r, 600) })
      }
    }
    throw lastErr
  })()
}

;(async function () {
  /* ---------------- A. 我们自己的代码 ---------------- */

  console.log('\n[A] 小程序自身文案')
  const files = walk(MP, ['.wxml', '.js', '.json'], [])
  let highHits = 0
  let warnHits = 0

  files.forEach(function (f) {
    const rel = path.relative(ROOT, f)
    let text = stripComments(stripIgnored(fs.readFileSync(f, 'utf8')))

    const high = scanText(text, rel, 'high')
    const warn = scanText(text, rel, 'warn')
    high.forEach(function (h) {
      highHits++
      problems++
      console.log('  ✗ 【高危】' + rel + ' → "' + h.word + '"  ...' + h.ctx + '...')
    })
    warn.forEach(function (h) {
      warnHits++
      notices++
      console.log('  ! 【注意】' + rel + ' → "' + h.word + '"  ...' + h.ctx + '...')
    })
  })

  if (!highHits && !warnHits) console.log('  ✓ 没有发现敏感词')
  else console.log('  扫描 ' + files.length + ' 个文件：高危 ' + highHits + ' 处，注意 ' + warnHits + ' 处')

  /* ---------------- B. 官网会渲染的内容 ---------------- */

  console.log('\n[B] 官网上会显示到小程序里的内容')
  const BASE = 'https://wydlgf.cn'
  let guideList = []

  try {
    guideList = await getJson(BASE + '/api/guides?limit=100')
    console.log('  攻略 ' + guideList.length + ' 篇')
    for (const g of guideList) {
      const head = [g.title, g.subtitle, g.summary, (g.tags || []).join(' ')].join(' | ')
      const high = scanText(head, g.slug, 'high')
      const warn = scanText(head, g.slug, 'warn')
      high.concat(warn).forEach(function (h) {
        const tier = HIGH.indexOf(h.word) !== -1 ? '【高危】' : '【注意】'
        notices++
        console.log('  ! ' + tier + '攻略 ' + g.slug + ' 的"标题/摘要/标签"含 "' + h.word + '"')
        console.log('      ' + h.ctx)
      })
    }

    // 正文（正文里出现服务字眼风险更高）
    for (const g of guideList) {
      let detail
      try {
        detail = await getJson(BASE + '/api/guides/' + encodeURIComponent(g.slug))
      } catch (e) {
        continue
      }
      const body = ((detail.sections || []).map(function (s) {
        return (s.heading || '') + ' ' + (s.bodyMarkdown || '')
      }).join(' ') + ' ' + (detail.faqs || []).map(function (f) {
        return f.question + ' ' + f.answer
      }).join(' '))
      const bodyHits = scanText(body, g.slug, 'high').concat(scanText(body, g.slug, 'warn'))
      const seen = {}
      bodyHits.forEach(function (h) {
        if (seen[h.word]) return
        seen[h.word] = true
        const tier = HIGH.indexOf(h.word) !== -1 ? '【高危】' : '【注意】'
        notices++
        console.log('  ! ' + tier + '攻略 ' + g.slug + ' 正文含 "' + h.word + '"  ...' + h.ctx + '...')
      })
    }

    // 相关攻略的标题也会显示
    const relatedTitles = new Set()
    for (const g of guideList) {
      try {
        const d = await getJson(BASE + '/api/guides/' + encodeURIComponent(g.slug))
        ;(d.relatedGuides || []).forEach(function (r) { relatedTitles.add(r.title) })
      } catch (e) {}
    }
    relatedTitles.forEach(function (title) {
      const hits = scanText(title, 'related', 'high').concat(scanText(title, 'related', 'warn'))
      hits.forEach(function (h) {
        const tier = HIGH.indexOf(h.word) !== -1 ? '【高危】' : '【注意】'
        notices++
        console.log('  ! ' + tier + '"相关攻略"标题含 "' + h.word + '"：' + title)
      })
    })
  } catch (e) {
    console.log('  · 官网接口没取到，跳过内容检查：' + e.message)
  }

  try {
    const tool = await getJson(BASE + '/api/tool-station')
    const texts = []
    ;(tool.games || []).forEach(function (game) {
      ;(game.weapons || []).forEach(function (w) {
        texts.push([w.name, w.nameZh, w.weaponType, w.unlockLabel, w.note].filter(Boolean).join(' '))
      })
      const modes = (game.camoTracks && game.camoTracks.modes) || {}
      Object.keys(modes).forEach(function (k) {
        texts.push(modes[k].name || '')
        ;(modes[k].items || []).forEach(function (it) {
          texts.push([it.name, it.nameZh, it.requirement].filter(Boolean).join(' '))
        })
      })
    })
    const joined = texts.join('\n')
    const hits = scanText(joined, 'tool-station', 'high').concat(scanText(joined, 'tool-station', 'warn'))
    if (!hits.length) console.log('  ✓ 武器/迷彩数据干净（' + texts.length + ' 条）')
    hits.forEach(function (h) {
      const tier = HIGH.indexOf(h.word) !== -1 ? '【高危】' : '【注意】'
      notices++
      console.log('  ! ' + tier + '武器/迷彩数据含 "' + h.word + '"  ...' + h.ctx + '...')
    })
  } catch (e) {
    console.log('  · 工具站接口没取到，跳过：' + e.message)
  }

  /* ---------------- C. 过滤之后页面实际会显示的文字 ---------------- */

  console.log('\n[C] 过滤后再扫一遍（页面真正会渲染的文字）')
  global.wx = {
    getStorageSync() { return '' },
    setStorageSync() {},
    removeStorageSync() {},
    request() {}
  }
  const guidesUtil = require(path.join(MP, 'utils', 'guides.js'))

  function runsText(runs) {
    return (runs || []).map(function (r) { return r.text }).join('')
  }
  function blockText(b) {
    if (!b) return ''
    if (b.type === 'list') return (b.items || []).map(runsText).join(' ')
    return runsText(b.runs)
  }

  let leaks = 0
  let hiddenCount = 0
  let droppedTotal = 0
  try {
    for (const g of guideList) {
      const raw = await getJson(BASE + '/api/guides/' + encodeURIComponent(g.slug))
      const d = guidesUtil.cleanDetail(raw)
      if (d.hidden) {
        hiddenCount++
        continue
      }
      droppedTotal += d.droppedBlocks
      const texts = []
      d.sections.forEach(function (s) {
        texts.push(s.heading)
        s.blocks.forEach(function (b) { texts.push(blockText(b)) })
      })
      d.faqs.forEach(function (f) { texts.push(f.question + ' ' + f.answer) })
      d.related.forEach(function (r) { texts.push(r.title) })
      const joined = texts.join('\n')
      scanText(joined, g.slug, 'high').forEach(function (h) {
        leaks++
        problems++
        console.log('  ✗ 【高危】过滤后仍会显示：' + g.slug + ' → "' + h.word + '"  ...' + h.ctx + '...')
      })
      scanText(joined, g.slug, 'warn').forEach(function (h) {
        notices++
        console.log('  ! 【注意】过滤后仍会显示：' + g.slug + ' → "' + h.word + '"  ...' + h.ctx + '...')
      })
    }
    if (!leaks) console.log('  ✓ 过滤后页面上不会再出现敏感词')
    console.log('  ' + guideList.length + ' 篇：整篇未收录 ' + hiddenCount + ' 篇，共剔除 ' + droppedTotal + ' 段服务推广内容')
  } catch (e) {
    console.log('  · 官网接口没取到，跳过：' + e.message)
  }

  console.log('\n结论：自身代码高危 ' + problems + ' 处；需人工确认 ' + notices + ' 处')
  console.log(problems === 0 ? '✅ 自身代码没有高危词，可以提审' : '❌ 自身代码有高危词，必须先改')
  process.exit(problems === 0 ? 0 : 1)
})()
