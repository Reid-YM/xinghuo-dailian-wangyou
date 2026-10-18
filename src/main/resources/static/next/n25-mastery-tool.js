(() => {
  'use strict';

  const ROUTE = '/next/mastery';
  const LEGACY_RUNTIME = '/assets/mw4-camo-tracker.js?v=n25-20261017';
  const LEGACY_STYLE = '/assets/mw4-camo-tracker.css?v=n25-20261017';
  let mounted = false;

  const $ = (selector, root = document) => root.querySelector(selector);
  const text = (value) => String(value ?? '').trim();

  function make(tag, value, className) {
    const node = document.createElement(tag);
    if (value !== undefined && value !== null) node.textContent = String(value);
    if (className) node.className = className;
    return node;
  }

  function onRoute() {
    return /^\/next\/mastery\/?$/.test(location.pathname);
  }

  function installStyle() {
    if ($('link[data-n25-mastery-style]')) return;
    const link = document.createElement('link');
    link.rel = 'stylesheet';
    link.href = LEGACY_STYLE;
    link.dataset.n25MasteryStyle = '1';
    document.head.append(link);
  }

  function field(labelText, control) {
    const label = make('label');
    label.append(make('span', labelText), control);
    return label;
  }

  function buildPage() {
    let page = $('.page[data-page="mastery"]');
    if (page) return page;
    page = document.createElement('main');
    page.className = 'page';
    page.dataset.page = 'mastery';
    page.id = 'n25MasteryPage';

    const shell = make('div', null, 'container camo-shell');
    const crumb = make('nav', null, 'camo-crumb');
    crumb.setAttribute('aria-label', '面包屑');
    const services = make('a', '服务与工具'); services.href = '/next/services';
    crumb.append(services, make('span', '/'), make('b', 'MW4 迷彩追踪器'));

    const hero = make('section', null, 'camo-hero');
    hero.setAttribute('aria-labelledby', 'camoTitle');
    const heroCopy = make('div', null, 'camo-hero-copy');
    const title = make('h1', 'MW4 迷彩追踪器'); title.id = 'camoTitle';
    heroCopy.append(
      title,
      make('p', '按多人、战区、DMZ 逐个记录每把武器的三层大师迷彩，并保留 Pyroclast 独立系列。现有本机进度与登录后的云同步继续沿用同一份数据。'),
      make('p', 'MW4 将于 2026-10-23 上线。迷彩名称已经确认，具体解锁条件与最终层级以上线后的官方信息为准。', 'camo-hero-note')
    );
    const quote = make('a', '带着进度询价', 'btn primary');
    quote.href = '/next/quote?toolProgress=1&gameCode=CODMW4';
    quote.id = 'quoteWithProgress';
    quote.dataset.primaryCta = '';
    hero.append(heroCopy, quote);

    const progress = make('section', null, 'camo-progress-card');
    progress.setAttribute('aria-label', '总进度');
    progress.append(make('span', '你的 MW4 迷彩进度', 'eyebrow'));
    const progressRow = make('div', null, 'camo-progress-row');
    const percent = make('strong', '0%'); percent.id = 'overallPercent';
    const count = make('span', '0 / 0 项迷彩'); count.id = 'overallCount';
    progressRow.append(percent, count);
    const track = make('div', null, 'camo-overall-track'); const bar = make('i'); bar.id = 'overallBar'; track.append(bar);
    const save = make('small', '进度会自动保存在当前浏览器'); save.id = 'saveState';
    progress.append(progressRow, track, save);

    const sync = make('div', null, 'camo-sync-bar'); sync.id = 'syncBar'; sync.dataset.state = 'checking';
    sync.append(make('span', '☁', 'camo-sync-icon'));
    const syncCopy = make('p'); const syncLead = make('b', '正在检查账号状态'); syncLead.id = 'syncLead'; const syncText = make('span', '登录后可在所有设备上同步进度。'); syncText.id = 'syncText'; syncCopy.append(syncLead, syncText);
    const syncAction = make('a', '登录同步', 'camo-sync-action hidden'); syncAction.id = 'syncAction'; syncAction.href = '/next/account?next=%2Fnext%2Fmastery';
    sync.append(syncCopy, syncAction);

    const tabs = make('div', null, 'camo-tabs'); tabs.id = 'modeTabs'; tabs.setAttribute('role', 'tablist'); tabs.setAttribute('aria-label', '迷彩模式');

    const modePanel = make('section', null, 'camo-mode-panel'); modePanel.id = 'modePanel'; modePanel.setAttribute('aria-label', '模式大师迷彩');
    const modeHead = make('div', null, 'camo-mode-head');
    const modeCopy = make('div'); const modeTitle = make('span', '多人 大师迷彩', 'camo-mode-title'); modeTitle.id = 'modeHeadTitle'; const modeMeta = make('small', '0 / 0 把武器'); modeMeta.id = 'modeHeadMeta'; modeCopy.append(modeTitle, modeMeta);
    const modeFinal = make('span', '', 'camo-mode-final'); modeFinal.id = 'modeHeadFinal'; modeHead.append(modeCopy, modeFinal);
    const tiers = make('div', null, 'camo-tier-grid'); tiers.id = 'tierCards'; const modeNote = make('p', '', 'camo-tier-note'); modeNote.id = 'modeNote'; modePanel.append(modeHead, tiers, modeNote);

    const pyro = make('section', null, 'camo-pyro-panel hidden'); pyro.id = 'pyroPanel'; pyro.setAttribute('aria-label', 'Pyroclast 迷彩');
    const pyroHead = make('div', null, 'camo-mode-head'); const pyroCopy = make('div'); pyroCopy.append(make('span', 'Pyroclast 系列', 'camo-mode-title')); const pyroMeta = make('small', '0 / 0 项'); pyroMeta.id = 'pyroMeta'; pyroCopy.append(pyroMeta); pyroHead.append(pyroCopy, make('span', '独立于三个模式', 'camo-mode-final'));
    const pyroCards = make('div', null, 'camo-pyro-grid'); pyroCards.id = 'pyroCards'; const secret = make('div', null, 'camo-secret'); secret.id = 'secretCard'; pyro.append(pyroHead, pyroCards, make('p', 'Pyroclast 是独立于多人 / 战区 / DMZ 的一套迷彩，直接按项勾选即可。', 'camo-tier-note'), secret);

    const filters = make('section', null, 'camo-filters'); filters.id = 'filterBar'; filters.setAttribute('aria-label', '筛选武器');
    const classFilter = document.createElement('select'); classFilter.id = 'classFilter'; classFilter.append(new Option('全部类型', ''));
    const statusFilter = document.createElement('select'); statusFilter.id = 'statusFilter'; [['all','全部武器'],['todo','未完成'],['done','已完成'],['fav','已收藏']].forEach(([value,label]) => statusFilter.append(new Option(label, value)));
    const search = document.createElement('input'); search.id = 'weaponSearch'; search.type = 'search'; search.placeholder = '输入武器名称或代号'; search.autocomplete = 'off';
    const searchField = field('搜索武器', search); searchField.classList.add('camo-search-field');
    const actions = make('div', null, 'camo-filter-actions'); const reset = make('button', '重置当前进度', 'btn ghost'); reset.id = 'resetProgress'; reset.type = 'button'; const clear = make('button', '清空收藏', 'btn ghost'); clear.id = 'clearFavorites'; clear.type = 'button'; actions.append(reset, clear);
    filters.append(field('武器类型', classFilter), field('完成状态', statusFilter), searchField, actions);

    const hint = make('p', '点击武器展开三层迷彩与解锁条件；星标可以把常用武器置顶。', 'camo-hint'); hint.id = 'weaponHint';
    const status = make('div', '正在读取 MW4 武器资料…', 'camo-status'); status.id = 'trackerStatus'; status.setAttribute('role', 'status');
    const groups = make('section', null, 'camo-groups'); groups.id = 'weaponGroups';
    const empty = make('div', null, 'camo-empty hidden'); empty.id = 'emptyState'; empty.append(make('b', '没有符合条件的武器'), make('span', '调整搜索、类型或完成状态后再试。'));

    const note = make('section', null, 'camo-note'); const noteCopy = make('div'); noteCopy.append(make('span', '工具说明', 'eyebrow'), make('h2', '先记录剩余项目，再决定要不要把时间交给别人'), make('p', '工具只记录迷彩勾选与收藏。未登录时延续当前浏览器本机记录；登录后继续使用现有账号云同步协议，不上传游戏账号、密码或验证码。'));
    const contact = make('a', '联系客服确认服务', 'btn ghost'); contact.href = '/contact?legacy=1'; contact.dataset.n25Legacy = '1'; note.append(noteCopy, contact);

    shell.append(crumb, hero, progress, sync, tabs, modePanel, pyro, filters, hint, status, groups, empty, note);
    page.append(shell);
    const footer = $('footer');
    if (footer) footer.before(page); else document.body.append(page);
    return page;
  }

  function normalizeLegacyLinks() {
    const syncAction = $('#syncAction');
    if (syncAction && /^\/account\/login(?:\?|$)/.test(syncAction.getAttribute('href') || '')) {
      syncAction.href = '/next/account?next=%2Fnext%2Fmastery';
    }
    document.querySelectorAll('a[href="/mastery"], a[href="/mastery?legacy=1"]').forEach((link) => {
      if (link.dataset.n25Legacy === '1') return;
      link.href = ROUTE;
      delete link.dataset.n14Legacy;
      delete link.dataset.n21Legacy;
      link.dataset.n25Mastery = '1';
    });
  }

  function loadLegacyRuntime() {
    return new Promise((resolve, reject) => {
      if (window.__xhN25MasteryRuntimeLoaded) { resolve(); return; }
      const existing = $('script[data-n25-mastery-runtime]');
      if (existing) {
        existing.addEventListener('load', () => resolve(), { once: true });
        existing.addEventListener('error', () => reject(new Error('runtime-load-failed')), { once: true });
        return;
      }
      const script = document.createElement('script');
      script.src = LEGACY_RUNTIME;
      script.dataset.n25MasteryRuntime = '1';
      script.addEventListener('load', () => { window.__xhN25MasteryRuntimeLoaded = true; resolve(); }, { once: true });
      script.addEventListener('error', () => reject(new Error('runtime-load-failed')), { once: true });
      document.body.append(script);
    });
  }

  function metadata() {
    const brand = text(window.XHFrontendShell?.configuration?.brandName) || '星火网游';
    document.title = `MW4 迷彩追踪器 · ${brand}`;
    let canonical = $('link[rel="canonical"]'); if (!canonical) { canonical = document.createElement('link'); canonical.rel = 'canonical'; document.head.append(canonical); }
    canonical.href = 'https://wydlgf.cn/mastery';
    let robots = $('meta[name="robots"]'); if (!robots) { robots = document.createElement('meta'); robots.name = 'robots'; document.head.append(robots); }
    robots.content = 'noindex,follow';
  }

  async function activate() {
    normalizeLegacyLinks();
    if (!onRoute()) return false;
    installStyle();
    const page = buildPage();
    document.querySelectorAll('.page').forEach((node) => node.classList.toggle('active', node === page));
    document.documentElement.dataset.frontendPage = 'mastery';
    document.body.classList.add('camo-tracker-body');
    metadata();
    if (mounted) return true;
    mounted = true;
    try {
      await loadLegacyRuntime();
      normalizeLegacyLinks();
      return true;
    } catch (error) {
      const status = $('#trackerStatus');
      if (status) status.textContent = '工具运行时加载失败，请刷新后重试。';
      return false;
    }
  }

  const observer = new MutationObserver(() => normalizeLegacyLinks());
  observer.observe(document.body, { childList: true, subtree: true, attributes: true, attributeFilter: ['href'] });
  window.addEventListener('pagehide', () => observer.disconnect(), { once: true });

  window.XHFrontendNextN25 = { activate, normalizeLegacyLinks };
  activate();
})();
