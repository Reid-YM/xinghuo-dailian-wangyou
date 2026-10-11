/* ==========================================================================
   星火 V2 · 账号认证页运行时（account-auth.js）
   --------------------------------------------------------------------------
   仅负责：account-login / account-register / account-reset。
   用户中心、设置、订单、进度、投稿继续由 account.js 负责。

   安全约束：
   - 写接口继续携带 X-Requested-With: fetch；
   - 不使用内联事件或第三方依赖；
   - 动态文本只通过 textContent 写入；
   - 登录回跳只允许同源 URL。
   ========================================================================== */
(() => {
  'use strict';

  const XH = window.XH || {};
  const $ = (selector, root = document) => (XH.$ ? XH.$(selector, root) : root.querySelector(selector));
  const $$ = (selector, root = document) => (XH.$$ ? XH.$$(selector, root) : [...root.querySelectorAll(selector)]);

  function el(tag, className, textContent) {
    const node = document.createElement(tag);
    if (className) node.className = className;
    if (textContent !== undefined && textContent !== null) node.textContent = String(textContent);
    return node;
  }

  function clear(node) {
    if (!node) return node;
    while (node.firstChild) node.removeChild(node.firstChild);
    return node;
  }

  function setText(node, value, fallback = '—') {
    if (!node) return;
    const raw = value === undefined || value === null || value === '' ? fallback : value;
    node.textContent = String(raw);
  }

  const ERROR_TEXT = {
    UNAUTHORIZED: '登录状态已失效，请重新登录。',
    UNAUTHENTICATED: '登录状态已失效，请重新登录。',
    FORBIDDEN: '没有权限执行该操作。',
    CSRF_HEADER_MISSING: '请求被安全策略拒绝，请刷新页面后重试。',
    INVALID_INPUT: '提交的内容有误，请检查后重试。',
    INTERNAL_ERROR: '服务暂时不可用，请稍后重试。',
    INVALID_CREDENTIALS: '联系方式或密码不正确。',
    ACCOUNT_NOT_FOUND: '账号不存在，请先注册。',
    ACCOUNT_DISABLED: '该账号已被停用，请联系客服。',
    ACCOUNT_LOCKED: '尝试次数过多，账号已临时锁定，请稍后再试。',
    CONTACT_TAKEN: '该联系方式已注册，请直接登录或找回密码。',
    CONTACT_EXISTS: '该联系方式已注册，请直接登录或找回密码。',
    CONTACT_INVALID: '联系方式格式不正确，请检查后重试。',
    WEAK_PASSWORD: '密码强度不足：至少 8 位，且需包含字母、数字、符号中的两类。',
    PASSWORD_INVALID: '密码强度不足：至少 8 位，且需包含字母、数字、符号中的两类。',
    PASSWORD_MISMATCH: '两次输入的密码不一致。',
    EMAIL_TAKEN: '该邮箱已被其他账号绑定。',
    EMAIL_INVALID: '邮箱格式不正确。',
    EMAIL_NOT_VERIFIED: '邮箱尚未验证，请先完成验证。',
    TOKEN_INVALID: '链接无效或已失效，请重新发起。',
    TOKEN_EXPIRED: '链接已过期，请重新发起。',
    RATE_LIMITED: '操作过于频繁，请稍后再试。',
    AGREE_REQUIRED: '请先阅读并同意服务规则与隐私说明。',
    VALIDATION_FAILED: '提交的内容有误，请检查后重试。',
    NETWORK_ERROR: '网络连接失败，请检查网络后重试。',
    SERVER_ERROR: '服务暂时不可用，请稍后重试。',
    INVALID_JSON: '服务返回内容异常，请稍后重试。',
    CODE_INVALID: '验证码无效或已过期，请重新发送。',
    VERIFY_CODE_INVALID: '验证码无效或已过期，请重新发送。',
    EMAIL_ONLY: '当前仅支持邮箱注册和邮箱登录。',
    INVALID_DISPLAY_NAME: '请填写昵称。'
  };

  function errorMessage(code, fallback) {
    if (fallback) return fallback;
    return ERROR_TEXT[code] || '请求失败，请稍后重试。';
  }

  class ApiError extends Error {
    constructor(code, message, status) {
      super(message);
      this.name = 'ApiError';
      this.code = code || 'UNKNOWN';
      this.status = status || 0;
    }
  }

  function redirectToLogin() {
    const returnTo = location.pathname + location.search;
    const target = returnTo && returnTo.indexOf('/account/login') !== 0
      ? `/account/login?next=${encodeURIComponent(returnTo)}`
      : '/account/login';
    location.replace(target);
  }

  function readReturnTo(params) {
    const raw = params.get('next') || params.get('redirect') || '';
    if (!raw) return '';
    let url;
    try {
      url = new URL(raw, location.origin);
    } catch (ignored) {
      return '';
    }
    if (url.origin !== location.origin) return '';
    const target = `${url.pathname}${url.search}${url.hash}`;
    return target.indexOf('/account/login') === 0 ? '' : target;
  }

  async function request(path, options = {}) {
    const method = (options.method || 'GET').toUpperCase();
    const headers = Object.assign(
      { Accept: 'application/json' },
      { 'X-Requested-With': 'fetch' },
      options.headers || {}
    );
    const init = { method, headers, credentials: 'same-origin', cache: 'no-store' };
    if (options.body !== undefined && options.body !== null && method !== 'GET') {
      headers['Content-Type'] = 'application/json';
      init.body = JSON.stringify(options.body);
    }

    let response;
    try {
      response = await fetch(path, init);
    } catch (err) {
      throw new ApiError('NETWORK_ERROR', ERROR_TEXT.NETWORK_ERROR, 0);
    }

    let payload = null;
    const contentType = response.headers.get('content-type') || '';
    if (response.status !== 204) {
      try {
        payload = contentType.includes('application/json') ? await response.json() : await response.text();
      } catch (err) {
        payload = null;
      }
    }
    if (response.ok) return payload;

    const code = payload && typeof payload === 'object' && payload.error
      ? String(payload.error)
      : (response.status === 401 ? 'UNAUTHENTICATED' : (response.status >= 500 ? 'INTERNAL_ERROR' : 'UNKNOWN'));
    const message = payload && typeof payload === 'object' && payload.message
      ? String(payload.message)
      : errorMessage(code);
    if (response.status === 401 && options.auth !== 'silent') redirectToLogin();
    throw new ApiError(code, message, response.status);
  }

  function notify(host, message, type, options) {
    if (!host) return;
    if (!message && options && options.silent) return;
    clear(host);
    if (!message) {
      host.hidden = true;
      return;
    }
    const box = el('div', `notice${type === 'error' ? ' error' : ''}`, message);
    box.setAttribute('role', type === 'error' ? 'alert' : 'status');
    host.appendChild(box);
    host.hidden = false;
  }

  function busy(button, isBusy, label) {
    if (!button) return;
    if (XH.setBusy) {
      XH.setBusy(button, isBusy, label || '处理中…');
      return;
    }
    if (isBusy) {
      button.dataset.label = button.textContent;
      button.textContent = label || '处理中…';
      button.disabled = true;
    } else {
      button.textContent = button.dataset.label || button.textContent;
      button.disabled = false;
    }
  }

  function isCredentialError(err) {
    return !!err && (err.status === 401 || err.code === 'INVALID_CREDENTIALS' || err.code === 'UNAUTHORIZED');
  }

  function accountErrorMessage(err, fallback) {
    if (!err) return fallback || errorMessage(null);
    if (err.code && ERROR_TEXT[err.code]) return ERROR_TEXT[err.code];
    if (err.status === 400 && err.message) return err.message;
    if (err.status === 409) return err.message || ERROR_TEXT.CONTACT_TAKEN;
    if (err.status === 429) return ERROR_TEXT.RATE_LIMITED;
    return err.message || fallback || errorMessage(null);
  }

  function startCooldown(button, seconds = 60, label = '重新发送') {
    if (!button) return;
    const original = button.dataset.cooldownLabel || button.dataset.label || button.textContent || label;
    button.dataset.cooldownLabel = original;
    let left = Math.max(1, Number(seconds) || 60);
    const render = () => {
      button.disabled = true;
      button.setAttribute('aria-disabled', 'true');
      button.textContent = `${label} ${left}s`;
    };
    window.clearInterval(Number(button.dataset.cooldownTimer || 0));
    render();
    const timer = window.setInterval(() => {
      left -= 1;
      if (left <= 0) {
        window.clearInterval(timer);
        button.disabled = false;
        button.removeAttribute('aria-disabled');
        button.textContent = original;
        button.dataset.cooldownTimer = '';
        return;
      }
      render();
    }, 1000);
    button.dataset.cooldownTimer = String(timer);
  }

  function fieldError(input, message) {
    if (!input) return;
    const holder = input.closest('.field') || input.parentElement;
    input.setAttribute('aria-invalid', message ? 'true' : 'false');
    if (!holder) return;
    let box = $('[data-field-error]', holder);
    if (!message) {
      if (box) box.hidden = true;
      return;
    }
    if (!box) {
      box = el('small', 'field-error');
      box.setAttribute('data-field-error', '');
      holder.appendChild(box);
    }
    box.textContent = message;
    box.hidden = false;
  }

  function clearFieldErrors(form) {
    $$('[data-field-error]', form).forEach((node) => { node.hidden = true; });
    $$('[aria-invalid="true"]', form).forEach((node) => node.setAttribute('aria-invalid', 'false'));
  }

  const CONTACT_RULES = {
    EMAIL: {
      label: '邮箱',
      placeholder: 'you@example.com',
      hint: '请输入可接收邮件的邮箱，用于登录、验证和找回密码。',
      test: (value) => /^[^@\s]+@[^@\s.]+(\.[^@\s.]+)+$/.test(value),
      error: '请输入有效的邮箱地址。'
    }
  };

  function contactRule(type) {
    return CONTACT_RULES[type] || CONTACT_RULES.EMAIL;
  }

  function utf8Bytes(value) {
    const text = String(value || '');
    if (typeof TextEncoder === 'function') return new TextEncoder().encode(text).length;
    return unescape(encodeURIComponent(text)).length;
  }

  function passwordStrength(value) {
    const text = String(value || '');
    if (!text) return { valid: false, score: 0, classes: 0, length: 0, bytes: 0, label: '等待输入' };
    const classes = [
      /[a-z]/.test(text),
      /[A-Z]/.test(text),
      /[0-9]/.test(text),
      /[^A-Za-z0-9]/.test(text)
    ].filter(Boolean).length;
    const length = text.length;
    const bytes = utf8Bytes(text);
    if (length < 8) return { valid: false, score: 1, classes, length, bytes, label: `还差 ${8 - length} 位（至少 8 位）` };
    if (bytes > 72) return { valid: false, score: 2, classes, length, bytes, label: '密码过长（中文约 24 个字符以内）' };
    if (classes < 2) return { valid: false, score: 2, classes, length, bytes, label: '需包含小写 / 大写 / 数字 / 符号中的至少两类' };
    let score = 2;
    if (length >= 10 && classes >= 2) score = 3;
    if (length >= 12 && classes >= 3) score = 4;
    return { valid: true, score, classes, length, bytes, label: score >= 4 ? '强' : (score >= 3 ? '中上' : '可用') };
  }

  function applyPasswordMeter(meter, value) {
    if (!meter) return;
    const result = passwordStrength(value);
    meter.setAttribute('data-score', String(result.score));
    const label = $('span', meter);
    if (label) label.textContent = `密码强度：${result.label}`;
  }

  function bindPasswordToggles(root) {
    $$('[data-toggle-password]', root).forEach((button) => {
      button.addEventListener('click', () => {
        const field = button.closest('.password-field');
        const input = field ? $('input', field) : null;
        if (!input) return;
        const revealing = input.type === 'password';
        input.type = revealing ? 'text' : 'password';
        button.textContent = revealing ? '隐藏' : '显示';
        button.setAttribute('aria-label', revealing ? '隐藏密码' : '显示密码');
        button.setAttribute('aria-pressed', revealing ? 'true' : 'false');
      });
    });
  }

  function bindCapsLockWarnings(root) {
    $$('input[type="password"]', root || document).forEach((input) => {
      const field = input.closest('.field, .password-field');
      if (!field) return;
      const warning = el('small', 'caps-lock-warning', '大写锁定已开启');
      warning.hidden = true;
      warning.setAttribute('role', 'status');
      field.insertAdjacentElement('afterend', warning);
      const sync = (event) => { warning.hidden = !(event.getModifierState && event.getModifierState('CapsLock')); };
      input.addEventListener('keydown', sync);
      input.addEventListener('keyup', sync);
      input.addEventListener('blur', () => { warning.hidden = true; });
    });
  }

  function bindSegmentedControl(root, onSelect) {
    const scope = root || document;
    const segments = $$('[data-contact-type-button]', scope);
    if (!segments.length) return;
    function activate(button, focus) {
      const type = button.dataset.contactTypeButton;
      segments.forEach((item) => {
        const active = item === button;
        item.classList.toggle('active', active);
        item.setAttribute('aria-selected', active ? 'true' : 'false');
        item.tabIndex = active ? 0 : -1;
      });
      if (focus) button.focus();
      const holder = button.closest('form') || scope;
      const input = $('[data-contact-type]', holder);
      if (input) input.value = type;
      if (typeof onSelect === 'function') onSelect(type, holder);
    }
    segments.forEach((button, index) => {
      button.addEventListener('click', () => activate(button, false));
      button.addEventListener('keydown', (event) => {
        const keys = ['ArrowRight', 'ArrowDown', 'ArrowLeft', 'ArrowUp', 'Home', 'End'];
        if (!keys.includes(event.key)) return;
        event.preventDefault();
        let next = index;
        if (event.key === 'ArrowRight' || event.key === 'ArrowDown') next = (index + 1) % segments.length;
        if (event.key === 'ArrowLeft' || event.key === 'ArrowUp') next = (index - 1 + segments.length) % segments.length;
        if (event.key === 'Home') next = 0;
        if (event.key === 'End') next = segments.length - 1;
        activate(segments[next], true);
      });
    });
  }

  function initLoginPage() {
    const form = $('#accountLoginForm');
    if (!form) return;
    const msg = $('#accountMsg');
    const submit = $('[data-submit]', form);
    const params = new URLSearchParams(location.search);
    const redirectInput = $('[name="redirect"]', form);
    const typeInput = $('[data-contact-type]', form);
    const valueInput = $('[name="contactValue"]', form);
    const valueError = $('#loginContactError');
    const returnTo = readReturnTo(params);
    if (redirectInput && returnTo) redirectInput.value = returnTo;
    if (params.get('reset') === '1') notify(msg, '密码已重置，请用新密码登录。', 'success');
    if (params.get('password') === 'changed') notify(msg, '密码已更新，所有设备需要重新登录。', 'success');
    if (params.get('email') === 'verified') notify(msg, '邮箱验证成功。', 'success');

    const hint = $('[data-login-contact-hint]');
    const applyType = (type) => {
      const rule = contactRule(type);
      if (valueInput) {
        valueInput.placeholder = rule.placeholder;
        valueInput.inputMode = 'email';
      }
      setText(hint, rule.hint, rule.hint);
      fieldError(valueInput, '');
      if (valueError) setText(valueError, rule.hint, rule.hint);
    };
    bindSegmentedControl(form, (type) => applyType(type));
    applyType((typeInput && typeInput.value) || 'EMAIL');

    form.addEventListener('submit', async (event) => {
      event.preventDefault();
      clearFieldErrors(form);
      notify(msg, '');
      const data = new FormData(form);
      const contactType = String(data.get('contactType') || 'EMAIL');
      const contactValue = String(data.get('contactValue') || '').trim();
      const password = String(data.get('password') || '');
      const remember = data.get('remember') === 'on';
      if (!contactValue) {
        fieldError(valueInput, `请输入${contactRule(contactType).label}。`);
        if (valueError) setText(valueError, `请输入${contactRule(contactType).label}。`);
        if (valueInput) valueInput.focus();
        return;
      }
      if (!password) {
        notify(msg, '请输入登录密码。', 'error');
        return;
      }
      busy(submit, true, '登录中…');
      try {
        const result = await request('/api/account/login', {
          method: 'POST', auth: 'silent', body: { contactType, contactValue, password, remember }
        });
        const fallback = returnTo || '/account';
        const target = result && typeof result === 'object' && typeof result.redirect === 'string' && result.redirect
          ? result.redirect : fallback;
        location.assign(target);
      } catch (err) {
        if (isCredentialError(err)) {
          fieldError($('[name="password"]', form), ERROR_TEXT.INVALID_CREDENTIALS);
          notify(msg, ERROR_TEXT.INVALID_CREDENTIALS, 'error');
        } else {
          notify(msg, accountErrorMessage(err), 'error');
        }
        busy(submit, false);
      }
    });

    const resetForm = $('#accountResetRequestForm');
    if (!resetForm) return;
    const resetMsg = $('#accountResetMsg');
    const resetSubmit = $('[data-submit]', resetForm);
    resetForm.addEventListener('submit', async (event) => {
      event.preventDefault();
      clearFieldErrors(resetForm);
      notify(resetMsg, '');
      const emailInput = $('[name="email"]', resetForm);
      const email = String(new FormData(resetForm).get('email') || '').trim();
      if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
        fieldError(emailInput, '请输入有效的邮箱地址。');
        return;
      }
      busy(resetSubmit, true, '提交中…');
      let sent = false;
      try {
        await request('/api/account/password/reset/request', { method: 'POST', auth: 'silent', body: { email } });
        sent = true;
        notify(resetMsg, '如果该邮箱已绑定账号，我们已发送重置验证码（30 分钟内有效）。', 'success');
        startCooldown(resetSubmit, 60, '重新发送');
        window.setTimeout(() => location.assign(`/account/reset?email=${encodeURIComponent(email)}`), 1200);
      } catch (err) {
        notify(resetMsg, accountErrorMessage(err), 'error');
      } finally {
        if (!sent) busy(resetSubmit, false);
      }
    });
  }

  function initRegisterPage() {
    const form = $('#accountRegisterForm');
    if (!form) return;
    const msg = $('#accountMsg');
    const submit = $('[data-submit]', form);
    const typeInput = $('[data-contact-type]', form);
    const valueInput = $('[name="contactValue"]', form);
    const valueLabel = $('[data-contact-label]');
    const valueHint = $('[data-contact-hint]');
    const meter = $('[data-password-meter]');
    const passwordInput = $('[name="password"]', form);
    const confirmInput = $('[name="passwordConfirm"]', form);
    const codeInput = $('[name="code"]', form);
    const codeButton = $('[data-register-code]', form);
    const agreeInput = $('[name="agree"]', form);
    const segments = $$('[data-contact-type-button]', form);

    function selectType(type) {
      const rule = contactRule(type);
      if (typeInput) typeInput.value = type;
      segments.forEach((button) => {
        const active = button.dataset.contactTypeButton === type;
        button.classList.toggle('active', active);
        button.setAttribute('aria-selected', active ? 'true' : 'false');
        button.tabIndex = active ? 0 : -1;
      });
      if (valueInput) {
        valueInput.placeholder = rule.placeholder;
        valueInput.inputMode = 'email';
        valueInput.autocomplete = 'email';
        if (valueInput.value) validateContact(false);
      }
      setText(valueLabel, rule.label, rule.label);
      setText(valueHint, rule.hint, rule.hint);
      fieldError(valueInput, '');
    }

    function validateContact(showError) {
      const type = (typeInput && typeInput.value) || 'EMAIL';
      const rule = contactRule(type);
      const value = valueInput ? valueInput.value.trim() : '';
      let error = '';
      if (!value) error = `请输入${rule.label}。`;
      else if (!rule.test(value)) error = rule.error;
      if (showError) fieldError(valueInput, error);
      else if (!error) fieldError(valueInput, '');
      return !error;
    }

    bindSegmentedControl(form, (type) => selectType(type));
    selectType((typeInput && typeInput.value) || 'EMAIL');
    if (passwordInput && meter) {
      applyPasswordMeter(meter, '');
      passwordInput.addEventListener('input', () => applyPasswordMeter(meter, passwordInput.value));
    }
    if (valueInput) {
      valueInput.addEventListener('blur', () => validateContact(true));
      valueInput.addEventListener('input', () => {
        if (valueInput.getAttribute('aria-invalid') === 'true') validateContact(true);
      });
    }

    if (codeButton) {
      codeButton.addEventListener('click', async () => {
        clearFieldErrors(form);
        notify(msg, '');
        if (!validateContact(true)) {
          if (valueInput) valueInput.focus();
          return;
        }
        const email = valueInput ? valueInput.value.trim() : '';
        busy(codeButton, true, '发送中…');
        let sent = false;
        try {
          await request('/api/account/register/verify/request', { method: 'POST', auth: 'silent', body: { email } });
          sent = true;
          notify(msg, `验证码已发送至 ${email}，请在 30 分钟内完成注册。`, 'success');
          startCooldown(codeButton, 60, '重新发送');
          if (codeInput) codeInput.focus();
        } catch (err) {
          const message = accountErrorMessage(err, '验证码发送失败，请稍后重试。');
          if (err && (err.code === 'CONTACT_TAKEN' || err.code === 'EMAIL_TAKEN')) fieldError(valueInput, message);
          notify(msg, message, 'error');
        } finally {
          if (!sent) busy(codeButton, false);
        }
      });
    }

    form.addEventListener('submit', async (event) => {
      event.preventDefault();
      clearFieldErrors(form);
      notify(msg, '');
      const data = new FormData(form);
      const displayName = String(data.get('displayName') || '').trim();
      const contactType = (typeInput && typeInput.value) || 'EMAIL';
      const contactValue = String(data.get('contactValue') || '').trim();
      const password = String(data.get('password') || '');
      const confirm = String(data.get('passwordConfirm') || '');
      const code = String(data.get('code') || '').trim();
      const agree = data.get('agree') === 'on';
      const displayInput = $('[name="displayName"]', form);
      let firstInvalid = null;
      if (displayName.length < 2) {
        fieldError(displayInput, '昵称至少 2 个字符。');
        firstInvalid = firstInvalid || displayInput;
      }
      if (!validateContact(true)) firstInvalid = firstInvalid || valueInput;
      const strength = passwordStrength(password);
      if (!strength.valid) {
        fieldError(passwordInput, strength.label === '等待输入' ? '请设置登录密码。' : strength.label);
        firstInvalid = firstInvalid || passwordInput;
      }
      if (confirm !== password) {
        fieldError(confirmInput, '两次输入的密码不一致。');
        firstInvalid = firstInvalid || confirmInput;
      }
      if (!/^\d{6}$/.test(code)) {
        fieldError(codeInput, '请输入 6 位邮箱验证码。');
        firstInvalid = firstInvalid || codeInput;
      }
      if (firstInvalid) {
        firstInvalid.focus();
        return;
      }
      if (!agree) {
        notify(msg, errorMessage('AGREE_REQUIRED'), 'error');
        if (agreeInput) agreeInput.focus();
        return;
      }
      busy(submit, true, '创建中…');
      try {
        await request('/api/account/register', {
          method: 'POST', auth: 'silent', body: { contactType, contactValue, displayName, password, code, agree: true }
        });
        location.assign('/account');
      } catch (err) {
        const message = accountErrorMessage(err, '注册失败，请检查邮箱、验证码和密码后重试。');
        if (err && (err.code === 'CONTACT_TAKEN' || err.code === 'CONTACT_EXISTS' || err.code === 'EMAIL_TAKEN')) {
          fieldError(valueInput, message);
        } else if (err && (err.code === 'CODE_INVALID' || err.code === 'VERIFY_CODE_INVALID')) {
          fieldError(codeInput, message);
        } else if (err && (err.code === 'WEAK_PASSWORD' || err.code === 'PASSWORD_INVALID')) {
          fieldError(passwordInput, message);
        }
        notify(msg, message, 'error');
        busy(submit, false);
      }
    });
  }

  function initResetPage() {
    const form = $('#accountResetConfirmForm');
    if (!form) return;
    const msg = $('#accountResetConfirmMsg');
    const submit = $('[data-submit]', form);
    const emailInput = $('[name="email"]', form);
    const codeInput = $('[name="code"]', form);
    const newInput = $('[name="newPassword"]', form);
    const confirmInput = $('[name="confirmPassword"]', form);
    const params = new URLSearchParams(location.search);
    const token = params.get('token') || '';
    const presetEmail = params.get('email') || '';
    if (emailInput && presetEmail) emailInput.value = presetEmail;

    form.addEventListener('submit', async (event) => {
      event.preventDefault();
      clearFieldErrors(form);
      notify(msg, '');
      const data = new FormData(form);
      const email = String(data.get('email') || '').trim();
      const code = String(data.get('code') || '').trim();
      const newPassword = String(data.get('newPassword') || '');
      const confirm = String(data.get('confirmPassword') || '');
      let firstInvalid = null;
      if (!token && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
        fieldError(emailInput, '请输入绑定邮箱。');
        firstInvalid = firstInvalid || emailInput;
      }
      if (!token && !/^\d{6}$/.test(code)) {
        fieldError(codeInput, '请输入 6 位邮箱验证码。');
        firstInvalid = firstInvalid || codeInput;
      }
      const strength = passwordStrength(newPassword);
      if (!strength.valid) {
        fieldError(newInput, strength.label === '等待输入' ? '请输入新密码。' : strength.label);
        firstInvalid = firstInvalid || newInput;
      }
      if (confirm !== newPassword) {
        fieldError(confirmInput, '两次输入的新密码不一致。');
        firstInvalid = firstInvalid || confirmInput;
      }
      if (firstInvalid) {
        firstInvalid.focus();
        return;
      }
      busy(submit, true, '提交中…');
      try {
        await request('/api/account/password/reset', {
          method: 'POST', auth: 'silent', body: token ? { token, newPassword } : { email, code, newPassword }
        });
        form.reset();
        notify(msg, '密码已重置，正在前往登录页…', 'success');
        window.setTimeout(() => location.assign('/account/login?reset=1'), 900);
      } catch (err) {
        notify(msg, err && err.message ? err.message : '重置失败，请重新发起。', 'error');
      } finally {
        busy(submit, false);
      }
    });
  }

  function boot() {
    bindPasswordToggles(document);
    bindCapsLockWarnings(document);
    switch (document.body.dataset.page) {
      case 'account-login': initLoginPage(); break;
      case 'account-register': initRegisterPage(); break;
      case 'account-reset': initResetPage(); break;
      default: break;
    }
  }

  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', boot, { once: true });
  else boot();

  const publicApi = { request, errorMessage, ERROR_TEXT, passwordStrength };
  window.XHAccountAuth = publicApi;
  window.XHAccount = publicApi;
})();
