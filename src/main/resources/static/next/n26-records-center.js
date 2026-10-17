(() => {
  'use strict';

  if (!/^\/next\/records\/?$/.test(location.pathname)) return;

  const STAGE = 'N26 · RECORDS CENTER';
  const KEY = 'xh_client_records_v1';
  const RECORD_RE = /^(XH[A-Z0-9]{10}|XH-[A-Z2-9]{24})$/;
  const $ = (selector, root = document) => root.querySelector(selector);
  const $$ = (selector, root = document) => [...root.querySelectorAll(selector)];
  const text = value => String(value ?? '').trim();
  let requestToken = 0;

  function node(tag, className, value) {
    const element = document.createElement(tag);
    if (className) element.className = className;
    if (value !== undefined && value !== null) element.textContent = String(value);
    return element;
  }

  function normalized(value) {
    const out = text(value).toUpperCase();
    return RECORD_RE.test(out) ? out : '';
  }

  function savedRows() {
    try {
      const rows = JSON.parse(localStorage.getItem(KEY) || '[]');
      return Array.isArray(rows) ? rows.filter(row => normalized(row?.businessNo)).slice(0, 30) : [];
    } catch (_) {
      return [];
    }
  }

  function remember(record) {
    const businessNo = normalized(record?.businessNo);
    if (!businessNo) return;
    const rows = savedRows().filter(row => normalized(row.businessNo) !== businessNo);
    rows.unshift({
      businessNo,
      stage: text(record?.stage).toUpperCase() || null,
      createdAt: record?.createdAt || null,
      savedAt: new Date().toISOString()
    });
    try { localStorage.setItem(KEY, JSON.stringify(rows.slice(0, 30))); } catch (_) {}
  }

  function clearRows() {
    try { localStorage.removeItem(KEY); } catch (_) {}
  }

  function statusLabel(stage, status) {
    const key = text(status).toUpperCase();
    const labels = {
      NEW: '已提交', CONTACTED: '客服已联系', QUOTED: '已报价', CONVERTED: '已进入履约', CLOSED: '已关闭', CANCELLED: '已取消',
      WAITING_CONFIRMATION: '等待确认', IN_PROGRESS: '进行中', COMPLETED: '已完成', EXCEPTION: '异常处理中'
    };
    return labels[key] || key || (text(stage).toUpperCase() === 'ORDER' ? '订单记录' : '询价记录');
  }

  function fmt(value) {
    if (!value) return '—';
    const date = new Date(value);
    return Number.isNaN(date.getTime()) ? text(value) : date.toLocaleString('zh-CN', { hour12: false });
  }

  function installStyles() {
    if ($('#frontend-next-n26-style')) return;
    const style = document.createElement('style');
    style.id = 'frontend-next-n26-style';
    style.textContent = `
      .n26-records{min-height:100vh;padding:112px 0 84px;background:radial-gradient(circle at 82% 8%,#47d78a12,transparent 30rem)}
      .n26-head{display:grid;grid-template-columns:minmax(0,1fr) auto;gap:18px;align-items:end;margin-bottom:16px}.n26-head h1{font-size:clamp(42px,6vw,70px);margin:10px 0 8px}.n26-head p{max-width:760px;color:#98a2ac;line-height:1.75;margin:0}.n26-kicker{color:#d8bb61;font-size:10px;font-weight:900;letter-spacing:.16em}.n26-actions{display:flex;gap:8px;flex-wrap:wrap}
      .n26-search{border:1px solid #ffffff18;border-radius:16px;background:#0d1116;padding:17px;margin-bottom:13px}.n26-search form{display:grid;grid-template-columns:minmax(0,1fr) auto;gap:9px}.n26-search input{margin:0;text-transform:uppercase}.n26-msg{min-height:18px;margin-top:8px;color:#7e8892;font-size:9px}.n26-msg[data-kind="error"]{color:#ff9b87}.n26-msg[data-kind="ok"]{color:#6be0a3}
      .n26-summary{display:flex;justify-content:space-between;gap:10px;align-items:center;margin-bottom:10px}.n26-summary h2{font-size:20px;margin:0}.n26-summary span{font-size:9px;color:#79838d}.n26-list{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:10px}.n26-card{display:flex;flex-direction:column;gap:9px;border:1px solid #ffffff15;border-radius:14px;background:#0d1116;padding:15px;transition:.2s}.n26-card:hover{transform:translateY(-3px);border-color:#ffcb2d4d}.n26-card-head{display:flex;justify-content:space-between;gap:8px;align-items:center}.n26-badge{font-size:8px;color:#d8bc68;border:1px solid #ffcb2d35;border-radius:999px;padding:4px 7px}.n26-card strong{font-size:16px;overflow-wrap:anywhere}.n26-card h3{font-size:14px;margin:0}.n26-card p{font-size:9px;color:#7d8791;line-height:1.55;margin:0}.n26-card footer{display:flex;justify-content:space-between;gap:8px;color:#69737d;font-size:8px;margin-top:auto}.n26-card.is-unavailable{border-style:dashed}.n26-card.is-unavailable .n26-badge{color:#999;border-color:#ffffff22}
      .n26-state{min-height:250px;border:1px solid #ffffff15;border-radius:14px;background:#0d1116;display:grid;place-items:center;text-align:center;padding:30px;color:#909aa4}.n26-state h2{color:#f2f4f6;margin:0 0 7px}.n26-state p{margin:0 0 13px;line-height:1.7}.n26-footnote{color:#68727d;font-size:9px;line-height:1.7;margin-top:13px}
      @media(max-width:760px){.n26-head{grid-template-columns:1fr}.n26-list{grid-template-columns:1fr}.n26-search form{grid-template-columns:1fr}.n26-actions{display:grid}.n26-records{padding-top:92px}}
    `;
    document.head.append(style);
  }

  function ensureSection() {
    let section = $('#n26RecordsCenter');
    if (section) return section;
    section = node('section', 'page n26-records');
    section.id = 'n26RecordsCenter';
    section.dataset.page = 'records';
    section.append(node('div', 'shell'));
    const footer = $('.next-footer') || $('.footer');
    if (footer) footer.before(section); else document.body.append(section);
    return section;
  }

  function activate(section) {
    $$('.page').forEach(page => page.classList.toggle('active', page === section));
    $$('.navlink[data-go]').forEach(link => { link.classList.remove('active'); link.removeAttribute('aria-current'); });
    document.documentElement.dataset.frontendPage = 'records';
    document.documentElement.dataset.frontendNextStage = 'n26-records-center';
  }

  function setMetadata() {
    const brand = text(window.XHFrontendShell?.configuration?.brandName) || '星火网游';
    document.title = `业务记录 · ${brand}`;
    let canonical = $('link[rel="canonical"]');
    if (!canonical) { canonical = document.createElement('link'); canonical.rel = 'canonical'; document.head.append(canonical); }
    canonical.href = 'https://wydlgf.cn/records';
    let robots = $('meta[name="robots"]');
    if (!robots) { robots = document.createElement('meta'); robots.name = 'robots'; document.head.append(robots); }
    robots.content = 'noindex,nofollow';
  }

  function message(value, kind = '') {
    const box = $('#n26Message');
    if (!box) return;
    box.textContent = text(value);
    if (kind) box.dataset.kind = kind; else box.removeAttribute('data-kind');
  }

  async function fetchRecord(businessNo) {
    const response = await fetch(`/api/records/${encodeURIComponent(businessNo)}`, {
      method: 'GET', credentials: 'same-origin', cache: 'no-store', headers: { Accept: 'application/json' }
    });
    let body = null;
    try { body = await response.json(); } catch (_) {}
    if (!response.ok) {
      const error = new Error(text(body?.message) || text(body?.error) || (response.status === 404 ? '没有找到对应业务记录。' : `读取失败（${response.status}）`));
      error.status = response.status;
      throw error;
    }
    return body || {};
  }

  function recordCard(record, unavailable = false) {
    const businessNo = normalized(record.businessNo);
    const card = node('a', `n26-card${unavailable ? ' is-unavailable' : ''}`);
    card.href = `/next/record/${encodeURIComponent(businessNo)}`;
    const head = node('div', 'n26-card-head');
    head.append(node('span', 'n26-badge', text(record.stage).toUpperCase() === 'ORDER' ? '履约' : '询价'));
    head.append(node('span', '', unavailable ? '待重新查询' : statusLabel(record.stage, record.status)));
    card.append(head, node('strong', '', businessNo));
    card.append(node('h3', '', text(record.title) || '业务记录'));
    if (text(record.progressText)) card.append(node('p', '', text(record.progressText)));
    const footer = document.createElement('footer');
    footer.append(node('span', '', unavailable ? '当前暂时无法读取，点击重试' : `更新 ${fmt(record.updatedAt)}`));
    if (record.progressPercent !== null && Number.isFinite(Number(record.progressPercent))) footer.append(node('span', '', `${Math.round(Number(record.progressPercent))}%`));
    card.append(footer);
    return card;
  }

  function renderShell(section) {
    const shell = $('.shell', section);
    shell.replaceChildren();
    const head = node('div', 'n26-head');
    const intro = node('div');
    intro.append(node('div', 'n26-kicker', STAGE), node('h1', '', '业务记录'));
    intro.append(node('p', '', '输入业务编号查询公开进度；当前浏览器曾访问或在 Next 提交过的业务编号会与旧版共用同一份本机历史。'));
    const actions = node('div', 'n26-actions');
    const legacy = node('a', 'btn', '对照旧版业务记录'); legacy.href = '/records'; legacy.dataset.n26Legacy = '1';
    const quote = node('a', 'btn gold', '提交新询价'); quote.href = '/next/quote';
    actions.append(legacy, quote); head.append(intro, actions); shell.append(head);

    const search = node('section', 'n26-search');
    const form = document.createElement('form'); form.id = 'n26LookupForm';
    const input = document.createElement('input'); input.id = 'n26BusinessNo'; input.name = 'businessNo'; input.placeholder = '例如 XHXXXXXXXXXX 或 XH-XXXXXXXXXXXXXXXXXXXXXXXX'; input.maxLength = 40; input.autocomplete = 'off';
    const submit = node('button', 'btn gold', '查询记录'); submit.type = 'submit';
    form.append(input, submit); search.append(form); const msg = node('div', 'n26-msg'); msg.id = 'n26Message'; search.append(msg); shell.append(search);

    const summary = node('div', 'n26-summary'); summary.append(node('h2', '', '当前浏览器记录'), node('span', '', '最多保留最近 30 个业务编号'));
    const clear = node('button', 'btn', '清除本机历史'); clear.type = 'button'; clear.id = 'n26Clear'; summary.append(clear); shell.append(summary);
    const listHost = node('div'); listHost.id = 'n26ListHost'; shell.append(listHost);
    shell.append(node('p', 'n26-footnote', '清除这里只会删除当前浏览器保存的编号索引，不会删除服务器上的业务记录。更换设备后仍可使用完整业务编号重新查询。'));

    form.addEventListener('submit', async event => {
      event.preventDefault();
      const businessNo = normalized(input.value);
      if (!businessNo) { message('业务编号格式不正确，请检查后重试。', 'error'); return; }
      submit.disabled = true; submit.textContent = '查询中…'; message('正在读取业务记录…');
      try {
        const record = await fetchRecord(businessNo);
        remember(record);
        message('已找到业务记录，正在打开详情。', 'ok');
        location.href = `/next/record/${encodeURIComponent(businessNo)}`;
      } catch (error) {
        message(error.message || '查询失败，请稍后重试。', 'error');
      } finally {
        submit.disabled = false; submit.textContent = '查询记录';
      }
    });
    clear.addEventListener('click', () => {
      if (!savedRows().length) return;
      if (!confirm('只清除当前浏览器保存的业务编号历史，不会删除服务器业务记录。确定继续？')) return;
      clearRows(); message('本机业务编号历史已清除。', 'ok'); loadSaved();
    });
  }

  async function loadSaved() {
    const host = $('#n26ListHost');
    if (!host) return;
    const token = ++requestToken;
    const saved = savedRows();
    if (!saved.length) {
      const empty = node('div', 'n26-state'); const box = node('div');
      box.append(node('h2', '', '当前浏览器还没有记录'), node('p', '', '提交询价或输入业务编号查询后，会把编号安全保存在当前浏览器。'));
      const quote = node('a', 'btn gold', '去提交询价'); quote.href = '/next/quote'; box.append(quote); empty.append(box); host.replaceChildren(empty); return;
    }
    const loading = node('div', 'n26-state'); loading.append(node('div', '', `正在读取 ${saved.length} 条业务记录…`)); host.replaceChildren(loading);
    const rows = await Promise.all(saved.map(async savedRow => {
      const businessNo = normalized(savedRow.businessNo);
      try {
        const record = await fetchRecord(businessNo);
        return { record, unavailable: false };
      } catch (_) {
        return { record: { businessNo, stage: savedRow.stage || 'QUOTE', createdAt: savedRow.createdAt, updatedAt: savedRow.savedAt, title: '历史业务记录' }, unavailable: true };
      }
    }));
    if (token !== requestToken) return;
    const grid = node('div', 'n26-list');
    rows.forEach(row => grid.append(recordCard(row.record, row.unavailable)));
    host.replaceChildren(grid);
  }

  async function load() {
    const section = ensureSection(); activate(section); installStyles(); setMetadata(); renderShell(section); await loadSaved();
  }

  window.XHFrontendNextN26 = { reload: load, remember, savedRows, normalized };
  load();
})();
