/**
 * 星火网站接口客户端。
 *
 * 域名在 config/business.js 的 SITE.base 里，改域名只改那一处。
 * 正式版必须先把域名加进「小程序后台 → 开发设置 → 服务器域名 →
 * request 合法域名 / downloadFile 合法域名」，否则请求会被直接拦掉。
 */
const business = require('../config/business')

function base() {
  return business.SITE.base.replace(/\/+$/, '')
}

/** 把接口返回的相对路径（/assets/xxx.webp）补成绝对地址，图片直接能用 */
function asset(p) {
  if (!p) return ''
  if (/^https?:\/\//.test(p)) return p
  return base() + (p.charAt(0) === '/' ? p : '/' + p)
}

/** GET 一个 JSON 接口；失败时 reject，由调用方决定兜底策略 */
function request(path, options) {
  const opts = options || {}
  return new Promise(function (resolve, reject) {
    if (typeof wx === 'undefined' || typeof wx.request !== 'function') {
      reject(new Error('当前环境没有 wx.request'))
      return
    }
    wx.request({
      url: base() + path,
      method: opts.method || 'GET',
      data: opts.data,
      timeout: business.SITE.timeout,
      header: {
        'content-type': 'application/json',
        'X-Client': 'miniprogram',
        'X-Requested-With': 'fetch'
      },
      success(res) {
        if (res.statusCode >= 200 && res.statusCode < 300) {
          resolve(res.data)
          return
        }
        // 把服务端的报错文案带出来（比如「进度查询正在维护，请联系客服查询」），
        // 比「接口返回 400」对用户友好得多
        const body = res.data || {}
        const err = new Error(body.message || body.error || ('接口返回 ' + res.statusCode))
        err.statusCode = res.statusCode
        err.body = body
        reject(err)
      },
      fail(err) {
        reject(new Error((err && err.errMsg) || '网络异常'))
      }
    })
  })
}

function fetchToolStation() {
  return request('/api/tool-station')
}

function fetchSiteSettings() {
  return request('/api/public/site-settings')
}

module.exports = {
  base,
  asset,
  request,
  fetchToolStation,
  fetchSiteSettings
}
