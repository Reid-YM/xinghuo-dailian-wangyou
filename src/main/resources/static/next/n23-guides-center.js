(() => {
  'use strict';

  if (!/^\/next\/guides\/?$/.test(location.pathname)) return;

  const STAGE = 'N23 · GUIDES CENTER';
  const $ = (selector, root = document) => root.querySelector(selector);
  const $$ = (selector, root = document) => [...root.querySelectorAll(selector)];
  const text = (value) => String(value ?? '').trim();
  const list = (value) => Array.isArray(value) ? value : [];
  let requestToken = 0;
  let guides = [];

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

  function params() {
    const search = new URLSearchParams(location.search);
    return {
      q: text(search.get('q')),
      game: text(search.get('game')).toUpperCase(),
      sort: ['featured', 'latest', 'popular'].includes(search.get('sort')) ? search.get('sort') : 'featured'
    };
  }

  function writeParams(next, mode = 'replace') {
    const url = new URL(location.href);
    ['q', 'game', 'sort'].forEach((key) => url.searchParams.delete(key));
    if (text(next.q)) url.searchParams.set('q', text(next.q));
    if (text(next.game)) url.searchParams.set('game', text(next.game).toUpperCase());
    if (next.sort && next.sort !== 'featured') url.searchParams.set('sort', next.sort);
    history[mode === 'push' ? 'pushState' : 'replaceState']({}, '', url.pathname + url.search + url.hash);
  }

  function installStyles() {
    if ($('#frontend-next-n23-style')) return;
    const style = document.createElement('style');
    style.id = 'frontend-next-n23-style';
    style.textContent = `
      .n23-guides{min-height:100vh;padding:112px 0 84px;background:radial-gradient(circle at 82% 8%,#ffcb2d14,transparent 28rem)}
      .n23-head{display:grid;grid-template-columns:minmax(0,1fr) auto;gap:20px;align-items:end;margin-bottom:20px}.n23-head h1{font-size:clamp(42px,6vw,72px);margin:10px 0 8px}.n23-head p{max-width:760px;color:#9ca5af;line-height:1.8;margin:0}.n23-kicker{color:#d9bb61;font-size:10px;font-weight:900;letter-spacing:.16em;text-transform:uppercase}.n23-head-actions{display:flex;gap:8px;flex-wrap:wrap}
      .n23-toolbar{position:sticky;top:86px;z-index:35;display:grid;grid-template-columns:minmax(220px,1.5fr) minmax(160px,.7fr) minmax(150px,.6fr);gap:8px;padding:12px;border:1px solid #ffffff18;border-radius:15px;background:#0a0e12e8;backdrop-filter:blur(16px);margin-bottom:16px}.n23-toolbar input,.n23-toolbar select{margin:0}
      .n23-summary{display:flex;gap:8px;flex-wrap:wrap;align-items:center;margin:0 0 14px;color:#89929c;font-size:11px}.n23-summary strong{color:#f1f3f5}.n23-chip{border:1px solid #ffffff14;border-radius:999px;padding:6px 9px;background:#ffffff04}
      .n23-grid{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:13px}.n23-card{display:flex;flex-direction:column;min-width:0;border:1px solid #ffffff18;border-radius:16px;background:#0d1116;overflow:hidden;transition:.22s}.n23-card:hover{transform:translateY(-4px);border-color:#ffcb2d55;box-shadow:0 22px 52px #0007}.n23-cover{aspect-ratio:16/9;background:radial-gradient(circle at 72% 32%,#ffcb2d2d,transparent 30%),linear-gradient(145deg,#211b12,#0b0e12);display:grid;place-items:center;overflow:hidden}.n23-cover img{width:100%;height:100%;object-fit:cover;display:block}.n23-cover span{color:#cbb66f;font-size:10px;letter-spacing:.12em;text-transform:uppercase}.n23-copy{display:flex;flex:1;flex-direction:column;padding:17px}.n23-card-meta{display:flex;gap:6px;flex-wrap:wrap;color:#848e98;font-size:9px}.n23-card h2{font-size:20px;line-height:1.3;margin:10px 0 8px}.n23-card p{color:#959ea8;font-size:11px;line-height:1.72;margin:0 0 14px}.n23-tags{display:flex;gap:5px;flex-wrap:wrap;margin-top:auto}.n23-tag{font-size:9px;color:#c9b76f;border:1px solid #ffcb2d2c;border-radius:999px;padding:4px 7px}.n23-open{display:flex;justify-content:space-between;gap:8px;align-items:center;margin-top:14px;padding-top:13px;border-top:1px solid #ffffff10}.n23-open span{color:#747e88;font-size:9px}.n23-open strong{color:#ffcf38;font-size:11px}
      .n23-state{min-height:280px;border:1px solid #ffffff18;border-radius:16px;background:#0d1116;display:grid;place-items:center;text-align:center;padding:30px;color:#9aa3ad}.n23-state h2{margin:0 0 8px;color:#f3f5f7}.n23-state p{margin:0 0 15px;line-height:1.7}.n23-state .btn{display:inline-block}
      @media(max-width:980px){.n23-grid{grid-template-columns:repeat(2,minmax(0,1fr))}.n23-head{grid-template-columns:1fr}.n23-toolbar{top:78px}}
      @media(max-width:650px){.n23-guides{padding-top:92px}.n23-toolbar{position:relative;top:auto;grid-template-columns:1fr}.n23-grid{grid-template-columns:1fr}.n23-head-actions{display:grid}.n23-head-actions .btn{text-align:center}}
      /* N29 mobile: allow long customer-provided and published content to wrap safely. */
      .n23-guides .n23-card,.n23-guides .n23-state,.n23-guides .n23-head{min-width:0;overflow-wrap:anywhere}
      .n23-guides .n23-card :is(h2,h3,p,a),.n23-guides .n23-state :is(h2,p){overflow-wrap:anywhere}
    `;
    document.head.append(style);
  }

  function activate(section) {
    $$('.page').forEach((page) => page.classList.toggle('active', page === section));
    $$('.navlink[data-go]').forEach((link) => {
      link.classList.remove('active');
      link.removeAttribute('aria-current');
    });
    document.documentElement.dataset.frontendPage = 'guides';
    document.documentElement.dataset.frontendNextStage = 'n23-guides-center';
  }

  function ensureSection() {
    let section = $('#n23GuidesCenter');
    if (section) return section;
    section = node('section', 'page n23-guides');
    section.id = 'n23GuidesCenter';
    section.setAttribute('role', 'main');
    section.tabIndex = -1;
    section.dataset.page = 'guides';
    section.append(node('div', 'shell'));
    const footer = $('.next-footer') || $('.footer');
    if (footer) footer.before(section); else document.body.append(section);
    return section;
  }

  function setMetadata() {
    const brand = text(window.XHFrontendShell?.configuration?.brandName) || '星火网游';
    document.title = `攻略中心 · ${brand}`;
    let description = $('meta[name="description"]');
    if (!description) {
      description = document.createElement('meta');
      description.name = 'description';
      document.head.append(description);
    }
    description.content = '游戏攻略、进度技巧与服务相关知识库。';
    let canonical = $('link[rel="canonical"]');
    if (!canonical) {
      canonical = document.createElement('link');
      canonical.rel = 'canonical';
      document.head.append(canonical);
    }
    canonical.href = 'https://wydlgf.cn/guides';
    let robots = $('meta[name="robots"]');
    if (!robots) {
      robots = document.createElement('meta');
      robots.name = 'robots';
      document.head.append(robots);
    }
    robots.content = 'noindex,follow';
  }

  function filtered() {
    const state = params();
    const needle = state.q.toLowerCase();
    let rows = guides.filter((guide) => {
      if (state.game && text(guide.gameCode).toUpperCase() !== state.game) return false;
      if (!needle) return true;
      return [guide.title, guide.subtitle, guide.summary, guide.gameName, guide.gameCode, ...list(guide.tags)]
        .some((value) => text(value).toLowerCase().includes(needle));
    });
    rows = [...rows].sort((a, b) => {
      if (state.sort === 'popular') return Number(b.viewCount || 0) - Number(a.viewCount || 0);
      if (state.sort === 'latest') return new Date(b.publishedAt || b.updatedAt || 0) - new Date(a.publishedAt || a.updatedAt || 0);
      if (!!a.featured !== !!b.featured) return a.featured ? -1 : 1;
      if (Number(a.sortOrder || 0) !== Number(b.sortOrder || 0)) return Number(a.sortOrder || 0) - Number(b.sortOrder || 0);
      return new Date(b.publishedAt || b.updatedAt || 0) - new Date(a.publishedAt || a.updatedAt || 0);
    });
    return rows;
  }

  function gameOptions() {
    const map = new Map();
    guides.forEach((guide) => {
      const code = text(guide.gameCode).toUpperCase();
      if (code && !map.has(code)) map.set(code, text(guide.gameName) || code);
    });
    return [...map.entries()].sort((a, b) => a[1].localeCompare(b[1], 'zh-CN'));
  }

  function guideCard(guide) {
    const slug = text(guide.slug);
    const link = node('a', 'n23-card');
    link.href = `/next/guides/${encodeURIComponent(slug)}`;
    const cover = node('div', 'n23-cover');
    const imageUrl = safeImage(guide.coverImage);
    if (imageUrl) {
      const image = document.createElement('img');
      image.src = imageUrl;
      image.alt = `${text(guide.title) || '攻略'}封面`;
      image.loading = 'lazy';
      cover.append(image);
    } else {
      cover.append(node('span', '', text(guide.gameName) || text(guide.gameCode) || 'GAME GUIDE'));
    }
    const copy = node('div', 'n23-copy');
    const meta = node('div', 'n23-card-meta');
    if (guide.featured) meta.append(node('span', 'n23-chip', '精选'));
    if (text(guide.gameName) || text(guide.gameCode)) meta.append(node('span', '', text(guide.gameName) || text(guide.gameCode)));
    if (text(guide.difficulty)) meta.append(node('span', '', `· ${text(guide.difficulty)}`));
    if (Number(guide.estimatedMinutes) > 0) meta.append(node('span', '', `· ${Number(guide.estimatedMinutes)} 分钟`));
    copy.append(meta, node('h2', '', text(guide.title) || '攻略'));
    copy.append(node('p', '', text(guide.summary) || text(guide.subtitle) || '查看完整攻略内容。'));
    const tags = node('div', 'n23-tags');
    list(guide.tags).slice(0, 4).forEach((tag) => { if (text(tag)) tags.append(node('span', 'n23-tag', text(tag))); });
    copy.append(tags);
    const open = node('div', 'n23-open');
    open.append(node('span', '', `${Number(guide.sectionCount || 0)} 个章节 · ${Number(guide.viewCount || 0)} 次阅读`), node('strong', '', '查看攻略 →'));
    copy.append(open);
    link.append(cover, copy);
    return link;
  }

  function renderCards(section) {
    const grid = $('.n23-grid', section);
    const summary = $('.n23-summary', section);
    if (!grid || !summary) return;
    const rows = filtered();
    const state = params();
    summary.replaceChildren(
      node('span', '', '当前显示'),
      node('strong', '', String(rows.length)),
      node('span', '', `篇攻略${state.game ? ` · ${state.game}` : ''}${state.q ? ` · 搜索“${state.q}”` : ''}`)
    );
    grid.replaceChildren();
    if (!rows.length) {
      const empty = node('div', 'n23-state');
      const box = node('div');
      box.append(node('h2', '', '没有匹配的攻略'), node('p', '', '调整搜索词或游戏筛选后再试。'));
      const clear = node('button', 'btn', '清除筛选');
      clear.type = 'button';
      clear.addEventListener('click', () => {
        writeParams({ q: '', game: '', sort: 'featured' });
        syncControls(section);
        renderCards(section);
      });
      box.append(clear);
      empty.append(box);
      grid.append(empty);
      return;
    }
    rows.forEach((guide) => grid.append(guideCard(guide)));
  }

  function syncControls(section) {
    const state = params();
    const search = $('#n23GuideSearch', section);
    const game = $('#n23GuideGame', section);
    const sort = $('#n23GuideSort', section);
    if (search) search.value = state.q;
    if (game) game.value = game.querySelector(`option[value="${CSS.escape(state.game)}"]`) ? state.game : '';
    if (sort) sort.value = state.sort;
  }

  function installContent(section) {
    const shell = $('.shell', section);
    if (!shell) return;
    shell.replaceChildren();

    const head = node('div', 'n23-head');
    const intro = node('div');
    intro.append(node('div', 'n23-kicker', '游戏攻略'), node('h1', '', '攻略中心'));
    intro.append(node('p', '', '按游戏查找攻略，浏览技巧、路线与进度说明。'));
    const actions = node('div', 'n23-head-actions');
    const legacy = node('a', 'btn', '打开旧版攻略中心');
    legacy.href = '/guides';
    legacy.dataset.n23Legacy = '1';
    const services = node('a', 'btn gold', '查看服务大厅');
    services.href = '/next/services';
    actions.append(legacy, services);
    head.append(intro, actions);

    const toolbar = node('div', 'n23-toolbar');
    const search = document.createElement('input');
    search.id = 'n23GuideSearch';
    search.setAttribute('aria-label', '搜索攻略');
    search.type = 'search';
    search.placeholder = '搜索攻略标题、标签、游戏…';
    search.autocomplete = 'off';
    const game = document.createElement('select');
    game.id = 'n23GuideGame';
    game.setAttribute('aria-label', '筛选攻略游戏');
    const all = document.createElement('option');
    all.value = '';
    all.textContent = '全部游戏';
    game.append(all);
    gameOptions().forEach(([code, name]) => {
      const option = document.createElement('option');
      option.value = code;
      option.textContent = `${name} · ${code}`;
      game.append(option);
    });
    const sort = document.createElement('select');
    sort.id = 'n23GuideSort';
    sort.setAttribute('aria-label', '攻略排序');
    [['featured', '精选优先'], ['latest', '最新发布'], ['popular', '阅读最多']].forEach(([value, label]) => {
      const option = document.createElement('option');
      option.value = value;
      option.textContent = label;
      sort.append(option);
    });
    toolbar.append(search, game, sort);

    const summary = node('div', 'n23-summary'); summary.setAttribute('role', 'status');
    const grid = node('div', 'n23-grid');
    shell.append(head, toolbar, summary, grid);

    let searchTimer = 0;
    search.addEventListener('input', () => {
      clearTimeout(searchTimer);
      searchTimer = setTimeout(() => {
        const state = params();
        writeParams({ ...state, q: search.value });
        renderCards(section);
      }, 180);
    });
    game.addEventListener('change', () => {
      const state = params();
      writeParams({ ...state, game: game.value });
      renderCards(section);
    });
    sort.addEventListener('change', () => {
      const state = params();
      writeParams({ ...state, sort: sort.value });
      renderCards(section);
    });

    syncControls(section);
    renderCards(section);
  }

  function renderLoading(section) {
    const shell = $('.shell', section);
    if (!shell) return;
    const state = node('div', 'n23-state');
    const box = node('div');
    box.append(node('h2', '', '正在读取攻略中心'), node('p', '', '正在加载已公开攻略…'));
    state.append(box);
    shell.replaceChildren(state);
  }

  function renderError(section, message) {
    const shell = $('.shell', section);
    if (!shell) return;
    const state = node('div', 'n23-state');
    const box = node('div');
    box.append(node('h2', '', '攻略读取失败'), node('p', '', text(message) || '暂时无法读取攻略，请重试。'));
    const retry = node('button', 'btn gold', '重新读取');
    retry.type = 'button';
    retry.addEventListener('click', load);
    box.append(retry);
    state.append(box);
    shell.replaceChildren(state);
  }

  async function load() {
    const section = ensureSection();
    activate(section);
    installStyles();
    setMetadata();
    renderLoading(section);
    const request = ++requestToken;
    try {
      const response = await fetch('/api/guides?limit=100', {
        method: 'GET',
        credentials: 'same-origin',
        cache: 'no-store',
        headers: { Accept: 'application/json' }
      });
      if (!response.ok) throw new Error(`读取失败（${response.status}）`);
      const body = await response.json();
      if (request !== requestToken || !/^\/next\/guides\/?$/.test(location.pathname)) return;
      guides = list(body);
      installContent(section);
    } catch (error) {
      if (request !== requestToken) return;
      renderError(section, error?.message);
    }
  }

  window.addEventListener('popstate', () => {
    if (!/^\/next\/guides\/?$/.test(location.pathname)) return;
    const section = ensureSection();
    if (!guides.length) load();
    else {
      activate(section);
      syncControls(section);
      renderCards(section);
    }
  });

  window.XHFrontendNextN23 = {
    reload: load,
    get state() { return { guides: [...guides], filters: params() }; }
  };

  load();
})();
