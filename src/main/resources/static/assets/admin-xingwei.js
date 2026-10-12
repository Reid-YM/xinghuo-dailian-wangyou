(() => {
  'use strict';

  const sidebar = document.getElementById('xwSidebar');
  const menuButton = document.getElementById('xwMenuButton');
  const search = document.getElementById('xwModuleSearch');
  const searchCount = document.getElementById('xwSearchCount');
  const backendStatus = document.getElementById('xwBackendStatus');
  const serverTime = document.getElementById('xwServerTime');
  const modules = Array.from(document.querySelectorAll('.xw-module'));
  const navItems = Array.from(document.querySelectorAll('.xw-nav-item'));

  function setMenu(open) {
    if (!sidebar || !menuButton) return;
    sidebar.classList.toggle('open', open);
    menuButton.setAttribute('aria-expanded', String(open));
  }

  menuButton?.addEventListener('click', () => {
    setMenu(!sidebar?.classList.contains('open'));
  });

  navItems.forEach((item) => {
    item.addEventListener('click', () => {
      navItems.forEach((entry) => entry.classList.remove('active'));
      item.classList.add('active');
      if (window.matchMedia('(max-width: 860px)').matches) setMenu(false);
    });
  });

  document.addEventListener('keydown', (event) => {
    if (event.key === 'Escape') setMenu(false);
    if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === 'k') {
      event.preventDefault();
      search?.focus();
    }
  });

  function normalize(value) {
    return String(value || '').toLocaleLowerCase('zh-CN').trim();
  }

  function filterModules() {
    const query = normalize(search?.value);
    let visible = 0;
    modules.forEach((module) => {
      const text = normalize(`${module.dataset.search || ''} ${module.textContent || ''}`);
      const match = !query || text.includes(query);
      module.hidden = !match;
      if (match) visible += 1;
    });
    if (searchCount) searchCount.textContent = query ? `匹配 ${visible} 个模块` : `全部 ${modules.length} 个模块`;
  }

  search?.addEventListener('input', filterModules);
  filterModules();

  async function checkBackend() {
    if (!backendStatus) return;
    try {
      const response = await fetch('/api/admin/xingwei/health', {
        method: 'GET',
        credentials: 'same-origin',
        headers: { 'Accept': 'application/json' }
      });
      if (!response.ok) throw new Error(`HTTP ${response.status}`);
      const data = await response.json();
      backendStatus.classList.remove('error');
      backendStatus.classList.add('ok');
      backendStatus.lastChild.textContent = '服务正常';
      if (serverTime && data.serverTime) {
        const parsed = new Date(data.serverTime);
        serverTime.textContent = Number.isNaN(parsed.getTime()) ? '后端已连接' : `后端已连接 · ${parsed.toLocaleString('zh-CN')}`;
      }
    } catch (error) {
      backendStatus.classList.remove('ok');
      backendStatus.classList.add('error');
      backendStatus.lastChild.textContent = '服务检查失败';
      if (serverTime) serverTime.textContent = '无法读取后台健康状态';
      console.warn('Xingwei admin health check failed', error);
    }
  }

  checkBackend();
})();
