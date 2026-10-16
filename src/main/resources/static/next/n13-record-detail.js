(() => {
  'use strict';

  const ROUTE_RE = /^\/next\/record\/([^/?#]+)\/?$/;
  const BUSINESS_NO_RE = /^(XH[A-Z0-9]{10}|XH-[A-Z2-9]{24})$/;
  const STATUS_RE = /^[A-Z][A-Z0-9_]{0,31}$/;
  const SORTS = new Set(['updated-desc', 'updated-asc']);
  const $ = (selector, root = document) => root.querySelector(selector);
  const $$ = (selector, root = document) => [...root.querySelectorAll(selector)];
  const text = value => String(value ?? '').trim();
  let routeRequest = 0;

  const statusLabels = {
    NEW: '询价已提交', CONTACTED: '客服已联系', QUOTED: '已完成报价', CONVERTED: '已转为订单', CLOSED: '询价已关闭', CANCELLED: '询价已取消',
    WAITING_CONFIRMATION: '待确认', IN_PROGRESS: '进行中', COMPLETED: '已完成', EXCEPTION: '异常处理中'
  };

  function make(tag, className, value) {
    const node = document.createElement(tag);
    if (className) node.className = className;
    if (value !== undefined && value !== null) node.textContent = String(value);
    return node;
  }

  function routeNo() {
    const match = location.pathname.match(ROUTE_RE);
    if (!match) return null;
    let value = match[1];
    try { value = decodeURIComponent(value); } catch (_) {}
    value = text(value).toUpperCase();
    return BUSINESS_NO_RE.test(value) ? value : '';
  }

  function appendAccountViewState(target, source = location.href) {
    const url = target instanceof URL ? target : new URL(target, location.origin);
    const params = new URL(source, location.origin).searchParams;
    const q = text(params.get('q')).slice(0, 60);
    const status = text(params.get('status')).toUpperCase();
    const sort = text(params.get('sort')).toLowerCase();
    if (q) url.searchParams.set('q', q);
    if (STATUS_RE.test(status)) url.searchParams.set('status', status);
    if (SORTS.has(sort)) url.searchParams.set('sort', sort);
    return url;
  }

  function accountReturnTarget(businessNo) {
    const normalized = text(businessNo).toUpperCase();
    if (!BUSINESS_NO_RE.test(normalized)) return '';
    const from = text(new URLSearchParams(location.search).get('from')).toLowerCase();
    const section = from === 'account-requests' ? 'requests' : from === 'account-orders' ? 'orders' : '';
    if (!section) return '';
    const target = appendAccountViewState(new URL(`/next/account/${section}/${encodeURIComponent(normalized)}`, location.origin));
    return target.pathname + target.search;
  }

  function installStyles() {
    if ($('#frontend-next-n13-style')) return;
    const style = document.createElement('style');
    style.id = 'frontend-next-n13-style';
    style.textContent = `
      .n13-record{min-height:100vh;padding:112px 0 84px;background:radial-gradient(circle at 85% 14%,#47d78a12,transparent 26rem)}
      .n13-grid{display:grid;grid-template-columns:minmax(0,1fr) 330px;gap:16px;align-items:start}.n13-card{border:1px solid #ffffff18;border-radius:18px;background:#0d1116;padding:22px;box-shadow:0 26px 70px #0006}.n13-kicker{color:#d9bb61;font-size:10px;font-weight:900;letter-spacing:.15em;text-transform:uppercase}.n13-title{font-size:clamp(30px,4.5vw,52px);line-height:1.05;margin:12px 0}.n13-muted{color:#9098a2;line-height:1.7}.n13-status{display:inline-flex;align-items:center;gap:7px;border:1px solid #ffcb2d48;border-radius:999px;padding:7px 10px;color:#f0cf70;font-size:11px;margin:6px 0 18px}.n13-status:before{content:'';width:7px;height:7px;border-radius:50%;background:#ffcb2d}.n13-facts{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:9px}.n13-fact{padding:13px;border:1px solid #ffffff12;border-radius:12px;background:#ffffff04}.n13-fact small{display:block;color:#7f8790;font-size:9px;margin-bottom:5px}.n13-fact strong{font-size:12px;overflow-wrap:anywhere}.n13-progress{margin-top:18px;padding:15px;border:1px solid #ffffff12;border-radius:14px;background:#090d11}.n13-progress-head{display:flex;justify-content:space-between;gap:14px}.n13-progress-bar{height:7px;margin-top:11px;background:#252b31;border-radius:999px;overflow:hidden}.n13-progress-bar i{display:block;height:100%;background:linear-gradient(90deg,#eaa600,#ffcb2d);border-radius:inherit}.n13-note{margin-top:13px;padding:13px;border-left:2px solid #ffcb2d;color:#abb2ba;background:#ffcb2d08;white-space:pre-wrap;overflow-wrap:anywhere}.n13-image{display:block;margin-top:14px;border:1px solid #ffffff14;border-radius:13px;overflow:hidden}.n13-image img{display:block;width:100%;max-height:440px;object-fit:contain;background:#07090c}.n13-actions{display:grid;gap:8px}.n13-actions .btn{text-align:center}.n13-safe{margin-top:14px;color:#848c95;font-size:10px;line-height:1.7}.n13-loading{min-height:300px;display:grid;place-items:center;text-align:center;color:#9aa2ac}.n13-error h1{font-size:34px;margin:10px 0}.n13-error p{color:#9aa2ac;line-height:1.7}
      @media(max-width:850px){.n13-grid{grid-template-columns:1fr}.n13-record{padding-top:92px}.n13-facts{grid-template-columns:1fr 1fr}}@media(max-width:520px){.n13-card{padding:16px}.n13-facts{grid-template-columns:1fr}}
    `;
    document.head.append(style);
  }

  function ensureSection() {
    let section = $('#n13RecordDetail');
    if (section) return section;
    section = make('section', 'page n13-record');
    section.id = 'n13RecordDetail';
    section.dataset.page = 'record-detail';
    const shell = make('div', 'shell');
    section.append(shell);
    const footer = $('.footer, .next-footer');
    if (footer) footer.before(section); else document.body.append(section);
    return section;
  }

  function activate(section) {
    $$('.page').forEach(page => page.classList.toggle('active', page === section));
    $$('.navlink[data-go]').forEach(link => { link.classList.remove('active'); link.removeAttribute('aria-current'); });
    document.documentElement.dataset.frontendPage = 'record-detail';
    document.documentElement.dataset.frontendNextStage = 'n13-record-detail';
  }

  function setMetadata(record) {
    const brand = text(window.XHFrontendShell?.configuration?.brandName) || '星火网游';
    document.title = `业务记录 ${text(record?.businessNo)} · ${brand}`;
    let robots = $('meta[name="robots"]');
    if (!robots) { robots = document.createElement('meta'); robots.name = 'robots'; document.head.append(robots); }
    robots.content = 'noindex,nofollow';
    let canonical = $('link[rel="canonical"]');
    if (!canonical) { canonical = document.createElement('link'); canonical.rel = 'canonical'; document.head.append(canonical); }
    canonical.href = `https://wydlgf.cn/record/${encodeURIComponent(text(record?.businessNo))}`;
  }

  function formatTime(value) {
    if (!value) return '—';
    const date = new Date(value);
    return Number.isNaN(date.getTime()) ? text(value) : date.toLocaleString('zh-CN', { hour12: false });
  }

  function fact(label, value) {
    const item = make('div', 'n13-fact');
    item.append(make('small', '', label), make('strong', '', value || '—'));
    return item;
  }

  function renderLoading(section, businessNo) {
    const shell = $('.shell', section);
    const card = make('div', 'n13-card n13-loading');
    card.append(make('div', '', businessNo ? `正在读取业务记录 ${businessNo}…` : '正在读取业务记录…'));
    shell?.replaceChildren(card);
  }

  function appendAccountReturn(actions, businessNo) {
    const target = accountReturnTarget(businessNo);
    if (!target) return;
    const from = new URLSearchParams(location.search).get('from');
    const label = from === 'account-requests' ? '返回询价详情' : '返回订单详情';
    const back = make('a', 'btn gold', label);
    back.href = target;
    back.dataset.n19AccountReturn = '1';
    actions.append(back);
  }

  function renderRecord(section, record) {
    const shell = $('.shell', section);
    if (!shell) return;
    setMetadata(record);
    const businessNo = text(record.businessNo).toUpperCase();
    const stage = text(record.stage).toUpperCase();
    const status = text(record.status).toUpperCase();
    const percent = Number(record.progressPercent);
    const hasPercent = Number.isFinite(percent) && percent >= 0 && percent <= 100;

    const grid = make('div', 'n13-grid');
    const main = make('article', 'n13-card');
    main.append(make('div', 'n13-kicker', stage === 'ORDER' ? 'Order Progress' : 'Quote Progress'));
    main.append(make('h1', 'n13-title', text(record.title) || (stage === 'ORDER' ? '服务订单' : '询价记录')));
    main.append(make('div', 'n13-status', statusLabels[status] || status || '状态待确认'));

    const facts = make('div', 'n13-facts');
    facts.append(
      fact('业务编号', businessNo),
      fact('当前阶段', stage === 'ORDER' ? '订单履约' : '询价确认'),
      fact('预计时间', text(record.expectedTime) || '以客服确认为准'),
      fact('最后更新', formatTime(record.updatedAt)),
      fact('创建时间', formatTime(record.createdAt))
    );
    main.append(facts);

    const progress = make('section', 'n13-progress');
    const progressHead = make('div', 'n13-progress-head');
    progressHead.append(make('strong', '', '当前进度'), make('span', '', hasPercent ? `${Math.round(percent)}%` : '按业务状态更新'));
    progress.append(progressHead, make('p', 'n13-muted', text(record.progressText) || '暂无更多公开进度说明。'));
    if (hasPercent) {
      const bar = make('div', 'n13-progress-bar');
      const fill = make('i'); fill.style.width = `${Math.max(0, Math.min(100, percent))}%`; bar.append(fill); progress.append(bar);
    }
    main.append(progress);
    if (text(record.customerNote)) main.append(make('div', 'n13-note', text(record.customerNote)));
    if (record.hasProgressImage === true) {
      const link = make('a', 'n13-image');
      link.href = `/api/records/${encodeURIComponent(businessNo)}/image`;
      link.target = '_blank'; link.rel = 'noopener';
      const image = document.createElement('img');
      image.src = link.href; image.alt = `${businessNo} 进度截图`; image.loading = 'lazy';
      link.append(image); main.append(link);
    }

    const aside = make('aside', 'n13-card');
    aside.append(make('div', 'n13-kicker', 'Record Actions'), make('h3', '', '下一步'));
    aside.append(make('p', 'n13-muted', stage === 'ORDER' ? '继续关注公开进度；需要补充需求或处理异常时请通过客服或个人中心操作。' : '客服确认需求和报价后，记录会继续更新；转为订单后仍沿用同一业务编号。'));
    const actions = make('div', 'n13-actions');
    appendAccountReturn(actions, businessNo);
    const quote = make('a', 'btn gold', '继续提交询价'); quote.href = '/next/quote';
    const account = make('a', 'btn', '个人中心'); account.href = '/next/account';
    const legacy = make('a', 'btn', '查看旧版记录页'); legacy.href = `/record/${encodeURIComponent(businessNo)}`; legacy.dataset.n13Legacy = '1';
    actions.append(quote, account, legacy); aside.append(actions);
    aside.append(make('p', 'n13-safe', 'Frontend Next N13 仅展示现有公开业务记录，不提供订单、支付、退款或售后写操作。旧版记录页继续保留。'));

    grid.append(main, aside); shell.replaceChildren(grid);
  }

  function renderError(section, message) {
    const shell = $('.shell', section);
    if (!shell) return;
    const card = make('div', 'n13-card n13-error');
    card.append(make('div', 'n13-kicker', 'Business Record'), make('h1', '', '暂时无法显示此业务记录'), make('p', '', message));
    const actions = make('div', 'n13-actions');
    appendAccountReturn(actions, routeNo());
    const retry = make('button', 'btn gold', '重新读取'); retry.type = 'button'; retry.dataset.n13Retry = '1';
    const quote = make('a', 'btn', '返回新版询价'); quote.href = '/next/quote';
    actions.append(retry, quote); card.append(actions); shell.replaceChildren(card);
  }

  async function getRecord(businessNo) {
    const response = await fetch(`/api/records/${encodeURIComponent(businessNo)}`, { method: 'GET', headers: { Accept: 'application/json' }, credentials: 'same-origin', cache: 'no-store' });
    let body = null;
    try { body = await response.json(); } catch (_) {}
    if (!response.ok) {
      const server = text(body?.message) || text(body?.error);
      if (response.status === 404) throw new Error('没有找到对应业务记录，请检查业务编号是否完整。');
      throw new Error(server || `业务记录读取失败（${response.status}），请稍后重试。`);
    }
    return body;
  }

  async function syncRoute() {
    const request = ++routeRequest;
    const businessNo = routeNo();
    if (businessNo === null) return;
    installStyles();
    const section = ensureSection(); activate(section);
    if (!businessNo) { renderError(section, '业务编号格式无效，请从提交成功页面或完整业务链接进入。'); return; }
    renderLoading(section, businessNo);
    try {
      const record = await getRecord(businessNo);
      if (request !== routeRequest || routeNo() !== businessNo) return;
      renderRecord(section, record || {});
    } catch (error) {
      if (request !== routeRequest || routeNo() !== businessNo) return;
      renderError(section, text(error?.message) || '业务记录读取失败，请稍后重试。');
    }
  }

  function boot() {
    document.addEventListener('click', event => {
      const target = event.target instanceof Element ? event.target : null;
      if (target?.closest('[data-n13-retry]')) syncRoute();
    });
    syncRoute();
    window.addEventListener('popstate', syncRoute);
  }

  window.XHFrontendNextN13 = { routeNo, syncRoute, accountReturnTarget, appendAccountViewState };
  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', boot, { once: true }); else boot();
})();
