(() => {
  'use strict';

  const $ = (selector, root = document) => root.querySelector(selector);
  const text = (value) => String(value ?? '').trim();
  const page = $('.page[data-page="account"]');
  if (!page) return;

  const state = {
    profile: null,
    orders: null,
    sessions: [],
    events: [],
    wechatEnabled: false,
    loading: false
  };

  function node(tag, className, value) {
    const element = document.createElement(tag);
    if (className) element.className = className;
    if (value !== undefined && value !== null) element.textContent = String(value);
    return element;
  }

  function labelFor(value, id) {
    const label = node('label', '', value);
    label.htmlFor = id;
    return label;
  }

  function append(parent, ...children) {
    children.filter(Boolean).forEach((child) => parent.append(child));
    return parent;
  }

  function button(label, className = 'btn') {
    const element = node('button', className, label);
    element.type = 'button';
    return element;
  }

  function link(label, href, className = '') {
    const element = node('a', className, label);
    element.href = href;
    return element;
  }

  function safeAvatar(url) {
    const value = text(url);
    return value.startsWith('/') || value.startsWith('https://') ? value : '';
  }

  function fmtDate(value) {
    if (!value) return '—';
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return '—';
    return new Intl.DateTimeFormat('zh-CN', {
      year: 'numeric', month: '2-digit', day: '2-digit',
      hour: '2-digit', minute: '2-digit'
    }).format(date);
  }

  function statusClass(status) {
    const value = text(status).toUpperCase();
    if (['DONE', 'COMPLETED', 'FINISHED', 'CLOSED'].includes(value)) return 'n4-status n4-ok';
    if (['CANCELLED', 'CANCELED', 'FAILED', 'REFUNDED'].includes(value)) return 'n4-status n4-muted';
    return 'n4-status n4-run';
  }

  async function request(url, options = {}) {
    const headers = Object.assign({ Accept: 'application/json' }, options.headers || {});
    const response = await fetch(url, Object.assign({ credentials: 'same-origin', cache: 'no-store' }, options, { headers }));
    let body = null;
    try { body = await response.json(); } catch (ignore) { body = null; }
    if (!response.ok) {
      const error = new Error(text(body?.message) || text(body?.error) || `请求失败（${response.status}）`);
      error.status = response.status;
      error.body = body;
      throw error;
    }
    return body;
  }

  function installStyles() {
    if ($('#frontend-next-n4-style')) return;
    const style = document.createElement('style');
    style.id = 'frontend-next-n4-style';
    style.textContent = `
      .n4-shell{padding:112px 0 30px;position:relative;overflow:hidden;border-bottom:1px solid var(--line)}
      .n4-shell:before{content:"";position:absolute;inset:0;z-index:-1;background:radial-gradient(circle at 78% 32%,#ffcb2d22,transparent 22rem),linear-gradient(110deg,#07090c 0 48%,#07090c77)}
      .n4-top{display:flex;align-items:center;justify-content:space-between;gap:24px;flex-wrap:wrap}
      .n4-profile{display:flex;align-items:center;gap:16px;margin-top:20px}.n4-avatar{width:72px;height:72px;border:2px solid var(--gold);border-radius:50%;display:grid;place-items:center;background:#15120c;color:var(--gold);font-size:26px;overflow:hidden}.n4-avatar img{width:100%;height:100%;object-fit:cover}.n4-profile h1{font-size:clamp(36px,5vw,60px);margin:0 0 8px}.n4-profile p{margin:0;color:var(--muted);font-size:12px}
      .n4-stats{display:grid;grid-template-columns:repeat(4,minmax(0,1fr));gap:10px;margin-top:28px}.n4-stat{border:1px solid var(--line);background:#10141acc;border-radius:14px;padding:16px}.n4-stat small{display:block;color:#858c95;margin-bottom:7px}.n4-stat strong{font-size:27px}.n4-stat span{display:block;color:#6f7780;font-size:9px;margin-top:5px}
      .n4-section{padding:28px 0 84px}.n4-grid{display:grid;grid-template-columns:minmax(0,1fr) 310px;gap:14px}.n4-panel{border:1px solid var(--line);background:#0c1015;border-radius:16px;padding:18px;min-width:0}.n4-panel h2,.n4-panel h3{margin:0 0 12px}.n4-panel-title{display:flex;justify-content:space-between;align-items:center;gap:12px;margin-bottom:14px}.n4-note{color:var(--muted);font-size:11px;line-height:1.65}.n4-table-wrap{overflow:auto}.n4-table{width:100%;border-collapse:collapse;min-width:700px}.n4-table th,.n4-table td{padding:12px 9px;border-bottom:1px solid #ffffff10;text-align:left;font-size:11px}.n4-table th{color:#7d848d;font-weight:600}.n4-status{display:inline-flex;align-items:center;gap:6px}.n4-status:before{content:"";width:6px;height:6px;border-radius:50%;background:currentColor;box-shadow:0 0 8px currentColor}.n4-run{color:var(--gold)}.n4-ok{color:var(--green)}.n4-muted{color:#8c939c}.n4-progress{min-width:100px}.n4-progress-bar{height:6px;background:#282e34;border-radius:99px;overflow:hidden;margin-bottom:5px}.n4-progress-bar i{display:block;height:100%;background:linear-gradient(90deg,#e7a300,var(--gold));border-radius:99px}.n4-empty{padding:34px 16px;text-align:center;border:1px dashed #ffffff18;border-radius:12px;color:#777f89;font-size:11px;line-height:1.7}.n4-stack{display:grid;gap:12px}.n4-item{padding:12px 0;border-bottom:1px solid #ffffff10}.n4-item:last-child{border-bottom:0}.n4-item b{display:block;font-size:12px;margin-bottom:5px}.n4-item span{color:#7f8791;font-size:10px;line-height:1.5}.n4-current{color:var(--green)!important}.n4-actions{display:flex;gap:8px;flex-wrap:wrap}.n4-actions .btn{font-size:11px}.n4-login-wrap{min-height:calc(100vh - 110px);padding:118px 0 70px;display:grid;place-items:center}.n4-login{width:min(940px,100%);display:grid;grid-template-columns:1.05fr .95fr;border:1px solid var(--line);border-radius:22px;overflow:hidden;background:#0c1015;box-shadow:0 30px 90px #0008}.n4-login-copy{padding:42px;background:radial-gradient(circle at 20% 10%,#ffcb2d20,transparent 18rem),#0f1318}.n4-login-copy h1{font-size:clamp(38px,5vw,62px);margin:18px 0}.n4-login-copy p{color:#9ba2ab;line-height:1.75}.n4-login-form{padding:38px}.n4-fields{display:grid;gap:10px}.n4-field{display:grid;gap:6px}.n4-field label{font-size:11px;color:#aab0b8}.n4-field input,.n4-field select{width:100%;background:#090d11;color:#fff;border:1px solid var(--line);border-radius:10px;padding:12px}.n4-field input:focus,.n4-field select:focus{outline:none;border-color:#ffcb2d88;box-shadow:0 0 0 3px #ffcb2d12}.n4-login-form .btn{width:100%;margin-top:10px}.n4-divider{display:flex;align-items:center;gap:10px;color:#666f79;font-size:10px;margin:18px 0}.n4-divider:before,.n4-divider:after{content:"";height:1px;background:var(--line);flex:1}.n4-message{min-height:18px;margin-top:10px;color:#9ea5ad;font-size:11px;line-height:1.5}.n4-message.error{color:#ff9b87}.n4-message.ok{color:#76e3a3}.n4-wechat{border-color:#2dcf73!important;color:#8ce9b3!important}.n4-mini-links{display:flex;justify-content:space-between;gap:10px;margin-top:15px;font-size:10px;color:#929aa3}.n4-mini-links a:hover{color:#fff}
      @media(max-width:900px){.n4-grid{grid-template-columns:1fr}.n4-stats{grid-template-columns:1fr 1fr}.n4-login{grid-template-columns:1fr}.n4-login-copy{padding:30px}.n4-login-form{padding:30px}}
      @media(max-width:560px){.n4-stats{grid-template-columns:1fr 1fr}.n4-shell{padding-top:94px}.n4-section{padding-top:18px}.n4-login-wrap{padding-top:88px}.n4-login-copy,.n4-login-form{padding:23px}.n4-profile h1{font-size:34px}}
    `;
    document.head.append(style);
  }

  function renderAvatar(host, profile) {
    host.replaceChildren();
    const url = safeAvatar(profile?.avatarUrl);
    if (url) {
      const image = document.createElement('img');
      image.src = url;
      image.alt = '';
      image.addEventListener('error', () => {
        host.replaceChildren(node('span', '', (text(profile?.displayName) || '星').slice(0, 1)));
      }, { once: true });
      host.append(image);
    } else {
      host.append(node('span', '', (text(profile?.displayName) || '星').slice(0, 1)));
    }
  }

  function renderLoading() {
    page.replaceChildren();
    const wrap = node('div', 'n4-login-wrap');
    const card = node('div', 'n4-panel');
    card.style.maxWidth = '460px';
    card.style.width = '100%';
    card.style.textAlign = 'center';
    append(card, node('div', 'kicker', '账号服务'), node('h2', '', '正在读取账号状态…'), node('p', 'n4-note', '只读取当前浏览器自己的登录会话。'));
    append(wrap, card);
    page.append(wrap);
  }

  function loginMessage(message, kind = '') {
    const host = $('#n4LoginMessage');
    if (!host) return;
    host.className = `n4-message ${kind}`.trim();
    host.textContent = message || '';
  }

  async function doLogin(form) {
    const submit = $('#n4LoginButton');
    if (!submit || submit.disabled) return;
    const contactType = text($('#n4ContactType')?.value);
    const contactValue = text($('#n4ContactValue')?.value);
    const password = String($('#n4Password')?.value || '');
    const remember = !!$('#n4Remember')?.checked;
    if (!contactValue || !password) {
      loginMessage('请填写登录账号和密码。', 'error');
      return;
    }
    submit.disabled = true;
    submit.textContent = '正在登录…';
    loginMessage('正在建立安全会话…');
    try {
      state.profile = await request('/api/account/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', 'X-Requested-With': 'fetch' },
        body: JSON.stringify({ contactType, contactValue, password, remember })
      });
      loginMessage('登录成功，正在载入个人中心。', 'ok');
      await hydrateAuthenticated();
      const shell=window.XHFrontendShell;
      if(shell){const target=shell.safeAccountReturn(new URLSearchParams(location.search).get('next')) || (/^\/account\/login$/.test(location.pathname)?shell.route('account'):null);if(target && target!==location.pathname+location.search+location.hash)location.assign(target);}
    } catch (error) {
      loginMessage(text(error.message) || '登录失败，请检查账号信息。', 'error');
    } finally {
      submit.disabled = false;
      submit.textContent = '登录并进入个人中心 →';
    }
  }

  async function loadWeChatAvailability() {
    try {
      const result = await request('/api/account/wechat/availability');
      state.wechatEnabled = !!result?.enabled;
    } catch (ignore) {
      state.wechatEnabled = false;
    }
  }

  function renderGuest() {
    page.replaceChildren();
    const outer = node('div', 'shell n4-login-wrap');
    const card = node('div', 'n4-login');
    const copy = node('section', 'n4-login-copy');
    append(copy,
      node('div', 'kicker', '账号服务'),
      node('h1', '', '登录后查看\n你的真实服务记录'),
      node('p', '', '查看当前账号的订单、登录设备和安全活动。')
    );
    const trust = node('div', 'trust');
    [['账号隔离', '只显示当前账号数据'], ['微信扫码', '配置可用时直接登录'], ['设备管理', '查看当前登录设备'], ['旧版回退', '原账号中心继续保留']].forEach(([a, b]) => {
      append(trust, append(node('div'), node('b', '', a), node('span', '', b)));
    });
    copy.append(trust);

    const form = node('form', 'n4-login-form');
    form.id = 'n4LoginForm';
    form.addEventListener('submit', (event) => { event.preventDefault(); doLogin(form); });
    append(form, node('h2', '', '账号登录'), node('p', 'n4-note', '支持邮箱、手机号、QQ、微信号凭据；也可使用微信开放平台扫码登录。'));
    const fields = node('div', 'n4-fields');
    const typeWrap = node('div', 'n4-field');
    const type = document.createElement('select');
    type.id = 'n4ContactType';
    [['PHONE', '手机号'], ['EMAIL', '邮箱'], ['QQ', 'QQ'], ['WECHAT', '微信号']].forEach(([value, label]) => {
      const option = node('option', '', label); option.value = value; type.append(option);
    });
    append(typeWrap, labelFor('登录方式', 'n4ContactType'), type);
    const contactWrap = node('div', 'n4-field');
    const contact = document.createElement('input'); contact.id = 'n4ContactValue'; contact.autocomplete = 'username'; contact.maxLength = 120; contact.placeholder = '请输入账号';
    append(contactWrap, labelFor('账号', 'n4ContactValue'), contact);
    const passwordWrap = node('div', 'n4-field');
    const password = document.createElement('input'); password.id = 'n4Password'; password.type = 'password'; password.autocomplete = 'current-password'; password.maxLength = 72; password.placeholder = '请输入密码';
    append(passwordWrap, labelFor('密码', 'n4Password'), password);
    append(fields, typeWrap, contactWrap, passwordWrap);
    form.append(fields);
    const rememberLabel = node('label', 'n4-note');
    rememberLabel.style.display = 'flex'; rememberLabel.style.gap = '8px'; rememberLabel.style.alignItems = 'center'; rememberLabel.style.marginTop = '11px';
    const remember = document.createElement('input'); remember.type = 'checkbox'; remember.id = 'n4Remember';
    append(rememberLabel, remember, document.createTextNode('在此设备保持登录'));
    form.append(rememberLabel);
    const submit = button('登录并进入个人中心 →', 'btn gold'); submit.id = 'n4LoginButton'; submit.type = 'submit'; form.append(submit);
    const message = node('div', 'n4-message'); message.id = 'n4LoginMessage'; message.setAttribute('aria-live', 'polite'); form.append(message);
    form.append(node('div', 'n4-divider', '或'));
    const wx = button(state.wechatEnabled ? '微信扫码登录' : '微信扫码登录暂不可用', 'btn n4-wechat');
    wx.id = 'n4WechatLogin'; wx.disabled = !state.wechatEnabled;
    wx.addEventListener('click', () => {
      if (!state.wechatEnabled) return;
      location.assign('/api/account/wechat/start?next=' + encodeURIComponent('/next/account'));
    });
    form.append(wx);
    const links = node('div', 'n4-mini-links');
    append(links, link('注册账号', '/account/register'), link('忘记密码', '/account/reset'), link('打开旧版账号中心', '/account?legacy=1'));
    form.append(links);
    append(card, copy, form);
    outer.append(card);
    page.append(outer);
  }

  function orderStatusText(order) {
    return text(order?.statusText) || text(order?.status) || '状态更新中';
  }

  function countOrders(items, kinds) {
    return items.filter((item) => kinds.includes(text(item?.status).toUpperCase())).length;
  }

  function stat(label, value, note) {
    const card = node('div', 'n4-stat');
    append(card, node('small', '', label), node('strong', '', value), node('span', '', note));
    return card;
  }

  function renderOrdersPanel() {
    const panel = node('section', 'n4-panel');
    const title = node('div', 'n4-panel-title');
    append(title, node('h2', '', '我的订单'), link('打开旧版完整中心 →', '/account?legacy=1', 'btn'));
    panel.append(title);
    const items = Array.isArray(state.orders?.items) ? state.orders.items : [];
    if (!items.length) {
      append(panel, node('div', 'n4-empty', '当前账号还没有已归属订单。\n历史订单可在旧版账号中心按业务编号认领。'));
      return panel;
    }
    const wrap = node('div', 'n4-table-wrap');
    const table = node('table', 'n4-table');
    const thead = document.createElement('thead');
    const hr = document.createElement('tr');
    ['业务编号', '服务', '游戏', '状态', '进度', '更新时间'].forEach((label) => hr.append(node('th', '', label)));
    thead.append(hr); table.append(thead);
    const tbody = document.createElement('tbody');
    items.forEach((order) => {
      const tr = document.createElement('tr');
      tr.append(node('td', '', text(order.businessNo) || '—'));
      tr.append(node('td', '', text(order.serviceName) || '—'));
      tr.append(node('td', '', text(order.gameName) || '—'));
      tr.append(append(node('td'), node('span', statusClass(order.status), orderStatusText(order))));
      const progressCell = node('td');
      const progress = Number(order.progressPercent);
      if (Number.isFinite(progress)) {
        const box = node('div', 'n4-progress');
        const bar = node('div', 'n4-progress-bar');
        const fill = document.createElement('i'); fill.style.width = `${Math.max(0, Math.min(100, progress))}%`; bar.append(fill);
        append(box, bar, node('span', 'n4-note', `${Math.round(progress)}%${text(order.progressText) ? ` · ${text(order.progressText)}` : ''}`));
        progressCell.append(box);
      } else {
        progressCell.textContent = text(order.progressText) || '—';
      }
      tr.append(progressCell, node('td', '', fmtDate(order.updatedAt || order.createdAt)));
      tbody.append(tr);
    });
    table.append(tbody); wrap.append(table); panel.append(wrap);
    return panel;
  }

  function renderSidePanel() {
    const side = node('aside', 'n4-stack');
    const devicePanel = node('section', 'n4-panel');
    append(devicePanel, node('h3', '', '登录设备'));
    if (!state.sessions.length) devicePanel.append(node('p', 'n4-note', '暂无设备信息。'));
    state.sessions.slice(0, 4).forEach((session) => {
      const item = node('div', 'n4-item');
      const title = text(session.deviceLabel) || '当前设备';
      const meta = `${text(session.ip) || 'IP 已脱敏'} · ${fmtDate(session.lastSeenAt)}`;
      append(item, node('b', session.current ? 'n4-current' : '', session.current ? `${title} · 当前` : title), node('span', '', meta));
      devicePanel.append(item);
    });
    const eventsPanel = node('section', 'n4-panel');
    append(eventsPanel, node('h3', '', '最近安全活动'));
    if (!state.events.length) eventsPanel.append(node('p', 'n4-note', '暂无安全活动记录。'));
    state.events.slice(0, 5).forEach((event) => {
      const item = node('div', 'n4-item');
      append(item, node('b', '', text(event.title) || '账号安全活动'), node('span', '', `${fmtDate(event.createdAt)} · ${text(event.deviceLabel) || '设备未知'} · ${text(event.ipMasked) || 'IP 已脱敏'}`));
      eventsPanel.append(item);
    });
    append(side, devicePanel, eventsPanel);
    return side;
  }

  function renderAuthenticated() {
    const profile = state.profile || {};
    const items = Array.isArray(state.orders?.items) ? state.orders.items : [];
    const total = Number.isFinite(Number(state.orders?.total)) ? Number(state.orders.total) : items.length;
    const running = countOrders(items, ['PENDING', 'PROCESSING', 'IN_PROGRESS', 'CONFIRMED', 'PAID']);
    const done = countOrders(items, ['DONE', 'COMPLETED', 'FINISHED', 'CLOSED']);

    page.replaceChildren();
    const head = node('section', 'n4-shell');
    const shell = node('div', 'shell');
    const top = node('div', 'n4-top');
    const left = node('div');
    left.append(node('div', 'kicker', '当前账号'));
    const profileRow = node('div', 'n4-profile');
    const avatar = node('div', 'n4-avatar'); renderAvatar(avatar, profile);
    const copy = node('div');
    append(copy, node('h1', '', `欢迎回来，${text(profile.displayName) || '星火用户'}`), node('p', '', `${text(profile.contactMasked) || '登录凭据已保护'} · 注册于 ${fmtDate(profile.createdAt)}`));
    append(profileRow, avatar, copy); left.append(profileRow);
    const actions = node('div', 'n4-actions');
    const legacy = link('旧版完整中心', '/account?legacy=1', 'btn');
    const logout = button('退出登录', 'btn');
    logout.addEventListener('click', doLogout);
    append(actions, legacy, logout);
    append(top, left, actions); shell.append(top);
    const stats = node('div', 'n4-stats');
    append(stats,
      stat('已归属订单', total, '真实订单总数'),
      stat('本页订单', items.length, `第 ${Number(state.orders?.page || 0) + 1} 页`),
      stat('本页进行中', running, '按当前页状态统计'),
      stat('本页已完成', done, '按当前页状态统计')
    );
    shell.append(stats); head.append(shell); page.append(head);

    const section = node('section', 'n4-section');
    const content = node('div', 'shell n4-grid');
    append(content, renderOrdersPanel(), renderSidePanel()); section.append(content); page.append(section);
    const badge = $('.badge'); if (badge) badge.textContent = location.pathname.startsWith('/next') ? '新版预览' : '星火服务平台';
  }

  async function doLogout() {
    try {
      await request('/api/account/logout', { method: 'POST', headers: { 'X-Requested-With': 'fetch' } });
    } catch (ignore) {
      // Even when the network response is lost, re-check /me before showing authenticated content again.
    }
    state.profile = null; state.orders = null; state.sessions = []; state.events = [];
    await boot();
  }

  async function hydrateAuthenticated() {
    const results = await Promise.allSettled([
      request('/api/account/orders?page=0&size=10'),
      request('/api/account/sessions'),
      request('/api/account/security/events')
    ]);
    state.orders = results[0].status === 'fulfilled' ? results[0].value : { items: [], total: 0, page: 0, size: 10 };
    state.sessions = results[1].status === 'fulfilled' && Array.isArray(results[1].value) ? results[1].value : [];
    state.events = results[2].status === 'fulfilled' && Array.isArray(results[2].value) ? results[2].value : [];
    renderAuthenticated();
  }

  async function boot() {
    if (state.loading) return;
    state.loading = true;
    installStyles();
    renderLoading();
    await loadWeChatAvailability();
    try {
      state.profile = await request('/api/account/me');
      await hydrateAuthenticated();
    } catch (error) {
      if (error.status === 401) {
        state.profile = null;
        renderGuest();
      } else {
        page.replaceChildren();
        const wrap = node('div', 'shell n4-login-wrap');
        const panel = node('div', 'n4-panel');
        append(panel, node('h2', '', '个人中心暂时无法读取'), node('p', 'n4-note', text(error.message) || '请稍后重试，旧版账号中心仍可使用。'), link('打开旧版账号中心 →', '/account?legacy=1', 'btn'));
        wrap.append(panel); page.append(wrap);
      }
    } finally {
      state.loading = false;
    }
  }

  // N1 的页面切换是单页路由；N4 预先读取一次账号状态，切到个人中心时无需再等待。
  // 页面重新聚焦时若当前正在个人中心，再轻量刷新，避免长时间打开后的会话状态过期。
  document.addEventListener('visibilitychange', () => {
    if (document.visibilityState === 'visible' && page.classList.contains('active')) boot();
  });

  window.XHFrontendNextN4 = { reload: boot, state };
  boot();
})();
