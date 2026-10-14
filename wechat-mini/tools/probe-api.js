/**
 * 官网接口结构探测：直接看 /api/tool-station 长什么样。
 * 接口字段变了、或者想知道谜彩/模式结构时，跑：
 *
 *   node tools/probe-api.js
 *
 * 只读，不改任何文件。
 */
const https = require('https')
function get(url) {
  return new Promise((res, rej) => {
    https.get(url, { headers: { 'User-Agent': 'Mozilla/5.0' } }, r => {
      let d = ''
      r.on('data', c => d += c)
      r.on('end', () => res(d))
    }).on('error', rej)
  })
}
;(async () => {
  const j = JSON.parse(await get('https://wydlgf.cn/api/tool-station'))
  const ct = j.games[0].camoTracks
  console.log('camoTracks 的 modes 键:', Object.keys(ct.modes).join(', '))
  const p = ct.modes.PYROCLAST
  console.log('\n=== PYROCLAST ===')
  console.log(JSON.stringify(p, null, 2).slice(0, 2000))
  console.log('\n=== 每个 mode 的 items 数量 / 是否 per-weapon ===')
  Object.keys(ct.modes).forEach(k => {
    const m = ct.modes[k]
    console.log(`  ${k}: name=${m.name} items=${(m.items||[]).length} 字段=${Object.keys(m).join(',')}`)
  })
  console.log('\n=== dataStatus / schemaVersion ===')
  console.log('schemaVersion=' + j.schemaVersion, '| storageVersion=' + j.storageVersion, '| camoTracks.version=' + ct.version)
  console.log('dataStatus=' + j.games[0].dataStatus, '| status=' + ct.modes.MULTIPLAYER.status)
  console.log('\n=== 谜题：MULTIPLAYER items[0] 的完整字段 ===')
  console.log(JSON.stringify(ct.modes.MULTIPLAYER.items[0], null, 2))
})().catch(e => console.log('失败', e.message))
