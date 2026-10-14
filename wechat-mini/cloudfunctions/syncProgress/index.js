// 云函数：syncProgress
// 作用：迷彩进度的云端存档。读写全部走云函数，所以集合权限可以设成
//      「所有用户不可读写」，比小程序端直连数据库更安全。
//
// 新版同步按客户端相对上次云端快照的增删应用；旧版客户端继续兼容并集。
// 单条记录以 openid 归属，存的是进度的 JSON 数组，不存任何个人敏感信息。
//
// 部署：开发者工具里右键 cloudfunctions/syncProgress → 「上传并部署：云端安装依赖」
const cloud = require('wx-server-sdk')

cloud.init({ env: cloud.DYNAMIC_CURRENT_ENV })
const db = cloud.database()
const COLLECTION = 'camo_progress'
const PROTOCOL_VERSION = 2
const MAX_DONE = 3000
const MAX_FAVS = 500

function cleanList(input, max) {
  const list = Array.isArray(input) ? input : []
  const seen = new Set()
  const out = []
  for (let i = 0; i < list.length && out.length < max; i++) {
    const v = list[i]
    if (typeof v === 'string' && v && !seen.has(v)) {
      seen.add(v)
      out.push(v)
    }
  }
  return out
}

function union(a, b) {
  const seen = new Set()
  const out = []
  const all = (a || []).concat(b || [])
  all.forEach(function (v) {
    if (v && !seen.has(v)) {
      seen.add(v)
      out.push(v)
    }
  })
  return out
}

function applyDelta(current, added, removed, max) {
  const deleted = new Set(cleanList(removed, max))
  const result = cleanList(current, max).filter(function (key) { return !deleted.has(key) })
  const seen = new Set(result)
  cleanList(added, max).forEach(function (key) {
    if (!seen.has(key) && result.length < max) {
      seen.add(key)
      result.push(key)
    }
  })
  return result
}

exports.main = async (event) => {
  const { OPENID } = cloud.getWXContext()
  if (!OPENID) {
    return { ok: false, error: '拿不到 openid：请确认云开发已开通、云函数已部署' }
  }

  const action = event.action || 'sync'
  const col = db.collection(COLLECTION)

  let doc = null
  try {
    const found = await col.where({ openid: OPENID }).limit(1).get()
    doc = found.data[0] || null
  } catch (e) {
    // 集合还不存在：先建出来（注意微信云开发的 add() 不会自动建集合），再当没有记录处理
    try {
      await db.createCollection(COLLECTION)
    } catch (e2) {}
    doc = null
  }

  if (action === 'load') {
    return {
      ok: true,
      done: (doc && doc.done) || [],
      favs: (doc && doc.favs) || []
    }
  }

  if (action === 'reset') {
    if (doc) {
      await col.doc(doc._id).update({
        data: { done: [], favs: [], doneCount: 0, favCount: 0, pct: 0, updatedAt: db.serverDate() }
      })
    }
    return { ok: true }
  }

  if (action === 'profile') {
    const p = event.profile || {}
    const patch = {
      nickName: String(p.nickName || '').slice(0, 32),
      avatarUrl: String(p.avatarUrl || '').slice(0, 512),
      lastSeenAt: db.serverDate()
    }
    if (doc) {
      await col.doc(doc._id).update({ data: patch })
    } else {
      await col.add({
        data: Object.assign({
          openid: OPENID,
          done: [],
          favs: [],
          doneCount: 0,
          pct: 0,
          createdAt: db.serverDate()
        }, patch)
      })
    }
    return { ok: true }
  }

  // 新客户端只发送相对上次同步的变化，旧客户端保留并集合并行为。
  const delta = event.protocol === 2
  const done = delta
    ? applyDelta((doc && doc.done) || [], event.doneAdd, event.doneRemove, MAX_DONE)
    : union((doc && doc.done) || [], cleanList(event.done, MAX_DONE))
  const favs = delta
    ? applyDelta((doc && doc.favs) || [], event.favsAdd, event.favsRemove, MAX_FAVS)
    : union((doc && doc.favs) || [], cleanList(event.favs, MAX_FAVS))
  const total = Number(event.total) || 0

  const payload = {
    done: done,
    favs: favs,
    doneCount: done.length,
    favCount: favs.length,
    pct: total ? Math.min(100, Math.round((done.length / total) * 100)) : 0,
    total: total,
    updatedAt: db.serverDate(),
    lastSeenAt: db.serverDate()
  }

  try {
    if (doc) {
      await col.doc(doc._id).update({ data: payload })
    } else {
      await col.add({
        data: Object.assign({ openid: OPENID, createdAt: db.serverDate() }, payload)
      })
    }
  } catch (e) {
    return {
      ok: false,
      error: '写入失败：' + (e && e.message ? e.message : '未知错误')
    }
  }

  return {
    ok: true,
    protocolVersion: PROTOCOL_VERSION,
    done: done,
    favs: favs,
    total: done.length
  }
}
