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

function requestPayment(params) {
  return new Promise((resolve, reject) => {
    wx.requestPayment({
      timeStamp: String(params.timeStamp || ''),
      nonceStr: String(params.nonceStr || ''),
      package: String(params.packageVal || ''),
      signType: String(params.signType || 'RSA'),
      paySign: String(params.paySign || ''),
      success: resolve,
      fail(error) {
        const message = String((error && error.errMsg) || '');
        if (/cancel/i.test(message)) {
          const cancelled = new Error('你已取消本次支付');
          cancelled.cancelled = true;
          reject(cancelled);
          return;
        }
        reject(new Error('微信支付未完成，请稍后重试或联系客服'));
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

function paymentView(payment) {
  const status = String(payment.status || '');
  const channel = String(payment.channel || '');
  const expires = payment.expiresAt ? new Date(payment.expiresAt) : null;
  const valid = Boolean(expires && !Number.isNaN(expires.getTime()) && expires.getTime() > Date.now());
  let hint = '等待客服确认支付方式';
  let canPay = false;
  if (status === 'CAPTURED') hint = '服务端已确认收款';
  else if (status === 'CREATED') { hint = '待付款，可直接在小程序发起微信支付'; canPay = true; }
  else if (status === 'APPROVED' && channel === 'NATIVE') hint = '已生成网页扫码支付单，请使用原二维码付款；需要改用小程序请联系客服关闭后重建';
  else if (status === 'APPROVED' && channel === 'MINI_PROGRAM' && valid) { hint = '小程序支付参数仍有效，可继续完成支付'; canPay = true; }
  else if (status === 'APPROVED' && channel === 'MINI_PROGRAM') hint = '小程序支付参数已过期，请联系客服重新确认支付单';
  else if (status === 'CANCELLED') hint = '支付单已关闭';
  return {
    ...payment,
    amountText: Number(payment.amount || 0).toFixed(2),
    hint,
    canPay,
    paid: status === 'CAPTURED'
  };
}

Page({
  data: {
    pairCode: '',
    busy: false,
    profileBusy: false,
    phoneBusy: false,
    paymentLoading: false,
    paymentBusyNo: '',
    payments: [],
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
    if (incoming) this.setData({ pairCode: incoming }, () => this.confirmCode());
    this.refreshProfile();
  },

  onShow() {
    if (this.data.linked && !this.data.profileBusy && !this.data.paymentLoading) {
      this.refreshProfile();
    }
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
      if (profile && profile.linked) await this.refreshPayments();
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
      if (profile && profile.linked) await this.refreshPayments();
      else this.setData({ payments: [] });
    } catch (_) {
      // 未关联或临时网络失败不影响扫码主流程。
    } finally {
      this.setData({ profileBusy: false });
    }
  },

  async refreshPayments() {
    if (!this.data.linked || this.data.paymentLoading) return this.data.payments;
    this.setData({ paymentLoading: true });
    try {
      const code = await loginCode();
      const result = await api('/api/account/wechat-mini/payments', { loginCode: code });
      const payments = (Array.isArray(result) ? result : []).map(paymentView);
      this.setData({ payments });
      return payments;
    } catch (error) {
      // 支付开关尚未启用时不影响账号功能；真实支付错误在用户主动支付时再展示。
      return this.data.payments;
    } finally {
      this.setData({ paymentLoading: false });
    }
  },

  async payPayment(event) {
    const paymentNo = String(event.currentTarget.dataset.paymentNo || '');
    if (!paymentNo || this.data.paymentBusyNo) return;
    this.setData({ paymentBusyNo: paymentNo });
    this.setStatus('正在向服务器申请微信支付参数…', '');
    try {
      const code = await loginCode();
      const params = await api(`/api/account/wechat-mini/payments/${encodeURIComponent(paymentNo)}/prepay`, { loginCode: code });
      await requestPayment(params);
      this.setStatus('微信支付界面已返回，正在向服务器核验最终收款状态…', '');
      const payments = await this.refreshPayments();
      const latest = (payments || []).find(item => item.paymentNo === paymentNo);
      if (latest && latest.paid) {
        this.setStatus('服务端已确认支付成功。', 'ok');
        wx.showToast({ title: '支付已确认', icon: 'success' });
      } else {
        this.setStatus('微信已返回支付流程，服务端仍在核验。可稍后点击“刷新支付状态”。', '');
        wx.showToast({ title: '结果确认中', icon: 'none' });
      }
    } catch (error) {
      this.setStatus(error.message || '支付未完成，请稍后重试。', error.cancelled ? '' : 'error');
    } finally {
      this.setData({ paymentBusyNo: '' });
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
