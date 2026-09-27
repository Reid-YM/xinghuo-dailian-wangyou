const csrf = document.querySelector('meta[name="_csrf"]')?.content;
const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;
const $ = (selector, root = document) => root.querySelector(selector);
const $$ = (selector, root = document) => [...root.querySelectorAll(selector)];
const esc = (value) => String(value ?? '').replace(/[&<>"']/g, (c) => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
let guideRows = [];
let games = [];
let services = [];
let currentDetail = null;

async function api(url, options = {}) {
  const headers = new Headers(options.headers || {});
  if (!(options.body instanceof FormData)) headers.set('Content-Type', 'application/json');
  if (csrf && csrfHeader && !['GET', 'HEAD'].includes((options.method || 'GET').toUpperCase())) headers.set(csrfHeader, csrf);
  const response = await fetch(url, { ...options, headers });
  if (!response.ok) {
    const text = await response.text();
    throw new Error(text || `请求失败（${response.status}）`);
  }
  if (response.status === 204) return null;
  return response.json();
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
  renderGameOptions();
}

function renderGuideRows() {
  $('#guideCount').textContent = guideRows.length;
  const host = $('#guideRows');
  if (!guideRows.length) {
    host.innerHTML = '<div class="empty">还没有攻略。可先点“从游戏目录批量生成”。</div>';
    return;
  }
  host.innerHTML = guideRows.map((item) => `
    <button class="guide-admin-row ${currentDetail?.guide?.id === item.id ? 'active' : ''}" type="button" data-guide-id="${item.id}">
      <b>${esc(item.title)}</b>
      <small>${esc(item.gameName)} · ${esc(item.status)} · ${item.productCount} 个商品</small>
    </button>`).join('');
  $$('[data-guide-id]', host).forEach((button) => button.addEventListener('click', () => openGuide(Number(button.dataset.guideId))));
}

function renderGameOptions(selected) {
  const select = $('#guideForm select[name="gameCode"]');
  select.innerHTML = games.map((game) => `<option value="${esc(game.code)}" ${game.code === selected ? 'selected' : ''}>${esc(game.name)}（${esc(game.code)}）</option>`).join('');
}

function gameByCode(code) {
  return games.find((game) => game.code === code);
}

function showForm(show) {
  $('#guideForm').classList.toggle('hidden', !show);
  $('#guideEmpty').classList.toggle('hidden', show);
}

function openNew() {
  currentDetail = null;
  const form = $('#guideForm');
  form.reset();
  form.id.value = '';
  form.status.value = 'DRAFT';
  form.estimatedMinutes.value = 60;
  form.sortOrder.value = 100;
  form.reason.value = 'P0-03 新建多游戏攻略';
  renderGameOptions(games[0]?.code);
  $('#editorTitle').textContent = '新建攻略';
  $('#sectionRows').innerHTML = '';
  addSection({ sectionKey: 'service-map', heading: '当前可接服务', bodyMarkdown: '在这里填写当前游戏可接的服务、适用人群与目标。', sortOrder: 10 });
  addSection({ sectionKey: 'route', heading: '推荐推进顺序', bodyMarkdown: '写明先从哪一步开始，再推进什么目标，以及需要客户提前准备的信息。', sortOrder: 20 });
  addSection({ sectionKey: 'safety', heading: '交付与账号安全', bodyMarkdown: '说明网页不收账号密码、验证码或令牌，以及担保交易和异常处理方式。', sortOrder: 30 });
  renderProductPicker([]);
  showForm(true);
  renderGuideRows();
}

async function openGuide(id) {
  const detail = await api(`/api/admin/guides/${id}`);
  currentDetail = detail;
  fillForm(detail);
  showForm(true);
  renderGuideRows();
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
  form.reason.value = 'P0-03 多游戏攻略维护';
  $('#editorTitle').textContent = `编辑：${guide.title}`;
  $('#sectionRows').innerHTML = '';
  (detail.sections || []).forEach(addSection);
  renderProductPicker(detail.products || []);
}

function addSection(section = {}) {
  const host = $('#sectionRows');
  const row = document.createElement('div');
  row.className = 'guide-section-editor';
  row.innerHTML = `
    <div class="guide-section-editor-head"><b>${esc(section.heading || '新段落')}</b><button class="btn small-btn danger" type="button" data-remove-section>删除</button></div>
    <input data-section-key placeholder="段落 key" value="${esc(section.sectionKey || '')}"/>
    <input data-section-heading placeholder="段落标题" value="${esc(section.heading || '')}"/>
    <textarea data-section-body placeholder="段落正文">${esc(section.bodyMarkdown || '')}</textarea>
    <input data-section-image placeholder="图片 URL（可空）" value="${esc(section.imageUrl || '')}"/>
    <input data-section-alt placeholder="图片说明（可空）" value="${esc(section.imageAlt || '')}"/>
    <input data-section-sort type="number" min="0" value="${Number(section.sortOrder || (host.children.length + 1) * 10)}" placeholder="排序"/>
  `;
  $('[data-remove-section]', row).addEventListener('click', () => row.remove());
  host.appendChild(row);
}

function renderProductPicker(selectedProducts) {
  const gameCode = $('#guideForm select[name="gameCode"]').value;
  const selected = new Set((selectedProducts || []).map((item) => Number(item.serviceId)));
  const rows = services.filter((service) => service.gameCode === gameCode);
  $('#productRows').innerHTML = rows.map((service) => `
    <label class="guide-product-option">
      <input type="checkbox" value="${service.id}" ${selected.has(Number(service.id)) ? 'checked' : ''}/>
      <span><b>${esc(service.name)}</b><small>${esc(service.shortDesc || service.slug || '')}</small></span>
    </label>`).join('') || '<div class="empty">当前游戏暂无在售服务。</div>';
}

function collectSections() {
  return $$('#sectionRows .guide-section-editor').map((row, index) => ({
    sectionKey: $('[data-section-key]', row).value.trim() || `section-${index + 1}`,
    heading: $('[data-section-heading]', row).value.trim(),
    bodyMarkdown: $('[data-section-body]', row).value,
    imageUrl: $('[data-section-image]', row).value.trim() || null,
    imageAlt: $('[data-section-alt]', row).value.trim() || null,
    sortOrder: Number($('[data-section-sort]', row).value || (index + 1) * 10)
  })).filter((section) => section.heading);
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
    reason: form.reason.value.trim() || 'P0-03 多游戏攻略维护'
  };
}

async function saveGuide(event) {
  event.preventDefault();
  const form = $('#guideForm');
  const id = form.id.value;
  const payload = collectPayload();
  const saved = id ? await api(`/api/admin/guides/${id}`, { method: 'PUT', body: JSON.stringify(payload) }) : await api('/api/admin/guides', { method: 'POST', body: JSON.stringify(payload) });
  currentDetail = saved;
  await loadAll();
  fillForm(saved);
  alert('攻略已保存');
}

async function archiveGuide() {
  const id = $('#guideForm').id.value;
  if (!id || !confirm('确认归档这篇攻略？前台将不再展示。')) return;
  await api(`/api/admin/guides/${id}`, { method: 'DELETE' });
  currentDetail = null;
  showForm(false);
  await loadAll();
}

async function importGames() {
  const result = await api('/api/admin/guides/import-games', { method: 'POST', body: JSON.stringify({ publish: true, refresh: false, reason: 'P0-03 从游戏目录生成多游戏攻略' }) });
  alert(`已处理 ${result.totalCandidates} 款候选游戏：新增 ${result.created}，刷新 ${result.refreshed}，跳过 ${result.skipped}`);
  await loadAll();
}

async function boot() {
  try {
    await loadAll();
    showForm(false);
    $('#newGuide').addEventListener('click', openNew);
    $('#reloadGuides').addEventListener('click', loadAll);
    $('#importGuides').addEventListener('click', () => importGames().catch((error) => alert(error.message)));
    $('#archiveGuide').addEventListener('click', () => archiveGuide().catch((error) => alert(error.message)));
    $('#addSection').addEventListener('click', () => addSection());
    $('#guideForm').addEventListener('submit', (event) => saveGuide(event).catch((error) => alert(error.message)));
    $('#guideForm select[name="gameCode"]').addEventListener('change', () => renderProductPicker([]));
  } catch (error) {
    alert(`加载攻略后台失败：${error.message}`);
  }
}
boot();