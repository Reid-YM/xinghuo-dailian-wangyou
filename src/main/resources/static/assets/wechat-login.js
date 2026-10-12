(() => {
  'use strict';

  const button = document.querySelector('[data-wechat-login]');
  const dialog = document.querySelector('#wechatMiniDialog');
  if (!button || !dialog) return;

  const image = dialog.querySelector('[data-wechat-mini-code]');
  const statusText = dialog.querySelector('[data-wechat-mini-status]');
  const closeButtons = dialog.querySelectorAll('[data-wechat-mini-close]');
  const params = new URLSearchParams(location.search);
  const rawReturn = params.get('next') || params.get('redirect') || '';
  const returnTo = safeReturnTo(rawReturn);
  let timer = null;
  let activeFlow = '';
  let inFlight = false;

  button.addEventListener('click', start);
  closeButtons.forEach((el) => el.addEventListener('click', closeDialog));
  dialog.addEventListener('click', (event) => {
    if (event.target === dialog) closeDialog();
  });

  const legacyStatus = params.get('wechat');
  if (legacyStatus) showLegacyStatus(legacyStatus);

  async function start() {
    if (inFlight) return;
    inFlight = true;
    const oldText = button.textContent;
    button.disabled = true;
    button.textContent = '正在生成小程序码…';
    try {
      const response = await fetch('/api/account/wechat-mini/start', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', 'Accept': 'application/json' },
        credentials: 'same-origin',
        body: JSON.stringify({ next: returnTo || '/account' })
      });
      const data = await readJson(response);
      if (!response.ok) throw new Error(data.message || '微信扫码登录暂时不可用');

      activeFlow = data.flow;
      image.src = `${data.codeUrl}&_=${Date.now()}`;
      statusText.textContent = '请使用微信扫描小程序码，并在手机上确认登录。';
      dialog.hidden = false;
      document.documentElement.style.overflow = 'hidden';
      schedulePoll(800);
    } catch (error) {
      showMessage(error.message || '微信扫码登录暂时不可用，请使用邮箱登录。');
    } finally {
      inFlight = false;
      button.disabled = false;
      button.textContent = oldText;
    }
  }

  async function poll() {
    if (!activeFlow || dialog.hidden) return;
    try {
      const response = await fetch(`/api/account/wechat-mini/status?flow=${encodeURIComponent(activeFlow)}`, {
        credentials: 'same-origin',
        headers: { 'Accept': 'application/json' },
        cache: 'no-store'
      });
      const data = await readJson(response);
      if (!response.ok) throw new Error(data.message || '登录状态检查失败');

      if (data.status === 'CONFIRMED' || data.status === 'CONSUMED') {
        statusText.textContent = '登录成功，正在进入账号中心…';
        stopPolling();
        location.assign(data.redirect || returnTo || '/account');
        return;
      }
      if (data.status === 'EXPIRED') {
        statusText.textContent = '二维码已失效，请关闭后重新生成。';
        stopPolling();
        return;
      }
      statusText.textContent = '等待手机微信确认…';
      schedulePoll(1400);
    } catch (error) {
      statusText.textContent = error.message || '状态检查失败，请重新生成二维码。';
      stopPolling();
    }
  }

  function schedulePoll(delay) {
    stopPolling();
    timer = window.setTimeout(poll, delay);
  }

  function stopPolling() {
    if (timer) window.clearTimeout(timer);
    timer = null;
  }

  function closeDialog() {
    stopPolling();
    activeFlow = '';
    image.removeAttribute('src');
    dialog.hidden = true;
    document.documentElement.style.overflow = '';
  }

  function showMessage(message) {
    const host = document.querySelector('#accountMsg');
    if (!host) return;
    while (host.firstChild) host.removeChild(host.firstChild);
    const box = document.createElement('div');
    box.className = 'notice error';
    box.setAttribute('role', 'alert');
    box.textContent = message;
    host.appendChild(box);
    host.hidden = false;
  }

  function showLegacyStatus(status) {
    const messages = {
      unavailable: '微信登录尚未完成配置，请先使用邮箱登录。',
      cancelled: '你已取消微信扫码登录，可重新扫码或使用邮箱登录。',
      state: '微信登录校验已失效，请重新发起扫码登录。',
      rate: '微信登录操作过于频繁，请稍后再试。',
      disabled: '该星火账号已被停用，请联系客服。',
      failed: '微信登录未完成，请重新扫码；如持续失败请联系客服。'
    };
    if (messages[status]) showMessage(messages[status]);
  }

  async function readJson(response) {
    try { return await response.json(); } catch (ignored) { return {}; }
  }

  function safeReturnTo(raw) {
    if (!raw) return '';
    let url;
    try { url = new URL(raw, location.origin); } catch (ignored) { return ''; }
    if (url.origin !== location.origin) return '';
    const target = `${url.pathname}${url.search}${url.hash}`;
    if (target.indexOf('/account/login') === 0 || target.indexOf('/api/account/wechat') === 0) return '';
    return target;
  }
})();
