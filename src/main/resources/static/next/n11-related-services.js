(() => {
  'use strict';

  const STAGE = 'N11 · RELATED SERVICES CONTINUITY';
  const DETAIL_RE = /^\/next\/service\/([^/?#]+)\/?$/;
  const $ = (selector, root = document) => root.querySelector(selector);
  const text = value => String(value ?? '').trim();
  const code = value => text(value).toUpperCase();
  let requestVersion = 0;
  let scheduled = false;

  function node(tag, className, value) {
    const element = document.createElement(tag);
    if (className) element.className = className;
    if (value !== undefined && value !== null) element.textContent = String(value);
    return element;
  }

  function routeSlug() {
    const match = location.pathname.match(DETAIL_RE);
    if (!match) return null;
    try { return decodeURIComponent(match[1]); } catch (_) { return match[1]; }
  }

  function safeImage(value) {
    const url = text(value);
    return (url.startsWith('/') && !url.startsWith('//')) || url.startsWith('https://') ? url : '';
  }

  function money(value) {
    if (value == null || value === '' || !Number.isFinite(Number(value)) || Number(value) < 0) return null;
    return Number(value).toLocaleString('zh-CN', { maximumFractionDigits: 2 });
  }

  function price(service) {
    const mode = code(service?.priceMode);
    const fixed = money(service?.priceFixed);
    const min = money(service?.priceMin);
    const max = money(service?.priceMax);
    if (mode === 'FIXED' && fixed !== null) return `¥${fixed}`;
    if (mode === 'RANGE' && min !== null && max !== null && min !== max) return `¥${min}–${max}`;
    if (mode === 'RANGE' && min !== null) return `¥${min} 起`;
    if (mode === 'RANGE' && max !== null) return `¥${max} 内`;
    return '人工询价';
  }

  function installStyles() {
    if ($('#frontend-next-n11-style')) return;
    const style = document.createElement('style');
    style.id = 'frontend-next-n11-style';
    style.textContent = `
      .n11-related{margin-top:18px;border:1px solid #ffffff12;border-radius:18px;background:#0b0f14;padding:20px}
      .n11-head{display:flex;justify-content:space-between;align-items:end;gap:14px;margin-bottom:14px}.n11-head h2{margin:5px 0 0;font-size:23px;letter-spacing:-.03em}.n11-head p{margin:0;color:#818994;font-size:11px;line-height:1.6}
      .n11-grid{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:10px}.n11-card{display:grid;grid-template-rows:112px auto;border:1px solid #ffffff12;border-radius:14px;overflow:hidden;background:#0d1116;transition:.2s}.n11-card:hover{transform:translateY(-3px);border-color:#ffcb2d55}.n11-cover{background:radial-gradient(circle at 70% 30%,#ffcb2d28,transparent 36%),linear-gradient(145deg,#201a11,#0b0e12);overflow:hidden}.n11-cover img{width:100%;height:100%;object-fit:var(--service-cover-fit,cover);object-position:var(--service-cover-position,50% 50%);display:block}.n11-body{padding:13px}.n11-body h3{margin:0 0 6px;font-size:14px}.n11-body p{margin:0;color:#838b95;font-size:10px;line-height:1.55;min-height:31px}.n11-meta{display:flex;justify-content:space-between;gap:8px;align-items:center;margin-top:11px;padding-top:10px;border-top:1px solid #ffffff0d}.n11-price{color:#ffcb2d;font-weight:900;font-size:12px}.n11-time{color:#7f8791;font-size:9px;text-align:right}
      @media(max-width:760px){.n11-grid{grid-template-columns:1fr}.n11-card{grid-template-columns:110px 1fr;grid-template-rows:auto;min-height:110px}.n11-head{display:block}.n11-head p{margin-top:5px}}
      @media(prefers-reduced-motion:reduce){.n11-card{transition:none}.n11-card:hover{transform:none}}
    `;
    document.head.append(style);
  }

  async function publicServices() {
    const cached = window.XHFrontendNextN2?.state?.services;
    if (Array.isArray(cached)) return cached;
    const response = await fetch('/api/services', {
      headers: { Accept: 'application/json' },
      credentials: 'same-origin',
      cache: 'no-store'
    });
    if (!response.ok) throw new Error(`HTTP ${response.status}`);
    const payload = await response.json();
    if (!Array.isArray(payload)) throw new Error('服务目录格式无效');
    return payload;
  }

  function relatedFor(services, current) {
    const currentGame = code(current?.gameCode);
    return services
      .filter(item => text(item?.slug) && text(item?.slug) !== text(current?.slug) && code(item?.gameCode) === currentGame)
      .slice(0, 3);
  }

  function card(service) {
    const link = node('a', 'n11-card');
    link.href = `/next/service/${encodeURIComponent(text(service?.slug))}`;
    link.dataset.n11Service = text(service?.slug);

    const cover = node('div', 'n11-cover');
    const imageUrl = safeImage(service?.coverImage);
    if (imageUrl) {
      const image = document.createElement('img');
      image.src = imageUrl;
      image.alt = `${text(service?.name) || '相关服务'}封面`;
      image.loading = 'lazy';
      cover.append(image);
    }

    const body = node('div', 'n11-body');
    body.append(node('h3', '', text(service?.name) || '服务项目'));
    body.append(node('p', '', text(service?.shortDesc) || '查看该服务的公开详情与询价入口。'));
    const meta = node('div', 'n11-meta');
    meta.append(node('span', 'n11-price', price(service)), node('span', 'n11-time', text(service?.durationText) || '工时待确认'));
    body.append(meta);
    link.append(cover, body);
    return link;
  }

  function removeRelated() {
    $('#n10RelatedServices')?.remove();
  }

  function renderRelated(slug, services) {
    const host = $('#n10ServiceDetail .shell');
    const detailGrid = $('#n10ServiceDetail .n10-grid');
    if (!host || !detailGrid) return;
    const current = services.find(item => text(item?.slug) === slug);
    if (!current) {
      removeRelated();
      return;
    }
    const related = relatedFor(services, current);
    if (!related.length) {
      removeRelated();
      return;
    }

    let section = $('#n10RelatedServices');
    if (!section) {
      section = node('section', 'n11-related');
      section.id = 'n10RelatedServices';
      host.append(section);
    }
    section.dataset.slug = slug;
    const head = node('div', 'n11-head');
    const title = node('div');
    title.append(node('div', 'n10-kicker', 'Related Services'), node('h2', '', '同游戏的其他服务'));
    head.append(title, node('p', '', '继续比较时保持在 Frontend Next 预览链路，不跳回旧页面。'));
    const grid = node('div', 'n11-grid');
    related.forEach(service => grid.append(card(service)));
    section.replaceChildren(head, grid);
  }

  async function sync() {
    const slug = routeSlug();
    const detailGrid = $('#n10ServiceDetail .n10-grid');
    if (!slug || !detailGrid) {
      if (!slug) removeRelated();
      return;
    }
    if ($('#n10RelatedServices')?.dataset.slug === slug) return;

    const request = ++requestVersion;
    try {
      const services = await publicServices();
      if (request !== requestVersion || routeSlug() !== slug || !$('#n10ServiceDetail .n10-grid')) return;
      renderRelated(slug, services);
    } catch (_) {
      if (request === requestVersion && routeSlug() === slug) removeRelated();
    }
  }

  function schedule() {
    if (scheduled) return;
    scheduled = true;
    queueMicrotask(() => {
      scheduled = false;
      sync();
    });
  }

  function bindTracking() {
    document.addEventListener('click', event => {
      const target = event.target instanceof Element ? event.target.closest('[data-n11-service]') : null;
      if (!target) return;
      window.XHFrontendShell?.track?.('service_card_click', target.dataset.n11Service || null);
    });
  }

  function boot() {
    installStyles();
    bindTracking();
    new MutationObserver(schedule).observe(document.body, { childList: true, subtree: true });
    window.addEventListener('popstate', schedule);
    schedule();
  }

  window.XHFrontendNextN11 = {
    stage: STAGE,
    sync,
    relatedFor
  };

  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', boot, { once: true });
  else boot();
})();
