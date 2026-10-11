/* ==========================================================================
   星火 V2 · 账号认证页运行时（account-auth.js）
   --------------------------------------------------------------------------
   仅负责 account-login / account-register / account-reset 页面流程。
   通用请求、安全回跳、错误映射、密码策略与字段工具由 account-core.js 提供。
   ========================================================================== */
(() => {
  'use strict';

  const core = window.XHAccountCore;
  if (!core) {
    console.error('account-auth.js requires account-core.js');
    return;
  }

  const {
    $, $$, setText,
    ERROR_TEXT, errorMessage, readReturnTo, request,
    notify, busy, isCredentialError, accountErrorMessage, startCooldown,
    fieldError, clearFieldErrors, contactRule,
    passwordStrength, applyPasswordMeter,
    bindPasswordToggles, bindCapsLockWarnings, bindSegmentedControl
  } = core;

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
          method: 'POST',
          auth: 'silent',
          body: { contactType, contactValue, password, remember }
        });
        const fallback = returnTo || '/account';
        const target = result && typeof result === 'object' && typeof result.redirect === 'string' && result.redirect
          ? result.redirect
          : fallback;
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
          method: 'POST',
          auth: 'silent',
          body: { contactType, contactValue, displayName, password, code, agree: true }
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
          method: 'POST',
          auth: 'silent',
          body: token ? { token, newPassword } : { email, code, newPassword }
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

  const publicApi = Object.freeze({ request, errorMessage, ERROR_TEXT, passwordStrength });
  window.XHAccountAuth = publicApi;
  window.XHAccount = publicApi;
})();
