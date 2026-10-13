(() => {
  const csrf = document.querySelector('meta[name=_csrf]')?.content;
  const csrfHeader = document.querySelector('meta[name=_csrf_header]')?.content;
  const $ = (s, r = document) => r.querySelector(s);
  const $$ = (s, r = document) => [...r.querySelectorAll(s)];
  const esc = x => String(x ?? '').replace(/[&<>'"]/g, c => ({
    '&': '&amp;', '<': '&lt;', '>': '&gt;', "'": '&#39;', '"': '&quot;'
  }[c]));
  function replaceAllCompat(value, search, replacement) {
    return String(value ?? '').split(search).join(replacement);
  }

  const ZH = {
    category: { ACTIVITY: '活动任务', WEAPON: '武器升级', CAMO: '迷彩任务', RANK: '排位 / 段位', OTHER: '其他任务' },
    priceMode: { FIXED: '固定价格', RANGE: '区间价格', QUOTE: '人工报价' },
    serviceStatus: { AVAILABLE: '可接单', BUSY: '繁忙 / 排队中', OFFLINE: '已下架' },
    siteStatus: { OPEN: '正常接单', BUSY: '繁忙 / 排队中', CLOSED: '暂停接单' },
    quoteStatus: { NEW: '新询价', CONTACTED: '已联系', QUOTED: '已报价', CONVERTED: '已生成履约订单', CLOSED: '已关闭', CANCELLED: '已取消' },
    orderStatus: { WAITING_CONFIRMATION: '待确认', IN_PROGRESS: '进行中', COMPLETED: '已完成', EXCEPTION: '异常订单' },
    feedbackStatus: { NEW: '待接收', ACCEPTED: '已接收', TESTING: '修改测试中', DONE: '已完成', PAUSED: '暂停' },
    feedbackPriority: { LOW: '低', NORMAL: '普通', HIGH: '高', URGENT: '紧急' },
    slotMode: { AUTO: '自动推荐', MANUAL: '手动指定' },
    action: {
      SERVICE_CREATE: '新增服务项目', SERVICE_UPDATE: '修改服务', SERVICE_OFFLINE: '下架服务项目', SERVICE_RESTORE: '恢复服务项目', GAME_CREATE: '新增游戏', GAME_UPDATE: '更新游戏', GAME_PROFILE_UPDATE: '维护游戏资料', GAME_BULK_UPDATE: '批量维护游戏', GAME_DELETE: '删除游戏', CATEGORY_CREATE: '新增需求类型', CATEGORY_UPDATE: '更新需求类型', CATEGORY_DELETE: '删除需求类型',
      SITE_CONFIG_UPDATE: '修改首页', QUOTE_STATUS_UPDATE: '更新询价状态', QUOTE_CONVERTED: '询价生成履约订单',
      ORDER_CREATE: '新建订单', ORDER_CREATE_FROM_QUOTE: '询价生成订单', ORDER_UPDATE: '更新订单',
      ORDER_IMAGE_UPDATE: '更新进度截图', SLOT_UPDATE: '更新展示位', TEST_FEEDBACK_CREATE: '提交测试反馈', TEST_FEEDBACK_UPDATE: '更新测试反馈', TEST_FEEDBACK_ATTACHMENT_UPDATE: '上传测试反馈附件'
    },
    target: { SERVICE: '服务项目', SITE_CONFIG: '站点设置', QUOTE: '询价线索', ORDER: '履约订单', HOT_SLOT: '首页推荐位', GAME: '游戏', SERVICE_CATEGORY: '需求类型', TEST_FEEDBACK: '测试反馈' }
  };
  const zh = (group, value) => ZH[group]?.[value] || value || '-';
  const iconUse = (key='service') => `<svg class="ui-icon" aria-hidden="true"><use href="/assets/icons.svg#i-${esc(key || 'service')}"></use></svg>`;
  const FEEDBACK_MODULES = [
    { label: '首页', path: '/', areas: ['Hero首屏', '服务卡片', '信任标签', '公告/状态', '询价按钮', '业务记录入口', '底部资质备案', '手机端布局', '页面动效', '加载速度'] },
    { label: '服务大厅', path: '/services', areas: ['服务列表', '服务分类', '服务封面', '价格展示', '详情入口', '空态加载', '手机端卡片', '筛选/搜索'] },
    { label: '服务详情页', path: '/services/{slug}', areas: ['标题文案', '卖点信息', '报价器', '立即询价按钮', '封面图片', '规则说明', '手机端布局'] },
    { label: '询价页', path: '/quote', areas: ['需求选择', '表单字段', '截图上传', '提交成功页', '复制编号', '微信二维码', '淘宝/闲鱼入口', '手机端填写'] },
    { label: '业务记录', path: '/records', areas: ['编号查询', '列表展示', '进度状态', '进度截图', '异常提示', '手机端查询'] },
    { label: '信任/FAQ', path: '/trust', areas: ['资质信息', '案例展示', 'FAQ文案', '支付说明', '安全提醒', '页面排版'] },
    { label: '规则页', path: '/rules', areas: ['红字提醒', '服务规则', '取消退款', '账号安全', '平台合规'] },
    { label: '联系页', path: '/contact', areas: ['微信号', '多个客服', '二维码', '营业时间', '联系按钮', '手机端展示'] },
    { label: '后台总览', path: '/admin', areas: ['统计卡片', '待办提示', '导航菜单', '页面速度', '深色样式'] },
    { label: '后台服务管理', path: '/admin#services', areas: ['新增服务', '编辑服务', '封面上传', '价格字段', '排序/上下架', '游戏资料'] },
    { label: '后台询价线索', path: '/admin#quotes', areas: ['线索列表', '生成订单', '状态变更', '搜索', '备注展示'] },
    { label: '后台履约订单', path: '/admin#orders', areas: ['订单列表', '进度更新', '截图上传', '异常订单', '客户可见说明'] },
    { label: '后台站点设置', path: '/admin#settings', areas: ['品牌信息', 'SEO', '客服配置', '二维码管理', '资质备案'] },
    { label: '后台测试反馈', path: '/admin#feedback', areas: ['反馈提交', '附件上传', '反馈列表', '状态更新', '复制给Codex'] },
    { label: '全站通用', path: '全站', areas: ['字体大小', '按钮样式', '颜色对比', '黑黄视觉', '动效卡顿', '手机端适配', '图片加载', 'SEO/标题', '备案资质', '其他位置'] },
    { label: '其他模块', path: '其他', areas: ['其他位置'] }
  ];
  const FEEDBACK_ISSUES = [
    '布局错位/重叠', '手机端不好看', '字体太大', '字体太小', '按钮不明显', '按钮点不了', '图片不合适', '图片加载慢',
    '页面卡顿', '动效太多', '颜色不协调', '黑黄风格不明显', '文案不准确', '价格/服务信息错误', '缺少入口',
    '表单不好填', '提交失败', '上传失败', '二维码不显示', '链接跳转错误', '备案/资质展示问题', '后台不好用',
    '数据没有刷新', '状态不正确', '需要新增功能', '需要删除内容', '安全/隐私提示问题', 'SEO问题', '其他问题'
  ];

  async function api(url, opt = {}) {
    const isForm = opt.body instanceof FormData;
    const headers = {
      ...(opt.body && !isForm ? { 'Content-Type': 'application/json' } : {}),
      ...(opt.headers || {})
    };
    if (csrf && csrfHeader && !['GET', 'HEAD'].includes((opt.method || 'GET').toUpperCase())) headers[csrfHeader] = csrf;

    const response = await fetch(url, { ...opt, headers });
    if (response.status === 401 || response.status === 403) {
      location.href = '/admin/login';
      throw new Error('登录已失效，请重新登录');
    }
    if (response.status === 204) return null;

    let payload = null;
    try { payload = await response.json(); } catch {}
    if (!response.ok) throw new Error(payload?.message || '请求失败，请稍后重试');
    return payload;
  }

  function toast(message, type = 'success') {
    const area = $('#toastArea');
    const item = document.createElement('div');
    item.className = `toast ${type === 'error' ? 'error' : ''}`;
    item.textContent = message;
    area.appendChild(item);
    setTimeout(() => item.remove(), 2600);
  }

  function setBusy(button, busy, text = '处理中…') {
    if (!button) return;
    if (busy) {
      if (!button.dataset.oldText) button.dataset.oldText = button.textContent;
      button.textContent = text;
      button.disabled = true;
    } else {
      button.textContent = button.dataset.oldText || button.textContent;
      button.disabled = false;
      delete button.dataset.oldText;
    }
  }

  function renderHeroPreview(url) {
    const box = $('#heroImagePreview');
    if (!box) return;
    if (!url) { box.innerHTML = ''; box.classList.add('hidden'); return; }
    box.innerHTML = `<img src="${esc(url)}" alt="首页主图预览">`;
    box.classList.remove('hidden');
  }

  function renderNamedPreview(selector, url, alt = '图片预览') {
    const box = $(selector);
    if (!box) return;
    if (!url) { box.innerHTML = ''; box.classList.add('hidden'); return; }
    box.innerHTML = `<img src="${esc(url)}" alt="${esc(alt)}">`;
    box.classList.remove('hidden');
  }

  function updateHeroBackgroundLivePreview() {
    const f = $('#configForm');
    const box = $('#heroBackgroundLivePreview');
    const layer = box?.querySelector('.admin-hero-bg-live-layer');
    if (!f || !box || !layer) return;
    const url = f.heroBackgroundImage.value || '';
    if (!url) {
      box.classList.add('hidden');
      layer.style.backgroundImage = '';
      return;
    }
    const config = Object.fromEntries(['heroBackgroundOpacity','heroBackgroundFit','heroBackgroundPositionX','heroBackgroundPositionY','heroBackgroundBlur'].map(k => [k, f[k].value]));
    window.SiteImageControls.background(box, layer, config, url);
    box.classList.remove('hidden');
  }

  async function uploadSiteImage(slot, file, hiddenInput, previewSelector, button, label) {
    if (!file) return toast(`请先选择${label}`, 'error');
    const fd = new FormData(); fd.append('file', file);
    setBusy(button, true, '上传中…');
    try {
      const out = await api(`/api/admin/site-config/image/${slot}`, { method: 'POST', body: fd });
      hiddenInput.value = out.url || '';
      renderNamedPreview(previewSelector, hiddenInput.value, label);
      if (slot === 'BACKGROUND') updateHeroBackgroundLivePreview();

      document.dispatchEvent(new CustomEvent('site-image-updated'));
      toast(`${label}已替换，原有调参已保留`);
    } catch (error) { toast(error.message, 'error'); }
    finally { setBusy(button, false); }
  }

  function applyHeroBackgroundAutoFit() {
    const f = $('#configForm');
    if (!f) return;
    f.heroBackgroundOpacity.value = 90;
    f.heroBackgroundFit.value = 'COVER';
    f.heroBackgroundPositionX.value = 50;
    f.heroBackgroundPositionY.value = 50;
    f.heroBackgroundBlur.value = 0;
  }

  async function clearSiteImage(slot, hiddenInput, fileInput, previewSelector, label) {
    try {
      await api(`/api/admin/site-config/image/${slot}`, { method: 'DELETE' });
      hiddenInput.value = ''; if (fileInput) fileInput.value = '';
      renderNamedPreview(previewSelector, '', label);
      if (slot === 'BACKGROUND') updateHeroBackgroundLivePreview();
      document.dispatchEvent(new CustomEvent('site-image-updated'));
      toast(`${label}已移除并立即生效`);
    } catch (error) { toast(error.message, 'error'); }
  }

  let contactChannels = [];
  let trustCases = [];
  let trustFaq = [];

  function normalizeContactChannels(rows = []) {
    const list = Array.isArray(rows) ? rows : [];
    return list.map((x, i) => ({
      label: String(x?.label || `客服${i + 1}`).trim(),
      wechatId: String(x?.wechatId || '').trim(),
      jumpUrl: String(x?.jumpUrl || '').trim(),
      qrImage: String(x?.qrImage || '').trim(),
      note: String(x?.note || '').trim()
    })).filter(x => x.label || x.wechatId || x.jumpUrl || x.qrImage || x.note).slice(0, 6);
  }

  function renderContactChannels(rows = contactChannels) {
    contactChannels = normalizeContactChannels(rows);
    const box = $('#contactChannelEditor');
    const hidden = $('#configForm')?.contactChannels;
    if (hidden) hidden.value = JSON.stringify(contactChannels);
    if (!box) return;
    box.innerHTML = contactChannels.map((row, i) => `<div class="admin-contact-channel" data-contact-channel="${i}">
      <div class="field"><label>客服名称</label><input data-contact-field="label" maxlength="40" value="${esc(row.label)}" placeholder="主客服 / 夜间客服"></div>
      <div class="field"><label>微信号</label><input data-contact-field="wechatId" maxlength="100" value="${esc(row.wechatId)}" placeholder="AAC9878"><small class="form-help">前台显示为一键复制。</small></div>
      <div class="field"><label>微信一键跳转链接（可空）</label><input data-contact-field="jumpUrl" maxlength="500" value="${esc(row.jumpUrl)}" placeholder="https://u.wechat.com/..."><small class="form-help">填二维码解析出的链接后，前台卡片会显示「一键跳转微信」。</small></div>
      <div class="field wide"><label>备注</label><input data-contact-field="note" maxlength="120" value="${esc(row.note)}" placeholder="发送业务编号确认报价"></div>
      <div class="field"><label>二维码</label><input data-contact-field="qrImage" type="hidden" value="${esc(row.qrImage)}"><input accept="image/jpeg,image/png,image/webp,image/*" data-contact-file type="file"></div>
      <div class="admin-contact-channel-preview">${row.qrImage ? `<img src="${esc(row.qrImage)}" alt="${esc(row.label || '客服')}二维码">` : '<span class="muted small">未上传二维码</span>'}</div>
      <div class="admin-contact-channel-actions"><button class="btn small-btn" type="button" data-contact-upload>上传二维码</button><button class="btn small-btn ghost" type="button" data-contact-remove>删除客服</button></div>
    </div>`).join('') || '<div class="empty">还没有多客服。点击“新增客服”开始添加。</div>';
  }

  function syncContactChannelsFromDom() {
    const rows = $$('[data-contact-channel]').map(card => ({
      label: card.querySelector('[data-contact-field="label"]')?.value || '',
      wechatId: card.querySelector('[data-contact-field="wechatId"]')?.value || '',
      jumpUrl: card.querySelector('[data-contact-field="jumpUrl"]')?.value || '',
      qrImage: card.querySelector('[data-contact-field="qrImage"]')?.value || '',
      note: card.querySelector('[data-contact-field="note"]')?.value || ''
    }));
    contactChannels = normalizeContactChannels(rows);
    const hidden = $('#configForm')?.contactChannels;
    if (hidden) hidden.value = JSON.stringify(contactChannels);
    return contactChannels;
  }

  function isWechatJumpValue(value) {
    const v = String(value || '').trim();
    return /^(https?:\/\/|weixin:\/\/|wechat:\/\/)/i.test(v);
  }

  async function decodeQrFromImageFile(file) {
    if (!file || !('BarcodeDetector' in window)) return '';
    try {
      const detector = new BarcodeDetector({ formats: ['qr_code'] });
      const bitmap = await createImageBitmap(file);
      const codes = await detector.detect(bitmap);
      bitmap.close?.();
      return String(codes?.[0]?.rawValue || '').trim();
    } catch {
      return '';
    }
  }

  async function uploadContactChannelQr(card, button) {
    const file = card?.querySelector('[data-contact-file]')?.files?.[0];
    if (!file) return toast('请先选择客服二维码图片', 'error');
    const decoded = await decodeQrFromImageFile(file);
    const fd = new FormData(); fd.append('file', file);
    setBusy(button, true, '上传中…');
    try {
      const out = await api('/api/admin/site-config/image/CONTACT_QR', { method: 'POST', body: fd });
      card.querySelector('[data-contact-field="qrImage"]').value = out.url || '';
      const jumpInput = card.querySelector('[data-contact-field="jumpUrl"]');
      if (jumpInput && isWechatJumpValue(decoded)) jumpInput.value = decoded;
      syncContactChannelsFromDom();
      renderContactChannels(contactChannels);
      toast(decoded && isWechatJumpValue(decoded) ? '客服二维码已上传，并已识别一键跳转链接；保存联系方式模块后前台生效' : '客服二维码已上传；如需点击直达微信，请在“微信一键跳转链接”中粘贴二维码链接后保存');
    } catch (error) { toast(error.message, 'error'); }
    finally { setBusy(button, false); }
  }

  function normalizeTrustCases(rows = []) {
    return (Array.isArray(rows) ? rows : []).map((x, i) => ({
      tag: String(x?.tag || `案例${i + 1}`).trim(),
      title: String(x?.title || '').trim(),
      metric: String(x?.metric || '').trim(),
      detail: String(x?.detail || '').trim()
    })).filter(x => x.tag || x.title || x.metric || x.detail).slice(0, 8);
  }

  function normalizeTrustFaq(rows = []) {
    return (Array.isArray(rows) ? rows : []).map((x, i) => ({
      question: String(x?.question || `问题${i + 1}`).trim(),
      answer: String(x?.answer || '').trim()
    })).filter(x => x.question || x.answer).slice(0, 12);
  }

  function renderTrustCases(rows = trustCases) {
    trustCases = normalizeTrustCases(rows);
    const box = $('#trustCaseEditor');
    const hidden = $('#configForm')?.trustCases;
    if (hidden) hidden.value = JSON.stringify(trustCases);
    if (!box) return;
    box.innerHTML = trustCases.map((row, i) => `<div class="admin-trust-editor-card" data-trust-case="${i}">
      <div class="field"><label>标签</label><input data-trust-case-field="tag" maxlength="40" value="${esc(row.tag)}" placeholder="武器升级"></div>
      <div class="field"><label>案例标题</label><input data-trust-case-field="title" maxlength="100" value="${esc(row.title)}" placeholder="MW4 武器 1→满级配件"></div>
      <div class="field"><label>价格 / 耗时</label><input data-trust-case-field="metric" maxlength="120" value="${esc(row.metric)}" placeholder="约 50–90 分钟 · ¥35–60"></div>
      <div class="field wide"><label>说明</label><textarea data-trust-case-field="detail" maxlength="240">${esc(row.detail)}</textarea></div>
      <button class="btn small-btn ghost" type="button" data-trust-case-remove>删除案例</button>
    </div>`).join('') || '<div class="empty">还没有案例。点击“新增案例”开始配置。</div>';
  }

  function renderTrustFaq(rows = trustFaq) {
    trustFaq = normalizeTrustFaq(rows);
    const box = $('#trustFaqEditor');
    const hidden = $('#configForm')?.trustFaq;
    if (hidden) hidden.value = JSON.stringify(trustFaq);
    if (!box) return;
    box.innerHTML = trustFaq.map((row, i) => `<div class="admin-trust-editor-card faq" data-trust-faq="${i}">
      <div class="field"><label>问题</label><input data-trust-faq-field="question" maxlength="100" value="${esc(row.question)}" placeholder="怎么查进度？"></div>
      <div class="field wide"><label>回答</label><textarea data-trust-faq-field="answer" maxlength="400">${esc(row.answer)}</textarea></div>
      <button class="btn small-btn ghost" type="button" data-trust-faq-remove>删除 FAQ</button>
    </div>`).join('') || '<div class="empty">还没有 FAQ。点击“新增 FAQ”开始配置。</div>';
  }

  function syncTrustCasesFromDom() {
    trustCases = normalizeTrustCases($$('[data-trust-case]').map(card => ({
      tag: card.querySelector('[data-trust-case-field="tag"]')?.value || '',
      title: card.querySelector('[data-trust-case-field="title"]')?.value || '',
      metric: card.querySelector('[data-trust-case-field="metric"]')?.value || '',
      detail: card.querySelector('[data-trust-case-field="detail"]')?.value || ''
    })));
    const hidden = $('#configForm')?.trustCases;
    if (hidden) hidden.value = JSON.stringify(trustCases);
    return trustCases;
  }

  function syncTrustFaqFromDom() {
    trustFaq = normalizeTrustFaq($$('[data-trust-faq]').map(card => ({
      question: card.querySelector('[data-trust-faq-field="question"]')?.value || '',
      answer: card.querySelector('[data-trust-faq-field="answer"]')?.value || ''
    })));
    const hidden = $('#configForm')?.trustFaq;
    if (hidden) hidden.value = JSON.stringify(trustFaq);
    return trustFaq;
  }

  function statusBadge(group, value) {
    const tone = ['COMPLETED', 'AVAILABLE', 'OPEN', 'CONVERTED'].includes(value) ? 'ok'
      : ['EXCEPTION', 'OFFLINE', 'CLOSED', 'CANCELLED'].includes(value) ? 'danger'
      : ['BUSY', 'IN_PROGRESS', 'CONTACTED', 'QUOTED'].includes(value) ? 'warn' : '';
    return `<span class="badge ${tone}">${esc(zh(group, value))}</span>`;
  }

  function yuanFromCents(cents) {
    return (Number(cents || 0) / 100).toFixed(2).replace(/\.00$/, '');
  }

  function centsFromYuan(value) {
    const n = Number(String(value || '').trim());
    if (!Number.isFinite(n) || n < 0) throw new Error('价格必须是大于等于 0 的数字');
    return Math.round(n * 100);
  }

  const loaders = {
    dashboard: loadDashboard,
    config: loadConfig,
    services: loadServices,
    catalog: loadCatalogManager,
    quotes: loadQuotes,
    orders: loadOrders,
    slots: loadSlots,
    feedback: loadFeedback,
    audits: loadAudits,
    users: loadUserPanel,
    employees: loadEmployees,
    community: loadCommunity
  };

  async function show(name, options = {}) {
    $$('[data-panel]').forEach(x => x.classList.add('hidden'));
    $(`[data-panel=${name}]`)?.classList.remove('hidden');
    $$('[data-nav]').forEach(x => x.classList.toggle('active', x.dataset.nav === name && (!options.communityTarget || x.dataset.communityTarget === options.communityTarget)));
    const activeSection = (options.communityTarget ? $(`[data-nav="${name}"][data-community-target="${options.communityTarget}"]`) : $(`[data-nav="${name}"]`))?.closest('.nav-section');
    activeSection?.classList.remove('collapsed');
    if (window.matchMedia('(max-width: 760px)').matches) {
      $$('.nav-section:not(.nav-section-primary)').forEach(section => {
        section.classList.toggle('collapsed', section !== activeSection);
      });
    }
    try { await (loaders[name] || (() => {}))(); } catch (error) { toast(error.message, 'error'); }
    if (name === 'community') activateCommunitySection(options.communityTarget || 'comments');
  }

  
// 行为埋点：转化漏斗。数据来自 /api/admin/tracking/summary（只读聚合）。
async function renderTrackingFunnel() {
  const card = document.getElementById("trackingCard");
  if (!card) return;
  const days = Number(document.getElementById("trackingDays")?.value || 7);
  let d;
  try {
    d = await api("/api/admin/tracking/summary?days=" + days);
  } catch (e) {
    card.hidden = true;
    return;
  }
  if (!d || !Number(d.total || 0)) { card.hidden = true; return; }
  card.hidden = false;

  const sub = document.getElementById("trackingSub");
  if (sub) {
    sub.textContent = "窗口内 " + (d.recent ?? 0) + " 条 / 累计 " + (d.total ?? 0) + " 条"
      + (d.lastEventAt ? "，最近一条 " + d.lastEventAt : "");
  }

  const box = document.getElementById("trackingFunnel");
  if (box) {
    const list = Array.isArray(d.funnel) ? d.funnel : [];
    const top = Math.max(1, ...list.map((x) => Number(x.count || 0)));
    box.textContent = "";
    list.forEach((step) => {
      const row = document.createElement("div");
      row.className = "tracking-step";

      const label = document.createElement("div");
      label.className = "tracking-step-label";
      const b = document.createElement("b");
      b.textContent = step.label || step.event;
      const small = document.createElement("small");
      small.textContent = step.event;
      label.append(b, small);

      const barWrap = document.createElement("div");
      barWrap.className = "tracking-step-bar";
      const bar = document.createElement("i");
      bar.style.width = Math.round((Number(step.count || 0) / top) * 100) + "%";
      barWrap.appendChild(bar);

      const val = document.createElement("div");
      val.className = "tracking-step-value";
      const n = document.createElement("b");
      n.textContent = String(step.count ?? 0);
      const r = document.createElement("small");
      r.textContent = (step.rate ?? 0) + "%";
      val.append(n, r);

      row.append(label, barWrap, val);
      box.appendChild(row);
    });
  }

  const evBox = document.getElementById("trackingEvents");
  if (evBox) {
    const list = Array.isArray(d.events) ? d.events : [];
    evBox.textContent = "";
    list.slice(0, 12).forEach((e) => {
      const cell = document.createElement("div");
      cell.className = "tracking-event";
      const n = document.createElement("b");
      n.textContent = e.event;
      const c = document.createElement("span");
      c.textContent = "窗口内 " + (e.recent ?? 0) + " · 累计 " + (e.total ?? 0);
      cell.append(n, c);
      evBox.appendChild(cell);
    });
  }
}

async function loadDashboard() {
    const d = await api('/api/admin/dashboard');
    const metrics = [
      { label:'服务总数', value:d.totalServices, hint:'当前数据库中的全部服务', nav:'services' },
      { label:'新询价', value:d.newQuotes, hint:'等待客服处理', nav:'quotes', attention:Number(d.newQuotes || 0) > 0 },
      { label:'进行中订单', value:d.inProgressOrders, hint:'需要持续更新进度', nav:'orders', attention:Number(d.inProgressOrders || 0) > 0 },
      { label:'已完成订单', value:d.completedOrders, hint:'累计完成', nav:'orders' },
      { label:'新测试反馈', value:d.newFeedback, hint:'等待 Codex 查看处理', nav:'feedback', attention:Number(d.newFeedback || 0) > 0 },
      { label:'行为埋点', value:d.trackingEvents, hint:'前台有效交互记录' }
    ];
    $('#kpis').innerHTML = metrics.map(x => x.nav
      ? `<button type="button" class="card kpi kpi-action ${x.attention ? 'attention' : ''}" data-nav="${x.nav}" aria-label="打开${esc(x.label)}"><span class="muted">${esc(x.label)}</span><b>${x.value ?? 0}</b><small>${esc(x.hint)}</small><em>进入处理 →</em></button>`
      : `<div class="card kpi"><span class="muted">${esc(x.label)}</span><b>${x.value ?? 0}</b><small>${esc(x.hint)}</small></div>`).join('');
    const focus = [];
    if (Number(d.newQuotes || 0) > 0) focus.push(`${d.newQuotes} 条新询价待处理`);
    if (Number(d.inProgressOrders || 0) > 0) focus.push(`${d.inProgressOrders} 个订单进行中`);
    if (Number(d.newFeedback || 0) > 0) focus.push(`${d.newFeedback} 条测试反馈待查看`);
    const box = $('#dashboardFocusText');
  renderTrackingFunnel();
    if (box) box.textContent = focus.length ? `当前优先：${focus.join(' · ')}` : '当前没有高优先级待处理事项，可以检查服务、展示位和站点配置。';
  }

  async function loadConfig() {
    const c = await api('/api/admin/site-config');
    const f = $('#configForm');
    f.brandName.value = c.brandName || '';
    f.legalEntityName.value = c.legalEntityName || '星维网络服务工作室';
    document.querySelectorAll('[data-admin-brand]').forEach(x => x.textContent = c.brandName || '星火网游代练平台');
    document.title = `运营后台 - ${c.brandName || '星火网游代练平台'}`;
    f.serviceStatus.value = (c.serviceStatus || 'OPEN').toUpperCase();
    f.quoteEnabled.checked = !!c.quoteEnabled;
    f.orderQueryEnabled.checked = !!c.orderQueryEnabled;
    f.heroTitle.value = c.heroTitle;
    f.heroSubtitle.value = c.heroSubtitle;
    f.startingPriceMode.value = (c.startingPriceMode || 'manual').toUpperCase();
    f.startingPrice.value = c.startingPrice;
    f.showStartingPrice.checked = c.showStartingPrice !== false;
    f.homeServiceLimit.value = c.homeServiceLimit || 4;
    f.heroImage.value = c.heroImage || '';
    renderHeroPreview(c.heroImage || '');
    f.heroImageHeight.value = c.heroImageHeight || 150;
    f.heroImageFit.value = (c.heroImageFit || 'cover').toUpperCase();
    f.heroImagePositionX.value = c.heroImagePositionX ?? 50;
    f.heroImagePositionY.value = c.heroImagePositionY ?? 50;
    f.heroBackgroundImage.value = c.heroBackgroundImage || '';
    renderNamedPreview('#heroBackgroundPreview', c.heroBackgroundImage || '', '首屏背景图预览');
    f.heroBackgroundOpacity.value = c.heroBackgroundOpacity ?? 90;
    f.heroBackgroundFit.value = (c.heroBackgroundFit || 'cover').toUpperCase();
    f.heroBackgroundPositionX.value = c.heroBackgroundPositionX ?? 50;
    f.heroBackgroundPositionY.value = c.heroBackgroundPositionY ?? 50;
    f.heroBackgroundBlur.value = c.heroBackgroundBlur ?? 0;
    updateHeroBackgroundLivePreview();
    f.serviceCoverHeight.value = c.serviceCoverHeight || 105;
    f.serviceCoverFit.value = (c.serviceCoverFit || 'cover').toUpperCase();
    f.serviceCoverPositionX.value = c.serviceCoverPositionX ?? 50;
    f.serviceCoverPositionY.value = c.serviceCoverPositionY ?? 50;
    const trustValues = Array.isArray(c.trustTags) ? c.trustTags : [];
    [...f.trustTags.options].forEach(o => { o.selected = trustValues.includes(o.value); });
    renderTrustCases(c.trustCases || []);
    renderTrustFaq(c.trustFaq || []);
    f.announcementEnabled.checked = !!c.announcementEnabled;
    f.announcementText.value = c.announcementText || '';
    f.contactTitle.value = c.contactTitle || '';
    f.contactIntro.value = c.contactIntro || '';
    f.wechatId.value = c.wechatId || '';
    f.wechatQrImage.value = c.wechatQrImage || '';
    renderNamedPreview('#wechatQrPreview', c.wechatQrImage || '', '微信二维码预览');
    f.wechatQrSize.value = c.wechatQrSize || 260;
    renderContactChannels(c.contactChannels || []);
    f.qqId.value = c.qqId || '';
    f.qqLink.value = c.qqLink || '';
    f.phone.value = c.phone || '';
    const hoursValue = c.serviceHours || '09:00-22:00';
    if (f.serviceHours && ![...f.serviceHours.options].some(o => o.value === hoursValue)) { const o=document.createElement('option'); o.value=hoursValue; o.textContent=hoursValue+'（历史值）'; f.serviceHours.appendChild(o); }
    f.serviceHours.value = hoursValue;
    f.taobaoUrl.value = c.taobaoUrl || '';
    f.xianyuUrl.value = c.xianyuUrl || '';
    f.contactNotice.value = c.contactNotice || '';
    f.wecomKefuUrl.value = c.wecomKefuUrl || '';
    f.wecomKefuNote.value = c.wecomKefuNote || '';
    f.quickContactTitle.value = c.quickContactTitle || '';
    f.taobaoLabel.value = c.taobaoLabel || '';
    f.xianyuLabel.value = c.xianyuLabel || '';
    f.wecomKefuLabel.value = c.wecomKefuLabel || '';
    f.wecomKefuButton.value = c.wecomKefuButton || '';
    f.quoteSuccessText.value = c.quoteSuccessText || '';
    f.orderHelpText.value = c.orderHelpText || '';
    f.footerText.value = c.footerText || '';
    f.seoTitle.value = c.seoTitle || '';
    f.seoDescription.value = c.seoDescription || '';
    document.dispatchEvent(new CustomEvent('site-config-loaded'));
  }

  $('#uploadHeroImage')?.addEventListener('click', event => {
    const f = $('#configForm');
    return uploadSiteImage('HERO', f?.heroImageFile?.files?.[0], f.heroImage, '#heroImagePreview', event.currentTarget, '首页主图');
  });
  $('#clearHeroImage')?.addEventListener('click', () => { const f=$('#configForm'); return clearSiteImage('HERO', f.heroImage, f.heroImageFile, '#heroImagePreview', '首页主图'); });
  $('#uploadHeroBackground')?.addEventListener('click', event => {
    const f = $('#configForm');
    return uploadSiteImage('BACKGROUND', f?.heroBackgroundImageFile?.files?.[0], f.heroBackgroundImage, '#heroBackgroundPreview', event.currentTarget, '首屏背景图');
  });
  $('#clearHeroBackground')?.addEventListener('click', () => { const f=$('#configForm'); return clearSiteImage('BACKGROUND', f.heroBackgroundImage, f.heroBackgroundImageFile, '#heroBackgroundPreview', '首屏背景图'); });
  $('#autoFitHeroBackground')?.addEventListener('click', () => {
    const f = $('#configForm');
    if (!f?.heroBackgroundImage?.value) return toast('请先上传首页首屏背景图', 'error');
    applyHeroBackgroundAutoFit();
    updateHeroBackgroundLivePreview();
    f.heroBackgroundOpacity.dispatchEvent(new Event('input', { bubbles: true }));
    toast('已套用前台自动适配参数，保存图片视觉模块后生效');
  });
  ['heroBackgroundOpacity','heroBackgroundFit','heroBackgroundPositionX','heroBackgroundPositionY','heroBackgroundBlur'].forEach(name => {
    $('#configForm')?.[name]?.addEventListener('input', updateHeroBackgroundLivePreview);
    $('#configForm')?.[name]?.addEventListener('change', updateHeroBackgroundLivePreview);
  });
  $('#uploadWechatQr')?.addEventListener('click', async event => {
    const f = $('#configForm');
    const file = f?.wechatQrImageFile?.files?.[0];
    const decoded = await decodeQrFromImageFile(file);
    if (f?.wechatId && isWechatJumpValue(decoded)) f.wechatId.value = decoded;
    return uploadSiteImage('WECHAT_QR', file, f.wechatQrImage, '#wechatQrPreview', event.currentTarget, '微信二维码');
  });
  $('#clearWechatQr')?.addEventListener('click', () => { const f=$('#configForm'); return clearSiteImage('WECHAT_QR', f.wechatQrImage, f.wechatQrImageFile, '#wechatQrPreview', '微信二维码'); });

  $$('[data-config-nav]').forEach(button => button.addEventListener('click', () => {
    const key = button.dataset.configNav; activeConfigSection = key;
    $$('[data-config-nav]').forEach(x => x.classList.toggle('active', x === button));
    $$('[data-config-section]').forEach(x => x.classList.toggle('hidden', x.dataset.configSection !== key));
    const title=$('#configSaveTitle'); if(title) title.textContent=`保存当前模块：${configSectionLabels[key] || key}`;
    if (key === 'gameStatus') loadGameStatus();
  }));

  let activeConfigSection = 'basic';
  const configSectionLabels = { gameStatus:'游戏运营状态', basic:'基础状态', home:'首页内容', visual:'图片视觉', notice:'公告', contact:'联系方式', trust:'信任FAQ', copy:'业务文案', seo:'搜索展示' };

  function configSectionPayload(section) {
    const f=$('#configForm'); const reason=$('#configSectionReason')?.value || '后台分区保存';
    const common={reason};
    if(section==='basic') return {...common,brandName:f.brandName.value,legalEntityName:f.legalEntityName.value,serviceStatus:f.serviceStatus.value,quoteEnabled:f.quoteEnabled.checked,orderQueryEnabled:f.orderQueryEnabled.checked};
    if(section==='home') return {...common,heroTitle:f.heroTitle.value,heroSubtitle:f.heroSubtitle.value,startingPriceMode:f.startingPriceMode.value,startingPrice:Number(f.startingPrice.value||0),showStartingPrice:f.showStartingPrice.checked,homeServiceLimit:Number(f.homeServiceLimit.value||4),trustTags:[...f.trustTags.selectedOptions].map(x=>x.value)};
    if(section==='visual') return {...common,heroImage:f.heroImage.value||null,heroImageHeight:Number(f.heroImageHeight.value||150),heroImageFit:f.heroImageFit.value,heroImagePositionX:Number(f.heroImagePositionX.value||50),heroImagePositionY:Number(f.heroImagePositionY.value||50),heroBackgroundImage:f.heroBackgroundImage.value||null,heroBackgroundOpacity:Number(f.heroBackgroundOpacity.value||0),heroBackgroundFit:f.heroBackgroundFit.value,heroBackgroundPositionX:Number(f.heroBackgroundPositionX.value||50),heroBackgroundPositionY:Number(f.heroBackgroundPositionY.value||50),heroBackgroundBlur:Number(f.heroBackgroundBlur.value||0),serviceCoverHeight:Number(f.serviceCoverHeight.value||105),serviceCoverFit:f.serviceCoverFit.value,serviceCoverPositionX:Number(f.serviceCoverPositionX.value||50),serviceCoverPositionY:Number(f.serviceCoverPositionY.value||50)};
    if(section==='notice') return {...common,announcementEnabled:f.announcementEnabled.checked,announcementText:f.announcementText.value||null};
    if(section==='contact') return {...common,contactTitle:f.contactTitle.value,contactIntro:f.contactIntro.value,wechatId:f.wechatId.value||null,wechatQrImage:f.wechatQrImage.value||null,wechatQrSize:Number(f.wechatQrSize.value||260),contactChannels:syncContactChannelsFromDom(),qqId:f.qqId.value||null,qqLink:f.qqLink.value||null,phone:f.phone.value||null,serviceHours:f.serviceHours.value||null,taobaoUrl:f.taobaoUrl.value||null,xianyuUrl:f.xianyuUrl.value||null,contactNotice:f.contactNotice.value,wecomKefuUrl:f.wecomKefuUrl.value||null,wecomKefuNote:f.wecomKefuNote.value||null,quickContactTitle:f.quickContactTitle.value||null,taobaoLabel:f.taobaoLabel.value||null,xianyuLabel:f.xianyuLabel.value||null,wecomKefuLabel:f.wecomKefuLabel.value||null,wecomKefuButton:f.wecomKefuButton.value||null};
    if(section==='trust') return {...common,trustCases:syncTrustCasesFromDom(),trustFaq:syncTrustFaqFromDom()};
    if(section==='copy') return {...common,quoteSuccessText:f.quoteSuccessText.value,orderHelpText:f.orderHelpText.value,footerText:f.footerText.value};
    if(section==='seo') return {...common,seoTitle:f.seoTitle.value||null,seoDescription:f.seoDescription.value||null};
    return common;
  }

  // V22 P0-01：游戏运营状态编辑
  let gameStatusRows = [];
  const GS_LABELS = {
    PREORDER: '正式版预约中', OPEN: '正常接单', PAUSED: '暂停接单',
    MAINTENANCE: '临时维护', LIMITED: '部分项目开放'
  };
  function gsEsc(v) {
    return String(v == null ? '' : v).replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
  }

  async function loadGameStatus() {
    const box = $('#gameStatusList');
    if (!box) return;
    const statuses = await api('/api/public/game-status/all').catch(() => []);
    const catalog = await api('/api/admin/catalog').catch(() => null);
    const games = (catalog && Array.isArray(catalog.games) ? catalog.games : []).filter(g => g.active !== false);
    const byCode = new Map((statuses || []).map(s => [s.game, s]));
    gameStatusRows = games.map(g => {
      const st = byCode.get(g.code) || {};
      return {
        code: g.code, name: g.name,
        status: String(st.status || 'PREORDER').toUpperCase(),
        label: st.label || `${g.name} 状态待确认`,
        ctaLabel: st.ctaLabel || '提前询价',
        detail: st.detail || ''
      };
    });
    (statuses || []).forEach(st => {
      if (!gameStatusRows.some(r => r.code === st.game)) {
        gameStatusRows.push({ code: st.game, name: st.game, status: String(st.status || 'PREORDER').toUpperCase(), label: st.label || '', ctaLabel: st.ctaLabel || '' });
      }
    });
    if (!gameStatusRows.length) { box.innerHTML = '<div class="empty card">暂无可维护的游戏</div>'; return; }
    box.innerHTML = gameStatusRows.map(r => `
      <div class="card" style="margin-bottom:12px">
        <div class="grid3">
          <div class="field"><label>游戏</label><input value="${gsEsc(r.name)}" disabled/></div>
          <div class="field"><label>运营状态</label><select data-gs-status="${gsEsc(r.code)}">
            ${Object.keys(GS_LABELS).map(k => `<option value="${k}"${k === r.status ? ' selected' : ''}>${GS_LABELS[k]}</option>`).join('')}
          </select></div>
          <div class="field"><label>按钮文案</label><input maxlength="40" data-gs-cta="${gsEsc(r.code)}" value="${gsEsc(r.ctaLabel)}" placeholder="提前询价"/></div>
        </div>
        <div class="field"><label>状态文案（前台原样显示）</label><input maxlength="80" data-gs-label="${gsEsc(r.code)}" value="${gsEsc(r.label)}" placeholder="MW4 正式版预约中 · 上线后陆续开放服务"/></div>
        <div class="field"><label>状态详情（可选）</label><input maxlength="240" data-gs-detail="${gsEsc(r.code)}" value="${gsEsc(r.detail)}" placeholder="上线后陆续开放服务，开放前提交为提前询价"/></div>
      </div>`).join('');
  }

  $('#saveGameStatus')?.addEventListener('click', async event => {
    const btn = event.currentTarget;
    setBusy(btn, true, '保存中…');
    try {
      for (const row of gameStatusRows) {
        const status = $(`[data-gs-status="${row.code}"]`)?.value;
        const label = $(`[data-gs-label="${row.code}"]`)?.value;
        const ctaLabel = $(`[data-gs-cta="${row.code}"]`)?.value;
        const detail = $(`[data-gs-detail="${row.code}"]`)?.value;
        if (!label || !label.trim()) throw new Error(`${row.name} 的状态文案不能为空`);
        await api(`/api/admin/game-status/${row.code}`, {
          method: 'PUT',
          body: JSON.stringify({ status, label: label.trim(), ctaLabel: ctaLabel || null, detail: detail || null })
        });
      }
      toast('运营状态已保存，前台已同步');
      await loadGameStatus();
    } catch (error) {
      toast(error.message, 'error');
    } finally {
      setBusy(btn, false);
    }
  });

  $('#saveConfigSection')?.addEventListener('click', async event => {
    const btn=event.currentTarget; setBusy(btn,true,'保存中…');
    try {
      const section = activeConfigSection;
      const preserve = section !== 'visual' && document.getElementById('visualSaveState')?.dataset.dirty === 'true';
      const drafts = preserve ? [...document.querySelectorAll('[data-config-section="visual"] input:not([type="file"]),[data-config-section="visual"] select')].map(input => [input, input.value]) : [];
      await api(`/api/admin/site-config/section/${section}`,{method:'PATCH',body:JSON.stringify(configSectionPayload(section))});
      toast(`${configSectionLabels[section]}已单独保存`);
      await loadConfig();
      drafts.forEach(([input,value]) => { input.value = value; });
      if (drafts.length) { updateHeroBackgroundLivePreview(); drafts[0][0].dispatchEvent(new Event('input', {bubbles:true})); }
    }
    catch(error){ toast(error.message,'error'); } finally { setBusy(btn,false); }
  });

  $('#addContactChannel')?.addEventListener('click', () => {
    syncContactChannelsFromDom();
    if (contactChannels.length >= 6) return toast('最多添加 6 个客服', 'error');
    contactChannels.push({ label: `客服${contactChannels.length + 1}`, wechatId: '', qrImage: '', note: '发送业务编号确认报价' });
    renderContactChannels(contactChannels);
  });

  $('#contactChannelEditor')?.addEventListener('input', event => {
    if (event.target.closest('[data-contact-channel]')) syncContactChannelsFromDom();
  });
  $('#contactChannelEditor')?.addEventListener('click', event => {
    const card = event.target.closest('[data-contact-channel]');
    if (!card) return;
    if (event.target.closest('[data-contact-remove]')) {
      syncContactChannelsFromDom();
      contactChannels.splice(Number(card.dataset.contactChannel), 1);
      renderContactChannels(contactChannels);
      return;
    }
    const upload = event.target.closest('[data-contact-upload]');
    if (upload) uploadContactChannelQr(card, upload);
  });

  $('#addTrustCase')?.addEventListener('click', () => {
    syncTrustCasesFromDom();
    if (trustCases.length >= 8) return toast('最多添加 8 条案例', 'error');
    trustCases.push({ tag: '案例', title: '', metric: '', detail: '' });
    renderTrustCases(trustCases);
  });
  $('#addTrustFaq')?.addEventListener('click', () => {
    syncTrustFaqFromDom();
    if (trustFaq.length >= 12) return toast('最多添加 12 条 FAQ', 'error');
    trustFaq.push({ question: '', answer: '' });
    renderTrustFaq(trustFaq);
  });
  $('#trustCaseEditor')?.addEventListener('input', event => {
    if (event.target.closest('[data-trust-case]')) syncTrustCasesFromDom();
  });
  $('#trustFaqEditor')?.addEventListener('input', event => {
    if (event.target.closest('[data-trust-faq]')) syncTrustFaqFromDom();
  });
  $('#trustCaseEditor')?.addEventListener('click', event => {
    const card = event.target.closest('[data-trust-case]');
    if (!card || !event.target.closest('[data-trust-case-remove]')) return;
    syncTrustCasesFromDom();
    trustCases.splice(Number(card.dataset.trustCase), 1);
    renderTrustCases(trustCases);
  });
  $('#trustFaqEditor')?.addEventListener('click', event => {
    const card = event.target.closest('[data-trust-faq]');
    if (!card || !event.target.closest('[data-trust-faq-remove]')) return;
    syncTrustFaqFromDom();
    trustFaq.splice(Number(card.dataset.trustFaq), 1);
    renderTrustFaq(trustFaq);
  });
  $$('[data-nav-group]').forEach(button => button.addEventListener('click', () => {
    const section = button.closest('.nav-section');
    const willOpen = section?.classList.contains('collapsed');
    if (window.matchMedia('(max-width: 760px)').matches && willOpen) {
      $$('.nav-section:not(.nav-section-primary)').forEach(item => item.classList.add('collapsed'));
    }
    section?.classList.toggle('collapsed');
  }));


  let taxonomy = { games: [], categories: [], platforms: [], gamePlatforms: [] };
  let selectedGameCodes = new Set();
  async function loadTaxonomy() {
    const out = await api('/api/admin/catalog');
    taxonomy = { games: Array.isArray(out?.games)?out.games:[], categories:Array.isArray(out?.categories)?out.categories:[], platforms:Array.isArray(out?.platforms)?out.platforms:[], gamePlatforms:Array.isArray(out?.gamePlatforms)?out.gamePlatforms:[] };
    return taxonomy;
  }
  const gameName = code => taxonomy.games.find(x=>x.code===code)?.name || code || '-';
  const categoryName = (game,code) => taxonomy.categories.find(x=>x.gameCode===game && x.code===code)?.name || zh('category',code);
  const businessLabel = value => ({ACTIVE:'已运营',READY:'可启用',PLANNED:'规划中',PAUSED:'暂停'}[value] || value || '规划中');
  const businessTone = value => value==='ACTIVE'?'ok':value==='READY'?'ready':value==='PAUSED'?'danger':'';
  const gamePlatformsFor = code => taxonomy.gamePlatforms.filter(x=>x.gameCode===code&&x.active).map(x=>taxonomy.platforms.find(p=>p.code===x.platformCode)).filter(Boolean);
  function fillGameSelect(select, includeAll=false, includeInactive=false) { if(!select)return; const cur=select.value; select.innerHTML=(includeAll?'<option value="">全部游戏</option>':'')+taxonomy.games.filter(x=>includeInactive||x.active).map(x=>`<option value="${esc(x.code)}">${esc(x.name)}${x.active?'':'（预设未启用）'}</option>`).join(''); if([...select.options].some(o=>o.value===cur))select.value=cur; else if(!includeAll&&select.options.length)select.selectedIndex=0; }
  function fillCategorySelect(select, game, includeAll=false) { if(!select)return; const cur=select.value; const rows=taxonomy.categories.filter(x=>x.active&&x.gameCode===game); select.innerHTML=(includeAll?'<option value="">全部需求类型</option>':'')+rows.map(x=>`<option value="${esc(x.code)}">${esc(x.name)}</option>`).join(''); if([...select.options].some(o=>o.value===cur))select.value=cur; else if(!includeAll&&select.options.length)select.selectedIndex=0; }

  async function loadCatalogManager(){
    await loadTaxonomy();
    fillGameSelect($('#catalogCategoryGame'), false, true);
    const pf=$('#gamePlatformFilter'); if(pf){const cur=pf.value;pf.innerHTML='<option value="">全部平台</option>'+taxonomy.platforms.map(x=>`<option value="${esc(x.code)}">${esc(x.name)}</option>`).join(''); if([...pf.options].some(o=>o.value===cur))pf.value=cur;}
    renderCatalogManager();
  }

  function gameMatchesFilters(x){
    const q=($('#gameCatalogSearch')?.value||'').trim().toLowerCase();
    const business=$('#gameBusinessFilter')?.value||'';
    const platform=$('#gamePlatformFilter')?.value||'';
    const active=$('#gameActiveFilter')?.value||'';
    const featured=$('#gameFeaturedFilter')?.checked;
    if(q && ![x.code,x.name,x.nameEn,x.aliases,x.publisher,x.genres].some(v=>String(v||'').toLowerCase().includes(q))) return false;
    if(business && x.businessStatus!==business) return false;
    if(platform && !gamePlatformsFor(x.code).some(p=>p.code===platform)) return false;
    if(active==='1' && !x.active) return false;
    if(active==='0' && x.active) return false;
    if(featured && !x.featured) return false;
    return true;
  }

  function renderGameKpis(){
    const games=taxonomy.games;
    const kpis=[
      ['游戏总数',games.length,'library'],['前台启用',games.filter(x=>x.active).length,'power'],['可启用',games.filter(x=>x.businessStatus==='READY').length,'star'],['重点游戏',games.filter(x=>x.featured).length,'rank'],['平台数量',taxonomy.platforms.length,'desktop']
    ];
    const box=$('#gameCatalogKpis'); if(box) box.innerHTML=kpis.map(([label,value,icon])=>`<div class="game-kpi"><span class="icon-chip">${iconUse(icon)}</span><div><b>${value}</b><small>${esc(label)}</small></div></div>`).join('');
  }

  function renderCatalogManager(){
    renderGameKpis();
    const selectedGame=$('#catalogCategoryGame')?.value || taxonomy.games.find(x=>x.active)?.code || taxonomy.games[0]?.code || '';
    const gameList=$('#catalogGameList');
    const categoryList=$('#catalogCategoryList');
    const rows=taxonomy.games.filter(gameMatchesFilters).sort((a,b)=>(Number(b.featured)-Number(a.featured))||(Number(b.popularityScore||0)-Number(a.popularityScore||0))||Number(a.sortOrder||0)-Number(b.sortOrder||0));
    const summary=$('#gameCatalogSummary'); if(summary) summary.textContent=`显示 ${rows.length} / ${taxonomy.games.length} 款 · 默认按重点与热度排序`;
    if(gameList) gameList.innerHTML=rows.map(x=>{
      const ps=gamePlatformsFor(x.code);
      const visual=x.coverImage?`<img src="${esc(x.coverImage)}" alt="">`:`${iconUse(x.iconKey||'gamepad')}`;
      return `<article class="game-library-row ${x.featured?'is-featured':''}" data-game-row="${esc(x.code)}">
        <label class="game-select"><input type="checkbox" data-game-select="${esc(x.code)}" ${selectedGameCodes.has(x.code)?'checked':''}><span class="sr-only">选择 ${esc(x.name)}</span></label>
        <div class="game-thumb">${visual}</div>
        <div class="game-main"><div class="game-title-line"><b>${esc(x.name)}</b>${x.nameEn&&x.nameEn!==x.name?`<span>${esc(x.nameEn)}</span>`:''}${x.featured?'<em>重点</em>':''}</div><p>${esc(x.description||x.aliases||'暂无业务说明')}</p><div class="game-tags">${ps.slice(0,5).map(p=>`<span>${esc(p.name)}</span>`).join('')}${ps.length>5?`<span>+${ps.length-5}</span>`:''}</div></div>
        <div class="game-health"><span class="badge ${businessTone(x.businessStatus)}">${esc(businessLabel(x.businessStatus))}</span><strong>${Number(x.popularityScore||0)}</strong><small>热度</small><span class="dependency-badge"><i class="status-dot ${x.active?'':'off'}"></i>${x.active?'前台启用':'前台隐藏'}</span></div>
        <div class="game-deps"><b>${Number(x.categoryCount||0)}</b><small>需求类型</small><b>${Number(x.serviceCount||0)}</b><small>服务项目</small></div>
        <div class="taxonomy-actions"><button class="btn small-btn" data-taxonomy-edit-game="${esc(x.code)}">${iconUse('edit')} 资料</button><button class="btn small-btn danger" data-taxonomy-delete-game="${esc(x.code)}" ${Number(x.categoryCount||0)+Number(x.serviceCount||0)>0?'disabled':''}>${iconUse('delete')} 删除</button></div>
      </article>`;
    }).join('')||'<div class="empty">没有符合筛选条件的游戏。</div>';

    const categoryRows=taxonomy.categories.filter(x=>x.gameCode===selectedGame);
    if(categoryList) categoryList.innerHTML=categoryRows.map(x=>`<div class="taxonomy-item-v20">
      <span class="icon-chip">${iconUse(x.iconKey||'service')}</span>
      <div class="taxonomy-meta"><b>${esc(x.name)}</b><small class="muted">${esc(x.description||'暂无辅助说明')}</small><span class="dependency-badge"><i class="status-dot ${x.active?'':'off'}"></i>${x.active?'已启用':'已停用'} · ${Number(x.serviceCount||0)} 个服务项目</span></div>
      <div class="taxonomy-actions"><button class="btn small-btn" data-taxonomy-edit-category="${x.id}">${iconUse('edit')} 编辑</button><button class="btn small-btn danger" data-taxonomy-delete-category="${x.id}" ${Number(x.serviceCount||0)>0?'disabled':''}>${iconUse('delete')} 删除</button></div>
    </div>`).join('')||'<div class="empty">当前游戏暂无需求类型</div>';
    updateGameBulkBar();
  }

  function updateGameBulkBar(){ const box=$('#gameBulkBar'),count=$('#gameBulkCount'); if(count)count.textContent=`已选 ${selectedGameCodes.size} 个`; box?.classList.toggle('hidden',selectedGameCodes.size===0); }
  function renderGameProfileVisual(item){ const box=$('#gameProfileVisual'); if(!box)return; const image=item?.coverImage||item?.logoImage; box.innerHTML=image?`<img src="${esc(image)}" alt=""><div><b>${esc(item?.name||'游戏资料')}</b><small>${esc(item?.publisher||item?.nameEn||'资料预览')}</small></div>`:`<span class="icon-chip">${iconUse(item?.iconKey||'gamepad')}</span><div><b>${esc(item?.name||'游戏资料预览')}</b><small>${esc(item?.publisher||item?.nameEn||'补充 Logo / 封面后会显示在这里')}</small></div>`; }
  function renderPlatformChoices(item){ const box=$('#gamePlatformChoices'); if(!box)return; const selected=new Set(item?.platformCodes||[]); box.innerHTML=taxonomy.platforms.map(p=>`<label class="platform-choice"><input type="checkbox" name="platformCodes" value="${esc(p.code)}" ${selected.has(p.code)?'checked':''}><span class="icon-chip">${iconUse(p.iconKey||'desktop')}</span><span><b>${esc(p.name)}</b><small>${esc(p.platformGroup||'平台')}</small></span></label>`).join(''); }

  function openGameProfile(item){
    const modal=$('#gameProfileModal'),f=$('#gameProfileForm'); if(!modal||!f)return;
    f.reset(); modal.classList.remove('hidden');
    f.code.value=item.code; f.name.value=item.name||''; f.nameEn.value=item.nameEn||''; f.aliases.value=item.aliases||''; f.iconKey.value=item.iconKey||'gamepad'; f.description.value=item.description||''; f.publisher.value=item.publisher||''; f.genres.value=item.genres||''; f.logoImage.value=item.logoImage||''; f.coverImage.value=item.coverImage||''; f.popularityScore.value=Number(item.popularityScore??50); f.featured.checked=!!item.featured; f.businessStatus.value=item.businessStatus||'PLANNED'; f.seoSlug.value=item.seoSlug||''; f.seoTitle.value=item.seoTitle||''; f.seoDescription.value=item.seoDescription||''; f.sortOrder.value=Number(item.sortOrder||0); f.active.checked=!!item.active; f.reason.value='游戏资料维护';
    $('#gameProfileTitle').textContent=`编辑 · ${item.name}`; renderPlatformChoices(item); renderNamedPreview('#gameCoverPreview',item.coverImage||'','游戏封面'); renderNamedPreview('#gameLogoPreview',item.logoImage||'','游戏 Logo'); renderGameProfileVisual(item);
    requestAnimationFrame(()=>{ f.scrollTop=0; modal.scrollTop=0; });
  }

  async function uploadGameImage(kind,file,hidden,preview,button,label){ if(!file)return toast(`请先选择${label}`,'error'); const fd=new FormData();fd.append('file',file);setBusy(button,true,'上传中…');try{const out=await api(`/api/admin/catalog/games/image/${kind}`,{method:'POST',body:fd});hidden.value=out.url||'';renderNamedPreview(preview,hidden.value,label);renderGameProfileVisual({...taxonomy.games.find(x=>x.code===$('#gameProfileForm')?.code?.value),[hidden.name]:hidden.value});toast(`${label}已上传`);}catch(error){toast(error.message,'error')}finally{setBusy(button,false)}}

  $('#catalogCategoryGame')?.addEventListener('change',renderCatalogManager);
  ['#gameCatalogSearch','#gameBusinessFilter','#gamePlatformFilter','#gameActiveFilter','#gameFeaturedFilter'].forEach(sel=>$(sel)?.addEventListener(sel.includes('Search')?'input':'change',renderCatalogManager));
  $('#refreshGameCenter')?.addEventListener('click',loadCatalogManager);
  $('#newGameProfile')?.addEventListener('click',()=>$('#newGameName')?.focus());
  $('#addGame')?.addEventListener('click',async e=>{
    // 必须在 await 之前捕获按钮：await 之后 event.currentTarget 变成 null，
    // setBusy(null,false) 在内部直接 return，按钮会永久停在 disabled + 「建立中…」。
    const btn=e.currentTarget;
    const name=$('#newGameName')?.value.trim(); if(!name)return toast('请输入游戏名称','error'); setBusy(btn,true,'建立中…');
    try{const created=await api('/api/admin/catalog/games',{method:'POST',body:JSON.stringify({name,iconKey:$('#newGameIcon')?.value||'gamepad',description:$('#newGameDescription')?.value.trim()||null,sortOrder:Number($('#newGameSort')?.value||10),active:false,reason:'新游戏预设'})}); $('#newGameName').value=''; $('#newGameDescription').value=''; await loadCatalogManager(); const item=taxonomy.games.find(x=>x.code===created.code); if(item)openGameProfile(item); toast('预设条目已建立，可继续完善资料');}
    catch(err){toast(err.message,'error')}finally{setBusy(btn,false)}
  });
  $('#addCategory')?.addEventListener('click',async e=>{
    const btn=e.currentTarget;
    const game=$('#catalogCategoryGame')?.value; const name=$('#newCategoryName')?.value.trim(); if(!game||!name)return toast('请选择所属游戏并输入需求类型名称','error');
    setBusy(btn,true,'新增中…'); try{await api('/api/admin/catalog/categories',{method:'POST',body:JSON.stringify({gameCode:game,name,iconKey:$('#newCategoryIcon')?.value||'service',description:$('#newCategoryDescription')?.value.trim()||null,sortOrder:Number($('#newCategorySort')?.value||10),active:true,reason:'新增需求类型'})}); $('#newCategoryName').value=''; $('#newCategoryDescription').value=''; toast('需求类型已新增'); await loadCatalogManager();} catch(err){toast(err.message,'error')}finally{setBusy(btn,false)}
  });
  function openTaxonomyEditor(kind,item){
    if(kind==='game') return openGameProfile(item);
    const modal=$('#taxonomyModal'), f=$('#taxonomyForm'); if(!modal||!f)return; modal.classList.remove('hidden'); f.reset(); f.kind.value=kind; f.key.value=item.id; f.name.value=item.name||''; f.iconKey.value=item.iconKey||'service'; f.description.value=item.description||''; f.sortOrder.value=String(item.sortOrder??0); f.active.checked=!!item.active; f.reason.value='日常运营调整'; $('#taxonomyModalTitle').textContent='编辑需求类型';
  }
  $('#closeTaxonomyModal')?.addEventListener('click',()=>$('#taxonomyModal')?.classList.add('hidden'));
  $('#closeGameProfileModal')?.addEventListener('click',()=>$('#gameProfileModal')?.classList.add('hidden'));
  $('#taxonomyForm')?.addEventListener('submit',async e=>{ e.preventDefault(); const f=e.currentTarget,key=f.key.value,btn=f.querySelector('button[type=submit]'); const body={name:f.name.value.trim(),iconKey:f.iconKey.value,description:f.description.value.trim()||null,sortOrder:Number(f.sortOrder.value||0),active:f.active.checked,reason:f.reason.value}; setBusy(btn,true,'保存中…');try{await api(`/api/admin/catalog/categories/${key}`,{method:'PUT',body:JSON.stringify(body)});toast('需求类型已保存');$('#taxonomyModal').classList.add('hidden');await loadCatalogManager();}catch(err){toast(err.message,'error')}finally{setBusy(btn,false)}});
  $('#gameProfileForm')?.addEventListener('submit',async e=>{
    e.preventDefault(); const f=e.currentTarget,code=f.code.value,btn=f.querySelector('button[type=submit]'); const platformCodes=[...f.querySelectorAll('input[name=platformCodes]:checked')].map(x=>x.value);
    const body={name:f.name.value.trim(),nameEn:f.nameEn.value.trim()||null,aliases:f.aliases.value.trim()||null,iconKey:f.iconKey.value,description:f.description.value.trim()||null,publisher:f.publisher.value.trim()||null,genres:f.genres.value.trim()||null,logoImage:f.logoImage.value||null,coverImage:f.coverImage.value||null,popularityScore:Number(f.popularityScore.value||0),featured:f.featured.checked,businessStatus:f.businessStatus.value,seoSlug:f.seoSlug.value.trim()||null,seoTitle:f.seoTitle.value.trim()||null,seoDescription:f.seoDescription.value.trim()||null,sortOrder:Number(f.sortOrder.value||0),active:f.active.checked,platformCodes,reason:f.reason.value};
    setBusy(btn,true,'保存中…');try{await api(`/api/admin/catalog/games/${encodeURIComponent(code)}/profile`,{method:'PUT',body:JSON.stringify(body)});toast('游戏资料已保存');$('#gameProfileModal').classList.add('hidden');await loadCatalogManager();}catch(err){toast(err.message,'error')}finally{setBusy(btn,false)}
  });
  $('#uploadGameCover')?.addEventListener('click',e=>{const f=$('#gameProfileForm');return uploadGameImage('COVER',f?.coverImageFile?.files?.[0],f.coverImage,'#gameCoverPreview',e.currentTarget,'游戏封面')});
  $('#uploadGameLogo')?.addEventListener('click',e=>{const f=$('#gameProfileForm');return uploadGameImage('LOGO',f?.logoImageFile?.files?.[0],f.logoImage,'#gameLogoPreview',e.currentTarget,'游戏 Logo')});
  $$('[data-clear-game-image]').forEach(btn=>btn.addEventListener('click',()=>{const f=$('#gameProfileForm'),name=btn.dataset.clearGameImage;if(f?.[name])f[name].value='';if(f?.[name+'File'])f[name+'File'].value='';renderNamedPreview(name==='coverImage'?'#gameCoverPreview':'#gameLogoPreview','');renderGameProfileVisual({...taxonomy.games.find(x=>x.code===f?.code?.value),[name]:null});}));
  $$('[data-game-bulk],[data-game-bulk-featured],[data-game-bulk-active]').forEach(btn=>btn.addEventListener('click',async()=>{if(!selectedGameCodes.size)return;const body={codes:[...selectedGameCodes],reason:'游戏资料批量运营'};if(btn.dataset.gameBulk)body.businessStatus=btn.dataset.gameBulk;if(btn.dataset.gameBulkFeatured)body.featured=btn.dataset.gameBulkFeatured==='1';if(btn.dataset.gameBulkActive)body.active=btn.dataset.gameBulkActive==='1';setBusy(btn,true,'处理中…');try{await api('/api/admin/catalog/games/bulk',{method:'PATCH',body:JSON.stringify(body)});toast(`已批量更新 ${selectedGameCodes.size} 款游戏`);selectedGameCodes.clear();await loadCatalogManager();}catch(error){toast(error.message,'error')}finally{setBusy(btn,false)}}));

  let services = [];
  let slots = [];
  async function loadServices() {
    await loadTaxonomy();
    const [serviceRows, slotRows] = await Promise.all([api('/api/admin/services'), api('/api/admin/slots')]);
    services = serviceRows || [];
    slots = slotRows || [];
    fillGameSelect($('#adminGameFilter'));
    fillCategorySelect($('#adminCategoryFilter'), $('#adminGameFilter')?.value || taxonomy.games[0]?.code || '', true);
    syncAdminServiceOptions();
    renderServices();
  }

  const gameLabel = value => gameName(String(value || ''));

  function syncAdminServiceOptions() {
    const game = $('#adminGameFilter')?.value || 'CODMW4';
    const category = $('#adminCategoryFilter')?.value || '';
    const select = $('#adminServiceFilter');
    if (!select) return;
    const current = select.value;
    const rows = services.filter(s => String(s.gameCode || 'CODMW4').toUpperCase() === game && (!category || s.category === category));
    select.innerHTML = '<option value="">全部具体服务</option>' + rows.map(s => `<option value="${s.id}">${esc(s.name)}</option>`).join('');
    if ([...select.options].some(x => x.value === current)) select.value = current;
  }

  function renderServices() {
    const keyword = ($('#serviceSearch')?.value || '').trim().toLowerCase();
    const game = $('#adminGameFilter')?.value || 'CODMW4';
    const category = $('#adminCategoryFilter')?.value || '';
    const selectedId = $('#adminServiceFilter')?.value || '';
    const filtered = services.filter(s => String(s.gameCode || 'CODMW4').toUpperCase() === game)
      .filter(s => !category || s.category === category)
      .filter(s => !selectedId || String(s.id) === selectedId)
      .filter(s => !keyword || [s.name, categoryName(s.gameCode, s.category), zh('serviceStatus', s.status)]
        .some(v => String(v || '').toLowerCase().includes(keyword)));
    const summary = $('#serviceFilterSummary');
    if (summary) summary.textContent = `当前：${gameLabel(game)} · ${category ? categoryName(game, category) : '全部需求类型'}${selectedId ? ' · 已定位具体服务' : ''}`;
    $('#serviceRows').innerHTML = filtered.map(s => `<tr>
      <td>${esc(gameLabel(s.gameCode || 'CODMW4'))}</td>
      <td>${esc(categoryName(s.gameCode, s.category))}</td>
      <td><strong>${esc(s.name)}</strong><div class="muted small">${esc(s.shortDesc || '服务项目')}</div></td>
      <td>${esc(zh('priceMode', s.priceMode))}</td>
      <td>${statusBadge('serviceStatus', s.status)}</td>
      <td>${s.active ? '是' : '否'}</td>
      <td><div class="row-actions"><button class="btn small-btn" data-service-hot="1" data-service-id="${s.id}" title="设为首页推荐位 1">推荐1</button><button class="btn small-btn" data-service-hot="2" data-service-id="${s.id}" title="设为首页推荐位 2">推荐2</button><button class="btn small-btn" data-service-edit="${s.id}">${iconUse('edit')} 编辑</button>${s.active&&s.status!=='OFFLINE'?`<button class="btn small-btn danger" data-service-offline="${s.id}">${iconUse('power')} 下架</button>`:`<button class="btn small-btn" data-service-restore="${s.id}">${iconUse('restore')} 恢复</button>`}</div></td>
    </tr>`).join('') || '<tr><td colspan="7" class="empty">当前一级 / 二级下没有匹配服务</td></tr>';
  }

  $('#serviceSearch')?.addEventListener('input', renderServices);
  $('#adminGameFilter')?.addEventListener('change', () => { fillCategorySelect($('#adminCategoryFilter'), $('#adminGameFilter').value, true); syncAdminServiceOptions(); renderServices(); });
  $('#adminCategoryFilter')?.addEventListener('change', () => { syncAdminServiceOptions(); renderServices(); });
  $('#adminServiceFilter')?.addEventListener('change', renderServices);
  $('#newService')?.addEventListener('click', () => openService(null));
  $('#closeServiceModal')?.addEventListener('click', () => $('#serviceModal').classList.add('hidden'));

  function openService(service) {
    const modal = $('#serviceModal');
    modal.classList.remove('hidden');
    $('#serviceModalTitle').textContent = service ? '编辑服务项目' : '新增服务项目';
    const f = $('#serviceForm');
    f.reset();
    fillGameSelect(f.gameCode);
    const wantedGame=service?.gameCode || $('#adminGameFilter')?.value || taxonomy.games[0]?.code || '';
    f.gameCode.value=wantedGame; fillCategorySelect(f.category,wantedGame);
    f.dataset.id = service?.id || '';
    if (f.slug) f.slug.dataset.keep = service ? '1' : '0';
    requestAnimationFrame(()=>{ f.scrollTop=0; modal.scrollTop=0; });
    for (const k of ['slug', 'name', 'shortDesc', 'priceMode', 'priceFixed', 'priceMin', 'priceMax', 'durationText', 'status', 'coverImage', 'hotScore', 'manualPriority', 'sortOrder']) {
      if (f[k]) f[k].value = service?.[k] ?? '';
    }
    if (service) { f.category.value = service.category || f.category.value; }
    if (!service) {
      f.gameCode.value = $('#adminGameFilter')?.value || 'CODMW4';
      fillCategorySelect(f.category,f.gameCode.value); f.category.value = $('#adminCategoryFilter')?.value || f.category.options[0]?.value || '';
      f.priceMode.value = 'QUOTE';
      f.status.value = 'AVAILABLE';
      f.hotScore.value = '0';
      f.manualPriority.value = '0';
      f.sortOrder.value = '0';
      f.durationText.value = '以实际报价为准';
      f.reason.value = '新增服务项目';
    }
    f.active.checked = service?.active ?? true;
    f.expiresAt.value = service?.expiresAt ? String(service.expiresAt).slice(0, 16) : '';
    if (f.coverImageFile) f.coverImageFile.value = '';
    renderNamedPreview('#serviceCoverPreview', f.coverImage.value || '', '服务封面预览');
    updatePriceFields();
  }

  function slugifyService() {
    const f = $('#serviceForm'); if (!f) return;
    if (f.dataset.id && f.slug.dataset.keep === '1') return;
    const category = String(f.category.value||'service').toLowerCase().replace(/[^a-z0-9]+/g,'-');
    const base = String(f.name.value || '').trim().toLowerCase()
      .replace(/[^a-z0-9]+/g, '-').replace(/^-+|-+$/g, '');
    const game=String(f.gameCode.value||'game').toLowerCase().replace(/[^a-z0-9]+/g,'-').replace(/^-+|-+$/g,'');
    f.slug.value = `${game}-${category}-${base || Date.now().toString().slice(-6)}`.slice(0,100);
  }
  $('#serviceForm')?.name?.addEventListener('input', slugifyService);
  $('#serviceForm')?.category?.addEventListener('change', slugifyService);
  $('#serviceForm')?.gameCode?.addEventListener('change', () => { const f=$('#serviceForm'); fillCategorySelect(f.category,f.gameCode.value); slugifyService(); });

  function updatePriceFields() {
    const mode = $('#serviceForm')?.priceMode?.value;
    $$('[data-price-field]').forEach(x => x.classList.toggle('hidden', x.dataset.priceField !== mode));
  }
  $('#serviceForm')?.priceMode?.addEventListener('change', updatePriceFields);

  $('#uploadServiceCover')?.addEventListener('click', async event => {
    const f = $('#serviceForm'); const file = f?.coverImageFile?.files?.[0];
    if (!file) return toast('请先选择服务封面', 'error');
    const fd = new FormData(); fd.append('file', file);
    // 同上：await 之后 event.currentTarget 为 null，按钮会卡在「上传中…」。
    const uploadBtn = event.currentTarget;
    setBusy(uploadBtn, true, '上传中…');
    try { const out = await api('/api/admin/services/cover-image', { method: 'POST', body: fd }); f.coverImage.value = out.url || ''; renderNamedPreview('#serviceCoverPreview', f.coverImage.value, '服务封面预览'); toast('服务封面已上传，保存服务项目后生效'); }
    catch (error) { toast(error.message, 'error'); } finally { setBusy(uploadBtn, false); }
  });
  $('#clearServiceCover')?.addEventListener('click', () => { const f=$('#serviceForm'); f.coverImage.value=''; if(f.coverImageFile)f.coverImageFile.value=''; renderNamedPreview('#serviceCoverPreview','','服务封面预览'); toast('服务封面已移除，保存服务项目后生效'); });

  $('#serviceForm')?.addEventListener('submit', async event => {
    event.preventDefault();
    const f = event.currentTarget;
    const id = f.dataset.id;
    if (!id) slugifyService();
    const btn = f.querySelector('button[type=submit]');
    const body = {
      gameCode: f.gameCode.value,
      slug: f.slug.value,
      name: f.name.value,
      category: f.category.value,
      shortDesc: f.shortDesc.value,
      priceMode: f.priceMode.value,
      priceFixed: f.priceFixed.value ? Number(f.priceFixed.value) : null,
      priceMin: f.priceMin.value ? Number(f.priceMin.value) : null,
      priceMax: f.priceMax.value ? Number(f.priceMax.value) : null,
      durationText: f.durationText.value,
      status: f.status.value,
      coverImage: f.coverImage.value || null,
      hotScore: Number(f.hotScore.value || 0),
      manualPriority: Number(f.manualPriority.value || 0),
      expiresAt: f.expiresAt.value ? new Date(f.expiresAt.value).toISOString() : null,
      active: f.active.checked,
      sortOrder: Number(f.sortOrder.value || 0),
      reason: f.reason.value
    };
    setBusy(btn, true, '保存中…');
    try {
      await api(id ? `/api/admin/services/${id}` : '/api/admin/services', {
        method: id ? 'PUT' : 'POST', body: JSON.stringify(body)
      });
      $('#serviceModal').classList.add('hidden');
      toast(id ? '服务已更新' : '服务已创建');
      await loadServices();
    } catch (error) { toast(error.message, 'error'); }
    finally { setBusy(btn, false); }
  });

  let quotes = [];
  async function loadQuotes() {
    quotes = (await api('/api/admin/quotes')) || [];
    renderQuotes();
  }

  function renderQuotes() {
    const keyword = ($('#quoteSearch')?.value || '').trim().toLowerCase();
    const filtered = quotes.filter(q => !keyword || [q.businessNo, q.game, q.demand, q.currentProgress, zh('quoteStatus', q.status)]
      .some(v => String(v || '').toLowerCase().includes(keyword)));
    $('#quoteRows').innerHTML = filtered.map(x => `<tr>
      <td><span class="mono">${esc(x.businessNo || '-')}</span></td>
      <td>${formatTime(x.createdAt)}</td>
      <td>${esc(x.game)}</td>
      <td><strong>${esc(x.demand)}</strong>${x.platform ? `<div class="muted">${esc(x.platform)}</div>` : ''}${x.customerContact ? `<div class="muted small">${esc(x.contactMethod || '联系')}：${esc(x.customerContact)}</div>` : ''}${x.customerIpHash ? `<div class="muted mono small">IP# ${esc(String(x.customerIpHash).slice(0,12))}</div>` : ''}</td>
      <td>${esc(x.currentProgress || '-')}</td>
      <td>${x.screenshotAssetId ? `<a class="text-link" target="_blank" rel="noopener" href="/api/admin/quotes/${x.id}/image">查看截图</a>` : '-'}</td>
      <td>
        <select class="compact-select" data-quote-status="${x.id}">
          ${Object.keys(ZH.quoteStatus).map(s => `<option value="${s}" ${s === x.status ? 'selected' : ''}>${ZH.quoteStatus[s]}</option>`).join('')}
        </select>
      </td>
      <td><div class="row-actions"><button class="btn small-btn primary" data-quote-convert="${x.id}" ${x.status === 'CONVERTED' ? 'disabled' : ''}>${x.status === 'CONVERTED' ? '已生成履约订单' : '生成履约订单'}</button><button class="btn small-btn danger" data-quote-delete="${x.id}">${iconUse('delete')} 删除</button></div></td>
    </tr>`).join('') || '<tr><td colspan="8" class="empty">暂无询价</td></tr>';
  }
  $('#quoteSearch')?.addEventListener('input', renderQuotes);

  let orders = [];
  async function loadOrders() {
    orders = (await api('/api/admin/orders')) || [];
    renderOrders();
  }

  function renderOrders() {
    const keyword = ($('#orderSearch')?.value || '').trim().toLowerCase();
    const filtered = orders.filter(o => !keyword || [o.businessNo, o.orderNo, o.progressText, o.customerNote, zh('orderStatus', o.status)]
      .some(v => String(v || '').toLowerCase().includes(keyword)));
    $('#orderRows').innerHTML = filtered.map(o => `<tr>
      <td><span class="mono">${esc(o.businessNo || o.orderNo)}</span>${o.sourceQuoteId ? '<div class="muted small">询价转单 · 编号保持不变</div>' : ''}</td>
      <td>${statusBadge('orderStatus', o.status)}</td>
      <td>${o.progressPercent == null ? '-' : `${o.progressPercent}%`}</td>
      <td>${esc(o.progressText)}</td>
      <td>${formatTime(o.updatedAt)}</td>
      <td><div class="row-actions"><button class="btn small-btn ghost" data-order-case="${esc(o.orderNo)}">设为案例</button><button class="btn small-btn ghost" data-order-case-off="${esc(o.orderNo)}">取消案例</button></div></td>
      <td><div class="row-actions"><button class="btn small-btn" data-order-copy="${esc(o.businessNo || o.orderNo)}">复制编号</button><button class="btn small-btn" data-order-edit="${esc(o.orderNo)}">编辑</button><button class="btn small-btn danger" data-order-delete="${esc(o.orderNo)}">${iconUse('delete')} 删除</button></div></td>
    </tr>`).join('') || '<tr><td colspan="7" class="empty">暂无订单</td></tr>';
  }
  $('#orderSearch')?.addEventListener('input', renderOrders);

  function editOrder(no) {
    const o = orders.find(x => x.orderNo === no);
    const modal = $('#orderModal');
    modal.classList.remove('hidden');
    const f = $('#orderForm');
    f.reset();
    f.dataset.orderNo = o?.orderNo || '';
    f.dataset.orderVersion = o?.version ?? '';
    f.orderNo.value = o?.orderNo || '';
    f.status.value = o?.status || 'WAITING_CONFIRMATION';
    f.progressPercent.value = o?.progressPercent ?? '';
    f.progressText.value = o?.progressText || '已创建订单，等待确认';
    f.customerNote.value = o?.customerNote || '';
    f.internalNote.value = o?.internalNote || '';
    $('#orderModalTitle').textContent = o ? `编辑业务 ${o.businessNo || o.orderNo}` : '新建订单';
    requestAnimationFrame(()=>{ f.scrollTop=0; modal.scrollTop=0; });
  }

  $('#newOrder')?.addEventListener('click', () => editOrder(null));
  $('#closeOrderModal')?.addEventListener('click', () => $('#orderModal').classList.add('hidden'));
  $('#orderForm')?.addEventListener('submit', async event => {
    event.preventDefault();
    const f = event.currentTarget;
    const no = f.dataset.orderNo;
    const btn = f.querySelector('button[type=submit]');
    const body = {
      ...(no ? { version: Number(f.dataset.orderVersion) } : {}),
      status: f.status.value,
      progressPercent: f.progressPercent.value === '' ? null : Number(f.progressPercent.value),
      progressText: f.progressText.value,
      customerNote: f.customerNote.value || null,
      internalNote: f.internalNote.value || null,
      reason: f.reason.value
    };
    setBusy(btn, true, '保存中…');
    try {
      const o = await api(no ? `/api/admin/orders/${encodeURIComponent(no)}` : '/api/admin/orders', {
        method: no ? 'PATCH' : 'POST', body: JSON.stringify(body)
      });
      if (f.image.files[0]) {
        const fd = new FormData();
        fd.append('file', f.image.files[0]);
        await api(`/api/admin/orders/${encodeURIComponent(o.orderNo)}/image`, { method: 'POST', body: fd });
      }
      $('#orderModal').classList.add('hidden');
      toast(no ? '订单已更新' : `业务已创建：${o.businessNo || o.orderNo}`);
      await loadOrders();
    } catch (error) { toast(error.message, 'error'); }
    finally { setBusy(btn, false); }
  });

  async function loadSlots() {
    const [ssRaw, slotsRaw] = await Promise.all([api('/api/admin/services'), api('/api/admin/slots')]); const ss = Array.isArray(ssRaw) ? ssRaw : []; const slots = Array.isArray(slotsRaw) ? slotsRaw : [];
    $('#slots').innerHTML = slots.map(x => `<div class="card slot-card">
      <div class="section-title"><div><span class="eyebrow">首页推荐</span><h3>展示位 ${x.slotNo}</h3></div>${statusBadge('slotMode', x.mode)}</div>
      <div class="field"><label>推荐方式</label><select data-slot-mode="${x.slotNo}">
        <option value="AUTO" ${x.mode === 'AUTO' ? 'selected' : ''}>自动推荐</option>
        <option value="MANUAL" ${x.mode === 'MANUAL' ? 'selected' : ''}>手动指定</option>
      </select></div>
      <div class="field" data-slot-service-wrap="${x.slotNo}"><label>指定服务</label><select data-slot-service="${x.slotNo}">
        <option value="">请选择服务</option>
        ${ss.filter(s => s.active && s.status !== 'OFFLINE').map(s => `<option value="${s.id}" ${s.id === x.serviceId ? 'selected' : ''}>${esc(s.name)}</option>`).join('')}
      </select></div>
      <button class="btn primary" data-slot-save="${x.slotNo}">保存展示位</button>
    </div>`).join('');
    slots.forEach(x => updateSlotServiceVisibility(x.slotNo));
  }

  function updateSlotServiceVisibility(no) {
    const mode = $(`[data-slot-mode="${no}"]`)?.value;
    $(`[data-slot-service-wrap="${no}"]`)?.classList.toggle('hidden', mode === 'AUTO');
  }

  let feedbackRows = [];
  function selectedFeedbackModule() {
    const label = $('#feedbackModule')?.value || FEEDBACK_MODULES[0].label;
    return FEEDBACK_MODULES.find(x => x.label === label) || FEEDBACK_MODULES[0];
  }

  function refreshFeedbackAreas() {
    const area = $('#feedbackArea');
    const current = area?.value;
    const mod = selectedFeedbackModule();
    if (!area) return;
    area.innerHTML = mod.areas.map(x => `<option value="${esc(x)}" ${x === current ? 'selected' : ''}>${esc(x)}</option>`).join('');
    if (![...area.options].some(x => x.value === current)) area.selectedIndex = 0;
    refreshFeedbackDraft();
  }

  function refreshFeedbackDraft() {
    const form = $('#feedbackForm');
    if (!form) return;
    const mod = selectedFeedbackModule();
    const el = form.elements;
    const area = el.area?.value || mod.areas[0] || '未指定位置';
    const issue = el.issueType?.value || FEEDBACK_ISSUES[0];
    const scope = el.scope?.value || '不确定';
    const other = (el.otherDetail?.value || '').trim();
    const needsOther = mod.label === '其他模块' || issue === '其他问题' || area === '其他位置';
    $('#feedbackOtherWrap')?.classList.toggle('hidden', !needsOther);
    el.pagePath.value = mod.path === '其他' ? area : `${mod.path} · ${area}`;
    el.title.value = `${mod.label} · ${area} · ${issue}`.slice(0, 120);
    el.content.value = [
      `问题模块：${mod.label}`,
      `页面/位置：${mod.path} · ${area}`,
      `问题类型：${issue}`,
      `影响范围：${scope}`,
      other ? `其他说明：${other}` : ''
    ].filter(Boolean).join('\n');
  }

  function initFeedbackOptions() {
    const form = $('#feedbackForm');
    if (!form || form.dataset.feedbackReady) return;
    form.dataset.feedbackReady = '1';
    const el = form.elements;
    el.module.innerHTML = FEEDBACK_MODULES.map(x => `<option value="${esc(x.label)}">${esc(x.label)}</option>`).join('');
    el.issueType.innerHTML = FEEDBACK_ISSUES.map(x => `<option value="${esc(x)}">${esc(x)}</option>`).join('');
    ['module', 'area', 'issueType', 'scope'].forEach(name => el[name]?.addEventListener('change', name === 'module' ? refreshFeedbackAreas : refreshFeedbackDraft));
    el.otherDetail?.addEventListener('input', refreshFeedbackDraft);
    refreshFeedbackAreas();
  }

  async function loadFeedback() {
    initFeedbackOptions();
    feedbackRows = (await api('/api/admin/test-feedback')) || [];
    renderFeedback();
  }

  function renderFeedback() {
    const keyword = ($('#feedbackSearch')?.value || '').trim().toLowerCase();
    const filtered = feedbackRows.filter(x => !keyword || [x.title, x.pagePath, x.content, x.operatorNote, zh('feedbackStatus', x.status), zh('feedbackPriority', x.priority)]
      .some(v => String(v || '').toLowerCase().includes(keyword)));
    const summary = $('#feedbackSummary');
    if (summary) {
      const fresh = feedbackRows.filter(x => x.status === 'NEW').length;
      summary.textContent = fresh ? `${fresh} 条新反馈待查看，共 ${feedbackRows.length} 条` : `暂无新反馈，共 ${feedbackRows.length} 条`;
    }
    $('#feedbackRows').innerHTML = filtered.map(x => `<tr>
      <td><strong>${esc(x.title)}</strong><div class="muted small feedback-content">${esc(x.content)}</div>${x.operatorNote ? `<div class="muted small">说明：${esc(x.operatorNote)}</div>` : ''}${x.attachmentAssetId ? `<div class="feedback-attachment-line"><a class="text-link" target="_blank" rel="noopener" href="/api/admin/test-feedback/${x.id}/attachment">查看附件</a></div>` : ''}</td>
      <td>${esc(x.pagePath || '-')}</td>
      <td>${statusBadge('feedbackPriority', x.priority)}</td>
      <td><select class="compact-select" data-feedback-status="${x.id}">
        ${Object.keys(ZH.feedbackStatus).map(s => `<option value="${s}" ${s === x.status ? 'selected' : ''}>${ZH.feedbackStatus[s]}</option>`).join('')}
      </select></td>
      <td>${formatTime(x.updatedAt)}</td>
      <td><button class="btn small-btn" type="button" data-feedback-copy="${x.id}">复制给 Codex</button></td>
    </tr>`).join('') || '<tr><td colspan="6" class="empty">暂无网页测试修改反馈</td></tr>';
  }
  $('#feedbackSearch')?.addEventListener('input', renderFeedback);

  $('#feedbackForm')?.addEventListener('submit', async event => {
    event.preventDefault();
    const f = event.currentTarget;
    const btn = f.querySelector('button[type=submit]');
    refreshFeedbackDraft();
    const el = f.elements;
    const body = {
      title: el.title.value,
      pagePath: el.pagePath.value || null,
      content: el.content.value,
      priority: el.priority.value,
      operatorNote: el.operatorNote.value || null
    };
    setBusy(btn, true, '提交中…');
    try {
      const created = await api('/api/admin/test-feedback', { method: 'POST', body: JSON.stringify(body) });
      if (f.attachment?.files?.[0]) {
        const fd = new FormData();
        fd.append('file', f.attachment.files[0]);
        await api(`/api/admin/test-feedback/${created.id}/attachment`, { method: 'POST', body: fd });
      }
      f.reset();
      refreshFeedbackAreas();
      toast('测试反馈已提交');
      await loadFeedback();
    } catch (error) { toast(error.message, 'error'); }
    finally { setBusy(btn, false); }
  });

  let employeeData = { employees: [], departments: [], positions: {}, writeConfigured: false };
  const employeeCanWrite = () =>
    $('meta[name="admin-is-admin"]')?.content === 'true' && employeeData.writeConfigured;

  async function loadEmployees() {
    $('#employeeSummary').textContent = '正在读取员工、部门和职务…';
    const [people, departments, positions] = await Promise.all([
      api('/api/admin/employees'),
      api('/api/admin/employees/departments'),
      api('/api/admin/employees/positions')
    ]);
    employeeData = {
      employees: Array.isArray(people?.items) ? people.items : [],
      departments: Array.isArray(departments?.items) ? departments.items : [],
      positions: positions && typeof positions === 'object' ? positions : {},
      writeConfigured: people?.writeConfigured === true
    };
    const count = Object.values(employeeData.positions).reduce((sum, jobs) => sum + (Array.isArray(jobs) ? jobs.length : 0), 0);
    const isAdmin = $('meta[name="admin-is-admin"]')?.content === 'true';
    $('#employeeSummary').textContent = `员工 ${employeeData.employees.length} 人 · 部门 ${employeeData.departments.length} 个 · 正式职务 ${count} 个` +
      (isAdmin && !employeeData.writeConfigured ? ' · 通讯录写入尚未配置' : '');
    $('#newEmployee').classList.toggle('hidden', !employeeCanWrite());
    $$('.employee-actions-head').forEach(x => x.classList.toggle('hidden', !employeeCanWrite()));
    $('#employeeDepartment').innerHTML = '<option value="">选择部门</option>' + employeeData.departments.map(d =>
      `<option value="${esc(d.id)}">${esc(d.name)}</option>`).join('');
    $('#employeePosition').innerHTML = '<option value="">选择正式职务</option>' + Object.entries(employeeData.positions).map(([center, jobs]) =>
      `<optgroup label="${esc(center)}">${(Array.isArray(jobs) ? jobs : []).map(job =>
        `<option value="${esc(job)}">${esc(job)}</option>`).join('')}</optgroup>`).join('');
    $('#employeeDepartments').innerHTML = employeeData.departments.map(d =>
      `<div>${esc(d.name)} <span class="muted">#${esc(d.id)}</span></div>`
    ).join('') || '暂无部门';
    $('#employeePositions').innerHTML = Object.entries(employeeData.positions).map(([center, jobs]) =>
      `<details><summary>${esc(center)}（${Array.isArray(jobs) ? jobs.length : 0}）</summary><p class="small">${(Array.isArray(jobs) ? jobs : []).map(esc).join(' · ')}</p></details>`
    ).join('') || '暂无职务';
    renderEmployees();
  }

  function renderEmployees() {
    const term = ($('#employeeSearch')?.value || '').trim().toLocaleLowerCase();
    const rows = employeeData.employees.filter(x =>
      [x.name, x.userId, x.position, ...(x.departments || [])].some(value => String(value || '').toLocaleLowerCase().includes(term))
    );
    $('#employeeRows').innerHTML = rows.map(x => `<tr>
      <td><b>${esc(x.name)}</b><br><small class="muted">${esc(x.userId)}</small></td>
      <td>${esc(x.position || '未设置')}</td>
      <td>${esc((x.departments || []).join('、') || '未设置')}</td>
      <td>${esc(x.status === 1 ? '在职' : `状态 ${x.status ?? '-'}`)}</td>
      <td>${esc(x.mobile || x.email || x.businessEmail || '-')}</td>
      <td class="employee-actions ${employeeCanWrite() ? '' : 'hidden'}">${employeeCanWrite() ?
        `<div class="row-actions"><button class="btn ghost small-btn" type="button" data-employee-transfer="${esc(x.userId)}">调岗</button><button class="btn ghost small-btn" type="button" data-employee-departure="${esc(x.userId)}" ${x.status === 1 ? '' : 'disabled'}>离职停用</button></div>` : ''}</td>
    </tr>`).join('') || '<tr><td colspan="6" class="empty">没有匹配的员工</td></tr>';
  }

  function openEmployeeForm(person) {
    if (!employeeCanWrite()) return;
    const form = $('#employeeForm');
    form.reset();
    form.dataset.mode = person ? 'transfer' : 'create';
    form.classList.remove('hidden');
    $('#employeeFormTitle').textContent = person ? `调岗：${person.name}` : '新增员工';
    $('#saveEmployee').textContent = person ? '确认调岗' : '新增员工';
    $('#employeeUserId').value = person?.userId || '';
    $('#employeeUserId').readOnly = !!person;
    $$('[data-employee-create-only]', form).forEach(x => x.classList.toggle('hidden', !!person));
    if (person) {
      $('#employeeName').value = person.name || '';
      $('#employeeDepartment').value = String(person.departmentIds?.[0] || '');
      $('#employeePosition').value = person.position || '';
    }
    form.scrollIntoView({ behavior: 'smooth', block: 'start' });
  }

  $('#newEmployee')?.addEventListener('click', () => openEmployeeForm(null));
  $('#cancelEmployeeEdit')?.addEventListener('click', () => $('#employeeForm').classList.add('hidden'));
  $('#employeeRows')?.addEventListener('click', async event => {
    if (!employeeCanWrite()) return;
    const transfer = event.target.closest('[data-employee-transfer]');
    if (transfer) {
      const person = employeeData.employees.find(x => x.userId === transfer.dataset.employeeTransfer);
      if (person) openEmployeeForm(person);
      return;
    }
    const departure = event.target.closest('[data-employee-departure]');
    if (!departure) return;
    const person = employeeData.employees.find(x => x.userId === departure.dataset.employeeDeparture);
    if (!person || !window.confirm(`确认将 ${person.name} 办理离职并停用企业微信账号？该成员记录会保留，可由管理员恢复。`)) return;
    setBusy(departure, true, '停用中…');
    try {
      await api(`/api/admin/employees/${encodeURIComponent(person.userId)}/departure`, { method: 'POST' });
      toast('成员账号已停用');
      await loadEmployees();
    } catch (error) { toast(error.message, 'error'); }
    finally { setBusy(departure, false); }
  });
  $('#employeeForm')?.addEventListener('submit', async event => {
    event.preventDefault();
    if (!employeeCanWrite()) return;
    const form = event.currentTarget;
    if (!form.reportValidity()) return;
    const data = Object.fromEntries(new FormData(form));
    data.departmentId = Number(data.departmentId);
    const transfer = form.dataset.mode === 'transfer';
    if (!transfer && !String(data.mobile || '').trim() && !String(data.email || '').trim()) return toast('手机号或邮箱至少填写一项', 'error');
    if (!window.confirm(transfer ? `确认调整 ${$('#employeeName').value} 的部门与职务？` : `确认新增员工 ${data.name}？`)) return;
    const button = $('#saveEmployee');
    setBusy(button, true);
    try {
      if (transfer) {
        await api(`/api/admin/employees/${encodeURIComponent(data.userId)}/assignment`, {
          method: 'PATCH', body: JSON.stringify({ departmentId: data.departmentId, position: data.position })
        });
      } else {
        await api('/api/admin/employees', { method: 'POST', body: JSON.stringify(data) });
      }
      toast(transfer ? '调岗已提交' : '员工已新增');
      form.classList.add('hidden');
      await loadEmployees();
    } catch (error) { toast(error.message, 'error'); }
    finally { setBusy(button, false); }
  });

  $('#employeeSearch')?.addEventListener('input', renderEmployees);
  $('#refreshEmployees')?.addEventListener('click', () => loadEmployees().catch(error => toast(error.message, 'error')));

  async function loadAudits() {
    const rows = (await api('/api/admin/audits')) || [];
    $('#auditRows').innerHTML = rows.map(x => `<tr>
      <td>${formatTime(x.createdAt)}</td><td>${esc(x.operatorName)}</td>
      <td>${esc(zh('action', x.action))}</td>
      <td>${esc(zh('target', x.targetType))}：${esc(x.targetId)}</td>
      <td>${esc(x.reason || '-')}</td><td>${esc(x.ipAddress || '-')}</td>
    </tr>`).join('') || '<tr><td colspan="6" class="empty">暂无操作审计</td></tr>';
  }

  function formatTime(value) {
    return value ? new Date(value).toLocaleString('zh-CN', { hour12: false }) : '-';
  }

  document.addEventListener('click', async event => {
    const nav = event.target.closest('[data-nav]');
    if (nav) return show(nav.dataset.nav, { communityTarget: nav.dataset.communityTarget });

    const hotServiceButton = event.target.closest('[data-service-hot]');
    if (hotServiceButton) {
      const slotNo = Number(hotServiceButton.dataset.serviceHot);
      const serviceId = Number(hotServiceButton.dataset.serviceId);
      setBusy(hotServiceButton, true, '设置中…');
      try {
        await api('/api/admin/slots', {
          method: 'PATCH',
          body: JSON.stringify({ slotNo, serviceId, mode: 'MANUAL', reason: `服务管理快速设为首页推荐位${slotNo}` })
        });
        toast(`服务已设为首页推荐位 ${slotNo}`);
        await loadServices();
      } catch (error) {
        toast(error.message, 'error');
      } finally {
        setBusy(hotServiceButton, false);
      }
      return;
    }

    const editServiceButton = event.target.closest('[data-service-edit]');
    if (editServiceButton) return openService(services.find(x => x.id === Number(editServiceButton.dataset.serviceEdit)));


    const offlineServiceButton = event.target.closest('[data-service-offline]');
    if (offlineServiceButton) {
      const id=Number(offlineServiceButton.dataset.serviceOffline); if(!confirm('确认下架这个服务项目？历史询价线索和履约订单会保留。'))return;
      setBusy(offlineServiceButton,true,'下架中…'); try{await api(`/api/admin/services/${id}`,{method:'DELETE'}); toast('服务项目已安全下架'); await loadServices();}catch(error){toast(error.message,'error')}finally{setBusy(offlineServiceButton,false)} return;
    }
    const restoreServiceButton = event.target.closest('[data-service-restore]');
    if (restoreServiceButton) {
      const id=Number(restoreServiceButton.dataset.serviceRestore); setBusy(restoreServiceButton,true,'恢复中…'); try{await api(`/api/admin/services/${id}/restore`,{method:'POST'}); toast('服务项目已恢复上架'); await loadServices();}catch(error){toast(error.message,'error')}finally{setBusy(restoreServiceButton,false)} return;
    }
    const gameSelect=event.target.closest('[data-game-select]'); if(gameSelect){const code=gameSelect.dataset.gameSelect;if(gameSelect.checked)selectedGameCodes.add(code);else selectedGameCodes.delete(code);updateGameBulkBar();return;}
    const editGameButton=event.target.closest('[data-taxonomy-edit-game]'); if(editGameButton){const item=taxonomy.games.find(x=>x.code===editGameButton.dataset.taxonomyEditGame); if(item)return openTaxonomyEditor('game',item);}
    const editCategoryButton=event.target.closest('[data-taxonomy-edit-category]'); if(editCategoryButton){const item=taxonomy.categories.find(x=>String(x.id)===editCategoryButton.dataset.taxonomyEditCategory); if(item)return openTaxonomyEditor('category',item);}
    const deleteGameButton=event.target.closest('[data-taxonomy-delete-game]'); if(deleteGameButton){if(!confirm('仅无依赖游戏可以永久删除。确认继续？'))return; try{await api(`/api/admin/catalog/games/${encodeURIComponent(deleteGameButton.dataset.taxonomyDeleteGame)}`,{method:'DELETE'});toast('游戏已永久删除');await loadCatalogManager();}catch(error){toast(error.message,'error')}return;}
    const deleteCategoryButton=event.target.closest('[data-taxonomy-delete-category]'); if(deleteCategoryButton){if(!confirm('仅无服务项目依赖的需求类型可以永久删除。确认继续？'))return; try{await api(`/api/admin/catalog/categories/${deleteCategoryButton.dataset.taxonomyDeleteCategory}`,{method:'DELETE'});toast('需求类型已永久删除');await loadCatalogManager();}catch(error){toast(error.message,'error')}return;}

    const convertButton = event.target.closest('[data-quote-convert]');
    if (convertButton) {
      const id = convertButton.dataset.quoteConvert;
      if (!confirm('确认将这条询价生成履约订单？业务编号会保持不变。')) return;
      setBusy(convertButton, true, '生成中…');
      try {
        const order = await api(`/api/admin/quotes/${id}/convert`, {
          method: 'POST', body: JSON.stringify({ reason: '后台确认询价并生成履约订单' })
        });
        toast(`已生成履约订单，业务编号保持不变：${order.businessNo || order.orderNo}`);
        try { await navigator.clipboard.writeText(order.businessNo || order.orderNo); toast('业务编号已复制'); } catch {}
        await Promise.all([loadQuotes(), loadOrders()]);
      } catch (error) { toast(error.message, 'error'); }
      finally { setBusy(convertButton, false); }
      return;
    }

    const deleteQuoteButton = event.target.closest('[data-quote-delete]');
    if (deleteQuoteButton) {
      if (!confirm('确认删除这条询价线索？已转出的履约订单不会一起删除。')) return;
      setBusy(deleteQuoteButton, true, '删除中…');
      try {
        await api(`/api/admin/quotes/${deleteQuoteButton.dataset.quoteDelete}`, { method: 'DELETE' });
        toast('询价线索已删除');
        setBusy(deleteQuoteButton, false);
        await loadQuotes();
      } catch (error) { toast(error.message, 'error'); }
      finally { setBusy(deleteQuoteButton, false); }
      return;
    }

    const editOrderButton = event.target.closest('[data-order-edit]');
    if (editOrderButton) return editOrder(editOrderButton.dataset.orderEdit);

    const deleteOrderButton = event.target.closest('[data-order-delete]');
    if (deleteOrderButton) {
      if (!confirm('确认删除这个履约订单？删除后客户将无法用该业务编号查询进度。')) return;
      setBusy(deleteOrderButton, true, '删除中…');
      try {
        await api(`/api/admin/orders/${encodeURIComponent(deleteOrderButton.dataset.orderDelete)}`, { method: 'DELETE' });
        toast('履约订单已删除');
        setBusy(deleteOrderButton, false);
        await loadOrders();
      } catch (error) { toast(error.message, 'error'); }
      finally { setBusy(deleteOrderButton, false); }
      return;
    }

    const copyOrderButton = event.target.closest('[data-order-copy]');
    if (copyOrderButton) {
      try { await navigator.clipboard.writeText(copyOrderButton.dataset.orderCopy); toast('业务编号已复制'); }
      catch { toast('复制失败，请手动复制', 'error'); }
      return;
    }

    const publishCaseButton = event.target.closest('[data-order-case]');
    if (publishCaseButton) {
      const order = orders.find(x => x.orderNo === publishCaseButton.dataset.orderCase);
      const fallback = order ? `${zh('orderStatus', order.status)} · ${order.progressText || order.customerNote || order.businessNo || order.orderNo}` : '';
      const summary = window.prompt('填写前台案例摘要（会展示在首页近期完成）', fallback);
      if (summary == null) return;
      setBusy(publishCaseButton, true, '保存中…');
      try {
        await api(`/api/admin/orders/${encodeURIComponent(publishCaseButton.dataset.orderCase)}/public-case`, {
          method: 'PATCH', body: JSON.stringify({ enabled: true, summary: summary.trim() || fallback })
        });
        toast('已设为前台案例');
      } catch (error) { toast(error.message, 'error'); }
      finally { setBusy(publishCaseButton, false); }
      return;
    }

    const unpublishCaseButton = event.target.closest('[data-order-case-off]');
    if (unpublishCaseButton) {
      if (!confirm('确认从前台近期完成中移除这个案例？')) return;
      setBusy(unpublishCaseButton, true, '取消中…');
      try {
        await api(`/api/admin/orders/${encodeURIComponent(unpublishCaseButton.dataset.orderCaseOff)}/public-case`, {
          method: 'PATCH', body: JSON.stringify({ enabled: false, summary: null })
        });
        toast('已取消前台案例');
      } catch (error) { toast(error.message, 'error'); }
      finally { setBusy(unpublishCaseButton, false); }
      return;
    }

    const feedbackCopy = event.target.closest('[data-feedback-copy]');
    if (feedbackCopy) {
      const item = feedbackRows.find(x => String(x.id) === String(feedbackCopy.dataset.feedbackCopy));
      if (!item) return;
      const text = `网页测试修改反馈 #${item.id}\n标题：${item.title}\n页面：${item.pagePath || '-'}\n优先级：${zh('feedbackPriority', item.priority)}\n状态：${zh('feedbackStatus', item.status)}\n附件：${item.attachmentAssetId ? `/api/admin/test-feedback/${item.id}/attachment` : '-'}\n内容：${item.content}\n补充：${item.operatorNote || '-'}`;
      try { await navigator.clipboard.writeText(text); toast('反馈内容已复制'); }
      catch { toast('复制失败，请手动复制', 'error'); }
      return;
    }

    const saveSlot = event.target.closest('[data-slot-save]');
    if (saveSlot) {
      const n = Number(saveSlot.dataset.slotSave);
      const mode = $(`[data-slot-mode="${n}"]`).value;
      const serviceId = mode === 'MANUAL' ? Number($(`[data-slot-service="${n}"]`).value) || null : null;
      if (mode === 'MANUAL' && !serviceId) return toast('手动指定模式必须选择服务', 'error');
      setBusy(saveSlot, true, '保存中…');
      try {
        await api('/api/admin/slots', {
          method: 'PATCH', body: JSON.stringify({ slotNo: n, serviceId, mode, reason: '后台调整首页推荐位' })
        });
        toast('展示位已保存');
        await loadSlots();
      } catch (error) { toast(error.message, 'error'); }
      finally { setBusy(saveSlot, false); }
    }
  });

  document.addEventListener('change', async event => {
    const quoteStatus = event.target.closest('[data-quote-status]');
    if (quoteStatus) {
      try {
        await api(`/api/admin/quotes/${quoteStatus.dataset.quoteStatus}/status`, {
          method: 'PATCH', body: JSON.stringify({ status: quoteStatus.value, reason: '后台更新询价状态' })
        });
        toast('询价状态已更新');
        await loadQuotes();
      } catch (error) { toast(error.message, 'error'); }
    }

    const slotMode = event.target.closest('[data-slot-mode]');
    if (slotMode) updateSlotServiceVisibility(slotMode.dataset.slotMode);

    const feedbackStatus = event.target.closest('[data-feedback-status]');
    if (feedbackStatus) {
      const item = feedbackRows.find(x => String(x.id) === String(feedbackStatus.dataset.feedbackStatus));
      if (!item) return;
      try {
        await api(`/api/admin/test-feedback/${item.id}`, {
          method: 'PUT',
          body: JSON.stringify({
            title: item.title,
            pagePath: item.pagePath,
            content: item.content,
            priority: item.priority,
            status: feedbackStatus.value,
            operatorNote: item.operatorNote
          })
        });
        toast('反馈状态已更新');
        await loadFeedback();
      } catch (error) { toast(error.message, 'error'); }
    }
  });

  /* ----------------------------------------- 用户系统：评论 / 投稿 / 用户账号 */

  let communityBound = false;

  function ensureCommunityBound() {
    if (!communityBound) {
      bindCommunity();
      communityBound = true;
    }
  }

  async function loadUserPanel() {
    ensureCommunityBound();
    await Promise.all([loadUsers(), loadOperationLogs()]);
  }

  async function loadCommunity() {
    ensureCommunityBound();
    await Promise.all([loadCommentQueue(), loadSubmissionQueue()]);
  }

  function activateCommunitySection(target) {
    if (!target) return;
    $$('[data-community-nav]').forEach(x => x.classList.toggle('active', x.dataset.communityNav === target));
    $$('[data-community-section]').forEach(section => {
      section.classList.toggle('hidden', section.dataset.communitySection !== target);
    });
  }

  function bindCommunity() {
    $$('[data-community-nav]').forEach(button => button.addEventListener('click', () => {
      activateCommunitySection(button.dataset.communityNav);
    }));
    const reload = () => loadCommunity().catch(error => toast(error.message, 'error'));
    $('#reloadCommunity')?.addEventListener('click', reload);
    $('#loadCommentsBtn')?.addEventListener('click', () => loadCommentQueue().catch(e => toast(e.message, 'error')));
    $('#loadSubmissionsBtn')?.addEventListener('click', () => loadSubmissionQueue().catch(e => toast(e.message, 'error')));
    $('#adminCommentStatus')?.addEventListener('change', () => loadCommentQueue().catch(e => toast(e.message, 'error')));
    $('#adminSubmissionStatus')?.addEventListener('change', () => loadSubmissionQueue().catch(e => toast(e.message, 'error')));
    $('#loadUsersBtn')?.addEventListener('click', () => loadUsers().catch(e => toast(e.message, 'error')));
    $('#adminUserSearch')?.addEventListener('input', () => loadUsers().catch(e => toast(e.message, 'error')));
    $('#createUserBtn')?.addEventListener('click', createUser);
    $('#loadOpsBtn')?.addEventListener('click', () => loadOperationLogs().catch(e => toast(e.message, 'error')));
  }

  function communityRow(title, meta, actions) {
    const row = document.createElement('div');
    row.className = 'card';
    const head = document.createElement('div');
    head.className = 'admin-toolbar';
    const left = document.createElement('div');
    const h = document.createElement('h3');
    h.textContent = title;
    left.appendChild(h);
    if (meta) {
      const small = document.createElement('p');
      small.className = 'muted small';
      small.textContent = meta;
      left.appendChild(small);
    }
    head.appendChild(left);
    if (actions && actions.length) {
      const box = document.createElement('div');
      box.className = 'row-actions';
      actions.forEach(a => box.appendChild(a));
      head.appendChild(box);
    }
    row.appendChild(head);
    return row;
  }

  function actionButton(label, kind, handler) {
    const button = document.createElement('button');
    button.type = 'button';
    button.className = kind === 'primary' ? 'btn primary small-btn' : 'btn ghost small-btn';
    button.textContent = label;
    button.addEventListener('click', async () => {
      button.disabled = true;
      try {
        await handler();
      } catch (error) {
        toast(error.message, 'error');
      } finally {
        button.disabled = false;
      }
    });
    return button;
  }

  async function loadCommentQueue() {
    const host = $('#adminCommentList');
    if (!host) return;
    const status = $('#adminCommentStatus')?.value || 'PENDING';
    host.textContent = '加载中…';
    let rows;
    try {
      rows = await api(`/api/admin/account/comments?status=${encodeURIComponent(status)}`);
    } catch (error) {
      // 原先没有 try/catch，接口失败时列表会永久停在「加载中…」，
      // 管理员以为还在加载。show() 只弹一个 toast，不会改这里。
      host.textContent = `加载失败：${error && error.message ? error.message : '请稍后重试'}`;
      return;
    }
    host.textContent = '';
    if (!rows || !rows.length) {
      host.textContent = '当前队列为空。';
      return;
    }
    rows.forEach(item => {
      const actions = [];
      if (status !== 'PUBLISHED') {
        actions.push(actionButton('通过', 'primary', async () => {
          await api(`/api/admin/account/comments/${item.id}/review`, {
            method: 'POST', body: JSON.stringify({ approve: true, note: '后台审核通过' })
          });
          toast('评论已通过');
          await loadCommentQueue();
        }));
      }
      if (status !== 'REJECTED') {
        actions.push(actionButton('驳回', 'ghost', async () => {
          await api(`/api/admin/account/comments/${item.id}/review`, {
            method: 'POST', body: JSON.stringify({ approve: false, note: '后台审核驳回' })
          });
          toast('评论已驳回');
          await loadCommentQueue();
        }));
      }
      actions.push(actionButton('删除', 'ghost', async () => {
        await api(`/api/admin/account/comments/${item.id}?note=后台删除`, { method: 'DELETE' });
        toast('评论已删除');
        await loadCommentQueue();
      }));
      // 评论正文只走 textContent：后台同样不能把用户内容当 HTML 插进 DOM
      const row = communityRow(item.authorName || '匿名用户', `${formatTime(item.createdAt)} · ${item.status}`,
        actions);
      const body = document.createElement('p');
      body.textContent = item.content;
      row.appendChild(body);
      host.appendChild(row);
    });
  }

  async function loadSubmissionQueue() {
    const host = $('#adminSubmissionList');
    if (!host) return;
    const status = $('#adminSubmissionStatus')?.value || 'SUBMITTED';
    host.textContent = '加载中…';
    let rows;
    try {
      rows = await api(`/api/admin/account/submissions?status=${encodeURIComponent(status)}`);
    } catch (error) {
      host.textContent = `加载失败：${error && error.message ? error.message : '请稍后重试'}`;
      return;
    }
    host.textContent = '';
    if (!rows || !rows.length) {
      host.textContent = '当前队列为空。';
      return;
    }
    for (const item of rows) {
      const actions = [];
      actions.push(actionButton('通过', 'primary', async () => {
        await api(`/api/admin/account/submissions/${item.id}/review`, {
          method: 'POST', body: JSON.stringify({ status: 'APPROVED', note: '后台审核通过' })
        });
        toast('投稿已通过');
        await loadSubmissionQueue();
      }));
      actions.push(actionButton('驳回', 'ghost', async () => {
        await api(`/api/admin/account/submissions/${item.id}/review`, {
          method: 'POST', body: JSON.stringify({ status: 'REJECTED', note: '后台审核驳回' })
        });
        toast('投稿已驳回');
        await loadSubmissionQueue();
      }));
      actions.push(actionButton('标记已发布', 'ghost', async () => {
        const slug = window.prompt('填写发布后的攻略 slug（需与 game_guides.slug 一致）');
        if (!slug) return;
        await api(`/api/admin/account/submissions/${item.id}/publish`, {
          method: 'POST', body: JSON.stringify({ slug })
        });
        toast('已标记为发布');
        await loadSubmissionQueue();
      }));
      const row = communityRow(item.title || '(无标题)',
        `${item.gameCode} · ${item.statusText || item.status} · ${formatTime(item.createdAt)}`, actions);
      if (item.summary) {
        const p = document.createElement('p');
        p.className = 'muted small';
        p.textContent = item.summary;
        row.appendChild(p);
      }
      host.appendChild(row);
    }
  }

  async function loadUsers() {
    const host = $('#adminUserList');
    if (!host) return;
    host.textContent = '加载中...';
    let rows;
    try {
      rows = await api('/api/admin/account/users?page=0&size=100');
    } catch (error) {
      host.textContent = `加载失败：${error && error.message ? error.message : '请稍后重试'}`;
      return;
    }
    const q = ($('#adminUserSearch')?.value || '').trim().toLowerCase();
    if (q) {
      rows = (rows || []).filter(item => [item.publicUserId, item.email, item.displayName, item.role, item.status]
        .some(value => String(value || '').toLowerCase().includes(q)));
    }
    host.textContent = '';
    if (!rows || !rows.length) {
      host.textContent = '暂无账号。';
      return;
    }
    rows.forEach(item => host.appendChild(userRow(item)));
  }

  function userRow(item) {
    const title = item.email || `账号 #${item.id}`;
    const meta = `${item.publicUserId || ('#' + item.id)} · ${item.displayName || '未命名'} · ${item.role || 'BUYER'} · ${item.status || 'ACTIVE'} · ${item.emailVerified ? '邮箱已验证' : '邮箱未验证'}`;
    const actions = [
      actionButton('编辑', 'ghost', () => editUser(item)),
      actionButton('禁用', 'ghost', async () => {
        if (!window.confirm(`确认禁用 ${item.email || item.id}？`)) return;
        await api(`/api/admin/account/users/${item.id}/disable`, { method: 'POST' });
        toast('账号已禁用');
        await loadUsers();
      }),
      actionButton(item.emailVerified ? '取消认证' : '邮箱认证', 'ghost', () => toggleUserEmailVerified(item)),
      actionButton('重置密码', 'ghost', () => resetUserPassword(item)),
      actionButton('登录日志', 'ghost', () => showUserLoginLogs(item)),
      actionButton('删除', 'ghost', async () => {
        if (!window.confirm(`确认彻底删除 ${item.email || item.publicUserId || item.id}？该账号会从账号表中直接删除。`)) return;
        await api(`/api/admin/account/users/${item.id}`, { method: 'DELETE' });
        toast('账号已删除');
        await loadUsers();
      })
    ];
    const row = communityRow(title, meta, actions);
    const detail = document.createElement('p');
    detail.className = 'muted small';
    detail.textContent = `专属ID：${item.publicUserId || '待生成'} · 创建：${formatTime(item.createdAt)} · 最近登录：${item.lastLoginAt ? formatTime(item.lastLoginAt) : '暂无'}`;
    row.appendChild(detail);
    return row;
  }

  async function createUser() {
    const button = $('#createUserBtn');
    setBusy(button, true, '创建中...');
    try {
      const body = {
        email: ($('#adminUserEmail')?.value || '').trim(),
        displayName: ($('#adminUserDisplayName')?.value || '').trim(),
        password: $('#adminUserPassword')?.value || '',
        role: $('#adminUserRole')?.value || 'BUYER',
        emailVerified: Boolean($('#adminUserVerified')?.checked)
      };
      await api('/api/admin/account/users', { method: 'POST', body: JSON.stringify(body) });
      ['#adminUserEmail', '#adminUserDisplayName', '#adminUserPassword'].forEach(id => { const el = $(id); if (el) el.value = ''; });
      toast('账号已创建');
      await loadUsers();
      await loadOperationLogs();
    } catch (error) { toast(error.message, 'error'); }
    finally { setBusy(button, false); }
  }

  async function editUser(item) {
    const displayName = window.prompt('昵称', item.displayName || '');
    if (displayName === null) return;
    const role = window.prompt('角色（BUYER / SELLER / BOOSTER / ADMIN）', item.role || 'BUYER');
    if (role === null) return;
    const status = window.prompt('状态（ACTIVE / LOCKED / DISABLED / DELETED）', item.status || 'ACTIVE');
    if (status === null) return;
    await api(`/api/admin/account/users/${item.id}`, {
      method: 'POST',
      body: JSON.stringify({ displayName, role, status })
    });
    toast('账号已更新');
    await loadUsers();
    await loadOperationLogs();
  }

  async function toggleUserEmailVerified(item) {
    const verified = !item.emailVerified;
    if (!window.confirm(`${verified ? '手动设为邮箱已认证' : '取消邮箱认证'}：${item.email || item.publicUserId || item.id}？`)) return;
    await api(`/api/admin/account/users/${item.id}/email-verified`, {
      method: 'POST',
      body: JSON.stringify({ verified })
    });
    toast(verified ? '邮箱已手动认证' : '邮箱认证已取消');
    await loadUsers();
    await loadOperationLogs();
  }

  async function resetUserPassword(item) {
    const newPassword = window.prompt(`输入 ${item.email || item.id} 的新密码`);
    if (!newPassword) return;
    await api(`/api/admin/account/users/${item.id}/reset-password`, {
      method: 'POST',
      body: JSON.stringify({ newPassword })
    });
    toast('密码已重置');
    await loadOperationLogs();
  }

  async function showUserLoginLogs(item) {
    const rows = await api(`/api/admin/account/users/${item.id}/login-logs?size=30`);
    const message = renderAuditSummary(rows || []);
    window.alert(message || '暂无登录日志');
  }

  async function loadOperationLogs() {
    const host = $('#adminOperationLogList');
    if (!host) return;
    host.textContent = '加载中...';
    let rows;
    try {
      rows = await api('/api/admin/account/operation-logs?size=80');
    } catch (error) {
      host.textContent = `加载失败：${error && error.message ? error.message : '请稍后重试'}`;
      return;
    }
    host.textContent = '';
    if (!rows || !rows.length) {
      host.textContent = '暂无操作日志。';
      return;
    }
    rows.slice(0, 20).forEach(item => {
      const row = communityRow(item.action || '操作', `${formatTime(item.createdAt)} · 用户 #${item.userId || '-'} · ${item.ip || '-'}`, []);
      const detail = document.createElement('p');
      detail.className = 'muted small';
      detail.textContent = item.detail || item.userAgent || '';
      row.appendChild(detail);
      host.appendChild(row);
    });
  }

  function renderAuditSummary(rows) {
    return rows.slice(0, 30).map(item => `${formatTime(item.createdAt)}  ${item.action || ''}  ${item.ip || '-'}  ${item.detail || ''}`).join('\n');
  }

  function initAdminCyberVisuals() {
    document.body.classList.add('admin-solid-ui');
  }

  // 必须写在 IIFE 内部：renderTrackingFunnel 定义在本 IIFE 中，
  // 原先这行在 })(); 之外，管理员切换「统计窗口」时会抛
  // ReferenceError: renderTrackingFunnel is not defined，漏斗图永不刷新。
  document.getElementById('trackingDays')?.addEventListener('change', () => { renderTrackingFunnel(); });

  initAdminCyberVisuals();
  show('dashboard');
})();

