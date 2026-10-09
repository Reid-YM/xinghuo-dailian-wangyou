/* Mobile Home V2 runtime — intentionally independent from mobile-app.js and legacy home shells. */
(() => {
  const layer = () => document.getElementById('heroBackground');
  let applying = false;

  function enforceArtwork() {
    const target = layer();
    if (!target || applying) return;
    applying = true;
    try {
      target.style.setProperty('background-size', 'cover', 'important');
      target.style.setProperty('background-repeat', 'no-repeat', 'important');
      target.style.setProperty('filter', 'none', 'important');
      target.style.setProperty('opacity', '1', 'important');
      target.style.setProperty('inset', '0', 'important');
      target.style.setProperty('width', '100%', 'important');
      target.style.setProperty('height', '100%', 'important');
      target.style.setProperty('transform', 'none', 'important');
      target.style.setProperty('transition', 'none', 'important');
    } finally {
      applying = false;
    }
  }

  function bindCopy() {
    document.querySelectorAll('[data-mobile-home="v2"] [data-copy]').forEach(button => {
      if (button.dataset.mh2CopyBound === '1') return;
      button.dataset.mh2CopyBound = '1';
      button.addEventListener('click', async () => {
        const target = document.querySelector(button.dataset.copy || '');
        const text = target?.textContent?.trim();
        if (!text) return;
        try {
          await navigator.clipboard.writeText(text);
          const old = button.textContent;
          button.textContent = '已复制';
          window.setTimeout(() => { button.textContent = old; }, 1200);
        } catch (_) {
          // Clipboard can be unavailable in embedded browsers; keep the visible value as fallback.
        }
      });
    });
  }

  function boot() {
    const target = layer();
    if (target) {
      enforceArtwork();
      const observer = new MutationObserver(enforceArtwork);
      observer.observe(target, { attributes: true, attributeFilter: ['style', 'class'] });
    }
    bindCopy();
  }

  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', boot, { once: true });
  else boot();
})();
