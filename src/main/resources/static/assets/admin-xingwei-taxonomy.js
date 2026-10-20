(() => {
  'use strict';

  const status = document.getElementById('xtaxStatus');
  const mode = document.getElementById('xtaxMode');
  const message = document.getElementById('xtaxMessage');
  const search = document.getElementById('xtaxSearch');
  const state = document.getElementById('xtaxState');
  const refresh = document.getElementById('xtaxRefresh');
  const gameRows = document.getElementById('xtaxGameRows');
  const categoryRows = document.getElementById('xtaxCategoryRows');
  const dialog = document.getElementById('xtaxDialog');
  const dialogMeta = document.getElementById('xtaxDialogMeta');
  const impactBox = document.getElementById('xtaxImpact');
  const effect = document.getElementById('xtaxEffect');
  const reasonWrap = document.getElementById('xtaxReasonWrap');
  const reason = document.getElementById('xtaxReason');
  const ackWrap = document.getElementById('xtaxAckWrap');
  const acknowledge = document.getElementById('xtaxAcknowledge');
  const toggle = document.getElementById('xtaxToggle');
  const visibleNow = document.getElementById('xtaxVisibleNow');
  const visibleAfter = document.getElementById('xtaxVisibleAfter');
  const visibilitySummary = document.getElementById('xtaxVisibilitySummary');
  const warnings = document.getElementById('xtaxWarnings');
  const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute('content') || '';
  const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content') || '';

  let snapshot = null;
  let canManage = false;
  let currentImpact = null;
  let currentTarget = null;

  function normalize(value) { return String(value ?? '').toLocaleLowerCase('zh-CN').trim(); }
  function append(parent, tag, text, className='') { const node=document.createElement(tag); if(className)node.className=className; node.textContent=text==null||text===''?'—':String(text); parent.appendChild(node); return node; }
  function setMessage(text, kind='') { message.textContent=text; message.classList.remove('ok','error'); if(kind)message.classList.add(kind); }
  function setStatus(node,text,ok=false,error=false){node.classList.toggle('ok',ok);node.classList.toggle('error',error);const label=node.querySelector('span');if(label)label.textContent=text;}
  function emptyRow(body,colspan,text){body.replaceChildren();const tr=document.createElement('tr');tr.className='xtax-empty';const td=document.createElement('td');td.colSpan=colspan;td.textContent=text;tr.appendChild(td);body.appendChild(tr);}
  function matchState(active){return !state.value||(state.value==='active'&&active)||(state.value==='inactive'&&!active);}
  function actionButton(kind,key,label='查看影响'){const button=document.createElement('button');button.type='button';button.className='xw-button xw-button-ghost';button.textContent=label;button.dataset.kind=kind;button.dataset.key=String(key);return button;}

  function filteredGames(){const q=normalize(search.value);return(snapshot?.games||[]).filter(row=>matchState(Boolean(row.active))&&(!q||normalize([row.name,row.code,row.businessStatus].join(' ')).includes(q)));}
  function filteredCategories(){const q=normalize(search.value);return(snapshot?.categories||[]).filter(row=>matchState(Boolean(row.active))&&(!q||normalize([row.name,row.code,row.gameCode].join(' ')).includes(q)));}

  function renderGames(){
    const rows=filteredGames();gameRows.replaceChildren();
    if(!rows.length){emptyRow(gameRows,6,'当前筛选条件没有游戏');return;}
    rows.forEach(row=>{
      const tr=document.createElement('tr');
      const name=document.createElement('td');append(name,'strong',row.name,'xtax-primary');tr.appendChild(name);
      tr.appendChild(cell(window.XWLabels?.value(row.businessStatus)||'未设置'));
      tr.appendChild(cell(row.categoryCount??0));
      tr.appendChild(cell(row.serviceCount??0));
      const stateCell=document.createElement('td');append(stateCell,'span',row.active?'启用':'停用',`xtax-state ${row.active?'on':'off'}`);tr.appendChild(stateCell);
      const actions=document.createElement('td');actions.className='xtax-row-actions';actions.appendChild(actionButton('game',row.code));if(canManage)actions.appendChild(actionButton('edit-game',row.code,'编辑'));tr.appendChild(actions);
      gameRows.appendChild(tr);
    });
  }

  function renderCategories(){
    const rows=filteredCategories();categoryRows.replaceChildren();
    if(!rows.length){emptyRow(categoryRows,5,'当前筛选条件没有分类');return;}
    rows.forEach(row=>{
      const tr=document.createElement('tr');
      const name=document.createElement('td');append(name,'strong',row.name,'xtax-primary');tr.appendChild(name);
      tr.appendChild(cell((snapshot.games||[]).find(game=>game.code===row.gameCode)?.name||'未分配游戏'));
      tr.appendChild(cell(row.serviceCount??0));
      const stateCell=document.createElement('td');append(stateCell,'span',row.active?'启用':'停用',`xtax-state ${row.active?'on':'off'}`);tr.appendChild(stateCell);
      const actions=document.createElement('td');actions.className='xtax-row-actions';actions.appendChild(actionButton('category',row.id));if(canManage)actions.appendChild(actionButton('edit-category',row.id,'编辑'));tr.appendChild(actions);
      categoryRows.appendChild(tr);
    });
  }

  function cell(text){const td=document.createElement('td');td.textContent=text==null||text===''?'—':String(text);return td;}

  function render(){
    const games=snapshot?.games||[];const categories=snapshot?.categories||[];
    document.getElementById('xtaxGameCount').textContent=String(games.length);
    document.getElementById('xtaxActiveGameCount').textContent=String(games.filter(row=>row.active).length);
    document.getElementById('xtaxCategoryCount').textContent=String(categories.length);
    document.getElementById('xtaxActiveCategoryCount').textContent=String(categories.filter(row=>row.active).length);
    renderGames();renderCategories();
  }

  async function parseResponse(response){if(response.status===204)return null;try{return await response.json();}catch(_){return null;}}

  async function loadAccess(){
    try{
      const response=await fetch('/api/admin/xingwei/access',{credentials:'same-origin',headers:{Accept:'application/json'}});
      if(!response.ok)throw new Error(`请求失败（状态码 ${response.status}）`);
      const body=await response.json();
      canManage=Boolean(body?.allowAll||(body?.permissionCodes||[]).includes('service:manage'));
      document.getElementById('xtaxAddGame').hidden=!canManage;document.getElementById('xtaxAddCategory').hidden=!canManage;
      setStatus(mode,canManage?'service:manage · 可管理':'service:view · 只读',canManage,false);
    }catch(error){canManage=false;setStatus(mode,'权限读取失败 · 只读',false,true);}
  }

  async function loadSnapshot(showLoading=true){
    if(showLoading)setMessage('正在读取游戏与分类……');refresh.disabled=true;
    try{
      const response=await fetch('/api/admin/xingwei/catalog/snapshot',{credentials:'same-origin',headers:{Accept:'application/json'}});
      const body=await parseResponse(response);if(!response.ok)throw new Error(body?.message||body?.error||`请求失败（状态码 ${response.status}）`);
      snapshot=body;render();setStatus(status,'游戏与分类已读取',true,false);
      if(showLoading)setMessage(`已读取 ${(snapshot.games||[]).length} 个游戏、${(snapshot.categories||[]).length} 个分类。${canManage?'启停必须核对前台可见性、填写原因并确认影响快照。':'当前账号只能查看影响。'}`,'ok');
    }catch(error){setStatus(status,'读取失败',false,true);setMessage(`读取失败或当前账号没有 service:view：${error.message}`,'error');}
    finally{refresh.disabled=false;}
  }

  function syncToggleReady(){
    if(!canManage||!currentImpact){toggle.disabled=true;return;}
    toggle.disabled=!(acknowledge.checked&&reason.value.trim());
  }

  function resetImpact(){
    currentImpact=null;currentTarget=null;dialogMeta.textContent='—';impactBox.textContent='正在读取影响……';effect.textContent='—';
    document.getElementById('xtaxImpactCategories').textContent='-';
    document.getElementById('xtaxImpactActiveCategories').textContent='-';
    document.getElementById('xtaxImpactServices').textContent='-';
    document.getElementById('xtaxImpactPublicServices').textContent='-';
    visibleNow.textContent='-';visibleAfter.textContent='-';visibilitySummary.textContent='正在计算……';
    warnings.replaceChildren();warnings.hidden=true;
    reason.value='维护游戏分类启停';acknowledge.checked=false;reasonWrap.hidden=true;ackWrap.hidden=true;toggle.hidden=true;toggle.disabled=true;
  }

  function renderWarnings(items){
    warnings.replaceChildren();
    (items||[]).forEach(text=>append(warnings,'li',text));
    warnings.hidden=!(items||[]).length;
  }

  function renderImpact(impact){
    currentImpact=impact;
    dialogMeta.textContent=`${impact.type==='GAME'?'游戏':'分类'} · ${impact.name}`;
    impactBox.textContent=`当前状态：${impact.active?'启用':'停用'}；目标状态：${impact.active?'停用':'启用'}`;
    document.getElementById('xtaxImpactCategories').textContent=String(impact.categoryCount??0);
    document.getElementById('xtaxImpactActiveCategories').textContent=String(impact.activeCategoryCount??0);
    document.getElementById('xtaxImpactServices').textContent=String(impact.serviceCount??0);
    document.getElementById('xtaxImpactPublicServices').textContent=String(impact.publicCandidateServiceCount??0);
    visibleNow.textContent=String(impact.currentPublicServiceCount??0);
    visibleAfter.textContent=String(impact.targetPublicServiceCount??0);
    visibilitySummary.textContent=impact.visibilitySummary||impact.effect||'—';
    renderWarnings(impact.warnings);
    effect.textContent=impact.effect||'—';
    reasonWrap.hidden=true;ackWrap.hidden=!canManage;toggle.hidden=!canManage;
    if(impact.active&&Number(impact.currentPublicServiceCount||0)>0){
      toggle.textContent=`确认停用并隐藏 ${impact.currentPublicServiceCount} 个前台服务`;
    }else if(!impact.active&&Number(impact.targetPublicServiceCount||0)>0){
      toggle.textContent=`确认启用并恢复 ${impact.targetPublicServiceCount} 个前台服务`;
    }else{
      toggle.textContent=impact.active?'确认停用':'确认启用';
    }
    syncToggleReady();
  }

  async function openImpact(kind,key){
    resetImpact();currentTarget={kind,key};if(!dialog.open)dialog.showModal();
    const url=kind==='game'
      ? `/api/admin/xingwei/catalog/taxonomy/games/${encodeURIComponent(key)}/impact`
      : `/api/admin/xingwei/catalog/taxonomy/categories/${encodeURIComponent(key)}/impact`;
    try{
      const response=await fetch(url,{credentials:'same-origin',headers:{Accept:'application/json'}});
      const body=await parseResponse(response);if(!response.ok)throw new Error(body?.message||body?.error||`请求失败（状态码 ${response.status}）`);
      if(!currentTarget||String(currentTarget.key)!==String(key)||currentTarget.kind!==kind)return;
      renderImpact(body);
    }catch(error){impactBox.textContent='影响预览读取失败';visibilitySummary.textContent='无法确认前台可见性，请勿执行启停。';effect.textContent=error.message;toggle.hidden=true;reasonWrap.hidden=true;ackWrap.hidden=true;}
  }

  async function applyToggle(){
    if(!canManage||!currentImpact||!currentTarget)return;
    const why=reason.value.trim();if(!why){setMessage('启停前必须填写原因。','error');reason.focus();return;}
    if(!acknowledge.checked){setMessage('请先确认已经核对前台可见性变化。','error');acknowledge.focus();return;}
    const next=!Boolean(currentImpact.active);
    const label=`${currentImpact.name}（${currentImpact.key}）`;
    const url=currentTarget.kind==='game'
      ? `/api/admin/xingwei/catalog/taxonomy/games/${encodeURIComponent(currentTarget.key)}/active`
      : `/api/admin/xingwei/catalog/taxonomy/categories/${encodeURIComponent(currentTarget.key)}/active`;
    const headers={Accept:'application/json','Content-Type':'application/json'};if(csrfToken&&csrfHeader)headers[csrfHeader]=csrfToken;
    toggle.disabled=true;
    try{
      const response=await fetch(url,{method:'PUT',credentials:'same-origin',headers,body:JSON.stringify({active:next,reason:why,impactToken:currentImpact.impactToken})});
      const body=await parseResponse(response);if(!response.ok)throw new Error(body?.message||body?.error||`请求失败（状态码 ${response.status}）`);
      dialog.close();currentImpact=null;currentTarget=null;await loadSnapshot(false);
      setMessage(`已${next?'启用':'停用'}「${label}」，本次影响快照已校验，可见性缓存已立即刷新，操作已进入游戏与分类审计。`,'ok');
    }catch(error){
      const stale=String(error.message||'').includes('影响数据已变化');
      if(stale&&currentTarget){const target={...currentTarget};await openImpact(target.kind,target.key);setMessage('影响数据已变化，已为你重新加载最新前台影响；请重新核对后确认。','error');}
      else{setMessage(`启停失败：${error.message}`,'error');toggle.disabled=false;}
    }
  }

  document.addEventListener('click',(event)=>{
    const button=event.target.closest('button[data-kind][data-key]');if(!button)return;
    if(button.dataset.kind.startsWith('edit-'))openEditor(button.dataset.kind,button.dataset.key);
    else void openImpact(button.dataset.kind,button.dataset.key);
  });
  document.getElementById('xtaxClose').addEventListener('click',()=>dialog.close());
  document.getElementById('xtaxCancel').addEventListener('click',()=>dialog.close());
  toggle.addEventListener('click',()=>void applyToggle());
  reason.addEventListener('input',syncToggleReady);
  acknowledge.addEventListener('change',syncToggleReady);
  search.addEventListener('input',render);
  state.addEventListener('change',render);
  refresh.addEventListener('click',()=>void loadSnapshot(true));

  const editDialog=document.getElementById('xtaxEditDialog');
  const editForm=document.getElementById('xtaxEditForm');
  let editTarget=null;
  function field(id){return document.getElementById(id);}
  async function openEditor(kind,key=null){
    if(!canManage||!snapshot)return;
    const category=kind.includes('category');
    let row=key==null?null:(category?(snapshot.categories||[]).find(x=>String(x.id)===String(key)):(snapshot.games||[]).find(x=>x.code===key));
    if(key!=null&&!row)return;
    let revision=null;
    if(key!=null){try{const response=await fetch('/api/admin/xingwei/catalog/taxonomy/'+(category?'categories/':'games/')+encodeURIComponent(key)+'/editor',{credentials:'same-origin',cache:'no-store'});const state=await parseResponse(response);if(!response.ok)throw new Error(state?.message||state?.error||'读取当前资料失败');row=state.data;revision=state.revision;}catch(error){setMessage(error.message,'error');return;}}
    editTarget={category,key,revision};editForm.reset();field('xtaxEditReason').value=key==null?'新增目录资料':'维护目录资料';
    field('xtaxEditTitle').textContent=(row?'编辑':'新增')+(category?'分类':'游戏');
    field('xtaxEditHint').textContent=row?'编辑基础资料，启停状态请另行调整。':'新增后默认停用，需另行通过影响预览启用。';
    field('xtaxEditGameWrap').hidden=!category||Boolean(row);
    const select=field('xtaxEditGame');select.replaceChildren();
    (snapshot.games||[]).forEach(g=>{const option=document.createElement('option');option.value=g.code;option.textContent=g.name;select.appendChild(option);});
    if(row){field('xtaxEditName').value=row.name||'';field('xtaxEditIcon').value=row.iconKey||'';field('xtaxEditDescription').value=row.description||'';field('xtaxEditSort').value=row.sortOrder??'';if(category)select.value=row.gameCode;}
    if(!editDialog.open)editDialog.showModal();
  }
  async function saveEditor(event){
    event.preventDefault();if(!canManage||!editTarget)return;
    const name=field('xtaxEditName').value.trim(),reasonText=field('xtaxEditReason').value.trim();
    if(!name||!reasonText){setMessage('名称和操作原因不能为空。','error');return;}
    const {category,key,revision}=editTarget;
    const payload={name,iconKey:field('xtaxEditIcon').value.trim()||null,description:field('xtaxEditDescription').value.trim(),sortOrder:field('xtaxEditSort').value===''?null:Number(field('xtaxEditSort').value),reason:reasonText};
    if(category&&key==null){if(!field('xtaxEditGame').value){setMessage('请先选择所属游戏。','error');return;}payload.gameCode=field('xtaxEditGame').value;}
    const base='/api/admin/xingwei/catalog/taxonomy/';
    const url=key==null?base+(category?'categories':'games'):base+(category?'categories/'+encodeURIComponent(key):'games/'+encodeURIComponent(key))+'/profile';
    const headers={Accept:'application/json','Content-Type':'application/json'};if(csrfToken&&csrfHeader)headers[csrfHeader]=csrfToken;if(key!=null)headers['If-Match']='"'+revision+'"';
    const save=field('xtaxEditSave');save.disabled=true;
    try{
      const response=await fetch(url,{method:key==null?'POST':'PUT',credentials:'same-origin',headers,body:JSON.stringify(payload)});
      const body=await parseResponse(response);if(!response.ok)throw new Error(body?.message||body?.error||'请求失败，状态码 '+response.status);
      editDialog.close();editTarget=null;await loadSnapshot(false);setMessage('资料保存成功。新增项目默认停用，启用需单独审核影响。','ok');
    }catch(error){setMessage('保存失败：'+error.message,'error');}finally{save.disabled=false;}
  }
  field('xtaxAddGame').addEventListener('click',()=>openEditor('game'));
  field('xtaxAddCategory').addEventListener('click',()=>openEditor('category'));
  field('xtaxEditClose').addEventListener('click',()=>editDialog.close());
  field('xtaxEditCancel').addEventListener('click',()=>editDialog.close());
  editForm.addEventListener('submit',event=>void saveEditor(event));

  (async()=>{await loadAccess();await loadSnapshot(true);})();
})();
