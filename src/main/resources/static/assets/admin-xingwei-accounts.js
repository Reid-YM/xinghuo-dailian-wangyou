(() => {
  'use strict';
  const $=s=>document.querySelector(s),base='/api/admin/xingwei/admin-accounts';
  let snapshot={},busy=false,sequence=0,save=null;
  const node=(tag,text,cls)=>{const n=document.createElement(tag);if(text!=null)n.textContent=String(text);if(cls)n.className=cls;return n;};
  const message=text=>{$('#adminAccountMessage').textContent=text||'';};
  async function api(path='',options={}){
    const headers=new Headers(options.headers||{});headers.set('Accept','application/json');
    if(options.body!=null){headers.set('Content-Type','application/json');headers.set(document.querySelector('meta[name=_csrf_header]').content,document.querySelector('meta[name=_csrf]').content);options={...options,body:JSON.stringify(options.body)};}
    const response=await fetch(base+path,{...options,headers,cache:'no-store',credentials:'same-origin'});let data={};try{data=await response.json();}catch(_){}
    if(!response.ok)throw new Error(data.message||(response.status===401?'登录已失效，请重新登录后台。':'请求失败（'+response.status+'）'));return data;
  }
  function field(name,label,value='',type='text',options){
    const wrap=node('label',label),input=node(options?'select':'input');input.name=name;input.id='adminAccount-'+name;
    if(options)options.forEach(option=>input.append(new Option(option.name||option.code,option.code)));else input.type=type;
    if(type==='checkbox')input.checked=!!value;else input.value=String(value??'');
    if(type==='password'){input.autocomplete='new-password';input.maxLength=72;input.minLength=8;input.required=true;}
    if(name==='username'){input.maxLength=64;input.pattern='[A-Za-z0-9_.-]{3,64}';input.autocomplete='off';input.required=true;}
    if(name==='displayName'){input.maxLength=80;input.required=true;}
    wrap.append(input);$('#adminAccountFields').append(wrap);return input;
  }
  function open(title,build,action){if(busy)return;$('#adminAccountFields').replaceChildren();$('#adminAccountFormMessage').textContent='';$('#adminAccountDialogTitle').textContent=title;save=action;build();$('#adminAccountDialog').showModal();}
  function values(){const result=Object.fromEntries(new FormData($('#adminAccountForm')));$('#adminAccountFields').querySelectorAll('input[type=checkbox]').forEach(input=>result[input.name]=input.checked);return result;}
  function create(){open('创建后台登录账号',()=>{
    field('username','登录名');field('displayName','员工姓名');field('departmentCode','所属中心','','text',[{code:'',name:'暂不分配中心'},...(snapshot.departments||[])]);
    field('password','初始密码','','password');field('legacyRole','登录角色','OPERATOR','text',snapshot.restoreAdmin?[{code:'OPERATOR',name:'员工'},{code:'ADMIN',name:'系统管理员'}]:[{code:'OPERATOR',name:'员工'}]);
    const roles=field('roleCodes','星维角色（员工至少选择一个）','','text',(snapshot.roles||[]).filter(role=>role.enabled!==false));roles.multiple=true;roles.size=Math.min(8,Math.max(2,roles.options.length));
    $('#adminAccountFields').append(node('p','使用至少8位密码，含字母、数字或符号中的至少两类；请通过安全渠道交付初始密码。'));
  },()=>{const data=values();data.roleCodes=[...$('#adminAccount-roleCodes').selectedOptions].map(option=>option.value);return api('',{method:'POST',body:data});});}
  function edit(item){open('维护 '+item.username,()=>{
    const role=field('legacyRole','登录角色',item.legacyRole,'text',snapshot.restoreAdmin?[{code:'OPERATOR',name:'员工'},{code:'ADMIN',name:'系统管理员'}]:[{code:'OPERATOR',name:'员工'}]);
    const enabled=field('enabled','允许后台登录',item.loginEnabled,'checkbox');
    if(item.username===document.body.dataset.adminUsername){role.disabled=true;enabled.disabled=true;$('#adminAccountFields').append(node('p','当前账号的登录角色和启用状态保持不变，防止锁住自己。'));}
    $('#adminAccountFields').append(node('p','保存后撤销该账号的现有登录会话。星维角色及档案请在组织工作台维护。'));
  },()=>{const data=values();return api('/'+item.id,{method:'PATCH',body:{legacyRole:data.legacyRole||item.legacyRole,enabled:item.username===document.body.dataset.adminUsername?item.loginEnabled:data.enabled,authVersion:item.authVersion}});});}
  function password(item){open('重置 '+item.username+' 的密码',()=>{field('newPassword','新密码','','password');$('#adminAccountFields').append(node('p',item.username===document.body.dataset.adminUsername?'保存后当前会话也将失效，请使用新密码重新登录。':'保存后该账号所有旧会话失效，需使用新密码重新登录。'));},()=>api('/'+item.id+'/password',{method:'POST',body:{...values(),authVersion:item.authVersion}}));}
  function button(label,action){const b=node('button',label,'xw-button xw-button-ghost');b.type='button';b.onclick=()=>{if(!busy)action();};return b;}
  async function load(){const id=++sequence;$('#adminAccountRows').setAttribute('aria-busy','true');try{
    const result=await api('?q='+encodeURIComponent(new FormData($('#adminAccountSearch')).get('q')||''));if(id!==sequence)return;snapshot=result;
    $('#adminAccountCreate').hidden=!snapshot.canCreate;
    $('#adminAccountRows').replaceChildren(...(snapshot.items||[]).map(item=>{
      const row=node('article',null,'xaa-record');row.append(node('h3',(item.displayName||item.username)+' · '+item.username),node('p',[item.loginEnabled?'允许登录':'已停用',item.legacyRole==='ADMIN'?'系统管理员':'员工',item.departmentName,...(item.roleCodes||[])].filter(Boolean).join(' · ')));
      if(snapshot.canManage&&(item.legacyRole!=='ADMIN'||snapshot.restoreAdmin)){const actions=node('div',null,'xwi-actions');actions.append(button('登录状态',()=>edit(item)),button('重置密码',()=>password(item)));row.append(actions);}return row;
    }));if(!snapshot.items?.length)$('#adminAccountRows').append(node('p','暂无符合条件的账号。'));
    message(snapshot.canManage?'可维护全站后台登录账号；星维业务权限仍按角色与数据范围执行。':'当前仅可查看后台登录账号。');
  }finally{if(id===sequence)$('#adminAccountRows').setAttribute('aria-busy','false');}}
  $('#adminAccountForm').onsubmit=async event=>{event.preventDefault();if(busy||!save)return;busy=true;
    try{const pending=save();$('#adminAccountFields').disabled=true;$('#adminAccountSave').disabled=true;$('#adminAccountClose').disabled=true;const result=await pending;$('#adminAccountDialog').close();message(result.message||'保存成功。');try{await load();}catch(error){message('已保存。'+error.message);}}
    catch(error){$('#adminAccountFormMessage').textContent=error.message;}
    finally{busy=false;$('#adminAccountFields').disabled=false;$('#adminAccountSave').disabled=false;$('#adminAccountClose').disabled=false;}};
  $('#adminAccountClose').onclick=()=>{if(!busy)$('#adminAccountDialog').close();};$('#adminAccountDialog').addEventListener('cancel',event=>{if(busy)event.preventDefault();});
  $('#adminAccountCreate').onclick=create;$('#adminAccountRefresh').onclick=()=>{if(!busy)load().catch(error=>message(error.message));};$('#adminAccountSearch').onsubmit=event=>{event.preventDefault();if(!busy)load().catch(error=>message(error.message));};
  load().catch(error=>message(error.message));
})();
