(() => {
  const page = document.body.dataset.page;
  const $ = XH.$;

  function bindRecordSearch(form) {
    if (!form) return;
    form.addEventListener('submit', event => {
      event.preventDefault();
      const input = form.businessNo || form.orderNo;
      const businessNo = XH.normalizeRecordNo(input?.value);
      if (!XH.validRecordNo(businessNo)) {
        XH.showMessage(form.querySelector('[data-message]'), '业务编号格式不正确，请检查后重试。', 'error');
        return;
      }
      XH.track('record_search', businessNo);
      location.href = `/record/${encodeURIComponent(businessNo)}`;
    });
  }

  function bindCopyButtons() {
    document.querySelectorAll('[data-copy]').forEach(button => {
      if (button.dataset.bound === '1') return;
      button.dataset.bound = '1';
      button.addEventListener('click', async () => {
        const target = document.querySelector(button.dataset.copy);
        const text = target?.textContent?.trim();
        if (!text || text === '未配置' || text.includes('在线客服')) return;
        try {
          await navigator.clipboard.writeText(text);
          const old = button.textContent;
          button.textContent = '已复制';
          setTimeout(() => button.textContent = old, 1200);
        } catch {}
      });
    });
  }

  function homeRecommendationPrice(service) {
    if (!service) return '';
    if (service.priceMode === 'fixed' && service.priceFixed != null) return `¥${service.priceFixed}`;
    if (service.priceMin != null && service.priceMax != null) return `¥${service.priceMin}–${service.priceMax}`;
    if (service.priceMin != null) return `¥${service.priceMin} 起`;
    return '按需求报价';
  }

  function renderHomeRecommendations(config) {
    const copy = document.querySelector('.hero-media-panel .hero-media-copy');
    if (!copy) return;
    const hot = Array.isArray(config?.hotServices) ? config.hotServices.slice(0, 2) : [];
    if (!hot.length) {
      copy.remove();
      return;
    }
    copy.innerHTML = `<span class="eyebrow">当前推荐</span><div class="hero-hot-list">${hot.map(service => `
      <a class="hero-hot-item" href="/service/${encodeURIComponent(service.slug || '')}">
        <b>${XH.esc(service.name || '热门任务')}</b>
        ${service.shortDesc ? `<small>${XH.esc(service.shortDesc)}</small>` : ''}
        <em>${XH.esc(homeRecommendationPrice(service))}</em>
      </a>`).join('')}</div>`;
  }

  function normalizeContactPage() {
    const neutralLabels = { '主客服': '微信', '主账号': '微信', '默认客服': '微信' };
    document.querySelectorAll('#contactChannelGrid .tag').forEach(tag => {
      const current = (tag.textContent || '').trim();
      if (neutralLabels[current]) tag.textContent = neutralLabels[current];
    });

    const qr = document.getElementById('wechatQr');
    const consoleBox = document.querySelector('.contact-console');
    if (!qr || !consoleBox) return;
    const notice = consoleBox.querySelector('.contact-safety');
    if (notice && qr.nextElementSibling !== notice) consoleBox.insertBefore(qr, notice);
    else if (!notice && qr.parentElement !== consoleBox) consoleBox.appendChild(qr);
  }

  function observeContactChannelRefresh() {
    const grid = document.getElementById('contactChannelGrid');
    if (!grid || grid.dataset.normalizerReady === '1') return;
    grid.dataset.normalizerReady = '1';
    new MutationObserver(normalizeContactPage).observe(grid, { childList: true, subtree: true });
  }

  async function initHome() {
    const config = await XH.loadConfig();
    const [services, taxonomy] = await Promise.all([XH.loadServices(), XH.loadTaxonomy()]);
    XH.updateDynamicStartingPrice(services);
    XH.bindUploadFields();
    renderHomeRecommendations(config);

    const grid = $('#serviceGrid');
    if (grid) grid.innerHTML = services.slice(0, config.homeServiceLimit || 4).map(s => XH.serviceCard(s)).join('');

    const gameRail = $('#homeGameRail');
    const gameSection = $('#gameEntrySection');
    const activeGameCodes = [...new Set(services.map(s => String(s.gameCode || '').toUpperCase()).filter(Boolean))];
    const activeGames = activeGameCodes.map(code => taxonomy.games.find(g => g.code === code)).filter(Boolean);
    if (gameRail && gameSection && activeGames.length > 1) {
      gameSection.classList.remove('hidden');
      gameRail.innerHTML = activeGames.slice(0, 10).map(g => {
        const href = g.seoSlug ? `/game/${encodeURIComponent(g.seoSlug)}` : `/services?game=${encodeURIComponent(g.code)}`;
        return `<a class="home-game-chip" href="${href}"><span class="home-game-icon">${g.coverImage ? `<img src="${XH.esc(g.coverImage)}" alt="">` : '<svg class="ui-icon"><use href="/assets/icons.svg#i-gamepad"></use></svg>'}</span><span><b>${XH.esc(g.name)}</b><small>${XH.esc(g.publisher || g.nameEn || '游戏服务')}</small></span></a>`;
      }).join('');
    }

    const quoteForm = $('#quoteForm');
    if (quoteForm) {
      try { await XH.initLinkedQuoteForm(quoteForm); } catch (error) { console.error('[quote init]', error); }
      quoteForm.addEventListener('focusin', () => XH.track('quote_started'), { once: true });
      quoteForm.addEventListener('submit', async event => {
        event.preventDefault();
        try { await XH.submitQuoteForm(quoteForm, $('#quoteMsg')); } catch {}
      });
    }
    bindRecordSearch($('#orderForm'));
  }

  async function initServices() {
    await XH.loadConfig();
    const list = $('#list');
    const gameBox = $('#publicGameFilters');
    const platformBox = $('#publicPlatformFilters');
    const categoryBox = $('#publicCategoryFilters');
    const search = $('#publicServiceSearch');
    const summary = $('#publicServiceSummary');
    let services;
    let taxonomy;
    try {
      [services, taxonomy] = await Promise.all([XH.loadServices(), XH.loadTaxonomy()]);
    } catch (error) {
      console.error('[service hall]', error);
      // Keep the server-rendered cards available while the service API is down.
      if (summary) {
        summary.innerHTML = '<span>最新服务暂时读取失败，页面已有内容仍可查看。</span> <button type="button" class="service-browser-retry">重新读取</button>';
        summary.querySelector('button')?.addEventListener('click', () => {
          summary.textContent = '正在重新读取服务…';
          initServices();
        }, { once: true });
      }
      if (list && !list.children.length) list.innerHTML = '<div class="empty card service-browser-empty"><strong>服务暂时无法加载</strong><a class="btn primary" href="/quote">提交需求</a></div>';
      return;
    }
    const params = new URLSearchParams(location.search);
    let selectedGame = params.get('game')?.toUpperCase() || 'all';
    let selectedPlatform = params.get('platform')?.toUpperCase() || 'all';
    let selectedCategory = 'all';

    const serviceGameCodes = [...new Set(services.map(s => String(s.gameCode || '').toUpperCase()).filter(Boolean))];
    const serviceGames = serviceGameCodes.map(code => taxonomy.games.find(g => g.code === code)).filter(Boolean);
    if (selectedGame !== 'all' && !serviceGameCodes.includes(selectedGame)) selectedGame = 'all';

    const platformCodesForGame = gameCode => taxonomy.gamePlatforms.filter(x => x.active !== false && String(x.gameCode||'').toUpperCase() === gameCode).map(x => String(x.platformCode||'').toUpperCase());
    const serviceMatchesPlatform = service => {
      if (selectedPlatform === 'all') return true;
      const gameCode = String(service.gameCode || '').toUpperCase();
      return platformCodesForGame(gameCode).includes(selectedPlatform);
    };

    const renderGameFilters = () => {
      if (!gameBox) return;
      gameBox.innerHTML = `<button class="filter-btn ${selectedGame==='all'?'active':''}" data-game-filter="all" aria-pressed="${selectedGame==='all'}">全部游戏</button>` + serviceGames.map(g => `<button class="filter-btn ${selectedGame===g.code?'active':''}" data-game-filter="${XH.esc(g.code)}" aria-pressed="${selectedGame===g.code}">${XH.esc(g.name)}</button>`).join('');
    };
    const renderPlatformFilters = () => {
      if (!platformBox) return;
      const games = selectedGame === 'all' ? serviceGameCodes : [selectedGame];
      const codes = [...new Set(games.flatMap(platformCodesForGame))];
      const rows = taxonomy.platforms.filter(p => codes.includes(String(p.code||'').toUpperCase()) && p.active !== false);
      if (selectedPlatform !== 'all' && !codes.includes(selectedPlatform)) selectedPlatform = 'all';
      platformBox.innerHTML = `<button class="filter-btn ${selectedPlatform==='all'?'active':''}" data-platform-filter="all" aria-pressed="${selectedPlatform==='all'}">全部平台</button>` + rows.map(p => `<button class="filter-btn ${selectedPlatform===String(p.code).toUpperCase()?'active':''}" data-platform-filter="${XH.esc(String(p.code).toUpperCase())}" aria-pressed="${selectedPlatform===String(p.code).toUpperCase()}">${XH.esc(p.name)}</button>`).join('');
    };
    const renderCategoryFilters = () => {
      if (!categoryBox) return;
      const available = services.filter(s => (selectedGame === 'all' || String(s.gameCode || '').toUpperCase() === selectedGame) && serviceMatchesPlatform(s));
      const codes = [...new Set(available.map(s => String(s.category || '').toUpperCase()).filter(Boolean))];
      if (selectedCategory !== 'all' && !codes.includes(selectedCategory.toUpperCase())) selectedCategory = 'all';
      categoryBox.innerHTML = `<button class="filter-btn ${selectedCategory==='all'?'active':''}" data-category-filter="all" aria-pressed="${selectedCategory==='all'}">全部需求</button>` + codes.map(code => `<button class="filter-btn ${String(selectedCategory).toUpperCase()===code?'active':''}" data-category-filter="${XH.esc(code.toLowerCase())}" aria-pressed="${String(selectedCategory).toUpperCase()===code}">${XH.esc(XH.categoryLabel(code, selectedGame==='all'?null:selectedGame))}</button>`).join('');
    };
    const render = () => {
      renderPlatformFilters();
      renderCategoryFilters();
      const q=(search?.value||'').trim().toLowerCase();
      const rows=services.filter(s => selectedGame==='all' || String(s.gameCode||'').toUpperCase()===selectedGame)
        .filter(serviceMatchesPlatform)
        .filter(s => selectedCategory==='all' || String(s.category||'').toLowerCase()===String(selectedCategory).toLowerCase())
        .filter(s => !q || [s.name,s.shortDesc,XH.gameLabel(s.gameCode),XH.categoryLabel(s.category,s.gameCode)].some(v=>String(v||'').toLowerCase().includes(q)));
      if (list) list.innerHTML = rows.length ? rows.map(s => XH.serviceCard(s)).join('') : '<div class="empty card service-browser-empty"><strong>暂时没有匹配服务</strong><button type="button" class="btn ghost" data-reset-service-filters>清除筛选</button><a class="btn primary" href="/quote">提交需求</a></div>';
      if(summary) {
        const platformName = taxonomy.platforms.find(p => String(p.code||'').toUpperCase()===selectedPlatform)?.name;
        summary.textContent=`${rows.length} 个可接服务${selectedGame==='all'?'':` · ${XH.gameLabel(selectedGame)}`}${selectedPlatform==='all'?'':` · ${platformName||selectedPlatform}`}`;
      }
      renderGameFilters();
    };
    render();
    gameBox?.addEventListener('click',e=>{const btn=e.target.closest('[data-game-filter]');if(!btn)return;selectedGame=btn.dataset.gameFilter;selectedPlatform='all';selectedCategory='all';render();});
    platformBox?.addEventListener('click',e=>{const btn=e.target.closest('[data-platform-filter]');if(!btn)return;selectedPlatform=btn.dataset.platformFilter;selectedCategory='all';render();});
    categoryBox?.addEventListener('click',e=>{const btn=e.target.closest('[data-category-filter]');if(!btn)return;selectedCategory=btn.dataset.categoryFilter;render();});
    search?.addEventListener('input',render);
    list?.addEventListener('click', e => {
      if (!e.target.closest('[data-reset-service-filters]')) return;
      selectedGame = 'all';
      selectedPlatform = 'all';
      selectedCategory = 'all';
      if (search) search.value = '';
      render();
      search?.focus();
    });
  }

  async function initQuote() {
    const config = await XH.loadConfig();
    XH.track('quote_started');
    bindCopyButtons();
    XH.bindUploadFields();

    const form = $('#quotePageForm');
    const params = new URLSearchParams(location.search);
    const serviceKey = params.get('service');
    try { await XH.initLinkedQuoteForm(form, serviceKey); } catch (error) {
      XH.showMessage($('#quotePageMsg'), '服务选项加载失败，请刷新后重试。', 'error');
    }
    if (!config.quoteEnabled) {
      [...form.elements].forEach(el => el.disabled = true);
      XH.showMessage($('#quotePageMsg'), '当前已关闭网页询价，请通过客服渠道联系。', 'error');
    }

    form.addEventListener('submit', async event => {
      event.preventDefault();
      try {
        const out = await XH.submitQuoteForm(form, $('#quotePageMsg'));
        if (out?.businessNo && !$('#quotePageMsg .quote-success-card')) {
          $('#quotePageMsg').insertAdjacentHTML('beforeend', `<div class="result-actions"><a class="btn small-btn" href="/record/${encodeURIComponent(out.businessNo)}">查看本次记录</a><a class="btn small-btn ghost" href="/contact">一对一咨询</a></div>`);
        }
      } catch {}
    });
  }

  async function initServiceDetail() {
    await XH.loadConfig();
    const root = $('[data-service-detail]');
    const slug = root?.dataset.serviceSlug || '';
    let active = [...document.querySelectorAll('[data-service-panel]')].find(panel => panel.dataset.servicePanel === slug);
    if (!active) active = $('[data-service-panel="weapon-leveling"]');
    document.querySelectorAll('[data-service-panel]').forEach(panel => panel.classList.toggle('is-active', panel === active));

    const camo = $('#camoEstimator');
    const camoOut = $('#camoEstimateOut');
    const updateCamo = () => {
      if (!camo || !camoOut) return;
      const target = camo.target.value;
      const count = camo.weaponCount.value;
      const progress = camo.progress.value;
      let base = target === '完整系列' ? 260 : target === '进阶段位' ? 140 : 80;
      if (count === '一类') base *= 2.4;
      if (count === '全枪') base *= 5.2;
      if (progress === '约一半') base *= .62;
      if (progress === '接近完成') base *= .36;
      const low = Math.max(60, Math.round(base / 10) * 10);
      camoOut.textContent = `参考区间：¥${low}–${low + Math.max(40, Math.round(low * .35 / 10) * 10)}`;
    };
    camo?.addEventListener('change', updateCamo);
    updateCamo();

    const rank = $('#rankEstimator');
    const rankOut = $('#rankEstimateOut');
    const updateRank = () => {
      if (!rank || !rankOut) return;
      const order = ['青铜','白银','黄金','铂金','钻石','大师'];
      const from = order.indexOf(rank.currentRank.value);
      const to = order.indexOf(rank.targetRank.value);
      const diff = Math.max(1, to - from);
      const low = diff * 100;
      rankOut.textContent = `参考区间：¥${low}–${low + diff * 80} · ${rank.platform.value || '平台待确认'}`;
    };
    rank?.addEventListener('change', updateRank);
    updateRank();
  }

  async function initOrder() {
    await XH.loadConfig();
    bindRecordSearch($('#orderLookupForm'));
  }

  function stageLabel(stage) {
    return stage === 'QUOTE' ? '询价阶段' : '履约阶段';
  }

  function lifecycle(stage, status) {
    const reachedOrder = stage === 'ORDER';
    const done = reachedOrder && status === 'COMPLETED';
    const working = reachedOrder && ['IN_PROGRESS','COMPLETED','EXCEPTION'].includes(status);
    return [
      ['01','需求已提交', true],
      ['02','客服已确认', reachedOrder || ['CONTACTED','QUOTED','CONVERTED'].includes(status)],
      ['03','进入履约', reachedOrder],
      ['04','完成交付', done]
    ].map(([no,label,on]) => `<div class="lifecycle-step ${on?'is-done':''}"><b>${no}</b><span>${label}</span></div>`).join('');
  }

  function statusClass(stage, status) {
    if (stage === 'ORDER' && status === 'COMPLETED') return 'ok';
    if ((stage === 'ORDER' && status === 'EXCEPTION') || (stage === 'QUOTE' && ['CANCELLED','CLOSED'].includes(status))) return 'danger-text';
    return 'working';
  }

  async function initRecordDetail() {
    const box = $('#recordDetailBox');
    const businessNo = XH.normalizeRecordNo(box.dataset.businessNo || '');
    try {
      const r = await XH.api(`/api/records/${encodeURIComponent(businessNo)}`);
      XH.rememberRecord({ businessNo: r.businessNo, stage: r.stage, createdAt: r.createdAt });
      XH.track('record_search_success', r.businessNo);
      const pct = r.progressPercent;
      const cls = statusClass(r.stage, r.status);
      box.innerHTML = `
        <div class="order-head">
          <div><span class="muted">统一业务编号</span><div class="mono order-no">${XH.esc(r.businessNo)}</div><div class="muted small">${stageLabel(r.stage)} · 进入履约后编号保持不变</div></div>
          <span class="status-pill ${cls}">${XH.esc(XH.recordStatusLabel(r.stage, r.status))}</span>
        </div>
        <div class="record-title"><span class="muted">业务内容</span><h2>${XH.esc(r.title || '服务记录')}</h2></div>
        ${r.expectedTime ? `<div class="record-delivery-card"><span class="muted">预计完成 / 交付时效</span><strong>${XH.esc(r.expectedTime)}</strong><small>实际时间以客服确认排期为准，进度会在本页持续更新。</small></div>` : ''}
        <div class="business-lifecycle" aria-label="业务生命周期">${lifecycle(r.stage, r.status)}</div>
        ${pct !== null ? `<div class="progress-block"><div class="progress-label"><span>当前进度</span><strong>${pct}%</strong></div><div class="progress"><div style="width:${pct}%"></div></div></div>` : ''}
        <div class="order-update"><span class="muted">最新状态</span><h2>${XH.esc(r.progressText || '等待更新')}</h2><div class="muted">最近更新：${new Date(r.updatedAt).toLocaleString('zh-CN')}</div></div>
        ${r.customerNote ? `<div class="notice">${XH.esc(r.customerNote)}</div>` : ''}
        <div class="actions order-actions">
          ${r.stage === 'ORDER' && r.hasProgressImage ? `<a class="btn" target="_blank" rel="noopener" href="/api/records/${encodeURIComponent(r.businessNo)}/image">查看最新截图</a>` : ''}
          <a class="btn" href="/records">业务记录</a>
          <a class="btn primary" href="/contact" data-track="contact_click">一对一咨询</a>
        </div>`;
    } catch {
      box.innerHTML = `<div class="notice error">未找到记录或业务编号无效。请检查编号后重试。</div><div class="actions"><a class="btn" href="/records">返回业务记录</a><a class="btn primary" href="/contact">一对一咨询</a></div>`;
    }
  }

  async function initRecords() {
    await XH.loadConfig();
    bindRecordSearch($('#recordLookupForm'));

    const list = $('#recordList');
    const saved = XH.getSavedRecords();
    if (!saved.length) {
      list.innerHTML = '<div class="empty card">当前浏览器还没有记录。提交询价后会自动保存在这里；更换设备时可输入业务编号查询。</div>';
    } else {
      const rows = await Promise.all(saved.map(async savedRow => {
        try { return await XH.api(`/api/records/${encodeURIComponent(savedRow.businessNo)}`); }
        catch { return { businessNo: savedRow.businessNo, stage: savedRow.stage || 'QUOTE', status: null, title: '历史业务记录', updatedAt: savedRow.savedAt || savedRow.createdAt, unavailable: true }; }
      }));
      list.innerHTML = rows.map(r => `<a class="card record-card" href="/record/${encodeURIComponent(r.businessNo)}">
        <div class="record-card-head"><span class="tag">${r.stage === 'ORDER' ? '履约' : '询价'}</span>${r.status ? `<span class="availability">${XH.esc(XH.recordStatusLabel(r.stage, r.status))}</span>` : '<span class="muted small">待重新查询</span>'}</div>
        <strong class="mono">${XH.esc(r.businessNo)}</strong>
        <h3>${XH.esc(r.title || '服务记录')}</h3>
        <div class="muted small">${r.unavailable ? '当前暂时无法读取，请点击重试' : `更新于 ${new Date(r.updatedAt).toLocaleString('zh-CN')}`}</div>
      </a>`).join('');
    }

    $('#clearLocalRecords')?.addEventListener('click', () => {
      if (!confirm('只清除当前浏览器保存的编号记录，不会删除已有业务记录。确定继续？')) return;
      XH.clearSavedRecords();
      location.reload();
    });
  }

  async function initContact() {
    await XH.loadConfig();
    normalizeContactPage();
    observeContactChannelRefresh();
    XH.track('contact_click');
    bindCopyButtons();
  }

  function runPage(init) {
    Promise.resolve().then(init).catch(error => {
      console.error('[XH page init]', error);
      if (!document.querySelector('.page-load-error')) {
        const box=document.createElement('div');
        box.className='page-load-error';
        box.setAttribute('role','status');
        box.textContent='部分数据暂时加载失败。你可以刷新重试，或直接进入一对一咨询；已填写的表单不会因为这条提示自动提交。';
        const main=document.querySelector('main');
        if (main) main.insertAdjacentElement('beforebegin',box);
        else document.body.prepend(box);
      }
    });
  }

  if (page === 'home') runPage(initHome);
  if (page === 'services') runPage(initServices);
  if (page === 'quote') runPage(initQuote);
  if (page === 'order') runPage(initOrder);
  if (page === 'record-detail') runPage(initRecordDetail);
  if (page === 'records') runPage(initRecords);
  if (page === 'contact') runPage(initContact);
  if (page === 'service-detail') runPage(initServiceDetail);
  if (page === 'rules') XH.track('rule_open');
  if (page === 'trust') XH.track('trust_open');
})();
