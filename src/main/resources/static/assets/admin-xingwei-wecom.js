(() => {
  'use strict';

  const status = document.getElementById('xwcStatus');
  const message = document.getElementById('xwcMessage');
  const search = document.getElementById('xwcSearch');
  const refresh = document.getElementById('xwcRefresh');
  const employeeRows = document.getElementById('xwcEmployeeRows');
  const departmentRows = document.getElementById('xwcDepartmentRows');
  const positionRows = document.getElementById('xwcPositionRows');
  let snapshot = null;

  function normalize(value) {
    return String(value ?? '').toLocaleLowerCase('zh-CN').trim();
  }

  function append(parent, tag, text, className = '') {
    const node = document.createElement(tag);
    if (className) node.className = className;
    node.textContent = text == null || text === '' ? '—' : String(text);
    parent.appendChild(node);
    return node;
  }

  function cell(text, className = '') {
    const td = document.createElement('td');
    if (className) td.className = className;
    td.textContent = text == null || text === '' ? '—' : String(text);
    return td;
  }

  function setMessage(text, kind = '') {
    message.textContent = text;
    message.classList.remove('ok', 'error');
    if (kind) message.classList.add(kind);
  }

  function statusLabel(value) {
    if (value === 1) return '已激活';
    if (value === 2) return '已禁用';
    if (value === 4) return '未激活';
    if (value === 5) return '退出企业';
    return `状态 ${value ?? '-'}`;
  }

  function searchText(employee) {
    return normalize([employee.name, employee.userId, employee.position, ...(employee.departments || [])].join(' '));
  }

  function emptyRow(body, colspan, text) {
    body.replaceChildren();
    const tr = document.createElement('tr');
    tr.className = 'xwc-empty';
    const td = document.createElement('td');
    td.colSpan = colspan;
    td.textContent = text;
    tr.appendChild(td);
    body.appendChild(tr);
  }

  function renderEmployees() {
    const query = normalize(search.value);
    const rows = (snapshot?.employees || []).filter((employee) => !query || searchText(employee).includes(query));
    employeeRows.replaceChildren();
    if (!rows.length) {
      emptyRow(employeeRows, 4, snapshot?.configured ? '没有匹配的员工' : '企业微信读取配置尚未完成');
      return;
    }
    rows.forEach((employee) => {
      const tr = document.createElement('tr');
      const employeeCell = document.createElement('td');
      append(employeeCell, 'strong', employee.name || '未命名');
      append(employeeCell, 'small', employee.userId, 'xw-block-code');
      tr.appendChild(employeeCell);
      tr.appendChild(cell(employee.position || '未设置'));
      tr.appendChild(cell((employee.departments || []).join('、') || '未设置'));
      tr.appendChild(cell(statusLabel(employee.status)));
      employeeRows.appendChild(tr);
    });
  }

  function renderDepartments() {
    departmentRows.replaceChildren();
    const rows = snapshot?.departments || [];
    if (!rows.length) {
      emptyRow(departmentRows, 3, snapshot?.configured ? '企业微信没有返回部门' : '尚未配置');
      return;
    }
    rows.forEach((department) => {
      const tr = document.createElement('tr');
      tr.appendChild(cell(department.id));
      tr.appendChild(cell(department.name));
      tr.appendChild(cell(department.parentId));
      departmentRows.appendChild(tr);
    });
  }

  function renderPositions() {
    positionRows.replaceChildren();
    const rows = snapshot?.positions || [];
    if (!rows.length) {
      emptyRow(positionRows, 3, 'V63 正式岗位为空');
      return;
    }
    rows.forEach((position) => {
      const tr = document.createElement('tr');
      tr.appendChild(cell(position.code, 'xw-code'));
      tr.appendChild(cell(position.name));
      tr.appendChild(cell(position.departmentName));
      positionRows.appendChild(tr);
    });
  }

  function render() {
    document.getElementById('xwcConfigured').textContent = snapshot?.configured ? '已配置 · 只读' : '未配置';
    document.getElementById('xwcEmployeeCount').textContent = String(snapshot?.counts?.employees ?? 0);
    document.getElementById('xwcDepartmentCount').textContent = String(snapshot?.counts?.departments ?? 0);
    document.getElementById('xwcPositionCount').textContent = String(snapshot?.counts?.positions ?? 0);
    renderEmployees();
    renderDepartments();
    renderPositions();
  }

  async function load() {
    setMessage('正在读取企业微信实时通讯录……');
    refresh.disabled = true;
    try {
      const response = await fetch('/api/admin/xingwei/wecom/directory', {
        credentials: 'same-origin',
        headers: {'Accept': 'application/json'}
      });
      let body = null;
      try { body = await response.json(); } catch (_) { body = null; }
      if (!response.ok) throw new Error(body?.error || body?.message || `HTTP ${response.status}`);
      snapshot = body;
      render();
      status.classList.remove('error');
      status.classList.add('ok');
      status.querySelector('span').textContent = snapshot.configured ? '企业微信已连接' : '只读配置待完成';
      setMessage(snapshot.configured
        ? `已读取 ${snapshot.counts?.employees ?? 0} 名员工、${snapshot.counts?.departments ?? 0} 个部门；正式岗位 ${snapshot.counts?.positions ?? 0} 个来自 V63。`
        : '页面已上线，但服务器尚未配置 WECOM_CORP_ID / WECOM_APP_SECRET；V63 正式岗位仍可正常读取。', snapshot.configured ? 'ok' : '');
    } catch (error) {
      status.classList.remove('ok');
      status.classList.add('error');
      status.querySelector('span').textContent = '读取失败';
      setMessage(`企业微信通讯录读取失败或当前账号没有 employee:view：${error.message}`, 'error');
    } finally {
      refresh.disabled = false;
    }
  }

  search.addEventListener('input', renderEmployees);
  refresh.addEventListener('click', load);
  load();
})();
