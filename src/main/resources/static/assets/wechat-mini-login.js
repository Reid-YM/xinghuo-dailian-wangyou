(() => {
  'use strict';
  const root = document.querySelector('[data-mini-login]');
  if (!root) return;
  const start = root.querySelector('[data-mini-start]');
  const refresh = root.querySelector('[data-mini-refresh]');
  const complete = root.querySelector('[data-mini-complete]');
  const panel = root.querySelector('[data-mini-challenge]');
  const code = root.querySelector('[data-mini-code]');
  const state = root.querySelector('[data-mini-state]');
  const unavailable = root.querySelector('[data-mini-unavailable]');
  let timer = null;
  let active = false;

  async function post(path) {
    const response = await fetch(path, {
      method: 'POST', credentials: 'same-origin', cache: 'no-store',
      headers: { 'X-Requested-With': 'fetch', Accept: 'application/json' }
    });
    if (!response.ok) throw new Error('暂时无法连接，请稍后重试。');
    return response.json();
  }

  function safeReturn() {
    const raw = new URLSearchParams(location.search).get('next') || '';
    if (!raw) return '/account';
    try {
      const url = new URL(raw, location.origin);
      if (url.origin === location.origin && url.pathname !== '/account/login') {
        return url.pathname + url.search + url.hash;
      }
    } catch (_) { /* use account home */ }
    return '/account';
  }

  async function poll() {
    if (!active) return;
    try {
      const result = await post('/api/account/wechat-mini/poll');
      if (result.status === 'ready') {
        active = false;
        state.textContent = `小程序已确认。请核对小程序中的微信识别码尾号 ${result.identityHint}，再完成确认。`;
        complete.hidden = false;
      } else if (result.status === 'expired') {
        active = false;
        state.textContent = '登录码已过期，请换一个。';
      } else {
        timer = setTimeout(poll, 2800);
      }
    } catch (_) {
      if (active) timer = setTimeout(poll, 5000);
    }
  }

  async function begin() {
    clearTimeout(timer);
    active = false;
    complete.hidden = true;
    code.textContent = '--------';
    start.disabled = true;
    state.textContent = '正在生成登录码…';
    panel.hidden = false;
    try {
      const result = await post('/api/account/wechat-mini/challenge');
      code.textContent = result.code.slice(0, 4) + ' ' + result.code.slice(4);
      state.textContent = result.binding ? '在小程序确认后会关联当前网站账号。' : '打开星火小程序 → 我的 → 登录网站，输入上方登录码。';
      active = true;
      timer = setTimeout(poll, 2800);
    } catch (error) {
      state.textContent = error.message;
    } finally {
      start.disabled = false;
    }
  }

  start.addEventListener('click', begin);
  refresh.addEventListener('click', begin);
  complete.addEventListener('click', async () => {
    complete.disabled = true;
    state.textContent = '正在完成登录…';
    try {
      await post('/api/account/wechat-mini/complete');
      state.textContent = '已完成，正在进入账号中心…';
      location.assign(safeReturn());
    } catch (error) {
      state.textContent = error.message;
      complete.disabled = false;
    }
  });
  fetch('/api/account/wechat-mini/availability', { cache: 'no-store' })
    .then((response) => response.ok ? response.json() : { enabled: false })
    .then((result) => {
      start.hidden = !result.enabled;
      unavailable.hidden = result.enabled;
    })
    .catch(() => {
      start.hidden = true;
      unavailable.hidden = false;
    });
  window.addEventListener('pagehide', () => { active = false; clearTimeout(timer); });
})();
