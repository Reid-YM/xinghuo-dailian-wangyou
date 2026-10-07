(() => {
  'use strict';
  if (window.__xinghuoAdminControlCenterV2) return;
  window.__xinghuoAdminControlCenterV2 = true;

  const $ = (selector, root = document) => root.querySelector(selector);
  const $$ = (selector, root = document) => [...root.querySelectorAll(selector)];

  function ensureStylesheet() {
    if ($('link[data-admin-control-center-v2]')) return;
    const link = document.createElement('link');
    link.rel = 'stylesheet';
    link.href = '/assets/admin-control-center-v2.css?v=20261007a002';
    link.dataset.adminControlCenterV2 = '1';
    document.head.appendChild(link);
  }

  function ensureFrontendControlEntry(sidebar) {
    const nav = $('.admin-workspace-nav', sidebar || document);
    if (!nav || nav.querySelector('a[href="/admin/frontend-controls"]')) return;

    const siteToggle = nav.querySelector('[data-nav-group="site"]');
    const siteSection = siteToggle?.closest('.nav-section');
    const items = siteSection?.querySelector('.nav-section-items');
    if (!items) return;

    const link = document.createElement('a');
    link.className = 'btn nav-item nav-level-2';
    link.href = '/admin/frontend-controls';
    link.innerHTML = '<span>前端控制中心</span><small>模块显隐 / 文案 / 主题</small>';
    items.appendChild(link);
  }

  function createChrome() {
    // V2 使用独立后台主题；移除旧版 admin-solid-ui，避免两套主题的 !important 规则互相覆盖。
    document.body.classList.remove('admin-solid-ui');
    document.body.classList.add('admin-control-center-v2');
    const main = $('.admin-main');
    const sidebar = $('.sidebar');
    if (!main || !sidebar) return;

    if (!$('#adminV2Topbar')) {
      const topbar = document.createElement('div');
      topbar.id = 'adminV2Topbar';
      topbar.className = 'admin-v2-topbar';
      topbar.innerHTML = `
        <div class="admin-v2-context">
          <button class="btn ghost admin-v2-menu" id="adminV2MenuButton" type="button" aria-label="打开后台导航">菜单</button>
          <span class="admin-v2-context-mark">X</span>
          <div><small>星火后台控制中心</small><strong id="adminV2CurrentTitle">运营总览</strong></div>
        </div>
        <div class="admin-v2-actions">
          <button class="btn ghost admin-v2-dashboard-shortcut" type="button" data-nav="dashboard">回到总览</button>
          <a class="btn ghost" href="/admin/frontend-controls">前端控制</a>
          <a class="btn ghost" href="/" target="_blank" rel="noopener">查看前台</a>
        </div>`;
      main.prepend(topbar);
    }

    if (!$('#adminV2Backdrop')) {
      const backdrop = document.createElement('button');
      backdrop.id = 'adminV2Backdrop';
      backdrop.className = 'admin-v2-backdrop';
      backdrop.type = 'button';
      backdrop.setAttribute('aria-label', '关闭后台导航');
      document.body.appendChild(backdrop);
    }

    if (!$('#adminV2NavSearch')) {
      const brand = $('.admin-brand', sidebar);
      const wrapper = document.createElement('label');
      wrapper.className = 'admin-v2-nav-search';
      wrapper.innerHTML = '<span>⌕</span><input id="adminV2NavSearch" type="search" autocomplete="off" placeholder="搜索后台功能" aria-label="搜索后台功能"><div class="admin-v2-nav-search-empty" id="adminV2SearchEmpty">没有匹配的后台功能</div>';
      if (brand?.nextSibling) brand.parentNode.insertBefore(wrapper, brand.nextSibling);
      else sidebar.prepend(wrapper);
    }

    ensureFrontendControlEntry(sidebar);
    $$('.admin-main>[data-panel]').forEach(panel => panel.classList.add('admin-v2-panel'));
  }

  function panelTitle(panel) {
    if (!panel) return '后台控制中心';
    return panel.querySelector(':scope > .admin-toolbar h1, :scope > .admin-toolbar h2, h1, h2')?.textContent?.trim()
      || panel.dataset.panel
      || '后台控制中心';
  }

  function syncCurrentContext() {
    const visible = $$('[data-panel]').find(panel => !panel.classList.contains('hidden'));
    const title = panelTitle(visible);
    const target = $('#adminV2CurrentTitle');
    if (target && target.textContent !== title) target.textContent = title;

    const panelName = visible?.dataset.panel || '';
    $$('.admin-workspace-nav [data-nav]').forEach(node => {
      const active = node.dataset.nav === panelName || node.classList.contains('active') || node.classList.contains('admin-nav-current');
      if (active) node.setAttribute('aria-current', 'page');
      else node.removeAttribute('aria-current');
    });
  }

  function refreshSearch() {
    const input = $('#adminV2NavSearch');
    const nav = $('.admin-workspace-nav');
    if (!input || !nav) return;
    const query = input.value.trim().toLowerCase();
    let matches = 0;

    $$('.nav-section', nav).forEach(section => {
      const items = $$('.nav-item', section);
      if (!items.length) return;
      let sectionMatches = 0;
      items.forEach(item => {
        const matched = !query || (item.textContent || '').toLowerCase().includes(query);
        item.classList.toggle('admin-v2-filter-hidden', !matched);
        if (matched) sectionMatches += 1;
      });
      section.classList.toggle('admin-v2-filter-hidden', query !== '' && sectionMatches === 0);
      if (query && sectionMatches) section.classList.remove('collapsed');
      matches += sectionMatches;
    });

    $('#adminV2SearchEmpty')?.classList.toggle('visible', Boolean(query) && matches === 0);
  }

  function closeMobileNav() {
    document.body.classList.remove('admin-v2-nav-open');
  }

  function bindInteractions() {
    if (document.body.dataset.adminV2Bound === '1') return;
    document.body.dataset.adminV2Bound = '1';

    document.addEventListener('input', event => {
      if (event.target?.id === 'adminV2NavSearch') refreshSearch();
    });
    document.addEventListener('click', event => {
      if (event.target.closest('#adminV2MenuButton')) {
        document.body.classList.toggle('admin-v2-nav-open');
        return;
      }
      if (event.target.closest('#adminV2Backdrop')) {
        closeMobileNav();
        return;
      }
      const navTarget = event.target.closest('.admin-workspace-nav [data-nav], .admin-workspace-nav [data-config-entry], .admin-workspace-nav [data-admin-panel], .admin-workspace-nav a[href]');
      if (navTarget && window.matchMedia('(max-width: 980px)').matches) closeMobileNav();
      if (navTarget?.matches('[data-nav], [data-config-entry], [data-admin-panel]')) window.setTimeout(syncCurrentContext, 0);
    });
    document.addEventListener('keydown', event => {
      if (event.key === 'Escape') closeMobileNav();
      if (event.key === '/' && !/INPUT|TEXTAREA|SELECT/.test(document.activeElement?.tagName || '')) {
        event.preventDefault();
        $('#adminV2NavSearch')?.focus();
      }
    });
  }

  function watchPanels() {
    const root = $('.admin-main');
    if (!root || root.dataset.adminV2Watch === '1') return;
    root.dataset.adminV2Watch = '1';
    const observer = new MutationObserver(records => {
      if (!records.some(record => record.type === 'attributes' && record.attributeName === 'class')) return;
      syncCurrentContext();
    });
    observer.observe(root, { subtree: true, attributes: true, attributeFilter: ['class'] });
  }

  function waitForDynamicNavigation() {
    const nav = $('.admin-workspace-nav');
    if (!nav) return;
    const observer = new MutationObserver(() => {
      ensureFrontendControlEntry($('.sidebar'));
      refreshSearch();
      syncCurrentContext();
    });
    observer.observe(nav, { childList: true, subtree: true });
    window.setTimeout(() => observer.disconnect(), 8000);
  }

  function boot() {
    ensureStylesheet();
    createChrome();
    bindInteractions();
    watchPanels();
    waitForDynamicNavigation();
    refreshSearch();
    syncCurrentContext();
  }

  ensureStylesheet();
  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', boot, { once: true });
  else boot();
})();
