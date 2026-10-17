(() => {
  'use strict';

  const ROUTE = '/next/guides';
  const API = '/api/guides?limit=100';
  let loaded = false;
  let loading = false;
  let items = [];
  let requestSeq = 0;

  const text = (value) => String(value ?? '').trim();
  const lower = (value) => text(value).toLocaleLowerCase('zh-CN');

  function make(tag, value, className) {
    const node = document.createElement(tag);
    if (value !== undefined && value !== null) node.textContent = String(value);
    if (className) node.className = className;
    return node;
  }

  function safeImage(value) {
    const raw = text(value);
    if (!raw) return '';
    if (raw.startsWith('/') && !raw.startsWith('//')) return raw;
    try {
      const url = new URL(raw);
      if (url.protocol === 'https:' && !url.username && !url.password) return url.href;
    } catch (ignore) {}
    return '';
  }

  function currentState() {
    const params = new URLSearchParams(location.search);
    return {
      q: text(params.get('q')).slice(0, 80),
      game: text(params.get('game')).slice(0, 40).toUpperCase()
    };
  }

  function writeState(next, mode = 'push') {
    const url = new URL(location.href);
    url.pathname = ROUTE;
    url.search = '';
    if (text(next.q)) url.searchParams.set('q', text(next.q).slice(0, 80));
    if (text(next.game)) url.searchParams.set('game', text(next.game).slice(0, 40).toUpperCase());
    url.hash = '';
    const target = url.pathname + url.search;
    const current = location.pathname + location.search + location.hash;
    if (target === current) return false;
    const state = { ...(history.state || {}), n23: true, q: next.q || '', game: next.game || '' };
    if (mode === 'replace') history.replaceState(state, '', target);
    else history.pushState(state, '', target);
    return true;
  }

  function installStyles() {
    if (document.getElementById('frontend-next-n23-style')) return;
    const style = document.createElement('style');
    style.id = 'frontend-next-n23-style';
    style.textContent = `
      .n23-shell{padding:118px 0 72px;min-height:100vh}.n23-head{display:grid;grid-template-columns:1fr auto;gap:18px;align-items:end;margin-bottom:22px}
      .n23-head h1{font-size:clamp(42px,5vw,68px);margin:14px 0 8px}.n23-actions{display:flex;gap:8px;flex-wrap:wrap;justify-content:flex-end}
      .n23-toolbar{display:grid;grid-template-columns:minmax(0,1.4fr) minmax(180px,.6fr);gap:10px;padding:14px;border:1px solid var(--line);border-radius:14px;background:#0b0f13;margin-bottom:14px}
      .n23-status{min-height:20px;color:#7f8791;font-size:11px;margin:8px 2px 18px}.n23-grid{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:12px}
      .n23-card{display:flex;flex-direction:column;min-width:0;border:1px solid var(--line);border-radius:16px;background:#0d1116;overflow:hidden;transition:.22s}.n23-card:hover{transform:translateY(-4px);border-color:#ffcb2d55;box-shadow:0 22px 50px #0006}
      .n23-cover{aspect-ratio:16/9;background:linear-gradient(145deg,#1f1a12,#0b0e12);overflow:hidden}.n23-cover img{width:100%;height:100%;display:block;object-fit:cover}.n23-body{display:flex;flex-direction:column;gap:10px;padding:16px;min-height:220px}
      .n23-meta{display:flex;gap:6px;flex-wrap:wrap;align-items:center;color:#88909a;font-size:9px}.n23-pill{border:1px solid #ffffff18;border-radius:99px;padding:4px 7px}.n23-featured{color:#17130a;background:var(--gold);border-color:var(--gold);font-weight:800}
      .n23-card h2{font-size:20px;line-height:1.25;margin:0;letter-spacing:-.02em}.n23-card p{margin:0;color:#939ba5;font-size:11px;line-height:1.7}.n23-tags{display:flex;gap:5px;flex-wrap:wrap;margin-top:auto}.n23-tag{color:#aeb5bd;font-size:9px;border:1px solid #ffffff12;border-radius:7px;padding:4px 6px}
      .n23-empty{grid-column:1/-1;padding:36px 18px;border:1px dashed #ffffff20;border-radius:14px;text-align:center;color:#858d96}.n23-retry{margin-top:12px}.n23-nav-link{white-space:nowrap}
      @media(max-width:980px){.n23-grid{grid-template-columns:1fr 1fr}}@media(max-width:680px){.n23-shell{padding-top:92px}.n23-head{grid-template-columns:1fr}.n23-actions{justify-content:flex-start}.n23-toolbar,.n23-grid{grid-template-columns:1fr}}
    `;
    document.head.append(style);
  }

  function installNav() {
    const nav = document.querySelector('.navlinks');
    if (!nav || nav.querySelector('[data-n23-guides]')) return;
    const link = make('a', '攻略', 'navlink n23-nav-link');
    link.href = ROUTE;
    link.dataset.n23Guides = '1';
    const quote = nav.querySelector('[data-go="quote"]');
    if (quote) quote.before(link); else nav.append(link);
  }

  function buildPage() {
    let page = document.querySelector('.page[data-page="guides"]');
    if (page) return page;
    page = document.createElement('main');
    page.className = 'page';
    page.dataset.page = 'guides';
    page.id = 'n23GuideCenter';

    const shell = make('div', null, 'shell n23-shell');
    const head = make('section', null, 'n23-head');
    const copy = make('div');
    copy.append(make('div', 'FRONTEND NEXT · N23', 'kicker'));
    const h1 = make('h1');
    h1.append(document.createTextNode('攻略'), make('span', '中心', 'g'));
    copy.append(h1, make('p', '按游戏与关键词浏览已公开攻略，进入详情后继续保持在 Frontend Next 预览链路。', 'sub'));

    const actions = make('div', null, 'n23-actions');
    const services = make('a', '浏览服务', 'btn');
    services.href = '/next/services';
    const legacy = make('a', '查看旧版攻略中心', 'btn');
    legacy.href = '/guides?legacy=1';
    legacy.dataset.n23Legacy = '1';
    actions.append(services, legacy);
    head.append(copy, actions);

    const toolbar = make('section', null, 'n23-toolbar');
    const search = document.createElement('input');
    search.type = 'search';
    search.id = 'n23GuideSearch';
    search.placeholder = '搜索标题、摘要、标签…';
    search.maxLength = 80;
    search.autocomplete = 'off';
    search.setAttribute('aria-label', '搜索攻略');
    const game = document.createElement('select');
    game.id = 'n23GuideGame';
    game.setAttribute('aria-label', '按游戏筛选攻略');
    toolbar.append(search, game);

    const status = make('div', '正在读取攻略…', 'n23-status');
    status.id = 'n23GuideStatus';
    status.setAttribute('role', 'status');
    const grid = make('section', null, 'n23-grid');
    grid.id = 'n23GuideGrid';
    grid.setAttribute('aria-live', 'polite');

    shell.append(head, toolbar, status, grid);
    page.append(shell);
    const footer = document.querySelector('footer');
    if (footer) footer.before(page); else document.body.append(page);

    search.addEventListener('input', () => {
      const state = currentState();
      writeState({ q: search.value, game: state.game }, 'replace');
      render();
    });
    game.addEventListener('change', () => {
      const state = currentState();
      writeState({ q: state.q, game: game.value }, 'push');
      render();
    });
    return page;
  }

  function gameOptions() {
    const select = document.getElementById('n23GuideGame');
    if (!select) return;
    const current = currentState().game;
    const unique = new Map();
    items.forEach((item) => {
      const code = text(item?.gameCode).toUpperCase();
      if (code && !unique.has(code)) unique.set(code, text(item?.gameName) || code);
    });
    const options = [new Option('全部游戏', '')];
    [...unique.entries()].sort((a, b) => a[1].localeCompare(b[1], 'zh-CN')).forEach(([code, name]) => options.push(new Option(name, code)));
    select.replaceChildren(...options);
    select.value = unique.has(current) ? current : '';
    if (current && !unique.has(current)) writeState({ ...currentState(), game: '' }, 'replace');
  }

  function matches(item, state) {
    if (state.game && text(item?.gameCode).toUpperCase() !== state.game) return false;
    if (!state.q) return true;
    const haystack = [item?.title, item?.subtitle, item?.summary, item?.gameName, ...(Array.isArray(item?.tags) ? item.tags : [])]
      .map(lower).join('\n');
    return haystack.includes(lower(state.q));
  }

  function card(item) {
    const link = make('a', null, 'n23-card');
    link.href = `/next/guides/${encodeURIComponent(text(item?.slug))}`;

    const cover = make('div', null, 'n23-cover');
    const image = safeImage(item?.coverImage);
    if (image) {
      const img = document.createElement('img');
      img.src = image;
      img.alt = text(item?.title) || '攻略封面';
      img.loading = 'lazy';
      cover.append(img);
    }

    const body = make('div', null, 'n23-body');
    const meta = make('div', null, 'n23-meta');
    if (item?.featured) meta.append(make('span', '精选', 'n23-pill n23-featured'));
    if (text(item?.gameName)) meta.append(make('span', item.gameName, 'n23-pill'));
    if (text(item?.difficulty)) meta.append(make('span', item.difficulty, 'n23-pill'));
    if (Number(item?.estimatedMinutes) > 0) meta.append(make('span', `${Number(item.estimatedMinutes)} 分钟`, 'n23-pill'));
    body.append(meta, make('h2', text(item?.title) || '攻略'));
    const summary = text(item?.summary) || text(item?.subtitle);
    if (summary) body.append(make('p', summary));
    const tags = make('div', null, 'n23-tags');
    (Array.isArray(item?.tags) ? item.tags.slice(0, 5) : []).forEach((value) => tags.append(make('span', value, 'n23-tag')));
    if (tags.childElementCount) body.append(tags);
    link.append(cover, body);
    return link;
  }

  function render() {
    if (!loaded) return;
    const state = currentState();
    const search = document.getElementById('n23GuideSearch');
    const game = document.getElementById('n23GuideGame');
    if (search && search.value !== state.q) search.value = state.q;
    if (game && game.value !== state.game) game.value = state.game;
    const filtered = items.filter((item) => matches(item, state));
    const grid = document.getElementById('n23GuideGrid');
    const status = document.getElementById('n23GuideStatus');
    if (!grid || !status) return;
    grid.replaceChildren(...filtered.map(card));
    if (!filtered.length) grid.append(make('div', '没有找到符合条件的攻略。可以清空搜索或切换游戏。', 'n23-empty'));
    status.textContent = `已公开 ${items.length} 篇 · 当前显示 ${filtered.length} 篇${state.game ? ' · 已筛选游戏' : ''}${state.q ? ' · 已搜索关键词' : ''}`;
    window.XHFrontendNextN14?.normalize?.(grid);
  }

  function renderError() {
    const grid = document.getElementById('n23GuideGrid');
    const status = document.getElementById('n23GuideStatus');
    if (!grid || !status) return;
    status.textContent = '攻略暂时无法读取。';
    const empty = make('div', '读取攻略失败，请稍后重试。', 'n23-empty');
    const retry = make('button', '重新读取', 'btn n23-retry');
    retry.type = 'button';
    retry.addEventListener('click', load);
    empty.append(document.createElement('br'), retry);
    grid.replaceChildren(empty);
  }

  async function load() {
    if (loading) return;
    loading = true;
    const request = ++requestSeq;
    const status = document.getElementById('n23GuideStatus');
    if (status) status.textContent = '正在读取攻略…';
    try {
      const response = await fetch(API, { method: 'GET', credentials: 'same-origin', cache: 'no-store', headers: { Accept: 'application/json' } });
      const data = await response.json().catch(() => []);
      if (request !== requestSeq) return;
      if (!response.ok || !Array.isArray(data)) throw new Error('guide-list-unavailable');
      items = data.filter((item) => text(item?.slug) && text(item?.title));
      loaded = true;
      gameOptions();
      render();
    } catch (error) {
      if (request === requestSeq) renderError();
    } finally {
      if (request === requestSeq) loading = false;
    }
  }

  function metadata() {
    document.title = '攻略中心 · 星火网游';
    let canonical = document.querySelector('link[rel="canonical"]');
    if (!canonical) {
      canonical = document.createElement('link');
      canonical.rel = 'canonical';
      document.head.append(canonical);
    }
    canonical.href = 'https://wydlgf.cn/guides';
    let robots = document.querySelector('meta[name="robots"]');
    if (!robots) {
      robots = document.createElement('meta');
      robots.name = 'robots';
      document.head.append(robots);
    }
    robots.content = 'noindex,follow';
  }

  function activate() {
    if (!/^\/next\/guides\/?$/.test(location.pathname)) return false;
    installStyles();
    installNav();
    const page = buildPage();
    document.querySelectorAll('.page').forEach((node) => node.classList.toggle('active', node === page));
    document.querySelectorAll('.navlink').forEach((node) => {
      const active = node.dataset.n23Guides === '1';
      node.classList.toggle('active', active);
      if (active) node.setAttribute('aria-current', 'page'); else node.removeAttribute('aria-current');
    });
    metadata();
    const state = currentState();
    const search = document.getElementById('n23GuideSearch');
    if (search) search.value = state.q;
    if (!loaded) load(); else { gameOptions(); render(); }
    document.documentElement.dataset.frontendPage = 'guides';
    return true;
  }

  installNav();
  activate();
  window.addEventListener('popstate', activate);
  window.addEventListener('xh:site-config', () => {
    if (/^\/next\/guides\/?$/.test(location.pathname)) document.title = `攻略中心 · ${text(window.XHFrontendShell?.configuration?.brandName) || '星火网游'}`;
  });

  window.XHFrontendNextN23 = { activate, load, currentState, writeState };
})();
