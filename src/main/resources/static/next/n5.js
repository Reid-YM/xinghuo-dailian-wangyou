(() => {
  'use strict';

  const $ = (selector, root = document) => root.querySelector(selector);
  const text = (value) => String(value ?? '').trim();
  const page = $('.page[data-page="account"]');
  if (!page) return;

  const state = {
    summary: null,
    quotes: null,
    orders: null,
    candidates: [],
    active: 'orders',
    detail: null,
    loading: false,
    generation: 0
  };

  let observer = null;
  let mountTimer = null;

  function node(tag, className, value) {
    const element = document.createElement(tag);
    if (className) element.className = className;
    if (value !== undefined && value !== null) element.textContent = String(value);
    return element;
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

  function fmtDate(value) {
    if (!value) return '—';
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return '—';
    return new Intl.DateTimeFormat('zh-CN', {
      year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit'
    }).format(date);
  }

  function fmtPercent(value) {
    const number = Number(value);
    return Number.isFinite(number) ? `${Math.max(0, Math.min(100, Math.round(number)))}%` : '—';
  }

  function safeKey(value) {
    return encodeURIComponent(text(value));
  }

  function statusTone(status) {
    const value = text(status).toUpperCase();
    if (['COMPLETED', 'DONE', 'FINISHED', 'CLOSED', 'CONVERTED', 'RESOLVED'].includes(value)) return 'n5-ok';
    if (['EXCEPTION', 'FAILED', 'CANCELLED', 'CANCELED'].includes(value)) return 'n5-warn';
    return 'n5-run';
  }

  async function request(url, options = {}) {
    const headers = Object.assign({ Accept: 'application/json' }, options.headers || {});
    const response = await fetch(url, Object.assign({
      credentials: 'same-origin',
      cache: 'no-store'
    }, options, { headers }));
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

  function profile() {
    return window.XHFrontendNextN4?.state?.profile || null;
  }

  function authenticated() {
    return !!profile() && !!$('.n4-shell', page);
  }

  function installStyles() {
    if ($('#frontend-next-n5-style')) return;
    const style = document.createElement('style');
    style.id = 'frontend-next-n5-style';
    style.textContent = `
      .n5-section{padding:0 0 92px}.n5-head{display:flex;justify-content:space-between;gap:18px;align-items:end;flex-wrap:wrap;margin:0 0 16px}.n5-head h2{margin:4px 0 0;font-size:26px}.n5-head p{margin:0;color:#818994;font-size:11px;line-height:1.65}.n5-live{display:inline-flex;align-items:center;gap:7px;border:1px solid #45df9133;color:#83eeb0;border-radius:99px;padding:6px 9px;font-size:9px}.n5-live:before{content:"";width:6px;height:6px;border-radius:50%;background:#45df91;box-shadow:0 0 10px #45df91}
      .n5-summary{display:grid;grid-template-columns:repeat(4,minmax(0,1fr));gap:10px;margin:0 0 14px}.n5-summary-card{border:1px solid var(--line);border-radius:14px;background:#0c1015;padding:15px}.n5-summary-card small{display:block;color:#777f89;margin-bottom:6px}.n5-summary-card strong{font-size:26px}.n5-summary-card span{display:block;margin-top:5px;color:#66707a;font-size:9px;line-height:1.45}
      .n5-layout{display:grid;grid-template-columns:minmax(0,1fr) 310px;gap:14px;align-items:start}.n5-panel{border:1px solid var(--line);background:#0c1015;border-radius:16px;padding:18px;min-width:0}.n5-tabs{display:flex;gap:8px;flex-wrap:wrap;margin-bottom:14px}.n5-tab{appearance:none;border:1px solid var(--line);background:#0a0e13;color:#858d97;border-radius:9px;padding:9px 12px;font:inherit;font-size:10px;cursor:pointer}.n5-tab[aria-selected="true"]{border-color:#ffcb2d77;color:#ffd863;background:#17140b}.n5-tab:focus-visible{outline:2px solid #ffcb2d88;outline-offset:2px}.n5-table-wrap{overflow:auto}.n5-table{width:100%;border-collapse:collapse;min-width:780px}.n5-table th,.n5-table td{padding:11px 8px;border-bottom:1px solid #ffffff10;text-align:left;font-size:10px;vertical-align:middle}.n5-table th{color:#737c86;font-weight:600}.n5-record{display:inline-flex;align-items:center;gap:6px}.n5-record:before{content:"";width:6px;height:6px;border-radius:50%;background:currentColor}.n5-run{color:var(--gold)}.n5-ok{color:#65dfa0}.n5-warn{color:#ff9e83}.n5-muted{color:#7e8790}.n5-mini-btn{appearance:none;border:1px solid #ffffff1a;background:#11171d;color:#d7dce1;border-radius:8px;padding:7px 9px;font:inherit;font-size:9px;cursor:pointer}.n5-mini-btn:hover{border-color:#ffcb2d66;color:#fff}.n5-mini-btn:disabled{opacity:.45;cursor:not-allowed}.n5-empty{padding:28px 16px;border:1px dashed #ffffff18;border-radius:12px;color:#78818b;text-align:center;font-size:10px;line-height:1.7}.n5-message{min-height:18px;margin-top:9px;color:#8d96a0;font-size:10px;line-height:1.55}.n5-message.ok{color:#76e3a3}.n5-message.error{color:#ff9b87}
      .n5-claim{display:grid;gap:12px}.n5-claim h3{margin:0}.n5-claim-list{display:grid;gap:8px}.n5-candidate{border:1px solid #ffffff12;border-radius:11px;padding:11px;background:#0a0e12}.n5-candidate b{display:block;font-size:10px;margin-bottom:5px}.n5-candidate span{display:block;color:#747d87;font-size:9px;line-height:1.45}.n5-claim-field{display:grid;gap:6px}.n5-claim-field label{font-size:10px;color:#9ca4ad}.n5-claim-field input{width:100%;border:1px solid var(--line);background:#090d11;color:#fff;border-radius:10px;padding:11px}.n5-claim-field input:focus{outline:none;border-color:#ffcb2d88;box-shadow:0 0 0 3px #ffcb2d12}.n5-claim-tip{font-size:9px;line-height:1.6;color:#727b85;margin:0}.n5-claim-tip strong{color:#d9dee4}.n5-claim .btn{width:100%}
      .n5-detail{margin-top:14px;border-top:1px solid #ffffff12;padding-top:16px}.n5-detail[hidden]{display:none}.n5-detail-head{display:flex;justify-content:space-between;gap:14px;align-items:start;margin-bottom:14px}.n5-detail-head h3{margin:0}.n5-detail-grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:9px}.n5-detail-card{border:1px solid #ffffff10;border-radius:10px;padding:11px;background:#090d11}.n5-detail-card small{display:block;color:#717a84;margin-bottom:5px;font-size:9px}.n5-detail-card b{font-size:10px;word-break:break-word}.n5-subhead{margin:16px 0 8px;font-size:11px}.n5-timeline{display:grid;gap:8px}.n5-timeline-item{position:relative;border-left:1px solid #3b434c;padding:0 0 11px 14px}.n5-timeline-item:before{content:"";position:absolute;left:-4px;top:3px;width:7px;height:7px;border-radius:50%;background:var(--gold)}.n5-timeline-item b{display:block;font-size:10px}.n5-timeline-item span{display:block;color:#77808a;font-size:9px;line-height:1.5;margin-top:4px}.n5-after{display:grid;gap:8px}.n5-after-item{border:1px solid #ffffff10;border-radius:10px;padding:11px}.n5-after-item b{font-size:10px}.n5-after-item p{margin:6px 0 0;color:#858e98;font-size:9px;line-height:1.55;white-space:pre-wrap}.n5-after-item small{display:block;color:#68717b;margin-top:6px}.n5-source{margin-top:14px;padding:11px;border:1px solid #ffcb2d24;border-radius:10px;background:#17140b}.n5-source b{display:block;font-size:10px;margin-bottom:5px}.n5-source span{color:#9a916d;font-size:9px;line-height:1.5}
      @media(max-width:960px){.n5-layout{grid-template-columns:1fr}.n5-summary{grid-template-columns:1fr 1fr}.n5-claim-list{grid-template-columns:repeat(2,minmax(0,1fr))}}
      @media(max-width:560px){.n5-summary{grid-template-columns:1fr 1fr}.n5-detail-grid{grid-template-columns:1fr}.n5-claim-list{grid-template-columns:1fr}.n5-panel{padding:14px}}
    `;
    document.head.append(style);
  }

  function summaryCard(label, value, note) {
    const card = node('div', 'n5-summary-card');
    append(card, node('small', '', label), node('strong', '', value), node('span', '', note));
    return card;
  }

  function setMessage(message, kind = '') {
    const host = $('#n5Message');
    if (!host) return;
    host.className = `n5-message ${kind}`.trim();
    host.textContent = message || '';
  }

  function renderSummary(host) {
    host.replaceChildren();
    const summary = state.summary;
    const verified = !!profile()?.contactVerified;
    append(host,
      summaryCard('我的询价', summary ? String(summary.quoteTotal ?? 0) : '—', '当前账号全部询价记录'),
      summaryCard('我的订单', summary ? String(summary.orderTotal ?? 0) : '—', '当前账号全部已归属订单'),
      summaryCard('进行中订单', summary ? String(summary.activeOrders ?? 0) : '—', '服务端按全部订单状态统计'),
      summaryCard('历史订单认领', verified ? '可用' : '关闭', verified ? '联系方式已验证，可按完整单号认领' : '需先完成联系方式核验')
    );
  }

  function renderProgress(cell, order) {
    const value = Number(order?.progressPercent);
    if (!Number.isFinite(value)) {
      cell.textContent = text(order?.progressText) || '—';
      return;
    }
    const wrap = node('div');
    const label = `${fmtPercent(value)}${text(order?.progressText) ? ` · ${text(order.progressText)}` : ''}`;
    append(wrap, node('span', '', label));
    cell.append(wrap);
  }

  function renderOrders(body) {
    const items = Array.isArray(state.orders?.items) ? state.orders.items : [];
    if (!items.length) {
      body.append(node('div', 'n5-empty', '当前账号还没有订单记录。历史订单可在右侧“历史订单认领”中按完整单号尝试归档。'));
      return;
    }
    const wrap = node('div', 'n5-table-wrap');
    const table = node('table', 'n5-table');
    const head = document.createElement('thead');
    const hr = document.createElement('tr');
    ['业务编号', '游戏 / 服务', '状态', '进度', '更新时间', '操作'].forEach((label) => hr.append(node('th', '', label)));
    head.append(hr); table.append(head);
    const tbody = document.createElement('tbody');
    items.forEach((order) => {
      const row = document.createElement('tr');
      row.append(node('td', '', text(order.businessNo) || text(order.orderNo) || '—'));
      row.append(node('td', '', [text(order.gameName), text(order.serviceName)].filter(Boolean).join(' · ') || '—'));
      const status = node('td'); status.append(node('span', `n5-record ${statusTone(order.status)}`, text(order.statusText) || text(order.status) || '状态更新中')); row.append(status);
      const progress = node('td'); renderProgress(progress, order); row.append(progress);
      row.append(node('td', '', fmtDate(order.updatedAt || order.createdAt)));
      const action = node('td');
      const open = button('查看详情', 'n5-mini-btn');
      open.addEventListener('click', () => loadOrderDetail(order.businessNo || order.orderNo));
      action.append(open); row.append(action); tbody.append(row);
    });
    table.append(tbody); wrap.append(table); body.append(wrap);
  }

  function renderQuotes(body) {
    const items = Array.isArray(state.quotes?.items) ? state.quotes.items : [];
    if (!items.length) {
      body.append(node('div', 'n5-empty', '当前账号还没有询价记录。你从 Frontend Next 提交的新询价，登录状态下会自动归入这里。'));
      return;
    }
    const wrap = node('div', 'n5-table-wrap');
    const table = node('table', 'n5-table');
    const head = document.createElement('thead');
    const hr = document.createElement('tr');
    ['业务编号', '游戏', '需求', '状态', '当前进度', '更新时间', '操作'].forEach((label) => hr.append(node('th', '', label)));
    head.append(hr); table.append(head);
    const tbody = document.createElement('tbody');
    items.forEach((quote) => {
      const row = document.createElement('tr');
      row.append(node('td', '', text(quote.businessNo) || '—'));
      row.append(node('td', '', text(quote.game) || '—'));
      row.append(node('td', '', text(quote.demand) || '—'));
      const status = node('td'); status.append(node('span', `n5-record ${statusTone(quote.status)}`, text(quote.statusText) || text(quote.status) || '状态更新中')); row.append(status);
      row.append(node('td', '', text(quote.currentProgress) || '—'));
      row.append(node('td', '', fmtDate(quote.updatedAt || quote.createdAt)));
      const action = node('td');
      const open = button('查看询价', 'n5-mini-btn'); open.addEventListener('click', () => loadQuoteDetail(quote.businessNo)); action.append(open); row.append(action); tbody.append(row);
    });
    table.append(tbody); wrap.append(table); body.append(wrap);
  }

  function renderRecords() {
    const body = $('#n5RecordsBody');
    if (!body) return;
    body.replaceChildren();
    if (state.active === 'quotes') renderQuotes(body); else renderOrders(body);
    document.querySelectorAll('.n5-tab').forEach((tab) => {
      tab.setAttribute('aria-selected', String(tab.dataset.n5Tab === state.active));
    });
  }

  function detailField(label, value) {
    const card = node('div', 'n5-detail-card');
    append(card, node('small', '', label), node('b', '', text(value) || '—'));
    return card;
  }

  function renderQuoteDetail(detail) {
    const host = $('#n5Detail');
    if (!host) return;
    host.hidden = false; host.replaceChildren();
    const head = node('div', 'n5-detail-head');
    append(head, append(node('div'), node('div', 'kicker', '询价详情'), node('h3', '', text(detail.businessNo) || '询价详情')));
    const close = button('关闭', 'n5-mini-btn'); close.addEventListener('click', () => { host.hidden = true; host.replaceChildren(); state.detail = null; }); head.append(close); host.append(head);
    const grid = node('div', 'n5-detail-grid');
    append(grid,
      detailField('游戏', detail.game), detailField('需求', detail.demand), detailField('平台', detail.platform),
      detailField('期望时间', detail.expectedTime), detailField('状态', detail.statusText || detail.status), detailField('更新时间', fmtDate(detail.updatedAt || detail.createdAt))
    );
    host.append(grid);
    if (text(detail.currentProgress)) {
      const progress = node('div', 'n5-source'); append(progress, node('b', '', '当前进度'), node('span', '', detail.currentProgress)); host.append(progress);
    }
    if (text(detail.customerSupplement)) {
      const supplement = node('div', 'n5-source'); append(supplement, node('b', '', '我的补充说明'), node('span', '', detail.customerSupplement)); host.append(supplement);
    }
  }

  function renderOrderDetail(detail) {
    const host = $('#n5Detail');
    if (!host) return;
    host.hidden = false; host.replaceChildren();
    const order = detail?.order || {};
    const head = node('div', 'n5-detail-head');
    append(head, append(node('div'), node('div', 'kicker', '订单详情'), node('h3', '', text(order.businessNo) || text(order.orderNo) || '订单详情')));
    const close = button('关闭', 'n5-mini-btn'); close.addEventListener('click', () => { host.hidden = true; host.replaceChildren(); state.detail = null; }); head.append(close); host.append(head);

    const grid = node('div', 'n5-detail-grid');
    append(grid,
      detailField('游戏', order.gameName), detailField('服务', order.serviceName), detailField('状态', order.statusText || order.status),
      detailField('真实进度', Number.isFinite(Number(order.progressPercent)) ? `${fmtPercent(order.progressPercent)}${text(order.progressText) ? ` · ${text(order.progressText)}` : ''}` : text(order.progressText)),
      detailField('创建时间', fmtDate(order.createdAt)), detailField('更新时间', fmtDate(order.updatedAt))
    );
    host.append(grid);

    if (detail?.sourceQuote) {
      const source = node('div', 'n5-source');
      append(source, node('b', '', `来源询价 · ${text(detail.sourceQuote.businessNo) || '—'}`), node('span', '', `${text(detail.sourceQuote.game) || '—'} · ${text(detail.sourceQuote.demand) || '—'} · ${text(detail.sourceQuote.statusText) || text(detail.sourceQuote.status) || '—'}`));
      host.append(source);
    }

    const timeline = Array.isArray(detail?.timeline) ? detail.timeline : [];
    host.append(node('h4', 'n5-subhead', '服务时间线'));
    const timelineHost = node('div', 'n5-timeline');
    if (!timeline.length) timelineHost.append(node('div', 'n5-empty', '暂无服务时间线。'));
    timeline.forEach((item) => {
      const row = node('div', 'n5-timeline-item');
      append(row, node('b', '', text(item.title) || '进度更新'), node('span', '', `${text(item.content) || '—'} · ${fmtDate(item.createdAt)}`)); timelineHost.append(row);
    });
    host.append(timelineHost);

    const cases = Array.isArray(detail?.afterSalesCases) ? detail.afterSalesCases : [];
    const records = Array.isArray(detail?.afterSalesRecords) ? detail.afterSalesRecords : [];
    host.append(node('h4', 'n5-subhead', '售后 / 问题记录'));
    const after = node('div', 'n5-after');
    if (!cases.length && !records.length) after.append(node('div', 'n5-empty', '当前订单没有售后问题记录。'));
    if (cases.length) {
      cases.forEach((item) => {
        const row = node('div', 'n5-after-item');
        append(row,
          node('b', statusTone(item.status), `${text(item.category) || '售后问题'} · ${text(item.status) || 'OPEN'}${item.pendingCustomerFollowup ? ' · 待客服跟进补充' : ''}`),
          node('p', '', text(item.content) || '—'),
          node('small', '', `提交于 ${fmtDate(item.submittedAt)}${text(item.customerReply) ? ` · 客服：${text(item.customerReply)}` : ''}`)
        );
        after.append(row);
      });
    } else {
      records.forEach((item) => {
        const row = node('div', 'n5-after-item');
        append(row, node('b', '', text(item.category) || '售后问题'), node('p', '', text(item.content) || '—'), node('small', '', fmtDate(item.submittedAt))); after.append(row);
      });
    }
    host.append(after);
  }

  async function loadQuoteDetail(businessNo) {
    if (!text(businessNo)) return;
    setMessage('正在读取询价详情…');
    try {
      state.detail = await request(`/api/console/quotes/${safeKey(businessNo)}`);
      renderQuoteDetail(state.detail);
      setMessage('询价详情已读取。', 'ok');
    } catch (error) {
      setMessage(text(error.message) || '询价详情读取失败。', 'error');
    }
  }

  async function loadOrderDetail(businessNo) {
    if (!text(businessNo)) return;
    setMessage('正在读取订单详情和进度…');
    try {
      state.detail = await request(`/api/console/orders/${safeKey(businessNo)}`);
      renderOrderDetail(state.detail);
      setMessage('订单详情、时间线和售后状态已读取。', 'ok');
    } catch (error) {
      setMessage(text(error.message) || '订单详情读取失败。', 'error');
    }
  }

  function renderCandidates() {
    const host = $('#n5Candidates');
    const claimInput = $('#n5ClaimNo');
    const claimButton = $('#n5ClaimButton');
    if (!host || !claimInput || !claimButton) return;
    host.replaceChildren();
    const verified = !!profile()?.contactVerified;
    claimInput.disabled = !verified;
    claimButton.disabled = !verified;
    if (!verified) {
      host.append(node('div', 'n5-empty', '当前登录联系方式尚未完成所有权验证。为保护历史订单隐私，候选提示和认领功能保持关闭。'));
      return;
    }
    if (!state.candidates.length) {
      host.append(node('div', 'n5-empty', '没有发现与当前已验证联系方式匹配的未归属历史订单。你仍可输入自己保存的完整订单号 / 业务编号尝试认领。'));
      return;
    }
    state.candidates.slice(0, 6).forEach((item) => {
      const card = node('div', 'n5-candidate');
      append(card,
        node('b', '', text(item.businessNo) || '业务编号已脱敏'),
        node('span', '', `${[text(item.gameName), text(item.serviceName)].filter(Boolean).join(' · ') || '服务信息待核对'} · ${fmtDate(item.createdAt)}`)
      );
      host.append(card);
    });
  }

  async function claimOrder() {
    const input = $('#n5ClaimNo');
    const submit = $('#n5ClaimButton');
    const businessNo = text(input?.value);
    if (!businessNo) {
      setMessage('请输入你自己保存的完整订单号或业务编号。', 'error');
      input?.focus();
      return;
    }
    if (!profile()?.contactVerified) {
      setMessage('当前登录联系方式尚未完成核验，暂不能认领历史订单。', 'error');
      return;
    }
    if (!window.confirm('确认认领这笔历史订单到当前登录账号？系统会再次校验完整单号和已验证联系方式。')) return;
    submit.disabled = true;
    submit.textContent = '正在核对并认领…';
    setMessage('服务器正在重新校验订单归属和联系方式…');
    try {
      const claimed = await request('/api/account/orders/claim', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', 'X-Requested-With': 'fetch' },
        body: JSON.stringify({ businessNo })
      });
      input.value = '';
      setMessage(`认领成功：${text(claimed?.businessNo) || '订单已归入当前账号'}。正在刷新个人中心…`, 'ok');
      if (window.XHFrontendNextN4?.reload) {
        await window.XHFrontendNextN4.reload();
      } else {
        await load();
      }
    } catch (error) {
      setMessage(text(error.message) || '无法认领该订单，请核对完整单号或联系客服。', 'error');
    } finally {
      if (submit.isConnected) {
        submit.disabled = !profile()?.contactVerified;
        submit.textContent = '确认认领到当前账号';
      }
    }
  }

  function buildShell() {
    const section = node('section', 'n5-section'); section.id = 'n5BusinessCenter';
    const shell = node('div', 'shell');
    const head = node('div', 'n5-head');
    const copy = node('div'); append(copy, node('div', 'kicker', 'Frontend Next · N5 Business Center'), node('h2', '', '询价、订单与服务进度'), node('p', '', '全部数据来自当前登录账号的生产接口；未返回的数据保持空白，不生成演示金额或虚构进度。'));
    append(head, copy, node('span', 'n5-live', 'REAL ACCOUNT DATA'));
    shell.append(head);

    const summary = node('div', 'n5-summary'); summary.id = 'n5Summary'; shell.append(summary);
    const layout = node('div', 'n5-layout');
    const records = node('section', 'n5-panel');
    const tabs = node('div', 'n5-tabs'); tabs.setAttribute('role', 'tablist');
    [['orders', '我的订单'], ['quotes', '询价记录']].forEach(([key, label]) => {
      const tab = button(label, 'n5-tab'); tab.dataset.n5Tab = key; tab.setAttribute('role', 'tab'); tab.setAttribute('aria-selected', String(state.active === key));
      tab.addEventListener('click', () => { state.active = key; state.detail = null; const detail = $('#n5Detail'); if (detail) { detail.hidden = true; detail.replaceChildren(); } renderRecords(); }); tabs.append(tab);
    });
    records.append(tabs);
    const body = node('div'); body.id = 'n5RecordsBody'; records.append(body);
    const message = node('div', 'n5-message'); message.id = 'n5Message'; message.setAttribute('aria-live', 'polite'); records.append(message);
    const detail = node('div', 'n5-detail'); detail.id = 'n5Detail'; detail.hidden = true; records.append(detail);

    const claim = node('aside', 'n5-panel n5-claim');
    append(claim, node('div', 'kicker', '历史订单归档'), node('h3', '', '历史订单认领'), node('p', 'n5-claim-tip', '候选只显示脱敏编号。认领时必须输入你自己保存的完整订单号 / 业务编号，服务器会再次校验已验证联系方式。'));
    const candidates = node('div', 'n5-claim-list'); candidates.id = 'n5Candidates'; claim.append(candidates);
    const field = node('div', 'n5-claim-field');
    const input = document.createElement('input'); input.id = 'n5ClaimNo'; input.maxLength = 40; input.autocomplete = 'off'; input.placeholder = '完整订单号 / 业务编号';
    append(field, node('label', '', '完整编号'), input); claim.append(field);
    const claimButton = button('确认认领到当前账号', 'btn gold'); claimButton.id = 'n5ClaimButton'; claimButton.addEventListener('click', claimOrder); claim.append(claimButton);
    const warning = node('p', 'n5-claim-tip'); append(warning, node('strong', '', '不会自动认领：'), document.createTextNode('候选列表只用于提示可能存在历史订单，完整编号不从页面泄露。')); claim.append(warning);

    append(layout, records, claim); shell.append(layout); section.append(shell); return section;
  }

  function render() {
    if (!authenticated()) return;
    const existing = $('#n5BusinessCenter');
    if (existing) existing.remove();
    const section = buildShell();
    const n4Section = $('.n4-section', page);
    if (n4Section) n4Section.after(section); else page.append(section);
    renderSummary($('#n5Summary'));
    renderRecords();
    renderCandidates();
    const badge = $('.badge'); if (badge) badge.textContent = 'FRONTEND NEXT · N5 BUSINESS LIVE';
    setMessage('真实业务数据已读取。');
  }

  async function load() {
    if (!authenticated() || state.loading) return;
    state.loading = true;
    const generation = ++state.generation;
    try {
      const results = await Promise.allSettled([
        request('/api/console/summary'),
        request('/api/console/quotes'),
        request('/api/console/orders'),
        request('/api/account/orders/candidates')
      ]);
      if (generation !== state.generation || !authenticated()) return;
      const failures = results.filter((result) => result.status === 'rejected');
      const authFailure = failures.find((result) => result.reason?.status === 401);
      if (authFailure) return;
      state.summary = results[0].status === 'fulfilled' ? results[0].value : null;
      state.quotes = results[1].status === 'fulfilled' ? results[1].value : { items: [], total: 0 };
      state.orders = results[2].status === 'fulfilled' ? results[2].value : { items: [], total: 0 };
      state.candidates = results[3].status === 'fulfilled' && Array.isArray(results[3].value) ? results[3].value : [];
      render();
      if (failures.length) setMessage('部分业务数据暂不可用，其余已成功读取；旧版账号中心仍可回退。', 'error');
    } finally {
      state.loading = false;
    }
  }

  function scheduleMount() {
    if (mountTimer) clearTimeout(mountTimer);
    mountTimer = setTimeout(() => {
      if (!authenticated()) {
        $('#n5BusinessCenter')?.remove();
        return;
      }
      if (!$('#n5BusinessCenter')) load();
    }, 60);
  }

  installStyles();
  observer = new MutationObserver(scheduleMount);
  observer.observe(page, { childList: true, subtree: true });
  document.addEventListener('visibilitychange', () => {
    if (document.visibilityState === 'visible' && page.classList.contains('active') && authenticated()) load();
  });
  window.addEventListener('pagehide', () => {
    if (observer) observer.disconnect();
    if (mountTimer) clearTimeout(mountTimer);
  });

  window.XHFrontendNextN5 = { reload: load, state };
  scheduleMount();
})();