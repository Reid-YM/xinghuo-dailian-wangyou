(() => {
  'use strict';

  const accessBadge = document.getElementById('xwbAccessBadge');
  const scopeText = document.getElementById('xwbScopeText');
  const message = document.getElementById('xwbMessage');
  const refreshButton = document.getElementById('xwbRefresh');
  const search = document.getElementById('xwbSearch');
  const searchLabel = document.getElementById('xwbSearchLabel');
  const tabs = Array.from(document.querySelectorAll('[data-tab]'));
  const quotePanel = document.getElementById('xwbQuotesPanel');
  const orderPanel = document.getElementById('xwbOrdersPanel');
  const orderHeading = document.getElementById('xwbOrderHeading');
  const orderDescription = document.getElementById('xwbOrderDescription');
  const noAccess = document.getElementById('xwbNoAccess');
  const quoteRows = document.getElementById('xwbQuoteRows');
  const orderRows = document.getElementById('xwbOrderRows');
  const afterSalesPager = document.getElementById('xwbAfterSalesPager');
  const afterSalesPrev = document.getElementById('xwbAfterSalesPrev');
  const afterSalesNext = document.getElementById('xwbAfterSalesNext');
  const afterSalesPageText = document.getElementById('xwbAfterSalesPageText');
  const kpiScope = document.getElementById('xwbKpiScope');
  const kpiVisibleLabel = document.getElementById('xwbKpiVisibleLabel');
  const kpiVisible = document.getElementById('xwbKpiVisible');
  const kpiFilteredLabel = document.getElementById('xwbKpiFilteredLabel');
  const kpiFiltered = document.getElementById('xwbKpiFiltered');
  const kpiUnassigned = document.getElementById('xwbKpiUnassigned');
  const dialog = document.getElementById('xwbAssignDialog');
  const assignForm = document.getElementById('xwbAssignForm');
  const assignTitle = document.getElementById('xwbAssignTitle');
  const assignCurrent = document.getElementById('xwbAssignCurrent');
  const assignTarget = document.getElementById('xwbAssignTarget');
  const assignState = document.getElementById('xwbAssignState');
  const csrfToken = document.querySelector('meta[name="_csrf"]')?.content || '';
  const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content || 'X-CSRF-TOKEN';

  let access = null;
  let activeTab = null;
  let currentSnapshot = null;
  let candidates = null;
  let assignment = null;
  let afterSalesPage = 0;
  let searchTimer = null;
  let requestVersion = 0;

  function normalize(value) {
    return String(value ?? '').toLocaleLowerCase('zh-CN').trim();
  }

  function can(permission) {
    if (!access || !permission) return false;
    if (access.allowAll || access.permissionCodes?.includes(permission)) return true;
    if (permission.endsWith(':view')) {
      const manage = `${permission.slice(0, -':view'.length)}:manage`;
      return Boolean(access.permissionCodes?.includes(manage));
    }
    return false;
  }

  function scopeLabel(snapshot) {
    switch (snapshot?.dataScope) {
      case 'ALL': return '全部数据';
      case 'DEPARTMENT': return snapshot.departmentCode ? `本中心 · ${snapshot.departmentCode}` : '本中心 · 无中心';
      case 'SELF': return '仅本人';
      default: return '无数据范围';
    }
  }

  function modeLabel(snapshot) {
    switch (snapshot?.mode) {
      case 'LEGACY_ADMIN_OVERRIDE': return '管理员兜底 · 全部数据';
      case 'LEGACY_OPERATOR_COMPAT': return 'OPERATOR 兼容 · 全部数据';
      case 'SHADOW_RBAC': return `RBAC · ${scopeLabel(snapshot)}`;
      default: return '权限不可用';
    }
  }

  function setMessage(text, state = '') {
    if (!message) return;
    message.textContent = text;
    message.classList.remove('ok', 'error');
    if (state) message.classList.add(state);
  }

  async function fetchJson(url, options = {}) {
    const {headers: optionHeaders = {}, ...rest} = options;
    const response = await fetch(url, {
      credentials: 'same-origin',
      ...rest,
      headers: {'Accept': 'application/json', ...optionHeaders}
    });
    if (!response.ok) {
      let detail = '';
      try {
        const body = await response.json();
        detail = body?.error || body?.message || '';
      } catch (_) {
        detail = '';
      }
      throw new Error(detail || `HTTP ${response.status}`);
    }
    return response.json();
  }

  function textCell(value, className = '') {
    const td = document.createElement('td');
    if (className) td.className = className;
    td.textContent = value == null || value === '' ? '—' : String(value);
    return td;
  }

  function appendText(parent, tag, text, className = '') {
    const node = document.createElement(tag);
    if (className) node.className = className;
    node.textContent = text == null || text === '' ? '—' : String(text);
    parent.appendChild(node);
    return node;
  }

  function formatTime(value) {
    if (!value) return '—';
    const date = new Date(value);
    return Number.isNaN(date.getTime()) ? String(value) : date.toLocaleString('zh-CN', {hour12: false});
  }

  function ownershipLabel(ownership) {
    if (!ownership?.assigned) return '未归属';
    const owner = ownership.ownerDisplayName || ownership.ownerUsername;
    return owner ? `${ownership.departmentName || ownership.departmentCode || '未知中心'} · ${owner}` : `${ownership.departmentName || ownership.departmentCode || '未知中心'} · 中心队列`;
  }

  function ownershipCell(ownership) {
    const td = document.createElement('td');
    const box = document.createElement('div');
    box.className = `xwb-owner${ownership?.assigned ? '' : ' unassigned'}`;
    appendText(box, 'strong', ownership?.assigned ? (ownership.ownerDisplayName || ownership.ownerUsername || '中心队列') : '未归属');
    appendText(box, 'small', ownership?.assigned ? (ownership.departmentName || ownership.departmentCode || '未知中心') : '仅 ALL 范围可见');
    td.appendChild(box);
    return td;
  }

  function actionCell(type, item) {
    const td = document.createElement('td');
    if (!item.canAssign) {
      appendText(td, 'span', '只读', 'xw-muted-label');
      return td;
    }
    const button = document.createElement('button');
    button.type = 'button';
    button.className = 'xw-mini-button';
    button.textContent = item.ownership?.assigned ? '调整归属' : '分配业务';
    button.addEventListener('click', () => openAssignment(type, item));
    td.appendChild(button);
    return td;
  }

  function quoteSearchText(item) {
    return normalize([
      item.businessNo, item.game, item.demand, item.currentProgress, item.platform,
      item.contactMethod, item.customerContact, item.status, ownershipLabel(item.ownership)
    ].join(' '));
  }

  function orderSearchText(item) {
    return normalize([
      item.businessNo, item.orderNo, item.status, item.progressText,
      item.customerNote, item.internalNote, ownershipLabel(item.ownership)
    ].join(' '));
  }

  function renderQuotes(items) {
    quoteRows.replaceChildren();
    if (!items.length) {
      const tr = document.createElement('tr');
      tr.className = 'xwb-row-empty';
      const td = document.createElement('td');
      td.colSpan = 7;
      td.textContent = '当前数据范围内没有询价记录';
      tr.appendChild(td);
      quoteRows.appendChild(tr);
      return;
    }
    for (const item of items) {
      const tr = document.createElement('tr');
      tr.dataset.search = quoteSearchText(item);

      const idCell = document.createElement('td');
      appendText(idCell, 'span', item.businessNo, 'xwb-primary');
      appendText(idCell, 'small', item.game || '未填写游戏', 'xwb-secondary');
      tr.appendChild(idCell);

      const demandCell = document.createElement('td');
      appendText(demandCell, 'span', item.demand || '未填写需求', 'xwb-primary');
      if (item.currentProgress) appendText(demandCell, 'small', item.currentProgress, 'xwb-secondary');
      tr.appendChild(demandCell);

      const contactCell = document.createElement('td');
      appendText(contactCell, 'span', item.customerContact || '未留联系方式', 'xwb-primary');
      appendText(contactCell, 'small', [item.platform, item.contactMethod].filter(Boolean).join(' · ') || '—', 'xwb-secondary');
      tr.appendChild(contactCell);

      tr.appendChild(textCell(item.status, 'xwb-primary'));
      tr.appendChild(ownershipCell(item.ownership));
      tr.appendChild(textCell(formatTime(item.createdAt)));
      tr.appendChild(actionCell('quotes', item));
      quoteRows.appendChild(tr);
    }
  }

  function renderOrders(items) {
    orderRows.replaceChildren();
    if (!items.length) {
      const tr = document.createElement('tr');
      tr.className = 'xwb-row-empty';
      const td = document.createElement('td');
      td.colSpan = 7;
      td.textContent = activeTab === 'after-sales' ? '当前范围没有匹配的售后问题记录' : '当前数据范围内没有订单记录';
      tr.appendChild(td);
      orderRows.appendChild(tr);
      return;
    }
    for (const item of items) {
      const tr = document.createElement('tr');
      tr.dataset.search = orderSearchText(item);

      const idCell = document.createElement('td');
      appendText(idCell, 'span', item.businessNo, 'xwb-primary');
      appendText(idCell, 'small', item.orderNo && item.orderNo !== item.businessNo ? `订单号 ${item.orderNo}` : '统一业务编号', 'xwb-secondary');
      tr.appendChild(idCell);

      tr.appendChild(textCell(item.status, 'xwb-primary'));

      const progressCell = document.createElement('td');
      progressCell.className = 'xwb-progress';
      appendText(progressCell, 'span', item.progressPercent == null ? '未设置百分比' : `${item.progressPercent}%`, 'xwb-primary');
      if (item.progressText) appendText(progressCell, 'small', item.progressText, 'xwb-secondary');
      if (item.progressPercent != null) {
        const bar = document.createElement('div');
        bar.className = 'xwb-progress-bar';
        const fill = document.createElement('i');
        fill.style.width = `${Math.max(0, Math.min(100, Number(item.progressPercent) || 0))}%`;
        bar.appendChild(fill);
        progressCell.appendChild(bar);
      }
      tr.appendChild(progressCell);

      const noteCell = document.createElement('td');
      if (item.customerNote) appendText(noteCell, 'span', `客户说明：${item.customerNote}`, 'xwb-secondary');
      if (item.internalNote) appendText(noteCell, 'small', `内部备注：${item.internalNote}`, 'xwb-secondary');
      if (!item.customerNote && !item.internalNote) appendText(noteCell, 'span', '—', 'xw-muted-label');
      tr.appendChild(noteCell);

      tr.appendChild(ownershipCell(item.ownership));
      tr.appendChild(textCell(formatTime(item.updatedAt)));
      tr.appendChild(actionCell('orders', item));
      orderRows.appendChild(tr);
    }
  }

  function applySearch() {
    if (activeTab === 'after-sales') {
      if (kpiFiltered) kpiFiltered.textContent = String(currentSnapshot?.visibleCount ?? 0);
      return;
    }
    const query = normalize(search?.value);
    const tbody = activeTab === 'quotes' ? quoteRows : orderRows;
    if (!tbody) return;
    let count = 0;
    tbody.querySelectorAll('tr[data-search]').forEach((row) => {
      const show = !query || row.dataset.search.includes(query);
      row.hidden = !show;
      if (show) count += 1;
    });
    if (kpiFiltered) kpiFiltered.textContent = String(count);
  }

  function updateKpis(snapshot) {
    if (kpiScope) kpiScope.textContent = scopeLabel(snapshot);
    if (kpiVisible) kpiVisible.textContent = String(activeTab === 'after-sales' ? snapshot?.totalCount ?? 0 : snapshot?.visibleCount ?? 0);
    if (kpiUnassigned) kpiUnassigned.textContent = snapshot?.unassignedVisible ? String(snapshot?.unassignedCount ?? 0) : '不可见';
    applySearch();
  }

  function updatePanels() {
    quotePanel.hidden = activeTab !== 'quotes';
    orderPanel.hidden = activeTab === 'quotes';
    if (afterSalesPager) afterSalesPager.hidden = activeTab !== 'after-sales';
    if (kpiVisibleLabel) kpiVisibleLabel.textContent = activeTab === 'after-sales' ? '匹配总数' : '接口可见';
    if (kpiFilteredLabel) kpiFilteredLabel.textContent = activeTab === 'after-sales' ? '本页条数' : '当前筛选';
    if (searchLabel) searchLabel.textContent = activeTab === 'after-sales' ? '检索全部售后问题记录' : '筛选当前列表';
    if (search) search.placeholder = activeTab === 'after-sales'
      ? '业务编号、订单号或客户说明……'
      : '业务编号、游戏、需求、客户联系方式、负责人……';
    if (orderHeading) orderHeading.textContent = activeTab === 'exceptions' ? '异常订单' : activeTab === 'after-sales' ? '售后问题记录' : '订单与履约';
    if (orderDescription) orderDescription.textContent = activeTab === 'after-sales'
      ? '这里集中展示客户曾提交售后说明的订单，按当前账号的数据范围筛选。记录不代表退款申请或处理状态。'
      : '询价转订单后沿用同一个 business_no，因此负责人和中心归属自动继承。异常订单由后端按状态和数据范围独立筛选。';
    tabs.forEach((button) => button.setAttribute('aria-selected', String(button.dataset.tab === activeTab)));
  }

  async function loadTab(tab, force = false) {
    if (tab === 'quotes' && !can('quote:view')) return;
    if (tab !== 'quotes' && !can('order:view')) return;
    if (tab !== 'after-sales') clearTimeout(searchTimer);
    if (tab === 'after-sales' && activeTab !== 'after-sales') afterSalesPage = 0;
    activeTab = tab;
    updatePanels();
    const thisRequest = ++requestVersion;
    const label = tab === 'quotes' ? '询价' : tab === 'exceptions' ? '异常订单' : tab === 'after-sales' ? '售后问题' : '订单';
    setMessage(`正在读取${label}数据……`);
    try {
      const endpoint = tab === 'exceptions' ? 'orders/exceptions' : tab === 'after-sales' ? 'orders/after-sales' : tab;
      const params = tab === 'after-sales'
        ? `?page=${afterSalesPage}&q=${encodeURIComponent(search?.value.trim() || '')}`
        : '';
      const snapshot = await fetchJson(`/api/admin/xingwei/business/${endpoint}${params}`);
      if (thisRequest !== requestVersion) return;
      currentSnapshot = snapshot;
      if (tab === 'quotes') renderQuotes(snapshot.items || []);
      else renderOrders(snapshot.items || []);
      updateKpis(snapshot);
      if (tab === 'after-sales') {
        afterSalesPage = snapshot.page ?? afterSalesPage;
        afterSalesPrev.disabled = afterSalesPage === 0;
        afterSalesNext.disabled = !snapshot.hasNext;
        afterSalesPageText.textContent = `第 ${afterSalesPage + 1} 页 · 共 ${snapshot.totalCount ?? 0} 条`;
      }
      setMessage(tab === 'after-sales'
        ? `已按 ${scopeLabel(snapshot)} 检索到 ${snapshot.totalCount ?? 0} 条售后问题记录。`
        : `已按 ${scopeLabel(snapshot)} 加载 ${snapshot.visibleCount ?? 0} 条${label}记录。`, 'ok');
      if (force) candidates = null;
    } catch (error) {
      if (thisRequest !== requestVersion) return;
      currentSnapshot = null;
      setMessage(`读取业务数据失败：${error.message}`, 'error');
      if (kpiVisible) kpiVisible.textContent = '—';
      if (kpiFiltered) kpiFiltered.textContent = '—';
      if (kpiUnassigned) kpiUnassigned.textContent = '—';
    }
  }

  async function loadCandidates() {
    if (candidates) return candidates;
    candidates = await fetchJson('/api/admin/xingwei/business/ownership-candidates');
    return candidates;
  }

  function candidateLabel(candidate) {
    const name = candidate.displayName || candidate.username || `账号 ${candidate.adminUserId}`;
    const dept = candidate.departmentName || candidate.departmentCode || '未知中心';
    const position = candidate.positionName ? ` · ${candidate.positionName}` : '';
    return `${dept} · ${name}${position}`;
  }

  async function openAssignment(type, item) {
    assignment = {type, item};
    assignTitle.textContent = `${type === 'quotes' ? '询价' : '订单'}归属 · ${item.businessNo}`;
    assignCurrent.textContent = `当前归属：${ownershipLabel(item.ownership)}`;
    assignState.textContent = '正在读取可分配对象……';
    assignTarget.replaceChildren();
    const loading = document.createElement('option');
    loading.value = '';
    loading.textContent = '正在加载……';
    assignTarget.appendChild(loading);
    dialog.showModal();
    try {
      const rows = await loadCandidates();
      assignTarget.replaceChildren();
      const placeholder = document.createElement('option');
      placeholder.value = '';
      placeholder.textContent = rows.length
        ? (access?.dataScope === 'SELF' ? '请选择本人' : '请选择负责人或中心队列')
        : '当前范围没有可分配员工';
      assignTarget.appendChild(placeholder);

      if (access?.dataScope !== 'SELF') {
        const departments = new Map();
        rows.forEach((candidate) => {
          if (candidate.departmentCode && !departments.has(candidate.departmentCode)) {
            departments.set(candidate.departmentCode, candidate.departmentName || candidate.departmentCode);
          }
        });
        if (item.ownership?.departmentCode && !departments.has(item.ownership.departmentCode)) {
          departments.set(item.ownership.departmentCode, item.ownership.departmentName || item.ownership.departmentCode);
        }
        departments.forEach((name, code) => {
          const option = document.createElement('option');
          option.value = `dept:${code}`;
          option.textContent = `${name} · 中心队列`;
          assignTarget.appendChild(option);
        });
      }
      rows.forEach((candidate) => {
        const option = document.createElement('option');
        option.value = `owner:${candidate.adminUserId}`;
        option.textContent = candidateLabel(candidate);
        assignTarget.appendChild(option);
      });
      assignState.textContent = '';
    } catch (error) {
      assignState.textContent = `读取候选失败：${error.message}`;
    }
  }

  async function saveAssignment(event) {
    event.preventDefault();
    if (!assignment) return;
    const value = assignTarget.value;
    if (!value) {
      assignState.textContent = '请选择负责人或中心队列';
      return;
    }
    const body = {};
    if (value.startsWith('owner:')) body.ownerAdminUserId = Number(value.slice('owner:'.length));
    if (value.startsWith('dept:')) body.departmentCode = value.slice('dept:'.length);
    const endpoint = `/api/admin/xingwei/business/${assignment.type}/${encodeURIComponent(assignment.item.businessNo)}/ownership`;
    assignState.textContent = '正在保存……';
    try {
      const headers = {'Content-Type': 'application/json'};
      if (csrfToken && csrfHeader) headers[csrfHeader] = csrfToken;
      await fetchJson(endpoint, {method: 'PUT', headers, body: JSON.stringify(body)});
      dialog.close();
      candidates = null;
      assignment = null;
      await loadTab(activeTab, true);
      setMessage('业务归属已保存，并写入操作审计。', 'ok');
    } catch (error) {
      assignState.textContent = `保存失败：${error.message}`;
    }
  }

  function selectInitialTab() {
    const requested = new URLSearchParams(location.search).get('tab');
    if (requested === 'after-sales' && can('order:view')) return 'after-sales';
    if (requested === 'exceptions' && can('order:view')) return 'exceptions';
    if (requested === 'orders' && can('order:view')) return 'orders';
    if (requested === 'quotes' && can('quote:view')) return 'quotes';
    if (can('quote:view')) return 'quotes';
    if (can('order:view')) return 'orders';
    return null;
  }

  async function init() {
    try {
      access = await fetchJson('/api/admin/xingwei/access');
      accessBadge.classList.add('ok');
      accessBadge.lastChild.textContent = modeLabel(access);
      scopeText.textContent = `${modeLabel(access)} · 权限点 ${(access.permissionCodes || []).length}`;
      tabs.find((button) => button.dataset.tab === 'quotes').hidden = !can('quote:view');
      tabs.find((button) => button.dataset.tab === 'orders').hidden = !can('order:view');
      tabs.find((button) => button.dataset.tab === 'exceptions').hidden = !can('order:view');
      tabs.find((button) => button.dataset.tab === 'after-sales').hidden = !can('order:view');
      activeTab = selectInitialTab();
      if (!activeTab) {
        noAccess.hidden = false;
        quotePanel.hidden = true;
        orderPanel.hidden = true;
        setMessage('当前账号没有新版询价或订单查看权限。', 'error');
        kpiScope.textContent = scopeLabel(access);
        return;
      }
      noAccess.hidden = true;
      await loadTab(activeTab);
    } catch (error) {
      access = {mode: 'DENIED', dataScope: 'NONE', allowAll: false, permissionCodes: []};
      accessBadge.classList.add('error');
      accessBadge.lastChild.textContent = '权限读取失败';
      scopeText.textContent = '受控业务模块默认关闭';
      noAccess.hidden = false;
      setMessage(`权限读取失败：${error.message}`, 'error');
    }
  }

  tabs.forEach((button) => button.addEventListener('click', () => loadTab(button.dataset.tab)));
  search?.addEventListener('input', () => {
    if (activeTab !== 'after-sales') {
      applySearch();
      return;
    }
    clearTimeout(searchTimer);
    requestVersion += 1;
    afterSalesPage = 0;
    searchTimer = setTimeout(() => loadTab('after-sales'), 250);
  });
  afterSalesPrev?.addEventListener('click', () => {
    if (afterSalesPage > 0) {
      afterSalesPage -= 1;
      loadTab('after-sales');
    }
  });
  afterSalesNext?.addEventListener('click', () => {
    if (!afterSalesNext.disabled) {
      afterSalesPage += 1;
      loadTab('after-sales');
    }
  });
  refreshButton?.addEventListener('click', () => activeTab && loadTab(activeTab, true));
  assignForm?.addEventListener('submit', saveAssignment);
  document.getElementById('xwbAssignClose')?.addEventListener('click', () => dialog.close());
  document.getElementById('xwbAssignCancel')?.addEventListener('click', () => dialog.close());
  dialog?.addEventListener('close', () => { assignment = null; assignState.textContent = ''; });

  init();
})();
