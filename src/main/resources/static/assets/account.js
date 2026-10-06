/* ==========================================================================
   星火 V2 · 账号模块脚本（account.js）
   --------------------------------------------------------------------------
   覆盖页面（按 <body data-page> 分发）：
     account-login     登录 + 邮箱找回密码
     account-register  注册（联系方式分段、即时校验、密码强度）
     account             用户中心（概览 / 我的订单 / 进度同步 / 我的设备）
     account-settings    账号设置（改密码 / 邮箱 / 设备 / 退出）

   硬约束（与站点 CSP 一致）：
     - 不使用任何内联脚本与内联事件属性，全部事件在此绑定；
     - 不引入外部 CDN，不依赖任何第三方库；
     - 所有动态文本走 textContent（或 el.text），禁止 innerHTML 拼接数据。
       （public.js 导出的 XH.esc 仍会被复用：它用于「把不可信值拼进
        URL 查询串」这类场景，不做 HTML 注入。）

   依赖：/assets/public.js 暴露的 window.XH（$ / $$ / esc / setBusy / showMessage）。
        未加载成功时本文件仍可工作（内部有最小兜底实现）。
   ========================================================================== */
(() => {
  'use strict';

  const XH = window.XH || {};

  /* ---------------------------------------------------------------- 基础工具 */

  const $ = (selector, root = document) => (XH.$ ? XH.$(selector, root) : root.querySelector(selector));
  const $$ = (selector, root = document) => (XH.$$ ? XH.$$(selector, root) : [...root.querySelectorAll(selector)]);

  /** 把不可信值安全地放进 URL 查询串（依赖 public.js 的 esc，兜底 encodeURIComponent） */
  const q = (value) => (XH.esc ? XH.esc(value == null ? '' : String(value)) : encodeURIComponent(value == null ? '' : String(value)));

  /** 创建元素：tag、可选类名、可选文本。属性一律用 setAttribute 显式设置。 */
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

  function show(node, visible) {
    if (!node) return;
    node.hidden = !visible;
  }

  /** 文本安全写入（唯一入口，防止任何 HTML 解析） */
  function setText(node, value, fallback = '—') {
    if (!node) return;
    const raw = value === undefined || value === null || value === '' ? fallback : value;
    node.textContent = String(raw);
  }

  function renderAvatarNode(node, me) {
    if (!node) return;
    clear(node);
    const avatar = me && me.avatarUrl ? String(me.avatarUrl) : '';
    if (avatar) {
      const img = el('img');
      img.src = avatar;
      img.alt = '';
      img.loading = 'lazy';
      node.appendChild(img);
    } else {
      node.appendChild(el('span', 'account-avatar-fallback', (me && me.displayName ? me.displayName : '星').slice(0, 1)));
    }
  }

  function fmtDate(value) {
    if (!value) return '—';
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return String(value);
    const pad = (n) => String(n).padStart(2, '0');
    return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`;
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

  /* ------------------------------------------------------- request / 错误映射 */

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
    AUTH_REQUIRED: '请先登录后再继续。',
    CONTACT_TAKEN: '该联系方式已注册，请直接登录或找回密码。',
    CONTACT_EXISTS: '该联系方式已注册，请直接登录或找回密码。',
    CONTACT_INVALID: '联系方式格式不正确，请检查后重试。',
    WEAK_PASSWORD: '密码强度不足：至少 8 位，且需包含字母、数字、符号中的两类。',
    PASSWORD_MISMATCH: '两次输入的密码不一致。',
    OLD_PASSWORD_INVALID: '原密码不正确。',
    OLD_PASSWORD_MISMATCH: '原密码不正确。',
    PASSWORD_SAME: '新密码不能与原密码相同。',
    SAME_PASSWORD: '新密码不能与原密码相同。',
    EMAIL_TAKEN: '该邮箱已被其他账号绑定。',
    EMAIL_INVALID: '邮箱格式不正确。',
    EMAIL_NOT_VERIFIED: '邮箱尚未验证，请先完成验证。',
    TOKEN_INVALID: '链接无效或已失效，请重新发起。',
    TOKEN_EXPIRED: '链接已过期，请重新发起。',
    RATE_LIMITED: '操作过于频繁，请稍后再试。',
    ORDER_NOT_FOUND: '未找到该业务编号，请核对后重试。',
    ORDER_ALREADY_CLAIMED: '该业务编号已被认领。',
    ORDER_NOT_CLAIMABLE: '该业务编号当前不可认领，请联系客服。',
    PROGRESS_NOT_FOUND: '云端还没有这份进度记录。',
    VERSION_CONFLICT: '云端进度已被其他设备更新，请刷新后重试。',
    PAYLOAD_INVALID: '进度内容不是合法的 JSON。',
    AGREE_REQUIRED: '请先阅读并同意服务规则与隐私说明。',
    VALIDATION_FAILED: '提交的内容有误，请检查后重试。',
    SESSION_NOT_FOUND: '该设备已下线。',
    CONTACT_VERIFICATION_REQUIRED: '登录联系方式尚未核验，请联系客服完成核验。',
    NETWORK_ERROR: '网络连接失败，请检查网络后重试。',
    SERVER_ERROR: '服务暂时不可用，请稍后重试。',
    INVALID_JSON: '服务返回内容异常，请稍后重试。',
    CODE_INVALID: '验证码无效或已过期，请重新发送。',
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

  /**
   * 统一的 401 处理：带上回跳参数跳登录页。
   * 参数名与后端 AccountAuthInterceptor 的 `/account/login?next=...` 保持一致。
   */
  function redirectToLogin() {
    const returnTo = location.pathname + location.search;
    const target = returnTo && returnTo.indexOf('/account/login') !== 0
      ? `/account/login?next=${encodeURIComponent(returnTo)}`
      : '/account/login';
    location.replace(target);
  }

  /** 读取登录回跳地址，兼容 next / redirect 两种参数名，且只接受站内相对路径 */
  function readReturnTo(params) {
    const raw = params.get('next') || params.get('redirect') || '';
    if (!raw) return '';
    // 解析成 URL 后校验同源，而不是做字符串前缀判断。
    //
    // 曾经的写法只拒绝「以 // 开头」，但 /\evil.com 能过关：首字符是 /，
    // indexOf('//') 也不是 0。而按 WHATWG URL 规范，特殊 scheme 下反斜杠
    // 等价于斜杠，location.assign('/\evil.com') 最终会跳到 https://evil.com
    // —— 一个可用的开放重定向，常被用来伪装钓鱼链接的落地页。
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

  /**
   * 统一请求封装。
   * - 自动 JSON 序列化 / 解析；
   * - 失败时抛出 ApiError（含后端 error 码与中文 message）；
   * - 401 时按需跳转登录页（由调用方传 auth: 'redirect' | 'silent'）。
   */
  async function request(path, options = {}) {
    const method = (options.method || 'GET').toUpperCase();
    const headers = Object.assign(
      { Accept: 'application/json' },
      // 与后端约定的写接口自定义头（CSP 不限制 fetch 头，服务端可据此判 CSRF）
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

    if (response.status === 401 && options.auth !== 'silent') {
      redirectToLogin();
    }

    throw new ApiError(code, message, response.status);
  }

  /* ---------------------------------------------------------------- 提示条 */

  /**
   * 统一提示条。
   * @param {Element} host 提示容器（#[id]Msg）
   * @param {string} message 空字符串表示清除提示
   * @param {string} type 'error' 显示为错误样式
   * @param {object} [options] { silent: true } 时本次清除请求被忽略，
   *        用于「一次用户操作后接连发生的后台刷新」不覆盖刚显示的成功提示。
   */
  function notify(host, message, type, options) {
    if (!host) return;
    const isError = type === 'error';
    if (!message && options && options.silent) return;
    clear(host);
    if (!message) {
      host.hidden = true;
      return;
    }
    const box = el('div', `notice${isError ? ' error' : ''}`, message);
    box.setAttribute('role', isError ? 'alert' : 'status');
    host.appendChild(box);
    host.hidden = false;
  }

  /** 按钮忙碌态（优先复用 public.js 的 setBusy） */
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



  /* 20261004-v015: 注册验证码、倒计时与账号表单错误兜底 */
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

  function toast(message, type) {
    if (!message) return;
    let host = $('[data-account-toast]');
    if (!host) {
      host = el('div', 'toast-area');
      host.setAttribute('data-account-toast', '');
      host.setAttribute('aria-live', 'polite');
      document.body.appendChild(host);
    }
    const node = el('div', `toast${type === 'error' ? ' error' : ''}`, message);
    node.setAttribute('role', type === 'error' ? 'alert' : 'status');
    host.appendChild(node);
    window.setTimeout(() => node.remove(), 3600);
  }

  /**
   * 统一确认对话框。相比浏览器原生 confirm，它能明确展示动作、影响与取消入口，
   * 同时保留 Esc、焦点回收和键盘操作。
   */
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

  /* ------------------------------------------------------------ 字段校验 */

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
      test: (v) => /^[^@\s]+@[^@\s.]+(\.[^@\s.]+)+$/.test(v),
      error: '请输入有效的邮箱地址。'
    }
  };

  function contactRule(type) {
    return CONTACT_RULES[type] || CONTACT_RULES.EMAIL;
  }

  /** UTF-8 字节数（后端按字节判定，BCrypt 只取前 72 字节） */
  function utf8Bytes(value) {
    const text = String(value || '');
    if (typeof TextEncoder === 'function') return new TextEncoder().encode(text).length;
    return unescape(encodeURIComponent(text)).length;
  }

  /**
   * 密码策略，与后端 com.xinghuo.account.service.PasswordPolicy 对齐：
   * 至少 8 位、UTF-8 不超过 72 字节、且小写 / 大写 / 数字 / 符号中至少包含两类。
   * 前端只做即时提示，最终判定仍以后端为准。
   */
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
    if (length < 8) {
      return { valid: false, score: 1, classes, length, bytes, label: `还差 ${8 - length} 位（至少 8 位）` };
    }
    if (bytes > 72) {
      return { valid: false, score: 2, classes, length, bytes, label: '密码过长（中文约 24 个字符以内）' };
    }
    if (classes < 2) {
      return { valid: false, score: 2, classes, length, bytes, label: '需包含小写 / 大写 / 数字 / 符号中的至少两类' };
    }
    let score = 2;
    if (length >= 10 && classes >= 2) score = 3;
    if (length >= 12 && classes >= 3) score = 4;
    const label = score >= 4 ? '强' : (score >= 3 ? '中上' : '可用');
    return { valid: true, score, classes, length, bytes, label };
  }

  function applyPasswordMeter(meter, value) {
    if (!meter) return;
    const result = passwordStrength(value);
    meter.setAttribute('data-score', String(result.score));
    const label = $('span', meter);
    if (label) label.textContent = `密码强度：${result.label}`;
  }

  function bindPasswordToogles(root) {
    $$('[data-toggle-password]', root).forEach((button) => {
      button.addEventListener('click', () => {
        const input = button.closest('.password-field') ? $('input', button.closest('.password-field')) : null;
        if (!input) return;
        const revealing = input.type === 'password';
        input.type = revealing ? 'text' : 'password';
        button.textContent = revealing ? '隐藏' : '显示';
        button.setAttribute('aria-label', revealing ? '隐藏密码' : '显示密码');
        button.setAttribute('aria-pressed', revealing ? 'true' : 'false');
      });
    });
  }

  /** 密码输入时就近提示大写锁定，避免连续登录失败。 */
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

  /**
   * 联系方式分段控件（微信 / QQ / 手机号）通用绑定。
   * 语义与账号中心标签页一致：容器 role="tablist"、按钮 role="tab" + aria-selected，
   * 并支持左右方向键切换；选中结果写入表单里的隐藏输入 [data-contact-type]。
   */
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

  /* ------------------------------------------------------------------ 标签页 */

  /**
   * 标签页控制器。
   * @param {Element} root 角色为 tablist 的容器
   * @param {object} options { panelsRoot, onActivate }
   *   panelsRoot 缺省为 root；账号中心里 tablist 与 tabpanel 不是父子关系，
   *   因此必须显式传入共同祖先，否则查不到任何 panel（历史 bug）。
   */
  function initTabs(root, options = {}) {
    if (!root) return null;
    const panelsRoot = options.panelsRoot || root;
    const tabs = $$('[role="tab"]', root);
    const panels = $$('[role="tabpanel"]', panelsRoot);
    if (!tabs.length || !panels.length) return null;

    const normalize = (name) => (tabs.some((tab) => tab.dataset.tab === name) ? name : tabs[0].dataset.tab);
    const loaded = new Set();

    function activate(name, opts = {}) {
      const target = normalize(name);
      tabs.forEach((tab) => {
        const selected = tab.dataset.tab === target;
        tab.setAttribute('aria-selected', selected ? 'true' : 'false');
        tab.tabIndex = selected ? 0 : -1;
      });
      panels.forEach((panel) => {
        const active = panel.dataset.panel === target;
        panel.hidden = !active;
        if (active && opts.focus) panel.focus({ preventScroll: true });
      });
      if (opts.updateHash !== false && location.hash !== `#${target}`) {
        // 用 replaceState 避免额外的滚动跳动
        history.replaceState(null, '', `${location.pathname}${location.search}#${target}`);
      }
      if (!loaded.has(target)) {
        loaded.add(target);
        if (typeof options.onActivate === 'function') options.onActivate(target, { first: true });
      } else if (typeof options.onActivate === 'function') {
        options.onActivate(target, { first: false });
      }
    }

    tabs.forEach((tab, index) => {
      tab.addEventListener('click', () => activate(tab.dataset.tab, { focus: false }));
      tab.addEventListener('keydown', (event) => {
        const keys = ['ArrowRight', 'ArrowDown', 'ArrowLeft', 'ArrowUp', 'Home', 'End'];
        if (!keys.includes(event.key)) return;
        event.preventDefault();
        let next = index;
        if (event.key === 'ArrowRight' || event.key === 'ArrowDown') next = (index + 1) % tabs.length;
        if (event.key === 'ArrowLeft' || event.key === 'ArrowUp') next = (index - 1 + tabs.length) % tabs.length;
        if (event.key === 'Home') next = 0;
        if (event.key === 'End') next = tabs.length - 1;
        tabs[next].focus();
        activate(tabs[next].dataset.tab, { focus: false, updateHash: false });
      });
    });

    const fromHash = (location.hash || '').replace('#', '');
    activate(fromHash || tabs[0].dataset.tab, { updateHash: false });
    return { activate };
  }

  /** 列表骨架 / 空态 */
  function renderSkeleton(host, rows = 3) {
    if (!host) return;
    const box = el('div', 'account-skeleton');
    for (let i = 0; i < rows; i += 1) box.appendChild(el('span'));
    clear(host).appendChild(box);
  }

  function renderEmpty(host, message, actionText, actionHref) {
    if (!host) return;
    clear(host);
    const box = el('div', 'empty');
    box.appendChild(el('p', null, message));
    if (actionText && actionHref) {
      const link = el('a', 'btn ghost', actionText);
      link.href = actionHref;
      box.appendChild(link);
    }
    host.appendChild(box);
  }

  /* ==================================================================
     页面：登录 account-login
     ================================================================== */

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

    // 联系方式分段：placeholder / 提示 / 已完成校验随类型切换
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
        valueInput && valueInput.focus();
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

    // 忘记密码：展开邮箱找回表单
    const resetForm = $('#accountResetRequestForm');
    if (resetForm) {
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
  }

  /* ==================================================================
     页面：注册 account-register
     ================================================================== */

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

    // 分段控件的选中态与隐藏输入由通用绑定维护，这里只响应选中后的字段变化
    bindSegmentedControl(form, (type) => selectType(type));
    selectType((typeInput && typeInput.value) || 'EMAIL');

    if (passwordInput && meter) {
      applyPasswordMeter(meter, '');
      passwordInput.addEventListener('input', () => applyPasswordMeter(meter, passwordInput.value));
    }
    if (valueInput) {
      valueInput.addEventListener('blur', () => validateContact(true));
      valueInput.addEventListener('input', () => { if (valueInput.getAttribute('aria-invalid') === 'true') validateContact(true); });
    }

    if (codeButton) {
      codeButton.addEventListener('click', async () => {
        clearFieldErrors(form);
        notify(msg, '');
        if (!validateContact(true)) {
          valueInput && valueInput.focus();
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
          codeInput && codeInput.focus();
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
        agreeInput && agreeInput.focus();
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

  /* ==================================================================
     页面：用户中心 account
     ================================================================== */

  function initAccountPage() {
    const shell = $('#accountShell');
    if (!shell) return;

    const state = {
      me: null,
      orders: { page: 0, size: 10, total: 0, items: [], loaded: false },
      progress: { tools: [], activeKey: null, loaded: false },
      sessions: { items: [], loaded: false }
    };

    const identity = {
      name: $('[data-account-name]'),
      meta: $('[data-account-meta]'),
      avatar: $('[data-account-avatar]')
    };

    /* ---------------------------------------------------------- 概览渲染 */

    function renderIdentity(me) {
      setText(identity.name, me.displayName, '未命名账号');
      setText(identity.avatar, (String(me.displayName || '星').trim().slice(0, 1) || '星'));

      if (me.avatarUrl && identity.avatar && identity.avatar.parentElement) {
        const holder = identity.avatar.parentElement;
        const img = el('img');
        img.alt = '';
        img.loading = 'lazy';
        img.decoding = 'async';
        img.src = me.avatarUrl;
        img.addEventListener('error', () => img.remove(), { once: true });
        holder.appendChild(img);
      }

      if (identity.meta) {
        clear(identity.meta);
        const chips = [
          `${contactTypeText(me.contactType)} ${me.contactMasked || '—'}`,
          me.email ? (me.emailVerified ? `邮箱 ${me.email}（已验证）` : `邮箱 ${me.email}（未验证）`) : '未绑定邮箱',
          `注册于 ${fmtDate(me.createdAt)}`
        ];
        chips.forEach((text) => identity.meta.appendChild(el('span', null, text)));
      }

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
    }

    function contactTypeText(type) {
      return ({ EMAIL: '邮箱' })[type] || '邮箱';
    }

    function renderOverview(me) {
      const grid = $('[data-account-info]');
      if (!grid) return;
      clear(grid);
      const rows = [
        ['显示昵称', me.displayName || '—', '仅用于账号中心展示'],
        ['登录方式', contactTypeText(me.contactType), '注册与登录使用的联系方式类型'],
        ['联系方式', me.contactMasked || '—', '页面只展示脱敏结果'],
        ['绑定邮箱', me.email || '未绑定', me.email ? (me.emailVerified ? '已完成验证' : '尚未验证') : '绑定后可用于找回密码'],
        ['注册时间', fmtDate(me.createdAt), '账号创建时间']
      ];
      rows.forEach(([label, value, note]) => {
        const item = el('div', 'account-info-item');
        item.appendChild(el('span', null, label));
        item.appendChild(el('strong', null, value));
        item.appendChild(el('small', null, note));
        grid.appendChild(item);
      });

      const entPanel = $('[data-account-entitlements]');
      const entList = $('[data-account-entitlement-list]');
      if (entPanel && entList) {
        const items = Array.isArray(me.entitlements) ? me.entitlements : [];
        clear(entList);
        if (!items.length) {
          show(entPanel, false);
        } else {
          show(entPanel, true);
          items.forEach((entry) => {
            const chip = el('span', 'badge');
            const label = typeof entry === 'string'
              ? entry
              : (entry && (entry.label || entry.name || entry.key)) || '权益';
            chip.textContent = String(label);
            entList.appendChild(chip);
          });
        }
      }
    }

    /* ---------------------------------------------------------- 订单渲染 */

    function orderStatusClass(status) {
      const value = String(status || '').toUpperCase();
      if (['COMPLETED', 'DONE', 'FINISHED', 'DELIVERED'].includes(value)) return 'badge ok';
      if (['PENDING', 'WAITING', 'REVIEW', 'PAID'].includes(value)) return 'badge warn';
      if (['CANCELED', 'CANCELLED', 'REFUNDED', 'FAILED'].includes(value)) return 'badge danger';
      return 'badge';
    }

    function renderOrderList() {
      const host = $('[data-order-list]');
      if (!host) return;
      clear(host);
      const items = state.orders.items;
      $$('[data-account-order-count]').forEach((node) => setText(node, `${state.orders.total || items.length} 笔业务`));
      if (!items.length) {
        renderEmpty(host, '当前账号还没有关联的业务记录。', '提交新询价', '/quote');
        return;
      }
      const list = el('ul', 'account-order-list');
      items.forEach((order) => {
        const li = el('li', 'account-order');

        const main = el('div', 'account-order-main');
        const title = el('div', 'account-order-title');
        title.appendChild(el('strong', null, order.gameName || '游戏服务'));
        title.appendChild(el('span', orderStatusClass(order.status), order.statusText || '处理中'));
        main.appendChild(title);
        const sub = el('div', 'account-order-sub');
        sub.appendChild(el('span', 'mono', order.businessNo || '业务编号待同步'));
        if (order.serviceName) sub.appendChild(el('span', null, order.serviceName));
        sub.appendChild(el('span', null, `更新于 ${fmtRelative(order.updatedAt || order.createdAt)}`));
        main.appendChild(sub);
        li.appendChild(main);

        const progress = Math.min(100, Math.max(0, Number(order.progressPercent || 0)));
        const progressBox = el('div', 'account-order-progress');
        const progressHead = el('div');
        progressHead.appendChild(el('span', null, order.progressText || '等待工作人员更新进度'));
        progressHead.appendChild(el('strong', null, order.progressPercent == null ? '待更新' : `${progress}%`));
        progressBox.appendChild(progressHead);
        const track = el('div', 'account-progress-track');
        const fill = el('i');
        fill.style.width = `${progress}%`;
        track.appendChild(fill);
        progressBox.appendChild(track);
        li.appendChild(progressBox);

        const action = el('div', 'account-order-action');
        if (order.businessNo) {
          const link = el('a', 'btn ghost small-btn', '查看进度');
          link.href = `/record/${encodeURIComponent(order.businessNo)}`;
          action.appendChild(link);
        } else {
          const link = el('a', 'btn ghost small-btn', '联系客服');
          link.href = '/contact';
          action.appendChild(link);
        }
        li.appendChild(action);

        list.appendChild(li);
      });
      host.appendChild(list);
    }

    function renderOrderPager() {
      const info = $('[data-order-page-info]');
      const prev = $('[data-order-prev]');
      const next = $('[data-order-next]');
      const { page, size, total, items } = state.orders;
      const pages = Math.max(1, Math.ceil((total || items.length) / size));
      if (info) {
        if (!total && !items.length) info.textContent = '暂无记录';
        else info.textContent = `第 ${page + 1} / ${pages} 页 · 本页 ${items.length} 条 · 共 ${total || items.length} 条`;
      }
      if (prev) prev.disabled = page <= 0;
      if (next) next.disabled = (page + 1) * size >= (total || items.length);
    }

    async function loadOrders(page) {
      const host = $('[data-order-list]');
      const panel = $('[data-panel="orders"]');
      const msg = $('#orderMsg');
      state.orders.page = Math.max(0, page || 0);
      notify(msg, '');
      renderSkeleton(host, 3);
      try {
        const data = await request(`/api/account/orders?page=${state.orders.page}&size=${state.orders.size}`);
        state.orders.items = Array.isArray(data && data.items) ? data.items : [];
        state.orders.total = data && Number.isFinite(data.total) ? data.total : state.orders.items.length;
        if (data && Number.isFinite(data.size)) state.orders.size = data.size;
        if (data && Number.isFinite(data.page)) state.orders.page = data.page;
        state.orders.loaded = true;
        renderOrderList();
        renderOrderPager();
      } catch (err) {
        renderEmpty(host, err && err.message ? err.message : '订单加载失败，请稍后重试。');
        notify(msg, err && err.message ? err.message : '订单加载失败，请稍后重试。', 'error');
        $$('[data-account-order-count]').forEach((node) => setText(node, '暂不可用'));
        if (panel) panel.setAttribute('data-load-error', '1');
      }
    }

    function bindOrderActions() {
      const prev = $('[data-order-prev]');
      const next = $('[data-order-next]');
      const refresh = $('[data-order-refresh]');
      if (prev) prev.addEventListener('click', () => loadOrders(Math.max(0, state.orders.page - 1)));
      if (next) next.addEventListener('click', () => loadOrders(state.orders.page + 1));
      if (refresh) refresh.addEventListener('click', () => loadOrders(state.orders.page));

      const claimForm = $('#accountClaimForm');
      if (!claimForm) return;
      const claimMsg = $('#claimMsg');
      const claimSubmit = $('[data-submit]', claimForm);
      const orderInput = $('[name="orderNo"]', claimForm);
      const candidatesHost = $('[data-claim-candidates]');

      async function loadCandidates() {
        if (!candidatesHost) return;
        clear(candidatesHost);
        try {
          const list = await request('/api/account/orders/candidates');
          const items = Array.isArray(list) ? list : [];
          if (!items.length) {
            candidatesHost.appendChild(el('p', 'account-inline-note', '没有找到可认领的历史业务编号。如果确有未归档的订单，请联系客服。'));
            return;
          }
          candidatesHost.appendChild(el('p', 'account-inline-note', '以下历史业务编号可确认来源（单号已部分脱敏）。请对照后在认领框手动输入完整业务编号：'));
          const wrap = el('div', 'account-tool-list');
          items.forEach((item) => {
            const button = el('button', 'account-tool-chip');
            button.type = 'button';
            button.appendChild(el('strong', null, item.businessNo || '业务编号待同步'));
            button.appendChild(el('span', null, ` · ${item.gameName || '游戏服务'} · ${fmtDate(item.createdAt)}`));
            button.addEventListener('click', () => {
              // 候选单号为脱敏展示（保留前6后2），不可直接用于认领提交。
              // 仅聚焦输入框并提示手动输入完整业务编号，避免把脱敏值填入导致提交失败。
              if (orderInput) { orderInput.focus(); fieldError(orderInput, ''); }
              notify(claimMsg, '该单号为脱敏后的识别信息，请手动输入完整业务编号后再提交认领。');
            });
            wrap.appendChild(button);
          });
          candidatesHost.appendChild(wrap);
        } catch (err) {
          candidatesHost.appendChild(el('p', 'account-inline-note', err && err.message ? err.message : '候选列表加载失败。'));
        }
      }

      claimForm.addEventListener('submit', async (event) => {
        event.preventDefault();
        notify(claimMsg, '');
        const orderNo = orderInput ? orderInput.value.trim() : '';
        if (!orderNo) {
          fieldError(orderInput, '请输入业务编号。');
          return;
        }
        fieldError(orderInput, '');
        busy(claimSubmit, true, '认领中…');
        try {
          await request('/api/account/orders/claim', { method: 'POST', body: { businessNo: orderNo } });
          notify(claimMsg, '认领成功，订单已归档到当前账号。', 'success');
          claimForm.reset();
          loadCandidates();
          loadOrders(0);
        } catch (err) {
          notify(claimMsg, err && err.message ? err.message : errorMessage(null), 'error');
        } finally {
          busy(claimSubmit, false);
        }
      });

      loadCandidates();
    }

    /* ------------------------------------------------------ 进度同步渲染 */

    function renderToolChips() {
      const host = $('[data-progress-tools]');
      if (!host) return;
      clear(host);
      const tools = state.progress.tools;
      if (!tools.length) {
        renderEmpty(host, '云端还没有工具记录。你仍可在工具站使用本机自动保存。', '打开 MW4 工具站', '/mastery');
        return;
      }
      tools.forEach((item) => {
        const key = item && item.toolKey ? String(item.toolKey) : '';
        if (!key) return;
        const card = el('article', 'account-save-card');
        const known = /mastery|mw4|camo/i.test(key);
        const copy = el('div');
        copy.appendChild(el('span', null, '云端工具记录'));
        copy.appendChild(el('strong', null, known ? 'MW4 迷彩解锁助手' : '游戏工具进度'));
        copy.appendChild(el('small', null, `最近更新：${fmtDate(item.updatedAt)}`));
        card.appendChild(copy);
        const link = el('a', 'btn ghost small-btn', known ? '打开工具' : '查看工具站');
        link.href = '/mastery';
        card.appendChild(link);
        host.appendChild(card);
      });
    }

    // silentClear：仅保存成功后的连锁回读使用，避免清掉刚显示的成功提示
    async function loadProgressList(silentClear) {
      const host = $('[data-progress-tools]');
      const msg = $('#progressMsg');
      notify(msg, '', 'success', { silent: !!silentClear });
      renderSkeleton(host, 2);
      try {
        const list = await request('/api/account/progress');
        state.progress.tools = Array.isArray(list) ? list : [];
        state.progress.loaded = true;
        renderToolChips();
      } catch (err) {
        renderEmpty(host, err && err.message ? err.message : '进度列表加载失败。');
      }
    }

    function bindProgressActions() {
      const refresh = $('[data-progress-refresh]');
      if (refresh) refresh.addEventListener('click', () => loadProgressList());
    }

    /* ------------------------------------------------------------ 设备渲染 */

    function renderSessions() {
      const host = $('[data-session-list]');
      if (!host) return;
      clear(host);
      const items = state.sessions.items;
      $$('[data-security-devices] strong, [data-security-device-count]').forEach((node) => {
        setText(node, `${items.length} 台在线`);
      });
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
        if (!session.current) {
          const button = el('button', 'btn ghost small-btn', '下线该设备');
          button.type = 'button';
          button.addEventListener('click', () => revokeSession(session, button));
          action.appendChild(button);
        } else {
          action.appendChild(el('span', 'account-hint', '正在使用'));
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
        state.sessions.items = Array.isArray(list) ? list : [];
        state.sessions.loaded = true;
        renderSessions();
      } catch (err) {
        renderEmpty(host, err && err.message ? err.message : '设备列表加载失败。');
        $$('[data-security-devices] strong, [data-security-device-count]').forEach((node) => setText(node, '暂不可用'));
      }
    }

    async function revokeSession(session, button) {
      const msg = $('[data-session-msg], #sessionMsg');
      busy(button, true, '下线中…');
      try {
        await request(`/api/account/sessions/${encodeURIComponent(session.id)}`, { method: 'DELETE' });
        notify(msg, '该设备已下线。', 'success');
        await loadSessions();
      } catch (err) {
        notify(msg, err && err.message ? err.message : '下线失败，请稍后重试。', 'error');
        busy(button, false);
      }
    }

    function bindSessionActions() {
      const refresh = $('[data-session-refresh]');
      const revokeAll = $('[data-session-revoke-all]');
      if (refresh) refresh.addEventListener('click', () => loadSessions());
      if (revokeAll) {
        revokeAll.addEventListener('click', async () => {
          const msg = $('[data-session-msg], #sessionMsg');
          if (!await confirmAction('下线其他设备', '除当前设备外，其余登录会话会立即失效，需要重新输入密码登录。', '全部下线')) return;
          busy(revokeAll, true, '处理中…');
          try {
            await request('/api/account/sessions/revoke-all', { method: 'POST', body: {} });
            notify(msg, '其他设备已全部下线。', 'success');
            state.sessions.loaded = false;
            await loadSessions();
          } catch (err) {
            notify(msg, err && err.message ? err.message : '批量下线失败，请稍后重试。', 'error');
          } finally {
            busy(revokeAll, false);
          }
        });
      }
    }

    /* ------------------------------------------------------------ 退出登录 */

    function bindLogout() {
      $$('[data-account-logout]').forEach((button) => {
        button.addEventListener('click', async () => {
          busy(button, true, '退出中…');
          try {
            await request('/api/account/logout', { method: 'POST', body: {}, auth: 'silent' });
          } catch (err) {
            /* 会话可能已失效，仍然回到登录页 */
          } finally {
            location.assign('/account/login');
          }
        });
      });
    }

    /* ---------------------------------------------------------------- 启动 */

    const tabController = initTabs($('[data-account-tabs]'), {
      // tablist 在 .account-tabs 里，tabpanel 在 .account-shell 里，需要共同祖先才能一起查到
      panelsRoot: shell,
      onActivate(target) {
        if (target === 'orders' && !state.orders.loaded) loadOrders(0);
        if (target === 'progress' && !state.progress.loaded) loadProgressList();
        if (target === 'devices' && !state.sessions.loaded) loadSessions();
      }
    });
    $$('[data-open-devices]').forEach((button) => {
      button.addEventListener('click', () => {
        if (tabController) tabController.activate('devices');
        $('[data-panel="devices"]')?.scrollIntoView({ behavior: 'smooth', block: 'start' });
      });
    });
    $$('[data-tab-jump]').forEach((button) => {
      button.addEventListener('click', () => {
        const target = button.dataset.tabJump;
        if (tabController && target) tabController.activate(target);
        $(`[data-panel="${target}"]`)?.scrollIntoView({ behavior: 'smooth', block: 'start' });
      });
    });
    bindOrderActions();
    bindProgressActions();
    bindSessionActions();
    bindLogout();

    (async () => {
      shell.setAttribute('data-loading', '1');
      try {
        const me = await request('/api/account/me');
        state.me = me || {};
        renderIdentity(state.me);
        renderOverview(state.me);
        shell.setAttribute('data-loading', '0');
        await Promise.allSettled([loadOrders(0), loadSessions()]);
      } catch (err) {
        // 401 已由 request() 跳转登录页；其他错误在此提示
        if (err && err.status !== 401) {
          shell.setAttribute('data-loading', '0');
          notify($('#accountMsg'), err && err.message ? err.message : '账号信息加载失败。', 'error');
        }
      }
    })();
  }

  /* ==================================================================
     页面：账号设置 account-settings
     ================================================================== */

  function initSettingsPage() {
    const root = $('#accountSettings');
    if (!root) return;

    const params = new URLSearchParams(location.search);
    const meMsg = $('#settingsMsg');

    /* ------------------------------------------------------------ 账号信息 */

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

    async function loadMe() {
      try {
        const me = await request('/api/account/me');
        // 注册时间 / 联系方式在自述区与只读信息区各出现一次，需要全部写入
        $$('[data-settings-name]').forEach((node) => setText(node, me.displayName, '未命名账号'));
        $$('[data-settings-public-id], [data-profile-public-id]').forEach((node) => setText(node, me.publicUserId, '—'));
        $$('[data-profile-avatar-preview], .nexus-profile .account-avatar').forEach((node) => renderAvatarNode(node, me));
        const profileForm = $('#accountProfileForm');
        if (profileForm) {
          const nameInput = $('[name="displayName"]', profileForm);
          const avatarInput = $('[name="avatarFile"]', profileForm);
          if (nameInput && document.activeElement !== nameInput) nameInput.value = me.displayName || '';
          if (avatarInput && document.activeElement !== avatarInput) avatarInput.value = '';
        }
        $$('[data-settings-contact]').forEach((node) => setText(node, `${contactTypeText(me.contactType)} ${me.contactMasked || '—'}`));
        $$('[data-settings-quote-contact]').forEach((node) => setText(node,
          me.quoteContactMasked ? `${contactTypeText(me.quoteContactType)} ${me.quoteContactMasked}` : '未绑定'));
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
        // 已绑定邮箱时，输入框直接提示当前地址，方便「重新发送」不再手输
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

    /* -------------------------------------------------------- 邮箱验证回调 */

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


    async function uploadProfileAvatar(file) {
      const data = new FormData();
      data.append('file', file);
      let response;
      try {
        response = await fetch('/api/account/profile/avatar', {
          method: 'POST',
          credentials: 'same-origin',
          cache: 'no-store',
          headers: { Accept: 'application/json', 'X-Requested-With': 'fetch' },
          body: data
        });
      } catch (err) {
        throw new ApiError('NETWORK_ERROR', ERROR_TEXT.NETWORK_ERROR, 0);
      }
      let payload = null;
      const contentType = response.headers.get('content-type') || '';
      if (response.status !== 204) {
        try { payload = contentType.includes('application/json') ? await response.json() : await response.text(); } catch (err) { payload = null; }
      }
      if (response.ok) return payload;
      const code = payload && typeof payload === 'object' && payload.error ? String(payload.error) : 'UNKNOWN';
      const message = payload && typeof payload === 'object' && payload.message ? String(payload.message) : errorMessage(code);
      if (response.status === 401) redirectToLogin();
      throw new ApiError(code, message, response.status);
    }

    function initProfileForm() {
      const form = $('#accountProfileForm');
      if (!form) return;
      const msg = $('[data-profile-msg]');
      const submit = $('[data-submit]', form);
      form.addEventListener('submit', async (event) => {
        event.preventDefault();
        clearFieldErrors(form);
        notify(msg, '');
        const data = new FormData(form);
        const displayName = String(data.get('displayName') || '').trim();
        const avatarFile = data.get('avatarFile');
        if (!displayName) {
          fieldError($('[name="displayName"]', form), '请填写昵称。');
          return;
        }
        busy(submit, true, '保存中…');
        try {
          let avatarUrl = '';
          if (avatarFile && avatarFile instanceof File && avatarFile.size > 0) {
            const uploaded = await uploadProfileAvatar(avatarFile);
            avatarUrl = uploaded && uploaded.avatarUrl ? uploaded.avatarUrl : '';
          }
          const body = { displayName, avatarUrl: avatarUrl || (state.me && state.me.avatarUrl) || '' };
          const me = await request('/api/account/profile', { method: 'POST', body });
          notify(msg, '资料已保存。', 'success');
          renderAvatarNode($('[data-profile-avatar-preview]'), me);
          await loadMe();
        } catch (err) {
          notify(msg, err && err.message ? err.message : '资料保存失败，请稍后重试。', 'error');
        } finally {
          busy(submit, false);
        }
      });
    }

    /* ------------------------------------------------------------ 修改密码 */

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

    /* ---------------------------------------------------------- 安全活动 */

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

    /* -------------------------------------------------------- 邮箱绑定表单 */

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
            await loadMe({ force: true });
          } catch (err) {
            notify(msg, err && err.message ? err.message : '邮箱验证失败，请重新发送验证码。', 'error');
          } finally {
            busy(codeSubmit, false);
          }
        });
      }

      // 「重新发送」优先用输入框里的地址，没有再回退到当前已绑定的邮箱
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

    /* ------------------------------------------------------------ 设备管理 */

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

    /* ---------------------------------------------------------------- 启动 */

    /* ---------------------------------------------------- 询价联系方式表单 */

    function initQuoteContactForm() {
      const form = $('#accountQuoteContactForm');
      if (!form) return;
      const msg = $('[data-quote-contact-msg]');
      const submit = $('[data-submit]', form);
      const typeInput = $('[name="contactType"]', form);
      const valueInput = $('[name="contactValue"]', form);
      const clearButton = $('[data-quote-contact-clear]');

      form.addEventListener('submit', async (event) => {
        event.preventDefault();
        clearFieldErrors(form);
        notify(msg, '');
        const contactType = String((typeInput && typeInput.value) || '').trim();
        const contactValue = String((valueInput && valueInput.value) || '').trim();
        if (!contactValue) {
          fieldError(valueInput, '请填写手机号 / 微信号 / QQ 号。');
          if (valueInput) valueInput.focus();
          return;
        }
        busy(submit, true, '保存中…');
        try {
          // 该接口暂不做验证，只校验格式；不会改动登录凭据
          await request('/api/account/quote-contact', {
            method: 'POST',
            body: { contactType, contactValue },
          });
          notify(msg, '已绑定。前台提交询价时会自动带上这个联系方式。', 'success');
          if (valueInput) valueInput.value = '';
          await loadMe();
        } catch (err) {
          notify(msg, accountErrorMessage(err, '绑定失败，请稍后重试。'), 'error');
        } finally {
          busy(submit, false);
        }
      });

      if (clearButton) {
        clearButton.addEventListener('click', async () => {
          if (!await confirmAction('解除询价联系方式', '解除后前台询价会重新要求填写联系方式；登录方式不受影响。', '解除绑定')) return;
          busy(clearButton, true, '处理中…');
          try {
            await request('/api/account/quote-contact', { method: 'DELETE' });
            notify(msg, '已解除绑定。', 'success');
            await loadMe();
          } catch (err) {
            notify(msg, accountErrorMessage(err, '解除失败，请稍后重试。'), 'error');
          } finally {
            busy(clearButton, false);
          }
        });
      }
    }

    initProfileForm();
    initPasswordForm();
    initEmailForm();
    initQuoteContactForm();

    const refresh = $('[data-session-refresh]');
    if (refresh) refresh.addEventListener('click', () => loadSessions());

    const revokeAll = $('[data-session-revoke-all]');
    if (revokeAll) {
      revokeAll.addEventListener('click', async () => {
        const msg = $('[data-session-msg], #sessionMsg');
        if (!await confirmAction('下线其他设备', '除当前设备外，其余登录会话会立即失效，需要重新输入密码登录。', '全部下线')) return;
        busy(revokeAll, true, '处理中…');
        try {
          await request('/api/account/sessions/revoke-all', { method: 'POST', body: {} });
          notify(msg, '其他设备已全部下线。', 'success');
          loadSessions();
        } catch (err) {
          notify(msg, err && err.message ? err.message : '批量下线失败，请稍后重试。', 'error');
        } finally {
          busy(revokeAll, false);
        }
      });
    }

    const eventsRefresh = $('[data-security-events-refresh]');
    if (eventsRefresh) eventsRefresh.addEventListener('click', () => loadSecurityEvents());

    const anchors = $$('[data-console-anchor]');
    const anchorTargets = anchors.map((anchor) => document.querySelector(anchor.getAttribute('href'))).filter(Boolean);
    anchors.forEach((anchor) => anchor.addEventListener('click', () => {
      anchors.forEach((item) => item.removeAttribute('aria-current'));
      anchor.setAttribute('aria-current', 'page');
    }));
    if ('IntersectionObserver' in window && anchorTargets.length) {
      const observer = new IntersectionObserver((entries) => {
        const visible = entries.filter((entry) => entry.isIntersecting).sort((a, b) => b.intersectionRatio - a.intersectionRatio)[0];
        if (!visible) return;
        anchors.forEach((anchor) => anchor.toggleAttribute('aria-current', anchor.getAttribute('href') === `#${visible.target.id}`));
      }, { rootMargin: '-15% 0px -65% 0px', threshold: [0, .25, .6] });
      anchorTargets.forEach((target) => observer.observe(target));
    }

    $$('[data-account-logout]').forEach((button) => {
      button.addEventListener('click', async () => {
        busy(button, true, '退出中…');
        try {
          await request('/api/account/logout', { method: 'POST', body: {}, auth: 'silent' });
        } catch (err) {
          /* 忽略：无论如何都回到登录页 */
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

  /* ------------------------------------------------------ 密码重置确认页 */

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
        // auth: 'silent' —— 重置密码本来就发生在未登录状态，401 不应触发跳转
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

  /* -------------------------------------------------------- 攻略投稿页 */

  function initSubmitPage() {
    const form = $('#accountSubmitForm');
    if (!form) return;
    const msg = $('#accountSubmitMsg');
    const submit = $('[data-submit]', form);
    const gameSelect = $('#submitGame', form);
    const listHost = $('#accountSubmissionList');
    const contentInput = $('#submitContent', form);
    const titleInput = $('#submitTitle', form);
    const summaryInput = $('#submitSummary', form);
    const preview = $('[data-submission-preview]');
    const count = $('[data-submission-count]');
    const draftState = $('[data-draft-state]');
    const clearDraft = $('[data-clear-draft]');
    const draftKey = 'xinghuo:submission-draft:v1';
    const gameNames = new Map();
    let draftTimer = 0;

    const STATUS_TEXT = {
      SUBMITTED: '已提交，等待审核',
      IN_REVIEW: '审核中',
      APPROVED: '已通过，等待发布',
      REJECTED: '未通过',
      PUBLISHED: '已发布',
      WITHDRAWN: '已撤回'
    };

    function statusBadge(status) {
      if (status === 'PUBLISHED' || status === 'APPROVED') return 'badge ok';
      if (status === 'REJECTED' || status === 'WITHDRAWN') return 'badge danger';
      return 'badge warn';
    }

    async function loadAuthor() {
      try {
        const me = await request('/api/account/me');
        $$('[data-submit-account-name]').forEach((node) => setText(node, me && me.displayName, '未命名账号'));
        $$('[data-submit-account-meta]').forEach((node) => {
          setText(node, me && me.emailVerified ? '安全邮箱已验证' : '投稿账号已连接');
        });
      } catch (err) {
        if (err && err.status !== 401) {
          $$('[data-submit-account-name]').forEach((node) => setText(node, '账号状态不可用'));
          $$('[data-submit-account-meta]').forEach((node) => setText(node, '请刷新后重试'));
        }
      }
    }

    function updateSubmissionSummary(rows) {
      const items = Array.isArray(rows) ? rows : [];
      const active = items.filter((item) => item.status === 'SUBMITTED' || item.status === 'IN_REVIEW' || item.status === 'APPROVED').length;
      const published = items.filter((item) => item.status === 'PUBLISHED').length;
      $$('[data-submission-total]').forEach((node) => setText(node, String(items.length)));
      $$('[data-submission-active]').forEach((node) => setText(node, `${active} 条处理中`));
      $$('[data-submission-published]').forEach((node) => setText(node, `${published} 条已发布`));
    }

    function renderRow(item) {
      const row = el('article', 'gc-comment');
      const head = el('div', 'gc-comment-head');
      head.appendChild(el('b', null, item.title || '(无标题)'));
      head.appendChild(el('span', statusBadge(item.status), STATUS_TEXT[item.status] || '处理中'));
      row.appendChild(head);
      const meta = el('div', 'gc-comment-meta');
      meta.appendChild(el('span', null, gameNames.get(item.gameCode) || '游戏攻略'));
      meta.appendChild(el('time', null, `更新于 ${fmtRelative(item.updatedAt || item.createdAt)}`));
      row.appendChild(meta);
      if (item.summary) {
        row.appendChild(el('p', 'gc-comment-body', item.summary));
      }
      if (item.reviewNote) {
        row.appendChild(el('p', 'account-inline-note', `审核意见：${item.reviewNote}`));
      }
      if (item.publishedSlug) {
        const link = el('a', 'account-inline-note', '查看已发布的攻略');
        link.setAttribute('href', `/guides/${encodeURIComponent(item.publishedSlug)}`);
        row.appendChild(link);
      }
      // 只有还在流程中的投稿可以撤回，与服务端 isWithdrawable 保持一致
      if (item.status === 'SUBMITTED' || item.status === 'IN_REVIEW') {
        const actions = el('div', 'gc-comment-actions');
        const button = el('button', null, '撤回');
        button.type = 'button';
        button.addEventListener('click', async () => {
          if (!await confirmAction('撤回投稿', '撤回后该内容将退出审核流程；如需重新提交，之后需要再次发起审核。', '确认撤回')) return;
          button.disabled = true;
          try {
            await request(`/api/account/submissions/${item.id}/withdraw`, { method: 'POST', body: {} });
            notify(msg, '投稿已撤回。', 'success');
            await loadMine();
          } catch (err) {
            notify(msg, err && err.message ? err.message : '撤回失败。', 'error');
          } finally {
            button.disabled = false;
          }
        });
        actions.appendChild(button);
        row.appendChild(actions);
      }
      return row;
    }

    async function loadMine() {
      if (!listHost) return;
      clear(listHost);
      listHost.appendChild(el('p', 'account-inline-note', '正在加载…'));
      try {
        const rows = await request('/api/account/submissions?size=50');
        clear(listHost);
        updateSubmissionSummary(rows);
        if (!Array.isArray(rows) || !rows.length) {
          renderEmpty(listHost, '还没有投稿记录。完成第一篇攻略后，审核状态会显示在这里。', '开始撰写', '#editor');
          return;
        }
        rows.forEach(item => listHost.appendChild(renderRow(item)));
      } catch (err) {
        $$('[data-submission-active], [data-submission-published], [data-submission-total]').forEach((node) => setText(node, '暂不可用'));
        clear(listHost);
        listHost.appendChild(el('p', 'account-inline-note',
          err && err.message ? err.message : '加载失败，请稍后重试。'));
      }
    }

    async function loadGames() {
      if (!gameSelect) return;
      try {
        const catalog = await request('/api/catalog', { auth: 'silent' });
        const games = (catalog && Array.isArray(catalog.games)) ? catalog.games : [];
        clear(gameSelect);
        const blank = document.createElement('option');
        blank.value = '';
        blank.textContent = games.length ? '请选择游戏' : '暂无可投稿的游戏';
        gameSelect.appendChild(blank);
        games.forEach(g => {
          gameNames.set(g.code, g.name || '游戏');
          const option = document.createElement('option');
          option.value = g.code;
          option.textContent = g.name || g.code;
          gameSelect.appendChild(option);
        });
      } catch (err) {
        clear(gameSelect);
        const option = document.createElement('option');
        option.value = '';
        option.textContent = '游戏列表加载失败';
        gameSelect.appendChild(option);
      }
    }

    function renderPreview() {
      const raw = contentInput ? contentInput.value : '';
      if (count) count.textContent = String(raw.length);
      if (!preview) return;
      clear(preview);
      if (!raw.trim()) {
        preview.appendChild(el('p', null, '开始输入后，这里会显示文章结构。'));
        return;
      }
      let list = null;
      raw.split(/\r?\n/).forEach((line) => {
        const text = line.trim();
        if (!text) { list = null; return; }
        const heading = text.match(/^(#{1,3})\s+(.+)$/);
        if (heading) {
          list = null;
          const level = Math.min(4, heading[1].length + 1);
          preview.appendChild(el(`h${level}`, null, heading[2]));
          return;
        }
        const bullet = text.match(/^(?:[-*]|\d+\.)\s+(.+)$/);
        if (bullet) {
          if (!list) { list = el('ul'); preview.appendChild(list); }
          list.appendChild(el('li', null, bullet[1].replace(/\*\*/g, '')));
          return;
        }
        list = null;
        preview.appendChild(el('p', null, text.replace(/\*\*/g, '')));
      });
    }

    function saveDraft() {
      clearTimeout(draftTimer);
      draftTimer = window.setTimeout(() => {
        try {
          localStorage.setItem(draftKey, JSON.stringify({
            gameCode: gameSelect ? gameSelect.value : '',
            title: titleInput ? titleInput.value : '',
            summary: summaryInput ? summaryInput.value : '',
            contentMd: contentInput ? contentInput.value : '',
            savedAt: Date.now()
          }));
          if (draftState) draftState.textContent = '已自动保存到本机';
        } catch (ignored) {
          if (draftState) draftState.textContent = '本机草稿保存不可用';
        }
      }, 350);
    }

    function restoreDraft() {
      try {
        const draft = JSON.parse(localStorage.getItem(draftKey) || 'null');
        if (!draft || !draft.savedAt) return;
        if (gameSelect && [...gameSelect.options].some((option) => option.value === draft.gameCode)) gameSelect.value = draft.gameCode || '';
        if (titleInput) titleInput.value = draft.title || '';
        if (summaryInput) summaryInput.value = draft.summary || '';
        if (contentInput) contentInput.value = draft.contentMd || '';
        if (draftState) draftState.textContent = `已恢复 ${fmtRelative(new Date(draft.savedAt).toISOString())} 的本机草稿`;
        renderPreview();
      } catch (ignored) { /* 损坏草稿直接忽略 */ }
    }

    [gameSelect, titleInput, summaryInput, contentInput].filter(Boolean).forEach((input) => {
      input.addEventListener('input', () => { renderPreview(); saveDraft(); });
      input.addEventListener('change', saveDraft);
    });
    if (clearDraft) clearDraft.addEventListener('click', async () => {
      if (!await confirmAction('清空本机草稿', '将清除当前设备上尚未提交的标题、摘要和正文。', '确认清空')) return;
      form.reset();
      try { localStorage.removeItem(draftKey); } catch (ignored) {}
      if (draftState) draftState.textContent = '草稿已清空';
      renderPreview();
    });

    form.addEventListener('submit', async (event) => {
      event.preventDefault();
      clearFieldErrors(form);
      notify(msg, '');
      const data = new FormData(form);
      const gameCode = String(data.get('gameCode') || '').trim();
      const title = String(data.get('title') || '').trim();
      const summary = String(data.get('summary') || '').trim();
      const contentMd = String(data.get('contentMd') || '');

      let firstInvalid = null;
      if (!gameCode) {
        notify(msg, '请选择投稿对应的游戏。', 'error');
        firstInvalid = firstInvalid || gameSelect;
      }
      if (!title) {
        fieldError($('[name="title"]', form), '请填写标题。');
        firstInvalid = firstInvalid || $('[name="title"]', form);
      }
      if (!contentMd.trim()) {
        fieldError($('[name="contentMd"]', form), '正文不能为空。');
        firstInvalid = firstInvalid || $('[name="contentMd"]', form);
      }
      if (firstInvalid) {
        firstInvalid.focus();
        return;
      }

      busy(submit, true, '提交中…');
      try {
        await request('/api/account/submissions', {
          method: 'POST',
          body: { gameCode, title, summary, contentMd }
        });
        form.reset();
        try { localStorage.removeItem(draftKey); } catch (ignored) {}
        if (draftState) draftState.textContent = '投稿已提交';
        renderPreview();
        notify(msg, '投稿已提交，我们会尽快审核。', 'success');
        await loadMine();
      } catch (err) {
        notify(msg, err && err.message ? err.message : '投稿失败，请稍后重试。', 'error');
      } finally {
        busy(submit, false);
      }
    });

    loadGames().then(restoreDraft);
    renderPreview();
    loadMine();
    loadAuthor();
    $$('[data-account-logout]').forEach((button) => {
      button.addEventListener('click', async () => {
        busy(button, true, '退出中…');
        try {
          await request('/api/account/logout', { method: 'POST', body: {}, auth: 'silent' });
        } catch (err) {
          /* 无论服务端是否已结束会话，都返回登录页 */
        } finally {
          location.assign('/account/login');
        }
      });
    });

    const anchors = $$('[data-console-anchor]');
    const anchorTargets = anchors.map((anchor) => document.querySelector(anchor.getAttribute('href'))).filter(Boolean);
    anchors.forEach((anchor) => anchor.addEventListener('click', () => {
      anchors.forEach((item) => item.removeAttribute('aria-current'));
      anchor.setAttribute('aria-current', 'page');
    }));
    if ('IntersectionObserver' in window && anchorTargets.length) {
      const observer = new IntersectionObserver((entries) => {
        const visible = entries.filter((entry) => entry.isIntersecting).sort((a, b) => b.intersectionRatio - a.intersectionRatio)[0];
        if (!visible) return;
        anchors.forEach((anchor) => anchor.toggleAttribute('aria-current', anchor.getAttribute('href') === `#${visible.target.id}`));
      }, { rootMargin: '-15% 0px -65% 0px', threshold: [0, .25, .6] });
      anchorTargets.forEach((target) => observer.observe(target));
    }
  }

  /* ---------------------------------------------------------------- 分发 */

  function boot() {
    bindPasswordToogles(document);
    bindCapsLockWarnings(document);
    switch (document.body.dataset.page) {
      case 'account-login':
        initLoginPage();
        break;
      case 'account-register':
        initRegisterPage();
        break;
      case 'account':
        initAccountPage();
        break;
      case 'account-settings':
        initSettingsPage();
        break;
      case 'account-reset':
        initResetPage();
        break;
      case 'account-submit':
        initSubmitPage();
        break;
      default:
        break;
    }
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', boot, { once: true });
  } else {
    boot();
  }

  // 便于排查：只暴露只读入口，不暴露内部状态
  window.XHAccount = { request, errorMessage, ERROR_TEXT, passwordStrength };
})();
