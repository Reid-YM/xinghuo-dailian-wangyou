(() => {
  'use strict';
  const runtimePath = pathname => window.XHFrontendShell?.runtimePath?.(pathname) || pathname || location.pathname;
  const runtimeHref = value => window.XHFrontendShell?.runtimeHref?.(value) || value;

  if (!/^\/next(?:\/|$)/.test(runtimePath())) return;

  const text = (value) => String(value ?? '').trim();
  const STAGE_RUNTIME_VERSIONS = (() => {
    try {
      return JSON.parse(document.currentScript?.dataset.stageVersions || '{}');
    } catch (ignore) {
      return {};
    }
  })();

  function explicitLegacy(link, url) {
    if (!link || !url) return false;
    if (link.dataset.n10Legacy === '1' || link.dataset.n13Legacy === '1' || link.dataset.n14Legacy === '1' || link.dataset.n23Legacy === '1' || link.dataset.n24Legacy === '1' || link.dataset.n25Legacy === '1' || link.dataset.n26Legacy === '1' || link.dataset.n27Legacy === '1' || link.dataset.n28Legacy === '1') return true;
    if (link.dataset.n22Legacy === '1') return true;
    return url.searchParams.get('legacy') === '1';
  }

  function sameOriginUrl(link) {
    const href = link?.getAttribute?.('href');
    if (!href || href.startsWith('#') || href.startsWith('javascript:')) return null;
    try {
      const url = new URL(href, location.origin);
      return url.origin === location.origin ? url : null;
    } catch (ignore) {
      return null;
    }
  }

  function nextUrl(url, pathname) {
    const target = new URL(url.href);
    target.pathname = pathname;
    target.searchParams.delete('legacy');
    return runtimeHref(target.pathname + target.search + target.hash);
  }

  function normalizedHref(link) {
    const url = sameOriginUrl(link);
    if (!url) return '';
    if (explicitLegacy(link, url)) {
      url.searchParams.delete('legacy');
      return url.pathname + url.search + url.hash;
    }
    if (/^\/next(?:\/|$)/.test(url.pathname)) return runtimeHref(url.pathname + url.search + url.hash);

    if (url.pathname === '/services') return nextUrl(url, '/next/services');
    if (url.pathname === '/quote') return nextUrl(url, '/next/quote');
    if (url.pathname === '/order') return nextUrl(url, '/next/records');
    const oldOrder = /^\/order\/([^/?#]+)\/?$/.exec(url.pathname);
    if (oldOrder) return nextUrl(url, `/next/record/${oldOrder[1]}`);
    if (url.pathname === '/loadouts' || url.pathname === '/loadouts/') return nextUrl(url, '/next/mastery');
    if (url.pathname === '/records') return nextUrl(url, '/next/records');
    if (url.pathname === '/contact') return nextUrl(url, '/next/contact');
    if (url.pathname === '/trust') return nextUrl(url, '/next/trust');
    if (url.pathname === '/rules') return nextUrl(url, '/next/rules');
    if (url.pathname === '/privacy') return nextUrl(url, '/next/privacy');
    if (url.pathname === '/rules-en' || url.pathname === '/privacy-en') return nextUrl(url, '/next' + url.pathname);
    if (url.pathname === '/account') return nextUrl(url, '/next/account');
    if (url.pathname === '/account/submit') return nextUrl(url, '/next/account/submit');
    if (url.pathname === '/guides') return nextUrl(url, '/next/guides');
    if (url.pathname === '/mastery') return nextUrl(url, '/next/mastery');

    const service = /^\/service\/([^/?#]+)\/?$/.exec(url.pathname);
    if (service) return nextUrl(url, `/next/service/${service[1]}`);

    const guide = /^\/guides\/([^/?#]+)\/?$/.exec(url.pathname);
    if (guide) return nextUrl(url, `/next/guides/${guide[1]}`);

    const record = /^\/record\/([^/?#]+)\/?$/.exec(url.pathname);
    if (record) return nextUrl(url, `/next/record/${record[1]}`);

    const game = /^\/game\/([^/?#]+)\/?$/.exec(url.pathname);
    if (game) return nextUrl(url, `/next/game/${game[1]}`);
    return '';
  }

  function normalizeLink(link) {
    if (!(link instanceof HTMLAnchorElement)) return false;
    const target = normalizedHref(link);
    if (!target || link.getAttribute('href') === target) return false;
    link.setAttribute('href', target);
    link.dataset.n14Normalized = '1';
    return true;
  }

  function normalizeTree(root = document) {
    if (root instanceof HTMLAnchorElement) normalizeLink(root);
    root.querySelectorAll?.('a[href]').forEach(normalizeLink);
  }

  function stageSource(path) {
    const version = STAGE_RUNTIME_VERSIONS[path];
    return /^[0-9a-f]{64}$/.test(version || '') ? `${path}?v=${version}` : path;
  }

  function ensureRuntime(id, source, matcher) {
    if (!matcher.test(runtimePath()) || document.getElementById(id)) return;
    const script = document.createElement('script');
    script.id = id;
    script.src = source;
    script.defer = true;
    const status = document.createElement('div');
    status.className = 'next-config-state';
    status.setAttribute('role', 'status');
    status.textContent = '正在加载页面…';
    document.querySelector('.nav')?.after(status);
    script.onload = () => status.remove();
    script.onerror = () => {
      if (!matcher.test(runtimePath())) { status.remove(); return; }
      status.textContent = '页面资源加载失败，请重试。';
      const retry = document.createElement('button');
      retry.type = 'button';
      retry.className = 'btn';
      retry.textContent = '重新加载页面';
      retry.onclick = () => {
        status.remove();
        script.remove();
        ensureRuntime(id, source, matcher);
      };
      status.append(retry);
    };
    document.head.append(script);
  }

  function ensureStageRuntimes() {
    ensureRuntime('frontend-game-runtime', stageSource('/next/game-detail.js'), /^\/next\/game\/[^/]+\/?$/);
    ensureRuntime('frontend-next-n23-runtime', stageSource('/next/n23-guides-center.js'), /^\/next\/guides\/?$/);
    ensureRuntime('frontend-next-n24-runtime', stageSource('/next/n24-mastery.js'), /^\/next\/mastery\/?$/);
    ensureRuntime('frontend-next-n25-runtime', stageSource('/next/n25-submission-workbench.js'), /^\/next\/account\/submit\/?$/);
    ensureRuntime('frontend-next-n26-runtime', stageSource('/next/n26-records-center.js'), /^\/next\/records\/?$/);
    ensureRuntime('frontend-next-n26-history-bridge', stageSource('/next/n26-record-history-bridge.js'), /^\/next\/(?:quote\/?$|record\/)/);
    ensureRuntime('frontend-next-n27-runtime', stageSource('/next/n27-support-trust.js'), /^\/next\/(?:contact|trust)\/?$/);
    ensureRuntime('frontend-next-n28-runtime', stageSource('/next/n28-legal-single-source.js'), /^\/next\/(?:rules|privacy)(?:-en)?\/?$/);
  }

  document.addEventListener('click', (event) => {
    const link = event.target.closest?.('a[href]');
    if (link) normalizeLink(link);
  }, true);

  const observer = new MutationObserver((records) => {
    records.forEach((record) => {
      if (record.type === 'attributes') normalizeLink(record.target);
      record.addedNodes.forEach((node) => {
        if (node instanceof Element) normalizeTree(node);
      });
    });
  });

  observer.observe(document.documentElement, {
    subtree: true,
    childList: true,
    attributes: true,
    attributeFilter: ['href']
  });

  window.addEventListener('xh:pagechange', () => {
    normalizeTree();
    ensureStageRuntimes();
  });
  normalizeTree();
  ensureStageRuntimes();

  window.XHFrontendNextN14 = {
    normalize: normalizeTree,
    normalizedHref
  };
})();
