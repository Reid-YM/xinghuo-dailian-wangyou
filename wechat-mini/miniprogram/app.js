const cloud = require('./utils/cloud')
const business = require('./config/business')

App({
  globalData: {
    version: business.VERSION
  },

  onLaunch() {
    // 初始化云开发能力，并恢复上次的登录状态。
    // 登录只靠 openid，不弹任何授权框，所以新用户可以直接自动登录。
    cloud.bootstrap().then(function (ok) {
      if (!ok) return
      if (cloud.getState().loggedIn) {
        cloud.syncNow({ silent: true })
      } else {
        // 新用户：静默登录一次；失败也不打扰（云开发没开通时退回手动按钮）
        cloud.login({ silent: true }).catch(function () {})
      }
    })
  },

  onHide() {
    // 退到后台时，把还没推上云的进度立刻同步掉，避免用户杀进程丢数据。
    cloud.flush()
  }
})
