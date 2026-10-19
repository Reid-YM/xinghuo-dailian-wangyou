(() => {
  'use strict';
  const $=id=>document.getElementById(id);
  const filters=$('auditFilters');
  const labels={password:'密码',email:'邮箱',phone:'联系电话',role:'登录角色',roleCodes:'员工角色',active:'启用状态',businessNo:'业务编号',status:'处理状态',serviceState:'服务状态',dataScope:'数据范围',version:'版本号',progressPercent:'进度百分比',ownerAdminUserId:'负责人',handledByAdminUserId:'处理人',title:'标题',details:'说明',content:'内容',note:'备注',reason:'操作原因',enabled:'启用状态',visible:'可见状态',published:'发布状态',isPrimary:'主角色',departmentCode:'所属中心'};
  const actionLabel=code=>window.XWLabels?.auditAction(code)||'其他操作';
  const targetLabel=code=>window.XWLabels?.auditTarget(code)||'其他业务对象';
  function changeValue(field,value){
    if(['status','serviceState','dataScope'].includes(field))return window.XWLabels?.value(value)??value;
    if(['enabled','visible','published','isPrimary'].includes(field))return value==='true'?'是':value==='false'?'否':value;
    return value;
  }
  let page=0,sequence=0,detailSequence=0,selectedId=null;
  function el(tag,text,className){const n=document.createElement(tag);if(text!==undefined)n.textContent=text;if(className)n.className=className;return n;}
  async function get(url){
    const controller=new AbortController(),timer=setTimeout(()=>controller.abort(),10000);
    try{
      const response=await fetch(url,{headers:{Accept:'application/json'},signal:controller.signal,credentials:'same-origin'});
      const data=await response.json().catch(()=>({}));
      if(!response.ok||response.redirected)throw new Error(data.error||(response.status===403?'没有查看权限或登录已失效。':'读取失败，请稍后重试。'));
      return data;
    }catch(error){if(error.name==='AbortError')throw new Error('读取超时，请重试。');throw error;}finally{clearTimeout(timer);}
  }
  function restore(){
    const p=new URLSearchParams(location.search);
    if(filters.elements.namedItem('source')) filters.elements.namedItem('source').value=p.get('source')==='members'?'members':'staff';
    for(const name of ['q','from','to'])filters.elements.namedItem(name).value=p.get(name)||'';
    const action=p.get('action')||'';const select=filters.elements.namedItem('action');
    if(action&&![...select.options].some(o=>o.value===action)){const o=el('option',actionLabel(action));o.value=action;select.append(o);}
    select.value=action;
    const n=Number(p.get('page')||0);page=Number.isInteger(n)&&n>=0&&n<=10000?n:0;
  }
  function query(){const p=new URLSearchParams(new FormData(filters));for(const [k,v]of[...p])if(!v)p.delete(k);if(page)p.set('page',String(page));return p;}
  function navigate(){const p=query().toString();history.pushState(null,'',location.pathname+(p?'?'+p:''));load();}
  async function load(){
    const current=++sequence;$('auditRows').replaceChildren();$('auditMessage').textContent='正在读取审计记录…';$('auditRetry').hidden=true;$('auditRows').setAttribute('aria-busy','true');$('auditPrev').disabled=true;$('auditNext').disabled=true;
    try{
      const memberSource=filters.elements.namedItem('source')?.value==='members';
      const data=await get('/api/admin/xingwei/audits'+(memberSource?'/member-activity':'')+'?'+query());if(current!==sequence)return;
      const select=filters.elements.namedItem('action'),selected=select.value;
      select.replaceChildren(el('option','全部类型'));select.firstChild.value='';
      for(const action of [...new Set([...data.actions,...(selected?[selected]:[])])]){const option=el('option',actionLabel(action));option.value=action;select.append(option);}select.value=selected;
      $('auditRows').replaceChildren();
      for(const entry of data.items){
        const row=el('article',undefined,'xwc-task');const title=el('h3',actionLabel(entry.action));row.append(title);
        if(memberSource){row.append(el('p','会员：'+entry.member),el('p','网络：'+entry.ipMasked),el('p',new Date(entry.createdAt).toLocaleString('zh-CN',{timeZone:'Asia/Shanghai'})));$('auditRows').append(row);continue;}
        const meta=el('div',undefined,'xwc-task-meta');meta.append(el('span','操作人：'+entry.operatorName),el('span',new Date(entry.createdAt).toLocaleString('zh-CN',{timeZone:'Asia/Shanghai'})));
        row.append(meta,el('p','对象：'+targetLabel(entry.targetType)));
        const button=el('button','查看变更摘要','xw-button xw-button-ghost');button.type='button';button.addEventListener('click',()=>openDetail(entry.id));row.append(button);$('auditRows').append(row);
      }
      if(!data.items.length)$('auditRows').append(el('p','当前权限范围与筛选条件下暂无审计记录。'));
      $('auditCount').textContent='共 '+data.totalCount+' 条记录';$('auditPage').textContent='第 '+(page+1)+' 页';$('auditPrev').disabled=page===0;$('auditNext').disabled=(page+1)*data.pageSize>=data.totalCount;
      $('auditMessage').textContent='审计记录已更新。';
    }catch(error){if(current===sequence){$('auditMessage').textContent=error.message;$('auditRetry').hidden=false;}}
    finally{if(current===sequence)$('auditRows').setAttribute('aria-busy','false');}
  }
  async function openDetail(id){
    selectedId=id;const current=++detailSequence;
    $('auditDetail').replaceChildren();$('auditDetailMessage').textContent='正在读取变更摘要…';$('auditDetailRetry').hidden=true;
    if(!$('auditDialog').open)$('auditDialog').showModal();
    try{
      const data=await get('/api/admin/xingwei/audits/'+encodeURIComponent(id));if(current!==detailSequence)return;
      const content=$('auditDetail');content.append(el('p',actionLabel(data.entry.action)+' · '+data.entry.operatorName));
      for(const change of data.changes){const row=el('p');const fieldLabel=el('strong',labels[change.field]||'其他字段');row.append(fieldLabel,document.createTextNode('：'+changeValue(change.field,change.before)+' → '+changeValue(change.field,change.after)));content.append(row);}
      $('auditDetailMessage').textContent=data.changes.length?'敏感值与完整内容已隐藏。':'没有可展示的字段变化。';
    }catch(error){if(current===detailSequence){$('auditDetailMessage').textContent=error.message;$('auditDetailRetry').hidden=false;}}
  }
  function close(){detailSequence++;$('auditDialog').close();}
  $('auditClose').addEventListener('click',close);$('auditDialog').addEventListener('cancel',e=>{e.preventDefault();close();});
  $('auditDetailRetry').addEventListener('click',()=>openDetail(selectedId));
  filters.elements.namedItem('source')?.addEventListener('change',()=>{filters.elements.namedItem('action').value='';page=0;navigate();});
  filters.addEventListener('submit',e=>{e.preventDefault();page=0;navigate();});
  $('auditRefresh').addEventListener('click',load);$('auditRetry').addEventListener('click',load);
  $('auditPrev').addEventListener('click',()=>{if(page>0){page--;navigate();}});$('auditNext').addEventListener('click',()=>{page++;navigate();});
  window.addEventListener('popstate',()=>{restore();load();});restore();load();
})();
