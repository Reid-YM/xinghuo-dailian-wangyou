/**
 * 云能力封装：微信登录（openid）+ 迷彩进度云端同步。
 *
 * 三条硬性原则，改之前请先想清楚：
 * 1. 云开发没开通、或者用户没登录时，整个小程序必须照常能用（纯本地模式）；
 * 2. 登录不弹任何授权框 —— openid 由云函数直接下发，用户无感，所以「登录」按钮
 *    的转化率会很高，这也是我们敢在首页直接放登录入口的原因；
 * 3. 同步只上传相对上次云端快照的变化，支持取消勾选和跨设备恢复。
 */
const storage = require('./storage')
const catalog = require('./catalog')
const business = require('../config/business')

const KEY_IDENTITY = 'xh_identity_v1'
const KEY_PROFILE = 'xh_profile_v1'
const KEY_SYNC_AT = 'xh_sync_at_v1'
const KEY_SYNC_BASE = 'xh_sync_base_v2_'
// 只存 openid 后 6 位：网站登录时用来核对「微信识别码尾号」。
// 退出登录时刻意不清它 —— 尾号不是登录凭据，抹掉反而会让网站登录卡在「核对尾号」这一步。
const KEY_TAIL = 'xh_openid_tail_v1'
const PUSH_DELAY = 4000

const state = {
  available: false,
  loggedIn: false,
  openid: '',
  tail: '',
  syncing: false,
  lastSyncAt: 0,
  profile: { nickName: '', avatarUrl: '' },
  hint: ''
}

const listeners = []
let bootPromise = null
let pushTimer = null
let syncPromise = null

function asSet(list) {
  const set = Object.create(null)
  ;(list || []).forEach(function (key) { if (key) set[key] = true })
  return set
}

function difference(a, b) {
  const other = asSet(b)
  return (a || []).filter(function (key) { return !other[key] })
}

function applyChanges(base, added, removed) {
  const result = asSet(base)
  ;(removed || []).forEach(function (key) { delete result[key] })
  ;(added || []).forEach(function (key) { result[key] = true })
  return result
}

function readSyncBase(openid) {
  try {
    const value = wx.getStorageSync(KEY_SYNC_BASE + openid)
    if (value && Array.isArray(value.done) && Array.isArray(value.favs)) return value
  } catch (e) {}
  return { done: [], favs: [] }
}

function writeSyncBase(openid, done, favs) {
  try {
    wx.setStorageSync(KEY_SYNC_BASE + openid, { done: done, favs: favs })
  } catch (e) {}
}

/* -------------------------------------------------------------- 状态订阅 */

function snapshot() {
  return Object.assign({}, state, { profile: Object.assign({}, state.profile) })
}

function notify() {
  const s = snapshot()
  listeners.forEach(function (fn) {
    try {
      fn(s)
    } catch (e) {}
  })
}

/** 页面在 onLoad 里订阅，onUnload 里调用返回的函数退订。 */
function subscribe(fn) {
  if (typeof fn !== 'function') return function () {}
  listeners.push(fn)
  fn(snapshot())
  return function () {
    const i = listeners.indexOf(fn)
    if (i >= 0) listeners.splice(i, 1)
  }
}

function getState() {
  return snapshot()
}

/* -------------------------------------------------------------- 本机缓存 */

function readCache() {
  try {
    const id = wx.getStorageSync(KEY_IDENTITY)
    if (id && id.openid) {
      state.openid = id.openid
      state.loggedIn = true
    }
  } catch (e) {}
  try {
    state.tail = String(wx.getStorageSync(KEY_TAIL) || '')
  } catch (e) {}
  if (!state.tail && state.openid) state.tail = String(state.openid).slice(-6)
  try {
    const p = wx.getStorageSync(KEY_PROFILE)
    if (p) {
      state.profile = { nickName: p.nickName || '', avatarUrl: p.avatarUrl || '' }
    }
  } catch (e) {}
  try {
    state.lastSyncAt = Number(wx.getStorageSync(KEY_SYNC_AT)) || 0
  } catch (e) {}
}

function writeIdentity(openid) {
  try {
    wx.setStorageSync(KEY_IDENTITY, { openid: openid, at: Date.now() })
  } catch (e) {}
  try {
    state.tail = String(openid || '').slice(-6)
    wx.setStorageSync(KEY_TAIL, state.tail)
  } catch (e) {}
}

function writeProfile(profile) {
  try {
    wx.setStorageSync(KEY_PROFILE, profile)
  } catch (e) {}
}

function writeSyncAt(ts) {
  try {
    wx.setStorageSync(KEY_SYNC_AT, ts)
  } catch (e) {}
}

