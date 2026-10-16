(() => {
  'use strict';
  const $=id=>document.getElementById(id), selector=$('siteSection'), form=$('siteForm');
  const text=(key,label,max=240,required=false)=>({key,label,type:'text',max,required});
  const number=(key,label,min,max)=>({key,label,type:'number',min,max});
  const check=(key,label)=>({key,label,type:'checkbox'});
  const choice=(key,label,options)=>({key,label,type:'select',options});
  const area=(key,label,max=500)=>({key,label,type:'textarea',max,required:['heroSubtitle','contactIntro','contactNotice','quoteSuccessText','orderHelpText','footerText','answer'].includes(key)});
  const image=(key,label)=>({...text(key,label,500),image:true});
  const repeat=(key,label,max,fields)=>({key,label,type:'repeat',max,fields});
  const fit=[['cover','裁切填满'],['contain','完整显示']];
  const schemas={
    basic:[text('brandName','品牌名称',80,true),text('legalEntityName','经营主体',120,true),choice('serviceStatus','全站兜底接单状态',[['RESERVATION','预约中'],['PREHEAT','预热'],['OPEN','正常接单'],['SUSPENDED','暂停接单'],['MAINTENANCE','维护中']]),text('statusShortText','状态短说明（游戏未单独配置时使用）',120),text('primaryButtonLabel','主操作按钮文案',40),check('quoteEnabled','允许提交询价'),check('orderQueryEnabled','允许查询订单')],
    home:[text('heroTitle','首页主标题',120,true),area('heroSubtitle','首页副标题',240),choice('startingPriceMode','起步价来源',[['auto','按服务价格自动计算'],['manual','手动价格']]),number('startingPrice','手动起步价（元）',0,9999999),check('showStartingPrice','显示起步价'),number('homeServiceLimit','首页服务数量',2,8),{key:'trustTags',label:'信任标签（每行一项，最多6项，每项30字）',type:'tags',max:300}],
    visual:[image('heroImage','首页主图地址'),number('heroImageHeight','主图高度（像素）',80,420),choice('heroImageFit','主图显示方式',fit),number('heroImagePositionX','主图水平焦点（%）',0,100),number('heroImagePositionY','主图垂直焦点（%）',0,100),image('heroBackgroundImage','首页背景图地址'),number('heroBackgroundOpacity','背景不透明度（%）',0,100),choice('heroBackgroundFit','背景显示方式',fit),number('heroBackgroundPositionX','背景水平焦点（%）',0,100),number('heroBackgroundPositionY','背景垂直焦点（%）',0,100),number('heroBackgroundBlur','背景模糊（像素）',0,24),number('serviceCoverHeight','服务封面高度（像素）',70,240),choice('serviceCoverFit','服务封面显示方式',fit),number('serviceCoverPositionX','服务封面水平焦点（%）',0,100),number('serviceCoverPositionY','服务封面垂直焦点（%）',0,100)],
    notice:[check('announcementEnabled','显示公告'),area('announcementText','公告内容',240)],
    contact:[text('contactTitle','联系页标题',80,true),area('contactIntro','联系页说明',300),text('wechatId','主客服微信号',100),image('wechatQrImage','主客服二维码地址'),number('wechatQrSize','二维码尺寸（像素）',120,420),text('qqId','QQ号',100),text('qqLink','QQ咨询地址',500),text('phone','联系电话',50),text('serviceHours','服务时间',100),text('taobaoUrl','淘宝店铺地址',500),text('xianyuUrl','闲鱼主页地址',500),area('contactNotice','联系提示',500),text('wecomKefuUrl','企业微信客服入口',500),area('wecomKefuNote','企业微信客服说明',300),text('quickContactTitle','快捷联系标题',40),text('taobaoLabel','淘宝入口文案',40),text('xianyuLabel','闲鱼入口文案',40),text('wecomKefuLabel','企微入口标题',40),text('wecomKefuButton','企微按钮文案',40),repeat('contactChannels','客服渠道（清空后沿用默认客服）',6,[text('label','客服名称',40),text('wechatId','微信号',100),image('qrImage','二维码地址'),area('note','提示说明',240),text('jumpUrl','跳转地址',500)])],
    trust:[repeat('trustCases','服务案例（清空后恢复默认案例）',8,[text('tag','案例分类',40),text('title','案例标题',100),text('metric','交付或价格摘要',120),area('detail','案例说明',240)]),repeat('trustFaq','常见问题（清空后恢复默认问题）',12,[text('question','问题',100,true),area('answer','回答',400)])],
    copy:[area('quoteSuccessText','询价提交成功提示',300),area('orderHelpText','订单查询帮助',300),area('footerText','页脚文案',200)],
    seo:[text('seoTitle','搜索标题',160),area('seoDescription','搜索描述',300)],
    tools:[check('toolPublicEnabled','允许公开访问工具站'),text('toolUpdatedNote','工具站更新说明',120)],
    games:[choice('statusCode','服务状态',[['OPEN','可接服务'],['PREORDER','接受预约'],['PAUSED','暂停服务'],['MAINTENANCE','维护中'],['LIMITED','限量服务']]),text('publicLabel','前台状态文案',80,true),area('publicDetail','状态详情',240),text('ctaLabel','主按钮文案',40)]
  };
  let cache=null, revision='', access={}, dirty=false, busy=false, serial=0, game='';
  const section=()=>selector.value;
  const endpoint=()=>'/api/admin/xingwei/site/'+(section()==='games'?'games':'site');
  function el(tag,label){const n=document.createElement(tag);if(label!==undefined)n.textContent=label;return n;}
  function canWrite(){return section()==='seo'?access.canManageSeo===true:access.canManageSite===true;}
  async function request(url,method='GET',body){
    const headers={Accept:'application/json'};
    if(body!==undefined){headers['Content-Type']='application/json';headers['If-Match']=revision;const token=document.querySelector('meta[name=_csrf]')?.content;if(token)headers[document.querySelector('meta[name=_csrf_header]')?.content||'X-CSRF-TOKEN']=token;}
    const controller=new AbortController(),timer=setTimeout(()=>controller.abort(),12000);
    try{const response=await fetch(url,{method,headers,body:body===undefined?undefined:JSON.stringify(body),signal:controller.signal,credentials:'same-origin',cache:'no-store'});const data=await response.json().catch(()=>({}));if(!response.ok||response.redirected)throw new Error(data.error||data.message||(response.status===403?'没有配置访问权限。':'读取或保存失败，请重试。'));return {data,revision:response.headers?.get?.('ETag')||''};}
    catch(error){if(error.name==='AbortError')throw new Error('请求超时；如果正在保存，请先重新读取核对结果。');throw error;}finally{clearTimeout(timer);}
  }
  function currentValues(){return section()==='games'?(cache?.find(row=>row.gameCode===game)||{}):(cache||{});}
  function field(def,value,nested=false){
    if(def.type==='repeat'){
      const box=el('fieldset');box.dataset.repeat=def.key;box.id='site-field-'+def.key;box.append(el('legend',def.label));
      const rows=el('section');box.append(rows);const add=el('button','新增一项');add.type='button';add.className='xw-button xw-button-ghost';
      function append(data={}){const row=el('div');row.dataset.repeatRow='true';for(const child of def.fields)row.append(field(child,data[child.key],true));const remove=el('button','移除此项');remove.type='button';remove.addEventListener('click',()=>{row.remove();add.disabled=!canWrite()||rows.children.length>=def.max;dirty=true;});row.append(remove);rows.append(row);add.disabled=!canWrite()||rows.children.length>=def.max;}
      for(const row of Array.isArray(value)?value:[])append(row);add.disabled=!canWrite()||rows.children.length>=def.max;
      add.addEventListener('click',()=>{if(rows.children.length<def.max){append();dirty=true;}});box.append(add);return box;
    }
    const label=el('label',def.label),input=el(def.type==='textarea'||def.type==='tags'?'textarea':def.type==='select'?'select':'input');
    input.dataset.field=def.key;if(!nested)input.id='site-field-'+def.key;
    if(def.type==='select')for(const [code,name]of def.options){const o=el('option',name);o.value=code;input.append(o);}
    else if(def.type!=='textarea'&&def.type!=='tags')input.type=def.type;
    if(def.type==='checkbox')input.checked=value===true||value==='true';else input.value=def.type==='tags'?(Array.isArray(value)?value.join('\n'):''):value??'';
    if(def.type==='number'){input.min=String(def.min);input.max=String(def.max);input.step=def.key==='startingPrice'?'0.01':'1';}
    if(def.max&&def.type!=='number')input.maxLength=def.max;if(def.required)input.required=true;
    label.append(input);
    if(def.image){const preview=el('img');preview.alt='当前图片预览';preview.hidden=true;const note=el('small','填写 https:// 图片地址或 / 开头的站内路径。');
      function show(){const url=input.value.trim();const safe=/^https?:\/\//i.test(url)||(/^\/(?!\/)/.test(url)&&!url.includes('\\'));preview.hidden=!safe;if(safe)preview.src=url;}
      preview.addEventListener('error',()=>{preview.hidden=true;note.textContent='图片暂时无法读取，请核对地址。';});input.addEventListener('input',show);label.append(note,preview);show();
      const upload=el('input');upload.type='file';upload.accept='image/png,image/jpeg,image/webp';upload.setAttribute('aria-label','上传'+def.label.replace('地址',''));
      upload.addEventListener('change',async()=>{
        if(!upload.files?.length||busy||!canWrite())return;
        const payload=new FormData();payload.append('file',upload.files[0]);const headers={Accept:'application/json'},token=document.querySelector('meta[name=_csrf]')?.content;
        if(token)headers[document.querySelector('meta[name=_csrf_header]')?.content||'X-CSRF-TOKEN']=token;
        busy=true;selector.disabled=true;$('siteReload').disabled=true;for(const c of form.querySelectorAll('input,select,textarea,button'))c.disabled=true;
        $('siteMessage').textContent='正在上传图片…';
        const controller=new AbortController(),timer=setTimeout(()=>controller.abort(),30000);
        try{const response=await fetch('/api/admin/xingwei/site/images/'+def.key,{method:'POST',headers,body:payload,signal:controller.signal,credentials:'same-origin',cache:'no-store'});const data=await response.json().catch(()=>({}));if(!response.ok||!data.url)throw new Error(data.error||data.message||'图片上传失败');input.value=data.url;input.dispatchEvent(new Event('input',{bubbles:true}));$('siteMessage').textContent='图片已上传；保存当前分区后才会在前台生效。';}
        catch(error){$('siteMessage').textContent=error.name==='AbortError'?'图片上传超时，请重试。':error.message;}
        finally{clearTimeout(timer);upload.value='';busy=false;selector.disabled=false;$('siteReload').disabled=false;for(const c of form.querySelectorAll('input,select,textarea,button'))c.disabled=!canWrite();}
      });label.append(upload);}
    return label;
  }
  function render(){
    const target=$('siteFields');target.replaceChildren();
    if(section()==='games'){
      const label=el('label','选择游戏'),select=el('select');select.id='siteGame';for(const row of cache||[]){const o=el('option',row.gameCode);o.value=row.gameCode;select.append(o);}select.value=game;
      select.addEventListener('change',()=>{if(dirty&&!confirm('切换游戏会放弃未保存的输入，是否继续？')){select.value=game;return;}game=select.value;dirty=false;render();});label.append(select);target.append(label);
    }
    const values=currentValues();for(const def of schemas[section()])target.append(field(def,values[def.key]));
    for(const control of form.querySelectorAll('input,select,textarea,button'))control.disabled=!canWrite()||(section()==='games'&&!game);
    $('siteSave').disabled=!canWrite()||!revision||(section()==='games'&&!game);dirty=false;
    // The form only exists after access and configuration have loaded.
    const anchor=location.hash.slice(1);
    if(anchor.startsWith('site-field-')){
      const found=document.getElementById(anchor);
      if(found){found.classList.add('xw-located-field');found.scrollIntoView({block:'center'});if(!found.disabled){if(found.tagName==='FIELDSET')found.tabIndex=-1;found.focus({preventScroll:true});}}
    }
  }
  async function load(){
    const id=++serial;busy=true;selector.disabled=true;$('siteReload').disabled=true;$('siteSave').disabled=true;$('siteMessage').textContent='正在读取配置…';
    try{access=(await request('/api/admin/xingwei/site/access')).data;
      if(access.canViewSite===false){for(const option of selector.options)option.hidden=option.value!=='seo';selector.value='seo';}
      const result=await request(endpoint());if(id!==serial)return;cache=result.data;revision=result.revision;
      if(section()==='games'&&(!game||!cache.some(x=>x.gameCode===game)))game=cache[0]?.gameCode||'';
      render();$('siteMessage').textContent=canWrite()?'已读取配置。修改后请填写原因并保存当前分区。':'当前账号只读。';
    }catch(error){if(id===serial){$('siteMessage').textContent=error.message;$('siteSave').disabled=true;}}
    finally{if(id===serial){busy=false;selector.disabled=false;$('siteReload').disabled=false;}}
  }
  function value(def,root){
    if(def.type==='repeat')return [...root.querySelector(`[data-repeat="${def.key}"]`).querySelectorAll('[data-repeat-row]')].map(row=>Object.fromEntries(def.fields.map(child=>[child.key,value(child,row)])));
    const input=root.querySelector(`[data-field="${def.key}"]`);if(def.type==='checkbox')return input.checked;if(def.type==='tags'){const tags=input.value.split(/\r?\n/).map(x=>x.trim()).filter(Boolean);if(tags.length>6||tags.some(t=>t.length>30))throw new Error('信任标签最多6项，每项30字');return tags;}if(def.type==='number')return input.value===''?null:Number(input.value);return input.value.trim();
  }
  form.addEventListener('input',()=>{dirty=true;});
  form.addEventListener('submit',async event=>{event.preventDefault();if(busy||!canWrite()||!revision)return;let body;
    try{body=Object.fromEntries(schemas[section()].map(def=>[def.key,value(def,$('siteFields'))]));body.reason=form.elements.namedItem('reason').value.trim();}catch(error){$('siteMessage').textContent=error.message;return;}
    const url=endpoint()+'/'+(section()==='games'?encodeURIComponent(game):section());busy=true;selector.disabled=true;$('siteReload').disabled=true;for(const control of form.querySelectorAll('input,select,textarea,button'))control.disabled=true;$('siteMessage').textContent='正在保存…';
    try{const result=await request(url,'PATCH',body);cache=result.data;revision=result.revision;render();form.elements.namedItem('reason').value='';$('siteMessage').textContent='当前分区已保存，前台刷新后读取最新配置。';}
    catch(error){$('siteMessage').textContent=error.message;}
    finally{busy=false;selector.disabled=false;$('siteReload').disabled=false;for(const control of form.querySelectorAll('input,select,textarea,button'))control.disabled=!canWrite();$('siteSave').disabled=!canWrite()||!revision;}
  });
  let selected=new URLSearchParams(location.search).get('section')||'basic';if(!schemas[selected])selected='basic';selector.value=selected;
  selector.addEventListener('change',()=>{if(dirty&&!confirm('切换分区会放弃未保存的输入，是否继续？')){selector.value=selected;return;}selected=selector.value;history.replaceState(null,'',location.pathname+'?section='+encodeURIComponent(selected));load();});
  $('siteReload').addEventListener('click',()=>{if(!dirty||confirm('重新读取会放弃当前输入，是否继续？'))load();});
  window.addEventListener('beforeunload',event=>{if(dirty){event.preventDefault();event.returnValue='';}});load();
})();
