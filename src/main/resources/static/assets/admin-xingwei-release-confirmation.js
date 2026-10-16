(() => {
  'use strict';

  const API = '/api/admin/xingwei/release-confirmation';
  const REPO = 'https://github.com/Reid-YM/xinghuo-dailian-wangyou';
  const REQUEST_TIMEOUT_MS = 8000;
  const REFRESH_INTERVAL_MS = 60000;
  const root = document.getElementById('xwrReleaseConfirmation');
  if (!root) return;

  const state = document.getElementById('xwrReleaseConfirmationState');
  const release = document.getElementById('xwrReleaseConfirmationRelease');
  const change = document.getElementById('xwrReleaseConfirmationChange');
  const eligibility = document.getElementById('xwrReleaseConfirmationEligibility');
  const result = document.getElementById('xwrReleaseConfirmationResult');
  const latest = document.getElementById('xwrReleaseConfirmationLatest');
  const history = document.getElementById('xwrReleaseConfirmationHistory');
  const note = document.getElementById('xwrReleaseConfirmationNote');
  const stableButton = document.getElementById('xwrReleaseConfirmStable');
  const attentionButton = document.getElementById('xwrReleaseConfirmAttention');
  const refreshButton = document.getElementById('xwrReleaseConfirmationRefresh');
  const message = document.getElementById('xwrReleaseConfirmationMessage');
  const csrfToken = document.querySelector('meta[name="_csrf"]')?.content || '';
  const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content || 'X-CSRF-TOKEN';

  let timer = 0;
  let inFlight = false;
  let lastView = null;

  function clearTimer() {
    if (!timer) return;
    window.clearTimeout(timer);
    timer = 0;
  }

  function schedule() {
    clearTimer();
    if (!document.hidden) timer = window.setTimeout(() => load('interval'), REFRESH_INTERVAL_MS);
  }

  function setState(tone, text) {
    if (!state) return;
    state.classList.toggle('ok', tone === 'ok');
    state.classList.toggle('warning', tone === 'warning');
    state.classList.toggle('error', tone === 'error');
    state.dataset.confirmationState = tone;
    const label = state.querySelector('span');
    if (label) label.textContent = text;
  }

  function formatTime(value) {
    if (!value) return '-';
    const date = new Date(value);
    return Number.isNaN(date.getTime()) ? '-' : date.toLocaleString('zh-CN');
  }

  function decisionLabel(value) {
    return ({STABLE:'已人工确认稳定', NEEDS_ATTENTION:'需要继续观察', PENDING:'待人工确认'})[value] || '待人工确认';
  }

  function eligibilityLabel(value) {
    return ({
      ELIGIBLE:'30 分钟观察完整正常 · 可确认稳定',
      WINDOW_IN_PROGRESS:'30 分钟观察尚未完成',
      COVERAGE_INCOMPLETE:'当前 JVM 无法完整覆盖发布观察',
      AUTOMATED_ALERT:'自动观察存在异常 · 不允许确认稳定',
      OBSERVATION_UNAVAILABLE:'自动观察暂不可用'
    })[value] || '自动观察状态未知';
  }

  function clearNode(node) {
    if (node) node.replaceChildren();
  }

  function addLink(container, text, href) {
    if (!container) return;
    const link = document.createElement('a');
    link.href = href;
    link.target = '_blank';
    link.rel = 'noopener noreferrer';
    link.textContent = text;
    container.appendChild(link);
  }

  function clearDisplay() {
    if (release) release.textContent = '-';
    clearNode(change);
    if (change) change.textContent = '-';
    if (eligibility) eligibility.textContent = '-';
    if (result) result.textContent = '-';
    if (latest) latest.textContent = '-';
    if (history) history.replaceChildren();
    if (note) note.value = '';
    if (stableButton) stableButton.disabled = true;
    if (attentionButton) attentionButton.disabled = true;
    root.dataset.confirmationReady = 'false';
    root.dataset.canConfirm = 'false';
    window.xwrReleaseConfirmation = null;
    lastView = null;
  }

  function renderHistory(items) {
    if (!history) return;
    history.replaceChildren();
    if (!Array.isArray(items) || items.length === 0) {
      const empty = document.createElement('p');
      empty.className = 'xwr-confirm-empty';
      empty.textContent = '当前 release 尚无人工确认记录。';
      history.appendChild(empty);
      return;
    }
    items.slice().reverse().forEach(item => {
      const row = document.createElement('article');
      row.className = 'xwr-confirm-history-item';
      row.dataset.tone = item.decision === 'STABLE' ? 'ok' : 'warning';
      const title = document.createElement('strong');
      title.textContent = decisionLabel(item.decision);
      const meta = document.createElement('span');
      meta.textContent = `${item.confirmedBy || '-'} · ${formatTime(item.confirmedAt)}`;
      const observed = document.createElement('small');
      observed.textContent = `确认时：${item.coverageState || 'UNKNOWN'} / ${item.window30State || 'UNKNOWN'} / 30分钟 5xx ${Number(item.window30ServerErrors || 0)} / ${Number(item.window30TotalRequests || 0)}`;
      row.append(title, meta, observed);
      if (item.note) {
        const noteLine = document.createElement('p');
        noteLine.textContent = item.note;
        row.appendChild(noteLine);
      }
      history.appendChild(row);
    });
  }

  function render(view) {
    lastView = view;
    window.xwrReleaseConfirmation = view;
    window.xwrReleaseConfirmationError = null;
    root.dataset.confirmationReady = 'true';
    root.dataset.canConfirm = view?.canConfirm ? 'true' : 'false';

    const snapshot = view?.confirmation || {};
    const ref = snapshot.release || {};
    if (release) release.textContent = ref.currentRelease || '-';

    clearNode(change);
    if (change) {
      if (ref.pullRequestNumber) {
        addLink(change, `PR #${ref.pullRequestNumber}`, `${REPO}/pull/${ref.pullRequestNumber}`);
        change.appendChild(document.createTextNode(' · '));
      }
      if (ref.gitSha) addLink(change, String(ref.gitSha).slice(0, 12), `${REPO}/commit/${ref.gitSha}`);
      if (!change.childNodes.length) change.textContent = '-';
    }

    if (eligibility) eligibility.textContent = eligibilityLabel(snapshot.stableEligibility);
    if (result) result.textContent = decisionLabel(snapshot.status);
    if (latest) {
      latest.textContent = snapshot.latest
        ? `${snapshot.latest.confirmedBy} · ${formatTime(snapshot.latest.confirmedAt)}`
        : '尚未确认';
    }
    renderHistory(snapshot.history);

    const canConfirm = Boolean(view?.canConfirm);
    const canWrite = canConfirm && snapshot.available && snapshot.status !== 'STORAGE_MISSING';
    if (note) note.disabled = !canWrite;
    if (stableButton) stableButton.disabled = !canWrite || !snapshot.stableEligible;
    if (attentionButton) attentionButton.disabled = !canWrite;

    if (!snapshot.available) setState('error', '人工确认记录不可用');
    else if (snapshot.status === 'STORAGE_MISSING') setState('error', '人工确认存储不可用');
    else if (snapshot.status === 'NEEDS_ATTENTION') setState('warning', '已标记继续观察');
    else if (snapshot.status === 'STABLE') setState('ok', '已人工确认稳定');
    else if (!snapshot.stableEligible) setState('warning', '等待观察完成');
    else setState('ok', '可进行人工确认');

    if (message) {
      if (!canConfirm) message.textContent = '当前账号可查看发布观察，但只有管理员可提交人工确认。';
      else if (snapshot.status === 'STORAGE_MISSING') message.textContent = '人工确认目录尚未准备完成；请先核对生产部署日志。';
      else message.textContent = '';
    }
  }

  function request(method, body) {
    const controller = new AbortController();
    const timeout = window.setTimeout(() => controller.abort(), REQUEST_TIMEOUT_MS);
    const headers = {'Accept':'application/json', 'Cache-Control':'no-cache'};
    if (body !== undefined) {
      headers['Content-Type'] = 'application/json';
      if (csrfToken) headers[csrfHeader] = csrfToken;
    }
    return fetch(API, {
      method,
      credentials:'same-origin',
      cache:'no-store',
      headers,
      body: body === undefined ? undefined : JSON.stringify(body),
      signal: controller.signal
    }).finally(() => window.clearTimeout(timeout));
  }

  async function parseError(response) {
    try {
      const payload = await response.json();
      return payload?.error || `请求失败 (${response.status})`;
    } catch (_) {
      return `请求失败 (${response.status})`;
    }
  }

  function renderFailure(error, status = 0) {
    window.xwrReleaseConfirmationError = {status, name:error?.name || 'Error'};
    if (status === 401 || status === 403) {
      clearDisplay();
      setState('error', status === 401 ? '登录状态失效' : '无发布查看权限');
      if (message) message.textContent = status === 401
        ? '登录状态已失效，请重新登录后台。'
        : '当前账号没有 deploy:view 权限，人工确认数据保持隐藏。';
      clearTimer();
      return;
    }
    if (lastView) {
      setState('warning', '复核失败 · 保留上次结果');
      if (message) message.textContent = error?.message || '本次人工确认状态复核失败，暂时保留上次结果。';
    } else {
      setState('error', '人工确认状态读取失败');
      if (message) message.textContent = error?.message || '无法读取人工确认状态。';
    }
  }

  async function load(reason = 'manual') {
    if (inFlight) return;
    if (navigator.onLine === false) {
      setState('warning', '浏览器离线 · 等待恢复');
      clearTimer();
      return;
    }
    inFlight = true;
    clearTimer();
    refreshButton?.setAttribute('disabled', 'disabled');
    try {
      const response = await request('GET');
      if (!response.ok) {
        const error = new Error(await parseError(response));
        error.status = response.status;
        throw error;
      }
      const view = await response.json();
      view.reason = reason;
      render(view);
    } catch (error) {
      renderFailure(error, Number(error?.status || 0));
    } finally {
      inFlight = false;
      refreshButton?.removeAttribute('disabled');
      if (![401,403].includes(Number(window.xwrReleaseConfirmationError?.status || 0))) schedule();
    }
  }

  async function submit(decision) {
    const snapshot = lastView?.confirmation;
    if (!snapshot?.release?.currentRelease || !snapshot?.release?.gitSha) return;
    const body = {
      decision,
      note: note?.value || '',
      expectedRelease: snapshot.release.currentRelease,
      expectedGitSha: snapshot.release.gitSha
    };
    stableButton?.setAttribute('disabled', 'disabled');
    attentionButton?.setAttribute('disabled', 'disabled');
    try {
      const response = await request('POST', body);
      if (!response.ok) {
        const error = new Error(await parseError(response));
        error.status = response.status;
        throw error;
      }
      const view = await response.json();
      if (note) note.value = '';
      render(view);
      window.dispatchEvent(new CustomEvent('xwr:release-confirmation-updated'));
      if (message) message.textContent = decision === 'STABLE'
        ? '已记录本次 release 的人工稳定确认。'
        : '已记录本次 release 需要继续观察。';
    } catch (error) {
      if (Number(error?.status || 0) === 409) {
        if (message) message.textContent = '生产版本已变化，未写入旧版本确认；请立即刷新后重新核对。';
        setState('warning', '版本已变化 · 需刷新');
      } else if (Number(error?.status || 0) === 403) {
        clearDisplay();
        setState('error', '仅管理员可确认');
        if (message) message.textContent = '当前账号没有人工确认权限。';
      } else {
        if (message) message.textContent = error?.message || '人工确认提交失败。';
        setState('warning', '人工确认提交失败');
      }
    } finally {
      const current = lastView?.confirmation;
      const canWrite = Boolean(lastView?.canConfirm)
        && Boolean(current?.available)
        && current?.status !== 'STORAGE_MISSING';
      if (stableButton) stableButton.disabled = !canWrite || !current?.stableEligible;
      if (attentionButton) attentionButton.disabled = !canWrite;
    }
  }

  stableButton?.addEventListener('click', () => submit('STABLE'));
  attentionButton?.addEventListener('click', () => submit('NEEDS_ATTENTION'));
  refreshButton?.addEventListener('click', () => load('manual'));
  document.addEventListener('visibilitychange', () => {
    if (document.hidden) clearTimer();
    else load('visibility');
  });
  window.addEventListener('online', () => load('online'));
  window.addEventListener('offline', () => {
    clearTimer();
    setState('warning', '浏览器离线 · 等待恢复');
  });

  load('initial');
})();

