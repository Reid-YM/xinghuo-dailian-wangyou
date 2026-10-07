(() => {
  'use strict';

  const $ = (selector, root = document) => root.querySelector(selector);
  const $$ = (selector, root = document) => [...root.querySelectorAll(selector)];

  function hasControlCenter() {
    return Boolean($('.admin-main') && $('.sidebar') && $$('[data-panel]').length);
  }

  function loadControlCenterV2() {
    // 此脚本是后台公共兜底，但 V2 只能装载到真正的运营后台工作区。
    // 独立攻略页、登录页、前端控制中心即使误加载 resilience，也保持自身布局。
    if (!hasControlCenter()) return;
    if (!$('link[data-admin-control-center-v2]')) {
      const link = document.createElement('link');
      link.rel = 'stylesheet';
      link.href = '/assets/admin-control-center-v2.css?v=20261007a005';
      link.dataset.adminControlCenterV2 = '1';
      document.head.appendChild(link);
    }
    if (!$('link[data-admin-light-theme]')) {
      const light = document.createElement('link');
      light.rel = 'stylesheet';
      light.href = '/assets/admin-light-theme.css?v=20261007l003';
      light.dataset.adminLightTheme = '1';
      document.head.appendChild(light);
    }
    if (!$('script[data-admin-control-center-v2]')) {
      const script = document.createElement('script');
      script.src = '/assets/admin-control-center-v2.js?v=20261007a004';
      script.dataset.adminControlCenterV2 = '1';
      document.head.appendChild(script);
    }
  }

  function injectStyles() {
    if ($('#adminResilienceStyles')) return;
    const style = document.createElement('style');
    style.id = 'adminResilienceStyles';
    style.textContent = `
      .admin-panel-load-error{margin:14px 0 18px;padding:16px 18px;border:1px solid rgba(176,0,32,.28);background:rgba(176,0,32,.055)}
      .admin-panel-load-error strong{display:block;margin-bottom:6px;color:#8a1427}
      .admin-panel-load-error p{margin:0;color:var(--muted,#667085);line-height:1.65}
      .admin-panel-load-error .row-actions{margin-top:12px}
    `;
    document.head.appendChild(style);
  }

  function visiblePanel() {
    return $$('[data-panel]').find(panel => !panel.classList.contains('hidden')) || null;
  }

  function restoreVisiblePanel() {
    const panels = $$('.admin-main>[data-panel]');
    if (!panels.length || panels.some(panel => !panel.classList.contains('hidden'))) return null;
    const fallback = panels.find(panel => panel.dataset.panel === 'dashboard') || panels[0];
    fallback.classList.remove('hidden');
    const name = fallback.dataset.panel || '';
    $$('[data-nav]').forEach(node => node.classList.toggle('active', Boolean(name) && node.dataset.nav === name));
    return fallback;
  }

  function clearPanelError(panel) {
    panel?.querySelector(':scope > [data-admin-panel-error]')?.remove();
  }

  function retryCurrentPanel(panel) {
    const name = panel?.dataset.panel || '';
    const active = $(`[data-nav="${name}"].active`) || $(`[data-nav="${name}"]`);
    if (active) {
      active.click();
      return;
    }
    location.reload();
  }

  function showPanelError(message, panel = visiblePanel()) {
    if (!panel) return;
    clearPanelError(panel);
    const box = document.createElement('section');
    box.className = 'card admin-panel-load-error';
    box.dataset.adminPanelError = '1';

    const title = document.createElement('strong');
    title.textContent = '当前模块加载失败';
    const detail = document.createElement('p');
    detail.textContent = String(message || '数据暂时无法读取，请重试。');
    const actions = document.createElement('div');
    actions.className = 'row-actions';
    const retry = document.createElement('button');
    retry.type = 'button';
    retry.className = 'btn small-btn';
    retry.textContent = '重新加载当前模块';
    retry.addEventListener('click', () => {
      clearPanelError(panel);
      retryCurrentPanel(panel);
    });
    const refresh = document.createElement('button');
    refresh.type = 'button';
    refresh.className = 'btn small-btn ghost';
    refresh.textContent = '刷新后台';
    refresh.addEventListener('click', () => location.reload());
    actions.append(retry, refresh);
    box.append(title, detail, actions);

    const toolbar = panel.querySelector(':scope > .admin-toolbar');
    if (toolbar?.nextSibling) toolbar.parentNode.insertBefore(box, toolbar.nextSibling);
    else panel.prepend(box);
  }

  function ensureTargetPanel(event) {
    const nav = event.target instanceof Element ? event.target.closest('[data-nav]') : null;
    if (!nav) return;
    const name = String(nav.dataset.nav || '').trim();
    if (!name) return;
    const target = $(`[data-panel="${name}"]`);
    if (target) {
      clearPanelError(target);
      return;
    }
    event.preventDefault();
    event.stopImmediatePropagation();
    const panel = restoreVisiblePanel() || visiblePanel();
    showPanelError(`后台模块“${name}”没有对应内容面板，已保留当前页面，避免出现空白。`, panel);
  }

  function watchPanelVisibility() {
    const root = $('.admin-main');
    if (!root || root.dataset.adminBlankGuard === '1') return;
    root.dataset.adminBlankGuard = '1';
    new MutationObserver(records => {
      if (!records.some(record => record.type === 'attributes' && record.attributeName === 'class')) return;
      const restored = restoreVisiblePanel();
      if (restored) {
        window.setTimeout(() => showPanelError('检测到后台模块切换异常，已自动恢复运营总览，避免出现空白页。', restored), 0);
      }
    }).observe(root, { subtree: true, attributes: true, attributeFilter: ['class'] });
  }

  function watchErrorToasts() {
    const area = $('#toastArea');
    if (!area || area.dataset.resilienceWatch === '1') return;
    area.dataset.resilienceWatch = '1';
    new MutationObserver(records => {
      records.flatMap(record => [...record.addedNodes]).forEach(node => {
        if (!(node instanceof Element) || !node.classList.contains('toast') || !node.classList.contains('error')) return;
        const text = (node.textContent || '').trim();
        window.setTimeout(() => {
          const panel = restoreVisiblePanel() || visiblePanel();
          showPanelError(text || '模块加载失败，请重试。', panel);
        }, 0);
      });
    }).observe(area, { childList: true });
  }

  function boot() {
    loadControlCenterV2();
    injectStyles();
    restoreVisiblePanel();
    watchPanelVisibility();
    watchErrorToasts();
  }

  document.addEventListener('click', ensureTargetPanel, true);
  window.addEventListener('unhandledrejection', event => {
    const message = event.reason?.message || String(event.reason || '后台异步任务执行失败');
    const panel = restoreVisiblePanel() || visiblePanel();
    showPanelError(message, panel);
  });
  window.addEventListener('error', event => {
    if (!event.error && !event.message) return;
    const panel = restoreVisiblePanel() || visiblePanel();
    showPanelError(event.error?.message || event.message || '后台脚本执行失败', panel);
  });

  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', boot, { once: true });
  else boot();
})();
