(() => {
  'use strict';
  const runtimePath = pathname => window.XHFrontendShell?.runtimePath?.(pathname) || pathname || location.pathname;
  const runtimeHref = value => window.XHFrontendShell?.runtimeHref?.(value) || value;
  const navigation = window.XHFrontendShell?.navigation || history;

  if (!/^\/next\/account(?:\/|$)/.test(runtimePath())) return;

  const transientParams = new Set(['auth', 'token', 'verifyToken', 'wechat']);
  let applying = false;
  let focusedRoute = '';

  const routes = new Map([
    ['requests', { canonical: 'requests', tab: 'quotes', focus: 'business' }],
    ['quotes', { canonical: 'requests', tab: 'quotes', focus: 'business' }],
    ['demands', { canonical: 'requests', tab: 'quotes', focus: 'business' }],
    ['orders', { canonical: 'orders', tab: 'orders', focus: 'business' }],
    ['order', { canonical: 'orders', tab: 'orders', focus: 'business' }],
    ['order-list', { canonical: 'orders', tab: 'orders', focus: 'business' }],
    ['progress', { canonical: 'progress', tab: 'orders', focus: 'progress' }],
    ['tools', { canonical: 'progress', tab: 'orders', focus: 'progress' }],
    ['tool-progress', { canonical: 'progress', tab: 'orders', focus: 'progress' }],
    ['favorites', { canonical: 'favorites', focus: 'favorites' }],
    ['submissions', { canonical: 'submissions', focus: 'submissions' }],
    ['settings', { canonical: 'settings', focus: 'settings' }],
    ['profile', { canonical: 'settings', hash: 'profile', focus: 'profile' }],
    ['settings/profile', { canonical: 'settings', hash: 'profile', focus: 'profile' }],
    ['contact', { canonical: 'settings', hash: 'contact', focus: 'contact' }],
    ['settings/contact', { canonical: 'settings', hash: 'contact', focus: 'contact' }],
    ['security', { canonical: 'settings', hash: 'security', focus: 'security' }],
    ['settings/security', { canonical: 'settings', hash: 'security', focus: 'security' }],
    ['devices', { canonical: 'settings', hash: 'devices', focus: 'devices' }],
    ['activity', { canonical: 'settings', hash: 'activity', focus: 'activity' }]
  ]);
  const settingsHashes = new Map([
    ['profile', routes.get('profile')],
    ['contact', routes.get('contact')],
    ['security', routes.get('security')],
    ['devices', routes.get('devices')],
    ['activity', routes.get('activity')]
  ]);

  function text(value) {
    return String(value ?? '').trim();
  }

  function suffix(pathname = runtimePath()) {
    pathname = runtimePath(pathname);
    const match = /^\/(?:next\/)?account(?:\/(.*?))?\/?$/.exec(pathname);
    return match ? text(match[1]) : '';
  }

  function routeInfo(pathname = runtimePath(), hash = location.hash) {
    const key = suffix(pathname);
    if (key === 'settings') {
      const anchor = text(hash).replace(/^#/, '').toLowerCase();
      if (settingsHashes.has(anchor)) return settingsHashes.get(anchor);
    }
    return routes.get(key) || null;
  }

  function buildCanonical(info, source = location.href) {
    if (!info) return '';
    const url = new URL(source, location.origin);
    url.pathname = `/next/account/${info.canonical}`;
    transientParams.forEach((key) => url.searchParams.delete(key));
    url.searchParams.delete('legacy');
    url.searchParams.delete('next');
    url.hash = info.hash ? `#${info.hash}` : '';
    return runtimeHref(url.pathname + url.search + url.hash);
  }

  function supportedAccountTarget(raw) {
    if (!raw) return '';
    let url;
    try { url = new URL(raw, location.origin); } catch (ignore) { return ''; }
    if (url.origin !== location.origin || url.searchParams.get('legacy') === '1') return '';
    const info = routeInfo(url.pathname, url.hash);
    if (!info) return '';
    return buildCanonical(info, url.href);
  }

  function normalizeReturnParam() {
    const current = new URL(location.href);
    const raw = current.searchParams.get('next');
    const target = supportedAccountTarget(raw);
    if (!target || target === raw) return;
    current.searchParams.set('next', target);
    navigation.replaceState(navigation.state, '', current.pathname + current.search + current.hash);
  }

  function ensureGuestReturnParam() {
    if (!document.querySelector('#n4LoginForm')) return;
    const info = routeInfo();
    if (!info) return;
    const current = new URL(location.href);
    if (current.searchParams.has('next')) return;
    const target = buildCanonical(info, current.href);
    if (!target) return;
    current.searchParams.set('next', target);
    navigation.replaceState(navigation.state, '', current.pathname + current.search + current.hash);
  }

  function normalizeAccountLink(link) {
    if (!(link instanceof HTMLAnchorElement)) return false;
    const raw = link.getAttribute('href');
    if (!raw || raw.startsWith('#')) return false;
    let url;
    try { url = new URL(raw, location.origin); } catch (ignore) { return false; }
    if (url.origin !== location.origin || url.searchParams.get('legacy') === '1') return false;
    const info = routeInfo(url.pathname, url.hash);
    if (!info) return false;
    const target = buildCanonical(info, url.href);
    if (!target || raw === target) return false;
    link.setAttribute('href', target);
    link.dataset.n16Normalized = '1';
    return true;
  }

  function normalizeLinks(root = document) {
    if (root instanceof HTMLAnchorElement) normalizeAccountLink(root);
    root.querySelectorAll?.('a[href]').forEach(normalizeAccountLink);
  }

  function securityActivityPanel() {
    return [...document.querySelectorAll('.n4-panel h3')]
      .find((heading) => text(heading.textContent) === '最近安全活动')
      ?.closest('.n4-panel') || null;
  }

  function focusTarget(info) {
    if (!info) return null;
    if (info.focus === 'business') return document.querySelector('#n5BusinessCenter');
    if (info.focus === 'progress') return document.querySelector('[data-personal-asset="progress"]');
    if (info.focus === 'favorites') return document.querySelector('[data-personal-asset="favorites"]');
    if (info.focus === 'submissions') return document.querySelector('[data-personal-asset="submissions"]');
    if (info.focus === 'settings') return document.querySelector('#n6AccountSecurity');
    if (info.focus === 'profile') return document.querySelector('#n6ProfileForm')?.closest('.n6-card');
    if (info.focus === 'contact') return document.querySelector('#n6ContactForm')?.closest('.n6-card');
    if (info.focus === 'security') return document.querySelector('#n6PasswordForm')?.closest('.n6-card') || document.querySelector('#n6AccountSecurity');
    if (info.focus === 'devices') return document.querySelector('#n6Devices')?.closest('.n6-card') || document.querySelector('#n6AccountSecurity');
    if (info.focus === 'activity') return securityActivityPanel() || document.querySelector('#n6AccountSecurity');
    return null;
  }

  function focusRoute(info) {
    if (!info) return false;
    const key = `${runtimePath()}${location.hash}:${info.focus || ''}`;
    if (focusedRoute === key) return true;
    const target = focusTarget(info);
    if (!target) return false;
    focusedRoute = key;
    target.scrollIntoView({ block: 'start' });
    if (typeof target.focus === 'function' && target.matches('input,button,a,select,textarea,[tabindex]')) {
      target.focus({ preventScroll: true });
    }
    return true;
  }

  function syncN5(info) {
    if (!info?.tab) return;
    const n5 = window.XHFrontendNextN5;
    if (n5?.state) n5.state.active = info.tab;
    const tab = document.querySelector(`.n5-tab[data-n5-tab="${info.tab}"]`);
    if (tab && tab.getAttribute('aria-selected') !== 'true') {
      applying = true;
      tab.click();
      applying = false;
    }
  }

  function canonicalizeAlias(info) {
    if (!info) return;
    const canonical = buildCanonical(info);
    const current = location.pathname + location.search + location.hash;
    if (canonical && canonical !== current) navigation.replaceState(navigation.state, '', canonical);
  }

  function applyRoute(options = {}) {
    const info = routeInfo();
    if (!info) return;
    if (options.canonicalize !== false) canonicalizeAlias(info);
    syncN5(info);
    window.requestAnimationFrame(() => focusRoute(info));
  }

  function tabTarget(tab) {
    return tab === 'quotes' ? '/next/account/requests' : '/next/account/orders';
  }

  document.addEventListener('click', (event) => {
    const link = event.target.closest?.('a[href]');
    if (link) normalizeAccountLink(link);

    const tab = event.target.closest?.('.n5-tab[data-n5-tab]');
    if (!tab || applying) return;
    const target = tabTarget(tab.dataset.n5Tab);
    const url = new URL(location.href);
    url.pathname = target;
    transientParams.forEach((key) => url.searchParams.delete(key));
    url.searchParams.delete('legacy');
    url.searchParams.delete('next');
    url.hash = '';
    const next = runtimeHref(url.pathname + url.search);
    if (next !== location.pathname + location.search + location.hash) {
      navigation.pushState({ ...(navigation.state || {}), n16AccountRoute: tab.dataset.n5Tab }, '', next);
      focusedRoute = '';
      window.requestAnimationFrame(() => focusRoute(routeInfo()));
    }
  });

  window.addEventListener('popstate', () => {
    focusedRoute = '';
    applyRoute({ canonicalize: false });
  });
  window.addEventListener('hashchange', () => {
    focusedRoute = '';
    applyRoute({ canonicalize: false });
  });

  window.addEventListener('xh:pagechange', (event) => {
    if (event.detail?.page === 'account') {
      focusedRoute = '';
      applyRoute();
    }
  });

  const observer = new MutationObserver((records) => {
    records.forEach((record) => record.addedNodes.forEach((node) => {
      if (node instanceof Element) normalizeLinks(node);
    }));
    normalizeReturnParam();
    ensureGuestReturnParam();
    const info = routeInfo();
    syncN5(info);
    focusRoute(info);
  });
  observer.observe(document.body, { childList: true, subtree: true });

  normalizeReturnParam();
  normalizeLinks();
  applyRoute();
  ensureGuestReturnParam();

  window.XHFrontendNextN16 = {
    routeInfo,
    supportedAccountTarget,
    normalizeAccountLink,
    applyRoute
  };
})();
