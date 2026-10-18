(() => {
  'use strict';

  if (!/^\/next(?:\/|$)/.test(location.pathname)) return;

  const text = (value) => String(value ?? '').trim();

  function explicitLegacy(link, url) {
    if (!link || !url) return false;
    if (link.dataset.n10Legacy === '1' || link.dataset.n13Legacy === '1' || link.dataset.n14Legacy === '1' || link.dataset.n22Legacy === '1' || link.dataset.n23Legacy === '1' || link.dataset.n24Legacy === '1' || link.dataset.n25Legacy === '1' || link.dataset.n26Legacy === '1') return true;
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
    return target.pathname + target.search + target.hash;
  }

  function catalogGameForSlug(slug) {
    const games = window.XHFrontendNextN2?.state?.catalog?.games;
    if (!Array.isArray(games)) return null;
    let normalized = text(slug);
    try {
      normalized = decodeURIComponent(normalized);
    } catch (ignore) {
      return null;
    }
    return games.find((game) => text(game?.seoSlug) === normalized && text(game?.code));
  }

  function gameHallTarget(url) {
    const match = /^\/game\/([^/?#]+)\/?$/.exec(url.pathname);
    if (!match) return '';
    const game = catalogGameForSlug(match[1]);
    if (!game) return '';
    const target = new URL('/next/services', location.origin);
    url.searchParams.forEach((value, key) => {
      if (key !== 'legacy' && key !== 'game') target.searchParams.append(key, value);
    });
    target.searchParams.set('game', text(game.code).toUpperCase());
    target.hash = url.hash;
    return target.pathname + target.search + target.hash;
  }

  function normalizedHref(link) {
    const url = sameOriginUrl(link);
    if (!url || explicitLegacy(link, url)) return '';

    if (url.pathname === '/services') return nextUrl(url, '/next/services');
    if (url.pathname === '/quote') return nextUrl(url, '/next/quote');
    if (url.pathname === '/account') return nextUrl(url, '/next/account');
    if (url.pathname === '/account/submit' || url.pathname === '/account/submit/') return nextUrl(url, '/next/account/submit');
    if (url.pathname === '/guides' || url.pathname === '/guides/') return nextUrl(url, '/next/guides');
    if (url.pathname === '/mastery' || url.pathname === '/mastery/') return nextUrl(url, '/next/mastery');
    if (url.pathname === '/records' || url.pathname === '/records/') return nextUrl(url, '/next/records');
    if (url.pathname === '/contact' || url.pathname === '/contact/') return nextUrl(url, '/next/contact');
    if (url.pathname === '/trust' || url.pathname === '/trust/') return nextUrl(url, '/next/trust');

    const service = /^\/service\/([^/?#]+)\/?$/.exec(url.pathname);
    if (service) return nextUrl(url, `/next/service/${service[1]}`);

    const guide = /^\/guides\/([^/?#]+)\/?$/.exec(url.pathname);
    if (guide) return nextUrl(url, `/next/guides/${guide[1]}`);

    const record = /^\/record\/([^/?#]+)\/?$/.exec(url.pathname);
    if (record) return nextUrl(url, `/next/record/${record[1]}`);

    return gameHallTarget(url);
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

  window.addEventListener('xh:pagechange', () => normalizeTree());
  normalizeTree();

  window.XHFrontendNextN14 = {
    normalize: normalizeTree,
    normalizedHref
  };
})();
