(() => {
  'use strict';

  // 仅保留投稿页身份头像增强。账号设置页由 account-center.js 负责导航，
  // account-profile-controller.js 负责资料表单，避免多个控制器争抢同一事件。
  if ((document.body.dataset.page || '') !== 'account-submit') return;

  const $ = (selector, root = document) => root.querySelector(selector);
  const $$ = (selector, root = document) => [...root.querySelectorAll(selector)];

  function initialOf(profile) {
    const name = String(profile?.displayName || '').trim();
    return (name || '星').slice(0, 1);
  }

  function renderAvatar(host, profile) {
    if (!host) return;
    const initial = initialOf(profile);
    host.replaceChildren();
    const url = String(profile?.avatarUrl || '').trim();
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
    image.addEventListener('error', () => {
      if (!host.contains(image)) return;
      const fallback = document.createElement('span');
      fallback.className = 'account-avatar-fallback';
      fallback.textContent = initial;
      host.replaceChildren(fallback);
    }, { once: true });
    host.appendChild(image);
  }

  async function loadIdentity() {
    const response = await fetch('/api/account/me', {
      credentials: 'same-origin',
      cache: 'no-store',
      headers: { Accept: 'application/json' }
    });
    if (response.status === 401) {
      const next = `${location.pathname}${location.search}${location.hash}`;
      location.replace(`/account/login?next=${encodeURIComponent(next)}`);
      return;
    }
    if (!response.ok) return;
    const profile = await response.json();
    renderAvatar($('.nexus-profile .account-avatar'), profile);
    $$('[data-submit-account-name]').forEach((node) => {
      node.textContent = profile?.displayName || '未命名账号';
    });
  }

  loadIdentity().catch(() => {
    // account-submit.js 负责投稿页主体错误处理；头像读取失败时保留静态占位。
  });
})();