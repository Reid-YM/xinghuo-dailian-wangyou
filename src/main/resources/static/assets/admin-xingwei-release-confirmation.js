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
    if (note) note.disabled = !canConfirm;
    if (stableButton) stableButton.disabled = !canConfirm || !snapshot.stableEligible;
    if (attentionButton) attentionButton.disabled = !canConfirm;

    if (!snapshot.available) setState('error', '人工确认记录不可用');
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
      const canConfirm = Boolean(lastView?.canConfirm);
      if (stableButton) stableButton.disabled = !canConfirm || !current?.stableEligible;
      if (attentionButton) attentionButton.disabled = !canConfirm;
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
