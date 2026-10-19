(function initializeAccount() {
  'use strict';
  const runtimePath = pathname => window.XHFrontendShell?.runtimePath?.(pathname) || pathname || location.pathname;
  const runtimeHref = value => window.XHFrontendShell?.runtimeHref?.(value) || value;
  const navigation = window.XHFrontendShell?.navigation || history;

  if (!/^\/next\/account(?:\/|$)/.test(runtimePath()) && !document.querySelector('.page[data-page="account"].active')) {
    window.addEventListener('xh:pagechange', initializeAccount, { once: true });
    return;
  }

  const BUSINESS_NO_RE = /^(XH[A-Z0-9]{10}|XH-[A-Z2-9]{24})$/;
  const SORTS = new Set(['source', 'updated-desc', 'updated-asc']);
  const STATUS_RE = /^[A-Z][A-Z0-9_]{0,31}$/;
  let observer = null;
  let mountTimer = null;

  function text(value) {
    return String(value ?? '').trim();
  }

  function normalizeBusinessNo(value) {
    let normalized = text(value).toUpperCase();
    try { normalized = decodeURIComponent(normalized).toUpperCase(); } catch (ignore) { return ''; }
    return BUSINESS_NO_RE.test(normalized) ? normalized : '';
  }

  function routeInfo(pathname = runtimePath()) {
    pathname = runtimePath(pathname);
    const match = /^\/next\/account\/(requests|orders)(?:\/([^/?#]+))?\/?$/.exec(pathname);
    if (!match) return null;
    return {
      section: match[1],
      kind: match[1] === 'requests' ? 'quote' : 'order',
      detailBusinessNo: match[2] ? normalizeBusinessNo(match[2]) : ''
    };
  }

  function sourceItems(info) {
    const state = window.XHFrontendNextN5?.state;
    if (!state || !info) return [];
    const source = info.kind === 'quote' ? state.quotes?.items : state.orders?.items;
    return Array.isArray(source) ? source : [];
  }

  function listLoaded(info) {
    const state = window.XHFrontendNextN5?.state;
    return !!state && (info?.kind === 'quote' ? state.quotes != null : state.orders != null);
  }

  function itemBusinessNo(info, item) {
    if (!info || !item) return '';
    return normalizeBusinessNo(info.kind === 'quote' ? item.businessNo : (item.businessNo || item.orderNo));
  }

  function itemStatus(item) {
    const value = text(item?.status).toUpperCase();
    return STATUS_RE.test(value) ? value : '';
  }

  function itemUpdatedAt(item) {
    const raw = item?.updatedAt || item?.createdAt;
    const value = new Date(raw || 0).getTime();
    return Number.isFinite(value) ? value : 0;
  }

  function itemSearchText(info, item) {
    const values = info.kind === 'quote'
      ? [item?.businessNo, item?.game, item?.demand, item?.status, item?.statusText, item?.currentProgress, item?.platform, item?.expectedTime]
      : [item?.businessNo, item?.orderNo, item?.gameName, item?.serviceName, item?.status, item?.statusText, item?.progressText];
    return values.map((value) => text(value).toLocaleLowerCase('zh-CN')).join('\n');
  }

  function rawViewState(source = location.href) {
    const url = new URL(source, location.origin);
    const q = text(url.searchParams.get('q')).slice(0, 60);
    const rawStatus = text(url.searchParams.get('status')).toUpperCase();
    const status = STATUS_RE.test(rawStatus) ? rawStatus : '';
    const rawSort = text(url.searchParams.get('sort')).toLowerCase();
    const sort = SORTS.has(rawSort) ? rawSort : 'source';
    return { q, status, sort };
  }

  function viewState(info, source = location.href) {
    const view = rawViewState(source);
    const items = sourceItems(info);
    if (view.status && listLoaded(info)) {
      const statuses = new Set(items.map(itemStatus).filter(Boolean));
      if (!statuses.has(view.status)) view.status = '';
    }
    return view;
  }

  function normalizedViewUrl(info, source = location.href) {
    const url = new URL(source, location.origin);
    const view = viewState(info, source);
    if (view.q) url.searchParams.set('q', view.q); else url.searchParams.delete('q');
    if (view.status) url.searchParams.set('status', view.status); else url.searchParams.delete('status');
    if (view.sort !== 'source') url.searchParams.set('sort', view.sort); else url.searchParams.delete('sort');
    return runtimeHref(url.pathname + url.search + url.hash);
  }

  function filteredItems(info, source = location.href) {
    const view = viewState(info, source);
    const q = view.q.toLocaleLowerCase('zh-CN');
    const items = sourceItems(info).filter((item) => {
      if (view.status && itemStatus(item) !== view.status) return false;
      if (q && !itemSearchText(info, item).includes(q)) return false;
      return true;
    });
    if (view.sort === 'updated-desc') items.sort((a, b) => itemUpdatedAt(b) - itemUpdatedAt(a));
    if (view.sort === 'updated-asc') items.sort((a, b) => itemUpdatedAt(a) - itemUpdatedAt(b));
    return items;
  }

  function visibleBusinessNos(info = routeInfo(), source = location.href) {
    if (!info) return null;
    return filteredItems(info, source).map((item) => itemBusinessNo(info, item)).filter(Boolean);
  }

  function appendViewState(target, source = location.href) {
    const url = target instanceof URL ? new URL(target.href) : new URL(target, location.origin);
    const view = rawViewState(source);
    if (view.q) url.searchParams.set('q', view.q); else url.searchParams.delete('q');
    if (view.status) url.searchParams.set('status', view.status); else url.searchParams.delete('status');
    if (view.sort !== 'source') url.searchParams.set('sort', view.sort); else url.searchParams.delete('sort');
    return runtimeHref(url.pathname + url.search + url.hash);
  }

  function writeViewState(info, patch) {
    if (!info) return false;
    const current = viewState(info);
    const next = {
      q: patch.q !== undefined ? text(patch.q).slice(0, 60) : current.q,
      status: patch.status !== undefined ? text(patch.status).toUpperCase() : current.status,
      sort: patch.sort !== undefined && SORTS.has(text(patch.sort).toLowerCase()) ? text(patch.sort).toLowerCase() : current.sort
    };
    if (!STATUS_RE.test(next.status)) next.status = '';
    const url = new URL(location.href);
    if (next.q) url.searchParams.set('q', next.q); else url.searchParams.delete('q');
    if (next.status) url.searchParams.set('status', next.status); else url.searchParams.delete('status');
    if (next.sort !== 'source') url.searchParams.set('sort', next.sort); else url.searchParams.delete('sort');
    const target = normalizedViewUrl(info, url.href);
    const currentUrl = location.pathname + location.search + location.hash;
    if (target !== currentUrl) navigation.replaceState({ ...(navigation.state || {}), n20AccountListView: true }, '', target);
    apply(info);
    return true;
  }

  function statusOptions(info) {
    const options = new Map();
    sourceItems(info).forEach((item) => {
      const value = itemStatus(item);
      if (!value || options.has(value)) return;
      options.set(value, text(item?.statusText) || value);
    });
    return [...options.entries()];
  }

  function statusSignature(info) {
    return statusOptions(info).map(([value, label]) => `${value}:${label}`).join('|');
  }

  function installStyles() {
    if (document.querySelector('#frontend-next-n20-style')) return;
    const style = document.createElement('style');
    style.id = 'frontend-next-n20-style';
    style.textContent = `
      .n20-list-view{display:grid;grid-template-columns:minmax(180px,1fr) minmax(140px,.45fr) minmax(140px,.45fr) auto;gap:8px;align-items:end;margin:0 0 12px;padding:11px;border:1px solid #ffffff10;border-radius:11px;background:#090d11}
      .n20-field{display:grid;gap:5px;min-width:0}.n20-field>span{font-size:8px;color:#727c86}.n20-field input,.n20-field select{width:100%;min-width:0;border:1px solid #ffffff18;background:#0b1015;color:#dce1e6;border-radius:8px;padding:8px 9px;font:inherit;font-size:9px}.n20-field input:focus,.n20-field select:focus{outline:none;border-color:#ffcb2d66;box-shadow:0 0 0 3px #ffcb2d0f}
      .n20-reset{appearance:none;border:1px solid #ffffff18;background:#10161d;color:#d9dee3;border-radius:8px;padding:9px 11px;font:inherit;font-size:9px;cursor:pointer}.n20-reset:disabled{opacity:.4;cursor:not-allowed}.n20-count{grid-column:1/-1;color:#727c86;font-size:8px;line-height:1.45}.n20-empty{margin-top:10px;padding:18px 12px;border:1px dashed #ffffff18;border-radius:10px;text-align:center;color:#78818b;font-size:9px;line-height:1.55}
      @media(max-width:760px){.n20-list-view{grid-template-columns:1fr 1fr}.n20-search{grid-column:1/-1}.n20-reset{width:100%}}@media(max-width:520px){.n20-list-view{grid-template-columns:1fr}.n20-search,.n20-count{grid-column:1}}
    `;
    document.head.append(style);
  }

  function field(label, control, extraClass = '') {
    const wrap = document.createElement('label');
    wrap.className = `n20-field ${extraClass}`.trim();
    const title = document.createElement('span');
    title.textContent = label;
    wrap.append(title, control);
    return wrap;
  }

  function buildToolbar(info) {
    const view = viewState(info);
    const toolbar = document.createElement('div');
    toolbar.id = 'n20ListView';
    toolbar.className = 'n20-list-view';
    toolbar.dataset.n20Section = info.section;
    toolbar.dataset.n20StatusSignature = statusSignature(info);

    const search = document.createElement('input');
    search.id = 'n20Search';
    search.type = 'search';
    search.maxLength = 60;
    search.autocomplete = 'off';
    search.placeholder = info.kind === 'quote' ? '业务编号 / 游戏 / 需求' : '业务编号 / 游戏 / 服务';
    search.value = view.q;
    search.addEventListener('input', () => writeViewState(info, { q: search.value }));

    const status = document.createElement('select');
    status.id = 'n20Status';
    const all = document.createElement('option');
    all.value = '';
    all.textContent = '全部状态';
    status.append(all);
    statusOptions(info).forEach(([value, label]) => {
      const option = document.createElement('option');
      option.value = value;
      option.textContent = label;
      status.append(option);
    });
    status.value = view.status;
    status.addEventListener('change', () => writeViewState(info, { status: status.value }));

    const sort = document.createElement('select');
    sort.id = 'n20Sort';
    [['source', '默认顺序'], ['updated-desc', '最近更新优先'], ['updated-asc', '较早更新优先']].forEach(([value, label]) => {
      const option = document.createElement('option'); option.value = value; option.textContent = label; sort.append(option);
    });
    sort.value = view.sort;
    sort.addEventListener('change', () => writeViewState(info, { sort: sort.value }));

    const reset = document.createElement('button');
    reset.id = 'n20Reset';
    reset.type = 'button';
    reset.className = 'n20-reset';
    reset.textContent = '清除筛选';
    reset.disabled = !view.q && !view.status && view.sort === 'source';
    reset.addEventListener('click', () => writeViewState(info, { q: '', status: '', sort: 'source' }));

    const count = document.createElement('div');
    count.id = 'n20Count';
    count.className = 'n20-count';
    count.setAttribute('aria-live', 'polite');

    toolbar.append(field('搜索已加载记录', search, 'n20-search'), field('状态', status), field('排序', sort), reset, count);
    return toolbar;
  }

  function rowBusinessNo(row) {
    return normalizeBusinessNo(row?.querySelector('td')?.textContent);
  }

  function applyRows(info) {
    const body = document.querySelector('#n5RecordsBody');
    if (!body) return false;
    const tbody = body.querySelector('tbody');
    const ids = visibleBusinessNos(info) || [];
    const visible = new Set(ids);
    const total = sourceItems(info).length;
    const count = document.querySelector('#n20Count');
    const countText = `当前已加载 ${total} 条 · 筛选后显示 ${ids.length} 条${total >= 50 ? ' · 最多显示最近 50 条' : ''}`;
    if (count && count.textContent !== countText) count.textContent = countText;

    const empty = body.querySelector('#n20Empty');
    if (!tbody) {
      empty?.remove();
      return true;
    }

    const rows = [...tbody.querySelectorAll('tr')];
    const byId = new Map(rows.map((row) => [rowBusinessNo(row), row]).filter(([id]) => id));
    rows.forEach((row) => { row.hidden = !visible.has(rowBusinessNo(row)); });

    const currentVisibleOrder = rows.filter((row) => !row.hidden).map(rowBusinessNo);
    if (currentVisibleOrder.join('|') !== ids.join('|')) {
      ids.forEach((id) => {
        const row = byId.get(id);
        if (row) tbody.append(row);
      });
    }

    if (!ids.length && total) {
      const notice = empty || document.createElement('div');
      notice.id = 'n20Empty';
      notice.className = 'n20-empty';
      const emptyText = '当前筛选条件没有匹配记录。清除筛选后可恢复已加载列表。';
      if (notice.textContent !== emptyText) notice.textContent = emptyText;
      if (!empty) body.append(notice);
    } else {
      empty?.remove();
    }
    return true;
  }

  function patchRecordLink() {
    const links = [...document.querySelectorAll('#n19DetailContext a[href]')];
    links.forEach((link) => {
      let url;
      try { url = new URL(link.href, location.origin); } catch (ignore) { return; }
      if (url.origin !== location.origin || !/^\/next\/record\//.test(runtimePath(url.pathname))) return;
      const target = appendViewState(url);
      if (link.getAttribute('href') !== target) link.setAttribute('href', target);
      link.dataset.n20ViewState = '1';
    });
  }

  function mountToolbar(info) {
    const body = document.querySelector('#n5RecordsBody');
    if (!body || !info) return false;
    let toolbar = document.querySelector('#n20ListView');
    const expected = viewState(info);
    const nextStatusSignature = statusSignature(info);
    if (!toolbar || toolbar.dataset.n20Section !== info.section || toolbar.dataset.n20StatusSignature !== nextStatusSignature) {
      toolbar?.remove();
      toolbar = buildToolbar(info);
      body.before(toolbar);
    } else {
      const search = toolbar.querySelector('#n20Search');
      const status = toolbar.querySelector('#n20Status');
      const sort = toolbar.querySelector('#n20Sort');
      if (search && search.value !== expected.q && document.activeElement !== search) search.value = expected.q;
      if (status && status.value !== expected.status) status.value = expected.status;
      if (sort && sort.value !== expected.sort) sort.value = expected.sort;
      const reset = toolbar.querySelector('#n20Reset');
      if (reset) reset.disabled = !expected.q && !expected.status && expected.sort === 'source';
    }
    return true;
  }

  function apply(info = routeInfo()) {
    if (!info) {
      document.querySelector('#n20ListView')?.remove();
      return false;
    }
    if (!document.querySelector('#n5BusinessCenter')) return false;
    installStyles();

    const normalized = normalizedViewUrl(info);
    const current = location.pathname + location.search + location.hash;
    if (normalized !== current) navigation.replaceState(navigation.state, '', normalized);

    mountToolbar(info);
    applyRows(info);
    patchRecordLink();
    window.XHFrontendNextN19?.mount?.();
    return true;
  }

  function scheduleApply() {
    if (mountTimer) clearTimeout(mountTimer);
    mountTimer = setTimeout(() => apply(), 0);
  }

  window.addEventListener('popstate', scheduleApply);
  window.addEventListener('xh:pagechange', (event) => {
    if (event.detail?.page === 'account') scheduleApply();
  });

  observer = new MutationObserver(scheduleApply);
  observer.observe(document.body, { childList: true, subtree: true });
  window.addEventListener('pageshow', event => {
    if (event.persisted) {
      observer?.observe(document.body, { childList: true, subtree: true });
      scheduleApply();
    }
  });
  window.addEventListener('pagehide', () => {
    observer?.disconnect();
    if (mountTimer) clearTimeout(mountTimer);
  });

  window.XHFrontendNextN20 = {
    routeInfo,
    viewState,
    visibleBusinessNos,
    appendViewState,
    apply
  };

  apply();
})();
