(() => {
  'use strict';

  const $ = (selector, root = document) => root.querySelector(selector);
  const text = (value) => String(value ?? '').trim();
  let timer = null;
  let active = false;
  let observer = null;

  function node(tag, className, value) {
    const element = document.createElement(tag);
    if (className) element.className = className;
    if (value !== undefined && value !== null) element.textContent = String(value);
    return element;
  }

  function append(parent, ...children) {
    children.filter(Boolean).forEach((child) => parent.append(child));
    return parent;
  }

  async function post(path) {
    const response = await fetch(path, {
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
    if (!response.ok) {
      throw new Error(text(body?.message) || text(body?.error) || '暂时无法连接小程序登录服务，请稍后重试。');
    }
    return body;
  }

  async function availability() {
    try {
      const response = await fetch('/api/account/wechat-mini/availability', {
        credentials: 'same-origin',
        cache: 'no-store',
        headers: { Accept: 'application/json' }
      });
      if (!response.ok) return false;
      const result = await response.json();
      return !!result?.enabled;
    } catch (ignore) {
      return false;
    }
  }

  function stopPolling() {
    active = false;
    if (timer) clearTimeout(timer);
    timer = null;
  }

  function formatCode(value) {
    const code = text(value).replace(/\s+/g, '');
    if (code.length <= 4) return code || '--------';
    return `${code.slice(0, 4)} ${code.slice(4)}`;
  }

  function setState(message, kind = '') {
    const host = $('#n4MiniState');
    if (!host) return;
    host.className = `n4-message ${kind}`.trim();
    host.textContent = message || '';
  }

  function resetChallenge() {
    stopPolling();
    const code = $('#n4MiniCode');
    const qr = $('#n4MiniQr');
    const qrWrap = $('#n4MiniQrWrap');
    const complete = $('#n4MiniComplete');
    if (code) code.textContent = '--------';
    if (qr) qr.removeAttribute('src');
    if (qrWrap) qrWrap.hidden = true;
    if (complete) {
      complete.hidden = true;
      complete.disabled = false;
    }
  }

  async function poll() {
    if (!active || !$('#n4MiniPanel')) return;
    try {
      const result = await post('/api/account/wechat-mini/poll');
      const status = text(result?.status).toLowerCase();
      if (status === 'ready') {
        stopPolling();
        const hint = text(result?.identityHint);
        setState(
          hint
            ? `小程序已确认。请核对小程序中的微信识别码尾号 ${hint}，确认无误后完成登录。`
            : '小程序已确认，请核对手机端身份提示后完成登录。',
          'ok'
        );
        const complete = $('#n4MiniComplete');
        if (complete) complete.hidden = false;
        return;
      }
      if (status === 'expired') {
        stopPolling();
        setState('登录二维码已过期，请刷新后重新扫描。', 'error');
        return;
      }
      timer = setTimeout(poll, 2800);
    } catch (error) {
      if (!active) return;
      setState(text(error?.message) || '正在重新连接扫码登录服务…', 'error');
      timer = setTimeout(poll, 5000);
    }
  }

  async function begin() {
    const start = $('#n4MiniStart');
    if (!start || start.disabled) return;
    resetChallenge();
    start.disabled = true;
    start.textContent = '正在生成二维码…';
    const panel = $('#n4MiniPanel');
    if (panel) panel.hidden = false;
    setState('正在生成 5 分钟有效的安全登录二维码…');
    try {
      const result = await post('/api/account/wechat-mini/challenge');
      const code = $('#n4MiniCode');
      if (code) code.textContent = formatCode(result?.code);
      const qrImage = text(result?.qrImage);
      const qr = $('#n4MiniQr');
      const qrWrap = $('#n4MiniQrWrap');
      if (qr && qrWrap && qrImage.startsWith('data:image/png;base64,')) {
        qr.src = qrImage;
        qrWrap.hidden = false;
      }
      setState(
        result?.binding
          ? '打开“星火迷彩追踪器”小程序扫码或输入关联码，确认后会关联当前网站账号。'
          : '打开“星火迷彩追踪器”小程序 → 扫码登录网站；也可以手动输入上方 8 位登录码。'
      );
      active = true;
      timer = setTimeout(poll, 2800);
    } catch (error) {
      setState(text(error?.message) || '二维码生成失败，请稍后重试。', 'error');
    } finally {
      start.disabled = false;
      start.textContent = '生成新的小程序登录二维码';
    }
  }

  async function complete() {
    const button = $('#n4MiniComplete');
    if (!button || button.disabled) return;
    button.disabled = true;
    setState('正在领取网站登录会话…');
    try {
      await post('/api/account/wechat-mini/complete');
      stopPolling();
      setState('登录完成，正在进入 Frontend Next 个人中心…', 'ok');
      location.assign('/next/account');
    } catch (error) {
      setState(text(error?.message) || '暂时无法完成登录，请刷新二维码后重试。', 'error');
      button.disabled = false;
    }
  }

  function installStyles() {
    if ($('#frontend-next-n4-mini-style')) return;
    const style = document.createElement('style');
    style.id = 'frontend-next-n4-mini-style';
    style.textContent = `
      .n4-mini-entry{margin-top:12px;border:1px solid #ffffff14;border-radius:14px;background:#090d11;padding:14px}
      .n4-mini-title{display:flex;align-items:center;justify-content:space-between;gap:12px;margin-bottom:9px}.n4-mini-title b{font-size:12px}.n4-mini-title span{font-size:9px;color:#7ee5a3;border:1px solid #39d98a33;border-radius:99px;padding:4px 7px}
      .n4-mini-panel{margin-top:12px;padding-top:12px;border-top:1px solid #ffffff12}.n4-mini-code{font:800 22px/1.2 ui-monospace,SFMono-Regular,Menlo,Consolas,monospace;letter-spacing:.12em;color:var(--gold);text-align:center;margin:7px 0 12px}
      .n4-mini-qr{display:grid;place-items:center;margin:0 auto 12px;padding:9px;background:#fff;border-radius:12px;width:min(230px,100%)}.n4-mini-qr img{display:block;width:100%;height:auto;aspect-ratio:1}.n4-mini-actions{display:grid;grid-template-columns:1fr 1fr;gap:8px}.n4-mini-actions .btn{margin-top:0!important;width:100%}.n4-mini-tip{margin:9px 0 0;color:#727a84;font-size:9px;line-height:1.55}
      @media(max-width:520px){.n4-mini-actions{grid-template-columns:1fr}}
    `;
    document.head.append(style);
  }

  async function mountGuestMiniLogin() {
    const form = $('#n4LoginForm');
    if (!form || form.dataset.miniInstalled === '1') return;
    form.dataset.miniInstalled = '1';
    installStyles();

    const oauth = $('#n4WechatLogin', form);
    if (oauth) {
      oauth.textContent = oauth.disabled ? '微信开放平台扫码暂不可用' : '微信开放平台扫码登录';
    }

    const entry = node('section', 'n4-mini-entry');
    entry.id = 'n4MiniLogin';
    const title = node('div', 'n4-mini-title');
    append(title, node('b', '', '星火迷彩追踪器 · 小程序扫码登录'), node('span', '', '推荐'));
    const intro = node('p', 'n4-note', '网站生成一次性二维码，小程序确认身份后才给当前浏览器建立会话；二维码与 8 位码均在 5 分钟后失效。');
    const start = node('button', 'btn gold', '正在检查小程序登录…');
    start.type = 'button'; start.id = 'n4MiniStart'; start.disabled = true;
    start.addEventListener('click', begin);

    const panel = node('div', 'n4-mini-panel'); panel.id = 'n4MiniPanel'; panel.hidden = true;
    append(panel, node('div', 'n4-note', '8 位登录码'), node('div', 'n4-mini-code', '--------'));
    panel.lastElementChild.id = 'n4MiniCode';
    const qrWrap = node('div', 'n4-mini-qr'); qrWrap.id = 'n4MiniQrWrap'; qrWrap.hidden = true;
    const qr = document.createElement('img'); qr.id = 'n4MiniQr'; qr.alt = '星火小程序扫码登录二维码'; qr.decoding = 'async';
    qrWrap.append(qr); panel.append(qrWrap);
    const state = node('div', 'n4-message', '请先生成二维码。'); state.id = 'n4MiniState'; state.setAttribute('aria-live', 'polite'); panel.append(state);
    const actions = node('div', 'n4-mini-actions');
    const refresh = node('button', 'btn', '刷新二维码'); refresh.type = 'button'; refresh.addEventListener('click', begin);
    const finish = node('button', 'btn gold', '确认并完成登录'); finish.type = 'button'; finish.id = 'n4MiniComplete'; finish.hidden = true; finish.addEventListener('click', complete);
    append(actions, refresh, finish); panel.append(actions);
    panel.append(node('p', 'n4-mini-tip', '请只在你自己的设备上完成扫码确认。网站不会要求你在此页面输入微信密码、验证码或令牌。'));
    append(entry, title, intro, start, panel);

    const links = $('.n4-mini-links', form);
    if (links) form.insertBefore(entry, links);
    else form.append(entry);

    const enabled = await availability();
    if (!$('#n4MiniStart')) return;
    start.disabled = !enabled;
    start.textContent = enabled ? '生成小程序登录二维码' : '小程序扫码登录暂不可用';
    if (!enabled) setState('当前服务器尚未完成小程序登录配置，可继续使用账号密码或微信开放平台登录。');
  }

  function watchGuestState() {
    mountGuestMiniLogin();
    if (observer) return;
    observer = new MutationObserver(() => {
      if ($('#n4LoginForm')) mountGuestMiniLogin();
      else stopPolling();
    });
    observer.observe(document.body, { childList: true, subtree: true });
  }

  window.addEventListener('pagehide', () => {
    stopPolling();
    observer?.disconnect();
  });

  window.XHFrontendNextN4Mini = { begin, complete, stop: stopPolling };
  watchGuestState();
})();
