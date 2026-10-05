(() => {
  const csrf = document.querySelector('meta[name=_csrf]')?.content;
  const csrfHeader = document.querySelector('meta[name=_csrf_header]')?.content;
  const $ = (s, r = document) => r.querySelector(s);
  const $$ = (s, r = document) => [...r.querySelectorAll(s)];
  const esc = value => String(value ?? '').replace(/[&<>'"]/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', "'": '&#39;', '"': '&quot;' }[c]));
  const lines = value => String(value || '').split(/\n+/).map(x => x.trim()).filter(Boolean);
  const numberOrNull = value => value === '' || value == null ? null : Number(value);
  let config = null;
  let game = null;
  let weapon = null;

  async function api(url, opt = {}) {
    const isForm = opt.body instanceof FormData;
    const headers = { ...(opt.body && !isForm ? { 'Content-Type': 'application/json' } : {}), ...(opt.headers || {}) };
    if (csrf && csrfHeader && !['GET', 'HEAD'].includes((opt.method || 'GET').toUpperCase())) headers[csrfHeader] = csrf;
    const response = await fetch(url, { ...opt, headers });
    let payload = null;
    try { payload = await response.json(); } catch {}
    if (!response.ok) throw new Error(payload?.message || '请求失败');
    return payload;
  }

  function toast(message, type = 'success') {
    const area = $('#toastArea');
    const item = document.createElement('div');
    item.className = `toast ${type === 'error' ? 'error' : ''}`;
    item.textContent = message;
    area?.appendChild(item);
    setTimeout(() => item.remove(), 2600);
  }

  function filteredWeapons() {
    const key = ($('#toolStationSearch')?.value || '').trim().toLowerCase();
    return (game?.weapons || []).filter(item => !key || [item.code, item.name, item.weaponType, item.unlockLabel].some(v => String(v || '').toLowerCase().includes(key)));
  }

  function renderWeaponList() {
    const list = filteredWeapons();
    $('#toolStationWeaponList').innerHTML = list.map(item => `<button class="tool-admin-weapon ${item.code === weapon?.code ? 'active' : ''}" type="button" data-tool-weapon="${esc(item.code)}"><b>${esc(item.name)}</b><small>${esc(item.weaponType || '')} · ${esc(item.unlockLabel || '')}</small></button>`).join('') || '<div class="empty">没有匹配武器</div>';
  }

  function setFormValue(name, value) {
    const field = $('#toolStationForm')?.elements?.[name];
    if (!field) return;
    if (field.type === 'checkbox') field.checked = !!value;
    else field.value = value ?? '';
  }

  function fillWeaponForm() {
    if (!weapon) return;
    $('#toolStationWeaponTitle').textContent = `${weapon.name} · ${weapon.code}`;
    setFormValue('weaponCode', weapon.code);
    setFormValue('code', weapon.code);
    setFormValue('name', weapon.name);
    setFormValue('weaponType', weapon.weaponType);
    setFormValue('unlockLabel', weapon.unlockLabel);
    setFormValue('unlockPlayerRank', weapon.unlockPlayerRank);
    setFormValue('maxLevel', weapon.maxLevel);
    setFormValue('levelingEnabled', weapon.levelingEnabled);
    setFormValue('gunsmith', weapon.gunsmith);
    setFormValue('note', weapon.note);
    setFormValue('apexAttachments', (weapon.apexAttachments || []).join('\n'));
    renderModeImages();
    renderWarzoneCamos();
    renderOnlineCamos();
  }

  function renderModeImages() {
    const modes = game?.camoTracks?.modes || {};
    const entries = [['WARZONE', '战区迷彩'], ['MULTIPLAYER', '多人迷彩'], ['DMZ', 'DMZ 迷彩']];
    $('#toolStationModeImages').innerHTML = entries.map(([code, label]) => {
      const image = modes[code]?.previewImage || '';
      return `<div class="tool-image-admin" data-mode-code="${esc(code)}"><label>${esc(label)}</label><input type="text" data-mode-image value="${esc(image)}" placeholder="图片 URL"/><div class="row-actions"><input type="file" accept="image/*" data-mode-file/><button class="btn small-btn" type="button" data-upload-mode>上传图片</button></div><div class="tool-image-preview">${image ? `<img src="${esc(image)}" alt="${esc(label)}预览">` : '<span>暂无图片</span>'}</div></div>`;
    }).join('');
  }

  function renderWarzoneCamos() {
    const items = weapon?.warzoneCamos || [];
    $('#toolStationCamos').innerHTML = items.length ? items.map((item, index) => `<div class="tool-camo-admin-item" data-camo-index="${index}"><div class="grid2"><div class="field"><label>迷彩名称</label><input data-camo-field="name" value="${esc(item.name || '')}"/></div><div class="field"><label>阶段</label><input type="number" min="1" data-camo-field="stage" value="${esc(item.stage || 1)}"/></div></div><div class="field"><label>解锁条件</label><textarea data-camo-field="requirement" maxlength="300">${esc(item.requirement || '')}</textarea></div></div>`).join('') : '<div class="empty">当前武器没有独立战区条件。</div>';
  }

  function renderOnlineCamos() {
    const sets = weapon?.onlineCamos || {};
    const rows = Object.entries(sets).flatMap(([mode, set]) => (set?.items || []).map((item, index) => ({ mode, title: set.title || mode, item, index })));
    $('#toolStationOnlineCamos').innerHTML = rows.length ? rows.map(row => `<div class="tool-online-admin-item" data-online-mode="${esc(row.mode)}" data-online-index="${row.index}"><div class="tool-admin-block-head"><div><b>${esc(row.title)} · ${esc(row.item.name || '')}</b><small>${esc(row.mode)}</small></div></div><div class="grid2"><div class="field"><label>迷彩名称</label><input data-online-field="name" value="${esc(row.item.name || '')}"/></div><div class="field"><label>图片 URL</label><input data-online-field="image" value="${esc(row.item.image || '')}"/></div></div><div class="field"><label>挑战条件</label><textarea data-online-field="challenge" maxlength="400">${esc(row.item.challenge || '')}</textarea></div><div class="row-actions"><input type="file" accept="image/*" data-online-file/><button class="btn small-btn" type="button" data-upload-online>上传迷彩图片</button></div></div>`).join('') : '<div class="empty">当前武器没有在线迷彩图片资料。</div>';
  }

  function selectWeapon(code) {
    weapon = (game?.weapons || []).find(item => item.code === code) || null;
    renderWeaponList();
    fillWeaponForm();
  }

  function applyWeaponForm() {
    if (!weapon) throw new Error('请先选择武器');
    const f = $('#toolStationForm');
    const name = f.name.value.trim();
    if (!name) throw new Error('武器名称不能为空');
    weapon.name = name;
    weapon.weaponType = f.weaponType.value.trim();
    weapon.unlockLabel = f.unlockLabel.value.trim();
    weapon.unlockPlayerRank = numberOrNull(f.unlockPlayerRank.value);
    weapon.maxLevel = Number(f.maxLevel.value || 1);
    weapon.levelingEnabled = f.levelingEnabled.checked;
    weapon.gunsmith = f.gunsmith.checked;
    weapon.note = f.note.value.trim();
    weapon.apexAttachments = lines(f.apexAttachments.value);
    $$('#toolStationCamos [data-camo-index]').forEach(row => {
      const index = Number(row.dataset.camoIndex);
      const item = weapon.warzoneCamos?.[index];
      if (!item) return;
      item.name = row.querySelector('[data-camo-field=name]').value.trim();
      item.stage = Number(row.querySelector('[data-camo-field=stage]').value || 1);
      item.requirement = row.querySelector('[data-camo-field=requirement]').value.trim();
    });
    $$('#toolStationOnlineCamos [data-online-mode]').forEach(row => {
      const set = weapon.onlineCamos?.[row.dataset.onlineMode];
      const item = set?.items?.[Number(row.dataset.onlineIndex)];
      if (!item) return;
      item.name = row.querySelector('[data-online-field=name]').value.trim();
      item.image = row.querySelector('[data-online-field=image]').value.trim();
      item.challenge = row.querySelector('[data-online-field=challenge]').value.trim();
    });
    $$('#toolStationModeImages [data-mode-code]').forEach(row => {
      const mode = game.camoTracks?.modes?.[row.dataset.modeCode];
      if (mode) mode.previewImage = row.querySelector('[data-mode-image]').value.trim();
    });
  }

  async function saveData() {
    applyWeaponForm();
    config = await api('/api/admin/tool-station', { method: 'PUT', body: JSON.stringify(config) });
    game = config.games?.find(item => item.code === 'CODMW4') || config.games?.[0];
    renderWeaponList();
    toast('工具站数据已保存');
  }

  async function uploadImage(file) {
    const fd = new FormData();
    fd.append('file', file);
    const out = await api('/api/admin/tool-station/image', { method: 'POST', body: fd });
    return out.url;
  }

  async function load() {
    config = await api('/api/admin/tool-station');
    game = config.games?.find(item => item.code === 'CODMW4') || config.games?.[0];
    if (!game) throw new Error('没有可编辑的游戏版本');
    selectWeapon((game.weapons || [])[0]?.code);
  }

  document.addEventListener('click', async event => {
    if (event.target.closest('[data-nav="toolStation"]')) {
      setTimeout(() => { if (!config) load().catch(e => toast(e.message, 'error')); }, 0);
      return;
    }
    const item = event.target.closest('[data-tool-weapon]');
    if (item) return selectWeapon(item.dataset.toolWeapon);
    if (event.target.closest('#toolStationRefresh')) return load().then(() => toast('数据已刷新')).catch(e => toast(e.message, 'error'));
    if (event.target.closest('#toolStationSaveAll')) return saveData().catch(e => toast(e.message, 'error'));
    if (event.target.closest('#toolStationReset')) {
      if (!confirm('恢复内置工具站数据？当前后台修改会被覆盖。')) return;
      try { config = await api('/api/admin/tool-station/reset', { method: 'POST' }); await load(); toast('已恢复内置工具站数据'); }
      catch (e) { toast(e.message, 'error'); }
      return;
    }
    const modeButton = event.target.closest('[data-upload-mode]');
    if (modeButton) {
      const row = modeButton.closest('[data-mode-code]');
      const file = row.querySelector('[data-mode-file]')?.files?.[0];
      if (!file) return toast('请先选择迷彩图片', 'error');
      try { const url = await uploadImage(file); row.querySelector('[data-mode-image]').value = url; await saveData(); renderModeImages(); }
      catch (e) { toast(e.message, 'error'); }
      return;
    }
    const onlineButton = event.target.closest('[data-upload-online]');
    if (onlineButton) {
      const row = onlineButton.closest('[data-online-mode]');
      const file = row.querySelector('[data-online-file]')?.files?.[0];
      if (!file) return toast('请先选择迷彩图片', 'error');
      try { const url = await uploadImage(file); row.querySelector('[data-online-field=image]').value = url; await saveData(); renderOnlineCamos(); }
      catch (e) { toast(e.message, 'error'); }
    }
  });

  $('#toolStationSearch')?.addEventListener('input', renderWeaponList);
  $('#toolStationForm')?.addEventListener('submit', event => {
    event.preventDefault();
    saveData().catch(e => toast(e.message, 'error'));
  });
})();