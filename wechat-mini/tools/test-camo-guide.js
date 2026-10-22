/** 完整迷彩挑战（camoGuide）归一化与武器映射回归：不联网，使用内联 fixture。 */
const assert = require('assert')
const path = require('path')

global.wx = {
  getStorageSync() { return '' },
  setStorageSync() {}
}

const root = path.join(__dirname, '..', 'miniprogram')
const catalog = require(path.join(root, 'utils', 'catalog.js'))

const tiers = [1, 2, 3].map(function (n) {
  return {
    code: 'TIER_' + n,
    tier: n,
    name: 'Camo ' + n,
    nameZh: '迷彩' + n,
    requirement: '条件' + n,
    image: ''
  }
})

const payload = {
  schemaVersion: 3,
  defaultGameCode: 'CODMW4',
  storageVersion: 'test',
  games: [{
    code: 'CODMW4',
    shortName: 'MW4',
    name: 'Call of Duty: Modern Warfare 4',
    accent: '',
    dataStatus: '',
    modes: [{ code: 'MULTIPLAYER', name: '多人', shortName: '多人' }],
    camoTracks: {
      version: 'test',
      order: ['MULTIPLAYER'],
      tiersPerMode: 3,
      modes: {
        MULTIPLAYER: {
          code: 'MULTIPLAYER',
          name: '多人迷彩',
          shortName: '多人',
          mastery: true,
          status: 'placeholder',
          items: tiers
        }
      }
    },
    weapons: [
      { code: 'AR1', name: 'AR One', nameZh: '步枪一号', image: '', weaponType: '突击步枪' },
      { code: 'SMG1', name: 'SMG One', nameZh: '冲锋枪一号', image: '', weaponType: '冲锋枪' },
      { code: 'LMG1', name: 'LMG One', nameZh: '机枪一号', image: '', weaponType: '轻机枪' }
    ],
    camoGuide: {
      version: 'v99r1',
      militaryImages: ['/assets/mw4camo/military-1.webp', '/assets/mw4camo/military-2.webp'],
      note: '测试说明',
      classes: {
        '突击步枪': {
          military: { count: 8, text: 'Lv 10/18/25/34/42/50/57/65', estimate: false, image: '/assets/mw4camo/military.webp', levels: [10, 18, 25, 34, 42, 50, 57, 65] },
          special: [
            { name: '特殊迷彩 1', text: 'Lv 25 + 架设 20 杀', estimate: false },
            { name: '特殊迷彩 2', text: 'Lv 46 + 消音器 50 杀', estimate: false },
            { name: '特殊迷彩 3', text: 'Lv 72 + 3 杀不死 ×15', estimate: false }
          ],
          completionist: [
            { name: 'Gold（金）', text: '单局 15 击杀', estimate: false },
            { name: 'Platinum（铂金）', text: '本职业全部金皮后：50 爆头', estimate: false },
            { name: 'Harmonic Break（谐波）', text: '完成本职业 8 个谐波挑战', estimate: false },
            { name: 'Divergence（分歧）', text: '铂金后：25 双杀', estimate: false }
          ]
        },
        '冲锋枪': {
          military: { count: 8, text: '按武器等级解锁（具体等级未公布）', estimate: true },
          special: [{ name: '特殊迷彩 1', text: '等级 + 挑战（未公布）', estimate: true }],
          completionist: [{ name: 'Gold（金）', text: '单局 15 击杀', estimate: false }]
        }
      },
      weapons: {
        SMG1: {
          military: { count: 8, text: '按武器等级解锁：12 / 19 / 25 / 31 / 38 / 44 / 50 / 57 级', estimate: false, image: '/assets/mw4camo/military.webp', levels: [12, 19, 25, 31, 38, 44, 50, 57] },
          special: [
            { name: 'Asphalt（沥青）', text: '22级；多人：瞄准射击击杀 50 名玩家', estimate: false },
            { name: 'Black Topo（黑色地形）', text: '44级；多人：对玩家完成 10 次双杀', estimate: false },
            { name: 'Brown（棕色）', text: '64级；多人：单局不死击杀 3 名玩家，累计 15 次', estimate: false }
          ],
          completionist: [{ name: 'Gold（金）', text: '单局 15 击杀', estimate: false }]
        }
      }
    }
  }]
}

