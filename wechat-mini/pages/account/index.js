const app = getApp();

function loginCode() {
  return new Promise((resolve, reject) => {
    wx.login({
      timeout: 10000,
      success(res) {
        if (res.code) resolve(res.code);
        else reject(new Error('微信登录凭证获取失败，请重试'));
      },
      fail() {
        reject(new Error('无法获取微信登录凭证，请检查网络'));
      }
    });
  });
}

function api(path, data) {
  return new Promise((resolve, reject) => {
    wx.request({
      url: app.globalData.apiBase + path,
      method: 'POST',
      data,
      timeout: 12000,
      header: { 'content-type': 'application/json', accept: 'application/json' },
      success(res) {
        if (res.statusCode >= 200 && res.statusCode < 300) {
          resolve(res.data || {});
          return;
        }
        const body = res.data || {};
        reject(new Error(body.message || body.error || `请求失败（${res.statusCode}）`));
      },
      fail() {
        reject(new Error('无法连接网站服务，请检查网络后重试'));
      }
    });
  });
}

function extractPairCode(raw) {
  let text = String(raw || '').trim();
  try { text = decodeURIComponent(text); } catch (_) { /* keep raw */ }
  const prefixed = text.match(/xingwei-login:(\d{8})/i);
  if (prefixed) return prefixed[1];
  const plain = text.match(/(?:^|\D)(\d{8})(?:\D|$)/);
  return plain ? plain[1] : '';
}

Page({
  data: {
    pairCode: '',
    busy: false,
    profileBusy: false,
    phoneBusy: false,
    linked: false,
    displayName: '',
    userId: '',
    orderCount: 0,
    nickName: '',
    status: '请扫描网站登录页二维码，或输入 8 位登录码。',
    statusType: ''
  },

  onLoad(options) {
    const incoming = extractPairCode((options && (options.code || options.scene)) || '');
    if (incoming) {
      this.setData({ pairCode: incoming }, () => this.confirmCode());
    }
    this.refreshProfile();
  },

  onCodeInput(event) {
    const pairCode = String(event.detail.value || '').replace(/\D/g, '').slice(0, 8);
    this.setData({ pairCode });
  },

  onNickInput(event) {
    this.setData({ nickName: String(event.detail.value || '').slice(0, 60) });
  },

  scanLogin() {
    if (this.data.busy) return;
    wx.scanCode({
      onlyFromCamera: true,
      scanType: ['qrCode'],
      success: (res) => {
        const pairCode = extractPairCode(res.result);
        if (!pairCode) {
          this.setStatus('这不是星维网络的网站登录二维码。', 'error');
          return;
        }
        this.setData({ pairCode }, () => this.confirmCode());
      },
      fail: (error) => {
        if (error && /cancel/i.test(error.errMsg || '')) return;
        this.setStatus('扫码失败，请重新扫描或手动输入登录码。', 'error');
      }
    });
  },

  async confirmCode() {
    const pairCode = String(this.data.pairCode || '').replace(/\D/g, '');
    if (!/^\d{8}$/.test(pairCode)) {
      this.setStatus('请输入完整的 8 位网站登录码。', 'error');
      return;
    }
    this.setData({ busy: true });
    this.setStatus('正在向网站确认当前微信身份…', '');
    try {
      const code = await loginCode();
      const profile = await api('/api/account/wechat-mini/confirm', { code: pairCode, loginCode: code });
      this.applyProfile(profile);
      this.setStatus('小程序已确认。请回到网页核对微信识别码尾号，再点击“确认登录此网站”。', 'ok');
      wx.showToast({ title: '已确认', icon: 'success' });
    } catch (error) {
      this.setStatus(error.message || '确认失败，请重试。', 'error');
    } finally {
      this.setData({ busy: false });
    }
  },

  async refreshProfile() {
    if (this.data.profileBusy) return;
    this.setData({ profileBusy: true });
    try {
      const code = await loginCode();
      const profile = await api('/api/account/wechat-mini/profile', { loginCode: code });
      this.applyProfile(profile);
    } catch (_) {
      // 未关联或临时网络失败不影响扫码主流程。
    } finally {
      this.setData({ profileBusy: false });
    }
  },

  async syncNickName() {
    const nickName = String(this.data.nickName || '').trim();
    if (!nickName) {
      this.setStatus('请先输入要同步的网站显示昵称。', 'error');
      return;
    }
    this.setData({ profileBusy: true });
    try {
      const code = await loginCode();
      const profile = await api('/api/account/wechat-mini/sync-profile', { loginCode: code, nickName });
      this.applyProfile(profile);
      this.setStatus('昵称已同步到网站账号。', 'ok');
      wx.showToast({ title: '已同步', icon: 'success' });
    } catch (error) {
      this.setStatus(error.message || '同步失败，请重试。', 'error');
    } finally {
      this.setData({ profileBusy: false });
    }
  },

  async syncPhone(event) {
    const phoneCode = event && event.detail && event.detail.code;
    if (!phoneCode) {
      this.setStatus('未获得手机号授权。', 'error');
      return;
    }
    this.setData({ phoneBusy: true });
    try {
      const code = await loginCode();
      const result = await api('/api/account/wechat-mini/sync-phone', { loginCode: code, phoneCode });
      this.setStatus(`手机号已验证：${result.phoneMasked || '已保存'}`, 'ok');
      wx.showToast({ title: '已验证', icon: 'success' });
    } catch (error) {
      this.setStatus(error.message || '手机号验证失败，请重试。', 'error');
    } finally {
      this.setData({ phoneBusy: false });
    }
  },

  applyProfile(profile) {
    const linked = Boolean(profile && profile.linked);
    this.setData({
      linked,
      displayName: linked ? (profile.displayName || '') : '',
      userId: linked ? (profile.userId || '') : '',
      orderCount: linked ? Number(profile.orderCount || 0) : 0,
      nickName: linked ? (profile.displayName || this.data.nickName || '') : this.data.nickName
    });
  },

  setStatus(status, statusType) {
    this.setData({ status, statusType: statusType || '' });
  }
});
