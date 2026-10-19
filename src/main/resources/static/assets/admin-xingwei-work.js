(() => {
  'use strict';

  const accessBadge = document.getElementById('xwwAccessBadge');
  const scopeText = document.getElementById('xwwScopeText');
  const message = document.getElementById('xwwMessage');
  const refreshButton = document.getElementById('xwwRefresh');
  const search = document.getElementById('xwwSearch');
  const rows = document.getElementById('xwwTaskRows');
  const noAccess = document.getElementById('xwwNoAccess');
  const panel = document.getElementById('xwwTaskPanel');
  const filterButtons = Array.from(document.querySelectorAll('[data-filter]'));
  const kpiButtons = Array.from(document.querySelectorAll('[data-kpi-filter]'));
  const kpiScope = document.getElementById('xwwKpiScope');
  const kpiMine = document.getElementById('xwwKpiMine');
  const kpiQueue = document.getElementById('xwwKpiQueue');
  const kpiUnassigned = document.getElementById('xwwKpiUnassigned');
  const dialog = document.getElementById('xwwEditDialog');
  const editForm = document.getElementById('xwwEditForm');
  const editTitle = document.getElementById('xwwEditTitle');
  const editContext = document.getElementById('xwwEditContext');
  const editStatus = document.getElementById('xwwEditStatus');
  const editPercent = document.getElementById('xwwEditPercent');
  const editProgress = document.getElementById('xwwEditProgress');
  const editCustomerNote = document.getElementById('xwwEditCustomerNote');
  const editReason = document.getElementById('xwwEditReason');
  const editState = document.getElementById('xwwEditState');
  const csrfToken = document.querySelector('meta[name="_csrf"]')?.content || '';
  const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content || 'X-CSRF-TOKEN';

  let snapshot = null;
  let activeFilter = 'ALL';
  let editing = null;

  function normalize(value) {
    return String(value ?? '').toLocaleLowerCase('zh-CN').trim();
  }

  function setMessage(text, state = '') {
    if (!message) return;
    message.textContent = text;
    message.classList.remove('ok', 'error');
    if (state) message.classList.add(state);
  }

  async function fetchJson(url, options = {}) {
    const headers = {'Accept': 'application/json', ...(options.headers || {})};
    const response = await fetch(url, {...options, credentials: 'same-origin', headers});
    if (!response.ok) {
      let detail = '';
      try {
        const body = await response.json();
        detail = body?.error || body?.message || '';
      } catch (_) {
        detail = '';
      }
      throw new Error(detail || `请求失败（状态码 ${response.status}）`);
    }
    return response.json();
  }

  function appendText(parent, tag, text, className = '') {
    const node = document.createElement(tag);
    if (className) node.className = className;
    node.textContent = text == null || text === '' ? '—' : String(text);
    parent.appendChild(node);
    return node;
  }

  function textCell(value, className = '') {
    const td = document.createElement('td');
    if (className) td.className = className;
    td.textContent = value == null || value === '' ? '—' : String(value);
    return td;
  }

  function formatTime(value) {
    if (!value) return '—';
    const date = new Date(value);
    return Number.isNaN(date.getTime()) ? String(value) : date.toLocaleString('zh-CN', {hour12: false});
  }

  function scopeLabel(data) {
    switch (data?.dataScope) {
      case 'ALL': return '全部范围';
      case 'DEPARTMENT': return data.departmentCode ? `本中心 · ${data.departmentCode}` : '本中心';
      case 'SELF': return '仅本人';
      default: return '无范围';
    }
  }

  function statusLabel(status) {
    switch (status) {
      case 'WAITING_CONFIRMATION': return '等待确认';
      case 'IN_PROGRESS': return '执行中';
      case 'COMPLETED': return '已完成';
      case 'EXCEPTION': return '异常';
      default: return status || '未知';
    }
  }

  function bucketLabel(bucket) {
    switch (bucket) {
      case 'MINE': return '我的任务';
      case 'DEPARTMENT_QUEUE': return '中心队列';
      case 'UNASSIGNED': return '未归属';
      default: return '未知';
    }
  }

  function bucketClass(bucket) {
    switch (bucket) {
      case 'MINE': return 'mine';
      case 'DEPARTMENT_QUEUE': return 'queue';
      case 'UNASSIGNED': return 'unassigned';
      default: return '';
    }
  }

  function searchText(item) {
    return normalize([
      item.businessNo, item.orderNo, item.game, item.demand, item.expectedTime,
      item.status, statusLabel(item.status), item.progressText, item.customerNote,
      bucketLabel(item.bucket), item.ownership?.departmentName
    ].join(' '));
  }

  function progressCell(item) {
    const td = document.createElement('td');
    td.className = 'xww-progress';
    appendText(td, 'span', item.progressPercent == null ? '未设置百分比' : `${item.progressPercent}%`, 'xww-primary');
    if (item.progressText) appendText(td, 'small', item.progressText, 'xww-secondary');
    if (item.progressPercent != null) {
      const bar = document.createElement('div');
      bar.className = 'xww-progress-bar';
      const fill = document.createElement('i');
      fill.style.width = `${Math.max(0, Math.min(100, Number(item.progressPercent) || 0))}%`;
      bar.appendChild(fill);
      td.appendChild(bar);
    }
    return td;
  }

  function ownershipCell(item) {
    const td = document.createElement('td');
    const badge = document.createElement('span');
    badge.className = `xww-bucket ${bucketClass(item.bucket)}`.trim();
    badge.textContent = bucketLabel(item.bucket);
    td.appendChild(badge);
    if (item.bucket === 'MINE') {
      appendText(td, 'small', item.ownership?.departmentName || item.ownership?.departmentCode || '本人', 'xww-secondary');
    } else if (item.bucket === 'DEPARTMENT_QUEUE') {
      appendText(td, 'small', item.ownership?.departmentName || item.ownership?.departmentCode || '中心待领取', 'xww-secondary');
    } else {
      appendText(td, 'small', '仅 ALL 范围可见', 'xww-secondary');
    }
    return td;
  }

  function actionCell(item) {
    const td = document.createElement('td');
    const box = document.createElement('div');
    box.className = 'xww-actions';
    if (item.canClaim) {
      const claim = document.createElement('button');
      claim.type = 'button';
      claim.className = 'xw-mini-button';
      claim.textContent = '领取任务';
      claim.addEventListener('click', () => claimTask(item, claim));
      box.appendChild(claim);
    }
    if (item.canUpdate) {
      const update = document.createElement('button');
      update.type = 'button';
      update.className = 'xw-mini-button';
      update.textContent = '更新履约';
      update.addEventListener('click', () => openEdit(item));
      box.appendChild(update);
    }
    if (!item.canClaim && !item.canUpdate) {
      appendText(box, 'span', '只读', 'xw-muted-label');
    }
    td.appendChild(box);
    return td;
  }

  function render(items) {
    rows.replaceChildren();
    if (!items.length) {
      const tr = document.createElement('tr');
      const td = document.createElement('td');
      td.colSpan = 7;
      td.className = 'xww-empty';
      const strong = document.createElement('strong');
      strong.textContent = '当前没有待办任务';
      const text = document.createElement('span');
      text.textContent = '可返回业务派单页查看订单归属，或稍后刷新。';
      td.append(strong, text);
      tr.appendChild(td);
      rows.appendChild(tr);
      return;
    }

    for (const item of items) {
      const tr = document.createElement('tr');
      tr.dataset.bucket = item.bucket || 'UNKNOWN';
      tr.dataset.search = searchText(item);

      const idCell = document.createElement('td');
      appendText(idCell, 'span', item.businessNo, 'xww-primary');
      if (item.orderNo && item.orderNo !== item.businessNo) appendText(idCell, 'small', `订单号 ${item.orderNo}`, 'xww-secondary');
      tr.appendChild(idCell);

      const serviceCell = document.createElement('td');
      appendText(serviceCell, 'span', item.game || '未填写游戏', 'xww-primary');
      appendText(serviceCell, 'small', item.demand || '未填写需求', 'xww-secondary');
      if (item.expectedTime) appendText(serviceCell, 'small', `期望：${item.expectedTime}`, 'xww-secondary');
      tr.appendChild(serviceCell);

      tr.appendChild(textCell(statusLabel(item.status), 'xww-primary'));
      tr.appendChild(progressCell(item));
      tr.appendChild(ownershipCell(item));
      tr.appendChild(textCell(formatTime(item.updatedAt)));
      tr.appendChild(actionCell(item));
      rows.appendChild(tr);
    }
    applyFilters();
  }

  function applyFilters() {
    const query = normalize(search?.value);
    let visible = 0;
    rows.querySelectorAll('tr[data-bucket]').forEach((row) => {
      const bucketMatches = activeFilter === 'ALL' || row.dataset.bucket === activeFilter;
      const textMatches = !query || row.dataset.search.includes(query);
      row.hidden = !(bucketMatches && textMatches);
      if (!row.hidden) visible += 1;
    });
    filterButtons.forEach((button) => button.setAttribute('aria-selected', String(button.dataset.filter === activeFilter)));
    kpiButtons.forEach((button) => button.dataset.active = String(button.dataset.kpiFilter === activeFilter));
    if (snapshot) setMessage(`当前显示 ${visible} / ${snapshot.visibleCount ?? 0} 条待办。`, 'ok');
  }

  function setFilter(value) {
    activeFilter = value || 'ALL';
    applyFilters();
  }

  function updateScopeControls(data) {
    const selfOnly = data?.dataScope === 'SELF';
    const allowUnassigned = data?.dataScope === 'ALL';
    const queueTab = filterButtons.find((button) => button.dataset.filter === 'DEPARTMENT_QUEUE');
    const unassignedTab = filterButtons.find((button) => button.dataset.filter === 'UNASSIGNED');
    if (queueTab) queueTab.hidden = selfOnly;
    if (unassignedTab) unassignedTab.hidden = !allowUnassigned;
    const queueKpi = kpiButtons.find((button) => button.dataset.kpiFilter === 'DEPARTMENT_QUEUE');
    const unassignedKpi = kpiButtons.find((button) => button.dataset.kpiFilter === 'UNASSIGNED');
    if (queueKpi) queueKpi.disabled = selfOnly;
    if (unassignedKpi) unassignedKpi.disabled = !allowUnassigned;
    if ((activeFilter === 'DEPARTMENT_QUEUE' && selfOnly) || (activeFilter === 'UNASSIGNED' && !allowUnassigned)) {
      activeFilter = 'ALL';
    }
  }

  async function load() {
    setMessage('正在读取受控任务列表……');
    try {
      const data = await fetchJson('/api/admin/xingwei/work');
      snapshot = data;
      noAccess.hidden = true;
      panel.hidden = false;
      accessBadge.classList.remove('error');
      accessBadge.classList.add('ok');
      accessBadge.textContent = `已按 ${scopeLabel(data)} 裁剪`;
      scopeText.textContent = `${scopeLabel(data)} · 最多 100 条待办`;
      kpiScope.textContent = scopeLabel(data);
      kpiMine.textContent = String(data.mineCount ?? 0);
      kpiQueue.textContent = String(data.queueCount ?? 0);
      kpiUnassigned.textContent = data.dataScope === 'ALL' ? String(data.unassignedCount ?? 0) : '不可见';
      updateScopeControls(data);
      render(data.items || []);
      setMessage(`已加载 ${data.visibleCount ?? 0} 条待办。`, 'ok');
    } catch (error) {
      snapshot = null;
      rows.replaceChildren();
      panel.hidden = true;
      noAccess.hidden = false;
      accessBadge.classList.remove('ok');
      accessBadge.classList.add('error');
      accessBadge.textContent = '任务读取失败';
      scopeText.textContent = '受控任务默认关闭';
      setMessage(`读取我的工作失败：${error.message}`, 'error');
    }
  }

  async function claimTask(item, button) {
    button.disabled = true;
    setMessage(`正在领取 ${item.businessNo}……`);
    try {
      const headers = {};
      if (csrfToken && csrfHeader) headers[csrfHeader] = csrfToken;
      await fetchJson(`/api/admin/xingwei/work/orders/${encodeURIComponent(item.businessNo)}/claim`, {
        method: 'PUT', headers
      });
      await load();
      setMessage(`任务 ${item.businessNo} 已领取到本人。`, 'ok');
    } catch (error) {
      button.disabled = false;
      setMessage(`领取失败：${error.message}`, 'error');
    }
  }

  function openEdit(item) {
    editing = item;
    editTitle.textContent = `更新履约 · ${item.businessNo}`;
    editContext.textContent = `${bucketLabel(item.bucket)}任务 · 保存前会核对最新状态。`;
    editStatus.value = item.status || 'IN_PROGRESS';
    editPercent.value = item.progressPercent == null ? '' : String(item.progressPercent);
    editProgress.value = item.progressText || '';
    editCustomerNote.value = item.customerNote || '';
    editReason.value = '';
    editState.textContent = '';
    dialog.showModal();
  }

  async function saveEdit(event) {
    event.preventDefault();
    if (!editing) return;
    const percentValue = editPercent.value.trim();
    const body = {
      version: editing.version,
      status: editStatus.value,
      progressPercent: percentValue === '' ? null : Number(percentValue),
      progressText: editProgress.value.trim(),
      customerNote: editCustomerNote.value.trim() || null,
      reason: editReason.value.trim() || null
    };
    if (!body.progressText) {
      editState.textContent = '请填写进度说明';
      return;
    }
    if (body.progressPercent != null && (!Number.isFinite(body.progressPercent) || body.progressPercent < 0 || body.progressPercent > 100)) {
      editState.textContent = '进度百分比必须在 0-100 之间';
      return;
    }

    editState.textContent = '正在保存……';
    try {
      const headers = {'Content-Type': 'application/json'};
      if (csrfToken && csrfHeader) headers[csrfHeader] = csrfToken;
      await fetchJson(`/api/admin/xingwei/work/orders/${encodeURIComponent(editing.businessNo)}`, {
        method: 'PATCH', headers, body: JSON.stringify(body)
      });
      const businessNo = editing.businessNo;
      dialog.close();
      editing = null;
      await load();
      setMessage(`任务 ${businessNo} 履约状态已保存。`, 'ok');
    } catch (error) {
      editState.textContent = `保存失败：${error.message}`;
    }
  }

  filterButtons.forEach((button) => button.addEventListener('click', () => setFilter(button.dataset.filter)));
  kpiButtons.forEach((button) => button.addEventListener('click', () => {
    if (!button.disabled) setFilter(button.dataset.kpiFilter);
  }));
  search?.addEventListener('input', applyFilters);
  refreshButton?.addEventListener('click', load);
  editForm?.addEventListener('submit', saveEdit);
  document.getElementById('xwwEditClose')?.addEventListener('click', () => dialog.close());
  document.getElementById('xwwEditCancel')?.addEventListener('click', () => dialog.close());
  dialog?.addEventListener('close', () => { editing = null; editState.textContent = ''; });

  load();
})();