const cat = catalog.normalize(payload, 'remote', Date.now())

assert.ok(cat.camoGuide, 'camoGuide should be parsed')
assert.strictEqual(cat.camoGuide.note, '测试说明')

const ar = cat.camoGuide.classes['突击步枪']
assert.ok(ar, 'AR guide should exist')
assert.strictEqual(ar.groups.length, 3, 'AR should have military/special/completionist groups')
assert.strictEqual(ar.groups[0].key, 'military')
assert.strictEqual(ar.groups[0].title, '军事迷彩 · 8 档')
assert.strictEqual(ar.groups[0].items.length, 8)
assert.strictEqual(ar.groups[0].items[0].image, '/images/camo/military-1.png', 'bundled swatch should map to local path')
assert.strictEqual(ar.groups[0].items[0].name, '军事迷彩 1')
assert.strictEqual(ar.groups[0].items[0].req, '武器等级 10')
assert.strictEqual(ar.militaryTiles.length, 8, 'military should expand to 8 tiles')
assert.strictEqual(ar.militaryTiles[0].label, '10级')
assert.strictEqual(ar.militaryTiles[0].image, '/images/camo/military-1.png')

// 勾选键 = 武器code + '::' + 条目key，同一职业内必须唯一
const tickKeys = ar.groups.reduce(function (acc, g) { return acc.concat(g.items.map(function (i) { return i.key })) }, [])
assert.strictEqual(new Set(tickKeys).size, tickKeys.length, 'tick keys must be unique')
assert.strictEqual(ar.groups[1].title, '特殊迷彩 · 3 档')
assert.strictEqual(ar.groups[1].items.length, 3)
assert.strictEqual(ar.groups[2].title, '完成主义 · 4 档')
assert.strictEqual(ar.groups[2].items.length, 4)
assert.strictEqual(ar.groups[2].items[2].name, 'Harmonic Break（谐波）')

const smg = cat.camoGuide.classes['冲锋枪']
assert.strictEqual(smg.groups[0].items[0].estimate, true, 'estimate flag should survive')
assert.strictEqual(smg.militaryTiles[0].label, '第1档')

const smgWeaponGuide = cat.camoGuide.weapons.SMG1
assert.ok(smgWeaponGuide, 'per-weapon camo guide should be parsed')
assert.strictEqual(smgWeaponGuide.militaryTiles.length, 8)
assert.strictEqual(smgWeaponGuide.militaryTiles[0].label, '12级')
assert.strictEqual(smgWeaponGuide.militaryTiles[0].name, '军事迷彩 1')
assert.strictEqual(smgWeaponGuide.militaryTiles[0].req, '武器等级 12')
assert.strictEqual(smgWeaponGuide.militaryTiles[0].image, '/images/camo/military-1.png', 'per-weapon tiles should use militaryImages')
assert.strictEqual(smgWeaponGuide.militaryTiles[7].label, '57级')
assert.strictEqual(smgWeaponGuide.groups[1].title, '特殊迷彩 · 3 档')
assert.strictEqual(smgWeaponGuide.groups[1].items[0].name, 'Asphalt（沥青）')
assert.strictEqual(smgWeaponGuide.groups[1].items[2].name, 'Brown（棕色）')

