(() => {
  const $ = (selector, root = document) => root.querySelector(selector);
  const $$ = (selector, root = document) => [...root.querySelectorAll(selector)];
  const esc = value => String(value ?? '').replace(/[&<>'"]/g, char => ({
    '&': '&amp;', '<': '&lt;', '>': '&gt;', "'": '&#39;', '"': '&quot;'
  }[char]));

  const STORAGE_KEY = 'xinghuo-mw4-camo-tracker-v2';
  const QUOTE_KEY = 'xinghuo-cod-tool-quote-snapshot';
  const MASTERY_FALLBACK = ['MULTIPLAYER', 'WARZONE', 'DMZ'];

  // 账号云同步：沿用 /api/account/progress 的乐观锁协议
  const TOOL_KEY = 'mw4_camo_tracker';
  const cloud = { enabled: false, version: null, timer: null, saving: false, dirty: false };
  // 写接口受 AccountAuthInterceptor 保护：缺少该自定义头会被判为 CSRF 并返回 403
  const ACCOUNT_HEADERS = { Accept: 'application/json', 'X-Requested-With': 'fetch' };

  // 数据同时保留英文名与中文名；界面按中文显示，英文名只用于搜索匹配。
  const zhName = entry => String(entry?.nameZh || entry?.name || '').trim();
  const resolveImage = path => window.XinghuoCamoImages?.[path] || path || '';
  const TIER_LABELS = ['一级', '二级', '三级'];

  let game = null;
  let modes = [];
  let activeMode = null;
  let state = loadState();
  let openWeapons = new Set();

  function loadState() {
    try {
      const parsed = JSON.parse(localStorage.getItem(STORAGE_KEY) || 'null');
      if (parsed && typeof parsed === 'object') {
        parsed.progress = parsed.progress && typeof parsed.progress === 'object' ? parsed.progress : {};
        parsed.favorites = Array.isArray(parsed.favorites) ? parsed.favorites : [];
        parsed.guideDone = parsed.guideDone && typeof parsed.guideDone === 'object' ? parsed.guideDone : {};
        return parsed;
      }
    } catch (ignored) {}
    return { activeMode: null, progress: {}, favorites: [], guideDone: {}, updatedAt: null };
  }

  function saveState() {
    state.activeMode = activeMode;
    state.updatedAt = new Date().toISOString();
    try { localStorage.setItem(STORAGE_KEY, JSON.stringify(state)); } catch (ignored) {}
    updateSaveState();
    scheduleCloudSave();
  }

  function updateSaveState() {
    const node = $('#saveState');
    if (!node) return;
    const time = state.updatedAt
      ? new Date(state.updatedAt).toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
      : '';
    node.textContent = time ? `进度已保存到当前设备 · ${time}` : '进度会自动保存在当前浏览器';
  }

  function modeByCode(code) {
    return modes.find(item => item.code === code) || null;
  }

  function isMastery(mode) {
    return mode?.mastery !== false;
  }

  function buckets(code) {
    state.progress[code] = state.progress[code] && typeof state.progress[code] === 'object' ? state.progress[code] : {};
    return state.progress[code];
  }

  function isDone(modeCode, weaponCode, itemCode) {
    return Boolean(state.progress?.[modeCode]?.[weaponCode]?.[itemCode]);
  }

  function isPyroDone(itemCode) {
    return Boolean(state.progress?.PYROCLAST?.['__global']?.[itemCode]);
  }

  // 单把武器的三层完成情况
  function weaponStats(mode, weapon) {
    const items = mode.items || [];
    const done = items.filter(item => isDone(mode.code, weapon.code, item.code)).length;
    return {
      items,
      done,
      total: items.length,
      complete: items.length > 0 && done === items.length,
      percent: items.length ? Math.round(done / items.length * 100) : 0
    };
  }

  // 单个模式里“达到某一层”的武器数
  function tierCount(mode, item) {
    return (game.weapons || []).filter(weapon => isDone(mode.code, weapon.code, item.code)).length;
  }

  function modeStats(mode) {
    if (!isMastery(mode)) {
      const items = mode.items || [];
      const done = items.filter(item => isPyroDone(item.code)).length;
      return { done, total: items.length, percent: items.length ? Math.round(done / items.length * 100) : 0 };
    }
    const weapons = (game.weapons || []).length;
    const done = (game.weapons || []).reduce((sum, weapon) => sum + weaponStats(mode, weapon).done, 0);
    return { done, total: weapons * (mode.items || []).length, percent: 0 };
  }

  function overallStats() {
    let done = 0;
    let total = 0;
    for (const mode of modes) {
      const stats = modeStats(mode);
      done += stats.done;
      total += stats.total;
    }
    return { done, total, percent: total ? Math.round(done / total * 100) : 0 };
  }

  // 机密迷彩：需要三个模式全部精通，属于派生状态，不需要单独勾选
  function masteredModeCount() {
    const weapons = game.weapons || [];
    return modes.filter(mode => {
      if (!isMastery(mode)) return false;
      return weapons.length > 0 && weapons.every(weapon => weaponStats(mode, weapon).complete);
    }).length;
  }

  function renderSecretCard() {
    const masteryModes = modes.filter(isMastery).length || 3;
    const mastered = masteredModeCount();
    const locked = mastered < masteryModes;
    return `<div class="camo-secret-card ${locked ? 'is-locked' : 'is-open'}">
      <div class="camo-secret-media" aria-hidden="true">?</div>
      <div class="camo-secret-body">
        <span class="camo-tier-label">机密</span>
        <b>机密迷彩</b>
        <p>要求：[已编辑]</p>
        <small>传闻集齐所有精通迷彩后可解锁隐藏迷彩。在《黑色行动 7》中名为 Nexus Horizon；《现代战争 4》的对应名称将在发售后公布。</small>
        <span class="camo-secret-state">${locked ? '🔒 已锁定' : '🔓 已解锁'} · 已掌握 ${mastered}/${masteryModes} 种模式</span>
      </div>
    </div>`;
  }

  function tabLabel(mode) {
    const stats = modeStats(mode);
    return `${esc(mode.shortName || mode.name || mode.code)} <small>${stats.done}/${stats.total}</small>`;
  }

  function renderTabs() {
    const host = $('#modeTabs');
    host.innerHTML = modes.map(mode =>
      `<button type="button" role="tab" aria-selected="${mode.code === activeMode ? 'true' : 'false'}" class="camo-tab ${mode.code === activeMode ? 'active' : ''}" data-mode="${esc(mode.code)}">${tabLabel(mode)}</button>`
    ).join('');
  }

  function renderProgress() {
    const overall = overallStats();
    $('#overallPercent').textContent = `${overall.percent}%`;
    $('#overallBar').style.width = `${overall.percent}%`;
    $('#overallCount').textContent = `${overall.done} / ${overall.total} 项迷彩`;
    updateSaveState();
  }

  function renderTierCards(mode) {
    const weapons = (game.weapons || []).length;
    $('#tierCards').innerHTML = (mode.items || []).map((item, index) => {
      const done = tierCount(mode, item);
      const unlocked = weapons > 0 && done === weapons;
      return `<label class="camo-tier-card ${unlocked ? 'is-done' : ''}">
        <input type="checkbox" data-mastery-item="${esc(item.code)}" ${unlocked ? 'checked' : ''}>
        <div class="camo-tier-media"><img src="${esc(resolveImage(item.image))}" alt="${esc(zhName(item))} 迷彩" loading="lazy"></div>
        <div class="camo-tier-body">
          <span class="camo-tier-label">${TIER_LABELS[index] || `第 ${index + 1} 层`}</span>
          <div class="camo-tier-title"><b>${esc(zhName(item))}</b><span>${unlocked ? '已解锁' : '未解锁'}</span></div>
          <p>${esc(item.requirement || '条件暂未公布')}</p>
          ${done > 0 && !unlocked ? `<small>已有 ${done}/${weapons} 把武器的历史记录，勾选后统一解锁</small>` : ''}
        </div>
      </label>`;
    }).join('');
  }

  function renderModePanel(mode) {
    const mastery = isMastery(mode);
    $('#modePanel').classList.toggle('hidden', !mastery);
    $('#pyroPanel').classList.toggle('hidden', mastery);
    $('#filterBar').classList.toggle('hidden', !mastery);
    $('#weaponHint').classList.toggle('hidden', !mastery);
    $('#weaponGroups').classList.toggle('hidden', !mastery);
    $('#emptyState').classList.add('hidden');

    if (!mastery) {
      const items = mode.items || [];
      const done = items.filter(item => isPyroDone(item.code)).length;
      $('#pyroMeta').textContent = `${done} / ${items.length} 项`;
      $('#pyroCards').innerHTML = items.map(item => {
        const checked = isPyroDone(item.code);
        return `<label class="camo-pyro-card ${checked ? 'is-done' : ''}">
          <input type="checkbox" data-pyro-item="${esc(item.code)}" ${checked ? 'checked' : ''}>
          <span class="camo-pyro-media"><img src="${esc(resolveImage(item.image))}" alt="${esc(zhName(item))} 迷彩" loading="lazy"></span>
          <span class="camo-pyro-body">
            <b>${esc(zhName(item))}</b>
            <small>${esc(item.requirement || '条件暂未公布')}</small>
          </span>
          <span class="camo-pyro-check">✓</span>
        </label>`;
      }).join('');
      $('#secretCard').innerHTML = renderSecretCard();
      return;
    }

    const weapons = (game.weapons || []).length;
    $('#modeHeadTitle').textContent = `${mode.shortName || mode.name || mode.code} 大师迷彩`;
    $('#modeHeadMeta').textContent = `${(mode.items || []).filter(item => weapons > 0 && tierCount(mode, item) === weapons).length} / ${(mode.items || []).length} 档已解锁 · 当前模式全部武器通用`;
    $('#modeHeadFinal').textContent = mode.finalCamoZh ? `最终层级 · ${mode.finalCamoZh}` : '';
    $('#modeNote').textContent = mode.note || '';
    renderTierCards(mode);
  }

  function filteredWeapons(mode) {
    const query = $('#weaponSearch').value.trim().toLowerCase();
    const type = $('#classFilter').value;
    const statusValue = $('#statusFilter').value;
    return (game.weapons || []).filter(weapon => {
      const stats = weaponStats(mode, weapon);
      const favorite = state.favorites.includes(weapon.code);
      if (type && weapon.weaponType !== type) return false;
      if (statusValue === 'todo' && stats.complete) return false;
      if (statusValue === 'done' && !stats.complete) return false;
      if (statusValue === 'fav' && !favorite) return false;
      const haystack = [weapon.nameZh, weapon.name, weapon.code, weapon.weaponType, weapon.unlockLabel]
        .join(' ').toLowerCase();
      return !query || haystack.includes(query);
    });
  }

  function renderGuide(mode, weapon) {
    const catalog = game.camoGuide || {};
    const specific = mode.code === 'MULTIPLAYER' && catalog.weapons?.[weapon.code];
    const guide = specific || catalog.classes?.[weapon.weaponType];
    if (!guide) return '';
    const checkbox = (key, title, text, image) => {
      const id = `${weapon.code}::${key}`;
      return `<label class="camo-guide-item"><input type="checkbox" data-guide-item="${esc(id)}" ${state.guideDone[id] ? 'checked' : ''}>
        ${image ? `<img src="${esc(resolveImage(image))}" alt="" loading="lazy">` : ''}
        <span><strong>${esc(title)}</strong>${text ? `<small>${esc(text)}</small>` : ''}</span></label>`;
    };
    const military = guide.military || {};
    const images = specific ? [military.image].filter(Boolean) : (catalog.militaryImages?.length ? catalog.militaryImages : [military.image].filter(Boolean));
    const tiles = Array.from({ length: military.count || 8 }, (_, i) => checkbox(`mil-${i}`,
      military.levels?.[i] ? `${military.levels[i]}级` : `第${i + 1}档`, '', images[i % images.length]));
    const groups = [['special', '特殊迷彩', 'special'], ['completionist', '完成主义', 'comp']].map(([field, title, prefix]) => {
      const rows = (guide[field] || []).map((item, i) => item.text ? checkbox(`${prefix}-${i}`, item.name || `${title} ${i + 1}`,
        `${item.estimate ? '参考条件 · ' : ''}${item.text}`, item.image) : '').join('');
      return rows ? `<section><h4>${title}</h4>${rows}</section>` : '';
    }).join('');
    return `<section class="camo-guide"><h3>完整迷彩挑战</h3>
      <p>${specific ? `满级 ${esc(guide.maxLevel || '待确认')} · ${esc(guide.unlock || '')}` : '职业通用挑战参考'} · 挑战勾选仅保存在本机，不计入大师进度</p>
      <section><h4>军事迷彩 · ${military.count || 8} 档</h4><p>${military.estimate ? '参考条件 · ' : ''}${esc(military.text || '')}</p><div class="camo-military-grid">${tiles.join('')}</div></section>
      ${groups}${guide.note ? `<p>${esc(guide.note)}</p>` : ''}</section>`;
  }

  function renderWeapon(mode, weapon) {
    const favorite = state.favorites.includes(weapon.code);
    const open = openWeapons.has(weapon.code);
    const label = zhName(weapon);
    const thumb = weapon.image
      ? `<img class="weapon-thumb" src="${esc(resolveImage(weapon.image))}" alt="" loading="lazy">`
      : '<span class="weapon-thumb is-empty" aria-hidden="true"></span>';
    return `<article class="weapon-tracker ${open ? 'is-open' : ''}" data-weapon-wrap="${esc(weapon.code)}">
      <div class="weapon-tracker-head">
        <button type="button" class="camo-fav ${favorite ? 'is-fav' : ''}" data-fav="${esc(weapon.code)}" aria-label="${favorite ? '取消收藏' : '收藏'} ${esc(label)}">★</button>
        <button type="button" class="weapon-tracker-main" data-toggle-weapon="${esc(weapon.code)}" aria-expanded="${open}">
          ${thumb}
          <span class="weapon-text">
            <strong>${esc(label)}</strong>
            <small>${esc(weapon.weaponType || '武器')} · ${esc(weapon.unlockLabel || '解锁条件待确认')}</small>
          </span>
          <span class="weapon-chevron" aria-hidden="true">⌄</span>
        </button>
      </div>
      <div class="weapon-tracker-body">${renderGuide(mode, weapon)}</div>
    </article>`;
  }

  function renderWeapons() {
    const mode = modeByCode(activeMode);
    if (!mode || !isMastery(mode)) return;
    const rows = filteredWeapons(mode);
    const favorites = rows.filter(weapon => state.favorites.includes(weapon.code));
    const others = rows.filter(weapon => !state.favorites.includes(weapon.code));
    const grouped = new Map();
    for (const weapon of others) {
      const key = weapon.weaponType || '其他武器';
      if (!grouped.has(key)) grouped.set(key, []);
      grouped.get(key).push(weapon);
    }
    let html = '';
    if (favorites.length) {
      html += `<section class="camo-class-group is-favorites">
        <header class="camo-class-head"><h2>已收藏</h2><span>${favorites.length} 把</span></header>
        ${favorites.map(weapon => renderWeapon(mode, weapon)).join('')}
      </section>`;
    }
    for (const [type, weapons] of grouped.entries()) {
      const done = weapons.filter(weapon => weaponStats(mode, weapon).complete).length;
      html += `<section class="camo-class-group">
        <header class="camo-class-head"><h2>${esc(type)}</h2><span>${done}/${weapons.length} 把完成</span></header>
        ${weapons.map(weapon => renderWeapon(mode, weapon)).join('')}
      </section>`;
    }
    $('#weaponGroups').innerHTML = html;
    $('#emptyState').classList.toggle('hidden', rows.length > 0);
    $('#trackerStatus').classList.add('hidden');
  }

  function render() {
    renderTabs();
    renderProgress();
    const mode = modeByCode(activeMode);
    if (mode) renderModePanel(mode);
    renderWeapons();
  }

  function publishQuoteSnapshot() {
    const summary = modes.map(mode => {
      const stats = modeStats(mode);
      return `${mode.shortName || mode.name || mode.code}:${stats.done}/${stats.total}`;
    }).join(' · ');
    try {
      localStorage.setItem(QUOTE_KEY, JSON.stringify({
        gameCode: 'CODMW4',
        gameName: game?.name,
        shortName: game?.shortName,
        summary,
        updatedAt: new Date().toISOString()
      }));
    } catch (ignored) {}
  }

  function toggleFavorite(code) {
    const set = new Set(state.favorites);
    set.has(code) ? set.delete(code) : set.add(code);
    state.favorites = [...set];
    saveState();
    render();
  }

  function toggleMastery(itemCode, checked) {
    const mode = modeByCode(activeMode);
    if (!mode || !isMastery(mode) || !(mode.items || []).some(item => item.code === itemCode)) return;
    const progress = buckets(activeMode);
    for (const weapon of game.weapons || []) {
      progress[weapon.code] = progress[weapon.code] || {};
      if (checked) progress[weapon.code][itemCode] = true;
      else delete progress[weapon.code][itemCode];
    }
    saveState();
    render();
  }

  function togglePyro(itemCode, checked) {
    state.progress.PYROCLAST = state.progress.PYROCLAST || {};
    state.progress.PYROCLAST['__global'] = state.progress.PYROCLAST['__global'] || {};
    state.progress.PYROCLAST['__global'][itemCode] = checked;
    saveState();
    render();
  }

  /* ---------------------------------------------------------- 账号云同步 */

  function setSyncBar(stateName, lead, text, actionLabel, actionHref) {
    const bar = $('#syncBar');
    if (!bar) return;
    bar.dataset.state = stateName;
    $('#syncLead').textContent = lead;
    $('#syncText').textContent = text;
    const action = $('#syncAction');
    if (!actionLabel) {
      action.classList.add('hidden');
      action.removeAttribute('href');
      action.removeAttribute('data-sync-retry');
      return;
    }
    action.classList.remove('hidden');
    action.textContent = actionLabel;
    if (actionHref) {
      action.setAttribute('href', actionHref);
      action.removeAttribute('data-sync-retry');
    } else {
      action.setAttribute('href', '#');
      action.setAttribute('data-sync-retry', '1');
    }
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
      try { localStorage.setItem(STORAGE_KEY, JSON.stringify(state)); } catch (ignored) {}
      return true;
    } catch (ignored) {
      return false;
    }
  }

  function markSynced() {
    setSyncBar('synced', '已同步到您的帐户。', '您在所有登录的设备上都能看到您的迷彩进度。');
  }

  async function checkAccount() {
    setSyncBar('checking', '正在检查账号状态', '登录后可在所有设备上同步进度。');
    try {
      const response = await fetch('/api/account/me', { headers: ACCOUNT_HEADERS });
      if (!response.ok) {
        setSyncBar('guest', '尚未登录。', '登录后可在所有设备上同步进度。', '登录同步', '/account/login');
        return;
      }
      cloud.enabled = true;
      setSyncBar('syncing', '正在同步', '正在读取账号里的迷彩进度…');
      await loadCloudProgress();
    } catch (ignored) {
      setSyncBar('guest', '尚未登录。', '登录后可在所有设备上同步进度。', '登录同步', '/account/login');
    }
  }

  async function loadCloudProgress() {
    try {
      const response = await fetch(`/api/account/progress/${TOOL_KEY}`, { headers: ACCOUNT_HEADERS });
      if (response.status === 401) {
        cloud.enabled = false;
        setSyncBar('guest', '尚未登录。', '登录后可在所有设备上同步进度。', '登录同步', '/account/login');
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
      markSynced();
    } catch (ignored) {
      setSyncBar('error', '同步失败。', '云端进度读取失败，本机进度仍然保留。', '重试');
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
    if (!silent) setSyncBar('syncing', '正在同步', '正在把本机进度保存到账号…');
    try {
      const response = await fetch(`/api/account/progress/${TOOL_KEY}`, {
        method: 'PUT',
        headers: Object.assign({ 'Content-Type': 'application/json' }, ACCOUNT_HEADERS),
        body: JSON.stringify({ version: cloud.version ?? 0, payload: cloudPayload() })
      });
      if (response.status === 401) {
        cloud.enabled = false;
        setSyncBar('guest', '登录状态已失效。', '重新登录后可在所有设备上同步进度。', '登录同步', '/account/login');
        return;
      }
      if (response.status === 409) {
        // 另一台设备已更新：载入云端版本，避免静默覆盖
        await loadCloudProgress();
        setSyncBar('conflict', '其他设备有新进度。', '已载入账号里的最新进度，请确认后再继续记录。');
        return;
      }
      if (!response.ok) throw new Error('save failed');
      const data = await response.json();
      cloud.version = Number(data.version) || cloud.version;
      markSynced();
    } catch (ignored) {
      setSyncBar('error', '同步失败。', '本机进度已保存，下次修改时会自动重试。', '重试');
    } finally {
      cloud.saving = false;
      if (cloud.dirty) scheduleCloudSave();
    }
  }

  function bindEvents() {
    $('#modeTabs').addEventListener('click', event => {
      const button = event.target.closest('[data-mode]');
      if (!button) return;
      activeMode = button.dataset.mode;
      openWeapons = new Set();
      saveState();
      render();
    });

    $('#weaponGroups').addEventListener('click', event => {
      const favorite = event.target.closest('[data-fav]');
      if (favorite) return toggleFavorite(favorite.dataset.fav);
      const toggle = event.target.closest('[data-toggle-weapon]');
      if (!toggle) return;
      const code = toggle.dataset.toggleWeapon;
      openWeapons.has(code) ? openWeapons.delete(code) : openWeapons.add(code);
      const wrap = $(`[data-weapon-wrap="${CSS.escape(code)}"]`);
      if (wrap) {
        wrap.classList.toggle('is-open', openWeapons.has(code));
        const main = wrap.querySelector('.weapon-tracker-main');
        if (main) main.setAttribute('aria-expanded', String(openWeapons.has(code)));
      }
    });

    $('#weaponGroups').addEventListener('change', event => {
      const guide = event.target.closest('[data-guide-item]');
      if (guide) {
        if (guide.checked) state.guideDone[guide.dataset.guideItem] = true;
        else delete state.guideDone[guide.dataset.guideItem];
        try { localStorage.setItem(STORAGE_KEY, JSON.stringify(state)); } catch (ignored) {}
        return;
      }

    });

    $('#tierCards').addEventListener('change', event => {
      const input = event.target.closest('[data-mastery-item]');
      if (input) toggleMastery(input.dataset.masteryItem, input.checked);
    });

    $('#pyroCards').addEventListener('change', event => {
      const input = event.target.closest('[data-pyro-item]');
      if (!input) return;
      togglePyro(input.dataset.pyroItem, input.checked);
    });

    $('#classFilter').addEventListener('change', renderWeapons);
    $('#statusFilter').addEventListener('change', renderWeapons);
    $('#weaponSearch').addEventListener('input', renderWeapons);

    $('#resetProgress').addEventListener('click', () => {
      const mode = modeByCode(activeMode);
      if (!mode) return;
      const label = mode.shortName || mode.name || mode.code;
      if (!confirm(`确定清空「${label}」的大师迷彩进度吗？完整挑战记录和收藏会保留。`)) return;
      if (isMastery(mode)) state.progress[activeMode] = {};
      else { state.progress.PYROCLAST = {}; }
      saveState();
      render();
    });

    $('#clearFavorites').addEventListener('click', () => {
      if (!confirm('确定清空全部武器收藏吗？')) return;
      state.favorites = [];
      saveState();
      render();
    });

    $('#quoteWithProgress').addEventListener('click', publishQuoteSnapshot);

    $('#syncAction').addEventListener('click', event => {
      if (!event.currentTarget.hasAttribute('data-sync-retry')) return;
      event.preventDefault();
      if (!cloud.enabled) {
        checkAccount();
        return;
      }
      pushCloudProgress(false);
    });
  }

  async function init() {
    try {
      const response = await fetch('/api/tool-station', { headers: { Accept: 'application/json' } });
      if (!response.ok) throw new Error('工具数据读取失败');
      const payload = await response.json();
      game = (payload.games || []).find(item => item.code === 'CODMW4') || (payload.games || [])[0];
      if (!game) throw new Error('没有找到 MW4 工具数据');

      const track = game.camoTracks || {};
      const order = Array.isArray(track.order) && track.order.length ? track.order : MASTERY_FALLBACK;
      modes = order.map(code => track.modes?.[code]).filter(Boolean);
      if (!modes.length) throw new Error('没有找到迷彩数据');
      activeMode = modes.some(item => item.code === state.activeMode) ? state.activeMode : modes[0].code;

      $('#classFilter').innerHTML = '<option value="">全部类型</option>' + [...new Set((game.weapons || []).map(item => item.weaponType).filter(Boolean))]
        .map(type => `<option value="${esc(type)}">${esc(type)}</option>`).join('');

      bindEvents();
      render();
      publishQuoteSnapshot();
      await checkAccount();
    } catch (error) {
      const node = $('#trackerStatus');
      node.classList.remove('hidden');
      node.textContent = error.message || '工具数据读取失败，请刷新后重试。';
    }
  }

  document.addEventListener('DOMContentLoaded', init);
})();
