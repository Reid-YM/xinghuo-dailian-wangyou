(function initializeAccount() {
  'use strict';
  const runtimePath = pathname => window.XHFrontendShell?.runtimePath?.(pathname) || pathname || location.pathname;
  const runtimeHref = value => window.XHFrontendShell?.runtimeHref?.(value) || value;
  const navigation = window.XHFrontendShell?.navigation || history;

  if (!/^\/next\/account(?:\/|$)/.test(runtimePath()) && !document.querySelector('.page[data-page="account"].active')) {
    window.addEventListener('xh:pagechange', initializeAccount, { once: true });
    return;
  }

  const BUSINESS_NO_RE = /^(XH[A-Z0-9]{10}|XH-[A-Z2-9]{24})$/;
  const transientParams = new Set(['auth', 'token', 'verifyToken', 'wechat', 'next', 'legacy']);
  let observer = null;
  let openingKey = '';
  let openingTimer = null;
  let closing = false;
  let lastMissing = '';
  let focusedDetail = '';

  function text(value) {
    return String(value ?? '').trim();
  }

  function normalizeBusinessNo(value) {
    let decoded = text(value);
    try { decoded = decodeURIComponent(decoded); } catch (ignore) { return ''; }
    decoded = decoded.toUpperCase();
    return BUSINESS_NO_RE.test(decoded) ? decoded : '';
  }

  function routeInfo(pathname = runtimePath()) {
    pathname = runtimePath(pathname);
    const match = /^\/next\/account\/(requests|orders)\/([^/?#]+)\/?$/.exec(pathname);
    if (!match) return null;
    const businessNo = normalizeBusinessNo(match[2]);
    if (!businessNo) return { invalid: true, section: match[1] };
    return {
      section: match[1],
      kind: match[1] === 'requests' ? 'quote' : 'order',
      tab: match[1] === 'requests' ? 'quotes' : 'orders',
      businessNo,
      parent: `/next/account/${match[1]}`,
      canonical: `/next/account/${match[1]}/${encodeURIComponent(businessNo)}`
    };
  }

  function cleanUrl(pathname, source = location.href) {
    const url = new URL(source, location.origin);
    url.pathname = pathname;
    transientParams.forEach((key) => url.searchParams.delete(key));
    url.hash = '';
    return runtimeHref(url.pathname + url.search);
  }

  function detailUrl(kind, businessNo) {
    const normalized = normalizeBusinessNo(businessNo);
    if (!normalized) return '';
    const section = kind === 'quote' ? 'requests' : 'orders';
    return cleanUrl(`/next/account/${section}/${encodeURIComponent(normalized)}`);
  }

  function parentUrl(info) {
    return info ? cleanUrl(info.parent) : '';
  }

  function currentDetailNo(kind) {
    const detail = window.XHFrontendNextN5?.state?.detail;
    if (!detail) return '';
    if (kind === 'quote') return normalizeBusinessNo(detail.businessNo);
    const order = detail.order || detail;
    return normalizeBusinessNo(order.businessNo || order.orderNo);
  }

  function updateAccountNav() {
    window.XHFrontendNextN17?.updateActive?.();
  }

  function syncParentContext(info) {
    if (!info || info.invalid) return;
    const n5 = window.XHFrontendNextN5;
    const selected = document.querySelector(`.n5-tab[data-n5-tab="${info.tab}"]`)?.getAttribute('aria-selected') === 'true';
    if (n5?.state?.active === info.tab && selected) return;

    const original = location.pathname + location.search + location.hash;
    const parent = parentUrl(info);
    if (!parent) return;
    navigation.replaceState(navigation.state, '', parent);
    try {
      window.XHFrontendNextN16?.applyRoute?.({ canonicalize: false });
    } finally {
      navigation.replaceState(navigation.state, '', original);
      updateAccountNav();
    }
  }

  function rowButton(info) {
    const rows = [...document.querySelectorAll('#n5RecordsBody tr')];
    for (const row of rows) {
      const first = normalizeBusinessNo(row.querySelector('td')?.textContent);
      if (first !== info.businessNo) continue;
      const button = row.querySelector('.n5-mini-btn');
      if (button) return button;
    }
    return null;
  }

  function detailHostVisible() {
    const host = document.querySelector('#n5Detail');
    return !!host && !host.hidden && host.childElementCount > 0;
  }

  function closeRenderedDetail() {
    if (!detailHostVisible()) return false;
    const close = document.querySelector('#n5Detail .n5-detail-head .n5-mini-btn');
    if (!close) return false;
    closing = true;
    try { close.click(); } finally { closing = false; }
    return true;
  }

  function setMessage(value, kind = '') {
    const host = document.querySelector('#n5Message');
    if (!host) return;
    host.className = `n5-message ${kind}`.trim();
    host.textContent = value || '';
  }

  function ensureGuestReturn(info) {
    if (!info || info.invalid || !document.querySelector('#n4LoginForm')) return false;
    const current = new URL(location.href);
    const target = cleanUrl(info.canonical, current.href);
    if (!target) return false;
    if (current.searchParams.get('next') === target) return true;
    current.searchParams.set('next', target);
    navigation.replaceState(navigation.state, '', current.pathname + current.search + current.hash);
    return true;
  }

  function markOpening(key) {
    openingKey = key;
    if (openingTimer) clearTimeout(openingTimer);
    openingTimer = setTimeout(() => {
      openingKey = '';
      openingTimer = null;
    }, 5000);
  }

  function applyDetailRoute() {
    const info = routeInfo();
    if (!info) {
      focusedDetail = '';
      if (detailHostVisible()) closeRenderedDetail();
      return false;
    }
    if (info.invalid) {
      setMessage('业务编号格式无效，请返回个人中心重新选择。', 'error');
      return false;
    }
    if (ensureGuestReturn(info)) return true;
    if (!document.querySelector('#n5BusinessCenter')) return false;

    syncParentContext(info);
    if (currentDetailNo(info.kind) === info.businessNo && detailHostVisible()) {
      openingKey = '';
      const focusKey = `${info.kind}:${info.businessNo}`;
      if (focusedDetail !== focusKey) {
        focusedDetail = focusKey;
        document.querySelector('#n5Detail')?.scrollIntoView({ block: 'start' });
      }
      return true;
    }

    const key = `${info.kind}:${info.businessNo}`;
    if (openingKey === key) return false;
    const button = rowButton(info);
    if (!button) {
      const state = window.XHFrontendNextN5?.state;
      const loaded = info.kind === 'quote' ? state?.quotes != null : state?.orders != null;
      if (loaded && lastMissing !== key) {
        lastMissing = key;
        setMessage('当前账号的业务列表中未找到这条记录，请返回列表重新选择。', 'error');
      }
      return false;
    }

    lastMissing = '';
    markOpening(key);
    button.click();
    return true;
  }

  function pushDetail(kind, businessNo) {
    const target = detailUrl(kind, businessNo);
    if (!target) return false;
    const current = location.pathname + location.search + location.hash;
    if (target === current) return true;
    navigation.pushState({ ...(navigation.state || {}), n18AccountDetail: `${kind}:${normalizeBusinessNo(businessNo)}` }, '', target);
    updateAccountNav();
    return true;
  }

  function pushParent(info) {
    const target = parentUrl(info);
    if (!target) return false;
    const current = location.pathname + location.search + location.hash;
    if (target === current) return true;
    navigation.pushState({ ...(navigation.state || {}), n18AccountDetail: null }, '', target);
    updateAccountNav();
    return true;
  }

  document.addEventListener('click', (event) => {
    const open = event.target.closest?.('#n5RecordsBody .n5-mini-btn');
    if (open) {
      const row = open.closest('tr');
      const businessNo = normalizeBusinessNo(row?.querySelector('td')?.textContent);
      if (businessNo) {
        const label = text(open.textContent);
        pushDetail(label.includes('询价') ? 'quote' : 'order', businessNo);
      }
      return;
    }

    const close = event.target.closest?.('#n5Detail .n5-detail-head .n5-mini-btn');
    if (close && !closing) {
      const info = routeInfo();
      if (info && !info.invalid) pushParent(info);
      return;
    }

    if (event.target.closest?.('#n17AccountNav a[href], .n5-tab[data-n5-tab]')) {
      queueMicrotask(() => {
        if (!routeInfo()) closeRenderedDetail();
      });
    }
  }, true);

  window.addEventListener('popstate', () => {
    openingKey = '';
    focusedDetail = '';
    lastMissing = '';
    if (routeInfo()) applyDetailRoute();
    else closeRenderedDetail();
    updateAccountNav();
  });

  window.addEventListener('xh:pagechange', (event) => {
    if (event.detail?.page === 'account') applyDetailRoute();
  });

  observer = new MutationObserver(() => applyDetailRoute());
  observer.observe(document.body, { childList: true, subtree: true });
  window.addEventListener('pageshow', event => {
    if (event.persisted) {
      observer?.observe(document.body, { childList: true, subtree: true });
      applyDetailRoute();
    }
  });
  window.addEventListener('pagehide', () => {
    observer?.disconnect();
    openingKey = '';
    focusedDetail = '';
    if (openingTimer) clearTimeout(openingTimer);
  });

  applyDetailRoute();

  window.XHFrontendNextN18 = {
    routeInfo,
    detailUrl,
    applyDetailRoute
  };
})();
