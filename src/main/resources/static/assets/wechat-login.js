(() => {
  'use strict';

  const link = document.querySelector('[data-wechat-login]');
  if (!link) return;

  const params = new URLSearchParams(location.search);
  const rawReturn = params.get('next') || params.get('redirect') || '';
  const returnTo = safeReturnTo(rawReturn);
  link.href = returnTo
    ? `/api/account/wechat/start?next=${encodeURIComponent(returnTo)}`
    : '/api/account/wechat/start';

  const status = params.get('wechat');
  if (!status) return;

  const messages = {
    unavailable: '微信登录尚未完成开放平台配置，请先使用邮箱登录。',
    cancelled: '你已取消微信扫码登录，可重新扫码或使用邮箱登录。',
    state: '微信登录校验已失效，请重新发起扫码登录。',
    rate: '微信登录操作过于频繁，请稍后再试。',
    disabled: '该星火账号已被停用，请联系客服。',
    failed: '微信登录未完成，请重新扫码；如持续失败请联系客服。'
  };
  const message = messages[status];
  if (!message) return;

  const host = document.querySelector('#accountMsg');
  if (!host) return;
  while (host.firstChild) host.removeChild(host.firstChild);
  const box = document.createElement('div');
  box.className = 'notice error';
  box.setAttribute('role', 'alert');
  box.textContent = message;
  host.appendChild(box);
  host.hidden = false;

  function safeReturnTo(raw) {
    if (!raw) return '';
    let url;
    try {
      url = new URL(raw, location.origin);
    } catch (ignored) {
      return '';
    }
    if (url.origin !== location.origin) return '';
    const target = `${url.pathname}${url.search}${url.hash}`;
    if (target.indexOf('/account/login') === 0 || target.indexOf('/api/account/wechat') === 0) {
      return '';
    }
    return target;
  }
})();
