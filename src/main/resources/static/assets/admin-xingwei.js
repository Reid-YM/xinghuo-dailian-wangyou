(() => {
  'use strict';

  const sidebar = document.getElementById('xwSidebar');
  const menuButton = document.getElementById('xwMenuButton');
  const search = document.getElementById('xwModuleSearch');
  const searchCount = document.getElementById('xwSearchCount');
  const backendStatus = document.getElementById('xwBackendStatus');
  const serverTime = document.getElementById('xwServerTime');
  const declaredVersion = document.querySelector('.xw-version-card strong')?.textContent?.trim() || '';
  const declaredStage = (document.querySelector('[data-runtime-stage]')?.dataset.runtimeStage || document.querySelector('#overview .xw-kicker')?.textContent)?.trim() || '';
  const modules = Array.from(document.querySelectorAll('.xw-module'));
  const navItems = Array.from(document.querySelectorAll('.xw-nav-item'));
  const workspaceNames = {overview:'运营总览',business:'业务中台',organization:'组织与权限',channels:'渠道与微信',system:'系统工具'};
  const panels = Array.from(document.querySelectorAll('[data-workspace]'));
  const workspaceGroups = {
    business:[['orders','询价与订单','处理客户需求、订单、售后与跟进，沿用当前岗位权限和业务数据范围。'],['catalog','服务与目录','维护服务项目、封面、有效期、游戏平台与搜索资料，发布及启停继续执行确认流程。'],['content','内容与社区','维护攻略、评论投稿与工具站内容，按权限查看和审核。'],['operations','会员与运营','处理会员、测试反馈、首页推荐与行为统计；全站资料仍受对应权限和数据范围约束。']],
    organization:[['structure','中心与岗位','查看正式中心架构与岗位词库，保持组织资料一致。'],['employees','员工与账号','查看员工档案、企业微信员工与账号安全，修改仍受岗位权限和数据范围约束。'],['roles','角色与权限','查看和维护角色权限，管理层安全兜底角色继续只读。']],
    channels:[['operations','渠道运营','安排和跟进官网、淘宝、闲鱼的渠道运营任务。'],['wechat','微信与支付','查看微信身份能力与受控支付入口，真实收款和外部操作仍按各自验收范围执行。'],['display','前台展示','维护前台导航、展示开关和主题参数。']],
    system:[['site','站点配置','维护品牌、文案、图片与搜索展示。'],['audit','操作审计','按权限和数据范围查询操作记录与变更摘要。'],['delivery','状态与帮助','查看服务连接提示与工作台使用说明。']]
  };
  const groupedItems = Array.from(document.querySelectorAll('[data-workspace-group]'));
  const allowedModules = new Set();
  const orgAnchors = new Map([['org-console','structure'],['org-positions','structure'],['org-employees','employees'],['org-roles','roles']]);
  function routeFor(hash) {
    const id = String(hash || '').replace(/^#/, '');
    if (orgAnchors.has(id)) return {workspace:'organization',group:orgAnchors.get(id)};
    const [workspace,group,...rest] = id.split('/');
    if (!Object.hasOwn(workspaceNames, workspace)) return {workspace:'overview',group:''};
    const validGroup = !rest.length && workspaceGroups[workspace]?.some(([key]) => key === group);
    return {workspace,group:validGroup ? group : ''};
  }
  const workspaceFor = hash => routeFor(hash).workspace;
  const initialRoute = routeFor(location.hash);
  let activeWorkspace = initialRoute.workspace;
  let activeGroup = initialRoute.group;
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

  function chooseWorkspace(hash) {
    const route = routeFor(hash);
    activeWorkspace = route.workspace;
    activeGroup = route.group;
    if (search) search.value = '';
    filterModules();
    if (window.matchMedia('(max-width: 860px)').matches) setMenu(false);
  }

  document.addEventListener('click', (event) => {
    if (event.button !== 0 || event.ctrlKey || event.metaKey || event.shiftKey || event.altKey) return;
    const link = event.target.closest?.('a[href^="#"]');
    if (!link || (!sidebar?.contains(link) && !document.querySelector('.xw-main')?.contains(link))) return;
    const hash = link.getAttribute('href');
    const id = hash.slice(1);
    const route = routeFor(hash);
    const groupLink = id === `${route.workspace}/${route.group}` && route.group;
    if (Object.hasOwn(workspaceNames, id) || groupLink) {
      event.preventDefault();
      if (location.hash !== hash) history.pushState(null, '', hash);
      chooseWorkspace(hash);
      window.scrollTo(0, 0);
    } else if (orgAnchors.has(id)) chooseWorkspace(hash);
  });
  window.addEventListener('hashchange', () => chooseWorkspace(location.hash));
  document.getElementById('xwSearchClear')?.addEventListener('click', () => {
    if (search) search.value = '';
    filterModules();
    search?.focus();
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

  function searchDetails(query) {
    const target = document.getElementById('xwSearchResults');
    if (!target) return [];
    target.hidden = !query;
    target.replaceChildren();
    const tokens = query.split(/\s+/).filter(Boolean);
    const readable = new Map([...allowedModules].map(module => [module.querySelector('.xw-module-index')?.textContent.trim(), module]));
    const seen = new Set();
    const rows = query ? (window.XWSearchIndex || []).filter(row => readable.has(row.module) && (!row.permission || can(row.permission)) && tokens.every(token => normalize(`${row.label} ${row.keywords} ${readable.get(row.module).querySelector('h3')?.textContent}`).includes(token))).filter(row => {const key = `${row.label}::${row.href}`;if(seen.has(key))return false;seen.add(key);return true;}) : [];
    rows.sort((a,b) => Number(b.label === query) - Number(a.label === query) || Number(b.label.includes(query)) - Number(a.label.includes(query)));
    for (const row of rows) {
      const item = document.createElement('li'), link = document.createElement('a'), label = document.createElement('strong'), detail = document.createElement('span');
      link.href = row.href;
      label.textContent = row.label;
      const module = readable.get(row.module), workspace = module.closest('[data-workspace]')?.dataset.workspace;
      detail.textContent = `${workspaceNames[workspace]} · ${module.querySelector('h3')?.textContent} · ${row.note}`;
      link.append(label, detail); item.append(link); target.append(item);
    }
    const status = document.getElementById('xwSearchDetailStatus');
    if (status) {status.hidden = !query; status.textContent = query ? (window.XWSearchIndex ? `找到 ${rows.length} 个功能或字段；可用键盘逐项进入。` : '细节索引加载失败，当前仅可筛选模块；请刷新重试。') : '';}
    return rows;
  }

  function filterModules() {
    const query = normalize(search?.value);
    let visible = 0;
    allowedModules.clear();
    modules.forEach(module => {
      const code = module.querySelector('.xw-module-index')?.textContent?.trim();
      const allowed = Boolean(access) && (code === 'S01' ? can('site:view') || can('seo:view') : code === 'B14' ? can('order:view') || can('quote:view') : can(modulePermission(module)));
      if (allowed && (!module.hasAttribute('data-global-scope') || access.allowAll || access.dataScope === 'ALL')) allowedModules.add(module);
    });
    const details = searchDetails(query);
    const matchedCodes = new Set(details.map(row => row.module));
    modules.forEach((module) => {
      const text = normalize(`${module.dataset.search || ''} ${module.textContent || ''}`);
      const code = module.querySelector('.xw-module-index')?.textContent?.trim();
      const allowed = allowedModules.has(module);
      const match = (!query || query.split(/\s+/).every(token => text.includes(token)) || matchedCodes.has(code)) && allowed;
      module.hidden = !match;
      if (match) visible += 1;
    });
    showWorkspace(query, visible);
    if (searchCount) {
      const suffix = access?.mode === 'SHADOW_RBAC' ? ` · 角色权限 · ${scopeLabel(access)}` : '';
      searchCount.textContent = !access ? '正在读取可用模块' : query ? `匹配 ${details.length} 项细节 / ${visible} 个模块${suffix}` : `可用 ${visible} / ${modules.length} 个模块${suffix}`;
    }
  }

  function groupReadable(item) {
    const permissions = item.dataset.groupPermission;
    return !permissions || Boolean(access) && permissions.split(' ').some(can);
  }

  function groupCount(workspace, group) {
    return groupedItems.filter(item => item.dataset.workspaceGroup === group && item.closest('[data-workspace]')?.dataset.workspace === workspace)
      .reduce((count, item) => count + [...item.querySelectorAll('.xw-module')].filter(module => allowedModules.has(module)).length + (item.hasAttribute('data-group-entry') && groupReadable(item) ? 1 : 0), 0);
  }

  function showGroups(query) {
    const groups = workspaceGroups[activeWorkspace] || [];
    if (!activeGroup && groups.length && access) activeGroup = (groups.find(([key]) => groupCount(activeWorkspace, key) > 0) || groups[0])[0];
    const raw = location.hash.slice(1);
    if (access && activeGroup && !orgAnchors.has(raw) && (raw === activeWorkspace || raw.startsWith(activeWorkspace + '/'))) {
      const canonical = `#${activeWorkspace}/${activeGroup}`;
      if (location.hash !== canonical) history.replaceState(null, '', canonical);
    }
    groupedItems.forEach(item => {
      item.hidden = query
        ? !item.querySelector('.xw-module:not([hidden])')
        : item.dataset.workspaceGroup !== activeGroup || !groupReadable(item);
    });
    const nav = document.getElementById('xwWorkspaceGroups');
    if (!nav) return;
    nav.hidden = Boolean(query) || !groups.length || !access;
    const links = groups.filter(([key]) => key === activeGroup || groupCount(activeWorkspace, key) > 0).map(([key, label]) => {
      const link = document.createElement('a');
      link.href = `#${activeWorkspace}/${key}`;
      link.setAttribute('aria-controls', groupedItems.filter(item => item.dataset.workspaceGroup === key && item.closest('[data-workspace]')?.dataset.workspace === activeWorkspace).map(item => item.id).filter(Boolean).join(' '));
      if (key === activeGroup) link.setAttribute('aria-current', 'page');
      link.append(document.createTextNode(label));
      const count = document.createElement('small');count.textContent = `${groupCount(activeWorkspace, key)} 项`;link.append(count);
      return link;
    });
    const signature = `${activeWorkspace}:` + links.map(link => link.textContent).join('|');
    // Preserve keyboard focus while typing in search or rechecking the same access snapshot.
    const focusedHref = nav.contains(document.activeElement) ? document.activeElement.getAttribute('href') : null;
    if (nav.dataset.signature !== signature) {
      nav.replaceChildren(...links);nav.dataset.signature = signature;
      if (focusedHref) [...nav.querySelectorAll('a')].find(link => link.getAttribute('href') === focusedHref)?.focus({preventScroll:true});
    }
    nav.querySelectorAll('a').forEach(link => {
      if (link.getAttribute('href') === `#${activeWorkspace}/${activeGroup}`) link.setAttribute('aria-current', 'page');
      else link.removeAttribute('aria-current');
    });
  }

  function showWorkspace(query, matches) {
    showGroups(query);
    panels.forEach((panel) => {
      panel.hidden = query
        ? !panel.querySelector('.xw-module:not([hidden])')
        : panel.dataset.workspace !== activeWorkspace;
    });
    navItems.forEach((item) => {
      const current = !query && workspaceFor(item.getAttribute('href')) === activeWorkspace;
      item.classList.toggle('active', current);
      if (current) item.setAttribute('aria-current', 'page');
      else item.removeAttribute('aria-current');
    });
    document.querySelectorAll('[data-workspace-heading],[data-workspace-description]').forEach(node => {
      const workspace = node.closest('[data-workspace]')?.dataset.workspace;
      const group = workspaceGroups[workspace]?.find(([key]) => key === activeGroup);
      if (node.hasAttribute('data-workspace-heading')) node.textContent = query ? workspaceNames[workspace] : group?.[1] || workspaceNames[workspace];
      else node.textContent = query ? '按当前权限展示此工作区中的匹配模块。' : group?.[2] || '';
    });
    const title = query ? '模块搜索' : workspaceNames[activeWorkspace];
    const heading = document.getElementById('xwWorkspaceTitle');
    if (heading) heading.textContent = title;
    document.title = `${title} · 星维管理后台`;
    const clear = document.getElementById('xwSearchClear');
    if (clear) clear.hidden = !query;
    document.querySelectorAll('[data-workspace-count]').forEach((count) => {
      const section = document.getElementById(count.dataset.workspaceCount);
      const available = section?.querySelectorAll('.xw-module:not([hidden])').length || 0;
      count.textContent = !access ? '正在核对权限' : `可用 ${available} 个模块 →`;
    });
    const empty = document.getElementById('xwWorkspaceEmpty');
    if (empty) {
      const section = document.getElementById(activeWorkspace);
      const available = activeGroup ? groupCount(activeWorkspace, activeGroup) : section?.querySelectorAll('.xw-module:not([hidden])').length || 0;
      empty.hidden = !access || (query ? matches > 0 : activeWorkspace === 'overview' || available > 0);
      empty.textContent = query ? '没有匹配的可用功能或字段，请更换关键词或清除搜索。' : '当前分类没有可用模块或资料，可选择其他分类或联系管理员核对权限。';
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
      case 'LEGACY_OPERATOR_COMPAT': return '旧版运营员工兼容 · 只读 · 全部数据';
      case 'SHADOW_RBAC': return `角色权限已生效 · ${scopeLabel(snapshot)}`;
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
      if (!response.ok) throw new Error(`请求失败（状态码 ${response.status}）`);
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
    setBackendStatus('offline', '网络已断开 · 等待恢复后复核', '浏览器当前离线；网络恢复后会自动重试。');
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
      if (!response.ok) throw new Error(`请求失败（状态码 ${response.status}）`);
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
        setBackendStatus('match', '服务正常 · 已核对', '服务连接正常。');
      } else {
        setBackendStatus('mismatch', '页面需要更新 · 请刷新', '页面内容与服务状态不一致，请刷新后继续操作。');
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
        setBackendStatus('stale', '复核暂时失败 · 稍后重试', '上一次服务连接正常，本次检查暂未成功，稍后自动重试。');
        if (serverTime) serverTime.textContent = `上次核对 ${snapshot.runtimeVersion} · 当前等待自动复核`;
      } else {
        setBackendStatus('error', error?.name === 'AbortError' ? '服务检查超时' : '服务检查失败', '暂时无法检查服务连接，请稍后重试。');
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
