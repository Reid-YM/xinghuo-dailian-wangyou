(() => {
  'use strict';

  const status = document.getElementById('xcatStatus');
  const manageMode = document.getElementById('xcatManageMode');
  const message = document.getElementById('xcatMessage');
  const search = document.getElementById('xcatSearch');
  const gameFilter = document.getElementById('xcatGameFilter');
  const stateFilter = document.getElementById('xcatStateFilter');
  const refresh = document.getElementById('xcatRefresh');
  const serviceRows = document.getElementById('xcatServiceRows');
  const gameRows = document.getElementById('xcatGameRows');
  const categoryRows = document.getElementById('xcatCategoryRows');
  const editDialog = document.getElementById('xcatEditDialog');
  const editForm = document.getElementById('xcatEditForm');
  const editMeta = document.getElementById('xcatEditMeta');
  const editName = document.getElementById('xcatEditName');
  const editShortDesc = document.getElementById('xcatEditShortDesc');
  const editPriceMode = document.getElementById('xcatEditPriceMode');
  const editPriceFixed = document.getElementById('xcatEditPriceFixed');
  const editPriceMin = document.getElementById('xcatEditPriceMin');
  const editPriceMax = document.getElementById('xcatEditPriceMax');
  const editDuration = document.getElementById('xcatEditDuration');
  const editSortOrder = document.getElementById('xcatEditSortOrder');
  const editPriority = document.getElementById('xcatEditPriority');
  const editReason = document.getElementById('xcatEditReason');
  const editSave = document.getElementById('xcatEditSave');
  const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute('content') || '';
  const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content') || '';

  let snapshot = null;
  let access = null;
  let canManage = false;
  let currentEdit = null;

  function normalize(value) { return String(value ?? '').toLocaleLowerCase('zh-CN').trim(); }
  function append(parent, tag, text, className = '') { const node=document.createElement(tag); if(className)node.className=className; node.textContent=text==null||text===''?'—':String(text); parent.appendChild(node); return node; }
  function cell(text, className = '') { const td=document.createElement('td'); if(className)td.className=className; td.textContent=text==null||text===''?'—':String(text); return td; }
  function setMessage(text, kind='') { message.textContent=text; message.classList.remove('ok','error'); if(kind)message.classList.add(kind); }
  function emptyRow(body, colspan, text) { body.replaceChildren(); const tr=document.createElement('tr'); tr.className='xcat-empty'; const td=document.createElement('td'); td.colSpan=colspan; td.textContent=text; tr.appendChild(td); body.appendChild(tr); }
  function setStatus(node, text, ok=false) { node.classList.toggle('ok', ok); node.classList.remove('error'); const label=node.querySelector('span'); if(label)label.textContent=text; }

  function priceLabel(item) {
    const mode=String(item.priceMode||'').toUpperCase();
    if(mode==='FIXED' && item.priceFixed!=null) return `¥${item.priceFixed}`;
    if(mode==='RANGE' && item.priceMin!=null && item.priceMax!=null) return `¥${item.priceMin} – ¥${item.priceMax}`;
    return '询价';
  }

  function serviceSearchText(item) { return normalize([item.name,item.slug,item.gameCode,item.category,item.status,item.shortDesc].join(' ')); }
  function filteredServices() {
    const query=normalize(search.value); const game=gameFilter.value; const state=stateFilter.value;
    return (snapshot?.services||[]).filter((item) => {
      if(query && !serviceSearchText(item).includes(query)) return false;
      if(game && item.gameCode!==game) return false;
      if(state==='active' && !item.active) return false;
      if(state==='inactive' && item.active) return false;
      if(state==='offline' && String(item.status).toUpperCase()!=='OFFLINE') return false;
      return true;
    });
  }

  function actionButton(label, action, id, className='xw-button xw-button-ghost') {
    const button=document.createElement('button');
    button.type='button'; button.className=className; button.textContent=label;
    button.dataset.action=action; button.dataset.id=String(id);
    return button;
  }

  function renderServices() {
    const rows=filteredServices(); serviceRows.replaceChildren();
    if(!rows.length){emptyRow(serviceRows,7,'当前筛选条件没有服务项目');return;}
    rows.forEach((item)=>{
      const tr=document.createElement('tr');
      const serviceCell=document.createElement('td'); append(serviceCell,'strong',item.name,'xcat-primary'); append(serviceCell,'small',item.slug,'xcat-secondary'); tr.appendChild(serviceCell);
      const gameCell=document.createElement('td'); append(gameCell,'span',item.gameCode,'xcat-primary'); append(gameCell,'small',item.category,'xcat-secondary'); tr.appendChild(gameCell);
      const priceCell=document.createElement('td'); append(priceCell,'span',priceLabel(item),'xcat-primary'); append(priceCell,'small',item.priceMode,'xcat-secondary'); tr.appendChild(priceCell);
      tr.appendChild(cell(item.durationText));
      const statusCell=document.createElement('td'); const badge=append(statusCell,'span',`${item.status} · ${item.active?'启用':'停用'}`,`xcat-status ${item.active?'on':'off'}`); badge.title=item.shortDesc||''; tr.appendChild(statusCell);
      const orderCell=document.createElement('td'); append(orderCell,'span',`展示 ${item.sortOrder}`,'xcat-primary'); append(orderCell,'small',`优先级 ${item.manualPriority} · v${item.version}`,'xcat-secondary'); tr.appendChild(orderCell);
      const actionCell=document.createElement('td'); actionCell.className='xcat-row-actions';
      if(canManage){
        actionCell.appendChild(actionButton('编辑','edit',item.id));
        const offline=String(item.status).toUpperCase()==='OFFLINE'||!item.active;
        actionCell.appendChild(actionButton(offline?'恢复':'下线',offline?'restore':'offline',item.id));
      }else{
        append(actionCell,'span','只读','xcat-readonly');
      }
      tr.appendChild(actionCell);
      serviceRows.appendChild(tr);
    });
  }

  function renderGames() {
    gameRows.replaceChildren(); const rows=snapshot?.games||[];
    if(!rows.length){emptyRow(gameRows,4,'暂无游戏资料');return;}
    rows.forEach((item)=>{const tr=document.createElement('tr'); const name=document.createElement('td'); append(name,'strong',item.name,'xcat-primary'); append(name,'small',item.code,'xcat-secondary'); tr.appendChild(name); tr.appendChild(cell(`${item.businessStatus||'—'} · ${item.active?'启用':'停用'}`)); tr.appendChild(cell(`${item.categoryCount??0} / ${item.serviceCount??0}`)); tr.appendChild(cell((item.platformCodes||[]).join('、')||'—')); gameRows.appendChild(tr);});
  }

  function renderCategories() {
    categoryRows.replaceChildren(); const rows=snapshot?.categories||[];
    if(!rows.length){emptyRow(categoryRows,4,'暂无分类资料');return;}
    rows.forEach((item)=>{const tr=document.createElement('tr'); tr.appendChild(cell(item.code,'xcat-primary')); tr.appendChild(cell(`${item.name}${item.active?'':' · 停用'}`)); tr.appendChild(cell(item.gameCode)); tr.appendChild(cell(item.serviceCount??0)); categoryRows.appendChild(tr);});
  }

  function fillGameFilter() {
    const selected=gameFilter.value; const first=gameFilter.firstElementChild; gameFilter.replaceChildren(); if(first)gameFilter.appendChild(first);
    (snapshot?.games||[]).forEach((game)=>{const option=document.createElement('option'); option.value=game.code; option.textContent=`${game.name} · ${game.code}`; gameFilter.appendChild(option);});
    if([...gameFilter.options].some((option)=>option.value===selected)) gameFilter.value=selected;
  }

  function render() {
    document.getElementById('xcatServiceCount').textContent=String(snapshot?.counts?.services??0);
    document.getElementById('xcatActiveCount').textContent=String(snapshot?.counts?.activeServices??0);
    document.getElementById('xcatGameCount').textContent=String(snapshot?.counts?.games??0);
    document.getElementById('xcatCategoryCount').textContent=String(snapshot?.counts?.categories??0);
    document.getElementById('xcatPlatformCount').textContent=String(snapshot?.counts?.platforms??0);
    fillGameFilter(); renderServices(); renderGames(); renderCategories();
  }

  function syncPriceFields() {
    const mode=editPriceMode.value;
    editPriceFixed.disabled=mode!=='FIXED';
    editPriceMin.disabled=mode!=='RANGE';
    editPriceMax.disabled=mode!=='RANGE';
  }

  function openEdit(item) {
    if(!canManage) return;
    currentEdit=item;
    editMeta.textContent=`${item.slug} · ${item.gameCode}/${item.category} · 当前 ${item.status}`;
    editName.value=item.name||'';
    editShortDesc.value=item.shortDesc||'';
    editPriceMode.value=String(item.priceMode||'QUOTE').toUpperCase();
    editPriceFixed.value=item.priceFixed??'';
    editPriceMin.value=item.priceMin??'';
    editPriceMax.value=item.priceMax??'';
    editDuration.value=item.durationText||'';
    editSortOrder.value=String(item.sortOrder??0);
    editPriority.value=String(item.manualPriority??0);
    editReason.value='';
    syncPriceFields();
    editDialog.showModal();
    editName.focus();
  }

  function closeEdit() { if(editDialog.open)editDialog.close(); currentEdit=null; }
  function decimalOrNull(input) { if(input.disabled||input.value.trim()==='')return null; const value=Number(input.value); return Number.isFinite(value)?value:null; }
  function integerValue(input, fallback=0) { const value=Number.parseInt(input.value,10); return Number.isFinite(value)?value:fallback; }

  function editPayload(item) {
    return {
      gameCode:item.gameCode,
      slug:item.slug,
      name:editName.value.trim(),
      category:item.category,
      shortDesc:editShortDesc.value.trim(),
      priceMode:editPriceMode.value,
      priceFixed:decimalOrNull(editPriceFixed),
      priceMin:decimalOrNull(editPriceMin),
      priceMax:decimalOrNull(editPriceMax),
      durationText:editDuration.value.trim(),
      status:item.status,
      coverImage:item.coverImage??null,
      hotScore:item.hotScore??0,
      manualPriority:integerValue(editPriority,item.manualPriority??0),
      expiresAt:item.expiresAt??null,
      active:Boolean(item.active),
      sortOrder:integerValue(editSortOrder,item.sortOrder??0),
      reason:editReason.value.trim()
    };
  }

  async function parseResponse(response) {
    if(response.status===204)return null;
    try{return await response.json();}catch(_){return null;}
  }

  async function write(url, method, body=null) {
    const headers={'Accept':'application/json'};
    if(body!==null)headers['Content-Type']='application/json';
    if(csrfToken&&csrfHeader)headers[csrfHeader]=csrfToken;
    const response=await fetch(url,{method,credentials:'same-origin',headers,body:body===null?undefined:JSON.stringify(body)});
    const result=await parseResponse(response);
    if(!response.ok)throw new Error(result?.message||result?.error||`HTTP ${response.status}`);
    return result;
  }

  async function loadAccess() {
    try{
      const response=await fetch('/api/admin/xingwei/access',{credentials:'same-origin',headers:{'Accept':'application/json'}});
      if(!response.ok)throw new Error(`HTTP ${response.status}`);
      access=await response.json();
      canManage=Boolean(access?.allowAll||(access?.permissionCodes||[]).includes('service:manage'));
      setStatus(manageMode,canManage?'service:manage · 可管理':'service:view · 只读',canManage);
    }catch(error){
      access=null; canManage=false; manageMode.classList.remove('ok'); manageMode.classList.add('error'); const label=manageMode.querySelector('span'); if(label)label.textContent='权限读取失败 · 只读';
    }
    if(snapshot)renderServices();
  }

  async function loadSnapshot(showLoading=true) {
    if(showLoading)setMessage('正在读取生产服务目录与游戏资料……'); refresh.disabled=true;
    try {
      const response=await fetch('/api/admin/xingwei/catalog/snapshot',{credentials:'same-origin',headers:{'Accept':'application/json'}});
      const body=await parseResponse(response);
      if(!response.ok) throw new Error(body?.error||body?.message||`HTTP ${response.status}`);
      snapshot=body; render(); status.classList.remove('error'); setStatus(status,'生产目录已读取',true);
      if(showLoading)setMessage(`已读取 ${snapshot.counts?.services??0} 个服务、${snapshot.counts?.games??0} 个游戏、${snapshot.counts?.categories??0} 个分类。${canManage?'当前账号可执行基础服务管理。':'当前账号为只读模式。'}`,'ok');
    } catch(error) {
      status.classList.remove('ok'); status.classList.add('error'); const label=status.querySelector('span'); if(label)label.textContent='读取失败'; setMessage(`读取失败或当前账号没有 service:view：${error.message}`,'error');
    } finally { refresh.disabled=false; }
  }

  async function saveEdit(event) {
    event.preventDefault();
    if(!currentEdit||!canManage)return;
    const payload=editPayload(currentEdit);
    if(!payload.reason){setMessage('保存前必须填写变更原因。','error');editReason.focus();return;}
    if(payload.priceMode==='FIXED'&&payload.priceFixed===null){setMessage('固定价模式必须填写固定价。','error');editPriceFixed.focus();return;}
    if(payload.priceMode==='RANGE'&&(payload.priceMin===null||payload.priceMax===null||payload.priceMin>payload.priceMax)){setMessage('区间价必须填写合法的最低价和最高价。','error');editPriceMin.focus();return;}
    editSave.disabled=true;
    const name=currentEdit.name;
    try{
      await write(`/api/admin/xingwei/catalog/services/${currentEdit.id}`,'PUT',payload);
      closeEdit(); await loadSnapshot(false); setMessage(`已保存「${name}」的基础资料，变更已进入现有审计日志。`,'ok');
    }catch(error){setMessage(`保存失败：${error.message}`,'error');}
    finally{editSave.disabled=false;}
  }

  async function changeAvailability(item, action, button) {
    if(!canManage)return;
    const restoring=action==='restore';
    const confirmed=window.confirm(restoring?`确认恢复「${item.name}」？`:`确认下线「${item.name}」？该操作会使公开服务不可见。`);
    if(!confirmed)return;
    button.disabled=true;
    try{
      if(restoring)await write(`/api/admin/xingwei/catalog/services/${item.id}/restore`,'POST');
      else await write(`/api/admin/xingwei/catalog/services/${item.id}`,'DELETE');
      await loadSnapshot(false); setMessage(`${restoring?'已恢复':'已下线'}「${item.name}」，操作已进入现有审计日志。`,'ok');
    }catch(error){setMessage(`${restoring?'恢复':'下线'}失败：${error.message}`,'error');}
    finally{button.disabled=false;}
  }

  serviceRows.addEventListener('click',(event)=>{
    const button=event.target.closest('button[data-action]'); if(!button)return;
    const id=Number(button.dataset.id); const item=(snapshot?.services||[]).find((row)=>Number(row.id)===id); if(!item)return;
    if(button.dataset.action==='edit')openEdit(item); else void changeAvailability(item,button.dataset.action,button);
  });
  editForm.addEventListener('submit',saveEdit);
  editPriceMode.addEventListener('change',syncPriceFields);
  document.getElementById('xcatEditClose').addEventListener('click',closeEdit);
  document.getElementById('xcatEditCancel').addEventListener('click',closeEdit);
  search.addEventListener('input',renderServices);
  gameFilter.addEventListener('change',renderServices);
  stateFilter.addEventListener('change',renderServices);
  refresh.addEventListener('click',()=>loadSnapshot(true));

  (async()=>{await loadAccess();await loadSnapshot(true);})();
})();