(() => {
  'use strict';

  const API = '/api/admin/xingwei/release-review-queue';
  const REPO = 'https://github.com/Reid-YM/xinghuo-dailian-wangyou';
  const REQUEST_TIMEOUT_MS = 8000;
  const REFRESH_INTERVAL_MS = 60000;
  const root = document.getElementById('xwrReleaseConfirmation');
  if (!root) return;

  let timer = 0;
  let inFlight = false;
  let disabledByAuth = false;

  function ensureUi() {
    let panel = document.getElementById('xwrReleaseReviewQueue');
    if (panel) return panel;
    panel = document.createElement('div');
    panel.id = 'xwrReleaseReviewQueue';
    panel.className = 'xwr-confirm-form';
    panel.dataset.queueReady = 'false';

    const head = document.createElement('div');
    head.className = 'xwr-stability-baseline-head';
    const titleWrap = document.createElement('div');
    const title = document.createElement('h3');
    title.textContent = '最近发布人工复核队列';
    const desc = document.createElement('p');
    desc.textContent = '汇总最近可信生产 release、人工确认状态与透明变更风险。历史版本不允许追认稳定。';
    titleWrap.append(title, desc);
    const state = document.createElement('p');
    state.id = 'xwrReleaseReviewQueueState';
    state.textContent = '正在读取';
    head.append(titleWrap, state);

    const summary = document.createElement('div');
    summary.className = 'xwr-confirm-grid';
    summary.id = 'xwrReleaseReviewQueueSummary';
    for (const [id, label, hint] of [
      ['Tracked','追踪版本','最近最多 10 个可信 release'],
      ['Stable','已稳定','最近人工结论为稳定'],
      ['Attention','继续观察','存在人工继续观察结论'],
      ['Pending','未确认','含当前待确认与历史未确认']
    ]) {
      const card = document.createElement('article');
      card.className = 'xwr-confirm-card';
      const span = document.createElement('span');
      span.textContent = label;
      const strong = document.createElement('strong');
      strong.id = `xwrReleaseQueue${id}`;
      strong.textContent = '-';
      const small = document.createElement('small');
      small.textContent = hint;
      card.append(span, strong, small);
      summary.appendChild(card);
    }

    const list = document.createElement('div');
    list.id = 'xwrReleaseReviewQueueList';
    list.className = 'xwr-confirm-history';
    const empty = document.createElement('p');
    empty.className = 'xwr-confirm-empty';
    empty.textContent = '正在汇总最近发布记录。';
    list.appendChild(empty);

    const rules = document.createElement('p');
    rules.id = 'xwrReleaseReviewRiskRules';
    rules.className = 'xwr-confirm-empty';
    rules.textContent = '风险规则：等待读取。';

    panel.append(head, summary, list, rules);
    const meta = root.querySelector('.xwr-confirm-meta');
    if (meta) root.insertBefore(panel, meta);
    else root.appendChild(panel);
    return panel;
  }

  function formatTime(value) {
    if (!value) return '-';
    const date = new Date(value);
    return Number.isNaN(date.getTime()) ? '-' : date.toLocaleString('zh-CN');
  }

  function actionLabel(value) {
    return ({
      COMPLETE:'已确认稳定',
      READY:'可确认稳定',
      WAITING:'等待 30 分钟观察',
      FOLLOW_UP:'需要继续观察',
      BLOCKED:'确认状态不可用',
      ARCHIVED_ATTENTION:'历史继续观察',
      ARCHIVED_UNCONFIRMED:'历史未确认',
      ARCHIVED_INVALID:'历史确认记录异常'
    })[value] || value || '未知';
  }

  function riskLabel(risk) {
    const level = risk?.level || 'UNKNOWN';
    const categories = Array.isArray(risk?.categories) && risk.categories.length
      ? risk.categories.join(' / ')
      : '无可用类别';
    const count = risk?.available ? `${Number(risk.changedFileCount || 0)} 个文件` : '文件差异不可用';
    return `${level} · ${count} · ${categories}`;
  }

  function setText(id, value) {
    const node = document.getElementById(id);
    if (node) node.textContent = String(value ?? '-');
  }

  function clearQueue(messageText) {
    const panel = ensureUi();
    panel.dataset.queueReady = 'false';
    setText('xwrReleaseQueueTracked', '-');
    setText('xwrReleaseQueueStable', '-');
    setText('xwrReleaseQueueAttention', '-');
    setText('xwrReleaseQueuePending', '-');
    setText('xwrReleaseReviewQueueState', messageText || '不可用');
    const list = document.getElementById('xwrReleaseReviewQueueList');
    if (list) list.replaceChildren();
    window.xwrReleaseReviewQueue = null;
  }

  function appendLink(container, text, href) {
    const link = document.createElement('a');
    link.href = href;
    link.target = '_blank';
    link.rel = 'noopener noreferrer';
    link.textContent = text;
    container.appendChild(link);
  }

  function render(view) {
    const panel = ensureUi();
    panel.dataset.queueReady = 'true';
    window.xwrReleaseReviewQueue = view;
    const summary = view?.summary || {};
    setText('xwrReleaseQueueTracked', summary.trackedReleaseCount ?? 0);
    setText('xwrReleaseQueueStable', summary.stableCount ?? 0);
    setText('xwrReleaseQueueAttention', summary.needsAttentionCount ?? 0);
    setText('xwrReleaseQueuePending', summary.unconfirmedCount ?? 0);
    setText('xwrReleaseReviewQueueState', summary.currentNeedsAction ? '当前版本仍需人工复核' : '当前版本人工复核已完成');

    const list = document.getElementById('xwrReleaseReviewQueueList');
    if (list) {
      list.replaceChildren();
      const releases = Array.isArray(view?.releases) ? view.releases : [];
      if (!releases.length) {
        const empty = document.createElement('p');
        empty.className = 'xwr-confirm-empty';
        empty.textContent = '暂无可验证的生产发布历史。';
        list.appendChild(empty);
      }
      releases.forEach(item => {
        const row = document.createElement('article');
        row.className = 'xwr-confirm-history-item';
        row.dataset.tone = item.actionState === 'COMPLETE' ? 'ok' : 'warning';
        const title = document.createElement('strong');
        title.textContent = `${item.current ? '当前 · ' : ''}${actionLabel(item.actionState)}`;
        const ref = document.createElement('span');
        if (item.pullRequestNumber) {
          appendLink(ref, `PR #${item.pullRequestNumber}`, `${REPO}/pull/${item.pullRequestNumber}`);
          ref.appendChild(document.createTextNode(' · '));
        }
        if (item.gitSha) appendLink(ref, String(item.gitSha).slice(0, 12), `${REPO}/commit/${item.gitSha}`);
        const release = document.createElement('small');
        release.textContent = `${item.release || '-'} · ${formatTime(item.publishedAt)}`;
        const risk = document.createElement('small');
        risk.textContent = `变更风险：${riskLabel(item.risk)}`;
        row.append(title, ref, release, risk);
        if (item.confirmedAt || item.confirmedBy) {
          const confirmed = document.createElement('small');
          confirmed.textContent = `人工记录：${item.confirmedBy || '-'} · ${formatTime(item.confirmedAt)}`;
          row.appendChild(confirmed);
        }
        if (item.note) {
          const note = document.createElement('p');
          note.textContent = item.note;
          row.appendChild(note);
        }
        list.appendChild(row);
      });
    }

    const rules = view?.riskRules || {};
    setText('xwrReleaseReviewRiskRules',
      `风险规则 ${rules.version || '-'}：HIGH=${(rules.high || []).join('/')}；MEDIUM=${(rules.medium || []).join('/')}；${rules.lowOrUnknown || ''}。风险标签只决定人工复核优先级，不触发自动回滚。`);
  }

  async function load() {
    if (inFlight || disabledByAuth) return;
    if (navigator.onLine === false) {
      setText('xwrReleaseReviewQueueState', '浏览器离线 · 等待恢复');
      return;
    }
    inFlight = true;
    const controller = new AbortController();
    const timeout = window.setTimeout(() => controller.abort(), REQUEST_TIMEOUT_MS);
    try {
      const response = await fetch(API, {
        credentials:'same-origin',
        cache:'no-store',
        headers:{'Accept':'application/json','Cache-Control':'no-cache'},
        signal: controller.signal
      });
      if (response.status === 401 || response.status === 403) {
        disabledByAuth = true;
        clearQueue(response.status === 401 ? '登录状态失效' : '无发布查看权限');
        return;
      }
      if (!response.ok) throw new Error(`请求失败 (${response.status})`);
      const view = await response.json();
      if (!view?.available) {
        clearQueue(`发布复核队列不可用 · ${view?.status || 'UNKNOWN'}`);
        return;
      }
      render(view);
    } catch (error) {
      setText('xwrReleaseReviewQueueState', error?.name === 'AbortError' ? '风险/队列读取超时' : '队列复核失败');
    } finally {
      window.clearTimeout(timeout);
      inFlight = false;
      if (!disabledByAuth && !document.hidden) {
        window.clearTimeout(timer);
        timer = window.setTimeout(load, REFRESH_INTERVAL_MS);
      }
    }
  }

  ensureUi();
  window.addEventListener('xwr:release-confirmation-updated', load);
  window.addEventListener('online', load);
  document.addEventListener('visibilitychange', () => {
    if (document.hidden) window.clearTimeout(timer);
    else load();
  });
  load();
})();