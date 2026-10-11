(() => {
  'use strict';

  const $ = (selector, root = document) => root.querySelector(selector);
  const $$ = (selector, root = document) => [...root.querySelectorAll(selector)];
  const csrf = $('meta[name="_csrf"]')?.content;
  const csrfHeader = $('meta[name="_csrf_header"]')?.content;

  const MODULES = {
    desktop: [
      ['hero', 'Hero 首屏'],
      ['command', '询价 / 进度 / 客服工作区'],
      ['game', '游戏入口'],
      ['why', '为什么选择星火'],
      ['hot', '热门服务'],
      ['services', '服务项目'],
      ['tool', '工具与交付说明'],
      ['cases', '近期完成记录'],
      ['faq', '常见问题']
    ],
    mobile: [
      ['hero', 'Hero 首屏'],
      ['launcher', '四宫格快捷入口'],
      ['game', '游戏入口'],
      ['quote', '快速询价'],
      ['support', '进度 / 客服双卡'],
      ['hot', '热门服务'],
      ['services', '服务项目'],
      ['why', '为什么选择星火'],
      ['tool', 'MW4 工具'],
      ['cases', '近期完成记录'],
      ['faq', '常见问题']
    ]
  };

  let scope = 'global';
  let previewDevice = 'desktop';
  let workspace = null;
  let formDirty = false;
  let busy = false;

  function bool(value, fallback = true) {
    if (value == null || value === '') return fallback;
    return String(value).toLowerCase() === 'true';
  }

  async function api(url, options = {}) {
    const method = String(options.method || 'GET').toUpperCase();
    const headers = { ...(options.body ? { 'Content-Type': 'application/json' } : {}), ...(options.headers || {}) };
    if (csrf && csrfHeader && !['GET', 'HEAD'].includes(method)) headers[csrfHeader] = csrf;
    const response = await fetch(url, { ...options, headers, cache: 'no-store' });
    let payload = null;
    try { payload = await response.json(); } catch (_) {}
    if (response.status === 401 || response.status === 403) {
      location.assign('/admin/login');
      throw new Error('登录已失效，请重新登录');
    }
    if (!response.ok) throw new Error(payload?.message || '请求失败，请稍后重试');
    return payload;
  }

  function toast(message, type = 'success') {
    const area = $('#toastArea');
    if (!area) return;
    const item = document.createElement('div');
    item.className = `toast ${type === 'error' ? 'error' : ''}`;
    item.textContent = message;
    area.appendChild(item);
    setTimeout(() => item.remove(), 3000);
  }

  function scopeLabel(value = scope) {
    return value === 'desktop' ? '电脑端' : value === 'mobile' ? '手机端' : '全局';
  }

  function ensureStyles() {
    if ($('#frontendControlV31Styles')) return;
    const style = document.createElement('style');
    style.id = 'frontendControlV31Styles';
    style.textContent = `
      .fc-v31-form-grid{display:grid!important;grid-template-columns:minmax(0,1fr) 360px;gap:18px;align-items:start}
      .fc-v31-editor{min-width:0;display:grid;gap:18px}.fc-v31-editor>.fc-card{margin:0}
      .fc-v31-preview-side{position:sticky;top:78px;display:grid;gap:12px;min-width:0}
      .fc-v31-panel{border:1px solid var(--fc-line);border-radius:16px;background:#fff;padding:14px;box-shadow:0 12px 30px rgba(15,23,42,.06)}
      .fc-v31-panel-head{display:flex;justify-content:space-between;gap:10px;align-items:flex-start;margin-bottom:12px}.fc-v31-panel-head b,.fc-v31-panel-head small{display:block}.fc-v31-panel-head small{color:var(--fc-muted);margin-top:3px}
      .fc-v31-status{display:flex;gap:6px;flex-wrap:wrap}.fc-v31-pill{display:inline-flex;align-items:center;gap:5px;padding:5px 8px;border-radius:999px;background:#f1f5f9;color:#475569;font-size:11px;font-weight:800}.fc-v31-pill.live{background:#ecfdf5;color:#047857}.fc-v31-pill.draft{background:#fff7ed;color:#c2410c}
      .fc-v31-actions{display:grid;grid-template-columns:1fr 1fr;gap:8px}.fc-v31-actions .btn{width:100%}.fc-v31-actions .wide{grid-column:1/-1}
      .fc-v31-device-tabs{display:flex;gap:6px}.fc-v31-device-tabs button{flex:1}
      .fc-v31-canvas-wrap{padding:10px;border-radius:14px;background:#eef2f7;overflow:hidden}
      .fc-v31-canvas{--p-accent:#f4bd34;--p-bg:#0b0d10;--p-surface:#171b21;--p-text:#f5f6f7;background:var(--p-bg);color:var(--p-text);border-radius:14px;overflow:hidden;box-shadow:0 18px 34px rgba(15,23,42,.18);transition:.2s ease}
      .fc-v31-canvas.desktop{width:100%;min-height:430px}.fc-v31-canvas.mobile{width:232px;min-height:430px;margin:0 auto;border-radius:24px;border:5px solid #0f172a}
      .fc-v31-preview-header{height:34px;display:flex;align-items:center;justify-content:space-between;padding:0 10px;border-bottom:1px solid rgba(255,255,255,.12);font-size:8px}.fc-v31-preview-brand{font-weight:900;color:var(--p-accent)}
      .fc-v31-preview-main{display:grid;gap:7px;padding:8px}.fc-v31-module{border:1px solid rgba(255,255,255,.1);background:var(--p-surface);border-radius:10px;padding:9px;min-height:34px}.fc-v31-module b{display:block;font-size:8px}.fc-v31-module small{display:block;font-size:6px;opacity:.65;margin-top:2px}.fc-v31-module.hero{display:grid;align-content:center;min-height:82px;background:linear-gradient(135deg,var(--p-surface),color-mix(in srgb,var(--p-accent) 16%,var(--p-surface)))}
      .fc-v31-module.hero strong{font-size:11px}.fc-v31-module.hero span{font-size:6px;opacity:.72;margin-top:3px}.fc-v31-preview-cta{display:inline-flex;width:max-content;margin-top:6px;padding:4px 7px;border-radius:7px;background:var(--p-accent);color:#111827;font-size:6px;font-weight:900}
      .fc-v31-preview-dock{display:grid;grid-template-columns:repeat(5,1fr);gap:2px;padding:6px;border-top:1px solid rgba(255,255,255,.1);background:var(--p-surface)}.fc-v31-preview-dock span{text-align:center;font-size:5px;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}.fc-v31-preview-dock span:last-child{color:var(--p-accent);font-weight:900}
      .fc-v31-preview-footer{height:24px;display:grid;place-items:center;font-size:5px;opacity:.58;border-top:1px solid rgba(255,255,255,.1)}
      .fc-v31-note{margin:8px 0 0;color:var(--fc-muted);font-size:12px;line-height:1.6}.fc-v31-inherit{color:#0369a1;font-weight:800}
      .fc-v31-form-grid .fc-sticky-save{position:sticky;bottom:10px;z-index:9}.fc-v31-form-grid .fc-sticky-save .fc-v31-publish{margin-left:8px}
      @media(max-width:1180px){.fc-v31-form-grid{grid-template-columns:1fr}.fc-v31-preview-side{position:relative;top:auto}.fc-v31-canvas.desktop{max-width:720px;margin:0 auto}}
      @media(max-width:680px){.fc-v31-actions{grid-template-columns:1fr}.fc-v31-actions .wide{grid-column:auto}.fc-v31-canvas.mobile{width:220px}}
    `;
    document.head.appendChild(style);
  }

  function ensureWorkspaceLayout() {
    const form = $('#frontendControlsForm');
    if (!form || form.classList.contains('fc-v31-form-grid')) return;
    const editor = document.createElement('div');
    editor.className = 'fc-v31-editor';
    [...form.children].forEach(child => editor.appendChild(child));

    const aside = document.createElement('aside');
    aside.className = 'fc-v31-preview-side';
    aside.innerHTML = `
      <section class="fc-v31-panel">
        <div class="fc-v31-panel-head"><div><b>V3.1 发布工作区</b><small id="fcV31ScopeText">全局 · 已发布</small></div><div class="fc-v31-status"><span class="fc-v31-pill live" id="fcV31PublishedPill">线上</span><span class="fc-v31-pill draft" id="fcV31DraftPill" hidden>草稿</span></div></div>
        <div class="fc-v31-actions">
          <button type="button" class="btn primary wide" id="fcV31Publish">发布到前台</button>
          <button type="button" class="btn ghost" id="fcV31Discard">丢弃草稿</button>
          <button type="button" class="btn ghost" id="fcV31Restore">恢复上一版</button>
        </div>
        <p class="fc-v31-note" id="fcV31StateNote">保存草稿不会影响线上页面；只有“发布到前台”才会替换正式配置。</p>
      </section>
      <section class="fc-v31-panel">
        <div class="fc-v31-panel-head"><div><b>实时草稿预览</b><small>示意布局，不调用业务写接口</small></div></div>
        <div class="fc-v31-device-tabs"><button type="button" class="btn primary" data-v31-preview-device="desktop">电脑端</button><button type="button" class="btn ghost" data-v31-preview-device="mobile">手机端</button></div>
        <div class="fc-v31-canvas-wrap"><div class="fc-v31-canvas desktop" id="fcV31Canvas"></div></div>
        <p class="fc-v31-note">预览会实时反映模块顺序、显隐、Hero 高度、主题色和手机 Dock 文案；正式前台仍以发布后的配置为准。</p>
      </section>`;

    form.append(editor, aside);
    form.classList.add('fc-v31-form-grid');

    const submit = $('.fc-sticky-save button[type="submit"]', editor);
    if (submit) submit.textContent = '保存草稿';
    const stateTitle = $('.fc-sticky-save b', editor);
    if (stateTitle) stateTitle.textContent = '首页、布局与主题草稿';

    const reset = $('#resetFrontendScope');
    if (reset) reset.textContent = '草稿恢复继承全局';
  }

  function modernizeLabels() {
    document.title = '前端控制中心 V3.1 - 星火网游';
    const brand = $('.fc-brand strong');
    if (brand) brand.textContent = '前端控制中心 V3.1';
    const eyebrow = $('.fc-hero .eyebrow');
    if (eyebrow) eyebrow.textContent = 'Frontend Control Center V3.1';
    const heroTitle = $('.fc-hero h1');
    if (heroTitle) heroTitle.textContent = '预览、草稿与安全发布';
    const heroCopy = $('.fc-hero p');
    if (heroCopy) heroCopy.textContent = '先在草稿工作区调整电脑端 / 手机端布局与主题，实时预览确认后再发布；正式前台只读取已发布配置。';
    const status = $('.fc-status-card');
    if (status) status.innerHTML = '<b>Draft → Preview → Publish</b><small>支持一键恢复上一版</small>';
  }

  function currentScopeFromUi() {
    return $('[data-frontend-scope].primary')?.dataset.frontendScope || scope || 'global';
  }

  function fillForm(values = {}) {
    const form = $('#frontendControlsForm');
    if (!form) return;
    $$('[name]', form).forEach(field => {
      if (!(field.name in values)) return;
      if (field.type === 'checkbox') field.checked = bool(values[field.name], false);
      else field.value = values[field.name] ?? '';
    });
  }

  function collectForm(reason = '前端控制中心 V3.1 草稿') {
    const form = $('#frontendControlsForm');
    const payload = { reason };
    if (!form) return payload;
    syncModuleOrderInput();
    $$('[name]', form).forEach(field => {
      payload[field.name] = field.type === 'checkbox' ? field.checked : String(field.value ?? '').trim();
    });
    return payload;
  }

  function moduleDefinitions(device = previewDevice) {
    return MODULES[device] || MODULES.desktop;
  }

  function normalizedOrder(raw, device = previewDevice) {
    const defs = moduleDefinitions(device);
    const allowed = new Set(defs.map(([id]) => id));
    const requested = String(raw || '').split(',').map(value => value.trim()).filter(id => allowed.has(id));
    return [...new Set([...requested, ...defs.map(([id]) => id)])];
  }

  function renderOrder(raw = '') {
    const list = $('#homeModuleOrderList');
    const input = $('#frontendControlsForm [name="homeModuleOrder"]');
    if (!list || !input) return;
    list.innerHTML = '';
    if (scope === 'global') {
      input.value = '';
      list.innerHTML = '<div class="fc-order-empty">全局层不强制统一 PC / Mobile 信息架构；右侧可分别预览两端默认顺序。</div>';
      return;
    }
    const defs = new Map(MODULES[scope] || []);
    const order = normalizedOrder(raw, scope);
    order.forEach((id, index) => {
      const row = document.createElement('div');
      row.className = 'fc-order-row';
      row.dataset.homeModuleId = id;
      const num = document.createElement('span');
      num.className = 'fc-order-index';
      num.textContent = String(index + 1).padStart(2, '0');
      const copy = document.createElement('span');
      copy.className = 'fc-order-copy';
      const title = document.createElement('b');
      title.textContent = defs.get(id) || id;
      const small = document.createElement('small');
      small.textContent = id;
      copy.append(title, small);
      const actions = document.createElement('span');
      actions.className = 'fc-order-actions';
      actions.innerHTML = '<button type="button" class="btn ghost" data-order-move="up" aria-label="上移">↑</button><button type="button" class="btn ghost" data-order-move="down" aria-label="下移">↓</button>';
      row.append(num, copy, actions);
      list.appendChild(row);
    });
    syncModuleOrderInput();
    syncOrderButtons();
  }

  function syncModuleOrderInput() {
    const input = $('#frontendControlsForm [name="homeModuleOrder"]');
    const list = $('#homeModuleOrderList');
    if (!input || !list) return;
    if (scope === 'global') {
      input.value = '';
      return;
    }
    input.value = $$('[data-home-module-id]', list).map(row => row.dataset.homeModuleId).join(',');
  }

  function syncOrderButtons() {
    const rows = $$('[data-home-module-id]', $('#homeModuleOrderList') || document.createElement('div'));
    rows.forEach((row, index) => {
      const up = $('[data-order-move="up"]', row);
      const down = $('[data-order-move="down"]', row);
      if (up) up.disabled = index === 0;
      if (down) down.disabled = index === rows.length - 1;
      const num = $('.fc-order-index', row);
      if (num) num.textContent = String(index + 1).padStart(2, '0');
    });
  }

  function fieldValue(name, fallback = '') {
    const field = $(`#frontendControlsForm [name="${name}"]`);
    if (!field) return fallback;
    return field.type === 'checkbox' ? field.checked : (field.value || fallback);
  }

  function moduleVisible(id) {
    const map = {
      hero: 'homeHeroVisible', launcher: 'homeLauncherVisible', game: 'homeGameEntryVisible',
      quote: 'homeQuotePanelVisible', hot: 'homeHotVisible', services: 'homeServicesVisible',
      why: 'homeWhyVisible', tool: 'homeToolVisible', cases: 'homeCasesVisible', faq: 'homeFaqVisible'
    };
    if (id === 'command') return Boolean(fieldValue('homeQuotePanelVisible')) || Boolean(fieldValue('homeRecordPanelVisible'));
    if (id === 'support') return Boolean(fieldValue('homeRecordPanelVisible'));
    return map[id] ? Boolean(fieldValue(map[id])) : true;
  }

  function renderPreview() {
    const canvas = $('#fcV31Canvas');
    if (!canvas) return;
    const accent = String(fieldValue('themeAccentColor', '#f4bd34'));
    const background = String(fieldValue('themeBackgroundColor', '#0b0d10'));
    const surface = String(fieldValue('themeSurfaceColor', '#171b21'));
    const text = String(fieldValue('themeTextColor', '#f5f6f7'));
    const radius = Math.max(6, Math.min(30, Number(fieldValue('themeRadius', 18)) || 18));
    canvas.style.setProperty('--p-accent', accent);
    canvas.style.setProperty('--p-bg', background);
    canvas.style.setProperty('--p-surface', surface);
    canvas.style.setProperty('--p-text', text);
    canvas.style.borderRadius = previewDevice === 'mobile' ? '24px' : `${Math.max(8, Math.round(radius * .75))}px`;
    canvas.className = `fc-v31-canvas ${previewDevice}`;
    canvas.innerHTML = '';

    if (fieldValue('siteHeaderVisible')) {
      const header = document.createElement('div');
      header.className = 'fc-v31-preview-header';
      const brand = document.createElement('span');
      brand.className = 'fc-v31-preview-brand';
      brand.textContent = '星火网游';
      const nav = document.createElement('span');
      nav.textContent = previewDevice === 'mobile' ? '菜单' : '服务 · 攻略 · 记录 · 客服';
      header.append(brand, nav);
      canvas.appendChild(header);
    }

    const main = document.createElement('div');
    main.className = 'fc-v31-preview-main';
    const rawOrder = scope === previewDevice ? fieldValue('homeModuleOrder', '') : '';
    const defs = new Map(moduleDefinitions(previewDevice));
    normalizedOrder(rawOrder, previewDevice).forEach(id => {
      if (!moduleVisible(id)) return;
      const block = document.createElement('div');
      block.className = `fc-v31-module ${id === 'hero' ? 'hero' : ''}`;
      if (id === 'hero') {
        const strong = document.createElement('strong');
        strong.textContent = '使命召唤 MW4 任务代练';
        const kicker = document.createElement('span');
        kicker.textContent = String(fieldValue('homeHeroKicker', '可靠手打 · 纯绿服务 · 进度可查'));
        const cta = document.createElement('span');
        cta.className = 'fc-v31-preview-cta';
        cta.textContent = String(fieldValue('homePrimaryCtaLabel', '提前询价'));
        block.append(strong, kicker, cta);
        const heroHeight = Number(fieldValue('homeHeroHeight', 0)) || 0;
        block.style.minHeight = heroHeight > 0 ? `${Math.max(82, Math.min(160, Math.round(heroHeight / 5)))}px` : '82px';
        block.title = heroHeight > 0 ? `Hero 最小高度 ${heroHeight}px` : 'Hero 使用设计默认高度';
      } else {
        const b = document.createElement('b');
        b.textContent = defs.get(id) || id;
        const small = document.createElement('small');
        if (id === 'quote') small.textContent = String(fieldValue('homeQuoteTitle', '选择需求，获取报价'));
        else if (id === 'why') small.textContent = String(fieldValue('homeWhyTitle', '四件可以被验证的事'));
        else if (id === 'game') small.textContent = String(fieldValue('homeGameTitle', '选择游戏'));
        else small.textContent = '模块草稿预览';
        block.append(b, small);
      }
      main.appendChild(block);
    });
    canvas.appendChild(main);

    if (previewDevice === 'mobile' && fieldValue('mobileDockVisible')) {
      const dock = document.createElement('div');
      dock.className = 'fc-v31-preview-dock';
      ['mobileDockServicesLabel','mobileDockGuidesLabel','mobileDockRecordsLabel','mobileDockContactLabel','mobileDockQuoteLabel'].forEach((name, index) => {
        const item = document.createElement('span');
        item.textContent = String(fieldValue(name, ['服务','攻略','记录','客服','询价'][index]));
        dock.appendChild(item);
      });
      canvas.appendChild(dock);
    }

    if (fieldValue('siteFooterVisible')) {
      const footer = document.createElement('div');
      footer.className = 'fc-v31-preview-footer';
      footer.textContent = '蜀ICP备2026047129号 · 川公网安备51162102000262号';
      canvas.appendChild(footer);
    }

    $$('[data-v31-preview-device]').forEach(button => {
      const active = button.dataset.v31PreviewDevice === previewDevice;
      button.classList.toggle('primary', active);
      button.classList.toggle('ghost', !active);
    });
  }

  function updateWorkspaceUi() {
    const draft = Boolean(workspace?.hasDraft || formDirty);
    const previous = Boolean(workspace?.hasPrevious);
    const inherit = Boolean(workspace?.draftInheritGlobal);
    const pill = $('#fcV31DraftPill');
    if (pill) {
      pill.hidden = !draft;
      pill.textContent = formDirty ? '未保存改动' : inherit ? '继承全局草稿' : '草稿';
    }
    const scopeText = $('#fcV31ScopeText');
    if (scopeText) scopeText.textContent = `${scopeLabel()} · ${draft ? '草稿待发布' : '已发布'}`;
    const note = $('#fcV31StateNote');
    if (note) {
      note.textContent = inherit
        ? `${scopeLabel()}已暂存“恢复继承全局”，发布后才会清除端级覆盖。`
        : formDirty
          ? '当前表单有尚未保存的改动；“发布到前台”会先自动保存草稿再发布。'
          : draft
            ? '草稿已安全保存，线上配置尚未改变。确认预览后再发布。'
            : '当前显示已发布配置；修改后先保存草稿，线上不会立即变化。';
      note.classList.toggle('fc-v31-inherit', inherit);
    }
    const publish = $('#fcV31Publish');
    const discard = $('#fcV31Discard');
    const restore = $('#fcV31Restore');
    if (publish) publish.disabled = busy || (!draft && !formDirty);
    if (discard) discard.disabled = busy || (!workspace?.hasDraft && !formDirty);
    if (restore) restore.disabled = busy || !previous;
    const submit = $('.fc-sticky-save button[type="submit"]');
    if (submit) submit.disabled = busy;
    const legacyReset = $('#resetFrontendScope');
    if (legacyReset) legacyReset.disabled = busy || scope === 'global';
  }

  function applyWorkspace(next) {
    workspace = next || null;
    fillForm(workspace?.draft || workspace?.published || {});
    renderOrder((workspace?.draft || workspace?.published || {}).homeModuleOrder || '');
    formDirty = false;
    const radius = $('#frontendControlsForm [name="themeRadius"]');
    if (radius) radius.dispatchEvent(new Event('input', { bubbles: true }));
    renderPreview();
    updateWorkspaceUi();
  }

  async function loadWorkspace(nextScope = scope) {
    scope = nextScope || 'global';
    if (scope === 'desktop' || scope === 'mobile') previewDevice = scope;
    busy = true;
    updateWorkspaceUi();
    try {
      const next = await api(`/api/admin/frontend-controls/workspace?scope=${encodeURIComponent(scope)}`);
      applyWorkspace(next);
      const state = $('#frontendControlState');
      if (state) state.textContent = `${scopeLabel()}工作区已读取`;
    } catch (error) {
      toast(error.message, 'error');
    } finally {
      busy = false;
      updateWorkspaceUi();
    }
  }

  async function saveDraft(showToast = true) {
    if (busy) return false;
    busy = true;
    updateWorkspaceUi();
    const state = $('#frontendControlState');
    if (state) state.textContent = '正在保存草稿…';
    try {
      const next = await api(`/api/admin/frontend-controls/draft?scope=${encodeURIComponent(scope)}`, {
        method: 'PATCH',
        body: JSON.stringify(collectForm(`前端控制中心 V3.1：${scopeLabel()}草稿`))
      });
      applyWorkspace(next);
      if (state) state.textContent = `${scopeLabel()}草稿已保存，线上未改变`;
      if (showToast) toast(`${scopeLabel()}草稿已保存`);
      return true;
    } catch (error) {
      if (state) state.textContent = '草稿保存失败';
      toast(error.message, 'error');
      return false;
    } finally {
      busy = false;
      updateWorkspaceUi();
    }
  }

  async function publishDraft() {
    if (busy) return;
    if (formDirty || !workspace?.hasDraft) {
      const saved = await saveDraft(false);
      if (!saved) return;
    }
    busy = true;
    updateWorkspaceUi();
    const state = $('#frontendControlState');
    if (state) state.textContent = '正在发布到前台…';
    try {
      const next = await api(`/api/admin/frontend-controls/publish?scope=${encodeURIComponent(scope)}`, { method: 'POST' });
      applyWorkspace(next);
      if (state) state.textContent = `${scopeLabel()}已发布，前台刷新后生效`;
      toast(`${scopeLabel()}配置已发布到前台`);
    } catch (error) {
      if (state) state.textContent = '发布失败';
      toast(error.message, 'error');
    } finally {
      busy = false;
      updateWorkspaceUi();
    }
  }

  async function discardDraft() {
    if (busy || (!workspace?.hasDraft && !formDirty)) return;
    busy = true;
    updateWorkspaceUi();
    try {
      const next = await api(`/api/admin/frontend-controls/draft?scope=${encodeURIComponent(scope)}`, { method: 'DELETE' });
      applyWorkspace(next);
      $('#frontendControlState').textContent = `${scopeLabel()}草稿已丢弃`;
      toast('草稿已丢弃，恢复显示已发布配置');
    } catch (error) {
      toast(error.message, 'error');
    } finally {
      busy = false;
      updateWorkspaceUi();
    }
  }

  async function restorePrevious() {
    if (busy || !workspace?.hasPrevious) return;
    busy = true;
    updateWorkspaceUi();
    try {
      const next = await api(`/api/admin/frontend-controls/restore-previous?scope=${encodeURIComponent(scope)}`, { method: 'POST' });
      applyWorkspace(next);
      $('#frontendControlState').textContent = `${scopeLabel()}已恢复上一版并立即生效`;
      toast(`${scopeLabel()}已恢复上一版`);
    } catch (error) {
      toast(error.message, 'error');
    } finally {
      busy = false;
      updateWorkspaceUi();
    }
  }

  async function stageInheritance() {
    if (scope === 'global' || busy) return;
    busy = true;
    updateWorkspaceUi();
    try {
      const next = await api(`/api/admin/frontend-controls/draft/inherit-global?scope=${encodeURIComponent(scope)}`, { method: 'POST' });
      applyWorkspace(next);
      $('#frontendControlState').textContent = `${scopeLabel()}已暂存“恢复继承全局”，尚未发布`;
      toast(`${scopeLabel()}继承全局已保存为草稿`);
    } catch (error) {
      toast(error.message, 'error');
    } finally {
      busy = false;
      updateWorkspaceUi();
    }
  }

  async function waitForLegacyReady() {
    for (let i = 0; i < 40; i += 1) {
      if ($('#frontendDeviceScopeCard') && $('#layout') && !$('#frontendControlsForm')?.classList.contains('fc-loading')) return;
      await new Promise(resolve => setTimeout(resolve, 75));
    }
  }

  async function afterScopeSwitch(nextScope) {
    for (let i = 0; i < 40; i += 1) {
      const active = $(`[data-frontend-scope="${nextScope}"].primary`);
      if (active) break;
      await new Promise(resolve => setTimeout(resolve, 75));
    }
    await loadWorkspace(nextScope);
  }

  function bind() {
    const form = $('#frontendControlsForm');
    if (!form) return;

    form.addEventListener('submit', event => {
      event.preventDefault();
      event.stopImmediatePropagation();
      saveDraft(true);
    }, true);

    form.addEventListener('input', event => {
      if (!event.target.matches('[name]')) return;
      formDirty = true;
      renderPreview();
      updateWorkspaceUi();
    });
    form.addEventListener('change', event => {
      if (!event.target.matches('[name]')) return;
      formDirty = true;
      renderPreview();
      updateWorkspaceUi();
    });

    document.addEventListener('click', event => {
      const resetInheritance = event.target.closest('#resetFrontendScope');
      if (resetInheritance) {
        event.preventDefault();
        event.stopImmediatePropagation();
        stageInheritance();
        return;
      }
      const publish = event.target.closest('#fcV31Publish');
      if (publish) { publishDraft(); return; }
      const discard = event.target.closest('#fcV31Discard');
      if (discard) { discardDraft(); return; }
      const restore = event.target.closest('#fcV31Restore');
      if (restore) { restorePrevious(); return; }
      const device = event.target.closest('[data-v31-preview-device]');
      if (device) {
        previewDevice = device.dataset.v31PreviewDevice || 'desktop';
        renderPreview();
        return;
      }
      const scopeButton = event.target.closest('[data-frontend-scope]');
      if (scopeButton) {
        const nextScope = scopeButton.dataset.frontendScope;
        if (nextScope && nextScope !== scope) afterScopeSwitch(nextScope);
        return;
      }
      if (event.target.closest('[data-order-move], #resetHomeModuleOrder')) {
        setTimeout(() => {
          syncModuleOrderInput();
          formDirty = true;
          renderPreview();
          updateWorkspaceUi();
        }, 0);
      }
    });
  }

  async function init() {
    await waitForLegacyReady();
    ensureStyles();
    modernizeLabels();
    ensureWorkspaceLayout();
    scope = currentScopeFromUi();
    if (scope === 'desktop' || scope === 'mobile') previewDevice = scope;
    bind();
    await loadWorkspace(scope);
  }

  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', init, { once: true });
  else init();
})();
