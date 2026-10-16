(() => {
  'use strict';

  const sidebar = document.getElementById('xwSidebar');
  const menuButton = document.getElementById('xwMenuButton');
  const search = document.getElementById('xwModuleSearch');
  const searchCount = document.getElementById('xwSearchCount');
  const backendStatus = document.getElementById('xwBackendStatus');
  const serverTime = document.getElementById('xwServerTime');
  const declaredVersion = document.querySelector('.xw-version-card strong')?.textContent?.trim() || '';
  const declaredStage = (document.querySelector('#overview .xw-kicker')?.dataset.runtimeStage || document.querySelector('#overview .xw-kicker')?.textContent)?.trim() || '';
  const modules = Array.from(document.querySelectorAll('.xw-module'));
  const navItems = Array.from(document.querySelectorAll('.xw-nav-item'));
  const REQUEST_TIMEOUT_MS = 8000;
  const HEALTH_CHECK_INTERVAL_MS = 60000;
  const MODULE_PERMISSIONS = {
    B01: 'quote:view', B02: 'order:view', B03: 'service:view', B04: 'content:view', B05: 'order:view',
    B06: 'customer:view', B07: 'content:view', B08: 'feedback:view', B09: 'site:view', B10: 'quote:view',
    B11: 'content:view', B12: 'dashboard:view', B13: 'service:view', B14: 'order:view',
    O01: 'org:view', O02: 'employee:view', O03: 'rbac:view', O04: 'employee:view',
    C01: 'channel:view', C02: 'wechat:view', C03: 'payment:view', C04: 'site:view',
    S01: 'site:view', S02: 'audit:view', S03: 'deploy:view', S04: 'site:view'
  };
  let access = null;
  let healthTimer = 0;
  let healthInFlight = false;
  let healthFailures = 0;

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

  async function fetchWithTimeout(url, options = {}) {
    const controller = new AbortController();
    const timer = window.setTimeout(() => controller.abort(), REQUEST_TIMEOUT_MS);
    try {
      return await fetch(url, {...options, signal: controller.signal});
    } finally {
      window.clearTimeout(timer);
    }
  }

  function can(permission) {
    if (!access || !permission) return true;
    if (access.allowAll || access.permissionCodes?.includes(permission)) return true;
    if (permission.endsWith(':view')) {
      const manage = `${permission.slice(0, -':view'.length)}:manage`;
      return Boolean(access.permissionCodes?.includes(manage));
    }
    return false;
  }

  function canManageGlobalRbac() {
    return Boolean(access && can('rbac:manage') && (access.allowAll || access.dataScope === 'ALL'));
  }

  function modulePermission(module) {
    const code = module.querySelector('.xw-module-index')?.textContent?.trim();
    return MODULE_PERMISSIONS[code] || 'dashboard:view';
  }

  function scopeLabel(snapshot) {
    switch (snapshot?.dataScope) {
      case 'ALL': return '全部数据';
      case 'DEPARTMENT': return snapshot.departmentCode ? `本中心 · ${snapshot.departmentCode}` : '本中心 · 未分配中心时仅本人';
      case 'SELF': return '仅本人';
      default: return '无数据范围';
    }
  }

  function filterModules() {
    const query = normalize(search?.value);
    let visible = 0;
    modules.forEach((module) => {
      const text = normalize(`${module.dataset.search || ''} ${module.textContent || ''}`);
      const code = module.querySelector('.xw-module-index')?.textContent?.trim();
      const allowed = code === 'S01' ? can('site:view') || can('seo:view') : code === 'B14' ? can('order:view') || can('quote:view') : can(modulePermission(module));
      const globalAllowed = !module.hasAttribute('data-global-scope') || !access || access.allowAll || access.dataScope === 'ALL';
      const match = (!query || text.includes(query)) && allowed && globalAllowed;
      module.hidden = !match;
      if (match) visible += 1;
    });
    if (searchCount) {
      const suffix = access?.mode === 'SHADOW_RBAC' ? ` · RBAC · ${scopeLabel(access)}` : '';
      searchCount.textContent = query ? `匹配 ${visible} 个模块${suffix}` : `可用 ${visible} / ${modules.length} 个模块${suffix}`;
    }
  }

  search?.addEventListener('input', filterModules);
  filterModules();

  function applyActionPermissions(root = document) {
    if (!access) return;
    root.querySelectorAll?.('[data-employee-edit]').forEach((node) => {
      node.hidden = !(can('employee:manage') || can('rbac:manage'));
    });
    root.querySelectorAll?.('[data-role-edit]').forEach((node) => {
      node.hidden = !canManageGlobalRbac();
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
      case 'LEGACY_ADMIN_OVERRIDE': return '管理员兜底 · 全权限 · 全部数据';
      case 'LEGACY_OPERATOR_COMPAT': return 'OPERATOR 兼容 · 只读 · 全部数据';
      case 'SHADOW_RBAC': return `RBAC 已生效 · ${scopeLabel(snapshot)}`;
      default: return '新版权限不可用';
    }
  }

  async function loadAccess() {
    const badge = ensureAccessBadge();
    try {
      const response = await fetchWithTimeout('/api/admin/xingwei/access', {
        credentials: 'same-origin',
        cache: 'no-store',
        headers: {'Accept': 'application/json', 'Cache-Control': 'no-cache'}
      });
      if (!response.ok) throw new Error(`HTTP ${response.status}`);
      access = await response.json();
      window.xwAccess = access;
      window.xwHasPermission = can;
      window.xwCanManageGlobalRbac = canManageGlobalRbac;
      if (badge) {
        badge.textContent = modeLabel(access);
        badge.classList.toggle('ok', access.mode !== 'DENIED');
        badge.classList.remove('error');
        badge.title = access.mode === 'SHADOW_RBAC'
          ? `角色：${(access.roleCodes || []).join('、') || '无'}；权限点：${(access.permissionCodes || []).length}；数据范围：${scopeLabel(access)}`
          : '旧后台恢复与历史接口仅限原 ADMIN，员工通过新版权限和数据范围工作台操作。';
      }
      filterModules();
      applyActionPermissions(document);
      document.dispatchEvent(new CustomEvent('xw:access-ready', {detail: access}));
    } catch (error) {
      access = {mode: 'DENIED', allowAll: false, permissionCodes: [], roleCodes: [], dataScope: 'NONE'};
      window.xwAccess = access;
      window.xwHasPermission = can;
      window.xwCanManageGlobalRbac = canManageGlobalRbac;
      if (badge) {
        badge.textContent = error?.name === 'AbortError'
          ? '新版权限读取超时 · 受控模块已隐藏'
          : '新版权限读取失败 · 受控模块已隐藏';
        badge.classList.remove('ok');
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

  function publishRuntimeHealth(snapshot) {
    window.xwRuntimeHealth = snapshot;
    document.documentElement.dataset.xwRuntimeState = snapshot.state;
    document.dispatchEvent(new CustomEvent('xw:runtime-health', {detail: snapshot}));
  }

  function setBackendStatus(state, text, title) {
    if (!backendStatus) return;
    backendStatus.classList.toggle('ok', state === 'match');
    backendStatus.classList.toggle('error', state !== 'match');
    backendStatus.dataset.runtimeState = state;
    if (backendStatus.lastChild) backendStatus.lastChild.textContent = text;
    backendStatus.title = title || '';
  }

  function formatRuntimeLine(data, state) {
    const parsed = new Date(data.serverTime || '');
    const time = Number.isNaN(parsed.getTime()) ? '' : ` · ${parsed.toLocaleString('zh-CN')}`;
    if (state === 'match') return `运行 ${data.version} · 版本已核对${time}`;
    return `页面 ${declaredVersion || '未知'} · 运行 ${data.version || '未知'}${time}`;
  }

  function clearHealthTimer() {
    if (!healthTimer) return;
    window.clearTimeout(healthTimer);
    healthTimer = 0;
  }

  function scheduleHealthCheck() {
    clearHealthTimer();
    if (document.hidden) return;
    healthTimer = window.setTimeout(() => checkBackend('interval'), HEALTH_CHECK_INTERVAL_MS);
  }

  function publishOfflineState() {
    const previous = window.xwRuntimeHealth;
    const snapshot = {
      state: 'offline',
      declaredVersion,
      declaredStage,
      runtimeVersion: previous?.runtimeVersion || '',
      runtimeStage: previous?.runtimeStage || '',
      serverTime: previous?.serverTime || null,
      checkedAt: new Date().toISOString(),
      reason: 'offline',
      failures: healthFailures
    };
    publishRuntimeHealth(snapshot);
    setBackendStatus('offline', '网络已断开 · 等待恢复后复核', '浏览器当前离线；网络恢复后会立即重新核对运行版本。');
    if (serverTime) {
      serverTime.textContent = snapshot.runtimeVersion
        ? `上次核对 ${snapshot.runtimeVersion} · 当前离线`
        : `页面 ${declaredVersion || '未知'} · 当前离线`;
    }
  }

  async function checkBackend(reason = 'manual') {
    if (!backendStatus || healthInFlight) return;
    if (navigator.onLine === false) {
      clearHealthTimer();
      publishOfflineState();
      return;
    }

    healthInFlight = true;
    clearHealthTimer();
    try {
      const response = await fetchWithTimeout('/api/admin/xingwei/health', {
        method: 'GET',
        credentials: 'same-origin',
        headers: { 'Accept': 'application/json', 'Cache-Control': 'no-cache' },
        cache: 'no-store'
      });
      if (!response.ok) throw new Error(`HTTP ${response.status}`);
      const data = await response.json();
      const runtimeVersion = String(data.version || '').trim();
      const runtimeStage = String(data.stage || '').trim();
      const versionMatches = Boolean(declaredVersion && runtimeVersion && declaredVersion === runtimeVersion);
      const stageMatches = Boolean(declaredStage && runtimeStage && declaredStage === runtimeStage);
      const state = versionMatches && stageMatches ? 'match' : 'mismatch';
      healthFailures = 0;
      const snapshot = {
        state,
        declaredVersion,
        declaredStage,
        runtimeVersion,
        runtimeStage,
        serverTime: data.serverTime || null,
        checkedAt: new Date().toISOString(),
        reason,
        failures: 0
      };
      publishRuntimeHealth(snapshot);

      if (state === 'match') {
        setBackendStatus('match', `服务正常 · ${runtimeVersion} 已核对`, `页面与后端运行版本一致：${runtimeVersion} / ${window.XWLabels?.stage(runtimeStage) ?? runtimeStage}`);
      } else {
        setBackendStatus('mismatch', '版本漂移 · 请刷新并检查发布', `页面：${declaredVersion || '未知'} / ${window.XWLabels?.stage(declaredStage) || declaredStage || '未知'}；运行：${runtimeVersion || '未知'} / ${window.XWLabels?.stage(runtimeStage) || runtimeStage || '未知'}`);
      }
      if (serverTime) serverTime.textContent = formatRuntimeLine(data, state);
    } catch (error) {
      healthFailures += 1;
      const previous = window.xwRuntimeHealth;
      const hasVerifiedSnapshot = Boolean(previous?.runtimeVersion && previous?.state === 'match');
      const state = hasVerifiedSnapshot && healthFailures === 1 ? 'stale' : 'error';
      const snapshot = {
        state,
        declaredVersion,
        declaredStage,
        runtimeVersion: previous?.runtimeVersion || '',
        runtimeStage: previous?.runtimeStage || '',
        serverTime: previous?.serverTime || null,
        checkedAt: new Date().toISOString(),
        reason,
        failures: healthFailures,
        error: error?.name === 'AbortError' ? 'timeout' : 'request_failed'
      };
      publishRuntimeHealth(snapshot);

      if (state === 'stale') {
        setBackendStatus('stale', '复核暂时失败 · 保留上次已核对版本', `最近一次已核对 ${snapshot.runtimeVersion}；本次${snapshot.error === 'timeout' ? '请求超时' : '请求失败'}，稍后自动重试。`);
        if (serverTime) serverTime.textContent = `上次核对 ${snapshot.runtimeVersion} · 当前等待自动复核`;
      } else {
        setBackendStatus('error', error?.name === 'AbortError' ? '服务检查超时' : '服务检查失败', '无法读取后台健康状态，版本一致性未完成核验。');
        if (serverTime) serverTime.textContent = `页面 ${declaredVersion || '未知'} · 无法读取运行版本`;
      }
      console.warn('Xingwei admin health check failed', error);
    } finally {
      healthInFlight = false;
      scheduleHealthCheck();
    }
  }

  document.addEventListener('visibilitychange', () => {
    if (document.hidden) {
      clearHealthTimer();
      return;
    }
    checkBackend('visibility');
  });
  window.addEventListener('online', () => checkBackend('online'));
  window.addEventListener('offline', () => {
    clearHealthTimer();
    publishOfflineState();
  });

  loadAccess();
  checkBackend('initial');
})();
