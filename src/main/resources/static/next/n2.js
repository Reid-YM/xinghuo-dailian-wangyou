(() => {
  'use strict';

  const $=(selector,root=document)=>root.querySelector(selector);
  const $$=(selector,root=document)=>[...root.querySelectorAll(selector)];
  const state={catalog:null,services:null,category:'all',query:''};
  const text=value=>String(value??'').trim();
  const code=value=>text(value).toUpperCase();
  const list=value=>Array.isArray(value)?value:[];

  function say(message){
    if(typeof window.toast==='function') window.toast(message);
  }

  async function getJson(path){
    const response=await fetch(path,{method:'GET',headers:{Accept:'application/json'},credentials:'same-origin'});
    if(!response.ok) throw new Error(`HTTP ${response.status}`);
    return response.json();
  }

  function make(tag,className,value){
    const element=document.createElement(tag);
    if(className) element.className=className;
    if(value!==undefined) element.textContent=String(value);
    return element;
  }

  function gameName(gameCode){
    const row=list(state.catalog?.games).find(item=>code(item?.code)===code(gameCode));
    return text(row?.name)||text(gameCode)||'游戏服务';
  }

  function categoryRow(categoryCode,gameCode){
    const categories=list(state.catalog?.categories);
    return categories.find(item=>code(item?.code)===code(categoryCode)&&code(item?.gameCode)===code(gameCode))
      || categories.find(item=>code(item?.code)===code(categoryCode));
  }

  function categoryName(categoryCode,gameCode){
    const row=categoryRow(categoryCode,gameCode);
    return text(row?.name)||text(categoryCode)||'未分类';
  }

  function price(service){
    const min=Number(service?.priceMin),max=Number(service?.priceMax);
    const hasMin=Number.isFinite(min)&&min>0,hasMax=Number.isFinite(max)&&max>0;
    if(hasMin&&hasMax&&max>min) return `¥${min}–${max}`;
    if(hasMin&&hasMax) return `¥${min}`;
    if(hasMin) return `¥${min} 起`;
    if(hasMax) return `¥${max} 内`;
    return '询价';
  }

  function safeImage(value){
    const url=text(value);
    return url.startsWith('/')||url.startsWith('https://')?url:'';
  }

  function serviceCard(service){
    const article=make('article','serviceCard');
    article.dataset.cat=code(service?.category).toLowerCase()||'uncategorized';
    article.dataset.game=code(service?.gameCode);
    article.dataset.serviceId=text(service?.id);
    article.dataset.serviceSlug=text(service?.slug);

    const thumb=make('div','thumb');
    const cover=safeImage(service?.coverImage);
    if(cover){
      const image=document.createElement('img');
      image.src=cover;
      image.alt=text(service?.name)||'服务封面';
      image.loading='lazy';
      image.style.cssText='width:100%;height:100%;object-fit:cover;opacity:.78;display:block';
      thumb.append(image);
    }

    const body=make('div','serviceBody');
    body.append(make('h3','',text(service?.name)||'未命名服务'));
    body.append(make('p','',text(service?.shortDesc)||text(service?.durationText)||`${gameName(service?.gameCode)} · ${categoryName(service?.category,service?.gameCode)}`));
    body.append(make('div','price',price(service)));

    const actions=make('div','serviceBtns');
    const detail=make('button','btn','详情');
    detail.type='button';
    detail.dataset.n2Detail='1';
    const quote=make('button','btn gold','询价');
    quote.type='button';
    quote.dataset.n2Quote='1';
    actions.append(detail,quote);
    body.append(actions);
    article.append(thumb,body);
    return article;
  }

  function renderServices(){
    const grid=$('.serviceGrid');
    if(!grid||!Array.isArray(state.services)||!state.services.length) return;
    const fragment=document.createDocumentFragment();
    state.services.forEach(service=>fragment.append(serviceCard(service)));
    grid.replaceChildren(fragment);
    renderCategories();
    applyFilter();
  }

  function renderCategories(){
    const side=$('.services .side');
    if(!side||!Array.isArray(state.services)||!state.services.length) return;
    const title=make('b','','服务分类');
    const counts=new Map();
    state.services.forEach(service=>{
      const key=code(service?.category).toLowerCase()||'uncategorized';
      const current=counts.get(key)||{key,label:categoryName(service?.category,service?.gameCode),count:0};
      current.count+=1;
      counts.set(key,current);
    });
    const fragment=document.createDocumentFragment();
    fragment.append(title);
    const all=make('div','cat active',`全部服务 (${state.services.length})`);
    all.dataset.cat='all';
    fragment.append(all);
    [...counts.values()].sort((a,b)=>a.label.localeCompare(b.label,'zh-CN')).forEach(row=>{
      const item=make('div','cat',`${row.label} (${row.count})`);
      item.dataset.cat=row.key;
      fragment.append(item);
    });
    side.replaceChildren(fragment);
  }

  function applyFilter(){
    const query=state.query.toLowerCase();
    $$('.serviceGrid .serviceCard').forEach(card=>{
      const categoryOk=state.category==='all'||card.dataset.cat===state.category;
      const queryOk=!query||card.textContent.toLowerCase().includes(query);
      card.style.display=categoryOk&&queryOk?'block':'none';
    });
  }

  function setOptions(select,rows,valueOf,labelOf,emptyLabel){
    if(!select) return;
    const previous=select.value;
    const fragment=document.createDocumentFragment();
    rows.forEach(row=>{
      const option=document.createElement('option');
      option.value=text(valueOf(row));
      option.textContent=text(labelOf(row))||option.value;
      fragment.append(option);
    });
    if(!rows.length){
      const option=document.createElement('option');
      option.value='';
      option.textContent=emptyLabel;
      fragment.append(option);
    }
    select.replaceChildren(fragment);
    if([...select.options].some(option=>option.value===previous)) select.value=previous;
  }

  function matchingServices(gameCode,categoryCode){
    return list(state.services).filter(service=>(!gameCode||code(service?.gameCode)===code(gameCode))&&(!categoryCode||code(service?.category)===code(categoryCode)));
  }

  function syncSummary(){
    const pairs=[['qGame','sGame'],['qCat','sCat'],['qSvc','sSvc']];
    pairs.forEach(([from,to])=>{
      const select=$(`#${from}`),target=$(`#${to}`);
      if(select&&target) target.textContent=select.selectedOptions[0]?.textContent||'待选择';
    });
  }

  function populateServices(){
    const game=$('#qGame')?.value||'';
    const category=$('#qCat')?.value||'';
    let rows=matchingServices(game,category);
    if(!rows.length) rows=matchingServices(game,'');
    setOptions($('#qSvc'),rows,row=>row?.slug||row?.id||row?.name,row=>row?.name||row?.slug,'暂无服务');
    syncSummary();
  }

  function populateCategories(){
    if(!state.catalog) return;
    const game=$('#qGame')?.value||'';
    const rows=list(state.catalog.categories).filter(row=>!game||code(row?.gameCode)===code(game));
    setOptions($('#qCat'),rows,row=>row?.code,row=>row?.name||row?.code,'暂无分类');
    populateServices();
  }

  function populateQuote(){
    if(!state.catalog) return;
    setOptions($('#qGame'),list(state.catalog.games),row=>row?.code,row=>row?.name||row?.code,'暂无游戏');
    populateCategories();
  }

  function chooseService(card){
    if(!Array.isArray(state.services)) return;
    const service=state.services.find(item=>(card.dataset.serviceSlug&&text(item?.slug)===card.dataset.serviceSlug)||(card.dataset.serviceId&&text(item?.id)===card.dataset.serviceId));
    if(!service) return;
    const game=$('#qGame'),category=$('#qCat'),serviceSelect=$('#qSvc');
    if(game&&[...game.options].some(option=>code(option.value)===code(service.gameCode))) game.value=text(service.gameCode);
    populateCategories();
    if(category&&[...category.options].some(option=>code(option.value)===code(service.category))) category.value=text(service.category);
    populateServices();
    const value=text(service.slug||service.id||service.name);
    if(serviceSelect&&[...serviceSelect.options].some(option=>option.value===value)) serviceSelect.value=value;
    syncSummary();
  }

  function status(message,kind){
    let node=$('#n2DataStatus');
    if(!node){
      node=make('div','',message);
      node.id='n2DataStatus';
      node.style.cssText='width:min(1420px,calc(100% - 40px));margin:12px auto 0;padding:10px 13px;border:1px solid #ffcb2d33;border-radius:10px;background:#ffcb2d0b;color:#aeb4bc;font-size:11px';
      $('.toolbar')?.after(node);
    }
    node.textContent=message;
    node.style.borderColor=kind==='ok'?'#39d98a44':'#ff9d2e55';
    node.style.color=kind==='ok'?'#8fe7b5':'#f2bd7b';
  }

  $('.services .side')?.addEventListener('click',event=>{
    const item=event.target.closest('.cat[data-cat]');
    if(!item) return;
    $$('.services .side .cat').forEach(row=>row.classList.remove('active'));
    item.classList.add('active');
    state.category=item.dataset.cat||'all';
    applyFilter();
  });

  $('#search')?.addEventListener('input',event=>{
    state.query=event.target.value.trim();
    applyFilter();
  });

  $('.serviceGrid')?.addEventListener('click',event=>{
    const card=event.target.closest('.serviceCard');
    if(!card) return;
    if(event.target.closest('[data-n2-detail]')){
      say('服务详情路由将在下一阶段接入');
      return;
    }
    if(event.target.closest('[data-n2-quote]')){
      chooseService(card);
      if(typeof window.go==='function') window.go('quote');
      else location.hash='quote';
    }
  });

  $('#qGame')?.addEventListener('change',populateCategories);
  $('#qCat')?.addEventListener('change',populateServices);
  $('#qSvc')?.addEventListener('change',syncSummary);

  async function load(){
    const [catalog,services]=await Promise.allSettled([getJson('/api/catalog'),getJson('/api/services')]);
    if(catalog.status==='fulfilled') state.catalog=catalog.value;
    if(services.status==='fulfilled'&&Array.isArray(services.value)) state.services=services.value;
    if(state.services?.length) renderServices();
    if(state.catalog) populateQuote();
    if(state.catalog&&Array.isArray(state.services)){
      status(`N2 已连接公开接口：${list(state.catalog.games).length} 个游戏 · ${list(state.catalog.categories).length} 个分类 · ${state.services.length} 个服务（只读）`,'ok');
    }else if(state.catalog||Array.isArray(state.services)){
      status('N2 部分公开接口暂不可用，已加载可用数据，其余继续使用静态回退。','warn');
    }else{
      status('N2 公开接口读取失败，当前继续使用静态回退内容，不影响旧前端。','warn');
    }
    const badge=$('.badge');
    if(badge) badge.textContent='FRONTEND NEXT · N2 READ-ONLY';
  }

  window.XHFrontendNextN2={reload:load,state};
  load().catch(()=>status('N2 公开接口读取失败，当前继续使用静态回退内容。','warn'));
})();
