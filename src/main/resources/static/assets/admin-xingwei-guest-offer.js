(() => {
  'use strict';

  const rows = document.getElementById('xwbQuoteRows');
  if (!rows) return;
  const csrfToken = document.querySelector('meta[name="_csrf"]')?.content || '';
  const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content || 'X-CSRF-TOKEN';

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
      const headers = {'Accept':'application/json','Content-Type':'application/json'};
      if (csrfToken && csrfHeader) headers[csrfHeader] = csrfToken;
      const response = await fetch(`/api/admin/xingwei/business/quotes/${encodeURIComponent(businessNo)}/offers`, {
        method:'POST',
        credentials:'same-origin',
        cache:'no-store',
        headers,
        body:JSON.stringify({
          amount,
          subject:subject.trim(),
          estimatedDuration:estimatedDuration.trim(),
          customerMessage:customerMessage.trim(),
          reason:'客服发布游客正式报价'
        })
      });
      if (!response.ok) {
        let detail = '';
        try {
          const body = await response.json();
          detail = body?.error || body?.message || '';
        } catch (_) {}
        throw new Error(detail || `发布失败（${response.status}）`);
      }
      alert(`报价已发布：¥${amount.toFixed(2)}。游客无需注册，可在原业务详情页直接确认并付款。`);
      document.getElementById('xwbRefresh')?.click();
    } catch (error) {
      alert(error.message || '发布报价失败');
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
      const button = document.createElement('button');
      button.type = 'button';
      button.className = 'xw-mini-button';
      button.textContent = '发布报价';
      button.title = '向持有该游客询价凭证的客户发布正式报价';
      button.addEventListener('click', () => publish(businessNo, button));
      actionCell.prepend(button);
      row.dataset.guestOfferEnhanced = '1';
    });
  }

  new MutationObserver(enhance).observe(rows, {childList:true, subtree:true});
  enhance();
})();
