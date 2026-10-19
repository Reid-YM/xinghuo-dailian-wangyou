(() => {
  'use strict';
  const runtimePath = pathname => window.XHFrontendShell?.runtimePath?.(pathname) || pathname || location.pathname;
  const navigation = window.XHFrontendShell?.navigation || history;

  if (!/^\/next\/mastery\/?$/.test(runtimePath())) return;

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
  const cloud = { enabled: false, ready: false, reading: false, conflict: null, needsChoice: false, version: null, timer: null, saving: false, dirty: false };
  let localRevision = 0;
  let syncView = null;
  let localStorageAvailable = true;
  let game = null;
  let modes = [];
  let activeMode = '';
  let state = loadState();
  cloud.dirty = state.syncPending === true;
  let requestToken = 0;
  let searchTimer = 0;
  let composingSearch = false;

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
    if (cloudSave) state.syncPending = true;
    state.activeMode = activeMode;
    state.updatedAt = new Date().toISOString();
    try { localStorage.setItem(STORAGE_KEY, JSON.stringify(state)); localStorageAvailable = true; }
    catch (_) { localStorageAvailable = false; }
    if (cloudSave) { localRevision++; cloud.dirty = true; scheduleCloudSave(); }
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
    navigation.replaceState({ ...(navigation.state || {}), n24Mastery: true }, '', url.pathname + url.search);
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
      .n24-sync{display:flex;gap:10px;align-items:center;justify-content:space-between;border:1px solid #ffffff15;border-radius:13px;padding:12px 14px;background:#0a0e12;margin-bottom:12px}.n24-sync [hidden]{display:none}.n24-sync{flex-wrap:wrap}.n24-sync-copy{display:grid;gap:3px}.n24-sync-copy strong{font-size:11px}.n24-sync-copy span{font-size:9px;color:#78828d}.n24-sync[data-state="synced"]{border-color:#47d78a44}.n24-sync[data-state="error"],.n24-sync[data-state="conflict"]{border-color:#ff9b8755}
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
    section.setAttribute('role', 'main');
    section.tabIndex = -1;
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
    if (window.XHFrontendShell?.isPreview === false) robots.remove();
    else robots.content = 'noindex,follow';
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
    syncView = { stateName, lead, detail, action };
    const bar = $('#n24Sync');
    if (!bar) return;
    bar.dataset.state = stateName;
    for (const id of ['n24UseLocal', 'n24UseCloud']) {
      const choice = document.getElementById(id);
      if (choice) choice.hidden = !cloud.conflict;
    }
    $('#n24SyncLead').textContent = lead;
    $('#n24SyncText').textContent = detail + (localStorageAvailable ? '' : ' 当前浏览器无法保存本机进度，请保持页面打开并重试同步。');
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
      state.syncPending = false;
      saveLocal({ cloudSave: false });
      return true;
    } catch (_) {
      return false;
    }
  }

  async function checkAccount() {
    cloud.ready = false;
    setSync('checking', '正在检查账号状态', '登录后可跨设备同步工具进度。');
    try {
      const response = await fetch('/api/account/me', { headers: ACCOUNT_HEADERS, credentials: 'same-origin', cache: 'no-store' });
      if (response.status === 401) {
        cloud.enabled = false;
        setSync('guest', '当前使用本机保存', '登录后可跨设备同步工具进度。', { label: '登录同步', href: '/next/account?next=%2Fnext%2Fmastery' });
        return;
      }
      if (!response.ok) throw new Error('account read failed');
      cloud.enabled = true;
      await loadCloudProgress();
    } catch (_) {
      cloud.enabled = false;
      setSync('error', '账号状态暂不可用', '本机进度保留，请重试账号状态。', { label: '重试同步', retry: true });
    }
  }

  function validCloudPayload(payload) {
    const parsed = typeof payload === 'string' ? JSON.parse(payload) : payload;
    if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed)
        || !parsed.progress || typeof parsed.progress !== 'object' || Array.isArray(parsed.progress)
        || !Array.isArray(parsed.favorites)) throw new Error('invalid cloud progress');
    return JSON.stringify({ progress: parsed.progress, favorites: parsed.favorites });
  }

  async function loadCloudProgress({ forceChoice = false } = {}) {
    if (!cloud.enabled || cloud.reading) return;
    cloud.reading = true;
    cloud.ready = false;
    const revision = localRevision;
    cloud.needsChoice = cloud.needsChoice || forceChoice || !!cloud.conflict;
    cloud.conflict = null;
    clearTimeout(cloud.timer);
    setSync('syncing', '正在读取账号进度', '此期间的勾选仍会保存在本机。');
    try {
      const response = await fetch(`/api/account/progress/${TOOL_KEY}`, {
        method: 'GET', headers: ACCOUNT_HEADERS, credentials: 'same-origin', cache: 'no-store'
      });
      if (response.status === 401) {
        cloud.enabled = false;
        setSync('guest', '登录状态已失效', '本机进度保留，重新登录后可继续同步。', { label: '重新登录', href: '/next/account?next=%2Fnext%2Fmastery' });
        return;
      }
      if (response.status === 404) {
        cloud.version = 0;
        cloud.needsChoice = false;
        cloud.conflict = null;
        cloud.ready = true;
        cloud.reading = false;
        if (cloud.saving) setSync('pending', '云端还没有进度', '本机进度已保留，可以重新同步。', { label: '同步本机进度', retry: true });
        else await pushCloudProgress(true);
        return;
      }
      if (!response.ok) throw new Error('read failed');
      const data = await response.json();
      if (!Number.isInteger(data.version) || data.version < 1) throw new Error('invalid version');
      const payload = validCloudPayload(data.payload);
      cloud.version = data.version;
      const hasLocal = state.syncPending === true || Object.keys(state.progress).length > 0 || state.favorites.length > 0;
      if (cloud.needsChoice || revision !== localRevision || (hasLocal && cloudPayload() !== payload)) {
        cloud.conflict = { payload };
        cloud.needsChoice = true;
        setSync('conflict', '本机与云端进度不同', '本机勾选已保留。请选择使用哪份进度，确认前不会自动覆盖。');
        return;
      }
      cloud.conflict = null;
      cloud.dirty = false;
      cloud.ready = true;
      if (!applyCloudPayload(payload)) throw new Error('invalid progress');
      render();
      setSync('synced', '已同步到您的账号', '修改后将自动同步。');
    } catch (_) {
      setSync('error', '云同步读取失败', '本机进度保留，读取成功前不会上传覆盖云端。', { label: '重试同步', retry: true });
    } finally {
      cloud.reading = false;
    }
  }

  function scheduleCloudSave() {
    if (!/^\/next\/mastery\/?$/.test(runtimePath()) || !cloud.enabled || !cloud.ready || cloud.conflict || cloud.reading) return;
    clearTimeout(cloud.timer);
    if (!cloud.saving) setSync('pending', '本机进度已保存', '正在等待同步到账号…');
    cloud.timer = setTimeout(() => pushCloudProgress(false), 800);
  }

  async function pushCloudProgress(silent) {
    if (!/^\/next\/mastery\/?$/.test(runtimePath()) || !cloud.enabled || !cloud.ready || cloud.reading || cloud.conflict || cloud.saving) return;
    cloud.saving = true;
    const revision = localRevision;
    let saved = false;
    if (!silent) setSync('syncing', '正在同步账号进度', '正在保存当前工具进度…');
    try {
      const response = await fetch(`/api/account/progress/${TOOL_KEY}`, {
        method: 'PUT', credentials: 'same-origin',
        headers: { ...ACCOUNT_HEADERS, 'Content-Type': 'application/json' },
        body: JSON.stringify({ version: cloud.version ?? 0, payload: cloudPayload() })
      });
      if (response.status === 401) {
        cloud.enabled = false;
        cloud.ready = false;
        setSync('guest', '登录状态已失效', '本机进度已保存，重新登录后可继续同步。', { label: '重新登录', href: '/next/account?next=%2Fnext%2Fmastery' });
        return;
      }
      if (response.status === 409) {
        cloud.ready = false;
        await loadCloudProgress({ forceChoice: true });
        return;
      }
      if (!response.ok) throw new Error('save failed');
      const data = await response.json();
      if (!Number.isInteger(data.version) || data.version <= cloud.version) throw new Error('invalid version');
      cloud.version = data.version;
      cloud.dirty = revision !== localRevision;
      if (!cloud.dirty) { state.syncPending = false; saveLocal({ cloudSave: false }); }
      saved = true;
      setSync(cloud.dirty ? 'syncing' : 'synced', cloud.dirty ? '正在保存后续修改' : '已同步到您的账号', '本机进度保留。');
    } catch (_) {
      cloud.dirty = true;
      setSync('error', '云同步保存失败', '本机进度已保存，请重试同步。', { label: '重试同步', retry: true });
    } finally {
      cloud.saving = false;
      if (saved && cloud.dirty) scheduleCloudSave();
    }
  }

  function chooseProgress(source) {
    if (!cloud.conflict || cloud.reading || cloud.saving) return;
    const remote = source === 'cloud';
    if (!confirm(remote ? '使用云端进度替换此设备当前进度？' : '使用此设备当前进度更新账号，替换云端记录？')) return;
    if (remote && !applyCloudPayload(cloud.conflict.payload)) return;
    cloud.conflict = null;
    cloud.needsChoice = false;
    cloud.ready = true;
    cloud.dirty = !remote;
    render();
    publishQuoteSnapshot();
    if (remote) setSync('synced', '已使用云端进度', '当前设备已保存所选进度。');
    else pushCloudProgress(false);
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
    bar.setAttribute('role', 'progressbar'); bar.setAttribute('aria-label', '总体进度');
    bar.setAttribute('aria-valuemin', '0'); bar.setAttribute('aria-valuemax', '100'); bar.setAttribute('aria-valuenow', String(overall.percent));
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
    tabs.setAttribute('role', 'tablist'); tabs.setAttribute('aria-label', '工具模式');
    modes.forEach((mode) => {
      const stats = modeStats(mode);
      const button = node('button', 'n24-tab', text(mode.shortName) || text(mode.name) || text(mode.code));
      button.type = 'button';
      button.dataset.mode = mode.code;
      button.id = 'n24Tab-' + mode.code;
      button.tabIndex = mode.code === activeMode ? 0 : -1;
      button.setAttribute('aria-controls', 'n24ModePanel');
      button.setAttribute('role', 'tab');
      button.setAttribute('aria-selected', String(mode.code === activeMode));
      button.append(node('small', '', `${stats.done}/${stats.total}`));
      tabs.append(button);
    });
    window.XHFrontendShell?.bindTabs?.(tabs);
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
    const imageUrl = safeImage(weapon.image);
    const image = document.createElement(imageUrl ? 'img' : 'span');
    image.className = 'n24-thumb';
    image.setAttribute('aria-hidden', 'true');
    if (imageUrl) {
      image.alt = ''; image.loading = 'lazy'; image.src = imageUrl;
      image.addEventListener('error', () => {
        const placeholder = node('span', 'n24-thumb'); placeholder.setAttribute('aria-hidden', 'true');
        image.replaceWith(placeholder);
      }, { once: true });
    }
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
    search.setAttribute('aria-label', '搜索武器');
    search.type = 'search';
    search.value = filters.q;
    search.placeholder = '搜索武器、中英文名、解锁条件…';
    search.autocomplete = 'off';
    const type = document.createElement('select');
    type.id = 'n24Type';
    type.setAttribute('aria-label', '筛选武器类型');
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
    status.setAttribute('aria-label', '筛选完成状态');
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

  function focusedControl(section) {
    const element = document.activeElement;
    if (!element || !section.contains(element)) return null;
    let selector = element.id ? '#' + CSS.escape(element.id) : '';
    if (!selector && element.matches('input[data-item-code]')) selector = 'input'
      + ['modeCode', 'weaponCode', 'itemCode'].map(key => '[data-' + key.replace(/[A-Z]/g, c => '-' + c.toLowerCase()) + '="' + CSS.escape(element.dataset[key]) + '"]').join('');
    if (!selector && element.dataset.favorite) selector = '[data-favorite="' + CSS.escape(element.dataset.favorite) + '"]';
    if (!selector) return null;
    return { selector, start: element.selectionStart, end: element.selectionEnd };
  }

  function render() {
    if (!/^\/next\/mastery\/?$/.test(runtimePath())) return;
    if (composingSearch && document.activeElement?.id === 'n24Search') return;
    composingSearch = false;
    clearTimeout(searchTimer);
    const section = ensureSection();
    const focused = focusedControl(section);
    activate(section);
    const shell = $('.shell', section);
    if (!shell || !game || !modes.length) return;
    const queryMode = routeState().mode;
    if (queryMode && modeByCode(queryMode)) activeMode = queryMode;
    if (!modeByCode(activeMode)) activeMode = modes[0].code;

    shell.replaceChildren();
    const head = node('div', 'n24-head');
    const intro = node('div');
    intro.append(node('div', 'n24-kicker', '武器与迷彩'));
    intro.append(node('h1', '', `${text(game.shortName) || 'MW4'} 工具站`));
    intro.append(node('p', '', '勾选已完成的武器与迷彩，进度自动保存在此设备；登录后可跨设备同步。'));
    const actions = node('div', 'n24-actions');
    const legacy = node('a', 'btn', '打开旧版工具站');
    legacy.href = '/mastery';
    legacy.dataset.n24Legacy = '1';
    const quote = node('a', 'btn gold', '带进度去询价');
    quote.href = '/next/quote?game=CODMW4';
    quote.addEventListener('click', publishQuoteSnapshot);
    actions.append(legacy, quote);
    head.append(intro, actions);
    shell.append(head);

    const sync = node('div', 'n24-sync');
    sync.id = 'n24Sync'; sync.setAttribute('role', 'status'); sync.tabIndex = -1;
    sync.dataset.state = cloud.enabled ? 'syncing' : 'checking';
    const syncCopy = node('div', 'n24-sync-copy');
    syncCopy.append(node('strong', '', '正在检查账号状态'), node('span', '', '本机进度始终保留。'));
    syncCopy.firstChild.id = 'n24SyncLead';
    syncCopy.lastChild.id = 'n24SyncText';
    const syncAction = node('a', 'btn', '重试同步');
    syncAction.id = 'n24SyncAction';
    syncAction.hidden = true;
    const useLocal = node('button', 'btn', '保留本机并同步');
    useLocal.type = 'button'; useLocal.id = 'n24UseLocal'; useLocal.hidden = true;
    const useCloud = node('button', 'btn', '使用云端进度');
    useCloud.type = 'button'; useCloud.id = 'n24UseCloud'; useCloud.hidden = true;
    sync.append(syncCopy, syncAction, useLocal, useCloud);
    shell.append(sync);

    renderOverview(shell);
    renderTabs(shell);
    const mode = modeByCode(activeMode);
    const panel = node('div'); panel.id = 'n24ModePanel'; panel.tabIndex = 0;
    panel.setAttribute('role', 'tabpanel'); panel.setAttribute('aria-labelledby', 'n24Tab-' + activeMode);
    renderToolbar(panel, mode);
    renderTierCards(panel, mode);
    if (isMastery(mode)) renderWeapons(panel, mode); else renderGlobal(panel, mode);
    shell.append(panel);
    shell.append(node('p', 'n24-note', '进度保存在当前浏览器。清理浏览器数据前，请先登录并确认账号同步完成。'));
    bindRenderedControls(section);
    updateSyncAfterRender();
    if (focused) {
      const control = section.querySelector(focused.selector);
      control?.focus({preventScroll:true});
      if (control && ['text', 'search'].includes(control.type) && focused.start != null)
        control.setSelectionRange(focused.start, focused.end);
    }
  }

  function updateSyncAfterRender() {
    if (syncView) {
      const { stateName, lead, detail, action } = syncView;
      setSync(stateName, lead, detail, action);
    } else {
      setSync('checking', '正在检查账号状态', '本机进度会自动保存。');
    }
  }

  function bindRenderedControls(section) {
    section.querySelectorAll('[data-mode]').forEach((button) => {
      button.addEventListener('click', () => {
        activeMode = button.dataset.mode;
        const filters = routeState();
        writeRoute({ ...filters, mode: activeMode });
        saveLocal({ cloudSave: false });
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
    const applySearch = () => {
      clearTimeout(searchTimer);
      if (!/^\/next\/mastery\/?$/.test(runtimePath())) return;
      writeRoute({ ...routeState(), mode: activeMode, q: search.value });
      searchTimer = setTimeout(render, 180);
    };
    search?.addEventListener('compositionstart', () => { composingSearch = true; clearTimeout(searchTimer); });
    search?.addEventListener('compositionend', () => { composingSearch = false; applySearch(); });
    search?.addEventListener('blur', () => { if (composingSearch) { composingSearch = false; applySearch(); } });
    search?.addEventListener('input', event => { if (!event.isComposing && !composingSearch) applySearch(); });
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
    $('#n24UseLocal', section)?.addEventListener('click', () => chooseProgress('local'));
    $('#n24UseCloud', section)?.addEventListener('click', () => chooseProgress('cloud'));
    $('#n24SyncAction', section)?.addEventListener('click', (event) => {
      const action = event.currentTarget;
      if (!action.dataset.retry) return;
      event.preventDefault();
      if (!cloud.enabled) checkAccount();
      else if (!cloud.ready) loadCloudProgress();
      else pushCloudProgress(false);
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
      if (request !== requestToken || !/^\/next\/mastery\/?$/.test(runtimePath())) return;
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
    if (!/^\/next\/mastery\/?$/.test(runtimePath())) return;
    const requested = routeState().mode;
    if (modeByCode(requested)) activeMode = requested;
    if (game) { render(); checkAccount(); } else load();
  });

  window.addEventListener('pagehide', () => { clearTimeout(cloud.timer); });
  window.addEventListener('pageshow', () => { if (cloud.dirty) scheduleCloudSave(); });

  window.XHFrontendNextN24 = {
    reload: load,
    get state() {
      return { activeMode, progress: state.progress, favorites: [...state.favorites], cloudEnabled: cloud.enabled, cloudVersion: cloud.version };
    }
  };

  load();
})();
