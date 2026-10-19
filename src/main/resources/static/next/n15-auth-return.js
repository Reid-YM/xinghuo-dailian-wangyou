(() => {
  'use strict';
  const runtimePath = pathname => window.XHFrontendShell?.runtimePath?.(pathname) || pathname || location.pathname;
  const runtimeHref = value => window.XHFrontendShell?.runtimeHref?.(value) || value;

  if (!/^\/next(?:\/|$)/.test(runtimePath())) return;

  const text = (value) => String(value ?? '').trim();
  const authModes = new Set(['login', 'register', 'reset']);
  const wechatMessages = {
    unavailable: '微信登录尚未完成开放平台配置，请先使用邮箱登录。',
    cancelled: '你已取消微信扫码登录，可重新扫码或使用邮箱登录。',
    state: '微信登录校验已失效，请重新发起扫码登录。',
    rate: '微信登录操作过于频繁，请稍后再试。',
    disabled: '该账号已被停用，请联系客服。',
    failed: '微信登录未完成，请重新扫码；如持续失败请联系客服。'
  };

  function fallback() {
    return window.XHFrontendShell?.route?.('account') || '/next/account';
  }

  function rawSafe(value) {
    if (!value) return null;
    const requested = text(value);
    const lower = requested.toLowerCase();
    if (!requested || requested.length > 1000) return fallback();
    if (!requested.startsWith('/') || requested.startsWith('//')) return fallback();
    if (requested.includes('\\') || lower.includes('%5c') || requested.includes('\r') || requested.includes('\n')) return fallback();

    let url;
    try {
      url = new URL(requested, location.origin);
    } catch (ignore) {
      return fallback();
    }
    if (url.origin !== location.origin) return fallback();
    if (/^\/(admin|api|logout)(\/|$)/i.test(url.pathname)) return fallback();
    if (/^\/account\/(login|register|reset)(\/|$)/i.test(url.pathname)) return fallback();

    if (['/next/account', '/account'].includes(url.pathname) && authModes.has(text(url.searchParams.get('auth')).toLowerCase())) {
      url.searchParams.delete('auth');
      url.searchParams.delete('token');
      url.searchParams.delete('wechat');
    }

    if (url.searchParams.get('legacy') !== '1') {
      if (url.pathname === '/' || url.pathname === '/home') url.pathname = '/next';
      else if (url.pathname === '/services') url.pathname = '/next/services';
      else if (url.pathname === '/quote') url.pathname = '/next/quote';
      else if (url.pathname === '/account') url.pathname = '/next/account';
      else if (/^\/account\/(?:(?:requests|orders)\/[^/]+|settings\/(?:profile|contact|security)|requests|quotes|demands|orders|order|order-list|progress|tools|tool-progress|favorites|submissions|settings|profile|contact|security|devices|activity)\/?$/.test(url.pathname)) url.pathname = '/next' + url.pathname.replace(/\/$/, '');
      else if (url.pathname === '/account/submit') url.pathname = '/next/account/submit';
      else if (url.pathname === '/guides') url.pathname = '/next/guides';
      else if (url.pathname === '/loadouts' || url.pathname === '/loadouts/') url.pathname = '/next/mastery';
      else if (url.pathname === '/order') url.pathname = '/next/records';
      else if (/^\/order\/[^/]+\/?$/.test(url.pathname)) url.pathname = '/next/record/' + url.pathname.split('/')[2];
      else if (url.pathname === '/mastery') url.pathname = '/next/mastery';
      else if (url.pathname === '/records') url.pathname = '/next/records';
      else if (url.pathname === '/contact') url.pathname = '/next/contact';
      else if (url.pathname === '/trust') url.pathname = '/next/trust';
      else if (url.pathname === '/rules') url.pathname = '/next/rules';
      else if (url.pathname === '/privacy') url.pathname = '/next/privacy';
      else if (url.pathname === '/rules-en' || url.pathname === '/privacy-en') url.pathname = '/next' + url.pathname;
      else if (/^\/guides\/[^/]+\/?$/.test(url.pathname)) url.pathname = '/next' + url.pathname.replace(/\/$/, '');
      else if (/^\/game\/[^/]+\/?$/.test(url.pathname)) url.pathname = '/next' + url.pathname.replace(/\/$/, '');
      else if (/^\/service\/[^/]+\/?$/.test(url.pathname)) url.pathname = '/next' + url.pathname.replace(/\/$/, '');
      else if (/^\/record\/[^/]+\/?$/.test(url.pathname)) url.pathname = '/next' + url.pathname.replace(/\/$/, '');
    }

    return runtimeHref(url.pathname + url.search + url.hash);
  }

  function safeAccountReturn(requested) {
    return requested ? rawSafe(requested) : null;
  }

  function requestedReturn() {
    return safeAccountReturn(new URLSearchParams(location.search).get('next')) || fallback();
  }

  function authUrl(mode) {
    const normalized = authModes.has(mode) ? mode : 'login';
    const url = new URL(window.XHFrontendShell?.isPreview === false ? '/account/' + normalized : '/next/account', location.origin);
    if (normalized !== 'login' && window.XHFrontendShell?.isPreview !== false) url.searchParams.set('auth', normalized);
    const target = requestedReturn();
    if (target !== fallback()) url.searchParams.set('next', target);
    return runtimeHref(url.pathname + url.search);
  }

  function explicitLegacy(url) {
    return url.searchParams.get('legacy') === '1';
  }

  function normalizeAuthLink(link) {
    if (!(link instanceof HTMLAnchorElement)) return;
    const href = link.getAttribute('href');
    if (!href) return;
    let url;
    try { url = new URL(href, location.origin); } catch (ignore) { return; }
    if (url.origin !== location.origin || explicitLegacy(url)) return;

    if (url.pathname === '/account/login') link.href = authUrl('login');
    else if (url.pathname === '/account/register') link.href = authUrl('register');
    else if (url.pathname === '/account/reset') link.href = authUrl('reset');
  }

  function normalizeAuthLinks(root = document) {
    if (root instanceof HTMLAnchorElement) normalizeAuthLink(root);
    root.querySelectorAll?.('a[href]').forEach(normalizeAuthLink);
  }

  function setMessage(value, kind = 'error') {
    const host = document.querySelector('#n4LoginMessage');
    if (!host) return false;
    host.className = `n4-message ${kind}`.trim();
    host.textContent = value || '';
    host.setAttribute('aria-live', 'polite');
    return true;
  }

  function applyWechatStatus() {
    const status = text(new URLSearchParams(location.search).get('wechat')).toLowerCase();
    const message = wechatMessages[status];
    if (!message) return true;
    return setMessage(message, 'error');
  }

  function startWechat(button) {
    if (!button || button.disabled) return;
    const target = requestedReturn();
    location.assign('/api/account/wechat/start?next=' + encodeURIComponent(target));
  }

  async function completeMini(button) {
    if (!button || button.disabled) return;
    button.disabled = true;
    const state = document.querySelector('#n4MiniState');
    if (state) {
      state.className = 'n4-message';
      state.textContent = '正在领取网站登录会话…';
    }
    try {
      const response = await fetch('/api/account/wechat-mini/complete', {
        method: 'POST',
        credentials: 'same-origin',
        cache: 'no-store',
        headers: {
          Accept: 'application/json',
          'X-Requested-With': 'fetch'
        }
      });
      let body = null;
      try { body = await response.json(); } catch (ignore) { body = null; }
      if (!response.ok) throw new Error(text(body?.message) || text(body?.error) || '暂时无法完成登录，请刷新二维码后重试。');
      try { window.XHFrontendNextN4Mini?.stop?.(); } catch (ignore) { /* no-op */ }
      if (state) {
        state.className = 'n4-message ok';
        state.textContent = '登录完成，正在返回原页面…';
      }
      location.assign(requestedReturn());
    } catch (error) {
      if (state) {
        state.className = 'n4-message error';
        state.textContent = text(error?.message) || '暂时无法完成登录，请刷新二维码后重试。';
      }
      button.disabled = false;
    }
  }

  const shell = window.XHFrontendShell;
  if (shell) shell.safeAccountReturn = safeAccountReturn;

  document.addEventListener('click', (event) => {
    const oauth = event.target.closest?.('#n4WechatLogin');
    if (oauth && !oauth.disabled) {
      event.preventDefault();
      event.stopImmediatePropagation();
      startWechat(oauth);
      return;
    }

    const mini = event.target.closest?.('#n4MiniComplete');
    if (mini && !mini.disabled) {
      event.preventDefault();
      event.stopImmediatePropagation();
      completeMini(mini);
    }
  }, true);

  const observer = new MutationObserver((records) => {
    records.forEach((record) => record.addedNodes.forEach((item) => {
      if (item instanceof Element) normalizeAuthLinks(item);
    }));
    normalizeAuthLinks();
    applyWechatStatus();
  });
  observer.observe(document.body, { childList: true, subtree: true });

  normalizeAuthLinks();
  applyWechatStatus();

  window.XHFrontendNextN15 = {
    safeAccountReturn,
    requestedReturn,
    authUrl
  };
})();
