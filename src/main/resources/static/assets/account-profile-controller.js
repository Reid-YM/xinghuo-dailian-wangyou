(() => {
  'use strict';

  if ((document.body.dataset.page || '') !== 'account-settings') return;

  const form = document.getElementById('accountProfileFormV2');
  if (!form) return;

  const $ = (selector, root = document) => root.querySelector(selector);
  const $$ = (selector, root = document) => [...root.querySelectorAll(selector)];
  const message = $('[data-profile-msg]');
  const submit = $('[data-submit]', form);
  const nameInput = $('[name="displayName"]', form);
  const fileInput = $('[name="avatarFile"]', form);
  let currentProfile = null;
  let previewObjectUrl = '';

  function showMessage(text, type = '') {
    if (!message) return;
    message.classList.remove('hidden', 'is-error', 'is-success', 'is-warning');
    if (!text) {
      message.textContent = '';
      message.hidden = true;
      message.classList.add('hidden');
      return;
    }
    message.textContent = text;
    message.hidden = false;
    if (type) message.classList.add(`is-${type}`);
  }

  function initialOf(profile) {
    const name = String(profile?.displayName || '').trim();
    return (name || '星').slice(0, 1);
  }

  function fallbackAvatarImage(image) {
    const host = image?.closest?.('.account-avatar');
    if (!host || !host.contains(image)) return;
    const fallback = document.createElement('span');
    fallback.className = 'account-avatar-fallback';
    fallback.textContent = host.dataset.avatarInitial || '星';
    host.replaceChildren(fallback);
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

  // 账号页还有其他渲染器会刷新头像 DOM。图片 error 不冒泡，因此用捕获阶段
  // 统一兜底，避免后写入的失效 avatarUrl 留下破图。
  document.addEventListener('error', (event) => {
    if (event.target instanceof HTMLImageElement && event.target.closest('.account-avatar')) {
      fallbackAvatarImage(event.target);
    }
  }, true);

  async function api(url, options = {}) {
    const method = String(options.method || 'GET').toUpperCase();
    const headers = new Headers(options.headers || {});
    headers.set('Accept', 'application/json');
    if (!['GET', 'HEAD'].includes(method)) headers.set('X-Requested-With', 'fetch');
    let body = options.body;
    if (body != null && !(body instanceof FormData)) {
      headers.set('Content-Type', 'application/json');
      body = JSON.stringify(body);
    }
    const response = await fetch(url, { method, headers, body, credentials: 'same-origin', cache: 'no-store' });
    let payload = null;
    if (response.status !== 204) {
      const contentType = response.headers.get('content-type') || '';
      try { payload = contentType.includes('application/json') ? await response.json() : await response.text(); }
      catch (_) { payload = null; }
    }
    if (response.status === 401) {
      const next = `${location.pathname}${location.search}${location.hash}`;
      location.replace(`/account/login?next=${encodeURIComponent(next)}`);
      throw new Error('登录状态已失效，请重新登录。');
    }
    if (!response.ok) {
      const text = payload && typeof payload === 'object' && payload.message
        ? String(payload.message)
        : '请求失败，请稍后重试。';
      throw new Error(text);
    }
    return payload;
  }

  function syncProfile(profile) {
    if (!profile) return;
    currentProfile = profile;
    if (nameInput && document.activeElement !== nameInput) nameInput.value = profile.displayName || '';
    $$('[data-profile-avatar-preview], [data-account-center-avatar]').forEach((node) => renderAvatar(node, profile));
    $$('[data-settings-name], [data-account-center-name]').forEach((node) => {
      node.textContent = profile.displayName || '未命名账号';
    });
    $$('[data-profile-public-id], [data-settings-public-id]').forEach((node) => {
      node.textContent = profile.publicUserId || '—';
    });
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
        $$('[data-profile-avatar-preview]').forEach((node) => renderAvatar(node, currentProfile));
        return;
      }
      if (!file.type.startsWith('image/')) {
        showMessage('请选择 JPG、PNG 或 WebP 图片。', 'error');
        fileInput.value = '';
        return;
      }
      previewObjectUrl = URL.createObjectURL(file);
      $$('[data-profile-avatar-preview]').forEach((node) => renderAvatar(node, currentProfile, previewObjectUrl));
      showMessage('头像预览已更新，点击“保存资料”后才会正式保存。', 'warning');
    });
  }

  form.addEventListener('submit', async (event) => {
    event.preventDefault();
    const displayName = String(nameInput?.value || '').trim();
    if (!displayName) {
      showMessage('请填写昵称。', 'error');
      nameInput?.focus();
      return;
    }

    const originalLabel = submit?.textContent || '保存资料';
    if (submit) {
      submit.disabled = true;
      submit.setAttribute('aria-busy', 'true');
      submit.textContent = '保存中…';
    }
    showMessage('');

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
      showMessage('资料已保存，头像与昵称已同步到个人中心。', 'success');
    } catch (error) {
      showMessage(
        avatarUploaded
          ? `头像已保存，但其他资料同步失败：${error.message || '请刷新后重试。'}`
          : (error.message || '资料保存失败，请稍后重试。'),
        'error'
      );
      try { await refreshProfile(); } catch (_) { /* 保留当前可见状态 */ }
    } finally {
      if (submit) {
        submit.disabled = false;
        submit.removeAttribute('aria-busy');
        submit.textContent = originalLabel;
      }
    }
  });

  refreshProfile().catch(() => {
    // account-settings.js 负责设置页的其余账号信息；资料读取失败时由其通用提示兜底。
  });
})();
