/**
 * 一对一咨询（微信客服）跳转封装。
 *
 * 为什么用它而不是 open-type="contact"：
 *   open-type="contact" 打开的是「小程序客服消息」——用户发消息、你在客服工具里回，
 *   属于留言式；而 wx.openCustomerServiceChat 直接打开你网站用的那套**微信客服会话**，
 *   白班/夜班都能选，和星火网站上的「一对一咨询」是同一个入口。
 *
 * 需要 config/business.js 的 KF.corpId（企业微信企业ID）。
 * 没配时 available() 返回 false，页面会自动退回 open-type="contact"。
 */
const business = require('../config/business')

function config() {
  return business.KF || { corpId: '', channels: [] }
}

function available() {
  const c = config()
  return !!(c.corpId && c.channels && c.channels.length && typeof wx !== 'undefined' && wx.openCustomerServiceChat)
}

/** 选客服：只有一个直接返回；多个弹选择（白班/夜班）；取消返回 null */
function choose() {
  const channels = config().channels || []
  if (!channels.length) return Promise.resolve(null)
  if (channels.length === 1) return Promise.resolve(channels[0])

  return new Promise(function (resolve) {
    wx.showActionSheet({
      itemList: channels.map(function (c) { return c.label || '客服' }),
      success(res) {
        resolve(channels[res.tapIndex] || null)
      },
      fail() {
        resolve(null)
      }
    })
  })
}

/** 打开指定客服会话；channel 为空则用第一个 */
function open(channel) {
  const c = config()
  return new Promise(function (resolve, reject) {
    if (!c.corpId) {
      reject(new Error('还没配置企业微信 corpId，先在 config/business.js 里填上'))
      return
    }
    if (typeof wx.openCustomerServiceChat !== 'function') {
      reject(new Error('当前微信版本不支持直接打开客服，请升级微信后再试'))
      return
    }
    const target = channel || c.channels[0]
    if (!target || !target.url) {
      reject(new Error('客服链接没配置'))
      return
    }
    wx.openCustomerServiceChat({
      extInfo: { url: target.url },
      corpId: c.corpId,
      success() {
        resolve(true)
      },
      fail(err) {
        reject(new Error((err && err.errMsg) || '打开客服失败'))
      }
    })
  })
}

/** 页面统一入口：选客服 -> 打开。返回 Promise */
function contact() {
  return choose().then(function (channel) {
    if (!channel) return null // 用户取消
    return open(channel)
  })
}

module.exports = {
  available,
  choose,
  open,
  contact,
  config
}
