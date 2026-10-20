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
  let canManage = false;
  let edit = null;
  let confirmation = null;
  let editGeneration = 0;
  const byId = (id) => document.getElementById(id);

  function actionButton(parent, label, action) {
    const button = append(parent, 'button', label, 'xw-button xw-button-ghost');
    button.type = 'button';
    button.addEventListener('click', action);
  }

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
    return '状态待同步';
  }

  function searchText(employee) {
    return normalize([employee.name, employee.position, ...(employee.departments || [])].join(' '));
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
      emptyRow(employeeRows, canManage ? 5 : 4, snapshot?.configured ? '没有匹配的员工' : '企业微信读取配置尚未完成');
      return;
    }
    rows.forEach((employee) => {
      const tr = document.createElement('tr');
      const employeeCell = document.createElement('td');
      append(employeeCell, 'strong', employee.name || '未命名');
      tr.appendChild(employeeCell);
      tr.appendChild(cell(employee.position || '未设置'));
      tr.appendChild(cell((employee.departments || []).join('、') || '未设置'));
      tr.appendChild(cell(statusLabel(employee.status)));
      if (canManage) {
        const actions = document.createElement('td');
        actionButton(actions, '调整岗位', () => openEditor('TRANSFER_MEMBER', employee));
        if (employee.status !== 2) actionButton(actions, '停用', () => openEditor('DISABLE_MEMBER', employee));
        tr.appendChild(actions);
      }
      employeeRows.appendChild(tr);
    });
  }

  function renderDepartments() {
    departmentRows.replaceChildren();
    const rows = snapshot?.departments || [];
    if (!rows.length) {
      emptyRow(departmentRows, canManage ? 3 : 2, snapshot?.configured ? '企业微信没有返回部门' : '尚未配置');
      return;
    }
    rows.forEach((department) => {
      const tr = document.createElement('tr');
      tr.appendChild(cell(department.name));
      tr.appendChild(cell(snapshot.departments.find(parent => parent.id === department.parentId)?.name || (Number(department.parentId) === 0 ? '根部门' : '未设置')));
      if (canManage) {
        const actions = document.createElement('td');
        if (Number(department.id) !== 1) actionButton(actions, '改名', () => openEditor('RENAME_DEPARTMENT', department));
        tr.appendChild(actions);
      }
      departmentRows.appendChild(tr);
    });
  }

  function renderPositions() {
    positionRows.replaceChildren();
    const rows = snapshot?.positions || [];
    if (!rows.length) {
      emptyRow(positionRows, 2, '正式岗位为空');
      return;
    }
    rows.forEach((position) => {
      const tr = document.createElement('tr');
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
    canManage = false;
    ['xwcCreateMember','xwcCreateDepartment','xwcEmployeeActionsHead','xwcDepartmentActionsHead'].forEach(id => { byId(id).hidden = true; });
    setMessage('正在读取企业微信实时通讯录……');
    refresh.disabled = true;
    try {
      const response = await fetch('/api/admin/xingwei/wecom/directory', {
        credentials: 'same-origin',
        headers: {'Accept': 'application/json'}
      });
      let body = null;
      try { body = await response.json(); } catch (_) { body = null; }
      if (!response.ok) throw new Error(body?.error || body?.message || `请求失败（状态码 ${response.status}）`);
      snapshot = body;
      // Management capability must be checked independently; directory access remains read-only.
      canManage = false;
      try {
        const capability = await fetch('/api/admin/xingwei/wecom/hr/capabilities', {credentials:'same-origin', cache:'no-store', headers:{'Accept':'application/json'}});
        const state = capability.ok ? await capability.json() : null;
        canManage = snapshot.configured === true && state?.canManage === true && state?.enabled === true;
      } catch (_) { /* fail closed */ }
      ['xwcCreateMember','xwcCreateDepartment','xwcEmployeeActionsHead','xwcDepartmentActionsHead'].forEach(id => { byId(id).hidden = !canManage; });
      render();
      status.classList.remove('error');
      status.classList.add('ok');
      status.querySelector('span').textContent = snapshot.configured ? '企业微信已连接' : '只读配置待完成';
      setMessage(snapshot.configured
        ? `已读取 ${snapshot.counts?.employees ?? 0} 名员工、${snapshot.counts?.departments ?? 0} 个部门；正式岗位 ${snapshot.counts?.positions ?? 0} 个。`
        : '企业微信通讯录尚未连接，仍可查看正式岗位资料。', snapshot.configured ? 'ok' : '');
    } catch (error) {
      status.classList.remove('ok');
      status.classList.add('error');
      status.querySelector('span').textContent = '读取失败';
      setMessage('企业微信通讯录暂时无法读取，请确认员工查看权限或稍后刷新。', 'error');
    } finally {
      refresh.disabled = false;
    }
  }

  const labels = {CREATE_MEMBER:'新增员工',TRANSFER_MEMBER:'调整岗位',DISABLE_MEMBER:'停用员工',CREATE_DEPARTMENT:'新增部门',RENAME_DEPARTMENT:'部门改名'};

  function positions() {
    const departmentName = snapshot.departments.find(item => String(item.id) === byId('xwcHrDepartment').value)?.name;
    byId('xwcHrPosition').replaceChildren();
    for (const position of snapshot.positions || []) {
      if (position.enabled !== true || position.departmentName !== departmentName) continue;
      const option = document.createElement('option'); option.value = position.code; option.textContent = position.name;
      byId('xwcHrPosition').appendChild(option);
    }
  }

  function openEditor(action, target = {}) {
    if (!canManage) return;
    editGeneration += 1;edit = {action, target}; confirmation = null;
    byId('xwcHrForm').reset();
    byId('xwcHrConfirm').required = false;
    byId('xwcHrTitle').textContent = labels[action];
    byId('xwcHrMessage').textContent = '';
    byId('xwcHrNotice').textContent = action === 'DISABLE_MEMBER'
      ? `停用 ${target.name || '该员工'} 将先撤销其后台访问，再向企业微信提交停用；外部结果不明确时后台访问仍保持停用。`
      : action === 'TRANSFER_MEMBER' ? '调岗将用选定部门替换现有部门，其他部门归属会移除；请核对后确认。'
      : action === 'RENAME_DEPARTMENT' ? '修改企业微信部门名称；正式岗位所属中心仍按岗位目录校验。'
      : '本操作会修改企业微信通讯录；先预览，再输入名称确认。';
    byId('xwcHrFields').hidden = false;
    byId('xwcHrPreview').hidden = true; byId('xwcHrConfirmField').hidden = true;
    byId('xwcHrNameField').hidden = !['CREATE_MEMBER','CREATE_DEPARTMENT','RENAME_DEPARTMENT'].includes(action);
    byId('xwcHrName').value = action === 'RENAME_DEPARTMENT' ? target.name || '' : '';
    byId('xwcHrName').required = !byId('xwcHrNameField').hidden;
    byId('xwcHrDepartmentField').hidden = ['DISABLE_MEMBER','RENAME_DEPARTMENT'].includes(action);
    byId('xwcHrPositionField').hidden = !['CREATE_MEMBER','TRANSFER_MEMBER'].includes(action);
    byId('xwcHrMobileField').hidden = action !== 'CREATE_MEMBER';
    byId('xwcHrEmailField').hidden = action !== 'CREATE_MEMBER';
    byId('xwcHrDepartmentLabel').textContent = action === 'CREATE_DEPARTMENT' ? '上级部门' : '部门';
    byId('xwcHrDepartment').replaceChildren();
    for (const department of snapshot.departments || []) {
      const option = document.createElement('option');option.value = department.id;option.textContent = department.name;
      byId('xwcHrDepartment').appendChild(option);
    }
    if (target.departmentIds?.length) byId('xwcHrDepartment').value = target.departmentIds[0];
    positions();
    const position = (snapshot.positions || []).find(item => item.name === target.position && item.enabled === true
      && item.departmentName === snapshot.departments.find(dep => String(dep.id) === byId('xwcHrDepartment').value)?.name);
    if (position) byId('xwcHrPosition').value = position.code;
    byId('xwcHrSubmit').disabled = false; byId('xwcHrSubmit').textContent = '预览变更';
    byId('xwcHrDialog').showModal();
  }

  async function write(path, body) {
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;
    const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
    if (!csrfHeader || !csrfToken) throw new Error('登录状态需要刷新，请重新进入通讯录');
    const response = await fetch(`/api/admin/xingwei/wecom/hr/${path}`, {
      method:'POST',credentials:'same-origin',cache:'no-store',
      headers:{'Accept':'application/json','Content-Type':'application/json',[csrfHeader]:csrfToken},body:JSON.stringify(body)
    });
    let data = null; try { data = await response.json(); } catch (_) { /* generic message below */ }
    if (!response.ok) {
      // Only messages from this dedicated controller's known response contract are displayed.
      const error = new Error(response.status === 401 || response.status === 403
        ? '登录或管理权限已变化，请重新进入通讯录'
        : response.status >= 500 && path === 'commit' ? '操作结果待核实，请先刷新通讯录或到企业微信管理端核对，勿重复提交'
        : '资料或变更预览已变化，请关闭后刷新并重新核对');
      error.submitted = path === 'commit'; throw error;
    }
    return data;
  }

  byId('xwcHrForm').addEventListener('submit', async (event) => {
    event.preventDefault();
    if (confirmation && byId('xwcHrConfirm').value.trim() !== confirmation.name) {
      byId('xwcHrMessage').textContent = '请输入预览中的完整名称确认。';return;
    }
    const generation = editGeneration;
    const submit = byId('xwcHrSubmit'); submit.disabled = true;
    const isCommit = confirmation !== null;
    try {
      if (!isCommit) {
        const data = await write('preview', {action:edit.action,userId:edit.target.userId || null,
          name:byId('xwcHrName').value,mobile:byId('xwcHrMobile').value,email:byId('xwcHrEmail').value,
          departmentId:edit.action === 'RENAME_DEPARTMENT' ? edit.target.id : Number(byId('xwcHrDepartment').value),
          positionCode:byId('xwcHrPosition').value});
        if (generation !== editGeneration) return;
        confirmation = data;
        byId('xwcHrFields').hidden = true;
        byId('xwcHrPreview').textContent = [data.actionLabel,data.name,data.departmentName,data.positionName].filter(Boolean).join(' · ');
        byId('xwcHrPreview').hidden = false;byId('xwcHrConfirmField').hidden = false;
        byId('xwcHrConfirm').required = true;byId('xwcHrConfirm').focus();
        byId('xwcHrMessage').textContent = data.localAccessWillBeRevoked ? '后台访问会先行停用；外部结果待核实时也不会自动恢复。' : '请核对名称、部门和岗位，输入上方名称后确认。';
        submit.textContent = '确认执行'; submit.disabled = false;
      } else {
        await write('commit', {confirmationToken:confirmation.confirmationToken,confirmName:byId('xwcHrConfirm').value});
        if (generation !== editGeneration) { await load();return; }
        byId('xwcHrDialog').close();confirmation = null; await load();
        setMessage('通讯录变更已回读核对完成。', 'ok');
      }
    } catch (error) {
      if (generation !== editGeneration) return;
      byId('xwcHrMessage').textContent = isCommit && edit.action === 'DISABLE_MEMBER'
        ? '后台访问可能已停用，企业微信结果需核实；请关闭后刷新，勿重复提交。'
        : error.message;
      // A commit may have reached the vendor. Never offer an automatic second submission.
      submit.disabled = isCommit;
      if (isCommit) confirmation = null;
    }
  });
  byId('xwcHrDepartment').addEventListener('change', positions);
  byId('xwcCreateMember').addEventListener('click', () => openEditor('CREATE_MEMBER'));
  byId('xwcCreateDepartment').addEventListener('click', () => openEditor('CREATE_DEPARTMENT'));
  byId('xwcHrClose').addEventListener('click', () => byId('xwcHrDialog').close());
  byId('xwcHrDialog').addEventListener('close', () => { editGeneration += 1;confirmation = null; });

  search.addEventListener('input', renderEmployees);
  refresh.addEventListener('click', load);
  load();
})();
