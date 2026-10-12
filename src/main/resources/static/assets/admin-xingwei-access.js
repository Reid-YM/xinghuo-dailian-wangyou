(() => {
  'use strict';

  let access = null;

  function allowed(permission) {
    if (!permission || !access) return false;
    if (access.allowAll) return true;
    return Array.isArray(access.permissionCodes) && access.permissionCodes.includes(permission);
  }

  function applyPermissions(root = document) {
    if (!access) return;
    root.querySelectorAll?.('[data-permission]').forEach((node) => {
      const permission = node.dataset.permission;
      const visible = allowed(permission);
      node.hidden = !visible;
      node.setAttribute('aria-hidden', visible ? 'false' : 'true');
    });
  }

  function modeLabel(snapshot) {
    switch (snapshot?.mode) {
      case 'LEGACY_ADMIN_OVERRIDE': return '管理员安全兜底 · 全权限';
      case 'LEGACY_OPERATOR_COMPAT': return 'OPERATOR 兼容模式 · 查看权限';
      case 'SHADOW_RBAC': return `影子 RBAC 已生效 · ${(snapshot.roleCodes || []).length} 个角色`;
      default: return '权限状态不可用';
    }
  }

  function setMode(snapshot, error = '') {
    const node = document.getElementById('xwAccessMode');
    if (!node) return;
    node.textContent = error || modeLabel(snapshot);
    node.dataset.mode = snapshot?.mode || 'DENIED';
    node.title = snapshot?.mode === 'SHADOW_RBAC'
      ? `角色：${(snapshot.roleCodes || []).join('、') || '无'}\n权限点：${(snapshot.permissionCodes || []).length}`
      : '旧后台仍沿用原 ADMIN / OPERATOR 安全边界；本状态仅控制新版星维功能。';
  }

  async function loadAccess() {
    try {
      const response = await fetch('/api/admin/xingwei/access', {
        credentials: 'same-origin',
        headers: {'Accept': 'application/json'}
      });
      if (!response.ok) throw new Error(`HTTP ${response.status}`);
      access = await response.json();
      window.xwAccess = access;
      window.xwHasPermission = allowed;
      setMode(access);
      applyPermissions(document);
      document.dispatchEvent(new CustomEvent('xw:access-ready', {detail: access}));
    } catch (error) {
      access = {mode: 'DENIED', allowAll: false, permissionCodes: [], roleCodes: []};
      window.xwAccess = access;
      window.xwHasPermission = allowed;
      setMode(access, '新版权限读取失败 · 已隐藏受控操作');
      applyPermissions(document);
      document.dispatchEvent(new CustomEvent('xw:access-ready', {detail: access}));
      console.warn('Xingwei access snapshot failed', error);
    }
  }

  const observer = new MutationObserver((mutations) => {
    if (!access) return;
    for (const mutation of mutations) {
      for (const node of mutation.addedNodes) {
        if (node.nodeType === Node.ELEMENT_NODE) {
          if (node.matches?.('[data-permission]')) {
            node.hidden = !allowed(node.dataset.permission);
            node.setAttribute('aria-hidden', node.hidden ? 'true' : 'false');
          }
          applyPermissions(node);
        }
      }
    }
  });

  const target = document.querySelector('.xw-main') || document.body;
  observer.observe(target, {childList: true, subtree: true});
  loadAccess();
})();
