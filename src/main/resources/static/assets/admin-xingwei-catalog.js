(() => {
  'use strict';
 const display=value=>window.XWLabels?.value(value)??value;

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
  const createOpen = document.getElementById('xcatCreateOpen');
  const createDialog = document.getElementById('xcatCreateDialog');
  const createForm = document.getElementById('xcatCreateForm');
  const createGame = document.getElementById('xcatCreateGame');
  const createCategory = document.getElementById('xcatCreateCategory');
  const createSlug = document.getElementById('xcatCreateSlug');
  const createName = document.getElementById('xcatCreateName');
  const createShortDesc = document.getElementById('xcatCreateShortDesc');
  const createPriceMode = document.getElementById('xcatCreatePriceMode');
  const createPriceFixed = document.getElementById('xcatCreatePriceFixed');
  const createPriceMin = document.getElementById('xcatCreatePriceMin');
  const createPriceMax = document.getElementById('xcatCreatePriceMax');
  const createDuration = document.getElementById('xcatCreateDuration');
  const createSortOrder = document.getElementById('xcatCreateSortOrder');
  const createPriority = document.getElementById('xcatCreatePriority');
  const createReason = document.getElementById('xcatCreateReason');
  const createSave = document.getElementById('xcatCreateSave');
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
  const reviewDialog = document.getElementById('xcatReviewDialog');
  const reviewMeta = document.getElementById('xcatReviewMeta');
  const reviewSummary = document.getElementById('xcatReviewSummary');
  const reviewChecks = document.getElementById('xcatReviewChecks');
  const reviewPublish = document.getElementById('xcatReviewPublish');
  const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute('content') || '';
  const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content') || '';

  let snapshot = null;
  let access = null;
  let canManage = false;
  let currentEdit = null;
  let currentReviewItem = null;
  let currentReview = null;

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
      const gameCell=document.createElement('td'); append(gameCell,'span',(snapshot.games||[]).find(game=>game.code===item.gameCode)?.name||'未分配游戏','xcat-primary'); append(gameCell,'small',(snapshot.categories||[]).find(category=>category.code===item.category&&category.gameCode===item.gameCode)?.name||'未分配分类','xcat-secondary'); tr.appendChild(gameCell);
      const priceCell=document.createElement('td'); append(priceCell,'span',priceLabel(item),'xcat-primary'); append(priceCell,'small',display(item.priceMode),'xcat-secondary'); tr.appendChild(priceCell);
      tr.appendChild(cell(item.durationText));
      const statusCell=document.createElement('td'); const badge=append(statusCell,'span',`${display(item.status)} · ${item.active?'启用':'停用'}`,`xcat-status ${item.active?'on':'off'}`); badge.title=item.shortDesc||''; tr.appendChild(statusCell);
      const orderCell=document.createElement('td'); append(orderCell,'span',`展示 ${item.sortOrder}`,'xcat-primary'); append(orderCell,'small',`优先级 ${item.manualPriority}`,'xcat-secondary'); tr.appendChild(orderCell);
      const actionCell=document.createElement('td'); actionCell.className='xcat-row-actions';
      if(canManage){
        actionCell.appendChild(actionButton('编辑','edit',item.id));
        const offline=String(item.status).toUpperCase()==='OFFLINE'||!item.active;
        actionCell.appendChild(actionButton(offline?'发布检查':'下线',offline?'review':'offline',item.id,offline?'xw-button':'xw-button xw-button-ghost'));
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
    rows.forEach((item)=>{const tr=document.createElement('tr'); const name=document.createElement('td'); append(name,'strong',item.name,'xcat-primary'); tr.appendChild(name); tr.appendChild(cell(`${display(item.businessStatus)||'—'} · ${item.active?'启用':'停用'}`)); tr.appendChild(cell(`${item.categoryCount??0} / ${item.serviceCount??0}`)); tr.appendChild(cell((item.platformCodes||[]).map(code=>(snapshot.platforms||[]).find(platform=>platform.code===code)?.name||'其他平台').join('、')||'—')); gameRows.appendChild(tr);});
  }

  function renderCategories() {
    categoryRows.replaceChildren(); const rows=snapshot?.categories||[];
    if(!rows.length){emptyRow(categoryRows,3,'暂无分类资料');return;}
    rows.forEach((item)=>{const tr=document.createElement('tr'); tr.appendChild(cell(`${item.name}${item.active?'':' · 停用'}`)); tr.appendChild(cell((snapshot.games||[]).find(game=>game.code===item.gameCode)?.name||'未分配游戏')); tr.appendChild(cell(item.serviceCount??0)); categoryRows.appendChild(tr);});
  }

  function fillGameFilter() {
    const selected=gameFilter.value; const first=gameFilter.firstElementChild; gameFilter.replaceChildren(); if(first)gameFilter.appendChild(first);
    (snapshot?.games||[]).forEach((game)=>{const option=document.createElement('option'); option.value=game.code; option.textContent=game.name; gameFilter.appendChild(option);});
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

  function syncPriceFields(mode, fixed, min, max) {
    fixed.disabled=mode.value!=='FIXED';
    min.disabled=mode.value!=='RANGE';
    max.disabled=mode.value!=='RANGE';
  }

  function fillCreateGames() {
    const previous=createGame.value;
    createGame.replaceChildren();
    const games=(snapshot?.games||[]).filter((game)=>game.active);
    games.forEach((game)=>{const option=document.createElement('option'); option.value=game.code; option.textContent=game.name; createGame.appendChild(option);});
    if([...createGame.options].some((option)=>option.value===previous)) createGame.value=previous;
    createGame.disabled=games.length===0;
    fillCreateCategories();
  }

  function fillCreateCategories() {
    const previous=createCategory.value;
    createCategory.replaceChildren();
    const categories=(snapshot?.categories||[]).filter((category)=>category.active&&category.gameCode===createGame.value);
    categories.forEach((category)=>{const option=document.createElement('option'); option.value=category.code; option.textContent=category.name; createCategory.appendChild(option);});
    if([...createCategory.options].some((option)=>option.value===previous)) createCategory.value=previous;
    createCategory.disabled=categories.length===0;
  }

  function openCreate() {
    if(!canManage||!snapshot) return;
    createForm.reset();
    createPriceMode.value='QUOTE';
    createSortOrder.value='0';
    createPriority.value='0';
    fillCreateGames();
    syncPriceFields(createPriceMode,createPriceFixed,createPriceMin,createPriceMax);
    if(createGame.disabled||createCategory.disabled){setMessage('当前没有可用于新增服务的启用游戏/分类，请先在游戏与分类工作台维护并启用对应项目。','error');return;}
    createDialog.showModal();
    createSlug.focus();
  }

  function closeCreate() { if(createDialog.open)createDialog.close(); }

  function openEdit(item) {
    if(!canManage) return;
    currentEdit=item;
    editMeta.textContent=`${item.name} · 当前 ${display(item.status)}`;
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
    syncPriceFields(editPriceMode,editPriceFixed,editPriceMin,editPriceMax);
    editDialog.showModal();
    editName.focus();
  }

  function closeEdit() { if(editDialog.open)editDialog.close(); currentEdit=null; }
  function closeReview() { if(reviewDialog.open)reviewDialog.close(); currentReviewItem=null; currentReview=null; }
  function decimalOrNull(input) { if(input.disabled||input.value.trim()==='')return null; const value=Number(input.value); return Number.isFinite(value)?value:null; }
  function integerValue(input, fallback=0) { const value=Number.parseInt(input.value,10); return Number.isFinite(value)?value:fallback; }

  function createPayload() {
    return {
      gameCode:createGame.value,
      slug:createSlug.value.trim().toLowerCase(),
      name:createName.value.trim(),
      category:createCategory.value,
      shortDesc:createShortDesc.value.trim(),
      priceMode:createPriceMode.value,
      priceFixed:decimalOrNull(createPriceFixed),
      priceMin:decimalOrNull(createPriceMin),
      priceMax:decimalOrNull(createPriceMax),
      durationText:createDuration.value.trim(),
      status:'OFFLINE',
      coverImage:null,
      hotScore:0,
      manualPriority:integerValue(createPriority,0),
      expiresAt:null,
      active:false,
      sortOrder:integerValue(createSortOrder,0),
      reason:createReason.value.trim()
    };
  }

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

  function validatePrice(payload, fixedInput, minInput) {
    if(payload.priceMode==='FIXED'&&payload.priceFixed===null){setMessage('固定价模式必须填写固定价。','error');fixedInput.focus();return false;}
    if(payload.priceMode==='RANGE'&&(payload.priceMin===null||payload.priceMax===null||payload.priceMin>payload.priceMax)){setMessage('区间价必须填写合法的最低价和最高价。','error');minInput.focus();return false;}
    return true;
  }

  async function parseResponse(response) {
    if(response.status===204)return null;
    try{return await response.json();}catch(_){return null;}
  }

  async function write(url, method, body=null) {
    const headers={'Accept':'application/json'};
    if(body!==null)headers['Content-Type']='application/json';
    if(method==='PUT'&&/\/services\/\d+$/.test(url)&&currentEdit)headers['If-Match']='"v'+String(currentEdit.version??0)+'"';
    if(csrfToken&&csrfHeader)headers[csrfHeader]=csrfToken;
    const response=await fetch(url,{method,credentials:'same-origin',headers,body:body===null?undefined:JSON.stringify(body)});
    const result=await parseResponse(response);
    if(!response.ok)throw new Error(result?.message||result?.error||`请求失败（状态码 ${response.status}）`);
    return result;
  }

  function renderReview(review) {
    currentReview=review;
    reviewChecks.replaceChildren();
    reviewSummary.classList.remove('ready','blocked');
    reviewSummary.classList.add(review.ready?'ready':'blocked');
    reviewSummary.textContent=review.ready
      ? `检查通过 · ${review.blockers??0} 个阻断 · ${review.warnings??0} 个警告`
      : `检查未通过 · ${review.blockers??0} 个阻断 · ${review.warnings??0} 个警告`;
    (review.checks||[]).forEach((check)=>{
      const row=document.createElement('div');
      const severity=String(check.severity||'').toUpperCase();
      row.className=`xcat-check ${check.passed?'pass':(severity==='BLOCKER'?'blocker':'warning')}`;
      const mark=append(row,'span',check.passed?'✓':'!','xcat-check-mark');
      mark.setAttribute('aria-hidden','true');
      const body=document.createElement('div');
      const title=document.createElement('div');
      append(title,'strong',check.label,'xcat-check-title');
      append(title,'small',severity==='BLOCKER'?'阻断项':'警告项','xcat-check-severity');
      body.appendChild(title);
      append(body,'p',check.detail,'xcat-check-detail');
      row.appendChild(body);
      reviewChecks.appendChild(row);
    });
    reviewPublish.hidden=!canManage||!review.ready;
    reviewPublish.disabled=false;
  }

  async function openReview(item) {
    if(!canManage)return;
    currentReviewItem=item;
    currentReview=null;
    reviewMeta.textContent=item.name;
    reviewSummary.classList.remove('ready','blocked');
    reviewSummary.textContent='正在执行发布前检查……';
    reviewChecks.replaceChildren();
    append(reviewChecks,'p','正在核对游戏与分类、基础内容、价格和到期时间。','xcat-check-loading');
    reviewPublish.hidden=true;
    reviewDialog.showModal();
    try{
      const response=await fetch(`/api/admin/xingwei/catalog/services/${item.id}/publish-review`,{credentials:'same-origin',headers:{'Accept':'application/json'}});
      const body=await parseResponse(response);
      if(!response.ok)throw new Error(body?.message||body?.error||`请求失败（状态码 ${response.status}）`);
      if(currentReviewItem?.id!==item.id)return;
      renderReview(body);
    }catch(error){
      reviewSummary.classList.add('blocked');
      reviewSummary.textContent='发布检查读取失败';
      reviewChecks.replaceChildren();
      append(reviewChecks,'p',error.message,'xcat-check-loading');
      reviewPublish.hidden=true;
    }
  }

  async function publishReviewed() {
    if(!canManage||!currentReviewItem||!currentReview?.ready)return;
    const item=currentReviewItem;
    const confirmed=window.confirm(`最终确认上架「${item.name}」？发布后该服务会按现有游戏分类与状态进入公开服务目录。`);
    if(!confirmed)return;
    reviewPublish.disabled=true;
    try{
      await write(`/api/admin/xingwei/catalog/services/${item.id}/restore`,'POST');
      closeReview();
      await loadSnapshot(false);
      setMessage(`「${item.name}」已通过发布检查并正式上架，SERVICE_RESTORE 已记录审计。`,'ok');
    }catch(error){
      setMessage(`上架失败：${error.message}`,'error');
      reviewPublish.disabled=false;
      if(reviewDialog.open)void openReview(item);
    }
  }

  async function loadAccess() {
    try{
      const response=await fetch('/api/admin/xingwei/access',{credentials:'same-origin',headers:{'Accept':'application/json'}});
      if(!response.ok)throw new Error(`请求失败（状态码 ${response.status}）`);
      access=await response.json();
      canManage=Boolean(access?.allowAll||(access?.permissionCodes||[]).includes('service:manage'));
      createOpen.hidden=!canManage;
      setStatus(manageMode,canManage?'service:manage · 可管理':'service:view · 只读',canManage);
    }catch(error){
      access=null; canManage=false; createOpen.hidden=true; manageMode.classList.remove('ok'); manageMode.classList.add('error'); const label=manageMode.querySelector('span'); if(label)label.textContent='权限读取失败 · 只读';
    }
    if(snapshot)renderServices();
  }

  async function loadSnapshot(showLoading=true) {
    if(showLoading)setMessage('正在读取生产服务目录与游戏资料……'); refresh.disabled=true;
    try {
      const response=await fetch('/api/admin/xingwei/catalog/snapshot',{credentials:'same-origin',headers:{'Accept':'application/json'}});
      const body=await parseResponse(response);
      if(!response.ok) throw new Error(body?.error||body?.message||`请求失败（状态码 ${response.status}）`);
      snapshot=body; render(); status.classList.remove('error'); setStatus(status,'生产目录已读取',true);
      if(showLoading)setMessage(`已读取 ${snapshot.counts?.services??0} 个服务、${snapshot.counts?.games??0} 个游戏、${snapshot.counts?.categories??0} 个分类。${canManage?'离线服务需先通过发布检查再上架。':'当前账号为只读模式。'}`,'ok');
    } catch(error) {
      status.classList.remove('ok'); status.classList.add('error'); const label=status.querySelector('span'); if(label)label.textContent='读取失败'; setMessage(`读取失败或当前账号没有 service:view：${error.message}`,'error');
    } finally { refresh.disabled=false; }
  }

  async function saveCreate(event) {
    event.preventDefault();
    if(!canManage||!snapshot)return;
    const payload=createPayload();
    if(!payload.gameCode||!payload.category){setMessage('必须选择现有游戏与分类。','error');return;}
    if(!/^[a-z0-9-]{2,100}$/.test(payload.slug)){setMessage('服务访问标识只能使用 2-100 位小写字母、数字和连字符。','error');createSlug.focus();return;}
    if((snapshot.services||[]).some((item)=>normalize(item.slug)===payload.slug)){setMessage('该服务访问标识已存在，请更换服务标识。','error');createSlug.focus();return;}
    const validTaxonomy=(snapshot.categories||[]).some((category)=>category.active&&category.gameCode===payload.gameCode&&category.code===payload.category);
    if(!validTaxonomy){setMessage('所选游戏/分类已失效，请刷新目录后重试。','error');return;}
    if(!payload.name||!payload.shortDesc||!payload.durationText){setMessage('服务名称、简短说明和预计时长不能为空。','error');return;}
    if(!payload.reason){setMessage('创建前必须填写新增原因。','error');createReason.focus();return;}
    if(!validatePrice(payload,createPriceFixed,createPriceMin))return;
    createSave.disabled=true;
    try{
      const created=await write('/api/admin/xingwei/catalog/services','POST',payload);
      closeCreate(); await loadSnapshot(false); setMessage(`已创建离线服务「${created?.name||payload.name}」。下一步请执行“发布检查”，通过后再确认上架；服务创建已记录审计。`,'ok');
    }catch(error){setMessage(`创建失败：${error.message}`,'error');}
    finally{createSave.disabled=false;}
  }

  async function saveEdit(event) {
    event.preventDefault();
    if(!currentEdit||!canManage)return;
    const payload=editPayload(currentEdit);
    if(!payload.reason){setMessage('保存前必须填写变更原因。','error');editReason.focus();return;}
    if(!validatePrice(payload,editPriceFixed,editPriceMin))return;
    editSave.disabled=true;
    const name=currentEdit.name;
    try{
      await write(`/api/admin/xingwei/catalog/services/${currentEdit.id}`,'PUT',payload);
      closeEdit(); await loadSnapshot(false); setMessage(`已保存「${name}」的基础资料，变更已进入现有审计日志。`,'ok');
    }catch(error){setMessage(`保存失败：${error.message}`,'error');}
    finally{editSave.disabled=false;}
  }

  async function offlineService(item, button) {
    if(!canManage)return;
    const confirmed=window.confirm(`确认下线「${item.name}」？该操作会使公开服务不可见。`);
    if(!confirmed)return;
    button.disabled=true;
    try{
      await write(`/api/admin/xingwei/catalog/services/${item.id}`,'DELETE');
      await loadSnapshot(false); setMessage(`已下线「${item.name}」，操作已进入现有审计日志。`,'ok');
    }catch(error){setMessage(`下线失败：${error.message}`,'error');}
    finally{button.disabled=false;}
  }

  serviceRows.addEventListener('click',(event)=>{
    const button=event.target.closest('button[data-action]'); if(!button)return;
    const id=Number(button.dataset.id); const item=(snapshot?.services||[]).find((row)=>Number(row.id)===id); if(!item)return;
    if(button.dataset.action==='edit')openEdit(item);
    else if(button.dataset.action==='review')void openReview(item);
    else if(button.dataset.action==='offline')void offlineService(item,button);
  });
  createOpen.addEventListener('click',openCreate);
  createForm.addEventListener('submit',saveCreate);
  createGame.addEventListener('change',fillCreateCategories);
  createPriceMode.addEventListener('change',()=>syncPriceFields(createPriceMode,createPriceFixed,createPriceMin,createPriceMax));
  document.getElementById('xcatCreateClose').addEventListener('click',closeCreate);
  document.getElementById('xcatCreateCancel').addEventListener('click',closeCreate);
  editForm.addEventListener('submit',saveEdit);
  editPriceMode.addEventListener('change',()=>syncPriceFields(editPriceMode,editPriceFixed,editPriceMin,editPriceMax));
  document.getElementById('xcatEditClose').addEventListener('click',closeEdit);
  document.getElementById('xcatEditCancel').addEventListener('click',closeEdit);
  document.getElementById('xcatReviewClose').addEventListener('click',closeReview);
  document.getElementById('xcatReviewCancel').addEventListener('click',closeReview);
  reviewPublish.addEventListener('click',()=>void publishReviewed());
  search.addEventListener('input',renderServices);
  gameFilter.addEventListener('change',renderServices);
  stateFilter.addEventListener('change',renderServices);
  refresh.addEventListener('click',()=>loadSnapshot(true));

  (async()=>{await loadAccess();await loadSnapshot(true);})();
})();
