(() => {
  'use strict';
  const runtimePath = pathname => window.XHFrontendShell?.runtimePath?.(pathname) || pathname || location.pathname;
  const pattern = /^\/next\/game\/([^/]+)\/?$/;
  if (!pattern.test(runtimePath())) return;
  const text = value => String(value ?? '').trim();
  const list = value => Array.isArray(value) ? value : [];
  const code = value => text(value).toUpperCase();
  const node = (tag, className, value) => {
    const element = document.createElement(tag);
    if (className) element.className = className;
    if (value != null) element.textContent = String(value);
    return element;
  };
  const link = (label, href, className = 'btn') => {
    const element = node('a', className, label); element.href = href; return element;
  };
  const safeImage = value => {
    const url = text(value);
    return (url.startsWith('/') && !url.startsWith('//')) || url.startsWith('https://') ? url : '';
  };
  let generation = 0;
  function section() {
    let root = document.getElementById('nextGameDetail');
    if (!root) {
      root = node('section', 'page next-game-detail'); root.id = 'nextGameDetail';
      root.dataset.page = 'game-detail'; root.setAttribute('role', 'main'); root.tabIndex = -1;
      root.append(node('div', 'shell'));
      (document.querySelector('.next-footer') || document.querySelector('.footer'))?.before(root);
      if (!root.isConnected) document.body.append(root);
    }
    document.querySelectorAll('.page').forEach(page => page.classList.toggle('active', page === root));
    document.querySelectorAll('.navlink[data-go]').forEach(item => {
      const active = item.dataset.go === 'services'; item.classList.toggle('active', active);
      if (active) item.setAttribute('aria-current', 'page'); else item.removeAttribute('aria-current');
    });
    document.documentElement.dataset.frontendPage = 'game-detail';
    return root;
  }
  function styles() {
    if (document.getElementById('next-game-detail-style')) return;
    const style = node('style'); style.id = 'next-game-detail-style';
    style.textContent = `
      .next-game-detail{min-height:100vh;padding:112px 0 84px;overflow-wrap:anywhere}
      .next-game-breadcrumb,.next-game-actions{display:flex;gap:12px;flex-wrap:wrap;align-items:center;margin-bottom:20px}
      .next-game-hero{display:grid;grid-template-columns:minmax(0,1fr) minmax(0,1fr);gap:24px;align-items:center;padding:24px;border:1px solid var(--line);border-radius:18px;background:var(--p)}
      .next-game-hero h1{font-size:clamp(32px,5vw,60px);line-height:1.15;margin:12px 0}.next-game-hero p{color:var(--muted);line-height:1.8;white-space:pre-wrap}.next-game-eyebrow{color:var(--gold)}
      .next-game-cover{display:block;width:100%;aspect-ratio:16/9;object-fit:cover;border-radius:12px}.next-game-logo{width:64px;height:64px;object-fit:contain;border-radius:12px}
      .next-game-facts{display:flex;gap:10px;flex-wrap:wrap;margin:18px 0}.next-game-facts span{padding:8px 12px;border:1px solid var(--line);border-radius:10px;color:var(--muted)}
      .next-game-services{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:16px}.next-game-card{min-width:0;border:1px solid var(--line);border-radius:16px;background:var(--p);padding:18px;display:flex;flex-direction:column;gap:12px}.next-game-card h3,.next-game-card p{margin:0}.next-game-card p{color:var(--muted);line-height:1.7}.next-game-card .next-game-actions{margin:auto 0 0;padding-top:8px}.next-game-price{font-weight:bold;color:var(--gold)}
      .next-game-heading{margin:30px 0 18px}.next-game-status{color:var(--muted);line-height:1.8}.next-game-state{padding:28px;border:1px solid var(--line);border-radius:16px}
      @media(max-width:850px){.next-game-detail{padding-top:94px}.next-game-hero{grid-template-columns:1fr}.next-game-services{grid-template-columns:repeat(2,minmax(0,1fr))}}
      @media(max-width:560px){.next-game-hero{padding:16px}.next-game-services{grid-template-columns:1fr}}
    `;
    document.head.append(style);
  }
  function image(value, alt, className) {
    const url = safeImage(value); if (!url) return null;
    const element = node('img', className); element.src = url; element.alt = alt; element.loading = 'lazy';
    element.addEventListener('error', () => element.remove(), {once:true}); return element;
  }
  function price(service) {
    const amount = value => value != null && value !== '' && Number.isFinite(Number(value)) && Number(value) >= 0
      ? Number(value).toLocaleString('zh-CN', {maximumFractionDigits:2}) : '';
    const fixed = amount(service.priceFixed), min = amount(service.priceMin), max = amount(service.priceMax);
    if (code(service.priceMode) === 'FIXED' && fixed) return `¥${fixed}`;
    if (code(service.priceMode) === 'RANGE' && min && max) return `参考 ¥${min}–${max}`;
    if (min) return `参考 ¥${min} 起`;
    return '按进度报价';
  }
  function metadata(game) {
    const title = text(game.seoTitle) || `${text(game.name)} 任务服务 · ${text(window.XHFrontendShell?.configuration?.brandName) || '星火网游'}`;
    document.title = title;
    const set = (name, content) => {
      let meta = document.querySelector(`meta[name="${name}"]`);
      if (!meta) { meta = node('meta'); meta.name = name; document.head.append(meta); }
      meta.content = content;
    };
    set('description', text(game.seoDescription) || text(game.description) || `${text(game.name)} 游戏任务服务，查看参考价格与预计时间。`);
    if (window.XHFrontendShell?.isPreview === false) document.querySelector('meta[name=robots]')?.remove();
    else set('robots', 'noindex,follow');
    let canonical = document.querySelector('link[rel=canonical]');
    if (!canonical) { canonical = node('link'); canonical.rel = 'canonical'; document.head.append(canonical); }
    canonical.href = 'https://wydlgf.cn/game/' + encodeURIComponent(text(game.seoSlug));
  }
  function render(root, game, services, catalog) {
    metadata(game);
    const shell = root.querySelector('.shell'); shell.replaceChildren();
    const breadcrumb = node('nav', 'next-game-breadcrumb'); breadcrumb.setAttribute('aria-label', '面包屑');
    breadcrumb.append(link('首页', '/next'), node('span', '', '/'), link('服务大厅', '/next/services'), node('span', '', '/'), node('span', '', game.name));
    shell.append(breadcrumb);
    const hero = node('div', 'next-game-hero'), intro = node('div');
    const logo = image(game.logoImage, '', 'next-game-logo'); if (logo) intro.append(logo);
    intro.append(node('div', 'next-game-eyebrow', text(game.nameEn) || '游戏服务'), node('h1', '', `${text(game.name)} 任务服务`));
    const description = text(game.description) || text(game.seoDescription) || '选择服务项目，查看参考价格与预计时间，提交需求后由客服按当前进度确认报价。';
    intro.append(node('p', '', description));
    const facts = node('div', 'next-game-facts');
    [text(game.publisher), text(game.genres), '网页不收密码', '业务编号可查'].filter(Boolean).forEach(value => facts.append(node('span', '', value)));
    const platforms = list(catalog.platforms).filter(platform => list(game.platformCodes).includes(platform.code)).map(platform => text(platform.name) || text(platform.code));
    if (platforms.length) facts.append(node('span', '', '平台：' + platforms.join(' / ')));
    intro.append(facts);
    const actions = node('div', 'next-game-actions');
    actions.append(link('按游戏筛选服务', '/next/services?game=' + encodeURIComponent(game.code)), link('提交需求', '/next/quote?game=' + encodeURIComponent(game.code), 'btn gold'));
    const legacy = link('打开旧版游戏资料', '/game/' + encodeURIComponent(game.seoSlug) + '?legacy=1'); legacy.dataset.n14Legacy = '1'; actions.append(legacy);
    intro.append(actions); hero.append(intro);
    const cover = image(game.coverImage, `${text(game.name)}封面`, 'next-game-cover'); if (cover) hero.append(cover);
    shell.append(hero, node('h2', 'next-game-heading', `${text(game.name)} 可接服务`));
    const grid = node('div', 'next-game-services');
    const accepting = window.XHFrontendShell?.configuration?.serviceStatus === 'open';
    const rows = list(services).filter(service => code(service.gameCode) === code(game.code));
    rows.forEach(service => {
      const card = node('article', 'next-game-card'); card.dataset.serviceSlug = text(service.slug);
      const cover = image(service.coverImage, text(service.name), 'next-game-cover'); if (cover) card.append(cover);
      card.append(node('h3', '', text(service.name) || '服务项目'), node('p', '', text(service.shortDesc)), node('div', 'next-game-price', price(service)));
      card.append(node('div', 'next-game-status', `${accepting && code(service.status) === 'AVAILABLE' ? '可接单' : code(service.status) === 'OFFLINE' ? '已下架' : '可预约'} · ${text(service.durationText) || '按进度确认时间'}`));
      const actions = node('div', 'next-game-actions');
      if (text(service.slug)) actions.append(link('查看详情', '/next/service/' + encodeURIComponent(service.slug)));
      if (code(service.status) !== 'OFFLINE') actions.append(link(accepting ? '询价' : '了解预约', '/next/quote?service=' + encodeURIComponent(service.slug), 'btn gold'));
      card.append(actions); grid.append(card);
    });
    if (!rows.length) grid.append(node('p', 'next-game-state', '当前暂无公开服务，可联系客服确认需求。'));
    shell.append(grid, node('p', 'next-game-status', '价格和预计时间仅供参考，最终以客服核对当前进度后的确认为准。'));
  }
  async function get(path) {
    const response = await fetch(path, {credentials:'same-origin',cache:'no-store',headers:{Accept:'application/json'}});
    if (!response.ok) throw new Error('读取失败，请稍后重试。');
    return response.json();
  }
  async function load() {
    if (!pattern.test(runtimePath())) return;
    const path = runtimePath(), current = ++generation; styles(); const root = section(), shell = root.querySelector('.shell');
    const loading = node('div', 'next-game-state', '正在读取游戏资料…'); loading.setAttribute('role', 'status'); shell.replaceChildren(loading);
    try {
      const [catalog, services] = await Promise.all([get('/api/catalog'), get('/api/services'), window.XHFrontendShell?.configurationReady]);
      if (current !== generation || path !== runtimePath()) return;
      const slug = decodeURIComponent(pattern.exec(path)[1]);
      const game = list(catalog.games).find(game => text(game.seoSlug) === slug && game.active !== false);
      if (!game) { shell.replaceChildren(node('h1', '', '游戏资料不存在或已停止公开'), link('返回服务大厅', '/next/services')); return; }
      render(root, game, services, catalog);
    } catch (_) {
      if (current !== generation || path !== runtimePath()) return;
      const state = node('div', 'next-game-state'); state.setAttribute('role', 'alert');
      state.append(node('h1', '', '游戏资料暂时无法读取'), node('p', '', '请检查网络后重试。'));
      const retry = node('button', 'btn gold', '重新读取'); retry.type = 'button'; retry.onclick = load;
      state.append(retry, link('返回服务大厅', '/next/services')); shell.replaceChildren(state);
    }
  }
  window.addEventListener('popstate', load);
  window.XHFrontendGame = {reload:load};
  load();
})();
