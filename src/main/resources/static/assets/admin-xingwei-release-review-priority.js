(() => {
  'use strict';

  const root = document.getElementById('xwrReleaseConfirmation');
  if (!root) return;

  const POLL_MS = 750;
  let lastSignature = '';

  function formatMillis(value) {
    const n = Number(value || 0);
    return Number.isFinite(n) ? `${Math.max(0, Math.round(n))} ms` : '-';
  }

  function formatDuration(value) {
    const n = Number(value || 0);
    if (!Number.isFinite(n) || n <= 0) return '-';
    if (n >= 3_600_000) return `${Math.round(n / 3_600_000)} 小时`;
    if (n >= 60_000) return `${Math.round(n / 60_000)} 分钟`;
    return `${Math.round(n / 1000)} 秒`;
  }

  function priorityLabel(value) {
    return ({
      REVIEW_NOW:'优先人工复核',
      REVIEW_READY:'可按流程复核',
      FOLLOW_UP:'继续观察',
      OBSERVE:'等待观察完成',
      BLOCKED:'先恢复可信数据',
      COMPLETE:'已完成',
      ARCHIVED_COMPLETE:'历史已完成',
      ARCHIVED_ATTENTION:'历史关注记录',
      ARCHIVED_GAP:'历史复核缺口'
    })[value] || value || '未知';
  }

  function ensurePanel() {
    let panel = document.getElementById('xwrReleaseReviewPriority');
    if (panel) return panel;
    panel = document.createElement('div');
    panel.id = 'xwrReleaseReviewPriority';
    panel.className = 'xwr-confirm-form';
    panel.dataset.priorityReady = 'false';

    const head = document.createElement('div');
    head.className = 'xwr-stability-baseline-head';
    const titleWrap = document.createElement('div');
    const title = document.createElement('h3');
    title.textContent = '复核优先级与风险查询预算';
    const desc = document.createElement('p');
    desc.textContent = '优先级只帮助管理员决定先看什么；不会自动确认、回滚、重启或部署。';
    titleWrap.append(title, desc);
    const state = document.createElement('p');
    state.id = 'xwrReleaseReviewPriorityState';
    state.textContent = '等待复核队列';
    head.append(titleWrap, state);

    const grid = document.createElement('div');
    grid.className = 'xwr-confirm-grid';
    for (const [id, label, hint] of [
      ['Current','当前优先级','由观察状态 + 透明风险共同解释'],
      ['Elapsed','本次风险查询','包含缓存读取与必要的外部查询'],
      ['Budget','外部请求预算','按实际查阅的 PR 数量计算'],
      ['Unknown','风险未知','未知不会被降级成低风险']
    ]) {
      const card = document.createElement('article');
      card.className = 'xwr-confirm-card';
      const span = document.createElement('span');
      span.textContent = label;
      const strong = document.createElement('strong');
      strong.id = `xwrReleasePriority${id}`;
      strong.textContent = '-';
      const small = document.createElement('small');
      small.textContent = hint;
      card.append(span, strong, small);
      grid.appendChild(card);
    }

    const reason = document.createElement('p');
    reason.id = 'xwrReleasePriorityReason';
    reason.className = 'xwr-confirm-empty';
    reason.textContent = '等待当前版本复核解释。';

    const cache = document.createElement('p');
    cache.id = 'xwrReleasePriorityCache';
    cache.className = 'xwr-confirm-empty';
    cache.textContent = '等待风险缓存规则。';

    panel.append(head, grid, reason, cache);
    const queue = document.getElementById('xwrReleaseReviewQueue');
    if (queue?.parentNode) queue.parentNode.insertBefore(panel, queue.nextSibling);
    else {
      const meta = root.querySelector('.xwr-confirm-meta');
      if (meta) root.insertBefore(panel, meta);
      else root.appendChild(panel);
    }
    return panel;
  }

  function setText(id, value) {
    const node = document.getElementById(id);
    if (node) node.textContent = String(value ?? '-');
  }

  function clear() {
    const panel = ensurePanel();
    panel.dataset.priorityReady = 'false';
    setText('xwrReleaseReviewPriorityState', '等待复核队列');
    setText('xwrReleasePriorityCurrent', '-');
    setText('xwrReleasePriorityElapsed', '-');
    setText('xwrReleasePriorityBudget', '-');
    setText('xwrReleasePriorityUnknown', '-');
    setText('xwrReleasePriorityReason', '当前没有可展示的可信复核优先级。');
    setText('xwrReleasePriorityCache', '风险查询结果不可用。');
  }

  function render(view) {
    const panel = ensurePanel();
    const releases = Array.isArray(view?.releases) ? view.releases : [];
    const current = releases.find(item => item?.current) || null;
    const runtime = view?.riskRuntime || {};
    const rules = view?.riskRules || {};
    const summary = view?.summary || {};
    const cacheHits = releases.filter(item => item?.risk?.cacheState === 'HIT').length;

    panel.dataset.priorityReady = 'true';
    setText('xwrReleaseReviewPriorityState', summary.currentNeedsAction ? '当前版本仍需复核' : '当前版本复核已完成');
    setText('xwrReleasePriorityCurrent', priorityLabel(current?.reviewPriority));
    setText('xwrReleasePriorityElapsed', formatMillis(runtime.elapsedMillis));
    setText('xwrReleasePriorityBudget', formatMillis(runtime.externalBudgetMillis));
    setText('xwrReleasePriorityUnknown', Number(runtime.unknownCount || 0));
    setText('xwrReleasePriorityReason', current?.priorityReason || '当前版本没有复核解释。');
    setText('xwrReleasePriorityCache',
      `单 PR 外部请求预算 ${formatMillis(rules.perPrLookupBudgetMillis || runtime.perLookupBudgetMillis)}；成功结果缓存 ${formatDuration(rules.successCacheTtlMillis)}；UNKNOWN 短缓存 ${formatDuration(rules.unknownCacheTtlMillis)}。本次尝试 ${Number(runtime.lookupCount || 0)} 个 PR，缓存命中 ${cacheHits} 个；预算只约束外部风险查询，不代表整个接口 SLA。`);
  }

  function tick() {
    const view = window.xwrReleaseReviewQueue;
    if (!view?.available) {
      if (lastSignature !== 'empty') {
        lastSignature = 'empty';
        clear();
      }
      return;
    }
    const releases = Array.isArray(view.releases) ? view.releases : [];
    const current = releases.find(item => item?.current);
    const runtime = view.riskRuntime || {};
    const cacheSignature = releases.map(item => item?.risk?.cacheState || '').join(',');
    const signature = [
      view.sampledAt || '',
      current?.reviewPriority || '',
      current?.priorityReason || '',
      runtime.elapsedMillis || 0,
      runtime.lookupCount || 0,
      runtime.unknownCount || 0,
      cacheSignature
    ].join('|');
    if (signature === lastSignature) return;
    lastSignature = signature;
    render(view);
  }

  ensurePanel();
  tick();
  window.setInterval(tick, POLL_MS);
})();
