/**
 * 进度统计。首页和「我的」页共用，避免两处各算一遍算错。
 * 数据源是 catalog（可能是线上接口，也可能是本地快照），这里不关心来源。
 */
const progress = require('./progress')

/** 按职业分组，返回 [{ cls, weapons: [...] }]，顺序跟官网一致 */
function groupByClass(cat, filterCls) {
  const groups = []
  cat.classes.forEach(function (cls) {
    if (filterCls && cls !== filterCls) return
    const weapons = cat.weapons.filter(function (w) { return w.cls === cls })
    if (weapons.length) groups.push({ cls: cls, weapons: weapons })
  })
  return groups
}

/** 某把武器在某个模式下的完成度 */
function weaponProgress(cat, mode, weaponCode, done) {
  const tiers = mode.tiers.map(function (t) {
    const key = progress.tierKey(mode.code, weaponCode, t.tier)
    return { key: key, tier: t.tier, item: t, on: !!done[key] }
  })
  const count = tiers.filter(function (t) { return t.on }).length
  return { tiers: tiers, count: count, complete: count === mode.tiers.length }
}

/** 某个模式下，第 index 档迷彩一共多少把武器达成了 */
function tierCounts(cat, mode, index, done) {
  let n = 0
  cat.weapons.forEach(function (w) {
    const t = mode.tiers[index]
    if (t && done[progress.tierKey(mode.code, w.code, t.tier)]) n++
  })
  return n
}

function computeStats(cat, done) {
  const map = done || {}

  const modes = cat.modes.map(function (m) {
    let count = 0
    cat.weapons.forEach(function (w) {
      m.tiers.forEach(function (t) {
        if (map[progress.tierKey(m.code, w.code, t.tier)]) count++
      })
    })
    const total = cat.weapons.length * m.tiers.length
    return {
      code: m.code,
      name: m.name,
      short: m.short,
      count: count,
      total: total,
      pct: total ? Math.round((count / total) * 100) : 0
    }
  })

  const eventItems = cat.eventMode ? cat.eventMode.items : []
  const eventDone = eventItems.filter(function (it) {
    return map[progress.eventKey(cat.eventMode.code, it.code)]
  }).length

  const count = modes.reduce(function (sum, m) { return sum + m.count }, 0) + eventDone

  return {
    count: count,
    total: cat.total,
    pct: cat.total ? Math.round((count / cat.total) * 100) : 0,
    modes: modes,
    eventDone: eventDone,
    eventTotal: eventItems.length
  }
}

module.exports = {
  groupByClass,
  weaponProgress,
  tierCounts,
  computeStats
}
