// 云函数：login
// 作用：把调用方的 openid 下发给小程序，作为「登录」凭据。
// 不弹授权框、不要手机号、不要用户资料，用户点一下就登录完成。
//
// 部署：开发者工具里右键 cloudfunctions/login → 「上传并部署：云端安装依赖」
const cloud = require('wx-server-sdk')

cloud.init({ env: cloud.DYNAMIC_CURRENT_ENV })
const db = cloud.database()
const COLLECTION = 'camo_progress'

exports.main = async () => {
  const { OPENID, UNIONID, APPID, ENV } = cloud.getWXContext()
  if (!OPENID) {
    return { ok: false, error: '拿不到 openid：请确认云开发已开通、云函数已部署' }
  }

  /** 集合不存在时自动创建一个（微信云开发的 add() 不会自动建集合） */
  async function createCollectionIfMissing() {
    try {
      await db.createCollection(COLLECTION)
      return true
    } catch (e) {
      return false
    }
  }

  async function upsertUser() {
    const col = db.collection(COLLECTION)
    const found = await col.where({ openid: OPENID }).limit(1).get()
    if (found.data.length) {
      await col.doc(found.data[0]._id).update({
        data: {
          unionid: UNIONID || '',
          lastSeenAt: db.serverDate()
        }
      })
    } else {
      await col.add({
        data: {
          openid: OPENID,
          unionid: UNIONID || '',
          nickName: '',
          avatarUrl: '',
          done: [],
          favs: [],
          doneCount: 0,
          pct: 0,
          createdAt: db.serverDate(),
          lastSeenAt: db.serverDate(),
          updatedAt: db.serverDate()
        }
      })
    }
  }

  try {
    await upsertUser()
  } catch (e) {
    // 集合还不存在：建一次再试（失败也不影响登录本身）
    await createCollectionIfMissing()
    try {
      await upsertUser()
    } catch (e2) {}
  }

  return {
    ok: true,
    openid: OPENID,
    unionid: UNIONID || '',
    appid: APPID,
    env: ENV
  }
}
