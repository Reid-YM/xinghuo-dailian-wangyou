const fs = require('fs')
const path = require('path')
const ROOT = path.join(__dirname, '..', 'miniprogram')

// 按页面真实取数路径走一遍，检查图片路径对不对、文件在不在
global.wx = { getStorageSync: function () { return '' }, setStorageSync: function () {}, removeStorageSync: function () {} }
const catalog = require(path.join(ROOT, 'utils', 'catalog.js'))

catalog.load().then(function (cat) {
  console.log('数据来源: ' + cat.source)
  const checks = []
  cat.modes.forEach(function (m) { m.tiers.forEach(function (t) { checks.push(['迷彩', t.name, t.image]) }) })
  if (cat.eventMode) cat.eventMode.items.forEach(function (i) { checks.push(['迷彩', i.name, i.image]) })
  cat.weapons.slice(0, 3).forEach(function (w) { checks.push(['武器', w.name, w.image]) })

  // 完整迷彩指南里的图片也逐个核对（按路径去重，避免重复刷屏）
  const seenCamoImages = {}
  function addCamoImage(kind, name, image) {
    if (!image || seenCamoImages[image]) return
    seenCamoImages[image] = true
    checks.push([kind, name, image])
  }
  if (cat.camoGuide) {
    Object.keys(cat.camoGuide.weapons || {}).forEach(function (code) {
      const guide = cat.camoGuide.weapons[code]
      ;(guide.militaryTiles || []).forEach(function (t) { addCamoImage('军事迷彩', code + ' ' + (t.name || t.label), t.image) })
      ;(guide.groups || []).forEach(function (g) {
        ;(g.items || []).forEach(function (i) { addCamoImage('迷彩', code + ' ' + i.name, i.image) })
      })
    })
  }

  let bad = 0
  checks.forEach(function (c) {
    const isLocal = c[2].indexOf('/images/') === 0
    const exists = isLocal ? fs.existsSync(path.join(ROOT, c[2])) : null
    if (!isLocal || exists === false) bad++
    console.log('  ' + (isLocal ? (exists ? '✓' : '✗') : '!') + ' [' + c[0] + '] ' + c[1] + ' -> ' + c[2])
  })
  console.log('\n共 ' + checks.length + ' 个，异常 ' + bad + ' 个')
  console.log('检查结论: ' + (bad === 0 ? '映射与文件都正确 ✅' : '有问题 ❌'))
})
