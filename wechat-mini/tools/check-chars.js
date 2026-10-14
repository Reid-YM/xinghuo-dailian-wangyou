const https = require('https')
function get(url) {
  return new Promise(function (res, rej) {
    https.get(url, { headers: { 'User-Agent': 'x' } }, function (r) {
      let d = ''
      r.on('data', function (c) { d += c })
      r.on('end', function () { res(JSON.parse(d)) })
    }).on('error', rej)
  })
}
function show(label, text) {
  console.log('\n' + label)
  console.log('  文本: ' + text)
  console.log('  码点: ' + Array.from(text).map(function (ch) {
    const cp = ch.codePointAt(0)
    const hex = 'U+' + cp.toString(16).toUpperCase().padStart(4, '0')
    return /[\u4e00-\u9fff]/.test(ch) ? ch : ch + '(' + hex + ')'
  }).join(' '))
}
;(async function () {
  const a = await get('https://wydlgf.cn/api/guides/mw4-monster-energy-codes')
  const sec = (a.sections || []).filter(function (s) { return (s.bodyMarkdown || '').indexOf('购买前先确认包装') !== -1 })[0]
  const line = String(sec.bodyMarkdown).split(/\r?\n/).filter(function (l) { return l.indexOf('购买前先确认包装') !== -1 })[0]
  show('monster-energy-codes', line)
  const b = await get('https://wydlgf.cn/api/guides/mw4-kd-explained')
  let line2 = ''
  ;(b.sections || []).forEach(function (s) {
    String(s.bodyMarkdown || '').split(/\r?\n/).forEach(function (l) {
      if (l.indexOf('K/D 提升通常需要代打') !== -1) line2 = l
    })
  })
  show('kd-explained', line2)
})().catch(function (e) { console.log('失败', e.message) })
