(()=>{
  'use strict';
  const $=id=>document.getElementById(id);
  const labels={OPEN:'待处理',IN_PROGRESS:'处理中',RESOLVED:'已解决'};
  const state={page:0,hasNext:false,total:0,access:null,selected:null,businessNo:null,acknowledgedFollowupId:0};
  const csrf=document.querySelector('meta[name="_csrf"]')?.content;
  const csrfHeader=document.querySelector('meta[name="_csrf_header"]')?.content||'X-CSRF-TOKEN';
  function node(tag,text,klass){const e=document.createElement(tag);if(klass)e.className=klass;e.textContent=String(text??'');return e;}
  function message(text){$('caseMessage').textContent=text;}
  function can(permission){const a=state.access;return Boolean(a&&(a.allowAll||a.permissionCodes?.includes(permission)||(permission.endsWith(':view')&&a.permissionCodes?.includes(permission.replace(':view',':manage')))));}
  function date(value){if(value==null)return '—';const d=new Date(typeof value==='number'?value:value);return Number.isNaN(d.getTime())?'—':d.toLocaleString('zh-CN');}
  async function api(path,options={}){
    const headers={'Accept':'application/json'};
    if(options.body){headers['Content-Type']='application/json';if(csrf)headers[csrfHeader]=csrf;}
    const response=await fetch(path,{credentials:'same-origin',...options,headers:{...headers,...options.headers}});
    const body=await response.json().catch(()=>({}));
    if(!response.ok){const error=new Error(response.status===409?'记录已变化，请刷新后重试':(body.error||body.message||`请求失败 (${response.status})`));error.status=response.status;throw error;}
    return body;
  }
  function field(host,name,value){const p=node('p',`${name}：${value??'—'}`);p.className='xw-case-field';host.append(p);}
  function renderQueue(snapshot){
    state.total=snapshot.totalCount||0;state.hasNext=Boolean(snapshot.hasNext);
    const rows=$('caseRows');rows.replaceChildren();
    for(const item of snapshot.items||[]){
      const r=item.record,tr=document.createElement('tr');
      const activity=item.latestCustomerFollowupAtMs?`${item.pendingCustomerFollowup?'待跟进 · ':''}客户补充 · ${date(item.latestCustomerFollowupAtMs)}`:date(r.submittedAtMs);
      for(const value of [item.businessNo,r.categoryLabel,r.content,labels[r.status]||r.status,activity])tr.append(node('td',value));
      const cell=document.createElement('td'),button=node('button','查看处理','xw-button xw-button-ghost');button.type='button';button.addEventListener('click',()=>openCase(item.businessNo,r.recordId));cell.append(button);tr.append(cell);rows.append(tr);
    }
    if(!rows.children.length){const tr=document.createElement('tr'),td=node('td','当前筛选没有售后记录');td.colSpan=6;tr.append(td);rows.append(tr);}
    $('casePrev').disabled=state.page===0;$('caseNext').disabled=!state.hasNext;
    $('casePage').textContent=`第 ${state.page+1} 页 · 共 ${state.total} 条`;
    message(`已读取 ${snapshot.items?.length||0} 条售后记录。`);
  }
  async function load(){
    if(!can('order:view')){message('当前账号没有订单查看权限。');return;}
    const form=new FormData($('caseFilters')),query=new URLSearchParams({page:String(state.page)});
    for(const key of ['status','category','followup','q']){const value=String(form.get(key)||'').trim();if(value)query.set(key,value);}
    message('正在读取售后记录……');
    try{renderQueue(await api(`/api/admin/xingwei/business/after-sales/cases?${query}`));}
    catch(error){message(error.message);}
  }
  async function openCase(businessNo,recordId){
    state.businessNo=businessNo;state.selected=null;state.acknowledgedFollowupId=0;$('caseDetail').hidden=false;$('caseCurrent').replaceChildren();$('caseFollowups').replaceChildren();$('caseEvents').replaceChildren();$('caseUpdate').hidden=true;
    message('正在读取处理历史……');
    try{
      const history=await api(`/api/admin/xingwei/business/orders/${encodeURIComponent(businessNo)}/after-sales/records/${encodeURIComponent(recordId)}/history`);
      const current=history.current;state.selected=current;
      state.acknowledgedFollowupId=(history.customerFollowups||[]).reduce((latest,item)=>Math.max(latest,Number(item.followupId)||0),0);
      field($('caseCurrent'),'业务编号',businessNo);field($('caseCurrent'),'问题类型',current.categoryLabel);field($('caseCurrent'),'客户说明',current.content);field($('caseCurrent'),'当前状态',labels[current.status]||current.status);field($('caseCurrent'),'内部处理说明',current.handlingNote);
      for(const followup of history.customerFollowups||[]){const article=node('article','', 'xw-case-event');article.append(node('strong',`${Number(followup.followupId)>Number(history.handledThroughFollowupId||0)?'待跟进 · ':''}客户补充 · ${date(followup.createdAt)}`));field(article,'补充内容',followup.content);$('caseFollowups').append(article);}
      if(!(history.customerFollowups||[]).length)$('caseFollowups').append(node('p','暂无客户补充。'));
      for(const event of history.events||[]){const article=node('article','', 'xw-case-event');const change=event.fromStatus===event.toStatus?'追加客户回复':`${labels[event.fromStatus]||event.fromStatus} → ${labels[event.toStatus]||event.toStatus}`;article.append(node('strong',`${change} · ${date(event.createdAt)}`));field(article,'内部处理说明',event.handlingNote);if(event.customerReply)field(article,'客户回复',event.customerReply);$('caseEvents').append(article);}
      if(!(history.events||[]).length)$('caseEvents').append(node('p','暂无 V4C 处理事件；此前处理动作可在审计记录查看。'));
      if(can('order:manage')){
        const form=$('caseUpdate'),status=form.elements.status;
        status.querySelector('[data-same-status]')?.remove();
        if(current.status==='IN_PROGRESS'||current.status==='RESOLVED'){
          const same=node('option','保持当前状态（追加客户回复）');same.value=current.status;same.dataset.sameStatus='1';status.append(same);
        }
        form.hidden=false;status.value=current.status==='IN_PROGRESS'?'RESOLVED':'IN_PROGRESS';
        form.elements.handlingNote.value='';form.elements.customerReply.value='';
      }
      message('处理历史已加载。');
    }catch(error){message(error.message);}
  }
  $('caseFilters').addEventListener('submit',event=>{event.preventDefault();state.page=0;load();});
  $('casePrev').addEventListener('click',()=>{if(state.page>0){state.page--;load();}});
  $('caseNext').addEventListener('click',()=>{if(state.hasNext){state.page++;load();}});
  $('caseUpdate').addEventListener('submit',async event=>{
    event.preventDefault();if(!state.selected||!can('order:manage'))return;
    const form=event.currentTarget,record=state.selected;
    const payload={status:form.elements.status.value,handlingNote:form.elements.handlingNote.value.trim(),customerReply:form.elements.customerReply.value.trim()||null,acknowledgedFollowupId:state.acknowledgedFollowupId,expectedVersion:record.version};
    if(payload.status===record.status&&!payload.customerReply){message('保持当前状态时须填写客户回复。');return;}
    message('正在保存处理结果……');
    try{await api(`/api/admin/xingwei/business/orders/${encodeURIComponent(state.businessNo)}/after-sales/records/${record.recordId}`,{method:'PUT',body:JSON.stringify(payload)});await openCase(state.businessNo,record.recordId);await load();message('处理结果已保存。');}
    catch(error){if(error.status===409)await openCase(state.businessNo,record.recordId);message(error.message);}
  });
  api('/api/admin/xingwei/access').then(access=>{state.access=access;load();}).catch(error=>message(error.message));
})();
