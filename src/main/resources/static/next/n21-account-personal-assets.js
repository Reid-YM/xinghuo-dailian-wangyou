(() => {
  'use strict';

  if (!/^\/next\/account(?:\/|$)/.test(location.pathname)) return;

  const PAGE_MAX = 50;
  const ASSETS = new Map([
    ['progress', { label: '工具存档', paged: false }],
    ['favorites', { label: '我的收藏', paged: true }],
    ['submissions', { label: '我的投稿', paged: true }]
  ]);
  let observer = null;
  let mountTimer = null;
  let applyingPager = false;
  let pendingPager = null;

  function text(value) {
    return String(value ?? '').trim();
  }

  function assetKey(pathname = location.pathname) {
    const match = /^\/next\/account(?:\/([^/?#]+))?\/?$/.exec(pathname);
    const child = text(match?.[1]).toLowerCase();
    if (['progress', 'tools', 'tool-progress'].includes(child)) return 'progress';
    if (child === 'favorites') return 'favorites';
    if (child === 'submissions') return 'submissions';
    return '';
  }

  function safePage(raw) {
    if (raw == null || raw === '') return 1;
    if (!/^\d{1,4}$/.test(String(raw))) return 1;
    const value = Number(raw);
    return Number.isInteger(value) && value >= 1 && value <= PAGE_MAX ? value : 1;
  }

  function requestedPage(key = assetKey()) {
    if (!ASSETS.get(key)?.paged) return 1;
    return safePage(new URLSearchParams(location.search).get('page'));
  }

  function canonicalUrl(key = assetKey(), page = requestedPage(key)) {
    const url = new URL(location.href);
    if (!ASSETS.has(key)) return url.pathname + url.search + url.hash;
    if (ASSETS.get(key).paged && page > 1) url.searchParams.set('page', String(page));
    else url.searchParams.delete('page');
    return url.pathname + url.search + url.hash;
  }

  function canonicalize() {
    const key = assetKey();
    if (!key) return;
    const target = canonicalUrl(key, requestedPage(key));
    const current = location.pathname + location.search + location.hash;
    if (target !== current) history.replaceState(history.state, '', target);
  }

  function assetSection(key) {
    return document.querySelector(`[data-personal-asset="${key}"]`);
  }

  function pagerInfo(key) {
    const section = assetSection(key);
    if (!section || !ASSETS.get(key)?.paged) return null;
    const navs = [...section.querySelectorAll('.n5-tabs')];
    const nav = navs.find((item) => [...item.querySelectorAll('button')].some((button) => text(button.textContent) === '上一页'));
    if (!nav) return null;
    const buttons = [...nav.querySelectorAll('button')];
    const previous = buttons.find((button) => text(button.textContent) === '上一页') || null;
    const next = buttons.find((button) => text(button.textContent) === '下一页') || null;
    const label = [...nav.querySelectorAll('span')].find((node) => /第\s*\d+\s*页/.test(text(node.textContent))) || null;
    const match = text(label?.textContent).match(/第\s*(\d+)\s*页/);
    const page = match ? Number(match[1]) : 1;
    return { nav, previous, next, page: Number.isInteger(page) && page > 0 ? page : 1 };
  }

  function writePage(key, page, mode = 'push') {
    if (!ASSETS.get(key)?.paged) return false;
    const safe = Math.min(PAGE_MAX, Math.max(1, Number(page) || 1));
    const url = new URL(location.href);
    url.pathname = `/next/account/${key}`;
    if (safe > 1) url.searchParams.set('page', String(safe));
    else url.searchParams.delete('page');
    ['auth', 'token', 'verifyToken', 'wechat', 'legacy', 'next'].forEach((name) => url.searchParams.delete(name));
    url.hash = '';
    const target = url.pathname + url.search;
    const current = location.pathname + location.search + location.hash;
    if (target === current) return false;
    const state = { ...(history.state || {}), n21Asset: key, n21Page: safe };
    if (mode === 'replace') history.replaceState(state, '', target);
    else history.pushState(state, '', target);
    return true;
  }

  function syncPager() {
    const key = assetKey();
    if (!ASSETS.get(key)?.paged) return true;
    const pager = pagerInfo(key);
    if (!pager) return false;
    const target = requestedPage(key);
    if (pager.page === target) return true;

    if (target > pager.page) {
      if (!pager.next || pager.next.disabled) {
        writePage(key, pager.page, 'replace');
        return true;
      }
      applyingPager = true;
      pager.next.click();
      applyingPager = false;
      return false;
    }

    if (!pager.previous || pager.previous.disabled) {
      writePage(key, pager.page, 'replace');
      return true;
    }
    applyingPager = true;
    pager.previous.click();
    applyingPager = false;
    return false;
  }

  function installStyles() {
    if (document.querySelector('#frontend-next-n21-style')) return;
    const style = document.createElement('style');
    style.id = 'frontend-next-n21-style';
    style.textContent = `
      .n21-asset-nav{display:flex;gap:7px;flex-wrap:wrap;margin:10px 0 14px;padding:10px;border:1px solid #ffffff10;border-radius:11px;background:#090d11}
      .n21-asset-link{flex:0 0 auto;border:1px solid #ffffff18;border-radius:8px;padding:8px 10px;color:#87909a;font-size:9px;line-height:1;text-decoration:none;background:#10161d}
      .n21-asset-link:hover{color:#fff;border-color:#ffffff32}.n21-asset-link[aria-current="page"]{color:#17130a;background:var(--gold);border-color:var(--gold);font-weight:800}
      .n21-asset-note{width:100%;color:#68727c;font-size:8px;line-height:1.5}
      .n21-asset-link:focus-visible{outline:2px solid #ffcb2d88;outline-offset:2px}
    `;
    document.head.append(style);
  }

  function buildNav() {
    const nav = document.createElement('nav');
    nav.id = 'n21AssetNav';
    nav.className = 'n21-asset-nav';
    nav.setAttribute('aria-label', '个人资产导航');
    ASSETS.forEach((meta, key) => {
      const link = document.createElement('a');
      link.className = 'n21-asset-link';
      link.dataset.n21Asset = key;
      link.href = `/next/account/${key}`;
      link.textContent = meta.label;
      nav.append(link);
    });
    const note = document.createElement('span');
    note.className = 'n21-asset-note';
    note.textContent = '收藏与投稿分页复用现有账号接口；工具站、攻略详情和投稿编辑器暂无 Next 对应页时继续使用旧版入口。';
    nav.append(note);
    return nav;
  }

  function updateNav() {
    const active = assetKey();
    document.querySelectorAll('#n21AssetNav [data-n21-asset]').forEach((link) => {
      if (link.dataset.n21Asset === active) link.setAttribute('aria-current', 'page');
      else link.removeAttribute('aria-current');
    });
  }

  function markLegacyTargets() {
    const root = document.querySelector('#n5PersonalAssets');
    if (!root) return;
    root.querySelectorAll('a[href]').forEach((link) => {
      const raw = link.getAttribute('href') || '';
      if (raw === '/mastery' || raw === '/account/submit' || /^\/guides\//.test(raw)) {
        link.dataset.n14Legacy = '1';
        link.dataset.n21Legacy = '1';
      }
    });
    window.XHFrontendNextN14?.normalize?.(root);
  }

  function focusAsset() {
    const key = assetKey();
    if (!key) return false;
    const section = assetSection(key);
    if (!section) return false;
    const marker = `${key}:${location.pathname}${location.search}`;
    if (section.dataset.n21Focused !== marker) {
      section.dataset.n21Focused = marker;
      section.scrollIntoView({ block: 'start' });
    }
    return true;
  }

  function mount() {
    const root = document.querySelector('#n5PersonalAssets');
    if (!root) return false;
    installStyles();
    let nav = document.querySelector('#n21AssetNav');
    if (!nav) {
      nav = buildNav();
      const heading = root.querySelector('h3');
      if (heading) heading.after(nav); else root.prepend(nav);
    }
    markLegacyTargets();
    updateNav();
    canonicalize();
    syncPager();
    focusAsset();
    return true;
  }

  function scheduleMount() {
    if (mountTimer) clearTimeout(mountTimer);
    mountTimer = setTimeout(() => mount(), 0);
  }

  document.addEventListener('click', (event) => {
    const assetLink = event.target.closest?.('#n21AssetNav a[data-n21-asset]');
    if (assetLink && !event.defaultPrevented && event.button === 0 && !event.metaKey && !event.ctrlKey && !event.shiftKey && !event.altKey) {
      event.preventDefault();
      const key = assetLink.dataset.n21Asset;
      const href = `/next/account/${key}`;
      history.pushState({ ...(history.state || {}), n21Asset: key, n21Page: 1 }, '', href);
      window.XHFrontendNextN17?.updateActive?.();
      window.XHFrontendNextN16?.applyRoute?.({ canonicalize: false });
      scheduleMount();
    }
  });

  document.addEventListener('click', (event) => {
    if (applyingPager) return;
    const path = event.composedPath?.() || [];
    const button = path.find((node) => node instanceof HTMLButtonElement && ['上一页', '下一页'].includes(text(node.textContent)));
    if (!button) return;
    const section = path.find((node) => node instanceof HTMLElement && node.dataset?.personalAsset);
    const key = section?.dataset?.personalAsset;
    if (!ASSETS.get(key)?.paged) return;
    const pager = pagerInfo(key);
    if (!pager) return;
    const delta = text(button.textContent) === '下一页' ? 1 : -1;
    pendingPager = { key, page: Math.max(1, pager.page + delta) };
  }, true);

  document.addEventListener('click', () => {
    if (!pendingPager) return;
    const pending = pendingPager;
    pendingPager = null;
    queueMicrotask(() => writePage(pending.key, pending.page, 'push'));
  });

  window.addEventListener('popstate', () => {
    window.XHFrontendNextN17?.updateActive?.();
    window.XHFrontendNextN16?.applyRoute?.({ canonicalize: false });
    scheduleMount();
  });
  window.addEventListener('xh:pagechange', (event) => {
    if (event.detail?.page === 'account') scheduleMount();
  });

  observer = new MutationObserver(scheduleMount);
  observer.observe(document.body, { childList: true, subtree: true });
  window.addEventListener('pagehide', () => {
    observer?.disconnect();
    if (mountTimer) clearTimeout(mountTimer);
  }, { once: true });

  window.XHFrontendNextN21 = {
    assetKey,
    requestedPage,
    pagerInfo,
    writePage,
    mount
  };

  mount();
})();
