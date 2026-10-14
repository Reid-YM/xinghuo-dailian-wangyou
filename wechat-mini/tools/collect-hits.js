const https = require('https')
function get(url) {
  return new Promise(function (res, rej) {
    https.get(url, { headers: { 'User-Agent': 'Mozilla/5.0' } }, function (r) {
      let d = ''
      r.on('data', function (c) { d += c })
      r.on('end', function () { res(JSON.parse(d)) })
    }).on('error', rej)
  })
}
const WORDS = ['代练', '代打', '代肝', '代做', '代刷', '陪练', '下单', '报价', '价格', '多少钱', '购买', '支付', '付款', '微信号', '加微信']
function hit(s) {
  const t = String(s || '')
  for (const w of WORDS) if (t.indexOf(w) !== -1) return w
  return ''
}
;(async function () {
  const list = await get('https://wydlgf.cn/api/guides?limit=100')
  const out = []
  for (const g of list) {
    const d = await get('https://wydlgf.cn/api/guides/' + g.slug)
    const fields = []
    const gg = d.guide || {}
    fields.push(['title', gg.title], ['subtitle', gg.subtitle], ['summary', gg.summary])
    ;(d.sections || []).forEach(function (s, i) {
      fields.push(['section[' + i + '].heading', s.heading])
      String(s.bodyMarkdown || '').split(/\r?\n/).forEach(function (line) {
        if (line.trim()) fields.push(['section[' + i + '].body', line])
      })
    })
    ;(d.faqs || []).forEach(function (f, i) {
      fields.push(['faq[' + i + '].question', f.question])
      fields.push(['faq[' + i + '].answer', f.answer])
    })
    fields.forEach(function (f) {
      const w = hit(f[1])
      if (w) out.push({ slug: g.slug, field: f[0], word: w, text: f[1] })
    })
  }
  console.log('共 ' + out.length + ' 处命中\n')
  out.forEach(function (o, i) {
    console.log('[' + (i + 1) + '] ' + o.slug + ' | ' + o.field + ' | 命中:' + o.word)
    console.log('    ' + o.text)
  })
  require('fs').writeFileSync(require('path').join(__dirname, 'guide-hits.json'), JSON.stringify(out, null, 2), 'utf8')
  console.log('\n已写到 tools/guide-hits.json（' + out.length + ' 条）')
})().catch(function (e) { console.log('失败', e.message) })