/* -------------------------------------------------------------- 初始化 */

/** 在 app.onLaunch 里调用一次；页面里也可以直接调，内部做了幂等。 */
function bootstrap() {
  if (bootPromise) return bootPromise
  readCache()
  bootPromise = new Promise(function (resolve) {
    if (!wx.cloud || typeof wx.cloud.callFunction !== 'function') {
      state.available = false
      state.hint = '未开通云开发：进度只存在这台手机上'
      notify()
      resolve(false)
      return
    }
    try {
      wx.cloud.init(business.CLOUD_ENV ? { env: business.CLOUD_ENV, traceUser: true } : { traceUser: true })
      state.available = true
      state.hint = state.loggedIn ? '' : '登录后进度存到微信，换手机不丢'
    } catch (e) {
      state.available = false
      state.hint = '云开发初始化失败：进度只存在这台手机上'
    }
    notify()
    resolve(state.available)
  })
  return bootPromise
}

function ready() {
  return bootstrap()
}

function call(name, data) {
  return wx.cloud.callFunction({ name: name, data: data || {} }).then(function (res) {
    return res && res.result
  })
}

/** 把云开发的报错翻译成人话，方便照着「上线清单」一步步排错。 */
function describeCloudError(err) {
  const msg = String((err && (err.errMsg || err.message)) || err || '')
  if (err && err.code === 'PROTOCOL_UNSUPPORTED') {
    return '云端同步版本过旧，请更新云函数后重试'
  }
  if (/FunctionName|-504002|function not found|not found|云函数/i.test(msg)) {
    return '云函数还没部署：开发者工具里右键 cloudfunctions 目录 → 上传并部署'
  }
  if (/env|环境|-501000|invalid/i.test(msg)) {
    return '云开发环境还没开通：点开发者工具左上角「云开发」开通后重试'
  }
  return '网络异常，请稍后再试'
}

/* -------------------------------------------------------------- 登录 */

/**
 * 登录。opts.silent = true 时失败不写状态提示（用在「进小程序自动登录」上，
 * 免得云开发没开通的用户一进来就看到一句报错）。
 */
function login(options) {
  const opts = options || {}
  if (state.loggedIn) return Promise.resolve(true)
  return ready().then(function (ok) {
    if (!ok) return Promise.reject(new Error(state.hint || '云开发不可用'))
    state.syncing = true
    notify()
    return call('login', {}).then(function (result) {
      if (!result || !result.ok || !result.openid) {
        throw new Error((result && result.error) || '登录失败，请稍后再试')
      }
      state.openid = result.openid
      state.loggedIn = true
      state.hint = ''
      writeIdentity(result.openid)
      notify()
      // 登录成功立刻双向合并一次：本机已有的进度会被带上云
      return syncNow({ silent: true }).then(function () {
        return true
      })
    })
  }).catch(function (err) {
    state.syncing = false
    const msg = describeCloudError(err)
    err.userMessage = msg
    if (!opts.silent) state.hint = msg
    notify()
    throw err
  })
}

/** 只清本机身份，本机进度全部保留（游客进度不会因为退出登录被清掉）。 */
function logout() {
  if (pushTimer) {
    clearTimeout(pushTimer)
    pushTimer = null
  }
  state.loggedIn = false
  state.openid = ''
  state.syncing = false
  state.lastSyncAt = 0
  state.hint = '已退出登录：进度仍然保存在这台手机上'
  try {
    wx.removeStorageSync(KEY_IDENTITY)
  } catch (e) {}
  try {
    wx.removeStorageSync(KEY_SYNC_AT)
  } catch (e) {}
  notify()
}

/* -------------------------------------------------------------- 同步 */

/**
 * 双向同步：只上传相对上次云端快照的增删，避免其他设备的旧进度复活。
 * 返回 { restored, total, done, favs }，restored = 从云端找回本机缺的条数。
 */
