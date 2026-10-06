(() => {
  const csrf = document.querySelector('meta[name=_csrf]')?.content;
  const csrfHeader = document.querySelector('meta[name=_csrf_header]')?.content;
  const esc = (value) => String(value ?? '').replace(/[&<>'"]/g, (c) => ({
    '&': '&amp;', '<': '&lt;', '>': '&gt;', "'": '&#39;', '"': '&quot;'
  }[c]));

  async function api(url, opt = {}) {
    const headers = { ...(opt.body ? { 'Content-Type': 'application/json' } : {}), ...(opt.headers || {}) };
    if (csrf && csrfHeader && !['GET', 'HEAD'].includes((opt.method || 'GET').toUpperCase())) headers[csrfHeader] = csrf;
    const response = await fetch(url, { ...opt, headers });
    let payload = null;
    try { payload = await response.json(); } catch (_) {}
    if (response.status === 401 || response.status === 403) {
      location.href = '/admin/login';
      throw new Error('登录已失效，请重新登录');
    }
    if (!response.ok) throw new Error(payload?.message || '请求失败');
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

  function field(name) {
    return document.querySelector(`#globalUiForm [name="${CSS.escape(name)}"]`);
  }

  function injectStyle() {
    if (document.querySelector('#globalUiAdminStyle')) return;
    const style = document.createElement('style');
    style.id = 'globalUiAdminStyle';
    style.textContent = `
      .global-ui-card{margin-bottom:18px;border:1px solid rgba(255,207,51,.2)}
      .global-ui-card .global-ui-head{display:flex;align-items:flex-start;justify-content:space-between;gap:16px;margin-bottom:16px}
      .global-ui-card .global-ui-head p{margin:.35rem 0 0}
      .global-ui-badge{display:inline-flex;align-items:center;min-height:28px;padding:0 10px;border-radius:999px;background:rgba(255,207,51,.1);color:#a56f00;font-size:12px;font-weight:800;white-space:nowrap}
      .global-ui-grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:12px 16px}
      .global-ui-grid .wide{grid-column:1/-1}
      .global-ui-card textarea{min-height:72px;resize:vertical}
      .global-ui-actions{display:flex;align-items:center;gap:10px;flex-wrap:wrap;margin-top:16px;padding-top:14px;border-top:1px solid rgba(0,0,0,.08)}
      .global-ui-locked{margin:0 0 16px;padding:11px 13px;border-radius:10px;background:rgba(20,23,28,.05);font-size:12px;line-height:1.6}
      @media(max-width:760px){.global-ui-grid{grid-template-columns:1fr}.global-ui-grid .wide{grid-column:auto}.global-ui-card .global-ui-head{display:grid}}
    `;
    document.head.appendChild(style);
  }

  function template(settings, site) {
    const checked = settings.floatingCustomerServiceEnabled !== 'false' ? ' checked' : '';
    const toolChecked = site.toolPublicEnabled !== false ? ' checked' : '';
    return `
      <section class="card global-ui-card" id="globalUiControlCard">
        <div class="global-ui-head">
          <div><span class="eyebrow">全局前端控制</span><h2>统一控制公共导航、悬浮客服与运行文案</h2><p class="muted small">这里保存后直接影响全站公共 UI；页面内重复客服卡片已固定停用，只保留右下角悬浮客服。</p></div>
          <span class="global-ui-badge">后端驱动前端</span>
        </div>
        <p class="global-ui-locked"><b>客服入口策略：</b>首页与普通内容页不再显示独立客服卡片或“联系客服”按钮；旧 /contact、/support/* 地址继续兼容，悬浮客服仍按四类问题进入对应页面。</p>
        <form id="globalUiForm">
          <div class="global-ui-grid">
            <label class="toggle-row wide"><input type="checkbox" name="floatingCustomerServiceEnabled"${checked}><span><b>显示右下角悬浮在线客服</b><small>这是全站唯一常驻客服入口。关闭后前台不显示悬浮客服，但兼容路由仍保留。</small></span></label>

            <div class="field"><label>顶部“服务”文案</label><input name="navServicesLabel" maxlength="100" value="${esc(settings.navServicesLabel)}"></div>
            <div class="field"><label>顶部“攻略”文案</label><input name="navGuidesLabel" maxlength="100" value="${esc(settings.navGuidesLabel)}"></div>
            <div class="field"><label>MW4 工具站文案</label><input name="navMasteryLabel" maxlength="100" value="${esc(settings.navMasteryLabel)}"></div>
            <div class="field"><label>业务记录文案</label><input name="navRecordsLabel" maxlength="100" value="${esc(settings.navRecordsLabel)}"></div>
            <div class="field"><label>个人空间文案</label><input name="navAccountLabel" maxlength="100" value="${esc(settings.navAccountLabel)}"></div>
            <div class="field"><label>主按钮文案</label><input name="primaryButtonLabel" maxlength="40" value="${esc(site.primaryButtonLabel || '')}" placeholder="提前询价"></div>

            <div class="field"><label>悬浮客服按钮文案</label><input name="floatingCustomerServiceLabel" maxlength="100" value="${esc(settings.floatingCustomerServiceLabel)}"></div>
            <div class="field"><label>悬浮客服标题</label><input name="floatingCustomerServiceTitle" maxlength="100" value="${esc(settings.floatingCustomerServiceTitle)}"></div>
            <div class="field wide"><label>悬浮客服说明</label><textarea name="floatingCustomerServiceIntro" maxlength="300">${esc(settings.floatingCustomerServiceIntro)}</textarea></div>
            <div class="field wide"><label>悬浮客服安全提示</label><input name="floatingCustomerServiceNote" maxlength="300" value="${esc(settings.floatingCustomerServiceNote)}"><small class="form-help">客服时间继续复用“联系方式”分区里的服务时间。</small></div>

            <div class="field"><label>状态短文案</label><input name="statusShortText" maxlength="120" value="${esc(site.statusShortText || '')}" placeholder="当前可正常接单"></div>
            <div class="field"><label>工具站更新时间提示</label><input name="toolUpdatedNote" maxlength="120" value="${esc(site.toolUpdatedNote || '')}" placeholder="数据更新时间"></div>
            <label class="toggle-row wide"><input type="checkbox" name="toolPublicEnabled"${toolChecked}><span><b>公开展示 MW4 工具站入口</b><small>关闭后公共导航中的工具站入口会隐藏；后台资料仍保留。</small></span></label>

            <div class="field"><label>ICP备案号</label><input name="icpNumber" maxlength="100" value="${esc(settings.icpNumber)}"></div>
            <div class="field"><label>公安备案号</label><input name="publicSecurityNumber" maxlength="100" value="${esc(settings.publicSecurityNumber)}"></div>
            <div class="field"><label>页脚规则文案</label><input name="footerRulesLabel" maxlength="100" value="${esc(settings.footerRulesLabel)}"></div>
            <div class="field"><label>页脚隐私文案</label><input name="footerPrivacyLabel" maxlength="100" value="${esc(settings.footerPrivacyLabel)}"></div>
          </div>
          <div class="global-ui-actions">
            <button class="btn primary" type="submit">保存全局前端配置</button>
            <button class="btn ghost" type="button" id="globalUiReload">重新读取</button>
            <a class="btn ghost" href="/" target="_blank" rel="noopener">打开前台检查</a>
          </div>
        </form>
      </section>`;
  }

  async function load() {
    const panel = document.querySelector('[data-panel="config"]');
    const configForm = panel?.querySelector('#configForm');
    if (!panel || !configForm) return;
    injectStyle();
    const [settings, site] = await Promise.all([
      api('/api/admin/global-ui-settings'),
      api('/api/admin/site-config')
    ]);
    document.querySelector('#globalUiControlCard')?.remove();
    configForm.insertAdjacentHTML('beforebegin', template(settings || {}, site || {}));
  }

  async function save(event) {
    event.preventDefault();
    const button = event.submitter || document.querySelector('#globalUiForm button[type="submit"]');
    if (button) { button.disabled = true; button.dataset.oldText = button.textContent; button.textContent = '保存中…'; }
    try {
      const globalPayload = {
        floatingCustomerServiceEnabled: field('floatingCustomerServiceEnabled').checked,
        navServicesLabel: field('navServicesLabel').value,
        navGuidesLabel: field('navGuidesLabel').value,
        navMasteryLabel: field('navMasteryLabel').value,
        navRecordsLabel: field('navRecordsLabel').value,
        navAccountLabel: field('navAccountLabel').value,
        floatingCustomerServiceLabel: field('floatingCustomerServiceLabel').value,
        floatingCustomerServiceTitle: field('floatingCustomerServiceTitle').value,
        floatingCustomerServiceIntro: field('floatingCustomerServiceIntro').value,
        floatingCustomerServiceNote: field('floatingCustomerServiceNote').value,
        icpNumber: field('icpNumber').value,
        publicSecurityNumber: field('publicSecurityNumber').value,
        footerRulesLabel: field('footerRulesLabel').value,
        footerPrivacyLabel: field('footerPrivacyLabel').value,
        reason: '管理员全局前端控制台保存'
      };
      await Promise.all([
        api('/api/admin/global-ui-settings', { method: 'PATCH', body: JSON.stringify(globalPayload) }),
        api('/api/admin/site-config/section/basic', { method: 'PATCH', body: JSON.stringify({
          statusShortText: field('statusShortText').value,
          primaryButtonLabel: field('primaryButtonLabel').value,
          reason: '补齐后台控制：状态与主按钮文案'
        }) }),
        api('/api/admin/site-config/section/tools', { method: 'PATCH', body: JSON.stringify({
          toolUpdatedNote: field('toolUpdatedNote').value,
          toolPublicEnabled: field('toolPublicEnabled').checked,
          reason: '补齐后台控制：工具站公开状态'
        }) })
      ]);
      toast('全局前端配置已保存并生效');
      await load();
    } catch (error) {
      toast(error.message || '保存失败', 'error');
    } finally {
      if (button) { button.disabled = false; button.textContent = button.dataset.oldText || '保存全局前端配置'; }
    }
  }

  document.addEventListener('submit', (event) => {
    if (event.target?.id === 'globalUiForm') save(event);
  });
  document.addEventListener('click', (event) => {
    if (event.target?.closest('#globalUiReload')) load().then(() => toast('全局配置已重新读取')).catch((e) => toast(e.message, 'error'));
    if (event.target?.closest('[data-nav="config"]') && !document.querySelector('#globalUiControlCard')) {
      setTimeout(() => load().catch((e) => toast(e.message, 'error')), 0);
    }
  });

  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', () => load().catch((e) => toast(e.message, 'error')), { once: true });
  else load().catch((e) => toast(e.message, 'error'));
})();
