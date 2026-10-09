/* Mobile frontend only. Desktop pages never load this file. */
(() => {
  const root = document.documentElement;
  root.dataset.frontend = 'mobile';

  const icon = name => ({
    home: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M3.5 10.7 12 3.5l8.5 7.2"/><path d="M5.5 9.7V20h13V9.7"/><path d="M9.5 20v-6h5v6"/></svg>',
    services: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M4 5h16v5H4z"/><path d="M4 14h7v5H4z"/><path d="M15 14h5v5h-5z"/></svg>',
    quote: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M6 4h12a2 2 0 0 1 2 2v8a2 2 0 0 1-2 2h-7l-5 4v-4a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2Z"/><path d="M8 9h8M8 12h5"/></svg>',
    records: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M6 4h12v16H6z"/><path d="M9 8h6M9 12h6M9 16h4"/></svg>',
    account: '<svg viewBox="0 0 24 24" aria-hidden="true"><circle cx="12" cy="8" r="3.5"/><path d="M5 20c.8-4 3.1-6 7-6s6.2 2 7 6"/></svg>'
  }[name] || '');

  const currentPath = () => {
    const path = String(location.pathname || '/').replace(/\/+$/, '');
    return path || '/';
  };

  const pathActive = (href, path) => {
    if (href === '/') return path === '/';
    if (href === '/quote') return path === '/quote';
    if (href === '/services') return path === '/services' || path.startsWith('/services/');
    if (href === '/records') return path === '/records' || path.startsWith('/record') || path.startsWith('/order');
    if (href === '/account') return path === '/account' || path.startsWith('/account/') || path.startsWith('/console');
    return path === href;
  };

  function installHeaderAction() {
    const topbar = document.querySelector('.site-header .topbar');
    if (!topbar || topbar.querySelector('.mobile-header-cta')) return;
    const action = document.createElement('a');
    action.className = 'mobile-header-cta';
    action.href = '/quote';
    action.textContent = currentPath() === '/quote' ? '服务大厅' : '立即询价';
    if (currentPath() === '/quote') action.href = '/services';
    topbar.appendChild(action);
  }

  function installPrimaryNav() {
    if (!document.body || document.getElementById('mobilePrimaryNav')) return;
    const path = currentPath();
    const nav = document.createElement('nav');
    nav.id = 'mobilePrimaryNav';
    nav.className = 'mobile-primary-nav';
    nav.setAttribute('aria-label', '手机主导航');

    const items = [
      ['/', 'home', '首页', ''],
      ['/services', 'services', '服务', ''],
      ['/quote', 'quote', '询价', 'mobile-nav-quote'],
      ['/records', 'records', '进度', ''],
      ['/account', 'account', '我的', '']
    ];

    nav.innerHTML = items.map(([href, iconName, label, extra]) => {
      const active = pathActive(href, path);
      const iconHtml = extra
        ? `<span class="mobile-nav-icon-wrap">${icon(iconName)}</span>`
        : icon(iconName);
      return `<a href="${href}" class="${extra}"${active ? ' aria-current="page"' : ''}>${iconHtml}<span>${label}</span></a>`;
    }).join('');
    document.body.appendChild(nav);
  }

  function normalizeHomeHeroEdge() {
    if (!document.body || document.body.dataset.page !== 'home') return;
    const hero = document.getElementById('homeHero');
    if (!hero) return;
    hero.style.setProperty('width', '100vw', 'important');
    hero.style.setProperty('max-width', 'none', 'important');
    hero.style.setProperty('margin-left', 'calc(50% - 50vw)', 'important');
    hero.style.setProperty('margin-right', 'calc(50% - 50vw)', 'important');
    hero.style.setProperty('border', '0', 'important');
    hero.style.setProperty('border-radius', '0', 'important');
  }

  function syncHeroBackdrop() {
    if (!document.body || document.body.dataset.page !== 'home') return;
    const hero = document.getElementById('homeHero');
    const layer = document.getElementById('heroBackground');
    if (!hero || !layer) return;

    let backdrop = hero.querySelector(':scope > .mobile-hero-backdrop');
    if (!backdrop) {
      backdrop = document.createElement('div');
      backdrop.className = 'mobile-hero-backdrop';
      backdrop.setAttribute('aria-hidden', 'true');
      hero.insertBefore(backdrop, layer);
    }

    let syncing = false;
    const sync = () => {
      if (syncing) return;
      syncing = true;
      try {
        const style = getComputedStyle(layer);
        const image = layer.style.getPropertyValue('background-image') || style.backgroundImage || 'none';
        const configuredSize = layer.style.getPropertyValue('background-size') || 'cover';
        const configuredPosition = layer.style.getPropertyValue('background-position') || '50% 50%';

        // Legacy shared CSS contains high-specificity !important cover/center rules.
        // Mobile owns its foreground renderer now, so preserve the runtime/admin fit + position
        // as inline-important values while the separate backdrop remains cover-filled.
        if (layer.style.getPropertyPriority('background-size') !== 'important') {
          layer.style.setProperty('background-size', configuredSize, 'important');
        }
        if (layer.style.getPropertyPriority('background-position') !== 'important') {
          layer.style.setProperty('background-position', configuredPosition, 'important');
        }

        backdrop.style.backgroundImage = image;
        backdrop.style.backgroundPosition = configuredPosition;
        backdrop.hidden = !image || image === 'none';
      } finally {
        syncing = false;
      }
    };

    sync();
    const observer = new MutationObserver(sync);
    observer.observe(layer, { attributes: true, attributeFilter: ['style', 'class'] });
  }

  function compactHomeQuoteDetails() {
    if (!document.body || document.body.dataset.page !== 'home') return;
    if (window.innerWidth > 520) return;
    document.querySelectorAll('details[data-responsive-details]').forEach(details => {
      if (!(details instanceof HTMLDetailsElement)) return;
      if (details.dataset.mobileInitialised) return;
      details.dataset.mobileInitialised = 'true';
      details.open = false;
    });
  }

  function setViewportUnit() {
    const height = window.visualViewport?.height || window.innerHeight || 0;
    if (height > 0) root.style.setProperty('--mobile-viewport-height', `${Math.round(height)}px`);
  }

  const boot = () => {
    document.body?.classList.add('mobile-frontend');
    document.body?.setAttribute('data-frontend', 'mobile');
    setViewportUnit();
    installHeaderAction();
    installPrimaryNav();
    normalizeHomeHeroEdge();
    syncHeroBackdrop();
    compactHomeQuoteDetails();
    window.addEventListener('resize', setViewportUnit, { passive: true });
    window.visualViewport?.addEventListener('resize', setViewportUnit, { passive: true });
  };

  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', boot, { once: true });
  else boot();
})();
