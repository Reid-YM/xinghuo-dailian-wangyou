const path = require('path')
const https = require('https')
const ROOT = path.join(__dirname, '..', 'miniprogram')
global.wx = { getStorageSync: function () { return '' }, setStorageSync: function () {} }
const guides = require(path.join(ROOT, 'utils', 'guides.js'))
function get(url) {
  return new Promise(function (res, rej) {
    https.get(url, { headers: { 'User-Agent': 'x' } }, function (r) {
      let d = ''
      r.on('data', function (c) { d += c })
      r.on('end', function () { res(JSON.parse(d)) })
    }).on('error', rej)
  })
}
;(async function () {
  const list = await get('https://wydlgf.cn/api/guides?limit=100')
  console.log('篇数 | 剔除/总段 | 比例 | 状态 | slug')
  for (const g of list) {
    const raw = await get('https://wydlgf.cn/api/guides/' + g.slug)
    const d = guides.cleanDetail(raw)
    const ratio = d.totalBlocks ? (d.droppedBlocks / d.totalBlocks) : 0
    console.log(
      (d.hidden ? '隐藏' : '展示') + ' | ' + d.droppedBlocks + '/' + d.totalBlocks +
      ' | ' + Math.round(ratio * 100) + '% | ' + g.slug
    )
  }
})().catch(function (e) { console.log('失败', e.message) })
