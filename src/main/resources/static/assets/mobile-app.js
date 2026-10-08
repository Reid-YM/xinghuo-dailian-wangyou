/* Mobile frontend only. Desktop pages never load this file. */
(() => {
  const root = document.documentElement;
  root.dataset.frontend = 'mobile';

  const boot = () => {
    document.body?.classList.add('mobile-frontend');
    document.body?.setAttribute('data-frontend', 'mobile');

    const setViewportUnit = () => {
      const height = window.visualViewport?.height || window.innerHeight || 0;
      if (height > 0) root.style.setProperty('--mobile-viewport-height', `${Math.round(height)}px`);
    };
    setViewportUnit();
    window.addEventListener('resize', setViewportUnit, { passive: true });
    window.visualViewport?.addEventListener('resize', setViewportUnit, { passive: true });

    document.querySelectorAll('[data-responsive-details]').forEach(details => {
      if (details instanceof HTMLDetailsElement && window.innerWidth < 768 && !details.dataset.mobileInitialised) {
        details.dataset.mobileInitialised = 'true';
      }
    });
  };

  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', boot, { once: true });
  else boot();
})();
