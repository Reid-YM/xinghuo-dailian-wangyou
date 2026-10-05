(() => {
  const $ = (selector, root = document) => root.querySelector(selector);
  const $$ = (selector, root = document) => [...root.querySelectorAll(selector)];
  const esc = value => String(value ?? '').replace(/[&<>'"]/g, char => ({
    '&': '&amp;', '<': '&lt;', '>': '&gt;', "'": '&#39;', '"': '&quot;'
  }[char]));

  const STORAGE_KEY = 'xinghuo-mw4-camo-tracker-v2';
  const QUOTE_KEY = 'xinghuo-cod-tool-quote-snapshot';
  const MASTERY_FALLBACK = ['MULTIPLAYER', 'WARZONE', 'DMZ'];

  // 数据同时保留英文名与中文名；界面按中文显示，英文名只用于搜索匹配。
  const zhName = entry => String(entry?.nameZh || entry?.name || '').trim();

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
        return parsed;
      }
    } catch (ignored) {}
    return { activeMode: null, progress: {}, favorites: [], updatedAt: null };
  }

  function saveState() {
    state.activeMode = activeMode;
    state.updatedAt = new Date().toISOString();
    try { localStorage.setItem(STORAGE_KEY, JSON.stringify(state)); } catch (ignored) {}
    updateSaveState();
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
      return `<article class="camo-tier-card">
        <div class="camo-tier-media"><img src="${esc(item.image || '')}" alt="${esc(zhName(item))} 迷彩" loading="lazy"></div>
        <div class="camo-tier-body">
          <span class="camo-tier-label">第 ${item.tier || index + 1} 层</span>
          <div class="camo-tier-title"><b>${esc(zhName(item))}</b><span class="mono">${done} / ${weapons}</span></div>
          <p>${esc(item.requirement || '条件暂未公布')}</p>
        </div>
      </article>`;
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
          <span class="camo-pyro-media"><img src="${esc(item.image || '')}" alt="${esc(zhName(item))} 迷彩" loading="lazy"></span>
          <span class="camo-pyro-body">
            <b>${esc(zhName(item))}</b>
            <small>${esc(item.requirement || '条件暂未公布')}</small>
          </span>
          <span class="camo-pyro-check">✓</span>
        </label>`;
      }).join('');
      return;
    }

    const stats = modeStats(mode);
    const weapons = (game.weapons || []).length;
    $('#modeHeadTitle').textContent = `${mode.shortName || mode.name || mode.code} 大师迷彩`;
    $('#modeHeadMeta').textContent = `${weightsDone(stats)} / ${weapons} 把武器完成任意一层`;
    $('#modeHeadFinal').textContent = mode.finalCamoZh ? `最终层级 · ${mode.finalCamoZh}` : '';
    $('#modeNote').textContent = mode.note || '';
    renderTierCards(mode);
  }

  function weightsDone(stats) {
    const mode = modeByCode(activeMode);
    if (!mode) return 0;
    return (game.weapons || []).filter(weapon => weaponStats(mode, weapon).done > 0).length;
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

  function renderCheckItem(mode, weapon, item) {
    const done = isDone(mode.code, weapon.code, item.code);
    return `<label class="camo-check-item ${done ? 'is-done' : ''}">
      <input type="checkbox" data-camo-item="${esc(item.code)}" data-weapon="${esc(weapon.code)}" ${done ? 'checked' : ''}>
      <span class="camo-check-box">✓</span>
      <span class="camo-check-copy">
        <strong>${esc(zhName(item))}</strong>
        <small>${esc(item.requirement || '条件暂未公布')}</small>
      </span>
    </label>`;
  }

  function renderWeapon(mode, weapon) {
    const stats = weaponStats(mode, weapon);
    const favorite = state.favorites.includes(weapon.code);
    const open = openWeapons.has(weapon.code);
    const label = zhName(weapon);
    const thumb = weapon.image
      ? `<img class="weapon-thumb" src="${esc(weapon.image)}" alt="" loading="lazy">`
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
          <span class="weapon-progress-mini ${stats.complete ? 'is-done' : ''}">${stats.done}/${stats.total}</span>
          <span class="weapon-chevron" aria-hidden="true">⌄</span>
        </button>
      </div>
      <div class="weapon-tracker-body">${stats.items.map(item => renderCheckItem(mode, weapon, item)).join('')}</div>
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

  function toggleItem(weaponCode, itemCode, checked) {
    buckets(activeMode);
    state.progress[activeMode][weaponCode] = state.progress[activeMode][weaponCode] || {};
    state.progress[activeMode][weaponCode][itemCode] = checked;
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
      const input = event.target.closest('[data-camo-item]');
      if (!input) return;
      toggleItem(input.dataset.weapon, input.dataset.camoItem, input.checked);
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
      if (!confirm(`确定清空「${label}」的全部勾选进度吗？收藏不会被清除。`)) return;
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
    } catch (error) {
      const node = $('#trackerStatus');
      node.classList.remove('hidden');
      node.textContent = error.message || '工具数据读取失败，请刷新后重试。';
    }
  }

  document.addEventListener('DOMContentLoaded', init);
})();
