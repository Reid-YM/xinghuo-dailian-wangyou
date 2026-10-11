/**
 * Transitional page-level compatibility only.
 *
 * Global shell responsibilities have moved to site-shell.js. Keep this file small and temporary:
 * - contact page normalizes legacy channel labels and QR placement after public.js renders config;
 * - home page renders the configured hot-service list inside the hero media panel.
 *
 * Do not add global navigation, account, polling, MutationObserver or tool-station patches here.
 */
(() => {
  'use strict';

  function normalizeContactPage() {
    if (document.body?.dataset?.page !== 'contact') return;
    const neutralLabels = { '主客服': '微信', '主账号': '微信', '默认客服': '微信' };
    document.querySelectorAll('#contactChannelGrid .tag').forEach((tag) => {
      const current = (tag.textContent || '').trim();
      if (neutralLabels[current]) tag.textContent = neutralLabels[current];
    });

    const qr = document.getElementById('wechatQr');
    const consoleBox = document.querySelector('.contact-console');
    if (!qr || !consoleBox) return;
    const notice = consoleBox.querySelector('.contact-safety');
    if (notice && qr.nextElementSibling !== notice) consoleBox.insertBefore(qr, notice);
    else if (!notice && qr.parentElement !== consoleBox) consoleBox.appendChild(qr);
  }

  function renderHomeRecommendations() {
    if (document.body?.dataset?.page !== 'home') return;
    const copy = document.querySelector('.hero-media-panel .hero-media-copy');
    if (!copy) return;

    const priceText = (service) => {
      if (!service) return '';
      if (service.priceMode === 'fixed' && service.priceFixed != null) return `¥${service.priceFixed}`;
      if (service.priceMin != null && service.priceMax != null) return `¥${service.priceMin}–${service.priceMax}`;
      if (service.priceMin != null) return `¥${service.priceMin} 起`;
      return '按需求报价';
    };

    fetch('/api/site-config', { cache: 'no-store' })
      .then((response) => (response.ok ? response.json() : null))
      .then((config) => {
        const hot = Array.isArray(config?.hotServices) ? config.hotServices.slice(0, 2) : [];
        if (!hot.length) {
          copy.remove();
          return;
        }

        const list = document.createElement('div');
        list.className = 'hero-hot-list';
        hot.forEach((service) => {
          const link = document.createElement('a');
          link.className = 'hero-hot-item';
          link.href = `/service/${encodeURIComponent(service.slug || '')}`;

          const name = document.createElement('b');
          name.textContent = service.name || '热门任务';
          link.appendChild(name);

          if (service.shortDesc) {
            const summary = document.createElement('small');
            summary.textContent = service.shortDesc;
            link.appendChild(summary);
          }

          const price = document.createElement('em');
          price.textContent = priceText(service);
          link.appendChild(price);
          list.appendChild(link);
        });

        copy.textContent = '';
        const eyebrow = document.createElement('span');
        eyebrow.className = 'eyebrow';
        eyebrow.textContent = '当前推荐';
        copy.append(eyebrow, list);
      })
      .catch(() => {});
  }

  renderHomeRecommendations();
  normalizeContactPage();
  window.addEventListener('load', normalizeContactPage, { once: true });
  window.setTimeout(normalizeContactPage, 1200);
})();
