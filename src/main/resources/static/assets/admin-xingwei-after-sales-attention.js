(()=>{
  'use strict';
  const $=id=>document.getElementById(id);
  const labels={OPEN:'待处理',IN_PROGRESS:'处理中',RESOLVED:'已解决'};
  const allowed={
    age:['ATTENTION','WITHIN_24H','FROM_24_TO_72H','OVER_72H'],
    status:['','OPEN','IN_PROGRESS','RESOLVED'],
    category:['','DELIVERY','CANCELLATION','REFUND_REVIEW','OTHER'],
    followup:['','PENDING']
  };
  const state={page:0,hasNext:false,total:0,access:null,request:0};

  function node(tag,text,klass){const element=document.createElement(tag);if(klass)element.className=klass;element.textContent=String(text??'');return element;}
  function message(text){$('attentionMessage').textContent=text;}
  function can(permission){const access=state.access;return Boolean(access&&(access.allowAll||access.permissionCodes?.includes(permission)||(permission.endsWith(':view')&&access.permissionCodes?.includes(permission.replace(':view',':manage')))));}
  function date(value){if(value==null)return '—';const parsed=new Date(typeof value==='number'?value:value);return Number.isNaN(parsed.getTime())?'—':parsed.toLocaleString('zh-CN');}

  async function api(path){
    const response=await fetch(path,{credentials:'same-origin',headers:{Accept:'application/json'}});
    const body=await response.json().catch(()=>({}));
    if(!response.ok)throw new Error(body.error||body.message||`请求失败 (${response.status})`);
    return body;
  }

  function restore(){
    const query=new URLSearchParams(location.search),form=$('attentionFilters');
    const age=query.get('age')||'ATTENTION';
    form.elements.age.value=allowed.age.includes(age)?age:'ATTENTION';
    for(const key of ['status','category','followup']){
      const value=query.get(key)||'';
      form.elements[key].value=allowed[key].includes(value)?value:'';
    }
    form.elements.q.value=(query.get('q')||'').slice(0,80);
    const rawPage=query.get('page')||'0';
    state.page=/^(0|[1-9]\d{0,5})$/.test(rawPage)?Number(rawPage):0;
  }

  function save(){
    const url=new URL(location.href),form=$('attentionFilters');
    for(const key of ['age','status','category','followup','q','page'])url.searchParams.delete(key);
    const age=form.elements.age.value||'ATTENTION';
    url.searchParams.set('age',age);
    for(const key of ['status','category','followup','q']){
      const value=String(form.elements[key].value||'').trim();
      if(value)url.searchParams.set(key,value);
    }
    if(state.page)url.searchParams.set('page',String(state.page));
    history.pushState(null,'',url);
  }

  function render(snapshot){
    state.total=Number(snapshot.totalCount)||0;
    state.hasNext=Boolean(snapshot.hasNext);
    const rows=$('attentionRows');rows.replaceChildren();
    for(const item of snapshot.items||[]){
      const record=item.record,tr=document.createElement('tr');
      const activity=item.latestCustomerFollowupAtMs
        ?`${item.pendingCustomerFollowup?'待跟进 · ':''}客户补充 · ${date(item.latestCustomerFollowupAtMs)}`
        :`售后提交 · ${date(record.submittedAtMs)}`;
      for(const value of [item.businessNo,record.categoryLabel,record.content,labels[record.status]||record.status,activity])tr.append(node('td',value));
      const cell=document.createElement('td');
      const link=node('a','回到售后处理','xw-button xw-button-ghost');
      link.href=`/admin/xingwei/after-sales/cases?q=${encodeURIComponent(item.businessNo)}`;
      cell.append(link);tr.append(cell);rows.append(tr);
    }
    if(!rows.children.length){const tr=document.createElement('tr'),td=node('td','当前筛选没有需要跟进的售后记录');td.colSpan=6;tr.append(td);rows.append(tr);}
    $('attentionPrev').disabled=state.page===0;
    $('attentionNext').disabled=!state.hasNext;
    $('attentionPage').textContent=`第 ${state.page+1} 页 · 共 ${state.total} 条`;
    message(`已读取 ${snapshot.items?.length||0} 条待跟进记录。`);
  }

  async function load(){
    if(!can('order:view')){message('当前账号没有订单查看权限。');return;}
    const request=++state.request,form=new FormData($('attentionFilters'));
    const query=new URLSearchParams({age:String(form.get('age')||'ATTENTION'),page:String(state.page)});
    for(const key of ['status','category','followup','q']){
      const value=String(form.get(key)||'').trim();
      if(value)query.set(key,value);
    }
    message('正在读取待跟进队列……');
    try{
      const snapshot=await api(`/api/admin/xingwei/business/after-sales/cases/attention-queue?${query}`);
      if(request===state.request)render(snapshot);
    }catch(error){if(request===state.request)message(error.message);}
  }

  $('attentionFilters').addEventListener('submit',event=>{event.preventDefault();state.page=0;save();load();});
  $('attentionPrev').addEventListener('click',()=>{if(state.page>0){state.page--;save();load();}});
  $('attentionNext').addEventListener('click',()=>{if(state.hasNext){state.page++;save();load();}});
  window.addEventListener('popstate',()=>{restore();load();});

  restore();
  api('/api/admin/xingwei/access').then(access=>{state.access=access;load();}).catch(error=>message(error.message));
})();
