/* ==========================================================================
   星火 V2 · 攻略投稿页运行时（account-submit.js）
   --------------------------------------------------------------------------
   仅负责 account-submit 页面：作者状态、投稿编辑器、本机草稿、预览、审核列表与撤回。
   通用请求、字段错误、提示与 busy 状态由 account-core.js 提供。
   ========================================================================== */
(() => {
  'use strict';

  const core = window.XHAccountCore;
  if (!core) {
    console.error('account-submit.js requires account-core.js');
    return;
  }

  const { $, $$, el, clear, setText, request, notify, busy, fieldError, clearFieldErrors } = core;

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
    return date.toLocaleString('zh-CN', { hour12: false, year: 'numeric', month: '2-digit', day: '2-digit' });
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

    const STATUS_TEXT = Object.freeze({
      SUBMITTED: '已提交，等待审核',
      IN_REVIEW: '审核中',
      APPROVED: '已通过，等待发布',
      REJECTED: '未通过',
      PUBLISHED: '已发布',
      WITHDRAWN: '已撤回'
    });

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

      if (item.summary) row.appendChild(el('p', 'gc-comment-body', item.summary));
      if (item.reviewNote) row.appendChild(el('p', 'account-inline-note', `审核意见：${item.reviewNote}`));
      if (item.publishedSlug) {
        const link = el('a', 'account-inline-note', '查看已发布的攻略');
        link.setAttribute('href', `/guides/${encodeURIComponent(item.publishedSlug)}`);
        row.appendChild(link);
      }

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
        rows.forEach((item) => listHost.appendChild(renderRow(item)));
      } catch (err) {
        $$('[data-submission-active], [data-submission-published], [data-submission-total]').forEach((node) => setText(node, '暂不可用'));
        clear(listHost);
        listHost.appendChild(el('p', 'account-inline-note', err && err.message ? err.message : '加载失败，请稍后重试。'));
      }
    }

    async function loadGames() {
      if (!gameSelect) return;
      try {
        const catalog = await request('/api/catalog', { auth: 'silent' });
        const games = catalog && Array.isArray(catalog.games) ? catalog.games : [];
        clear(gameSelect);
        const blank = document.createElement('option');
        blank.value = '';
        blank.textContent = games.length ? '请选择游戏' : '暂无可投稿的游戏';
        gameSelect.appendChild(blank);
        games.forEach((game) => {
          gameNames.set(game.code, game.name || '游戏');
          const option = document.createElement('option');
          option.value = game.code;
          option.textContent = game.name || game.code;
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
        if (!text) {
          list = null;
          return;
        }
        const heading = text.match(/^(#{1,3})\s+(.+)$/);
        if (heading) {
          list = null;
          const level = Math.min(4, heading[1].length + 1);
          preview.appendChild(el(`h${level}`, null, heading[2]));
          return;
        }
        const bullet = text.match(/^(?:[-*]|\d+\.)\s+(.+)$/);
        if (bullet) {
          if (!list) {
            list = el('ul');
            preview.appendChild(list);
          }
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
      } catch (ignored) {
        // 损坏草稿直接忽略。
      }
    }

    [gameSelect, titleInput, summaryInput, contentInput].filter(Boolean).forEach((input) => {
      input.addEventListener('input', () => {
        renderPreview();
        saveDraft();
      });
      input.addEventListener('change', saveDraft);
    });

    if (clearDraft) {
      clearDraft.addEventListener('click', async () => {
        if (!await confirmAction('清空本机草稿', '将清除当前设备上尚未提交的标题、摘要和正文。', '确认清空')) return;
        form.reset();
        try { localStorage.removeItem(draftKey); } catch (ignored) {}
        if (draftState) draftState.textContent = '草稿已清空';
        renderPreview();
      });
    }

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
          // 无论服务端是否已结束会话，都返回登录页。
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
        const visible = entries
          .filter((entry) => entry.isIntersecting)
          .sort((a, b) => b.intersectionRatio - a.intersectionRatio)[0];
        if (!visible) return;
        anchors.forEach((anchor) => anchor.toggleAttribute('aria-current', anchor.getAttribute('href') === `#${visible.target.id}`));
      }, { rootMargin: '-15% 0px -65% 0px', threshold: [0, .25, .6] });
      anchorTargets.forEach((target) => observer.observe(target));
    }
  }

  function boot() {
    if (document.body.dataset.page === 'account-submit') initSubmitPage();
  }

  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', boot, { once: true });
  else boot();

  window.XHAccountSubmit = Object.freeze({ init: initSubmitPage });
})();
