(() => {
  'use strict';

  const ROUTE = '/next/account/submit';
  const LIST_API = '/api/account/submissions?size=50';
  const CREATE_API = '/api/account/submissions';
  let mounted = false;
  let loading = false;
  let requestVersion = 0;
  let observer = null;
  const gameNames = new Map();

  const $ = (selector, root = document) => root.querySelector(selector);
  const text = (value) => String(value ?? '').trim();

  function make(tag, value, className) {
    const node = document.createElement(tag);
    if (value !== undefined && value !== null) node.textContent = String(value);
    if (className) node.className = className;
    return node;
  }

  function onRoute() {
    return /^\/next\/account\/submit\/?$/.test(location.pathname);
  }

  async function request(url, options = {}) {
    const method = String(options.method || 'GET').toUpperCase();
    const headers = {
      Accept: 'application/json',
      'X-Requested-With': 'fetch',
      ...(options.headers || {})
    };
    const init = {
      method,
      credentials: 'same-origin',
      cache: method === 'GET' ? 'no-store' : 'default',
      headers
    };
    if (options.body !== undefined) {
      headers['Content-Type'] = 'application/json';
      init.body = JSON.stringify(options.body);
    }
    const response = await fetch(url, init);
    const body = await response.json().catch(() => ({}));
    if (!response.ok) {
      const error = new Error(text(body?.message) || text(body?.error) || `请求失败（${response.status}）`);
      error.status = response.status;
      error.body = body;
      throw error;
    }
    return body;
  }

  function installStyles() {
    if ($('#frontend-next-n24-style')) return;
    const style = document.createElement('style');
    style.id = 'frontend-next-n24-style';
    style.textContent = `
      .n24-console{width:min(var(--max,1420px),calc(100% - 40px));margin:0 auto;padding:112px 0 70px}.n24-head{display:grid;grid-template-columns:1fr auto;gap:18px;align-items:end;margin-bottom:20px}
      .n24-head h1{font-size:clamp(40px,5vw,64px);margin:14px 0 8px}.n24-actions{display:flex;gap:8px;flex-wrap:wrap;justify-content:flex-end}.n24-grid{display:grid;grid-template-columns:minmax(0,1.15fr) minmax(300px,.85fr);gap:14px}
      .n24-panel{border:1px solid var(--line);border-radius:16px;background:#0c1015;padding:18px}.n24-panel h2{margin:0 0 6px;font-size:20px}.n24-note{color:#818a94;font-size:10px;line-height:1.6;margin:0 0 14px}
      .n24-form{display:grid;gap:12px}.n24-row{display:grid;grid-template-columns:1fr 1fr;gap:10px}.n24-field{display:grid;gap:6px}.n24-field label{font-size:10px;color:#aab1b9}.n24-field textarea{min-height:260px;resize:vertical}.n24-field small{color:#707984;font-size:9px}.n24-message{min-height:20px;font-size:10px;color:#87909a;margin-top:4px}.n24-message.error{color:#ff9b87}.n24-message.ok{color:#76e3a3}
      .n24-preview{display:grid;gap:8px;max-height:520px;overflow:auto;padding-right:4px}.n24-preview h3,.n24-preview h4{margin:7px 0 0}.n24-preview p,.n24-preview li{color:#a0a8b1;font-size:10px;line-height:1.7}.n24-preview ul{margin:0;padding-left:18px}.n24-list{display:grid;gap:10px;margin-top:12px}.n24-item{border:1px solid #ffffff12;border-radius:12px;padding:13px;background:#090d11}.n24-item-head{display:flex;gap:8px;justify-content:space-between;align-items:flex-start}.n24-item h3{font-size:13px;margin:0;line-height:1.35}.n24-status{font-size:8px;border:1px solid #ffffff18;border-radius:99px;padding:4px 7px;color:#bdc4cb;white-space:nowrap}.n24-status.good{color:#17130a;background:var(--gold);border-color:var(--gold)}.n24-status.bad{color:#ff9b87;border-color:#ff9b8744}.n24-meta{display:flex;gap:8px;flex-wrap:wrap;color:#727b85;font-size:8px;margin:8px 0}.n24-item p{color:#9199a2;font-size:9px;line-height:1.6;margin:8px 0}.n24-item-actions{display:flex;gap:7px;flex-wrap:wrap;margin-top:10px}.n24-empty{padding:24px 12px;border:1px dashed #ffffff1d;border-radius:12px;color:#858d96;text-align:center;font-size:10px}.n24-account-hidden{display:none!important}
      @media(max-width:900px){.n24-grid{grid-template-columns:1fr}.n24-preview{max-height:none}}@media(max-width:680px){.n24-console{width:calc(100% - 26px);padding-top:92px}.n24-head{grid-template-columns:1fr}.n24-actions{justify-content:flex-start}.n24-row{grid-template-columns:1fr}.n24-field textarea{min-height:220px}}
    `;
    document.head.append(style);
  }

  function retargetLegacyWriter() {
    document.querySelectorAll('#n5PersonalAssets a[href="/account/submit"], a[href="/account/submit"][data-n21-legacy]').forEach((link) => {
      link.setAttribute('href', ROUTE);
      delete link.dataset.n14Legacy;
      delete link.dataset.n21Legacy;
      link.dataset.n24Submission = '1';
    });
  }

  function ensureGuestReturn() {
    if (!onRoute() || !$('#n4LoginForm')) return false;
    const url = new URL(location.href);
    if (url.searchParams.get('next') !== ROUTE) {
      url.searchParams.set('next', ROUTE);
      history.replaceState(history.state, '', url.pathname + url.search + url.hash);
    }
    return true;
  }

  function statusClass(status) {
    if (status === 'PUBLISHED' || status === 'APPROVED') return 'n24-status good';
    if (status === 'REJECTED' || status === 'WITHDRAWN') return 'n24-status bad';
    return 'n24-status';
  }

  function statusText(item) {
    return text(item?.statusText) || ({
      SUBMITTED: '已提交，等待审核',
      IN_REVIEW: '审核中',
      APPROVED: '已通过，等待发布',
      REJECTED: '未通过',
      PUBLISHED: '已发布',
      WITHDRAWN: '已撤回'
    }[text(item?.status)] || '处理中');
  }

  function relative(value) {
    if (!value) return '—';
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return text(value);
    return date.toLocaleString('zh-CN', { hour12: false, year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' });
  }

  function message(value, kind = '') {
    const node = $('#n24Message');
    if (!node) return;
    node.textContent = text(value);
    node.className = `n24-message${kind ? ` ${kind}` : ''}`;
  }

  function previewMarkdown(raw) {
    const host = $('#n24Preview');
    const count = $('#n24Count');
    if (count) count.textContent = `${String(raw || '').length} 字符`;
    if (!host) return;
    host.replaceChildren();
    const source = String(raw || '');
    if (!source.trim()) {
      host.append(make('div', '开始输入正文后，这里会显示安全的结构预览。', 'n24-empty'));
      return;
    }
    let list = null;
    source.split(/\r?\n/).forEach((line) => {
      const value = line.trim();
      if (!value) { list = null; return; }
      const heading = value.match(/^(#{1,3})\s+(.+)$/);
      if (heading) {
        list = null;
        host.append(make(heading[1].length === 1 ? 'h3' : 'h4', heading[2]));
        return;
      }
      const bullet = value.match(/^(?:[-*]|\d+\.)\s+(.+)$/);
      if (bullet) {
        if (!list) { list = make('ul'); host.append(list); }
        list.append(make('li', bullet[1].replace(/\*\*/g, '')));
        return;
      }
      list = null;
      host.append(make('p', value.replace(/\*\*/g, '')));
    });
  }

  function buildConsole() {
    const page = $('.page[data-page="account"]');
    if (!page) return null;
    let root = $('#n24SubmissionConsole');
    if (root) return root;

    root = make('section', null, 'n24-console');
    root.id = 'n24SubmissionConsole';
    const head = make('div', null, 'n24-head');
    const copy = make('div');
    copy.append(make('div', 'FRONTEND NEXT · N24', 'kicker'));
    const h1 = make('h1');
    h1.append(document.createTextNode('攻略投稿'), make('span', '控制台', 'g'));
    copy.append(h1, make('p', '撰写新投稿、查看审核状态并撤回仍在审核流程中的内容。使用现有账号投稿接口，不建立第二套数据。', 'sub'));
    const actions = make('div', null, 'n24-actions');
    const back = make('a', '返回我的投稿', 'btn'); back.href = '/next/account/submissions';
    const legacy = make('a', '旧版投稿页', 'btn'); legacy.href = '/account/submit?legacy=1'; legacy.dataset.n24Legacy = '1';
    actions.append(back, legacy); head.append(copy, actions);

    const grid = make('div', null, 'n24-grid');
    const editor = make('section', null, 'n24-panel');
    editor.append(make('h2', '撰写新投稿'), make('p', '当前后端支持新建投稿、查看投稿和撤回；没有修改已提交投稿的更新接口，因此本页不会伪造“编辑已提交稿件”。', 'n24-note'));
    const form = make('form', null, 'n24-form'); form.id = 'n24Form';
    const row = make('div', null, 'n24-row');
    const gameField = make('div', null, 'n24-field'); const gameLabel = make('label', '游戏 *'); gameLabel.htmlFor = 'n24Game'; const game = document.createElement('select'); game.id = 'n24Game'; game.name = 'gameCode'; game.required = true; gameField.append(gameLabel, game);
    const titleField = make('div', null, 'n24-field'); const titleLabel = make('label', '标题 *'); titleLabel.htmlFor = 'n24Title'; const title = document.createElement('input'); title.id = 'n24Title'; title.name = 'title'; title.maxLength = 180; title.required = true; title.placeholder = '例如：MW4 某武器迷彩解锁路线'; titleField.append(titleLabel, title);
    row.append(gameField, titleField);
    const summaryField = make('div', null, 'n24-field'); const summaryLabel = make('label', '摘要'); summaryLabel.htmlFor = 'n24Summary'; const summary = document.createElement('textarea'); summary.id = 'n24Summary'; summary.name = 'summary'; summary.maxLength = 500; summary.rows = 3; summary.placeholder = '简要说明这篇攻略能解决什么问题'; summaryField.append(summaryLabel, summary);
    const contentField = make('div', null, 'n24-field'); const contentLabel = make('label', '正文 Markdown *'); contentLabel.htmlFor = 'n24Content'; const content = document.createElement('textarea'); content.id = 'n24Content'; content.name = 'contentMd'; content.required = true; content.placeholder = '# 标题\n\n写下步骤、注意事项和经验…'; const counter = make('small', '0 字符'); counter.id = 'n24Count'; contentField.append(contentLabel, content, counter);
    const submit = make('button', '提交审核', 'btn gold'); submit.type = 'submit'; submit.id = 'n24Submit';
    const msg = make('div', '', 'n24-message'); msg.id = 'n24Message'; msg.setAttribute('role', 'status');
    form.append(row, summaryField, contentField, submit, msg); editor.append(form);

    const side = make('div');
    const previewPanel = make('section', null, 'n24-panel'); previewPanel.append(make('h2', '正文预览'), make('p', '只做结构化安全预览，不执行 Markdown HTML。', 'n24-note')); const preview = make('div', null, 'n24-preview'); preview.id = 'n24Preview'; previewPanel.append(preview);
    const listPanel = make('section', null, 'n24-panel'); listPanel.style.marginTop = '14px'; listPanel.append(make('h2', '我的投稿'), make('p', '最近投稿状态来自账号接口。', 'n24-note')); const list = make('div', null, 'n24-list'); list.id = 'n24List'; listPanel.append(list); side.append(previewPanel, listPanel);
    grid.append(editor, side); root.append(head, grid); page.prepend(root);

    content.addEventListener('input', () => previewMarkdown(content.value));
    form.addEventListener('submit', submitForm);
    previewMarkdown('');
    return root;
  }

  function hideAccountBase(hidden) {
    const page = $('.page[data-page="account"]');
    if (!page) return;
    [...page.children].forEach((child) => {
      if (child.id === 'n24SubmissionConsole') return;
      child.classList.toggle('n24-account-hidden', hidden);
    });
  }

  async function loadGames() {
    const select = $('#n24Game');
    if (!select) return;
    select.replaceChildren(new Option('正在读取游戏…', ''));
    try {
      const catalog = await request('/api/catalog');
      const games = Array.isArray(catalog?.games) ? catalog.games : [];
      gameNames.clear();
      const options = [new Option(games.length ? '请选择游戏' : '暂无可投稿游戏', '')];
      games.forEach((item) => {
        const code = text(item?.code);
        if (!code) return;
        const name = text(item?.name) || code;
        gameNames.set(code, name);
        options.push(new Option(name, code));
      });
      select.replaceChildren(...options);
    } catch (error) {
      select.replaceChildren(new Option('游戏列表读取失败', ''));
    }
  }

  function submissionItem(item) {
    const row = make('article', null, 'n24-item');
    const head = make('div', null, 'n24-item-head');
    head.append(make('h3', text(item?.title) || '(无标题)'), make('span', statusText(item), statusClass(text(item?.status))));
    const meta = make('div', null, 'n24-meta');
    meta.append(make('span', gameNames.get(text(item?.gameCode)) || text(item?.gameCode) || '游戏攻略'), make('span', `更新于 ${relative(item?.updatedAt || item?.createdAt)}`));
    row.append(head, meta);
    if (text(item?.summary)) row.append(make('p', item.summary));
    if (text(item?.reviewNote)) row.append(make('p', `审核意见：${item.reviewNote}`));
    const actions = make('div', null, 'n24-item-actions');
    if (text(item?.publishedSlug)) {
      const link = make('a', '查看已发布攻略', 'btn');
      link.href = `/next/guides/${encodeURIComponent(text(item.publishedSlug))}`;
      actions.append(link);
    }
    if (['SUBMITTED', 'IN_REVIEW'].includes(text(item?.status)) && Number.isInteger(Number(item?.id))) {
      const withdraw = make('button', '撤回投稿', 'btn'); withdraw.type = 'button';
      withdraw.addEventListener('click', () => withdrawSubmission(Number(item.id), withdraw));
      actions.append(withdraw);
    }
    if (actions.childElementCount) row.append(actions);
    return row;
  }

  async function loadSubmissions() {
    const list = $('#n24List');
    if (!list) return;
    const request = ++requestVersion;
    list.replaceChildren(make('div', '正在读取投稿…', 'n24-empty'));
    try {
      const rows = await request(LIST_API);
      if (request !== requestVersion) return;
      const data = Array.isArray(rows) ? rows : [];
      list.replaceChildren(...data.map(submissionItem));
      if (!data.length) list.append(make('div', '还没有投稿记录。完成第一篇后，审核状态会显示在这里。', 'n24-empty'));
    } catch (error) {
      if (request !== requestVersion) return;
      list.replaceChildren(make('div', error.status === 401 ? '登录状态已失效，请重新登录。' : (text(error.message) || '投稿列表读取失败。'), 'n24-empty'));
    }
  }

  async function submitForm(event) {
    event.preventDefault();
    const form = event.currentTarget;
    const button = $('#n24Submit');
    const gameCode = text($('#n24Game')?.value);
    const title = text($('#n24Title')?.value);
    const summary = text($('#n24Summary')?.value);
    const contentMd = String($('#n24Content')?.value || '');
    if (!gameCode) { message('请选择投稿对应的游戏。', 'error'); $('#n24Game')?.focus(); return; }
    if (!title) { message('请填写投稿标题。', 'error'); $('#n24Title')?.focus(); return; }
    if (!contentMd.trim()) { message('正文不能为空。', 'error'); $('#n24Content')?.focus(); return; }
    if (button) { button.disabled = true; button.textContent = '提交中…'; }
    message('正在提交审核…');
    try {
      await request(CREATE_API, { method: 'POST', body: { gameCode, title, summary, contentMd, coverImage: null } });
      form.reset();
      previewMarkdown('');
      message('投稿已提交，我们会尽快审核。', 'ok');
      await loadSubmissions();
      window.XHFrontendNextN5?.reload?.().catch?.(() => {});
    } catch (error) {
      message(error.status === 401 ? '登录状态已失效，请重新登录后再提交。' : (text(error.message) || '投稿失败，请稍后重试。'), 'error');
    } finally {
      if (button) { button.disabled = false; button.textContent = '提交审核'; }
    }
  }

  async function withdrawSubmission(id, button) {
    if (!window.confirm('确认撤回这篇投稿？撤回后将退出审核流程。')) return;
    button.disabled = true;
    try {
      await request(`/api/account/submissions/${encodeURIComponent(String(id))}/withdraw`, { method: 'POST', body: {} });
      message('投稿已撤回。', 'ok');
      await loadSubmissions();
      window.XHFrontendNextN5?.reload?.().catch?.(() => {});
    } catch (error) {
      message(text(error.message) || '撤回失败，请稍后重试。', 'error');
    } finally {
      button.disabled = false;
    }
  }

  function metadata() {
    const brand = text(window.XHFrontendShell?.configuration?.brandName) || '星火网游';
    document.title = `攻略投稿控制台 · ${brand}`;
    let canonical = $('link[rel="canonical"]');
    if (!canonical) { canonical = document.createElement('link'); canonical.rel = 'canonical'; document.head.append(canonical); }
    canonical.href = 'https://wydlgf.cn/account/submit';
    let robots = $('meta[name="robots"]');
    if (!robots) { robots = document.createElement('meta'); robots.name = 'robots'; document.head.append(robots); }
    robots.content = 'noindex,nofollow';
  }

  async function activate() {
    retargetLegacyWriter();
    if (!onRoute()) return false;
    installStyles();
    metadata();
    if (ensureGuestReturn()) {
      hideAccountBase(false);
      return false;
    }
    if (loading || mounted) {
      if (mounted) hideAccountBase(true);
      return mounted;
    }
    loading = true;
    try {
      await request('/api/account/me');
      if (!onRoute()) return false;
      buildConsole();
      hideAccountBase(true);
      mounted = true;
      await loadGames();
      await loadSubmissions();
      return true;
    } catch (error) {
      if (error.status === 401) {
        ensureGuestReturn();
        hideAccountBase(false);
        return false;
      }
      buildConsole();
      hideAccountBase(true);
      message(text(error.message) || '账号状态读取失败，请稍后重试。', 'error');
      mounted = true;
      return true;
    } finally {
      loading = false;
    }
  }

  observer = new MutationObserver(() => {
    retargetLegacyWriter();
    if (!onRoute()) return;
    if (ensureGuestReturn()) return;
    if (!mounted && !loading) activate();
    if (mounted) hideAccountBase(true);
  });
  observer.observe(document.body, { childList: true, subtree: true });

  window.addEventListener('popstate', () => {
    if (onRoute()) activate();
    else hideAccountBase(false);
  });
  window.addEventListener('pagehide', () => observer?.disconnect(), { once: true });

  window.XHFrontendNextN24 = { activate, loadSubmissions, request };
  activate();
})();
