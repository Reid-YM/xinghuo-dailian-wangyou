(() => {
  'use strict';

  const detail = document.getElementById('recordDetailBox');
  const panel = document.getElementById('guestBusinessPanel');
  if (!detail || !panel) return;

  const businessNo = detail.dataset.businessNo;
  if (!businessNo) return;
  let pollTimer = null;

  const money = value => {
    const n = Number(value);
    return Number.isFinite(n) ? `¥${n.toFixed(2)}` : '—';
  };
  const time = value => {
    if (!value) return '—';
    const d = new Date(value);
    return Number.isNaN(d.getTime()) ? String(value) : d.toLocaleString('zh-CN', {hour12:false});
  };
  const button = (text, onClick, primary = false) => {
    const node = document.createElement('button');
    node.type = 'button';
    node.className = primary ? 'btn primary' : 'btn';
    node.textContent = text;
    node.addEventListener('click', onClick);
    return node;
  };
  const text = (tag, value, className = '') => {
    const node = document.createElement(tag);
    if (className) node.className = className;
    node.textContent = value;
    return node;
  };

  async function api(path, options = {}) {
    const response = await fetch(path, {
      credentials: 'same-origin',
      cache: 'no-store',
      headers: {'Accept':'application/json', ...(options.headers || {})},
      ...options
    });
    if (response.status === 404 || response.status === 403) {
      const error = new Error('GUEST_ACCESS_UNAVAILABLE');
      error.silent = true;
      throw error;
    }
    if (!response.ok) {
      let message = '';
      try {
        const body = await response.json();
        message = body?.message || body?.error || '';
      } catch (_) {}
      throw new Error(message || `请求失败（${response.status}）`);
    }
    return response.status === 204 ? null : response.json();
  }

  function row(label, value) {
    const wrap = document.createElement('div');
    wrap.className = 'detail-row';
    wrap.append(text('span', label, 'muted'), text('strong', value || '—'));
    return wrap;
  }

  function schedulePoll(state) {
    clearTimeout(pollTimer);
    const status = state?.payment?.status;
    if (!status || ['CAPTURED','CANCELLED','REFUNDED','FAILED'].includes(status)) return;
    pollTimer = setTimeout(load, 5000);
  }

  function render(state) {
    panel.hidden = false;
    panel.replaceChildren();
    panel.append(text('div', '游客专属业务中心', 'eyebrow'));
    panel.append(text('h2', state.offer ? '正式报价与付款' : '等待客服报价'));

    const intro = state.offer
      ? '这份报价只对当前浏览器持有的游客凭证可见，无需注册账号即可确认和付款。'
      : '询价已保存。客服发布正式报价后，你可以直接在这里确认并付款，无需注册。';
    panel.append(text('p', intro, 'muted'));

    const offer = state.offer;
    if (offer) {
      const offerBox = document.createElement('section');
      offerBox.className = 'card soft-card';
      offerBox.append(
        row('服务项目', offer.subject),
        row('正式报价', money(offer.amount)),
        row('预计时长', offer.estimatedDuration || '客服未单独填写'),
        row('报价状态', offer.status),
        row('有效期至', time(offer.expiresAt))
      );
      if (offer.customerMessage) {
        const note = document.createElement('div');
        note.className = 'notice';
        note.append(text('strong', '客服说明'), text('p', offer.customerMessage));
        offerBox.append(note);
      }
      panel.append(offerBox);

      if (state.canAccept) {
        const actions = document.createElement('div');
        actions.className = 'actions';
        const accept = button('接受报价并生成订单', async () => {
          if (!confirm(`确认接受 ${money(offer.amount)} 的正式报价？`)) return;
          accept.disabled = true;
          accept.textContent = '正在确认…';
          try {
            const next = await api(`/api/guest/business/${encodeURIComponent(businessNo)}/accept`, {method:'POST'});
            render(next);
          } catch (error) {
            alert(error.message || '确认报价失败');
            accept.disabled = false;
            accept.textContent = '接受报价并生成订单';
          }
        }, true);
        actions.append(accept);
        panel.append(actions);
      }
    }

    if (state.order) {
      const orderBox = document.createElement('section');
      orderBox.className = 'card soft-card';
      orderBox.append(text('h3', '订单状态'));
      orderBox.append(
        row('订单号', state.order.orderNo),
        row('状态', state.order.status),
        row('当前进度', state.order.progressText || '等待更新')
      );
      panel.append(orderBox);
    }

    const payment = state.payment;
    if (payment) {
      const payBox = document.createElement('section');
      payBox.className = 'card soft-card';
      payBox.append(text('h3', '微信支付'));
      payBox.append(row('金额', money(payment.amount)), row('支付状态', payment.status));

      if (payment.status === 'CAPTURED') {
        const ok = document.createElement('div');
        ok.className = 'notice success';
        ok.append(text('strong', '支付成功'), text('p', '订单已经收到付款，后续进度可继续在本页面查看。'));
        payBox.append(ok);
      } else if (payment.status === 'APPROVED') {
        const qr = document.createElement('img');
        qr.alt = '微信支付二维码';
        qr.width = 320;
        qr.height = 320;
        qr.loading = 'eager';
        qr.src = `/api/guest/business/${encodeURIComponent(businessNo)}/payment/qr.png?v=${Date.now()}`;
        qr.style.maxWidth = '100%';
        qr.style.height = 'auto';
        const qrWrap = document.createElement('div');
        qrWrap.className = 'payment-qr';
        qrWrap.append(qr, text('p', `请使用微信扫码支付 · 二维码有效期至 ${time(payment.expiresAt)}`, 'muted'));
        payBox.append(qrWrap);
      } else if (payment.status === 'CREATED') {
        const issue = button('生成微信支付二维码', async () => {
          issue.disabled = true;
          issue.textContent = '正在生成…';
          try {
            await api(`/api/guest/business/${encodeURIComponent(businessNo)}/payment/native`, {method:'POST'});
            await load();
          } catch (error) {
            alert(error.message || '生成支付二维码失败');
            issue.disabled = false;
            issue.textContent = '生成微信支付二维码';
          }
        }, true);
        const actions = document.createElement('div');
        actions.className = 'actions';
        actions.append(issue);
        payBox.append(actions);
      } else {
        payBox.append(text('p', '当前支付单不可继续支付，请联系客服重新确认报价。', 'muted'));
      }
      panel.append(payBox);
    }

    if (state.order && !payment) {
      const payBox = document.createElement('section');
      payBox.className = 'card soft-card';
      payBox.append(text('h3', '微信支付'));
      if (!state.paymentEnabled) {
        payBox.append(text('p', '当前网站尚未启用微信在线支付。订单和报价已经保留，请联系客服确认付款方式。', 'notice'));
      } else if (offer?.status === 'ACCEPTED') {
        payBox.append(text('p', '报价已确认，订单尚未生成付款单，可以在这里继续获取支付二维码。', 'muted'));
        const retry = button('生成微信支付二维码', async () => {
          retry.disabled = true;
          try {
            await api(`/api/guest/business/${encodeURIComponent(businessNo)}/payment/native`, {method:'POST'});
            await load();
          } catch (error) {
            alert(error.message || '创建支付二维码失败');
            retry.disabled = false;
          }
        }, true);
        payBox.append(retry);
      } else {
        payBox.append(text('p', '请先点击上方的「接受报价并生成订单」，确认金额后才能生成微信支付二维码。', 'muted'));
      }
      panel.append(payBox);
    }

    schedulePoll(state);
  }

  async function load() {
    try {
      const state = await api(`/api/guest/business/${encodeURIComponent(businessNo)}`);
      render(state);
    } catch (error) {
      clearTimeout(pollTimer);
      if (error.silent) {
        panel.hidden = true;
        panel.replaceChildren();
        return;
      }
      panel.hidden = false;
      panel.replaceChildren(text('p', `读取游客报价失败：${error.message}`, 'notice error'));
    }
  }

  load();
})();
