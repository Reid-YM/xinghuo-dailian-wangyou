(() => {
  const csrf = document.querySelector('meta[name=_csrf]')?.content;
  const csrfHeader = document.querySelector('meta[name=_csrf_header]')?.content;
  const $ = (selector, root = document) => root.querySelector(selector);
  const $$ = (selector, root = document) => [...root.querySelectorAll(selector)];
  const esc = value => String(value ?? '').replace(/[&<>'"]/g, c => ({
    '&':'&amp;', '<':'&lt;', '>':'&gt;', "'":'&#39;', '"':'&quot;'
  }[c]));

  async function api(url, opt = {}) {
    const headers = { ...(opt.body ? { 'Content-Type':'application/json' } : {}), ...(opt.headers || {}) };
    if (csrf && csrfHeader && !['GET','HEAD'].includes((opt.method || 'GET').toUpperCase())) headers[csrfHeader] = csrf;
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
    const area = $('#toastArea');
    if (!area) return;
    const item = document.createElement('div');
    item.className = `toast ${type === 'error' ? 'error' : ''}`;
    item.textContent = message;
    area.appendChild(item);
    setTimeout(() => item.remove(), 2800);
  }

  function setBusy(button, busy, text = '保存中…') {
    if (!button) return;
    if (busy) {
      button.dataset.oldText = button.textContent;
      button.textContent = text;
      button.disabled = true;
    } else {
      button.textContent = button.dataset.oldText || button.textContent;
      button.disabled = false;
      delete button.dataset.oldText;
    }
  }

  function injectStyle() {
    if ($('#adminControlCenterV4Style')) return;
    const style = document.createElement('style');
    style.id = 'adminControlCenterV4Style';
    style.textContent = `
      [data-panel="config"]>.admin-subnav{display:none!important}
      #adminConfigBridge{display:none!important}
      .admin-control-card{margin-bottom:18px;border:1px solid rgba(255,207,51,.22)}
      .admin-control-head{display:flex;align-items:flex-start;justify-content:space-between;gap:16px;margin-bottom:16px}
      .admin-control-head p{margin:.35rem 0 0}.admin-control-badge{display:inline-flex;align-items:center;min-height:28px;padding:0 10px;border-radius:999px;background:rgba(255,207,51,.1);color:#a56f00;font-size:12px;font-weight:800;white-space:nowrap}
      .admin-control-grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:12px 16px}.admin-control-grid .wide{grid-column:1/-1}
      .admin-control-row{display:grid;grid-template-columns:minmax(0,1fr) auto;gap:12px;align-items:end}
      .admin-control-actions{display:flex;align-items:center;gap:10px;flex-wrap:wrap;margin-top:16px;padding-top:14px;border-top:1px solid rgba(0,0,0,.08)}
      .config-runtime-extra,.tool-runtime-card{margin-top:16px;padding-top:16px;border-top:1px solid rgba(0,0,0,.08)}
      .admin-nav-current{outline:2px solid rgba(255,207,51,.35)}
      @media(max-width:760px){.admin-control-grid,.admin-control-row{grid-template-columns:1fr}.admin-control-grid .wide{grid-column:auto}.admin-control-head{display:grid}}
    `;
    document.head.appendChild(style);
  }

  function navMarkup() {
    return `
      <div class="nav-section nav-section-primary"><span class="nav-section-kicker">工作台</span><button class="btn nav-item nav-level-1" data-nav="dashboard" type="button"><span>运营总览</span><small>待办 / 数据 / 快捷入口</small></button></div>
      <div class="nav-section"><button class="nav-section-toggle" data-control-nav-group type="button"><span>站点运营</span><small>页面与公共配置</small></button><div class="nav-section-items">
        <button class="btn nav-item nav-level-2" data-admin-panel="globalUiV4" type="button"><span>全局导航与备案</span><small>顶部 / 页脚 / 显示开关</small></button>
        <button class="btn nav-item nav-level-2" data-config-entry="basic" type="button"><span>站点状态</span><small>品牌 / 接单 / 询价</small></button>
        <button class="btn nav-item nav-level-2" data-config-entry="home" type="button"><span>首页内容</span><small>首屏 / 价格 / 标签</small></button>
        <button class="btn nav-item nav-level-2" data-config-entry="visual" type="button"><span>首页视觉</span><small>主图 / 背景 / 封面</small></button>
        <button class="btn nav-item nav-level-2" data-config-entry="notice" type="button"><span>公告</span><small>临时运营通知</small></button>
        <button class="btn nav-item nav-level-2" data-config-entry="contact" type="button"><span>客服与联系方式</span><small>微信 / 企微 / 淘宝 / 闲鱼</small></button>
        <button class="btn nav-item nav-level-2" data-config-entry="trust" type="button"><span>信任 FAQ</span><small>案例 / 常见问题</small></button>
        <button class="btn nav-item nav-level-2" data-config-entry="copy" type="button"><span>业务文案</span><small>询价 / 订单 / 页脚</small></button>
        <button class="btn nav-item nav-level-2" data-config-entry="seo" type="button"><span>搜索展示</span><small>SEO 标题 / 描述</small></button>
        <button class="btn nav-item nav-level-2" data-nav="slots" type="button"><span>首页展示位</span><small>推荐服务</small></button>
      </div></div>
      <div class="nav-section"><button class="nav-section-toggle" data-control-nav-group type="button"><span>内容中心</span><small>服务与游戏资料</small></button><div class="nav-section-items">
        <button class="btn nav-item nav-level-2" data-nav="services" type="button"><span>服务项目</span><small>价格 / 上下架</small></button>
        <button class="btn nav-item nav-level-2" data-nav="catalog" type="button"><span>游戏资料中心</span><small>游戏 / 分类 / 平台</small></button>
        <button class="btn nav-item nav-level-2" data-config-entry="gameStatus" type="button"><span>游戏运营状态</span><small>接单状态 / 按钮文案</small></button>
        <a class="btn nav-item nav-level-2" href="/admin/guides"><span>攻略组件</span><small>组件化攻略编辑</small></a>
        <button class="btn nav-item nav-level-2" data-nav="toolStation" type="button"><span>MW4 工具站</span><small>公开开关 / 枪械 / 迷彩</small></button>
      </div></div>
      <div class="nav-section"><button class="nav-section-toggle" data-control-nav-group type="button"><span>客户履约</span><small>线索与交付</small></button><div class="nav-section-items">
        <button class="btn nav-item nav-level-2" data-nav="quotes" type="button"><span>询价线索</span><small>客户需求</small></button>
        <button class="btn nav-item nav-level-2" data-nav="orders" type="button"><span>履约订单</span><small>进度同步</small></button>
      </div></div>
      <div class="nav-section"><button class="nav-section-toggle" data-control-nav-group type="button"><span>用户与系统</span><small>账号 / 社区 / 审计</small></button><div class="nav-section-items">
        <button class="btn nav-item nav-level-2" data-nav="users" type="button"><span>用户账号</span><small>账号 / 密码 / 登录日志</small></button>
        <button class="btn nav-item nav-level-2" data-nav="community" data-community-target="comments" type="button"><span>社区与投稿</span><small>评论 / 投稿审核</small></button>
        <button class="btn nav-item nav-level-2" data-nav="feedback" type="button"><span>测试反馈</span><small>问题收集</small></button>
        <button class="btn nav-item nav-level-2" data-nav="audits" type="button"><span>操作审计</span><small>变更记录</small></button>
      </div></div>`;
  }

  function rebuildNavigation() {
    const nav = $('.admin-workspace-nav');
    if (!nav || nav.dataset.controlCenterV4 === '1') return;
    nav.dataset.controlCenterV4 = '1';
    nav.innerHTML = navMarkup();
    nav.querySelectorAll('[data-control-nav-group]').forEach(button => {
      button.addEventListener('click', () => button.closest('.nav-section')?.classList.toggle('collapsed'));
    });
    const dashboardConfig = $('.dashboard-action-grid [data-nav="config"]');
    if (dashboardConfig) {
      dashboardConfig.removeAttribute('data-nav');
      dashboardConfig.dataset.configEntry = 'basic';
      dashboardConfig.textContent = '站点状态与前台配置';
    }
    if (!$('#adminConfigBridge')) {
      const bridge = document.createElement('button');
      bridge.id = 'adminConfigBridge';
      bridge.type = 'button';
      bridge.dataset.nav = 'config';
      document.body.appendChild(bridge);
    }
  }

  function ensurePanels() {
    const config = $('[data-panel="config"]');
    if (!config) return;
    if (!$('[data-panel="globalUiV4"]')) {
      config.insertAdjacentHTML('beforebegin', '<section class="hidden" data-panel="globalUiV4" id="globalUiV4Panel"></section>');
    }
  }

  function markCustomActive(trigger) {
    $$('.admin-workspace-nav .nav-item').forEach(node => node.classList.remove('active', 'admin-nav-current'));
    if (trigger) trigger.classList.add('active', 'admin-nav-current');
    trigger?.closest('.nav-section')?.classList.remove('collapsed');
  }

  function showStandalone(panelName, trigger) {
    $$('[data-panel]').forEach(panel => panel.classList.add('hidden'));
    $(`[data-panel="${panelName}"]`)?.classList.remove('hidden');
    markCustomActive(trigger);
  }

  async function openConfigEntry(section, trigger) {
    $('#adminConfigBridge')?.click();
    await new Promise(resolve => setTimeout(resolve, 0));
    $(`[data-config-nav="${section}"]`)?.click();
    markCustomActive(trigger);
    await loadExtendedSiteFields();
  }

  function toggleField(name, label, help) {
    return `<label class="toggle-row"><input type="checkbox" name="${name}"><span><b>${label}</b><small>${help}</small></span></label>`;
  }

  function globalTemplate(settings = {}) {
    const labelField = (name, label, value) => `<div class="field"><label>${label}</label><input name="${name}" maxlength="120" value="${esc(value || '')}"></div>`;
    return `<div class="admin-toolbar"><div><span class="eyebrow">Global Frontend</span><h1>全局导航与备案</h1><p class="muted small">公共页头、页脚、显示开关和备案只在这里维护；首页内容、客服资料和工具站分别回到各自入口。</p></div><a class="btn ghost" href="/" target="_blank" rel="noopener">预览前台</a></div>
      <section class="card admin-control-card"><div class="admin-control-head"><div><h2>顶部导航</h2><p class="muted small">文案和是否显示都由后端持久化，移动端抽屉同步使用同一设置。</p></div><span class="admin-control-badge">全站生效</span></div>
      <form id="globalUiV4Form"><div class="admin-control-grid">
        ${labelField('navServicesLabel','服务入口文案',settings.navServicesLabel)}${toggleField('navServicesVisible','显示服务入口','隐藏只影响导航，不会删除页面。')}
        ${labelField('navGuidesLabel','攻略入口文案',settings.navGuidesLabel)}${toggleField('navGuidesVisible','显示攻略入口','')}
        ${labelField('navMasteryLabel','MW4 工具站文案',settings.navMasteryLabel)}${toggleField('navMasteryVisible','显示工具站导航','工具站自身还有独立公开开关，两者任一关闭都会隐藏入口。')}
        ${labelField('navRecordsLabel','业务记录文案',settings.navRecordsLabel)}${toggleField('navRecordsVisible','显示业务记录入口','')}
        ${labelField('navContactLabel','咨询入口文案',settings.navContactLabel)}${toggleField('navContactVisible','显示咨询入口','')}
        ${labelField('navAccountLabel','账号入口文案',settings.navAccountLabel)}${toggleField('navAccountVisible','显示账号入口','')}
        ${toggleField('headerQuoteVisible','显示顶部询价按钮','关闭只隐藏公共页头按钮，不影响首页/服务详情里的询价能力。')}
      </div>
      <div class="admin-control-head" style="margin-top:22px"><div><h2>页脚与首页辅助入口</h2></div></div><div class="admin-control-grid">
        ${labelField('footerServicesLabel','页脚服务文案',settings.footerServicesLabel)}${toggleField('footerServicesVisible','显示页脚服务入口','')}
        ${labelField('footerRecordsLabel','页脚业务记录文案',settings.footerRecordsLabel)}${toggleField('footerRecordsVisible','显示页脚业务记录','')}
        ${labelField('footerConsoleLabel','页脚用户控制台文案',settings.footerConsoleLabel)}${toggleField('footerConsoleVisible','显示页脚用户控制台','')}
        ${labelField('footerRulesLabel','页脚规则文案',settings.footerRulesLabel)}${toggleField('footerRulesVisible','显示服务规则','')}
        ${labelField('footerPrivacyLabel','页脚隐私文案',settings.footerPrivacyLabel)}${toggleField('footerPrivacyVisible','显示隐私说明','')}
        ${labelField('footerContactLabel','页脚咨询文案',settings.footerContactLabel)}${toggleField('footerContactVisible','显示页脚咨询入口','')}
        ${toggleField('homeQuickContactVisible','显示首页快捷联系卡片','这是现有首页快捷联系模块，不改变 /contact 页面。')}
      </div>
      <div class="admin-control-head" style="margin-top:22px"><div><h2>备案与合规展示</h2></div></div><div class="admin-control-grid">
        ${labelField('icpNumber','ICP备案号',settings.icpNumber)}${toggleField('icpVisible','显示 ICP 备案','')}
        ${labelField('publicSecurityNumber','公安备案号',settings.publicSecurityNumber)}${toggleField('publicSecurityVisible','显示公安备案','链接会按备案号数字自动生成。')}
      </div>
      <div class="admin-control-actions"><button class="btn primary" type="submit">保存全局前端配置</button><button class="btn ghost" type="button" data-global-reload>重新读取</button></div></form></section>`;
  }

  function fillBooleanFields(form, settings) {
    if (!form) return;
    $$('input[type="checkbox"][name]', form).forEach(input => {
      input.checked = settings[input.name] !== 'false';
    });
  }

  async function loadGlobalPanel() {
    const panel = $('#globalUiV4Panel');
    if (!panel) return;
    const settings = await api('/api/admin/global-ui-settings');
    panel.innerHTML = globalTemplate(settings || {});
    fillBooleanFields($('#globalUiV4Form'), settings || {});
  }

  async function saveGlobalPanel(event) {
    event.preventDefault();
    const form = event.target;
    const button = event.submitter || $('button[type="submit"]', form);
    setBusy(button, true);
    try {
      const payload = { reason:'管理员全局导航与备案保存' };
      $$('input[name]', form).forEach(input => {
        payload[input.name] = input.type === 'checkbox' ? input.checked : input.value;
      });
      await api('/api/admin/global-ui-settings', { method:'PATCH', body:JSON.stringify(payload) });
      toast('全局导航与备案已保存，前台刷新后生效');
      await loadGlobalPanel();
    } catch (error) { toast(error.message, 'error'); }
    finally { setBusy(button, false); }
  }

  function ensureExtendedSiteFields() {
    const basic = $('[data-config-section="basic"]');
    if (basic && !$('#extendedSiteStateControls')) {
      basic.insertAdjacentHTML('beforeend', `<div class="config-runtime-extra" id="extendedSiteStateControls"><h3 class="config-subhead">前台状态文案</h3><p class="form-help">后端已经支持这两个字段，但旧后台没有输入入口。现在与站点状态放在一起维护。</p><div class="grid2"><div class="field"><label>状态短文案</label><input name="statusShortTextV4" maxlength="120" placeholder="当前可正常接单"></div><div class="field"><label>全局主按钮文案</label><input name="primaryButtonLabelV4" maxlength="40" placeholder="提前询价"></div></div></div>`);
    }

    const toolPanel = $('[data-panel="toolStation"]');
    if (toolPanel && !$('#toolRuntimeV4')) {
      const toolbar = $('.admin-toolbar', toolPanel);
      toolbar?.insertAdjacentHTML('afterend', `<section class="card tool-runtime-card" id="toolRuntimeV4"><div class="admin-control-head"><div><h2>前台公开设置</h2><p class="muted small">资料编辑留在下方；公开开关和更新时间属于工具站自身入口。</p></div><span class="admin-control-badge">后端已有字段</span></div><div class="grid2"><label class="toggle-row"><input type="checkbox" id="toolPublicEnabledV4"><span><b>公开展示 MW4 工具站</b><small>关闭后公共导航会隐藏工具站入口，后台资料不删除。</small></span></label><div class="field"><label>更新时间提示</label><input id="toolUpdatedNoteV4" maxlength="120" placeholder="数据更新时间"></div></div><div class="admin-control-actions"><button class="btn primary" type="button" id="saveToolRuntimeV4">保存工具站公开设置</button></div></section>`);
    }
  }

  async function loadExtendedSiteFields() {
    ensureExtendedSiteFields();
    const site = await api('/api/admin/site-config');
    const status = $('[name="statusShortTextV4"]');
    const primary = $('[name="primaryButtonLabelV4"]');
    if (status) status.value = site.statusShortText || '';
    if (primary) primary.value = site.primaryButtonLabel || '';
    const toolEnabled = $('#toolPublicEnabledV4');
    const toolNote = $('#toolUpdatedNoteV4');
    if (toolEnabled) toolEnabled.checked = site.toolPublicEnabled !== false;
    if (toolNote) toolNote.value = site.toolUpdatedNote || '';
    return site;
  }

  function basicSectionVisible() {
    const section = $('[data-config-section="basic"]');
    return !!section && !section.classList.contains('hidden');
  }

  async function saveBasicWithoutLosingExtendedFields(button) {
    const form = $('#configForm');
    if (!form) return;
    setBusy(button, true);
    try {
      await api('/api/admin/site-config/section/basic', {
        method:'PATCH',
        body:JSON.stringify({
          brandName:form.brandName.value,
          legalEntityName:form.legalEntityName.value,
          serviceStatus:form.serviceStatus.value,
          quoteEnabled:form.quoteEnabled.checked,
          orderQueryEnabled:form.orderQueryEnabled.checked,
          statusShortText:$('[name="statusShortTextV4"]')?.value || '',
          primaryButtonLabel:$('[name="primaryButtonLabelV4"]')?.value || '',
          reason:$('#configSectionReason')?.value || '站点状态分区保存（保留扩展字段）'
        })
      });
      toast('站点状态已保存');
      await loadExtendedSiteFields();
    } catch (error) { toast(error.message, 'error'); }
    finally { setBusy(button, false); }
  }

  async function saveToolRuntime(button) {
    setBusy(button, true);
    try {
      await api('/api/admin/site-config/section/tools', {
        method:'PATCH',
        body:JSON.stringify({
          toolPublicEnabled:$('#toolPublicEnabledV4')?.checked !== false,
          toolUpdatedNote:$('#toolUpdatedNoteV4')?.value || '',
          reason:'MW4 工具站前台公开设置'
        })
      });
      toast('工具站公开设置已保存');
      await loadExtendedSiteFields();
    } catch (error) { toast(error.message, 'error'); }
    finally { setBusy(button, false); }
  }

  function initialize() {
    injectStyle();
    rebuildNavigation();
    ensurePanels();
    ensureExtendedSiteFields();
  }

  document.addEventListener('submit', event => {
    if (event.target?.id === 'globalUiV4Form') saveGlobalPanel(event);
  });

  // P0：旧后台 basic payload 不含 statusShortText / primaryButtonLabel，而后端缺失字段会写 null。
  // 捕获阶段只接管“基础状态”保存，确保这两个后端已有字段不会被其它基础设置误清空。
  document.addEventListener('click', event => {
    const button = event.target?.closest('#saveConfigSection');
    if (!button || !basicSectionVisible()) return;
    event.preventDefault();
    event.stopPropagation();
    event.stopImmediatePropagation();
    saveBasicWithoutLosingExtendedFields(button);
  }, true);

  document.addEventListener('click', event => {
    const standalone = event.target.closest('[data-admin-panel="globalUiV4"]');
    if (standalone) {
      showStandalone('globalUiV4', standalone);
      loadGlobalPanel().catch(error => toast(error.message, 'error'));
      return;
    }
    const entry = event.target.closest('[data-config-entry]');
    if (entry) {
      openConfigEntry(entry.dataset.configEntry, entry).catch(error => toast(error.message, 'error'));
      return;
    }
    const reload = event.target.closest('[data-global-reload]');
    if (reload) {
      loadGlobalPanel().then(() => toast('全局配置已重新读取')).catch(error => toast(error.message, 'error'));
      return;
    }
    const toolNav = event.target.closest('[data-nav="toolStation"]');
    if (toolNav) setTimeout(() => loadExtendedSiteFields().catch(error => toast(error.message, 'error')), 0);
    const saveTool = event.target.closest('#saveToolRuntimeV4');
    if (saveTool) saveToolRuntime(saveTool);
  });

  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', initialize, { once:true });
  else initialize();
})();
