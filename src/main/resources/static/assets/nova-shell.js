(() => {
  'use strict';
  const ready = (fn) => document.readyState === 'loading' ? document.addEventListener('DOMContentLoaded', fn, { once: true }) : fn();
  const normalize = (value) => {
    const path = String(value || '/').split('?')[0].replace(/\/+$/, '') || '/';
    if (path === '/') return '/';
    if (path.startsWith('/service/') || path === '/services' || path === '/mastery') return '/services';
    if (path.startsWith('/account')) return '/account';
    if (path.startsWith('/records') || path.startsWith('/record/') || path.startsWith('/order')) return '/records';
    if (path.startsWith('/quote')) return '/quote';
    return path;
  };
  const dockMarkup = () => [
    ['/', '首页'], ['/services', '服务'], ['/quote', '询价'], ['/records', '进度'], ['/account', '我的']
  ].map(([href, label]) => `<a href="${href}" data-nv-route class="${href === '/quote' ? 'nv-dock-primary' : ''}">${label}</a>`).join('');

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
      drawer.addEventListener('click', (event) => { if (event.target.closest('a')) closeDrawer(); });
      document.addEventListener('keydown', (event) => { if (event.key === 'Escape') closeDrawer(); });
      document.addEventListener('pointerdown', (event) => {
        if (!drawer.classList.contains('is-open')) return;
        if (!drawer.contains(event.target) && !toggle.contains(event.target)) closeDrawer();
      });
    }

    const current = normalize(location.pathname);
    const applyActive = (root = document) => root.querySelectorAll('[data-nv-route]').forEach((link) => {
      const active = normalize(link.getAttribute('href')) === current;
      link.classList.toggle('is-active', active);
      if (active) link.setAttribute('aria-current', 'page'); else link.removeAttribute('aria-current');
    });
    applyActive();

    const announcement = document.getElementById('announcementBar');
    if (announcement) announcement.classList.add('nv-announcement');

    document.querySelectorAll('.mobile-dock:not(.nv-mobile-dock)').forEach((dock) => {
      dock.className = 'nv-mobile-dock';
      dock.innerHTML = dockMarkup();
      applyActive(dock);
    });

    const gameSection = document.getElementById('gameEntrySection');
    const gameRail = document.getElementById('homeGameRail');
    if (gameSection && gameRail) {
      const syncGames = () => {
        const visible = gameRail.children.length > 0;
        gameSection.hidden = !visible;
        gameSection.classList.toggle('hidden', !visible);
        if (visible) gameRail.classList.add('home-game-rail');
      };
      syncGames();
      new MutationObserver(syncGames).observe(gameRail, { childList: true });
    }

    const visual = document.getElementById('heroVisual');
    if (visual) {
      const syncVisual = () => {
        const image = visual.querySelector('img');
        visual.classList.toggle('has-image', !!image);
      };
      syncVisual();
      new MutationObserver(syncVisual).observe(visual, { childList: true });
    }
  });
})();
