(() => {
  'use strict';

  const pathMatch = /^\/next\/(contact|trust)\/?$/.exec(location.pathname);
  if (!pathMatch) return;

  const STAGE = 'N27 · SUPPORT & TRUST';
  const $ = (selector, root = document) => root.querySelector(selector);
  const $$ = (selector, root = document) => [...root.querySelectorAll(selector)];
  const text = (value) => String(value ?? '').trim();
  const list = (value) => Array.isArray(value) ? value : [];
  let publicCases = [];
  let publicCasesStatus = 'idle'; // idle | loading | ready | error
  let publicCasesRequest = 0;

  function currentPage() {
    const match = /^\/next\/(contact|trust)\/?$/.exec(location.pathname);
    return match ? match[1] : '';
  }

  function node(tag, className, value) {
    const element = document.createElement(tag);
    if (className) element.className = className;
    if (value !== undefined && value !== null) element.textContent = String(value);
    return element;
  }

  function safeLink(value) {
    const raw = text(value);
    if (raw.startsWith('/') && !raw.startsWith('//')) return raw;
    try {
      const url = new URL(raw);
      if (url.protocol === 'https:' && !url.username && !url.password) return url.href;
    } catch (_) {}
    return '';
  }

  function ensureSection(page) {
    const id = page === 'contact' ? 'n27Contact' : 'n27Trust';
    let section = document.getElementById(id);
    if (section) return section;
    section = node('section', `page n27-page n27-${page}`);
    section.id = id;
    section.setAttribute('role', 'main');
    section.tabIndex = -1;
    section.dataset.page = page;
    section.append(node('div', 'shell'));
    const footer = $('.next-footer') || $('.footer');
    if (footer) footer.before(section); else document.body.append(section);
    return section;
  }

  function activate(section, page) {
    $$('.page').forEach((candidate) => candidate.classList.toggle('active', candidate === section));
    $$('.navlink[data-go]').forEach((link) => {
      link.classList.remove('active');
      link.removeAttribute('aria-current');
    });
    document.documentElement.dataset.frontendPage = page;
    document.documentElement.dataset.frontendNextStage = 'n27-support-trust';
  }

  function installStyles() {
    if ($('#frontend-next-n27-style')) return;
    const style = document.createElement('style');
    style.id = 'frontend-next-n27-style';
    style.textContent = `
      .n27-page{min-height:100vh;padding:112px 0 84px;background:radial-gradient(circle at 80% 8%,#ffcb2d13,transparent 30rem)}
      .n27-head{display:grid;grid-template-columns:minmax(0,1fr) auto;align-items:end;gap:18px;margin-bottom:16px}.n27-head h1{font-size:clamp(42px,6vw,72px);margin:10px 0 8px}.n27-head p{max-width:760px;color:#9ba4ae;line-height:1.75;margin:0}.n27-kicker{color:#d9bb61;font-size:10px;font-weight:900;letter-spacing:.16em;text-transform:uppercase}.n27-actions{display:flex;gap:8px;flex-wrap:wrap}
      .n27-grid{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:11px}.n27-card{border:1px solid #ffffff16;border-radius:15px;background:#0d1116;padding:17px;min-width:0}.n27-card h2,.n27-card h3{margin:0 0 8px}.n27-card p{color:#919ba5;line-height:1.7;margin:5px 0}.n27-card small{color:#c9b76f}.n27-card .btn{display:inline-flex;margin-top:12px}.n27-card[data-primary="1"]{border-color:#ffcb2d44;background:linear-gradient(145deg,#17140d,#0d1116)}
      .n27-strip{display:flex;gap:7px;flex-wrap:wrap;margin:0 0 13px}.n27-chip{border:1px solid #ffffff15;border-radius:999px;background:#ffffff05;padding:7px 10px;font-size:9px;color:#c5cbd1}.n27-status{display:grid;grid-template-columns:1fr auto;gap:14px;align-items:center;border:1px solid #ffffff15;border-radius:14px;background:#0a0e12;padding:14px;margin-bottom:13px}.n27-status strong{display:block}.n27-status span{display:block;color:#818b95;font-size:9px;margin-top:4px}.n27-status b{color:#d6bb65;font-size:11px}
      .n27-section{margin-top:24px}.n27-section-head{display:flex;justify-content:space-between;gap:12px;align-items:end;margin-bottom:11px}.n27-section-head h2{margin:0}.n27-section-head p{margin:0;color:#7f8993;font-size:10px}.n27-faq{display:grid;gap:8px}.n27-faq details{border:1px solid #ffffff15;border-radius:12px;background:#0d1116;padding:13px}.n27-faq summary{cursor:pointer;font-weight:800}.n27-faq p{color:#8f99a3;line-height:1.75;margin:10px 0 0}.n27-empty{border:1px dashed #ffffff20;border-radius:14px;padding:24px;text-align:center;color:#818b95}.n27-case-meta{display:flex;justify-content:space-between;gap:8px;color:#717c87;font-size:9px;margin-top:10px}.n27-note{margin-top:18px;color:#66717c;font-size:9px;line-height:1.7}
      @media(max-width:900px){.n27-head{grid-template-columns:1fr}.n27-grid{grid-template-columns:1fr 1fr}}
      @media(max-width:650px){.n27-page{padding-top:92px}.n27-actions{display:grid}.n27-grid{grid-template-columns:1fr}.n27-status{grid-template-columns:1fr}.n27-section-head{align-items:flex-start;flex-direction:column}}
      /* N29 mobile: allow long customer-provided and published content to wrap safely. */
      .n27-page .n27-card,.n27-page .n27-empty,.n27-page .n27-status>*,.n27-page .n27-section-head>*{min-width:0}
      .n27-page .n27-card,.n27-page .n27-empty,.n27-page .n27-section-head,.n27-page .n27-faq{overflow-wrap:anywhere}
    `;
    document.head.append(style);
  }

  function setMetadata(page, configuration) {
    const brand = text(configuration?.brandName) || '星火网游';
    const title = page === 'contact' ? `一对一咨询 · ${brand}` : `信任中心 · ${brand}`;
    const description = page === 'contact'
      ? '查看当前真实客服渠道、服务状态与提交询价入口。'
      : '查看服务保障、公开完成案例与常见问题。';
    document.title = title;
    let meta = $('meta[name="description"]');
    if (!meta) {
      meta = document.createElement('meta');
      meta.name = 'description';
      document.head.append(meta);
    }
    meta.content = description;
    let canonical = $('link[rel="canonical"]');
    if (!canonical) {
      canonical = document.createElement('link');
      canonical.rel = 'canonical';
      document.head.append(canonical);
    }
    canonical.href = `https://wydlgf.cn/${page}`;
    let robots = $('meta[name="robots"]');
    if (!robots) {
      robots = document.createElement('meta');
      robots.name = 'robots';
      document.head.append(robots);
    }
    robots.content = 'noindex,follow';
  }

  function header(page) {
    const head = node('div', 'n27-head');
    const intro = node('div');
    intro.append(node('div', 'n27-kicker', '咨询与服务保障'));
    intro.append(node('h1', '', page === 'contact' ? '一对一咨询' : '信任中心'));
    intro.append(node('p', '', page === 'contact'
      ? '查看客服渠道和接单状态，也可提交需求获取报价。'
      : '了解服务保障、已完成的交付案例和下单前常见问题。'));
    const actions = node('div', 'n27-actions');
    const legacy = node('a', 'btn', page === 'contact' ? '打开旧版咨询页' : '打开旧版信任页');
    legacy.href = `/${page}`;
    legacy.dataset.n27Legacy = '1';
    const primary = node('a', 'btn gold', page === 'contact' ? '提交询价' : '查看服务大厅');
    primary.href = page === 'contact' ? '/next/quote' : '/next/services';
    actions.append(legacy, primary);
    head.append(intro, actions);
    return head;
  }

  function serviceStatus(configuration) {
    const box = node('div', 'n27-status');
    const copy = node('div');
    copy.append(node('strong', '', '当前服务状态'));
    copy.append(node('span', '', pageStatusDetail(configuration)));
    box.append(copy, node('b', '', text(configuration?.statusShortText) || statusLabel(configuration?.serviceStatus)));
    return box;
  }

  function statusLabel(value) {
    return ({
      open: '当前正常接单',
      reservation: '预约中 · 可提前询价',
      preheat: '预热中 · 可提前询价',
      maintenance: '维护中',
      suspended: '暂停接单',
      closed: '暂停接单',
      busy: '当前繁忙'
    })[text(value)] || '状态以客服确认为准';
  }

  function pageStatusDetail(configuration) {
    if (configuration?.quoteEnabled === false) return '当前网页询价入口已暂停，请使用后台配置的客服渠道确认。';
    return '网页询价与客服渠道均以后台当前配置为准，不在新版页面写死联系方式。';
  }

  function contactCard(item, primary = false) {
    const card = node('article', 'n27-card');
    if (primary) card.dataset.primary = '1';
    card.append(node('small', '', primary ? '推荐联系' : '客服渠道'));
    card.append(node('h2', '', text(item?.label) || '在线客服'));
    if (text(item?.wechatId)) card.append(node('p', '', `微信：${text(item.wechatId)}`));
    if (text(item?.note)) card.append(node('p', '', text(item.note)));
    const url = safeLink(item?.jumpUrl);
    if (url) {
      const link = node('a', 'btn gold', '打开客服');
      link.href = url;
      link.dataset.supportDirect = '';
      card.append(link);
    }
    return card;
  }

  function renderContact(shell, configuration) {
    shell.replaceChildren(header('contact'), serviceStatus(configuration));
    const channels = list(configuration?.contactChannels).filter((item) => item && typeof item === 'object');
    const grid = node('div', 'n27-grid');
    channels.forEach((item, index) => grid.append(contactCard(item, index === 0)));

    const fallback = safeLink(configuration?.wecomKefuUrl);
    if (fallback && !channels.some((item) => safeLink(item?.jumpUrl) === fallback)) {
      grid.append(contactCard({ label: '企业微信客服', note: '后台配置的一对一客服入口', jumpUrl: fallback }, channels.length === 0));
    }
    if (!grid.childElementCount) {
      const empty = node('div', 'n27-empty', '当前没有配置可直接打开的客服渠道，可先提交询价留下需求与联系方式。');
      grid.append(empty);
    }
    shell.append(grid);

    const flow = node('section', 'n27-section');
    const title = node('div', 'n27-section-head');
    title.append(node('h2', '', '建议流程'), node('p', '', '减少重复沟通，先描述需求再确认最终金额。'));
    flow.append(title);
    const steps = node('div', 'n27-grid');
    [
      ['01', '选择服务或提交需求', '在服务大厅选择项目，也可以直接进入询价说明目标。'],
      ['02', '客服核对当前进度', '根据平台、目标、已有进度和时效要求确认可行性。'],
      ['03', '确认报价再进入交易', '最终金额以客服确认结果为准，网页不会把未确认价格当成成交价。']
    ].forEach(([tag, titleText, detail]) => {
      const card = node('article', 'n27-card');
      card.append(node('small', '', tag), node('h3', '', titleText), node('p', '', detail));
      steps.append(card);
    });
    flow.append(steps);
    shell.append(flow, node('p', 'n27-note', '本页只消费 `/api/site-config` 已公开的客服配置；联系方式仍由后台统一维护。'));
  }

  function trustCaseCard(item) {
    const card = node('article', 'n27-card');
    card.append(node('small', '', text(item?.tag) || '服务说明'));
    card.append(node('h3', '', text(item?.title) || '服务保障'));
    if (text(item?.metric)) card.append(node('p', '', text(item.metric)));
    if (text(item?.detail)) card.append(node('p', '', text(item.detail)));
    return card;
  }

  function publicCaseCard(item) {
    const card = node('article', 'n27-card');
    card.append(node('small', '', '已完成 · 公开脱敏记录'));
    card.append(node('h3', '', text(item?.maskedNo) || '已脱敏业务记录'));
    card.append(node('p', '', text(item?.summary) || '已完成交付'));
    const meta = node('div', 'n27-case-meta');
    meta.append(node('span', '', item?.finishedAt ? `完成日期 ${String(item.finishedAt).slice(0, 10)}` : '完成日期已脱敏'));
    card.append(meta);
    return card;
  }

  function renderTrust(shell, configuration) {
    shell.replaceChildren(header('trust'));
    const tags = list(configuration?.trustTags).map(text).filter(Boolean);
    if (tags.length) {
      const strip = node('div', 'n27-strip');
      tags.forEach((value) => strip.append(node('span', 'n27-chip', value)));
      shell.append(strip);
    }

    const configured = node('section', 'n27-section');
    const configuredHead = node('div', 'n27-section-head');
    configuredHead.append(node('h2', '', '服务保障'), node('p', '', '由后台站点配置统一维护。'));
    configured.append(configuredHead);
    const configuredGrid = node('div', 'n27-grid');
    list(configuration?.trustCases).forEach((item) => configuredGrid.append(trustCaseCard(item)));
    if (!configuredGrid.childElementCount) configuredGrid.append(node('div', 'n27-empty', '当前暂无额外保障说明。'));
    configured.append(configuredGrid);
    shell.append(configured);

    const cases = node('section', 'n27-section');
    const casesHead = node('div', 'n27-section-head');
    casesHead.append(node('h2', '', '公开完成案例'), node('p', '', '仅展示服务端明确允许公开的脱敏记录。'));
    cases.append(casesHead);
    const caseGrid = node('div', 'n27-grid');
    caseGrid.id = 'n27PublicCases';
    caseGrid.setAttribute('aria-live', 'polite');
    if (publicCasesStatus === 'ready') {
      publicCases.slice(0, 6).forEach((item) => caseGrid.append(publicCaseCard(item)));
      if (!caseGrid.childElementCount) caseGrid.append(node('div', 'n27-empty', '当前没有公开完成案例。'));
    } else if (publicCasesStatus === 'error') {
      const state = node('div', 'n27-empty');
      state.append(node('p', '', '公开案例暂时读取失败，不能据此判断当前没有案例。'));
      const retry = node('button', 'btn', '重新读取公开案例');
      retry.type = 'button';
      retry.addEventListener('click', () => loadPublicCases());
      state.append(retry);
      caseGrid.append(state);
    } else {
      caseGrid.append(node('div', 'n27-empty', '正在读取公开完成案例…'));
    }
    cases.append(caseGrid);
    shell.append(cases);

    const faq = list(configuration?.trustFaq);
    const faqSection = node('section', 'n27-section');
    const faqHead = node('div', 'n27-section-head');
    faqHead.append(node('h2', '', '常见问题'), node('p', '', '答案继续复用后台现有信任 FAQ。'));
    faqSection.append(faqHead);
    const faqGrid = node('div', 'n27-faq');
    faq.forEach((item) => {
      const details = document.createElement('details');
      details.append(node('summary', '', text(item?.question) || '常见问题'));
      details.append(node('p', '', text(item?.answer) || '请联系客服确认。'));
      faqGrid.append(details);
    });
    if (!faqGrid.childElementCount) faqGrid.append(node('div', 'n27-empty', '当前暂无常见问题配置。'));
    faqSection.append(faqGrid);
    shell.append(faqSection);

    const actions = node('div', 'n27-actions');
    const records = node('a', 'btn', '业务记录中心');
    records.href = '/next/records';
    const contact = node('a', 'btn gold', '一对一咨询');
    contact.href = '/next/contact';
    actions.append(records, contact);
    shell.append(actions, node('p', 'n27-note', 'N27 不复制服务规则或隐私政策正文；法律文本继续以正式 `/rules` 与 `/privacy` 为唯一来源。'));
  }

  async function loadPublicCases() {
    if (publicCasesStatus === 'loading' || publicCasesStatus === 'ready') return;
    const request = ++publicCasesRequest;
    publicCasesStatus = 'loading';
    const caseGrid = $('#n27PublicCases');
    if (caseGrid) caseGrid.replaceChildren(node('div', 'n27-empty', '正在读取公开完成案例…'));
    try {
      const response = await fetch('/api/public/cases', {
        method: 'GET',
        credentials: 'same-origin',
        cache: 'no-store',
        headers: { Accept: 'application/json' }
      });
      if (!response.ok) throw new Error('public cases unavailable');
      const body = await response.json();
      if (!Array.isArray(body)) throw new Error('public cases invalid response');
      if (request !== publicCasesRequest) return;
      publicCases = body;
      publicCasesStatus = 'ready';
    } catch (_) {
      if (request !== publicCasesRequest) return;
      publicCasesStatus = 'error';
    } finally {
      if (request === publicCasesRequest && currentPage() === 'trust') render();
    }
  }

  async function configuration() {
    const shell = window.XHFrontendShell;
    if (!shell) return null;
    try { await shell.configurationReady; } catch (_) {}
    return shell.configuration || null;
  }

  async function render() {
    const page = currentPage();
    if (!page) return;
    installStyles();
    const section = ensureSection(page);
    activate(section, page);
    const shell = $('.shell', section);
    const config = await configuration();
    if (page !== currentPage()) return;
    setMetadata(page, config);
    if (!config) {
      shell.replaceChildren(header(page), node('div', 'n27-empty', '站点配置暂时无法读取，请刷新后重试。'));
      return;
    }
    if (page === 'contact') renderContact(shell, config);
    else {
      renderTrust(shell, config);
      if (publicCasesStatus === 'idle') loadPublicCases();
    }
  }

  window.addEventListener('xh:site-config', () => render());
  window.addEventListener('popstate', () => {
    if (currentPage()) render();
  });

  window.XHFrontendNextN27 = {
    render,
    get state() { return { page: currentPage(), publicCases: [...publicCases], publicCasesStatus }; }
  };

  render();
})();
