(() => {
  'use strict';

  const KEY = 'xh_client_records_v1';
  const RECORD_RE = /^(XH[A-Z0-9]{10}|XH-[A-Z2-9]{24})$/;
  const text = value => String(value ?? '').trim();

  function normalized(value) {
    const out = text(value).toUpperCase();
    return RECORD_RE.test(out) ? out : '';
  }

  function readRows() {
    try {
      const rows = JSON.parse(localStorage.getItem(KEY) || '[]');
      return Array.isArray(rows) ? rows.filter(row => row && normalized(row.businessNo)).slice(0, 30) : [];
    } catch (_) {
      return [];
    }
  }

  function remember(businessNo, stage = null, createdAt = null) {
    const no = normalized(businessNo);
    if (!no) return false;
    const rows = readRows().filter(row => normalized(row.businessNo) !== no);
    rows.unshift({
      businessNo: no,
      stage: text(stage).toUpperCase() || null,
      createdAt: createdAt || null,
      savedAt: new Date().toISOString()
    });
    try {
      localStorage.setItem(KEY, JSON.stringify(rows.slice(0, 30)));
      return true;
    } catch (_) {
      return false;
    }
  }

  function routeBusinessNo() {
    const match = location.pathname.match(/^\/next\/record\/([^/?#]+)\/?$/);
    if (!match) return '';
    let value = match[1];
    try { value = decodeURIComponent(value); } catch (_) {}
    return normalized(value);
  }

  function rememberQuoteSuccess() {
    document.querySelectorAll('a[href^="/next/record/"]').forEach(link => {
      try {
        const url = new URL(link.getAttribute('href'), location.origin);
        const match = url.pathname.match(/^\/next\/record\/([^/?#]+)\/?$/);
        if (match) remember(decodeURIComponent(match[1]), 'QUOTE');
      } catch (_) {}
    });
  }

  function enhanceRecordDetail() {
    const businessNo = routeBusinessNo();
    if (!businessNo) return;
    const actions = document.querySelector('.n13-actions');
    const main = document.querySelector('.n13-card');
    if (!actions || !main) return;
    const stageText = text(document.querySelector('.n13-kicker')?.textContent).toUpperCase();
    remember(businessNo, stageText.includes('ORDER') ? 'ORDER' : 'QUOTE');
    if (!actions.querySelector('[data-n26-records-center]')) {
      const link = document.createElement('a');
      link.className = 'btn';
      link.href = '/next/records';
      link.dataset.n26RecordsCenter = '1';
      link.textContent = '业务记录中心';
      actions.insertBefore(link, actions.lastElementChild || null);
    }
  }

  function sync() {
    if (/^\/next\/quote\/?$/.test(location.pathname)) rememberQuoteSuccess();
    if (/^\/next\/record\//.test(location.pathname)) enhanceRecordDetail();
  }

  const observer = new MutationObserver(sync);
  observer.observe(document.documentElement, { childList: true, subtree: true });
  sync();

  window.XHFrontendNextN26RecordHistory = { remember, readRows };
})();
