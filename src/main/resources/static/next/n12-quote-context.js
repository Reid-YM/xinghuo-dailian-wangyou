(() => {
  'use strict';
  const runtimePath = pathname => window.XHFrontendShell?.runtimePath?.(pathname) || pathname || location.pathname;
  const navigation = window.XHFrontendShell?.navigation || history;

  const QUOTE_PATH = '/next/quote';
  const $ = (selector, root = document) => root.querySelector(selector);
  const text = value => String(value ?? '').trim();
  const code = value => text(value).toUpperCase();
  const list = value => Array.isArray(value) ? value : [];
  let applyingUrlSelection = false;
  let optionObserver = null;

  function make(tag, className, value) {
    const node = document.createElement(tag);
    if (className) node.className = className;
    if (value !== undefined && value !== null) node.textContent = String(value);
    return node;
  }

  function services() {
    return list(window.XHFrontendNextN2?.state?.services);
  }

  function catalog() {
    return window.XHFrontendNextN2?.state?.catalog || null;
  }

  function gameName(gameCode) {
    const row = list(catalog()?.games).find(item => code(item?.code) === code(gameCode));
    return text(row?.name) || text(gameCode) || '游戏服务';
  }

  function categoryName(categoryCode, gameCode) {
    const rows = list(catalog()?.categories);
    const row = rows.find(item => code(item?.code) === code(categoryCode) && code(item?.gameCode) === code(gameCode))
      || rows.find(item => code(item?.code) === code(categoryCode));
    return text(row?.name) || text(categoryCode) || '服务项目';
  }

  function money(value) {
    if (value == null || value === '' || !Number.isFinite(Number(value)) || Number(value) < 0) return null;
    return Number(value).toLocaleString('zh-CN', { maximumFractionDigits: 2 });
  }

  function price(service) {
    const mode = code(service?.priceMode);
    const fixed = money(service?.priceFixed);
    const min = money(service?.priceMin);
    const max = money(service?.priceMax);
    if (mode === 'FIXED' && fixed !== null) return `¥${fixed}`;
    if (mode === 'RANGE' && min !== null && max !== null && min !== max) return `¥${min}–${max}`;
    if (mode === 'RANGE' && min !== null) return `¥${min} 起`;
    if (mode === 'RANGE' && max !== null) return `¥${max} 内`;
    return '联系客服询价';
  }

  function findService(value) {
    const target = text(value);
    if (!target) return null;
    return services().find(item => text(item?.slug) === target)
      || services().find(item => text(item?.id) === target)
      || services().find(item => text(item?.name) === target)
      || null;
  }

  function selectedService() {
    return findService($('#qSvc')?.value);
  }

  function installStyles() {
    if ($('#frontend-next-n12-style')) return;
    const style = document.createElement('style');
    style.id = 'frontend-next-n12-style';
    style.textContent = `
      .n12-context{margin-bottom:12px;border:1px solid #ffcb2d42;border-radius:15px;background:linear-gradient(135deg,#ffcb2d0d,#0d1116 58%);padding:16px 18px;display:grid;grid-template-columns:minmax(0,1fr) auto;gap:16px;align-items:center}
      .n12-context[hidden]{display:none!important}.n12-context small{display:block;color:#d5b85f;font-size:9px;font-weight:900;letter-spacing:.14em;text-transform:uppercase;margin-bottom:6px}.n12-context h3{margin:0;font-size:18px}.n12-context p{margin:6px 0 0;color:#929aa4;font-size:11px;line-height:1.65}.n12-context-meta{display:flex;gap:7px;flex-wrap:wrap;margin-top:9px}.n12-context-meta span{border:1px solid #ffffff14;border-radius:999px;padding:5px 8px;color:#b7bec6;font-size:9px;background:#ffffff05}.n12-context-actions{display:flex;gap:7px;flex-wrap:wrap;justify-content:flex-end}.n12-context-actions .btn{font-size:10px;white-space:nowrap}.n12-context-empty{border-color:#ffffff18;background:#0d1116}.n12-context-warning{border-color:#ff9d2e55;background:#ff9d2e0a}
      @media(max-width:760px){.n12-context{grid-template-columns:1fr}.n12-context-actions{justify-content:flex-start}.n12-context-actions .btn{flex:1;text-align:center}}
    `;
    document.head.append(style);
  }

  function ensureContext() {
    let context = $('#n12QuoteContext');
    if (context) return context;
    const quoteMain = $('.page[data-page="quote"] .quote main');
    const panel = quoteMain?.querySelector('.panel');
    if (!quoteMain || !panel) return null;
    context = make('section', 'n12-context n12-context-empty');
    context.id = 'n12QuoteContext';
    context.setAttribute('aria-live', 'polite');
    panel.before(context);
    return context;
  }

  function actionLink(label, href, className = 'btn') {
    const link = make('a', className, label);
    link.href = href;
    return link;
  }

  function renderUnavailable(slug) {
    const context = ensureContext();
    if (!context) return;
    context.className = 'n12-context n12-context-warning';
    const copy = make('div');
    copy.append(make('small', '', 'Quote Context'), make('h3', '', '链接中的服务当前不可用'));
    copy.append(make('p', '', slug ? `服务标识 ${slug} 当前不在公开目录中，可能已下架或链接已更新。你仍可重新选择其他服务。` : '请从当前公开目录中选择服务。'));
    const actions = make('div', 'n12-context-actions');
    actions.append(actionLink('浏览服务大厅', '/next/services'));
    context.replaceChildren(copy, actions);
  }

  function render() {
    const context = ensureContext();
    if (!context) return;
    const service = selectedService();
    const requested = new URLSearchParams(location.search).get('service');
    if (!service) {
      if (requested && runtimePath() === QUOTE_PATH && services().length) {
        renderUnavailable(requested);
        return;
      }
      context.className = 'n12-context n12-context-empty';
      const copy = make('div');
      copy.append(make('small', '', 'Quote Context'), make('h3', '', '选择服务后继续填写需求'));
      copy.append(make('p', '', '服务选择会同步到当前预览 URL，便于返回详情核对，也方便刷新后继续确认同一服务。'));
      const actions = make('div', 'n12-context-actions');
      actions.append(actionLink('浏览服务大厅', '/next/services'));
      context.replaceChildren(copy, actions);
      return;
    }

    const slug = text(service.slug);
    const copy = make('div');
    copy.append(make('small', '', 'Current Service'), make('h3', '', text(service.name) || slug || '已选服务'));
    copy.append(make('p', '', text(service.shortDesc) || '当前询价已绑定此公开服务；需求、平台和时间仍以本次提交内容为准。'));
    const meta = make('div', 'n12-context-meta');
    meta.append(
      make('span', '', gameName(service.gameCode)),
      make('span', '', categoryName(service.category, service.gameCode)),
      make('span', '', price(service))
    );
    if (text(service.durationText)) meta.append(make('span', '', text(service.durationText)));
    copy.append(meta);

    const actions = make('div', 'n12-context-actions');
    if (slug) actions.append(actionLink('返回服务详情', `/next/service/${encodeURIComponent(slug)}`, 'btn gold'));
    const reselect = make('button', 'btn', '重新选择');
    reselect.type = 'button';
    reselect.addEventListener('click', () => {
      const select = $('#qSvc');
      select?.scrollIntoView({ behavior: 'smooth', block: 'center' });
      select?.focus({ preventScroll: true });
    });
    actions.append(reselect);
    context.className = 'n12-context';
    context.replaceChildren(copy, actions);
  }

  function readyForUrlSync() {
    const select = $('#qSvc');
    return runtimePath() === QUOTE_PATH && Array.isArray(window.XHFrontendNextN2?.state?.services)
      && !!select && !select.disabled && select.options.length > 0;
  }

  function syncUrlFromSelection() {
    if (applyingUrlSelection || !readyForUrlSync()) return;
    const service = selectedService();
    const url = new URL(location.href);
    const current = url.searchParams.get('service') || '';
    const next = text(service?.slug);
    if (next) url.searchParams.set('service', next); else url.searchParams.delete('service');
    if (current === next) return;
    navigation.replaceState(navigation.state, '', url.pathname + url.search + url.hash);
  }

  function selectFromUrl() {
    if (runtimePath() !== QUOTE_PATH) return false;
    const slug = new URLSearchParams(location.search).get('service');
    if (!slug || !services().length) return false;
    const service = services().find(item => text(item?.slug) === slug);
    if (!service) {
      renderUnavailable(slug);
      return false;
    }
    const game = $('#qGame');
    const category = $('#qCat');
    const serviceSelect = $('#qSvc');
    if (!game || !category || !serviceSelect || game.disabled) return false;
    applyingUrlSelection = true;
    try {
      if ([...game.options].some(option => code(option.value) === code(service.gameCode))) {
        game.value = text(service.gameCode);
        game.dispatchEvent(new Event('change', { bubbles: true }));
      }
      if ([...category.options].some(option => code(option.value) === code(service.category))) {
        category.value = text(service.category);
        category.dispatchEvent(new Event('change', { bubbles: true }));
      }
      const value = text(service.slug || service.id || service.name);
      if ([...serviceSelect.options].some(option => option.value === value)) serviceSelect.value = value;
    } finally {
      applyingUrlSelection = false;
    }
    render();
    return true;
  }

  function refreshContext({ syncUrl = true } = {}) {
    if (runtimePath() !== QUOTE_PATH && document.documentElement.dataset.frontendPage !== 'quote') return;
    render();
    if (syncUrl) syncUrlFromSelection();
  }

  function watchOptions() {
    const serviceSelect = $('#qSvc');
    if (!serviceSelect || optionObserver) return;
    optionObserver = new MutationObserver(() => {
      queueMicrotask(() => {
        if (runtimePath() === QUOTE_PATH) {
          const requested = new URLSearchParams(location.search).get('service');
          if (requested && text(selectedService()?.slug) !== requested) selectFromUrl();
        }
        refreshContext();
      });
    });
    optionObserver.observe(serviceSelect, { childList: true });
  }

  function boot() {
    installStyles();
    ensureContext();
    watchOptions();
    $('#qSvc')?.addEventListener('change', () => refreshContext());
    $('#qGame')?.addEventListener('change', () => queueMicrotask(() => refreshContext()));
    $('#qCat')?.addEventListener('change', () => queueMicrotask(() => refreshContext()));
    window.addEventListener('xh:pagechange', event => {
      if (event.detail?.page !== 'quote') return;
      queueMicrotask(() => {
        selectFromUrl();
        refreshContext();
      });
    });
    window.addEventListener('popstate', () => queueMicrotask(() => {
      selectFromUrl();
      refreshContext({ syncUrl: false });
    }));
    queueMicrotask(() => {
      selectFromUrl();
      refreshContext();
    });
  }

  window.XHFrontendNextN12 = {
    render,
    selectFromUrl,
    selectedService,
    syncUrlFromSelection
  };

  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', boot, { once: true });
  else boot();
})();
