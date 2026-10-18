// 本机存储层。键名沿用网页版（mw4_camo_v1 / mw4_camo_favs_v1），
// 这样以后网页版老用户的进度可以直接搬过来，schema 不用动。
const KEY_DONE = 'mw4_camo_v1'
const KEY_FAVS = 'mw4_camo_favs_v1'
// 完整迷彩挑战的勾选（按武器+条目，独立于主进度）
const KEY_GUIDE_DONE = 'xh_camo_guide_v1'

function toMap(list) {
  const map = {}
  if (Array.isArray(list)) {
    list.forEach(function (key) {
      if (key) map[key] = true
    })
  }
  return map
}

function toList(map) {
  return Object.keys(map).filter(function (k) { return map[k] })
}

function loadDone() {
  try {
    return toMap(wx.getStorageSync(KEY_DONE))
  } catch (e) {
    return {}
  }
}

function loadFavs() {
  try {
    return toMap(wx.getStorageSync(KEY_FAVS))
  } catch (e) {
    return {}
  }
}

function saveDone(map) {
  try {
    wx.setStorageSync(KEY_DONE, toList(map || {}))
  } catch (e) {}
}

function loadGuideDone() {
  try {
    return toMap(wx.getStorageSync(KEY_GUIDE_DONE))
  } catch (e) {
    return {}
  }
}

function saveGuideDone(map) {
  try {
    wx.setStorageSync(KEY_GUIDE_DONE, toList(map || {}))
  } catch (e) {}
}

function saveFavs(map) {
  try {
    wx.setStorageSync(KEY_FAVS, toList(map || {}))
  } catch (e) {}
}

function listDone() {
  return toList(loadDone())
}

function listFavs() {
  return toList(loadFavs())
}

module.exports = {
  KEY_DONE,
  KEY_FAVS,
  KEY_GUIDE_DONE,
  toMap,
  toList,
  loadDone,
  loadFavs,
  loadGuideDone,
  saveGuideDone,
  saveDone,
  saveFavs,
  listDone,
  listFavs
}
