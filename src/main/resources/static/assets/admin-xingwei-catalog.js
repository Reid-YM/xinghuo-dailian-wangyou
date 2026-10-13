(() => {
  'use strict';

  const status = document.getElementById('xcatStatus');
  const message = document.getElementById('xcatMessage');
  const search = document.getElementById('xcatSearch');
  const gameFilter = document.getElementById('xcatGameFilter');
  const stateFilter = document.getElementById('xcatStateFilter');
  const refresh = document.getElementById('xcatRefresh');
  const serviceRows = document.getElementById('xcatServiceRows');
  const gameRows = document.getElementById('xcatGameRows');
  const categoryRows = document.getElementById('xcatCategoryRows');
  let snapshot = null;

  function normalize(value) { return String(value ?? '').toLocaleLowerCase('zh-CN').trim(); }
  function append(parent, tag, text, className = '') { const node=document.createElement(tag); if(className)node.className=className; node.textContent=text==null||text===''?'—':String(text); parent.appendChild(node); return node; }
  function cell(text, className = '') { const td=document.createElement('td'); if(className)td.className=className; td.textContent=text==null||text===''?'—':String(text); return td; }
  function setMessage(text, kind='') { message.textContent=text; message.classList.remove('ok','error'); if(kind)message.classList.add(kind); }
  function emptyRow(body, colspan, text) { body.replaceChildren(); const tr=document.createElement('tr'); tr.className='xcat-empty'; const td=document.createElement('td'); td.colSpan=colspan; td.textContent=text; tr.appendChild(td); body.appendChild(tr); }

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

  function renderServices() {
    const rows=filteredServices(); serviceRows.replaceChildren();
    if(!rows.length){emptyRow(serviceRows,6,'当前筛选条件没有服务项目');return;}
    rows.forEach((item)=>{
      const tr=document.createElement('tr');
      const serviceCell=document.createElement('td'); append(serviceCell,'strong',item.name,'xcat-primary'); append(serviceCell,'small',item.slug,'xcat-secondary'); tr.appendChild(serviceCell);
      const gameCell=document.createElement('td'); append(gameCell,'span',item.gameCode,'xcat-primary'); append(gameCell,'small',item.category,'xcat-secondary'); tr.appendChild(gameCell);
      const priceCell=document.createElement('td'); append(priceCell,'span',priceLabel(item),'xcat-primary'); append(priceCell,'small',item.priceMode,'xcat-secondary'); tr.appendChild(priceCell);
      tr.appendChild(cell(item.durationText));
      const statusCell=document.createElement('td'); const badge=append(statusCell,'span',`${item.status} · ${item.active?'启用':'停用'}`,`xcat-status ${item.active?'on':'off'}`); badge.title=item.shortDesc||''; tr.appendChild(statusCell);
      const orderCell=document.createElement('td'); append(orderCell,'span',`展示 ${item.sortOrder}`,'xcat-primary'); append(orderCell,'small',`优先级 ${item.manualPriority} · v${item.version}`,'xcat-secondary'); tr.appendChild(orderCell);
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
    const selected=gameFilter.value; const first=gameFilter.firstElementChild; gameFilter.replaceChildren(first);
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

  async function load() {
    setMessage('正在读取生产服务目录与游戏资料……'); refresh.disabled=true;
    try {
      const response=await fetch('/api/admin/xingwei/catalog/snapshot',{credentials:'same-origin',headers:{'Accept':'application/json'}});
      let body=null; try{body=await response.json();}catch(_){body=null;}
      if(!response.ok) throw new Error(body?.error||body?.message||`HTTP ${response.status}`);
      snapshot=body; render(); status.classList.remove('error'); status.classList.add('ok'); status.querySelector('span').textContent='生产目录已读取';
      setMessage(`已读取 ${snapshot.counts?.services??0} 个服务、${snapshot.counts?.games??0} 个游戏、${snapshot.counts?.categories??0} 个分类。V3A 仅查看，不会修改生产数据。`,'ok');
    } catch(error) {
      status.classList.remove('ok'); status.classList.add('error'); status.querySelector('span').textContent='读取失败'; setMessage(`读取失败或当前账号没有 service:view：${error.message}`,'error');
    } finally { refresh.disabled=false; }
  }

  search.addEventListener('input',renderServices); gameFilter.addEventListener('change',renderServices); stateFilter.addEventListener('change',renderServices); refresh.addEventListener('click',load); load();
})();
