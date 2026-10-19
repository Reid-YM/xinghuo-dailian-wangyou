(() => {
  'use strict';
  const $ = id => document.getElementById(id);
  const names = {WEBSITE:'官网',TAOBAO:'淘宝',XIANYU:'闲鱼'};
  const statuses = {TODO:'待处理',IN_PROGRESS:'进行中',DONE:'已完成',CANCELLED:'已取消'};
  const states = {UNKNOWN:'待核对',AVAILABLE:'可接服务',PAUSED:'暂停接单'};
  const filters = $('channelFilters');
  const taskForm = $('channelTaskForm');
  const stateForm = $('channelStateForm');
  const taskDialog = $('channelTaskDialog');
  const stateDialog = $('channelStateDialog');
  let snapshot = null;
  let page = 0;
  let sequence = 0;
  let taskSaving = false;
  let stateSaving = false;
  let dirtyTask = false;
  let dirtyState = false;

  function element(tag, text, className) {
    const node = document.createElement(tag);
    if (text !== undefined) node.textContent = text;
    if (className) node.className = className;
    return node;
  }
  function button(text, action) {
    const node = element('button', text, 'xw-button xw-button-ghost');
    node.type = 'button'; node.addEventListener('click', action); return node;
  }
  async function request(url, method = 'GET', body) {
    const headers = {Accept:'application/json'};
    if (body !== undefined) {
      headers['Content-Type'] = 'application/json';
      const token = document.querySelector('meta[name="_csrf"]')?.content;
      const header = document.querySelector('meta[name="_csrf_header"]')?.content || 'X-CSRF-TOKEN';
      if (token) headers[header] = token;
    }
    const controller = new AbortController();
    const timer = setTimeout(() => controller.abort(), 12000);
    try {
      const response = await fetch(url, {method,headers,body:body === undefined ? undefined : JSON.stringify(body),signal:controller.signal,credentials:'same-origin'});
      const data = await response.json().catch(() => ({}));
      if (!response.ok) throw new Error(data.error || (response.status === 403 ? '权限不足或登录已失效，请重新登录后重试。' : '读取或保存失败，请稍后重试。'));
      if (response.redirected) throw new Error('登录已失效，请重新登录后重试。');
      return data;
    } catch (error) {
      if (error.name === 'AbortError') throw new Error('请求超时。保存操作请先刷新核对结果，再决定是否重试。');
      throw error;
    } finally { clearTimeout(timer); }
  }
  function restoreUrl() {
    const params = new URLSearchParams(location.search);
    for (const name of ['channel','status','q']) filters.elements.namedItem(name).value = params.get(name) || '';
    const parsed = Number(params.get('page') || 0);
    page = Number.isInteger(parsed) && parsed >= 0 && parsed <= 10000 ? parsed : 0;
  }
  function params() {
    const result = new URLSearchParams(new FormData(filters));
    for (const [key,value] of [...result]) if (!value) result.delete(key);
    if (page) result.set('page',String(page));
    return result;
  }
  function navigate() {
    const query = params().toString();
    history.pushState(null,'',location.pathname + (query ? '?' + query : ''));
    load();
  }
  async function load(successMessage = '') {
    const current = ++sequence;
    $('channelMessage').textContent = successMessage || '正在读取渠道运营数据…';
    $('channelRetry').hidden = true;
    $('channelTasks').setAttribute('aria-busy','true');
    $('channelPrev').disabled = true; $('channelNext').disabled = true;
    try {
      const data = await request('/api/admin/xingwei/channels?' + params());
      if (current !== sequence) return;
      snapshot = data;
      render();
      $('channelMessage').textContent = successMessage || (data.canManage ? '可维护当前数据范围内的运营任务。' : '当前账号仅可查看运营任务。');
    } catch (error) {
      if (current !== sequence) return;
      $('channelMessage').textContent = (successMessage ? successMessage + '；列表未能刷新。' : '渠道运营数据读取失败。') + error.message;
      $('channelRetry').hidden = false;
      if (!snapshot) { $('channelNew').hidden = true; $('channelTasks').replaceChildren(element('p','暂时无法展示任务，请重试。')); }
    } finally {
      if (current === sequence) $('channelTasks').setAttribute('aria-busy','false');
    }
  }
  function render() {
    $('channelNew').hidden = !snapshot.canManage;
    $('channelStates').replaceChildren();
    for (const state of snapshot.channels) {
      const card = element('article',undefined,'xwc-channel');
      card.append(element('h2',names[state.channelCode] || state.channelCode), element('span',states[state.serviceState] || '待核对','xwc-state'));
      if (snapshot.canManageChannels) card.append(button('维护状态',() => openState(state)));
      $('channelStates').append(card);
    }
    $('channelTasks').replaceChildren();
    if (!snapshot.items.length) $('channelTasks').append(element('p',snapshot.totalCount ? '此页没有记录，可返回上一页。' : '当前范围与筛选条件下暂无任务。'));
    for (const task of snapshot.items) {
      const row = element('article',undefined,'xwc-task');
      const heading = element('div',undefined,'xwc-heading');
      heading.append(element('h3',task.title),element('strong',statuses[task.status] || task.status));
      const meta = element('div',undefined,'xwc-task-meta');
      meta.append(element('span',names[task.channelCode] || task.channelCode),element('span','负责人：' + (task.ownerName || '已停用员工')),
        element('span','中心：' + (task.departmentCode || '未分配')),element('span','截止：' + (task.dueDate || '未设置')));
      row.append(heading,meta);
      if (task.details) {
        const detail = element('details'); detail.append(element('summary','任务说明'),element('p',task.details)); row.append(detail);
      }
      if (snapshot.canManage) row.append(button('编辑任务',() => openTask(task)));
      $('channelTasks').append(row);
    }
    $('channelCount').textContent = '共 ' + snapshot.totalCount + ' 项任务';
    $('channelPage').textContent = '第 ' + (page + 1) + ' 页';
    $('channelPrev').disabled = page <= 0;
    $('channelNext').disabled = (page + 1) * snapshot.pageSize >= snapshot.totalCount;
  }
  function openTask(task) {
    taskForm.reset();
    const ownerSelect = taskForm.elements.namedItem('ownerAdminUserId');
    ownerSelect.replaceChildren(element('option','请选择负责人'));
    ownerSelect.firstChild.value = '';
    for (const owner of snapshot.owners) {
      const option = element('option',owner.name + (owner.departmentCode ? ' · ' + owner.departmentCode : ''));
      option.value = String(owner.id); ownerSelect.append(option);
    }
    for (const name of ['id','version','title','details','channelCode','status','ownerAdminUserId','dueDate']) {
      const fallback = name === 'channelCode' ? 'WEBSITE' : name === 'status' ? 'TODO' : '';
      taskForm.elements.namedItem(name).value = task?.[name] ?? fallback;
    }
    $('channelTaskHeading').textContent = task ? '编辑任务' : '新增任务';
    $('channelTaskFeedback').textContent = '';
    dirtyTask = false; taskDialog.showModal();
    taskForm.elements.namedItem('title').focus();
  }
  function openState(state) {
    for (const name of ['channelCode','version','serviceState']) stateForm.elements.namedItem(name).value = state[name] ?? '';
    $('channelStateHeading').textContent = '维护' + names[state.channelCode] + '服务状态';
    $('channelStateFeedback').textContent = '';
    dirtyState = false; stateDialog.showModal();
  }
  function close(kind) {
    const isTask = kind === 'task';
    if (isTask ? taskSaving : stateSaving) return;
    if ((isTask ? dirtyTask : dirtyState) && !window.confirm('尚有未保存的修改，确认放弃编辑？')) return;
    (isTask ? taskDialog : stateDialog).close();
  }
  document.querySelectorAll('[data-close]').forEach(node => node.addEventListener('click',() => close(node.dataset.close)));
  taskDialog.addEventListener('cancel',event => { event.preventDefault(); close('task'); });
  stateDialog.addEventListener('cancel',event => { event.preventDefault(); close('state'); });
  taskForm.addEventListener('input',() => { dirtyTask = true; });
  stateForm.addEventListener('input',() => { dirtyState = true; });
  async function save(event, kind) {
    event.preventDefault();
    const isTask = kind === 'task';
    if (isTask ? taskSaving : stateSaving) return;
    const form = isTask ? taskForm : stateForm;
    const feedback = $(isTask ? 'channelTaskFeedback' : 'channelStateFeedback');
    const data = Object.fromEntries(new FormData(form));
    const id = data.id;
    data.version = data.version === '' ? null : Number(data.version);
    if (isTask) { data.ownerAdminUserId = Number(data.ownerAdminUserId); data.dueDate = data.dueDate || null; delete data.id; }
    const url = isTask ? '/api/admin/xingwei/channels/tasks' + (id ? '/' + encodeURIComponent(id) : '') : '/api/admin/xingwei/channels/' + data.channelCode + '/state';
    if (isTask) taskSaving = true; else stateSaving = true;
    const submit = form.querySelector('[type=submit]'); submit.disabled = true;
    feedback.textContent = '正在保存…';
    try {
      await request(url,isTask && !id ? 'POST' : 'PUT',data);
      if (isTask) dirtyTask = false; else dirtyState = false;
      (isTask ? taskDialog : stateDialog).close();
      await load(isTask ? '任务已保存。' : '渠道服务状态已保存。');
    } catch (error) { feedback.textContent = error.message; }
    finally { if (isTask) taskSaving = false; else stateSaving = false; submit.disabled = false; }
  }
  taskForm.addEventListener('submit',event => save(event,'task'));
  stateForm.addEventListener('submit',event => save(event,'state'));
  filters.addEventListener('submit',event => { event.preventDefault(); page = 0; navigate(); });
  $('channelPrev').addEventListener('click',() => { if (page > 0) { page--; navigate(); } });
  $('channelNext').addEventListener('click',() => { page++; navigate(); });
  $('channelNew').addEventListener('click',() => openTask());
  $('channelRefresh').addEventListener('click',() => load());
  $('channelRetry').addEventListener('click',() => load());
  window.addEventListener('popstate',() => { restoreUrl(); load(); });
  restoreUrl(); load();
})();
