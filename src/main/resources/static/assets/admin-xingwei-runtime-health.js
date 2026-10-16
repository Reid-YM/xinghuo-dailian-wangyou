(() => {
  'use strict';

  const API = '/api/admin/xingwei/runtime-health';
  const REQUEST_TIMEOUT_MS = 8000;
  const REFRESH_INTERVAL_MS = 60000;

  const root = document.getElementById('xwrRuntimeHealth');
  if (!root) return;

  const stateBadge = document.getElementById('xwrRuntimeHealthState');
  const refreshButton = document.getElementById('xwrRuntimeHealthRefresh');
  const message = document.getElementById('xwrRuntimeHealthMessage');
  const sampledAt = document.getElementById('xwrRuntimeSampledAt');
  const lastServerError = document.getElementById('xwrRuntimeLastServerError');
  const values = {
    database: document.getElementById('xwrRuntimeDatabase'),
    uptime: document.getElementById('xwrRuntimeUptime'),
    heap: document.getElementById('xwrRuntimeHeap'),
    requests: document.getElementById('xwrRuntimeRequests'),
    clientErrors: document.getElementById('xwrRuntime4xx'),
    serverErrors: document.getElementById('xwrRuntime5xx')
  };

  let timer = 0;
  let inFlight = false;
  let lastSnapshot = null;

  function withTimeout(url, options = {}) {
    const controller = new AbortController();
    const timeout = window.setTimeout(() => controller.abort(), REQUEST_TIMEOUT_MS);
    return fetch(url, {...options, signal: controller.signal}).finally(() => window.clearTimeout(timeout));
  }

  function formatBytes(bytes) {
    const value = Number(bytes || 0);
    if (!Number.isFinite(value) || value < 0) return '-';
    if (value < 1024) return `${value} B`;
    const units = ['KB','MB','GB','TB'];
    let scaled = value / 1024;
    let index = 0;
    while (scaled >= 1024 && index < units.length - 1) { scaled /= 1024; index += 1; }
    return `${scaled >= 100 ? scaled.toFixed(0) : scaled >= 10 ? scaled.toFixed(1) : scaled.toFixed(2)} ${units[index]}`;
  }

  function formatDuration(ms) {
    const totalSeconds = Math.max(0, Math.floor(Number(ms || 0) / 1000));
    const days = Math.floor(totalSeconds / 86400);
    const hours = Math.floor((totalSeconds % 86400) / 3600);
    const minutes = Math.floor((totalSeconds % 3600) / 60);
    if (days > 0) return `${days}天 ${hours}小时`;
    if (hours > 0) return `${hours}小时 ${minutes}分钟`;
    return `${minutes}分钟`;
  }

  function formatTime(value) {
    if (!value) return '-';
    const parsed = new Date(value);
    return Number.isNaN(parsed.getTime()) ? '-' : parsed.toLocaleString('zh-CN');
  }

  function setCardTone(element, tone) {
    element?.closest('.xwr-health-card')?.setAttribute('data-tone', tone || '');
  }

  function setBadge(tone, text) {
    if (!stateBadge) return;
    stateBadge.classList.toggle('ok', tone === 'ok');
    stateBadge.classList.toggle('warning', tone === 'warning');
    stateBadge.classList.toggle('error', tone === 'error');
    stateBadge.dataset.healthState = tone;
    const label = stateBadge.querySelector('span');
    if (label) label.textContent = text;
  }

  function render(snapshot) {
    lastSnapshot = snapshot;
    window.xwrRuntimeHealth = snapshot;
    window.xwrRuntimeHealthError = null;

    const databaseUp = Boolean(snapshot?.database?.up);
    const totalRequests = Number(snapshot?.requests?.totalRequests || 0);
    const clientErrors = Number(snapshot?.requests?.clientErrors || 0);
    const serverErrors = Number(snapshot?.requests?.serverErrors || 0);
    const usedHeap = Number(snapshot?.jvm?.usedHeapBytes || 0);
    const maxHeap = Number(snapshot?.jvm?.maxHeapBytes || 0);
    const heapPercent = maxHeap > 0 ? Math.round((usedHeap / maxHeap) * 100) : 0;

    if (values.database) values.database.textContent = databaseUp ? '数据库正常' : '数据库异常';
    setCardTone(values.database, databaseUp ? 'ok' : 'error');
    const dbSmall = values.database?.nextElementSibling;
    if (dbSmall) dbSmall.textContent = databaseUp ? `SELECT 1 · ${snapshot.database.checkMillis} ms` : `连通检查失败 · ${snapshot.database.checkMillis} ms`;

    if (values.uptime) values.uptime.textContent = formatDuration(snapshot.uptimeMillis);
    const uptimeSmall = values.uptime?.nextElementSibling;
    if (uptimeSmall) uptimeSmall.textContent = `启动于 ${formatTime(snapshot.startedAt)}`;

    if (values.heap) values.heap.textContent = `${formatBytes(usedHeap)} / ${formatBytes(maxHeap)}`;
    const heapSmall = values.heap?.nextElementSibling;
    if (heapSmall) heapSmall.textContent = `堆占用约 ${heapPercent}% · ${snapshot?.jvm?.threadCount ?? '-'} 线程`;
    setCardTone(values.heap, heapPercent >= 90 ? 'warning' : '');

    if (values.requests) values.requests.textContent = String(totalRequests);
    if (values.clientErrors) values.clientErrors.textContent = String(clientErrors);
    if (values.serverErrors) values.serverErrors.textContent = String(serverErrors);
    setCardTone(values.clientErrors, clientErrors > 0 ? 'warning' : '');
    setCardTone(values.serverErrors, serverErrors > 0 ? 'warning' : 'ok');

    const last = snapshot?.requests?.lastServerError;
    if (lastServerError) lastServerError.textContent = last
      ? `最近 5xx：${last.status} · ${formatTime(last.at)}`
      : '最近 5xx：当前 JVM 启动后尚无记录';
    if (sampledAt) sampledAt.textContent = `采样时间：${formatTime(snapshot.sampledAt)}`;

    if (!databaseUp) setBadge('error', '数据库异常');
    else if (serverErrors > 0) setBadge('warning', '运行正常 · 有历史 5xx');
    else setBadge('ok', '运行正常');

    if (message) message.textContent = '';
    root.dataset.healthReady = 'true';
    document.dispatchEvent(new CustomEvent('xwr:runtime-health', {detail: snapshot}));
  }

  function renderFailure(error, status = 0) {
    window.xwrRuntimeHealthError = {status, name: error?.name || 'Error'};
    root.dataset.healthReady = 'false';
    if (status === 403) {
      setBadge('error', '无运行诊断权限');
      if (message) message.textContent = '当前账号没有 deploy:view 权限；版本记录仍可查看，运行诊断数据保持隐藏。';
      clearTimer();
      return;
    }
    if (status === 401) {
      setBadge('error', '登录状态失效');
      if (message) message.textContent = '登录状态已失效，请重新登录后台后再复核运行健康。';
    } else if (lastSnapshot) {
      setBadge('warning', '复核失败 · 保留上次结果');
      if (message) message.textContent = error?.name === 'AbortError' ? '本次运行健康请求超时，页面暂时保留上一次成功结果。' : '本次运行健康请求失败，页面暂时保留上一次成功结果。';
    } else {
      setBadge('error', error?.name === 'AbortError' ? '运行健康检查超时' : '运行健康检查失败');
      if (message) message.textContent = '无法读取运行健康数据；这不等同于生产已故障，请结合发布流水线和服务器核验。';
    }
  }

  function clearTimer() {
    if (!timer) return;
    window.clearTimeout(timer);
    timer = 0;
  }

  function schedule() {
    clearTimer();
    if (document.hidden) return;
    timer = window.setTimeout(() => load('interval'), REFRESH_INTERVAL_MS);
  }

  async function load(reason = 'manual') {
    if (inFlight) return;
    if (navigator.onLine === false) {
      renderFailure(new Error('offline'));
      setBadge('warning', '浏览器离线 · 等待恢复');
      clearTimer();
      return;
    }
    inFlight = true;
    clearTimer();
    refreshButton?.setAttribute('disabled', 'disabled');
    try {
      const response = await withTimeout(API, {
        credentials: 'same-origin',
        cache: 'no-store',
        headers: {'Accept':'application/json','Cache-Control':'no-cache'}
      });
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
      refreshButton?.removeAttribute('disabled');
      if (Number(window.xwrRuntimeHealthError?.status || 0) !== 403) schedule();
    }
  }

  refreshButton?.addEventListener('click', () => load('manual'));
  document.addEventListener('visibilitychange', () => {
    if (document.hidden) clearTimer();
    else load('visibility');
  });
  window.addEventListener('online', () => load('online'));
  window.addEventListener('offline', () => {
    clearTimer();
    setBadge('warning', '浏览器离线 · 等待恢复');
  });

  load('initial');
})();
