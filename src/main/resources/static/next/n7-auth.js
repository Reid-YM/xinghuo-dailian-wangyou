(() => {
  'use strict';

  const $ = (selector, root = document) => root.querySelector(selector);
  const $$ = (selector, root = document) => [...root.querySelectorAll(selector)];
  const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  let observer = null;
  let cooldownTimer = null;
  let cooldownLeft = 0;
  let activeMode = 'login';

  function text(value) {
    return String(value ?? '').trim();
  }

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

  function button(label, className = 'btn') {
    const element = node('button', className, label);
    element.type = 'button';
    return element;
  }

  function message(host, value, kind = '') {
    if (!host) return;
    host.className = `n4-message n7-message ${kind}`.trim();
    host.textContent = value || '';
    host.setAttribute('aria-live', 'polite');
  }

  async function request(path, options = {}) {
    const method = String(options.method || 'GET').toUpperCase();
    const headers = Object.assign({
      Accept: 'application/json',
      'X-Requested-With': 'XMLHttpRequest'
    }, options.headers || {});
    const init = {
      method,
      credentials: 'same-origin',
      cache: 'no-store',
      headers
    };
    if (options.body !== undefined && options.body !== null && method !== 'GET') {
      headers['Content-Type'] = 'application/json';
      init.body = JSON.stringify(options.body);
    }
    let response;
    try {
      response = await fetch(path, init);
    } catch (error) {
      const failure = new Error('网络连接失败，请检查网络后重试。');
      failure.status = 0;
      throw failure;
    }
    let payload = null;
    try { payload = await response.json(); } catch (ignore) { payload = null; }
    if (response.ok) return payload;
    const failure = new Error(text(payload?.message) || text(payload?.error) || '请求失败，请稍后重试。');
    failure.status = response.status;
    failure.code = text(payload?.error);
    throw failure;
  }

  function utf8Bytes(value) {
    const raw = String(value || '');
    if (typeof TextEncoder === 'function') return new TextEncoder().encode(raw).length;
    return unescape(encodeURIComponent(raw)).length;
  }

  function passwordCheck(value) {
    const raw = String(value || '');
    if (raw.length < 8) return '密码至少 8 位。';
    if (utf8Bytes(raw) > 72) return '密码过长，请控制在 72 字节以内。';
    const kinds = [/[a-z]/.test(raw), /[A-Z]/.test(raw), /[0-9]/.test(raw), /[^A-Za-z0-9]/.test(raw)].filter(Boolean).length;
    if (kinds < 2) return '密码需包含小写、大写、数字、符号中的至少两类。';
    return '';
  }

  function validEmail(value) {
    return EMAIL_RE.test(text(value));
  }

  function installStyles() {
    if ($('#frontend-next-n7-auth-style')) return;
    const style = document.createElement('style');
    style.id = 'frontend-next-n7-auth-style';
    style.textContent = `
      .n7-auth-switcher{grid-column:1/-1;display:grid;grid-template-columns:repeat(3,1fr);gap:6px;margin:0 0 12px;padding:5px;border:1px solid #ffffff12;border-radius:13px;background:#090d11}
      .n7-auth-tab{border:0;border-radius:9px;background:transparent;color:#89919a;padding:10px 8px;font-size:11px;cursor:pointer;transition:.2s}
      .n7-auth-tab:hover{color:#fff;background:#ffffff08}.n7-auth-tab.active{color:#111;background:linear-gradient(135deg,var(--gold),#ffb80e);font-weight:900}
      .n7-auth-panel{grid-column:1/-1;border:1px solid #ffffff14;border-radius:16px;background:#0c1015;padding:18px;align-self:start}
      .n7-auth-panel[hidden]{display:none}.n7-auth-panel h2{margin:0 0 6px;font-size:22px}.n7-auth-panel>p{margin:0 0 15px;color:#878f99;font-size:11px;line-height:1.65}
      .n7-grid{display:grid;grid-template-columns:1fr 1fr;gap:10px}.n7-field{display:grid;gap:6px}.n7-field.full{grid-column:1/-1}.n7-field label{font-size:11px;color:#bcc1c7}
      .n7-field input{width:100%;border:1px solid #ffffff12;background:#090d11;color:#eef0f2;border-radius:10px;padding:12px;outline:none}.n7-field input:focus{border-color:var(--gold);box-shadow:0 0 0 3px #ffcb2d12}
      .n7-actions{display:flex;gap:8px;align-items:center;flex-wrap:wrap;margin-top:13px}.n7-actions .btn{margin-top:0}.n7-message{margin-top:10px;min-height:18px}.n7-message.ok{color:#6fe4a1}.n7-message.error{color:#ff8e8e}
      .n7-terms{display:flex;gap:8px;align-items:flex-start;color:#858d96;font-size:10px;line-height:1.5;margin-top:10px}.n7-terms input{margin-top:2px}
      .n7-fallback{margin-top:12px;padding-top:12px;border-top:1px solid #ffffff10;color:#707984;font-size:9px;line-height:1.55}.n7-fallback a{color:#c8ad5d}
      .n7-reset-confirm{margin-top:14px;padding-top:14px;border-top:1px solid #ffffff10}.n7-auth-email-only{display:none!important}
      @media(max-width:700px){.n7-grid{grid-template-columns:1fr}.n7-field.full{grid-column:auto}.n7-auth-switcher{grid-template-columns:1fr 1fr 1fr}.n7-auth-tab{padding:9px 4px}.n7-auth-panel{padding:14px}}
    `;
    document.head.append(style);
  }

  function field(label, input) {
    const wrap = node('div', 'n7-field');
    append(wrap, node('label', '', label), input);
    return wrap;
  }

  function input(id, type, placeholder, maxLength, autocomplete) {
    const element = document.createElement('input');
    element.id = id;
    element.type = type;
    element.placeholder = placeholder || '';
    if (maxLength) element.maxLength = maxLength;
    if (autocomplete) element.autocomplete = autocomplete;
    return element;
  }

  function stopMiniLogin() {
    try { window.XHFrontendNextN4Mini?.stop?.(); } catch (ignore) { /* no-op */ }
  }

  function setMode(mode) {
    const allowed = ['login', 'register', 'reset'];
    activeMode = allowed.includes(mode) ? mode : 'login';
    const form = $('#n4LoginForm');
    const register = $('#n7RegisterPanel');
    const reset = $('#n7ResetPanel');
    if (form) form.hidden = activeMode !== 'login';
    if (register) register.hidden = activeMode !== 'register';
    if (reset) reset.hidden = activeMode !== 'reset';
    $$('.n7-auth-tab').forEach((tab) => {
      const active = tab.dataset.mode === activeMode;
      tab.classList.toggle('active', active);
      tab.setAttribute('aria-selected', active ? 'true' : 'false');
    });
    if (activeMode !== 'login') stopMiniLogin();
    const target = activeMode === 'login' ? $('#n4ContactValue') : activeMode === 'register' ? $('#n7RegisterEmail') : $('#n7ResetEmail');
    window.setTimeout(() => target?.focus(), 0);
  }

  function normalizeLoginForm(form) {
    const typeSelect = $('#n4ContactType', form);
    if (typeSelect) {
      const option = node('option', '', '邮箱');
      option.value = 'EMAIL';
      typeSelect.replaceChildren(option);
      typeSelect.value = 'EMAIL';
      const typeWrap = typeSelect.closest('.n4-field');
      if (typeWrap) typeWrap.classList.add('n7-auth-email-only');
    }
    const contact = $('#n4ContactValue', form);
    if (contact) {
      contact.type = 'email';
      contact.inputMode = 'email';
      contact.autocomplete = 'username';
      contact.placeholder = '请输入邮箱地址';
      contact.maxLength = 120;
    }
    const notes = $$('.n4-note', form);
    if (notes.length) notes[0].textContent = '当前账号密码体系仅支持邮箱登录；也可使用微信开放平台或小程序扫码登录。';
    const links = $('.n4-mini-links', form);
    if (links) {
      links.replaceChildren();
      const legacy = document.createElement('a');
      legacy.href = '/account/login';
      legacy.textContent = '旧版登录入口';
      links.append(legacy);
    }
  }

  function createSwitcher(card, form) {
    const switcher = node('div', 'n7-auth-switcher');
    switcher.id = 'n7AuthSwitcher';
    switcher.setAttribute('role', 'tablist');
    [['login', '登录'], ['register', '注册'], ['reset', '找回密码']].forEach(([mode, label]) => {
      const tab = button(label, 'n7-auth-tab');
      tab.dataset.mode = mode;
      tab.setAttribute('role', 'tab');
      tab.addEventListener('click', () => setMode(mode));
      switcher.append(tab);
    });
    card.insertBefore(switcher, form);
    return switcher;
  }

  function createRegisterPanel(card, form) {
    const panel = node('section', 'n7-auth-panel');
    panel.id = 'n7RegisterPanel';
    panel.hidden = true;
    append(panel,
      node('h2', '', '注册邮箱账号'),
      node('p', '', '验证码会发送到你的邮箱。注册成功后会直接建立当前浏览器登录会话。')
    );
    const grid = node('div', 'n7-grid');
    const email = input('n7RegisterEmail', 'email', 'you@example.com', 160, 'email');
    const name = input('n7RegisterName', 'text', '请输入昵称', 60, 'nickname');
    const code = input('n7RegisterCode', 'text', '6 位邮箱验证码', 12, 'one-time-code');
    code.inputMode = 'numeric';
    const password = input('n7RegisterPassword', 'password', '至少 8 位，至少两类字符', 72, 'new-password');
    const confirm = input('n7RegisterConfirm', 'password', '再次输入密码', 72, 'new-password');
    append(grid, field('邮箱', email), field('昵称', name), field('邮箱验证码', code), field('登录密码', password), field('确认密码', confirm));
    panel.append(grid);

    const terms = node('label', 'n7-terms');
    const agree = document.createElement('input'); agree.type = 'checkbox'; agree.id = 'n7RegisterAgree';
    append(terms, agree, document.createTextNode('我已阅读并同意现有服务规则与隐私说明。'));
    panel.append(terms);
    const actions = node('div', 'n7-actions');
    const send = button('发送邮箱验证码', 'btn'); send.id = 'n7RegisterSendCode';
    const submit = button('创建账号 →', 'btn gold'); submit.id = 'n7RegisterSubmit';
    append(actions, send, submit); panel.append(actions);
    const status = node('div', 'n4-message n7-message'); status.id = 'n7RegisterMessage'; panel.append(status);
    const fallback = node('div', 'n7-fallback');
    const legacy = document.createElement('a'); legacy.href = '/account/register'; legacy.textContent = '打开旧版注册页';
    append(fallback, document.createTextNode('遇到兼容问题时仍可 '), legacy, document.createTextNode('。旧账号流程不会删除。'));
    panel.append(fallback);

    send.addEventListener('click', () => sendRegisterCode(email, send, status));
    submit.addEventListener('click', () => registerAccount({ email, name, code, password, confirm, agree, submit, status }));
    card.insertBefore(panel, form.nextSibling);
    return panel;
  }

  function createResetPanel(card) {
    const panel = node('section', 'n7-auth-panel');
    panel.id = 'n7ResetPanel';
    panel.hidden = true;
    append(panel,
      node('h2', '', '找回登录密码'),
      node('p', '', '为避免账号枚举，无论邮箱是否存在，申请步骤都会返回同一种提示。')
    );
    const email = input('n7ResetEmail', 'email', '请输入绑定邮箱', 160, 'email');
    const requestGrid = node('div', 'n7-grid');
    const emailField = field('绑定邮箱', email); emailField.classList.add('full'); requestGrid.append(emailField); panel.append(requestGrid);
    const requestActions = node('div', 'n7-actions');
    const send = button('发送重置验证码', 'btn gold'); send.id = 'n7ResetRequest'; requestActions.append(send); panel.append(requestActions);

    const confirmWrap = node('div', 'n7-reset-confirm'); confirmWrap.id = 'n7ResetConfirmWrap'; confirmWrap.hidden = true;
    const confirmGrid = node('div', 'n7-grid');
    const code = input('n7ResetCode', 'text', '6 位验证码', 12, 'one-time-code'); code.inputMode = 'numeric';
    const password = input('n7ResetPassword', 'password', '设置新密码', 72, 'new-password');
    const confirm = input('n7ResetConfirm', 'password', '再次输入新密码', 72, 'new-password');
    append(confirmGrid, field('邮箱验证码', code), field('新密码', password), field('确认新密码', confirm));
    confirmWrap.append(confirmGrid);
    const confirmActions = node('div', 'n7-actions');
    const submit = button('确认重置 →', 'btn gold'); submit.id = 'n7ResetSubmit'; confirmActions.append(submit); confirmWrap.append(confirmActions); panel.append(confirmWrap);
    const status = node('div', 'n4-message n7-message'); status.id = 'n7ResetMessage'; panel.append(status);
    const fallback = node('div', 'n7-fallback');
    const legacy = document.createElement('a'); legacy.href = '/account/reset'; legacy.textContent = '打开旧版找回页';
    append(fallback, document.createTextNode('旧版找回流程继续保留，可随时 '), legacy, document.createTextNode('。'));
    panel.append(fallback);

    send.addEventListener('click', () => requestReset(email, send, confirmWrap, status));
    submit.addEventListener('click', () => confirmReset({ email, code, password, confirm, submit, status }));
    card.append(panel);
    return panel;
  }

  function startCooldown(button) {
    if (cooldownTimer) clearInterval(cooldownTimer);
    cooldownLeft = 60;
    const render = () => {
      if (!button?.isConnected) return;
      button.disabled = cooldownLeft > 0;
      button.textContent = cooldownLeft > 0 ? `重新发送 ${cooldownLeft}s` : '重新发送邮箱验证码';
    };
    render();
    cooldownTimer = setInterval(() => {
      cooldownLeft -= 1;
      render();
      if (cooldownLeft <= 0) {
        clearInterval(cooldownTimer);
        cooldownTimer = null;
      }
    }, 1000);
  }

  async function sendRegisterCode(emailInput, button, status) {
    const email = text(emailInput.value);
    if (!validEmail(email)) {
      message(status, '请输入有效的邮箱地址。', 'error');
      emailInput.focus();
      return;
    }
    button.disabled = true;
    button.textContent = '发送中…';
    message(status, '正在发送邮箱验证码…');
    try {
      const result = await request('/api/account/register/verify/request', { method: 'POST', body: { email } });
      message(status, text(result?.message) || '验证码已发送，请在 30 分钟内完成注册。', 'ok');
      startCooldown(button);
    } catch (error) {
      message(status, text(error.message) || '验证码发送失败，请稍后重试。', 'error');
      button.disabled = false;
      button.textContent = '发送邮箱验证码';
    }
  }

  async function registerAccount(fields) {
    const email = text(fields.email.value);
    const displayName = text(fields.name.value);
    const code = text(fields.code.value);
    const password = String(fields.password.value || '');
    const confirm = String(fields.confirm.value || '');
    if (!validEmail(email)) return message(fields.status, '请输入有效的邮箱地址。', 'error');
    if (displayName.length < 2) return message(fields.status, '昵称至少 2 个字符。', 'error');
    if (!/^\d{6}$/.test(code)) return message(fields.status, '请输入 6 位邮箱验证码。', 'error');
    const passwordError = passwordCheck(password);
    if (passwordError) return message(fields.status, passwordError, 'error');
    if (confirm !== password) return message(fields.status, '两次输入的密码不一致。', 'error');
    if (!fields.agree.checked) return message(fields.status, '请先阅读并同意服务规则与隐私说明。', 'error');

    fields.submit.disabled = true;
    fields.submit.textContent = '正在创建…';
    message(fields.status, '正在创建安全账号会话…');
    try {
      await request('/api/account/register', {
        method: 'POST',
        body: { contactType: 'EMAIL', contactValue: email, displayName, password, code, agree: true }
      });
      message(fields.status, '注册成功，正在进入个人中心…', 'ok');
      location.assign('/next/account');
    } catch (error) {
      message(fields.status, text(error.message) || '注册失败，请检查邮箱、验证码和密码后重试。', 'error');
      fields.submit.disabled = false;
      fields.submit.textContent = '创建账号 →';
    }
  }

  async function requestReset(emailInput, button, confirmWrap, status) {
    const email = text(emailInput.value);
    if (!validEmail(email)) {
      message(status, '请输入有效的邮箱地址。', 'error');
      emailInput.focus();
      return;
    }
    button.disabled = true;
    button.textContent = '发送中…';
    try {
      const result = await request('/api/account/password/reset/request', { method: 'POST', body: { email } });
      confirmWrap.hidden = false;
      message(status, text(result?.message) || '如果该邮箱已绑定账号，我们已发送重置验证码。', 'ok');
      $('#n7ResetCode')?.focus();
    } catch (error) {
      message(status, text(error.message) || '暂时无法发送重置验证码，请稍后再试。', 'error');
    } finally {
      button.disabled = false;
      button.textContent = '重新发送重置验证码';
    }
  }

  async function confirmReset(fields) {
    const email = text(fields.email.value);
    const code = text(fields.code.value);
    const password = String(fields.password.value || '');
    const confirm = String(fields.confirm.value || '');
    if (!validEmail(email)) return message(fields.status, '请输入有效的邮箱地址。', 'error');
    if (!/^\d{6}$/.test(code)) return message(fields.status, '请输入 6 位邮箱验证码。', 'error');
    const passwordError = passwordCheck(password);
    if (passwordError) return message(fields.status, passwordError, 'error');
    if (confirm !== password) return message(fields.status, '两次输入的新密码不一致。', 'error');

    fields.submit.disabled = true;
    fields.submit.textContent = '正在重置…';
    try {
      const result = await request('/api/account/password/reset', { method: 'POST', body: { email, code, newPassword: password } });
      setMode('login');
      const loginMessage = $('#n4LoginMessage');
      message(loginMessage, text(result?.message) || '密码已重置，请使用新密码登录。', 'ok');
      const loginEmail = $('#n4ContactValue');
      if (loginEmail) loginEmail.value = email;
    } catch (error) {
      message(fields.status, text(error.message) || '密码重置失败，请重新获取验证码。', 'error');
    } finally {
      fields.submit.disabled = false;
      fields.submit.textContent = '确认重置 →';
    }
  }

  function mount() {
    const form = $('#n4LoginForm');
    if (!form || form.dataset.n7Installed === '1') return;
    form.dataset.n7Installed = '1';
    installStyles();
    normalizeLoginForm(form);
    const card = form.parentElement;
    if (!card) return;
    createSwitcher(card, form);
    createRegisterPanel(card, form);
    createResetPanel(card);
    const requested = new URLSearchParams(location.search).get('auth');
    setMode(['register', 'reset'].includes(requested) ? requested : 'login');
  }

  function watch() {
    mount();
    if (observer) return;
    observer = new MutationObserver(() => mount());
    observer.observe(document.body, { childList: true, subtree: true });
  }

  window.addEventListener('pagehide', () => {
    observer?.disconnect();
    observer = null;
    if (cooldownTimer) clearInterval(cooldownTimer);
    cooldownTimer = null;
  });

  window.XHFrontendNextN7Auth = {
    show: setMode,
    passwordCheck,
    request
  };
  watch();
})();
