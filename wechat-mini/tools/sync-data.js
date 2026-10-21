/**
 * 数据同步脚本：把星火网站的工具站数据抓成小程序的离线兜底快照。
 *
 *   node tools/sync-data.js
 *
 * 为什么需要它：
 *   小程序运行时会优先请求 https://wydlgf.cn/api/tool-station 拿最新数据，
 *   但在「合法域名还没配置」「用户断网」这些情况下会退回用本文件生成的快照。
 *   快照和新数据用的是同一套结构，所以两条路径的行为完全一致。
 *
 * 什么时候要重跑：
 *   官网公布新武器/新迷彩、或调整挑战要求之后，重跑一次并提交新版本。
 */
const https = require('https')
const fs = require('fs')
const path = require('path')

const API = 'https://wydlgf.cn/api/tool-station'
const OUT = path.join(__dirname, '..', 'miniprogram', 'data', 'weapons.js')
const LOCAL_FILE = process.argv[2] === '--file' ? path.resolve(process.argv[3] || '') : ''

function guideVersionRank(version) {
  const m = String(version || '').match(/(\d+)r(\d+)/i)
  return m ? Number(m[1]) * 1000 + Number(m[2]) : 0
}

function fetchJson(url) {
  return new Promise(function (resolve, reject) {
    https.get(url, { headers: { 'User-Agent': 'xinghuo-miniprogram-sync' } }, function (res) {
      let body = ''
      res.on('data', function (c) { body += c })
      res.on('end', function () {
        if (res.statusCode !== 200) return reject(new Error('HTTP ' + res.statusCode))
        try {
          resolve(JSON.parse(body))
        } catch (e) {
          reject(new Error('返回的不是合法 JSON：' + e.message))
        }
      })
    }).on('error', reject)
  })
}

;(async function () {
  const source = LOCAL_FILE || API
  console.log((LOCAL_FILE ? '读取 ' : '拉取 ') + source)
  const json = LOCAL_FILE ? JSON.parse(fs.readFileSync(LOCAL_FILE, 'utf8')) : await fetchJson(API)

  const game = (json.games || []).find(function (g) { return g.code === json.defaultGameCode }) || (json.games || [])[0]
  if (!game || !game.camoTracks) throw new Error('返回结构变了：找不到 games[]/camoTracks')

  // 线上接口可能还没更新 camoGuide：保留快照中版本更高的完整迷彩数据
  let previous = null
  try { previous = require(OUT) } catch (e) {}
  const previousGame = previous && ((previous.games || []).find(function (g) { return g.code === (previous.defaultGameCode || json.defaultGameCode) }) || (previous.games || [])[0])
  if (previousGame && previousGame.camoGuide && game.camoGuide &&
      guideVersionRank(previousGame.camoGuide.version) > guideVersionRank(game.camoGuide.version)) {
    game.camoGuide = previousGame.camoGuide
    console.log('  保留快照中更新的 camoGuide：' + game.camoGuide.version)
  }

  const weapons = game.weapons || []
  const modes = (game.camoTracks.order || []).map(function (k) { return game.camoTracks.modes[k] }).filter(Boolean)
  const perWeapon = modes.filter(function (m) { return m.mastery })
  const total = weapons.length * perWeapon.length * (game.camoTracks.tiersPerMode || 3) +
    modes.filter(function (m) { return !m.mastery }).reduce(function (n, m) { return n + (m.items || []).length }, 0)

  const header = [
    '/**',
    ' * 武器 / 迷彩数据快照（离线兜底用）—— **这个文件是自动生成的，不要手改**。',
    ' *',
    ' * 来源：' + (LOCAL_FILE ? '本地 ' + path.basename(LOCAL_FILE) + '（与 ' + API + ' 同结构）' : API),
    ' * 生成：node tools/sync-data.js' + (LOCAL_FILE ? ' --file <本地 JSON>' : ''),
    ' * camoTracks 版本：' + game.camoTracks.version + ' · camoGuide 版本：' + ((game.camoGuide && game.camoGuide.version) || '-') + ' · storageVersion：' + json.storageVersion,
    ' *',
    ' * 运行时优先用线上接口（改了网站就立刻生效）；接口不可用时才用这份快照。',
    ' * 官方公布新武器 / 新迷彩后，重跑同步脚本并提交新版本即可。',
    ' */',
    ''
  ].join('\n')

  const body = 'module.exports = ' + JSON.stringify(json, null, 2) + '\n'
  fs.writeFileSync(OUT, header + body, { encoding: 'utf8' })

  console.log('已写入 ' + path.relative(path.join(__dirname, '..'), OUT))
  console.log('  武器 ' + weapons.length + ' 把 · 职业 ' + new Set(weapons.map(function (w) { return w.weaponType })).size + ' 个')
  console.log('  模式 ' + modes.map(function (m) { return m.shortName || m.code }).join('/'))
  console.log('  进度项合计 ' + total + ' 项')
  const noImage = weapons.filter(function (w) { return !w.image }).map(function (w) { return w.name })
  if (noImage.length) console.log('  ⚠ 官网这几把武器还没有图：' + noImage.join(', '))
})().catch(function (e) {
  console.error('同步失败：' + e.message)
  process.exit(1)
})
