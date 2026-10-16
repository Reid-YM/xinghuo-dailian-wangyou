(() => {
  'use strict';

  const API = '/api/admin/xingwei/release-attestation';
  const REQUEST_TIMEOUT_MS = 8000;
  const REFRESH_INTERVAL_MS = 60000;
  const root = document.getElementById('xwrReleaseAttestation');
  if (!root) return;

  const state = document.getElementById('xwrReleaseAttestationState');
  const message = document.getElementById('xwrReleaseAttestationMessage');
  const publishedAt = document.getElementById('xwrReleasePublishedAt');
  const values = {
    gitSha: document.getElementById('xwrReleaseGitSha'),
    jarSha: document.getElementById('xwrReleaseJarSha'),
    current: document.getElementById('xwrReleaseCurrent'),
    previous: document.getElementById('xwrReleasePrevious'),
    ports: document.getElementById('xwrReleasePorts'),
    checks: document.getElementById('xwrReleaseChecks')
  };

  let timer = 0;
  let inFlight = false;

  function setState(tone, text) {
    if (!state) return;
    state.classList.toggle('ok', tone === 'ok');
    state.classList.toggle('warning', tone === 'warning');
    state.classList.toggle('error', tone === 'error');
    state.dataset.releaseState = tone;
    const label = state.querySelector('span');
    if (label) label.textContent = text;
  }

  function formatTime(value) {
    const date = new Date(value);
    return Number.isNaN(date.getTime()) ? '-' : date.toLocaleString('zh-CN');
  }

  function shortSha(value) {
    return typeof value === 'string' && value.length >= 12 ? `${value.slice(0, 12)}…` : '-';
  }

  function withTimeout(url) {
    const controller = new AbortController();
    const timeout = window.setTimeout(() => controller.abort(), REQUEST_TIMEOUT_MS);
    return fetch(url, {
      credentials: 'same-origin',
      cache: 'no-store',
      headers: {'Accept': 'application/json', 'Cache-Control': 'no-cache'},
      signal: controller.signal
    }).finally(() => window.clearTimeout(timeout));
  }

  function render(snapshot) {
    window.xwrReleaseAttestation = snapshot;
    if (!snapshot?.available || !snapshot?.attestation) {
      root.dataset.releaseReady = 'false';
      setState('warning', snapshot?.status === 'INVALID' ? '发布凭证无效' : '发布凭证尚未生成');
      if (message) message.textContent = '当前没有可验证的主机侧成功发布凭证；这不等同于生产故障，请以 Deploy Production 和服务器现场为准。';
      return;
    }

    const item = snapshot.attestation;
    if (values.gitSha) {
      values.gitSha.textContent = shortSha(item.gitSha);
      values.gitSha.title = item.gitSha;
    }
    if (values.jarSha) {
      values.jarSha.textContent = shortSha(item.jarSha256);
      values.jarSha.title = item.jarSha256;
    }
    if (values.current) values.current.textContent = item.currentRelease || '-';
    if (values.previous) values.previous.textContent = item.previousRelease || '-';
    if (values.ports) values.ports.textContent = `Nginx → ${item.nginxUpstreamPort} · 正式 ${item.mainPort} · 金丝雀 ${item.canaryPort} 已停止`;
    if (values.checks) values.checks.textContent = item.checks?.predeploy18080 && item.checks?.canary8081 && item.checks?.production8080
      ? '18080 / 8081 / 8080 全部通过'
      : '发布凭证检查项不完整';
    if (publishedAt) publishedAt.textContent = `主机凭证时间：${formatTime(item.publishedAt)}`;
    if (message) message.textContent = '';
    setState('ok', '最近成功发布已核验');
    root.dataset.releaseReady = 'true';
  }

  function renderFailure(error, status = 0) {
    root.dataset.releaseReady = 'false';
    if (status === 403) {
      setState('error', '无发布查看权限');
      if (message) message.textContent = '当前账号没有 deploy:view 权限，生产发布凭证保持隐藏。';
      clearTimer();
      return;
    }
    setState('warning', status === 401 ? '登录状态失效' : '发布凭证复核失败');
    if (message) message.textContent = error?.name === 'AbortError'
      ? '读取主机侧发布凭证超时，请结合 Deploy Production 记录核验。'
      : '暂时无法读取主机侧发布凭证，请结合 Deploy Production 记录核验。';
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
    try {
      const response = await withTimeout(API);
      if (!response.ok) {
        const error = new Error(`status ${response.status}`);
        error.status = response.status;
        throw error;
      }
      render(await response.json());
    } catch (error) {
      renderFailure(error, Number(error?.status || 0));
    } finally {
      inFlight = false;
      if (state?.dataset.releaseState !== 'error') schedule();
    }
  }

  document.addEventListener('visibilitychange', () => {
    if (document.hidden) clearTimer();
    else load();
  });
  window.addEventListener('online', load);
  window.addEventListener('offline', clearTimer);
  load();
})();
