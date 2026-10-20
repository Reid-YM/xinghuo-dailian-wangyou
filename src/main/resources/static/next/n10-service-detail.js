(() => {
  'use strict';
  const runtimePath = pathname => window.XHFrontendShell?.runtimePath?.(pathname) || pathname || location.pathname;
  const runtimeHref = value => window.XHFrontendShell?.runtimeHref?.(value) || value;

  const STAGE = 'N10 · SERVICE DETAIL PREVIEW';
  const DETAIL_RE = /^\/next\/service\/([^/?#]+)\/?$/;
  const $ = (selector, root = document) => root.querySelector(selector);
  const $$ = (selector, root = document) => [...root.querySelectorAll(selector)];
  const text = value => String(value ?? '').trim();
  const code = value => text(value).toUpperCase();
  const list = value => Array.isArray(value) ? value : [];
  let routeRequest = 0;

  function node(tag, className, value) {
    const element = document.createElement(tag);
    if (className) element.className = className;
    if (value !== undefined && value !== null) element.textContent = String(value);
    return element;
  }

  function safeImage(value) {
    const url = text(value);
    if ((url.startsWith('/') && !url.startsWith('//')) || url.startsWith('https://')) return url;
    return '';
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
    return '联系客服询价';
  }

  function installStyles() {
    if ($('#frontend-next-n10-style')) return;
    const style = document.createElement('style');
    style.id = 'frontend-next-n10-style';
    style.textContent = `
      .n10-detail{min-height:100vh;padding:112px 0 84px;background:radial-gradient(circle at 82% 12%,#ffcb2d16,transparent 28rem)}
      .n10-breadcrumb{display:flex;gap:8px;align-items:center;color:#8f97a0;font-size:11px;margin-bottom:18px}.n10-breadcrumb a{color:#d9bc65}.n10-breadcrumb span{opacity:.55}
      .n10-grid{display:grid;grid-template-columns:minmax(0,1.1fr) minmax(300px,.9fr);gap:18px;align-items:start}
      .n10-card{border:1px solid #ffffff18;border-radius:18px;background:#0d1116;padding:22px;box-shadow:0 26px 70px #0007}
      .n10-cover{min-height:260px;border:1px solid #ffffff12;border-radius:15px;overflow:hidden;background:radial-gradient(circle at 72% 30%,#ffcb2d2d,transparent 28%),linear-gradient(145deg,#211b12,#0b0e12);margin-bottom:20px;display:grid;place-items:center}
      .n10-cover img{display:block;width:100%;height:100%;min-height:260px;max-height:430px;object-fit:var(--service-cover-fit,cover);object-position:var(--service-cover-position,50% 50%)}
      .n10-kicker{color:#d9bb61;font-size:10px;font-weight:900;letter-spacing:.16em;text-transform:uppercase}.n10-title{font-size:clamp(34px,5vw,58px);line-height:1.02;letter-spacing:-.045em;margin:13px 0}.n10-desc{color:#a9b0b8;line-height:1.75;max-width:760px}
      .n10-facts{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:9px;margin-top:22px}.n10-fact{padding:13px;border:1px solid #ffffff12;border-radius:12px;background:#ffffff05}.n10-fact small{display:block;color:#818994;font-size:10px;margin-bottom:5px}.n10-fact strong{font-size:13px}
      .n10-price{font-size:clamp(30px,4vw,48px);font-weight:900;color:#ffcb2d;margin:10px 0 4px}.n10-note{color:#89919b;font-size:11px;line-height:1.7;margin:0 0 16px}
      .n10-actions{display:grid;gap:9px}.n10-actions .btn{text-align:center;padding:13px 16px}.n10-actions .btn.gold{font-size:14px}
      .n10-safe{margin-top:16px;padding:13px;border:1px solid #ffffff12;border-radius:12px;color:#8f97a0;font-size:11px;line-height:1.7;background:#090d11}
      .n10-loading{min-height:300px;display:grid;place-items:center;text-align:center;color:#9aa2ac}.n10-error h1{font-size:36px;margin:10px 0}.n10-error p{color:#9aa2ac;line-height:1.7}.n10-error-actions{display:flex;gap:9px;flex-wrap:wrap;margin-top:16px}
      @media(max-width:860px){.n10-detail{padding-top:92px}.n10-grid{grid-template-columns:1fr}.n10-cover{min-height:210px}.n10-cover img{min-height:210px}.n10-actions{grid-template-columns:1fr 1fr}}
      @media(max-width:560px){.n10-facts{grid-template-columns:1fr}.n10-actions{grid-template-columns:1fr}.n10-card{padding:16px}.n10-title{font-size:38px}.n10-error-actions{display:grid}}
    `;
    document.head.append(style);
  }

  function detailRoute() {
    const match = runtimePath().match(DETAIL_RE);
    if (!match) return null;
    try { return decodeURIComponent(match[1]); } catch (_) { return match[1]; }
  }

  function rewriteServiceLink(link) {
    if (!(link instanceof HTMLAnchorElement) || link.dataset.n10Legacy === '1') return;
    const raw = link.getAttribute('href') || '';
    if (!raw.startsWith('/service/') || raw.startsWith('/next/service/')) return;
    link.setAttribute('href', runtimeHref('/next' + raw));
  }

  function rewriteServiceLinks(root = document) {
    if (!runtimePath().startsWith('/next')) return;
    if (root instanceof HTMLAnchorElement) rewriteServiceLink(root);
    if (root.querySelectorAll) root.querySelectorAll('a[href^="/service/"]').forEach(rewriteServiceLink);
  }

  function watchServiceLinks() {
    rewriteServiceLinks();
    const observer = new MutationObserver(records => {
      records.forEach(record => record.addedNodes.forEach(added => {
        if (added instanceof Element) rewriteServiceLinks(added);
      }));
    });
    observer.observe(document.body, { childList: true, subtree: true });
    return observer;
  }

  function activateDetailPage(section) {
    $$('.page').forEach(page => page.classList.toggle('active', page === section));
    $$('.navlink[data-go]').forEach(link => {
      const active = link.dataset.go === 'services';
      link.classList.toggle('active', active);
      if (active) link.setAttribute('aria-current', 'page'); else link.removeAttribute('aria-current');
    });
    document.documentElement.dataset.frontendPage = 'service-detail';
    document.documentElement.dataset.frontendNextStage = 'n10-service-detail';
  }

  function ensureDetailSection() {
    let section = $('#n10ServiceDetail');
    if (section) return section;
    section = node('section', 'page n10-detail');
    section.id = 'n10ServiceDetail';
    section.setAttribute('role', 'main');
    section.tabIndex = -1;
    section.dataset.page = 'service-detail';
    section.append(node('div', 'shell'));
    const footer = $('.footer');
    if (footer) footer.before(section); else document.body.append(section);
    return section;
  }

  function renderLoading(section, slug) {
    const shell = $('.shell', section);
    if (!shell) return;
    const loading = node('div', 'n10-card n10-loading', `正在读取 ${slug || '服务'} 详情…`);
    loading.id = 'n10DetailState';
    loading.setAttribute('role', 'status');
    loading.setAttribute('aria-live', 'polite');
    shell.replaceChildren(loading);
  }

  function gameName(catalog, gameCode) {
    const row = list(catalog?.games).find(item => code(item?.code) === code(gameCode));
    return text(row?.name) || text(gameCode) || '游戏服务';
  }

  function categoryName(catalog, categoryCode, gameCode) {
    const rows = list(catalog?.categories);
    const row = rows.find(item => code(item?.code) === code(categoryCode) && code(item?.gameCode) === code(gameCode))
      || rows.find(item => code(item?.code) === code(categoryCode));
    return text(row?.name) || text(categoryCode) || '服务项目';
  }

  function setMetadata(service, game) {
    const brand = text(window.XHFrontendShell?.configuration?.brandName) || '星火网游';
    document.title = `${text(service?.name) || '服务详情'} · ${game} · ${brand}`;
    let description = $('meta[name="description"]');
    if (!description) {
      description = document.createElement('meta');
      description.name = 'description';
      document.head.append(description);
    }
    description.content = text(service?.shortDesc) || `${game} ${text(service?.name)} 服务详情与询价入口`;
    let canonical = $('link[rel="canonical"]');
    if (!canonical) {
      canonical = document.createElement('link');
      canonical.rel = 'canonical';
      document.head.append(canonical);
    }
    canonical.href = `https://wydlgf.cn/service/${encodeURIComponent(text(service?.slug))}`;
    let robots = $('meta[name="robots"]');
    if (!robots) {
      robots = document.createElement('meta');
      robots.name = 'robots';
      document.head.append(robots);
    }
    if (window.XHFrontendShell?.isPreview === false) robots.remove();
    else robots.content = 'noindex,follow';
  }

  function fact(label, value) {
    const box = node('div', 'n10-fact');
    box.append(node('small', '', label), node('strong', '', value || '以客服确认为准'));
    return box;
  }

  function renderDetail(section, service, catalog) {
    const shell = $('.shell', section);
    if (!shell) return;
    const game = gameName(catalog, service?.gameCode);
    const category = categoryName(catalog, service?.category, service?.gameCode);
    const slug = text(service?.slug);
    setMetadata(service, game);

    const breadcrumb = node('nav', 'n10-breadcrumb');
    breadcrumb.setAttribute('aria-label', '面包屑');
    const services = node('a', '', '服务大厅');
    services.href = '/next/services';
    breadcrumb.append(services, node('span', '', '›'), node('span', '', game), node('span', '', '›'), node('span', '', text(service?.name) || '服务详情'));

    const grid = node('div', 'n10-grid');
    const main = node('article', 'n10-card');
    const cover = node('div', 'n10-cover');
    const imageUrl = safeImage(service?.coverImage);
    if (imageUrl) {
      const image = document.createElement('img');
      image.src = imageUrl;
      image.alt = `${text(service?.name) || '服务'}封面`;
      image.loading = 'eager';
      cover.append(image);
    } else {
      cover.append(node('span', 'n10-kicker', `${game} · ${category}`));
    }
    main.append(cover, node('div', 'n10-kicker', `${game} · ${category}`), node('h1', 'n10-title', text(service?.name) || '服务详情'));
    main.append(node('p', 'n10-desc', text(service?.shortDesc) || '该服务的具体目标、当前进度与工时会在询价时进一步核对。'));
    const facts = node('div', 'n10-facts');
    facts.append(
      fact('所属游戏', game),
      fact('服务分类', category),
      fact('预计时间', text(service?.durationText)),
      fact('价格方式', code(service?.priceMode) === 'QUOTE' ? '人工询价' : code(service?.priceMode) === 'RANGE' ? '参考区间' : '参考价格')
    );
    main.append(facts);

    const aside = node('aside', 'n10-card');
    aside.append(node('div', 'n10-kicker', 'Reference Price'), node('div', 'n10-price', price(service)));
    aside.append(node('p', 'n10-note', '页面金额仅作为公开目录参考；具体目标、已有进度、平台和时间要求需要在询价后确认，最终以客服确认结果为准。'));
    const actions = node('div', 'n10-actions');
    const quote = node('a', 'btn gold', '带入此服务询价 →');
    quote.href = `/next/quote?service=${encodeURIComponent(slug)}`;
    quote.dataset.n10Quote = slug;
    const back = node('a', 'btn', '返回服务大厅');
    back.href = '/next/services';
    actions.append(quote, back);
    aside.append(actions);
    const safe = node('div', 'n10-safe');
    safe.append(document.createTextNode('也可打开 '));
    const legacy = node('a', '', '重新打开服务详情');
    legacy.href = `/service/${encodeURIComponent(slug)}`;
    legacy.dataset.n10Legacy = '1';
    safe.append(legacy, document.createTextNode('。'));
    aside.append(safe);

    grid.append(main, aside);
    shell.replaceChildren(breadcrumb, grid);
  }

  function renderError(section, message) {
    const shell = $('.shell', section);
    if (!shell) return;
    const card = node('div', 'n10-card n10-error');
    card.append(node('div', 'n10-kicker', 'Service Detail'), node('h1', '', '暂时无法显示此服务'), node('p', '', message));
    const actions = node('div', 'n10-error-actions');
    const retry = node('button', 'btn gold', '重新读取');
    retry.type = 'button';
    retry.dataset.n10Retry = '1';
    const back = node('a', 'btn', '返回新版服务大厅');
    back.href = '/next/services';
    actions.append(retry, back);
    card.append(actions);
    shell.replaceChildren(card);
  }

  async function getJson(path) {
    const response = await fetch(path, { headers: { Accept: 'application/json' }, credentials: 'same-origin', cache: 'no-store' });
    if (!response.ok) throw new Error(`HTTP ${response.status}`);
    return response.json();
  }

  async function loadDetail(slug) {
    const state = window.XHFrontendNextN2?.state;
    let services = Array.isArray(state?.services) ? state.services : null;
    let catalog = state?.catalog || null;
    if (!services || !catalog) {
      const [servicesResult, catalogResult] = await Promise.allSettled([
        services ? Promise.resolve(services) : getJson('/api/services'),
        catalog ? Promise.resolve(catalog) : getJson('/api/catalog')
      ]);
      if (servicesResult.status === 'fulfilled' && Array.isArray(servicesResult.value)) services = servicesResult.value;
      if (catalogResult.status === 'fulfilled') catalog = catalogResult.value;
    }
    if (!Array.isArray(services)) throw new Error('服务目录读取失败，请返回服务大厅后重试。');
    const service = services.find(item => text(item?.slug) === slug);
    if (!service) throw new Error('没有找到对应的公开服务，可能已下架或链接已更新。');
    return { service, catalog: catalog || {} };
  }

  async function syncRoute() {
    const request = ++routeRequest;
    const slug = detailRoute();
    if (!slug) return;
    installStyles();
    const section = ensureDetailSection();
    activateDetailPage(section);
    renderLoading(section, slug);
    try {
      const detail = await loadDetail(slug);
      if (request !== routeRequest || detailRoute() !== slug) return;
      renderDetail(section, detail.service, detail.catalog);
    } catch (error) {
      if (request !== routeRequest || detailRoute() !== slug) return;
      renderError(section, error?.message || '服务详情读取失败，请稍后重试。');
    }
  }

  function bindActions() {
    document.addEventListener('click', event => {
      const target = event.target instanceof Element ? event.target : null;
      const quote = target?.closest('[data-n10-quote]');
      if (quote) window.XHFrontendShell?.track?.('service_card_click', quote.dataset.n10Quote || null);
      const retry = target?.closest('[data-n10-retry]');
      if (retry) syncRoute();
    });
  }

  function boot() {
    watchServiceLinks();
    bindActions();
    syncRoute();
    window.addEventListener('popstate', syncRoute);
  }

  window.XHFrontendNextN10 = {
    stage: STAGE,
    rewriteServiceLinks,
    syncRoute,
    detailRoute
  };

  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', boot, { once: true });
  else boot();
})();
