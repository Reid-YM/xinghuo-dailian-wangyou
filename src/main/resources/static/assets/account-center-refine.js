(() => {
  'use strict';

  const page = document.body.dataset.page || '';
  const $ = (selector, root = document) => root.querySelector(selector);
  const $$ = (selector, root = document) => [...root.querySelectorAll(selector)];

  function initialOf(profile) {
    const name = String(profile?.displayName || '').trim();
    return (name || '星').slice(0, 1);
  }

  function renderAvatar(host, profile, previewUrl = '') {
    if (!host) return;
    const initial = initialOf(profile);
    host.dataset.avatarInitial = initial;
    host.replaceChildren();
    const url = String(previewUrl || profile?.avatarUrl || '').trim();
    if (!url) {
      const fallback = document.createElement('span');
      fallback.className = 'account-avatar-fallback';
      fallback.textContent = initial;
      host.appendChild(fallback);
      return;
    }
    const image = document.createElement('img');
    image.src = url;
    image.alt = '';
    image.loading = 'lazy';
    image.decoding = 'async';
    image.addEventListener('error', () => fallbackAvatarImage(image), { once: true });
    host.appendChild(image);
  }

  function fallbackAvatarImage(image) {
    const host = image?.closest?.('.account-avatar');
    if (!host || !host.contains(image)) return;
    const fallback = document.createElement('span');
    fallback.className = 'account-avatar-fallback';
    fallback.textContent = host.dataset.avatarInitial || '星';
    host.replaceChildren(fallback);
  }

  // 图片地址失效时不能留下空白头像。error 不冒泡，因此用捕获阶段兜底。
  document.addEventListener('error', event => {
    if (event.target instanceof HTMLImageElement && event.target.closest('.account-avatar')) {
      fallbackAvatarImage(event.target);
    }
  }, true);

  function showNotice(node, text, type = '') {
    if (!node) return;
    node.classList.remove('hidden', 'is-error', 'is-success', 'is-warning');
    if (!text) {
      node.textContent = '';
      node.hidden = true;
      node.classList.add('hidden');
      return;
    }
    node.textContent = text;
    node.hidden = false;
    if (type) node.classList.add(`is-${type}`);
  }

  async function api(url, options = {}) {
    const method = String(options.method || 'GET').toUpperCase();
    const headers = new Headers(options.headers || {});
    headers.set('Accept', 'application/json');
    if (method !== 'GET' && method !== 'HEAD') headers.set('X-Requested-With', 'fetch');

    let body = options.body;
    if (body != null && !(body instanceof FormData)) {
      headers.set('Content-Type', 'application/json');
      body = JSON.stringify(body);
    }

    let response;
    try {
      response = await fetch(url, {
        method,
        headers,
        body,
        credentials: 'same-origin',
        cache: 'no-store'
      });
    } catch (error) {
      throw new Error('网络连接失败，请检查网络后重试。');
    }

    let payload = null;
    if (response.status !== 204) {
      const contentType = response.headers.get('content-type') || '';
      try {
        payload = contentType.includes('application/json') ? await response.json() : await response.text();
      } catch (error) {
        payload = null;
      }
    }

    if (response.status === 401) {
      const next = `${location.pathname}${location.search}${location.hash}`;
      location.replace(`/account/login?next=${encodeURIComponent(next)}`);
      throw new Error('登录状态已失效，请重新登录。');
    }
    if (!response.ok) {
      const message = payload && typeof payload === 'object' && payload.message
        ? String(payload.message)
        : '请求失败，请稍后重试。';
      throw new Error(message);
    }
    return payload;
  }

  function activateSettingsView(raw) {
    const root = $('[data-settings-hub]');
    if (!root) return;
    const map = {
      profile: 'profile',
      contact: 'contact',
      quotecontact: 'contact',
      security: 'security',
      password: 'security',
      email: 'security',
      devices: 'security',
      activity: 'security',
      session: 'security'
    };
    const key = String(raw || 'profile').replace(/^#/, '').toLowerCase();
    const view = map[key] || 'profile';
    $$('[data-settings-view]', root).forEach(panel => {
      panel.hidden = panel.dataset.settingsView !== view;
    });
    $$('[data-settings-target]', root).forEach(link => {
      link.classList.toggle('is-active', link.dataset.settingsTarget === view);
    });
    if (['password', 'email', 'devices', 'activity', 'session'].includes(key)) {
      window.setTimeout(() => document.getElementById(key)?.scrollIntoView({ block: 'start' }), 0);
    }
  }

  function initSettingsNavigationFallback() {
    const root = $('[data-settings-hub]');
    if (!root) return;
    activateSettingsView(location.hash || 'profile');
    root.addEventListener('click', event => {
      const link = event.target.closest('[data-settings-target]');
      if (!link) return;
      const target = link.dataset.settingsTarget || 'profile';
      history.replaceState(null, '', `#${target}`);
      activateSettingsView(target);
    });
    window.addEventListener('hashchange', () => activateSettingsView(location.hash));
  }

  function initProfileController() {
    const form = $('#accountProfileForm');
    if (!form) return;

    const message = $('[data-profile-msg]');
    const submit = $('[data-submit]', form);
    const nameInput = $('[name="displayName"]', form);
    const fileInput = $('[name="avatarFile"]', form);
    let currentProfile = null;
    let previewObjectUrl = '';

    function syncProfile(profile) {
      if (!profile) return;
      currentProfile = profile;
      $$('[data-profile-avatar-preview], [data-account-center-avatar]').forEach(node => renderAvatar(node, profile));
      $$('[data-settings-name], [data-account-center-name]').forEach(node => {
        node.textContent = profile.displayName || '未命名账号';
      });
      $$('[data-profile-public-id], [data-settings-public-id]').forEach(node => {
        node.textContent = profile.publicUserId || '—';
      });
      if (nameInput && document.activeElement !== nameInput) nameInput.value = profile.displayName || '';
    }

    async function refreshProfile() {
      const profile = await api('/api/account/me');
      syncProfile(profile);
      return profile;
    }

    if (fileInput) {
      fileInput.addEventListener('change', () => {
        if (previewObjectUrl) {
          URL.revokeObjectURL(previewObjectUrl);
          previewObjectUrl = '';
        }
        const file = fileInput.files?.[0];
        if (!file) {
          $$('[data-profile-avatar-preview]').forEach(node => renderAvatar(node, currentProfile));
          return;
        }
        if (!file.type.startsWith('image/')) {
          showNotice(message, '请选择 JPG、PNG 或 WebP 图片。', 'error');
          fileInput.value = '';
          return;
        }
        previewObjectUrl = URL.createObjectURL(file);
        $$('[data-profile-avatar-preview]').forEach(node => renderAvatar(node, currentProfile, previewObjectUrl));
        showNotice(message, '头像预览已更新，点击“保存资料”后才会正式保存。', 'warning');
      });
    }

    // 个人资料保存由本页控制器接管，避免旧处理器引用未定义 state.me 后中断，
    // 以及只改昵称时把已有 avatarUrl 意外清空。
    form.addEventListener('submit', async event => {
      event.preventDefault();
      event.stopImmediatePropagation();

      const displayName = String(nameInput?.value || '').trim();
      if (!displayName) {
        showNotice(message, '请填写昵称。', 'error');
        nameInput?.focus();
        return;
      }

      if (submit) {
        submit.disabled = true;
        submit.classList.add('is-busy');
        submit.setAttribute('aria-busy', 'true');
        submit.textContent = '保存中…';
      }
      showNotice(message, '');

      let avatarUploaded = false;
      try {
        if (!currentProfile) await refreshProfile();
        const file = fileInput?.files?.[0];
        if (file && file.size > 0) {
          const data = new FormData();
          data.append('file', file);
          const uploaded = await api('/api/account/profile/avatar', { method: 'POST', body: data });
          if (uploaded) {
            currentProfile = uploaded;
            avatarUploaded = true;
          }
        }

        const saved = await api('/api/account/profile', {
          method: 'POST',
          body: {
            displayName,
            avatarUrl: String(currentProfile?.avatarUrl || '')
          }
        });
        syncProfile(saved);
        if (fileInput) fileInput.value = '';
        if (previewObjectUrl) {
          URL.revokeObjectURL(previewObjectUrl);
          previewObjectUrl = '';
        }
        showNotice(message, '资料已保存，头像与昵称已同步到个人中心。', 'success');
      } catch (error) {
        showNotice(
          message,
          avatarUploaded
            ? `头像已保存，但其他资料同步失败：${error.message || '请刷新后重试。'}`
            : (error.message || '资料保存失败，请稍后重试。'),
          'error'
        );
        try { await refreshProfile(); } catch (ignored) { /* 页面仍保留当前可见状态 */ }
      } finally {
        if (submit) {
          submit.disabled = false;
          submit.classList.remove('is-busy');
          submit.removeAttribute('aria-busy');
          submit.textContent = '保存资料';
        }
      }
    }, true);

    refreshProfile().catch(() => {
      // account.js 仍会负责页面其余账号信息；这里仅保证资料控制器有自己的安全兜底。
    });
  }

  async function initSubmissionIdentityAvatar() {
    const host = $('.nexus-profile .account-avatar');
    if (!host) return;
    try {
      const profile = await api('/api/account/me');
      renderAvatar(host, profile);
      $$('[data-submit-account-name]').forEach(node => {
        node.textContent = profile?.displayName || '未命名账号';
      });
    } catch (error) {
      // account.js 负责投稿页主体错误处理；头像读取失败时保留静态占位即可。
    }
  }

  if (page === 'account-settings') {
    initSettingsNavigationFallback();
    initProfileController();
  } else if (page === 'account-submit') {
    initSubmissionIdentityAvatar();
  }
})();
