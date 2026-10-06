(() => {
  const csrf = document.querySelector('meta[name=_csrf]')?.content;
  const csrfHeader = document.querySelector('meta[name=_csrf_header]')?.content;
  const esc = (value) => String(value ?? '').replace(/[&<>'"]/g, (c) => ({
    '&': '&amp;', '<': '&lt;', '>': '&gt;', "'": '&#39;', '"': '&quot;'
  }[c]));
  const state = { global: {}, customer: {}, site: {} };

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

  function injectStyle() {
    if (document.querySelector('#adminControlCenterV2Style')) return;
    const style = document.createElement('style');
    style.id = 'adminControlCenterV2Style';
    style.textContent = `
      .admin-control-card{margin-bottom:18px;border:1px solid rgba(255,207,51,.2)}
      .admin-control-head{display:flex;align-items:flex-start;justify-content:space-between;gap:16px;margin-bottom:16px}
      .admin-control-head p{margin:.35rem 0 0}.admin-control-badge{display:inline-flex;align-items:center;min-height:28px;padding:0 10px;border-radius:999px;background:rgba(255,207,51,.1);color:#a56f00;font-size:12px;font-weight:800;white-space:nowrap}
      .admin-control-grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:12px 16px}.admin-control-grid .wide{grid-column:1/-1}.admin-control-card textarea{min-height:76px;resize:vertical}
      .admin-control-actions{display:flex;align-items:center;gap:10px;flex-wrap:wrap;margin-top:16px;padding-top:14px;border-top:1px solid rgba(0,0,0,.08)}
      .admin-control-note{margin:0 0 16px;padding:11px 13px;border-radius:10px;background:rgba(20,23,28,.05);font-size:12px;line-height:1.7}
      .admin-control-url{font-family:ui-monospace,SFMono-Regular,Menlo,monospace;font-size:12px}
      [data-panel="config"]>.admin-subnav{display:none!important}
      #adminConfigBridge{display:none!important}
      .tool-runtime-card{margin:0 0 16px}.tool-runtime-card .grid2{align-items:end}
      .config-runtime-extra{margin-top:14px;padding-top:14px;border-top:1px solid rgba(0,0,0,.08)}
      @media(max-width:760px){.admin-control-grid{grid-template-columns:1fr}.admin-control-grid .wide{grid-column:auto}.admin-control-head{display:grid}}
    `;
    document.head.appendChild(style);
  }

  function navMarkup() {
    return `
      <div class="nav-section nav-section-primary"><span class="nav-section-kicker">工作台</span><button class="btn nav-item nav-level-1" data-nav="dashboard" type="button"><span>运营总览</span><small>待办 / 数据 / 快捷入口</small></button></div>
      <div class="nav-section"><button class="nav-section-toggle" data-control-nav-group type="button"><span>站点运营</span><small>前台展示与客服</small></button><div class="nav-section-items">
        <button class="btn nav-item nav-level-2" data-admin-panel="globalUiV2" type="button"><span>全局导航与备案</span><small>顶部 / 页脚 / 备案</small></button>
        <button class="btn nav-item nav-level-2" data-config-entry="basic" type="button"><span>站点状态</span><small>品牌 / 接单 / 询价</small></button>
        <button class="btn nav-item nav-level-2" data-config-entry="home" type="button"><span>首页内容</span><small>首屏 / 价格 / 标签</small></button>
        <button class="btn nav-item nav-level-2" data-config-entry="visual" type="button"><span>首页视觉</span><small>主图 / 背景 / 封面</small></button>
        <button class="btn nav-item nav-level-2" data-config-entry="notice" type="button"><span>公告</span><small>临时运营通知</small></button>
        <button class="btn nav-item nav-level-2" data-config-entry="seo" type="button"><span>搜索展示</span><small>SEO 标题 / 描述</small></button>
        <button class="btn nav-item nav-level-2" data-admin-panel="customerServiceV2" type="button"><span>客服中心</span><small>悬浮客服 / 企业微信</small></button>
        <button class="btn nav-item nav-level-2" data-config-entry="trust" type="button"><span>信任 FAQ</span><small>案例 / 常见问题</small></button>
        <button class="btn nav-item nav-level-2" data-config-entry="copy" type="button"><span>业务文案</span><small>询价 / 订单 / 页脚</small></button>
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
    const nav = document.querySelector('.admin-workspace-nav');
    if (!nav || nav.dataset.controlCenterV2 === '1') return;
    nav.dataset.controlCenterV2 = '1';
    nav.innerHTML = navMarkup();
    nav.querySelectorAll('[data-control-nav-group]').forEach((button) => {
      button.addEventListener('click', () => button.closest('.nav-section')?.classList.toggle('collapsed'));
    });
    const dashboardConfig = document.querySelector('.dashboard-action-grid [data-nav="config"]');
    if (dashboardConfig) {
      dashboardConfig.removeAttribute('data-nav');
      dashboardConfig.dataset.configEntry = 'basic';
      dashboardConfig.textContent = '站点状态与前台配置';
    }
    if (!document.querySelector('#adminConfigBridge')) {
      const bridge = document.createElement('button');
      bridge.id = 'adminConfigBridge';
      bridge.type = 'button';
      bridge.dataset.nav = 'config';
      document.body.appendChild(bridge);
    }
  }

  function ensurePanels() {
    const config = document.querySelector('[data-panel="config"]');
    if (!config) return;
    if (!document.querySelector('[data-panel="globalUiV2"]')) {
      config.insertAdjacentHTML('beforebegin', '<section class="hidden" data-panel="globalUiV2" id="globalUiV2Panel"></section>');
    }
    if (!document.querySelector('[data-panel="customerServiceV2"]')) {
      config.insertAdjacentHTML('beforebegin', '<section class="hidden" data-panel="customerServiceV2" id="customerServiceV2Panel"></section>');
    }
  }

  function globalTemplate(settings) {
    return `<div class="admin-toolbar"><div><span class="eyebrow">全局前端控制</span><h1>全局导航与备案</h1><p class="muted small">只放全站公共导航、页脚和备案信息；客服与业务状态已经拆到独立入口。</p></div><a class="btn ghost" href="/" target="_blank" rel="noopener">预览前台</a></div>
      <section class="card admin-control-card"><div class="admin-control-head"><div><h2>公共导航与合规信息</h2><p class="muted small">保存后通过公开只读配置接口即时应用到公共页头和页脚。</p></div><span class="admin-control-badge">全站生效</span></div>
      <form id="globalUiForm"><div class="admin-control-grid">
        <div class="field"><label>顶部“服务”文案</label><input name="navServicesLabel" maxlength="100" value="${esc(settings.navServicesLabel)}"></div>
        <div class="field"><label>顶部“攻略”文案</label><input name="navGuidesLabel" maxlength="100" value="${esc(settings.navGuidesLabel)}"></div>
        <div class="field"><label>MW4 工具站文案</label><input name="navMasteryLabel" maxlength="100" value="${esc(settings.navMasteryLabel)}"></div>
        <div class="field"><label>业务记录文案</label><input name="navRecordsLabel" maxlength="100" value="${esc(settings.navRecordsLabel)}"></div>
        <div class="field"><label>个人空间文案</label><input name="navAccountLabel" maxlength="100" value="${esc(settings.navAccountLabel)}"></div>
        <div class="field"><label>页脚服务文案</label><input name="footerServicesLabel" maxlength="100" value="${esc(settings.footerServicesLabel)}"></div>
        <div class="field"><label>页脚业务记录文案</label><input name="footerRecordsLabel" maxlength="100" value="${esc(settings.footerRecordsLabel)}"></div>
        <div class="field"><label>页脚个人空间文案</label><input name="footerAccountLabel" maxlength="100" value="${esc(settings.footerAccountLabel)}"></div>
        <div class="field"><label>页脚规则文案</label><input name="footerRulesLabel" maxlength="100" value="${esc(settings.footerRulesLabel)}"></div>
        <div class="field"><label>页脚隐私文案</label><input name="footerPrivacyLabel" maxlength="100" value="${esc(settings.footerPrivacyLabel)}"></div>
        <div class="field"><label>ICP备案号</label><input name="icpNumber" maxlength="100" value="${esc(settings.icpNumber)}"></div>
        <div class="field"><label>公安备案号</label><input name="publicSecurityNumber" maxlength="100" value="${esc(settings.publicSecurityNumber)}"></div>
      </div><div class="admin-control-actions"><button class="btn primary" type="submit">保存全局导航与备案</button><button class="btn ghost" type="button" data-control-reload>重新读取</button></div></form></section>`;
  }

  function customerTemplate(globalSettings, customer, site) {
    const enabled = customer.customerServiceEnabled !== 'false' ? ' checked' : '';
    const floating = globalSettings.floatingCustomerServiceEnabled !== 'false' ? ' checked' : '';
    return `<div class="admin-toolbar"><div><span class="eyebrow">Customer Service</span><h1>客服中心</h1><p class="muted small">这里只管理悬浮客服和企业微信官方会话入口；旧微信号、QQ、电话等历史字段不再作为日常入口暴露。</p></div><a class="btn ghost" href="/contact" target="_blank" rel="noopener">预览客服流程</a></div>
      <section class="card admin-control-card"><div class="admin-control-head"><div><h2>悬浮客服</h2><p class="muted small">全站普通页面只保留这一处常驻客服入口。</p></div><span class="admin-control-badge">前台唯一常驻客服</span></div>
      <form id="customerServiceForm"><p class="admin-control-note"><b>路由规则：</b>每个分类可以配置独立企业微信官方链接；留空时使用默认客服链接。后台保存后下一次点击立即生效，无需重启服务。</p>
      <div class="admin-control-grid">
        <label class="toggle-row wide"><input type="checkbox" name="customerServiceEnabled"${enabled}><span><b>启用企业微信在线客服</b><small>关闭后四类客服开始会话时回到客服中心提示不可用。</small></span></label>
        <label class="toggle-row wide"><input type="checkbox" name="floatingCustomerServiceEnabled"${floating}><span><b>显示右下角悬浮在线客服</b><small>关闭只影响悬浮入口，不删除兼容客服路由。</small></span></label>
        <div class="field"><label>客服按钮文案</label><input name="floatingCustomerServiceLabel" maxlength="100" value="${esc(globalSettings.floatingCustomerServiceLabel)}"></div>
        <div class="field"><label>客服面板标题</label><input name="floatingCustomerServiceTitle" maxlength="100" value="${esc(globalSettings.floatingCustomerServiceTitle)}"></div>
        <div class="field wide"><label>客服面板说明</label><textarea name="floatingCustomerServiceIntro" maxlength="300">${esc(globalSettings.floatingCustomerServiceIntro)}</textarea></div>
        <div class="field"><label>客服时间</label><input name="serviceHours" maxlength="100" value="${esc(site.serviceHours || '10:00-24:00')}" placeholder="10:00-24:00"></div>
        <div class="field"><label>安全提示</label><input name="floatingCustomerServiceNote" maxlength="300" value="${esc(globalSettings.floatingCustomerServiceNote)}"></div>
        <div class="field wide"><label>默认企业微信客服链接</label><input class="admin-control-url" name="customerServiceDefaultUrl" maxlength="500" value="${esc(customer.customerServiceDefaultUrl)}" placeholder="https://work.weixin.qq.com/kfid/..."><small class="form-help">四个分类未单独配置时回退到这里。</small></div>
        <div class="field"><label>游戏咨询</label><input class="admin-control-url" name="customerServiceGameUrl" maxlength="500" value="${esc(customer.customerServiceGameUrl)}" placeholder="留空则使用默认链接"></div>
        <div class="field"><label>订单咨询</label><input class="admin-control-url" name="customerServiceOrderUrl" maxlength="500" value="${esc(customer.customerServiceOrderUrl)}" placeholder="留空则使用默认链接"></div>
        <div class="field"><label>售后处理</label><input class="admin-control-url" name="customerServiceAfterSalesUrl" maxlength="500" value="${esc(customer.customerServiceAfterSalesUrl)}" placeholder="留空则使用默认链接"></div>
        <div class="field"><label>商务合作</label><input class="admin-control-url" name="customerServiceBusinessUrl" maxlength="500" value="${esc(customer.customerServiceBusinessUrl)}" placeholder="留空则使用默认链接"></div>
      </div><div class="admin-control-actions"><button class="btn primary" type="submit">保存客服中心配置</button><button class="btn ghost" type="button" data-control-reload>重新读取</button></div></form></section>`;
  }

  function injectBasicRuntimeFields() {
    const basic = document.querySelector('[data-config-section="basic"]');
    if (!basic || basic.querySelector('.config-runtime-extra')) return;
    const anchor = basic.querySelector('[name="serviceStatus"]')?.closest('.field');
    const html = `<div class="grid2 config-runtime-extra"><div class="field"><label>兜底状态短文案</label><input name="statusShortText" maxlength="120" placeholder="当前可正常接单"><small class="form-help">游戏有独立运营状态时，以游戏状态文案为准。</small></div><div class="field"><label>兜底主按钮文案</label><input name="primaryButtonLabel" maxlength="40" placeholder="提前询价"><small class="form-help">游戏状态设置了 CTA 时，以游戏状态为准。</small></div></div>`;
    (anchor || basic).insertAdjacentHTML(anchor ? 'afterend' : 'beforeend', html);
  }

  function injectToolRuntimeCard() {
    const panel = document.querySelector('[data-panel="toolStation"]');
    const toolbar = panel?.querySelector('.admin-toolbar');
    if (!panel || !toolbar || panel.querySelector('#toolRuntimeForm')) return;
    toolbar.insertAdjacentHTML('afterend', `<section class="card tool-runtime-card"><form id="toolRuntimeForm"><div class="grid2"><label class="toggle-row"><input type="checkbox" name="toolPublicEnabled"><span><b>公开展示 MW4 工具站</b><small>关闭后公共导航和前台入口隐藏，后台资料不删除。</small></span></label><div class="field"><label>前台更新时间提示</label><input name="toolUpdatedNote" maxlength="120" placeholder="数据更新时间"></div></div><div class="admin-control-actions"><button class="btn primary small-btn" type="submit">保存工具站公开设置</button></div></form></section>`);
  }

  function setBasicRuntimeValues(site) {
    const form = document.querySelector('#configForm');
    if (!form) return;
    if (form.elements.statusShortText) form.elements.statusShortText.value = site.statusShortText || '';
    if (form.elements.primaryButtonLabel) form.elements.primaryButtonLabel.value = site.primaryButtonLabel || '';
  }

  function setToolRuntimeValues(site) {
    const form = document.querySelector('#toolRuntimeForm');
    if (!form) return;
    form.elements.toolPublicEnabled.checked = site.toolPublicEnabled !== false;
    form.elements.toolUpdatedNote.value = site.toolUpdatedNote || '';
  }

  function markCustomActive(button) {
    document.querySelectorAll('.admin-workspace-nav .nav-item').forEach((item) => item.classList.remove('active'));
    button?.classList.add('active');
    button?.closest('.nav-section')?.classList.remove('collapsed');
  }

  function showStandalone(panelName, button) {
    document.querySelectorAll('[data-panel]').forEach((panel) => panel.classList.add('hidden'));
    document.querySelector(`[data-panel="${panelName}"]`)?.classList.remove('hidden');
    markCustomActive(button);
  }

  const CONFIG_TITLES = {
    basic: ['站点状态', '品牌、接单状态、询价开关和业务进度查询'],
    home: ['首页内容', '首页首屏、起步价、服务数量和信任标签'],
    visual: ['首页视觉', '首页主图、背景图和服务封面显示方式'],
    notice: ['公告', '版本维护、赛季更新和排队提醒'],
    seo: ['搜索展示', '站点 SEO 标题和描述'],
    trust: ['信任 FAQ', '公开案例和常见问题'],
    copy: ['业务文案', '询价成功、订单说明和页脚业务文案'],
    gameStatus: ['游戏运营状态', '按游戏控制接单状态、公开文案和 CTA']
  };

  async function openConfigSection(section, button) {
    document.querySelector('#adminConfigBridge')?.click();
    await new Promise((resolve) => setTimeout(resolve, 0));
    document.querySelector(`[data-config-nav="${section}"]`)?.click();
    const title = CONFIG_TITLES[section] || [section, ''];
    const toolbar = document.querySelector('[data-panel="config"] .admin-toolbar');
    if (toolbar) {
      const eyebrow = toolbar.querySelector('.eyebrow');
      const h1 = toolbar.querySelector('h1');
      if (eyebrow) eyebrow.textContent = '按功能入口管理';
      if (h1) h1.textContent = title[0];
      let p = toolbar.querySelector('p');
      if (!p) {
        p = document.createElement('p');
        p.className = 'muted small';
        h1?.insertAdjacentElement('afterend', p);
      }
      if (p) p.textContent = title[1];
    }
    markCustomActive(button);
    if (section === 'basic') {
      try {
        state.site = await api('/api/admin/site-config');
        setBasicRuntimeValues(state.site);
      } catch (error) { toast(error.message, 'error'); }
    }
  }

  function globalPayload(form) {
    return {
      navServicesLabel: form.elements.navServicesLabel.value,
      navGuidesLabel: form.elements.navGuidesLabel.value,
      navMasteryLabel: form.elements.navMasteryLabel.value,
      navRecordsLabel: form.elements.navRecordsLabel.value,
      navAccountLabel: form.elements.navAccountLabel.value,
      footerServicesLabel: form.elements.footerServicesLabel.value,
      footerRecordsLabel: form.elements.footerRecordsLabel.value,
      footerAccountLabel: form.elements.footerAccountLabel.value,
      footerRulesLabel: form.elements.footerRulesLabel.value,
      footerPrivacyLabel: form.elements.footerPrivacyLabel.value,
      icpNumber: form.elements.icpNumber.value,
      publicSecurityNumber: form.elements.publicSecurityNumber.value,
      reason: '管理员全局导航与备案保存'
    };
  }

  function customerGlobalPayload(form) {
    return {
      floatingCustomerServiceEnabled: form.elements.floatingCustomerServiceEnabled.checked,
      floatingCustomerServiceLabel: form.elements.floatingCustomerServiceLabel.value,
      floatingCustomerServiceTitle: form.elements.floatingCustomerServiceTitle.value,
      floatingCustomerServiceIntro: form.elements.floatingCustomerServiceIntro.value,
      floatingCustomerServiceNote: form.elements.floatingCustomerServiceNote.value,
      reason: '管理员客服中心保存悬浮客服配置'
    };
  }

  function customerRoutingPayload(form) {
    return {
      customerServiceEnabled: form.elements.customerServiceEnabled.checked,
      customerServiceDefaultUrl: form.elements.customerServiceDefaultUrl.value,
      customerServiceGameUrl: form.elements.customerServiceGameUrl.value,
      customerServiceOrderUrl: form.elements.customerServiceOrderUrl.value,
      customerServiceAfterSalesUrl: form.elements.customerServiceAfterSalesUrl.value,
      customerServiceBusinessUrl: form.elements.customerServiceBusinessUrl.value,
      reason: '管理员客服中心保存企业微信路由'
    };
  }

  function contactPayload(site, serviceHours) {
    return {
      contactTitle: site.contactTitle,
      contactIntro: site.contactIntro,
      wechatId: site.wechatId,
      wechatQrImage: site.wechatQrImage,
      wechatQrSize: site.wechatQrSize,
      contactChannels: site.contactChannels || [],
      qqId: site.qqId,
      qqLink: site.qqLink,
      phone: site.phone,
      serviceHours,
      taobaoUrl: site.taobaoUrl,
      xianyuUrl: site.xianyuUrl,
      contactNotice: site.contactNotice,
      wecomKefuUrl: site.wecomKefuUrl,
      wecomKefuNote: site.wecomKefuNote,
      quickContactTitle: site.quickContactTitle,
      taobaoLabel: site.taobaoLabel,
      xianyuLabel: site.xianyuLabel,
      wecomKefuLabel: site.wecomKefuLabel,
      wecomKefuButton: site.wecomKefuButton,
      reason: '管理员客服中心仅更新客服时间（保留历史渠道字段）'
    };
  }

  async function loadAll() {
    const [globalSettings, customer, site] = await Promise.all([
      api('/api/admin/global-ui-settings'),
      api('/api/admin/customer-service-settings'),
      api('/api/admin/site-config')
    ]);
    state.global = globalSettings || {};
    state.customer = customer || {};
    state.site = site || {};
    const globalPanel = document.querySelector('#globalUiV2Panel');
    const customerPanel = document.querySelector('#customerServiceV2Panel');
    if (globalPanel) globalPanel.innerHTML = globalTemplate(state.global);
    if (customerPanel) customerPanel.innerHTML = customerTemplate(state.global, state.customer, state.site);
    injectBasicRuntimeFields();
    injectToolRuntimeCard();
    setBasicRuntimeValues(state.site);
    setToolRuntimeValues(state.site);
  }

  async function saveGlobal(form, button) {
    const old = button?.textContent;
    if (button) { button.disabled = true; button.textContent = '保存中…'; }
    try {
      state.global = await api('/api/admin/global-ui-settings', { method: 'PATCH', body: JSON.stringify(globalPayload(form)) });
      toast('全局导航与备案已保存');
      await loadAll();
    } catch (error) { toast(error.message, 'error'); }
    finally { if (button) { button.disabled = false; button.textContent = old || '保存'; } }
  }

  async function saveCustomer(form, button) {
    const old = button?.textContent;
    if (button) { button.disabled = true; button.textContent = '保存中…'; }
    try {
      const currentSite = await api('/api/admin/site-config');
      state.global = await api('/api/admin/global-ui-settings', { method: 'PATCH', body: JSON.stringify(customerGlobalPayload(form)) });
      state.customer = await api('/api/admin/customer-service-settings', { method: 'PATCH', body: JSON.stringify(customerRoutingPayload(form)) });
      state.site = await api('/api/admin/site-config/section/contact', { method: 'PATCH', body: JSON.stringify(contactPayload(currentSite, form.elements.serviceHours.value)) });
      toast('客服中心配置已保存并即时生效');
      await loadAll();
    } catch (error) { toast(error.message, 'error'); }
    finally { if (button) { button.disabled = false; button.textContent = old || '保存'; } }
  }

  async function saveToolRuntime(form, button) {
    const old = button?.textContent;
    if (button) { button.disabled = true; button.textContent = '保存中…'; }
    try {
      state.site = await api('/api/admin/site-config/section/tools', { method: 'PATCH', body: JSON.stringify({
        toolUpdatedNote: form.elements.toolUpdatedNote.value,
        toolPublicEnabled: form.elements.toolPublicEnabled.checked,
        reason: 'MW4 工具站入口保存公开状态'
      }) });
      setToolRuntimeValues(state.site);
      toast('工具站公开设置已保存');
    } catch (error) { toast(error.message, 'error'); }
    finally { if (button) { button.disabled = false; button.textContent = old || '保存'; } }
  }

  async function saveLegacyBasicWithoutLosingRuntime(button) {
    const form = document.querySelector('#configForm');
    if (!form) return;
    const oldText = button.textContent;
    button.disabled = true;
    button.textContent = '保存中…';
    try {
      state.site = await api('/api/admin/site-config/section/basic', {
        method: 'PATCH',
        body: JSON.stringify({
          brandName: form.brandName.value,
          legalEntityName: form.legalEntityName.value,
          serviceStatus: form.serviceStatus.value,
          quoteEnabled: form.quoteEnabled.checked,
          orderQueryEnabled: form.orderQueryEnabled.checked,
          statusShortText: form.elements.statusShortText?.value || '',
          primaryButtonLabel: form.elements.primaryButtonLabel?.value || '',
          reason: document.querySelector('#configSectionReason')?.value || '后台站点状态入口保存'
        })
      });
      setBasicRuntimeValues(state.site);
      toast('站点状态已保存');
    } catch (error) { toast(error.message, 'error'); }
    finally { button.disabled = false; button.textContent = oldText; }
  }

  function initEvents() {
    document.addEventListener('submit', (event) => {
      if (event.target?.id === 'globalUiForm') {
        event.preventDefault();
        saveGlobal(event.target, event.submitter).catch((e) => toast(e.message, 'error'));
      } else if (event.target?.id === 'customerServiceForm') {
        event.preventDefault();
        saveCustomer(event.target, event.submitter).catch((e) => toast(e.message, 'error'));
      } else if (event.target?.id === 'toolRuntimeForm') {
        event.preventDefault();
        saveToolRuntime(event.target, event.submitter).catch((e) => toast(e.message, 'error'));
      }
    });

    document.addEventListener('click', (event) => {
      const standalone = event.target?.closest('[data-admin-panel]');
      if (standalone) {
        event.preventDefault();
        showStandalone(standalone.dataset.adminPanel, standalone);
        return;
      }
      const configEntry = event.target?.closest('[data-config-entry]');
      if (configEntry) {
        event.preventDefault();
        openConfigSection(configEntry.dataset.configEntry, configEntry).catch((e) => toast(e.message, 'error'));
        return;
      }
      if (event.target?.closest('[data-control-reload]')) {
        loadAll().then(() => toast('后台配置已重新读取')).catch((e) => toast(e.message, 'error'));
        return;
      }
      const normalNav = event.target?.closest('[data-nav]');
      if (normalNav && normalNav.id !== 'adminConfigBridge') {
        document.querySelectorAll('.admin-workspace-nav [data-admin-panel],.admin-workspace-nav [data-config-entry]').forEach((item) => item.classList.remove('active'));
        if (normalNav.dataset.nav === 'toolStation') {
          api('/api/admin/site-config').then((site) => { state.site = site; setToolRuntimeValues(site); }).catch(() => {});
        }
      }
    });

    document.addEventListener('click', (event) => {
      const button = event.target?.closest('#saveConfigSection');
      const basic = document.querySelector('[data-config-section="basic"]');
      if (!button || !basic || basic.classList.contains('hidden')) return;
      event.preventDefault();
      event.stopPropagation();
      event.stopImmediatePropagation();
      saveLegacyBasicWithoutLosingRuntime(button);
    }, true);
  }

  async function init() {
    injectStyle();
    rebuildNavigation();
    ensurePanels();
    injectBasicRuntimeFields();
    injectToolRuntimeCard();
    initEvents();
    await loadAll();
  }

  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', () => init().catch((e) => toast(e.message, 'error')), { once: true });
  else init().catch((e) => toast(e.message, 'error'));
})();
