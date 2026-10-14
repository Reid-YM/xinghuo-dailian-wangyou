(() => {
  'use strict';
  const PAGE = document.body.dataset.page || '';
  const $ = (s, r = document) => r.querySelector(s);
  const $$ = (s, r = document) => [...r.querySelectorAll(s)];

  function guardUserDestinations() {
    document.addEventListener('click', event => {
      const link = event.target instanceof Element ? event.target.closest('a[href]') : null;
      if (!link) return;
      const href = String(link.getAttribute('href') || '').trim();
      if (href === '/admin' || href.startsWith('/admin/')) {
        event.preventDefault();
        event.stopImmediatePropagation();
        location.assign('/account');
      }
    }, true);
  }

  function redirectLegacyAccountHash() {
    if (location.pathname !== '/account' || !location.hash) return false;
    const key = location.hash.replace(/^#/, '').toLowerCase();
    const targets = {
      overview: '/account',
      requests: '/account/requests',
      quotes: '/account/requests',
      orders: '/account/orders',
      progress: '/account/progress',
      tools: '/account/progress',
      profile: '/account/settings#profile',
      contact: '/account/settings#contact',
      quotecontact: '/account/settings#contact',
      security: '/account/settings#security',
      password: '/account/settings#password',
      email: '/account/settings#email',
      devices: '/account/settings#devices',
      activity: '/account/settings#activity'
    };
    const target = targets[key];
    if (!target || target === `${location.pathname}${location.hash}`) return false;
    location.replace(target);
    return true;
  }

  guardUserDestinations();
  if (redirectLegacyAccountHash()) return;

  function initSettingsViews() {
    const root = $('[data-settings-hub]'); if (!root) return;
    const map = { profile:'profile', contact:'contact', quotecontact:'contact', security:'security', password:'security', email:'security', devices:'security', activity:'security', session:'security' };
    const activate = raw => {
      const key = String(raw || 'profile').replace(/^#/,'').toLowerCase();
      const view = map[key] || 'profile';
      $$('[data-settings-view]', root).forEach(p => { p.hidden = p.dataset.settingsView !== view; });
      $$('[data-settings-target]', root).forEach(a => a.classList.toggle('is-active', a.dataset.settingsTarget === view));
      if (['password','email','devices','activity','session'].includes(key)) window.setTimeout(() => document.getElementById(key)?.scrollIntoView({ block:'start' }), 0);
    };
    root.addEventListener('click', e => { const a=e.target.closest('[data-settings-target]'); if(!a)return; e.preventDefault(); const target=a.dataset.settingsTarget; history.replaceState(null,'',`#${target}`); activate(target); });
    window.addEventListener('hashchange', () => activate(location.hash));
    activate(location.hash);
  }

  if (PAGE === 'account-settings') { initSettingsViews(); return; }
  if (!PAGE.startsWith('account-center-')) return;

  const el = (tag, cls, text) => { const n=document.createElement(tag); if(cls)n.className=cls; if(text!=null)n.textContent=String(text); return n; };
  const time = value => { if(!value)return '—'; const d=new Date(value); return Number.isNaN(d.getTime())?'—':d.toLocaleString('zh-CN',{hour12:false}); };

  async function api(url, options={}) {
    const method=(options.method||'GET').toUpperCase(); const headers=new Headers(options.headers||{}); headers.set('Accept','application/json');
    if(method!=='GET'&&method!=='HEAD')headers.set('X-Requested-With','fetch'); let body=options.body;
    if(body&&!(body instanceof FormData)){headers.set('Content-Type','application/json');body=JSON.stringify(body);}
    const response=await fetch(url,{...options,method,headers,body,credentials:'same-origin',cache:'no-store'});
    if(response.status===401){location.replace('/account/login?next='+encodeURIComponent(location.pathname+location.search+location.hash));return null;}
    let payload=null; try{payload=response.status===204?null:await response.json();}catch(_){try{payload=await response.text();}catch(__){}}
    if(!response.ok)throw new Error((payload&&payload.message)||(typeof payload==='string'&&payload)||`请求失败 ${response.status}`); return payload;
  }
  function paintAvatar(node,me){if(!node)return;node.textContent='';if(me?.avatarUrl){const img=el('img');img.src=me.avatarUrl;img.alt='';img.loading='lazy';node.appendChild(img);}else node.appendChild(el('span','account-avatar-fallback',String(me?.displayName||'星').slice(0,1)));}
  async function loadIdentity(){const me=await api('/api/account/me');if(!me)return null;$$('[data-account-center-name]').forEach(n=>n.textContent=me.displayName||'星火用户');$$('[data-account-center-meta]').forEach(n=>n.textContent=me.email||me.contactMasked||'账号已登录');$$('[data-account-center-avatar]').forEach(n=>paintAvatar(n,me));return me;}
  function itemShell(status,title,description,meta){const article=el('article','account-center-item');const main=el('div','account-center-item-main');main.append(el('span','',status||'处理中'),el('h3','',title||'业务记录'),el('p','',description||'暂无更新'),el('small','',meta||''));const actions=el('div','account-center-item-actions');article.append(main,actions);return{article,actions};}
  function empty(host,text){if(!host)return;host.textContent='';host.appendChild(el('div','account-center-empty',text));}
  function renderQuote(host,q,compact=false){const row=itemShell(q.statusText||'处理中',`${q.game||'游戏需求'} · ${q.demand||'需求'}`,q.customerSupplement||q.currentProgress||'等待客服处理',time(q.updatedAt||q.createdAt));if(!compact){const b=el('button','btn ghost small-btn','补充说明');b.type='button';b.dataset.supplementQuote=q.businessNo||'';row.actions.appendChild(b);}host.appendChild(row.article);}
  function renderOrder(host,o,compact=false){const row=itemShell(o.statusText||'处理中',`${o.gameName||'业务订单'} · ${o.serviceName||'订单'}`,o.progressText||'等待进度更新',time(o.updatedAt||o.createdAt));const pct=Math.max(0,Math.min(100,Number(o.progressPercent)||0));const progress=el('div','account-center-progress'),bar=el('i');bar.style.width=`${pct}%`;progress.appendChild(bar);row.article.querySelector('.account-center-item-main').appendChild(progress);if(!compact){const d=el('button','btn ghost small-btn','查看详情');d.type='button';d.dataset.orderDetail=o.businessNo||o.orderNo||'';const s=el('button','btn ghost small-btn','补充说明');s.type='button';s.dataset.supplementOrder=o.businessNo||o.orderNo||'';row.actions.append(d,s);}host.appendChild(row.article);}
  async function supplement(kind,no){if(!no)return;const content=window.prompt('填写要补充给客服的说明（不要填写游戏密码、验证码或令牌）');if(!content||!content.trim())return;await api(`/api/console/${kind}/${encodeURIComponent(no)}/supplement`,{method:'POST',body:{content:content.trim()}});window.alert('补充说明已提交');}
  async function initOverview(){const s=await api('/api/console/summary');if(!s)return;const v={requests:s.quoteTotal,orders:s.orderTotal,active:s.activeOrders,security:s.securityEvents};Object.entries(v).forEach(([k,x])=>{const n=$(`[data-hub-kpi="${k}"]`);if(n)n.textContent=String(x??0);});const q=$('[data-hub-recent-requests]');if(q){q.textContent='';(s.recentQuotes||[]).forEach(x=>renderQuote(q,x,true));if(!(s.recentQuotes||[]).length)empty(q,'暂无需求记录');}const o=$('[data-hub-recent-orders]');if(o){o.textContent='';(s.recentOrders||[]).forEach(x=>renderOrder(o,x,true));if(!(s.recentOrders||[]).length)empty(o,'暂无订单记录');}}
  async function initRequests(){const host=$('[data-hub-requests]');if(!host)return;const d=await api('/api/console/quotes');host.textContent='';(d?.items||[]).forEach(x=>renderQuote(host,x));if(!(d?.items||[]).length)empty(host,'暂无需求记录。提交新需求后会自动出现在这里。');}
  async function initOrders(){const host=$('[data-hub-orders]');if(!host)return;const d=await api('/api/console/orders');host.textContent='';(d?.items||[]).forEach(x=>renderOrder(host,x));if(!(d?.items||[]).length)empty(host,'暂无订单记录。询价确认成交后会出现在这里。');}
  let paymentPoll=null;
  function renderAfterSalesHistory(host,note){
    host.textContent='';
    const source=String(note||'');
    const entries=[...source.matchAll(/^\[售后问题 · (交付或进度|取消服务|退款核对|其他问题) · ([^\]\n]+)\]\n/gm)];
    host.hidden=entries.length===0;
    if(!entries.length)return;
    host.append(el('h3','','已提交的售后问题'),el('p','','客服将按业务编号核对，处理结果以客服沟通为准。'));
    entries.forEach((entry,index)=>{
      const untilNext=source.slice(entry.index+entry[0].length,entries[index+1]?.index??source.length);
      const content=untilNext.split(/\n\n\[用户补充 [^\]\n]+\]\n/,1)[0].trim();
      const item=el('article','account-after-sales-entry');
      item.append(el('strong','',entry[1]),el('small','',time(entry[2])),el('p','',content));
      host.appendChild(item);
    });
  }
  function renderPayment(host,p){
    host.textContent='';
    if(!p)return;
    host.appendChild(el('h3','','微信扫码支付'));
    host.appendChild(el('p','',`${p.subject} · ¥${p.amount}`));
    if(p.status==='CAPTURED'){
      host.appendChild(el('strong','','已支付成功'));
      return;
    }
    if(p.status==='CANCELLED'){
      host.appendChild(el('p','','支付单已关闭，请联系客服确认新的报价。'));
      return;
    }
    if(p.status==='APPROVED'&&p.expiresAt&&new Date(p.expiresAt)>new Date()){
      const img=el('img','account-payment-qr');
      img.src=`/api/account/payments/${encodeURIComponent(p.paymentNo)}/qr.png`;
      img.alt='微信支付二维码';
      img.width=240;img.height=240;
      host.append(img,el('p','','请用微信扫描二维码付款。手机访问时可在另一台设备上打开此页扫码。'));
      return;
    }
    if(p.status==='APPROVED'){
      host.appendChild(el('p','','二维码已过期，请联系客服重新确认支付单。'));
      return;
    }
    const button=el('button','btn primary small-btn','生成支付二维码');
    button.type='button';button.dataset.startWechatPayment=p.paymentNo;
    host.appendChild(button);
  }
  async function showOrderDetail(no){
    if(paymentPoll){clearInterval(paymentPoll);paymentPoll=null;}
    const d=await api('/api/console/orders/'+encodeURIComponent(no));if(!d)return;
    const host=$('[data-hub-order-detail]');if(!host)return;
    host.textContent='';host.hidden=false;
    const h=el('header');h.append(el('h3','','进度详情'));
    const close=el('button','btn ghost small-btn','关闭');close.type='button';close.dataset.closeOrderDetail='1';
    h.appendChild(close);host.appendChild(h);
    const t=el('div','account-center-timeline');
    (d.timeline||[]).forEach(x=>{const i=el('div');i.append(el('strong','',x.title||'进度'),el('p','',x.content||''),el('small','',time(x.createdAt)));t.appendChild(i);});
    host.appendChild(t);
    const afterSalesHistory=el('section','account-after-sales-history');afterSalesHistory.dataset.afterSalesHistory='1';
    renderAfterSalesHistory(afterSalesHistory,d.order?.customerNote);host.appendChild(afterSalesHistory);
    const afterSales=el('form','account-after-sales');
    afterSales.dataset.afterSalesForm=d.order?.businessNo||d.order?.orderNo||no;
    afterSales.append(el('h3','','售后问题说明'),el('p','','提交后由客服按业务编号核对。此处不会自动取消订单或发起退款；淘宝、闲鱼订单仍按对应平台规则处理。'));
    const categoryLabel=el('label','','问题类型');const category=el('select');category.name='category';category.required=true;
    [['','选择问题类型'],['DELIVERY','交付或进度'],['CANCELLATION','取消服务'],['REFUND_REVIEW','退款核对'],['OTHER','其他问题']].forEach(([value,label])=>{const option=el('option','',label);option.value=value;category.appendChild(option);});
    categoryLabel.appendChild(category);
    const descriptionLabel=el('label','','问题说明');const description=el('textarea');description.name='content';description.required=true;description.maxLength=240;description.rows=4;description.placeholder='说明问题、期望客服核对的内容；请勿填写游戏密码、验证码或令牌。';descriptionLabel.appendChild(description);
    const submit=el('button','btn primary small-btn','提交问题说明');submit.type='submit';
    const message=el('p','account-after-sales-message');message.dataset.afterSalesMessage='1';message.setAttribute('role','status');
    afterSales.append(categoryLabel,descriptionLabel,submit,message);
    host.appendChild(afterSales);
    const contact=el('a','btn ghost small-btn','联系客服');contact.href='/contact';host.appendChild(contact);
    const paymentHost=el('section','account-payment');host.appendChild(paymentHost);
    try{
      const p=await api(`/api/account/payments/order/${encodeURIComponent(no)}`);
      if(!p||host.hidden)return;
      renderPayment(paymentHost,p);
      if(p.status==='APPROVED'){
        paymentPoll=setInterval(async()=>{
          if(host.hidden){clearInterval(paymentPoll);paymentPoll=null;return;}
          try{const latest=await api(`/api/account/payments/${encodeURIComponent(p.paymentNo)}`);if(latest){renderPayment(paymentHost,latest);if(latest.status==='CAPTURED'||latest.status==='CANCELLED'){clearInterval(paymentPoll);paymentPoll=null;}}}catch(_){}
        },10000);
      }
    }catch(_){/* Payment is unavailable until the merchant feature switch is enabled. */}
  }
  function toolDisplayName(toolKey){const key=String(toolKey||'').toLowerCase();if(key.includes('mastery')||key.includes('mw4'))return 'MW4 工具存档';if(key.includes('quote'))return '询价工具存档';if(key.includes('price'))return '价格工具存档';return '工具存档';}
  async function initProgress(){const host=$('[data-hub-progress]');if(!host)return;const items=await api('/api/account/progress');host.textContent='';if(!(items||[]).length){empty(host,'还没有云端工具存档。进入 MW4 工具站后可按页面提示同步。');return;}(items||[]).forEach(x=>{const row=itemShell('云端存档',toolDisplayName(x.toolKey),'进度内容请回到对应工具继续编辑，个人中心只负责查看存档状态。',`最近同步：${time(x.updatedAt)}`);const a=el('a','btn primary small-btn','打开工具站');a.href='/mastery';row.actions.appendChild(a);host.appendChild(row.article);});}

  document.addEventListener('click',async e=>{const logout=e.target.closest('[data-account-logout]');if(logout){try{await api('/api/account/logout',{method:'POST',body:{}});}finally{location.href='/';}return;}const q=e.target.closest('[data-supplement-quote]');if(q){try{await supplement('quotes',q.dataset.supplementQuote);await initRequests();}catch(err){alert(err.message);}return;}const o=e.target.closest('[data-supplement-order]');if(o){try{await supplement('orders',o.dataset.supplementOrder);await initOrders();}catch(err){alert(err.message);}return;}const d=e.target.closest('[data-order-detail]');if(d){try{await showOrderDetail(d.dataset.orderDetail);}catch(err){alert(err.message);}return;}const pay=e.target.closest('[data-start-wechat-payment]');if(pay){pay.disabled=true;try{const p=await api(`/api/account/payments/${encodeURIComponent(pay.dataset.startWechatPayment)}/native`,{method:'POST',body:{}});renderPayment(pay.closest('.account-payment'),p);await showOrderDetail(p.businessNo);}catch(err){alert(err.message);pay.disabled=false;}return;}if(e.target.closest('[data-close-order-detail]')){const host=$('[data-hub-order-detail]');if(host)host.hidden=true;if(paymentPoll){clearInterval(paymentPoll);paymentPoll=null;}}});

  document.addEventListener('submit',async e=>{
    const form=e.target.closest('[data-after-sales-form]');if(!form)return;
    e.preventDefault();const button=form.querySelector('[type=submit]'),message=form.querySelector('[data-after-sales-message]');
    const category=form.elements.category.value,content=form.elements.content.value.trim();
    if(!category||!content){message.textContent='请选择问题类型并填写说明。';return;}
    button.disabled=true;message.textContent='正在提交…';
    try{
      const no=form.dataset.afterSalesForm;
      const result=await api(`/api/console/orders/${encodeURIComponent(no)}/after-sales`,{method:'POST',body:{category,content}});
      if(!result)return;
      message.textContent=result.message||'问题说明已提交';form.elements.content.value='';form.elements.category.value='';
      try{
        const detail=await api(`/api/console/orders/${encodeURIComponent(no)}`);
        const history=form.parentElement?.querySelector('[data-after-sales-history]');
        if(detail&&history)renderAfterSalesHistory(history,detail.order?.customerNote);
      }catch(_){message.textContent='问题说明已提交。记录暂时无法更新，请刷新页面查看。';}
    }
    catch(err){message.textContent=err.message||'提交失败，请稍后重试。';}
    finally{button.disabled=false;}
  });

  (async()=>{try{await loadIdentity();if(PAGE==='account-center-overview')await initOverview();if(PAGE==='account-center-requests')await initRequests();if(PAGE==='account-center-orders')await initOrders();if(PAGE==='account-center-progress')await initProgress();}catch(err){const host=$('[data-hub-message]');if(host){host.hidden=false;host.textContent=err.message||'加载失败，请刷新重试。';}else console.error(err);}})();
})();

