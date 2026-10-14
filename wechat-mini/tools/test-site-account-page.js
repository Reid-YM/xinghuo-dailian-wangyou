/** 「我的」页的网站账号状态回归：成功、不可用、网络失败和请求乱序。 */
const assert = require('assert')
const path = require('path')

global.wx = {}
global.Page = function (definition) { global.pageDefinition = definition }

const root = path.join(__dirname, '..', 'miniprogram')
const siteAccountPath = path.join(root, 'utils', 'site-account.js')
const requests = []
const confirmations = []
let confirmationError = null
require.cache[require.resolve(siteAccountPath)] = {
  id: siteAccountPath,
  filename: siteAccountPath,
  loaded: true,
  exports: {
    profile() {
      return new Promise(function (resolve, reject) { requests.push({ resolve, reject }) })
    },
    confirm(code) {
      confirmations.push(code)
      if (confirmationError) return Promise.reject(confirmationError)
      return Promise.resolve({ linked: true })
    }
  }
}
require(path.join(root, 'pages', 'me', 'me.js'))

const page = Object.create(global.pageDefinition)
page.data = JSON.parse(JSON.stringify(global.pageDefinition.data))
page.setData = function (patch) {
  for (const key of Object.keys(patch)) {
    const parts = key.split('.')
    let target = this.data
    for (let i = 0; i < parts.length - 1; i++) target = target[parts[i]]
    target[parts[parts.length - 1]] = patch[key]
  }
}

function flush() { return new Promise(function (resolve) { setImmediate(resolve) }) }

;(async function () {
  page.refreshWebsite()
  assert.strictEqual(page.data.website.loading, true)
  requests.shift().resolve({ linked: false })
  await flush()
  assert.strictEqual(page.data.website.checked, true)
  assert.strictEqual(page.data.website.linked, false)
  assert.strictEqual(page.data.website.error, '')

  page.refreshWebsite()
  requests.shift().resolve({ linked: true, displayName: '星火用户', avatarUrl: '', userId: 'XH1001', orderCount: 2 })
  await flush()
  assert.strictEqual(page.data.displayName, '星火用户')
  assert.strictEqual(page.data.website.orderCount, 2)

  page.refreshWebsite()
  requests.shift().reject(new Error('network'))
  await flush()
  assert.strictEqual(page.data.website.available, true)
  assert.match(page.data.website.error, /网络/)
  assert.strictEqual(page.data.website.linked, true)

  page.onRetryWebsite()
  const missing = new Error('not found')
  missing.statusCode = 404
  requests.shift().reject(missing)
  await flush()
  assert.strictEqual(page.data.website.available, false)
  assert.match(page.data.website.error, /服务/)

  page.refreshWebsite()
  const old = requests.shift()
  page.refreshWebsite()
  const current = requests.shift()
  current.resolve({ linked: false })
  await flush()
  old.resolve({ linked: true, displayName: '过期结果', userId: 'OLD' })
  await flush()
  assert.strictEqual(page.data.website.linked, false)
  assert.strictEqual(page.data.website.available, true)

  const notices = []
  const errors = []
  wx.showLoading = function () {}
  wx.hideLoading = function () {}
  wx.showToast = function (value) { notices.push(value.title) }
  wx.showModal = function (value) { errors.push(value.content) }
  wx.scanCode = function (value) { value.success({ result: 'xingwei-login:12345678' }) }
  page.scanWebsiteLogin()
  assert.deepStrictEqual(confirmations, ['12345678'])
  await flush()
  requests.shift().resolve({ linked: true, displayName: '星火用户', userId: 'XH1001' })
  await flush()
  assert.strictEqual(page.data.website.linked, true)

  wx.scanCode = function (value) { value.success({ result: 'https://invalid.example/12345678' }) }
  page.scanWebsiteLogin()
  assert.deepStrictEqual(confirmations, ['12345678'])
  assert.ok(notices.includes('这不是网站登录二维码'))

  wx.scanCode = function (value) { value.fail({ errMsg: 'scanCode:fail cancel' }) }
  page.scanWebsiteLogin()
  assert.deepStrictEqual(confirmations, ['12345678'])
  assert.ok(!notices.includes('扫码失败，请输入登录码'))

  wx.scanCode = function (value) { value.success({ result: 'xingwei-login:1234567' }) }
  page.scanWebsiteLogin()
  assert.deepStrictEqual(confirmations, ['12345678'])

  confirmationError = new Error('登录码无效或已过期，请在网站重新获取')
  wx.scanCode = function (value) { value.success({ result: 'xingwei-login:87654321' }) }
  page.scanWebsiteLogin()
  await flush()
  assert.deepStrictEqual(confirmations, ['12345678', '87654321'])
  assert.ok(errors.includes('登录码无效或已过期，请在网站重新获取'))
  console.log('网站账号页面状态回归通过')
})().catch(function (error) {
  console.error(error)
  process.exitCode = 1
})
