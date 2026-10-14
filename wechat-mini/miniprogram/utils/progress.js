/**
 * 进度键格式（和网站同源，简单可读）：
 *
 *   多人/战区/DMZ 的枪械迷彩 ->  模式::武器code::档位
 *                              例：MULTIPLAYER::M4::1
 *   Event 类迷彩            ->  模式::迷彩code
 *                              例：PYROCLAST::PYROCLAST_VERGE
 *
 * 键里用 code（M4、HAN86）而不是显示名，避免以后官网把中文名改了导致进度失联。
 */

function tierKey(modeCode, weaponCode, tier) {
  return modeCode + '::' + weaponCode + '::' + tier
}

function eventKey(modeCode, itemCode) {
  return modeCode + '::' + itemCode
}

// 合法的模式代码取自本地快照（和官网接口用的是同一套 code）。
// 官网以后新增模式时，跑 tools/sync-data.js 刷新快照，这里就自动认了。
const snapshot = require('../data/weapons')

const MODE_CODES = (function () {
  const games = (snapshot && snapshot.games) || []
  const game = games.filter(function (g) { return g.code === snapshot.defaultGameCode })[0] || games[0]
  const modes = (game && game.camoTracks && game.camoTracks.modes) || {}
  const perWeapon = []
  const event = []
  Object.keys(modes).forEach(function (code) {
    if (modes[code] && modes[code].mastery) perWeapon.push(code)
    else event.push(code)
  })
  return { perWeapon: perWeapon, event: event }
})()

/** 导入分享码时挡掉乱码 / 手滑粘进来的垃圾串 */
function isValidKey(key) {
  if (typeof key !== 'string' || !key) return false
  const parts = key.split('::')
  // 枪械迷彩：模式::武器code::档位（档位从 1 开始）
  if (parts.length === 3) {
    return MODE_CODES.perWeapon.indexOf(parts[0]) !== -1 &&
      parts[1].length > 0 && /^[1-9][0-9]*$/.test(parts[2])
  }
  // Event 迷彩：模式::迷彩code
  if (parts.length === 2) {
    return MODE_CODES.event.indexOf(parts[0]) !== -1 && /^[A-Z0-9_]+$/.test(parts[1])
  }
  return false
}

module.exports = {
  tierKey,
  eventKey,
  isValidKey
}
