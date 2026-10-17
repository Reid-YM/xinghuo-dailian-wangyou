(() => {
  'use strict';
 const display=value=>window.XWLabels?.value(value)??value;

  const accessBadge = document.getElementById('xwbAccessBadge');
  const scopeText = document.getElementById('xwbScopeText');
  const message = document.getElementById('xwbMessage');
  const refreshButton = document.getElementById('xwbRefresh');
  const search = document.getElementById('xwbSearch');
  const searchLabel = document.getElementById('xwbSearchLabel');
  const afterSalesCategoryLabel = document.getElementById('xwbAfterSalesCategoryLabel');
  const afterSalesCategory = document.getElementById('xwbAfterSalesCategory');
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
  const afterSalesDetailDialog = document.getElementById('xwbAfterSalesDetailDialog');
  const afterSalesDetailMeta = document.getElementById('xwbAfterSalesDetailMeta');
  const afterSalesDetailNote = document.getElementById('xwbAfterSalesDetailNote');
  const afterSalesDetailInternal = document.getElementById('xwbAfterSalesDetailInternal');
  const csrfToken = document.querySelector('meta[name="_csrf"]')?.content || '';
  const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content || 'X-CSRF-TOKEN';

  let access = null;
  let activeTab = null;
  let currentSnapshot = null;
  let assignment = null;
  let assignmentBusy = false, assignmentReady = false, assignmentDirty = false, assignmentRequest = 0;
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
      case 'LEGACY_OPERATOR_COMPAT': return '旧版运营员工兼容 · 全部数据';
      case 'SHADOW_RBAC': return `角色权限 · ${scopeLabel(snapshot)}`;
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
      cache: 'no-store',
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
      throw new Error(detail || `请求失败（状态码 ${response.status}）`);
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
    if (activeTab === 'after-sales') {
      const detailButton = document.createElement('button');
      detailButton.type = 'button';
      detailButton.className = 'xw-mini-button';
      detailButton.textContent = '查看说明';
      detailButton.addEventListener('click', () => openAfterSalesDetail(item));
      td.appendChild(detailButton);
    }
    if ((type === 'quotes' && item.hasScreenshot) || (type === 'orders' && item.hasProgressImage)) {
      const image = document.createElement('a'); image.textContent = '查看截图'; image.className = 'xw-mini-button';
      image.href = `/api/admin/xingwei/business/${type}/${encodeURIComponent(item.businessNo)}/image`; image.target = '_blank'; image.rel = 'noopener'; td.appendChild(image);
    }
    if (item.canManage && activeTab !== 'after-sales') {
      const edit = document.createElement('button'); edit.type = 'button'; edit.className = 'xw-mini-button';
      edit.textContent = type === 'quotes' ? '处理询价' : '编辑订单'; edit.addEventListener('click', () => type === 'quotes' ? openQuoteAction(item) : openOrderAction(item)); td.appendChild(edit);
    }
    if (!item.canAssign) {
      if (activeTab !== 'after-sales' && !td.children.length) appendText(td, 'span', '只读', 'xw-muted-label');
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

  function openAfterSalesDetail(item) {
    afterSalesDetailMeta.textContent = `业务编号 ${item.businessNo || '—'} · 订单号 ${item.orderNo || '—'} · ${display(item.status) || '状态未知'} · 更新于 ${formatTime(item.updatedAt)}`;
    afterSalesDetailNote.textContent = item.customerNote || '暂无客户说明';
    afterSalesDetailInternal.textContent = item.internalNote || '暂无内部备注';
    afterSalesDetailDialog.showModal();
  }

  function quoteSearchText(item) {
    return normalize([
      item.businessNo, item.game, item.demand, item.currentProgress, item.platform,
      item.contactMethod, item.customerContact, item.status, display(item.status), ownershipLabel(item.ownership)
    ].join(' '));
  }

  function orderSearchText(item) {
    return normalize([
      item.businessNo, item.orderNo, item.status, display(item.status), item.progressText,
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
      tr.dataset.guestOfferAllowed = item.canOffer === true ? 'true' : 'false';

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

      tr.appendChild(textCell(display(item.status), 'xwb-primary'));
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

      tr.appendChild(textCell(display(item.status), 'xwb-primary'));

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
    if (afterSalesCategoryLabel) afterSalesCategoryLabel.hidden = activeTab !== 'after-sales';
    if (kpiVisibleLabel) kpiVisibleLabel.textContent = activeTab === 'after-sales' ? '匹配总数' : '接口可见';
    if (kpiFilteredLabel) kpiFilteredLabel.textContent = activeTab === 'after-sales' ? '本页条数' : '当前筛选';
    if (searchLabel) searchLabel.textContent = activeTab === 'after-sales' ? '检索全部售后问题记录' : '筛选当前列表';
    if (search) search.placeholder = activeTab === 'after-sales'
      ? '业务编号、订单号或客户说明……'
      : '业务编号、游戏、需求、客户联系方式、负责人……';
    if (orderHeading) orderHeading.textContent = activeTab === 'exceptions' ? '异常订单' : activeTab === 'after-sales' ? '售后问题记录' : '订单与履约';
    if (orderDescription) orderDescription.textContent = activeTab === 'after-sales'
      ? '这里集中展示客户曾提交售后说明的订单，按当前账号的数据范围筛选。记录不代表退款申请或处理状态。'
      : '询价转订单后沿用同一个业务编号，因此负责人和中心归属自动继承。异常订单由后端按状态和数据范围独立筛选。';
    tabs.forEach((button) => button.setAttribute('aria-selected', String(button.dataset.tab === activeTab)));
  }

  async function loadTab(tab, {afterWrite = false} = {}) {
    if (!afterWrite && (actionBusy || assignmentBusy)) return false;
    if (tab === 'quotes' && !can('quote:view')) return;
    if (tab !== 'quotes' && !can('order:view')) return;
    if (tab !== 'after-sales') clearTimeout(searchTimer);
    if (tab === 'after-sales' && activeTab !== 'after-sales') afterSalesPage = 0;
    activeTab = tab;
    updatePanels();
    currentSnapshot = null;
    quoteRows.replaceChildren(); orderRows.replaceChildren();
    quoteRows.setAttribute('aria-busy','true'); orderRows.setAttribute('aria-busy','true');
    for (const node of [kpiVisible,kpiFiltered,kpiUnassigned]) if(node) node.textContent='—';
    if(afterSalesPrev) afterSalesPrev.disabled=true;
    if(afterSalesNext) afterSalesNext.disabled=true;
    if(afterSalesPageText) afterSalesPageText.textContent='';
    const thisRequest = ++requestVersion;
    const label = tab === 'quotes' ? '询价' : tab === 'exceptions' ? '异常订单' : tab === 'after-sales' ? '售后问题' : '订单';
    setMessage(`正在读取${label}数据……`);
    try {
      const endpoint = tab === 'exceptions' ? 'orders/exceptions' : tab === 'after-sales' ? 'orders/after-sales' : tab;
      const params = tab === 'after-sales'
        ? `?page=${afterSalesPage}&q=${encodeURIComponent(search?.value.trim() || '')}&category=${encodeURIComponent(afterSalesCategory?.value || '')}`
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
      return true;
    } catch (error) {
      if (thisRequest !== requestVersion) return;
      currentSnapshot = null;
      setMessage(`读取业务数据失败：${error.message}`, 'error');
      if (kpiVisible) kpiVisible.textContent = '—';
      if (kpiFiltered) kpiFiltered.textContent = '—';
      if (kpiUnassigned) kpiUnassigned.textContent = '—';
      return false;
    } finally {
      if(thisRequest===requestVersion){quoteRows.setAttribute('aria-busy','false');orderRows.setAttribute('aria-busy','false');}
    }
  }

  function candidateLabel(candidate) {
    const name = candidate.displayName || candidate.username || `账号 ${candidate.adminUserId}`;
    const dept = candidate.departmentName || candidate.departmentCode || '未知中心';
    const position = candidate.positionName ? ` · ${candidate.positionName}` : '';
    return `${dept} · ${name}${position}`;
  }

  function assignmentControls(){
    assignForm.querySelectorAll('button,input,select,textarea').forEach(node=>{node.disabled=assignmentBusy||(!assignmentReady&&(node===assignTarget||node.type==='submit'));});
  }
  function clearAssignment(){assignmentRequest++;assignment=null;assignmentReady=false;assignmentDirty=false;assignTarget.replaceChildren();assignState.textContent='';}
  function closeAssignment(){
    if(assignmentBusy)return false;
    if(assignmentDirty&&!confirm('归属选择尚未保存，确认放弃？'))return false;
    clearAssignment();dialog.close();return true;
  }
  async function openAssignment(type, item) {
    if(actionBusy||assignmentBusy)return;
    if(dialog.open&&!closeAssignment())return;
    const pending={type,item},id=++assignmentRequest;
    assignment=pending;assignmentReady=false;assignmentDirty=false;
    document.getElementById('xwbAssignRetry').hidden=true;
    assignTitle.textContent = `${type === 'quotes' ? '询价' : '订单'}归属 · ${item.businessNo}`;
    assignCurrent.textContent = `当前归属：${ownershipLabel(item.ownership)}`;
    assignState.textContent = '正在读取可分配对象……';
    assignTarget.replaceChildren();
    const loading = document.createElement('option');
    loading.value = '';
    loading.textContent = '正在加载……';
    assignTarget.appendChild(loading);
    assignmentControls();dialog.showModal();
    try {
      const rows = await fetchJson('/api/admin/xingwei/business/ownership-candidates');
      if(id!==assignmentRequest||assignment!==pending||!dialog.open)return;
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
      assignmentReady=assignTarget.options.length>1;
      assignState.textContent=assignmentReady?'':'当前范围没有可分配对象，可重新读取候选。';
      document.getElementById('xwbAssignRetry').hidden=assignmentReady;
    } catch (error) {
      if(id!==assignmentRequest||assignment!==pending||!dialog.open)return;
      assignState.textContent = `读取候选失败：${error.message}`;
      document.getElementById('xwbAssignRetry').hidden=false;
    } finally {if(id===assignmentRequest)assignmentControls();}
  }

  async function saveAssignment(event) {
    event.preventDefault();
    if (!assignment || assignmentBusy || actionBusy || !assignmentReady) return;
    const value = assignTarget.value;
    if (!value) {
      assignState.textContent = '请选择负责人或中心队列';
      return;
    }
    const body = {};
    if (value.startsWith('owner:')) body.ownerAdminUserId = Number(value.slice('owner:'.length));
    if (value.startsWith('dept:')) body.departmentCode = value.slice('dept:'.length);
    const endpoint = `/api/admin/xingwei/business/${assignment.type}/${encodeURIComponent(assignment.item.businessNo)}/ownership`;
    assignmentBusy=true;assignmentControls();
    const tab=activeTab;
    assignState.textContent = '正在保存……';
    try {
      const headers = {'Content-Type': 'application/json'};
      if (csrfToken && csrfHeader) headers[csrfHeader] = csrfToken;
      await actionRequest(endpoint, {method:'PUT',headers,body:JSON.stringify(body)},'业务归属请求超时，写入结果尚未确认。请关闭窗口并刷新业务列表核对，勿重复提交。');
      clearAssignment();dialog.close();
      const loaded=await loadTab(tab,{afterWrite:true});
      setMessage(loaded?'业务归属已保存，并写入操作审计。':'业务归属已保存，但列表刷新失败，请手动刷新核对。',loaded?'ok':'error');
    } catch (error) {
      if(error.uncertain){assignmentReady=false;document.getElementById('xwbAssignRetry').hidden=true;}
      assignState.textContent = error.uncertain?error.message:`保存失败：${error.message}`;
    } finally {assignmentBusy=false;assignmentControls();}
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
      const createOrder = document.getElementById('xwbCreateOrder');
      if (createOrder) createOrder.hidden = !can('order:manage') || !(access.allowAll || access.dataScope === 'ALL');
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

  const quoteActionDialog = document.getElementById('xwbQuoteActionDialog');
  const quoteActionForm = document.getElementById('xwbQuoteActionForm');
  const orderActionDialog = document.getElementById('xwbOrderActionDialog');
  const orderActionForm = document.getElementById('xwbOrderActionForm');
  const orderImageForm = document.getElementById('xwbOrderImageForm');
  let actionBusy = false, quoteDirty = false, orderDirty = false, imageDirty = false;
  function fillAction(form, values) { for (const [name,value] of Object.entries(values)) { const input = form.elements.namedItem(name); if (input) input.value = value ?? ''; } }
  function openQuoteAction(item) {
    if (!quoteActionDialog || actionBusy || assignmentBusy) return;
    quoteActionForm.reset(); fillAction(quoteActionForm, {businessNo:item.businessNo,expectedStatus:item.status,status:item.status});
    document.getElementById('xwbQuoteActionTitle').textContent = '处理询价 · ' + item.businessNo;
    document.getElementById('xwbConvertQuote').hidden = !item.canConvert;
    document.getElementById('xwbQuoteActionState').textContent = ''; quoteDirty = false; quoteActionDialog.showModal();
  }
  function openOrderAction(item = null) {
    if (!orderActionDialog || actionBusy || assignmentBusy) return;
    orderActionForm.reset(); fillAction(orderActionForm,{businessNo:item?.businessNo,version:item?.version,status:item?.status || 'WAITING_CONFIRMATION',progressPercent:item?.progressPercent,progressText:item?.progressText,customerNote:item?.customerNote,internalNote:item?.internalNote});
    document.getElementById('xwbOrderActionTitle').textContent = item ? '编辑订单 · ' + item.businessNo : '新建订单';
    document.getElementById('xwbOrderActionState').textContent = '';
    orderImageForm.hidden = !item; orderImageForm.reset(); orderDirty = false; imageDirty=false; orderActionDialog.showModal();
  }
  function closeAction(kind) {
    if (actionBusy || assignmentBusy) return;
    if ((kind === 'quote' ? quoteDirty : orderDirty||imageDirty) && !confirm('尚有未保存的输入，确认放弃？')) return;
    if(kind==='quote'){quoteDirty=false;quoteActionForm.reset();quoteActionDialog.close();}
    else{orderDirty=false;imageDirty=false;orderActionForm.reset();orderImageForm.reset();orderActionDialog.close();}
  }
  function actionControls(form, busy) { form?.querySelectorAll('button,input,select,textarea').forEach(control => {control.disabled = busy;}); }
  async function actionRequest(url, options, timeoutMessage='请求超时，写入结果尚未确认。请先刷新核对，勿重复新建订单。') {
    const controller = new AbortController(), timer = setTimeout(() => controller.abort(), 20000);
    try { return await fetchJson(url,{...options,signal:controller.signal}); }
    catch(error) { if (['AbortError','TypeError','NetworkError','SyntaxError'].includes(error.name)) {const uncertain=new Error(error.name==='AbortError'?timeoutMessage:timeoutMessage.replace('请求超时',error.name==='SyntaxError'?'响应无法解析':'请求中断'));uncertain.uncertain=true;throw uncertain;} throw error; }
    finally { clearTimeout(timer); }
  }
  async function refreshAfterAction(text, tab) {
    const loaded = await loadTab(tab,{afterWrite:true});
    if (activeTab === tab) setMessage(loaded ? text : text + '；列表刷新失败，请手动刷新核对。', loaded ? 'ok' : 'error');
  }
  async function saveQuoteAction(convert) {
    if (actionBusy || !quoteActionForm.reportValidity()) return;
    const values = Object.fromEntries(new FormData(quoteActionForm)), no = values.businessNo; delete values.businessNo;
    if (convert && !confirm('确认已沟通需求并转为订单？本操作不会发起收款。')) return;
    actionBusy = true; actionControls(quoteActionForm,true);
    const state = document.getElementById('xwbQuoteActionState'); state.textContent = '正在保存…';
    try {
      const result = await actionRequest(`/api/admin/xingwei/business/quotes/${encodeURIComponent(no)}/${convert ? 'convert' : 'status'}`,{method:convert ? 'POST' : 'PATCH',headers:{'Content-Type':'application/json',[csrfHeader]:csrfToken},body:JSON.stringify(values)});
      quoteDirty = false; quoteActionDialog.close(); await refreshAfterAction(convert ? '已转为订单 · ' + (result.businessNo || result.orderNo || no) : '询价状态已保存。', convert ? 'orders' : 'quotes');
    } catch(error) { state.textContent = error.message; }
    finally { actionBusy = false; actionControls(quoteActionForm,false); }
  }
  quoteActionForm?.addEventListener('submit',event => {event.preventDefault(); saveQuoteAction(false);});
  for(const event of ['input','change']){
    quoteActionForm?.addEventListener(event,()=>{quoteDirty=true;});
    orderActionForm?.addEventListener(event,()=>{orderDirty=true;});
  }
  orderImageForm?.addEventListener('change',()=>{imageDirty=!!orderImageForm.querySelector('input[type=file]').files.length;});
  window.addEventListener('beforeunload',event=>{if(actionBusy||assignmentBusy||(quoteDirty&&quoteActionDialog?.open)||((orderDirty||imageDirty)&&orderActionDialog?.open)||(assignmentDirty&&dialog?.open)){event.preventDefault();event.returnValue='';}});
  document.getElementById('xwbConvertQuote')?.addEventListener('click',() => saveQuoteAction(true));
  document.getElementById('xwbCreateOrder')?.addEventListener('click',() => openOrderAction());
  document.querySelectorAll('[data-action-close]').forEach(button => button.addEventListener('click',() => closeAction(button.dataset.actionClose)));
  quoteActionDialog?.addEventListener('cancel',event => {event.preventDefault();closeAction('quote');});
  orderActionDialog?.addEventListener('cancel',event => {event.preventDefault();closeAction('order');});
  orderActionForm?.addEventListener('submit',async event => {
    event.preventDefault(); if (actionBusy || !orderActionForm.reportValidity()) return;
    if(imageDirty&&!confirm('已选择的截图尚未上传，保存订单会清除该选择。确认先保存订单？'))return;
    const values = Object.fromEntries(new FormData(orderActionForm)), no = values.businessNo;
    delete values.businessNo; values.progressPercent = values.progressPercent === '' ? null : Number(values.progressPercent);
    if (no) values.version = Number(values.version); else delete values.version;
    actionBusy = true; actionControls(orderActionForm,true); actionControls(orderImageForm,true);
    const state = document.getElementById('xwbOrderActionState'); state.textContent = '正在保存…';
    try {
      await actionRequest('/api/admin/xingwei/business/orders' + (no ? '/' + encodeURIComponent(no) : ''),{method:no ? 'PATCH' : 'POST',headers:{'Content-Type':'application/json',[csrfHeader]:csrfToken},body:JSON.stringify(values)});
      orderDirty = false; imageDirty=false;orderImageForm.reset();orderActionDialog.close(); await refreshAfterAction(no ? '订单进度已保存。' : '订单已创建，请在订单列表分配业务归属。','orders');
    } catch(error) {state.textContent = error.message;}
    finally {actionBusy = false;actionControls(orderActionForm,false);actionControls(orderImageForm,false);}
  });
  orderImageForm?.addEventListener('submit',async event => {
    event.preventDefault(); if (actionBusy) return;
    const state = document.getElementById('xwbOrderActionState');
    if (orderDirty) {state.textContent = '请先保存订单修改，再更新截图。';return;}
    const no = orderActionForm.elements.namedItem('businessNo').value, body = new FormData(orderImageForm);
    body.set('version',orderActionForm.elements.namedItem('version').value);
    actionBusy = true;actionControls(orderActionForm,true);actionControls(orderImageForm,true);state.textContent = '正在更新截图…';
    try {const result = await actionRequest(`/api/admin/xingwei/business/orders/${encodeURIComponent(no)}/image`,{method:'POST',headers:{[csrfHeader]:csrfToken},body});orderActionForm.elements.namedItem('version').value = result.version;orderImageForm.reset();imageDirty=false;state.textContent = '进度截图已更新。';}
    catch(error) {state.textContent = error.message;}
    finally {actionBusy = false;actionControls(orderActionForm,false);actionControls(orderImageForm,false);}
  });

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
  afterSalesCategory?.addEventListener('change', () => {
    if (activeTab !== 'after-sales') return;
    clearTimeout(searchTimer);
    requestVersion += 1;
    afterSalesPage = 0;
    loadTab('after-sales');
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
  refreshButton?.addEventListener('click', () => activeTab && loadTab(activeTab));
  assignForm?.addEventListener('submit', saveAssignment);
  document.getElementById('xwbAssignClose')?.addEventListener('click', closeAssignment);
  document.getElementById('xwbAssignCancel')?.addEventListener('click', closeAssignment);
  document.getElementById('xwbAfterSalesDetailClose')?.addEventListener('click', () => afterSalesDetailDialog.close());
  dialog?.addEventListener('cancel',event=>{event.preventDefault();closeAssignment();});
  dialog?.addEventListener('close',()=>{if(!dialog.open&&!assignmentBusy)clearAssignment();});
  assignTarget?.addEventListener('change',()=>{assignmentDirty=!!assignTarget.value;});
  document.getElementById('xwbAssignRetry')?.addEventListener('click',()=>{if(assignment&&!assignmentBusy)openAssignment(assignment.type,assignment.item);});

  init();
})();
