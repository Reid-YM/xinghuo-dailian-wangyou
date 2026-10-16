(() => {
  'use strict';

  const $ = (selector, root = document) => root.querySelector(selector);

  const STAGE = 'N8 · TOTAL INTEGRATION';
  const HOME_PROGRESS_COPY = '示意进度卡仅用于展示界面，不代表当前账号真实订单；登录后请以个人中心真实业务记录为准。';
  const HOME_STATUS_COPY = '服务大厅、真实询价、账号、业务记录与安全设置已逐步接入现有接口；Frontend Next 仍以 /next 并行运行，旧前端继续保留回退。';

  function node(tag, className, text) {
    const element = document.createElement(tag);
    if (className) element.className = className;
    if (text !== undefined && text !== null) element.textContent = String(text);
    return element;
  }

  function installStyles() {
    if ($('#frontend-next-n8-style')) return;
    const style = document.createElement('style');
    style.id = 'frontend-next-n8-style';
    style.textContent = `
      .n8-network-status{box-sizing:border-box;position:fixed;z-index:260;left:50%;top:86px;transform:translateX(-50%);width:min(760px,calc(100% - 28px));padding:11px 14px;border:1px solid #ff9d2e66;border-radius:12px;background:#171109f2;color:#f2c17f;font-size:11px;line-height:1.55;box-shadow:0 16px 42px #0008;backdrop-filter:blur(12px)}
      .n8-network-status[hidden]{display:none}
      .badge[title]{cursor:help}
      @media(max-width:680px){.n8-network-status{top:72px}}
      @media(prefers-reduced-motion:reduce){html{scroll-behavior:auto!important}.page.active{animation:none!important}.float,.card,.serviceCard,.btn,.navlink,.toast,.n4-login,.n7-auth-tab{transition:none!important;transform:none!important}.float:hover,.card:hover,.serviceCard:hover,.btn:hover{transform:none!important}}
    `;
    document.head.append(style);
  }

  function refreshTruthCopy() {
    const home = $('.page[data-page="home"]');
    if (!home) return;

    const progress = [...home.querySelectorAll('.float .sub')].find((item) =>
      item.textContent.includes('V1') || item.textContent.includes('不连接生产订单') || item.textContent.includes('演示')
    );
    if (progress) progress.textContent = HOME_PROGRESS_COPY;

    const status = [...home.querySelectorAll('.section .sub')].find((item) =>
      item.textContent.includes('逐步接入') || item.textContent.includes('先统一视觉') || item.textContent.includes('新版后台')
    );
    if (status) status.textContent = location.pathname.startsWith('/next') ? HOME_STATUS_COPY : '服务、询价、账号与业务进度已进入新版入口；原有业务数据与权限规则继续生效。';

    const badge = $('.badge');
    if (badge) {
      badge.textContent = location.pathname.startsWith('/next') ? 'FRONTEND NEXT · N8 INTEGRATION' : '星火 · 新版服务平台';
      badge.title = location.pathname.startsWith('/next') ? '并行运行中：旧前端仍保留，可随时回退。' : '当前使用新版服务入口，业务数据以服务器记录为准。';
    }
    document.documentElement.dataset.frontendNextStage = 'n8-integration';
  }

  function ensureNetworkStatus() {
    let banner = $('#n8NetworkStatus');
    if (banner) return banner;
    banner = node('div', 'n8-network-status');
    banner.id = 'n8NetworkStatus';
    banner.hidden = true;
    banner.setAttribute('role', 'status');
    banner.setAttribute('aria-live', 'polite');
    document.body.append(banner);
    return banner;
  }

  function setNetworkState(online) {
    const banner = ensureNetworkStatus();
    if (online) {
      banner.hidden = true;
      banner.textContent = '';
      document.documentElement.dataset.networkState = 'online';
      return;
    }
    banner.textContent = '当前网络已断开。请保留已填写内容，网络恢复后再重试提交。';
    banner.hidden = false;
    document.documentElement.dataset.networkState = 'offline';
  }

  function bindNetwork() {
    setNetworkState(navigator.onLine !== false);
    window.addEventListener('offline', () => setNetworkState(false));
    window.addEventListener('online', () => {
      setNetworkState(true);
      if (typeof window.toast === 'function') window.toast('网络连接已恢复，可以继续操作。');
    });
  }

  function verifyRuntimeOrder() {
    // This is informational only. CI owns the hard replacement gate; runtime never blocks users.
    const expected = [
      'XHFrontendNextN2',
      'XHFrontendNextN3',
      'XHFrontendNextN4',
      'XHFrontendNextN4Mini',
      'XHFrontendNextN5',
      'XHFrontendNextN5Actions',
      'XHFrontendNextN6',
      'XHFrontendNextN7Auth'
    ];
    const available = expected.filter((name) => typeof window[name] !== 'undefined');
    document.documentElement.dataset.frontendNextRuntimes = `${available.length}/${expected.length}`;
  }

  function boot() {
    installStyles();
    refreshTruthCopy();
    bindNetwork();
    verifyRuntimeOrder();
  }

  window.XHFrontendNextN8 = {
    stage: STAGE,
    refreshTruthCopy,
    setNetworkState,
    verifyRuntimeOrder
  };

  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', boot, { once: true });
  else boot();
})();
