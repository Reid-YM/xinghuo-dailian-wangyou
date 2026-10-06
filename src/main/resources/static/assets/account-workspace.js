(() => {
  'use strict';

  const shell = document.querySelector('#accountShell');
  if (!shell) return;

  const quoteHost = document.querySelector('[data-account-quotes]');
  const quoteMsg = document.querySelector('#quoteWorkspaceMsg');
  const orderHost = document.querySelector('[data-order-list]');
  const orderMsg = document.querySelector('#orderMsg');

  function el(tag, className, text) {
    const node = document.createElement(tag);
    if (className) node.className = className;
    if (text !== undefined && text !== null) node.textContent = String(text);
    return node;
  }

  function clear(node) {
    if (!node) return;
    while (node.firstChild) node.removeChild(node.firstChild);
  }

  function fmt(value) {
    if (!value) return '—';
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return String(value);
    return date.toLocaleString('zh-CN', { hour12: false });
  }

  function notify(host, message, error) {
    if (!host) return;
    host.textContent = message || '';
    host.hidden = !message;
    host.classList.toggle('error', !!error);
  }

  async function request(path, options = {}) {
    const headers = new Headers(options.headers || {});
    headers.set('Accept', 'application/json');
    headers.set('X-Requested-With', 'fetch');
    const init = { method: options.method || 'GET', headers, credentials: 'same-origin', cache: 'no-store' };
    if (options.body !== undefined) {
      headers.set('Content-Type', 'application/json');
      init.body = JSON.stringify(options.body);
    }
    const response = await fetch(path, init);
    if (response.status === 401) {
      location.assign('/account/login?next=' + encodeURIComponent(location.pathname + location.hash));
      throw new Error('登录状态已失效');
    }
    let payload = null;
    try { payload = await response.json(); } catch (ignored) { /* no-op */ }
    if (!response.ok) throw new Error(payload?.message || '请求失败，请稍后重试');
    return payload;
  }

  async function supplement(kind, businessNo, host) {
    const label = kind === 'quotes' ? '需求' : '订单';
    const content = window.prompt(`补充${label}说明（最多 1200 字；不要填写密码、验证码或令牌）`);
    if (!content || !content.trim()) return;
    const trimmed = content.trim();
    if (trimmed.length > 1200) {
      notify(host, '补充说明不能超过 1200 字。', true);
      return;
    }
    try {
      await request(`/api/console/${kind}/${encodeURIComponent(businessNo)}/supplement`, {
        method: 'POST',
        body: { content: trimmed }
      });
      notify(host, '补充说明已提交，客服会在对应业务记录中看到。', false);
      if (kind === 'quotes') await loadQuotes();
    } catch (error) {
      notify(host, error.message || '补充说明提交失败。', true);
    }
  }

  function quoteCard(quote) {
    const card = el('article', 'account-order');
    const main = el('div', 'account-order-main');
    const title = el('div', 'account-order-title');
    title.appendChild(el('strong', null, `${quote.game || '游戏服务'} · ${quote.demand || '需求'}`));
    title.appendChild(el('span', 'badge', quote.statusText || quote.status || '处理中'));
    main.appendChild(title);

    const meta = el('div', 'account-order-sub');
    meta.appendChild(el('span', 'mono', quote.businessNo || '业务编号待同步'));
    if (quote.platform) meta.appendChild(el('span', null, quote.platform));
    meta.appendChild(el('span', null, `更新于 ${fmt(quote.updatedAt || quote.createdAt)}`));
    main.appendChild(meta);

    const description = el('p', 'account-inline-note', quote.nextAction || quote.currentProgress || '等待客服处理');
    main.appendChild(description);
    card.appendChild(main);

    const actions = el('div', 'account-order-action');
    if (quote.businessNo) {
      const supplementButton = el('button', 'btn ghost small-btn', '补充说明');
      supplementButton.type = 'button';
      supplementButton.addEventListener('click', () => supplement('quotes', quote.businessNo, quoteMsg));
      actions.appendChild(supplementButton);
    }
    const support = el('a', 'btn ghost small-btn', '需求客服');
    support.href = '/support/game';
    actions.appendChild(support);
    card.appendChild(actions);
    return card;
  }

  async function loadQuotes() {
    if (!quoteHost) return;
    notify(quoteMsg, '', false);
    clear(quoteHost);
    quoteHost.appendChild(el('div', 'account-skeleton', '正在读取需求记录…'));
    try {
      const data = await request('/api/console/quotes');
      const items = Array.isArray(data?.items) ? data.items : [];
      clear(quoteHost);
      if (!items.length) {
        const empty = el('div', 'empty');
        empty.appendChild(el('p', null, '当前账号还没有询价需求。'));
        const link = el('a', 'btn primary', '提交新需求');
        link.href = '/quote';
        empty.appendChild(link);
        quoteHost.appendChild(empty);
        return;
      }
      const list = el('div', 'account-order-list');
      items.forEach((item) => list.appendChild(quoteCard(item)));
      quoteHost.appendChild(list);
    } catch (error) {
      clear(quoteHost);
      quoteHost.appendChild(el('div', 'empty', error.message || '需求记录加载失败。'));
      notify(quoteMsg, error.message || '需求记录加载失败。', true);
    }
  }

  function augmentOrderCards() {
    if (!orderHost) return;
    orderHost.querySelectorAll('.account-order').forEach((card) => {
      if (card.querySelector('[data-account-order-supplement]')) return;
      const businessNo = card.querySelector('.mono')?.textContent?.trim();
      const actions = card.querySelector('.account-order-action');
      if (!businessNo || !actions || businessNo.includes('待同步')) return;
      const button = el('button', 'btn ghost small-btn', '补充说明');
      button.type = 'button';
      button.dataset.accountOrderSupplement = businessNo;
      button.addEventListener('click', () => supplement('orders', businessNo, orderMsg));
      actions.appendChild(button);
    });
  }

  if (orderHost) {
    new MutationObserver(augmentOrderCards).observe(orderHost, { childList: true, subtree: true });
    augmentOrderCards();
  }

  loadQuotes();
})();
