const xingweiGuideMode = document.body.dataset.xingweiMode === 'true';
let guideWritable = !xingweiGuideMode;
let guideBusy = false;
let guideDirty = false;
const guideRevisions = new Map();
const csrf = document.querySelector('meta[name="_csrf"]')?.content;
const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;
const $ = (selector, root = document) => root.querySelector(selector);
const $$ = (selector, root = document) => [...root.querySelectorAll(selector)];
const esc = (value) => String(value ?? '').replace(/[&<>"']/g, (c) => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
let guideRows = [];
let games = [];
let services = [];
let currentDetail = null;
let selectedGuideIds = new Set();
let guideFilters = { query: '', game: '', status: '' };
const COMPONENTS = {
  title: { label: '标题', heading: '标题', body: '用于章节标题或短引导。' },
  paragraph: { label: '段落', heading: '正文段落', body: '填写正文内容，可使用简单列表和加粗。' },
  image: { label: '图片', heading: '图片', body: '填写图片说明，并在图片地址中填入图片 URL。' },
  gallery: { label: '图集', heading: '图集', body: '每行一张图：图片URL | 图片说明' },
  video: { label: '视频', heading: '视频', body: '填写视频地址；支持 mp4/webm 或可嵌入的 HTTPS 地址。' },
  steps: { label: '步骤', heading: '步骤', body: '1. 第一步\n2. 第二步\n3. 第三步' },
  tip: { label: '提示', heading: '提示', body: '填写注意事项、风险提示或运营提醒。' },
  table: { label: '表格', heading: '表格', body: '列1 | 列2 | 列3\n内容1 | 内容2 | 内容3' },
  attachment: { label: '附件', heading: '附件', body: '每行一个附件：名称 | 下载地址' },
  map: { label: '地图', heading: '地图', body: '填写地图/坐标/路线说明或地图链接。' },
  related: { label: '关联内容', heading: '关联内容', body: '每行一个关联：标题 | 链接 | 说明' }
};

async function api(url, options = {}) {
  const headers = new Headers(options.headers || {});
  const method = String(options.method || 'GET').toUpperCase();
  if (!(options.body instanceof FormData)) headers.set('Content-Type', 'application/json');
  if (csrf && csrfHeader && !['GET', 'HEAD'].includes(method)) headers.set(csrfHeader, csrf);
  if (xingweiGuideMode && !['GET','HEAD'].includes(method)) {
    if (!guideWritable) throw new Error('当前账号只读；全站攻略写入需要内容管理权限与全部数据范围');
    if (/^\/api\/admin\/guides\/\d+(?:\/status)?$/.test(url)) {
      const revision = guideRevisions.get(url.replace(/\/status$/, ''));
      if (!revision) throw new Error('请先重新读取这篇攻略再保存');
      headers.set('If-Match', revision);
    }
  }
  const endpoint = !xingweiGuideMode ? url : url === '/api/admin/catalog' ? '/api/admin/xingwei/guides/catalog' : url === '/api/admin/services' ? '/api/admin/xingwei/guides/services' : url.replace(/^\/api\/admin\/guides/, '/api/admin/xingwei/guides');
  const response = await fetch(endpoint, { ...options, headers, cache: 'no-store' });
  if (!response.ok) {
    const text = await response.text();
    let detail = text; try { const data = JSON.parse(text); detail = data.error || data.message || text; } catch (_) {}
    throw new Error(detail || `请求失败（${response.status}）`);
  }
  if (response.status === 204) return null;
  const payload = await response.json();
  if (xingweiGuideMode) {
    const revision = response.headers?.get?.('ETag');
    if (revision) { guideRevisions.set(url, revision); if (payload?.guide?.id) guideRevisions.set('/api/admin/guides/' + payload.guide.id, revision); }
  }
  return payload;
}

function applyGuidePermissions() {
  if (!xingweiGuideMode) return;
  const disabled = !guideWritable || guideBusy;
  for (const id of ['newGuide','importGuides','archiveAllGuides','addSection']) { const node = $('#' + id); if (node) node.disabled = disabled; }
  $$('#guideForm input,#guideForm select,#guideForm textarea,#guideForm button').forEach(node => {node.disabled = node.id === 'previewGuide' ? false : disabled;});
  $$('#guideRows .guide-admin-row-main').forEach(node => {node.disabled = guideBusy;});
  if ($('#batchDeleteGuides')) $('#batchDeleteGuides').disabled = disabled || selectedGuideIds.size === 0;
  $$('#guideFilters input,#guideFilters select,#guideFilters button').forEach(node => {node.disabled = guideBusy;});
  $$('#guideRows input[data-guide-check],#guideSelectAll').forEach(node => {node.disabled = guideBusy;});
  if ($('#reloadGuides')) $('#reloadGuides').disabled = guideBusy;
  const state = $('#xingweiGuideState'); if (state) state.textContent = guideBusy ? '正在处理，请稍候…' : guideWritable ? '可编辑全站攻略；保存会核对当前版本。' : '当前账号只读；编辑全站内容需要内容管理权限与全部数据范围。';
}
async function runGuideWrite(action) {
  if (xingweiGuideMode && (guideBusy || !guideWritable)) return;
  if (xingweiGuideMode) {guideBusy = true;applyGuidePermissions();}
  try {await action();} catch(error) {alert(error.message);}
  finally {if (xingweiGuideMode) {guideBusy = false;applyGuidePermissions();}}
}

async function loadAll() {
  const [rows, catalog, serviceRows] = await Promise.all([
    api('/api/admin/guides'),
    api('/api/admin/catalog'),
    api('/api/admin/services')
  ]);
  guideRows = Array.isArray(rows) ? rows : [];
  games = Array.isArray(catalog?.games) ? catalog.games : [];
  services = Array.isArray(serviceRows) ? serviceRows : [];
  renderGuideRows();
  renderGameOptions(currentDetail?.guide?.gameCode);
  const filter=$('#guideGameFilter');if(filter){const selected=filter.value;filter.replaceChildren(new Option('全部游戏',''),...games.map(game=>new Option(game.name,game.code)));filter.value=selected;}
  applyGuidePermissions();
}

function visibleGuides() {
  const { query, game, status } = guideFilters;
  return guideRows.filter(row =>
    (!query || [row.title, row.summary, row.gameName].join(' ').toLocaleLowerCase('zh-CN').includes(query)) &&
    (!game || row.gameCode === game) && (!status || row.status === status));
}
function applyGuideFilters() {
  if (guideBusy) return;
  guideFilters = {
    query: $('#guideQuery').value.trim().toLocaleLowerCase('zh-CN'),
    game: $('#guideGameFilter').value,
    status: $('#guideStatusFilter').value
  };
  selectedGuideIds.clear();
  renderGuideRows();
  const url = new URL(location.href);
  for (const [key, id] of [['q','guideQuery'], ['game','guideGameFilter'], ['status','guideStatusFilter']]) {
    const value = $('#' + id).value.trim();
    if (value) url.searchParams.set(key, value);
    else url.searchParams.delete(key);
  }
  history.replaceState(null, '', url);
}
async function reloadGuides() {
  if (guideBusy || (xingweiGuideMode && guideDirty && !confirm('放弃草稿并重新读取？'))) return;
  guideBusy = true;
  applyGuidePermissions();
  try {
    await loadAll();
    if (currentDetail?.guide?.id) {
      const detail = await api('/api/admin/guides/' + currentDetail.guide.id);
      currentDetail = detail;
      fillForm(detail);
    } else {
      showForm(false);
    }
    guideDirty = false;
  } catch (error) {
    alert(error.message);
  } finally {
    guideBusy = false;
    applyGuidePermissions();
  }
}
function renderGuideRows() {
  $('#guideCount').textContent = guideRows.length;
  const rows=visibleGuides();if($('#guideFilterCount'))$('#guideFilterCount').textContent=`共 ${guideRows.length} 篇，当前筛选 ${rows.length} 篇`;
  const host = $('#guideRows');
  if (!rows.length) {
    host.innerHTML = guideRows.length?'<div class="empty">没有符合筛选条件的攻略，请调整条件。</div>':'<div class="empty">还没有攻略。可先新建一篇草稿。</div>';
    selectedGuideIds.clear();
    syncSelection();
    return;
  }
  host.innerHTML = rows.map((item) => `
    <div class="guide-admin-row ${currentDetail?.guide?.id === item.id ? 'active' : ''}">
      <label class="guide-admin-row-check" title="选择这篇攻略">
        <input type="checkbox" data-guide-check="${item.id}" ${selectedGuideIds.has(item.id) ? 'checked' : ''}/>
      </label>
      <button class="guide-admin-row-main" type="button" data-guide-id="${item.id}">
        <b>${esc(item.title)}</b>
        <small>${esc(item.gameName)} · ${esc(({DRAFT:'草稿',PUBLISHED:'已发布',ARCHIVED:'已归档'})[item.status]||item.status)} · ${item.productCount} 个关联服务</small>
      </button>
    </div>`).join('');
  $$('[data-guide-check]', host).forEach((input) => input.addEventListener('change', () => {
    const id = Number(input.dataset.guideCheck);
    if (input.checked) selectedGuideIds.add(id);
    else selectedGuideIds.delete(id);
    syncSelection();
  }));
  $$('.guide-admin-row-main[data-guide-id]', host).forEach((button) => button.addEventListener('click', () => openGuide(Number(button.dataset.guideId))));
  syncSelection();
}

function renderGameOptions(selected) {
  const select = $('#guideForm select[name="gameCode"]');
  select.innerHTML = games.map((game) => `<option value="${esc(game.code)}" ${game.code === selected ? 'selected' : ''}>${esc(game.name)}</option>`).join('');
}

function syncSelection() {
  const visible=visibleGuides(),total = visible.length;
  const selected = [...selectedGuideIds].filter((id) => visible.some((row) => row.id === id));
  selectedGuideIds = new Set(selected);
  $$('#guideRows input[data-guide-check]').forEach((input) => {
    input.checked = selectedGuideIds.has(Number(input.dataset.guideCheck));
  });
  const selectAll = $('#guideSelectAll');
  if (selectAll) {
    selectAll.checked = total > 0 && selected.length === total;
    selectAll.indeterminate = selected.length > 0 && selected.length < total;
  }
  const count = $('#guideSelectedCount');
  if (count) count.textContent = selected.length;
  const button = $('#batchDeleteGuides');
  if (button) button.disabled = selected.length === 0 || (xingweiGuideMode && (!guideWritable || guideBusy));
}

function gameByCode(code) {
  return games.find((game) => game.code === code);
}

function showForm(show) {
  $('#guideForm').classList.toggle('hidden', !show);
  $('#guideEmpty').classList.toggle('hidden', show);
  applyGuidePermissions();
}

function openNew() {
  if (xingweiGuideMode && (guideBusy || !guideWritable || (guideDirty && !confirm('放弃未保存的攻略输入并新建？')))) return;
  guideDirty = false;
  currentDetail = null;
  const form = $('#guideForm');
  form.reset();
  form.id.value = '';
  form.status.value = 'DRAFT';
  form.estimatedMinutes.value = 60;
  form.sortOrder.value = 100;
  form.reason.value = '新建攻略';
  renderGameOptions(games[0]?.code);
  $('#editorTitle').textContent = '新建攻略';
  $('#sectionRows').innerHTML = '';
  addSection({ sectionKey: 'paragraph', heading: '当前可接服务', bodyMarkdown: '在这里填写当前游戏可接的服务、适用人群与目标。', sortOrder: 10 });
  addSection({ sectionKey: 'steps', heading: '推荐推进顺序', bodyMarkdown: '1. 写明先从哪一步开始。\n2. 说明后续推进目标。\n3. 列出客户需要提前准备的信息。', sortOrder: 20 });
  addSection({ sectionKey: 'tip', heading: '交付与账号安全', bodyMarkdown: '网页不收账号密码、验证码或令牌；具体交付方式只通过客服确认。', sortOrder: 30 });
  renderProductPicker([]);
  showForm(true);
  renderGuideRows();
}

async function openGuide(id) {
  if (xingweiGuideMode && (guideBusy || (guideDirty && !confirm('放弃未保存的攻略输入并切换？')))) return;
  guideBusy = true;
  applyGuidePermissions();
  try {
    const detail = await api(`/api/admin/guides/${id}`);
    currentDetail = detail;
    fillForm(detail);
    guideDirty = false;
    showForm(true);
    renderGuideRows();
  } catch (error) {
    alert(error.message);
  } finally {
    guideBusy = false;
    applyGuidePermissions();
  }
}

function fillForm(detail) {
  const guide = detail.guide;
  const form = $('#guideForm');
  form.reset();
  form.id.value = guide.id ?? '';
  renderGameOptions(guide.gameCode);
  form.gameCode.value = guide.gameCode || '';
  form.status.value = guide.status || 'DRAFT';
  form.title.value = guide.title || '';
  form.subtitle.value = guide.subtitle || '';
  form.summary.value = guide.summary || '';
  form.coverImage.value = guide.coverImage || '';
  form.difficulty.value = guide.difficulty || 'BEGINNER';
  form.estimatedMinutes.value = guide.estimatedMinutes || 60;
  form.tags.value = (guide.tags || []).join(', ');
  form.sortOrder.value = guide.sortOrder ?? 100;
  form.seoTitle.value = guide.seoTitle || '';
  form.seoDescription.value = guide.seoDescription || '';
  form.featured.checked = Boolean(guide.featured);
  form.reason.value = '攻略内容维护';
  $('#editorTitle').textContent = `编辑：${guide.title}`;
  $('#sectionRows').innerHTML = '';
  (detail.sections || []).forEach(addSection);
  renderProductPicker(detail.products || []);
}

function addSection(section = {}) {
  const host = $('#sectionRows');
  const type = section.sectionKey || $('#sectionType')?.value || 'paragraph';
  const preset = COMPONENTS[type] || COMPONENTS.paragraph;
  const row = document.createElement('div');
  row.className = 'guide-section-editor';
  row.innerHTML = `
    <div class="guide-section-editor-head">
      <b>${esc(section.heading || preset.heading || '新组件')}</b>
      <div class="row-actions">
        <button class="btn small-btn ghost" type="button" data-move-section="up">上移</button>
        <button class="btn small-btn ghost" type="button" data-move-section="down">下移</button>
        <button class="btn small-btn ghost" type="button" data-copy-section>复制</button>
        <button class="btn small-btn danger" type="button" data-remove-section>删除</button>
      </div>
    </div>
    <label class="field"><span>组件类型</span><select data-section-key>${Object.entries(COMPONENTS).map(([key, meta]) => `<option value="${esc(key)}" ${key === type ? 'selected' : ''}>${esc(meta.label)}</option>`).join('')}</select></label>
    <input data-section-heading placeholder="组件标题" value="${esc(section.heading || preset.heading || '')}"/>
    <textarea data-section-body placeholder="组件内容">${esc(section.bodyMarkdown || preset.body || '')}</textarea>
    <input data-section-image placeholder="图片 / 封面地址（可空）" value="${esc(section.imageUrl || '')}"/>
    <input data-section-alt placeholder="图片说明（可空）" value="${esc(section.imageAlt || '')}"/>
    <input data-section-sort type="hidden" value="${Number(section.sortOrder || (host.children.length + 1) * 10)}"/>
  `;
  const syncTitle = () => {
    const heading = $('[data-section-heading]', row)?.value || COMPONENTS[$('[data-section-key]', row)?.value]?.label || '组件';
    $('.guide-section-editor-head b', row).textContent = heading;
  };
  $('[data-remove-section]', row).addEventListener('click', () => row.remove());
  $('[data-copy-section]', row).addEventListener('click', () => {
    const clone = sectionFromRow(row, host.children.length);
    clone.heading = `${clone.heading || '组件'}（副本）`;
    addSection(clone);
  });
  $$('[data-move-section]', row).forEach((button) => button.addEventListener('click', () => {
    if (button.dataset.moveSection === 'up' && row.previousElementSibling) host.insertBefore(row, row.previousElementSibling);
    if (button.dataset.moveSection === 'down' && row.nextElementSibling) host.insertBefore(row.nextElementSibling, row);
  }));
  $('[data-section-heading]', row).addEventListener('input', syncTitle);
  $('[data-section-key]', row).addEventListener('change', () => {
    const selected = COMPONENTS[$('[data-section-key]', row).value] || COMPONENTS.paragraph;
    const body = $('[data-section-body]', row);
    if (!body.value.trim()) body.value = selected.body || '';
    syncTitle();
  });
  host.appendChild(row);
}

function sectionFromRow(row, index) {
  return {
    sectionKey: $('[data-section-key]', row).value.trim() || `section-${index + 1}`,
    heading: $('[data-section-heading]', row).value.trim(),
    bodyMarkdown: $('[data-section-body]', row).value,
    imageUrl: $('[data-section-image]', row).value.trim() || null,
    imageAlt: $('[data-section-alt]', row).value.trim() || null,
    sortOrder: (index + 1) * 10
  };
}

function renderProductPicker(selectedProducts) {
  const gameCode = $('#guideForm select[name="gameCode"]').value;
  const selected = new Set((selectedProducts || []).map((item) => Number(item.serviceId)));
  const rows = services.filter((service) => service.gameCode === gameCode);
  $('#productRows').innerHTML = rows.map((service) => `
    <label class="guide-product-option">
      <input type="checkbox" value="${service.id}" ${selected.has(Number(service.id)) ? 'checked' : ''}/>
      <span><b>${esc(service.name)}</b><small>${esc(service.shortDesc || '已关联服务')}${xingweiGuideMode && service.active === false ? ' · 未启用' : ''}</small></span>
    </label>`).join('') || '<div class="empty">当前游戏暂无在售服务。</div>';
}

function collectSections() {
  return $$('#sectionRows .guide-section-editor').map(sectionFromRow).filter((section) => section.heading);
}

function collectProducts() {
  const checked = new Set($$('#productRows input[type="checkbox"]:checked').map((input) => Number(input.value)));
  return services.filter((service) => checked.has(Number(service.id))).map((service, index) => ({
    serviceId: Number(service.id),
    label: service.name,
    description: service.shortDesc || null,
    imageUrl: null,
    featured: index < 2,
    sortOrder: (index + 1) * 10
  }));
}

function collectPayload() {
  const form = $('#guideForm');
  return {
    gameCode: form.gameCode.value,
    slug: currentDetail?.guide?.slug || null,
    title: form.title.value.trim(),
    subtitle: form.subtitle.value.trim() || null,
    summary: form.summary.value.trim(),
    coverImage: form.coverImage.value.trim() || gameByCode(form.gameCode.value)?.coverImage || null,
    difficulty: form.difficulty.value,
    estimatedMinutes: Number(form.estimatedMinutes.value || 60),
    language: 'zh-CN',
    authorName: currentDetail?.guide?.authorName || '星火攻略组',
    sourceName: currentDetail?.guide?.sourceName || gameByCode(form.gameCode.value)?.publisher || null,
    sourceUrl: currentDetail?.guide?.sourceUrl || null,
    status: form.status.value,
    featured: form.featured.checked,
    sortOrder: Number(form.sortOrder.value || 100),
    tags: form.tags.value.split(/[,，]/).map((tag) => tag.trim()).filter(Boolean),
    seoTitle: form.seoTitle.value.trim() || null,
    seoDescription: form.seoDescription.value.trim() || null,
    canonicalUrl: currentDetail?.guide?.canonicalUrl || null,
    sections: collectSections(),
    products: collectProducts(),
    faqs: currentDetail?.faqs?.length ? currentDetail.faqs : [
      { question: '现在可以直接下单吗？', answer: '提交需求后由客服确认区服、进度、目标与开工时间，确认前不会承诺立即履约。', sortOrder: 10 },
      { question: '需要提供账号密码吗？', answer: '不需要。网页不收集账号密码、验证码或令牌，具体交付方式只通过客服确认。', sortOrder: 20 }
    ],
    relatedGuideIds: (currentDetail?.relatedGuides || []).map((item) => item.id),
    reason: form.reason.value.trim() || '攻略内容维护'
  };
}

async function saveGuide(event) {
  event.preventDefault();
  const form = $('#guideForm');
  const id = form.id.value;
  const payload = collectPayload();
  const saved = id ? await api(`/api/admin/guides/${id}`, { method: 'PUT', body: JSON.stringify(payload) }) : await api('/api/admin/guides', { method: 'POST', body: JSON.stringify(payload) });
  currentDetail = saved;
  guideDirty = false;
  if (xingweiGuideMode) {fillForm(saved);try {await loadAll();} catch(error) {alert('攻略已保存，但列表刷新失败：' + error.message);return;}} else {await loadAll();}
  fillForm(saved);
  alert('攻略已保存');
}

function previewGuide() {
  if (xingweiGuideMode && currentDetail?.guide?.id) {window.open('/admin/xingwei/guides/' + currentDetail.guide.id + '/preview', '_blank', 'noopener');return;}
  const slug = currentDetail?.guide?.slug;
  if (!slug) {
    alert('请先保存攻略，再打开前台预览。');
    return;
  }
  window.open(`/guides/${encodeURIComponent(slug)}`, '_blank', 'noopener');
}

async function archiveGuide() {
  const id = $('#guideForm').id.value;
  if (!id || !confirm('确认归档这篇攻略？前台将不再展示。')) return;
  await api(`/api/admin/guides/${id}`, { method: 'DELETE' });
  selectedGuideIds.delete(Number(id));
  currentDetail = null;
  showForm(false);
  await loadAll();
}

async function batchDeleteGuides() {
  const ids = [...selectedGuideIds];
  if (!ids.length) return;
  if (!confirm(`确认删除选中的 ${ids.length} 篇攻略？\n\n删除后前台将不再展示，订单和询价记录不受影响。`)) return;
  const result = await api('/api/admin/guides/batch', { method: 'DELETE', body: JSON.stringify({ ids, reason: '后台批量删除攻略' }) });
  selectedGuideIds.clear();
  if (currentDetail && ids.includes(currentDetail.guide.id)) {
    currentDetail = null;
    showForm(false);
  }
  await loadAll();
  alert(`已删除 ${result.archived} 篇，跳过 ${result.skipped} 篇。`);
}

async function archiveAllGuides() {
  const total = guideRows.length;
  if (!total) return;
  if (!confirm(`确认归档全部 ${total} 篇攻略？\n\n全部归档后前台不再展示任何攻略，订单和询价记录不受影响。`)) return;
  const result = await api('/api/admin/guides/archive-all', { method: 'POST', body: JSON.stringify({ reason: '后台全部归档攻略' }) });
  selectedGuideIds.clear();
  currentDetail = null;
  showForm(false);
  await loadAll();
  alert(`已归档 ${result.archived} 篇，跳过 ${result.skipped} 篇。`);
}

async function importGames() {
  if (xingweiGuideMode && !confirm('确认按当前游戏目录批量生成并发布攻略？')) return;
  const result = await api('/api/admin/guides/import-games', { method: 'POST', body: JSON.stringify({ publish: true, refresh: false, reason: '从游戏目录生成攻略' }) });
  alert(`已处理 ${result.totalCandidates} 款候选游戏：新增 ${result.created}，刷新 ${result.refreshed}，跳过 ${result.skipped}`);
  await loadAll();
}

async function boot() {
  try {
    if (xingweiGuideMode) { const access = await api('/api/admin/xingwei/guides/access'); guideWritable = access.canManage === true; }
    await loadAll();
    showForm(false);
    $('#newGuide').addEventListener('click', openNew);
    $('#reloadGuides').addEventListener('click', reloadGuides);
    const filters = $('#guideFilters');
    if (filters) {
      const params = new URLSearchParams(location.search);
      $('#guideQuery').value = params.get('q') || '';
      $('#guideGameFilter').value = params.get('game') || '';
      $('#guideStatusFilter').value = params.get('status') || '';
      filters.addEventListener('submit', event => { event.preventDefault(); applyGuideFilters(); });
      filters.addEventListener('reset', event => {
        event.preventDefault();
        if (guideBusy) return;
        for (const id of ['guideQuery','guideGameFilter','guideStatusFilter']) $('#' + id).value = '';
        applyGuideFilters();
      });
      applyGuideFilters();
    }
    $('#guideSelectAll').addEventListener('change', (event) => {
      if (event.target.checked) visibleGuides().forEach((row) => selectedGuideIds.add(row.id));
      else selectedGuideIds.clear();
      syncSelection();
    });
    $('#batchDeleteGuides').addEventListener('click', () => runGuideWrite(batchDeleteGuides));
    $('#archiveAllGuides').addEventListener('click', () => runGuideWrite(archiveAllGuides));
    $('#importGuides').addEventListener('click', () => runGuideWrite(importGames));
    $('#archiveGuide').addEventListener('click', () => runGuideWrite(archiveGuide));
    $('#previewGuide').addEventListener('click', previewGuide);
    $('#addSection').addEventListener('click', () => addSection());
    $('#guideForm').addEventListener('submit', (event) => {event.preventDefault();runGuideWrite(() => saveGuide(event));});
    $('#guideForm select[name="gameCode"]').addEventListener('change', () => renderProductPicker([]));
    if (xingweiGuideMode) {
      $('#guideForm').addEventListener('input', () => {guideDirty = true;});
      $('#guideForm').addEventListener('click', event => {if (event.target.closest('[data-remove-section],[data-copy-section],[data-move-section],#addSection')) guideDirty = true;});
    }
    applyGuidePermissions();
  } catch (error) {
    alert(`加载攻略后台失败：${error.message}`);
  }
}
if (xingweiGuideMode) window.addEventListener('beforeunload', event => {if (guideDirty) {event.preventDefault();event.returnValue = '';}});
boot();
