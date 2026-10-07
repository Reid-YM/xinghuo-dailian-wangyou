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
  const time = value => { if(!value)return '—'; const d=new Date(value); return Number.isNaN(d.getTime())?String(value):d.toLocaleString('zh-CN',{hour12:false}); };

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
  async function loadIdentity(){const me=await api('/api/account/me');if(!me)return null;$$('[data-account-center-name]').forEach(n=>n.textContent=me.displayName||'星火用户');$$('[data-account-center-meta]').forEach(n=>n.textContent=[me.publicUserId,me.email||me.contactMasked].filter(Boolean).join(' · ')||'账号已登录');$$('[data-account-center-avatar]').forEach(n=>paintAvatar(n,me));return me;}
  function itemShell(status,title,description,meta){const article=el('article','account-center-item');const main=el('div','account-center-item-main');main.append(el('span','',status||'处理中'),el('h3','',title||'业务记录'),el('p','',description||'暂无更新'),el('small','',meta||''));const actions=el('div','account-center-item-actions');article.append(main,actions);return{article,actions};}
  function empty(host,text){if(!host)return;host.textContent='';host.appendChild(el('div','account-center-empty',text));}
  function renderQuote(host,q,compact=false){const row=itemShell(q.statusText||q.status,`${q.game||'未知游戏'} · ${q.demand||'需求'}`,q.currentProgress||q.nextAction||'等待客服处理',`${q.businessNo||'—'} · ${time(q.updatedAt||q.createdAt)}`);if(!compact){const b=el('button','btn ghost small-btn','补充说明');b.type='button';b.dataset.supplementQuote=q.businessNo||'';row.actions.appendChild(b);}host.appendChild(row.article);}
  function renderOrder(host,o,compact=false){const row=itemShell(o.statusText||o.status,`${o.gameName||'业务订单'} · ${o.serviceName||o.businessNo||'订单'}`,o.progressText||'等待进度更新',`${o.businessNo||o.orderNo||'—'} · ${time(o.updatedAt||o.createdAt)}`);const pct=Math.max(0,Math.min(100,Number(o.progressPercent)||0));const progress=el('div','account-center-progress'),bar=el('i');bar.style.width=`${pct}%`;progress.appendChild(bar);row.article.querySelector('.account-center-item-main').appendChild(progress);if(!compact){const d=el('button','btn ghost small-btn','查看详情');d.type='button';d.dataset.orderDetail=o.businessNo||o.orderNo||'';const s=el('button','btn ghost small-btn','补充说明');s.type='button';s.dataset.supplementOrder=o.businessNo||o.orderNo||'';row.actions.append(d,s);}host.appendChild(row.article);}
  async function supplement(kind,no){if(!no)return;const content=window.prompt('填写要补充给客服的说明（不要填写游戏密码、验证码或令牌）');if(!content||!content.trim())return;await api(`/api/console/${kind}/${encodeURIComponent(no)}/supplement`,{method:'POST',body:{content:content.trim()}});window.alert('补充说明已提交');}
  async function initOverview(){const s=await api('/api/console/summary');if(!s)return;const v={requests:s.quoteTotal,orders:s.orderTotal,active:s.activeOrders,security:s.securityEvents};Object.entries(v).forEach(([k,x])=>{const n=$(`[data-hub-kpi="${k}"]`);if(n)n.textContent=String(x??0);});const q=$('[data-hub-recent-requests]');if(q){q.textContent='';(s.recentQuotes||[]).forEach(x=>renderQuote(q,x,true));if(!(s.recentQuotes||[]).length)empty(q,'暂无需求记录');}const o=$('[data-hub-recent-orders]');if(o){o.textContent='';(s.recentOrders||[]).forEach(x=>renderOrder(o,x,true));if(!(s.recentOrders||[]).length)empty(o,'暂无订单记录');}}
  async function initRequests(){const host=$('[data-hub-requests]');if(!host)return;const d=await api('/api/console/quotes');host.textContent='';(d?.items||[]).forEach(x=>renderQuote(host,x));if(!(d?.items||[]).length)empty(host,'暂无需求记录。提交新需求后会自动出现在这里。');}
  async function initOrders(){const host=$('[data-hub-orders]');if(!host)return;const d=await api('/api/console/orders');host.textContent='';(d?.items||[]).forEach(x=>renderOrder(host,x));if(!(d?.items||[]).length)empty(host,'暂无订单记录。询价确认成交后会出现在这里。');}
  async function showOrderDetail(no){const d=await api('/api/console/orders/'+encodeURIComponent(no));if(!d)return;const host=$('[data-hub-order-detail]');if(!host)return;host.textContent='';host.hidden=false;const h=el('header');h.append(el('h3','',`${d.order?.businessNo||no} · 进度详情`));const close=el('button','btn ghost small-btn','关闭');close.type='button';close.dataset.closeOrderDetail='1';h.appendChild(close);host.appendChild(h);const t=el('div','account-center-timeline');(d.timeline||[]).forEach(x=>{const i=el('div');i.append(el('strong','',x.title||'进度'),el('p','',x.content||''),el('small','',time(x.createdAt)));t.appendChild(i);});host.appendChild(t);}
  async function initProgress(){const host=$('[data-hub-progress]');if(!host)return;const items=await api('/api/account/progress');host.textContent='';if(!(items||[]).length){empty(host,'还没有云端工具存档。进入 MW4 工具站后可按页面提示同步。');return;}(items||[]).forEach(x=>{const row=itemShell('云端存档',x.toolKey||'工具记录','进度内容请回到对应工具继续编辑，个人中心只负责查看存档状态。',`最近同步：${time(x.updatedAt)}`);const a=el('a','btn primary small-btn','打开工具站');a.href='/mastery';row.actions.appendChild(a);host.appendChild(row.article);});}

  document.addEventListener('click',async e=>{const logout=e.target.closest('[data-account-logout]');if(logout){try{await api('/api/account/logout',{method:'POST',body:{}});}finally{location.href='/';}return;}const q=e.target.closest('[data-supplement-quote]');if(q){try{await supplement('quotes',q.dataset.supplementQuote);await initRequests();}catch(err){alert(err.message);}return;}const o=e.target.closest('[data-supplement-order]');if(o){try{await supplement('orders',o.dataset.supplementOrder);await initOrders();}catch(err){alert(err.message);}return;}const d=e.target.closest('[data-order-detail]');if(d){try{await showOrderDetail(d.dataset.orderDetail);}catch(err){alert(err.message);}return;}if(e.target.closest('[data-close-order-detail]')){const host=$('[data-hub-order-detail]');if(host)host.hidden=true;}});

  (async()=>{try{await loadIdentity();if(PAGE==='account-center-overview')await initOverview();if(PAGE==='account-center-requests')await initRequests();if(PAGE==='account-center-orders')await initOrders();if(PAGE==='account-center-progress')await initProgress();}catch(err){const host=$('[data-hub-message]');if(host){host.hidden=false;host.textContent=err.message||'加载失败，请刷新重试。';}else console.error(err);}})();
})();
