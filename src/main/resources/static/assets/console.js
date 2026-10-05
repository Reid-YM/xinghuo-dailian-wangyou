(() => {
  const $ = (s, r = document) => r.querySelector(s);
  const $$ = (s, r = document) => [...r.querySelectorAll(s)];
  const esc = (v) => String(v ?? '').replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
  const time = (v) => v ? new Date(v).toLocaleString('zh-CN', { hour12: false }) : '-';
  let currentMe = null;
  async function api(url, options = {}) {
    const headers = new Headers(options.headers || {});
    if (options.body && !(options.body instanceof FormData)) headers.set('Content-Type', 'application/json');
    if (options.method && options.method !== 'GET') headers.set('X-Requested-With', 'fetch');
    const res = await fetch(url, { ...options, headers, body: options.body && !(options.body instanceof FormData) ? JSON.stringify(options.body) : options.body });
    if (res.status === 401) { location.href = '/account/login?next=' + encodeURIComponent(location.pathname); return null; }
    if (!res.ok) throw new Error(await res.text() || `请求失败 ${res.status}`);
    return res.status === 204 ? null : res.json();
  }
  function showTab(name) {
    $$('[data-console-tab]').forEach(b => b.classList.toggle('active', b.dataset.consoleTab === name));
    $$('[data-console-panel]').forEach(p => p.classList.toggle('hidden', p.dataset.consolePanel !== name));
    if (history.replaceState) history.replaceState(null, '', '#' + name);
  }
  function empty(text) { return `<div class="console-empty">${esc(text)}</div>`; }
  function avatarInitial(me) { return String(me?.displayName || '星').trim().slice(0, 1) || '星'; }
  function paintAvatar(node, me) {
    if (!node) return;
    const avatar = me?.avatarUrl || '';
    node.textContent = '';
    node.style.backgroundImage = avatar ? `url("${avatar.replace(/"/g, '%22')}")` : '';
    if (!avatar) node.textContent = avatarInitial(me);
  }
  function fillProfileForm(me) {
    const form = $('[data-console-profile-form]');
    if (!form || !me) return;
    form.publicUserId.value = me.publicUserId || '';
    form.displayName.value = me.displayName || '';
    if (form.avatarFile) form.avatarFile.value = '';
    paintAvatar($('[data-console-avatar-preview]'), me);
  }
  function quoteCard(q) {
    return `<article class="console-card"><div><span>${esc(q.statusText || q.status)}</span><h3>${esc(q.game || '未知游戏')} · ${esc(q.demand || '需求')}</h3><p>${esc(q.currentProgress || q.nextAction || '等待客服处理')}</p><small>${esc(q.businessNo)} · ${time(q.updatedAt || q.createdAt)}</small></div><button class="btn ghost small-btn" data-supplement-quote="${esc(q.businessNo)}">补充说明</button></article>`;
  }
  function orderCard(o) {
    const pct = Number.isFinite(Number(o.progressPercent)) ? Number(o.progressPercent) : 0;
    return `<article class="console-card"><div><span>${esc(o.statusText || o.status)}</span><h3>${esc(o.gameName || '业务订单')} · ${esc(o.serviceName || o.businessNo)}</h3><p>${esc(o.progressText || '等待进度更新')}</p><div class="console-progress"><i style="width:${Math.max(0, Math.min(100, pct))}%"></i></div><small>${esc(o.businessNo)} · ${time(o.updatedAt || o.createdAt)}</small></div><div class="card-actions"><button class="btn ghost small-btn" data-order-detail-btn="${esc(o.businessNo)}">详情</button><button class="btn ghost small-btn" data-supplement-order="${esc(o.businessNo)}">补充</button></div></article>`;
  }
  function loginCard(x) { return `<article class="console-card compact"><div><span>${esc(x.action || 'LOGIN')}</span><h3>${esc(x.ip || '未知 IP')}</h3><p>${esc(x.userAgent || x.detail || '登录记录')}</p><small>${time(x.createdAt)}</small></div></article>`; }
  async function loadMe() {
    const me = await api('/api/account/me'); if (!me) return;
    currentMe = me;
    $('[data-console-name]').textContent = me.displayName || '星火用户';
    $('[data-console-email]').textContent = `${me.publicUserId || ''}${me.publicUserId ? ' · ' : ''}${me.email || me.contactMasked || '邮箱账号'}`;
    $('[data-console-verify]').textContent = me.emailVerified ? '邮箱已验证' : '邮箱未验证';
    $('[data-console-verify]').classList.toggle('warn', !me.emailVerified);
    fillProfileForm(me);
  }
  async function loadSummary() {
    const s = await api('/api/console/summary'); if (!s) return;
    ['quoteTotal','orderTotal','activeOrders','securityEvents'].forEach(k => { const el = $(`[data-kpi="${k}"]`); if (el) el.textContent = s[k] ?? 0; });
    $('[data-recent-quotes]').innerHTML = (s.recentQuotes || []).map(quoteCard).join('') || empty('暂无需求记录');
    $('[data-recent-orders]').innerHTML = (s.recentOrders || []).map(orderCard).join('') || empty('暂无订单记录');
  }
  async function loadQuotes() { const d = await api('/api/console/quotes'); $('[data-console-quotes]').innerHTML = (d?.items || []).map(quoteCard).join('') || empty('暂无需求记录'); }
  async function loadOrders() { const d = await api('/api/console/orders'); $('[data-console-orders]').innerHTML = (d?.items || []).map(orderCard).join('') || empty('暂无订单记录'); }
  async function loadLogins() { const d = await api('/api/console/security/logins'); $('[data-console-logins]').innerHTML = (d || []).map(loginCard).join('') || empty('暂无登录记录'); }
  async function showOrderDetail(no) {
    const d = await api('/api/console/orders/' + encodeURIComponent(no));
    const host = $('[data-order-detail]');
    host.classList.remove('hidden');
    host.innerHTML = `<header><h3>${esc(d.order.businessNo)} 进度详情</h3><button class="btn ghost small-btn" data-close-detail>关闭</button></header><div class="timeline">${(d.timeline || []).map(x => `<div><b>${esc(x.title)}</b><p>${esc(x.content)}</p><small>${time(x.createdAt)}</small></div>`).join('')}</div>`;
  }
  async function supplement(kind, no) {
    const content = prompt('填写要补充给客服的说明（不要填写游戏密码、验证码或令牌）');
    if (!content) return;
    await api(`/api/console/${kind}/${encodeURIComponent(no)}/supplement`, { method: 'POST', body: { content } });
    alert('已提交补充说明');
    await Promise.all([loadSummary(), kind === 'orders' ? loadOrders() : loadQuotes()]);
  }
  document.addEventListener('click', async (e) => {
    const tab = e.target.closest('[data-console-tab],[data-jump]');
    if (tab) { showTab(tab.dataset.consoleTab || tab.dataset.jump); return; }
    const q = e.target.closest('[data-supplement-quote]'); if (q) return supplement('quotes', q.dataset.supplementQuote);
    const o = e.target.closest('[data-supplement-order]'); if (o) return supplement('orders', o.dataset.supplementOrder);
    const detail = e.target.closest('[data-order-detail-btn]'); if (detail) return showOrderDetail(detail.dataset.orderDetailBtn);
    if (e.target.closest('[data-close-detail]')) $('[data-order-detail]')?.classList.add('hidden');
    if (e.target.closest('[data-refresh-orders]')) loadOrders().catch(err => alert(err.message));
    if (e.target.closest('[data-refresh-security]')) loadLogins().catch(err => alert(err.message));
    if (e.target.closest('[data-console-logout]')) { await api('/api/account/logout', { method: 'POST', body: {} }); location.href = '/'; }
  });
  const profileForm = document.querySelector('[data-console-profile-form]');
  if (profileForm) {
    profileForm.addEventListener('submit', async (event) => {
      event.preventDefault();
      const displayName = profileForm.displayName.value.trim();
      const avatarFile = profileForm.avatarFile?.files?.[0];
      if (!displayName) { alert('请填写昵称'); return; }
      try {
        let avatarUrl = currentMe?.avatarUrl || '';
        if (avatarFile) {
          const data = new FormData();
          data.append('file', avatarFile);
          const uploaded = await api('/api/account/profile/avatar', { method: 'POST', body: data });
          avatarUrl = uploaded?.avatarUrl || avatarUrl;
        }
        currentMe = await api('/api/account/profile', { method: 'POST', body: { displayName, avatarUrl } });
        await loadMe();
        alert('资料已保存');
      } catch (err) {
        alert(err.message || '保存资料失败');
      }
    });
  }
  (async () => { try { await loadMe(); await Promise.all([loadSummary(), loadQuotes(), loadOrders(), loadLogins()]); showTab((location.hash || '#overview').slice(1)); } catch (e) { alert(e.message); } })();
})();
