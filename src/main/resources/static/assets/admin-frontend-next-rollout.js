(() => {
  'use strict';

  const $ = selector => document.querySelector(selector);
  const csrf = $('meta[name="_csrf"]')?.content || '';
  const csrfHeader = $('meta[name="_csrf_header"]')?.content || '';
  let currentMode = 'legacy';
  let revision = null;
  let writable = false;
  let loaded = false;
  let busy = false;
  const settingsEndpoint = '/api/admin/xingwei/site/global-ui';

  const labelFor = mode => mode === 'next' ? '新版直达入口' : '默认服务入口';

  function setMessage(value, kind = '') {
    const host = $('#rolloutMessage');
    if (!host) return;
    host.className = `fnr-message ${kind}`.trim();
    host.textContent = value || '';
  }

  function render(mode) {
    currentMode = mode === 'next' ? 'next' : 'legacy';
    const select = $('#servicesEntryMode');
    if (select) select.value = currentMode;
    const label = $('#rolloutModeLabel');
    if (label) label.textContent = labelFor(currentMode);
    const href = $('#rolloutHref');
    if (href) href.textContent = labelFor(currentMode);
    document.documentElement.dataset.servicesEntryMode = currentMode;
  }

  function syncControls() {
    $('#servicesEntryMode').disabled = busy || !loaded || !writable;
    $('#saveRolloutMode').disabled = busy || !loaded || !writable;
    $('#reloadRolloutMode').disabled = busy;
  }

  async function api(path, options = {}) {
    const method = String(options.method || 'GET').toUpperCase();
    const headers = Object.assign({ Accept: 'application/json' }, options.headers || {});
    if (!['GET', 'HEAD'].includes(method) && csrf && csrfHeader) headers[csrfHeader] = csrf;
    if (options.body !== undefined) {
      if (!loaded || !writable || !revision) throw new Error('请先读取配置并确认保存权限。');
      headers['Content-Type'] = 'application/json';
      headers['If-Match'] = revision;
    }
    const response = await fetch(path, {
      method,
      headers,
      credentials: 'same-origin',
      cache: 'no-store',
      body: options.body === undefined ? undefined : JSON.stringify(options.body)
    });
    let payload = null;
    try { payload = await response.json(); } catch (ignore) { payload = null; }
    if (response.status === 401) throw new Error('登录状态已失效，请重新登录。');
    if (response.status === 403) throw new Error('没有操作权限，请联系管理员。');
    if (response.status === 409) {
      loaded = false;
      revision = null;
      throw new Error('配置已被其他人员修改，请重新读取后核对再保存。');
    }
    if (!response.ok) throw new Error(payload?.error || payload?.message || '请求失败，请稍后重试。');
    if (path === settingsEndpoint) revision = response.headers.get('ETag');
    return payload || {};
  }

  async function load() {
    loaded = false;
    revision = null;
    busy = true;
    syncControls();
    setMessage('正在读取当前入口…');
    try {
      const [access, settings] = await Promise.all([
        api('/api/admin/xingwei/site/access'), api(settingsEndpoint)
      ]);
      render(settings.servicesEntryMode);
      writable = access.canManageSite === true;
      loaded = Boolean(revision);
      if (!loaded) throw new Error('未能确认配置状态，请重新读取。');
      setMessage(writable ? '当前配置已读取。' : '当前配置已读取，您拥有查看权限。', 'ok');
    } catch (error) {
      loaded = false;
      revision = null;
      setMessage(error.message || '读取失败，请重新读取。', 'error');
    } finally {
      busy = false;
      syncControls();
    }
  }

  async function save(event) {
    event.preventDefault();
    if (busy || !loaded || !writable || !revision) return;
    const select = $('#servicesEntryMode');
    const button = $('#saveRolloutMode');
    const requested = select?.value === 'next' ? 'next' : 'legacy';

    if (requested === currentMode) {
      setMessage('入口目标没有变化，无需重复保存。');
      return;
    }
    if (requested === 'next') {
      const ok = window.confirm('确认将服务导航改为新版直达入口？这里只调整导航，不更改网站统一版本设置。');
      if (!ok) {
        if (select) select.value = currentMode;
        setMessage('已取消切换，入口配置未改变。');
        return;
      }
    }

    busy = true;
    syncControls();
    if (button) {
      button.dataset.label = button.textContent;
      button.textContent = '保存中…';
    }
    setMessage('正在保存并写入操作审计…');
    try {
      const settings = await api(settingsEndpoint, {
        method: 'PATCH',
        body: {
          servicesEntryMode: requested,
          reason: requested === 'next'
            ? '服务导航改为新版直达入口'
            : '服务导航恢复默认入口'
        }
      });
      render(settings.servicesEntryMode);
      setMessage(`保存成功：公共服务入口配置现在指向 ${labelFor(currentMode)}。`, 'ok');
    } catch (error) {
      if (select) select.value = currentMode;
      setMessage(error.message || '保存失败，入口保持原值。', 'error');
    } finally {
      busy = false;
      syncControls();
      if (button) {
        button.textContent = button.dataset.label || '保存入口目标';
        delete button.dataset.label;
      }
    }
  }

  $('#frontendNextRolloutForm')?.addEventListener('submit', save);
  $('#reloadRolloutMode')?.addEventListener('click', load);
  load();
})();
