(() => {
  'use strict';
 const display=value=>window.XWLabels?.value(value)??value;
  const $=s=>document.querySelector(s),base='/api/admin/xingwei/catalog-details';let snapshot={},mode='services',busy=false,loading=false,uncertain=false,baseline='',sequence=0,save=null,saveAllowed=()=>true,selected=new Set();
  const node=(tag,text,cls)=>{const n=document.createElement(tag);if(text!=null)n.textContent=String(text);if(cls)n.className=cls;return n;};
  const message=text=>{$('#catalogDetailsMessage').textContent=text||'';};
  async function api(path='',options={}){
    const writing=options.method&&options.method!=='GET',controller=new AbortController();
    const timer=setTimeout(()=>controller.abort(),options.body instanceof FormData?60000:8000);
    try{
      const headers=new Headers(options.headers||{});
      if(writing)headers.set(document.querySelector('meta[name=_csrf_header]').content,document.querySelector('meta[name=_csrf]').content);
      if(options.body!=null&&!(options.body instanceof FormData)){headers.set('Content-Type','application/json');options={...options,body:JSON.stringify(options.body)};}
      const response=await fetch(path.startsWith('/api/')?path:base+path,{...options,headers,signal:controller.signal,cache:'no-store',credentials:'same-origin'});
      let data=null;
      try{if(response.status!==204)data=await response.json();}catch(error){if(response.ok)throw error;}
      if(!response.ok){const error=new Error(data?.message||data?.error||'请求失败（'+response.status+'）');error.status=response.status;throw error;}
      return data;
    }catch(error){if(error.name==='AbortError')error=new Error('请求超时，请重新读取核对。');if(!error.status)error.uncertain=!!writing;throw error;}
    finally{clearTimeout(timer);}
  }
  function fingerprint(){return JSON.stringify([...$('#catalogDetailsFields').querySelectorAll('input,select,textarea')].map(input=>input.type==='file'?[...input.files].map(file=>[file.name,file.size,file.lastModified]):input.type==='checkbox'?input.checked:input.multiple?[...input.selectedOptions].map(option=>option.value):input.value));}
  const dirty=()=>!!save&&$('#catalogDetailsDialog').open&&fingerprint()!==baseline;
  function controls(){
    $('#catalogDetailsFields').disabled=busy||!save;
    $('#catalogDetailsFields').querySelectorAll('button,input[type=file]').forEach(input=>input.disabled=busy||uncertain);
    $('#catalogDetailsSave').disabled=busy||uncertain||!saveAllowed();
    $('#catalogDetailsClose').disabled=busy;
    $('#catalogDetailsFilters').querySelectorAll('input,select,button').forEach(input=>input.disabled=busy||(loading&&input.id!=='catalogDetailsReload'));
    for(const id of ['catalogDetailsBulkMetadata','catalogDetailsBulkVisibility'])$('#'+id).disabled=busy||loading;
  }
  function invalidateView(){snapshot={};selected.clear();$('#catalogDetailsRows').replaceChildren(node('p','操作结果尚未确认，请关闭编辑窗口并重新读取资料。'));$('#catalogDetailsBulk').hidden=true;$('#catalogDetailsSelected').textContent='已选0个游戏';}
  function closeDialog(){if(busy)return false;if(dirty()&&!confirm('资料尚未保存，确定放弃修改和未上传图片吗？'))return false;$('#catalogDetailsDialog').close();return true;}
  const match=state=>({'If-Match':'"'+state.revision+'"'});
  function button(label,action){const b=node('button',label,'xw-button xw-button-ghost');b.type='button';b.onclick=()=>{if(!busy&&!loading&&!uncertain)Promise.resolve(action()).catch(error=>message(error.message));};return b;}
  const limits={slug:100,name:120,nameEn:120,aliases:240,iconKey:40,description:240,publisher:120,genres:160,logoImage:500,coverImage:500,seoSlug:120,seoTitle:160,seoDescription:300,shortDesc:240,durationText:100,reason:500,confirmation:120};
  function field(name,label,value='',type='text',options){if(name==='reason'){value='维护目录资料';type='hidden';}const wrap=node('label',label),input=node(options?'select':type==='textarea'?'textarea':'input');input.name=name;input.id='catalogDetails-'+name;if(options)options.forEach(row=>input.append(new Option(Array.isArray(row)?row[1]:display(row),Array.isArray(row)?row[0]:row)));else if(type!=='textarea')input.type=type;if(type==='checkbox')input.checked=!!value;else input.value=String(value??'');if(limits[name])input.maxLength=limits[name];if(type==='textarea')input.rows=3;if(type==='hidden')wrap.hidden=true;wrap.append(input);$('#catalogDetailsFields').append(wrap);return input;}
  function values(){const data=Object.fromEntries(new FormData($('#catalogDetailsForm')));$('#catalogDetailsFields').querySelectorAll('input[type=checkbox]').forEach(input=>data[input.name]=input.checked);return data;}
  function open(title,build,action){if(busy||loading)return;if($('#catalogDetailsDialog').open&&!closeDialog())return;$('#catalogDetailsFields').replaceChildren();$('#catalogDetailsFormMessage').textContent='';$('#catalogDetailsTitle').textContent=title;save=action;uncertain=false;saveAllowed=()=>true;$('#catalogDetailsSave').hidden=!action;build();baseline=fingerprint();controls();$('#catalogDetailsDialog').showModal();}
  function image(name,label,value,kind){const target=field(name,label,value),wrap=node('div',null,'xcd-wide'),preview=node('img');preview.alt=label+'预览';const render=()=>{const url=target.value.trim();preview.hidden=!(url.startsWith('/')&&!url.startsWith('//')||url.startsWith('https://'));if(!preview.hidden)preview.src=url;};target.addEventListener('input',render);render();wrap.append(preview);if(snapshot.canManage){const file=node('input');file.type='file';file.accept='image/*';file.id='catalogDetails-upload-'+name;wrap.append(file,button('上传'+label,async()=>{if(!file.files?.length)throw new Error('请先选择图片');const body=new FormData();body.append('file',file.files[0]);busy=true;controls();$('#catalogDetailsFormMessage').textContent='正在上传图片…';try{const result=await api('/images/'+kind,{method:'POST',body});if(typeof result?.url!=='string'||!result.url){const error=new Error('上传响应不完整');error.uncertain=true;throw error;}target.value=result.url;file.value='';render();$('#catalogDetailsFormMessage').textContent='图片已上传，请保存资料使其生效。';}catch(error){uncertain=!!error.uncertain;if(uncertain)invalidateView();$('#catalogDetailsFormMessage').textContent=uncertain?'图片上传结果尚未确认，请关闭后重新读取核对。':error.message;}finally{busy=false;controls();}}));}$('#catalogDetailsFields').append(wrap);return target;}
  function number(name,label,value,min, max){const input=field(name,label,value,'number');input.step='1';if(min!=null)input.min=min;if(max!=null)input.max=max;return input;}
  function reason(){field('reason','修改说明','','textarea').required=true;}
  const nullable=value=>value===''||value==null?null:value;
  const integer=value=>value===''||value==null?null:Number(value);
  function localTime(value){if(!value)return '';const date=new Date(value);return Number.isNaN(date.getTime())?'':new Date(date.getTime()-date.getTimezoneOffset()*60000).toISOString().slice(0,16);}
  function editor(state){const d=state.data,manage=snapshot.canManage,kind=mode;
    open((manage?'编辑':'查看')+(kind==='services'?'服务':'游戏')+' · '+d.name,()=>{
      if(kind==='services'){
        field('name','服务名称',d.name).required=true;field('slug','服务地址标识',d.slug).required=true;
        const game=field('gameCode','所属游戏',d.gameCode,'text',(snapshot.games||[]).map(item=>[item.data.code,item.data.name]));const category=field('category','所属分类',d.category,'text',[]);category.required=true;
        const categories=()=>{category.replaceChildren(...(snapshot.categories||[]).filter(item=>item.data.gameCode===game.value).map(item=>new Option(item.data.name,item.data.code)));if([...category.options].some(option=>option.value===d.category))category.value=d.category;};game.onchange=categories;categories();
        field('shortDesc','服务说明',d.shortDesc,'textarea').required=true;field('priceMode','价格模式',d.priceMode,'text',['QUOTE','FIXED','RANGE']);for(const [name,label] of [['priceFixed','固定价'],['priceMin','最低价'],['priceMax','最高价'],['hotScore','热度']]){const input=field(name,label,d[name],'number');input.min=0;input.step='0.01';}
        field('durationText','预计时长',d.durationText).required=true;number('manualPriority','人工优先级',d.manualPriority);number('sortOrder','展示排序',d.sortOrder);field('expiresAt','有效期（本地时间，留空长期有效）',localTime(d.expiresAt),'datetime-local');
        const statuses=d.active&&d.status!=='OFFLINE'?['AVAILABLE','BUSY']:['OFFLINE','AVAILABLE','BUSY'];field('status','服务状态',d.status,'text',statuses);image('coverImage','服务封面',d.coverImage,'service');
        $('#catalogDetailsFields').append(node('p','资料保存保留当前启用开关。有效期和归属会影响前台展示；启用或停用请回到服务目录执行发布检查。','xcd-wide'));
      }else{
        for(const [name,label] of [['name','游戏名称'],['nameEn','英文名'],['aliases','别名'],['publisher','发行方'],['genres','游戏类型']])field(name,label,d[name]);$('#catalogDetails-name').maxLength=80;$('#catalogDetails-name').required=true;
        field('description','游戏说明',d.description,'textarea');number('popularityScore','热度（0–100）',d.popularityScore,0,100);field('featured','重点游戏',d.featured,'checkbox');field('businessStatus','业务状态',d.businessStatus,'text',['ACTIVE','READY','PLANNED','PAUSED']);number('sortOrder','展示排序',d.sortOrder);
        const platforms=field('platformCodes','支持平台','','text',(snapshot.platforms||[]).map(item=>[item.code,item.name]));platforms.multiple=true;platforms.size=Math.min(8,Math.max(2,platforms.options.length));[...platforms.options].forEach(option=>option.selected=(d.platformCodes||[]).includes(option.value));
        image('logoImage','游戏 Logo',d.logoImage,'logo');image('coverImage','游戏封面',d.coverImage,'cover');field('seoSlug','搜索地址标识',d.seoSlug);field('seoTitle','搜索标题',d.seoTitle);field('seoDescription','搜索说明',d.seoDescription,'textarea');
        $('#catalogDetailsFields').append(node('p','此处维护资料并保留启用状态；启停请使用游戏与分类工作台的影响预览。','xcd-wide'));
      }
      if(manage)reason();
    },manage?()=>{
      const data=values();if(kind==='services'){const body={...data,active:d.active};for(const key of ['priceFixed','priceMin','priceMax','hotScore'])body[key]=nullable(data[key]);for(const key of ['sortOrder','manualPriority'])body[key]=integer(data[key]);body.expiresAt=data.expiresAt?new Date(data.expiresAt).toISOString():null;body.coverImage=nullable(data.coverImage);return api('/services/'+d.id,{method:'PUT',headers:match(state),body});}
      const body={...data,iconKey:d.iconKey??null,active:d.active,popularityScore:integer(data.popularityScore),sortOrder:integer(data.sortOrder),platformCodes:[...$('#catalogDetails-platformCodes').selectedOptions].map(option=>option.value)};for(const key of ['nameEn','aliases','iconKey','description','publisher','genres','logoImage','coverImage','seoSlug','seoTitle','seoDescription'])body[key]=nullable(body[key]);return api('/games/'+encodeURIComponent(d.code),{method:'PUT',headers:match(state),body});
    }:null);
  }
  function remove(state){const d=state.data,key=mode==='games'?d.code:String(d.id);open('永久删除 '+d.name,()=>{field('confirmation','准确输入名称「'+d.name+'」确认').required=true;$('#catalogDetailsFields').append(node('p','仅无关联记录的资料可以删除。请核对名称，此操作无法在本页恢复。','xcd-wide'));},()=>api('/'+mode+'/'+encodeURIComponent(key),{method:'DELETE',headers:match(state),body:values()}));}
  function selectedItems(){const items=(snapshot.games||[]).filter(item=>selected.has(item.data.code));if(!items.length||items.length>100)throw new Error('请选择1至100个游戏');return items;}
  function bulk(visibility){const items=selectedItems();let prepared=null;
    open(visibility?'批量启停：先核对影响':'批量更新游戏资料',()=>{
      if(visibility){const active=field('active','目标状态','true','text',[['true','启用'],['false','停用']]);const area=node('div',null,'xcd-wide'),ack=field('acknowledge','已核对所有选中游戏的公开影响',false,'checkbox');$('#catalogDetailsSave').disabled=true;saveAllowed=()=>!!prepared&&ack.checked;ack.onchange=controls;active.onchange=()=>{prepared=null;ack.checked=false;area.replaceChildren();controls();};
        $('#catalogDetailsFields').append(button('读取影响预览',async()=>{prepared=null;ack.checked=false;$('#catalogDetailsSave').disabled=true;const desired=active.value;busy=true;controls();$('#catalogDetailsFormMessage').textContent='正在读取影响预览…';try{const impacts=await Promise.all(items.map(item=>api('/api/admin/xingwei/catalog/taxonomy/games/'+encodeURIComponent(item.data.code)+'/impact')));area.replaceChildren(...impacts.map(impact=>node('p',impact.name+' · '+(String(impact.active)===desired?'已处于目标状态，无需变更':impact.visibilitySummary))));prepared=impacts;$('#catalogDetailsFormMessage').textContent='已读取影响，请核对后确认。';}catch(error){$('#catalogDetailsFormMessage').textContent=error.message;}finally{busy=false;controls();}}),area);
      }else{field('featured','重点游戏','','text',[['','保持不变'],['true','是'],['false','否']]);field('businessStatus','业务状态','','text',['','ACTIVE','READY','PLANNED','PAUSED']);number('popularityScore','热度（留空保持不变）','',0,100);}
      reason();
    },()=>{const data=values();if(visibility&&(!prepared||!data.acknowledge))throw new Error('请先读取并确认影响预览');const body={items:items.map((item,index)=>({code:item.data.code,revision:item.revision,impactToken:prepared?.[index]?.impactToken})),reason:data.reason};if(visibility)body.active=data.active==='true';else{body.featured=data.featured===''?null:data.featured==='true';body.businessStatus=nullable(data.businessStatus);body.popularityScore=integer(data.popularityScore);}return api('/games/bulk',{method:'POST',body});});
  }
  function render(){mode=$('#catalogDetailsMode').value;const query=$('#catalogDetailsQuery').value.trim().toLowerCase();const rows=(snapshot[mode]||[]).filter(item=>!query||[item.data.name,item.data.nameEn,item.data.slug,item.data.description,item.data.shortDesc].filter(Boolean).join(' ').toLowerCase().includes(query));$('#catalogDetailsRows').replaceChildren(...rows.map(state=>{const d=state.data,row=node('article',null,'xcd-record');if(mode==='games'&&snapshot.canManage){const label=node('label','选择','xcd-select'),check=node('input');check.type='checkbox';check.checked=selected.has(d.code);check.onchange=()=>{check.checked?selected.add(d.code):selected.delete(d.code);$('#catalogDetailsSelected').textContent='已选'+selected.size+'个游戏';};label.prepend(check);row.append(label);}row.append(node('h3',d.name||d.slug),node('p',[(snapshot.games||[]).find(item=>item.data.code===d.gameCode)?.data.name,display(d.status||d.businessStatus),d.active?'启用':'停用',d.expiresAt].filter(value=>value!=null&&value!=='').join(' · ')));
    const actions=node('div',null,'xwi-actions');if(mode!=='categories')actions.append(button(snapshot.canManage?'完整资料与图片':'查看资料',()=>editor(state)));else{const link=node('a','编辑分类资料','xw-button xw-button-ghost');link.href='/admin/xingwei/taxonomy';actions.append(link);}if(snapshot.restoreAdmin&&snapshot.canManage&&mode!=='services')actions.append(button('永久删除',()=>remove(state)));row.append(actions);return row;}));if(!rows.length)$('#catalogDetailsRows').append(node('p','暂无符合条件的资料。'));$('#catalogDetailsBulk').hidden=mode!=='games'||!snapshot.canManage;$('#catalogDetailsSelected').textContent='已选'+selected.size+'个游戏';}
  async function load(){
    const id=++sequence;loading=true;snapshot={};selected.clear();
    $('#catalogDetailsRows').replaceChildren();$('#catalogDetailsRows').setAttribute('aria-busy','true');$('#catalogDetailsBulk').hidden=true;$('#catalogDetailsSelected').textContent='已选0个游戏';message('正在读取完整资料…');controls();
    try{
      const result=await api();if(id!==sequence)return false;
      if(!result||!Array.isArray(result.services)||!Array.isArray(result.games)||!Array.isArray(result.categories))throw new Error('资料响应不完整，请重新读取。');
      snapshot=result;render();message(snapshot.canManage?'完整资料保存会核对版本。发布及启停仍使用受控工作流。':'当前只能查看目录资料。');return true;
    }catch(error){if(id!==sequence)return false;$('#catalogDetailsRows').replaceChildren(node('p','读取失败，当前未展示旧记录。请点击重新读取。'));throw error;}
    finally{if(id===sequence){loading=false;$('#catalogDetailsRows').setAttribute('aria-busy','false');controls();}}
  }
  $('#catalogDetailsForm').onsubmit=async event=>{
    event.preventDefault();if(busy||loading||uncertain||!save||!saveAllowed())return;
    if([...$('#catalogDetailsFields').querySelectorAll('input[type=file]')].some(input=>input.files.length)&&!confirm('仍有已选择但未上传的图片，本次保存不会包含这些图片。确定继续保存吗？'))return;
    busy=true;
    try{
      $('#catalogDetailsFormMessage').textContent='正在保存，请稍候…';const pending=save();controls();await pending;baseline=fingerprint();$('#catalogDetailsDialog').close();
      try{const refreshed=await load();message(refreshed?'保存成功。':'已保存，请重新读取列表核对。');}catch(error){message('已保存，但列表刷新失败：'+error.message);}
    }catch(error){uncertain=!!error.uncertain;if(uncertain)invalidateView();$('#catalogDetailsFormMessage').textContent=uncertain?'保存结果尚未确认，请勿重复提交。请关闭后重新读取资料核对。':error.message;}
    finally{busy=false;controls();}
  };
  $('#catalogDetailsClose').onclick=closeDialog;
  $('#catalogDetailsDialog').addEventListener('cancel',event=>{event.preventDefault();closeDialog();});
  $('#catalogDetailsDialog').addEventListener('close',()=>{if($('#catalogDetailsDialog').open)return;save=null;baseline='';uncertain=false;$('#catalogDetailsFields').replaceChildren();});
  window.addEventListener('beforeunload',event=>{if(busy||dirty()){event.preventDefault();event.returnValue='';}});
  $('#catalogDetailsFilters').onsubmit=event=>{event.preventDefault();if(!busy&&!loading&&!$('#catalogDetailsDialog').open)render();};
  $('#catalogDetailsMode').onchange=()=>{if(busy||loading||$('#catalogDetailsDialog').open){$('#catalogDetailsMode').value=mode;return;}selected.clear();render();};
  $('#catalogDetailsReload').onclick=()=>{if(!busy&&(!$('#catalogDetailsDialog').open||closeDialog()))load().catch(error=>message(error.message));};
  $('#catalogDetailsBulkMetadata').onclick=()=>{if(!busy&&!loading)try{bulk(false);}catch(error){message(error.message);}};
  $('#catalogDetailsBulkVisibility').onclick=()=>{if(!busy&&!loading)try{bulk(true);}catch(error){message(error.message);}};
  load().catch(error=>message(error.message));
})();
