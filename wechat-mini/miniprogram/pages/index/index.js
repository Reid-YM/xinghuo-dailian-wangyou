const storage = require('../../utils/storage')
const progress = require('../../utils/progress')
const stats = require('../../utils/stats')
const catalog = require('../../utils/catalog')
const cloud = require('../../utils/cloud')
const business = require('../../config/business')
const kf = require('../../utils/kf')
const format = require('../../utils/format')

/** 完整迷彩挑战：按武器 + 条目生成可勾选状态（本机记录，不计入主进度）。 */
function guideForClass(guide, weaponCode, guideDone) {
  if (!guide) return null
  function on(key) {
    return !!(guideDone && guideDone[weaponCode + '::' + key])
  }
  function mark(item) {
    return {
      key: item.key,
      name: item.name,
      req: item.req,
      estimate: !!item.estimate,
      image: item.image,
      imageRemote: item.imageRemote,
      on: on(item.key)
    }
  }
  return {
    note: guide.note || '',
    groups: (guide.groups || []).map(function (g) {
      return { key: g.key, title: g.title, items: (g.items || []).map(mark) }
    }),
    militaryTiles: (guide.militaryTiles || []).map(function (item) {
      return {
        key: item.key,
        label: item.label,
        image: item.image,
        imageRemote: item.imageRemote,
        on: on(item.key)
      }
    })
  }
}

