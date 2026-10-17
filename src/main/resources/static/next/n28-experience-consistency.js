(() => {
  'use strict';

  if (!/^\/next(?:\/|$)/.test(location.pathname)) return;

  let observer = null;
  let scheduled = false;
  const $ = (selector, root = document) => root.querySelector(selector);

  function installStyles() {
    if ($('#frontend-next-n28-style')) return;
    const style = document.createElement('style');
    style.id = 'frontend-next-n28-style';
    style.textContent = `
      .n28-skip{position:fixed;left:14px;top:10px;z-index:9999;transform:translateY(-160%);padding:10px 14px;border-radius:9px;background:var(--gold,#ffcb2d);color:#111;font-size:12px;font-weight:900;box-shadow:0 8px 28px #0008;transition:transform .16s ease}.n28-skip:focus{transform:translateY(0);outline:3px solid #fff;outline-offset:2px}
      :where(.page.active,[data-page].active) :where(a,button,input,select,textarea,[tabindex]):focus-visible{outline:2px solid var(--gold,#ffcb2d);outline-offset:3px;box-shadow:0 0 0 5px #ffcb2d18}
      :where(.page.active,[data-page].active) :where(button,.btn)[disabled],:where(.page.active,[data-page].active) :where(button,.btn)[aria-disabled="true"]{opacity:.48!important;cursor:not-allowed!important;transform:none!important;filter:saturate(.5)}
      :where(.page.active,[data-page].active) :where(input,select,textarea)[disabled]{opacity:.58;cursor:not-allowed}
      :where(.n23-empty,.n24-empty,.camo-empty,.n26-empty,.n4-empty){min-height:88px;display:grid;place-items:center;align-content:center;gap:6px;line-height:1.65}
      :where(#n23GuideStatus,#n24Message,#trackerStatus,#n26RecordMessage)[aria-busy="true"]{position:relative;padding-left:18px}
      :where(#n23GuideStatus,#n24Message,#trackerStatus,#n26RecordMessage)[aria-busy="true"]:before{content:"";position:absolute;left:0;top:50%;width:9px;height:9px;margin-top:-5px;border:2px solid #ffffff2a;border-top-color:var(--gold,#ffcb2d);border-radius:50%;animation:n28-spin .75s linear infinite}
      .n28-runtime-note{position:fixed;right:14px;bottom:14px;z-index:80;max-width:min(360px,calc(100vw - 28px));padding:9px 12px;border:1px solid #ffffff14;border-radius:10px;background:#090d11e8;color:#9ba3ac;font-size:9px;line-height:1.5;box-shadow:0 12px 34px #0008;backdrop-filter:blur(10px)}.n28-runtime-note[hidden]{display:none}
      @keyframes n28-spin{to{transform:rotate(360deg)}}
      @media(max-width:680px){:where(.page.active,[data-page].active) :where(button,.btn,select,input[type="button"],input[type="submit"]){min-height:44px}:where(.page.active,[data-page].active) :where(input:not([type="checkbox"]):not([type="radio"]),select,textarea){font-size:16px}.n28-runtime-note{left:13px;right:13px;bottom:74px;max-width:none}}
      @media(prefers-reduced-motion:reduce){html:focus-within{scroll-behavior:auto!important}*,*:before,*:after{scroll-behavior:auto!important;animation-duration:.01ms!important;animation-iteration-count:1!important;transition-duration:.01ms!important}.n28-skip{transition:none}}
      @media(forced-colors:active){.n28-skip{border:2px solid ButtonText}:where(.page.active,[data-page].active) :focus-visible{outline:3px solid Highlight}}
    `;
    document.head.append(style);
  }

  function activePage() {
    return document.querySelector('.page.active') || document.querySelector('main.page') || document.querySelector('main');
  }

  function ensureMainTarget() {
    const main = activePage();
    if (!main) return '';
    if (!main.id) main.id = 'frontend-next-main';
    if (!main.hasAttribute('tabindex')) main.tabIndex = -1;
    return main.id;
  }

  function ensureSkipLink() {
    let link = $('.n28-skip');
    if (!link) {
      link = document.createElement('a');
      link.className = 'n28-skip';
      link.textContent = '跳到主要内容';
      document.body.prepend(link);
    }
    const target = ensureMainTarget();
    if (target) link.href = `#${target}`;
  }

  function classifyStatus(node) {
    if (!node) return;
    if (!node.hasAttribute('aria-live')) node.setAttribute('aria-live', 'polite');
    const value = String(node.textContent || '').trim();
    const busy = /正在|读取中|提交中|加载中|查询中|同步中/.test(value);
    node.setAttribute('aria-busy', busy ? 'true' : 'false');
  }

  function normalizeStatusRegions() {
    ['#n23GuideStatus', '#n24Message', '#trackerStatus', '#n26RecordMessage', '#n4LoginMessage', '#n7RegisterMessage', '#n7ResetMessage']
      .forEach((selector) => classifyStatus($(selector)));
  }

  function normalizeImages(root = document) {
    root.querySelectorAll?.('img:not([alt])').forEach((image) => image.setAttribute('alt', ''));
    root.querySelectorAll?.('img[loading="lazy"]').forEach((image) => image.setAttribute('decoding', 'async'));
  }

  function normalizeExternalLinks(root = document) {
    root.querySelectorAll?.('a[target="_blank"]').forEach((link) => {
      const rel = new Set(String(link.rel || '').split(/\s+/).filter(Boolean));
      rel.add('noopener');
      rel.add('noreferrer');
      link.rel = [...rel].join(' ');
    });
  }

  function normalizeButtons(root = document) {
    root.querySelectorAll?.('button:not([type])').forEach((button) => {
      if (!button.closest('form')) button.type = 'button';
    });
    root.querySelectorAll?.('button[disabled]').forEach((button) => button.setAttribute('aria-disabled', 'true'));
    root.querySelectorAll?.('button:not([disabled])[aria-disabled="true"]').forEach((button) => button.removeAttribute('aria-disabled'));
  }

  function announceRoute() {
    let note = $('#n28RuntimeNote');
    if (!note) {
      note = document.createElement('div');
      note.id = 'n28RuntimeNote';
      note.className = 'n28-runtime-note';
      note.setAttribute('role', 'status');
      note.setAttribute('aria-live', 'polite');
      note.hidden = true;
      document.body.append(note);
    }
    note.textContent = `已进入 ${document.title || 'Frontend Next'}`;
    note.hidden = false;
    window.clearTimeout(announceRoute.timer);
    announceRoute.timer = window.setTimeout(() => { note.hidden = true; }, 1800);
  }

  function apply(root = document) {
    installStyles();
    ensureSkipLink();
    normalizeStatusRegions();
    normalizeImages(root);
    normalizeExternalLinks(root);
    normalizeButtons(root);
    document.documentElement.dataset.n28Experience = 'ready';
  }

  function schedule(root = document) {
    if (scheduled) return;
    scheduled = true;
    window.requestAnimationFrame(() => {
      scheduled = false;
      apply(root);
    });
  }

  observer = new MutationObserver((records) => {
    let root = document;
    for (const record of records) {
      if (record.target instanceof Element) root = record.target;
    }
    schedule(root);
  });
  observer.observe(document.body, { childList: true, subtree: true, attributes: true, attributeFilter: ['disabled'] });

  window.addEventListener('xh:pagechange', () => { schedule(); announceRoute(); });
  window.addEventListener('popstate', () => { schedule(); announceRoute(); });
  window.addEventListener('pagehide', () => observer.disconnect(), { once: true });

  apply();
  window.XHFrontendNextN28 = { apply, activePage, ensureSkipLink };
})();
