// 验证真实云函数与客户端同步层的增删、跨设备恢复及请求期间修改。
const assert = require('assert')
const fs = require('fs')
const path = require('path')
const vm = require('vm')

const root = path.join(__dirname, '..')
const local = new Map()
let document = null
let holdNextSync = false
let releaseSync = null

const db = {
  serverDate: function () { return new Date().toISOString() },
  createCollection: async function () {},
  collection: function () {
    return {
      where: function () { return { limit: function () { return { get: async function () { return { data: document ? [document] : [] } } } } } },
      doc: function () { return { update: async function (opts) { Object.assign(document, opts.data) } } },
      add: async function (opts) { document = Object.assign({ _id: 'one' }, opts.data) }
    }
  }
}
const sdk = {
  DYNAMIC_CURRENT_ENV: 'test',
  init: function () {},
  database: function () { return db },
  getWXContext: function () { return { OPENID: 'user-1' } }
}
const source = fs.readFileSync(path.join(root, 'cloudfunctions/syncProgress/index.js'), 'utf8')
const moduleRef = { exports: {} }
const sandbox = { module: moduleRef, exports: moduleRef.exports, require: function () { return sdk } }
vm.runInNewContext(source, sandbox, { filename: 'syncProgress/index.js' })
const server = sandbox.module.exports.main

global.wx = {
  getStorageSync: function (key) { return local.has(key) ? local.get(key) : '' },
  setStorageSync: function (key, value) { local.set(key, value) },
  removeStorageSync: function (key) { local.delete(key) },
  showToast: function () {},
  cloud: {
    init: function () {},
    callFunction: function (opts) {
      if (opts.name === 'login') return Promise.resolve({ result: { ok: true, openid: 'user-1' } })
      if (holdNextSync) {
        holdNextSync = false
        return new Promise(function (resolve) {
          releaseSync = async function () { resolve({ result: await server(opts.data) }) }
        })
      }
      return server(opts.data).then(function (result) { return { result: result } })
    }
  }
}

const storage = require(path.join(root, 'miniprogram/utils/storage.js'))
const cloud = require(path.join(root, 'miniprogram/utils/cloud.js'))
function sameList(actual, expected, label) {
  assert.strictEqual(JSON.stringify(actual), JSON.stringify(expected), label)
}

;(async function () {
  storage.saveDone({ A: true })
  storage.saveFavs({ F: true })
  await cloud.bootstrap()
  await cloud.login()
  sameList(document.done, ['A'], 'initial local progress uploads')
  assert.strictEqual(document.total, 319, 'cloud receives current catalog total')

  const currentCallFunction = wx.cloud.callFunction
  wx.cloud.callFunction = function () {
    return Promise.resolve({ result: { ok: true, done: ['OLD'], favs: [] } })
  }
  assert.strictEqual(await cloud.syncNow({ silent: true }), null, 'old cloud function is rejected')
  sameList(storage.listDone(), ['A'], 'old cloud response cannot replace local progress')
  wx.cloud.callFunction = currentCallFunction

  storage.saveDone({})
  await cloud.syncNow()
  sameList(document.done, [], 'uncheck deletes remote progress')
  sameList(storage.listDone(), [], 'uncheck stays deleted locally')

  await server({ action: 'sync', protocol: 2, doneAdd: ['B'], favsAdd: [] })
  await cloud.syncNow()
  sameList(storage.listDone(), ['B'], 'other device progress restores')

  await server({ action: 'sync', protocol: 2, doneRemove: ['B'], favsAdd: [] })
  await cloud.syncNow()
  sameList(storage.listDone(), [], 'stale local state does not revive remote deletion')

  holdNextSync = true
  const pending = cloud.syncNow()
  // Let syncNow reach wx.cloud.callFunction before editing local storage.
  await new Promise(function (resolve) { setTimeout(resolve, 0) })
  storage.saveDone({ C: true })
  await releaseSync()
  await pending
  sameList(storage.listDone(), ['C'], 'edit during request survives response')
  await cloud.flush()
  sameList(document.done, ['C'], 'edit during request is uploaded next')

  storage.saveDone({})
  await cloud.resetCloud()
  await cloud.flush()
  sameList(document.done, [], 'clear removes remote progress')
  sameList(document.favs, ['F'], 'clear preserves favorites')

  await server({ action: 'sync', protocol: 2, doneAdd: ['D'], favsAdd: [] })
  holdNextSync = true
  const beforeLogout = cloud.syncNow()
  await new Promise(function (resolve) { setTimeout(resolve, 0) })
  cloud.logout()
  await releaseSync()
  await beforeLogout
  sameList(storage.listDone(), [], 'late response after logout does not change local progress')
  console.log('sync regression passed')
})().catch(function (err) {
  console.error(err)
  process.exitCode = 1
})