Page({
  data: {
    brand: business.BRAND.name,
    copy: business.serviceCopy(),
    kfReady: kf.available(),
    kfButtonText: (business.KF && business.KF.buttonText) || '一对一咨询',
    sessionFrom: '',
    showImages: business.SITE.showImages,

    loading: true,
    sourceText: '',
    dataNote: '',

    cloud: {
      available: false,
      loggedIn: false,
      syncing: false,
      syncText: '还没同步过',
      hint: ''
    },

    modes: [],
    activeMode: '',
    activeModeName: '',
    activeTiers: [],
    finalCamo: '',
    tierCounts: [],
    weaponCount: 0,

    classOptions: ['全部枪械'],
    classIndex: 0,
    statusFilter: 'all',
    keyword: '',
    visibleCount: 0,
    hasActiveFilters: false,
    groups: [],
    eventTitle: '',
    eventList: [],
    modeStats: [],
    stats: { count: 0, total: 0, pct: 0 },
    done: {},
    favs: {},
    expanded: {},
    collapsed: {},
    guideDone: {},
    guideOpen: false,
    // 包内图片读不出来时（比如没打进上传包）整体切到官网地址
    useRemoteImages: false
  },

  onLoad() {
    this._lastSyncAt = -1
    this._loggedIn = null
    this.cat = null
    this.setData({
      done: storage.loadDone(),
      favs: storage.loadFavs(),
      guideDone: storage.loadGuideDone()
    })
    this._unsub = cloud.subscribe(this.applyCloudState.bind(this))
    // ready 是给测试脚本 await 用的，页面里不需要关心
    this.ready = this.loadCatalog()
  },


  onHide() {
    cloud.flush()
  },

  onUnload() {
    if (this._unsub) this._unsub()
    if (this._searchTimer) clearTimeout(this._searchTimer)
  },

  /** 拉官网数据（失败自动退回本地快照），然后渲染 */
  loadCatalog(options) {
    const that = this
    return catalog.load(options).then(function (cat) {
      that.cat = cat
      const modes = cat.modes.map(function (m) {
        return { code: m.code, name: m.name, short: m.short }
      })
      const first = cat.modes[0]
      const eventMode = cat.eventMode
      that.setData({
        loading: false,
        modes: modes,
        activeMode: first.code,
        activeModeName: first.name,
        activeTiers: first.tiers,
        finalCamo: first.finalCamoZh || first.finalCamo || '',
        weaponCount: cat.weapons.length,
        classOptions: ['全部枪械'].concat(cat.classes),
        eventTitle: eventMode ? (eventMode.short || eventMode.name) : '',
        sourceText: that.describeSource(cat),
        dataNote: cat.game && cat.game.placeholder
          ? '部分挑战条件尚未公布，数据更新后将同步显示。'
          : ''
      })
      that.render()
      return cat
    })
  },

  describeSource(cat) {
    if (cat.source === 'remote') return '官网数据 · ' + (cat.version || '最新版本')
    if (cat.source === 'cache') return '本机缓存 · 更新于 ' + format.timeAgo(cat.fetchedAt)
    return '离线快照 · 当前无法获取官网数据'
  },

  onPullDownRefresh() {
    const that = this
    const jobs = [this.loadCatalog({ force: true })]
    if (this.data.cloud.loggedIn) jobs.push(cloud.syncNow({ silent: true }))
    Promise.all(jobs).then(function (results) {
      wx.stopPullDownRefresh()
      that.refreshFromLocal()
      wx.showToast({
        title: results[0].source === 'remote' ? '已拉取官网最新数据' : '网络不可用，已显示本地数据',
        icon: 'none'
      })
    }).catch(function () {
      wx.stopPullDownRefresh()
    })
  },

  /* ------------------------------------------------------------ 云状态 */

  applyCloudState(state) {
    const needRefresh =
      state.lastSyncAt !== this._lastSyncAt || state.loggedIn !== this._loggedIn
    this._lastSyncAt = state.lastSyncAt
    this._loggedIn = state.loggedIn

    this.setData({
      cloud: {
        available: state.available,
        loggedIn: state.loggedIn,
        syncing: state.syncing,
        syncText: format.syncStatus(state),
        hint: state.hint
      }
    })

    if (needRefresh) this.refreshFromLocal()
  },

  refreshFromLocal() {
    this.setData({
      done: storage.loadDone(),
      favs: storage.loadFavs()
    })
    this.render()
  },

  onLogin() {
    const that = this
    wx.showLoading({ title: '登录中', mask: true })
    cloud.login().then(function () {
      wx.hideLoading()
      wx.showToast({ title: '登录成功', icon: 'none' })
      that.refreshFromLocal()
    }).catch(function (err) {
      wx.hideLoading()
      wx.showModal({
        title: '登录失败',
        content: (err && err.userMessage) || (err && err.message) || '请稍后再试',
        showCancel: false
      })
    })
  },

  onSync() {
    const that = this
    wx.showLoading({ title: '同步中', mask: true })
    cloud.syncNow({}).then(function (res) {
      wx.hideLoading()
      if (!res) return
      that.refreshFromLocal()
      wx.showToast({
        title: res.restored ? '从云端恢复 ' + res.restored + ' 条' : '进度已同步',
        icon: 'none'
      })
    })
  },

  /* ------------------------------------------------------------ 列表交互 */

  onMode(e) {
    const code = e.currentTarget.dataset.key
    const mode = (this.cat.modes || []).filter(function (m) { return m.code === code })[0]
    if (!mode) return
    this.setData({
      activeMode: mode.code,
      activeModeName: mode.name,
      activeTiers: mode.tiers,
      finalCamo: mode.finalCamoZh || mode.finalCamo || ''
    })
    this.render()
  },

  onClassChange(e) {
    this.setData({ classIndex: Number(e.detail.value) })
    this.render()
  },

  onStatus(e) {
    this.setData({ statusFilter: e.currentTarget.dataset.v })
    this.render()
  },

  onResetFilters() {
    if (this._searchTimer) clearTimeout(this._searchTimer)
    this._searchTimer = null
    this.setData({ keyword: '', classIndex: 0, statusFilter: 'all' })
    this.render()
  },

  onSearch(e) {
    this.setData({ keyword: e.detail.value })
    if (this._searchTimer) clearTimeout(this._searchTimer)
    const that = this
    this._searchTimer = setTimeout(function () {
      that._searchTimer = null
      that.render()
    }, 180)
  },


  onGoRecord() {
    wx.switchTab({ url: '/pages/record/record' })
  },

  /** 图片读不出来（包内图缺失/损坏）→ 整体改用官网地址，域名配好后就能显示 */
  onImgError() {
    if (this.data.useRemoteImages) return
    this.setData({ useRemoteImages: true })
    this.render()
  },

  /** 点迷彩图看大图 */
  onPreviewCamo(e) {
    const url = e.currentTarget.dataset.url
    if (!url) return
    wx.previewImage({ urls: [url] })
  },

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

  onToggleGuide() {
    this.setData({ guideOpen: !this.data.guideOpen })
  },

  onToggleTier(e) {
    const key = e.currentTarget.dataset.key
    if (!key) return
    const done = Object.assign({}, this.data.done)
    const parts = key.split('::')
    const tier = parts.length === 3 ? parseInt(parts[2], 10) : NaN
    if (!isNaN(tier)) {
      // 迷彩按顺序解锁：勾第 N 档要连带勾上前几档；取消第 N 档要连带取消后几档
      const modeCode = parts[0]
      const weaponCode = parts[1]
      const mode = (this.cat.modes || []).filter(function (m) { return m.code === modeCode })[0]
      const total = mode ? mode.tiers.length : 3
      if (done[key]) {
        for (let i = tier; i <= total; i++) delete done[modeCode + '::' + weaponCode + '::' + i]
      } else {
        for (let i = 1; i <= tier; i++) done[modeCode + '::' + weaponCode + '::' + i] = true
      }
    } else if (done[key]) {
      delete done[key]
    } else {
      done[key] = true
    }
    storage.saveDone(done)
    this.setData({ done: done })
    this.render()
    cloud.queuePush()
  },

  onToggleGuideItem(e) {
    const key = e.currentTarget.dataset.key
    if (!key) return
    const guideDone = Object.assign({}, this.data.guideDone)
    if (guideDone[key]) delete guideDone[key]
    else guideDone[key] = true
    storage.saveGuideDone(guideDone)
    this.setData({ guideDone: guideDone })
    this.render()
  },

  /** 精通迷彩是模式级共享：顶部点一下 = 该模式全部武器该档一起标记 */
  onToggleMastery(e) {
    const tier = Number(e.currentTarget.dataset.tier)
    const modeCode = this.data.activeMode
    if (!tier || !modeCode || !this.cat) return
    const done = Object.assign({}, this.data.done)
    const keys = this.cat.weapons.map(function (w) { return progress.tierKey(modeCode, w.code, tier) })
    const unlocked = keys.every(function (k) { return done[k] })
    keys.forEach(function (k) { if (unlocked) delete done[k]; else done[k] = true })
    storage.saveDone(done)
    this.setData({ done: done })
    this.render()
    cloud.queuePush()
  },

  onToggleFav(e) {
    const code = e.currentTarget.dataset.weapon
    if (!code) return
    const favs = Object.assign({}, this.data.favs)
    if (favs[code]) {
      delete favs[code]
    } else {
      favs[code] = true
    }
    storage.saveFavs(favs)
    this.setData({ favs: favs })
    this.render()
    cloud.queuePush()
  },

  onToggleExpand(e) {
    const code = e.currentTarget.dataset.weapon
    if (!code) return
    const expanded = Object.assign({}, this.data.expanded)
    expanded[code] = !expanded[code]
    this.setData({ expanded: expanded })
    this.render()
  },

  onToggleClass(e) {
    const cls = e.currentTarget.dataset.cls
    if (!cls) return
    const collapsed = Object.assign({}, this.data.collapsed)
    collapsed[cls] = !collapsed[cls]
    this.setData({ collapsed: collapsed })
    this.render()
  },

  /* ------------------------------------------------------------ 分享 */

  onShareAppMessage() {
    const pct = this.data.stats.pct
    return {
      title: pct > 0
        ? 'MW4 迷彩我已经解到 ' + pct + '% 了，你也来对一下进度'
        : business.SHARE.title,
      path: '/pages/index/index'
    }
  },

  onShareTimeline() {
    const pct = this.data.stats.pct
    return {
      title: pct > 0 ? 'MW4 全武器迷彩追踪 · 我的进度 ' + pct + '%' : business.SHARE.timelineTitle,
      query: ''
    }
  },

  /* ------------------------------------------------------------ 渲染 */

  render() {
    if (this._searchTimer) {
      clearTimeout(this._searchTimer)
      this._searchTimer = null
    }
    const cat = this.cat
    if (!cat) return

    const done = this.data.done
    const favs = this.data.favs
    const guideDone = this.data.guideDone || {}
    const expanded = this.data.expanded
    const useRemote = this.data.useRemoteImages
    const collapsed = this.data.collapsed
    const activeMode = this.data.activeMode
    const mode = cat.modes.filter(function (m) { return m.code === activeMode })[0]
    if (!mode) return
    const statusFilter = this.data.statusFilter
    const keyword = this.data.keyword.trim().toLowerCase()
    const classFilter = this.data.classIndex > 0 ? cat.classes[this.data.classIndex - 1] : ''

    const groups = []
    stats.groupByClass(cat, classFilter).forEach(function (g) {
      const weapons = []
      g.weapons.forEach(function (w) {
        const wp = stats.weaponProgress(cat, mode, w.code, done)
        const fav = !!favs[w.code]
        const hay = (w.nameZh + ' ' + w.name + ' ' + w.code).toLowerCase()
        const matchesKeyword = !keyword || hay.indexOf(keyword) !== -1
        const matchesStatus =
          statusFilter === 'all' ||
          (statusFilter === 'todo' && !wp.complete) ||
          (statusFilter === 'done' && wp.complete) ||
          (statusFilter === 'fav' && fav)
        if (!matchesKeyword || !matchesStatus) return
        weapons.push({
          code: w.code,
          name: w.name,
          nameZh: w.nameZh,
          image: useRemote ? (w.imageRemote || w.image) : w.image,
          imgFallback: !!w.imgFallback,
          hint: w.hint,
          fav: fav,
          count: wp.count,
          total: mode.tiers.length,
          complete: wp.complete,
          expanded: !!expanded[w.code],
          camoGuide: guideForClass((mode.code === 'MULTIPLAYER' && cat.camoGuide && cat.camoGuide.weapons && cat.camoGuide.weapons[w.code]) || (cat.camoGuide && cat.camoGuide.classes ? cat.camoGuide.classes[w.cls] : null), w.code, guideDone),
          tiers: wp.tiers.map(function (t) {
            return {
              key: t.key,
              tier: t.tier,
              name: t.item.nameZh || t.item.name,
              nameEn: t.item.name,
              image: useRemote ? (t.item.imageRemote || t.item.image) : t.item.image,
              on: t.on
            }
          })
        })
      })
      if (weapons.length) {
        groups.push({
          cls: g.cls,
          weapons: weapons,
          count: weapons.filter(function (w) { return w.complete }).length,
          total: weapons.length,
          collapsed: !!collapsed[g.cls]
        })
      }
    })

    const tierCounts = mode.tiers.map(function (t, i) {
      return {
        tier: t.tier,
        name: t.nameZh || t.name,
        nameEn: t.name,
        image: useRemote ? (t.imageRemote || t.image) : t.image,
        requirement: t.requirement || '',
        count: stats.tierCounts(cat, mode, i, done)
      }
    })

    const summary = stats.computeStats(cat, done)

    const eventList = cat.eventMode && !classFilter && statusFilter !== 'fav'
      ? cat.eventMode.items.map(function (it) {
          const key = progress.eventKey(cat.eventMode.code, it.code)
          return {
            code: it.code,
            key: key,
            name: it.nameZh || it.name,
            nameEn: it.name,
            image: useRemote ? (it.imageRemote || it.image) : it.image,
            requirement: it.requirement || '',
            on: !!done[key]
          }
        }).filter(function (it) {
          const hay = (it.name + ' ' + it.nameEn + ' ' + it.code).toLowerCase()
          return (!keyword || hay.indexOf(keyword) !== -1) &&
            (statusFilter === 'all' ||
              (statusFilter === 'done' && it.on) ||
              (statusFilter === 'todo' && !it.on))
        })
      : []

    const visibleCount = groups.reduce(function (sum, group) { return sum + group.weapons.length }, 0) + eventList.length

    this.setData({
      groups: groups,
      tierCounts: tierCounts,
      modeStats: summary.modes,
      eventList: eventList,
      visibleCount: visibleCount,
      hasActiveFilters: !!(keyword || classFilter || statusFilter !== 'all'),
      stats: { count: summary.count, total: summary.total, pct: summary.pct },
      sessionFrom: business.sessionFrom({ mode: mode.name, pct: summary.pct })
    })
  }
})
