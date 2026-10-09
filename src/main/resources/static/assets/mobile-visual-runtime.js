/* Mobile-only independent visual overrides. Desktop never loads this asset. */
(() => {
  let config = null;
  let applying = false;

  const clamp = (value, min, max, fallback) => {
    const n = Number(value);
    return Number.isFinite(n) ? Math.max(min, Math.min(max, n)) : fallback;
  };

  function apply() {
    if (!config || applying) return;
    const hero = document.getElementById('homeHero');
    const layer = document.getElementById('heroBackground');
    if (!hero || !layer) return;

    applying = true;
    try {
      const opacityPercent = clamp(config.heroBackgroundMobileOpacity, 0, 100, 90);
      const blur = clamp(config.heroBackgroundMobileBlur, 0, 24, 0);
      const opacity = opacityPercent / 100;
      const dim = Math.max(.18, .58 - opacity * .4);
      const brightness = Math.max(1.02, Math.min(1.22, .98 + opacity * .22));
      const foregroundFilter = `brightness(${brightness}) saturate(1.04) blur(${blur}px)`;

      if (layer.style.getPropertyValue('opacity') !== String(opacity)
          || layer.style.getPropertyPriority('opacity') !== 'important') {
        layer.style.setProperty('opacity', String(opacity), 'important');
      }
      if (layer.style.getPropertyValue('filter') !== foregroundFilter
          || layer.style.getPropertyPriority('filter') !== 'important') {
        layer.style.setProperty('filter', foregroundFilter, 'important');
      }
      hero.style.setProperty('--image-text-dim', String(dim));

      const backdrop = hero.querySelector(':scope > .mobile-hero-backdrop');
      if (backdrop) {
        const backdropOpacity = Math.max(0, Math.min(.62, opacity * .62));
        const backdropFilter = `blur(${12 + blur}px) brightness(.68) saturate(1.08)`;
        if (backdrop.style.opacity !== String(backdropOpacity)) {
          backdrop.style.opacity = String(backdropOpacity);
        }
        if (backdrop.style.filter !== backdropFilter) {
          backdrop.style.filter = backdropFilter;
        }
      }
    } finally {
      applying = false;
    }
  }

  async function load() {
    try {
      const response = await fetch('/api/public/mobile-visual-config', { cache: 'no-store' });
      if (!response.ok) return;
      config = await response.json();
      apply();
    } catch (_) {
      // Fail open: shared desktop values from site-config remain in effect.
    }
  }

  function boot() {
    const layer = document.getElementById('heroBackground');
    const hero = document.getElementById('homeHero');
    if (!layer || !hero) return;

    const observer = new MutationObserver(apply);
    observer.observe(layer, { attributes: true, attributeFilter: ['style', 'class'] });
    const heroObserver = new MutationObserver(apply);
    heroObserver.observe(hero, { childList: true });
    load();
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', boot, { once: true });
  } else {
    boot();
  }
})();
