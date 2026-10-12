/* ==========================================================================
   星火 V2 · 账号设置运行时（account-settings.js）
   --------------------------------------------------------------------------
   负责 account-settings 中除个人资料表单外的设置职责：
   - 账号只读信息 / 安全概览；
   - 询价回访联系方式；
   - 修改密码；
   - 安全邮箱验证；
   - 在线设备与安全活动；
   - 当前会话退出。

   个人资料（头像 / 昵称）由 account-profile-controller.js 单独负责；
   设置分区导航由 account-center.js 负责；
   通用请求与安全契约由 account-core.js 提供。
   ========================================================================== */
(() => {
  'use strict';

  const core = window.XHAccountCore;
  if (!core) {
    console.error('account-settings.js requires account-core.js');
    return;
  }

  const {
    $, $$, el, clear, setText,
    errorMessage, request,
    notify, busy, accountErrorMessage, startCooldown,
    fieldError, clearFieldErrors,
    passwordStrength, applyPasswordMeter,
    bindPasswordToggles, bindCapsLockWarnings
  } = core;

  function fmtDate(value) {
    if (!value) return '—';
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return String(value);
    return date.toLocaleString('zh-CN', {
      hour12: false,
      year: 'numeric', month: '2-digit', day: '2-digit',
      hour: '2-digit', minute: '2-digit'
    });
  }

  function fmtRelative(value) {
    if (!value) return '—';
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return String(value);
    const diff = Date.now() - date.getTime();
    if (diff < 0) return '刚刚';
    const min = Math.floor(diff / 60000);
    if (min < 1) return '刚刚';
    if (min < 60) return `${min} 分钟前`;
    const hour = Math.floor(min / 60);
    if (hour < 24) return `${hour} 小时前`;
    const day = Math.floor(hour / 24);
    if (day < 30) return `${day} 天前`;
    return fmtDate(value);
  }

  function renderEmpty(host, message) {
    if (!host) return;
    clear(host);
    const box = el('div', 'empty');
    box.appendChild(el('p', null, message));
    host.appendChild(box);
  }

  function renderSkeleton(host, count = 2) {
    if (!host) return;
    clear(host);
    const shell = el('div', 'account-skeleton');
    for (let i = 0; i < count; i += 1) shell.appendChild(el('span'));
    host.appendChild(shell);
  }

  function confirmAction(title, message, confirmText = '确认操作') {
    return new Promise((resolve) => {
      const dialog = el('dialog', 'account-confirm');
      const panel = el('div', 'account-confirm-panel');
      const eyebrow = el('span', 'eyebrow', '请确认');
      const heading = el('h2', null, title);
      const copy = el('p', null, message);
      const actions = el('div', 'account-confirm-actions');
      const cancel = el('button', 'btn ghost', '取消');
      const confirm = el('button', 'btn danger', confirmText);
      cancel.type = 'button';
      confirm.type = 'button';
      actions.append(cancel, confirm);
      panel.append(eyebrow, heading, copy, actions);
      dialog.appendChild(panel);
      document.body.appendChild(dialog);

      let settled = false;
      const finish = (value) => {
        if (settled) return;
        settled = true;
        dialog.close();
        dialog.remove();
        resolve(value);
      };
      cancel.addEventListener('click', () => finish(false));
      confirm.addEventListener('click', () => finish(true));
      dialog.addEventListener('cancel', (event) => {
        event.preventDefault();
        finish(false);
      });
      dialog.addEventListener('click', (event) => {
        if (event.target === dialog) finish(false);
      });
      dialog.showModal();
      cancel.focus();
    });
  }

  function show(node, visible) {
    if (!node) return;
    node.hidden = !visible;
  }

  function initSettingsPage() {
    const root = $('#accountSettings');
    if (!root) return;

    const params = new URLSearchParams(location.search);
    const meMsg = $('#settingsMsg');
    const QUOTE_CONTACT_TYPES = Object.freeze({ PHONE: '手机号', WECHAT: '微信号', QQ: 'QQ 号' });

    function contactTypeText(type) {
      return ({ EMAIL: '邮箱' })[type] || '邮箱';
    }

    function renderSecurityScore(me) {
      const score = $('[data-security-score]');
      const value = $('[data-security-score-value]');
      const next = $('[data-security-score-next]');
      if (!score) return;
      const completed = 1 + (me && me.contactVerified ? 1 : 0) + (me && me.emailVerified ? 1 : 0);
      score.dataset.level = completed === 3 ? 'strong' : (completed === 2 ? 'medium' : 'basic');
      setText(value, `${completed} / 3`);
      if (!me || !me.email) setText(next, '下一步：绑定并验证安全邮箱');
      else if (!me.emailVerified) setText(next, '下一步：打开邮件完成邮箱验证');
      else if (!me.contactVerified) setText(next, '下一步：联系客服核验登录联系方式');
      else setText(next, '关键保护项已完成');
    }

    function renderQuoteContactRows(host, contacts, editable) {
      if (!host) return;
      clear(host);
      if (!contacts.length) {
        renderEmpty(host, editable
          ? '还没有绑定询价联系方式。未登录或未绑定时，提交询价需要手动填写联系方式。'
          : '还没有绑定询价联系方式，提交询价时需要手动填写。可到「安全设置」里添加。');
        return;
      }
      contacts.forEach((item, index) => {
        const row = el('div', 'quote-contact-row');
        const meta = el('div', 'quote-contact-meta');
        meta.appendChild(el('span', null, QUOTE_CONTACT_TYPES[item.type] || item.type || '联系方式'));
        meta.appendChild(el('strong', null, item.masked || '—'));
        row.appendChild(meta);
        if (editable) {
          const remove = el('button', 'btn ghost small-btn', '删除');
          remove.type = 'button';
          remove.setAttribute('data-quote-contact-remove', String(index));
          row.appendChild(remove);
        }
        host.appendChild(row);
      });
    }

    function renderQuoteContacts(list) {
      const contacts = Array.isArray(list) ? list : [];
      $$('[data-settings-quote-contact]').forEach((node) => setText(node,
        contacts.length ? `已绑定 ${contacts.length} 条` : '未绑定'));
      renderQuoteContactRows($('#quoteContactList'), contacts, true);
      renderQuoteContactRows($('#consoleQuoteContactList'), contacts, false);
    }

    async function loadMe() {
      try {
        const me = await request('/api/account/me');
        $$('[data-settings-contact]').forEach((node) => setText(node, `${contactTypeText(me.contactType)} ${me.contactMasked || '—'}`));
        renderQuoteContacts(me.quoteContacts);
        $$('[data-settings-created]').forEach((node) => setText(node, fmtDate(me.createdAt)));
        $$('[data-settings-email]').forEach((node) => setText(node, me.email || '未绑定邮箱'));
        $$('[data-settings-email-state]').forEach((emailState) => {
          clear(emailState);
          if (!me.email) emailState.appendChild(el('span', 'badge', '未绑定'));
          else if (me.emailVerified) emailState.appendChild(el('span', 'badge ok', '已验证'));
          else emailState.appendChild(el('span', 'badge warn', '待验证'));
        });
        $$('[data-security-contact]').forEach((node) => {
          setText(node, me.contactVerified ? '已验证' : '待核验');
          node.classList.toggle('is-ok', !!me.contactVerified);
          node.classList.toggle('is-warn', !me.contactVerified);
        });
        $$('[data-security-email]').forEach((node) => {
          setText(node, me.email ? (me.emailVerified ? '已验证' : '待验证') : '未绑定');
          node.classList.toggle('is-ok', !!me.emailVerified);
          node.classList.toggle('is-warn', !me.emailVerified);
        });
        renderSecurityScore(me);
        const resend = $('[data-resend-verify]');
        if (resend) resend.disabled = !me.email || !!me.emailVerified;
        const emailInput = $('#settingsEmail');
        if (emailInput && me.email) emailInput.placeholder = me.email;
        return me;
      } catch (err) {
        if (err && err.status !== 401) {
          notify(meMsg, err && err.message ? err.message : '账号信息加载失败。', 'error');
        }
        return null;
      }
    }

    async function handleVerifyToken(token) {
      const host = $('[data-email-verify-result]');
      if (!host) return;
      notify(host, '正在验证邮箱…');
      try {
        await request(`/api/account/email/verify?token=${encodeURIComponent(token)}`, { auth: 'silent' });
        notify(host, '邮箱验证成功，后续可用邮箱找回密码。', 'success');
      } catch (err) {
        notify(host, err && err.message ? err.message : errorMessage('TOKEN_INVALID'), 'error');
      }
    }

    function initPasswordForm() {
      const form = $('#accountPasswordForm');
      if (!form) return;
      const msg = $('[data-password-msg]');
      const submit = $('[data-submit]', form);
      const oldInput = $('[name="oldPassword"]', form);
      const newInput = $('[name="newPassword"]', form);
      const confirmInput = $('[name="newPasswordConfirm"]', form);
      const meter = $('[data-password-meter]', form);

      if (newInput && meter) {
        applyPasswordMeter(meter, '');
        newInput.addEventListener('input', () => applyPasswordMeter(meter, newInput.value));
      }

      form.addEventListener('submit', async (event) => {
        event.preventDefault();
        clearFieldErrors(form);
        notify(msg, '');
        const data = new FormData(form);
        const oldPassword = String(data.get('oldPassword') || '');
        const newPassword = String(data.get('newPassword') || '');
        const confirm = String(data.get('newPasswordConfirm') || '');

        let firstInvalid = null;
        if (!oldPassword) {
          fieldError(oldInput, '请输入当前密码。');
          firstInvalid = firstInvalid || oldInput;
        }
        const strength = passwordStrength(newPassword);
        if (!strength.valid) {
          fieldError(newInput, strength.label === '等待输入' ? '请输入新密码。' : strength.label);
          firstInvalid = firstInvalid || newInput;
        } else if (newPassword === oldPassword) {
          fieldError(newInput, '新密码不能与当前密码相同。');
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
          await request('/api/account/password/change', { method: 'POST', body: { oldPassword, newPassword } });
          form.reset();
          applyPasswordMeter(meter, '');
          location.assign('/account/login?password=changed');
        } catch (err) {
          notify(msg, err && err.message ? err.message : '密码修改失败，请稍后重试。', 'error');
        } finally {
          busy(submit, false);
        }
      });
    }

    function renderSecurityEvents(items) {
      const host = $('[data-security-events]');
      if (!host) return;
      clear(host);
      if (!items.length) {
        renderEmpty(host, '暂时没有可展示的安全活动。');
        return;
      }
      const list = el('ol', 'account-activity-list');
      items.forEach((item) => {
        const row = el('li', 'account-activity');
        const marker = el('span', 'account-activity-marker');
        marker.setAttribute('aria-hidden', 'true');
        const copy = el('div', 'account-activity-copy');
        copy.appendChild(el('strong', null, item.title || '账号安全活动'));
        copy.appendChild(el('p', null, item.description || '该操作已写入账号安全记录。'));
        const meta = el('div', 'account-activity-meta');
        meta.appendChild(el('span', null, item.deviceLabel || '未知设备'));
        meta.appendChild(el('span', 'mono', item.ipMasked || '未知网络'));
        meta.appendChild(el('time', null, fmtDate(item.createdAt)));
        copy.appendChild(meta);
        row.append(marker, copy);
        list.appendChild(row);
      });
      host.appendChild(list);
    }

    async function loadSecurityEvents() {
      const host = $('[data-security-events]');
      const msg = $('#securityEventsMsg');
      if (!host) return;
      notify(msg, '');
      renderSkeleton(host, 3);
      try {
        const items = await request('/api/account/security/events');
        renderSecurityEvents(Array.isArray(items) ? items : []);
      } catch (err) {
        renderEmpty(host, err && err.message ? err.message : '安全活动加载失败，请稍后重试。');
        notify(msg, err && err.message ? err.message : '安全活动加载失败，请稍后重试。', 'error');
      }
    }

    function initEmailForm() {
      const form = $('#accountEmailForm');
      if (!form) return;
      const msg = $('[data-email-msg]');
      const submit = $('[data-submit]', form);
      const emailInput = $('[name="email"]', form);
      const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

      async function sendVerify(target, button, prefix) {
        busy(button, true, '发送中…');
        let sent = false;
        try {
          await request('/api/account/email/verify/request', { method: 'POST', body: { email: target } });
          sent = true;
          notify(msg, `${prefix}${target}，请在 30 分钟内输入邮件里的 6 位验证码完成验证。`, 'success');
          startCooldown(button, 60, '重新发送');
          return true;
        } catch (err) {
          notify(msg, accountErrorMessage(err, '验证邮件发送失败，请稍后重试。'), 'error');
          return false;
        } finally {
          if (!sent) busy(button, false);
        }
      }

      form.addEventListener('submit', async (event) => {
        event.preventDefault();
        clearFieldErrors(form);
        notify(msg, '');
        const email = String(new FormData(form).get('email') || '').trim();
        if (!EMAIL_RE.test(email)) {
          fieldError(emailInput, '请输入有效的邮箱地址。');
          emailInput && emailInput.focus();
          return;
        }
        await sendVerify(email, submit, '验证邮件已发送至 ');
      });

      const codeForm = $('#accountEmailCodeForm');
      if (codeForm) {
        const codeSubmit = $('[data-submit]', codeForm);
        const codeInput = $('[name="code"]', codeForm);
        codeForm.addEventListener('submit', async (event) => {
          event.preventDefault();
          clearFieldErrors(codeForm);
          const code = String(new FormData(codeForm).get('code') || '').trim();
          if (!/^\d{6}$/.test(code)) {
            fieldError(codeInput, '请输入 6 位邮箱验证码。');
            codeInput && codeInput.focus();
            return;
          }
          busy(codeSubmit, true, '验证中…');
          try {
            await request('/api/account/email/verify/confirm', { method: 'POST', body: { code } });
            notify(msg, '邮箱验证成功。', 'success');
            codeForm.reset();
            await loadMe();
          } catch (err) {
            notify(msg, err && err.message ? err.message : '邮箱验证失败，请重新发送验证码。', 'error');
          } finally {
            busy(codeSubmit, false);
          }
        });
      }

      const resend = $('[data-resend-verify]');
      if (resend) {
        resend.addEventListener('click', async () => {
          const typed = emailInput ? emailInput.value.trim() : '';
          const bound = $('[data-settings-email]');
          const target = typed || (bound ? bound.textContent.trim() : '');
          if (!EMAIL_RE.test(target)) {
            notify(msg, '请先填写要验证的邮箱地址。', 'error');
            emailInput && emailInput.focus();
            return;
          }
          await sendVerify(target, resend, '验证邮件已重新发送至 ');
        });
      }
    }

    function renderSessions(items) {
      const host = $('[data-session-list]');
      if (!host) return;
      clear(host);
      $$('[data-security-device-count]').forEach((node) => setText(node, `${items.length} 台在线`));
      if (!items.length) {
        renderEmpty(host, '没有其他在线设备。');
        return;
      }
      const list = el('ul', 'account-device-list');
      items.forEach((session) => {
        const li = el('li', 'account-device');
        li.dataset.current = session.current ? '1' : '0';
        const title = el('div', 'account-device-title');
        title.appendChild(el('strong', null, session.deviceLabel || '未知设备'));
        if (session.current) title.appendChild(el('span', 'badge ok', '当前设备'));
        li.appendChild(title);

        const action = el('div', 'account-device-action');
        if (session.current) {
          action.appendChild(el('span', 'account-hint', '正在使用'));
        } else {
          const button = el('button', 'btn ghost small-btn', '下线该设备');
          button.type = 'button';
          button.addEventListener('click', async () => {
            const msg = $('[data-session-msg], #sessionMsg');
            busy(button, true, '下线中…');
            try {
              await request(`/api/account/sessions/${encodeURIComponent(session.id)}`, { method: 'DELETE' });
              notify(msg, '该设备已下线。', 'success');
              loadSessions();
            } catch (err) {
              notify(msg, err && err.message ? err.message : '下线失败，请稍后重试。', 'error');
              busy(button, false);
            }
          });
          action.appendChild(button);
        }
        li.appendChild(action);

        const meta = el('div', 'account-device-meta');
        meta.appendChild(el('span', 'mono', session.ip || 'IP 未知'));
        meta.appendChild(el('span', null, `登录于 ${fmtDate(session.createdAt)}`));
        meta.appendChild(el('span', null, `最近活跃 ${fmtRelative(session.lastSeenAt)}`));
        li.appendChild(meta);
        list.appendChild(li);
      });
      host.appendChild(list);

      const revokeAll = $('[data-session-revoke-all]');
      if (revokeAll) {
        const others = items.filter((item) => !item.current).length;
        revokeAll.disabled = others === 0;
        revokeAll.textContent = others ? `下线其他 ${others} 台设备` : '没有其他设备';
      }
    }

    async function loadSessions() {
      const host = $('[data-session-list]');
      const msg = $('[data-session-msg], #sessionMsg');
      notify(msg, '');
      renderSkeleton(host, 2);
      try {
        const list = await request('/api/account/sessions');
        renderSessions(Array.isArray(list) ? list : []);
      } catch (err) {
        renderEmpty(host, err && err.message ? err.message : '设备列表加载失败。');
      }
    }

    function initQuoteContactForm() {
      const form = $('#accountQuoteContactForm');
      if (!form) return;
      const msg = $('[data-quote-contact-msg]');
      const submit = $('[data-submit]', form);
      const typeInput = $('[name="contactType"]', form);
      const valueInput = $('[name="contactValue"]', form);

      form.addEventListener('submit', async (event) => {
        event.preventDefault();
        clearFieldErrors(form);
        notify(msg, '');
        const type = String((typeInput && typeInput.value) || '').trim();
        const value = String((valueInput && valueInput.value) || '').trim();
        if (!value) {
          fieldError(valueInput, '请填写手机号 / 微信号 / QQ 号。');
          if (valueInput) valueInput.focus();
          return;
        }
        busy(submit, true, '添加中…');
        try {
          await request('/api/account/quote-contacts', { method: 'POST', body: { type, value } });
          notify(msg, '已添加。前台提交询价时会自动带上第一条，也可以在询价页临时改。', 'success');
          if (valueInput) valueInput.value = '';
          await loadMe();
        } catch (err) {
          notify(msg, accountErrorMessage(err, '添加失败，请检查格式后重试。'), 'error');
        } finally {
          busy(submit, false);
        }
      });

      const host = $('#quoteContactList');
      if (host) {
        host.addEventListener('click', async (event) => {
          const button = event.target.closest('[data-quote-contact-remove]');
          if (!button) return;
          const index = Number(button.getAttribute('data-quote-contact-remove'));
          if (!Number.isInteger(index) || index < 0) return;
          if (!await confirmAction('删除询价联系方式', '删除后提交询价不再自动带上这一条；登录方式不受影响。', '删除')) return;
          busy(button, true, '处理中…');
          try {
            await request(`/api/account/quote-contacts/${index}`, { method: 'DELETE' });
            notify(msg, '已删除。', 'success');
            await loadMe();
          } catch (err) {
            notify(msg, accountErrorMessage(err, '删除失败，请稍后重试。'), 'error');
            busy(button, false);
          }
        });
      }
    }

    initPasswordForm();
    initEmailForm();
    initQuoteContactForm();

    const refresh = $('[data-session-refresh]');
    if (refresh) refresh.addEventListener('click', loadSessions);

    const revokeAll = $('[data-session-revoke-all]');
    if (revokeAll) {
      revokeAll.addEventListener('click', async () => {
        const msg = $('[data-session-msg], #sessionMsg');
        if (!await confirmAction('下线其他设备', '除当前设备外，其余登录会话会立即失效，需要重新输入密码登录。', '全部下线')) return;
        busy(revokeAll, true, '处理中…');
        try {
          await request('/api/account/sessions/revoke-all', { method: 'POST', body: {} });
          notify(msg, '其他设备已全部下线。', 'success');
          await loadSessions();
        } catch (err) {
          notify(msg, err && err.message ? err.message : '批量下线失败，请稍后重试。', 'error');
        } finally {
          busy(revokeAll, false);
        }
      });
    }

    const eventsRefresh = $('[data-security-events-refresh]');
    if (eventsRefresh) eventsRefresh.addEventListener('click', loadSecurityEvents);

    $$('[data-account-logout]').forEach((button) => {
      button.addEventListener('click', async () => {
        busy(button, true, '退出中…');
        try {
          await request('/api/account/logout', { method: 'POST', body: {}, auth: 'silent' });
        } catch (err) {
          // 无论服务端是否已结束会话，都返回登录页。
        } finally {
          location.assign('/account/login');
        }
      });
    });

    (async () => {
      const verifyToken = params.get('verifyToken') || params.get('token');
      await loadMe();
      if (verifyToken) {
        const cleanUrl = new URL(location.href);
        cleanUrl.searchParams.delete('verifyToken');
        cleanUrl.searchParams.delete('token');
        history.replaceState(null, '', `${cleanUrl.pathname}${cleanUrl.search}${cleanUrl.hash}`);
        const box = $('[data-email-verify-panel]');
        show(box, true);
        await handleVerifyToken(verifyToken);
        await loadMe();
      }
      await Promise.all([loadSessions(), loadSecurityEvents()]);
    })();
  }

  function boot() {
    if (document.body.dataset.page !== 'account-settings') return;
    bindPasswordToggles(document);
    bindCapsLockWarnings(document);
    initSettingsPage();
  }

  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', boot, { once: true });
  else boot();

  window.XHAccountSettings = Object.freeze({ init: initSettingsPage });
})();
