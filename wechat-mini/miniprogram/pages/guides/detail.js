/**
 * 攻略详情。内容来自 GET /api/guides/{slug}
 *
 * 页面上刻意不出现的东西：服务/价格（products，数据层已丢）、站外链接（只留文字）。
 * 读完攻略自然接客服入口 —— 这是攻略页真正的商业价值。
 */
const guides = require('../../utils/guides')
const business = require('../../config/business')
const format = require('../../utils/format')

Page({
  data: {
    copy: business.serviceCopy(),
    sessionFrom: business.sessionFrom({}),
    showImages: business.SITE.showImages,
    loading: true,
    error: '',
    guide: null,
    openFaq: {},
    updatedText: ''
  },

  onLoad(query) {
    this.slug = (query && query.slug) || ''
    if (!this.slug) {
      this.setData({ loading: false, error: '缺少攻略标识，返回列表重新进入' })
      return
    }
    this.ready = this.load()
  },

  onPullDownRefresh() {
    this.load({ force: true }).then(function () {
      wx.stopPullDownRefresh()
    })
  },

  load(options) {
    const that = this
    this.setData({ loading: true, error: '' })
    return guides.loadDetail(this.slug, options).then(function (guide) {
      that.setData({
        loading: false,
        error: '',
        guide: guide,
        openFaq: {},
        updatedText: guide.updatedAt ? format.timeAgo(Date.parse(guide.updatedAt)) : '',
        sessionFrom: business.sessionFrom({ note: '攻略:' + (guide.title || '') })
      })
      if (guide.title) {
        wx.setNavigationBarTitle({ title: guide.title })
      }
      return guide
    }).catch(function (err) {
      that.setData({
        loading: false,
        guide: null,
        error: (err && err.message) || '这篇攻略暂时打不开，稍后再试'
      })
    })
  },

  onRetry() {
    this.load({ force: true })
  },

  onBackToList() {
    wx.navigateBack({
      fail() {
        wx.switchTab({ url: '/pages/guides/guides' })
      }
    })
  },

  onToggleFaq(e) {
    const i = Number(e.currentTarget.dataset.i)
    const openFaq = Object.assign({}, this.data.openFaq)
    openFaq[i] = !openFaq[i]
    this.setData({ openFaq: openFaq })
  },

  onTapRelated(e) {
    const slug = e.currentTarget.dataset.slug
    if (!slug) return
    // 用 redirectTo：避免相关攻略一直点下去把页面栈堆满
    wx.redirectTo({ url: '/pages/guides/detail?slug=' + encodeURIComponent(slug) })
  },

  onPreviewCover() {
    const url = this.data.guide && this.data.guide.cover
    if (!url) return
    wx.previewImage({ urls: [url] })
  },

  onShareAppMessage() {
    const g = this.data.guide
    return {
      title: g ? g.title : business.SHARE.title,
      path: '/pages/guides/detail?slug=' + encodeURIComponent(this.slug)
    }
  },

  onShareTimeline() {
    const g = this.data.guide
    return {
      title: g ? g.title : business.SHARE.timelineTitle,
      query: 'slug=' + encodeURIComponent(this.slug)
    }
  }
})
