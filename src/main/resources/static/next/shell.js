(() => {
  'use strict';
  const pages = new Set(['home', 'services', 'quote', 'account']);
  const titles = {home:'星火网游工作室',services:'服务大厅 · 星火网游',quote:'提交询价 · 星火网游',account:'个人中心 · 星火网游'};
  const base = location.pathname === '/next' || location.pathname.startsWith('/next/') ? '/next' : '';
  const route = page => page === 'home' ? (base || '/') : base + '/' + page;
  function resolveLocation() {
    const hash = location.hash.slice(1);
    if (pages.has(hash)) return hash;
    const path = base ? location.pathname.slice(base.length) : location.pathname;
    if (path === '/services') return 'services';
    if (path === '/quote') return 'quote';
    if (path === '/account' || path.startsWith('/account/')) return 'account';
    return 'home';
  }
  function bindTabs(list) {
    list.addEventListener('keydown', event => {
      if (!['ArrowLeft','ArrowRight','Home','End'].includes(event.key)) return;
      const tabs = [...list.querySelectorAll('[role=tab]')].filter(tab => !tab.disabled);
      const index = tabs.indexOf(event.target.closest('[role=tab]'));
      if (index < 0 || !tabs.length) return;
      event.preventDefault();
      const nextIndex = event.key === 'Home' ? 0 : event.key === 'End' ? tabs.length - 1
        : (index + (event.key === 'ArrowRight' ? 1 : -1) + tabs.length) % tabs.length;
      const next = tabs[nextIndex];
      next.click();
      (document.getElementById(next.id) || next).focus();
    });
  }
  document.querySelector('.next-skip-link')?.addEventListener('click', () => {
    const page = document.querySelector('.page.active');
    if (!page) return;
    page.tabIndex = -1;
    page.focus({preventScroll:true});
    page.scrollIntoView({block:'start',behavior:'auto'});
  });
  function go(page, push = true) {
    if (!pages.has(page)) page = 'home';
    document.querySelectorAll('.page').forEach(node => node.classList.toggle('active', node.dataset.page === page));
    document.querySelectorAll('.navlink[data-go]').forEach(node => {
      const active = node.dataset.go === page;
      node.classList.toggle('active', active);
      if (active) node.setAttribute('aria-current', 'page'); else node.removeAttribute('aria-current');
    });
    if (push && location.pathname + location.search + location.hash !== route(page)) history.pushState({page}, '', route(page));
    document.title = titles[page];
    document.documentElement.dataset.frontendPage = page;
    if(push)window.scrollTo({top:0,behavior:'instant'});
    let canonical=document.querySelector('link[rel=canonical]');if(!canonical){canonical=document.createElement('link');canonical.rel='canonical';document.head.append(canonical);}
    canonical.href='https://wydlgf.cn'+(page==='home'?'/':'/'+page);
    let robots=document.querySelector('meta[name=robots]');if(page==='account'){if(!robots){robots=document.createElement('meta');robots.name='robots';document.head.append(robots);}robots.content='noindex,nofollow';}else robots?.remove();
    window.dispatchEvent(new CustomEvent('xh:pagechange', {detail:{page}}));
  }
  function toast(message) {
    const node = document.getElementById('toast'); if (!node) return;
    node.textContent = message; node.setAttribute('role', 'status'); node.classList.add('show');
    clearTimeout(window.tt); window.tt = setTimeout(() => node.classList.remove('show'), 2000);
  }
  document.querySelectorAll('a[data-go]').forEach(node => node.href = route(node.dataset.go));
  document.addEventListener('click', event => {
    const node = event.target.closest('[data-go]');
    if (!node || event.defaultPrevented || event.button > 0 || event.metaKey || event.ctrlKey || event.shiftKey || event.altKey) return;
    if (node.tagName === 'A' && node.getAttribute('href') !== route(node.dataset.go)) return;
    event.preventDefault(); go(node.dataset.go);
  });
  window.addEventListener('popstate', () => go(resolveLocation(), false));
  window.addEventListener('hashchange', () => {if(pages.has(location.hash.slice(1)))go(resolveLocation(), false);});
  let configuration = null;
  let currentServices = null;
  const attribution=(()=>{
    const params=new URLSearchParams(location.search),keys=['utm_source','utm_medium','utm_campaign'];let previous=null;
    try{previous=JSON.parse(sessionStorage.getItem('xh_next_attribution_v1')||'null');}catch(_){}
    if(!keys.some(key=>params.has(key))&&previous&&typeof previous==='object'){const clean={};for(const key of ['firstTouchSource','utmSource','utmMedium','utmCampaign'])clean[key]=typeof previous[key]==='string'?previous[key].slice(0,64):null;if(clean.firstTouchSource)return clean;}
    let source='direct';try{const ref=new URL(document.referrer);if(ref.hostname!==location.hostname)source=ref.hostname.slice(0,64);}catch(_){}
    const value={firstTouchSource:params.get('utm_source')?.slice(0,64)||source,utmSource:params.get('utm_source')?.slice(0,64)||null,utmMedium:params.get('utm_medium')?.slice(0,64)||null,utmCampaign:params.get('utm_campaign')?.slice(0,64)||null};
    try{sessionStorage.setItem('xh_next_attribution_v1',JSON.stringify(value));}catch(_){}return value;
  })();
  function track(eventName,refValue=null){
    if(!['quote_started','contact_click','quote_submit','hero_quote_click','hero_order_click','service_card_click','screenshot_upload'].includes(eventName))return;
    fetch('/api/track',{method:'POST',headers:{'Content-Type':'application/json'},credentials:'same-origin',keepalive:true,body:JSON.stringify({eventName,path:route(resolveLocation()),refValue})}).catch(()=>{});
  }
  const number = (value,min,max,fallback) => value != null && value !== '' && Number.isFinite(Number(value)) ? Math.max(min,Math.min(max,Number(value))) : fallback;
  const make = (tag,text,className) => {const node=document.createElement(tag);if(text!=null)node.textContent=String(text);if(className)node.className=className;return node;};
  function safeLink(value) {
    const text = String(value || '').trim();
    if (text.startsWith('/') && !text.startsWith('//')) return text;
    try { const url = new URL(text); if (url.protocol === 'https:' && !url.username && !url.password) return url.href; } catch (_) {}
    return '';
  }
  function serviceCards(target,items,empty) {
    const grid=document.getElementById(target);if(!grid)return;
    grid.replaceChildren(...items.map(item=>{
      const card=make('a',null,'home-service-card');card.href='/service/'+encodeURIComponent(String(item.slug||''));
      const image=safeLink(item.coverImage);if(image){const cover=make('div',null,'service-cover'),img=make('img');img.src=image;img.alt=String(item.name||'服务封面');img.loading='lazy';cover.append(img);card.append(cover);}
      card.append(make('h3',item.name||'服务项目'),make('p',item.shortDesc||''));
      const fixed=item.priceFixed,min=item.priceMin,max=item.priceMax,mode=String(item.priceMode||'').toUpperCase();
      const amount=value=>value!=null&&value!==''&&Number.isFinite(Number(value))&&Number(value)>=0;
      let price='联系客服询价';
      if(mode==='FIXED'&&amount(fixed))price='¥'+Number(fixed);
      else if(mode==='RANGE'&&amount(min)&&amount(max))price='¥'+Number(min)+'–'+Number(max);
      else if(mode==='RANGE'&&amount(min))price='¥'+Number(min)+' 起';
      card.append(make('p',price,'price'));return card;
    }));
    if(!items.length)grid.append(make('p',empty));
  }
  function updateServices(services) {
    if(Array.isArray(services))currentServices=services;
    if(!Array.isArray(currentServices))return;
    serviceCards('nextHomeServices',currentServices.slice(0,number(configuration?.homeServiceLimit,2,8,4)),'当前暂无可展示的服务。');
  }
  function updateCatalog(catalog) {
    const grid=document.getElementById('nextHomeGames');if(!grid||!Array.isArray(catalog?.games))return;
    grid.replaceChildren(...catalog.games.map(game=>{const link=make('a',null,'home-service-card');link.href=game.seoSlug?'/game/'+encodeURIComponent(String(game.seoSlug)):route('services')+'?game='+encodeURIComponent(String(game.code));link.append(make('h3',game.name||game.code));return link;}));
    if(!catalog.games.length)grid.append(make('p','当前暂无公开游戏目录。'));
  }
  function dataUnavailable(target) {
    const grid=document.getElementById(target);if(!grid)return;
    const retry=make('button','重新读取','btn');retry.type='button';retry.onclick=async()=>{retry.disabled=true;try{await window.XHFrontendNextN2?.reload();}finally{retry.disabled=false;}};
    grid.replaceChildren(make('p','目录暂时无法读取，请重试。'),retry);
  }
  function configuredContent(site) {
    serviceCards('nextHotServices',Array.isArray(site.hotServices)?site.hotServices:[],'暂无热门推荐，可浏览全部服务或联系客服。');
    updateServices();
    const cases=Array.isArray(site.trustCases)?site.trustCases:[],caseGrid=document.getElementById('nextTrustCaseGrid');
    if(caseGrid){caseGrid.replaceChildren(...cases.map(item=>{const card=make('article',null,'card');card.append(make('small',item.tag||'服务说明'),make('h3',item.title||''),make('p',item.metric||''),make('p',item.detail||''));return card;}));const section=document.getElementById('nextTrustCases');section.dataset.empty=String(!cases.length);section.hidden=!cases.length;}
    const channels=document.getElementById('nextContactChannels');
    if(channels){channels.replaceChildren(...(Array.isArray(site.contactChannels)?site.contactChannels:[]).map(item=>{
      const card=make('article',null,'card');card.append(make('h3',item.label||'客服'),make('p',item.wechatId||''),make('p',item.note||''));
      const url=safeLink(item.jumpUrl);if(url){const link=make('a','联系此客服','btn');link.href=url;link.dataset.supportDirect='';card.append(link);}return card;
    }));}
    const faq=Array.isArray(site.trustFaq)?site.trustFaq:[],faqGrid=document.getElementById('nextFaqList');
    if(faqGrid){faqGrid.replaceChildren(...faq.map(item=>{const details=make('details');details.append(make('summary',item.question||'常见问题'),make('p',item.answer||''));return details;}));document.getElementById('nextFaq').hidden=!faq.length;}
    const status=document.getElementById('nextServiceStatus');
    if(status)status.textContent=site.statusShortText||({open:'当前正常接单',reservation:'预约中 · 可提前询价',preheat:'预热中 · 可提前询价',maintenance:'维护中 · 暂时无法开工',suspended:'当前暂停接单',closed:'当前暂停接单',busy:'当前繁忙，建议先询价'}[site.serviceStatus]||'服务状态待客服确认');
    const starting=document.getElementById('nextStartingPrice');
    if(starting){const amount=Number(site.startingPrice);starting.hidden=site.showStartingPrice!==true||site.startingPrice==null||!Number.isFinite(amount)||amount<0;starting.textContent='参考起价 ¥'+amount+'，最终金额由客服确认';}
  }
  async function publicCases() {
    const section=document.getElementById('nextPublicCases'),grid=document.getElementById('nextPublicCaseGrid');if(!section||!grid)return;
    try{const response=await fetch('/api/public/cases',{cache:'no-store',credentials:'same-origin'});if(!response.ok)return;const items=await response.json();if(!Array.isArray(items)||!items.length)return;
      grid.replaceChildren(...items.slice(0,6).map(item=>{const card=make('article',null,'card');card.append(make('small','已完成'),make('h3',item.maskedNo||'已脱敏记录'),make('p',item.summary||'已完成交付'));if(item.finishedAt)card.append(make('small','完成日期 '+String(item.finishedAt).slice(0,10)));return card;}));section.hidden=false;
    }catch(_){/* Only explicitly public records are shown; failure keeps this optional section hidden. */}
  }
  function applyConfiguration(site) {
    configuration = site;
    for (const field of ['brandName','heroTitle','heroSubtitle','footerText','quickContactTitle']) {
      document.querySelectorAll('[data-config="'+field+'"]').forEach(node => {node.textContent=String(site[field] || '');});
    }
    const brand = site.brandName || '星火网游';
    Object.assign(titles,{home:site.seoTitle || brand,services:'服务大厅 · '+brand,quote:'提交询价 · '+brand,account:'个人中心 · '+brand});
    document.title=titles[resolveLocation()];
    document.querySelectorAll('[data-support-link]').forEach(node => {node.href=safeLink(site.wecomKefuUrl) || '/contact';});
    const heroImage=document.querySelector('[data-config="heroImage"]'),visual=document.getElementById('nextHeroVisual');
    if(heroImage&&visual){const url=safeLink(site.heroImage);visual.hidden=!url;if(url){heroImage.src=url;window.SiteImageControls?.main(visual,heroImage,site);}else heroImage.removeAttribute('src');}
    window.SiteImageControls?.background(document.getElementById('homeHero'),document.getElementById('heroBackground'),site,safeLink(site.heroBackgroundImage));
    document.documentElement.style.setProperty('--service-cover-height',number(site.serviceCoverHeight,70,240,115)+'px');
    document.documentElement.style.setProperty('--service-cover-fit',['cover','contain','fill'].includes(site.serviceCoverFit)?site.serviceCoverFit:'cover');
    document.documentElement.style.setProperty('--service-cover-position',number(site.serviceCoverPositionX,0,100,50)+'% '+number(site.serviceCoverPositionY,0,100,50)+'%');
    configuredContent(site);
    const promises=document.querySelector('#homeHero .hero-promise');
    if(promises && Array.isArray(site.trustTags)) promises.replaceChildren(...site.trustTags.map(value=>{const item=document.createElement('div');item.textContent=String(value);return item;}));
    let announcement=document.getElementById('nextAnnouncement');
    if(!announcement){announcement=document.createElement('p');announcement.id='nextAnnouncement';announcement.className='next-config-state';announcement.setAttribute('role','status');document.querySelector('.nav')?.after(announcement);}
    announcement.textContent=site.announcementEnabled?String(site.announcementText||''):'';announcement.hidden=!site.announcementEnabled;
    document.querySelectorAll('[data-primary-cta],.nav .actions [data-go="quote"]').forEach(node=>{if(site.primaryButtonLabel)node.textContent=site.primaryButtonLabel;});
    const submit=document.getElementById('submit');if(submit)submit.disabled=site.quoteEnabled===false;
    document.documentElement.dataset.siteConfiguration='ready';
    window.dispatchEvent(new CustomEvent('xh:site-config',{detail:site}));
  }
  async function loadConfiguration() {
    let message=document.getElementById('nextConfigState');
    if(message)message.remove();
    try {
      const response=await fetch('/api/site-config',{cache:'no-store',credentials:'same-origin'});
      if(!response.ok)throw new Error();
      const data=await response.json();if(!data || typeof data.brandName!=='string')throw new Error();
      applyConfiguration(data);return data;
    } catch (_) {
      document.documentElement.dataset.siteConfiguration='error';
      if(!configuration){document.getElementById('nextHotServices')?.replaceChildren(make('p','推荐服务暂时无法读取，请重试站点配置。'));}
      message=document.createElement('div');message.id='nextConfigState';message.className='next-config-state';message.setAttribute('role','status');
      message.append(document.createTextNode('站点配置暂时无法读取，请重试。'));
      const retry=document.createElement('button');retry.type='button';retry.className='btn';retry.textContent='重新读取';retry.onclick=()=>{window.XHFrontendShell.configurationReady=loadConfiguration();};message.append(retry);document.querySelector('.nav')?.after(message);
      return null;
    }
  }
  function safeAccountReturn(requested) {
    if(requested){
      try{if(!String(requested).startsWith('/') || String(requested).startsWith('//'))return route('account');const target=new URL(requested,location.origin);if(target.origin===location.origin&&!/^\/(admin|api|logout)(\/|$)/i.test(target.pathname)&&!/^\/account\/(login|register|reset)$/.test(target.pathname))return target.pathname+target.search+target.hash;}catch(_){}
      return route('account');
    }
    return null;
  }
  window.go = go; window.toast = toast;
  document.querySelector('.page[data-page=quote]')?.addEventListener('focusin',()=>track('quote_started'),{once:true});
  document.addEventListener('click',event=>{const link=event.target.closest('[data-support-link],[data-support-direct]');if(!link)return;const target=new URL(link.href,location.origin);if(target.origin===location.origin&&target.pathname==='/contact')return;track('contact_click');});
  document.addEventListener('click',event=>{
    if(event.target.closest('#homeHero [data-primary-cta]'))track('hero_quote_click');
    if(event.target.closest('.record-card a[href="/records"]'))track('hero_order_click');
    const service=event.target.closest('a.home-service-card[href^="/service/"]');if(service)track('service_card_click',service.getAttribute('href').slice('/service/'.length));
  });
  window.XHFrontendShell = {bindTabs, go, route, resolveLocation, applyConfiguration, updateServices, updateCatalog, dataUnavailable, safeAccountReturn, track, attribution, get configuration(){return configuration;}};
  window.XHFrontendShell.configurationReady=loadConfiguration();
  publicCases();
  go(resolveLocation(), false);
})();
