const https = require('https')
function get(url) {
  return new Promise(function (res, rej) {
    https.get(url, { headers: { 'User-Agent': 'Mozilla/5.0' } }, function (r) {
      let d = ''
      r.on('data', function (c) { d += c })
      r.on('end', function () { res(d) })
    }).on('error', rej)
  })
}
;(async function () {
  const d = JSON.parse(await get('https://wydlgf.cn/api/guides/mw4-mastery-camos'))
  console.log('=== guide 字段 ===')
  console.log(Object.keys(d.guide).join(', '))
  console.log('guide.title =', d.guide.title)
  console.log('guide.difficulty =', d.guide.difficulty, '| minutes =', d.guide.estimatedMinutes)
  console.log('guide.tags =', JSON.stringify(d.guide.tags))
  console.log('\n=== sections[' + d.sections.length + '] 第一段的字段 ===')
  console.log(Object.keys(d.sections[0]).join(', '))
  console.log('\n第一段完整内容:')
  console.log(JSON.stringify(d.sections[0], null, 2).slice(0, 1200))
  console.log('\n各段的 type/heading:')
  d.sections.forEach(function (s, i) {
    console.log('  [' + i + '] type=' + s.type + ' heading=' + s.heading + ' | 字段:' + Object.keys(s).join(','))
  })
  console.log('\n=== faqs ===')
  console.log(JSON.stringify(d.faqs && d.faqs.slice ? d.faqs.slice(0, 1) : d.faqs, null, 2).slice(0, 400))
  console.log('\n=== products（不能渲染，只看看） ===')
  console.log(JSON.stringify(d.products && d.products.slice ? d.products.slice(0, 1) : d.products, null, 2).slice(0, 500))
  console.log('\n=== relatedGuides ===')
  console.log(JSON.stringify(d.relatedGuides && d.relatedGuides.slice ? d.relatedGuides.slice(0, 1) : d.relatedGuides, null, 2).slice(0, 400))
})().catch(function (e) { console.log('失败', e.message) })
