/** 星火网站账号接口。每次使用 wx.login 的一次性 code，由网站服务端向微信校验。 */
const api = require('./api')

function loginCode() {
  return new Promise(function (resolve, reject) {
    wx.login({
      success(res) {
        if (res && res.code) resolve(res.code)
        else reject(new Error('微信身份验证失败，请重试'))
      },
      fail() { reject(new Error('微信身份验证失败，请重试')) }
    })
  })
}

function profile() {
  return loginCode().then(function (code) {
    return api.request('/api/account/wechat-mini/profile', {
      method: 'POST', data: { loginCode: code }
    })
  })
}

function confirm(pairCode) {
  return loginCode().then(function (code) {
    return api.request('/api/account/wechat-mini/confirm', {
      method: 'POST', data: { loginCode: code, code: pairCode }
    })
  })
}

/**
 * 把微信侧授权到的昵称 / 头像同步到**已关联**的网站账号。
 *
 * 有头像文件时走 multipart（wx.uploadFile），纯昵称走 JSON（uploadFile 必须带文件）。
 * 网站侧复用账号头像存储，所以网站与小程序显示的是同一个头像地址。
 */
function syncProfile(options) {
  const opts = options || {}
  const nickName = opts.nickName == null ? '' : String(opts.nickName)
  return loginCode().then(function (code) {
    if (!opts.avatarPath) {
      return api.request('/api/account/wechat-mini/sync-profile', {
        method: 'POST',
        data: { loginCode: code, nickName: nickName }
      })
    }
    return new Promise(function (resolve, reject) {
      wx.uploadFile({
        url: api.base() + '/api/account/wechat-mini/sync-profile',
        filePath: opts.avatarPath,
        name: 'avatar',
        formData: { loginCode: code, nickName: nickName },
        header: { 'X-Requested-With': 'fetch' },
        success(res) {
          let body = {}
          try { body = JSON.parse(res.data || '{}') } catch (e) {}
          if (res.statusCode >= 200 && res.statusCode < 300) resolve(body)
          else reject(new Error((body && body.message) || ('同步失败（' + res.statusCode + '）')))
        },
        fail(err) {
          reject(new Error((err && err.errMsg) || '同步失败，请稍后再试'))
        }
      })
    })
  })
}

/**
 * 手机号快速验证：把 <button open-type="getPhoneNumber"> 拿到的 code 交给网站换手机号。
 * 网站侧用服务端 access_token 调微信，然后存进账号的联系方式（客服可见）。
 */
function syncPhone(phoneCode) {
  return loginCode().then(function (code) {
    return api.request('/api/account/wechat-mini/sync-phone', {
      method: 'POST',
      data: { loginCode: code, phoneCode: phoneCode }
    })
  })
}

module.exports = { profile, confirm, syncProfile, syncPhone }
