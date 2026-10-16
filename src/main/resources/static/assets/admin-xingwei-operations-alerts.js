(() => {
  'use strict';

  const API = '/api/admin/xingwei/operations-alerts';
  const REQUEST_TIMEOUT_MS = 8000;
  const REFRESH_INTERVAL_MS = 60000;
  const root = document.getElementById('xwrOperationsAlerts');
  if (!root) return;

  const state = document.getElementById('xwrOperationsAlertState');
  const message = document.getElementById('xwrOperationsAlertMessage');
  const sampledAt = document.getElementById('xwrOperationsAlertSampledAt');
  const list = document.getElementById('xwrOperationsAlertList');
  const values = {
    severity: document.getElementById('xwrOperationsSeverity'),
    serverErrors: document.getElementById('xwrOperations5xx'),
    heap: document.getElementById('xwrOperationsHeap'),
    database: document.getElementById('xwrOperationsDatabase')
  };

  let timer = 0;
  let inFlight = false;

  function setState(tone, text) {
    if (!state) return;
    state.classList.toggle('ok', tone === 'ok');
    state.classList.toggle('warning', tone === 'watch' || tone === 'warning');
    state.classList.toggle('error', tone === 'critical' || tone === 'error');
    state.dataset.alertState = tone;
    const label = state.querySelector('span');
    if (label) label.textContent = text;
  }

  function formatTime(value) {
    const date = new Date(value);
    return Number.isNaN(date.getTime()) ? '-' : date.toLocaleString('zh-CN');
  }

  function percent(value) {
    const number = Number(value || 0);
    return Number.isFinite(number) ? `${number.toFixed(1)}%` : '-';
  }

  function severityTone(severity) {
    const value = String(severity || '').toUpperCase();
    if (value === 'CRITICAL') return 'critical';
    if (value === 'WARNING') return 'warning';
    if (value === 'WATCH') return 'watch';
    return 'ok';
  }

  function clearValues() {
    Object.values(values).forEach(element => {
      if (element) element.textContent = '-';
    });
    if (list) list.replaceChildren();
    if (sampledAt) sampledAt.textContent = '自动复核：不可用';
    root.dataset.alertReady = 'false';
    window.xwrOperationsAlerts = null;
  }

  function renderAlerts(alerts) {
    if (!list) return;
    list.replaceChildren();
    if (!Array.isArray(alerts) || alerts.length === 0) {
      const empty = document.createElement('div');
      empty.className = 'xwr-ops-empty';
      empty.textContent = '当前没有达到观察阈值的运行异常。';
      list.appendChild(empty);
      return;
    }

    alerts.forEach(alert => {
      const article = document.createElement('article');
      const tone = severityTone(alert?.level);
      article.className = 'xwr-ops-alert';
      article.dataset.level = tone;

      const head = document.createElement('div');
      head.className = 'xwr-ops-alert-head';
      const title = document.createElement('strong');
      title.textContent = alert?.title || '待复核项';
      const source = document.createElement('span');
      source.textContent = alert?.source === 'HOST_ATTESTATION' ? '主机发布真值' : '应用运行真值';
      head.append(title, source);

      const detail = document.createElement('p');
      detail.textContent = alert?.detail || '';
      const action = document.createElement('small');
      action.textContent = `建议复核：${alert?.recommendedAction || '结合运行健康与发布记录继续核验。'}`;
      article.append(head, detail, action);
      list.appendChild(article);
    });
  }

  function render(snapshot) {
    window.xwrOperationsAlerts = snapshot;
    const runtime = snapshot?.runtime || {};
    const tone = severityTone(snapshot?.severity);
    const count = Number(snapshot?.activeAlertCount || 0);

    if (values.severity) values.severity.textContent = `${snapshot?.severityLabel || '正常'} · ${count} 项`;
    if (values.serverErrors) {
      values.serverErrors.textContent = `${Number(runtime.recentServerErrors || 0)} / ${Number(runtime.recentRequests || 0)} · ${percent(Number(runtime.recentServerErrorRate || 0) * 100)}`;
    }
    if (values.heap) values.heap.textContent = percent(runtime.heapPercent);
    if (values.database) values.database.textContent = runtime.databaseUp
      ? `正常 · ${Number(runtime.databaseCheckMillis || 0)} ms`
      : '连通失败';

    renderAlerts(snapshot?.alerts || []);
    setState(tone, count > 0 ? `${snapshot?.severityLabel || '观察'} · ${count} 项` : '稳定运行');
    if (sampledAt) sampledAt.textContent = `采样：${formatTime(snapshot?.sampledAt)} · ${Number(snapshot?.recheckAfterSeconds || 60)} 秒后自动复核`;
    if (message) message.textContent = '';
    root.dataset.alertReady = 'true';
    root.dataset.severity = tone;
  }

  function renderFailure(error, status = 0) {
    root.dataset.alertReady = 'false';
    if (status === 403 || status === 401) clearValues();
    if (status === 403) {
      setState('error', '无稳定运营查看权限');
      if (message) message.textContent = '当前账号没有 deploy:view 权限，稳定运营告警保持隐藏。';
      clearTimer();
      return;
    }
    if (status === 401) {
      setState('error', '登录状态失效');
      if (message) message.textContent = '登录状态已失效，稳定运营告警保持隐藏。';
      clearTimer();
      return;
    }
    setState('warning', '自动复核失败');
    if (message) message.textContent = error?.name === 'AbortError'
      ? '稳定运营复核请求超时；这不等同于生产故障，请结合运行健康与发布流水线核验。'
      : '暂时无法读取稳定运营告警；这不等同于生产故障，请结合运行健康与发布流水线核验。';
  }

  function clearTimer() {
    if (!timer) return;
    window.clearTimeout(timer);
    timer = 0;
  }

  function schedule() {
    clearTimer();
    if (!document.hidden) timer = window.setTimeout(load, REFRESH_INTERVAL_MS);
  }

  async function load() {
    if (inFlight) return;
    inFlight = true;
    clearTimer();
    const controller = new AbortController();
    const timeout = window.setTimeout(() => controller.abort(), REQUEST_TIMEOUT_MS);
    try {
      const response = await fetch(API, {
        credentials: 'same-origin',
        cache: 'no-store',
        headers: {'Accept':'application/json','Cache-Control':'no-cache'},
        signal: controller.signal
      });
      if (!response.ok) {
        const error = new Error(`status ${response.status}`);
        error.status = response.status;
        throw error;
      }
      render(await response.json());
    } catch (error) {
      renderFailure(error, Number(error?.status || 0));
    } finally {
      window.clearTimeout(timeout);
      inFlight = false;
      if (state?.dataset.alertState !== 'error') schedule();
    }
  }

  document.addEventListener('visibilitychange', () => {
    if (document.hidden) clearTimer();
    else load();
  });
  window.addEventListener('online', load);
  window.addEventListener('offline', () => {
    clearTimer();
    setState('warning', '浏览器离线 · 等待恢复');
  });
  load();
})();
