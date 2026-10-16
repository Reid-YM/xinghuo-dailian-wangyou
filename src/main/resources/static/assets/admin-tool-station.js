(() => {
  const csrf = document.querySelector('meta[name=_csrf]')?.content;
  const csrfHeader = document.querySelector('meta[name=_csrf_header]')?.content;
  const $ = (s, r = document) => r.querySelector(s);
  const $$ = (s, r = document) => [...r.querySelectorAll(s)];
  const esc = value => String(value ?? '').replace(/[&<>'"]/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', "'": '&#39;', '"': '&quot;' }[c]));
  const lines = value => String(value || '').split(/\n+/).map(x => x.trim()).filter(Boolean);
  const numberOrNull = value => value === '' || value == null ? null : Number(value);
  const nativeMode=document.body.dataset.xingweiTools==='true';
  let canManage=!nativeMode,toolRevision=null,mutationBusy=false,dirty=false;
  let config = null;
  let game = null;
  let weapon = null;

  async function api(url, opt = {}) {
    const method=(opt.method||'GET').toUpperCase(),mutation=!['GET','HEAD'].includes(method);
    if(nativeMode){url=url.replace(/^\/api\/admin\/tool-station/, '/api/admin/xingwei/tool-station');if(mutation){if(!canManage)throw new Error('当前只能查看工具站资料');if(mutationBusy)throw new Error('正在保存，请稍候');}}
    const isForm = opt.body instanceof FormData;
    const headers = { ...(opt.body && !isForm ? { 'Content-Type': 'application/json' } : {}), ...(opt.headers || {}) };
    if (csrf && csrfHeader && mutation) headers[csrfHeader] = csrf;
    if(nativeMode&&mutation&&!url.endsWith('/image')){
      if(!toolRevision)throw new Error('请先读取工具站当前版本');
      const reason=prompt('请填写本次修改说明（最多500字）');if(!reason?.trim())throw new Error('已取消，尚未写入资料');if(reason.length>500)throw new Error('修改说明不能超过500字');
      headers['If-Match']=toolRevision;url+='?reason='+encodeURIComponent(reason.trim());
    }
    if(nativeMode&&mutation)mutationBusy=true;
    try{
      const response = await fetch(url, { ...opt, headers, credentials:'same-origin',cache:'no-store' });
      let payload = null;try { payload = await response.json(); } catch {}
      if(response.status===401||(!nativeMode&&response.status===403)){location.href='/admin/login';throw new Error('登录已失效，请重新登录');}
      if(!response.ok)throw new Error(payload?.message||'请求失败');
      const revision=response.headers?.get?.('ETag');if(nativeMode&&revision)toolRevision=revision;
      return payload;
    }finally{if(nativeMode&&mutation)mutationBusy=false;}
  }

  function applyPermissions(){
    if(!nativeMode)return;
    $$('#toolStationForm input,#toolStationForm select,#toolStationForm textarea,#toolStationForm button,#toolStationSaveAll,#toolStationReset').forEach(field=>field.disabled=!canManage);
    const status=$('#toolStationAccessState');if(status)status.textContent=canManage?'可编辑全站工具站资料，写入时核对版本和修改说明。':'当前只读，可选择武器查看资料。';
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
    applyPermissions();
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

  function renderMissingWeaponState(message) {
    weapon = null;
    renderWeaponList();
    const title = $('#toolStationWeaponTitle');
    if (title) title.textContent = '暂无可编辑武器';
    const text = `<div class="empty">${esc(message || '当前游戏没有武器资料。')}</div>`;
    const modes = $('#toolStationModeImages');
    const camos = $('#toolStationCamos');
    const online = $('#toolStationOnlineCamos');
    if (modes) modes.innerHTML = text;
    if (camos) camos.innerHTML = text;
    if (online) online.innerHTML = text;
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
    if(nativeMode&&mutationBusy)throw new Error('正在保存，请稍候');
    applyWeaponForm();
    const selectedCode=weapon?.code;
    const body=JSON.stringify(config),controls=$$('#toolStationForm input,#toolStationForm textarea,#toolStationForm select,#toolStationForm button,#toolStationSaveAll,#toolStationReset,#toolStationRefresh,[data-tool-weapon]');
    const disabled=controls.map(control=>control.disabled);controls.forEach(control=>control.disabled=true);
    try{
      config=await api('/api/admin/tool-station',{method:'PUT',body});
      game=config.games?.find(item=>item.code==='CODMW4')||config.games?.[0];
      weapon=(game?.weapons||[]).find(item=>item.code===selectedCode)||game?.weapons?.[0];
      dirty=false;renderWeaponList();fillWeaponForm();toast('工具站数据已保存');
    }finally{controls.forEach((control,index)=>control.disabled=disabled[index]);applyPermissions();}
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
    if (!game) {
      renderMissingWeaponState('没有可编辑的游戏版本，可使用“恢复内置”恢复默认资料。');
      throw new Error('没有可编辑的游戏版本');
    }
    const firstWeapon = (game.weapons || [])[0];
    if (!firstWeapon) {
      renderMissingWeaponState('当前游戏没有武器资料，可使用“恢复内置”恢复默认资料。');
      throw new Error('当前游戏没有可编辑的武器资料');
    }
    selectWeapon(firstWeapon.code);
  }

  document.addEventListener('click', async event => {
    if (event.target.closest('[data-nav="toolStation"]')) {
      setTimeout(() => { if (!config) load().catch(e => toast(e.message, 'error')); }, 0);
      return;
    }
    const item = event.target.closest('[data-tool-weapon]');
    if(item){if(nativeMode&&dirty&&!confirm('当前武器修改尚未保存，确认放弃并切换？'))return;dirty=false;return selectWeapon(item.dataset.toolWeapon);}
    if(event.target.closest('#toolStationRefresh')){if(nativeMode&&dirty&&!confirm('有未保存修改，确认重新读取并放弃这些修改？'))return;dirty=false;return boot().then(() => toast('数据已刷新')).catch(e=>{if(nativeMode&&$('#toolStationAccessState'))$('#toolStationAccessState').textContent=e.message;toast(e.message,'error');});}
    if (event.target.closest('#toolStationSaveAll')) return saveData().catch(e => toast(e.message, 'error'));
    if (event.target.closest('#toolStationReset')) {
      if (!confirm('恢复内置工具站数据？当前后台修改会被覆盖。')) return;
      try { config = await api('/api/admin/tool-station/reset', { method: 'POST' }); dirty=false; await load(); toast('已恢复内置工具站数据'); }
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
  async function boot(){if(nativeMode){const access=await api('/api/admin/xingwei/tool-station/access');canManage=access.canManage===true;applyPermissions();}return load();}
  if(nativeMode){boot().catch(error=>{applyPermissions();const status=$('#toolStationAccessState');if(status)status.textContent=error.message;});}
  $('#toolStationForm')?.addEventListener('input',()=>{dirty=true;});

  $('#toolStationForm')?.addEventListener('submit', event => {
    event.preventDefault();
    saveData().catch(e => toast(e.message, 'error'));
  });
})();

(() => {
  if(document.body.dataset.xingweiTools==='true')return;
  const loadScript = (selector, datasetName, src) => {
    if (document.querySelector(selector)) return;
    const script = document.createElement('script');
    script.src = src;
    script.dataset[datasetName] = '1';
    document.head.appendChild(script);
  };
  loadScript('script[data-admin-global-ui]', 'adminGlobalUi', '/assets/admin-global-ui.js?v=20261007g004');
  loadScript('script[data-admin-resilience]', 'adminResilience', '/assets/admin-resilience.js?v=20261007r001');
})();
