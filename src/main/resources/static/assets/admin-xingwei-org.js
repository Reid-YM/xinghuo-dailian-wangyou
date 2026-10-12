(() => {
  'use strict';

  const csrfToken = document.querySelector('meta[name="_csrf"]')?.content || '';
  const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content || 'X-CSRF-TOKEN';
  let orgSnapshot = null;

  function escapeHtml(value) {
    return String(value ?? '')
      .replaceAll('&', '&amp;')
      .replaceAll('<', '&lt;')
      .replaceAll('>', '&gt;')
      .replaceAll('"', '&quot;')
      .replaceAll("'", '&#39;');
  }

  function hasPermission(permission) {
    return typeof window.xwHasPermission === 'function' && window.xwHasPermission(permission);
  }

  function dataScopeLabel(scope, departmentCode = '') {
    if (scope === 'ALL') return '全部数据';
    if (scope === 'DEPARTMENT') return departmentCode ? `本中心 · ${departmentCode}` : '本中心 · 未分配中心时仅本人';
    if (scope === 'SELF') return '仅本人';
    return '无数据范围';
  }

  async function apiJson(url, options = {}) {
    const method = String(options.method || 'GET').toUpperCase();
    const headers = {
      'Accept': 'application/json',
      ...(options.body ? {'Content-Type': 'application/json'} : {}),
      ...(options.headers || {})
    };
    if (method !== 'GET' && method !== 'HEAD' && csrfToken) headers[csrfHeader] = csrfToken;
    const response = await fetch(url, {credentials: 'same-origin', ...options, method, headers});
    let data = null;
    try { data = await response.json(); } catch (_) { data = null; }
    if (!response.ok) throw new Error(data?.error || data?.message || `HTTP ${response.status}`);
    return data;
  }

  function setOrgMessage(message, kind = '') {
    const node = document.getElementById('xwOrgMessage');
    if (!node) return;
    node.textContent = message;
    node.classList.remove('ok', 'error');
    if (kind) node.classList.add(kind);
  }

  function renderKpis() {
    const counts = orgSnapshot?.counts || {};
    document.querySelectorAll('[data-org-kpi]').forEach((node) => {
      node.textContent = String(counts[node.dataset.orgKpi] ?? '-');
    });
  }

  function renderDepartments() {
    const body = document.getElementById('xwDepartmentRows');
    const filter = document.getElementById('xwPositionDepartment');
    if (!body || !orgSnapshot) return;
    const departments = orgSnapshot.departments || [];
    body.innerHTML = departments.map((d) => `
      <tr>
        <td><span class="xw-code">${escapeHtml(d.code)}</span></td>
        <td><strong>${escapeHtml(d.name)}</strong></td>
        <td>${escapeHtml(d.positionCount)}</td>
        <td><span class="xw-pill ${d.enabled ? 'ok' : ''}">${d.enabled ? '启用' : '停用'}</span></td>
      </tr>`).join('');
    if (filter) {
      const selected = filter.value;
      filter.innerHTML = '<option value="">全部中心</option>' + departments
        .map((d) => `<option value="${escapeHtml(d.code)}">${escapeHtml(d.name)}</option>`).join('');
      filter.value = selected;
    }
  }

  function renderPositions() {
    const body = document.getElementById('xwPositionRows');
    const filter = document.getElementById('xwPositionDepartment');
    if (!body || !orgSnapshot) return;
    const departmentCode = filter?.value || '';
    const rows = (orgSnapshot.positions || []).filter((p) => !departmentCode || p.departmentCode === departmentCode);
    body.innerHTML = rows.map((p) => `
      <tr>
        <td><span class="xw-code">${escapeHtml(p.code)}</span></td>
        <td><strong>${escapeHtml(p.name)}</strong></td>
        <td>${escapeHtml(p.category)}</td>
        <td>${escapeHtml(p.departmentName)}</td>
      </tr>`).join('');
  }

  function roleLabel(code) {
    return (orgSnapshot?.roles || []).find((role) => role.code === code)?.name || code;
  }

  function renderEmployees() {
    const body = document.getElementById('xwEmployeeRows');
    if (!body || !orgSnapshot) return;
    body.innerHTML = (orgSnapshot.employees || []).map((employee) => {
      const roles = (employee.roleCodes || []).map(roleLabel).join('、') || '未配置';
      const name = employee.displayName || '未归档';
      const number = employee.employeeNo ? `<small class="xw-block-code">${escapeHtml(employee.employeeNo)}</small>` : '';
      return `
        <tr>
          <td><strong>${escapeHtml(employee.username)}</strong></td>
          <td>${escapeHtml(name)}${number}</td>
          <td>${escapeHtml(employee.departmentName || '未分配')}</td>
          <td>${escapeHtml(employee.positionName || '未分配')}</td>
          <td>${escapeHtml(roles)}</td>
          <td><span class="xw-pill">${escapeHtml(employee.legacyRole)}</span></td>
          <td><button class="xw-mini-button" type="button" data-employee-edit="${employee.id}">配置</button></td>
        </tr>`;
    }).join('');
  }

  function renderRoles() {
    const body = document.getElementById('xwRoleRows');
    if (!body || !orgSnapshot) return;
    body.innerHTML = (orgSnapshot.roles || []).map((role) => `
      <tr>
        <td><strong>${escapeHtml(role.name)}</strong><small class="xw-block-code">${escapeHtml(role.code)}</small></td>
        <td>${escapeHtml(dataScopeLabel(role.dataScope))}</td>
        <td>${escapeHtml(role.permissionCount)} / ${escapeHtml((orgSnapshot.permissions || []).length)}</td>
        <td><span class="xw-pill ${role.enabled ? 'ok' : ''}">${role.enabled ? '启用' : '停用'}</span></td>
        <td>${role.code === 'XW_MANAGEMENT'
          ? '<span class="xw-muted-label">安全兜底 · 只读</span>'
          : `<button class="xw-mini-button" type="button" data-role-edit="${escapeHtml(role.code)}">配置权限</button>`}
        </td>
      </tr>`).join('');
  }

  function renderOrg() {
    renderKpis();
    renderDepartments();
    renderPositions();
    renderEmployees();
    renderRoles();
  }

  async function loadOrg() {
    if (!document.getElementById('org-console')) return;
    try {
      setOrgMessage('正在读取组织数据……');
      orgSnapshot = await apiJson('/api/admin/xingwei/org/snapshot');
      renderOrg();
      const scope = dataScopeLabel(orgSnapshot.dataScope, orgSnapshot.scopeDepartmentCode);
      setOrgMessage(`组织数据已连接 · ${orgSnapshot.counts?.departments ?? 0} 个中心 / ${orgSnapshot.counts?.positions ?? 0} 个岗位 · V2C 数据范围：${scope}`, 'ok');
    } catch (error) {
      setOrgMessage(`组织数据读取失败或无查看权限：${error.message}`, 'error');
      console.warn('Xingwei org snapshot failed', error);
    }
  }

  document.getElementById('xwPositionDepartment')?.addEventListener('change', renderPositions);

  function fillEmployeePositionOptions(departmentCode, selectedCode = '') {
    const select = document.getElementById('xwEmployeePosition');
    if (!select || !orgSnapshot) return;
    const rows = (orgSnapshot.positions || []).filter((p) => !departmentCode || p.departmentCode === departmentCode);
    select.innerHTML = '<option value="">未分配岗位</option>' + rows
      .map((p) => `<option value="${escapeHtml(p.code)}">${escapeHtml(p.code)} · ${escapeHtml(p.name)}</option>`).join('');
    select.value = selectedCode && rows.some((p) => p.code === selectedCode) ? selectedCode : '';
  }

  function setEmployeeDialogMode(canProfile, canRoles) {
    ['xwEmployeeName', 'xwEmployeeNo', 'xwEmployeeDepartment', 'xwEmployeePosition', 'xwEmployeeStatus', 'xwEmployeeNotes']
      .forEach((id) => {
        const field = document.getElementById(id);
        if (field) field.disabled = !canProfile;
      });
    const roleFieldset = document.getElementById('xwEmployeeRoleChecks')?.closest('fieldset');
    if (roleFieldset) roleFieldset.hidden = !canRoles;
    const form = document.getElementById('xwEmployeeForm');
    if (form) {
      form.dataset.canProfile = String(canProfile);
      form.dataset.canRoles = String(canRoles);
    }
  }

  function availableEmployeeDepartments() {
    const departments = orgSnapshot?.departments || [];
    if (orgSnapshot?.dataScope === 'DEPARTMENT' && orgSnapshot.scopeDepartmentCode) {
      return departments.filter((department) => department.code === orgSnapshot.scopeDepartmentCode);
    }
    return departments;
  }

  function openEmployeeDialog(employeeId) {
    if (!orgSnapshot) return;
    const canProfile = hasPermission('employee:manage');
    const canRoles = hasPermission('rbac:manage');
    if (!canProfile && !canRoles) return;
    const employee = (orgSnapshot.employees || []).find((row) => Number(row.id) === Number(employeeId));
    const dialog = document.getElementById('xwEmployeeDialog');
    if (!employee || !dialog) return;

    document.getElementById('xwEmployeeId').value = employee.id;
    document.getElementById('xwEmployeeDialogTitle').textContent = `配置员工 · ${employee.username}`;
    document.getElementById('xwEmployeeName').value = employee.displayName || '';
    document.getElementById('xwEmployeeNo').value = employee.employeeNo || '';
    document.getElementById('xwEmployeeStatus').value = employee.status || 'ACTIVE';
    document.getElementById('xwEmployeeNotes').value = employee.notes || '';

    const department = document.getElementById('xwEmployeeDepartment');
    const allowUnassigned = orgSnapshot.dataScope !== 'DEPARTMENT';
    department.innerHTML = (allowUnassigned ? '<option value="">未分配中心</option>' : '') + availableEmployeeDepartments()
      .map((d) => `<option value="${escapeHtml(d.code)}">${escapeHtml(d.name)}</option>`).join('');
    department.value = employee.departmentCode || '';
    if (!department.value && orgSnapshot.dataScope === 'DEPARTMENT' && orgSnapshot.scopeDepartmentCode) {
      department.value = orgSnapshot.scopeDepartmentCode;
    }
    fillEmployeePositionOptions(department.value, employee.positionCode || '');

    const selectedRoles = new Set(employee.roleCodes || []);
    document.getElementById('xwEmployeeRoleChecks').innerHTML = (orgSnapshot.roles || []).map((role) => `
      <label class="xw-check-item">
        <input type="checkbox" value="${escapeHtml(role.code)}" ${selectedRoles.has(role.code) ? 'checked' : ''}/>
        <span><strong>${escapeHtml(role.name)}</strong><small>${escapeHtml(dataScopeLabel(role.dataScope))}</small></span>
      </label>`).join('');

    setEmployeeDialogMode(canProfile, canRoles);
    document.getElementById('xwEmployeeSaveState').textContent = '';
    dialog.showModal();
  }

  document.getElementById('xwEmployeeDepartment')?.addEventListener('change', (event) => {
    fillEmployeePositionOptions(event.target.value);
  });

  function openRoleDialog(roleCode) {
    if (!orgSnapshot || roleCode === 'XW_MANAGEMENT' || !hasPermission('rbac:manage')) return;
    const role = (orgSnapshot.roles || []).find((row) => row.code === roleCode);
    const dialog = document.getElementById('xwRoleDialog');
    if (!role || !dialog) return;
    document.getElementById('xwRoleCode').value = role.code;
    document.getElementById('xwRoleDialogTitle').textContent = `配置权限 · ${role.name}`;
    document.getElementById('xwRoleSaveState').textContent = '';
    const selected = new Set(role.permissionCodes || []);
    const grouped = new Map();
    (orgSnapshot.permissions || []).forEach((permission) => {
      if (!grouped.has(permission.module)) grouped.set(permission.module, []);
      grouped.get(permission.module).push(permission);
    });
    document.getElementById('xwPermissionChecks').innerHTML = Array.from(grouped.entries()).map(([moduleName, rows]) => `
      <fieldset><legend>${escapeHtml(moduleName)}</legend>${rows.map((permission) => `
        <label class="xw-check-item">
          <input type="checkbox" value="${escapeHtml(permission.code)}" ${selected.has(permission.code) ? 'checked' : ''}/>
          <span><strong>${escapeHtml(permission.name)}</strong><small>${escapeHtml(permission.code)}</small></span>
        </label>`).join('')}</fieldset>`).join('');
    dialog.showModal();
  }

  document.addEventListener('click', (event) => {
    const employeeButton = event.target.closest('[data-employee-edit]');
    if (employeeButton) openEmployeeDialog(employeeButton.dataset.employeeEdit);
    const roleButton = event.target.closest('[data-role-edit]');
    if (roleButton) openRoleDialog(roleButton.dataset.roleEdit);
    const closeButton = event.target.closest('[data-dialog-close]');
    if (closeButton) document.getElementById(closeButton.dataset.dialogClose)?.close();
  });

  document.getElementById('xwEmployeeForm')?.addEventListener('submit', async (event) => {
    event.preventDefault();
    if (!orgSnapshot) return;
    const form = event.currentTarget;
    const canProfile = form.dataset.canProfile === 'true';
    const canRoles = form.dataset.canRoles === 'true';
    const id = Number(document.getElementById('xwEmployeeId').value);
    const state = document.getElementById('xwEmployeeSaveState');
    const roleCodes = Array.from(document.querySelectorAll('#xwEmployeeRoleChecks input:checked')).map((node) => node.value);
    state.textContent = '正在保存……';
    try {
      if (canProfile) {
        await apiJson(`/api/admin/xingwei/org/employees/${id}/profile`, {
          method: 'PUT',
          body: JSON.stringify({
            employeeNo: document.getElementById('xwEmployeeNo').value,
            displayName: document.getElementById('xwEmployeeName').value,
            departmentCode: document.getElementById('xwEmployeeDepartment').value,
            positionCode: document.getElementById('xwEmployeePosition').value,
            status: document.getElementById('xwEmployeeStatus').value,
            notes: document.getElementById('xwEmployeeNotes').value
          })
        });
      }
      if (canRoles) {
        await apiJson(`/api/admin/xingwei/org/employees/${id}/roles`, {
          method: 'PUT',
          body: JSON.stringify({roleCodes})
        });
      }
      if (!canProfile && !canRoles) throw new Error('当前账号没有可保存的权限');
      await loadOrg();
      document.getElementById('xwEmployeeDialog').close();
    } catch (error) {
      state.textContent = `保存失败：${error.message}`;
    }
  });

  document.getElementById('xwRoleForm')?.addEventListener('submit', async (event) => {
    event.preventDefault();
    if (!orgSnapshot || !hasPermission('rbac:manage')) return;
    const roleCode = document.getElementById('xwRoleCode').value;
    const state = document.getElementById('xwRoleSaveState');
    const permissionCodes = Array.from(document.querySelectorAll('#xwPermissionChecks input:checked')).map((node) => node.value);
    state.textContent = '正在保存……';
    try {
      await apiJson(`/api/admin/xingwei/org/roles/${encodeURIComponent(roleCode)}/permissions`, {
        method: 'PUT',
        body: JSON.stringify({permissionCodes})
      });
      await loadOrg();
      document.getElementById('xwRoleDialog').close();
    } catch (error) {
      state.textContent = `保存失败：${error.message}`;
    }
  });

  document.addEventListener('xw:access-ready', () => {
    if (orgSnapshot) renderOrg();
  });

  loadOrg();
})();
