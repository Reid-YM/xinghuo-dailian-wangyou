(() => {
  'use strict';

  const display = value => window.XWLabels?.value(value) ?? value;
  const $ = selector => document.querySelector(selector);
  const base = '/api/admin/xingwei/payments';
  const requestedOrder = new URLSearchParams(window.location.search).get('order')?.trim() || '';

  let page = 0;
  let busy = false;
  let sequence = 0;
  let state = {};
  let order = null;

  const node = (tag, text, cls) => {
    const element = document.createElement(tag);
    if (text != null) element.textContent = String(text);
    if (cls) element.className = cls;
    return element;
  };

  const message = text => {
    $('#paymentMessage').textContent = text || '';
  };

  async function api(path = '', options = {}) {
    const headers = new Headers(options.headers || {});
    if (options.method && options.method !== 'GET') {
      headers.set(document.querySelector('meta[name=_csrf_header]').content,
        document.querySelector('meta[name=_csrf]').content);
    }
    if (options.body) {
      headers.set('Content-Type', 'application/json');
      options = {...options, body: JSON.stringify(options.body)};
    }
    const response = await fetch(base + path, {
      ...options,
      headers,
      credentials: 'same-origin',
      cache: 'no-store'
    });
    let result = {};
    try { result = await response.json(); } catch (_) {}
    if (!response.ok) {
      throw new Error(result.message || `支付操作失败（${response.status}），请重新读取核对。`);
    }
    return result;
  }

  const revision = value => ({'If-Match': `"${value}"`});

  function renderPrepareAvailability() {
    $('#paymentPrepare').hidden = false;
    const status = $('#paymentPreparePermission');
    if (!state.enabled) {
      status.textContent = '真实收款开关当前关闭：可以读取订单和历史记录，但不能建立或关闭待支付单。';
      $('#paymentPrepareForm').hidden = true;
      return;
    }
    if (!state.canManage) {
      status.textContent = '当前账号只有支付查看权限。要录入最终报价并开放支付，需要 payment:manage 权限。';
      $('#paymentPrepareForm').hidden = true;
      return;
    }
    status.textContent = '当前账号具备支付管理权限，可以读取订单、确认最终报价并建立待支付单。';
  }

  async function load() {
    const id = ++sequence;
    $('#paymentRows').setAttribute('aria-busy', 'true');
    try {
      const result = await api(`?q=${encodeURIComponent(new FormData($('#paymentFilters')).get('q') || '')}&page=${page}`);
      if (id !== sequence) return;
      state = result;
      renderPrepareAvailability();

      $('#paymentRows').replaceChildren(...(state.items || []).map(item => {
        const row = node('article', null, 'xwp-record');
        row.append(
          node('h3', item.subject || '支付记录'),
          node('p', `${item.paymentNo} · ${item.businessNo}`),
          node('p', `${item.amount} ${display(item.currency)} · ${display(item.status)}`)
        );
        if (state.enabled && state.canManage && ['CREATED', 'APPROVED'].includes(item.status)) {
          const close = node('button', '关闭待支付单', 'xw-button xw-button-ghost');
          close.type = 'button';
          close.onclick = async () => {
            if (busy || !confirm(`确认关闭 ${item.paymentNo}？服务端会先核对是否已支付，已到账的支付单不能关闭。`)) return;
            busy = true;
            close.disabled = true;
            try {
              const result = await api(`/${encodeURIComponent(item.paymentNo)}/close`, {
                method: 'POST',
                headers: revision(item.revision)
              });
              message(result.status === 'CAPTURED'
                ? '服务端确认已到账，本次没有关闭。'
                : '服务端已关闭待支付单。');
              try { await load(); }
              catch (error) { message(`操作已返回，请刷新核对最新状态。${error.message}`); }
            } catch (error) {
              message(error.message);
            } finally {
              busy = false;
              close.disabled = false;
            }
          };
          row.append(close);
        }
        return row;
      }));

      if (!state.items?.length) {
        $('#paymentRows').append(node('p', '当前数据范围内暂无符合条件的支付记录。'));
      }
      $('#paymentPage').textContent = `第 ${page + 1} 页 · 共 ${state.total} 条`;
      $('#paymentPrev').disabled = page === 0;
      $('#paymentNext').disabled = (page + 1) * 25 >= state.total;

      if (!state.enabled) message('真实收款开关关闭，当前仅可查看已有记录和订单资格。');
      else if (!state.canManage) message('当前账号为支付只读权限；报价与收款入口已显示，但写入操作保持锁定。');
      else message('支付管理已就绪：可读取订单并确认最终报价。');
    } finally {
      if (id === sequence) $('#paymentRows').setAttribute('aria-busy', 'false');
    }
  }

  async function lookupOrder(identifier) {
    if (busy) return;
    const value = String(identifier || '').trim();
    if (!value) return;

    busy = true;
    order = null;
    $('#paymentOrderIdentifier').disabled = true;
    $('#paymentPrepareForm').hidden = true;
    $('#paymentOrderDetails').replaceChildren();
    $('#paymentPrepareMessage').textContent = '正在读取订单…';

    try {
      const found = await api(`/orders/${encodeURIComponent(value)}`);
      order = found;
      $('#paymentOrderDetails').append(
        node('p', `${found.orderNo} · ${found.businessNo} · ${display(found.status)}`),
        node('p', found.customerBound ? '订单已关联客户账号' : '尚未关联客户账号，请先在业务工作台完成关联。')
      );

      const eligible = found.customerBound && found.paymentEligible !== false;
      const canPrepare = Boolean(eligible && state.enabled && state.canManage);
      $('#paymentPrepareForm').hidden = !canPrepare;

      if (!eligible) {
        $('#paymentPrepareMessage').textContent = '请先在业务工作台补齐统一业务编号和客户账号归属。';
      } else if (!state.enabled) {
        $('#paymentPrepareMessage').textContent = '订单具备支付资格，但真实收款开关当前关闭。';
      } else if (!state.canManage) {
        $('#paymentPrepareMessage').textContent = '订单具备支付资格，但当前账号缺少 payment:manage 权限，不能录入最终报价。';
      } else {
        $('#paymentPrepareMessage').textContent = '订单具备支付资格，请核对最终金额和服务名称。';
      }
    } catch (error) {
      $('#paymentPrepareMessage').textContent = error.message;
    } finally {
      busy = false;
      $('#paymentOrderIdentifier').disabled = false;
    }
  }

  $('#paymentOrderLookup').onsubmit = async event => {
    event.preventDefault();
    await lookupOrder($('#paymentOrderIdentifier').value);
  };

  $('#paymentOrderIdentifier').addEventListener('input', () => {
    order = null;
    $('#paymentPrepareForm').hidden = true;
    $('#paymentOrderDetails').replaceChildren();
    $('#paymentPrepareMessage').textContent = '';
  });

  $('#paymentPrepareForm').onsubmit = async event => {
    event.preventDefault();
    if (busy || !order || !state.enabled || !state.canManage) return;

    const body = {
      amount: $('#paymentAmount').value,
      subject: $('#paymentSubject').value.trim(),
      reason: $('#paymentReason').value.trim()
    };
    const selected = order;
    if (!confirm(`确认已与客户核对业务 ${selected.businessNo} 的 ${body.amount} 元及服务名称，并建立待支付单？`)) return;

    busy = true;
    $('#paymentPrepareFields').disabled = true;
    $('#paymentOrderIdentifier').disabled = true;
    try {
      const result = await api(`/orders/${encodeURIComponent(selected.orderNo)}`, {
        method: 'POST',
        headers: revision(selected.revision),
        body
      });
      $('#paymentPrepareMessage').textContent = `已建立待支付单 ${result.paymentNo}，服务端确认金额 ${result.amount} 元；等待客户主动支付。`;
      $('#paymentPrepareForm').reset();
      $('#paymentPrepareForm').hidden = true;
      order = null;
      try { await load(); }
      catch (error) { message(`建单已完成，但列表刷新失败：${error.message}`); }
    } catch (error) {
      $('#paymentPrepareMessage').textContent = error.message;
    } finally {
      busy = false;
      $('#paymentPrepareFields').disabled = false;
      $('#paymentOrderIdentifier').disabled = false;
    }
  };

  $('#paymentFilters').onsubmit = event => {
    event.preventDefault();
    if (!busy) {
      page = 0;
      load().catch(error => message(error.message));
    }
  };
  $('#paymentRefresh').onclick = () => {
    if (!busy) load().catch(error => message(error.message));
  };
  $('#paymentPrev').onclick = () => {
    if (!busy && page > 0) {
      page--;
      load().catch(error => message(error.message));
    }
  };
  $('#paymentNext').onclick = () => {
    if (!busy) {
      page++;
      load().catch(error => message(error.message));
    }
  };

  (async () => {
    try {
      await load();
      if (requestedOrder) {
        $('#paymentOrderIdentifier').value = requestedOrder;
        await lookupOrder(requestedOrder);
      }
    } catch (error) {
      message(error.message);
    }
  })();
})();
