/**
 * 攻略列表。内容来自网站公开接口 GET /api/guides
 * 纯资讯内容，不含任何服务/价格信息（guides 数据层已经把 products 丢掉了）。
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
    keyword: '',
    items: [],
    shown: [],
    filteredOut: 0,
    updatedText: ''
  },

  onLoad() {
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
    return guides.loadList(options).then(function (res) {
      that.setData({
        loading: false,
        error: '',
        items: res.items,
        shown: guides.filterList(res.items, that.data.keyword),
        filteredOut: res.filteredOut || 0,
        updatedText: res.cached ? '缓存于 ' + format.timeAgo(res.fetchedAt) : '刚刚更新'
      })
      return res
    }).catch(function (err) {
      that.setData({
        loading: false,
        items: [],
        shown: [],
        error: (err && err.message) || '攻略加载失败，检查一下网络再试'
      })
    })
  },

  onRetry() {
    this.load({ force: true })
  },

  onSearch(e) {
    this.setData({ keyword: e.detail.value })
    this.applyFilter()
  },

  onResetSearch() {
    this.setData({ keyword: '' })
    this.applyFilter()
  },

  applyFilter() {
    this.setData({ shown: guides.filterList(this.data.items, this.data.keyword) })
  },

  onTap(e) {
    const slug = e.currentTarget.dataset.slug
    if (!slug) return
    wx.navigateTo({ url: '/pages/guides/detail' + '?slug=' + encodeURIComponent(slug) })
  },

  onShareAppMessage() {
    return {
      title: business.SHARE.title,
      path: '/pages/guides/guides'
    }
  },

  onShareTimeline() {
    return {
      title: business.SHARE.timelineTitle,
      query: ''
    }
  }
})
