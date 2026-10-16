(() => {
  'use strict';

  const API = '/api/admin/xingwei/release-stability';
  const REQUEST_TIMEOUT_MS = 8000;
  const REFRESH_INTERVAL_MS = 60000;
  const root = document.getElementById('xwrReleaseStability');
  if (!root) return;

  const state = document.getElementById('xwrReleaseStabilityState');
  const refresh = document.getElementById('xwrReleaseStabilityRefresh');
  const message = document.getElementById('xwrReleaseStabilityMessage');
  const sampledAt = document.getElementById('xwrReleaseStabilitySampledAt');
  const observationAge = document.getElementById('xwrReleaseObservationAge');
  const coverage = document.getElementById('xwrReleaseObservationCoverage');
  const baselineState = document.getElementById('xwrReleaseBaselineState');
  const baselineMedian = document.getElementById('xwrReleaseBaselineMedian');
  const baselineReleases = document.getElementById('xwrReleaseBaselineReleases');
  const historyList = document.getElementById('xwrReleaseStabilityHistory');
  const windows = new Map([
    [5, document.getElementById('xwrReleaseWindow5')],
    [15, document.getElementById('xwrReleaseWindow15')],
    [30, document.getElementById('xwrReleaseWindow30')]
  ]);

  let timer = 0;
  let inFlight = false;
  let lastSnapshot = null;

  function clearTimer() {
    if (!timer) return;
    window.clearTimeout(timer);
    timer = 0;
  }

  function schedule() {
    clearTimer();
    if (!document.hidden) timer = window.setTimeout(() => load('interval'), REFRESH_INTERVAL_MS);
  }

  function withTimeout() {
    const controller = new AbortController();
    const timeout = window.setTimeout(() => controller.abort(), REQUEST_TIMEOUT_MS);
    return fetch(API, {
      credentials: 'same-origin',
      cache: 'no-store',
      headers: {'Accept': 'application/json', 'Cache-Control': 'no-cache'},
      signal: controller.signal
    }).finally(() => window.clearTimeout(timeout));
  }

  function setState(tone, text) {
    if (!state) return;
    state.classList.toggle('ok', tone === 'ok');
    state.classList.toggle('warning', tone === 'warning');
    state.classList.toggle('error', tone === 'error');
    state.dataset.stabilityState = tone;
    const label = state.querySelector('span');
    if (label) label.textContent = text;
  }

  function formatTime(value) {
    if (!value) return '-';
    const date = new Date(value);
    return Number.isNaN(date.getTime()) ? '-' : date.toLocaleString('zh-CN');
  }

  function formatAge(seconds) {
    const value = Math.max(0, Number(seconds || 0));
    const minutes = Math.floor(value / 60);
    if (minutes < 60) return `${minutes} 分钟`;
    const hours = Math.floor(minutes / 60);
    return `${hours} 小时 ${minutes % 60} 分钟`;
  }

  function percent(value) {
    const number = Number(value || 0) * 100;
    return Number.isFinite(number) ? `${number.toFixed(number >= 10 ? 1 : 2)}%` : '-';
  }

  function toneForSeverity(severity) {
    if (severity === 'CRITICAL') return 'critical';
    if (severity === 'WARNING') return 'warning';
    if (severity === 'WATCH') return 'watch';
    if (severity === 'OK') return 'ok';
    return '';
  }

  function labelForSeverity(severity) {
    return ({OK:'正常', WATCH:'观察', WARNING:'警告', CRITICAL:'严重', UNKNOWN:'未知'})[severity] || '未知';
  }

  function coverageLabel(value) {
    return ({
      COVERED:'当前 JVM 覆盖完整',
      PROCESS_RESTARTED:'JVM 已重启 · 窗口不完整',
      EXPIRED:'分钟聚合已过保留期',
      CLOCK_SKEW:'主机时间存在偏差',
      UNAVAILABLE:'当前发布凭证不可用'
    })[value] || '覆盖状态未知';
  }

  function renderWindow(item) {
    const element = windows.get(Number(item?.minutes || 0));
    if (!element) return;
    const card = element.closest('.xwr-stability-card');
    card?.setAttribute('data-tone', toneForSeverity(item?.severity));
    if (!item || !['IN_PROGRESS', 'COMPLETE'].includes(item.state)) {
      element.textContent = '覆盖不足';
      const small = element.nextElementSibling;
      if (small) small.textContent = coverageLabel(item?.state);
      return;
    }
    element.textContent = `${item.serverErrors} / ${item.totalRequests} 次`;
    const small = element.nextElementSibling;
    if (small) {
      const progress = item.state === 'COMPLETE' ? '已完成' : `${item.observedMinutes}/${item.minutes} 分钟`;
      small.textContent = `${progress} · 5xx ${percent(item.serverErrorRate)} · ${labelForSeverity(item.severity)}`;
    }
  }

  function clearDisplay() {
    if (observationAge) observationAge.textContent = '-';
    if (coverage) coverage.textContent = '-';
    if (baselineState) baselineState.textContent = '-';
    if (baselineMedian) baselineMedian.textContent = '-';
    if (baselineReleases) baselineReleases.textContent = '-';
    windows.forEach(element => {
      if (!element) return;
      element.textContent = '-';
      element.closest('.xwr-stability-card')?.removeAttribute('data-tone');
      const small = element.nextElementSibling;
      if (small) small.textContent = '等待观察数据';
    });
    if (historyList) historyList.innerHTML = '<p class="xwr-stability-empty">历史基线保持隐藏。</p>';
    if (sampledAt) sampledAt.textContent = '自动复核：不可用';
    root.dataset.stabilityReady = 'false';
    window.xwrReleaseStability = null;
  }

  function renderHistory(items) {
    if (!historyList) return;
    if (!Array.isArray(items) || items.length === 0) {
      historyList.innerHTML = '<p class="xwr-stability-empty">生产主机尚未形成可展示的历史发布凭证；完成后续受控发布后会自动累积。</p>';
      return;
    }
    historyList.innerHTML = '';
    items.forEach(item => {
      const row = document.createElement('article');
      row.className = 'xwr-stability-history-item';
      row.dataset.tone = item.state === 'WATCH' ? 'watch' : '';
      const release = document.createElement('strong');
      release.textContent = item.currentRelease || '-';
      const latency = document.createElement('span');
      latency.textContent = item.healthComparable ? `发布后最慢 ${item.afterMaxMillis} ms` : '旧凭证 · 无健康对比';
      const delta = document.createElement('small');
      delta.textContent = item.healthComparable
        ? `${item.deltaMillis >= 0 ? '+' : ''}${item.deltaMillis} ms · ${item.state === 'WATCH' ? '观察' : '稳定'}`
        : formatTime(item.publishedAt);
      row.append(release, latency, delta);
      historyList.appendChild(row);
    });
  }

  function render(snapshot) {
    lastSnapshot = snapshot;
    window.xwrReleaseStability = snapshot;
    window.xwrReleaseStabilityError = null;
    root.dataset.stabilityReady = 'true';

    const current = snapshot?.current;
    if (current?.available) {
      if (observationAge) observationAge.textContent = formatAge(current.ageSeconds);
      if (coverage) coverage.textContent = coverageLabel(current.coverageState);
      (current.windows || []).forEach(renderWindow);
    } else {
      if (observationAge) observationAge.textContent = '凭证不可用';
      if (coverage) coverage.textContent = coverageLabel('UNAVAILABLE');
      windows.forEach(element => {
        if (!element) return;
        element.textContent = '-';
        const small = element.nextElementSibling;
        if (small) small.textContent = '无法建立发布观察窗口';
      });
    }

    const baseline = snapshot?.baseline || {};
    if (baselineState) baselineState.textContent = baseline.ready ? '历史基线已建立' : '历史基线构建中';
    if (baselineMedian) baselineMedian.textContent = baseline.comparableReleaseCount > 0
      ? `${baseline.medianPostDeployMaxMillis} ms`
      : '-';
    if (baselineReleases) baselineReleases.textContent = baseline.comparableReleaseCount > 0
      ? `${baseline.comparableReleaseCount} 次可比 · ${baseline.stableReleaseCount} 稳定 · ${baseline.watchReleaseCount} 观察`
      : '等待至少 2 次可比发布';
    renderHistory(snapshot?.history);

    const severity = current?.severity || 'UNKNOWN';
    if (severity === 'CRITICAL') setState('error', '发布后观察 · 严重');
    else if (severity === 'WARNING') setState('warning', '发布后观察 · 警告');
    else if (severity === 'WATCH') setState('warning', '发布后观察 · 观察');
    else if (!current?.available || current.coverageState !== 'COVERED') setState('warning', '观察覆盖不足');
    else if (!baseline.ready) setState('warning', '运行稳定 · 基线构建中');
    else setState('ok', '发布后观察正常');

    if (sampledAt) sampledAt.textContent = `自动复核：${formatTime(snapshot?.sampledAt)} · 进程保留 ${snapshot?.processRetentionMinutes || '-'} 分钟`;
    if (message) {
      message.textContent = current?.coverageState === 'PROCESS_RESTARTED'
        ? '当前 JVM 在本次发布凭证之后重新启动，因此不能完整重建最初 5/15/30 分钟窗口；历史主机基线仍可独立查看。'
        : current?.coverageState === 'EXPIRED'
          ? '本次发布的分钟级进程数据已超过保留窗口；历史主机发布基线仍然有效。'
          : '';
    }
  }

  function renderFailure(error, status = 0) {
    window.xwrReleaseStabilityError = {status, name: error?.name || 'Error'};
    if (status === 403) {
      clearDisplay();
      setState('error', '无发布查看权限');
      if (message) message.textContent = '当前账号没有 deploy:view 权限，发布观察与历史基线保持隐藏。';
      clearTimer();
      return;
    }
    if (status === 401) {
      clearDisplay();
      setState('error', '登录状态失效');
      if (message) message.textContent = '登录状态已失效，请重新登录后台后再查看发布稳定性。';
      clearTimer();
      return;
    }
    root.dataset.stabilityReady = 'false';
    if (lastSnapshot) {
      setState('warning', '复核失败 · 保留上次结果');
      if (message) message.textContent = error?.name === 'AbortError' ? '本次发布稳定性请求超时，暂时保留上一次成功结果。' : '本次发布稳定性请求失败，暂时保留上一次成功结果。';
    } else {
      setState('error', error?.name === 'AbortError' ? '发布稳定性检查超时' : '发布稳定性检查失败');
      if (message) message.textContent = '无法读取发布稳定性数据；这不等同于生产故障，请结合 Deploy Production 与服务器现场核验。';
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
    refresh?.setAttribute('disabled', 'disabled');
    try {
      const response = await withTimeout();
      if (!response.ok) {
        const error = new Error(`status ${response.status}`);
        error.status = response.status;
        throw error;
      }
      const snapshot = await response.json();
      snapshot.reason = reason;
      render(snapshot);
    } catch (error) {
      renderFailure(error, Number(error?.status || 0));
    } finally {
      inFlight = false;
      refresh?.removeAttribute('disabled');
      if (Number(window.xwrReleaseStabilityError?.status || 0) !== 403
          && Number(window.xwrReleaseStabilityError?.status || 0) !== 401) schedule();
    }
  }

  refresh?.addEventListener('click', () => load('manual'));
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
