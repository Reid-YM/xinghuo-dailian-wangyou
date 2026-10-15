(() => {
  'use strict';

  const $ = selector => document.querySelector(selector);
  const csrf = $('meta[name="_csrf"]')?.content || '';
  const csrfHeader = $('meta[name="_csrf_header"]')?.content || '';
  let currentMode = 'legacy';

  const hrefFor = mode => mode === 'next' ? '/next/services' : '/services';
  const labelFor = mode => mode === 'next' ? '新版候选' : '旧版稳定';

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
    if (href) href.textContent = hrefFor(currentMode);
    document.documentElement.dataset.servicesEntryMode = currentMode;
  }

  async function api(path, options = {}) {
    const method = String(options.method || 'GET').toUpperCase();
    const headers = Object.assign({ Accept: 'application/json' }, options.headers || {});
    if (!['GET', 'HEAD'].includes(method) && csrf && csrfHeader) headers[csrfHeader] = csrf;
    if (options.body !== undefined) headers['Content-Type'] = 'application/json';
    const response = await fetch(path, {
      method,
      headers,
      credentials: 'same-origin',
      cache: 'no-store',
      body: options.body === undefined ? undefined : JSON.stringify(options.body)
    });
    let payload = null;
    try { payload = await response.json(); } catch (ignore) { payload = null; }
    if (response.status === 401 || response.status === 403) {
      location.assign('/admin/login');
      throw new Error('登录状态已失效，请重新登录。');
    }
    if (!response.ok) throw new Error(payload?.message || '请求失败，请稍后重试。');
    return payload || {};
  }

  async function load() {
    setMessage('正在读取当前入口…');
    try {
      const settings = await api('/api/admin/global-ui-settings');
      render(settings.servicesEntryMode);
      setMessage('当前配置已读取。', 'ok');
    } catch (error) {
      render('legacy');
      setMessage(`${error.message || '读取失败'} 页面按安全默认值显示旧版入口。`, 'error');
    }
  }

  async function save(event) {
    event.preventDefault();
    const select = $('#servicesEntryMode');
    const button = $('#saveRolloutMode');
    const requested = select?.value === 'next' ? 'next' : 'legacy';

    if (requested === currentMode) {
      setMessage('入口目标没有变化，无需重复保存。');
      return;
    }
    if (requested === 'next') {
      const ok = window.confirm('确认把公共“服务”入口切到新版 /next/services？旧 /services 仍会保留，可随时恢复。');
      if (!ok) {
        if (select) select.value = currentMode;
        setMessage('已取消切换，现网入口未改变。');
        return;
      }
    }

    if (button) {
      button.disabled = true;
      button.dataset.label = button.textContent;
      button.textContent = '保存中…';
    }
    setMessage('正在保存并写入操作审计…');
    try {
      const settings = await api('/api/admin/global-ui-settings', {
        method: 'PATCH',
        body: {
          servicesEntryMode: requested,
          reason: requested === 'next'
            ? 'Frontend Next N9：公共服务入口切换到新版候选'
            : 'Frontend Next N9：公共服务入口回退到旧版稳定入口'
        }
      });
      render(settings.servicesEntryMode);
      setMessage(`保存成功：公共服务入口现在指向 ${hrefFor(currentMode)}。`, 'ok');
    } catch (error) {
      if (select) select.value = currentMode;
      setMessage(error.message || '保存失败，入口保持原值。', 'error');
    } finally {
      if (button) {
        button.disabled = false;
        button.textContent = button.dataset.label || '保存入口目标';
        delete button.dataset.label;
      }
    }
  }

  $('#frontendNextRolloutForm')?.addEventListener('submit', save);
  load();
})();
