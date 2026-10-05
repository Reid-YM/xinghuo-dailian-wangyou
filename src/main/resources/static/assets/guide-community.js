/**
 * 内容页社区模块：攻略页的评论与收藏。
 *
 * 独立于 account.js：攻略页是全站访问量最大的页面之一，不该为了一个评论区
 * 加载账号中心那 67KB 脚本。这里只保留评论与收藏必需的部分。
 *
 * 渲染一律走 textContent —— 评论内容是用户输入，任何情况下都不能当 HTML 插入。
 * 服务端也只存纯文本，这里是第二道防线。
 */
(() => {
  const root = document.querySelector('[data-guide-community]');
  if (!root) return;
  const slug = root.dataset.guideSlug || '';
  if (!slug) return;

  const note = root.querySelector('[data-community-note]');
  const form = root.querySelector('[data-comment-form]');
  const list = root.querySelector('[data-comment-list]');
  const errorBox = root.querySelector('[data-comment-error]');
  const favButton = root.querySelector('[data-favorite-toggle]');
  const favLabel = root.querySelector('[data-favorite-label]');

  let signedIn = false;
  let favorited = false;

  /* ---------------------------------------------------------------- 请求 */

  async function request(path, options = {}) {
    const method = (options.method || 'GET').toUpperCase();
    const headers = { Accept: 'application/json', 'X-Requested-With': 'fetch' };
    const init = { method, headers, credentials: 'same-origin', cache: 'no-store' };
    if (options.body !== undefined && options.body !== null) {
      headers['Content-Type'] = 'application/json';
      init.body = JSON.stringify(options.body);
    }
    let response;
    try {
      response = await fetch(path, init);
    } catch (cause) {
      throw Object.assign(new Error('网络异常，请检查连接后重试'), { status: 0 });
    }
    let payload = null;
    if (response.status !== 204) {
      try {
        payload = await response.json();
      } catch (ignored) {
        payload = null;
      }
    }
    if (!response.ok) {
      throw Object.assign(
        new Error((payload && payload.message) || '请求失败，请稍后再试'),
        { status: response.status, code: payload && payload.error }
      );
    }
    return payload;
  }

  /* ---------------------------------------------------------------- 渲染 */

  function setNote(text) {
    if (note) note.textContent = text;
  }

  function setError(text) {
    if (!errorBox) return;
    errorBox.textContent = text || '';
    errorBox.hidden = !text;
  }

  function commentNode(item) {
    const wrap = document.createElement('article');
    wrap.className = 'gc-comment';
    if (item.parentId) wrap.classList.add('gc-reply');

    const head = document.createElement('div');
    head.className = 'gc-comment-head';
    const author = document.createElement('b');
    author.textContent = item.authorName || '匿名用户';
    const time = document.createElement('span');
    time.textContent = formatTime(item.createdAt);
    head.appendChild(author);
    head.appendChild(time);
    // 按真实状态显示：此前所有非 PUBLISHED 都标成「待审核」，
    // 被驳回的评论会让作者一直等一个不会来的审核结果。
    if (item.status && item.status !== 'PUBLISHED') {
      const state = document.createElement('span');
      state.className = 'gc-pending';
      state.textContent = item.status === 'REJECTED' ? '未通过审核' : '待审核';
      head.appendChild(state);
    }
    wrap.appendChild(head);

    const body = document.createElement('p');
    body.className = 'gc-comment-body';
    body.textContent = item.content || '';
    wrap.appendChild(body);

    if (signedIn) {
      const actions = document.createElement('div');
      actions.className = 'gc-comment-actions';
      const reply = document.createElement('button');
      reply.type = 'button';
      reply.textContent = '回复';
      reply.addEventListener('click', () => {
        const input = root.querySelector('.gc-input');
        if (!input) return;
        input.focus();
        input.dataset.parentId = String(item.id);
        setNote(`正在回复 ${item.authorName || '匿名用户'}，发表后将作为该评论的回复。`);
      });
      actions.appendChild(reply);
      if (!item.mine) {
        const report = document.createElement('button');
        report.type = 'button';
        report.textContent = '举报';
        report.addEventListener('click', async () => {
          if (!window.confirm('确认举报这条评论？')) return;
          try {
            await request(`/api/account/comments/${item.id}/report`, {
              method: 'POST',
              body: { reason: '用户举报' }
            });
            setNote('举报已提交，我们会尽快处理。');
          } catch (error) {
            setNote(error.message);
          }
        });
        actions.appendChild(report);
      }
      wrap.appendChild(actions);
    }
    return wrap;
  }

  function formatTime(value) {
    if (!value) return '';
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return '';
    return date.toLocaleString('zh-CN', { hour12: false });
  }

  /* ---------------------------------------------------------------- 加载 */

  async function loadMe() {
    try {
      const me = await request('/api/account/me');
      signedIn = true;
      setNote(me && me.displayName
        ? `${me.displayName}，欢迎参与讨论。`
        : '欢迎参与讨论。');
      if (form) form.hidden = false;
      await loadFavorite();
    } catch (error) {
      signedIn = false;
      if (form) form.hidden = true;
      // 401 是未登录的正常状态，不该显示成错误
      setNote(error.status === 401
        ? '登录后可以收藏本篇并参与讨论。'
        : error.message);
    }
  }

  async function loadComments() {
    if (!list) return;
    list.textContent = '';
    const loading = document.createElement('p');
    loading.className = 'gc-empty';
    loading.textContent = '正在加载评论…';
    list.appendChild(loading);

    const target = `targetType=GUIDE&targetId=${encodeURIComponent(slug)}`;
    try {
      const published = await request(`/api/account/comments?${target}&size=50`);
      // 公开列表只含已发布内容，自己的待审评论要单独取一次 ——
      // 否则用户提交后看不到任何痕迹，会以为没发出去而反复重发。
      const own = signedIn
        ? await request(`/api/account/comments/mine?${target}`).catch(() => [])
        : [];

      const merged = Array.isArray(published) ? published.slice() : [];
      (Array.isArray(own) ? own : []).forEach(item => {
        // 已删除（DELETED）的不再渲染 —— 否则管理员删掉的违规内容，
        // 作者在页面上仍然能看到原文，审核等于没做。
        if (!item || item.status === 'DELETED') return;
        if (item.status !== 'PUBLISHED' && !merged.some(x => x.id === item.id)) {
          merged.push(item);
        }
      });
      merged.sort((a, b) => String(a.createdAt).localeCompare(String(b.createdAt)));

      list.textContent = '';
      if (!merged.length) {
        const empty = document.createElement('p');
        empty.className = 'gc-empty';
        empty.textContent = '还没有讨论，来说说你的经验吧。';
        list.appendChild(empty);
        return;
      }
      merged.forEach(item => list.appendChild(commentNode(item)));
    } catch (error) {
      list.textContent = '';
      const failed = document.createElement('p');
      failed.className = 'gc-empty';
      failed.textContent = error.message;
      list.appendChild(failed);
    }
  }

  async function loadFavorite() {
    if (!favLabel) return;
    try {
      const rows = await request('/api/account/favorites?type=GUIDE&size=50');
      favorited = Array.isArray(rows) && rows.some(x => x.targetId === slug);
      favLabel.textContent = favorited ? '已收藏' : '收藏本篇';
    } catch (ignored) {
      favLabel.textContent = '收藏本篇';
    }
  }

  /* ---------------------------------------------------------------- 交互 */

  if (favButton) {
    favButton.addEventListener('click', async () => {
      if (!signedIn) {
        location.assign('/account/login?next=' + encodeURIComponent(location.pathname));
        return;
      }
      favButton.disabled = true;
      try {
        const out = await request('/api/account/favorites/toggle', {
          method: 'POST',
          body: { targetType: 'GUIDE', targetId: slug }
        });
        favorited = !!(out && out.favorited);
        if (favLabel) favLabel.textContent = favorited ? '已收藏' : '收藏本篇';
      } catch (error) {
        setNote(error.message);
      } finally {
        favButton.disabled = false;
      }
    });
  }

  if (form) {
    form.addEventListener('submit', async event => {
      event.preventDefault();
      setError('');
      const input = root.querySelector('.gc-input');
      const content = input ? input.value.trim() : '';
      if (!content) {
        setError('请先写点什么再发表。');
        return;
      }
      const submit = form.querySelector('[data-comment-submit]');
      if (submit) submit.disabled = true;
      try {
        await request('/api/account/comments', {
          method: 'POST',
          body: {
            targetType: 'GUIDE',
            targetId: slug,
            parentId: input.dataset.parentId ? Number(input.dataset.parentId) : null,
            content
          }
        });
        input.value = '';
        delete input.dataset.parentId;
        setNote('评论已提交。新账号的首条评论会先经过审核。');
        await loadComments();
      } catch (error) {
        if (error.status === 401) {
          location.assign('/account/login?next=' + encodeURIComponent(location.pathname));
          return;
        }
        setError(error.message);
      } finally {
        if (submit) submit.disabled = false;
      }
    });
  }

  loadMe().then(loadComments);
})();
