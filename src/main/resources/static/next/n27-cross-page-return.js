(() => {
  'use strict';

  if (!/^\/next(?:\/|$)/.test(location.pathname)) return;

  const text = (value) => String(value ?? '').trim();
  const accountPaths = new Set([
    'requests','quotes','demands','orders','order','order-list','progress','tools','tool-progress',
    'favorites','submissions','submit','settings','profile','settings/profile','contact','settings/contact',
    'security','settings/security','devices','activity'
  ]);

  function parse(raw) {
    const value = text(raw);
    if (!value || value.length > 1000 || !value.startsWith('/') || value.startsWith('//')) return null;
    if (value.includes('\\') || value.includes('\r') || value.includes('\n') || value.toLowerCase().includes('%5c')) return null;
    try {
      const url = new URL(value, location.origin);
      return url.origin === location.origin ? url : null;
    } catch (ignore) {
      return null;
    }
  }

  function nextPath(url) {
    if (!url || url.searchParams.get('legacy') === '1') return '';
    const path = url.pathname.replace(/\/$/, '') || '/';

    const exact = new Map([
      ['/', '/next'],
      ['/home', '/next'],
      ['/services', '/next/services'],
      ['/quote', '/next/quote'],
      ['/guides', '/next/guides'],
      ['/mastery', '/next/mastery'],
      ['/records', '/next/records'],
      ['/contact', '/next/contact'],
      ['/trust', '/next/trust'],
      ['/account', '/next/account'],
      ['/account/submit', '/next/account/submit']
    ]);
    if (exact.has(path)) return exact.get(path);

    if (/^\/service\/[^/]+$/.test(path)) return '/next' + path;
    if (/^\/guides\/[^/]+$/.test(path)) return '/next' + path;
    if (/^\/record\/[^/]+$/.test(path)) return '/next' + path;
    if (/^\/account\/(requests|orders)\/[^/]+$/.test(path)) return '/next' + path;

    const account = /^\/account\/(.+)$/.exec(path);
    if (account && accountPaths.has(account[1])) return '/next' + path;
    return '';
  }

  function normalizeReturn(raw) {
    const url = parse(raw);
    if (!url) return raw || '';
    const path = nextPath(url);
    if (!path) return url.pathname + url.search + url.hash;
    url.pathname = path;
    url.searchParams.delete('legacy');
    return url.pathname + url.search + url.hash;
  }

  function normalizeCurrentNext() {
    const current = new URL(location.href);
    const raw = current.searchParams.get('next');
    if (!raw) return false;
    const normalized = normalizeReturn(raw);
    if (!normalized || normalized === raw) return false;
    current.searchParams.set('next', normalized);
    history.replaceState(history.state, '', current.pathname + current.search + current.hash);
    return true;
  }

  function normalizeNestedNext(link) {
    if (!(link instanceof HTMLAnchorElement)) return false;
    const raw = link.getAttribute('href');
    if (!raw || raw.startsWith('#')) return false;
    let url;
    try { url = new URL(raw, location.origin); } catch (ignore) { return false; }
    if (url.origin !== location.origin || !url.searchParams.has('next')) return false;
    if (!/^\/(?:next\/account|account\/(?:login|register|reset)|api\/account\/wechat\/start)/.test(url.pathname)) return false;
    const before = url.searchParams.get('next');
    const after = normalizeReturn(before);
    if (!after || after === before) return false;
    url.searchParams.set('next', after);
    link.setAttribute('href', url.pathname + url.search + url.hash);
    link.dataset.n27Return = '1';
    return true;
  }

  function normalizeLinks(root = document) {
    if (root instanceof HTMLAnchorElement) normalizeNestedNext(root);
    root.querySelectorAll?.('a[href*="next="]').forEach(normalizeNestedNext);
  }

  function wrapShellReturn() {
    const shell = window.XHFrontendShell;
    if (!shell || shell.__n27ReturnWrapped) return false;
    const previous = typeof shell.safeAccountReturn === 'function' ? shell.safeAccountReturn.bind(shell) : null;
    shell.safeAccountReturn = (requested) => {
      const normalized = normalizeReturn(requested);
      return previous ? previous(normalized) : normalized || null;
    };
    shell.__n27ReturnWrapped = true;
    return true;
  }

  normalizeCurrentNext();
  wrapShellReturn();
  normalizeLinks();

  const observer = new MutationObserver((records) => {
    normalizeCurrentNext();
    wrapShellReturn();
    records.forEach((record) => record.addedNodes.forEach((node) => {
      if (node instanceof Element) normalizeLinks(node);
    }));
  });
  observer.observe(document.body, { childList: true, subtree: true });

  window.addEventListener('popstate', () => {
    normalizeCurrentNext();
    wrapShellReturn();
    normalizeLinks();
  });
  window.addEventListener('xh:pagechange', () => {
    normalizeCurrentNext();
    wrapShellReturn();
    normalizeLinks();
  });
  window.addEventListener('pagehide', () => observer.disconnect(), { once: true });

  window.XHFrontendNextN27 = { normalizeReturn, normalizeCurrentNext, normalizeNestedNext };
})();
