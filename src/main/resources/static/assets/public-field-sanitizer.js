(() => {
  const serviceSlugFromHref = card => {
    const href = String(card.getAttribute('href') || '');
    const match = href.match(/^\/service\/([^?#/]+)/);
    if (!match) return '';
    try { return decodeURIComponent(match[1]); } catch { return match[1]; }
  };

  const sanitizeCard = card => {
    if (!(card instanceof Element)) return;
    const slug = serviceSlugFromHref(card);
    card.removeAttribute('data-service-id');

    if (card.getAttribute('data-track') === 'service_card_click') {
      if (slug) card.setAttribute('data-ref', slug);
      else card.removeAttribute('data-ref');
    }
  };

  const sanitize = root => {
    if (!root) return;
    if (root instanceof Element && root.hasAttribute('data-service-id')) sanitizeCard(root);
    root.querySelectorAll?.('[data-service-id]').forEach(sanitizeCard);
  };

  const start = () => {
    sanitize(document);
    if (!document.body) return;
    new MutationObserver(records => {
      records.forEach(record => record.addedNodes.forEach(node => {
        if (node.nodeType === Node.ELEMENT_NODE) sanitize(node);
      }));
    }).observe(document.body, { childList: true, subtree: true });
  };

  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', start, { once: true });
  else start();
})();
