/**
 * 把官网的迷彩图 / 武器图下载进小程序包，并生成路径映射表。
 *
 *   node tools/sync-images.js
 *
 * 为什么要打包而不是走网络：
 *   小程序正式版要显示网络图片，必须把域名加进「downloadFile 合法域名」。
 *   把图标这类核心视觉资源打包进包里，就不用等域名配置，离线也能看。
 *   （攻略封面这类内容图仍走网络，配好域名后一切正常。）
 *
 * 产物：
 *   miniprogram/images/camo/*.webp     迷彩图
 *   miniprogram/images/weapon/*.webp   武器图
 *   miniprogram/data/image-map.js      官网路径 -> 包内路径
 */
const https = require('https')
const fs = require('fs')
const path = require('path')

const BASE = 'https://wydlgf.cn'
const ROOT = path.join(__dirname, '..')
const snapshot = require(path.join(ROOT, 'miniprogram', 'data', 'weapons.js'))

const OUT_CAMO = path.join(ROOT, 'miniprogram', 'images', 'camo')
const OUT_WEAPON = path.join(ROOT, 'miniprogram', 'images', 'weapon')
const MAP_FILE = path.join(ROOT, 'miniprogram', 'data', 'image-map.js')

function download(url, dest) {
  return new Promise(function (resolve, reject) {
    https.get(url, { headers: { 'User-Agent': 'xinghuo-images' } }, function (res) {
      if (res.statusCode !== 200) {
        res.resume()
        return reject(new Error('HTTP ' + res.statusCode))
      }
      const chunks = []
      res.on('data', function (c) { chunks.push(c) })
      res.on('end', function () {
        const buf = Buffer.concat(chunks)
        // 简单校验一下确实是图片（webp/jpeg/png 的magic number）
        const isWebp = buf.slice(0, 4).toString('ascii') === 'RIFF' && buf.slice(8, 12).toString('ascii') === 'WEBP'
        const isJpg = buf[0] === 0xff && buf[1] === 0xd8
        const isPng = buf.slice(1, 4).toString('ascii') === 'PNG'
        if (!isWebp && !isJpg && !isPng) return reject(new Error('返回的不是图片'))
        fs.mkdirSync(path.dirname(dest), { recursive: true })
        fs.writeFileSync(dest, buf)
        resolve(buf.length)
      })
    }).on('error', reject)
  })
}

;(async function () {
  const games = (snapshot && snapshot.games) || []
  const game = games.filter(function (g) { return g.code === snapshot.defaultGameCode })[0] || games[0]
  if (!game) throw new Error('快照里没有 games[]，先跑 tools/sync-data.js')

  const camoPaths = []
  const tracks = (game.camoTracks && game.camoTracks.modes) || {}
  Object.keys(tracks).forEach(function (k) {
    ;(tracks[k].items || []).forEach(function (it) {
      if (it.image && camoPaths.indexOf(it.image) === -1) camoPaths.push(it.image)
    })
  })
  const weaponPaths = (game.weapons || []).map(function (w) { return w.image }).filter(Boolean)

  const map = {}
  let total = 0
  let failed = []

  console.log('[1] 迷彩图 ' + camoPaths.length + ' 张')
  for (const p of camoPaths) {
    const name = path.basename(p)
    const dest = path.join(OUT_CAMO, name)
    try {
      const size = fs.existsSync(dest) && process.env.SKIP_EXISTING
        ? fs.statSync(dest).size
        : await download(BASE + p, dest)
      map[p] = '/images/camo/' + name
      total += size
      console.log('  ✓ ' + name + '  ' + Math.round(size / 1024) + ' KB')
    } catch (e) {
      failed.push(p + ' (' + e.message + ')')
      console.log('  ✗ ' + name + '  ' + e.message)
    }
  }

  console.log('[2] 武器图 ' + weaponPaths.length + ' 张')
  for (const p of weaponPaths) {
    const name = path.basename(p)
    const dest = path.join(OUT_WEAPON, name)
    try {
      const size = fs.existsSync(dest) && process.env.SKIP_EXISTING
        ? fs.statSync(dest).size
        : await download(BASE + p, dest)
      map[p] = '/images/weapon/' + name
      total += size
      console.log('  ✓ ' + name + '  ' + Math.round(size / 1024) + ' KB')
    } catch (e) {
      failed.push(p + ' (' + e.message + ')')
      console.log('  ✗ ' + name + '  ' + e.message)
    }
  }

  const lines = []
  lines.push('/**')
  lines.push(' * 官网图片路径 -> 小程序包内路径 映射表')
  lines.push(' *')
  lines.push(' * **自动生成，不要手改**：node tools/sync-images.js')
  lines.push(' * catalog.js 会先查这张表：命中的用包内图片（不需要域名、离线可用），')
  lines.push(' * 没命中的才回退到官网地址（需要 downloadFile 合法域名）。')
  lines.push(' */')
  lines.push('module.exports = ' + JSON.stringify(map, null, 2))
  lines.push('')
  fs.writeFileSync(MAP_FILE, lines.join('\n'), 'utf8')

  console.log('\n[3] 已写 ' + path.relative(ROOT, MAP_FILE) + '，' + Object.keys(map).length + ' 条映射')
  console.log('    图片合计 ' + Math.round(total / 1024) + ' KB')
  if (failed.length) {
    console.log('    ⚠ 失败 ' + failed.length + ' 张：')
    failed.forEach(function (f) { console.log('      ' + f) })
    process.exit(1)
  }
})().catch(function (e) { console.error('失败：' + e.message); process.exit(1) })
