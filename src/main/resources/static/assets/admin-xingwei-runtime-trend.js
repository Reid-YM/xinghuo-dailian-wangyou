(() => {
  'use strict';

  const root = document.getElementById('xwrRuntimeTrend');
  const summary = document.getElementById('xwrRuntimeTrendSummary');
  if (!root) return;

  function timeLabel(value) {
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return '--:--';
    return date.toLocaleTimeString('zh-CN', {hour: '2-digit', minute: '2-digit', hour12: false});
  }

  function render(snapshot) {
    const buckets = Array.isArray(snapshot?.requests?.recentMinutes) ? snapshot.requests.recentMinutes : [];
    root.replaceChildren();
    if (!buckets.length) {
      root.textContent = '暂无分钟趋势数据';
      if (summary) summary.textContent = '当前进程尚未形成趋势窗口。';
      return;
    }

    const maxTotal = Math.max(1, ...buckets.map(item => Number(item.totalRequests || 0)));
    let totalRequests = 0;
    let clientErrors = 0;
    let serverErrors = 0;

    buckets.forEach(item => {
      const total = Number(item.totalRequests || 0);
      const clients = Number(item.clientErrors || 0);
      const servers = Number(item.serverErrors || 0);
      totalRequests += total;
      clientErrors += clients;
      serverErrors += servers;

      const column = document.createElement('div');
      column.className = 'xwr-trend-column';
      column.dataset.serverErrors = servers > 0 ? 'true' : 'false';
      column.dataset.clientErrors = clients > 0 ? 'true' : 'false';
      column.setAttribute('aria-label', `${timeLabel(item.minute)}：${total} 次请求，${clients} 次 4xx，${servers} 次 5xx`);

      const bar = document.createElement('i');
      bar.style.setProperty('--xwr-trend-height', `${Math.max(total > 0 ? 8 : 2, Math.round(total / maxTotal * 100))}%`);
      const label = document.createElement('small');
      label.textContent = timeLabel(item.minute);
      column.append(bar, label);
      root.append(column);
    });

    if (summary) {
      summary.textContent = `最近 15 分钟：${totalRequests} 次请求 · ${clientErrors} 次 4xx · ${serverErrors} 次 5xx。仅为当前 JVM 聚合趋势，进程重启后重新开始。`;
    }
  }

  document.addEventListener('xwr:runtime-health', event => render(event.detail));
  if (window.xwrRuntimeHealth) render(window.xwrRuntimeHealth);
})();
