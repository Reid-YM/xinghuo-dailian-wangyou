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
  const toggle = document.getElementById('xtaxToggle');
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
      const name=document.createElement('td');append(name,'strong',row.name,'xtax-primary');append(name,'small',row.code,'xtax-secondary');tr.appendChild(name);
      tr.appendChild(cell(row.businessStatus||'—'));
      tr.appendChild(cell(row.categoryCount??0));
      tr.appendChild(cell(row.serviceCount??0));
      const stateCell=document.createElement('td');append(stateCell,'span',row.active?'启用':'停用',`xtax-state ${row.active?'on':'off'}`);tr.appendChild(stateCell);
      const actions=document.createElement('td');actions.className='xtax-row-actions';actions.appendChild(actionButton('game',row.code));tr.appendChild(actions);
      gameRows.appendChild(tr);
    });
  }

  function renderCategories(){
    const rows=filteredCategories();categoryRows.replaceChildren();
    if(!rows.length){emptyRow(categoryRows,5,'当前筛选条件没有分类');return;}
    rows.forEach(row=>{
      const tr=document.createElement('tr');
      const name=document.createElement('td');append(name,'strong',row.name,'xtax-primary');append(name,'small',row.code,'xtax-secondary');tr.appendChild(name);
      tr.appendChild(cell(row.gameCode));
      tr.appendChild(cell(row.serviceCount??0));
      const stateCell=document.createElement('td');append(stateCell,'span',row.active?'启用':'停用',`xtax-state ${row.active?'on':'off'}`);tr.appendChild(stateCell);
      const actions=document.createElement('td');actions.className='xtax-row-actions';actions.appendChild(actionButton('category',row.id));tr.appendChild(actions);
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
      if(!response.ok)throw new Error(`HTTP ${response.status}`);
      const body=await response.json();
      canManage=Boolean(body?.allowAll||(body?.permissionCodes||[]).includes('service:manage'));
      setStatus(mode,canManage?'service:manage · 可启停':'service:view · 只读',canManage,false);
    }catch(error){canManage=false;setStatus(mode,'权限读取失败 · 只读',false,true);}
  }

  async function loadSnapshot(showLoading=true){
    if(showLoading)setMessage('正在读取游戏与分类……');refresh.disabled=true;
    try{
      const response=await fetch('/api/admin/xingwei/catalog/snapshot',{credentials:'same-origin',headers:{Accept:'application/json'}});
      const body=await parseResponse(response);if(!response.ok)throw new Error(body?.message||body?.error||`HTTP ${response.status}`);
      snapshot=body;render();setStatus(status,'taxonomy 已读取',true,false);
      if(showLoading)setMessage(`已读取 ${(snapshot.games||[]).length} 个游戏、${(snapshot.categories||[]).length} 个分类。${canManage?'所有启停必须先查看影响并填写原因。':'当前账号只能查看影响。'}`,'ok');
    }catch(error){setStatus(status,'读取失败',false,true);setMessage(`读取失败或当前账号没有 service:view：${error.message}`,'error');}
    finally{refresh.disabled=false;}
  }

  function resetImpact(){
    currentImpact=null;currentTarget=null;dialogMeta.textContent='—';impactBox.textContent='正在读取影响……';effect.textContent='—';
    document.getElementById('xtaxImpactCategories').textContent='-';
    document.getElementById('xtaxImpactActiveCategories').textContent='-';
    document.getElementById('xtaxImpactServices').textContent='-';
    document.getElementById('xtaxImpactPublicServices').textContent='-';
    reason.value='';reasonWrap.hidden=true;toggle.hidden=true;toggle.disabled=false;
  }

  function renderImpact(impact){
    currentImpact=impact;
    dialogMeta.textContent=`${impact.type==='GAME'?'游戏':'分类'} · ${impact.name} · ${impact.key}`;
    impactBox.textContent=`当前状态：${impact.active?'启用':'停用'}；目标状态：${impact.active?'停用':'启用'}`;
    document.getElementById('xtaxImpactCategories').textContent=String(impact.categoryCount??0);
    document.getElementById('xtaxImpactActiveCategories').textContent=String(impact.activeCategoryCount??0);
    document.getElementById('xtaxImpactServices').textContent=String(impact.serviceCount??0);
    document.getElementById('xtaxImpactPublicServices').textContent=String(impact.publicCandidateServiceCount??0);
    effect.textContent=impact.effect||'—';
    reasonWrap.hidden=!canManage;
    toggle.hidden=!canManage;
    toggle.textContent=impact.active?'确认停用':'确认启用';
  }

  async function openImpact(kind,key){
    resetImpact();currentTarget={kind,key};if(!dialog.open)dialog.showModal();
    const url=kind==='game'
      ? `/api/admin/xingwei/catalog/taxonomy/games/${encodeURIComponent(key)}/impact`
      : `/api/admin/xingwei/catalog/taxonomy/categories/${encodeURIComponent(key)}/impact`;
    try{
      const response=await fetch(url,{credentials:'same-origin',headers:{Accept:'application/json'}});
      const body=await parseResponse(response);if(!response.ok)throw new Error(body?.message||body?.error||`HTTP ${response.status}`);
      if(!currentTarget||String(currentTarget.key)!==String(key)||currentTarget.kind!==kind)return;
      renderImpact(body);
    }catch(error){impactBox.textContent='影响预览读取失败';effect.textContent=error.message;toggle.hidden=true;reasonWrap.hidden=true;}
  }

  async function applyToggle(){
    if(!canManage||!currentImpact||!currentTarget)return;
    const why=reason.value.trim();if(!why){setMessage('启停前必须填写原因。','error');reason.focus();return;}
    const next=!Boolean(currentImpact.active);
    const label=`${currentImpact.name}（${currentImpact.key}）`;
    if(!window.confirm(`确认${next?'启用':'停用'}${currentImpact.type==='GAME'?'游戏':'分类'}「${label}」？`))return;
    const url=currentTarget.kind==='game'
      ? `/api/admin/xingwei/catalog/taxonomy/games/${encodeURIComponent(currentTarget.key)}/active`
      : `/api/admin/xingwei/catalog/taxonomy/categories/${encodeURIComponent(currentTarget.key)}/active`;
    const headers={Accept:'application/json','Content-Type':'application/json'};if(csrfToken&&csrfHeader)headers[csrfHeader]=csrfToken;
    toggle.disabled=true;
    try{
      const response=await fetch(url,{method:'PUT',credentials:'same-origin',headers,body:JSON.stringify({active:next,reason:why})});
      const body=await parseResponse(response);if(!response.ok)throw new Error(body?.message||body?.error||`HTTP ${response.status}`);
      dialog.close();currentImpact=null;currentTarget=null;await loadSnapshot(false);
      setMessage(`已${next?'启用':'停用'}「${label}」，可见性缓存已立即刷新，操作已进入现有 taxonomy 审计。`,'ok');
    }catch(error){setMessage(`启停失败：${error.message}`,'error');toggle.disabled=false;}
  }

  document.addEventListener('click',(event)=>{
    const button=event.target.closest('button[data-kind][data-key]');if(!button)return;
    void openImpact(button.dataset.kind,button.dataset.key);
  });
  document.getElementById('xtaxClose').addEventListener('click',()=>dialog.close());
  document.getElementById('xtaxCancel').addEventListener('click',()=>dialog.close());
  toggle.addEventListener('click',()=>void applyToggle());
  search.addEventListener('input',render);
  state.addEventListener('change',render);
  refresh.addEventListener('click',()=>void loadSnapshot(true));

  (async()=>{await loadAccess();await loadSnapshot(true);})();
})();
