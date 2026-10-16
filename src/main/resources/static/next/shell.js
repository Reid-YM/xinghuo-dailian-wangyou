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
  function safeLink(value) {
    const text = String(value || '').trim();
    if (text.startsWith('/') && !text.startsWith('//')) return text;
    try { const url = new URL(text); if (url.protocol === 'https:' && !url.username && !url.password) return url.href; } catch (_) {}
    return '';
  }
  function applyConfiguration(site) {
    configuration = site;
    for (const field of ['brandName','heroTitle','heroSubtitle','footerText']) {
      document.querySelectorAll('[data-config="'+field+'"]').forEach(node => {node.textContent=String(site[field] || '');});
    }
    const brand = site.brandName || '星火网游';
    Object.assign(titles,{home:site.seoTitle || brand,services:'服务大厅 · '+brand,quote:'提交询价 · '+brand,account:'个人中心 · '+brand});
    document.title=titles[resolveLocation()];
    document.querySelectorAll('[data-support-link]').forEach(node => {node.href=safeLink(site.wecomKefuUrl) || '/contact';});
    const heroImage=document.querySelector('[data-config="heroImage"]');
    if(heroImage){const url=safeLink(site.heroImage);heroImage.hidden=!url;if(url){heroImage.src=url;heroImage.style.objectFit=['cover','contain'].includes(site.heroImageFit)?site.heroImageFit:'contain';heroImage.style.height=Math.max(80,Math.min(420,Number(site.heroImageHeight)||220))+'px';heroImage.style.width='100%';heroImage.style.objectPosition=(Number(site.heroImagePositionX)||50)+'% '+(Number(site.heroImagePositionY)||50)+'%';}}
    const promises=document.querySelector('#homeHero .hero-promise');
    if(promises && Array.isArray(site.trustTags)) promises.replaceChildren(...site.trustTags.map(value=>{const item=document.createElement('div');item.textContent=String(value);return item;}));
    let announcement=document.getElementById('nextAnnouncement');
    if(!announcement){announcement=document.createElement('p');announcement.id='nextAnnouncement';announcement.className='next-config-state';announcement.setAttribute('role','status');document.querySelector('.nav')?.after(announcement);}
    announcement.textContent=site.announcementEnabled?String(site.announcementText||''):'';announcement.hidden=!site.announcementEnabled;
    document.querySelectorAll('[data-go="quote"]').forEach(node=>{if(site.primaryButtonLabel)node.textContent=site.primaryButtonLabel;});
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
      message=document.createElement('div');message.id='nextConfigState';message.className='next-config-state';message.setAttribute('role','status');
      message.append(document.createTextNode('站点配置暂时无法读取，请重试。'));
      const retry=document.createElement('button');retry.type='button';retry.className='btn';retry.textContent='重新读取';retry.onclick=()=>{window.XHFrontendShell.configurationReady=loadConfiguration();};message.append(retry);document.querySelector('.nav')?.after(message);
      return null;
    }
  }
  window.go = go; window.toast = toast;
  window.XHFrontendShell = {go, route, resolveLocation, applyConfiguration, get configuration(){return configuration;}};
  window.XHFrontendShell.configurationReady=loadConfiguration();
  go(resolveLocation(), false);
})();
