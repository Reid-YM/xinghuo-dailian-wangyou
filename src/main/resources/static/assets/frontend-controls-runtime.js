(() => {
  'use strict';
  const $$ = (selector, root = document) => [...root.querySelectorAll(selector)];
  const bool = (value, fallback = true) => value == null || value === '' ? fallback : String(value).toLowerCase() === 'true';
  const text = (selector, value, root = document) => {
    if (value == null || value === '') return;
    $$(selector, root).forEach(node => { node.textContent = value; });
  };
  const visible = (selector, show, root = document) => $$(selector, root).forEach(node => { node.hidden = !show; });
  const safeHref = value => {
    const href = String(value || '').trim();
    if (href.startsWith('#')) return href.length > 1 ? href : null;
    if (href.startsWith('/')) return href.startsWith('//') ? null : href;
    return href.startsWith('https://') ? href : null;
  };
  const href = (selector, value, root = document) => {
    const safe = safeHref(value);
    if (safe) $$(selector, root).forEach(node => node.setAttribute('href', safe));
  };

  function rgb(hex) {
    const match = /^#([0-9a-f]{6})$/i.exec(String(hex || '').trim());
    if (!match) return null;
    const value = Number.parseInt(match[1], 16);
    return `${value >> 16 & 255},${value >> 8 & 255},${value & 255}`;
  }

  function applyTheme(control = {}) {
    const root = document.documentElement;
    const accent = String(control.themeAccentColor || '').trim();
    const accentRgb = rgb(accent);
    if (accentRgb) {
      root.style.setProperty('--accent-rgb', accentRgb);
      ['--accent', '--accent-hover', '--accent-text', '--yellow', '--yellow-2'].forEach(key => root.style.setProperty(key, accent));
    }
    const background = String(control.themeBackgroundColor || '').trim();
    const surface = String(control.themeSurfaceColor || '').trim();
    const foreground = String(control.themeTextColor || '').trim();
    if (rgb(background)) root.style.setProperty('--bg', background);
    if (rgb(surface)) root.style.setProperty('--surface-2', surface);
    if (rgb(foreground)) root.style.setProperty('--text', foreground);

    const radius = Number(control.themeRadius);
    const width = Number(control.themeContentWidth);
    const scale = Number(control.themeFontScale);
    if (radius >= 6 && radius <= 30) root.style.setProperty('--radius', `${radius}px`);
    if (width >= 960 && width <= 1440) root.style.setProperty('--max', `${width}px`);
    if (scale >= 90 && scale <= 115) root.style.fontSize = `${scale}%`;

    if (!document.getElementById('frontendMotionControlStyle')) {
      const style = document.createElement('style');
      style.id = 'frontendMotionControlStyle';
      style.textContent = '.frontend-motion-off *, .frontend-motion-off *::before, .frontend-motion-off *::after{animation-duration:.001ms!important;animation-iteration-count:1!important;transition-duration:.001ms!important;scroll-behavior:auto!important}';
      document.head.appendChild(style);
    }
    root.classList.toggle('frontend-motion-off', !bool(control.motionEnabled));
  }

  function applyHome(control = {}) {
    if (location.pathname !== '/' && !document.querySelector('[data-frontend=next] .page[data-page=home]')) return;
    visible('#homeHero', bool(control.homeHeroVisible));
    text('#homeHero .hero-kicker', control.homeHeroKicker);
    visible('#homeHero .hero-promise', bool(control.homeHeroPromisesVisible));
    visible('#homeHero .hero-media-panel', bool(control.homeHeroMediaVisible));

    text('#homeHero [data-primary-cta]', control.homePrimaryCtaLabel);
    href('#homeHero [data-primary-cta]', control.homePrimaryCtaHref);
    const secondary = '#homeHero .hero-actions .btn.ghost:not(.hero-progress-link)';
    visible(secondary, bool(control.homeSecondaryCtaVisible));
    text(secondary, control.homeSecondaryCtaLabel);
    href(secondary, control.homeSecondaryCtaHref);
    visible('#homeHero .hero-progress-link', bool(control.homeProgressCtaVisible));
    text('#homeHero .hero-progress-link', control.homeProgressCtaLabel);
    href('#homeHero .hero-progress-link', control.homeProgressCtaHref);

    visible('.quote-card-home', bool(control.homeQuotePanelVisible));
    text('.quote-card-home .quote-card-head .eyebrow', control.homeQuoteEyebrow);
    text('.quote-card-home .quote-card-head h2', control.homeQuoteTitle);
    visible('.desktop-secondary-panel', bool(control.homeRecordPanelVisible));
    text('.desktop-secondary-panel .quote-card-head .eyebrow', control.homeRecordEyebrow);
    text('.desktop-secondary-panel .quote-card-head h2', control.homeRecordTitle);

    visible('#gameEntrySection', bool(control.homeGameEntryVisible));
    text('#gameEntrySection .game-entry-head .eyebrow', control.homeGameEyebrow);
    text('#gameEntrySection .game-entry-head h2', control.homeGameTitle);
    text('#gameEntrySection .game-entry-head .text-link', control.homeGameLinkLabel);
    href('#gameEntrySection .game-entry-head .text-link', control.homeGameLinkHref);

    const why = document.querySelector('main.container section[aria-label="为什么选择星火"], [data-frontend=next] section[aria-label="为什么选择星火"]');
    if (why) {
      why.hidden = !bool(control.homeWhyVisible) || why.dataset.empty === 'true';
      text('.section-title .eyebrow', control.homeWhyEyebrow, why);
      text('.section-title h2', control.homeWhyTitle, why);
      text('.section-title .text-link', control.homeWhyLinkLabel, why);
      href('.section-title .text-link', control.homeWhyLinkHref, why);
    }
  }

  async function load() {
    try {
      const response = await fetch('/api/public/frontend-controls', { cache: 'no-store' });
      if (!response.ok) return;
      const control = await response.json();
      const apply = () => { applyTheme(control || {}); applyHome(control || {}); };
      apply();
      window.addEventListener('xh:pagechange', apply);
      window.addEventListener('xh:site-config', apply);
      [120, 600, 1500].forEach(delay => setTimeout(apply, delay));
    } catch (_) {
      // 配置层异常时保留模板默认值，不阻断任何业务页面。
    }
  }

  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', load, { once: true });
  else load();
})();
