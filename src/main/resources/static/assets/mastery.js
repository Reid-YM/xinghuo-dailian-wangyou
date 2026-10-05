(() => {
  const $ = (selector, root = document) => root.querySelector(selector);
  const esc = value => String(value ?? '').replace(/[&<>'"]/g, char => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', "'": '&#39;', '"': '&quot;' }[char]));
  const clamp = (value, min, max) => Math.min(max, Math.max(min, Number(value) || 0));
  const STORAGE = 'xinghuo-cod-tool-v23r6';
  const LEGACY_R5_STORAGE = 'xinghuo-cod-tool-v23r5';
  const LEGACY_R4_STORAGE = 'xinghuo-mw4-helper-v23r4';
  let config = null;
  let currentGame = null;
  let currentWeapon = null;
  let selectedModeCode = 'WARZONE';
  let selectedGroupCode = 'BASE';

  const readStorage = key => {
    try { return JSON.parse(localStorage.getItem(key) || 'null'); } catch { return null; }
  };
  const state = readStorage(STORAGE) || readStorage(LEGACY_R5_STORAGE) || {};
  if (!state.games || typeof state.games !== 'object') state.games = {};
  if (!state.currentGameCode) state.currentGameCode = 'CODMW4';

  const save = () => {
    state.updatedAt = new Date().toISOString();
    localStorage.setItem(STORAGE, JSON.stringify(state));
    if (currentGame) publishSnapshot();
    updateSaveStatus();
  };

  const gameState = game => {
    if (!state.games[game.code]) state.games[game.code] = { playerLevel: 0, weapons: {} };
    const result = state.games[game.code];
    if (!result.weapons) result.weapons = {};
    if (!Number.isFinite(+result.playerLevel)) result.playerLevel = 0;
    return result;
  };

  const trackDefinition = (game, code) => game.camoTracks?.modes?.[code] || { code, name: code, status: 'placeholder', items: [] };
  const camoTracksForWeapon = (game, weapon) => {
    const definitions = game.camoTracks || {};
    const order = Array.isArray(definitions.order) && definitions.order.length ? definitions.order : ['WARZONE', 'MULTIPLAYER', 'DMZ'];
    return order.map(code => {
      const base = trackDefinition(game, code);
      const hasOfficialWarzone = code === 'WARZONE' && Array.isArray(weapon.warzoneCamos) && weapon.warzoneCamos.length;
      const items = hasOfficialWarzone ? weapon.warzoneCamos : (base.items || []);
      return {
        code,
        name: base.name || ({ WARZONE: '战区迷彩', MULTIPLAYER: '多人迷彩', DMZ: 'DMZ迷彩' }[code] || code),
        status: hasOfficialWarzone ? 'official' : (base.status || 'placeholder'),
        note: hasOfficialWarzone ? '' : (base.note || ''),
        previewImage: base.previewImage || '',
        previewAlt: base.previewAlt || `${base.name || code}预览图`,
        items
      };
    }).filter(track => track.items.length);
  };

  const weaponState = (game, weapon) => {
    const gameData = gameState(game);
    if (!gameData.weapons[weapon.code]) gameData.weapons[weapon.code] = { currentLevel: 0, camos: {} };
    const result = gameData.weapons[weapon.code];
    result.currentLevel = clamp(result.currentLevel, 0, weapon.maxLevel || 9999);
    if (!result.camos || typeof result.camos !== 'object') result.camos = {};
    camoTracksForWeapon(game, weapon).forEach(track => {
      if (!result.camos[track.code]) result.camos[track.code] = {};
      track.items.forEach(item => {
        if (typeof result.camos[track.code][item.code] !== 'boolean') result.camos[track.code][item.code] = false;
      });
    });
    return result;
  };

  const specialAccess = weapon => {
    if (weapon.specialAccess === true) return true;
    if (weapon.levelingEnabled || weapon.unlockPlayerRank != null) return false;
    return /限定|战役|活动|特殊/.test(String(weapon.unlockLabel || ''));
  };
  const unlockStatus = (game, weapon) => {
    if (specialAccess(weapon)) return 'special';
    if (weapon.unlockPlayerRank == null) return 'unlocked';
    return gameState(game).playerLevel >= weapon.unlockPlayerRank ? 'unlocked' : 'locked';
  };
  const typeShort = type => ({ '突击步枪': '突击', '冲锋枪': '冲锋', '狙击步枪': '狙击', '射手步枪': '射手', '霰弹枪': '霰弹', '轻机枪': '轻机', '手枪': '手枪', '发射器': '发射器', '近战': '近战' }[type] || type);
  const modeName = code => ({ MULTIPLAYER: '多人', DMZ: 'DMZ', WARZONE: '战区', ZOMBIES: '僵尸' }[code] || code);
  const itemChecked = (saved, modeCode, itemCode) => !!saved.camos?.[modeCode]?.[itemCode];
  const itemAvailable = (track, item, saved) => (item.requires || []).every(code => itemChecked(saved, track.code, code));
  const itemName = (track, code) => track.items.find(item => item.code === code)?.name || code;
  const clearDependents = (track, itemCode, saved) => {
    track.items.filter(item => (item.requires || []).includes(itemCode) && itemChecked(saved, track.code, item.code)).forEach(item => {
      saved.camos[track.code][item.code] = false;
      clearDependents(track, item.code, saved);
    });
  };

  function migrateLegacyMW4(game) {
    if (game.code !== 'CODMW4' || state.legacyMigratedV23R4) return;
    const old = readStorage(LEGACY_R4_STORAGE);
    if (old && typeof old === 'object') {
      const gameData = gameState(game);
      if (Number.isFinite(+old.playerLevel)) gameData.playerLevel = Math.max(+gameData.playerLevel || 0, +old.playerLevel || 0);
      for (const weapon of (game.weapons || [])) {
        const oldWeapon = old.weapons?.[weapon.code];
        if (!oldWeapon) continue;
        const saved = weaponState(game, weapon);
        saved.currentLevel = clamp(oldWeapon.currentLevel, 0, weapon.maxLevel || 9999);
      }
    }
    state.legacyMigratedV23R4 = true;
    localStorage.setItem(STORAGE, JSON.stringify(state));
  }

  function smartNextText(game, weapon, saved, unlock) {
    if (unlock === 'locked') return `先把玩家等级提升到 Lv.${weapon.unlockPlayerRank}，解锁 ${weapon.name}`;
    if (unlock === 'special') return `${weapon.name} 属于限定武器，请先确认当前可用条件`;
    if (weapon.levelingEnabled && weapon.maxLevel && saved.currentLevel < weapon.maxLevel) return `${weapon.name} 继续升到 Lv.${weapon.maxLevel}；迷彩可以同步勾选`;
    const orderedTracks = [...camoTracksForWeapon(game, weapon)];
    orderedTracks.sort((a, b) => (a.code === selectedModeCode ? -1 : b.code === selectedModeCode ? 1 : 0));
    for (const track of orderedTracks) {
      const next = track.items.find(item => !itemChecked(saved, track.code, item.code) && itemAvailable(track, item, saved));
      if (next) return `${track.name}：${next.name}${next.requirement ? ` · ${next.requirement}` : ''}`;
    }
    return `${weapon.name} 当前助手进度已全部完成`;
  }

  function hasSavedProgress(saved) {
    if (Number(saved.currentLevel) > 0) return true;
    return Object.values(saved.camos || {}).some(track => Object.values(track || {}).some(Boolean));
  }

  function updateSaveStatus() {
    const gameData = currentGame ? gameState(currentGame) : null;
    const touched = gameData ? Object.values(gameData.weapons || {}).filter(hasSavedProgress).length : 0;
    $('#localSaveText').textContent = '进度已自动保存';
    $('#localSaveDetail').textContent = currentGame ? `${currentGame.shortName} · ${touched} 把武器已记录` : '切换记录不会丢失';
  }

  function renderVersions() {
    const games = config.games || [];
    const singleGame = games.length === 1;
    $('#versionPanel').classList.toggle('hidden', singleGame);
    document.querySelector('.tool-flow')?.classList.toggle('single-game', singleGame);
    const weaponStep = $('#weaponPanel .panel-title > span');
    const progressStep = $('#progressPanel .panel-title > span');
    if (weaponStep) weaponStep.textContent = singleGame ? '01' : '02';
    if (progressStep) progressStep.textContent = singleGame ? '02' : '03';
    $('#gameSelect').innerHTML = games.map(game => `<option value="${esc(game.code)}">${esc(game.shortName)} · ${game.year}</option>`).join('');
    $('#gameSelect').value = currentGame.code;
    $('#versionList').innerHTML = games.map(game => `<button type="button" class="version-item ${game.code === currentGame.code ? 'active' : ''}" data-game="${esc(game.code)}"><span>${esc(game.year)}</span><b>${esc(game.shortName)}</b><small>${game.weapons?.length ? `${game.weapons.length} 把武器` : ''}</small></button>`).join('');
  }

  function renderCamoPreviews(game) {
    const box = $('#camoPreviewGrid');
    if (!box) return;
    const order = game.camoTracks?.order || [];
    box.innerHTML = order.map(code => {
      const mode = game.camoTracks?.modes?.[code] || { code, name: code };
      const image = mode.previewImage;
      const media = image
        ? `<img src="${esc(image)}" alt="${esc(mode.previewAlt || `${mode.name}预览图`)}" loading="lazy">`
        : `<div class="camo-preview-empty"><span class="camo-preview-icon">▧</span><b>${esc(mode.previewAlt || `${mode.name}预览图`)}</b><small>预览暂缺</small></div>`;
      return `<article class="camo-preview-card ${image ? '' : 'placeholder'}" data-preview-mode="${esc(code)}"><div class="camo-preview-media">${media}</div><div class="camo-preview-caption"><b>${esc(mode.name || code)}</b><span>MW4</span></div></article>`;
    }).join('');
  }

  function applyGameVisual() {
    const game = currentGame;
    $('#gameArtwork').style.backgroundImage = `url("${String(game.artworkUrl || '').replace(/"/g, '')}")`;
    document.documentElement.style.setProperty('--tool-accent', game.accent || '#f4c934');
    $('#heroGameName').textContent = `${game.shortName} · ${game.year}`;
    $('#heroDataStatus').textContent = game.dataStatus;
    $('#officialGameLink').href = game.officialSource;
    $('#weaponPanelHint').textContent = game.weapons?.length ? `${game.shortName} · ${game.weapons.length} 把武器` : game.shortName;
    $('#playerLevelRow').classList.toggle('hidden', game.playerLevelEnabled === false);
    $('#quoteWithProgress').href = `/quote?toolProgress=1&gameCode=${encodeURIComponent(game.code)}`;
    $('#mobileQuoteWithProgress').href = $('#quoteWithProgress').href;
  }

  function renderWeaponFilters() {
    const types = [...new Set((currentGame.weapons || []).map(weapon => weapon.weaponType).filter(Boolean))];
    const current = $('#weaponType').value;
    $('#weaponType').innerHTML = '<option value="">全部类型</option>' + types.map(type => `<option value="${esc(type)}">${esc(type)}</option>`).join('');
    if (types.includes(current)) $('#weaponType').value = current;
  }

  function filteredWeapons() {
    const query = $('#weaponSearch').value.trim().toLowerCase();
    const type = $('#weaponType').value;
    return (currentGame.weapons || []).filter(weapon => (!type || weapon.weaponType === type) && (!query || [weapon.name, weapon.weaponType, weapon.unlockLabel, ...(weapon.apexAttachments || [])].join(' ').toLowerCase().includes(query)));
  }

  function weaponProgressPercent(game, weapon) {
    const saved = weaponState(game, weapon);
    const parts = [];
    if (weapon.levelingEnabled && weapon.maxLevel) parts.push(clamp(saved.currentLevel / weapon.maxLevel * 100, 0, 100));
    camoTracksForWeapon(game, weapon).forEach(track => {
      const done = track.items.filter(item => itemChecked(saved, track.code, item.code)).length;
      parts.push(done / track.items.length * 100);
    });
    return parts.length ? Math.round(parts.reduce((total, value) => total + value, 0) / parts.length) : 0;
  }

  function renderWeapons() {
    const hasWeapons = (currentGame.weapons || []).length > 0;
    $('#versionPending').classList.toggle('hidden', hasWeapons);
    $('#weaponList').classList.toggle('hidden', !hasWeapons);
    if (!hasWeapons) {
      $('#weaponList').innerHTML = '';
      currentWeapon = null;
      renderProgress();
      return;
    }
    const rows = filteredWeapons();
    $('#weaponList').innerHTML = rows.map(weapon => {
      const unlock = unlockStatus(currentGame, weapon);
      const saved = weaponState(currentGame, weapon);
      const progress = weaponProgressPercent(currentGame, weapon);
      const badge = unlock === 'unlocked' ? '可用' : unlock === 'locked' ? `Lv.${weapon.unlockPlayerRank}` : '限定';
      const levelText = weapon.levelingEnabled ? `Lv.${saved.currentLevel}/${weapon.maxLevel}` : (weapon.warzoneCamos?.length ? '战区迷彩' : '进度');
      return `<button type="button" class="weapon-row ${currentWeapon?.code === weapon.code ? 'active' : ''}" data-weapon="${esc(weapon.code)}"><span class="weapon-mark">${esc(typeShort(weapon.weaponType))}</span><span class="weapon-row-main"><b>${esc(weapon.name)}</b><small>${esc(weapon.unlockLabel)}</small></span><span class="weapon-row-side"><em class="unlock-mini ${unlock}">${badge}</em><small>${levelText} · ${progress}%</small></span><i>›</i></button>`;
    }).join('') || '<div class="list-empty">没有符合筛选的武器</div>';
  }

  function stage(title, done, sub, auto = false) {
    return `<div class="rail-stage ${done ? 'done' : ''}"><span>${done ? '✓' : '○'}</span><div><b>${esc(title)}</b><small>${esc(sub)}</small></div>${auto ? '<em>自动</em>' : ''}</div>`;
  }

  function camoGroupsForMode(game, track) {
    const configured = game.camoTracks?.groups;
    if (Array.isArray(configured) && configured.length) {
      return configured.map(group => ({ ...group, items: (group.items || []).map(code => track.items.find(item => item.code === code)).filter(Boolean) })).filter(group => group.items.length);
    }
    const size = Math.ceil(track.items.length / 3) || 1;
    return Array.from({ length: 3 }, (_, index) => ({ code: 'G' + (index + 1), name: ['基础迷彩', '特殊迷彩', '大师迷彩'][index], description: '迷彩阶段', items: track.items.slice(index * size, (index + 1) * size) })).filter(group => group.items.length);
  }

  function camoGroupStats(group, track, saved) {
    const done = group.items.filter(item => itemChecked(saved, track.code, item.code)).length;
    return { done, total: group.items.length, percent: group.items.length ? Math.round(done / group.items.length * 100) : 0 };
  }

  function renderCamoItem(track, item, saved) {
    const checked = itemChecked(saved, track.code, item.code);
    const available = itemAvailable(track, item, saved);
    const locked = !checked && !available;
    const prerequisiteNames = (item.requires || []).map(code => itemName(track, code)).join('、');
    const prerequisiteLine = prerequisiteNames ? `<small class="camo-prereq ${locked ? 'locked' : ''}">${locked ? '需先完成' : '前置'}：${esc(prerequisiteNames)}</small>` : '';
    return `<label class="camo-check ${locked ? 'locked' : ''}" title="${locked ? `需先完成：${esc(prerequisiteNames)}` : ''}"><input type="checkbox" data-camo-mode="${esc(track.code)}" data-camo-item="${esc(item.code)}" ${checked ? 'checked' : ''} ${locked ? 'disabled' : ''}><span class="camo-box">✓</span><span class="camo-copy"><b>${esc(item.name)}<em class="camo-stage">阶段 ${esc(item.stage || 1)}</em></b><small class="camo-condition"><em>条件</em>${esc(item.requirement || '条件暂未提供')}</small>${prerequisiteLine}</span></label>`;
  }

  function renderCamoModeWorkspace(game, weapon, saved) {
    const tracks = camoTracksForWeapon(game, weapon);
    if (!tracks.length) return '';
    if (!tracks.some(track => track.code === selectedModeCode)) selectedModeCode = tracks[0].code;
    const track = tracks.find(item => item.code === selectedModeCode) || tracks[0];
    const groups = camoGroupsForMode(game, track);
    if (!groups.some(group => group.code === selectedGroupCode)) selectedGroupCode = groups[0]?.code || '';
    const group = groups.find(item => item.code === selectedGroupCode) || groups[0];
    const statusLabel = track.status === 'official' ? '数据已整理' : '条件整理中';
    const tabs = tracks.map(item => `<button type="button" class="camo-mode-tab ${item.code === track.code ? 'active' : ''}" data-mode-tab="${esc(item.code)}">${esc(item.name.replace('迷彩', ''))}</button>`).join('');
    const preview = track.previewImage
      ? `<div class="camo-mode-preview-art has-image"><img src="${esc(track.previewImage)}" alt="${esc(track.previewAlt)}" loading="lazy"></div>`
      : `<div class="camo-mode-preview-art"><span class="camo-preview-icon">▧</span><b>${esc(track.previewAlt)}</b><small>模式预览暂缺</small></div>`;
    const cards = groups.map(item => {
      const stats = camoGroupStats(item, track, saved);
      return `<button type="button" class="camo-group-card ${item.code === group.code ? 'active' : ''}" data-group-tab="${esc(item.code)}"><span class="camo-group-head"><b>${esc(item.name)}</b><em>${stats.done} / ${stats.total}</em></span><small>${esc(item.description || '')}</small><span class="camo-group-bar"><i style="width:${stats.percent}%"></i></span><span class="camo-group-items">${item.items.map(entry => `<em class="${itemChecked(saved, track.code, entry.code) ? 'done' : ''}">${esc(entry.name)}</em>`).join('')}</span></button>`;
    }).join('');
    const stats = camoGroupStats(group, track, saved);
    const onlineSet = weapon.onlineCamos?.[track.code];
    const onlineStrip = onlineSet?.items?.length
      ? `<section class="online-camo-strip"><div class="online-camo-head"><div><b>${esc(onlineSet.title || '在线迷彩资料')}</b><small>挑战条件与外观</small></div></div><div class="online-camo-grid">${onlineSet.items.map(item => `<article class="online-camo-card"><div class="online-camo-media"><img src="${esc(item.image)}" alt="${esc(item.name)}" loading="eager"></div><div class="online-camo-copy"><b>${esc(item.name)}</b><small>${esc(item.challenge || '')}</small></div></article>`).join('')}</div></section>`
      : '';
    return `<div class="camo-mode-workspace" data-mode="${esc(track.code)}"><div class="camo-mode-tabs">${tabs}</div><div class="camo-scheme-one"><div class="camo-mode-preview">${preview}<div class="camo-mode-preview-foot"><b>${esc(track.name)}</b><span><em class="data-badge ${track.status}">${statusLabel}</em>${esc(track.note)}</span></div></div><div class="camo-group-cards">${cards}</div></div>${onlineStrip}<section class="camo-detail-panel"><div class="camo-detail-head"><div><b>${esc(group.name)}条件</b><small>勾选后自动保存进度</small></div><span>${stats.done} / ${stats.total}</span></div><div class="camo-grid">${group.items.map(item => renderCamoItem(track, item, saved)).join('')}</div></section></div>`;
  }

  function renderProgress() {
    if (!currentWeapon) {
      $('#progressEmpty').classList.remove('hidden');
      $('#progressContent').classList.add('hidden');
      return;
    }
    const game = currentGame;
    const weapon = currentWeapon;
    const saved = weaponState(game, weapon);
    const unlock = unlockStatus(game, weapon);
    const gameData = gameState(game);
    $('#progressEmpty').classList.add('hidden');
    $('#progressContent').classList.remove('hidden');
    $('#selectedWeaponType').textContent = weapon.weaponType;
    $('#selectedWeaponName').textContent = weapon.name;
    $('#selectedUnlockText').textContent = weapon.unlockLabel + (weapon.note ? ` · ${weapon.note}` : '');
    const badge = $('#selectedUnlockBadge');
    badge.className = `unlock-pill ${unlock}`;
    badge.textContent = unlock === 'unlocked' ? '已解锁' : unlock === 'locked' ? `需 Lv.${weapon.unlockPlayerRank}` : '限定条件';
    const levelCard = $('#levelCard');
    levelCard.classList.toggle('disabled', !weapon.levelingEnabled);
    $('#weaponLevelRange').disabled = !weapon.levelingEnabled;
    $('#weaponLevelInput').disabled = !weapon.levelingEnabled;
    $('#markMaxLevel').disabled = !weapon.levelingEnabled;
    $('#weaponLevelRange').max = weapon.maxLevel || 1;
    $('#weaponLevelInput').max = weapon.maxLevel || 9999;
    $('#weaponLevelRange').value = saved.currentLevel;
    $('#weaponLevelInput').value = saved.currentLevel;
    $('#levelReadout').textContent = weapon.levelingEnabled ? `Lv.${saved.currentLevel} / ${weapon.maxLevel}` : '无需武器等级';
    const apex = (weapon.apexAttachments || []).join(' / ') || '无 Apex';
    const unlocked = unlock === 'unlocked';
    const maxed = weapon.levelingEnabled ? saved.currentLevel >= weapon.maxLevel : unlock !== 'locked';
    $('#smartNext').querySelector('b').textContent = smartNextText(game, weapon, saved, unlock);
    const stages = [
      stage('武器解锁', unlocked, unlock === 'locked' ? `玩家 Lv.${gameData.playerLevel} / ${weapon.unlockPlayerRank}` : weapon.unlockLabel, true),
      weapon.levelingEnabled ? stage('武器等级', maxed, `Lv.${saved.currentLevel} / ${weapon.maxLevel}`) : stage('战区资料', !!weapon.warzoneCamos?.length, weapon.warzoneCamos?.length ? `${weapon.warzoneCamos.length} 项条件` : '条件暂缺'),
      weapon.warzoneCamos?.length ? stage('战区迷彩', camoTracksForWeapon(game, weapon).find(track => track.code === 'WARZONE')?.items.every(item => itemChecked(saved, 'WARZONE', item.code)) || false, '独立勾选并保存') : stage('Apex', maxed && !!(weapon.apexAttachments || []).length, apex)
    ];
    $('#progressRail').innerHTML = stages.join('<div class="rail-arrow">→</div>');
    $('#modeProgress').innerHTML = renderCamoModeWorkspace(game, weapon, saved);

    $('#progressNote').innerHTML = '<b>进度说明</b><span>勾选后会自动保存在当前浏览器，可随时继续。</span>';
  }

  function selectGame(code, scroll = false) {
    const game = (config.games || []).find(item => item.code === code) || config.games[0];
    if (!game) return;
    currentGame = game;
    state.currentGameCode = game.code;
    currentWeapon = null;
    gameState(game);
    migrateLegacyMW4(game);
    save();
    renderVersions();
    applyGameVisual();
    renderWeaponFilters();
    $('#playerLevelInput').value = gameState(game).playerLevel;
    renderWeapons();
    renderProgress();
    updateSaveStatus();
    if (scroll) scrollPanel('#weaponPanel');
  }

  function selectWeapon(code, scroll = false) {
    currentWeapon = (currentGame.weapons || []).find(item => item.code === code) || null;
    renderWeapons();
    renderProgress();
    if (scroll) scrollPanel('#progressPanel');
  }

  function scrollPanel(selector) {
    if (window.matchMedia('(max-width:900px)').matches) $(selector)?.scrollIntoView({ behavior: 'smooth', block: 'nearest', inline: 'start' });
  }

  function syncLevel(value) {
    if (!currentWeapon || !currentWeapon.levelingEnabled) return;
    const saved = weaponState(currentGame, currentWeapon);
    const next = clamp(value, 0, currentWeapon.maxLevel);
    saved.currentLevel = next;
    $('#weaponLevelRange').value = next;
    $('#weaponLevelInput').value = next;
    save();
    renderWeapons();
    renderProgress();
  }

  function compactSnapshot() {
    const game = currentGame;
    const gameData = gameState(game);
    const parts = [];
    for (const weapon of (game.weapons || [])) {
      const saved = gameData.weapons[weapon.code];
      if (!saved) continue;
      const trackParts = [];
      let touched = Number(saved.currentLevel) > 0;
      for (const track of camoTracksForWeapon(game, weapon)) {
        const done = track.items.filter(item => itemChecked(saved, track.code, item.code)).length;
        if (done) touched = true;
        trackParts.push(`${track.code.slice(0, 2)}${done}/${track.items.length}`);
      }
      if (touched) parts.push(`${weapon.code}:L${saved.currentLevel}${trackParts.length ? ',' + trackParts.join(',') : ''}`);
    }
    return `工具站|${game.code}|PL${gameData.playerLevel}|${parts.join(';')}`.slice(0, 490);
  }

  function publishSnapshot() {
    localStorage.setItem('xinghuo-cod-tool-quote-snapshot', JSON.stringify({ gameCode: currentGame.code, gameName: currentGame.name, shortName: currentGame.shortName, summary: compactSnapshot(), updatedAt: new Date().toISOString() }));
  }

  async function load() {
    try {
      let response = await fetch('/api/tool-station', { cache: 'no-store' });
      if (!response.ok) response = await fetch('/assets/tool-station-games.json?v=20260927v21', { cache: 'no-store' });
      if (!response.ok) throw new Error('工具站配置读取失败');
      config = await response.json();
      const wanted = new URLSearchParams(location.search).get('game') || state.currentGameCode || config.defaultGameCode;
      selectGame(wanted);
      const wantedWeapon = new URLSearchParams(location.search).get('weapon');
      if (wantedWeapon && (currentGame.weapons || []).some(weapon => weapon.code === wantedWeapon)) selectWeapon(wantedWeapon);
      $('#playerLevelInput').value = gameState(currentGame).playerLevel;
    } catch (error) {
      $('#weaponList').innerHTML = `<div class="list-empty">${esc(error.message || '读取失败')}</div>`;
    }
  }

  $('#gameSelect').addEventListener('change', event => selectGame(event.target.value, true));
  $('#versionList').addEventListener('click', event => { const button = event.target.closest('[data-game]'); if (button) selectGame(button.dataset.game, true); });
  $('#playerLevelInput').addEventListener('input', event => { if (!currentGame) return; gameState(currentGame).playerLevel = clamp(event.target.value, 0, 9999); save(); renderWeapons(); if (currentWeapon) renderProgress(); });
  $('#weaponSearch').addEventListener('input', renderWeapons);
  $('#weaponType').addEventListener('change', renderWeapons);
  $('#weaponList').addEventListener('click', event => { const button = event.target.closest('[data-weapon]'); if (button) selectWeapon(button.dataset.weapon, true); });
  $('#weaponLevelRange').addEventListener('input', event => syncLevel(event.target.value));
  $('#weaponLevelInput').addEventListener('input', event => syncLevel(event.target.value));
  $('#markMaxLevel').addEventListener('click', () => syncLevel(currentWeapon?.maxLevel || 0));
  $('#modeProgress').addEventListener('click', event => {
    const modeButton = event.target.closest('[data-mode-tab]');
    if (modeButton) { selectedModeCode = modeButton.dataset.modeTab; selectedGroupCode = 'BASE'; renderProgress(); return; }
    const groupButton = event.target.closest('[data-group-tab]');
    if (groupButton) { selectedGroupCode = groupButton.dataset.groupTab; renderProgress(); }
  });
  $('#modeProgress').addEventListener('change', event => {
    if (!currentWeapon || !event.target.matches('[data-camo-item]')) return;
    const saved = weaponState(currentGame, currentWeapon);
    const modeCode = event.target.dataset.camoMode;
    const itemCode = event.target.dataset.camoItem;
    if (!saved.camos[modeCode]) saved.camos[modeCode] = {};
    const track = camoTracksForWeapon(currentGame, currentWeapon).find(item => item.code === modeCode);
    const item = track?.items.find(entry => entry.code === itemCode);
    if (!track || !item) return;
    if (event.target.checked && !itemAvailable(track, item, saved)) return;
    saved.camos[modeCode][itemCode] = event.target.checked;
    if (!event.target.checked) clearDependents(track, itemCode, saved);
    save();
    renderWeapons();
    renderProgress();
  });
  $('#clearCurrentGame').addEventListener('click', () => {
    if (!currentGame || !confirm(`清空 ${currentGame.shortName} 在当前浏览器保存的全部进度？`)) return;
    state.games[currentGame.code] = { playerLevel: 0, weapons: {} };
    save();
    currentWeapon = null;
    $('#playerLevelInput').value = 0;
    renderWeapons();
    renderProgress();
  });
  $('#quoteWithProgress').addEventListener('click', publishSnapshot);
  $('#mobileQuoteWithProgress').addEventListener('click', publishSnapshot);
  load();
})();
