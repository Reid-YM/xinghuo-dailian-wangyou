(() => {
  'use strict';

  const rows = document.getElementById('xwbQuoteRows');
  if (!rows) return;
  const csrfToken = document.querySelector('meta[name="_csrf"]')?.content || '';
  const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content || 'X-CSRF-TOKEN';

  function headers(json = false) {
    const value = {'Accept':'application/json'};
    if (json) value['Content-Type'] = 'application/json';
    if (csrfToken && csrfHeader) value[csrfHeader] = csrfToken;
    return value;
  }

  async function errorOf(response, fallback) {
    let detail = '';
    try {
      const body = await response.json();
      detail = body?.error || body?.message || '';
    } catch (_) {}
    return new Error(detail || fallback || `请求失败（${response.status}）`);
  }

  async function publish(businessNo, button) {
    const amountRaw = prompt(`为游客询价 ${businessNo} 输入正式报价金额（人民币）`);
    if (amountRaw == null) return;
    const amount = Number(amountRaw.trim());
    if (!Number.isFinite(amount) || amount <= 0) {
      alert('请输入大于 0 的有效金额。');
      return;
    }
    const subject = prompt('服务名称', '星火网游代练服务');
    if (subject == null || !subject.trim()) return;
    const estimatedDuration = prompt('预计完成时长（可留空）', '') ?? '';
    const customerMessage = prompt('给客户的报价说明（可留空）', '') ?? '';
    if (!confirm(`确认向游客发布 ${amount.toFixed(2)} 元正式报价？\n\n服务：${subject.trim()}`)) return;

    button.disabled = true;
    const original = button.textContent;
    button.textContent = '发布中…';
    try {
      const response = await fetch(`/api/admin/xingwei/business/quotes/${encodeURIComponent(businessNo)}/offers`, {
        method:'POST',
        credentials:'same-origin',
        cache:'no-store',
        headers:headers(true),
        body:JSON.stringify({
          amount,
          subject:subject.trim(),
          estimatedDuration:estimatedDuration.trim(),
          customerMessage:customerMessage.trim(),
          reason:'客服发布游客正式报价'
        })
      });
      if (!response.ok) throw await errorOf(response, `发布失败（${response.status}）`);
      alert(`报价已发布：¥${amount.toFixed(2)}。游客无需注册，可在原业务详情页或客服安全链接中确认并付款。`);
      document.getElementById('xwbRefresh')?.click();
    } catch (error) {
      alert(error.message || '发布报价失败');
    } finally {
      button.disabled = false;
      button.textContent = original;
    }
  }

  async function copyGuestLink(businessNo, button) {
    button.disabled = true;
    const original = button.textContent;
    button.textContent = '生成中…';
    try {
      const response = await fetch(`/api/admin/xingwei/business/quotes/${encodeURIComponent(businessNo)}/guest-link`, {
        method:'POST',
        credentials:'same-origin',
        cache:'no-store',
        headers:headers(false)
      });
      if (!response.ok) throw await errorOf(response, `生成失败（${response.status}）`);
      const data = await response.json();
      const url = new URL(data.path, window.location.origin).toString();
      let copied = false;
      try {
        await navigator.clipboard.writeText(url);
        copied = true;
      } catch (_) {}
      if (copied) {
        alert(`游客安全链接已复制。\n\n${url}\n\n该链接一次有效，24 小时内使用；打开后会换成 HttpOnly 游客会话。`);
      } else {
        prompt('复制下面的游客安全链接（一次有效，24 小时内使用）', url);
      }
    } catch (error) {
      alert(error.message || '生成游客安全链接失败');
    } finally {
      button.disabled = false;
      button.textContent = original;
    }
  }

  function enhance() {
    rows.querySelectorAll('tr[data-search]').forEach(row => {
      if (row.dataset.guestOfferEnhanced === '1') return;
      const businessNo = row.querySelector('td:first-child .xwb-primary')?.textContent?.trim();
      const actionCell = row.lastElementChild;
      if (!businessNo || !actionCell) return;

      const link = document.createElement('button');
      link.type = 'button';
      link.className = 'xw-mini-button';
      link.textContent = '复制游客链接';
      link.title = '生成一次性安全链接，客户在另一浏览器打开后仍无需注册';
      link.addEventListener('click', () => copyGuestLink(businessNo, link));

      const offer = document.createElement('button');
      offer.type = 'button';
      offer.className = 'xw-mini-button';
      offer.textContent = '发布报价';
      offer.title = '向游客发布正式报价';
      offer.addEventListener('click', () => publish(businessNo, offer));

      actionCell.prepend(link);
      actionCell.prepend(offer);
      row.dataset.guestOfferEnhanced = '1';
    });
  }

  new MutationObserver(enhance).observe(rows, {childList:true, subtree:true});
  enhance();
})();
