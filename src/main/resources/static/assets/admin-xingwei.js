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
  const MODULE_PERMISSIONS = {
    B01: 'quote:view', B02: 'order:view', B03: 'service:view', B04: 'content:view',
    O01: 'org:view', O02: 'employee:view', O03: 'rbac:view', O04: 'employee:view',
    C01: 'channel:view', C02: 'wechat:view', C03: 'payment:view', C04: 'site:view',
    S01: 'site:view', S02: 'audit:view', S03: 'deploy:view', S04: 'site:view'
  };
  let access = null;

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

  function can(permission) {
    if (!access || !permission) return true;
    return Boolean(access.allowAll || access.permissionCodes?.includes(permission));
  }

  function modulePermission(module) {
    const code = module.querySelector('.xw-module-index')?.textContent?.trim();
    return MODULE_PERMISSIONS[code] || 'dashboard:view';
  }

  function filterModules() {
    const query = normalize(search?.value);
    let visible = 0;
    modules.forEach((module) => {
      const text = normalize(`${module.dataset.search || ''} ${module.textContent || ''}`);
      const match = (!query || text.includes(query)) && can(modulePermission(module));
      module.hidden = !match;
      if (match) visible += 1;
    });
    if (searchCount) {
      const suffix = access?.mode === 'SHADOW_RBAC' ? ' · 已按角色过滤' : '';
      searchCount.textContent = query ? `匹配 ${visible} 个模块${suffix}` : `可用 ${visible} / ${modules.length} 个模块${suffix}`;
    }
  }

  search?.addEventListener('input', filterModules);
  filterModules();

  function applyActionPermissions(root = document) {
    if (!access) return;
    root.querySelectorAll?.('[data-employee-edit]').forEach((node) => {
      node.hidden = !can('employee:manage');
    });
    root.querySelectorAll?.('[data-role-edit]').forEach((node) => {
      node.hidden = !can('rbac:manage');
    });
  }

  function ensureAccessBadge() {
    const actions = document.querySelector('.xw-topbar-actions');
    if (!actions) return null;
    let badge = document.getElementById('xwAccessMode');
    if (!badge) {
      badge = document.createElement('span');
      badge.id = 'xwAccessMode';
      badge.className = 'xw-status';
      actions.prepend(badge);
    }
    return badge;
  }

  function modeLabel(snapshot) {
    switch (snapshot?.mode) {
      case 'LEGACY_ADMIN_OVERRIDE': return '管理员兜底 · 全权限';
      case 'LEGACY_OPERATOR_COMPAT': return 'OPERATOR 兼容 · 只读';
      case 'SHADOW_RBAC': return `RBAC 已生效 · ${(snapshot.roleCodes || []).length} 角色`;
      default: return '新版权限不可用';
    }
  }

  async function loadAccess() {
    const badge = ensureAccessBadge();
    try {
      const response = await fetch('/api/admin/xingwei/access', {
        credentials: 'same-origin',
        headers: {'Accept': 'application/json'}
      });
      if (!response.ok) throw new Error(`HTTP ${response.status}`);
      access = await response.json();
      window.xwAccess = access;
      window.xwHasPermission = can;
      if (badge) {
        badge.textContent = modeLabel(access);
        badge.classList.toggle('ok', access.mode !== 'DENIED');
        badge.title = access.mode === 'SHADOW_RBAC'
          ? `角色：${(access.roleCodes || []).join('、') || '无'}；权限点：${(access.permissionCodes || []).length}`
          : '旧后台仍沿用原 ADMIN / OPERATOR 安全边界。';
      }
      filterModules();
      applyActionPermissions(document);
      document.dispatchEvent(new CustomEvent('xw:access-ready', {detail: access}));
    } catch (error) {
      access = {mode: 'DENIED', allowAll: false, permissionCodes: [], roleCodes: []};
      window.xwAccess = access;
      window.xwHasPermission = can;
      if (badge) {
        badge.textContent = '新版权限读取失败 · 受控模块已隐藏';
        badge.classList.add('error');
      }
      filterModules();
      applyActionPermissions(document);
      console.warn('Xingwei access snapshot failed', error);
    }
  }

  const mutationObserver = new MutationObserver((mutations) => {
    if (!access) return;
    for (const mutation of mutations) {
      for (const node of mutation.addedNodes) {
        if (node.nodeType === Node.ELEMENT_NODE) applyActionPermissions(node);
      }
    }
  });
  mutationObserver.observe(document.querySelector('.xw-main') || document.body, {childList: true, subtree: true});

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

  loadAccess();
  checkBackend();
})();
