(() => {
  'use strict';

  if (!/^\/next\/mastery\/?$/.test(location.pathname)) return;

  const STAGE = 'N24 · MASTERY TRACKER';
  const STORAGE_KEY = 'xinghuo-mw4-camo-tracker-v2';
  const QUOTE_KEY = 'xinghuo-cod-tool-quote-snapshot';
  const TOOL_KEY = 'mw4_camo_tracker';
  const MASTERY_FALLBACK = ['MULTIPLAYER', 'WARZONE', 'DMZ'];
  const ACCOUNT_HEADERS = { Accept: 'application/json', 'X-Requested-With': 'fetch' };
  const $ = (selector, root = document) => root.querySelector(selector);
  const $$ = (selector, root = document) => [...root.querySelectorAll(selector)];
  const text = (value) => String(value ?? '').trim();
  const list = (value) => Array.isArray(value) ? value : [];
  const cloud = { enabled: false, version: null, timer: null, saving: false, dirty: false };
  let game = null;
  let modes = [];
  let activeMode = '';
  let state = loadState();
  let requestToken = 0;

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

  function loadState() {
    try {
      const parsed = JSON.parse(localStorage.getItem(STORAGE_KEY) || 'null');
      if (parsed && typeof parsed === 'object') {
        parsed.progress = parsed.progress && typeof parsed.progress === 'object' ? parsed.progress : {};
        parsed.favorites = Array.isArray(parsed.favorites) ? parsed.favorites : [];
        return parsed;
      }
    } catch (_) {}
    return { activeMode: null, progress: {}, favorites: [], updatedAt: null };
  }

  function saveLocal({ cloudSave = true } = {}) {
    state.activeMode = activeMode;
    state.updatedAt = new Date().toISOString();
    try { localStorage.setItem(STORAGE_KEY, JSON.stringify(state)); } catch (_) {}
    if (cloudSave) scheduleCloudSave();
  }

  function routeState() {
    const query = new URLSearchParams(location.search);
    return {
      mode: text(query.get('mode')).toUpperCase(),
      q: text(query.get('q')),
      type: text(query.get('type')),
      status: ['all', 'todo', 'done', 'fav'].includes(query.get('status')) ? query.get('status') : 'all'
    };
  }

  function writeRoute(next) {
    const url = new URL(location.href);
    ['mode', 'q', 'type', 'status', 'legacy'].forEach((key) => url.searchParams.delete(key));
    if (next.mode) url.searchParams.set('mode', next.mode);
    if (next.q) url.searchParams.set('q', next.q);
    if (next.type) url.searchParams.set('type', next.type);
    if (next.status && next.status !== 'all') url.searchParams.set('status', next.status);
    history.replaceState({ ...(history.state || {}), n24Mastery: true }, '', url.pathname + url.search);
  }

  function isMastery(mode) {
    return mode?.mastery !== false;
  }

  function modeByCode(code) {
    return modes.find((mode) => text(mode?.code) === text(code)) || null;
  }

  function ensureBuckets(modeCode, weaponCode) {
    state.progress[modeCode] = state.progress[modeCode] && typeof state.progress[modeCode] === 'object' ? state.progress[modeCode] : {};
    state.progress[modeCode][weaponCode] = state.progress[modeCode][weaponCode] && typeof state.progress[modeCode][weaponCode] === 'object'
      ? state.progress[modeCode][weaponCode]
      : {};
    return state.progress[modeCode][weaponCode];
  }

  function isDone(modeCode, weaponCode, itemCode) {
    return Boolean(state.progress?.[modeCode]?.[weaponCode]?.[itemCode]);
  }

  function weaponStats(mode, weapon) {
    const items = list(mode?.items);
    const done = items.filter((item) => isDone(mode.code, weapon.code, item.code)).length;
    return { done, total: items.length, complete: items.length > 0 && done === items.length };
  }

  function modeStats(mode) {
    if (!isMastery(mode)) {
      const items = list(mode?.items);
      const done = items.filter((item) => isDone(mode.code, '__global', item.code)).length;
      return { done, total: items.length };
    }
    const weapons = list(game?.weapons);
    const total = weapons.length * list(mode?.items).length;
    const done = weapons.reduce((sum, weapon) => sum + weaponStats(mode, weapon).done, 0);
    return { done, total };
  }

  function overallStats() {
    const totals = modes.reduce((acc, mode) => {
      const stats = modeStats(mode);
      acc.done += stats.done;
      acc.total += stats.total;
      return acc;
    }, { done: 0, total: 0 });
    return { ...totals, percent: totals.total ? Math.round(totals.done / totals.total * 100) : 0 };
  }

  function installStyles() {
    if ($('#frontend-next-n24-style')) return;
    const style = document.createElement('style');
    style.id = 'frontend-next-n24-style';
    style.textContent = `
      .n24-mastery{min-height:100vh;padding:112px 0 84px;background:radial-gradient(circle at 80% 8%,#ffcb2d14,transparent 30rem)}
      .n24-head{display:grid;grid-template-columns:minmax(0,1fr) auto;gap:18px;align-items:end;margin-bottom:16px}.n24-head h1{font-size:clamp(42px,6vw,72px);margin:10px 0 8px}.n24-head p{max-width:760px;color:#9ba4ae;line-height:1.75;margin:0}.n24-kicker{color:#d9bb61;font-size:10px;font-weight:900;letter-spacing:.16em;text-transform:uppercase}.n24-actions{display:flex;gap:8px;flex-wrap:wrap}
      .n24-sync{display:flex;gap:10px;align-items:center;justify-content:space-between;border:1px solid #ffffff15;border-radius:13px;padding:12px 14px;background:#0a0e12;margin-bottom:12px}.n24-sync-copy{display:grid;gap:3px}.n24-sync-copy strong{font-size:11px}.n24-sync-copy span{font-size:9px;color:#78828d}.n24-sync[data-state="synced"]{border-color:#47d78a44}.n24-sync[data-state="error"],.n24-sync[data-state="conflict"]{border-color:#ff9b8755}
      .n24-overview{display:grid;grid-template-columns:1.2fr repeat(3,.7fr);gap:9px;margin-bottom:12px}.n24-stat{border:1px solid #ffffff15;border-radius:13px;background:#0d1116;padding:15px}.n24-stat small{color:#7d8791;font-size:9px}.n24-stat strong{display:block;font-size:26px;margin-top:5px}.n24-progress{height:7px;border-radius:99px;background:#252b32;overflow:hidden;margin-top:10px}.n24-progress i{display:block;height:100%;background:var(--gold);border-radius:99px}
      .n24-tabs{display:flex;gap:7px;overflow:auto;padding:5px 0 11px}.n24-tab{flex:0 0 auto;border:1px solid #ffffff15;background:#0d1116;color:#929ba5;border-radius:10px;padding:10px 12px}.n24-tab[aria-selected="true"]{background:var(--gold);color:#17130a;border-color:var(--gold);font-weight:900}.n24-tab small{display:block;font-size:8px;opacity:.7;margin-top:3px}
      .n24-toolbar{display:grid;grid-template-columns:minmax(210px,1.3fr) minmax(150px,.7fr) minmax(140px,.6fr) auto;gap:8px;position:sticky;top:86px;z-index:35;padding:10px;border:1px solid #ffffff15;border-radius:13px;background:#090d11e9;backdrop-filter:blur(16px);margin-bottom:12px}.n24-toolbar input,.n24-toolbar select{margin:0}.n24-toolbar .btn{white-space:nowrap}
      .n24-tier-grid{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:9px;margin-bottom:12px}.n24-tier{border:1px solid #ffffff15;border-radius:13px;background:#0d1116;overflow:hidden}.n24-tier-media{aspect-ratio:16/7;background:#090d11;display:grid;place-items:center;overflow:hidden}.n24-tier-media img{width:100%;height:100%;object-fit:cover}.n24-tier-body{padding:13px}.n24-tier-body small{color:#c9b76f}.n24-tier-body strong{display:block;margin:5px 0}.n24-tier-body p{color:#87909a;font-size:9px;line-height:1.55;margin:0}
      .n24-weapons{display:grid;gap:10px}.n24-group{display:grid;gap:8px}.n24-group-head{display:flex;justify-content:space-between;gap:10px;align-items:center;margin-top:6px}.n24-group-head h2{font-size:18px;margin:0}.n24-group-head span{color:#79838d;font-size:9px}.n24-weapon{border:1px solid #ffffff15;border-radius:14px;background:#0d1116;padding:14px}.n24-weapon-head{display:grid;grid-template-columns:auto 72px minmax(0,1fr) auto;gap:11px;align-items:center}.n24-fav{border:0;background:transparent;color:#57616c;font-size:19px;padding:5px}.n24-fav[aria-pressed="true"]{color:var(--gold)}.n24-thumb{width:72px;height:50px;object-fit:contain;background:#090d11;border-radius:8px}.n24-weapon-title strong{display:block}.n24-weapon-title span{display:block;color:#7f8993;font-size:9px;margin-top:4px}.n24-weapon-score{color:#d8bb61;font-size:11px;font-weight:800}.n24-checks{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:7px;margin-top:12px}.n24-check{display:flex;gap:8px;align-items:flex-start;border:1px solid #ffffff10;border-radius:10px;padding:10px;background:#090d11}.n24-check input{width:auto;margin:2px 0 0;accent-color:#ffcb2d}.n24-check strong{display:block;font-size:10px}.n24-check small{display:block;color:#77818b;font-size:8px;line-height:1.45;margin-top:3px}.n24-check.is-done{border-color:#47d78a44;background:#47d78a0a}
      .n24-global{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:10px}.n24-global .n24-check{min-height:110px}.n24-empty,.n24-state{min-height:260px;border:1px solid #ffffff15;border-radius:14px;background:#0d1116;display:grid;place-items:center;text-align:center;padding:28px;color:#919ba5}.n24-state h2,.n24-empty h2{color:#f2f4f6;margin:0 0 8px}.n24-state p,.n24-empty p{margin:0 0 14px;line-height:1.7}.n24-note{margin-top:14px;color:#66717c;font-size:9px;line-height:1.65}
      @media(max-width:980px){.n24-head{grid-template-columns:1fr}.n24-overview{grid-template-columns:1fr 1fr}.n24-tier-grid,.n24-checks,.n24-global{grid-template-columns:1fr 1fr}.n24-toolbar{top:78px;grid-template-columns:1fr 1fr}}
      @media(max-width:650px){.n24-mastery{padding-top:92px}.n24-actions{display:grid}.n24-toolbar{position:relative;top:auto;grid-template-columns:1fr}.n24-tier-grid,.n24-checks,.n24-global,.n24-overview{grid-template-columns:1fr}.n24-weapon-head{grid-template-columns:44px 58px minmax(0,1fr)}.n24-thumb{width:58px}.n24-weapon-score{grid-column:2/-1}.n24-sync{align-items:flex-start;flex-direction:column}}
      /* N29 mobile: allow long customer-provided and published content to wrap safely. */
      .n24-mastery .n24-weapon-head,.n24-mastery .n24-weapon-head>*,.n24-mastery .n24-state,.n24-mastery .n24-check{min-width:0}
      .n24-mastery .n24-weapon-head :is(h2,h3,p,span),.n24-mastery .n24-check,.n24-mastery .n24-state{overflow-wrap:anywhere}
    `;
    document.head.append(style);
  }

  function ensureSection() {
    let section = $('#n24Mastery');
    if (section) return section;
    section = node('section', 'page n24-mastery');
    section.id = 'n24Mastery';
    section.dataset.page = 'mastery';
    section.append(node('div', 'shell'));
    const footer = $('.next-footer') || $('.footer');
    if (footer) footer.before(section); else document.body.append(section);
    return section;
  }

  function activate(section) {
    $$('.page').forEach((page) => page.classList.toggle('active', page === section));
    $$('.navlink[data-go]').forEach((link) => {
      link.classList.remove('active');
      link.removeAttribute('aria-current');
    });
    document.documentElement.dataset.frontendPage = 'mastery';
    document.documentElement.dataset.frontendNextStage = 'n24-mastery';
  }

  function setMetadata() {
    const brand = text(window.XHFrontendShell?.configuration?.brandName) || '星火网游';
    document.title = `MW4 工具站 · ${brand}`;
    let description = $('meta[name="description"]');
    if (!description) {
      description = document.createElement('meta');
      description.name = 'description';
      document.head.append(description);
    }
    description.content = 'MW4 迷彩与武器进度工具，支持本机保存和登录账号云同步。';
    let canonical = $('link[rel="canonical"]');
    if (!canonical) {
      canonical = document.createElement('link');
      canonical.rel = 'canonical';
      document.head.append(canonical);
    }
    canonical.href = 'https://wydlgf.cn/mastery';
    let robots = $('meta[name="robots"]');
    if (!robots) {
      robots = document.createElement('meta');
      robots.name = 'robots';
      document.head.append(robots);
    }
    robots.content = 'noindex,follow';
  }

  function publishQuoteSnapshot() {
    if (!game) return;
    const summary = modes.map((mode) => {
      const stats = modeStats(mode);
      return `${text(mode.shortName) || text(mode.name) || text(mode.code)}:${stats.done}/${stats.total}`;
    }).join(' · ');
    try {
      localStorage.setItem(QUOTE_KEY, JSON.stringify({
        gameCode: text(game.code) || 'CODMW4',
        gameName: text(game.name),
        shortName: text(game.shortName),
        summary,
        updatedAt: new Date().toISOString()
      }));
    } catch (_) {}
  }

  function setSync(stateName, lead, detail, action = null) {
    const bar = $('#n24Sync');
    if (!bar) return;
    bar.dataset.state = stateName;
    $('#n24SyncLead').textContent = lead;
    $('#n24SyncText').textContent = detail;
    const button = $('#n24SyncAction');
    if (!button) return;
    if (!action) {
      button.hidden = true;
      button.removeAttribute('href');
      button.removeAttribute('data-retry');
      return;
    }
    button.hidden = false;
    button.textContent = action.label;
    button.href = action.href || '#';
    if (action.retry) button.dataset.retry = '1';
    else button.removeAttribute('data-retry');
  }

  function cloudPayload() {
    return JSON.stringify({ progress: state.progress, favorites: state.favorites });
  }

  function applyCloudPayload(payload) {
    try {
      const parsed = typeof payload === 'string' ? JSON.parse(payload) : payload;
      if (!parsed || typeof parsed !== 'object') return false;
      state.progress = parsed.progress && typeof parsed.progress === 'object' ? parsed.progress : {};
      state.favorites = Array.isArray(parsed.favorites) ? parsed.favorites : [];
      saveLocal({ cloudSave: false });
      return true;
    } catch (_) {
      return false;
    }
  }

  async function checkAccount() {
    setSync('checking', '正在检查账号状态', '登录后可跨设备同步工具进度。');
    try {
      const response = await fetch('/api/account/me', { headers: ACCOUNT_HEADERS, credentials: 'same-origin', cache: 'no-store' });
      if (!response.ok) {
        setSync('guest', '当前使用本机保存', '登录后可跨设备同步工具进度。', { label: '登录同步', href: '/next/account?next=%2Fnext%2Fmastery' });
        return;
      }
      cloud.enabled = true;
      await loadCloudProgress();
    } catch (_) {
      setSync('guest', '当前使用本机保存', '账号状态暂不可用，本机进度不受影响。', { label: '登录同步', href: '/next/account?next=%2Fnext%2Fmastery' });
    }
  }

  async function loadCloudProgress() {
    if (!cloud.enabled) return;
    setSync('syncing', '正在同步账号进度', '正在读取账号里的迷彩进度…');
    try {
      const response = await fetch(`/api/account/progress/${TOOL_KEY}`, {
        method: 'GET', headers: ACCOUNT_HEADERS, credentials: 'same-origin', cache: 'no-store'
      });
      if (response.status === 401) {
        cloud.enabled = false;
        setSync('guest', '登录状态已失效', '重新登录后可继续同步。', { label: '重新登录', href: '/next/account?next=%2Fnext%2Fmastery' });
        return;
      }
      if (response.status === 404) {
        cloud.version = 0;
        await pushCloudProgress(true);
        return;
      }
      if (!response.ok) throw new Error('read failed');
      const data = await response.json();
      cloud.version = Number(data.version) || 0;
      if (applyCloudPayload(data.payload)) render();
      setSync('synced', '已同步到您的账号', '当前进度可在登录设备间继续使用。');
    } catch (_) {
      setSync('error', '云同步读取失败', '本机进度仍然保留。', { label: '重试同步', retry: true });
    }
  }

  function scheduleCloudSave() {
    if (!cloud.enabled) return;
    cloud.dirty = true;
    clearTimeout(cloud.timer);
    cloud.timer = setTimeout(() => pushCloudProgress(false), 800);
  }

  async function pushCloudProgress(silent) {
    if (!cloud.enabled || cloud.saving) return;
    cloud.saving = true;
    cloud.dirty = false;
    if (!silent) setSync('syncing', '正在同步账号进度', '正在保存当前工具进度…');
    try {
      const response = await fetch(`/api/account/progress/${TOOL_KEY}`, {
        method: 'PUT',
        credentials: 'same-origin',
        headers: { ...ACCOUNT_HEADERS, 'Content-Type': 'application/json' },
        body: JSON.stringify({ version: cloud.version ?? 0, payload: cloudPayload() })
      });
      if (response.status === 401) {
        cloud.enabled = false;
        setSync('guest', '登录状态已失效', '本机进度已保存，重新登录后可继续同步。', { label: '重新登录', href: '/next/account?next=%2Fnext%2Fmastery' });
        return;
      }
      if (response.status === 409) {
        await loadCloudProgress();
        setSync('conflict', '其他设备有更新', '已载入账号里的最新版本，避免静默覆盖。');
        return;
      }
      if (!response.ok) throw new Error('save failed');
      const data = await response.json();
      cloud.version = Number(data.version) || cloud.version;
      setSync('synced', '已同步到您的账号', '当前进度可在登录设备间继续使用。');
    } catch (_) {
      setSync('error', '云同步保存失败', '本机进度已保存，下次修改会自动重试。', { label: '重试同步', retry: true });
    } finally {
      cloud.saving = false;
      if (cloud.dirty) scheduleCloudSave();
    }
  }

  function toggleFavorite(code) {
    const set = new Set(state.favorites);
    set.has(code) ? set.delete(code) : set.add(code);
    state.favorites = [...set];
    saveLocal();
    render();
  }

  function toggleItem(modeCode, weaponCode, itemCode, checked) {
    ensureBuckets(modeCode, weaponCode)[itemCode] = checked;
    saveLocal();
    publishQuoteSnapshot();
    render();
  }

  function filteredWeapons(mode) {
    const filters = routeState();
    const needle = filters.q.toLowerCase();
    return list(game?.weapons).filter((weapon) => {
      const stats = weaponStats(mode, weapon);
      if (filters.type && text(weapon.weaponType) !== filters.type) return false;
      if (filters.status === 'todo' && stats.complete) return false;
      if (filters.status === 'done' && !stats.complete) return false;
      if (filters.status === 'fav' && !state.favorites.includes(weapon.code)) return false;
      if (!needle) return true;
      return [weapon.nameZh, weapon.name, weapon.code, weapon.weaponType, weapon.unlockLabel]
        .some((value) => text(value).toLowerCase().includes(needle));
    });
  }

  function renderOverview(root) {
    const overall = overallStats();
    const active = modeByCode(activeMode);
    const activeStats = active ? modeStats(active) : { done: 0, total: 0 };
    const completedWeapons = active && isMastery(active)
      ? list(game?.weapons).filter((weapon) => weaponStats(active, weapon).complete).length
      : 0;
    const cards = node('div', 'n24-overview');
    const total = node('div', 'n24-stat');
    total.append(node('small', '', '总体进度'), node('strong', '', `${overall.percent}%`));
    const bar = node('div', 'n24-progress');
    const fill = node('i');
    fill.style.width = `${overall.percent}%`;
    bar.append(fill);
    total.append(bar);
    const current = node('div', 'n24-stat');
    current.append(node('small', '', '当前模式'), node('strong', '', `${activeStats.done}/${activeStats.total}`));
    const weapons = node('div', 'n24-stat');
    weapons.append(node('small', '', '当前模式完成武器'), node('strong', '', String(completedWeapons)));
    const favorites = node('div', 'n24-stat');
    favorites.append(node('small', '', '武器收藏'), node('strong', '', String(state.favorites.length)));
    cards.append(total, current, weapons, favorites);
    root.append(cards);
  }

  function renderTabs(root) {
    const tabs = node('div', 'n24-tabs');
    tabs.setAttribute('role', 'tablist');
    modes.forEach((mode) => {
      const stats = modeStats(mode);
      const button = node('button', 'n24-tab', text(mode.shortName) || text(mode.name) || text(mode.code));
      button.type = 'button';
      button.dataset.mode = mode.code;
      button.setAttribute('role', 'tab');
      button.setAttribute('aria-selected', String(mode.code === activeMode));
      button.append(node('small', '', `${stats.done}/${stats.total}`));
      tabs.append(button);
    });
    root.append(tabs);
  }

  function renderTierCards(root, mode) {
    if (!isMastery(mode) || !list(mode.items).length) return;
    const grid = node('div', 'n24-tier-grid');
    list(mode.items).forEach((item, index) => {
      const card = node('article', 'n24-tier');
      const media = node('div', 'n24-tier-media');
      const imageUrl = safeImage(item.image);
      if (imageUrl) {
        const image = document.createElement('img');
        image.src = imageUrl;
        image.alt = `${text(item.nameZh) || text(item.name) || '迷彩'}预览`;
        image.loading = 'lazy';
        media.append(image);
      }
      const body = node('div', 'n24-tier-body');
      const done = list(game?.weapons).filter((weapon) => isDone(mode.code, weapon.code, item.code)).length;
      body.append(node('small', '', `第 ${index + 1} 层 · ${done}/${list(game?.weapons).length}`));
      body.append(node('strong', '', text(item.nameZh) || text(item.name) || text(item.code)));
      body.append(node('p', '', text(item.requirement) || '条件暂未公布'));
      card.append(media, body);
      grid.append(card);
    });
    root.append(grid);
  }

  function weaponCard(mode, weapon) {
    const stats = weaponStats(mode, weapon);
    const card = node('article', 'n24-weapon');
    const head = node('div', 'n24-weapon-head');
    const fav = node('button', 'n24-fav', '★');
    fav.type = 'button';
    fav.dataset.favorite = weapon.code;
    fav.setAttribute('aria-pressed', String(state.favorites.includes(weapon.code)));
    fav.setAttribute('aria-label', `${state.favorites.includes(weapon.code) ? '取消收藏' : '收藏'} ${text(weapon.nameZh) || text(weapon.name)}`);
    const image = document.createElement('img');
    image.className = 'n24-thumb';
    image.alt = '';
    image.loading = 'lazy';
    const imageUrl = safeImage(weapon.image);
    if (imageUrl) image.src = imageUrl;
    const title = node('div', 'n24-weapon-title');
    title.append(node('strong', '', text(weapon.nameZh) || text(weapon.name) || text(weapon.code)));
    title.append(node('span', '', `${text(weapon.weaponType) || '武器'} · ${text(weapon.unlockLabel) || '解锁条件待确认'}`));
    const score = node('span', 'n24-weapon-score', `${stats.done}/${stats.total}`);
    head.append(fav, image, title, score);
    card.append(head);
    const checks = node('div', 'n24-checks');
    list(mode.items).forEach((item) => {
      const checked = isDone(mode.code, weapon.code, item.code);
      const label = node('label', `n24-check${checked ? ' is-done' : ''}`);
      const input = document.createElement('input');
      input.type = 'checkbox';
      input.checked = checked;
      input.dataset.modeCode = mode.code;
      input.dataset.weaponCode = weapon.code;
      input.dataset.itemCode = item.code;
      const copy = node('span');
      copy.append(node('strong', '', text(item.nameZh) || text(item.name) || text(item.code)));
      copy.append(node('small', '', text(item.requirement) || '条件暂未公布'));
      label.append(input, copy);
      checks.append(label);
    });
    card.append(checks);
    return card;
  }

  function renderWeapons(root, mode) {
    const rows = filteredWeapons(mode);
    if (!rows.length) {
      const empty = node('div', 'n24-empty');
      const box = node('div');
      box.append(node('h2', '', '没有匹配的武器'), node('p', '', '调整搜索、武器类型或完成状态后再试。'));
      empty.append(box);
      root.append(empty);
      return;
    }
    const grouped = new Map();
    rows.forEach((weapon) => {
      const type = text(weapon.weaponType) || '其他武器';
      if (!grouped.has(type)) grouped.set(type, []);
      grouped.get(type).push(weapon);
    });
    const host = node('div', 'n24-weapons');
    for (const [type, weapons] of grouped) {
      const group = node('section', 'n24-group');
      const heading = node('header', 'n24-group-head');
      const done = weapons.filter((weapon) => weaponStats(mode, weapon).complete).length;
      heading.append(node('h2', '', type), node('span', '', `${done}/${weapons.length} 把完成`));
      group.append(heading);
      weapons.forEach((weapon) => group.append(weaponCard(mode, weapon)));
      host.append(group);
    }
    root.append(host);
  }

  function renderGlobal(root, mode) {
    const host = node('div', 'n24-global');
    list(mode.items).forEach((item) => {
      const checked = isDone(mode.code, '__global', item.code);
      const label = node('label', `n24-check${checked ? ' is-done' : ''}`);
      const input = document.createElement('input');
      input.type = 'checkbox';
      input.checked = checked;
      input.dataset.modeCode = mode.code;
      input.dataset.weaponCode = '__global';
      input.dataset.itemCode = item.code;
      const copy = node('span');
      copy.append(node('strong', '', text(item.nameZh) || text(item.name) || text(item.code)));
      copy.append(node('small', '', text(item.requirement) || '条件暂未公布'));
      label.append(input, copy);
      host.append(label);
    });
    root.append(host);
  }

  function renderToolbar(root, mode) {
    if (!isMastery(mode)) return;
    const filters = routeState();
    const toolbar = node('div', 'n24-toolbar');
    const search = document.createElement('input');
    search.id = 'n24Search';
    search.type = 'search';
    search.value = filters.q;
    search.placeholder = '搜索武器、中英文名、解锁条件…';
    search.autocomplete = 'off';
    const type = document.createElement('select');
    type.id = 'n24Type';
    const allTypes = document.createElement('option');
    allTypes.value = '';
    allTypes.textContent = '全部武器类型';
    type.append(allTypes);
    [...new Set(list(game?.weapons).map((weapon) => text(weapon.weaponType)).filter(Boolean))].forEach((value) => {
      const option = document.createElement('option');
      option.value = value;
      option.textContent = value;
      type.append(option);
    });
    type.value = filters.type;
    const status = document.createElement('select');
    status.id = 'n24Status';
    [['all', '全部状态'], ['todo', '未完成'], ['done', '已完成'], ['fav', '仅收藏']].forEach(([value, label]) => {
      const option = document.createElement('option');
      option.value = value;
      option.textContent = label;
      status.append(option);
    });
    status.value = filters.status;
    const reset = node('button', 'btn', '清空当前模式');
    reset.type = 'button';
    reset.id = 'n24Reset';
    toolbar.append(search, type, status, reset);
    root.append(toolbar);
  }

  function render() {
    const section = ensureSection();
    activate(section);
    const shell = $('.shell', section);
    if (!shell || !game || !modes.length) return;
    const queryMode = routeState().mode;
    if (queryMode && modeByCode(queryMode)) activeMode = queryMode;
    if (!modeByCode(activeMode)) activeMode = modes[0].code;

    shell.replaceChildren();
    const head = node('div', 'n24-head');
    const intro = node('div');
    intro.append(node('div', 'n24-kicker', STAGE));
    intro.append(node('h1', '', `${text(game.shortName) || 'MW4'} 工具站`));
    intro.append(node('p', '', '记录武器与迷彩完成进度；沿用旧工具站本机存档，并在登录后继续使用原账号云同步协议。'));
    const actions = node('div', 'n24-actions');
    const legacy = node('a', 'btn', '对照旧版工具站');
    legacy.href = '/mastery';
    legacy.dataset.n24Legacy = '1';
    const quote = node('a', 'btn gold', '带进度去询价');
    quote.href = '/next/quote?game=CODMW4';
    quote.addEventListener('click', publishQuoteSnapshot);
    actions.append(legacy, quote);
    head.append(intro, actions);
    shell.append(head);

    const sync = node('div', 'n24-sync');
    sync.id = 'n24Sync';
    sync.dataset.state = cloud.enabled ? 'syncing' : 'checking';
    const syncCopy = node('div', 'n24-sync-copy');
    syncCopy.append(node('strong', '', '正在检查账号状态'), node('span', '', '本机进度始终保留。'));
    syncCopy.firstChild.id = 'n24SyncLead';
    syncCopy.lastChild.id = 'n24SyncText';
    const syncAction = node('a', 'btn', '重试同步');
    syncAction.id = 'n24SyncAction';
    syncAction.hidden = true;
    sync.append(syncCopy, syncAction);
    shell.append(sync);

    renderOverview(shell);
    renderTabs(shell);
    const mode = modeByCode(activeMode);
    renderToolbar(shell, mode);
    renderTierCards(shell, mode);
    if (isMastery(mode)) renderWeapons(shell, mode); else renderGlobal(shell, mode);
    shell.append(node('p', 'n24-note', `本机存档键：${STORAGE_KEY} · 账号云同步工具键：${TOOL_KEY}。新版与旧版共享同一份进度结构。`));
    bindRenderedControls(section);
    updateSyncAfterRender();
  }

  function updateSyncAfterRender() {
    if (!cloud.enabled) {
      setSync('guest', '当前使用本机保存', state.updatedAt ? `最近保存：${new Date(state.updatedAt).toLocaleString('zh-CN')}` : '修改后会自动保存到当前浏览器。', { label: '登录同步', href: '/next/account?next=%2Fnext%2Fmastery' });
    } else if (!cloud.saving) {
      setSync('synced', '账号同步已启用', state.updatedAt ? `最近本机保存：${new Date(state.updatedAt).toLocaleString('zh-CN')}` : '修改后会自动同步。');
    }
  }

  function bindRenderedControls(section) {
    section.querySelectorAll('[data-mode]').forEach((button) => {
      button.addEventListener('click', () => {
        activeMode = button.dataset.mode;
        const filters = routeState();
        writeRoute({ ...filters, mode: activeMode });
        saveLocal();
        render();
      });
    });
    section.querySelectorAll('[data-favorite]').forEach((button) => {
      button.addEventListener('click', () => toggleFavorite(button.dataset.favorite));
    });
    section.querySelectorAll('input[data-item-code]').forEach((input) => {
      input.addEventListener('change', () => toggleItem(input.dataset.modeCode, input.dataset.weaponCode, input.dataset.itemCode, input.checked));
    });
    const search = $('#n24Search', section);
    const type = $('#n24Type', section);
    const status = $('#n24Status', section);
    let timer = 0;
    search?.addEventListener('input', () => {
      clearTimeout(timer);
      timer = setTimeout(() => {
        writeRoute({ ...routeState(), mode: activeMode, q: search.value });
        render();
      }, 180);
    });
    type?.addEventListener('change', () => {
      writeRoute({ ...routeState(), mode: activeMode, type: type.value });
      render();
    });
    status?.addEventListener('change', () => {
      writeRoute({ ...routeState(), mode: activeMode, status: status.value });
      render();
    });
    $('#n24Reset', section)?.addEventListener('click', () => {
      const mode = modeByCode(activeMode);
      const label = text(mode?.shortName) || text(mode?.name) || activeMode;
      if (!confirm(`确定清空「${label}」的全部勾选进度吗？收藏不会清除。`)) return;
      state.progress[activeMode] = {};
      saveLocal();
      publishQuoteSnapshot();
      render();
    });
    $('#n24SyncAction', section)?.addEventListener('click', (event) => {
      const action = event.currentTarget;
      if (!action.dataset.retry) return;
      event.preventDefault();
      if (cloud.enabled) pushCloudProgress(false); else checkAccount();
    });
  }

  function renderState(section, title, detail, retry = false) {
    const shell = $('.shell', section);
    if (!shell) return;
    const stateNode = node('div', 'n24-state');
    const box = node('div');
    box.append(node('h2', '', title), node('p', '', detail));
    if (retry) {
      const button = node('button', 'btn gold', '重新读取');
      button.type = 'button';
      button.addEventListener('click', load);
      box.append(button);
    }
    stateNode.append(box);
    shell.replaceChildren(stateNode);
  }

  async function load() {
    const section = ensureSection();
    activate(section);
    installStyles();
    setMetadata();
    renderState(section, '正在读取 MW4 工具数据', '正在加载武器、模式与迷彩配置…');
    const request = ++requestToken;
    try {
      const response = await fetch('/api/tool-station', {
        method: 'GET', credentials: 'same-origin', cache: 'no-store', headers: { Accept: 'application/json' }
      });
      if (!response.ok) throw new Error(`工具数据读取失败（${response.status}）`);
      const payload = await response.json();
      if (request !== requestToken || !/^\/next\/mastery\/?$/.test(location.pathname)) return;
      game = list(payload.games).find((item) => text(item.code) === 'CODMW4') || list(payload.games)[0] || null;
      if (!game) throw new Error('没有找到可用的工具站游戏数据');
      const tracks = game.camoTracks || {};
      const order = list(tracks.order).length ? tracks.order : MASTERY_FALLBACK;
      modes = order.map((code) => tracks.modes?.[code]).filter(Boolean);
      if (!modes.length) throw new Error('没有找到迷彩模式数据');
      const requested = routeState().mode;
      activeMode = modeByCode(requested)?.code || modeByCode(state.activeMode)?.code || modes[0].code;
      writeRoute({ ...routeState(), mode: activeMode });
      render();
      publishQuoteSnapshot();
      await checkAccount();
    } catch (error) {
      if (request !== requestToken) return;
      renderState(section, '工具站读取失败', text(error?.message) || '暂时无法读取工具数据。', true);
    }
  }

  window.addEventListener('popstate', () => {
    if (!/^\/next\/mastery\/?$/.test(location.pathname)) return;
    const requested = routeState().mode;
    if (modeByCode(requested)) activeMode = requested;
    if (game) render(); else load();
  });

  window.addEventListener('pagehide', () => {
    clearTimeout(cloud.timer);
  }, { once: true });

  window.XHFrontendNextN24 = {
    reload: load,
    get state() {
      return { activeMode, progress: state.progress, favorites: [...state.favorites], cloudEnabled: cloud.enabled, cloudVersion: cloud.version };
    }
  };

  load();
})();
