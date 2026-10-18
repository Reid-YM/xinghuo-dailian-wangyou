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
      version: 'v1',
      militaryImages: ['/assets/mw4camo/military-1.webp', '/assets/mw4camo/military-2.webp'],
      note: '测试说明',
      classes: {
        '突击步枪': {
          military: { count: 8, text: 'Lv 10/18/25/34/42/50/57/65', estimate: false, image: '/assets/mw4camo/military.webp', levels: [10, 18, 25, 34, 42, 50, 57, 65] },
          special: [
            { name: '特种迷彩 1', text: 'Lv 25 + 架设 20 杀', estimate: false },
            { name: '特种迷彩 2', text: 'Lv 46 + 消音器 50 杀', estimate: false },
            { name: '特种迷彩 3', text: 'Lv 72 + 3 杀不死 ×15', estimate: false }
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
          special: [{ name: '特种迷彩 1', text: '等级 + 挑战（未公布）', estimate: true }],
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
assert.strictEqual(ar.groups[0].items.length, 1)
assert.strictEqual(ar.groups[0].items[0].image, '/images/camo/military.jpg', 'bundled swatch should map to local path')
assert.strictEqual(ar.militaryTiles.length, 8, 'military should expand to 8 tiles')
assert.strictEqual(ar.militaryTiles[0].label, '10级')
assert.strictEqual(ar.militaryTiles[0].image, '/images/camo/military-1.jpg')
assert.strictEqual(ar.groups[1].title, '特种迷彩 · 3 档')
assert.strictEqual(ar.groups[1].items.length, 3)
assert.strictEqual(ar.groups[2].title, '完成主义 · 4 档')
assert.strictEqual(ar.groups[2].items.length, 4)
assert.strictEqual(ar.groups[2].items[2].name, 'Harmonic Break（谐波）')

const smg = cat.camoGuide.classes['冲锋枪']
assert.strictEqual(smg.groups[0].items[0].estimate, true, 'estimate flag should survive')
assert.strictEqual(smg.militaryTiles[0].label, '第1档')

// 页面渲染时按武器职业取指南；没有数据的职业应当是 undefined（模板里有 wx:if 保护）
const arWeapon = cat.weapons.filter(function (w) { return w.code === 'AR1' })[0]
const lmgWeapon = cat.weapons.filter(function (w) { return w.code === 'LMG1' })[0]
assert.strictEqual(cat.camoGuide.classes[arWeapon.cls].groups.length, 3)
assert.strictEqual(cat.camoGuide.classes[lmgWeapon.cls], undefined)

// 模式精通条件仍按数据透传
assert.strictEqual(cat.modes[0].tiers[0].requirement, '条件1')

console.log('camoGuide regression passed')