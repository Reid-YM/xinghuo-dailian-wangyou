(() => {
  'use strict';

  const getBar = () => document.getElementById('announcementBar');
  const brandLogoHref = '/assets/brand-logo.svg?v=20261008logo01';
  const FALLBACK_LINKS = [
    ['/services', '服务'],
    ['/guides', '攻略'],
    ['/mastery', 'MW4 工具站'],
    ['/records', '业务记录'],
    ['/account', '我的'],
    ['/contact', '一对一咨询']
  ];
  const SCROLLERS = [
    '.filters', '.trust', '.hero-flow', '.page-stepbar', '.service-filterbar',
    '.game-filter-rail', '.platform-filter-rail', '.browser-row .filters',
    '.home-proof-strip', '.hot-rail', '.home-game-rail', '.mobile-x-scroll',
    '.actions', '.row-actions', '.sidebar-nav'
  ].join(',');

  let announcementFrame = 0;

  function ensureBrandAssets() {
    if (!document.head) return;
    let icon = document.head.querySelector('link[data-xh-brand-favicon]');
    if (!icon) {
      icon = document.createElement('link');
      icon.rel = 'icon';
      icon.type = 'image/svg+xml';
      icon.setAttribute('sizes', 'any');
      icon.setAttribute('data-xh-brand-favicon', 'true');
      document.head.appendChild(icon);
    }
    icon.href = brandLogoHref;
  }

  function ensureFloatingSupport() {
    if (document.getElementById('floatingCustomerService')) return;
    const link = document.createElement('a');
    link.id = 'floatingCustomerService';
    link.className = 'floating-customer-service';
    link.href = '/contact';
    link.setAttribute('aria-label', '联系客服');
    link.setAttribute('data-global-visible', 'navContactVisible');
    link.innerHTML = '<span class="floating-customer-service-icon" aria-hidden="true"><svg viewBox="0 0 24 24" focusable="false" style="width:20px;height:20px;fill:none;stroke:currentColor;stroke-width:1.9;stroke-linecap:round;stroke-linejoin:round"><use href="/assets/icons.svg#i-support"></use></svg></span><span class="floating-customer-service-text">联系客服</span>';
    link.style.setProperty('background', '#07c160', 'important');
    link.style.setProperty('color', '#fff', 'important');
    link.style.setProperty('border-color', 'rgba(7,193,96,.62)', 'important');
    link.style.setProperty('box-shadow', '0 14px 34px rgba(0,0,0,.28), 0 8px 24px rgba(7,193,96,.20)', 'important');
    const icon = link.querySelector('.floating-customer-service-icon');
    if (icon) {
      icon.style.setProperty('background', '#05984b', 'important');
      icon.style.setProperty('color', '#fff', 'important');
    }
    document.body.appendChild(link);
  }

  function currentNavLinks(header) {
    const nav = header.querySelector('.nav');
    if (!nav) return FALLBACK_LINKS;
    const links = [...nav.querySelectorAll('a')]
      .filter((a) => !a.classList.contains('btn'))
      .filter((a) => a.hidden !== true && a.getAttribute('aria-hidden') !== 'true')
      .map((a) => [a.getAttribute('href') || '/', (a.textContent || '').trim()])
      .filter((pair) => pair[0] && pair[1]);
    return links.length ? links : FALLBACK_LINKS;
  }

  function ensureMobileNavigation() {
    const header = document.querySelector('.site-header');
    if (!header) return;

    let toggle = header.querySelector('.nav-toggle');
    if (!toggle) {
      toggle = document.createElement('button');
      toggle.type = 'button';
      toggle.className = 'nav-toggle';
      toggle.setAttribute('aria-label', '打开导航菜单');
      for (let i = 0; i < 3; i += 1) toggle.append(document.createElement('span'));
      (header.querySelector('.topbar') || header).append(toggle);
    }
    toggle.setAttribute('aria-expanded', 'false');
    toggle.setAttribute('aria-controls', 'mobileNavDrawer');

    let drawer = header.querySelector('#mobileNavDrawer');
    if (!drawer) {
      drawer = document.createElement('div');
      drawer.className = 'nav-drawer';
      drawer.id = 'mobileNavDrawer';
      header.append(drawer);
    }
    drawer.hidden = true;

    const renderLinks = () => {
      const list = document.createElement('div');
      list.className = 'nav-drawer-links';
      currentNavLinks(header).forEach(([href, label]) => {
        const item = document.createElement('a');
        item.href = href;
        item.textContent = label;
        list.append(item);
      });
      const quote = document.createElement('a');
      quote.className = 'btn primary nav-drawer-cta';
      quote.href = '/quote';
      quote.textContent = '提交询价';
      list.append(quote);
      drawer.replaceChildren(list);
      document.dispatchEvent(new CustomEvent('xh:mobile-nav-rendered', { detail: { drawer } }));
    };

    const setOpen = (open) => {
      if (open) renderLinks();
      drawer.hidden = !open;
      toggle.classList.toggle('is-open', open);
      toggle.setAttribute('aria-expanded', String(open));
      toggle.setAttribute('aria-label', open ? '关闭导航菜单' : '打开导航菜单');
    };

    renderLinks();
    if (toggle.dataset.navReady === '1') return;
    toggle.dataset.navReady = '1';
    toggle.addEventListener('click', () => setOpen(drawer.hidden));
    drawer.addEventListener('click', (event) => {
      if (event.target instanceof Element && event.target.closest('a')) setOpen(false);
    });
    document.addEventListener('keydown', (event) => {
      if (event.key === 'Escape') setOpen(false);
    });
    const wide = window.matchMedia('(min-width: 761px)');
    const onChange = (event) => { if (event.matches) setOpen(false); };
    if (wide.addEventListener) wide.addEventListener('change', onChange);
    else if (wide.addListener) wide.addListener(onChange);
  }

  function markOverflow(element) {
    const more = element.scrollWidth - element.clientWidth;
    element.classList.toggle('x-overflow', more > 4);
    element.classList.toggle('x-scrolled', more > 4 && element.scrollLeft > 2);
  }

  function bindOverflowHints() {
    document.querySelectorAll(SCROLLERS).forEach((element) => {
      if (element.dataset.xHint === '1') {
        markOverflow(element);
        return;
      }
      element.dataset.xHint = '1';
      element.addEventListener('scroll', () => markOverflow(element), { passive: true });
      markOverflow(element);
    });
  }

  function hydrateAccountEntry() {
    const entry = document.querySelector('[data-account-entry]');
    if (!entry || entry.dataset.accountHydrated === '1') return;
    entry.dataset.accountHydrated = '1';
    fetch('/api/account/me', {
      credentials: 'same-origin',
      cache: 'no-store',
      headers: { Accept: 'application/json' }
    })
      .then((response) => (response.ok ? response.json() : null))
      .then((me) => {
        if (!me) return;
        entry.href = '/account';
        entry.classList.add('account-entry-authed');
        entry.textContent = '';
        const avatar = document.createElement('span');
        avatar.className = 'account-entry-avatar';
        if (me.avatarUrl) {
          avatar.style.backgroundImage = `url("${String(me.avatarUrl).replace(/"/g, '%22')}")`;
        } else {
          avatar.textContent = String(me.displayName || '星').trim().slice(0, 1) || '星';
        }
        const name = document.createElement('span');
        name.className = 'account-entry-name';
        name.textContent = me.displayName || me.publicUserId || '个人中心';
        entry.append(avatar, name);
      })
      .catch(() => {});
  }

  function evaluateAnnouncementMode(bar) {
    const first = bar?.querySelector('.announcement-ticker span:not([aria-hidden="true"])');
    if (!bar || !first) return;
    const shouldScroll = first.getBoundingClientRect().width > Math.max(320, bar.clientWidth * 0.72);
    bar.classList.toggle('announcement-static', !shouldScroll);
  }

  function enhanceAnnouncement() {
    const bar = getBar();
    if (!bar || bar.classList.contains('hidden')) return;
    if (bar.querySelector('.announcement-ticker')) {
      evaluateAnnouncementMode(bar);
      return;
    }

    const text = String(bar.textContent || '').trim();
    if (!text) return;
    bar.textContent = '';
    bar.setAttribute('aria-label', text);

    const track = document.createElement('div');
    track.className = 'announcement-ticker';
    const primary = document.createElement('span');
    primary.textContent = text;
    const duplicate = document.createElement('span');
    duplicate.textContent = text;
    duplicate.setAttribute('aria-hidden', 'true');
    track.append(primary, duplicate);
    bar.append(track);
    requestAnimationFrame(() => evaluateAnnouncementMode(bar));
  }

  function scheduleAnnouncementEnhance() {
    cancelAnimationFrame(announcementFrame);
    announcementFrame = requestAnimationFrame(enhanceAnnouncement);
  }

  function startAnnouncement() {
    const bar = getBar();
    if (!bar) return;
    enhanceAnnouncement();
    const observer = new MutationObserver(scheduleAnnouncementEnhance);
    observer.observe(bar, {
      childList: true,
      subtree: true,
      characterData: true,
      attributes: true,
      attributeFilter: ['class']
    });
    window.addEventListener('resize', scheduleAnnouncementEnhance, { passive: true });
  }

  function start() {
    ensureBrandAssets();
    ensureFloatingSupport();
    ensureMobileNavigation();
    hydrateAccountEntry();
    bindOverflowHints();
    startAnnouncement();

    window.addEventListener('resize', bindOverflowHints, { passive: true });
    window.addEventListener('load', bindOverflowHints, { once: true });
    window.setTimeout(bindOverflowHints, 800);
  }

  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', start, { once: true });
  else start();
})();
