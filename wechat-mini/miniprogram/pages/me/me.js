const storage = require('../../utils/storage')
const catalog = require('../../utils/catalog')
const progress = require('../../utils/progress')
const stats = require('../../utils/stats')
const cloud = require('../../utils/cloud')
const business = require('../../config/business')
const kf = require('../../utils/kf')
const format = require('../../utils/format')
const siteAccount = require('../../utils/site-account')

function nameInitial(name) {
  return name && name.trim() ? name.trim().slice(0, 1) : '星'
}

Page({
  data: {
    brand: business.BRAND,
    copy: business.serviceCopy(),
    kfReady: kf.available(),
    kfButtonText: (business.KF && business.KF.buttonText) || '一对一咨询',
    version: business.VERSION,
    icp: business.ICP,
    hasIcp: !!business.ICP,
    sessionFrom: '',

    cloud: {
      available: false,
      loggedIn: false,
      syncing: false,
      syncText: '还没同步过',
      hint: ''
    },
    profile: { nickName: '', avatarUrl: '' },
    avatarText: '星',
    accountText: '',
    identityHint: '',
    displayName: '微信用户',
    displayAvatar: '',
    website: { linked: false, loading: false, available: true, checked: false, error: '', displayName: '', avatarUrl: '', userId: '', orderCount: 0 },
    phoneMasked: '',
    summary: { count: 0, total: 0, pct: 0, modes: [], eventDone: 0, eventTotal: 0 },
    dataSource: '',
    weaponCount: 0
  },

  onLoad() {
    try { this.setData({ phoneMasked: wx.getStorageSync('xh_phone_masked_v1') || '' }) } catch (e) {}
    const that = this
    this.cat = null
    this._unsub = cloud.subscribe(this.applyCloudState.bind(this))
    this.ready = catalog.load().then(function (cat) {
      that.cat = cat
      that.setData({
        dataSource: that.describeSource(cat),
        weaponCount: cat.weapons.length
      })
      that.refresh()
      return cat
    })
  },

  describeSource(cat) {
    if (cat.source === 'remote') return '星火官网工具站 ' + (cat.version || '')
    if (cat.source === 'cache') return '本机缓存'
    return '离线快照模式'
  },

  onShow() {
    this.refresh()
    this.refreshWebsite()
  },

  onUnload() {
    this._websiteRequestId = (this._websiteRequestId || 0) + 1
    if (this._unsub) this._unsub()
  },

  /* ------------------------------------------------------------ 云状态 */

  applyCloudState(state) {
    const name = this.data.website.linked && this.data.website.displayName
      ? this.data.website.displayName : (state.profile.nickName || '微信用户')
    this.setData({
      cloud: {
        available: state.available,
        loggedIn: state.loggedIn,
        syncing: state.syncing,
        syncText: format.syncStatus(state),
        hint: state.hint
      },
      profile: state.profile,
      avatarText: nameInitial(name),
      displayName: name,
      displayAvatar: this.data.website.linked
        ? (this.data.website.avatarUrl || state.profile.avatarUrl || '')
        : (state.profile.avatarUrl || ''),
      accountText: this.data.website.linked ? '星火账号 ' + this.data.website.userId
        : (state.loggedIn ? '微信进度已连接' : '本机进度'),
      // 尾号优先用云状态里记住的那 6 位：退出登录后 openid 被清掉，
      // 但网站登录仍要用它对「微信识别码尾号」，所以不能用 openid 的有无来决定。
      identityHint: state.tail || (state.openid ? String(state.openid).slice(-6) : '')
    })
    this.refresh()
  },

  refreshWebsite() {
    const that = this
    const requestId = this._websiteRequestId = (this._websiteRequestId || 0) + 1
    this.setData({ 'website.loading': true, 'website.error': '' })
    siteAccount.profile().then(function (result) {
      if (requestId !== that._websiteRequestId) return
      const linked = !!(result && result.linked)
      that.setData({
        website: {
          linked: linked, loading: false, available: true, checked: true, error: '',
          displayName: linked ? result.displayName : '',
          avatarUrl: linked ? result.avatarUrl : '',
          userId: linked ? result.userId : '',
          orderCount: linked ? Number(result.orderCount) || 0 : 0
        },
        displayName: linked ? result.displayName : (that.data.profile.nickName || '微信用户'),
        displayAvatar: (linked ? result.avatarUrl : '') || that.data.profile.avatarUrl || '',
        avatarText: nameInitial(linked ? result.displayName : that.data.profile.nickName),
        accountText: linked ? '星火账号 ' + result.userId
          : (that.data.cloud.loggedIn ? '微信进度已连接' : '本机进度')
      })
    }).catch(function (err) {
      if (requestId !== that._websiteRequestId) return
      // 5xx、以及网站明说的「微信服务暂不可用」，都属于服务端问题，不要误报成用户网络问题
      const code = err && err.body && err.body.error
      const unavailable = !!(
        err && (
          err.statusCode === 404 ||
          err.statusCode >= 500 ||
          code === 'WECHAT_SERVICE_UNAVAILABLE' ||
          code === 'SERVICE_UNAVAILABLE'
        )
      )
      that.setData({
        'website.loading': false,
        'website.available': !unavailable,
        'website.checked': true,
        'website.error': unavailable
          ? '网站账号服务暂不可用，请稍后重试。'
          : '暂时无法读取网站账号，请检查网络后重试。'
      })
    })
  },

  onRetryWebsite() {
    if (!this.data.website.loading) this.refreshWebsite()
  },

  onPairWebsite() {
    const that = this
    wx.showModal({
      title: '登录星火网站',
      // 注意：editable 弹窗里的 content 会变成输入框的初始文字，用户得先删掉才能输码，
      // 所以这里不放说明文案；说明放在页面卡片上。
      editable: true,
      placeholderText: '输入网站显示的 8 位登录码',
      success(res) {
        if (!res.confirm) return
        const code = String(res.content || '').replace(/\s/g, '')
        if (!/^\d{8}$/.test(code)) {
          wx.showToast({ title: '请输入 8 位登录码', icon: 'none' })
          return
        }
        that.confirmWebsiteCode(code)
      }
    })
  },

  scanWebsiteLogin() {
    if (!this.data.website.available || this.data.website.loading) return
    wx.scanCode({
      onlyFromCamera: true,
      scanType: ['qrCode'],
      success: (res) => {
        const match = String(res.result || '').trim().match(/^xingwei-login:(\d{8})$/i)
        if (!match) {
          wx.showToast({ title: '这不是网站登录二维码', icon: 'none' })
          return
        }
        this.confirmWebsiteCode(match[1])
      },
      fail: (error) => {
        if (error && /cancel/i.test(error.errMsg || '')) return
        wx.showToast({ title: '扫码失败，请输入登录码', icon: 'none' })
      }
    })
  },

  confirmWebsiteCode(code) {
    wx.showLoading({ title: '正在确认', mask: true })
    siteAccount.confirm(code).then(() => {
      wx.hideLoading()
      wx.showToast({ title: '已确认，请回网站完成登录', icon: 'none' })
      this.refreshWebsite()
    }).catch((err) => {
      wx.hideLoading()
      wx.showModal({ title: '确认失败', content: err.message || '请稍后再试', showCancel: false })
    })
  },

  refresh() {
    if (!this.cat) return
    const done = storage.loadDone()
    const summary = stats.computeStats(this.cat, done)
    this.setData({
      summary: summary,
      sessionFrom: business.sessionFrom({ pct: summary.pct })
    })
  },

  onLogin() {
    wx.showLoading({ title: '登录中', mask: true })
    cloud.login().then(function () {
      wx.hideLoading()
      wx.showToast({ title: '登录成功', icon: 'none' })
    }).catch(function (err) {
      wx.hideLoading()
      wx.showModal({
        title: '登录失败',
        content: (err && err.userMessage) || (err && err.message) || '请稍后再试',
        showCancel: false
      })
    })
  },

  onLogout() {
    wx.showModal({
      title: '退出登录',
      content: '退出后，本机进度将保留，云端同步会暂停。确定退出吗？',
      success(res) {
        if (!res.confirm) return
        cloud.logout()
        wx.showToast({ title: '已退出登录', icon: 'none' })
      }
    })
  },

  onSync() {
    const that = this
    wx.showLoading({ title: '同步中', mask: true })
    cloud.syncNow({}).then(function (res) {
      wx.hideLoading()
      if (!res) return
      that.refresh()
      wx.showToast({
        title: res.restored ? '从云端恢复 ' + res.restored + ' 条' : '进度已同步',
        icon: 'none'
      })
    })
  },

  /* ------------------------------------------------------------ 头像昵称 */

  /** 一对一咨询：直达微信客服会话（没配 corpId 时页面上是另一个按钮，走不到这里） */
  onContact() {
    kf.contact().then(function (opened) {
      if (opened) return
    }).catch(function (err) {
      wx.showModal({
        title: '打开客服失败',
        content: (err && err.message) || '请稍后再试',
        showCancel: false
      })
    })
  },
  /** 手机号快速验证：授权后交给网站换手机号并存进账号联系方式 */
  onGetPhone(e) {
    const that = this
    const code = e.detail && e.detail.code
    if (!code) {
      wx.showToast({ title: '没有拿到手机号授权', icon: 'none' })
      return
    }
    wx.showLoading({ title: '正在绑定', mask: true })
    siteAccount.syncPhone(code).then(function (res) {
      wx.hideLoading()
      const masked = (res && res.phoneMasked) || ''
      try { wx.setStorageSync('xh_phone_masked_v1', masked) } catch (err) {}
      that.setData({ phoneMasked: masked })
      wx.showToast({ title: '手机号已保存到网站账号', icon: 'none' })
    }).catch(function (err) {
      wx.hideLoading()
      wx.showModal({
        title: '手机号绑定失败',
        content: (err && err.message) || '请稍后再试',
        showCancel: false
      })
    })
  },

  onChooseAvatar(e) {
    const filePath = e.detail && e.detail.avatarUrl
    if (!filePath) return
    const that = this
    // 已关联网站账号：头像同步到网站（网站与小程序显示同一张），网站侧成为唯一来源
    if (this.data.website.linked) {
      wx.showLoading({ title: '同步到网站', mask: true })
      siteAccount.syncProfile({ avatarPath: filePath, nickName: this.data.profile.nickName })
        .then(function () {
          wx.hideLoading()
          wx.showToast({ title: '头像已同步到网站账号', icon: 'none' })
          that.refreshWebsite()
        })
        .catch(function (err) {
          wx.hideLoading()
          wx.showModal({ title: '同步失败', content: (err && err.message) || '请稍后再试', showCancel: false })
        })
      return
    }
    if (!this.data.cloud.available) {
      cloud.saveProfile({ avatarUrl: filePath })
      wx.showToast({ title: '头像只在这台手机上显示', icon: 'none' })
      return
    }
    wx.showLoading({ title: '上传头像', mask: true })
    cloud.uploadAvatar(filePath).then(function (fileID) {
      wx.hideLoading()
      cloud.saveProfile({ avatarUrl: fileID || filePath })
      that.refresh()
    }).catch(function () {
      wx.hideLoading()
      cloud.saveProfile({ avatarUrl: filePath })
      wx.showToast({ title: '头像上传失败，先存在本机', icon: 'none' })
    })
  },

  onNickChange(e) {
    const that = this
    const nickName = (e.detail && e.detail.value) || ''
    cloud.saveProfile({ nickName: nickName.trim() })
    // 已关联网站账号时，昵称一并同步过去
    if (this.data.website.linked && nickName.trim()) {
      siteAccount.syncProfile({ nickName: nickName.trim() })
        .then(function () { that.refreshWebsite() })
        .catch(function () { wx.showToast({ title: '昵称同步到网站失败', icon: 'none' }) })
    }
    this.refresh()
  },
/* ------------------------------------------------------------ 分享码 */

  onShareCode() {
    const keys = storage.listDone()
    if (!keys.length) {
      wx.showToast({ title: '还没有已完成的迷彩', icon: 'none' })
      return
    }
    if (keys.length > business.SHARE_CODE_LIMIT) {
      wx.showModal({
        title: '分享码太长了',
        content: '当前已有 ' + keys.length + ' 条进度，超过分享码上限 ' + business.SHARE_CODE_LIMIT + ' 条。请使用云端同步。',
        showCancel: false
      })
      return
    }
    wx.setClipboardData({
      data: keys.join('|'),
      success() {
        wx.showModal({
          title: '分享码已复制',
          content: '已复制 ' + keys.length + ' 条进度。请在目标设备的「我的」页面选择「导入分享码」。',
          showCancel: false
        })
      }
    })
  },

  onImportCode() {
    const that = this
    wx.showModal({
      title: '导入分享码',
      editable: true,
      placeholderText: '粘贴分享码',
      success(res) {
        if (!res.confirm || !res.content) return
        const done = storage.loadDone()
        let added = 0
        let skipped = 0
        res.content.split('|').forEach(function (raw) {
          const key = raw.trim()
          if (!key || done[key]) return
          if (!progress.isValidKey(key)) {
            skipped++
            return
          }
          done[key] = true
          added++
        })
        storage.saveDone(done)
        that.refresh()
        cloud.queuePush()
        wx.showToast({
          title: skipped ? '导入 ' + added + ' 条，忽略 ' + skipped + ' 条' : '导入 ' + added + ' 条',
          icon: 'none'
        })
      }
    })
  },

  /** 把进度摘要复制到剪贴板，方便用户直接在客服会话里粘贴给你 */
  onCopySummary() {
    const s = this.data.summary
    const modes = s.modes.map(function (m) {
      return m.short + ' ' + m.count + '/' + m.total
    }).join('，')
    const text = 'MW4 迷彩进度：' + s.count + '/' + s.total + '（' + s.pct + '%）｜' +
      modes + '｜Event ' + s.eventDone + '/' + s.eventTotal
    wx.setClipboardData({
      data: text,
      success() {
        wx.showModal({
          title: '进度已复制',
          content: '可在客服会话中粘贴并发送进度摘要。',
          showCancel: false
        })
      }
    })
  },

  onClear() {
    const that = this
    wx.showModal({
      title: '清空进度',
      content: this.data.cloud.loggedIn
        ? '本机和云端的全部勾选都会被删除，确定继续？'
        : '本机保存的全部迷彩勾选会被删除，确定继续？',
      confirmColor: '#f87171',
      success(res) {
        if (!res.confirm) return
        storage.saveDone({})
        that.refresh()
        cloud.resetCloud()
      }
    })
  },

  onShareAppMessage() {
    return {
      title: business.SHARE.title,
      path: '/pages/index/index'
    }
  },

  onShareTimeline() {
    return {
      title: business.SHARE.timelineTitle,
      query: ''
    }
  }
})
