(() => {
  'use strict';

  function mount(panel, businessNo, options = {}) {
    if (!panel || !businessNo) return { dispose() {} };
    let pollTimer = null;
    let disposed = false;
    let generation = 0;
    const isActive = () => !disposed && panel.isConnected && (!options.isActive || options.isActive());

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
      node.addEventListener('click', event => {
        if (!isActive()) return;
        clearTimeout(pollTimer);
        generation++;
        onClick(event);
      });
      return node;
    };
    const text = (tag, value, className = '') => {
      const node = document.createElement(tag);
      if (className) node.className = className;
      node.textContent = value;
      return node;
    };

    async function api(path, options = {}) {
      const headers = {'Accept':'application/json', ...(options.headers || {})};
      if (/^(POST|PUT|PATCH|DELETE)$/i.test(options.method || 'GET')) {
        headers['X-Requested-With'] = 'fetch';
      }
      const response = await fetch(path, {
        credentials: 'same-origin',
        cache: 'no-store',
        ...options,
        headers
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

    const offerLabels = {QUOTED:'待确认',ACCEPTED:'已接受',EXPIRED:'已过期',CANCELLED:'已取消'};
    const orderLabels = {
      WAITING_CONFIRMATION: '待安排服务', IN_PROGRESS: '服务进行中',
      COMPLETED: '服务已完成', CLOSED: '订单已结单', EXCEPTION: '异常处理中'
    };
    const paymentLabels = {
      CREATED: '待生成支付二维码', APPROVED: '等待微信付款确认',
      CAPTURED: '微信已收款', CANCELLED: '已关闭', REFUNDED: '已退款', FAILED: '支付失败'
    };

    function schedulePoll(state) {
      clearTimeout(pollTimer);
      if (!isActive()) return;
      const status = state?.payment?.status;
      // After a successful payment the service can take hours/days. Keep a
      // lightweight guest-authorized poll so completion/closure appears without
      // relying on the customer to refresh or create another payment.
      if (status === 'CAPTURED' && !['CLOSED'].includes(state?.order?.status)) {
        pollTimer = setTimeout(load, 20000);
      } else if (status && !['CAPTURED','CANCELLED','REFUNDED','FAILED'].includes(status)) {
        pollTimer = setTimeout(load, 5000);
      }
    }

    function render(state) {
      if (!isActive()) return;
      panel.hidden = false;
      panel.replaceChildren();
      panel.append(text('div', '游客专属业务中心', 'eyebrow'));
      const paymentCaptured = state.payment?.status === 'CAPTURED';
      const serviceFinished = ['COMPLETED','CLOSED'].includes(state.order?.status);
      panel.append(text('h2', serviceFinished ? (state.order.status === 'CLOSED' ? '订单已完成并结单' : '服务已完成')
        : paymentCaptured ? '已付款 · 订单履约进度' : state.offer ? '正式报价与付款' : '等待客服报价'));

      const intro = paymentCaptured
        ? '微信付款已经确认，接下来由客服安排服务并更新履约进度。付款成功与服务完成是两个不同阶段。'
        : state.offer
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
          row('报价状态', offerLabels[offer.status] || offer.status),
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
              if (!isActive()) return;
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
          row('履约状态', orderLabels[state.order.status] || state.order.status),
          row('当前进度', state.order.progressText || '等待客服更新'),
          row('进度比例', state.order.progressPercent == null ? '尚未设置' : `${state.order.progressPercent}%`),
          row('最近更新', time(state.order.updatedAt))
        );
        if (state.order.finishedAt) orderBox.append(row('服务完成时间', time(state.order.finishedAt)));
        const milestones = [
          ['1 · 确认报价', state.offer?.status === 'ACCEPTED' || paymentCaptured],
          ['2 · 微信付款成功', paymentCaptured],
          ['3 · 服务开始', ['IN_PROGRESS','COMPLETED','CLOSED'].includes(state.order.status)],
          ['4 · 服务交付完成', serviceFinished],
          ['5 · 订单结单', state.order.status === 'CLOSED']
        ];
        const track = document.createElement('div');
        track.className = 'business-lifecycle';
        track.setAttribute('aria-label', '付款与履约时间线');
        for (const [label, done] of milestones) {
          const item = text('span', label, done ? 'is-done' : 'muted');
          item.style.display = 'inline-block';
          item.style.margin = '6px';
          track.append(item);
        }
        orderBox.append(track);
        if (state.order.status === 'CLOSED') {
          orderBox.append(text('p', '此订单已结单，服务履约结束。需要售后支持请联系平台客服。', 'notice success'));
        } else if (state.order.status === 'COMPLETED') {
          orderBox.append(text('p', '服务已完成，等待平台工作人员核对并结单。', 'notice success'));
        }
        panel.append(orderBox);
      }

      const payment = state.payment;
      if (payment) {
        const payBox = document.createElement('section');
        payBox.className = 'card soft-card';
        payBox.append(text('h3', '微信支付'));
        payBox.append(row('金额', money(payment.amount)), row('支付状态', paymentLabels[payment.status] || payment.status));
        if (payment.paidAt) payBox.append(row('确认收款时间', time(payment.paidAt)));

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
              if (!isActive()) return;
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
              if (!isActive()) return;
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
      if (!isActive()) return;
      const request = ++generation;
      try {
        const state = await api(`/api/guest/business/${encodeURIComponent(businessNo)}`);
        if (request !== generation || !isActive()) return;
        render(state);
      } catch (error) {
        if (request !== generation || !isActive()) return;
        clearTimeout(pollTimer);
        if (error.silent) {
          panel.hidden = true;
          panel.replaceChildren();
          return;
        }
        panel.hidden = false;
        panel.replaceChildren(text('p', `读取游客报价失败：${error.message}`, 'notice error'), button('重新读取报价与付款', load));
      }
    }

    function stop() { clearTimeout(pollTimer); generation++; }
    function resume(event) { if (event.persisted && isActive()) load(); }
    function dispose() {
      disposed = true; stop();
      window.removeEventListener('pagehide', stop);
      window.removeEventListener('pageshow', resume);
    }
    window.addEventListener('pagehide', stop);
    window.addEventListener('pageshow', resume);
    load();
    return { reload: load, dispose };
  }

  window.XHGuestBusiness = { mount };
  const detail = document.getElementById('recordDetailBox');
  const panel = document.getElementById('guestBusinessPanel');
  if (detail && panel && detail.dataset.businessNo) mount(panel, detail.dataset.businessNo);
})();