function syncNow(options) {
  const opts = options || {}
  if (syncPromise) return syncPromise
  if (!state.loggedIn) {
    if (!opts.silent) wx.showToast({ title: '请先登录', icon: 'none' })
    return Promise.resolve(null)
  }

  let sent = null
  const syncOpenid = state.openid
  syncPromise = ready()
    .then(function (ok) {
      if (!ok) return null
      state.syncing = true
      state.hint = ''
      notify()
      const base = readSyncBase(syncOpenid)
      const done = storage.listDone()
      const favs = storage.listFavs()
      sent = { done: done, favs: favs }
      return call('syncProgress', {
        action: 'sync',
        protocol: 2,
        // 旧版云函数仍读 done/favs；先部署新版前不会丢已有进度。
        done: done,
        favs: favs,
        doneAdd: difference(done, base.done),
        doneRemove: difference(base.done, done),
        favsAdd: difference(favs, base.favs),
        favsRemove: difference(base.favs, favs),
        total: catalog.currentTotal()
      })
    })
    .then(function (result) {
      if (!result) return null
      if (!result.ok) throw new Error(result.error || '同步失败')
      if (result.protocolVersion !== 2) {
        const err = new Error('云端同步协议不匹配')
        err.code = 'PROTOCOL_UNSUPPORTED'
        throw err
      }
      if (!state.loggedIn || state.openid !== syncOpenid) return null

      const serverDone = Array.isArray(result.done) ? result.done : []
      const serverFavs = Array.isArray(result.favs) ? result.favs : []
      const currentDone = storage.listDone()
      const currentFavs = storage.listFavs()
      // 请求途中用户继续勾选或取消时，保留这些尚未发出的改动。
      const lateDoneAdd = difference(currentDone, sent.done)
      const lateDoneRemove = difference(sent.done, currentDone)
      const lateFavsAdd = difference(currentFavs, sent.favs)
      const lateFavsRemove = difference(sent.favs, currentFavs)
      const doneMap = applyChanges(serverDone, lateDoneAdd, lateDoneRemove)
      const favMap = applyChanges(serverFavs, lateFavsAdd, lateFavsRemove)
      const restored = difference(serverDone, sent.done).length + difference(serverFavs, sent.favs).length
      storage.saveDone(doneMap)
      storage.saveFavs(favMap)
      writeSyncBase(syncOpenid, serverDone, serverFavs)
      if (lateDoneAdd.length || lateDoneRemove.length || lateFavsAdd.length || lateFavsRemove.length) {
        queuePush()
      }

      state.lastSyncAt = Date.now()
      state.syncing = false
      writeSyncAt(state.lastSyncAt)
      notify()

      return {
        restored: restored,
        total: result.total || 0,
        done: doneMap,
        favs: favMap
      }
    })
    .catch(function (err) {
      state.syncing = false
      state.hint = '同步失败：' + describeCloudError(err)
      notify()
      if (!opts.silent) wx.showToast({ title: state.hint, icon: 'none' })
      return null
    })
    .then(function (res) {
      syncPromise = null
      return res
    })

  return syncPromise
}

/** 勾选之后延迟推送，避免用户连点十下就发十次请求。 */
function queuePush() {
  if (!state.loggedIn) return
  if (pushTimer) clearTimeout(pushTimer)
  pushTimer = setTimeout(function () {
    pushTimer = null
    syncNow({ silent: true })
  }, PUSH_DELAY)
}

/** 退到后台 / 离开页面时立刻结算，避免用户杀进程丢进度。 */
function flush() {
  if (pushTimer) {
    clearTimeout(pushTimer)
    pushTimer = null
  }
  if (state.loggedIn && state.available) return syncNow({ silent: true })
  return Promise.resolve(null)
}

/** 清空进度通过增量同步发送删除记录，收藏保持不变。 */
function resetCloud() {
  if (!state.loggedIn || !state.available) return Promise.resolve(false)
  queuePush()
  return Promise.resolve(true)
}

/* -------------------------------------------------------------- 头像昵称 */

function saveProfile(profile) {
  const next = {
    nickName: (profile && profile.nickName) || state.profile.nickName || '',
    avatarUrl: (profile && profile.avatarUrl) || state.profile.avatarUrl || ''
  }
  state.profile = next
  writeProfile(next)
  notify()
  if (state.loggedIn && state.available) {
    call('syncProgress', { action: 'profile', profile: next }).catch(function () {})
  }
  return next
}

/** 把用户选的头像传到云存储，返回 fileID；云开发不可用时直接失败，由调用方提示。 */
function uploadAvatar(filePath) {
  if (!state.available) return Promise.reject(new Error('云开发未开通'))
  const parts = String(filePath).split('.')
  const ext = parts.length > 1 ? parts[parts.length - 1].toLowerCase() : 'png'
  const cloudPath = 'avatars/' + (state.openid || 'anon') + '-' + Date.now() + '.' + ext
  return wx.cloud.uploadFile({ cloudPath: cloudPath, filePath: filePath }).then(function (res) {
    return res && res.fileID
  })
}

module.exports = {
  describeCloudError,
  bootstrap,
  ready,
  getState,
  subscribe,
  login,
  logout,
  syncNow,
  queuePush,
  flush,
  resetCloud,
  saveProfile,
  uploadAvatar
}
