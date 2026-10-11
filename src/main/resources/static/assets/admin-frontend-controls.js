(() => {
  'use strict';
  const csrf = document.querySelector('meta[name="_csrf"]')?.content;
  const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;
  const $ = (selector, root = document) => root.querySelector(selector);
  const $$ = (selector, root = document) => [...root.querySelectorAll(selector)];
  const ADMIN_BRAND_FAVICON = '/assets/brand-logo.svg?v=20261008logo02';
  let frontendScope = 'global';

  const HOME_MODULES = {
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

  function ensureBrandLayoutFixes() {
    if (document.querySelector('link[data-admin-brand-layout-fixes]')) return;
    const link = document.createElement('link');
    link.rel = 'stylesheet';
    link.href = '/assets/admin-brand-layout-fixes.css?v=20261008b002';
    link.dataset.adminBrandLayoutFixes = '1';
    document.head.appendChild(link);
  }

  function ensureAdminBrandFavicon() {
    if (!document.head) return;
    let icon = document.head.querySelector('link[data-xh-admin-favicon]');
    if (!icon) {
      icon = document.createElement('link');
      icon.rel = 'icon';
      icon.type = 'image/svg+xml';
      icon.setAttribute('sizes', 'any');
      icon.setAttribute('data-xh-admin-favicon', 'true');
      document.head.appendChild(icon);
    }
    icon.href = ADMIN_BRAND_FAVICON;
  }

  function ensureV3Styles() {
    if ($('#frontendControlV3Styles')) return;
    const style = document.createElement('style');
    style.id = 'frontendControlV3Styles';
    style.textContent = `
      .fc-v3-summary{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:10px;margin:16px 0}
      .fc-v3-summary article{padding:12px;border:1px solid var(--fc-line);border-radius:12px;background:var(--fc-surface-soft)}
      .fc-v3-summary b,.fc-v3-summary span{display:block}.fc-v3-summary span{margin-top:3px;color:var(--fc-muted);font-size:12px}
      .fc-order-list{display:grid;gap:8px;margin-top:10px}
      .fc-order-row{display:grid;grid-template-columns:34px minmax(0,1fr) auto;gap:10px;align-items:center;padding:10px;border:1px solid var(--fc-line);border-radius:12px;background:#fff}
      .fc-order-index{display:grid;place-items:center;width:30px;height:30px;border-radius:9px;background:var(--fc-accent-soft);color:#765600;font-weight:900}
      .fc-order-copy b,.fc-order-copy small{display:block}.fc-order-copy small{margin-top:2px;color:var(--fc-muted)}
      .fc-order-actions{display:flex;gap:6px}.fc-order-actions .btn{min-width:42px;padding:6px 9px}
      .fc-order-empty{padding:14px;border:1px dashed #cbd5e1;border-radius:12px;background:#f8fafc;color:var(--fc-muted)}
      .fc-layout-note{margin:8px 0 0;color:var(--fc-muted);font-size:12px}
      @media(max-width:780px){.fc-v3-summary{grid-template-columns:1fr}.fc-order-row{grid-template-columns:30px minmax(0,1fr)}.fc-order-actions{grid-column:1/-1;justify-content:flex-end}}
    `;
    document.head.appendChild(style);
  }

  function ensureDeviceScopeControls() {
    const form = $('#frontendControlsForm');
    if (!form || $('#frontendDeviceScopeCard')) return;
    const card = document.createElement('section');
    card.className = 'fc-card';
    card.id = 'frontendDeviceScopeCard';
    card.innerHTML = `
      <div class="fc-card-head"><div><span class="eyebrow">Device Scope</span><h2>电脑端 / 手机端独立控制</h2><p>全局是默认值；电脑端和手机端可以单独覆盖。恢复继承全局会清除当前端覆盖。</p></div><span class="fc-badge" id="frontendScopeBadge">全局</span></div>
      <div class="fc-v3-summary"><article><b>全局</b><span>公共默认值</span></article><article><b>电脑端</b><span>24 寸桌面与宽屏布局</span></article><article><b>手机端</b><span>320–430px 与底部 Dock</span></article></div>
      <div class="fc-grid two">
        <div class="field wide"><label>当前配置作用域</label><div class="fc-top-actions" id="frontendScopeButtons">
          <button type="button" class="btn primary" data-frontend-scope="global">全局</button>
          <button type="button" class="btn ghost" data-frontend-scope="desktop">电脑端</button>
          <button type="button" class="btn ghost" data-frontend-scope="mobile">手机端</button>
        </div></div>
        <div class="field wide"><label>端级继承</label><button type="button" class="btn ghost" id="resetFrontendScope" disabled>恢复继承全局</button></div>
      </div>
      <h3>前端功能入口</h3><div class="fc-grid two">
        <label class="toggle-row"><input type="checkbox" name="siteHeaderVisible"><span><b>显示顶部导航</b><small>只控制前端展示，不改变后端权限。</small></span></label>
        <label class="toggle-row"><input type="checkbox" name="siteFooterVisible"><span><b>显示页脚</b></span></label>
        <label class="toggle-row"><input type="checkbox" name="servicesEntryVisible"><span><b>显示服务入口</b></span></label>
        <label class="toggle-row"><input type="checkbox" name="quoteEntryVisible"><span><b>显示询价入口</b></span></label>
        <label class="toggle-row"><input type="checkbox" name="recordsEntryVisible"><span><b>显示业务记录入口</b></span></label>
        <label class="toggle-row"><input type="checkbox" name="mobileDockVisible"><span><b>显示手机底部快捷栏</b><small>电脑端保存该项不会产生可见副作用。</small></span></label>
      </div>`;
    form.prepend(card);
  }

  function ensureLayoutControls() {
    const form = $('#frontendControlsForm');
    const theme = $('#theme');
    if (!form || !theme || $('#layout')) return;
    const card = document.createElement('section');
    card.className = 'fc-card';
    card.id = 'layout';
    card.innerHTML = `
      <div class="fc-card-head"><div><span class="eyebrow">V3 Layout</span><h2>分端布局与模块排序</h2><p>只移动和显示现有安全模块，不注入任意 HTML、CSS 或脚本。</p></div><span class="fc-badge">PC / Mobile</span></div>
      <h3>尺寸</h3><div class="fc-grid two">
        <div class="field"><label>Hero 最小高度（0–900px）</label><input type="number" name="homeHeroHeight" min="0" max="900"><small>0 = 沿用当前设计默认高度。</small></div>
        <div class="field"><label>内容卡片尺寸（0 或 70–140%）</label><input type="number" name="homeCardScale" min="0" max="140"><small>0 = 沿用当前设计；只调整服务卡与信任卡。</small></div>
      </div>
      <h3>模块显示</h3><div class="fc-grid two">
        <label class="toggle-row"><input type="checkbox" name="homeLauncherVisible"><span><b>显示手机四宫格入口</b><small>电脑端无可见副作用。</small></span></label>
        <label class="toggle-row"><input type="checkbox" name="homeContactVisible"><span><b>显示首页客服卡片</b></span></label>
        <label class="toggle-row"><input type="checkbox" name="homeHotVisible"><span><b>显示热门服务</b></span></label>
        <label class="toggle-row"><input type="checkbox" name="homeServicesVisible"><span><b>显示服务项目</b></span></label>
        <label class="toggle-row"><input type="checkbox" name="homeToolVisible"><span><b>显示 MW4 工具 / 交付模块</b></span></label>
        <label class="toggle-row"><input type="checkbox" name="homeCasesVisible"><span><b>允许显示近期完成记录</b><small>仍需原后台存在公开案例才会出现。</small></span></label>
        <label class="toggle-row"><input type="checkbox" name="homeFaqVisible"><span><b>显示常见问题</b></span></label>
      </div>
      <h3>首页模块排序</h3>
      <div class="field wide"><label id="homeModuleOrderLabel">当前端模块顺序</label><input type="hidden" name="homeModuleOrder"><div id="homeModuleOrderList" class="fc-order-list"></div><p class="fc-layout-note">排序只在电脑端或手机端配置时启用；全局层保留各端自己的设计顺序。</p><button type="button" class="btn ghost" id="resetHomeModuleOrder">恢复当前端设计顺序</button></div>
      <h3>手机底部 Dock 文案</h3><div class="fc-grid two" id="mobileDockLabelControls">
        <div class="field"><label>服务</label><input name="mobileDockServicesLabel" maxlength="120"></div>
        <div class="field"><label>攻略</label><input name="mobileDockGuidesLabel" maxlength="120"></div>
        <div class="field"><label>记录</label><input name="mobileDockRecordsLabel" maxlength="120"></div>
        <div class="field"><label>客服</label><input name="mobileDockContactLabel" maxlength="120"></div>
        <div class="field wide"><label>询价主按钮</label><input name="mobileDockQuoteLabel" maxlength="120"></div>
      </div>`;
    form.insertBefore(card, theme);
  }

  function updateScopeUi() {
    const labels = { global: '全局', desktop: '电脑端', mobile: '手机端' };
    $$('[data-frontend-scope]').forEach(button => {
      const active = button.dataset.frontendScope === frontendScope;
      button.classList.toggle('primary', active);
      button.classList.toggle('ghost', !active);
    });
    const badge = $('#frontendScopeBadge');
    if (badge) badge.textContent = labels[frontendScope] || frontendScope;
    const reset = $('#resetFrontendScope');
    if (reset) reset.disabled = frontendScope === 'global';
    const orderReset = $('#resetHomeModuleOrder');
    if (orderReset) orderReset.disabled = frontendScope === 'global';
    const orderLabel = $('#homeModuleOrderLabel');
    if (orderLabel) orderLabel.textContent = frontendScope === 'desktop' ? '电脑端模块顺序' : frontendScope === 'mobile' ? '手机端模块顺序' : '端级模块顺序';
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
    setTimeout(() => item.remove(), 2800);
  }

  function setBusy(form, busy) {
    form?.classList.toggle('fc-loading', busy);
    $$('button,input,select,textarea', form).forEach(node => { node.disabled = busy; });
    if (!busy) updateScopeUi();
  }

  function fillForm(form, values = {}) {
    if (!form) return;
    $$('[name]', form).forEach(field => {
      if (!(field.name in values)) return;
      if (field.type === 'checkbox') field.checked = String(values[field.name]).toLowerCase() === 'true';
      else field.value = values[field.name] ?? '';
    });
  }

  function formPayload(form, reason) {
    const payload = { reason };
    $$('[name]', form).forEach(field => {
      payload[field.name] = field.type === 'checkbox' ? field.checked : String(field.value ?? '').trim();
    });
    return payload;
  }

  function syncThemePreview() {
    const form = $('#frontendControlsForm');
    const preview = $('#themePreview');
    if (!form || !preview) return;
    const fields = form.elements;
    preview.style.setProperty('--preview-accent', fields.themeAccentColor?.value || '#f4bd34');
    preview.style.setProperty('--preview-bg', fields.themeBackgroundColor?.value || '#0b0d10');
    preview.style.setProperty('--preview-surface', fields.themeSurfaceColor?.value || '#171b21');
    preview.style.setProperty('--preview-text', fields.themeTextColor?.value || '#f5f6f7');
    preview.style.borderRadius = `${Number(fields.themeRadius?.value || 18)}px`;
    $$('input[type="color"][name]', form).forEach(input => {
      const output = $(`[data-color-output="${input.name}"]`);
      if (output) output.textContent = input.value;
    });
  }

  function moduleDefinitions() {
    return HOME_MODULES[frontendScope] || [];
  }

  function normalizedModuleOrder(raw) {
    const definitions = moduleDefinitions();
    const allowed = new Set(definitions.map(([id]) => id));
    const requested = String(raw || '').split(',').map(value => value.trim()).filter(id => allowed.has(id));
    return [...new Set([...requested, ...definitions.map(([id]) => id)])];
  }

  function syncModuleOrderInput() {
    const input = $('#frontendControlsForm [name="homeModuleOrder"]');
    const list = $('#homeModuleOrderList');
    if (!input || !list) return;
    if (frontendScope === 'global') {
      input.value = '';
      return;
    }
    input.value = $$('[data-home-module-id]', list).map(row => row.dataset.homeModuleId).join(',');
  }

  function syncOrderButtons() {
    const rows = $$('[data-home-module-id]', $('#homeModuleOrderList') || document);
    rows.forEach((row, index) => {
      const up = $('[data-order-move="up"]', row);
      const down = $('[data-order-move="down"]', row);
      if (up) up.disabled = index === 0;
      if (down) down.disabled = index === rows.length - 1;
      const indexNode = $('.fc-order-index', row);
      if (indexNode) indexNode.textContent = String(index + 1).padStart(2, '0');
    });
  }

  function renderModuleOrder(raw = '') {
    const list = $('#homeModuleOrderList');
    const input = $('#frontendControlsForm [name="homeModuleOrder"]');
    if (!list || !input) return;
    list.innerHTML = '';
    if (frontendScope === 'global') {
      input.value = '';
      list.innerHTML = '<div class="fc-order-empty">请选择“电脑端”或“手机端”后调整模块顺序。全局层不会强行覆盖两端各自的信息架构。</div>';
      updateScopeUi();
      return;
    }
    const definitions = new Map(moduleDefinitions());
    const order = normalizedModuleOrder(raw);
    order.forEach(id => {
      const row = document.createElement('div');
      row.className = 'fc-order-row';
      row.dataset.homeModuleId = id;
      row.innerHTML = `<span class="fc-order-index"></span><span class="fc-order-copy"><b>${definitions.get(id)}</b><small>${id}</small></span><span class="fc-order-actions"><button type="button" class="btn ghost" data-order-move="up" aria-label="上移">↑</button><button type="button" class="btn ghost" data-order-move="down" aria-label="下移">↓</button></span>`;
      list.appendChild(row);
    });
    syncModuleOrderInput();
    syncOrderButtons();
    updateScopeUi();
  }

  function moveModule(button) {
    const row = button.closest('[data-home-module-id]');
    const list = $('#homeModuleOrderList');
    if (!row || !list || frontendScope === 'global') return;
    if (button.dataset.orderMove === 'up' && row.previousElementSibling) {
      list.insertBefore(row, row.previousElementSibling);
    } else if (button.dataset.orderMove === 'down' && row.nextElementSibling) {
      list.insertBefore(row.nextElementSibling, row);
    }
    syncModuleOrderInput();
    syncOrderButtons();
  }

  async function loadFrontendScope(scope = frontendScope) {
    const form = $('#frontendControlsForm');
    setBusy(form, true);
    $('#frontendControlState').textContent = `正在读取${scope === 'global' ? '全局' : scope === 'desktop' ? '电脑端' : '手机端'}配置…`;
    try {
      const values = await api(`/api/admin/frontend-controls?scope=${encodeURIComponent(scope)}`);
      frontendScope = scope;
      fillForm(form, values || {});
      renderModuleOrder(values?.homeModuleOrder || '');
      syncThemePreview();
      updateScopeUi();
      $('#frontendControlState').textContent = '已读取后台当前配置';
    } catch (error) {
      $('#frontendControlState').textContent = '读取失败';
      toast(error.message, 'error');
    } finally {
      setBusy(form, false);
    }
  }

  async function load() {
    ensureBrandLayoutFixes();
    ensureAdminBrandFavicon();
    ensureV3Styles();
    ensureDeviceScopeControls();
    ensureLayoutControls();
    updateScopeUi();
    const globalForm = $('#globalUiForm');
    const frontendForm = $('#frontendControlsForm');
    setBusy(globalForm, true);
    setBusy(frontendForm, true);
    try {
      const [globalSettings, frontendSettings] = await Promise.all([
        api('/api/admin/global-ui-settings'),
        api('/api/admin/frontend-controls?scope=global')
      ]);
      fillForm(globalForm, globalSettings || {});
      fillForm(frontendForm, frontendSettings || {});
      renderModuleOrder(frontendSettings?.homeModuleOrder || '');
      $('#globalUiState').textContent = '已读取后台当前配置';
      $('#frontendControlState').textContent = '已读取全局前端配置';
      syncThemePreview();
    } catch (error) {
      $('#globalUiState').textContent = '读取失败';
      $('#frontendControlState').textContent = '读取失败';
      toast(error.message, 'error');
    } finally {
      setBusy(globalForm, false);
      setBusy(frontendForm, false);
    }
  }

  async function saveGlobal(event) {
    event.preventDefault();
    const form = event.currentTarget;
    setBusy(form, true);
    $('#globalUiState').textContent = '正在保存…';
    try {
      const values = await api('/api/admin/global-ui-settings', {
        method: 'PATCH',
        body: JSON.stringify(formPayload(form, '前端控制中心：导航与备案'))
      });
      fillForm(form, values || {});
      $('#globalUiState').textContent = '保存成功，前台刷新后生效';
      toast('导航与备案已保存');
    } catch (error) {
      $('#globalUiState').textContent = '保存失败';
      toast(error.message, 'error');
    } finally { setBusy(form, false); }
  }

  async function saveFrontend(event) {
    event.preventDefault();
    const form = event.currentTarget;
    syncModuleOrderInput();
    setBusy(form, true);
    $('#frontendControlState').textContent = '正在保存…';
    try {
      const label = frontendScope === 'global' ? '全局' : frontendScope === 'desktop' ? '电脑端' : '手机端';
      const values = await api(`/api/admin/frontend-controls?scope=${encodeURIComponent(frontendScope)}`, {
        method: 'PATCH',
        body: JSON.stringify(formPayload(form, `前端控制中心 V3：${label}配置`))
      });
      fillForm(form, values || {});
      renderModuleOrder(values?.homeModuleOrder || '');
      syncThemePreview();
      $('#frontendControlState').textContent = `${label}配置保存成功，前台刷新后生效`;
      toast(`${label}前端配置已保存`);
    } catch (error) {
      $('#frontendControlState').textContent = '保存失败';
      toast(error.message, 'error');
    } finally { setBusy(form, false); }
  }

  async function resetFrontendScope() {
    if (frontendScope === 'global') return;
    const form = $('#frontendControlsForm');
    const label = frontendScope === 'desktop' ? '电脑端' : '手机端';
    setBusy(form, true);
    try {
      const values = await api(`/api/admin/frontend-controls?scope=${encodeURIComponent(frontendScope)}`, { method: 'DELETE' });
      fillForm(form, values || {});
      renderModuleOrder(values?.homeModuleOrder || '');
      syncThemePreview();
      $('#frontendControlState').textContent = `${label}已恢复继承全局配置`;
      toast(`${label}已恢复继承全局`);
    } catch (error) {
      toast(error.message, 'error');
    } finally { setBusy(form, false); }
  }

  $('#globalUiForm')?.addEventListener('submit', saveGlobal);
  $('#frontendControlsForm')?.addEventListener('submit', saveFrontend);
  $('#frontendControlsForm')?.addEventListener('input', event => {
    if (event.target.matches('input[type="color"], input[name="themeRadius"]')) syncThemePreview();
  });
  document.addEventListener('click', event => {
    const scopeButton = event.target.closest('[data-frontend-scope]');
    if (scopeButton) {
      const scope = scopeButton.dataset.frontendScope;
      if (scope && scope !== frontendScope) loadFrontendScope(scope);
      return;
    }
    if (event.target.closest('#resetFrontendScope')) {
      resetFrontendScope();
      return;
    }
    const move = event.target.closest('[data-order-move]');
    if (move) {
      moveModule(move);
      return;
    }
    if (event.target.closest('#resetHomeModuleOrder') && frontendScope !== 'global') {
      renderModuleOrder('');
    }
  });
  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', load, { once: true });
  else load();
})();