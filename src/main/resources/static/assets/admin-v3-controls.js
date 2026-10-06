(() => {
  const csrf = document.querySelector('meta[name=_csrf]')?.content;
  const csrfHeader = document.querySelector('meta[name=_csrf_header]')?.content;
  const bool = (value, fallback = true) => {
    if (value == null || value === '') return fallback;
    return String(value).toLowerCase() === 'true';
  };

  const VISIBILITY_FIELDS = [
    ['navServicesVisible', '顶部 · 服务项目', '控制顶部和手机导航中的服务入口，不会下线 /services 页面。'],
    ['navGuidesVisible', '顶部 · 攻略教程', '只控制导航曝光，不影响攻略页面与搜索收录策略。'],
    ['navMasteryVisible', '顶部 · MW4 工具站', '与工具站公开开关独立；这里只控制导航入口。'],
    ['navRecordsVisible', '顶部 · 查询中心', '隐藏后业务记录页面仍可通过历史地址访问。'],
    ['navAccountVisible', '顶部 · 个人空间', '隐藏后登录与账号接口仍保持可用。'],
    ['footerServicesVisible', '页脚 · 服务大厅', '控制页脚服务入口。'],
    ['footerRecordsVisible', '页脚 · 业务记录', '控制页脚查询入口。'],
    ['footerAccountVisible', '页脚 · 个人空间', '控制页脚账号入口。'],
    ['footerRulesVisible', '页脚 · 服务规则', '控制页脚规则入口，规则页本身不会下线。'],
    ['footerPrivacyVisible', '页脚 · 隐私说明', '控制页脚隐私入口，隐私页本身不会下线。']
  ];

  async function api(url, opt = {}) {
    const headers = { ...(opt.body ? { 'Content-Type': 'application/json' } : {}), ...(opt.headers || {}) };
    if (csrf && csrfHeader && !['GET', 'HEAD'].includes((opt.method || 'GET').toUpperCase())) headers[csrfHeader] = csrf;
    const response = await fetch(url, { ...opt, headers, cache: 'no-store' });
    if (response.status === 401 || response.status === 403) {
      location.href = '/admin/login';
      throw new Error('登录已失效，请重新登录');
    }
    let payload = null;
    try { payload = await response.json(); } catch (_) {}
    if (!response.ok) throw new Error(payload?.message || '请求失败，请稍后重试');
    return payload;
  }

  function toast(message, type = 'success') {
    const area = document.querySelector('#toastArea');
    if (!area) return;
    const item = document.createElement('div');
    item.className = `toast ${type === 'error' ? 'error' : ''}`;
    item.textContent = message;
    area.appendChild(item);
    setTimeout(() => item.remove(), 2800);
  }

  function visibilityMarkup() {
    const rows = VISIBILITY_FIELDS.map(([name, label, help]) => `
      <label class="toggle-row admin-v3-visibility-row">
        <input type="checkbox" name="${name}">
        <span><b>${label}</b><small>${help}</small></span>
      </label>`).join('');
    return `<section class="card admin-control-card" id="adminV3VisibilityCard">
      <div class="admin-control-head"><div><h2>导航入口显示开关</h2><p class="muted small">这里控制“入口是否显示”，不会删除页面、不会改路由，也不会影响后台数据。适合临时收起暂不主推的功能。</p></div><span class="admin-control-badge">V3</span></div>
      <form id="adminV3VisibilityForm"><div class="admin-control-grid">${rows}</div>
      <div class="admin-control-actions"><button class="btn primary" type="submit">保存入口显示状态</button><button class="btn ghost" type="button" data-v3-visibility-reload>重新读取</button></div></form>
    </section>`;
  }

  async function loadVisibility(form) {
    const settings = await api('/api/admin/global-ui-settings');
    VISIBILITY_FIELDS.forEach(([name]) => {
      if (form.elements[name]) form.elements[name].checked = bool(settings?.[name], true);
    });
  }

  async function saveVisibility(form, button) {
    const payload = { reason: '管理员 V3 导航入口显示控制' };
    VISIBILITY_FIELDS.forEach(([name]) => { payload[name] = !!form.elements[name]?.checked; });
    const old = button.textContent;
    button.disabled = true;
    button.textContent = '保存中…';
    try {
      await api('/api/admin/global-ui-settings', { method: 'PATCH', body: JSON.stringify(payload) });
      toast('导航入口显示状态已保存并即时生效');
      await loadVisibility(form);
    } catch (error) {
      toast(error.message, 'error');
    } finally {
      button.disabled = false;
      button.textContent = old;
    }
  }

  function installVisibilityCard() {
    const panel = document.querySelector('#globalUiV2Panel');
    if (!panel || panel.querySelector('#adminV3VisibilityCard')) return;
    panel.insertAdjacentHTML('beforeend', visibilityMarkup());
    const form = panel.querySelector('#adminV3VisibilityForm');
    form.addEventListener('submit', (event) => {
      event.preventDefault();
      saveVisibility(form, form.querySelector('button[type=submit]'));
    });
    form.querySelector('[data-v3-visibility-reload]')?.addEventListener('click', () => {
      loadVisibility(form).then(() => toast('入口显示状态已重新读取')).catch((error) => toast(error.message, 'error'));
    });
    loadVisibility(form).catch((error) => toast(error.message, 'error'));
  }

  function installLegacyContactCard() {
    const panel = document.querySelector('#customerServiceV2Panel');
    if (!panel || panel.querySelector('#adminV3LegacyContactCard')) return;
    panel.insertAdjacentHTML('beforeend', `<section class="card admin-control-card" id="adminV3LegacyContactCard">
      <details>
        <summary><b>历史联系方式兼容（旧数据）</b></summary>
        <p class="muted small">旧微信号、二维码、QQ、电话、淘宝和闲鱼字段继续保留用于历史数据兼容，但它们不再是当前公开客服体系。日常客服请只维护上方“客服中心”的企业微信配置。</p>
        <div class="admin-control-actions"><button class="btn ghost" type="button" data-v3-open-legacy-contact data-config-entry="contact">打开旧数据兼容编辑</button></div>
      </details>
    </section>`);
  }

  function markLegacyContactSection() {
    const section = document.querySelector('[data-config-section="contact"]');
    if (!section) return;
    const heading = section.querySelector('h2');
    if (heading) heading.textContent = '历史联系方式兼容（旧数据）';
    if (!section.querySelector('[data-v3-legacy-note]')) {
      const note = document.createElement('div');
      note.dataset.v3LegacyNote = '1';
      note.className = 'dashboard-focus';
      note.textContent = '当前公开客服已统一使用“客服中心 → 企业微信官方客服”。本分区只用于维护历史兼容字段，请勿把这里当作新的客服入口。';
      heading?.insertAdjacentElement('afterend', note);
    }
  }

  function install() {
    installVisibilityCard();
    installLegacyContactCard();
    markLegacyContactSection();
  }

  document.addEventListener('click', (event) => {
    if (!event.target.closest('[data-v3-open-legacy-contact]')) return;
    setTimeout(markLegacyContactSection, 0);
  });

  const observer = new MutationObserver(install);
  observer.observe(document.documentElement, { childList: true, subtree: true });
  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', install, { once: true });
  else install();
})();
