(() => {
  'use strict';

  if (!/^\/next\/account(?:\/|$)/.test(location.pathname)) return;

  const items = [
    ['overview', '总览', '/next/account'],
    ['requests', '询价记录', '/next/account/requests'],
    ['orders', '订单记录', '/next/account/orders'],
    ['progress', '工具存档', '/next/account/progress'],
    ['settings', '账号设置', '/next/account/settings']
  ];
  let observer = null;

  function routeKey(pathname = location.pathname) {
    const match = /^\/next\/account(?:\/([^?#]+))?\/?$/.exec(pathname);
    const child = String(match?.[1] || '').replace(/\/$/, '');
    if (['requests', 'quotes', 'demands'].includes(child)) return 'requests';
    if (['orders', 'order', 'order-list'].includes(child)) return 'orders';
    if (['progress', 'tools', 'tool-progress'].includes(child)) return 'progress';
    if (child === 'settings' || child === 'profile' || child === 'contact' || child === 'security'
        || child === 'devices' || child === 'activity' || child.startsWith('settings/')) return 'settings';
    return 'overview';
  }

  function installStyles() {
    if (document.querySelector('#frontend-next-n17-style')) return;
    const style = document.createElement('style');
    style.id = 'frontend-next-n17-style';
    style.textContent = `
      .n17-account-nav-wrap{border-bottom:1px solid var(--line);background:#080b0fcc;backdrop-filter:blur(12px)}
      .n17-account-nav{width:min(var(--max,1420px),calc(100% - 40px));margin:auto;display:flex;gap:7px;padding:12px 0;overflow-x:auto;scrollbar-width:thin}
      .n17-account-link{flex:0 0 auto;border:1px solid #ffffff14;border-radius:10px;padding:9px 12px;color:#8d96a0;font-size:10px;line-height:1;text-decoration:none;background:#0c1015;transition:.18s}
      .n17-account-link:hover{color:#fff;border-color:#ffffff30}.n17-account-link[aria-current="page"]{color:#17130a;background:var(--gold);border-color:var(--gold);font-weight:800}
      .n17-account-link:focus-visible{outline:2px solid #ffcb2d88;outline-offset:2px}
      @media(max-width:680px){.n17-account-nav{width:calc(100% - 26px);padding:10px 0}.n17-account-link{padding:9px 10px}}
    `;
    document.head.append(style);
  }

  function updateActive() {
    const active = routeKey();
    document.querySelectorAll('#n17AccountNav [data-n17-key]').forEach((link) => {
      const selected = link.dataset.n17Key === active;
      if (selected) link.setAttribute('aria-current', 'page');
      else link.removeAttribute('aria-current');
    });
    return active;
  }

  function navigate(href) {
    if (href === location.pathname + location.search + location.hash) return;
    history.pushState({ ...(history.state || {}), n17AccountRoute: href }, '', href);
    const active = updateActive();
    if (active === 'overview') {
      document.querySelector('.n4-shell')?.scrollIntoView({ block: 'start' });
      return;
    }
    window.XHFrontendNextN16?.applyRoute?.();
  }

  function buildNav() {
    const wrap = document.createElement('div');
    wrap.className = 'n17-account-nav-wrap';
    wrap.id = 'n17AccountNavWrap';
    const nav = document.createElement('nav');
    nav.className = 'n17-account-nav';
    nav.id = 'n17AccountNav';
    nav.setAttribute('aria-label', '个人中心导航');
    items.forEach(([key, label, href]) => {
      const link = document.createElement('a');
      link.className = 'n17-account-link';
      link.dataset.n17Key = key;
      link.href = href;
      link.textContent = label;
      nav.append(link);
    });
    wrap.append(nav);
    return wrap;
  }

  function mount() {
    const guest = document.querySelector('#n4LoginForm');
    const head = document.querySelector('.page[data-page="account"] > .n4-shell');
    const existing = document.querySelector('#n17AccountNavWrap');
    if (guest || !head) {
      existing?.remove();
      return false;
    }
    installStyles();
    if (!existing) head.after(buildNav());
    updateActive();
    return true;
  }

  document.addEventListener('click', (event) => {
    const link = event.target.closest?.('#n17AccountNav a[href]');
    if (!link || event.defaultPrevented || event.button > 0 || event.metaKey || event.ctrlKey || event.shiftKey || event.altKey) return;
    const url = new URL(link.href, location.origin);
    if (url.origin !== location.origin || !/^\/next\/account(?:\/|$)/.test(url.pathname)) return;
    event.preventDefault();
    navigate(url.pathname + url.search + url.hash);
  });

  window.addEventListener('popstate', () => {
    const active = updateActive();
    if (active === 'overview') document.querySelector('.n4-shell')?.scrollIntoView({ block: 'start' });
  });
  window.addEventListener('hashchange', updateActive);
  window.addEventListener('xh:pagechange', (event) => {
    if (event.detail?.page === 'account') mount();
  });

  observer = new MutationObserver(() => mount());
  observer.observe(document.body, { childList: true, subtree: true });
  window.addEventListener('pagehide', () => observer?.disconnect(), { once: true });

  mount();

  window.XHFrontendNextN17 = { mount, updateActive, routeKey, navigate };
})();
