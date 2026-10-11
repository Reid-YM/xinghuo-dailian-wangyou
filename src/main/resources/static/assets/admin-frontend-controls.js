(() => {
  'use strict';
  const csrf = document.querySelector('meta[name="_csrf"]')?.content;
  const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;
  const $ = (selector, root = document) => root.querySelector(selector);
  const $$ = (selector, root = document) => [...root.querySelectorAll(selector)];
  const ADMIN_BRAND_FAVICON = '/assets/brand-logo.svg?v=20261008logo02';
  let frontendScope = 'global';

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

  function ensureDeviceScopeControls() {
    const form = $('#frontendControlsForm');
    if (!form || $('#frontendDeviceScopeCard')) return;
    const card = document.createElement('section');
    card.className = 'fc-card';
    card.id = 'frontendDeviceScopeCard';
    card.innerHTML = `
      <div class="fc-card-head"><div><span class="eyebrow">Device Scope</span><h2>电脑端 / 手机端独立控制</h2><p>全局是默认值；电脑端和手机端可以单独覆盖。恢复继承全局会清除当前端覆盖。</p></div><span class="fc-badge" id="frontendScopeBadge">全局</span></div>
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

  async function loadFrontendScope(scope = frontendScope) {
    const form = $('#frontendControlsForm');
    setBusy(form, true);
    $('#frontendControlState').textContent = `正在读取${scope === 'global' ? '全局' : scope === 'desktop' ? '电脑端' : '手机端'}配置…`;
    try {
      const values = await api(`/api/admin/frontend-controls?scope=${encodeURIComponent(scope)}`);
      frontendScope = scope;
      fillForm(form, values || {});
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
    ensureDeviceScopeControls();
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
    setBusy(form, true);
    $('#frontendControlState').textContent = '正在保存…';
    try {
      const label = frontendScope === 'global' ? '全局' : frontendScope === 'desktop' ? '电脑端' : '手机端';
      const values = await api(`/api/admin/frontend-controls?scope=${encodeURIComponent(frontendScope)}`, {
        method: 'PATCH',
        body: JSON.stringify(formPayload(form, `前端控制中心：${label}配置`))
      });
      fillForm(form, values || {});
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
    if (event.target.closest('#resetFrontendScope')) resetFrontendScope();
  });
  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', load, { once: true });
  else load();
})();