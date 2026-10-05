(() => {
  const $ = (selector, root = document) => root.querySelector(selector);
  const $$ = (selector, root = document) => [...root.querySelectorAll(selector)];
  const esc = value => String(value ?? '').replace(/[&<>'"]/g, char => ({
    '&': '&amp;', '<': '&lt;', '>': '&gt;', "'": '&#39;', '"': '&quot;'
  }[char]));
  const clamp = (value, min, max) => Math.min(max, Math.max(min, Number(value) || 0));
  const isDone = (state, mode, weapon, item) => Boolean(state.progress?.[mode]?.[weapon.code]?.[item.code]);
  const STORAGE_KEY = 'xinghuo-mw4-camo-tracker-v1';
  const QUOTE_KEY = 'xinghuo-cod-tool-quote-snapshot';
  const MODE_ORDER = ['MULTIPLAYER', 'WARZONE', 'DMZ'];
  const MODE_LABELS = { MULTIPLAYER: '多人', WARZONE: '战区', DMZ: 'DMZ' };
  const STAGE_LABELS = { 1: '基础', 2: '特殊', 3: '金虎', 4: '国王赎金', 5: '催化剂', 6: '深渊' };

  let game = null;
  let state = loadState();
  let activeMode = state.activeMode || 'MULTIPLAYER';
  let openWeapons = new Set();
  let status = { className: '', complete: false };

  function loadState() {
    try {
      const parsed = JSON.parse(localStorage.getItem(STORAGE_KEY) || 'null');
      if (parsed && typeof parsed === 'object') {
        parsed.progress = parsed.progress && typeof parsed.progress === 'object' ? parsed.progress : {};
        parsed.favorites = Array.isArray(parsed.favorites) ? parsed.favorites : [];
        return parsed;
      }
    } catch (ignored) {}
    return { activeMode: 'MULTIPLAYER', progress: {}, favorites: [], updatedAt: null };
  }

  function saveState() {
    state.activeMode = activeMode;
    state.updatedAt = new Date().toISOString();
    try { localStorage.setItem(STORAGE_KEY, JSON.stringify(state)); } catch (ignored) {}
    updateSaveState();
    publishQuoteSnapshot();
  }

  function updateSaveState() {
    const node = $('#saveState');
    if (!node) return;
    const time = state.updatedAt ? new Date(state.updatedAt).toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' }) : '';
    node.textContent = time ? `进度已保存到当前设备 · ${time}` : '进度会自动保存在当前浏览器';
  }

  function modeTrack(code) {
    return game?.camoTracks?.modes?.[code] || { items: [], name: MODE_LABELS[code] || code };
  }

  function itemsForWeapon(mode, weapon) {
    const online = weapon.onlineCamos?.[mode]?.items;
    if (Array.isArray(online) && online.length) {
      return online.map((item, index) => normalizeItem(item, index, true));
    }
    if (mode === 'WARZONE' && Array.isArray(weapon.warzoneCamos) && weapon.warzoneCamos.length) {
      return weapon.warzoneCamos.map((item, index) => normalizeItem(item, index, false));
    }
    return (modeTrack(mode).items || []).map((item, index) => normalizeItem(item, index, false));
  }

  function normalizeItem(item, index, online) {
    const code = item.code || `ITEM_${index + 1}`;
    return {
      code,
      name: item.name || `迷彩 ${index + 1}`,
      requirement: item.requirement || item.challenge || MODE_LABELS[activeMode] + '迷彩条件暂未提供',
      stage: item.stage || index + 1,
      requires: Array.isArray(item.requires) ? item.requires : [],
      image: item.image || (online ? '' : localCamoImage(code))
    };
  }

  function localCamoImage(code) {
    const known = ['BASE', 'SPECIAL_1', 'SPECIAL_2', 'GOLD_TIGER', 'KINGS_RANSOM', 'CATALYST', 'ABYSS'];
    return known.includes(code) ? `/assets/camo/${code}.jpg` : '';
  }

  function itemAvailable(weapon, item, mode) {
    return item.requires.every(code => isDone(state, mode, weapon, { code }));
  }

  function weaponStats(mode, weapon) {
    const items = itemsForWeapon(mode, weapon);
    const done = items.filter(item => isDone(state, mode, weapon, item)).length;
    return { items, done, total: items.length, complete: items.length > 0 && done === items.length, percent: items.length ? Math.round(done / items.length * 100) : 0 };
  }

  function modeStats(mode) {
    let done = 0;
    let total = 0;
    for (const weapon of game.weapons || []) {
      const stats = weaponStats(mode, weapon);
      done += stats.done;
      total += stats.total;
    }
    return { done, total, percent: total ? Math.round(done / total * 100) : 0 };
  }

  function overallStats() {
    let done = 0;
    let total = 0;
    for (const mode of MODE_ORDER) {
      const stats = modeStats(mode);
      done += stats.done;
      total += stats.total;
    }
    return { done, total, percent: total ? Math.round(done / total * 100) : 0 };
  }

  function renderModeTabs() {
    const host = $('#modeTabs');
    host.innerHTML = MODE_ORDER
      .filter(code => modeTrack(code).items.length || code === 'MULTIPLAYER')
      .map(code => {
        const stats = modeStats(code);
        return `<button type="button" class="camo-mode-tab ${code === activeMode ? 'active' : ''}" data-mode="${esc(code)}">${esc(MODE_LABELS[code] || code)} <small>${stats.done}/${stats.total}</small></button>`;
      }).join('');
  }

  function renderSummary() {
    const overall = overallStats();
    $('#overallPercent').textContent = `${overall.percent}%`;
    $('#overallBar').style.width = `${overall.percent}%`;
    $('#overallCount').textContent = `${overall.done} / ${overall.total}`;

    const summary = $('#modeSummary');
    summary.innerHTML = MODE_ORDER.map(code => {
      const stats = modeStats(code);
      return `<article class="camo-mode-stat ${code === activeMode ? 'is-active' : ''}">
        <span>${esc(MODE_LABELS[code] || code)}迷彩</span>
        <strong>${stats.percent}%</strong>
        <small>${stats.done} / ${stats.total} 项完成</small>
        <div class="camo-mode-progress"><i style="width:${stats.percent}%"></i></div>
      </article>`;
    }).join('');
    updateSaveState();
  }

  function filteredWeapons() {
    const query = $('#weaponSearch').value.trim().toLowerCase();
    const type = $('#classFilter').value;
    const statusValue = $('#statusFilter').value;
    return (game.weapons || []).filter(weapon => {
      const stats = weaponStats(activeMode, weapon);
      const favorite = state.favorites.includes(weapon.code);
      if (type && weapon.weaponType !== type) return false;
      if (statusValue === 'todo' && stats.complete) return false;
      if (statusValue === 'done' && !stats.complete) return false;
      if (statusValue === 'fav' && !favorite) return false;
      const haystack = [weapon.name, weapon.code, weapon.weaponType, weapon.unlockLabel].join(' ').toLowerCase();
      return !query || haystack.includes(query);
    });
  }

  function renderItem(weapon, item) {
    const done = isDone(state, activeMode, weapon, item);
    const locked = !done && !itemAvailable(weapon, item, activeMode);
    const label = `${esc(item.name)} · ${esc(item.requirement)}`;
    const image = item.image
      ? `<img class="camo-item-image" src="${esc(item.image)}" alt="${esc(item.name)}" loading="lazy">`
      : '';
    return `<label class="camo-check-item ${done ? 'is-done' : ''} ${locked ? 'is-locked' : ''}" title="${label}">
      <input type="checkbox" data-camo-item="${esc(item.code)}" data-weapon="${esc(weapon.code)}" ${done ? 'checked' : ''} ${locked ? 'disabled' : ''}>
      <span class="camo-check-box">✓</span>
      <span class="camo-check-copy">
        <strong>${esc(item.name)}</strong>
        <small>${esc(item.requirement)}</small>
      </span>
      ${image}
    </label>`;
  }

  function renderWeapon(weapon) {
    const stats = weaponStats(activeMode, weapon);
    const favorite = state.favorites.includes(weapon.code);
    const open = openWeapons.has(weapon.code);
    return `<article class="weapon-tracker ${open ? 'is-open' : ''}" data-weapon-wrap="${esc(weapon.code)}">
      <div class="weapon-tracker-head">
        <button type="button" class="camo-fav ${favorite ? 'is-fav' : ''}" data-fav="${esc(weapon.code)}" aria-label="${favorite ? '取消收藏' : '收藏'} ${esc(weapon.name)}">★</button>
        <button type="button" class="weapon-tracker-main" data-toggle-weapon="${esc(weapon.code)}">
          <strong>${esc(weapon.name)}</strong>
          <small>${esc(weapon.weaponType || '武器')} · ${esc(weapon.unlockLabel || '解锁条件待确认')}</small>
        </button>
        <span class="weapon-progress-mini ${stats.complete ? 'is-done' : ''}">${stats.done}/${stats.total} · ${stats.percent}%</span>
        <button type="button" class="weapon-expand" data-toggle-weapon="${esc(weapon.code)}" aria-label="展开 ${esc(weapon.name)}">⌄</button>
      </div>
      <div class="weapon-tracker-body">${stats.items.map(item => renderItem(weapon, item)).join('')}</div>
    </article>`;
  }

  function renderWeapons() {
    const rows = filteredWeapons();
    const grouped = new Map();
    for (const weapon of rows) {
      const key = weapon.weaponType || '其他武器';
      if (!grouped.has(key)) grouped.set(key, []);
      grouped.get(key).push(weapon);
    }
    $('#weaponGroups').innerHTML = [...grouped.entries()].map(([type, weapons]) => {
      const complete = weapons.length > 0 && weapons.every(weapon => weaponStats(activeMode, weapon).complete);
      const done = weapons.filter(weapon => weaponStats(activeMode, weapon).complete).length;
      return `<section class="camo-class-group ${complete ? 'is-complete' : ''}">
        <header class="camo-class-head"><h2>${esc(type)}</h2><span>${done}/${weapons.length} 把完成</span></header>
        ${weapons.map(renderWeapon).join('')}
      </section>`;
    }).join('');
    $('#emptyState').classList.toggle('hidden', rows.length > 0);
    $('#trackerStatus').classList.add('hidden');
  }

  function render() {
    renderModeTabs();
    renderSummary();
    renderWeapons();
  }

  function publishQuoteSnapshot() {
    const summary = MODE_ORDER.map(code => {
      const stats = modeStats(code);
      return `${MODE_LABELS[code]}:${stats.done}/${stats.total}`;
    }).join(' · ');
    try {
      localStorage.setItem(QUOTE_KEY, JSON.stringify({
        gameCode: 'CODMW4',
        gameName: game.name,
        shortName: game.shortName,
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

  function toggleItem(weaponCode, itemCode, checked) {
    const weapon = (game.weapons || []).find(item => item.code === weaponCode);
    if (!weapon) return;
    const item = itemsForWeapon(activeMode, weapon).find(entry => entry.code === itemCode);
    if (!item) return;
    if (checked && !itemAvailable(weapon, item, activeMode)) return;
    state.progress[activeMode] = state.progress[activeMode] || {};
    state.progress[activeMode][weaponCode] = state.progress[activeMode][weaponCode] || {};
    state.progress[activeMode][weaponCode][itemCode] = checked;
    if (!checked) {
      for (const entry of itemsForWeapon(activeMode, weapon)) {
        if (entry.requires.includes(itemCode)) state.progress[activeMode][weaponCode][entry.code] = false;
      }
    }
    saveState();
    render();
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
      if (wrap) wrap.classList.toggle('is-open', openWeapons.has(code));
    });

    $('#weaponGroups').addEventListener('change', event => {
      const input = event.target.closest('[data-camo-item]');
      if (!input) return;
      toggleItem(input.dataset.weapon, input.dataset.camoItem, input.checked);
    });

    $('#classFilter').addEventListener('change', renderWeapons);
    $('#statusFilter').addEventListener('change', renderWeapons);
    $('#weaponSearch').addEventListener('input', renderWeapons);

    $('#resetProgress').addEventListener('click', () => {
      if (!confirm(`确定清空「${MODE_LABELS[activeMode]}」的全部勾选进度吗？收藏不会被清除。`)) return;
      state.progress[activeMode] = {};
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
  }

  async function init() {
    try {
      const response = await fetch('/api/tool-station', { headers: { Accept: 'application/json' } });
      if (!response.ok) throw new Error('工具数据读取失败');
      const payload = await response.json();
      game = (payload.games || []).find(item => item.code === 'CODMW4') || (payload.games || [])[0];
      if (!game) throw new Error('没有找到 MW4 工具数据');
      if (!MODE_ORDER.includes(activeMode)) activeMode = MODE_ORDER[0];
      $('#classFilter').innerHTML = '<option value="">全部类型</option>' + [...new Set((game.weapons || []).map(item => item.weaponType).filter(Boolean))]
        .map(type => `<option value="${esc(type)}">${esc(type)}</option>`).join('');
      bindEvents();
      render();
      publishQuoteSnapshot();
    } catch (error) {
      status = { className: 'error', complete: true };
      const node = $('#trackerStatus');
      node.classList.remove('hidden');
      node.textContent = error.message || '工具数据读取失败，请刷新后重试。';
    }
  }

  document.addEventListener('DOMContentLoaded', init);
})();
