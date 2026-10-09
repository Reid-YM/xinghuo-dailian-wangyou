(() => {
  'use strict';
  const ready = (fn) => document.readyState === 'loading' ? document.addEventListener('DOMContentLoaded', fn, { once: true }) : fn();
  const normalize = (value) => {
    const path = String(value || '/').split('?')[0].replace(/\/+$/, '') || '/';
    if (path === '/') return '/';
    if (path.startsWith('/service/') || path === '/services' || path === '/mastery') return '/services';
    if (path.startsWith('/account')) return '/account';
    if (path.startsWith('/records') || path.startsWith('/order')) return '/records';
    if (path.startsWith('/quote')) return '/quote';
    return path;
  };

  ready(() => {
    document.documentElement.dataset.publicUi = 'nova';

    const toggle = document.querySelector('[data-nv-menu-toggle]');
    const drawer = document.querySelector('[data-nv-drawer]');
    const closeDrawer = () => {
      if (!drawer || !toggle) return;
      drawer.classList.remove('is-open');
      toggle.setAttribute('aria-expanded', 'false');
    };
    if (toggle && drawer) {
      toggle.addEventListener('click', () => {
        const open = !drawer.classList.contains('is-open');
        drawer.classList.toggle('is-open', open);
        toggle.setAttribute('aria-expanded', open ? 'true' : 'false');
      });
      drawer.addEventListener('click', (event) => {
        if (event.target.closest('a')) closeDrawer();
      });
      document.addEventListener('keydown', (event) => {
        if (event.key === 'Escape') closeDrawer();
      });
      document.addEventListener('pointerdown', (event) => {
        if (!drawer.classList.contains('is-open')) return;
        if (!drawer.contains(event.target) && !toggle.contains(event.target)) closeDrawer();
      });
    }

    const current = normalize(location.pathname);
    document.querySelectorAll('[data-nv-route]').forEach((link) => {
      const target = normalize(link.getAttribute('href'));
      const active = target === current;
      link.classList.toggle('is-active', active);
      if (active) link.setAttribute('aria-current', 'page');
      else link.removeAttribute('aria-current');
    });

    const announcement = document.getElementById('announcementBar');
    if (announcement) announcement.classList.add('nv-announcement');

    document.querySelectorAll('.mobile-dock:not(.nv-mobile-dock)').forEach((dock) => {
      dock.classList.add('nv-mobile-dock');
      const links = [...dock.querySelectorAll('a')];
      links.forEach((link) => {
        link.dataset.nvRoute = '';
        if (normalize(link.getAttribute('href')) === current) link.classList.add('is-active');
      });
      if (links.length === 3) {
        const model = [
          ['/', '首页'],
          ['/services', '服务'],
          ['/quote', '询价'],
          ['/records', '进度'],
          ['/account', '我的']
        ];
        dock.innerHTML = model.map(([href, label]) => `<a href="${href}" data-nv-route class="${href === '/quote' ? 'nv-dock-primary' : ''}">${label}</a>`).join('');
        dock.querySelectorAll('[data-nv-route]').forEach((link) => {
          if (normalize(link.getAttribute('href')) === current) link.classList.add('is-active');
        });
      }
    });

    const artwork = document.querySelector('[data-nv-hero-art]');
    if (artwork && !artwork.style.backgroundImage) {
      const source = document.querySelector('#heroVisual img, #heroBackground');
      if (source?.tagName === 'IMG' && source.currentSrc) artwork.style.backgroundImage = `url("${source.currentSrc}")`;
    }
  });
})();
