(() => {
  'use strict';

  const $ = (selector, root = document) => root.querySelector(selector);
  const text = (value) => String(value ?? '').trim();
  const page = $('.page[data-page="account"]');
  if (!page) return;

  let observer = null;
  let mountTimer = null;

  function node(tag, className, value) {
    const element = document.createElement(tag);
    if (className) element.className = className;
    if (value !== undefined && value !== null) element.textContent = String(value);
    return element;
  }

  function append(parent, ...children) {
    children.filter(Boolean).forEach((child) => parent.append(child));
    return parent;
  }

  function safeKey(value) {
    return encodeURIComponent(text(value));
  }

  function installStyles() {
    if ($('#frontend-next-n5-actions-style')) return;
    const style = document.createElement('style');
    style.id = 'frontend-next-n5-actions-style';
    style.textContent = `
      .n5a-center{margin-top:18px;padding-top:18px;border-top:1px solid #ffffff12;display:grid;gap:12px}.n5a-head{display:flex;justify-content:space-between;gap:12px;align-items:start;flex-wrap:wrap}.n5a-head h4{margin:4px 0 0;font-size:15px}.n5a-head p{margin:0;color:#737c86;font-size:9px;line-height:1.55;max-width:620px}.n5a-grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:10px}.n5a-card{border:1px solid #ffffff12;border-radius:12px;background:#090d11;padding:13px;min-width:0}.n5a-card h5{margin:0 0 6px;font-size:11px}.n5a-card p{margin:0 0 9px;color:#737c86;font-size:9px;line-height:1.5}.n5a-field{display:grid;gap:6px}.n5a-field label{font-size:9px;color:#939ca6}.n5a-field textarea,.n5a-field select{width:100%;border:1px solid #ffffff19;background:#070a0e;color:#fff;border-radius:9px;padding:10px;font:inherit;font-size:10px}.n5a-field textarea{resize:vertical;min-height:88px;line-height:1.55}.n5a-field textarea:focus,.n5a-field select:focus{outline:none;border-color:#ffcb2d88;box-shadow:0 0 0 3px #ffcb2d12}.n5a-meta{display:flex;justify-content:space-between;gap:8px;color:#626b75;font-size:8px;margin-top:5px}.n5a-submit{appearance:none;border:1px solid #ffcb2d77;background:#ffcb2d;color:#17130a;border-radius:9px;padding:9px 11px;font:700 10px/1 inherit;cursor:pointer;margin-top:9px}.n5a-submit:hover:not(:disabled){filter:brightness(1.06)}.n5a-submit:disabled{opacity:.55;cursor:not-allowed}.n5a-followups{display:grid;gap:9px}.n5a-followup{border:1px solid #ffffff10;border-radius:10px;padding:11px}.n5a-followup-title{display:flex;justify-content:space-between;gap:8px;align-items:start;margin-bottom:7px}.n5a-followup-title b{font-size:10px}.n5a-followup-title span{font-size:8px;color:#7f8993}.n5a-toast{position:fixed;right:22px;bottom:22px;z-index:9999;max-width:min(420px,calc(100vw - 32px));border:1px solid #4ade9166;background:#0b1711;color:#9bf3bd;border-radius:12px;padding:12px 14px;box-shadow:0 18px 45px #0009;font-size:10px;line-height:1.55}.n5a-toast.error{border-color:#ff866766;background:#1a0e0b;color:#ffb09d}.n5a-empty{border:1px dashed #ffffff18;border-radius:10px;padding:12px;color:#717b85;font-size:9px;line-height:1.55}
      @media(max-width:760px){.n5a-grid{grid-template-columns:1fr}.n5a-toast{right:16px;bottom:16px}}
    `;
    document.head.append(style);
  }

  function toast(message, kind = '') {
    let host = $('#n5ActionsToast');
    if (!host) {
      host = node('div', 'n5a-toast');
      host.id = 'n5ActionsToast';
      host.setAttribute('role', 'status');
      host.setAttribute('aria-live', 'polite');
      document.body.append(host);
    }
    host.className = `n5a-toast ${kind}`.trim();
    host.textContent = message;
    window.setTimeout(() => {
      if (host.isConnected && host.textContent === message) host.remove();
    }, 4500);
  }

  async function request(url, options = {}) {
    const headers = Object.assign({ Accept: 'application/json' }, options.headers || {});
    const response = await fetch(url, Object.assign({
      credentials: 'same-origin',
      cache: 'no-store'
    }, options, { headers }));
    let body = null;
    try { body = await response.json(); } catch (ignore) { body = null; }
    if (!response.ok) {
      const error = new Error(text(body?.message) || text(body?.error) || `请求失败（${response.status}）`);
      error.status = response.status;
      throw error;
    }
    return body;
  }

  async function postJson(url, body) {
    return request(url, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'X-Requested-With': 'fetch'
      },
      body: JSON.stringify(body)
    });
  }

  async function refreshAfterWrite(message) {
    toast(message || '已提交，业务数据正在刷新。');
    try {
      if (window.XHFrontendNextN5?.reload) await window.XHFrontendNextN5.reload();
    } catch (ignore) {
      // The write already succeeded. The user can reopen the detail to refresh manually.
    }
  }

  function counter(textarea, max) {
    const meta = node('div', 'n5a-meta');
    const note = node('span', '', `最多 ${max} 字`);
    const count = node('span', '', `0 / ${max}`);
    const update = () => { count.textContent = `${textarea.value.length} / ${max}`; };
    textarea.addEventListener('input', update);
    append(meta, note, count);
    return meta;
  }

  function formCard(title, description) {
    const card = node('section', 'n5a-card');
    append(card, node('h5', '', title), node('p', '', description));
    return card;
  }

  function addTextareaForm(card, options) {
    const form = document.createElement('form');
    form.dataset.n5Action = options.action;
    const field = node('div', 'n5a-field');
    const label = node('label', '', options.label);
    const textarea = document.createElement('textarea');
    textarea.maxLength = options.max;
    textarea.required = true;
    textarea.placeholder = options.placeholder;
    append(field, label, textarea, counter(textarea, options.max));
    const submit = node('button', 'n5a-submit', options.submitLabel);
    submit.type = 'submit';
    append(form, field, submit);
    form.addEventListener('submit', async (event) => {
      event.preventDefault();
      const content = text(textarea.value);
      if (!content) {
        toast('请先填写需要提交的内容。', 'error');
        textarea.focus();
        return;
      }
      submit.disabled = true;
      const original = submit.textContent;
      submit.textContent = '正在提交…';
      try {
        const result = await options.submit(content);
        textarea.value = '';
        textarea.dispatchEvent(new Event('input'));
        await refreshAfterWrite(text(result?.message) || options.successMessage);
      } catch (error) {
        toast(text(error?.message) || '提交失败，请稍后重试。', 'error');
      } finally {
        if (submit.isConnected) {
          submit.disabled = false;
          submit.textContent = original;
        }
      }
    });
    card.append(form);
  }

  function quoteActions(detail) {
    const businessNo = text(detail?.businessNo);
    if (!businessNo) return null;
    const card = formCard('补充询价说明', '补充新的需求、平台变化或时间要求。内容会追加到原询价，不覆盖之前记录。');
    addTextareaForm(card, {
      action: 'quote-supplement', label: '补充内容', max: 1200,
      placeholder: '例如：希望优先处理某个武器，时间调整到本周末前。',
      submitLabel: '提交询价补充', successMessage: '询价补充已提交。',
      submit: (content) => postJson(`/api/console/quotes/${safeKey(businessNo)}/supplement`, { content })
    });
    return card;
  }

  function orderSupplement(detail) {
    const order = detail?.order || {};
    const businessNo = text(order.businessNo) || text(order.orderNo);
    if (!businessNo) return null;
    const card = formCard('补充订单说明', '用于补充执行偏好或需要客服留意的信息，不会修改订单状态和当前进度。');
    addTextareaForm(card, {
      action: 'order-supplement', label: '补充内容', max: 1200,
      placeholder: '例如：今晚不方便登录，请明天继续。',
      submitLabel: '提交订单补充', successMessage: '订单补充说明已提交。',
      submit: (content) => postJson(`/api/console/orders/${safeKey(businessNo)}/supplement`, { content })
    });
    return card;
  }

  function afterSalesForm(detail) {
    const order = detail?.order || {};
    const businessNo = text(order.businessNo) || text(order.orderNo);
    if (!businessNo) return null;
    const card = formCard('提交售后问题', '出现交付、进度、取消或退款核对问题时，在订单下建立正式问题记录。');
    const form = document.createElement('form');
    form.dataset.n5Action = 'after-sales';
    const categoryField = node('div', 'n5a-field');
    const category = document.createElement('select');
    [['DELIVERY', '交付或进度'], ['CANCELLATION', '取消服务'], ['REFUND_REVIEW', '退款核对'], ['OTHER', '其他问题']].forEach(([value, label]) => {
      const option = document.createElement('option'); option.value = value; option.textContent = label; category.append(option);
    });
    append(categoryField, node('label', '', '问题类型'), category);
    const contentField = node('div', 'n5a-field');
    const content = document.createElement('textarea');
    content.maxLength = 240; content.required = true; content.placeholder = '请描述具体情况，避免填写账号密码、验证码等敏感信息。';
    append(contentField, node('label', '', '问题说明'), content, counter(content, 240));
    const submit = node('button', 'n5a-submit', '提交售后问题'); submit.type = 'submit';
    append(form, categoryField, contentField, submit);
    form.addEventListener('submit', async (event) => {
      event.preventDefault();
      const value = text(content.value);
      if (!value) {
        toast('请填写售后问题说明。', 'error');
        content.focus();
        return;
      }
      submit.disabled = true; submit.textContent = '正在提交…';
      try {
        const result = await postJson(`/api/console/orders/${safeKey(businessNo)}/after-sales`, {
          category: category.value,
          content: value
        });
        content.value = ''; content.dispatchEvent(new Event('input'));
        await refreshAfterWrite(text(result?.message) || '售后问题已提交。');
      } catch (error) {
        toast(text(error?.message) || '售后问题提交失败，请稍后重试。', 'error');
      } finally {
        if (submit.isConnected) { submit.disabled = false; submit.textContent = '提交售后问题'; }
      }
    });
    card.append(form);
    return card;
  }

  function followupCard(detail) {
    const order = detail?.order || {};
    const businessNo = text(order.businessNo) || text(order.orderNo);
    const cases = Array.isArray(detail?.afterSalesCases) ? detail.afterSalesCases.filter((item) => Number(item?.recordId) > 0) : [];
    if (!businessNo || !cases.length) return null;
    const card = formCard('售后追问 / 补充', '在原售后记录中继续补充信息，客服处理历史会保留在同一条记录下。');
    const host = node('div', 'n5a-followups');
    cases.forEach((item) => {
      const recordId = Number(item.recordId);
      const wrap = node('div', 'n5a-followup');
      const title = node('div', 'n5a-followup-title');
      append(title,
        node('b', '', `${text(item.category) || '售后问题'} · #${recordId}`),
        node('span', '', `${text(item.status) || 'OPEN'}${item.pendingCustomerFollowup ? ' · 待客服跟进' : ''}`)
      );
      const form = document.createElement('form');
      form.dataset.n5Action = 'after-sales-followup';
      form.dataset.recordId = String(recordId);
      const field = node('div', 'n5a-field');
      const input = document.createElement('textarea');
      input.maxLength = 240; input.required = true; input.placeholder = '补充新的情况或对客服回复继续说明。';
      append(field, node('label', '', '补充说明'), input, counter(input, 240));
      const submit = node('button', 'n5a-submit', '提交到原记录'); submit.type = 'submit';
      append(form, field, submit);
      form.addEventListener('submit', async (event) => {
        event.preventDefault();
        const content = text(input.value);
        if (!content) { toast('请填写补充说明。', 'error'); input.focus(); return; }
        submit.disabled = true; submit.textContent = '正在提交…';
        try {
          const result = await postJson(
            `/api/console/orders/${safeKey(businessNo)}/after-sales/records/${recordId}/followups`,
            { content }
          );
          input.value = ''; input.dispatchEvent(new Event('input'));
          await refreshAfterWrite(text(result?.message) || '售后补充已提交。');
        } catch (error) {
          toast(text(error?.message) || '补充说明提交失败，请稍后重试。', 'error');
        } finally {
          if (submit.isConnected) { submit.disabled = false; submit.textContent = '提交到原记录'; }
        }
      });
      append(wrap, title, form); host.append(wrap);
    });
    card.append(host);
    return card;
  }

  function detailKey(detail) {
    if (detail?.order) return `order:${text(detail.order.businessNo) || text(detail.order.orderNo)}`;
    if (detail?.businessNo) return `quote:${text(detail.businessNo)}`;
    return '';
  }

  function mount() {
    const detailHost = $('#n5Detail');
    const detail = window.XHFrontendNextN5?.state?.detail;
    if (!detailHost || detailHost.hidden || !detail) {
      $('#n5ActionCenter')?.remove();
      return;
    }
    const key = detailKey(detail);
    if (!key) return;
    const existing = $('#n5ActionCenter');
    if (existing?.dataset.key === key) return;
    existing?.remove();

    const center = node('section', 'n5a-center');
    center.id = 'n5ActionCenter'; center.dataset.key = key;
    const head = node('div', 'n5a-head');
    const copy = node('div');
    append(copy, node('div', 'kicker', 'N5 · CUSTOMER ACTIONS'), node('h4', '', '业务补充与售后操作'), node('p', '', '写入动作直接提交到当前账号对应的生产业务记录；提交成功后会刷新个人中心数据。请勿填写密码、短信验证码或其他登录凭据。'));
    head.append(copy); center.append(head);
    const grid = node('div', 'n5a-grid');
    if (detail.order) {
      append(grid, orderSupplement(detail), afterSalesForm(detail), followupCard(detail));
    } else {
      append(grid, quoteActions(detail));
    }
    if (!grid.children.length) grid.append(node('div', 'n5a-empty', '当前记录暂无可执行的用户操作。'));
    center.append(grid); detailHost.append(center);
  }

  function scheduleMount() {
    if (mountTimer) clearTimeout(mountTimer);
    mountTimer = setTimeout(mount, 45);
  }

  installStyles();
  observer = new MutationObserver(scheduleMount);
  observer.observe(page, { childList: true, subtree: true, attributes: true, attributeFilter: ['hidden'] });
  window.addEventListener('pagehide', () => {
    if (observer) observer.disconnect();
    if (mountTimer) clearTimeout(mountTimer);
  });
  window.XHFrontendNextN5Actions = { mount };
  scheduleMount();
})();