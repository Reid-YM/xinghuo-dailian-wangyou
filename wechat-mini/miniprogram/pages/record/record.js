/**
 * 业务进度查询。
 *
 * 数据来自网站已经有的公开接口：GET /api/records/{业务单号}
 *   - 未知单号 -> 404
 *   - 网站在维护查询功能 -> 400，带一句中文说明
 * 这个页面不做任何交易动作，只把「客服登记过的进度」展示给用户，
 * 页面不展示交易操作；实际发布要求仍需按平台规则核对。
 */
const api = require('../../utils/api')
const business = require('../../config/business')
const kf = require('../../utils/kf')
const format = require('../../utils/format')

const HISTORY_KEY = 'xh_record_history_v1'
const MAX_HISTORY = 5

// 网站用的状态码 -> 中文。没列到的原样显示，不会显示成空白。
const STATUS_LABELS = {
  // 订单
  WAITING_CONFIRMATION: '等待确认',
  IN_PROGRESS: '进行中',
  COMPLETED: '已完成',
  EXCEPTION: '异常',
  // 询价单
  NEW: '待处理',
  CONTACTED: '已联系',
  QUOTED: '已给出方案',
  CONVERTED: '已转订单',
  CLOSED: '已关闭',
  CANCELLED: '已取消'
}

Page({
  data: {
    copy: business.serviceCopy(),
    kfReady: kf.available(),
    kfButtonText: (business.KF && business.KF.buttonText) || '一对一咨询',
    sessionFrom: '',
    showImages: business.SITE.showImages,
    no: '',
    loading: false,
    result: null,
    error: '',
    history: []
  },

  onLoad() {
    this._querySeq = 0
    this.setData({ history: this.readHistory() })
  },

  onUnload() {
    this._querySeq++
  },

  /* ------------------------------------------------------------ 本机历史 */

  readHistory() {
    try {
      const list = wx.getStorageSync(HISTORY_KEY)
      return Array.isArray(list) ? list : []
    } catch (e) {
      return []
    }
  },

  writeHistory(list) {
    try {
      wx.setStorageSync(HISTORY_KEY, list)
    } catch (e) {}
  },

  pushHistory(no) {
    const list = this.readHistory().filter(function (x) { return x !== no })
    list.unshift(no)
    const next = list.slice(0, MAX_HISTORY)
    this.writeHistory(next)
    this.setData({ history: next })
  },

  onTapHistory(e) {
    this.setData({ no: e.currentTarget.dataset.no })
    this.query()
  },

  onClearHistory() {
    const that = this
    wx.showModal({
      title: '清空查询记录',
      content: '只清本机记住的单号，不影响订单本身。',
      success(res) {
        if (!res.confirm) return
        that.writeHistory([])
        that.setData({ history: [] })
      }
    })
  },

  /* ------------------------------------------------------------ 查询 */

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
  onInput(e) {
    this.setData({ no: e.detail.value })
  },

  onScan() {
    const that = this
    wx.scanCode({
      onlyFromCamera: false,
      success(res) {
        const text = (res.result || '').trim()
        // 扫到的可能是一整串链接，取最后一段当单号
        const guess = text.split('/').pop()
        that.setData({ no: guess.toUpperCase() })
        that.query()
      },
      fail() {}
    })
  },

  onQuery() {
    this.query()
  },

  query() {
    const that = this
    const no = (this.data.no || '').trim().toUpperCase()
    if (!no) {
      wx.showToast({ title: '请先输入业务单号', icon: 'none' })
      return
    }
    if (this.data.loading && this._pendingNo === no) return
    const seq = ++this._querySeq
    this._pendingNo = no
    this.setData({ loading: true, error: '', result: null })

    return api.request('/api/records/' + encodeURIComponent(no))
      .then(function (data) {
        if (seq !== that._querySeq) return
        that._pendingNo = ''
        that.setData({
          loading: false,
          error: '',
          result: that.decorate(data),
          sessionFrom: business.sessionFrom({ note: '单号:' + no })
        })
        that.pushHistory(no)
      })
      .catch(function (err) {
        if (seq !== that._querySeq) return
        that._pendingNo = ''
        const is404 = err && err.statusCode === 404
        that.setData({
          loading: false,
          result: null,
          sessionFrom: business.sessionFrom({ note: '单号:' + no }),
          error: is404
            ? '没查到「' + no + '」。确认一下有没有输错，或者直接问客服。'
            : ((err && err.message) || '查询失败，请稍后再试')
        })
      })
  },

  /** 把服务端字段整理成页面直接能用的形状 */
  decorate(data) {
    const d = data || {}
    const hasPercent = typeof d.progressPercent === 'number' && d.progressPercent >= 0
    return {
      businessNo: d.businessNo || '',
      stageText: d.stage === 'ORDER' ? '服务订单' : '询价单',
      status: d.status || '',
      statusText: STATUS_LABELS[d.status] || d.status || '',
      title: d.title || '',
      hasPercent: hasPercent,
      progressPercent: hasPercent ? d.progressPercent : 0,
      progressText: d.progressText || '',
      expectedTime: d.expectedTime || '',
      createdText: format.dateTime(d.createdAt),
      updatedText: format.timeAgo(d.updatedAt ? Date.parse(d.updatedAt) : 0),
      hasProgressImage: !!d.hasProgressImage,
      imageUrl: d.hasProgressImage && d.businessNo
        ? api.asset('/api/records/' + encodeURIComponent(d.businessNo) + '/image')
        : '',
      customerNote: d.customerNote || ''
    }
  },

  onPreviewImage() {
    const url = this.data.result && this.data.result.imageUrl
    if (!url) return
    wx.previewImage({ urls: [url] })
  },

  /* ------------------------------------------------------------ 分享 */

  onShareAppMessage() {
    return {
      title: '星火迷彩追踪器 · 业务进度随时查',
      path: '/pages/record/record'
    }
  }
})
