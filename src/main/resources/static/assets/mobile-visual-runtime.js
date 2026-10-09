/* Mobile-only independent visual overrides. Desktop never loads this asset. */
(() => {
  let config = null;
  let applying = false;

  const clamp = (value, min, max, fallback) => {
    const n = Number(value);
    return Number.isFinite(n) ? Math.max(min, Math.min(max, n)) : fallback;
  };

  const setImportant = (element, name, value) => {
    if (!element) return;
    const next = String(value);
    if (element.style.getPropertyValue(name) === next
        && element.style.getPropertyPriority(name) === 'important') return;
    element.style.setProperty(name, next, 'important');
  };

  const isCompactHomeHero = () => document.body
    && document.body.classList.contains('mobile-frontend')
    && document.body.dataset.page === 'home';

  function enforceCompactHomeHero(hero, layer) {
    const positionX = clamp(config?.heroBackgroundMobilePositionX, 0, 100, 50);
    const positionY = clamp(config?.heroBackgroundMobilePositionY, 0, 100, 50);

    /* The mobile homepage Hero has its own existing UI geometry. Public image
       controls may still update the configured image URL/position, but they
       must not stretch or blur that artwork across the title/action deck. */
    setImportant(layer, 'position', 'absolute');
    setImportant(layer, 'inset', 'auto');
    setImportant(layer, 'top', 'var(--xh-r4-nav-h)');
    setImportant(layer, 'left', '0');
    setImportant(layer, 'right', '0');
    setImportant(layer, 'bottom', 'auto');
    setImportant(layer, 'width', '100%');
    setImportant(layer, 'height', 'var(--xh-r4-media-h)');
    setImportant(layer, 'min-height', 'var(--xh-r4-media-h)');
    setImportant(layer, 'max-height', 'var(--xh-r4-media-h)');
    setImportant(layer, 'background-size', 'cover');
    setImportant(layer, 'background-position', `${positionX}% ${positionY}%`);
    setImportant(layer, 'background-repeat', 'no-repeat');
    setImportant(layer, 'opacity', '1');
    setImportant(layer, 'filter', 'none');
    setImportant(layer, 'transform', 'none');
    setImportant(layer, 'transition', 'none');

    hero.style.removeProperty('--hero-mobile-background-height');
    hero.style.setProperty('--image-text-dim', '0');

    const backdrop = hero.querySelector(':scope > .mobile-hero-backdrop');
    if (backdrop) {
      setImportant(backdrop, 'display', 'none');
      setImportant(backdrop, 'opacity', '0');
      setImportant(backdrop, 'filter', 'none');
      setImportant(backdrop, 'background-image', 'none');
      setImportant(backdrop, 'transform', 'none');
    }
  }

  function apply() {
    if (applying) return;
    const hero = document.getElementById('homeHero');
    const layer = document.getElementById('heroBackground');
    if (!hero || !layer) return;

    applying = true;
    try {
      if (isCompactHomeHero()) {
        enforceCompactHomeHero(hero, layer);
        return;
      }
      if (!config) return;

      const opacityPercent = clamp(config.heroBackgroundMobileOpacity, 0, 100, 90);
      const blur = clamp(config.heroBackgroundMobileBlur, 0, 24, 0);
      const opacity = opacityPercent / 100;
      const dim = Math.max(.18, .58 - opacity * .4);
      const brightness = Math.max(1.02, Math.min(1.22, .98 + opacity * .22));
      const foregroundFilter = `brightness(${brightness}) saturate(1.04) blur(${blur}px)`;

      setImportant(layer, 'opacity', opacity);
      setImportant(layer, 'filter', foregroundFilter);
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
      /* The homepage compact Hero is enforced before this request, so a
         config/network failure cannot restore the old blurred filler. */
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

    apply();
    load();
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', boot, { once: true });
  } else {
    boot();
  }
})();