// 线上 API 还没更新 camoGuide 时，内置快照的新版本必须胜出
const oldRemote = JSON.parse(JSON.stringify(payload))
oldRemote.games[0].camoGuide.version = 'v1'
const merged = catalog.normalize(oldRemote, 'remote', Date.now())
const snapshot = require(path.join(root, 'data', 'weapons.js'))
const snapshotGame = snapshot.games.filter(function (g) { return g.code === 'CODMW4' })[0]
assert.strictEqual(merged.camoGuide.version, snapshotGame.camoGuide.version)
const kg = snapshotGame.camoGuide.weapons.KG7VULCAN
assert.deepStrictEqual(kg.military.levels, [13, 22, 31, 38, 47, 54, 61, 69])
assert.strictEqual(kg.special[0].name, 'Treeline（林线）')
assert.strictEqual(kg.special[0].text, '腰射10次；DMZ：无伤2次累计30次')
const mergedKg = merged.camoGuide.weapons.KG7VULCAN
assert.strictEqual(mergedKg.militaryTiles[0].name, '军事迷彩 1')
assert.strictEqual(mergedKg.militaryTiles[0].req, '武器等级 13')
const mergedKgSpecial = mergedKg.groups.filter(function (g) { return g.key === 'special' })[0]
assert.strictEqual(mergedKgSpecial.items[0].image, '/images/camo/special-KG7VULCAN-1.jpg')
assert.strictEqual(new Set(mergedKgSpecial.items.map(function (i) { return i.image })).size, 3)
for (const code of ['KG7VULCAN', 'REZI12', 'ISONIGHTSHADE', 'FINNLMG', 'MAR9', 'SANG9MM']) {
  const guide = merged.camoGuide.weapons[code]
  const special = guide.groups.filter(function (g) { return g.key === 'special' })[0]
  assert.strictEqual(new Set(special.items.map(function (i) { return i.image })).size, special.items.length, code + ' special camo images should be distinct')
}
assert.strictEqual(kg.completionist[0].text, '单场比赛15次；DMZ：单场比赛100次')
assert.strictEqual(kg.completionist[1].text, '卧倒75次；DMZ：卧倒150次')
const iso = snapshotGame.camoGuide.weapons.ISONIGHTSHADE
assert.strictEqual(iso.completionist[1].text, '背身25次；DMZ：背身75次')
const sang = snapshotGame.camoGuide.weapons.SANG9MM
assert.strictEqual(sang.special[1].name, 'Snakeskin（蛇皮）')
assert.strictEqual(sang.special[1].text, '不死亡3累计5次；DMZ：不受伤3累计15次')
assert.strictEqual(sang.special[2].name, 'Space Time（时空）')
assert.strictEqual(sang.special[2].text, '双持50次；DMZ：双持125次')
for (const code of ['KG7VULCAN', 'REZI12', 'ISONIGHTSHADE', 'FINNLMG', 'MAR9', 'SANG9MM']) {
  const guide = snapshotGame.camoGuide.weapons[code]
  assert.strictEqual(guide.military.levels.length, 8, code + ' should have 8 military levels')
  assert.strictEqual(guide.special.length, 3, code + ' should have 3 special camos')
  assert.ok(guide.special.every(function (s) { return s.name.indexOf('特殊迷彩') !== 0 }), code + ' should use real special camo names')
  assert.ok(guide.completionist.slice(0, 3).every(function (s) { return s.text.indexOf('DMZ：') >= 0 }), code + ' completionist should include DMZ conditions')
}


// 页面渲染时按武器职业取指南；没有数据的职业应当是 undefined（模板里有 wx:if 保护）
const arWeapon = cat.weapons.filter(function (w) { return w.code === 'AR1' })[0]
const lmgWeapon = cat.weapons.filter(function (w) { return w.code === 'LMG1' })[0]
assert.strictEqual(cat.camoGuide.classes[arWeapon.cls].groups.length, 3)
assert.strictEqual(cat.camoGuide.classes[lmgWeapon.cls], undefined)

// 模式精通条件仍按数据透传
assert.strictEqual(cat.modes[0].tiers[0].requirement, '条件1')

console.log('camoGuide regression passed')