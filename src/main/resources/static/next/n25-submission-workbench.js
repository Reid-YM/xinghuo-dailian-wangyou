(() => {
  'use strict';
  const runtimePath = pathname => window.XHFrontendShell?.runtimePath?.(pathname) || pathname || location.pathname;

  if (!/^\/next\/account\/submit\/?$/.test(runtimePath())) return;

  const STAGE = 'N25 · SUBMISSION WORKBENCH';
  const DRAFT_KEY = 'xinghuo:submission-draft:v1';
  const ACCOUNT_HEADERS = { Accept: 'application/json', 'X-Requested-With': 'fetch' };
  const $ = (selector, root = document) => root.querySelector(selector);
  const text = (value) => String(value ?? '').trim();
  const list = (value) => Array.isArray(value) ? value : [];
  const gameNames = new Map();
  let submissions = [];
  let loadToken = 0;
  let draftTimer = 0;
  let draftPending = false;
  let submitting = false;
  let accountReady = false;

  const STATUS = Object.freeze({
    SUBMITTED: '已提交，等待审核',
    IN_REVIEW: '审核中',
    APPROVED: '已通过，等待发布',
    REJECTED: '未通过',
    PUBLISHED: '已发布',
    WITHDRAWN: '已撤回'
  });

  function node(tag, className, value) {
    const element = document.createElement(tag);
    if (className) element.className = className;
    if (value !== undefined && value !== null) element.textContent = String(value);
    return element;
  }

  function labelFor(value, id) {
    const label = node('label', '', value);
    label.htmlFor = id;
    return label;
  }

  function installStyles() {
    if ($('#frontend-next-n25-style')) return;
    const style = document.createElement('style');
    style.id = 'frontend-next-n25-style';
    style.textContent = `
      .n25-submit{min-height:100vh;padding:112px 0 84px;background:radial-gradient(circle at 82% 10%,#ffcb2d14,transparent 30rem)}
      .n25-head{display:grid;grid-template-columns:minmax(0,1fr) auto;gap:18px;align-items:end;margin-bottom:15px}.n25-head h1{font-size:clamp(40px,6vw,68px);margin:10px 0 8px}.n25-head p{max-width:760px;color:#98a2ac;line-height:1.75;margin:0}.n25-kicker{color:#d8bb61;font-size:10px;font-weight:900;letter-spacing:.16em}.n25-actions{display:flex;gap:8px;flex-wrap:wrap}
      .n25-auth{display:flex;justify-content:space-between;gap:12px;align-items:center;border:1px solid #ffffff15;border-radius:13px;background:#0b0f14;padding:12px 14px;margin-bottom:12px}.n25-auth strong{display:block;font-size:11px}.n25-auth span{display:block;color:#77818c;font-size:9px;margin-top:3px}.n25-auth[data-state="ok"]{border-color:#47d78a44}.n25-auth[data-state="error"]{border-color:#ff9b8755}
      .n25-grid{display:grid;grid-template-columns:minmax(0,1.1fr) minmax(320px,.9fr);gap:12px;align-items:start}.n25-panel{border:1px solid #ffffff15;border-radius:15px;background:#0d1116;padding:17px}.n25-panel h2{font-size:21px;margin:0 0 5px}.n25-panel>.sub{color:#7f8993;font-size:9px;line-height:1.6;margin-bottom:14px}.n25-field{display:grid;gap:6px;margin-bottom:10px}.n25-field label{font-size:9px;color:#9aa4ae}.n25-field input,.n25-field select,.n25-field textarea{margin:0}.n25-field textarea{min-height:260px;resize:vertical;line-height:1.65}.n25-row{display:grid;grid-template-columns:1fr 1fr;gap:9px}.n25-form-actions{display:flex;gap:8px;align-items:center;flex-wrap:wrap}.n25-msg{min-height:18px;margin-top:10px;font-size:9px;color:#87919b}.n25-msg[data-kind="ok"]{color:#67dda0}.n25-msg[data-kind="error"]{color:#ff9b87}.n25-draft{color:#6e7882;font-size:8px;margin-left:auto}
      .n25-preview{min-height:220px;border:1px solid #ffffff10;border-radius:12px;background:#090d11;padding:15px;line-height:1.7}.n25-preview h2,.n25-preview h3,.n25-preview h4{margin:12px 0 6px}.n25-preview p{margin:8px 0;color:#c7cdd3}.n25-preview ul{padding-left:20px;color:#c7cdd3}.n25-count{color:#707a84;font-size:8px;margin-top:8px;text-align:right}
      .n25-summary{display:grid;grid-template-columns:repeat(3,1fr);gap:8px;margin:12px 0}.n25-stat{border:1px solid #ffffff10;border-radius:11px;background:#090d11;padding:12px}.n25-stat small{color:#77818b;font-size:8px}.n25-stat strong{display:block;font-size:20px;margin-top:4px}
      .n25-list{display:grid;gap:8px}.n25-item{border:1px solid #ffffff12;border-radius:12px;background:#090d11;padding:13px}.n25-item-head{display:flex;gap:8px;justify-content:space-between;align-items:flex-start}.n25-item h3{font-size:14px;margin:0}.n25-badge{font-size:8px;border-radius:999px;padding:4px 7px;background:#ffcb2d14;color:#d7bc68;white-space:nowrap}.n25-badge.ok{background:#47d78a14;color:#78e7ac}.n25-badge.danger{background:#ff7e6914;color:#ff9b87}.n25-meta{display:flex;gap:7px;flex-wrap:wrap;color:#717c86;font-size:8px;margin:7px 0}.n25-item p{font-size:9px;color:#929ca6;line-height:1.6;margin:7px 0}.n25-review{border-left:2px solid #ffcb2d55;padding-left:8px}.n25-item-actions{display:flex;gap:7px;flex-wrap:wrap;margin-top:9px}.n25-empty,.n25-state{min-height:190px;border:1px solid #ffffff10;border-radius:12px;background:#090d11;display:grid;place-items:center;text-align:center;padding:22px;color:#8c96a0}.n25-empty h3,.n25-state h2{color:#f1f3f5;margin:0 0 7px}.n25-empty p,.n25-state p{margin:0 0 12px;line-height:1.6}
      .n25-dialog{width:min(780px,calc(100% - 24px));max-height:85vh;overflow:auto;padding:20px;border:1px solid #ffffff33;border-radius:16px;background:#0d1116;color:#f5f6f7;overflow-wrap:anywhere}
      .n25-dialog::backdrop{background:#000b}.n25-dialog .btn{min-height:44px;font-size:16px}.n25-dialog h2{margin:0 0 12px}
      @media(max-width:900px){.n25-head{grid-template-columns:1fr}.n25-grid{grid-template-columns:1fr}}
      @media(max-width:620px){.n25-submit{padding-top:92px}.n25-actions{display:grid}.n25-row,.n25-summary{grid-template-columns:1fr}.n25-auth{align-items:flex-start;flex-direction:column}.n25-form-actions{display:grid}.n25-draft{margin-left:0}}
      /* N29 mobile: allow long customer-provided and published content to wrap safely. */
      .n25-submit .n25-grid>*,.n25-submit .n25-panel,.n25-submit .n25-item,.n25-submit .n25-item-head>*{min-width:0}
      .n25-submit .n25-preview,.n25-submit .n25-item,.n25-submit .n25-msg,.n25-submit .n25-state{overflow-wrap:anywhere}
    `;
    document.head.append(style);
  }

  function ensureSection() {
    let section = $('#n25SubmissionWorkbench');
    if (section) return section;
    section = node('section', 'page n25-submit');
    section.id = 'n25SubmissionWorkbench';
    section.setAttribute('role', 'main');
    section.tabIndex = -1;
    section.dataset.page = 'account-submit';
    section.append(node('div', 'shell'));
    const footer = $('.next-footer') || $('.footer');
    if (footer) footer.before(section); else document.body.append(section);
    return section;
  }

  function activate(section) {
    document.querySelectorAll('.page').forEach((page) => page.classList.toggle('active', page === section));
    document.documentElement.dataset.frontendPage = 'account-submit';
    document.documentElement.dataset.frontendNextStage = 'n25-submission-workbench';
  }

  function setMetadata() {
    const brand = text(window.XHFrontendShell?.configuration?.brandName) || '星火网游';
    document.title = `攻略投稿 · ${brand}`;
    let canonical = $('link[rel="canonical"]');
    if (!canonical) { canonical = document.createElement('link'); canonical.rel = 'canonical'; document.head.append(canonical); }
    canonical.href = 'https://wydlgf.cn/account/submit';
    let robots = $('meta[name="robots"]');
    if (!robots) { robots = document.createElement('meta'); robots.name = 'robots'; document.head.append(robots); }
    robots.content = 'noindex,nofollow';
  }

  function message(value, kind = '') {
    const target = $('#n25Message');
    if (!target) return;
    target.textContent = text(value);
    if (kind) target.dataset.kind = kind; else target.removeAttribute('data-kind');
  }

  async function api(url, options = {}) {
    const method = text(options.method || 'GET').toUpperCase();
    const init = {
      method,
      credentials: 'same-origin',
      cache: 'no-store',
      headers: { ...ACCOUNT_HEADERS }
    };
    if (options.body !== undefined) {
      init.headers['Content-Type'] = 'application/json';
      init.body = JSON.stringify(options.body);
    }
    const response = await fetch(url, init);
    let payload = null;
    try { payload = await response.json(); } catch (_) {}
    if (!response.ok) {
      const error = new Error(text(payload?.message) || text(payload?.error) || `请求失败（${response.status}）`);
      error.status = response.status;
      error.payload = payload;
      throw error;
    }
    return payload;
  }

  function fmt(value) {
    if (!value) return '—';
    const date = new Date(value);
    return Number.isNaN(date.getTime()) ? text(value) : date.toLocaleString('zh-CN', { hour12: false });
  }

  function renderMarkdown(raw, host) {
    host.replaceChildren();
    const source = String(raw || '');
    if (!source.trim()) {
      host.append(node('p', '', '开始输入后，可在此查看正文预览。'));
      return;
    }
    let bullets = null;
    source.split(/\r?\n/).forEach((line) => {
      const value = line.trim();
      if (!value) { bullets = null; return; }
      const heading = value.match(/^(#{1,3})\s+(.+)$/);
      if (heading) {
        bullets = null;
        host.append(node(`h${Math.min(4, heading[1].length + 1)}`, '', heading[2]));
        return;
      }
      const bullet = value.match(/^(?:[-*]|\d+\.)\s+(.+)$/);
      if (bullet) {
        if (!bullets) { bullets = node('ul'); host.append(bullets); }
        bullets.append(node('li', '', bullet[1].replace(/\*\*/g, '')));
        return;
      }
      bullets = null;
      host.append(node('p', '', value.replace(/\*\*/g, '')));
    });
  }

  function draftValue() {
    try { return JSON.parse(localStorage.getItem(DRAFT_KEY) || 'null'); } catch (_) { return null; }
  }

  function flushDraft() {
    clearTimeout(draftTimer);
    if (!draftPending) return;
    draftPending = false;
    try {
      localStorage.setItem(DRAFT_KEY, JSON.stringify({
        gameCode: text($('#n25Game')?.value) || text(draftValue()?.gameCode),
        title: text($('#n25Title')?.value),
        summary: text($('#n25Summary')?.value),
        contentMd: String($('#n25Content')?.value || ''),
        savedAt: Date.now()
      }));
      const target = $('#n25DraftState');
      if (target) target.textContent = '草稿已自动保存到本机';
    } catch (_) {
      const target = $('#n25DraftState');
      if (target) target.textContent = '本机草稿保存不可用，请保留当前页面';
    }
  }

  function saveDraft() {
    clearTimeout(draftTimer);
    draftPending = true;
    draftTimer = setTimeout(flushDraft, 350);
  }

  function discardDraft() {
    clearTimeout(draftTimer);
    draftPending = false;
    try { localStorage.removeItem(DRAFT_KEY); } catch (_) {}
  }

  function restoreDraft() {
    const draft = draftValue();
    if (!draft?.savedAt) return;
    const game = $('#n25Game');
    if (game && [...game.options].some((option) => option.value === draft.gameCode)) game.value = draft.gameCode || '';
    if ($('#n25Title')) $('#n25Title').value = draft.title || '';
    if ($('#n25Summary')) $('#n25Summary').value = draft.summary || '';
    if ($('#n25Content')) $('#n25Content').value = draft.contentMd || '';
    const target = $('#n25DraftState');
    if (target) target.textContent = `已恢复本机草稿 · ${fmt(draft.savedAt)}`;
    updatePreview();
  }

  function updatePreview() {
    const content = $('#n25Content');
    const preview = $('#n25Preview');
    if (!content || !preview) return;
    renderMarkdown(content.value, preview);
    const count = $('#n25Count');
    if (count) count.textContent = `${content.value.length} 字符`;
  }

  function summaryStats(root) {
    const active = submissions.filter((item) => ['SUBMITTED', 'IN_REVIEW', 'APPROVED'].includes(item.status)).length;
    const published = submissions.filter((item) => item.status === 'PUBLISHED').length;
    const stats = node('div', 'n25-summary');
    [['总投稿', submissions.length], ['处理中', active], ['已发布', published]].forEach(([label, value]) => {
      const card = node('div', 'n25-stat');
      card.append(node('small', '', label), node('strong', '', String(value)));
      stats.append(card);
    });
    root.append(stats);
  }

  function statusClass(status) {
    if (status === 'PUBLISHED' || status === 'APPROVED') return 'n25-badge ok';
    if (status === 'REJECTED' || status === 'WITHDRAWN') return 'n25-badge danger';
    return 'n25-badge';
  }

  async function withdraw(id, button) {
    if (!confirm('确定撤回这篇投稿吗？撤回后将退出当前审核流程。')) return;
    button.disabled = true;
    try {
      await api(`/api/account/submissions/${encodeURIComponent(id)}/withdraw`, { method: 'POST', body: {} });
      message('投稿已撤回。', 'ok');
      await loadMine();
    } catch (error) {
      message(error.message || '撤回失败。', 'error');
    } finally {
      button.disabled = false;
    }
  }

  function renderMine(root) {
    root.replaceChildren();
    root.append(node('h2', '', '我的投稿'));
    root.append(node('p', 'sub', '查看审核状态、审核意见与已发布攻略；提交中的内容可受控撤回。'));
    summaryStats(root);
    const host = node('div', 'n25-list');
    if (!submissions.length) {
      const empty = node('div', 'n25-empty');
      const box = node('div');
      box.append(node('h3', '', '还没有投稿记录'), node('p', '', '完成左侧第一篇攻略后，审核状态会显示在这里。'));
      empty.append(box); host.append(empty); root.append(host); return;
    }
    submissions.forEach((item) => {
      const card = node('article', 'n25-item');
      const head = node('div', 'n25-item-head');
      head.append(node('h3', '', text(item.title) || '(无标题)'), node('span', statusClass(item.status), text(item.statusText) || STATUS[item.status] || '处理中'));
      const meta = node('div', 'n25-meta');
      meta.append(node('span', '', gameNames.get(item.gameCode) || text(item.gameCode) || '游戏攻略'));
      meta.append(node('span', '', `更新：${fmt(item.updatedAt || item.createdAt)}`));
      card.append(head, meta);
      if (text(item.summary)) card.append(node('p', '', text(item.summary)));
      if (text(item.reviewNote)) card.append(node('p', 'n25-review', `审核意见：${text(item.reviewNote)}`));
      const actions = node('div', 'n25-item-actions');
      if (text(item.publishedSlug)) {
        const published = node('a', 'btn', '查看已发布攻略');
        published.href = `/next/guides/${encodeURIComponent(item.publishedSlug)}`;
        actions.append(published);
      }
      const detail = node('button', 'btn', '查看正文');
      detail.type = 'button';
      detail.addEventListener('click', async () => {
        detail.disabled = true;
        try {
          const data = await api(`/api/account/submissions/${encodeURIComponent(item.id)}`);
          const dialog = document.createElement('dialog');
          dialog.className = 'account-confirm n25-dialog';
          dialog.setAttribute('aria-labelledby', 'n25DetailTitle');
          const panel = node('div', 'account-confirm-panel');
          const title = node('h2', '', text(data.title) || '投稿正文'); title.id = 'n25DetailTitle';
          panel.append(title);
          if (text(data.summary)) panel.append(node('p', '', text(data.summary)));
          const preview = node('div', 'n25-preview');
          renderMarkdown(data.contentMd, preview);
          panel.append(preview);
          const close = node('button', 'btn', '关闭'); close.type = 'button'; close.addEventListener('click', () => dialog.close());
          panel.append(close); dialog.append(panel); document.body.append(dialog);
          dialog.addEventListener('close', () => { dialog.remove(); detail.focus({preventScroll:true}); }, { once: true });
          dialog.showModal();
        } catch (error) { message(error.message || '正文读取失败。', 'error'); }
        finally { detail.disabled = false; }
      });
      actions.append(detail);
      if (item.status === 'SUBMITTED' || item.status === 'IN_REVIEW') {
        const button = node('button', 'btn', '撤回'); button.type = 'button'; button.addEventListener('click', () => withdraw(item.id, button)); actions.append(button);
      }
      card.append(actions); host.append(card);
    });
    root.append(host);
  }

  function showGameError(error) {
    const select = $('#n25Game');
    if (!select) return;
    let state = $('#n25GameLoadState');
    if (!state) {
      state = node('div', 'n25-game-error');
      state.id = 'n25GameLoadState';
      state.setAttribute('role', 'alert');
      select.after(state);
    }
    const retry = node('button', 'btn', '重新读取游戏列表');
    retry.type = 'button';
    retry.addEventListener('click', async () => {
      retry.disabled = true;
      retry.textContent = '读取中…';
      try { await loadGames(); }
      catch (retryError) { showGameError(retryError); message('游戏列表读取失败，可再次重试。', 'error'); }
    });
    state.replaceChildren(node('p', '', text(error?.message) || '游戏列表暂时无法读取。'), retry);
  }

  async function loadGames() {
    const select = $('#n25Game');
    if (!select) return;
    const payload = await api('/api/catalog');
    if (!Array.isArray(payload?.games)) throw new Error('游戏目录返回异常，请重新读取。');
    const games = payload.games;
    const selected = text(select.value) || text(draftValue()?.gameCode);
    select.replaceChildren();
    const blank = document.createElement('option'); blank.value = ''; blank.textContent = games.length ? '请选择游戏' : '暂无可投稿游戏'; select.append(blank);
    gameNames.clear();
    games.forEach((game) => {
      const code = text(game.code); if (!code) return;
      gameNames.set(code, text(game.name) || code);
      const option = document.createElement('option'); option.value = code; option.textContent = text(game.name) || code; select.append(option);
    });
    if ([...select.options].some((option) => option.value === selected)) select.value = selected;
    $('#n25GameLoadState')?.remove();
  }

  async function loadMine() {
    const root = $('#n25Mine');
    if (!root) return;
    try {
      submissions = list(await api('/api/account/submissions?size=50'));
      renderMine(root);
    } catch (error) {
      if (error.status === 401) return showGuest();
      root.replaceChildren();
      const state = node('div', 'n25-state'); const box = node('div');
      box.append(node('h2', '', '投稿记录读取失败'), node('p', '', error.message || '请稍后重试。'));
      const retry = node('button', 'btn', '重试'); retry.type = 'button'; retry.addEventListener('click', loadMine); box.append(retry); state.append(box); root.append(state);
    }
  }

  function showGuest() {
    const auth = $('#n25Auth');
    if (auth) {
      auth.dataset.state = 'guest';
      $('#n25AuthLead').textContent = '需要登录后投稿';
      $('#n25AuthText').textContent = '登录后可创建投稿、查看审核状态与撤回自己的内容。';
      const action = $('#n25AuthAction'); action.hidden = false; action.href = '/next/account?next=%2Fnext%2Faccount%2Fsubmit'; action.textContent = '登录 / 注册';
    }
    accountReady = false;
    $('#n25AuthAction')?.removeAttribute('onclick');
    if ($('#n25AuthAction')) $('#n25AuthAction').onclick = null;
    const submit = $('#n25Submit'); if (submit) submit.disabled = true;
  }

  async function loadAccount() {
    try {
      const me = await api('/api/account/me');
      const auth = $('#n25Auth');
      if (auth) auth.dataset.state = 'ok';
      $('#n25AuthLead').textContent = `投稿账号：${text(me?.displayName) || '已登录用户'}`;
      $('#n25AuthText').textContent = me?.emailVerified ? '安全邮箱已验证 · 投稿归属于当前账号' : '投稿归属于当前账号';
      $('#n25AuthAction').hidden = true;
      $('#n25AuthAction').onclick = null;
      accountReady = true;
      $('#n25Submit').disabled = false;
      return true;
    } catch (error) {
      if (error.status === 401) { showGuest(); return false; }
      const auth = $('#n25Auth'); if (auth) auth.dataset.state = 'error';
      accountReady = false;
      $('#n25Submit').disabled = true;
      $('#n25AuthLead').textContent = '账号状态暂不可用'; $('#n25AuthText').textContent = '请重试账号状态，当前稿件会保留。';
      const action = $('#n25AuthAction');
      action.hidden = false; action.textContent = '重试账号状态'; action.href = '#';
      action.onclick = async event => {
        event.preventDefault();
        if (await loadAccount()) await loadMine();
      };
      return false;
    }
  }

  function buildUi(section) {
    const shell = $('.shell', section); shell.replaceChildren();
    const head = node('div', 'n25-head');
    const intro = node('div'); intro.append(node('div', 'n25-kicker', '创作与投稿'), node('h1', '', '攻略投稿工作台')); intro.append(node('p', '', '撰写攻略、自动保存本机草稿、安全预览正文，并跟踪审核与发布状态。'));
    const actions = node('div', 'n25-actions');
    const legacy = node('a', 'btn', '重新打开投稿页'); legacy.href = '/account/submit'; legacy.dataset.n25Legacy = '1';
    const assets = node('a', 'btn gold', '返回我的投稿'); assets.href = '/next/account/submissions'; actions.append(legacy, assets); head.append(intro, actions); shell.append(head);

    const auth = node('div', 'n25-auth'); auth.id = 'n25Auth'; auth.dataset.state = 'checking';
    const authCopy = node('div'); const lead = node('strong', '', '正在检查账号状态'); lead.id = 'n25AuthLead'; const copy = node('span', '', '投稿需要归属到登录账号。'); copy.id = 'n25AuthText'; authCopy.append(lead, copy);
    const authAction = node('a', 'btn', '登录 / 注册'); authAction.id = 'n25AuthAction'; authAction.hidden = true; auth.append(authCopy, authAction); shell.append(auth);

    const grid = node('div', 'n25-grid');
    const editor = node('section', 'n25-panel'); editor.append(node('h2', '', '新建攻略'), node('p', 'sub', '提交后可查看审核状态或撤回。未提交内容会自动保存到当前浏览器。'));
    const form = document.createElement('form'); form.id = 'n25Form'; form.noValidate = true;
    const row = node('div', 'n25-row');
    const gameField = node('div', 'n25-field'); gameField.append(labelFor('游戏 *', 'n25Game')); const game = document.createElement('select'); game.id = 'n25Game'; game.required = true; gameField.append(game);
    const titleField = node('div', 'n25-field'); titleField.append(labelFor('标题 *', 'n25Title')); const title = document.createElement('input'); title.id = 'n25Title'; title.required = true; title.maxLength = 180; title.placeholder = '例如：多人迷彩路线与注意事项'; titleField.append(title); row.append(gameField, titleField); form.append(row);
    const summaryField = node('div', 'n25-field'); summaryField.append(labelFor('摘要', 'n25Summary')); const summary = document.createElement('input'); summary.id = 'n25Summary'; summary.maxLength = 500; summary.placeholder = '用一句话说明攻略适合谁、解决什么问题'; summaryField.append(summary); form.append(summaryField);
    const contentField = node('div', 'n25-field'); contentField.append(labelFor('正文 *', 'n25Content')); const content = document.createElement('textarea'); content.id = 'n25Content'; content.required = true; content.placeholder = '# 一级标题\n\n- 支持标题、列表和普通段落\n- 请勿填写账号密码、验证码等敏感信息'; contentField.append(content); form.append(contentField);
    const formActions = node('div', 'n25-form-actions'); const submit = node('button', 'btn gold', '提交审核'); submit.type = 'submit'; submit.id = 'n25Submit'; submit.disabled = true; const clear = node('button', 'btn', '清空本机草稿'); clear.type = 'button'; clear.id = 'n25ClearDraft'; const draft = node('span', 'n25-draft', '草稿等待输入'); draft.id = 'n25DraftState'; formActions.append(submit, clear, draft); form.append(formActions);
    const msg = node('div', 'n25-msg'); msg.id = 'n25Message'; msg.setAttribute('role', 'status'); form.append(msg); editor.append(form);
    const previewPanel = node('section', 'n25-panel'); previewPanel.append(node('h2', '', '安全预览'), node('p', 'sub', '预览标题、列表和正文，核对后再提交。')); const preview = node('div', 'n25-preview'); preview.id = 'n25Preview'; previewPanel.append(preview); const count = node('div', 'n25-count', '0 字符'); count.id = 'n25Count'; previewPanel.append(count);
    const left = node('div'); left.append(editor, previewPanel);
    const mine = node('section', 'n25-panel'); mine.id = 'n25Mine';
    grid.append(left, mine); shell.append(grid);

    [game, title, summary, content].forEach((input) => { input.addEventListener('input', () => { input.removeAttribute('aria-invalid'); input.removeAttribute('aria-describedby'); updatePreview(); saveDraft(); }); input.addEventListener('change', () => { input.removeAttribute('aria-invalid'); input.removeAttribute('aria-describedby'); saveDraft(); }); });
    clear.addEventListener('click', () => {
      if (!confirm('确定清空当前设备上的未提交草稿吗？')) return;
      form.reset(); discardDraft(); draft.textContent = '草稿已清空'; updatePreview();
    });
    form.addEventListener('submit', submitForm);
    updatePreview();
  }

  function invalidField(id, detail) {
    message(detail, 'error');
    const control = document.getElementById(id);
    control.setAttribute('aria-invalid', 'true');
    control.setAttribute('aria-describedby', 'n25Message');
    control.focus();
  }

  async function submitForm(event) {
    event.preventDefault();
    if (submitting || !accountReady) return;
    const button = $('#n25Submit');
    const body = { gameCode: text($('#n25Game')?.value), title: text($('#n25Title')?.value), summary: text($('#n25Summary')?.value), contentMd: String($('#n25Content')?.value || '') };
    if (!body.gameCode) return invalidField('n25Game', '请选择投稿对应的游戏。');
    if (!body.title) return invalidField('n25Title', '请填写标题。');
    if (!body.contentMd.trim()) return invalidField('n25Content', '正文不能为空。');
    submitting = true;
    flushDraft();
    const fields = [...$('#n25Form').elements];
    fields.forEach(field => { field.disabled = true; });
    button.disabled = true; button.textContent = '提交中…'; message('正在安全提交投稿…');
    try {
      await api('/api/account/submissions', { method: 'POST', body });
      $('#n25Form').reset(); discardDraft();
      $('#n25DraftState').textContent = '投稿已提交'; updatePreview(); message('投稿已提交，我们会尽快审核。', 'ok'); await loadMine();
    } catch (error) {
      if (error.status === 401) showGuest();
      message(error.message || '投稿失败，请稍后重试。', 'error');
    } finally { submitting = false; fields.forEach(field => { field.disabled = false; }); button.disabled = !accountReady; button.textContent = '提交审核'; }
  }

  async function load() {
    const section = ensureSection(); activate(section); installStyles(); setMetadata(); buildUi(section);
    restoreDraft();
    const token = ++loadToken;
    try { await loadGames(); } catch (error) { if (token === loadToken) { showGameError(error); message('游戏列表读取失败，可在游戏选择处重试。', 'error'); } }
    const logged = await loadAccount();
    if (token !== loadToken) return;
    if (logged) await loadMine(); else renderMine($('#n25Mine'));
  }

  window.addEventListener('pagehide', flushDraft);
  document.addEventListener('visibilitychange', () => { if (document.hidden) flushDraft(); });
  window.XHFrontendNextN25 = { reload: load, get state() { return { submissions: [...submissions], draft: draftValue() }; } };
  load();
})();
