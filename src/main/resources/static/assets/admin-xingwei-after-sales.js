(()=>{
  'use strict';
  const $=id=>document.getElementById(id);
  const labels={OPEN:'待处理',IN_PROGRESS:'处理中',RESOLVED:'已解决'};
  const state={page:0,hasNext:false,total:0,access:null,selected:null,businessNo:null,acknowledgedFollowupId:0,queueRequest:0};
  const filterKeys=['status','category','followup','q'];
  const allowed={status:['','OPEN','IN_PROGRESS','RESOLVED'],category:['','DELIVERY','CANCELLATION','REFUND_REVIEW','OTHER'],followup:['','PENDING']};
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
  function syncStatusActions(){
    const selected=$('caseFilters').elements.status.value;
    document.querySelectorAll('[data-case-status]').forEach(button=>button.setAttribute('aria-pressed',String(button.dataset.caseStatus===selected)));
  }
  function syncCategoryActions(){
    const selected=$('caseFilters').elements.category.value;
    document.querySelectorAll('[data-case-category]').forEach(button=>button.setAttribute('aria-pressed',String(button.dataset.caseCategory===selected)));
  }
  function restoreFilters(){
    const query=new URLSearchParams(location.search),form=$('caseFilters');
    for(const key of filterKeys){
      const raw=query.get(key)||'';
      form.elements[key].value=key==='q'?raw.slice(0,80):(allowed[key].includes(raw)?raw:'');
    }
    const rawPage=query.get('page')||'0';
    state.page=/^(0|[1-9]\d{0,5})$/.test(rawPage)?Number(rawPage):0;
    syncStatusActions();
    syncCategoryActions();
  }
  function saveFiltersToUrl(){
    const url=new URL(location.href),form=$('caseFilters');
    for(const key of [...filterKeys,'page'])url.searchParams.delete(key);
    for(const key of filterKeys){const value=String(form.elements[key].value||'').trim();if(value)url.searchParams.set(key,value);}
    if(state.page)url.searchParams.set('page',String(state.page));
    history.pushState(null,'',url);
  }
  async function loadSummary(){
    try{
      const summary=await api('/api/admin/xingwei/business/after-sales/cases/summary');
      $('caseTotalCount').textContent=String(summary.totalCount??0);
      $('casePendingCount').textContent=String(summary.pendingFollowupCount??0);
      for(const [status,key,id] of [['OPEN','openCount','caseOpenCount'],['IN_PROGRESS','inProgressCount','caseInProgressCount'],['RESOLVED','resolvedCount','caseResolvedCount']]){
        const button=document.querySelector(`[data-case-status="${status}"]`);
        if(button){$(id).textContent=String(summary[key]??0);button.disabled=false;}
      }
      $('caseSummaryMessage').textContent='数字统计当前账号可查看的全部售后记录；点击状态会保留下面的问题类型、客户补充和搜索条件。';
    }catch(error){
      $('caseTotalCount').textContent='—';$('casePendingCount').textContent='—';
      document.querySelectorAll('[data-case-status]').forEach(button=>{button.disabled=true;button.querySelector('strong').textContent='—';});
      $('caseSummaryMessage').textContent=`概览暂不可用：${error.message}`;
    }
  }
  async function loadCategorySummary(){
    const actions=[...document.querySelectorAll('[data-case-category]')],host=$('caseCategorySummaryMessage');
    if(!actions.length||!host)return;
    try{
      const summary=await api('/api/admin/xingwei/business/after-sales/cases/category-summary');
      for(const [category,key,id] of [['DELIVERY','deliveryCount','caseDeliveryCount'],['CANCELLATION','cancellationCount','caseCancellationCount'],['REFUND_REVIEW','refundReviewCount','caseRefundReviewCount'],['OTHER','otherCount','caseOtherCount']]){
        const button=document.querySelector(`[data-case-category="${category}"]`);
        if(button&&$(id)){$(id).textContent=String(summary[key]??0);button.disabled=false;}
      }
      host.textContent='问题类型数字统计当前账号可查看的全部售后记录；点击问题类型会保留处理状态、客户补充和搜索条件。';
    }catch(error){
      actions.forEach(button=>{button.disabled=true;const strong=button.querySelector('strong');if(strong)strong.textContent='—';});
      host.textContent=`问题类型分布暂不可用：${error.message}`;
    }
  }
  async function loadAttentionAgeSummary(){
    const host=$('caseAttentionAgeMessage');
    if(!host)return;
    const fields=[['attentionCount','caseAttentionCount'],['within24hCount','caseAttentionWithin24hCount'],['from24To72hCount','caseAttention24To72hCount'],['over72hCount','caseAttentionOver72hCount']];
    try{
      const summary=await api('/api/admin/xingwei/business/after-sales/cases/attention-age-summary');
      for(const [key,id] of fields){if($(id))$(id).textContent=String(summary[key]??0);}
      host.textContent='需跟进 = 尚未开始处理，或存在尚未确认的客户补充；时长从最近一次需要客服响应的客户动作开始计算。';
    }catch(error){
      for(const [,id] of fields){if($(id))$(id).textContent='—';}
      host.textContent=`待跟进时效暂不可用：${error.message}`;
    }
  }
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
    const request=++state.queueRequest;
    const form=new FormData($('caseFilters')),query=new URLSearchParams({page:String(state.page)});
    for(const key of ['status','category','followup','q']){const value=String(form.get(key)||'').trim();if(value)query.set(key,value);}
    message('正在读取售后记录……');
    try{const snapshot=await api(`/api/admin/xingwei/business/after-sales/cases?${query}`);if(request===state.queueRequest)renderQueue(snapshot);}
    catch(error){if(request===state.queueRequest)message(error.message);}
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
  $('caseFilters').addEventListener('submit',event=>{event.preventDefault();state.page=0;syncStatusActions();syncCategoryActions();saveFiltersToUrl();load();});
  document.querySelectorAll('[data-case-status]').forEach(button=>button.addEventListener('click',()=>{
    if(button.disabled)return;
    $('caseFilters').elements.status.value=button.dataset.caseStatus;
    state.page=0;syncStatusActions();saveFiltersToUrl();load();
  }));
  document.querySelectorAll('[data-case-category]').forEach(button=>button.addEventListener('click',()=>{
    if(button.disabled)return;
    $('caseFilters').elements.category.value=button.dataset.caseCategory;
    state.page=0;syncCategoryActions();saveFiltersToUrl();load();
  }));
  $('casePrev').addEventListener('click',()=>{if(state.page>0){state.page--;saveFiltersToUrl();load();}});
  $('caseNext').addEventListener('click',()=>{if(state.hasNext){state.page++;saveFiltersToUrl();load();}});
  window.addEventListener('popstate',()=>{restoreFilters();load();});
  $('caseUpdate').addEventListener('submit',async event=>{
    event.preventDefault();if(!state.selected||!can('order:manage'))return;
    const form=event.currentTarget,record=state.selected;
    const payload={status:form.elements.status.value,handlingNote:form.elements.handlingNote.value.trim(),customerReply:form.elements.customerReply.value.trim()||null,acknowledgedFollowupId:state.acknowledgedFollowupId,expectedVersion:record.version};
    if(payload.status===record.status&&!payload.customerReply){message('保持当前状态时须填写客户回复。');return;}
    message('正在保存处理结果……');
    try{await api(`/api/admin/xingwei/business/orders/${encodeURIComponent(state.businessNo)}/after-sales/records/${record.recordId}`,{method:'PUT',body:JSON.stringify(payload)});await openCase(state.businessNo,record.recordId);await load();await loadSummary();await loadAttentionAgeSummary();message('处理结果已保存。');}
    catch(error){if(error.status===409)await openCase(state.businessNo,record.recordId);message(error.message);}
  });
  restoreFilters();
  api('/api/admin/xingwei/access').then(access=>{
    state.access=access;load();
    if(can('order:view')){loadSummary();loadCategorySummary();loadAttentionAgeSummary();}
    else{
      $('caseSummaryMessage').textContent='当前账号没有订单查看权限。';
      if($('caseCategorySummaryMessage'))$('caseCategorySummaryMessage').textContent='当前账号没有订单查看权限。';
      if($('caseAttentionAgeMessage'))$('caseAttentionAgeMessage').textContent='当前账号没有订单查看权限。';
    }
  }).catch(error=>{
    message(error.message);$('caseSummaryMessage').textContent='售后概览暂不可用。';
    if($('caseCategorySummaryMessage'))$('caseCategorySummaryMessage').textContent='问题类型分布暂不可用。';
    if($('caseAttentionAgeMessage'))$('caseAttentionAgeMessage').textContent='待跟进时效暂不可用。';
  });
})();