(() => {
  'use strict';

  if (!/^\/next\/account(?:\/|$)/.test(location.pathname)) return;

  const BUSINESS_NO_RE = /^(XH[A-Z0-9]{10}|XH-[A-Z2-9]{24})$/;
  let observer = null;

  function text(value) {
    return String(value ?? '').trim();
  }

  function normalizeBusinessNo(value) {
    let normalized = text(value).toUpperCase();
    try { normalized = decodeURIComponent(normalized).toUpperCase(); } catch (ignore) { return ''; }
    return BUSINESS_NO_RE.test(normalized) ? normalized : '';
  }

  function currentInfo() {
    const info = window.XHFrontendNextN18?.routeInfo?.();
    if (!info || info.invalid || !info.businessNo || !['quote', 'order'].includes(info.kind)) return null;
    return info;
  }

  function listItems(info) {
    const state = window.XHFrontendNextN5?.state;
    if (!state || !info) return [];
    const source = info.kind === 'quote' ? state.quotes?.items : state.orders?.items;
    return Array.isArray(source) ? source : [];
  }

  function itemBusinessNo(info, item) {
    if (!info || !item) return '';
    return normalizeBusinessNo(info.kind === 'quote' ? item.businessNo : (item.businessNo || item.orderNo));
  }

  function listContext(info) {
    const items = listItems(info);
    const ids = items.map((item) => itemBusinessNo(info, item)).filter(Boolean);
    const index = ids.indexOf(info.businessNo);
    return {
      ids,
      index,
      previous: index > 0 ? ids[index - 1] : '',
      next: index >= 0 && index < ids.length - 1 ? ids[index + 1] : ''
    };
  }

  function loadedQuote(businessNo) {
    const normalized = normalizeBusinessNo(businessNo);
    if (!normalized) return false;
    const items = window.XHFrontendNextN5?.state?.quotes?.items;
    return Array.isArray(items) && items.some((item) => normalizeBusinessNo(item?.businessNo) === normalized);
  }

  function sourceQuoteNo(info) {
    if (info?.kind !== 'order') return '';
    const detail = window.XHFrontendNextN5?.state?.detail;
    const source = normalizeBusinessNo(detail?.sourceQuote?.businessNo);
    return source && loadedQuote(source) ? source : '';
  }

  function installStyles() {
    if (document.querySelector('#frontend-next-n19-style')) return;
    const style = document.createElement('style');
    style.id = 'frontend-next-n19-style';
    style.textContent = `
      .n19-detail-context{margin:0 0 14px;padding:12px;border:1px solid #ffffff12;border-radius:12px;background:#090d11;display:flex;justify-content:space-between;gap:12px;align-items:center;flex-wrap:wrap}
      .n19-detail-meta{display:grid;gap:3px}.n19-detail-meta strong{font-size:10px}.n19-detail-meta span{color:#737d87;font-size:9px;line-height:1.45}
      .n19-detail-actions{display:flex;gap:7px;align-items:center;flex-wrap:wrap}.n19-detail-actions .n19-btn{appearance:none;border:1px solid #ffffff18;background:#10161d;color:#d9dee3;border-radius:8px;padding:8px 10px;font:inherit;font-size:9px;line-height:1;cursor:pointer;text-decoration:none}
      .n19-detail-actions .n19-btn:hover:not(:disabled){border-color:#ffcb2d66;color:#fff}.n19-detail-actions .n19-btn.primary{border-color:#ffcb2d66;color:#ffd863;background:#17140b}.n19-detail-actions .n19-btn:disabled{opacity:.38;cursor:not-allowed}
      @media(max-width:620px){.n19-detail-context{align-items:stretch}.n19-detail-actions{width:100%}.n19-detail-actions .n19-btn{flex:1 1 auto;text-align:center}}
    `;
    document.head.append(style);
  }

  function routeTo(kind, businessNo) {
    const target = window.XHFrontendNextN18?.detailUrl?.(kind, businessNo);
    if (!target || target === location.pathname + location.search + location.hash) return false;
    history.pushState({ ...(history.state || {}), n19DetailContext: `${kind}:${normalizeBusinessNo(businessNo)}` }, '', target);
    window.XHFrontendNextN17?.updateActive?.();
    window.XHFrontendNextN18?.applyDetailRoute?.();
    return true;
  }

  function backToList(info) {
    if (!info?.parent) return false;
    const url = new URL(location.href);
    url.pathname = info.parent;
    ['auth', 'token', 'verifyToken', 'wechat', 'next', 'legacy', 'from'].forEach((key) => url.searchParams.delete(key));
    url.hash = '';
    const target = url.pathname + url.search;
    if (target === location.pathname + location.search + location.hash) return false;
    history.pushState({ ...(history.state || {}), n19DetailContext: null }, '', target);
    window.XHFrontendNextN17?.updateActive?.();
    window.XHFrontendNextN18?.applyDetailRoute?.();
    return true;
  }

  function button(label, disabled, action, primary = false) {
    const node = document.createElement('button');
    node.type = 'button';
    node.className = `n19-btn${primary ? ' primary' : ''}`;
    node.textContent = label;
    node.disabled = !!disabled;
    node.addEventListener('click', action);
    return node;
  }

  function link(label, href, primary = false) {
    const node = document.createElement('a');
    node.className = `n19-btn${primary ? ' primary' : ''}`;
    node.textContent = label;
    node.href = href;
    return node;
  }

  function build(info) {
    const context = listContext(info);
    const sourceQuote = sourceQuoteNo(info);
    const wrap = document.createElement('div');
    wrap.id = 'n19DetailContext';
    wrap.className = 'n19-detail-context';
    wrap.dataset.n19Key = `${info.kind}:${info.businessNo}`;

    const meta = document.createElement('div');
    meta.className = 'n19-detail-meta';
    const title = document.createElement('strong');
    title.textContent = info.kind === 'quote' ? '询价详情上下文' : '订单详情上下文';
    const status = document.createElement('span');
    status.textContent = context.index >= 0
      ? `当前已加载列表第 ${context.index + 1} / ${context.ids.length} 条 · ${info.businessNo}`
      : `当前详情 · ${info.businessNo}`;
    meta.append(title, status);

    const actions = document.createElement('div');
    actions.className = 'n19-detail-actions';
    actions.append(
      button('上一条', !context.previous, () => routeTo(info.kind, context.previous)),
      button('下一条', !context.next, () => routeTo(info.kind, context.next)),
      button(info.kind === 'quote' ? '返回询价列表' : '返回订单列表', false, () => backToList(info), true)
    );

    if (sourceQuote) {
      actions.append(button('来源询价', false, () => routeTo('quote', sourceQuote)));
    }

    const record = new URL(`/next/record/${encodeURIComponent(info.businessNo)}`, location.origin);
    record.searchParams.set('from', info.kind === 'quote' ? 'account-requests' : 'account-orders');
    actions.append(link('查看公开业务记录', record.pathname + record.search));

    wrap.append(meta, actions);
    return wrap;
  }

  function mount() {
    const info = currentInfo();
    const detail = document.querySelector('#n5Detail');
    const head = detail?.querySelector('.n5-detail-head');
    const existing = document.querySelector('#n19DetailContext');
    if (!info || !detail || detail.hidden || !head) {
      existing?.remove();
      return false;
    }

    installStyles();
    const key = `${info.kind}:${info.businessNo}`;
    if (existing?.dataset.n19Key === key) {
      const expected = listContext(info);
      const label = existing.querySelector('.n19-detail-meta span');
      const nextText = expected.index >= 0
        ? `当前已加载列表第 ${expected.index + 1} / ${expected.ids.length} 条 · ${info.businessNo}`
        : `当前详情 · ${info.businessNo}`;
      if (label && label.textContent !== nextText) {
        existing.replaceWith(build(info));
      }
      return true;
    }
    existing?.remove();
    head.after(build(info));
    return true;
  }

  window.addEventListener('popstate', () => queueMicrotask(mount));
  window.addEventListener('xh:pagechange', (event) => {
    if (event.detail?.page === 'account') queueMicrotask(mount);
  });

  observer = new MutationObserver(() => mount());
  observer.observe(document.body, { childList: true, subtree: true });
  window.addEventListener('pagehide', () => observer?.disconnect(), { once: true });

  mount();
  window.XHFrontendNextN19 = { mount, listContext, routeTo, backToList };
})();
