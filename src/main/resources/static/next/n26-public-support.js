(() => {
  'use strict';

  const RECORDS_KEY = 'xh_client_records_v1';
  const routeMap = new Map([
    ['/next/records', 'records'],
    ['/next/records/', 'records'],
    ['/next/contact', 'contact'],
    ['/next/contact/', 'contact'],
    ['/next/trust', 'trust'],
    ['/next/trust/', 'trust']
  ]);
  const pages = new Map();
  const text = (value) => String(value ?? '').trim();
  const $ = (selector, root = document) => root.querySelector(selector);

  function make(tag, value, className) {
    const node = document.createElement(tag);
    if (value !== undefined && value !== null) node.textContent = String(value);
    if (className) node.className = className;
    return node;
  }

  function routeKey() {
    return routeMap.get(location.pathname) || '';
  }

  function installStyles() {
    if ($('#frontend-next-n26-style')) return;
    const style = document.createElement('style');
    style.id = 'frontend-next-n26-style';
    style.textContent = `
      .n26-shell{width:min(var(--max,1420px),calc(100% - 40px));margin:0 auto;padding:118px 0 72px;min-height:100vh}.n26-head{display:grid;grid-template-columns:1fr auto;gap:18px;align-items:end;margin-bottom:22px}.n26-head h1{font-size:clamp(42px,5vw,66px);margin:14px 0 8px}.n26-actions{display:flex;gap:8px;flex-wrap:wrap;justify-content:flex-end}.n26-panel{border:1px solid var(--line);border-radius:16px;background:#0c1015;padding:18px}.n26-muted{color:#89919b;font-size:10px;line-height:1.7}.n26-grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:12px}.n26-grid.three{grid-template-columns:repeat(3,minmax(0,1fr))}
      .n26-record-form{display:grid;grid-template-columns:minmax(0,1fr) auto;gap:10px;align-items:end}.n26-record-form label{display:grid;gap:6px;color:#aab1b9;font-size:10px}.n26-record-message{min-height:20px;margin-top:9px;font-size:10px;color:#87909a}.n26-record-message.error{color:#ff9b87}.n26-list{display:grid;gap:9px;margin-top:14px}.n26-record{display:flex;align-items:center;justify-content:space-between;gap:12px;border:1px solid #ffffff12;border-radius:12px;padding:13px;background:#090d11}.n26-record strong{font-size:12px}.n26-record small{display:block;color:#747d87;font-size:9px;margin-top:4px}.n26-empty{padding:26px 14px;border:1px dashed #ffffff20;border-radius:12px;text-align:center;color:#858d96;font-size:10px}
      .n26-contact-card{display:grid;gap:10px}.n26-contact-card strong{font-size:17px}.n26-contact-card p{margin:0;color:#9099a2;font-size:10px;line-height:1.7}.n26-contact-actions{display:flex;gap:7px;flex-wrap:wrap}.n26-qr{width:132px;max-width:100%;aspect-ratio:1;object-fit:contain;border-radius:12px;background:#fff;padding:5px}.n26-contact-layout{display:grid;grid-template-columns:minmax(0,1.25fr) minmax(280px,.75fr);gap:14px}.n26-contact-rows{display:grid;gap:9px}.n26-contact-row{display:grid;grid-template-columns:100px 1fr auto;gap:10px;align-items:center;padding:10px 0;border-bottom:1px solid #ffffff0d}.n26-contact-row:last-child{border-bottom:0}.n26-contact-row span:first-child{color:#77808a;font-size:9px}.n26-notice{margin-top:12px;border:1px solid #ffcb2d2b;background:#ffcb2d0a;border-radius:12px;padding:13px;color:#bdc3ca;font-size:10px;line-height:1.7}
      .n26-verify{display:grid;gap:0}.n26-verify-row{display:grid;grid-template-columns:150px 1fr;gap:14px;padding:13px 0;border-bottom:1px solid #ffffff0d}.n26-verify-row:last-child{border-bottom:0}.n26-verify-row strong{font-size:10px}.n26-verify-row span{color:#959da6;font-size:10px;line-height:1.7}.n26-case h2,.n26-faq h2{font-size:16px;margin:8px 0}.n26-case p,.n26-case small,.n26-faq p{color:#939ba4;font-size:10px;line-height:1.7;margin:0}.n26-tag{display:inline-flex;border:1px solid #ffffff18;border-radius:99px;padding:4px 7px;color:#bdc4cb;font-size:8px}.n26-section-title{font-size:22px;margin:28px 0 12px}.n26-copy-status{min-height:18px;font-size:9px;color:#76e3a3;margin-top:8px}
      @media(max-width:900px){.n26-grid.three{grid-template-columns:1fr 1fr}.n26-contact-layout{grid-template-columns:1fr}}@media(max-width:680px){.n26-shell{width:calc(100% - 26px);padding-top:92px}.n26-head{grid-template-columns:1fr}.n26-actions{justify-content:flex-start}.n26-grid,.n26-grid.three{grid-template-columns:1fr}.n26-record-form{grid-template-columns:1fr}.n26-record{align-items:flex-start;flex-direction:column}.n26-contact-row,.n26-verify-row{grid-template-columns:1fr;gap:5px}}
    `;
    document.head.append(style);
  }

  function header(kicker, title, accent, subtitle, legacyHref) {
    const head = make('section', null, 'n26-head');
    const copy = make('div');
    copy.append(make('div', kicker, 'kicker'));
    const h1 = make('h1'); h1.append(document.createTextNode(title), make('span', accent, 'g'));
    copy.append(h1, make('p', subtitle, 'sub'));
    const actions = make('div', null, 'n26-actions');
    const services = make('a', '浏览服务', 'btn'); services.href = '/next/services';
    const legacy = make('a', '查看旧版', 'btn'); legacy.href = legacyHref; legacy.dataset.n26Legacy = '1';
    actions.append(services, legacy); head.append(copy, actions); return head;
  }

  function normalizeRecordNo(value) {
    return text(value).toUpperCase().replace(/\s+/g, '');
  }

  function validRecordNo(value) {
    return /^XH[A-Z0-9]{10}$/.test(value) || /^XH-[A-Z2-9]{24}$/.test(value);
  }

  function savedRecords() {
    try {
      const rows = JSON.parse(localStorage.getItem(RECORDS_KEY) || '[]');
      return Array.isArray(rows) ? rows.filter((row) => row && validRecordNo(normalizeRecordNo(row.businessNo))).slice(0, 30) : [];
    } catch (ignore) { return []; }
  }

  function rememberRecord(record) {
    const businessNo = normalizeRecordNo(record?.businessNo);
    if (!validRecordNo(businessNo)) return;
    const rows = savedRecords().filter((row) => normalizeRecordNo(row.businessNo) !== businessNo);
    rows.unshift({ businessNo, stage: text(record?.stage), createdAt: record?.createdAt || null });
    try { localStorage.setItem(RECORDS_KEY, JSON.stringify(rows.slice(0, 30))); } catch (ignore) {}
  }

  function renderSavedRecords() {
    const host = $('#n26RecordList');
    if (!host) return;
    const rows = savedRecords();
    if (!rows.length) {
      host.replaceChildren(make('div', '当前浏览器还没有业务记录。提交询价后，或在上方输入业务编号查询，即可在这里快速返回。', 'n26-empty'));
      return;
    }
    host.replaceChildren(...rows.map((row) => {
      const card = make('article', null, 'n26-record');
      const copy = make('div'); copy.append(make('strong', normalizeRecordNo(row.businessNo)));
      const details = [text(row.stage), row.createdAt ? new Date(row.createdAt).toLocaleString('zh-CN', { hour12: false }) : ''].filter(Boolean).join(' · ');
      if (details) copy.append(make('small', details));
      const link = make('a', '查看记录 →', 'btn'); link.href = `/next/record/${encodeURIComponent(normalizeRecordNo(row.businessNo))}`;
      card.append(copy, link); return card;
    }));
  }

  async function lookupRecord(event) {
    event.preventDefault();
    const input = $('#n26BusinessNo');
    const message = $('#n26RecordMessage');
    const submit = $('#n26RecordSubmit');
    const businessNo = normalizeRecordNo(input?.value);
    if (!validRecordNo(businessNo)) {
      message.textContent = '业务编号格式不正确，请检查后重试。'; message.className = 'n26-record-message error'; input?.focus(); return;
    }
    submit.disabled = true; submit.textContent = '查询中…'; message.textContent = '正在核对业务记录…'; message.className = 'n26-record-message';
    try {
      const response = await fetch(`/api/records/${encodeURIComponent(businessNo)}`, { method: 'GET', credentials: 'same-origin', cache: 'no-store', headers: { Accept: 'application/json' } });
      const body = await response.json().catch(() => ({}));
      if (!response.ok || !text(body?.businessNo)) throw new Error(text(body?.message) || '没有找到该业务编号。');
      rememberRecord(body);
      location.assign(`/next/record/${encodeURIComponent(businessNo)}?from=records`);
    } catch (error) {
      message.textContent = text(error?.message) || '查询失败，请稍后重试。'; message.className = 'n26-record-message error';
      submit.disabled = false; submit.textContent = '查询并加入记录';
    }
  }

  function recordsPage() {
    const page = document.createElement('main'); page.className = 'page'; page.dataset.page = 'records';
    const shell = make('div', null, 'n26-shell');
    shell.append(header('FRONTEND NEXT · N26', '业务', '记录', '输入业务编号查询当前状态；当前浏览器保存的历史编号继续沿用旧版同一份本机记录。', '/records?legacy=1'));
    const panel = make('section', null, 'n26-panel');
    const form = make('form', null, 'n26-record-form'); form.id = 'n26RecordForm';
    const label = make('label'); label.append(make('span', '业务编号')); const input = document.createElement('input'); input.id = 'n26BusinessNo'; input.name = 'businessNo'; input.autocomplete = 'off'; input.autocapitalize = 'characters'; input.required = true; input.placeholder = '例如：XH7KM4Q2AZ8P'; label.append(input);
    const submit = make('button', '查询并加入记录', 'btn gold'); submit.id = 'n26RecordSubmit'; submit.type = 'submit'; form.append(label, submit); form.addEventListener('submit', lookupRecord);
    const message = make('div', '', 'n26-record-message'); message.id = 'n26RecordMessage'; message.setAttribute('aria-live', 'polite');
    const list = make('div', null, 'n26-list'); list.id = 'n26RecordList';
    panel.append(form, message, make('h2', '当前浏览器的最近记录', 'n26-section-title'), list); shell.append(panel); page.append(shell); return page;
  }

  function safeExternal(value) {
    const raw = text(value);
    if (!raw) return '';
    if (/^(weixin|wechat):\/\//i.test(raw)) return raw;
    try {
      const url = new URL(raw);
      return ['https:', 'http:'].includes(url.protocol) && !url.username && !url.password ? url.href : '';
    } catch (ignore) { return ''; }
  }

  function safeImage(value) {
    const raw = text(value);
    if (!raw) return '';
    if (raw.startsWith('/') && !raw.startsWith('//')) return raw;
    return safeExternal(raw);
  }

  async function copyText(value, host) {
    const raw = text(value);
    if (!raw) return;
    try {
      await navigator.clipboard.writeText(raw);
      if (host) host.textContent = '已复制';
    } catch (ignore) {
      if (host) host.textContent = '复制失败，请手动选择';
    }
  }

  function contactChannel(item, index) {
    const card = make('article', null, 'n26-panel n26-contact-card');
    card.append(make('span', text(item?.label) || `客服 ${index + 1}`, 'n26-tag'));
    const display = text(item?.wechatId) || (safeExternal(item?.jumpUrl) ? '点击联系客服' : '在线客服');
    card.append(make('strong', display));
    if (text(item?.note)) card.append(make('p', item.note));
    const image = safeImage(item?.qrImage);
    if (image) { const img = document.createElement('img'); img.className = 'n26-qr'; img.src = image; img.alt = `${text(item?.label) || '客服'}二维码`; img.loading = 'lazy'; card.append(img); }
    const actions = make('div', null, 'n26-contact-actions');
    const status = make('span', '', 'n26-copy-status');
    if (text(item?.wechatId) && !safeExternal(item.wechatId)) { const copy = make('button', '复制微信', 'btn'); copy.type = 'button'; copy.addEventListener('click', () => copyText(item.wechatId, status)); actions.append(copy); }
    const jump = safeExternal(item?.jumpUrl) || safeExternal(item?.wechatId);
    if (jump) { const open = make('a', '打开客服', 'btn gold'); open.href = jump; open.target = '_blank'; open.rel = 'noopener noreferrer'; actions.append(open); }
    if (actions.childElementCount) card.append(actions, status);
    return card;
  }

  function contactRow(label, value, action) {
    const row = make('div', null, 'n26-contact-row'); row.append(make('span', label), make('strong', value || '—'));
    const actions = make('div', null, 'n26-contact-actions');
    if (action) actions.append(action);
    row.append(actions); return row;
  }

  function contactPage(config) {
    const page = document.createElement('main'); page.className = 'page'; page.dataset.page = 'contact';
    const shell = make('div', null, 'n26-shell');
    shell.append(header('FRONTEND NEXT · N26', '一对一', '咨询', text(config?.contactIntro) || '联系客服确认游戏区服、当前进度、任务目标和交付时间。网页不收账号密码、验证码或令牌。', '/contact?legacy=1'));
    const layout = make('div', null, 'n26-contact-layout');
    const left = make('section');
    const channels = Array.isArray(config?.contactChannels) ? config.contactChannels.filter((row) => row && (text(row.wechatId) || text(row.jumpUrl) || text(row.qrImage))) : [];
    const fallback = channels.length ? channels : [{ label: '主客服', wechatId: text(config?.wechatId) || 'AAC9878', qrImage: config?.wechatQrImage, note: '发送业务编号确认报价。' }];
    const channelGrid = make('div', null, 'n26-grid'); fallback.slice(0, 6).forEach((item, index) => channelGrid.append(contactChannel(item, index)));
    const wecom = safeExternal(config?.wecomKefuUrl);
    if (wecom) {
      const card = make('article', null, 'n26-panel n26-contact-card'); card.append(make('span', text(config?.wecomKefuLabel) || '企业微信客服', 'n26-tag'), make('strong', text(config?.quickContactTitle) || '企业微信在线咨询'), make('p', text(config?.wecomKefuNote) || '发送业务编号或截图说明需求。'));
      const open = make('a', text(config?.wecomKefuButton) || '联系客服', 'btn gold'); open.href = wecom; open.target = '_blank'; open.rel = 'noopener noreferrer'; card.append(open); channelGrid.prepend(card);
    }
    left.append(channelGrid);

    const side = make('section', null, 'n26-panel n26-contact-rows');
    const copyStatus = make('div', '', 'n26-copy-status');
    const wechat = text(config?.wechatId) || 'AAC9878'; const copyWechat = make('button', '复制', 'btn'); copyWechat.type = 'button'; copyWechat.addEventListener('click', () => copyText(wechat, copyStatus)); side.append(contactRow('微信', wechat, copyWechat));
    if (text(config?.qqId)) { const qq = safeExternal(config?.qqLink); const action = qq ? make('a', '打开 QQ', 'btn') : make('button', '复制', 'btn'); if (qq) { action.href = qq; action.target = '_blank'; action.rel = 'noopener'; } else { action.type = 'button'; action.addEventListener('click', () => copyText(config.qqId, copyStatus)); } side.append(contactRow('QQ', config.qqId, action)); }
    if (text(config?.phone)) { const call = make('a', '拨打', 'btn'); call.href = `tel:${text(config.phone).replace(/[^+\d]/g, '')}`; side.append(contactRow('电话', config.phone, call)); }
    side.append(contactRow('客服时间', text(config?.serviceHours) || '10:00–24:00'));
    const taobao = safeExternal(config?.taobaoUrl); if (taobao) { const a = make('a', text(config?.taobaoLabel) || '打开淘宝', 'btn'); a.href = taobao; a.target = '_blank'; a.rel = 'noopener'; side.append(contactRow('淘宝', text(config?.taobaoLabel) || '店铺入口', a)); }
    const xianyu = safeExternal(config?.xianyuUrl); if (xianyu) { const a = make('a', text(config?.xianyuLabel) || '打开闲鱼', 'btn'); a.href = xianyu; a.target = '_blank'; a.rel = 'noopener'; side.append(contactRow('闲鱼', text(config?.xianyuLabel) || '主页入口', a)); }
    side.append(contactRow('经营主体', text(config?.legalEntityName) || '星维网络服务工作室'), copyStatus);
    const notice = make('div', text(config?.contactNotice) || '网页不要发送账号密码、验证码或令牌。客服确认金额后，请按实际交易渠道核对订单与售后。', 'n26-notice');
    side.append(copyStatus, notice); layout.append(left, side); shell.append(layout); page.append(shell); return page;
  }

  function externalLink(label, href) {
    const link = make('a', label); link.href = href; link.target = '_blank'; link.rel = 'noopener noreferrer'; return link;
  }

  function verifyRow(label, content) {
    const row = make('div', null, 'n26-verify-row'); row.append(make('strong', label)); const value = make('span');
    if (content instanceof Node) value.append(content); else value.textContent = String(content || ''); row.append(value); return row;
  }

  function trustPage(config) {
    const page = document.createElement('main'); page.className = 'page'; page.dataset.page = 'trust';
    const shell = make('div', null, 'n26-shell');
    shell.append(header('FRONTEND NEXT · N26', '信任与', 'FAQ', '服务口径、主体备案、进度核验与常见问题。案例只用于说明服务方式，不作为成交记录。', '/trust?legacy=1'));
    const verify = make('section', null, 'n26-panel n26-verify');
    verify.append(
      verifyRow('经营主体', text(config?.legalEntityName) || '星维网络服务工作室'),
      verifyRow('网站备案', externalLink('蜀ICP备2026047129号', 'https://beian.miit.gov.cn/')),
      verifyRow('公安备案', externalLink('川公网安备51162102000262号', 'https://www.beian.gov.cn/portal/registerSystemInfo?recordcode=51162102000262')),
      verifyRow('交易与付款', '客服确认报价后，可选择淘宝、闲鱼担保；已绑定账号且开放支付的官网订单可在个人中心使用微信扫码。请按实际付款渠道核对订单与售后。'),
      verifyRow('我们不做什么', '不在网页索取账号密码、验证码或令牌；不使用外挂、注入器与脚本。进度与异常处理按业务编号同步。')
    );
    const recordCopy = make('span'); recordCopy.append(document.createTextNode('每笔需求生成唯一业务编号，可在 ')); const recordLink = make('a', '业务记录'); recordLink.href = '/next/records'; recordCopy.append(recordLink, document.createTextNode(' 中自行核对当前状态与客服备注。')); verify.append(verifyRow('可自行核对', recordCopy)); shell.append(verify);

    const cases = Array.isArray(config?.trustCases) ? config.trustCases : [];
    if (cases.length) {
      shell.append(make('h2', '服务示例', 'n26-section-title'));
      const grid = make('section', null, 'n26-grid three');
      cases.slice(0, 8).forEach((item) => { const card = make('article', null, 'n26-panel n26-case'); card.append(make('span', text(item?.tag) || '服务示例', 'n26-tag'), make('h2', text(item?.title)), make('p', text(item?.metric))); if (text(item?.detail)) card.append(make('small', item.detail)); grid.append(card); });
      shell.append(grid);
    }
    const faq = Array.isArray(config?.trustFaq) ? config.trustFaq : [];
    if (faq.length) {
      shell.append(make('h2', '常见问题', 'n26-section-title'));
      const grid = make('section', null, 'n26-grid');
      faq.slice(0, 12).forEach((item) => { const card = make('article', null, 'n26-panel n26-faq'); card.append(make('h2', text(item?.question)), make('p', text(item?.answer))); grid.append(card); });
      shell.append(grid);
    }
    page.append(shell); return page;
  }

  async function configuration() {
    const shell = window.XHFrontendShell;
    if (shell?.configurationReady) {
      try { await shell.configurationReady; } catch (ignore) {}
      if (shell.configuration) return shell.configuration;
    }
    try {
      const response = await fetch('/api/site-config', { cache: 'no-store', headers: { Accept: 'application/json' } });
      return response.ok ? await response.json() : {};
    } catch (ignore) { return {}; }
  }

  function attachPage(page, key) {
    page.id = `n26-${key}`;
    const footer = $('footer'); if (footer) footer.before(page); else document.body.append(page);
    pages.set(key, page); return page;
  }

  function metadata(key, config) {
    const brand = text(config?.brandName) || '星火网游';
    const map = {
      records: ['业务记录', '/records', 'noindex,follow'],
      contact: ['一对一咨询', '/contact', 'noindex,follow'],
      trust: ['信任与 FAQ', '/trust', 'noindex,follow']
    };
    const [title, canonicalPath, robotsValue] = map[key]; document.title = `${title} · ${brand}`;
    let canonical = $('link[rel="canonical"]'); if (!canonical) { canonical = document.createElement('link'); canonical.rel = 'canonical'; document.head.append(canonical); } canonical.href = `https://wydlgf.cn${canonicalPath}`;
    let robots = $('meta[name="robots"]'); if (!robots) { robots = document.createElement('meta'); robots.name = 'robots'; document.head.append(robots); } robots.content = robotsValue;
  }

  async function activate() {
    const key = routeKey();
    if (!key) return false;
    installStyles();
    const config = await configuration();
    if (key !== routeKey()) return false;
    let page = pages.get(key);
    if (!page) {
      page = key === 'records' ? recordsPage() : key === 'contact' ? contactPage(config) : trustPage(config);
      attachPage(page, key);
    }
    document.querySelectorAll('.page').forEach((node) => node.classList.toggle('active', node === page));
    document.documentElement.dataset.frontendPage = key;
    metadata(key, config);
    if (key === 'records') renderSavedRecords();
    window.XHFrontendNextN14?.normalize?.(page);
    return true;
  }

  window.addEventListener('popstate', activate);
  window.XHFrontendNextN26 = { activate, validRecordNo, savedRecords, rememberRecord };
  activate();
})();
